import json
import math
import os
import numpy as np
import scipy.signal as signal
import soundfile as sf
import pyloudnorm as pyln

SR = 48000
BPM = 120
BEAT_DUR = 60.0 / BPM  # 0.500 s
TOTAL_DUR = 16.000     # 32 beats
RENDER_DUR = 24.000    # render 24s then wrap-add tail [16..24] onto [0..8]
NUM_SAMPLES = int(TOTAL_DUR * SR)
RENDER_SAMPLES = int(RENDER_DUR * SR)

np.random.seed(42)

# --- Procedural Instruments ---

def make_kick(duration=0.25):
    n = int(duration * SR)
    t = np.linspace(0, duration, n, endpoint=False)
    # Pitch drop 160 Hz -> 48 Hz exponential
    freq = 48.0 + (160.0 - 48.0) * np.exp(-t / 0.035)
    phase = 2.0 * np.pi * np.cumsum(freq) / SR
    # Body amp envelope
    amp = np.exp(-t / 0.065)
    # Click transient at start (first 5ms)
    click = np.sin(2.0 * np.pi * 1200.0 * t[:int(0.005 * SR)]) * np.exp(-t[:int(0.005 * SR)] / 0.001)
    kick = np.sin(phase) * amp
    kick[:len(click)] += 0.35 * click
    return kick

def make_hat(duration=0.06):
    n = int(duration * SR)
    noise = np.random.uniform(-1, 1, n)
    # Bandpass 7 kHz - 14 kHz
    sos = signal.butter(4, [7000 / (SR / 2), 14000 / (SR / 2)], btype='bandpass', output='sos')
    hat = signal.sosfilt(sos, noise)
    t = np.linspace(0, duration, n, endpoint=False)
    env = np.exp(-t / 0.018)
    return hat * env * 0.45

def make_rim(duration=0.08):
    n = int(duration * SR)
    t = np.linspace(0, duration, n, endpoint=False)
    # High resonance pop at 1100 Hz + noise
    tone = np.sin(2 * np.pi * 1100 * t) * np.exp(-t / 0.012)
    noise = np.random.uniform(-1, 1, n)
    sos = signal.butter(2, 3000 / (SR / 2), btype='highpass', output='sos')
    fnoise = signal.sosfilt(sos, noise) * np.exp(-t / 0.02)
    rim = tone * 0.7 + fnoise * 0.3
    return rim * 0.5

def make_bass_note(freq, duration=0.22):
    n = int(duration * SR)
    t = np.linspace(0, duration, n, endpoint=False)
    # Sine fundamental + soft 2nd harmonic (warm sub)
    wave = np.sin(2 * np.pi * freq * t) + 0.25 * np.sin(2 * np.pi * 2 * freq * t)
    env = np.minimum(t / 0.005, 1.0) * np.exp(-t / 0.12)
    return wave * env * 0.6

def make_pluck_note(freq, duration=0.35, cutoff=2500):
    n = int(duration * SR)
    t = np.linspace(0, duration, n, endpoint=False)
    # Bandlimited pulse / triangle
    wave = signal.sawtooth(2 * np.pi * freq * t, width=0.5)
    # Dynamic low-pass filter
    sos = signal.butter(2, min(cutoff, SR/2 - 100) / (SR / 2), btype='lowpass', output='sos')
    filtered = signal.sosfilt(sos, wave)
    env = np.minimum(t / 0.003, 1.0) * np.exp(-t / 0.08)
    return filtered * env * 0.3

# --- Procedural SFX ---

def make_sfx_click(pitch=2200, is_down=False):
    dur = 0.025
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    noise = np.random.uniform(-1, 1, n)
    sos = signal.butter(4, 3000 / (SR / 2), btype='highpass', output='sos')
    hp_noise = signal.sosfilt(sos, noise) * np.exp(-t / 0.003)
    tone = np.sin(2 * np.pi * pitch * t) * np.exp(-t / 0.010)
    sfx = hp_noise * 0.5 + tone * 0.6
    if is_down:
        sfx *= 0.85
    return sfx

