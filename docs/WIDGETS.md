# Kanri Widget Subsystem & Triple Aesthetic Engine

## 1. Overview
Kanri provides a comprehensive Android Home Screen widget suite and in-app 1:1 preview gallery, allowing users to choose between three distinctive visual languages:
1. **iPhone Fluid (`WidgetStyle.IPHONE`)**: Apple Human Interface Guidelines (HIG) aesthetics with squircle continuous geometry, Cupertino vibrancy, and spring motion.
2. **100x Apple Liquid Glass (`WidgetStyle.GLASS_100X`)**: Apple WWDC25 Liquid Glass materials elevated to 100x with high-specular rim reflections, ambient radial backlights, shimmering light gleams, and neon arcs.
3. **Pixel DotTiles (`WidgetStyle.PIXEL_MATRIX`)**: Native port of the Louis Studios DotTiles specification, featuring 7-row dot-matrix font numerals, live jitter drift, tick sliders with marker blocks, stepped histograms, and dot timelines.

---

## 2. Supported Widgets

Kanri provides 8 standalone widgets:
1. **Money Spent Today** (`MoneySpentTodayWidgetProvider`)
   - Displays today's total debit expenditure and number of expenses.
   - Shows hourly pacing hints and micro bar visual.
2. **Wallet Balances** (`WalletBalanceWidgetProvider`)
   - Live cash wallet and online bank balance breakdown and overall net worth.
3. **Budget Ring Chart** (`BudgetRingWidgetProvider`)
   - Circular progress ring comparing current month's spend against monthly budget limit.
   - Features central left/over remaining amounts with dynamic color state.
4. **Budget Progress Bar** (`BudgetProgressWidgetProvider`)
   - Linear progress gauge displaying percent of budget used and remaining days in the budget cycle.
5. **Savings Goals Ring** (`GoalsRingWidgetProvider`)
   - Circular progress ring tracking progress towards the user's primary savings goal with custom emoji and percentage.
6. **Lend & Borrow Dues** (`LendBorrowWidgetProvider`)
   - Real-time ledger summary of receivables and payables with net balance status.
7. **Quick Record** (`QuickAddWidgetProvider`)
   - One-tap rapid action buttons to immediately launch Add Expense or Add Income sheets.
8. **Split The Bill** (`SplitBillWidgetProvider`)
   - Quick group bill splitting tool and calculator launcher.

---

## 3. The Three Widget Design Systems

### A. iPhone Fluid Style (`WidgetStyle.IPHONE`)
- **Geometry**: Continuous squircle curvature (`24dp` radius) with 0.75dp hairline border.
- **Color Palette**: Apple Dark Widget canvas (`#F01C1C1E`) with vibrant Cupertino accents:
  - Apple Health Coral / Crimson: `#FF2D55` (Spends & Budgets)
  - Apple Mint: `#30D158` (Income & Goals)
  - Cupertino Electric Blue: `#0A84FF` (Wallets & Balances)
  - Cupertino Indigo: `#5E5CE6` (Pacing & Cycle)
- **Data Visuals**:
  - Apple Fitness Activity concentric rings with overlapping shadow depth on Canvas.
  - Apple Health micro bar charts with rounded pill caps.
  - Apple Wallet dual card stack representation.
- **Motion**: Fluid spring physics (`spring(dampingRatio = 0.75f, stiffness = 300f)`).

### B. 100x Liquid Glass Style (`WidgetStyle.GLASS_100X`)
- **Geometry**: Soft `24dp` corners with a 1.5dp high-specular gradient stroke (`rgba(255,255,255,0.60)` top to `rgba(255,255,255,0.05)` bottom).
- **Surface**: Multi-layer frosted translucent gradient (`#30FFFFFF` down to `#0CFFFFFF`).
- **Lighting & Specular Reflections**:
  - Ambient radial colored backlight bloom behind main values and circular charts.
  - Shimmering specular reflection sweep across the card surface.
  - Radiant neon gradient arcs (Cyan `#00E5FF` to Emerald `#00E676`, Violet `#BF5AF2` to Cyan `#0A84FF`).
  - Luminous glass buttons and frosted pills with specular inner borders.

### C. Pixel DotTiles Style (`WidgetStyle.PIXEL_MATRIX`)
- **Geometry**: Matte obsidian black glass tile (`#08080A`) with subtle 1px border (`#24FFFFFF`) and `22dp` corners.
- **Typography**: 7-row dot-matrix font renderer (`DotMatrixFont`) supporting digits `0-9`, symbols `:`, `.`, `-`, `₹`, `$`, `%`, `k`, `M`, `/`.
- **Motion & Micro-Interactions**:
  - Pop-in dot radius easing on load.
  - Numbers count up dot-by-dot on entry.
  - Subtle periodic jitter drift every few seconds.
- **Bottom Visuals**:
  1. `Slider`: Row of fine ticks with a marker block (glow or fill mode).
  2. `Steps`: Stepped histogram bar chart across the time axis.
  3. `Dots`: Timeline with sized dots and faint hour slots.
  4. `Blocks`: Sparse timeline blocks for session tracking.
  5. `Rise`: Stepped 4-tier rise gauge.

---

## 4. Settings Integration (`WidgetsScreen`)

The in-app Widgets gallery is accessible from **Settings > Customisation > Widgets**:
- **Segmented Style Switcher**: Toggle between ` iPhone Fluid`, `✦ 100x Glass`, and `⠶ Pixel DotTiles`.
- **1:1 Live Previews**: All 8 widgets rendered with live device data from Room.
- **Set as Active Widget Style**: One-tap action updating `WidgetPreferences` and triggering `KanriWidgetsUpdater.updateAllWidgets(context)`.
- **Pin to Home Screen**: Uses `AppWidgetManager.requestPinAppWidget` to pin any chosen widget directly from the app.
- **Tap to Replay**: Retriggers animations, spring bounces, and dot-matrix counts on demand.

