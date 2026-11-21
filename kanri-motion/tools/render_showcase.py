import argparse
import concurrent.futures
import os
import shutil
import subprocess
import sys
import time
from playwright.sync_api import sync_playwright

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(BASE_DIR, "out", "showcase")
CHUNKS_DIR = os.path.join(OUT_DIR, "chunks")
DELIVERABLES_DIR = os.path.join(BASE_DIR, "deliverables")
AUDIO_FILE = os.path.join(BASE_DIR, "audio", "reference_audio.wav")

NUM_CHUNKS = 15
FPS = 60
FRAMES_PER_CHUNK = 60
TOTAL_FRAMES = NUM_CHUNKS * FRAMES_PER_CHUNK  # 900 frames = 15.000s
W = 1920
H = 1080

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
        return int(res.stdout.strip()) == expected_frames
    except Exception:
        return False

def render_chunk(chunk_idx):
    chunk_file = os.path.join(CHUNKS_DIR, f"chunk_{chunk_idx:02d}.mkv")
    if verify_chunk_frames(chunk_file, 60):
        print(f"[Showcase Chunk {chunk_idx:02d}] Already exists and verified with 60 frames. Skipping.")
        return chunk_idx

    chunk_sub_dir = os.path.join(CHUNKS_DIR, f"frames_{chunk_idx:02d}")
    os.makedirs(chunk_sub_dir, exist_ok=True)

    start_frame = chunk_idx * 60
    end_frame = start_frame + 60

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
        page = browser.new_page(viewport={"width": W, "height": H}, device_scale_factor=1)
        page.goto("http://127.0.0.1:8765/showcase.html")
        page.wait_for_function("window.__ready === true", timeout=15000)

        for f in range(start_frame, end_frame):
            t = f / float(FPS)
            t_wrapped = ((t % 15.000) + 15.000) % 15.000
            page.evaluate(f"window.seek({t_wrapped})")
            png_path = os.path.join(chunk_sub_dir, f"frame_{f - start_frame:04d}.png")
            page.screenshot(path=png_path)

        browser.close()

    render_dur = time.time() - t0

    # Encode chunk to lossless FFV1
    encode_cmd = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-framerate", "60",
        "-i", os.path.join(chunk_sub_dir, "frame_%04d.png"),
        "-c:v", "ffv1",
        chunk_file
    ]
    subprocess.run(encode_cmd, check=True)
    shutil.rmtree(chunk_sub_dir, ignore_errors=True)

    is_valid = verify_chunk_frames(chunk_file, 60)
    print(f"[Showcase Chunk {chunk_idx:02d}] Done in {render_dur:.1f}s. Valid: {is_valid}")
    return chunk_idx

def render_all_chunks(workers=8):
    os.makedirs(CHUNKS_DIR, exist_ok=True)
    os.makedirs(DELIVERABLES_DIR, exist_ok=True)

    print(f"--- STARTING SHOWCASE PARALLEL RENDER ({NUM_CHUNKS} chunks, {workers} workers) ---")
    start_t = time.time()

    with concurrent.futures.ProcessPoolExecutor(max_workers=workers) as executor:
        futures = {executor.submit(render_chunk, c): c for c in range(NUM_CHUNKS)}
        for f in concurrent.futures.as_completed(futures):
            c_idx = futures[f]
            try:
                f.result()
            except Exception as e:
                print(f"[Showcase Chunk {c_idx:02d}] Retrying due to {e}")
                render_chunk(c_idx)

    total_time = time.time() - start_t
    print(f"All {NUM_CHUNKS} chunks rendered in {total_time:.1f}s!")

    # Losslessly concatenate chunks
    concat_list = os.path.join(OUT_DIR, "chunks_list.txt")
    with open(concat_list, "w", encoding="utf-8") as f:
        for c in range(NUM_CHUNKS):
            c_path = os.path.abspath(os.path.join(CHUNKS_DIR, f"chunk_{c:02d}.mkv")).replace("\\", "/")
            f.write(f"file '{c_path}'\n")

    chunks_all = os.path.join(OUT_DIR, "showcase_all.mkv")
    concat_cmd = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", concat_list,
        "-c", "copy",
        chunks_all
    ]
    subprocess.run(concat_cmd, check=True)
    print(f"Concatenated into {chunks_all}")
    return chunks_all