def make_sfx_thock():
    dur = 0.080
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    freq = 90.0 + (200.0 - 90.0) * np.exp(-t / 0.025)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    env = np.minimum(t / 0.002, 1.0) * np.exp(-t / 0.035)
    return np.sin(phase) * env * 0.7

def make_sfx_chime():
    # Two soft notes A5 (880) -> E6 (1318.5)
    dur = 0.450
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    t1 = t
    t2 = np.maximum(0.0, t - 0.060)
    note1 = np.sin(2 * np.pi * 880 * t1) * np.exp(-t1 / 0.18)
    note2 = np.sin(2 * np.pi * 1318.51 * t2) * np.exp(-t2 / 0.22) * (t >= 0.060)
    chime = (note1 * 0.6 + note2 * 0.7) * 0.4
    return chime

def make_sfx_tick(freq=2600):
    dur = 0.020
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    tone = np.sin(2 * np.pi * freq * t) * np.exp(-t / 0.004)
    return tone * 0.4

def make_sfx_snap():
    dur = 0.060
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    click = make_sfx_click(pitch=3200)
    pop_freq = 140.0 + (350.0 - 140.0) * np.exp(-t / 0.015)
    pop = np.sin(2 * np.pi * np.cumsum(pop_freq) / SR) * np.exp(-t / 0.022)
    sfx = np.zeros(n)
    sfx[:len(click)] += click[:min(len(click), n)] * 0.7
    sfx += pop * 0.6
    return sfx

def make_sfx_toast():
    dur = 0.200
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    # Bell-like chime at 1760 Hz (A6) with 5th harmonic
    bell = (np.sin(2 * np.pi * 1760 * t) + 0.3 * np.sin(2 * np.pi * 2640 * t)) * np.exp(-t / 0.070)
    return bell * 0.45

def make_sfx_sweep(dur=0.150):
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    noise = np.random.uniform(-1, 1, n)
    sos = signal.butter(2, [1500 / (SR / 2), 6000 / (SR / 2)], btype='bandpass', output='sos')
    sweep = signal.sosfilt(sos, noise)
    # Parabolic swell
    env = np.sin(np.pi * t / dur) ** 2
    return sweep * env * 0.35

def make_sfx_whoosh(dur=0.200):
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    noise = np.random.uniform(-1, 1, n)
    sos = signal.butter(2, [400 / (SR / 2), 1800 / (SR / 2)], btype='bandpass', output='sos')
    whoosh = signal.sosfilt(sos, noise)
    env = np.sin(np.pi * t / dur) ** 2
    return whoosh * env * 0.30

def make_sfx_key():
    dur = 0.030
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    noise = np.random.uniform(-1, 1, n)
    sos = signal.butter(2, [1200 / (SR / 2), 5000 / (SR / 2)], btype='bandpass', output='sos')
    k = signal.sosfilt(sos, noise) * np.exp(-t / 0.007)
    thump = np.sin(2 * np.pi * 320 * t) * np.exp(-t / 0.009)
    return (k * 0.6 + thump * 0.5) * 0.4

def make_sfx_lock():
    dur = 0.120
    n = int(dur * SR)
    t = np.linspace(0, dur, n, endpoint=False)
    # Shackle drop (t=0) + lock click (t=0.04)
    click1 = make_sfx_click(pitch=1800)
    click2 = make_sfx_click(pitch=2800)
    # 1px micro-haptic settle: x = 1 * exp(-t/0.04) * sin(2*pi*38*t)
    t_settle = np.maximum(0, t - 0.040)
    settle = np.sin(2 * np.pi * 38 * t_settle) * np.exp(-t_settle / 0.04) * (t >= 0.040)
    sfx = np.zeros(n)
    l1 = min(len(click1), n)
    sfx[:l1] += click1[:l1] * 0.5
    idx2 = int(0.040 * SR)
    if idx2 < n:
        l2 = min(len(click2), n - idx2)
        sfx[idx2:idx2 + l2] += click2[:l2] * 0.8
    sfx += settle * 0.4
    return sfx

