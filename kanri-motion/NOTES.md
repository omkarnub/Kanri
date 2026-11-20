# Kanri Motion Graphics — Engineering Notes

## Environment Configuration
- **OS**: Windows 11 (AMD64)
- **Node.js**: v22.13.1
- **Python**: 3.12.10
- **FFmpeg**: ffmpeg version 2025-02-17-git-5867ffc8ef-full_build-www.gyan.dev
  - Video encoders: `libx264`, `libvpx-vp9` verified
  - Audio encoder: `aac` verified
- **Playwright**: 1.50.0 (Chromium 156.0.8078.4 headless shell)
- **Audio Stack**: `numpy` 2.2.3, `scipy` 1.15.2, `soundfile` 0.13.1, `pyloudnorm` 0.2.0
- **Typography Engine**: `fontTools` 4.56.0
- **Logical CPU Cores**: 12 (Parallel rendering set to 8 workers)
- **Free Disk Space**: > 120 GB (exceeds 10 GB requirement)

## Design Decisions
1. **Asset Source**: Real Panchang Bold OTF, Kanri vectors, and Google Sans font weights imported from the local Android project repository `app/src/main/res/font/` and `drawable/`.
2. **Icons**: 11 SVG icons authored on a 24-unit grid with exact `stroke-width: 2`, `round` caps/joins. All passed programmatic validation.
3. **Audio Synthesis**: Pure procedural numpy synthesis at 48 kHz, 24-bit float, 120 BPM A minor lo-fi tech groove with sidechain compression and wrap-add tail blending for seamless looping.
4. **Motion Engine**: Closed-form analytic spring step-response with continuous velocity retargeting and periodic summation across $t \in [0, 16)\text{ s}$.