def encode_showcase_deliverables(chunks_all):
    print("--- ENCODING SHOWCASE DELIVERABLES ---")

    # 1. Master 1920x1080 60fps with audio
    master_mp4 = os.path.join(DELIVERABLES_DIR, "kanri_showcase_1920_60.mp4")
    cmd_master = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", chunks_all,
        "-i", AUDIO_FILE,
        "-vf", "scale=in_range=full:out_range=tv:out_color_matrix=bt709:flags=lanczos,format=yuv420p",
        "-sws_dither", "ed",
        "-c:v", "libx264", "-preset", "slow", "-crf", "14", "-profile:v", "high", "-level", "5.0",
        "-x264-params", "aq-mode=3:deblock=-1,-1", "-r", "60",
        "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv",
        "-c:a", "aac", "-b:a", "320k", "-ar", "48000", "-shortest", "-movflags", "+faststart",
        master_mp4
    ]
    subprocess.run(cmd_master, check=True)
    print(f"Master Showcase MP4: {master_mp4} ({os.path.getsize(master_mp4) / 1024 / 1024:.2f} MB)")

    # 2. 1080x1080 Square Cut
    square_mp4 = os.path.join(DELIVERABLES_DIR, "kanri_showcase_1080_60.mp4")
    cmd_square = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", chunks_all,
        "-i", AUDIO_FILE,
        "-vf", "crop=1080:1080:(in_w-1080)/2:0,scale=in_range=full:out_range=tv:out_color_matrix=bt709:flags=lanczos,format=yuv420p",
        "-sws_dither", "ed",
        "-c:v", "libx264", "-preset", "slow", "-crf", "14", "-profile:v", "high", "-level", "5.0",
        "-x264-params", "aq-mode=3:deblock=-1,-1", "-r", "60",
        "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv",
        "-c:a", "aac", "-b:a", "320k", "-ar", "48000", "-shortest", "-movflags", "+faststart",
        square_mp4
    ]
    subprocess.run(cmd_square, check=True)
    print(f"Square Showcase MP4: {square_mp4} ({os.path.getsize(square_mp4) / 1024 / 1024:.2f} MB)")

    # 3. Silent Web Video
    silent_mp4 = os.path.join(DELIVERABLES_DIR, "kanri_showcase_silent.mp4")
    cmd_silent = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", chunks_all,
        "-vf", "scale=in_range=full:out_range=tv:out_color_matrix=bt709:flags=lanczos,format=yuv420p",
        "-sws_dither", "ed",
        "-c:v", "libx264", "-preset", "slow", "-crf", "14", "-profile:v", "high", "-level", "5.0",
        "-x264-params", "aq-mode=3:deblock=-1,-1", "-r", "60",
        "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv",
        "-an", "-movflags", "+faststart",
        silent_mp4
    ]
    subprocess.run(cmd_silent, check=True)
    print(f"Silent Showcase MP4: {silent_mp4} ({os.path.getsize(silent_mp4) / 1024 / 1024:.2f} MB)")

    # 4. WebM (VP9)
    webm_file = os.path.join(DELIVERABLES_DIR, "kanri_showcase.webm")
    cmd_webm = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", chunks_all,
        "-c:v", "libvpx-vp9", "-crf", "24", "-b:v", "0", "-cpu-used", "4", "-row-mt", "1", "-threads", "8", "-an",
        webm_file
    ]
    subprocess.run(cmd_webm, check=True)
    print(f"WebM Showcase: {webm_file} ({os.path.getsize(webm_file) / 1024 / 1024:.2f} MB)")

    # 5. Palette-optimized GIF (640x360, 24fps)
    gif_file = os.path.join(DELIVERABLES_DIR, "kanri_showcase.gif")
    cmd_gif = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", chunks_all,
        "-vf", "fps=24,scale=640:-1:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=4",
        "-loop", "0",
        gif_file
    ]
    subprocess.run(cmd_gif, check=True)
    print(f"Showcase GIF: {gif_file} ({os.path.getsize(gif_file) / 1024 / 1024:.2f} MB)")

    # 6. Poster Frame (t=2.5s Brand Slam)
    poster_png = os.path.join(DELIVERABLES_DIR, "showcase_poster.png")
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": W, "height": H})
        page.goto("http://127.0.0.1:8765/showcase.html")
        page.wait_for_function("window.__ready === true")
        page.evaluate("window.seek(2.50)")
        page.screenshot(path=poster_png)
        browser.close()
    print(f"Showcase Poster: {poster_png}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers", type=int, default=8)
    args = parser.parse_args()

    chunks_all = render_all_chunks(workers=args.workers)
    encode_showcase_deliverables(chunks_all)

