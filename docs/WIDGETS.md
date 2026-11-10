# Kanri Pixel DotTiles Widget System

Current Version: **1.12.2** (Build 38)

## 1. Overview
Kanri's widget subsystem is unified around a single, cohesive visual language: **Pixel DotTiles** (Nothing-inspired dot-matrix & tactile cards). Designed with precision, restraint, and obsessive detail, the system delivers 12 specialized home screen widgets rendered by a pure Canvas engine shared 1:1 between Android `RemoteViews` bitmaps and Jetpack Compose in-app previews.

---

## 2. Design System Foundations

### Typography & Fonts (`PixelFont` & `Ndot57`)
- **Authentic Ndot57 Font**: Integrated official `Ndot57-Regular.otf` (`res/font/ndot57.otf`) for tabular dot-matrix numerals, percentages, and metrics.
- **Clean Sans Typography**: High-contrast, clean system sans (`sans-serif-medium` / `sans-serif`) for headers, titles, subtitles, currency signs (`₹`), and axis labels.
- **Optical Indian Currency Alignment**: Sized and optically centered currency symbol (`₹`) with Ndot57 numerals, preventing tofu boxes and glyph clipping.
- **Zero Distortion & Strict Proportions**: Scaled using `fitCenter` and orientation-aware target dimensions (`minW` x `maxH`), completely eliminating text elongation and stretched aspect ratios.

### Layout Geometry & Zero Overlap Architecture
- **Adaptive Vertical Zones**: Strict vertical zone boundaries partition Header, Middle Hero Metric, and Bottom Visual.
- **Compact Height Detection (`isCompactH`)**: When container height is under `115dp` (e.g. `2x1` and `4x1`), hero text size scales dynamically and bottom tick labels are hidden so numbers and visual primitives never collide.
- **Parabolic Arc Clamping**: The Financial Sky celestial arc strictly computes `x(t), y(t)` inside the recessed well bounds, preventing sun/moon markers from penetrating top or bottom borders.

### Fluid Animation Engine (`DotVisuals` & `PixelMotion`)
All visual primitives accept an interactive `progress: Float = 1.0f` parameter:
- **Slider**: Marker block eases from 0 to target position while lit ticks sweep in from left to right.
- **Steps**: Histogram bars grow smoothly upward from the axis baseline.
- **Dots**: 24-hour dots sweep in sequentially with an expanding cyan indicator ring around the current hour.
- **Blocks**: Session queue blocks illuminate consecutively.
- **Rise**: 6-tier proportional staircase rises step by step.
- **Grid**: Calendar dot matrix cells fade and intensify smoothly.
- **Arc**: Celestial marker rides the parabolic arc trajectory.
- **RemoteViews Frame Burst**: 8-frame burst animation on home screen placement and updates.
- **Interactive In-App Previews**: 60fps Compose animation triggered on tap or replay.

### Palette
The design system enforces a strict, zero-drift color palette exposed via `PixelTheme`:
- **Tile Background**: `#08080A` (Deep Obsidian Glass)
- **Inner Border**: 1px `#24FFFFFF` (14% frosted edge)
- **Corner Radius**: `22dp`
- **Tile Padding**: `14dp`
- **Dot On (Primary)**: `#F2F2F5`
- **Dot Dim (Inactive Grid)**: `#1E1E24`
- **Dot Mid (Secondary Data & Labels)**: `#6B6B76`
- **State Accents (strictly semantic, never decorative)**:
  - **Mint (`#30D158`)**: Healthy budgets, positive receivables, goal completion, downward spending deltas.
  - **Amber (`#FFB020`)**: Pacing warnings (70%–90% budget), pending reviews, active streak counters.
  - **Coral (`#FF453A`)**: Over-budget (>90% or negative remaining), negative net worth, upward spending deltas.
  - **Cyan (`#4DD0E1`)**: Today/current indicators, milestone targets, neutral pacing hints.

### Layout Grammar
Every tile strictly obeys a three-tiered layout grammar:
1. **Top**: Small dot-matrix label (uppercase, `Dot Mid` `#6B6B76`, 5–7px tall).
2. **Middle**: Hero value in large dot-matrix typography (dynamically auto-fitted to avoid overflow).
3. **Bottom**: Exactly one visual primitive from the shared `DotVisuals` library.

### Dot-Matrix Typography (`DotMatrixFont`)
- 7-row bitmap glyphs, 5 columns wide with 1 column spacing (tabular monospace digits).
- Full glyph set: `0-9 A-Z : . , - + ₹ $ % / x k M L ▲ ▼ ✓ ·` and space.
- Compact Indian number formatting: `₹850`, `₹1.2k`, `₹12.4k`, `₹1.2L`, `₹1.5Cr`.
- Reveal progress parameter (`0f..1f`) driving dot-by-dot counting entry animations.

