# Transaction Details, Search & Filters Documentation

Current Version: **1.19.0** (Build 67)

## 1. Overview
This document covers the UI/UX architecture, data flow, and styling implementations for:
1. **Transaction Details Screen** (`TransactionDetailScreen.kt`)
2. **Search, Filters & Sort Experience** (`SearchScreen.kt`, `SearchFilterSheet.kt`, `SearchFilterModels.kt`) — excluding duplicate-flagged records from totals and dynamically formatting currency preset labels from the active `CurrencyUtils` currency.

---

## 2. Transaction Details Screen

### What it does
- Displays the complete metadata of any expense, income, or transfer record.
- Provides hero metrics (formatted amount, transaction type badge, payee/merchant title, exact timestamp, balance after transaction).
- Offers quick action buttons to **Modify Details** (via `EditTransactionSheet`) and **Delete** (with confirmation dialog).
- Handles special transaction states:
  - Needs Review flag resolution.
  - Cash deposit / ATM withdrawal conversion to transfer.
  - Category re-assignment (`CategoryPickerSheet`).
  - Raw SMS inspection and reference number / UTR copying.

### Architecture & Glass Theming Integration
- **Files Involved**: `app/src/main/java/com/omkarnub/kanri/ui/home/TransactionDetailScreen.kt`
- **Background Styling**:
  - Previously, an opaque dark overlay (`Color(0xFF0C0D12)` with heavy haze tint and 94% opacity) was drawn over the screen, creating a muddy, pitch-black backdrop that obstructed the app's dynamic wallpaper/aurora theme.
  - In v1.2.3, the root modifier utilizes `Modifier.background(if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background)`, fully aligning with `HomeScreen`, `SettingsScreen`, and `CategoryDetailScreen`. The live WebGL aurora shader / backdrop shines through cleanly while content cards maintain their optical frosted glass refraction (`GlassLevel.Blur` and `GlassLevel.Flat`).

---

## 3. Search, Filters & Sort Experience

### What it does
- **Search Screen (`SearchScreen.kt`)**: Instant query filtering for payees, counterparties, banks, reference numbers, and notes.
- **Quick Filters Bar (`QuickFilterChipsRow`)**: Prominent horizontal quick-access pills above transaction results:
  - **Filters Button**: Opens the filter sheet on the `Filters` tab, displaying active filter count.
  - **Sort Button**: Direct 1-tap launcher displaying current order (e.g. `Sort: Newest`, `Sort: Highest`), opening directly to the `Sort` tab.
  - **Quick Type Toggle**: Cycles through All Types → Spent Only → Received Only.
  - **Quick Wallet Toggle**: Cycles through All → Cash → Online → Transfers.
  - **Active Preset Pills**: Shows active date preset, category count, and amount filter pills.
  - **Reset Pill**: 1-tap reset when any filter or query is active.

### Filters & Sort Bottom Sheet (`SearchFilterSheet.kt`)
- **Segmented Tab Switcher**: Separates the previously cluttered 7-section sheet into two dedicated tabs:
  1. **Filters Tab**:
     - **Transaction Type**: 3 segmented cards (All, Spent, Received) with 100% horizontally and vertically centered text and directional indicator icons.
     - **Wallet & Accounts**: Organized 2x2 grid (All, Cash, Online, Transfers) with wallet icons, preventing text crowding or truncation.
     - **Date Range**: Uniform 36dp glass pills for presets (Today, This Week, This Month, Last 30 Days, This Year, All Time).
     - **Categories**: Category icon pills with count badges, "Clear" shortcut, and smart "+N More" expand/collapse toggle to eliminate endless scrolling.
     - **Amount Range**: Clean pills for preset bands (Under ₹500, ₹500 – ₹2,000, ₹2,000 – ₹10,000, Over ₹10,000).
     - **Payment Method**: Clean pills for UPI, Card, ATM, Bank Transfer.
  2. **Sort Tab**:
     - Dedicated full-width glass cards for each sort option:
       - **Date: Newest First** (Default - most recent transactions first)
       - **Date: Oldest First**
       - **Amount: Highest First** (Largest transactions first)
       - **Amount: Lowest First** (Smallest transactions first)
     - Each card features an icon badge, title, explanatory subtitle, and luminous radio checkmark indicator.
  3. **Centered Button Text**:
     - Every button and chip utilizes `Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)` with `Text(textAlign = TextAlign.Center)` to guarantee 100% optical centering across all screen sizes and display densities.
  4. **Docked Action Bar**:
     - A full-width `KanriGlassButton` at the bottom of the sheet displaying live result counts (`Show Results (N)` / `Done`) with haptic feedback.

### Files Involved
- `app/src/main/java/com/omkarnub/kanri/ui/search/SearchFilterSheet.kt`
- `app/src/main/java/com/omkarnub/kanri/ui/search/SearchScreen.kt`
- `app/src/main/java/com/omkarnub/kanri/ui/search/SearchFilterModels.kt`
- `app/src/main/java/com/omkarnub/kanri/ui/search/SearchViewModel.kt`
