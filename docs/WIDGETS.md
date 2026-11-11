# Kanri Pixel DotTiles Widget System

Current Version: **1.18.12** (Build 63)

## 1. Overview
Kanri's widget subsystem is unified around a single, cohesive visual language: **Pixel DotTiles** (Nothing-inspired dot-matrix & tactile cards). Designed with precision, restraint, and obsessive detail, the system delivers 12 specialized home screen widgets rendered by a pure Canvas engine shared 1:1 between Android `RemoteViews` bitmaps and Jetpack Compose in-app previews, with full multi-currency support (`CurrencyUtils` / `CurrencyPreferences` synchronization), custom budget cycle awareness (`BudgetPreferences.budgetCycleStartDay`), and thread-safe `ThreadLocal` `Paint` rendering.

---

## 2. Design System Foundations

### Typography & Fonts (`PixelFont` & `Ndot57`)
- **Authentic Ndot57 Font**: Integrated official `Ndot57-Regular.otf` (`res/font/ndot57.otf`) for tabular dot-matrix numerals, percentages, and metrics.
- **Clean Sans Typography**: High-contrast, clean system sans (`sans-serif-medium` / `sans-serif`) for headers, titles, subtitles, currency signs (`₹`), and axis labels.
- **Optical Indian Currency Alignment**: Sized and optically centered currency symbol (`₹`) with Ndot57 numerals, preventing tofu boxes and glyph clipping.
- **Zero Distortion & Strict Proportions**: Scaled using `fitCenter` and orientation-aware target dimensions (`minW` x `maxH`), completely eliminating text elongation and stretched aspect ratios.

### Adaptive Layout Geometry & Zero Dead Space Engine
- **Two-Column Breakpoint (`isWide = width > 220dp`)**:
  - Wide 4x2 widgets automatically adapt to balanced two-column layouts, utilizing the right side for detailed cards, breakdown meters, or session matrix cards instead of leaving empty dead space.
  - **Today (4x2)**: Left column has header, hero spend, and daily limit badge; right column renders the pacing bar and 4 checkpoint cards (`MORN · AFTN · EVE · NIGHT`).
  - **Budget (4x2)**: Left column has remaining hero, days left, and spent-of-total badge; right column renders dual progress bars and daily safe allowance.
  - **Wallets (4x2)**: Left column has net worth hero and allocation ratio; right column renders two full-width cards for Cash and Bank with balances and dot meters.
  - **Dues (4x2)**: Left column has net hero and get/owe breakdown; right column renders up to 4 top contacts in individual cards with `+₹` / `-₹` amounts in Ndot57 numerals.
  - **Month Delta (4x2)**: Left column has hero delta % and pacing status; right column renders two comparison tiles for this month vs last month same day.
- **Compact Wide Breakpoint (`isWide && isCompactH`)**:
  - Wide 4x1 widgets (`Goals`, `Streak`, `Safe to Spend`) pair left-aligned hero values with full right-side visual tracks (milestone sliders or 10-flame timeline).
- **Compact Height Detection (`isCompactH = height < 115dp`)**:
  - Compact 2x1 and 4x1 widgets adapt font sizing and margins so numbers and tracks never collide.
- **Strict Vertical Zones in 2x2**:
  - Square 2x2 widgets enforce strict vertical partitioning between Header, Middle Hero Metric, and Bottom Visual Primitives.
- **Parabolic Arc Clamping**: The Financial Sky celestial arc strictly computes `x(t), y(t)` inside the recessed well bounds, preventing sun/moon markers from penetrating top or bottom borders.

### Fluid Animation Engine (`DotVisuals` & `PixelMotion`)
All visual primitives accept an interactive `progress: Float = 1.0f` parameter:
- **Slider**: Marker block eases from 0 to target position while lit ticks sweep in from left to right.
- **Dual Progress Bar**: Used budget and total budget tracks animate filled dot meters with expected pacing marker.
- **Steps**: Histogram bars grow smoothly upward from the axis baseline.
- **Today Pacing**: Spending pacing track with daily budget limit comparison and 4-session checkpoint breakdown.
- **GitHub Heatmap**: 7-row calendar contribution grid with 4 intensity levels illuminating smoothly.
- **Pixel Flame Row**: 7-day row of pixelated flame icons (lit for no-spend, dim for spend).
- **Weather Scene**: Astronomical sun/moon arc, horizon hills, clouds, stars, and rain drops.
- **Pixel Glyphs**: Authentic 7x7 dot-matrix action icons for Expense (`-`), Income (`+`), and Split (`÷`).
- **RemoteViews Frame Burst**: 8-frame burst animation on home screen placement and updates.
- **Interactive In-App Previews**: 60fps Compose animation triggered on tap or replay.

### Palette & Surface Treatment
The design system enforces a strict, zero-drift color palette exposed via `PixelTheme`:
- **Tile Background**: `#08080A` (Pure Deep Obsidian Glass)
- **Borderless Surface**: Clean, unoutlined dark cards allowing typography, dot-matrix numerals, and glowing visual primitives to stand out without competing white borders.
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
- **Dual Progress Bar**: Two distinct bars: upper used bar with % and lower total budget reference scale with pacing marker.
- **Today Pacing Bar**: Pacing bar comparing spent vs daily limit with 4 session checkpoints (`MORN`, `AFTN`, `EVE`, `NIGHT`).
- **Steps**: Stepped histogram bars built of dot columns on a shared scale.
- **Blocks**: Sparse solid session blocks for pending review queue items (older items dimmer).
- **GitHub Heatmap**: 7 rows (Mon..Sun) by 5 columns (weeks) with 4 discrete intensity levels and day labels (`M`, `W`, `F`).
- **Pixel Flame Row**: 7x7 pixelated flame glyphs for recent days (lit amber for no-spend, dim for spend).
- **Weather Scene**: Astronomical sun/moon arc, horizon hills, clouds, stars, and rain drops.
- **Pixel Glyphs**: 7x7 dot-matrix icons: `EXPENSE` (pixel minus `-`), `INCOME` (pixel plus `+`), `SPLIT` (pixel division `÷`), `TRANSFER` (`⇄`), `FLAME_LIT`, `FLAME_DIM`.

