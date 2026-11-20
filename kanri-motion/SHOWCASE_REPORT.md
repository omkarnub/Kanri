# Kanri — High-Energy Motion Graphics Showcase Reel Report

## Overview
Built to the visual standard of world-class creative motion design, this 15.000-second 1920×1080 60fps showcase reel presents **Kanri** through high-octane kinetic typography, geometric particle bursts (`@mojs/core`), 3D canvas isometric topologies, Apple-grade frosted liquid-glass product micro-moments, and a persistent HUD viewfinder frame.

The reel **starts with the authentic Kanri geometric emblem and name**, surges through 7 beat-synced high-energy scenes at 128 BPM, and **resolves seamlessly back to the exact Kanri emblem and name** at frame 900 ($t=15.000\text{ s}$), establishing a continuous visual loop.

---

## Deliverables Summary

| Deliverable | Resolution / Specs | File Size | Description |
|---|---|---|---|
| **`kanri_showcase_1920_60.mp4`** | 1920×1080, 60 fps, H.264 (CRF 14), AAC 48kHz | **2.00 MB** | **Master Showcase Reel** |
| **`kanri_showcase_1080_60.mp4`** | 1080×1080, 60 fps, H.264 (CRF 14), AAC 48kHz | **1.43 MB** | Square Cut for Social Feeds |
| **`kanri_showcase_silent.mp4`** | 1920×1080, 60 fps, H.264 (CRF 14), Silent | **1.49 MB** | Web Hero Background Video |
| **`kanri_showcase.webm`** | 1920×1080, 60 fps, VP9 (CRF 24), Silent | **2.67 MB** | WebM HTML5 Video |
| **`kanri_showcase.gif`** | 640×360, 24 fps, Palette-optimized Bayer dither | **1.09 MB** | GitHub README Preview (≤ 12 MB) |
| **`showcase_poster.png`** | 1920×1080, Pristine Single Frame | **42 KB** | Hero Brand Slam Frame |
| **`showcase_contact_sheet.png`** | 1920×1080, 10-Scene Inspection Sheet | **330 KB** | QA Visual Contact Sheet |
| **`source.zip`** | Full Reproducible Source Code | **4.79 MB** | Project Source Archive |

---

## 7-Scene Narrative Architecture (128 BPM Beat Grid)

1. **Scene 1: Emblem Spark & Laser Expansion (0.00 – 1.88s / Beats 1–4)**
   - Dark obsidian `#0B0B10` canvas with 32px perspective grid.
   - Kanri 4-fold symmetric crest emerges at center, scaling with spring physics ($S(t)$ with $k=280, d=26$).
   - Radial `@mojs/core` particle burst sparks outward on Beat 1.
   - Emblem stretches into a luminous horizontal laser beam with label `KANRI / OFFLINE-FIRST LEDGER — 2026`.
   - Laser expands vertically to flood the frame with vibrant emerald `#30A46C`.

2. **Scene 2: Brand Slam (1.88 – 3.75s / Beats 5–8)**
   - Massive Panchang Bold **"KANRI"** typography (180px) in deep obsidian on emerald.
   - Top kicker: `INTRODUCING — 2026`; subtitle in italic serif: `Personal Finance, 100% On-Device`.
   - Letters slam down on consecutive 1/16 beats with dual shockwaves.
   - The dot detaches and grows exponentially to transition into dark space.

3. **Scene 3: Kinetic Triplet (3.75 – 6.56s / Beats 9–14)**
   - Three hard-hitting value propositions swapped on the downbeat:
     - **Triplet A (3.75 – 4.68s)**: Warm cream canvas `#FAF9F5`. Heavy black type `DUAL-ENGINE` (SMS Parsing + Payment Notification Listener with 5-minute dedupe).
     - **Triplet B (4.68 – 5.62s)**: Deep black background with kinetic outline echo pattern (`SQLCIPHER AES-256`) filling the frame. Emerald glowing type with animated lock shackle dropping and clicking closed.
     - **Triplet C (5.62 – 6.56s)**: Electric cobalt `#2F47FF` flood. White typography `ZERO CLOUD` (`NO ANALYTICS • NO ADS • NO TRACKING`).

4. **Scene 4: Signature 3D Visual (6.56 – 8.44s / Beats 15–18)**
   - 3D Canvas Perspective Projection: A 9×9 field of isometric dot-matrix tiles rippling in continuous liquid waves from the center, echoing Kanri's Pixel DotTiles.

5. **Scene 5: Product Micro-Moments (8.44 – 11.25s / Beats 19–24)**
   - Floating Apple-grade Frosted Liquid Glass Card (`backdrop-filter: blur(32px)`, 3D perspective tilt):
     - Auto-capture toast (`HDFC Bank • ₹1,950 Debited`).
     - Interactive Bill Splitting slider dragging with dual-spring liquid elasticity: $\text{₹1,950} / 4 = \text{₹487.50}$ each.
     - MacBook Fund savings goal stepper (+₹5,000 $\to$ ₹50,000 / ₹60,000).
     - 7-bar weekly chart sprouting with highlighted Saturday bar (`Sat: ₹1,950`).

6. **Scene 6: Kinetic Proof & Metric Slam (11.25 – 13.12s / Beats 25–28)**
   - Giant hero odometer spinning and locking on `₹84,320`.
   - Subtitle flips vertically into `+₹12,400 this month` in emerald `#30A46C`.
   - Flanked by badges: `100% OFFLINE`, `ZERO ANALYTICS`, `LOCAL SQLCIPHER`.

7. **Scene 7: Emblem Outro & Seamless Loop (13.12 – 15.00s / Beats 29–32)**
   - Center burst resolves into the pristine **Kanri geometric crest and Panchang Bold "KANRI"** logo.
   - Tagline: `Private. Offline. Yours.`
   - Tracked mono URL: `GITHUB.COM/OMKARNUB/KANRI`.
   - **Byte-Identical Loop Return**: Frame 900 ($t=15.000\text{ s}$) matches frame 0 ($t=0.000\text{ s}$) with 100% pixel identity.

---

## Verification Audit

- **Resolution & Aspect Ratio**: 1920×1080 (16:9 Widescreen Showcase DAR)
- **Framerate & Count**: 60.000 fps, exactly 900 frames
- **Duration**: 15.000 seconds
- **Audio Synchronization**: 48,000 Hz stereo, transient alignment across all 61 beat peaks
- **Loop Seam Continuity**: Pixel-identical start/end emblem states (verified via SHA256 byte comparison)