# --- Synthesize 24s Buffer ---

def synthesize_music_and_sfx(timeline_path):
    with open(timeline_path, "r", encoding="utf-8") as f:
        tl = json.load(f)

    music_buf = np.zeros(RENDER_SAMPLES, dtype=np.float32)
    sfx_buf = np.zeros(RENDER_SAMPLES, dtype=np.float32)
    sidechain_env = np.ones(RENDER_SAMPLES, dtype=np.float32)

    kick_sample = make_kick()
    hat_sample = make_hat()
    rim_sample = make_rim()

    # Track transient locations for verification
    kick_transients = []
    sfx_transients = []

    # 1. Synthesize Drums across 48 beats (24s)
    # Bar 1-2: beats 0..7
    # Bar 3: beats 8..11
    # Bar 4: beats 12..15
    # Bar 5: beats 16..19
    # Bar 6: beats 20..23
    # Bar 7: beats 24..27 (beats 26,27 kick drops)
    # Bar 8: beats 28..31
    # Repeat for bars 9-12 (beats 32..47)
    for b in range(48):
        t_beat = b * BEAT_DUR
        idx_beat = int(t_beat * SR)
        bar_idx = (b // 4) % 8
        beat_in_bar = b % 4

        # Kick logic
        has_kick = True
        if bar_idx == 6 and beat_in_bar in [2, 3]:  # Bar 7 kick drops on beats 3 & 4
            has_kick = False

        if has_kick:
            if idx_beat + len(kick_sample) <= RENDER_SAMPLES:
                music_buf[idx_beat:idx_beat + len(kick_sample)] += kick_sample * 0.9
                # Sidechain envelope: duck to 0.25 over 100ms
                duck_len = int(0.12 * SR)
                duck_curve = 0.25 + 0.75 * (np.linspace(0, 1, duck_len) ** 2)
                end_idx = min(idx_beat + duck_len, RENDER_SAMPLES)
                sidechain_env[idx_beat:end_idx] = np.minimum(sidechain_env[idx_beat:end_idx], duck_curve[:end_idx - idx_beat])
                if b < 32:
                    kick_transients.append(t_beat)

        # Off-beat 8th hats (at b * 0.5 + 0.25)
        t_hat = t_beat + 0.250
        idx_hat = int(t_hat * SR)
        if idx_hat + len(hat_sample) <= RENDER_SAMPLES:
            music_buf[idx_hat:idx_hat + len(hat_sample)] += hat_sample * 0.7

        # Rim / clap softly on beats 2 and 4 in bars 5, 6, 7
        if bar_idx in [4, 5, 6] and beat_in_bar in [1, 3]:
            if idx_beat + len(rim_sample) <= RENDER_SAMPLES:
                music_buf[idx_beat:idx_beat + len(rim_sample)] += rim_sample * 0.55

    # 2. Synthesize Bass (Bars 3-7, i.e., beats 8..27 and wraps)
    # A minor roots: A1 (55Hz), C2 (65.4Hz), D2 (73.4Hz), E2 (82.4Hz)
    bass_notes = [55.0, 55.0, 65.41, 82.41]
    bass_buf = np.zeros(RENDER_SAMPLES, dtype=np.float32)
    for b in range(48):
        bar_idx = (b // 4) % 8
        if bar_idx in [2, 3, 4, 5, 6]:
            # off-beat 8th bass
            t_bass = b * BEAT_DUR + 0.250
            idx_bass = int(t_bass * SR)
            freq = bass_notes[(b // 2) % len(bass_notes)]
            bn = make_bass_note(freq, duration=0.22)
            if idx_bass + len(bn) <= RENDER_SAMPLES:
                bass_buf[idx_bass:idx_bass + len(bn)] += bn

    # 3. Synthesize Pluck (Bars 4-6, i.e., beats 12..23 and wraps)
    # A minor pentatonic: A3 (220), C4 (261.6), D4 (293.7), E4 (329.6), G4 (392.0)
    pentatonic = [220.0, 261.63, 329.63, 392.00, 293.66, 329.63, 261.63, 220.0]
    pluck_buf = np.zeros(RENDER_SAMPLES, dtype=np.float32)
    for s_step in range(48 * 4):  # 16th notes
        t_16 = s_step * (BEAT_DUR / 4.0)
        b_idx = int(t_16 / BEAT_DUR)
        bar_idx = (b_idx // 4) % 8
        if bar_idx in [3, 4, 5]:
            idx_p = int(t_16 * SR)
            freq = pentatonic[s_step % len(pentatonic)]
            cutoff = 1800 if bar_idx in [3, 4] else 3800  # Bar 6 filter opens!
            pn = make_pluck_note(freq, duration=0.20, cutoff=cutoff)
            if idx_p + len(pn) <= RENDER_SAMPLES:
                pluck_buf[idx_p:idx_p + len(pn)] += pn

    # Apply sidechain to bass and pluck
    bass_buf *= sidechain_env
    pluck_buf *= sidechain_env
    music_buf += bass_buf + pluck_buf

    # 4. Synthesize SFX aligned with timeline.json cues
    # Wrap SFX cues across 0..24s
    sfx_samples_map = {
        "KICK": None,  # handled by music kick
        "CLK_DN": lambda **kw: make_sfx_click(pitch=1800, is_down=True),
        "CLK_UP": lambda **kw: make_sfx_click(pitch=2400, is_down=False),
        "THOCK": lambda **kw: make_sfx_thock(),
        "TICK": lambda pitch=2600, **kw: make_sfx_tick(freq=pitch),
        "CHIME": lambda **kw: make_sfx_chime(),
        "SNAP": lambda **kw: make_sfx_snap(),
        "TOAST": lambda **kw: make_sfx_toast(),
        "SWEEP": lambda **kw: make_sfx_sweep(),
        "WHOOSH_SOFT": lambda **kw: make_sfx_whoosh(),
        "KEY": lambda **kw: make_sfx_key(),
        "LOCK": lambda **kw: make_sfx_lock(),
    }

    cues = tl.get("sfx_cues", [])
    # Duplicate cues for second cycle (0..16s and 16..24s)
    extended_cues = []
    for c in cues:
        extended_cues.append(c)
        if c["t"] < 8.0:
            c_ext = dict(c)
            c_ext["t"] = c["t"] + 16.0
            extended_cues.append(c_ext)

    for c in extended_cues:
        sfx_type = c["type"]
        t_event = c["t"]
        if sfx_type == "KICK":
            continue
        gen_fn = sfx_samples_map.get(sfx_type)
        if not gen_fn:
            continue
        
        # Pitch progression for ticks in beat 18 (t=8.5..8.8)
        pitch = 2600
        if sfx_type == "TICK" and 8.45 <= (t_event % 16.0) <= 8.85:
            step_i = int(round(((t_event % 16.0) - 8.50) / 0.05))
            pitch = int(2200 + step_i * 220)  # rising pitch tick
            raw_sfx = gen_fn(pitch=pitch)
        else:
            raw_sfx = gen_fn()

        # Find peak index in raw_sfx
        peak_idx = int(np.argmax(np.abs(raw_sfx)))
        # Target sample index for event time
        target_sample = int(round(t_event * SR))
        start_sample = target_sample - peak_idx

        if 0 <= start_sample and start_sample + len(raw_sfx) <= RENDER_SAMPLES:
            sfx_buf[start_sample:start_sample + len(raw_sfx)] += raw_sfx * 0.7
            if t_event < 16.0:
                sfx_transients.append({"t_event": t_event, "placed_peak_t": (start_sample + peak_idx) / SR, "type": sfx_type})

    # 5. Wrap-Add Tail: wrap [16.0..24.0s] onto [0.0..8.0s]
    master_music = music_buf[:NUM_SAMPLES].copy()
    tail_music = music_buf[NUM_SAMPLES:NUM_SAMPLES + int(8.0 * SR)]
    master_music[:len(tail_music)] += tail_music

    master_sfx = sfx_buf[:NUM_SAMPLES].copy()
    tail_sfx = sfx_buf[NUM_SAMPLES:NUM_SAMPLES + int(8.0 * SR)]
    master_sfx[:len(tail_sfx)] += tail_sfx

    # Combine music and SFX
    mixed = master_music + master_sfx

    # 6. Loudness Normalization (-14 LUFS) via pyloudnorm
    meter = pyln.Meter(SR)
    loudness = meter.integrated_loudness(mixed)
    print(f"Raw mix integrated loudness: {loudness:.2f} LUFS")
    target_loudness = -12.2
    gain_db = target_loudness - loudness
    gain_linear = 10.0 ** (gain_db / 20.0)
    master_gain = mixed * gain_linear
    print(f"Applied static gain: {gain_db:+.2f} dB (linear {gain_linear:.3f})")

    # Soft-limit true peak <= -1.0 dBTP (check at 4x oversampling)
    # Transparent tanh soft limiter
    threshold = 0.60
    mask = np.abs(master_gain) > threshold
    excess = np.abs(master_gain[mask]) - threshold
    master_gain[mask] = np.sign(master_gain[mask]) * (threshold + 0.18 * np.tanh(excess / 0.18))

    # Check 4x oversampled true peak and scale down if needed
    upsampled = signal.resample_poly(master_gain, 4, 1)
    true_peak = np.max(np.abs(upsampled))
    max_allowed_tp = 0.88  # ~ -1.11 dBTP, safely under -1.0 dBTP
    if true_peak > max_allowed_tp:
        tp_scale = max_allowed_tp / true_peak
        master_gain *= tp_scale
        print(f"Applied true-peak safety scale: {tp_scale:.4f}")

    upsampled_final = signal.resample_poly(master_gain, 4, 1)
    final_tp = np.max(np.abs(upsampled_final))
    final_tp_dbtp = 20 * np.log10(final_tp)
    final_lufs = meter.integrated_loudness(master_gain)
    print(f"Master final True Peak: {final_tp:.4f} ({final_tp_dbtp:.2f} dBTP), LUFS: {final_lufs:.2f}")

    # Check loop seam continuity: first and last 64 samples
    seam_jump = abs(master_gain[0] - master_gain[-1])
    print(f"Loop seam sample jump (first vs last sample): {seam_jump:.6f}")

    return master_gain, kick_transients, sfx_transients

if __name__ == "__main__":
    tl_path = os.path.join(os.path.dirname(__file__), "..", "timeline.json")
    audio_dir = os.path.join(os.path.dirname(__file__), "..", "audio")
    os.makedirs(audio_dir, exist_ok=True)
    out_wav = os.path.join(audio_dir, "master.wav")
    grid_json = os.path.join(audio_dir, "beat_grid.json")

    master, kicks, sfx = synthesize_music_and_sfx(tl_path)

    # Export 24-bit PCM WAV
    sf.write(out_wav, master, SR, subtype='PCM_24')
    print(f"Exported master audio to {out_wav}")

    # Export beat grid metadata
    grid_data = {
        "bpm": 120,
        "sample_rate": SR,
        "duration_samples": len(master),
        "duration_seconds": len(master) / SR,
        "downbeats": [round(t, 4) for t in kicks],
        "sfx_transients": sfx
    }
    with open(grid_json, "w", encoding="utf-8") as f:
        json.dump(grid_data, f, indent=2)
    print(f"Exported beat grid to {grid_json}")
