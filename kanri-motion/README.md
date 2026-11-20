# Kanri — 16-Second Seamless-Loop Motion Graphics

This directory contains the complete source code, mathematical motion system, procedural audio synthesizer, parallel Playwright renderer, and encoding pipeline for the 16.000-second seamless-loop motion graphics video for **Kanri** (Android offline-first personal finance ledger).

## Architecture Overview

- **`index.html`**: 1440×1440 pure state stage exposing `window.seek(t)`, `window.qa()`, and `window.__ready`. Zero CSS transitions or accumulated animation state.
- **`tokens.json` & `timeline.json`**: Single source of truth for color tokens, 32-beat keyframe targets, and audio cues.
- **`tools/audio.py`**: Procedural numpy music & SFX synthesis at 48 kHz, 24-bit float, 120 BPM A minor lo-fi tech groove with sidechain compression and wrap-add tail blending.
- **`tools/render.py`**: Sharded parallel chunk renderer (16 chunks × 60 frames × 4 subframes = 3,840 subframes) with FFmpeg `tmix` subframe motion blur.
- **`tools/encode.py`**: Lossless FFV1 chunk concatenation and multi-target delivery encoding (`.mp4`, `.webm`, `.gif`, poster, zip).
- **`tools/verify.py`**: 12-gate automated QA verification engine.

## How to Re-render and Export

1. Start the local stage server:
   ```bash
   python tools/serve.py
   ```

2. Synthesize audio and verify alignment:
   ```bash
   python tools/audio.py
   python tools/analyze_audio.py
   ```

3. Render all 16 chunks in parallel:
   ```bash
   python tools/render.py --mode final --workers 8
   ```

4. Encode deliverables:
   ```bash
   python tools/encode.py
   ```

5. Verify all QA gates:
   ```bash
   python tools/verify.py
   ```

## Final Deliverables (in `./deliverables/`)

1. `kanri_loop_1440_60.mp4`: Master 1440×1440 60fps H.264 video with 48kHz stereo audio.
2. `kanri_loop_1080_60.mp4`: 1080×1080 social/web version with audio.
3. `kanri_loop_silent_1440.mp4`: 1440×1440 silent video for web embeds.
4. `kanri_loop.webm`: VP9 video (no audio).
5. `kanri_preview_640.gif`: Palette-optimized 640px 24fps GIF (≤ 12 MB) for GitHub README.
6. `poster.png`: Pristine single frame at t=9.0s (Saturday Swiggy hover).
7. `contact_sheet_settled.png` & `contact_sheet_mid.png`: 32-frame QA contact sheets.
8. `audio/master.wav` & `audio/beat_grid.json`: 24-bit 48kHz procedural master audio.
9. `source.zip`: Complete self-contained project archive.
10. `REPORT.md`: Complete QA validation audit and metrics report.

