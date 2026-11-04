# Changelog

All notable changes to Kanri will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2026-11-04

### Added
- **Thistle Portico Live Glass Background**: High-fidelity procedural Canvas 2D scene featuring a stone Gothic portico, wisteria and hydrangea foliage, drifting and landing blossom petals, racemes swaying in the wind, a pale distant castle, and animated sun shafts and dust motes.
- **Still Sunset Meadow Live Glass Background**: Full-featured Three.js 3D voxel landscape with interactive grass parting on touch, blooming block flowers, cube clouds, and a real-time day cycle that automatically synchronizes with the user's local time zone (daylight, golden sunset, and starry moonlit night).
- **10-Shader Glass Customization Grid**: Expanded the Theme Settings glass background picker from 8 to 10 choices in a balanced 2-column grid with dedicated gradient swatches and descriptions.
- Bumped application version to `1.3.0` (Build 9).

## [1.2.3] - 2026-11-04

### Fixed
- **Transaction Details Dark Background Glitch**: Removed the heavy opaque dark scrim and haze tint overlay (`Color(0xFF0C0D12)`) in `TransactionDetailScreen.kt`, aligning it with the rest of the application so that the dynamic theme/aurora backdrop shines through transparently while content cards retain their frosted glass appearance.
- **Button Text Centering in Filters & Sort**: Resolved off-center and left-aligned text across filter segments and wallet cards by enforcing `Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)` and `Text(textAlign = TextAlign.Center)`.

### Changed
- **Redesigned Filters & Sort Experience**:
  - Introduced a **Segmented Tab Switcher** at the top of `SearchFilterSheet` dividing controls into clean **Filters** and **Sort** tabs.
  - Added dedicated full-width glass cards for each sort order (Date: Newest, Date: Oldest, Amount: Highest, Amount: Lowest) with clear descriptions and radio indicators.
  - Added a direct **1-tap Sort Order Launcher** in the `QuickFilterChipsRow` on the Search/History screen.
  - Reorganized wallet filters into a spacious 2x2 grid with dedicated icons.
  - Replaced disparate M3 filter chips with unified 36dp `KanriFilterChipPill` glass components featuring smart category expand/collapse.
- Bumped application version to `1.2.3` (Build 8).

## [1.2.2] - 2026-11-04

### Changed
- **Gemini Aurora Multi-Color Fluid Orbs with In-Place Chromatic Morphing**:
  - Restored the iconic multi-color ambient spatial distribution across the entire screen so all 3 colors are visible and actively mixing simultaneously at all times.
  - Positioned distinct floating, rotating Gaussian orbs across the screen:
    - **Cycle 1**: Lime (`#7cff67`) on the top-left, Turquoise (`#22d3c5`) on the top-right and center, and Electric Violet (`#8f5cff`) anchoring the bottom and ambient diffusion.
    - **Smooth Transition**: Each orb smoothly morphs in-place over a 4–5s window into **Cycle 2**: Coral (`#ff8a5c`), Pink (`#ff5c8a`), and Violet (`#8a5cff`).
    - Continually cycles seamlessly without ever collapsing to a single tint or losing the multi-colored glass aura.
- Bumped application version to `1.2.2` (Build 7).

## [1.2.1] - 2026-11-04

### Changed
- **Gemini Aurora Dual-Cycle Chromatic Morphs**: Upgraded `aurora.html` to a high-performance WebGL shader faithfully replicating Gemini mobile's exact two-phase color cycles:
  - **Cycle 1**: `#7cff67` (Electric Lime), `#22d3c5` (Turquoise), and `#8f5cff` (Violet).
  - **Cycle 2**: `#ff8a5c` (Sunset Coral), `#ff5c8a` (Neon Rose), and `#8a5cff` (Violet).
  - Implemented continuous mathematical color interpolation using a sinusoidal hermite S-curve (`smoothstep(-0.65, 0.65, -cos(t * omega))`) with fluid domain-warped waves, an incandescent pulsing spark core, and filmic tone mapping for butter-smooth 60/120fps transitions.
- Updated Settings gradient preview swatch and description to represent the dual-cycle blend.
- Bumped application version to `1.2.1` (Build 6).

## [1.2.0] - 2026-11-04

### Added
- **Gemini Mobile Aurora Shader Background**: Redesigned the `aurora.html` live background to faithfully reproduce Google Gemini's signature mobile app aurora effect.
  - Implemented the official Google Gemini spectral palette: Gemini Electric Blue (`#1A73E8`, `#4285F4`), Radiant Cyan spark (`#00E5FF`, `#00D2FF`), Vibrant Violet (`#7C3AED`, `#9333EA`), and Vivid Fuchsia / Magenta (`#D946EF`, `#FF2E93`).
  - Added the living incandescent neural spark core (`rgba(240, 249, 255, 0.95)` with cyan diffusion) pulsing like Gemini's active thinking/listening state.
  - Incorporated Google's signature multi-spectral perimeter ribbon accents (Warm Sunset Coral Red `#EA4335`, Amber Gold `#FBBC04`, and Emerald Jade `#34A853`).
  - Choreographed harmonic multi-phase wave animations with smooth non-linear easing and zero-alpha color termination to eliminate fringing.

### Changed
- Updated the Aurora shader option in `ThemeSettingsScreen` to display as **Gemini Aurora** with a matching Gemini linear gradient preview swatch and updated description.
- Bumped application version to `1.2.0` (Build 5).

## [1.1.0] - 2026-11-04

### Added
- Dedicated **Themes & Appearance window** (`ThemeSettingsScreen`) with visual preview cards for System, Light, Dark, AMOLED, and Glass.
- **Side-by-side (2-column) live WebGL shader background cards** in Glass mode (Aurora, Grain Gradient, Hero Geometric, Prism Gradient, Cloud Shader, Ink Ridges, Fuji Sakura, Sumi-e Mist).
- **User Birthday setting** in Profile (`ProfileScreen`) using `KanriGlassDatePickerDialog`.
- **Dynamic Birthday Greeting** on the Home screen wishing the user on their special day.
- **Birthday Celebration Notifications** via `BirthdayHelper` scheduled on the user's birthday.
- Redesigned **Permissions & Detection** with interactive toggle switches for SMS Detection, Notification Listener, Notifications, Overlay, and Battery Exemption.
- Streamlined **Customise Glass Components** screen with 4 essential sliders: Frosted blur, Glass tint, Border & edge shine, and Corner curvature, with dedicated "Reset to default" and "Save & Exit" buttons.

### Changed
- Strictly decoupled Glass engine and live WebGL backgrounds from standard themes: live backgrounds and glass customizers are now completely excluded from composition in Light, Dark, and AMOLED modes.
- Added lifecycle hooks to `GlassBackgroundView` to explicitly stop and destroy WebGL WebViews when exiting Glass mode, eliminating background CPU/GPU/battery drain.
- Normalized contrast ratios and colors across all themes (Light cream `#FBF8EE`, Dark charcoal `#121212`, AMOLED pitch black `#000000`, and Glass frosted tint) adhering to WCAG AA guidelines.
- Bumped application version to `1.1.0` (Build 4).

### Removed
- Removed Google Drive Cloud Backup to preserve a 100% offline-first architecture with local encrypted/plain JSON backups.
- Removed Test Lock Screen action from Security & Privacy.
- Removed Dynamic Colors toggle and Reduce Transparency toggle.
- Removed all presets and live simulated preview widget from the Glass Customizer.
- Removed obsolete `ThemesCustomizationScreen.kt`.

## [1.0.2] - 2026-09-27
- Initial release with offline SMS parsing, lending/borrowing ledger, split expenses, and Material 3 design.
