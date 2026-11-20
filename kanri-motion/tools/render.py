import argparse
import concurrent.futures
import glob
import json
import os
import shutil
import subprocess
import sys
import time
from playwright.sync_api import sync_playwright

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(BASE_DIR, "out")
CHUNKS_DIR = os.path.join(OUT_DIR, "chunks")
DELIVERABLES_DIR = os.path.join(BASE_DIR, "deliverables")

CHOP_SECONDS = 1.0
TOTAL_SECONDS = 16.0
NUM_CHUNKS = 16
FPS = 60
SUBFRAMES = 4
SHUTTER = 0.5  # 180 degrees

def verify_chunk_frames(chunk_path, expected_frames=60):
    if not os.path.exists(chunk_path):
        return False
    cmd = [
        "ffprobe", "-v", "error", "-select_streams", "v:0",
        "-count_packets", "-show_entries", "stream=nb_read_packets",
        "-of", "csv=p=0", chunk_path
    ]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        count = int(res.stdout.strip())
        return count == expected_frames
    except Exception:
        return False

def render_chunk(chunk_idx):
    chunk_file = os.path.join(CHUNKS_DIR, f"chunk_{chunk_idx:02d}.mkv")
    if verify_chunk_frames(chunk_file, 60):
        print(f"[Chunk {chunk_idx:02d}] Already exists and verified with 60 frames. Skipping.")
        return chunk_idx

    chunk_sub_dir = os.path.join(CHUNKS_DIR, f"sub_{chunk_idx:02d}")
    os.makedirs(chunk_sub_dir, exist_ok=True)

    start_frame = chunk_idx * 60
    end_frame = start_frame + 60
    W = (SHUTTER / FPS)

    t0 = time.time()
    with sync_playwright() as p:
        browser = p.chromium.launch(
            args=[
                "--force-color-profile=srgb",
                "--font-render-hinting=none",
                "--disable-lcd-text",
                "--hide-scrollbars",
                "--run-all-compositor-stages-before-draw"
            ]
        )
        page = browser.new_page(viewport={"width": 1440, "height": 1440}, device_scale_factor=1)
        page.goto("http://127.0.0.1:8765/index.html")
        page.wait_for_function("window.__ready === true", timeout=15000)

        sub_idx = 0
        for f in range(start_frame, end_frame):
            for k in range(SUBFRAMES):
                t_sub = (f / FPS) + ((k + 0.5) / SUBFRAMES) * W - (W / 2.0)
                t_wrapped = ((t_sub % TOTAL_SECONDS) + TOTAL_SECONDS) % TOTAL_SECONDS
                page.evaluate(f"window.seek({t_wrapped})")
                png_path = os.path.join(chunk_sub_dir, f"sub_{sub_idx:05d}.png")
                page.screenshot(path=png_path)
                sub_idx += 1

        browser.close()

    render_dur = time.time() - t0

    # Blend subframes with ffmpeg tmix and encode to FFV1
    blend_cmd = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-framerate", "240",
        "-i", os.path.join(chunk_sub_dir, "sub_%05d.png"),
        "-vf", "tmix=frames=4:weights='1 1 1 1',select='eq(mod(n\\,4)\\,3)',setpts=N/(60*TB)",
        "-r", "60",
        "-c:v", "ffv1",
        chunk_file
    ]
    subprocess.run(blend_cmd, check=True)

    # Clean up subframes
    shutil.rmtree(chunk_sub_dir, ignore_errors=True)

    is_valid = verify_chunk_frames(chunk_file, 60)
    print(f"[Chunk {chunk_idx:02d}] Done in {render_dur:.1f}s. Valid 60 frames: {is_valid}")
    return chunk_idx

def render_draft(workers=4):
    print("--- RENDERING DRAFT (480x480, 30 fps) ---")
    draft_dir = os.path.join(OUT_DIR, "draft_frames")
    os.makedirs(draft_dir, exist_ok=True)
    draft_mp4 = os.path.join(OUT_DIR, "draft.mp4")
    audio_wav = os.path.join(BASE_DIR, "audio", "master.wav")

    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 480, "height": 480})
        page.goto("http://127.0.0.1:8765/index.html")
        page.wait_for_function("window.__ready === true")

        total_frames = 16 * 30  # 480 frames
        for f in range(total_frames):
            t = f / 30.0
            page.evaluate(f"window.seek({t})")
            page.screenshot(path=os.path.join(draft_dir, f"draft_{f:04d}.png"))
        browser.close()

    cmd = [
        "ffmpeg", "-y", "-framerate", "30",
        "-i", os.path.join(draft_dir, "draft_%04d.png"),
        "-i", audio_wav,
        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "22",
        "-c:a", "aac", "-shortest",
        draft_mp4
    ]
    subprocess.run(cmd, check=True)
    shutil.rmtree(draft_dir, ignore_errors=True)
    print(f"Draft render complete: {draft_mp4}")

def render_final(workers=8):
    os.makedirs(CHUNKS_DIR, exist_ok=True)
    os.makedirs(DELIVERABLES_DIR, exist_ok=True)

    print(f"--- STARTING FINAL PARALLEL RENDER ({NUM_CHUNKS} chunks, {workers} workers) ---")
    start_time = time.time()

    with concurrent.futures.ProcessPoolExecutor(max_workers=workers) as executor:
        futures = {executor.submit(render_chunk, c): c for c in range(NUM_CHUNKS)}
        for f in concurrent.futures.as_completed(futures):
            c_idx = futures[f]
            try:
                res = f.result()
            except Exception as e:
                print(f"[Chunk {c_idx:02d}] FAILED with {e}. Retrying once...")
                render_chunk(c_idx)

    total_time = time.time() - start_time
    print(f"All {NUM_CHUNKS} chunks rendered in {total_time:.1f}s!")

    # Losslessly concatenate all 16 chunks
    concat_list = os.path.join(OUT_DIR, "chunks_list.txt")
    with open(concat_list, "w", encoding="utf-8") as f:
        for c in range(NUM_CHUNKS):
            c_path = os.path.abspath(os.path.join(CHUNKS_DIR, f"chunk_{c:02d}.mkv")).replace("\\", "/")
            f.write(f"file '{c_path}'\n")

    chunks_all = os.path.join(OUT_DIR, "chunks_all.mkv")
    concat_cmd = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", concat_list,
        "-c", "copy",
        chunks_all
    ]
    subprocess.run(concat_cmd, check=True)
    print(f"Losslessly concatenated chunks into {chunks_all}")

    # Extract poster at t=9.0s (Saturday hover)
    poster_png = os.path.join(DELIVERABLES_DIR, "poster.png")
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 1440})
        page.goto("http://127.0.0.1:8765/index.html")
        page.wait_for_function("window.__ready === true")
        page.evaluate("window.seek(9.00)")
        page.screenshot(path=poster_png)
        browser.close()
    print(f"Extracted poster frame to {poster_png}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["draft", "final"], default="final")
    parser.add_argument("--workers", type=int, default=8)
    args = parser.parse_args()

    if args.mode == "draft":
        render_draft(workers=args.workers)
    else:
        render_final(workers=args.workers)

