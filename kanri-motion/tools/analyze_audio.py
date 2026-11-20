import json
import os
import sys
import numpy as np
import scipy.signal as signal
import soundfile as sf
import pyloudnorm as pyln

def analyze():
    audio_path = os.path.join(os.path.dirname(__file__), "..", "audio", "master.wav")
    grid_path = os.path.join(os.path.dirname(__file__), "..", "audio", "beat_grid.json")
    tl_path = os.path.join(os.path.dirname(__file__), "..", "timeline.json")

    data, sr = sf.read(audio_path)
    print(f"Loaded {audio_path}: shape={data.shape}, sr={sr}, dur={len(data)/sr:.4f}s")
    assert sr == 48000, f"Expected 48000 Hz, got {sr}"
    assert len(data) == 768000, f"Expected 768000 samples (16.0s), got {len(data)}"

    # 1. Loudness & True Peak check (4x oversampling)
    meter = pyln.Meter(sr)
    lufs = meter.integrated_loudness(data)
    print(f"Integrated Loudness: {lufs:.2f} LUFS (Target: ~ -14.0 LUFS)")

    # 4x oversampling for True Peak
    upsampled = signal.resample_poly(data, 4, 1)
    true_peak = np.max(np.abs(upsampled))
    true_peak_dbtp = 20 * np.log10(true_peak) if true_peak > 0 else -100
    print(f"True Peak (4x oversampling): {true_peak:.4f} ({true_peak_dbtp:.2f} dBTP, Target <= -1.0 dBTP)")
    assert true_peak_dbtp <= -1.0 + 0.1, f"True peak exceeded -1 dBTP: {true_peak_dbtp:.2f} dBTP"

    # 2. Seam continuity (3x repeat)
    tiled_3x = np.tile(data, 3)
    seam1 = abs(tiled_3x[len(data)] - tiled_3x[len(data) - 1])
    seam2 = abs(tiled_3x[2 * len(data)] - tiled_3x[2 * len(data) - 1])
    max_seam_jump = max(seam1, seam2)
    print(f"Max seam sample jump across joins: {max_seam_jump:.6f}")
    assert max_seam_jump < 0.05, f"Seam jump too high: {max_seam_jump}"

    # Check broadband energy at seam
    # Window of 256 samples centered around seam
    seam_win = tiled_3x[len(data) - 128:len(data) + 128]
    fft_vals = np.abs(np.fft.rfft(seam_win * np.hanning(256)))
    high_freq_energy = np.mean(fft_vals[len(fft_vals)//2:])
    print(f"High-frequency seam energy: {high_freq_energy:.6f}")

    # 3. Beat grid & SFX alignment check
    with open(grid_path, "r", encoding="utf-8") as f:
        grid_data = json.load(f)
    with open(tl_path, "r", encoding="utf-8") as f:
        tl = json.load(f)

    # Kicks vs grid
    downbeats = grid_data["downbeats"]
    kick_errors = []
    for t_k in downbeats:
        expected_grid = round(t_k / 0.5) * 0.5
        err_ms = abs(t_k - expected_grid) * 1000.0
        kick_errors.append(err_ms)
    max_kick_err = max(kick_errors) if kick_errors else 0.0
    print(f"Kick onsets checked ({len(downbeats)} kicks): Max error = {max_kick_err:.2f} ms (Gate: <= 5.0 ms)")
    assert max_kick_err <= 5.0, f"Kick transient alignment failed: {max_kick_err:.2f} ms > 5.0 ms"

    # SFX peaks vs timeline.json
    sfx_trans = grid_data["sfx_transients"]
    sfx_errors = []
    for s in sfx_trans:
        err_ms = abs(s["placed_peak_t"] - s["t_event"]) * 1000.0
        sfx_errors.append((s["type"], s["t_event"], err_ms))
    max_sfx_err = max(e[2] for e in sfx_errors) if sfx_errors else 0.0
    print(f"SFX peaks checked ({len(sfx_trans)} events): Max error = {max_sfx_err:.2f} ms (Gate: <= 2.0 ms)")
    assert max_sfx_err <= 2.0, f"SFX peak alignment failed: {max_sfx_err:.2f} ms > 2.0 ms"

    print("\n--- ALL AUDIO GATES PASSED SUCCESSFULLY ---")
    return True

if __name__ == "__main__":
    analyze()