### Shared Visuals Library (`DotVisuals`)
Reusable pure Canvas primitives:
- **Slider**: Row of fine ticks with a marker block (glow or fill mode) and threshold color shifts.
- **Steps**: Stepped histogram bars built of dot columns on a shared scale.
- **Dots**: 24-slot timeline with dots sized by hourly expenditure and active slot markers.
- **Blocks**: Sparse solid session blocks for pending review queue items (older items dimmer).
- **Rise**: 4 to 6-tier stepped gauge for wallet asset split proportions.
- **Grid**: Calendar dot grid with 5-level percentile quantization and today ring indicator.
- **Arc**: Dotted celestial arc traversed by sun/moon.
- **Icon**: 7x7 dot-matrix icons (`PLUS`, `MINUS`, `SPLIT`, `INBOX`, `FLAME`, `ARROW_UP`, `ARROW_DOWN`, `CHECK`, `SUN`, `MOON`).

---

## 3. The 12 Final Widgets

| # | Widget | Provider | Supported Sizes | Visual Primitive | Description |
|---|--------|----------|-----------------|------------------|-------------|
| 1 | **Today** | `TodayWidgetProvider` | 2x2, 4x2 | `Dots` timeline | Spent today, count of expenses, 24-hour dot pacing timeline, and average spend comparison. |
| 2 | **Budget** | `BudgetWidgetProvider` | 2x1, 2x2, 4x2 | `Slider` | Remaining budget amount, % used, days left in cycle, and expected vs actual pacing marker. |
| 3 | **Wallets** | `WalletsWidgetProvider` | 2x2, 4x2 | `Rise` | Cash, bank, and net worth balances with asset proportion stepped gauge. |
| 4 | **Goals** | `GoalsWidgetProvider` | 2x2, 4x1 | `Slider` | Primary savings target progress, percentage saved, and 25/50/75/100% milestone ticks. |
| 5 | **Dues** | `DuesWidgetProvider` | 2x2, 4x2 | `Steps` | Receivable (`GET`), payable (`OWE`), net balance, and overdue dues count. |
| 6 | **Sky** | `SkyWidgetProvider` | 4x2 | `Arc` + `Steps` | Flagship financial weather: sun arc, astronomical moon phase, twinkling stars, terrain silhouette, climate status, and 7-day spend pulse. |
| 7 | **Month Heatmap**| `HeatmapWidgetProvider` | 4x3, 4x2 | `Grid` | Calendar dot grid with 5 quantized percentile intensity levels and current day ring. |
| 8 | **No-Spend Streak**| `StreakWidgetProvider` | 2x2, 4x1 | 30-dot row | Consecutive no-spend day streak, all-time best streak, and 30-day timeline. |
| 9 | **Safe to Spend** | `SafeToSpendWidgetProvider` | 2x2, 4x1 | `Slider` | Remaining daily spending allowance until cycle end vs today's spend. |
| 10 | **Review Inbox** | `ReviewInboxWidgetProvider` | 2x1, 2x2 | `Blocks` | Count of SMS-detected expenses awaiting categorization and pending amount. |
| 11 | **Month Delta** | `MonthDeltaWidgetProvider` | 2x2, 4x2 | `Steps` | Spend so far this month compared to the exact same day last month with ▲ / ▼ delta %. |
| 12 | **Quick Actions** | `QuickActionsWidgetProvider` | 4x1, 2x1 | `Icon` buttons | One-tap dot-icon action buttons for `+ EXPENSE`, `+ INCOME`, and `SPLIT BILL`. |

---

## 4. Rendering & Animation Architecture

### Platform-Independent Canvas Rendering
- Core rendering resides in `WidgetRenderer.kt`, drawing strictly to `android.graphics.Canvas`.
- In Compose (`WidgetsScreen.kt`), previews invoke `WidgetRenderer` directly inside `Canvas { drawIntoCanvas { ... } }`, providing 100% identical pixel fidelity.
- On the Home Screen, `BasePixelWidgetProvider` invokes `WidgetRenderer.renderToBitmap` to generate crisp, memory-capped bitmaps passed to `RemoteViews.setImageViewBitmap`.

### Animation Tiers
1. **In-App Preview Gallery (60 FPS Compose)**:
   - Full-fidelity entrance animation: dot reveal progress via `Animatable` with `FastOutSlowIn` easing.
   - Tap-to-replay on every card.
   - Respects system `ANIMATOR_DURATION_SCALE == 0` (immediately snaps to final frame).
2. **Home Screen (Rate-Limited Entrance Bursts)**:
   - Zero battery drain: no endless timer loops.
   - On data change (transaction insert/edit/delete, review resolved, midnight reset), a short burst of 10 frames at 85 ms intervals is rendered before resting on the final frame.
   - Bursts are strictly rate-limited to at most one per 5 seconds per widget.
   - Periodic 15-minute `WorkManager` refresh for `SkyWidgetProvider` keeps sun and astronomical moon positions accurate throughout the day and night.

---

## 5. Data Flow & Update Rules
- All widget queries are centralized in `WidgetDataSource.kt`, using indexed Room aggregate queries.
- `KanriWidgetsUpdater.updateAllWidgets(context)` updates only widgets that are actively placed on the user's home screen.
- Deep links route through `KanriWidgetActions.kt` directly into target screens:
  - Today / Month Delta / Heatmap → History & Transactions
  - Budget / Safe to Spend → Budget Manager
  - Wallets → Wallet Sheet
  - Goals → Savings Goals
  - Dues → Lend & Borrow
  - Review Inbox → Needs Review Queue
  - Quick Actions → Add Expense / Add Income / Split Bill
