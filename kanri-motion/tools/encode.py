import os
import subprocess
import sys
import zipfile

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(BASE_DIR, "out")
DELIVERABLES_DIR = os.path.join(BASE_DIR, "deliverables")
CHUNKS_ALL = os.path.join(OUT_DIR, "chunks_all.mkv")
AUDIO_MASTER = os.path.join(BASE_DIR, "audio", "master.wav")

def encode_all():
    assert os.path.exists(CHUNKS_ALL), f"Missing {CHUNKS_ALL}"
    assert os.path.exists(AUDIO_MASTER), f"Missing {AUDIO_MASTER}"

    os.makedirs(DELIVERABLES_DIR, exist_ok=True)

    # 1. Master: 1440x1440, 60fps H.264 with audio
    master_mp4 = os.path.join(DELIVERABLES_DIR, "kanri_loop_1440_60.mp4")
    print(f"Encoding Master: {master_mp4}...")
    cmd_master = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", CHUNKS_ALL,
        "-i", AUDIO_MASTER,
        "-vf", "scale=in_range=full:out_range=tv:out_color_matrix=bt709:flags=lanczos+accurate_rnd+full_chroma_int,format=yuv420p",
        "-sws_dither", "ed",
        "-c:v", "libx264", "-preset", "slow", "-crf", "14", "-profile:v", "high", "-level", "5.0",
        "-x264-params", "aq-mode=3:deblock=-1,-1", "-r", "60",
        "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv",
        "-c:a", "aac", "-b:a", "320k", "-ar", "48000", "-shortest", "-movflags", "+faststart",
        master_mp4
    ]
    subprocess.run(cmd_master, check=True)
    print(f"Master encoded successfully ({os.path.getsize(master_mp4) / 1024 / 1024:.2f} MB)")

    # 2. 1080p Web/Social version with audio
    p1080_mp4 = os.path.join(DELIVERABLES_DIR, "kanri_loop_1080_60.mp4")
    print(f"Encoding 1080p: {p1080_mp4}...")
    cmd_1080 = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", CHUNKS_ALL,
        "-i", AUDIO_MASTER,
        "-vf", "scale=1080:1080:flags=lanczos,scale=in_range=full:out_range=tv:out_color_matrix=bt709:flags=lanczos+accurate_rnd+full_chroma_int,format=yuv420p",
        "-sws_dither", "ed",
        "-c:v", "libx264", "-preset", "slow", "-crf", "14", "-profile:v", "high", "-level", "5.0",
        "-x264-params", "aq-mode=3:deblock=-1,-1", "-r", "60",
        "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv",
        "-c:a", "aac", "-b:a", "320k", "-ar", "48000", "-shortest", "-movflags", "+faststart",
        p1080_mp4
    ]
    subprocess.run(cmd_1080, check=True)
    print(f"1080p encoded successfully ({os.path.getsize(p1080_mp4) / 1024 / 1024:.2f} MB)")

    # 3. Silent 1440p version
    silent_mp4 = os.path.join(DELIVERABLES_DIR, "kanri_loop_silent_1440.mp4")
    print(f"Encoding Silent: {silent_mp4}...")
    cmd_silent = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", CHUNKS_ALL,
        "-vf", "scale=in_range=full:out_range=tv:out_color_matrix=bt709:flags=lanczos+accurate_rnd+full_chroma_int,format=yuv420p",
        "-sws_dither", "ed",
        "-c:v", "libx264", "-preset", "slow", "-crf", "14", "-profile:v", "high", "-level", "5.0",
        "-x264-params", "aq-mode=3:deblock=-1,-1", "-r", "60",
        "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv",
        "-an", "-movflags", "+faststart",
        silent_mp4
    ]
    subprocess.run(cmd_silent, check=True)
    print(f"Silent encoded successfully ({os.path.getsize(silent_mp4) / 1024 / 1024:.2f} MB)")

    # 4. WebM (VP9, no audio)
    webm_path = os.path.join(DELIVERABLES_DIR, "kanri_loop.webm")
    print(f"Encoding WebM: {webm_path}...")
    cmd_webm = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", CHUNKS_ALL,
        "-c:v", "libvpx-vp9", "-crf", "24", "-b:v", "0", "-row-mt", "1", "-an",
        webm_path
    ]
    subprocess.run(cmd_webm, check=True)
    print(f"WebM encoded successfully ({os.path.getsize(webm_path) / 1024 / 1024:.2f} MB)")

    # 5. Palette-optimized GIF (640px, 24 fps, <= 12 MB)
    gif_path = os.path.join(DELIVERABLES_DIR, "kanri_preview_640.gif")
    print(f"Encoding GIF: {gif_path}...")
    cmd_gif = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-i", CHUNKS_ALL,
        "-vf", "fps=24,scale=640:640:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=4",
        "-loop", "0",
        gif_path
    ]
    subprocess.run(cmd_gif, check=True)
    gif_size_mb = os.path.getsize(gif_path) / 1024 / 1024
    print(f"GIF encoded: {gif_size_mb:.2f} MB")
    if gif_size_mb > 12.0:
        print("GIF exceeded 12 MB, optimizing at 540px 20fps...")
        cmd_gif_opt = [
            "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
            "-i", CHUNKS_ALL,
            "-vf", "fps=20,scale=540:540:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=4",
            "-loop", "0",
            gif_path
        ]
        subprocess.run(cmd_gif_opt, check=True)
        print(f"Optimized GIF: {os.path.getsize(gif_path) / 1024 / 1024:.2f} MB")

    # 6. Source Zip Package
    zip_path = os.path.join(DELIVERABLES_DIR, "source.zip")
    print(f"Packaging source to {zip_path}...")
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
        for root, dirs, files in os.walk(BASE_DIR):
            # Exclude out/, .venv/, __pycache__, deliverables
            rel_root = os.path.relpath(root, BASE_DIR)
            parts = rel_root.split(os.sep)
            if parts[0] in ["out", ".venv", "__pycache__", "deliverables"]:
                continue
            if any(p == "__pycache__" for p in parts):
                continue
            for file in files:
                if file.endswith((".pyc", ".pyo")):
                    continue
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, BASE_DIR)
                z.write(file_path, arcname)
    print(f"Source package created ({os.path.getsize(zip_path) / 1024 / 1024:.2f} MB)")

if __name__ == "__main__":
    encode_all()