---

## 3. The 12 Final Widgets

| # | Widget | Provider | Supported Sizes | Visual Primitive | Description |
|---|--------|----------|-----------------|------------------|-------------|
| 1 | **Today** | `TodayWidgetProvider` | 2x2, 4x2 | `TodayPacing` | Spent today, count of expenses, pacing bar vs daily budget limit, and 4-session checkpoints (`MORN · AFTN · EVE · NIGHT`). |
| 2 | **Budget** | `BudgetWidgetProvider` | 2x1, 2x2, 4x2 | `DualProgressBar` | Dual progress bars: used budget (% used) and total budget scale with days left and expected pacing marker. |
| 3 | **Wallets** | `WalletsWidgetProvider` | 2x2, 4x2 | `Breakdown Rows` | Net balance hero metric in large Ndot57 numerals, clean Cash and Bank breakdown rows with allocation percentages. |
| 4 | **Goals** | `GoalsWidgetProvider` | 2x2, 4x1 | `Slider` | Primary savings target progress, percentage saved, and 25/50/75/100% milestone ticks. |
| 5 | **Dues** | `DuesWidgetProvider` | 2x2, 4x2 | `Contact Rows` | Net balance subtitle and top lend/borrow contacts list with name and `+₹amount` (receivable) or `-₹amount` (payable). |
| 6 | **Sky** | `SkyWidgetProvider` | 4x2 | `WeatherSky` + `Steps` | Flagship financial weather: recessed dot-matrix sky well (sun/moon arc, stars, clouds, hills), today spend, safe to spend per day, and 7-day spending pacing track. |
| 7 | **Month Heatmap**| `HeatmapWidgetProvider` | 4x3, 4x2 | `GitHubHeatmap` | GitHub-style 7-row (Mon..Sun) by 5-week contribution grid with 4 intensity levels, day labels, and today ring. |
| 8 | **No-Spend Streak**| `StreakWidgetProvider` | 2x2, 4x1 | `PixelFlameRow` | Consecutive no-spend day streak in Ndot57 numerals, all-time best streak, and a row of 7 pixelated flame icons. |
| 9 | **Safe to Spend** | `SafeToSpendWidgetProvider` | 2x2, 4x1 | `Slider` | Remaining daily spending allowance until cycle end vs today's spend. |
| 10 | **Review Inbox** | `ReviewInboxWidgetProvider` | 2x1, 2x2 | `Blocks` | Count of SMS-detected expenses awaiting categorization and pending amount. |
| 11 | **Month Delta** | `MonthDeltaWidgetProvider` | 2x2, 4x2 | `Steps` | Spend so far this month compared to the exact same day last month with ▲ / ▼ delta %. |
| 12 | **Quick Actions** | `QuickActionsWidgetProvider` | 4x1, 2x1 | `PixelGlyph` buttons | One-tap pixel-glyph action buttons for `EXPENSE` (`-`), `INCOME` (`+`), and `SPLIT` (`÷`). |

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

### Launcher Previews (System Widget Picker)
- Modern Android launchers (Pixel Launcher, Samsung One UI) display previews in the "Add widgets" sheet.
- Each widget provider configures a dedicated 1:1 preview in `res/xml/widget_*_info.xml` via `android:previewImage="@drawable/widget_preview_*"`.
- High-fidelity preview drawables reside in `res/drawable-nodpi/` (`widget_preview_*.webp`, lossless WebP) rendered directly from the pure Canvas engine, providing pixel-accurate previews before placement.
- `android:previewLayout` attributes are omitted so the launcher never falls back to an inaccurate generic placeholder.

---

## 5. Data Flow & Update Rules
- All widget queries are centralized in `WidgetDataSource.kt`, using indexed Room aggregate queries.
- `KanriWidgetsUpdater.updateAllWidgets(context)` updates only widgets that are actively placed on the user's home screen.
- **Floating Quick Entry Workflow with True Frosted Glass Blur (No Main App Opening)**:
  - Tapping **Expense**, **Income**, or **Split** on `QuickActionsWidgetProvider`, or tapping the **Today** or **Safe to Spend** widgets, deep-links directly to `QuickEntryActivity.kt`.
  - Configured with `Theme.Kanri.QuickEntry`, `excludeFromRecents="true"`, and `taskAffinity=""`.
  - Applies native API 31+ window blur behind (`FLAG_BLUR_BEHIND` with `blurBehindRadius = 120` and `FLAG_DIM_BEHIND` with `0.35f` dim).
  - Renders the tactile Compose numpad and full manual transaction workflow (`AddTransactionDialog` / `SplitExpenseEntryDialog`) as a translucent frosted glass sheet directly over the home screen wallpaper.
  - Transactions save instantly to Room DB, trigger an 8-frame burst widget refresh across all placed widgets (`KanriWidgetsUpdater.updateAllWidgets(applicationContext, playBurst = true)`), show a brief confirmation toast, and dismiss without launching `MainActivity`.
- Other widget deep links route through `KanriWidgetActions.kt` directly into target screens:
  - Month Delta / Heatmap → History & Transactions
  - Budget → Budget Manager
  - Wallets → Wallet Sheet
  - Goals → Savings Goals
  - Dues → Lend & Borrow
  - Review Inbox → Needs Review Queue
