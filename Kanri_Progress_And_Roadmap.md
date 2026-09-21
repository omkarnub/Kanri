# Kanri — Progress Report & Roadmap

**App name:** Kanri  
**Package name:** com.omkarnub.kanri  
**Purpose:** Personal all-expenses tracker — automatically detects UPI payments, ATM withdrawals, card usage, and bank transfers (sent and received), tracks daily/monthly totals, budgets, lending/borrowing, savings goals, and exports data.  
**Target device:** Android devices running Android 8.0+ (Min SDK 26), tested extensively on Pixel 8 (API 34/35/37) and physical hardware (Android 14+).  
**Platform:** Android, Kotlin 2.2.10, Jetpack Compose (BOM 2026.02.01), Material 3  
**Design Philosophy:** Strict minimalist monochrome luxury theme (pure AMOLED black, refined off-whites, frosted glass accents), zero-clutter layout, and fluid choreographed micro-animations.

---

## 1. What's Done So Far ✅

1. **Development environment set up** — Android Studio installed on Windows, project created (Kotlin + Jetpack Compose, Empty Activity template).
2. **Emulator working** — Pixel 8 virtual device created and running; confirmed Compose launches successfully.
3. **SMS permissions declared** — `RECEIVE_SMS` and `READ_SMS` added to `AndroidManifest.xml`.
4. **SmsReceiver class created** — `BroadcastReceiver` (`SmsReceiver.kt`) that listens for `SMS_RECEIVED_ACTION`.
5. **Receiver registered in the manifest** — added as `<receiver>` block inside `<application>` for background detection.
6. **Runtime permission request added** — `MainActivity.kt` checks for SMS permissions on launch with system dialogs.
7. **Multi-part SMS concatenation & unified logging** — `SmsReceiver.kt` reconstructs multi-part SMS messages into single bodies.
8. **Regex transaction parser (`SmsParser.kt`)** — extracts amount, type (debit/credit), source (ATM, UPI, Card, Bank Transfer), counterparty, bank, and reference number. Backed by `SmsParserTest.kt`.
9. **Room database integration** — Room 2.8.5 + KSP 2.2.10 with `TransactionEntity`, `TransactionDao`, and `KanriDatabase`.
10. **Duplicate protection** — reference number deduplication prevents duplicate records across multiple SMS alerts.
11. **Basic functional UI built** — `HomeScreen.kt` with live reactive transaction list via Room `Flow` and "+ Add" manual transaction entry.
12. **Live end-to-end verification** — verified on Pixel 8 emulator with Bank of Baroda ATM withdrawal, Kotak UPI, HDFC UPI credit, and manual cash entries.
13. **Categorization System with Memory Auto-mapping** — 9 default categories, tap-to-label interactive bottom sheet (`CategoryPickerSheet.kt`), and counterparty memory auto-mapping (`counterparty_category_map`).
14. **Live Categorization Verification** — verified automated categorization with zero manual taps on recurring merchants.
15. **Liquid-Fill "Water Box" Budget Gauge & Safe-to-Spend Engine** — Room budget persistence (`BudgetEntity`, `BudgetDao`), `BudgetCalculator` with unit tests, and interactive `SetBudgetDialog`.
16. **Reversal, Refund & Declined Transaction Handling** — `SmsParser` accurately detects refund and reversal SMS alerts.
17. **Monthly Analytics, Category Donut Chart & Daily Spend Trends** — `MonthViewScreen`, custom Canvas `CategoryDonutChart`, and `DailySpendBarChart` with day-by-day spend bars.
18. **Lending & Borrowed Tracker Module (`LendingEntity`, `LendingDao`, `LendingViewModel`, `LendingScreen`)** — peer-to-peer debt ledger tracking 'Money I Lent' vs 'Money I Borrowed', real-time Net Debt balance, return date presets, and settled toggles.
19. **Split Expense Tracker with Animated Circle Gauge (`SplitCircleGauge.kt`, `SplitExpenseSheet.kt`)** — interactive group bill splitting engine dividing dynamically among 2 to 10+ participants with dual-role ledger integration.
20. **Native System App Lock (`SecurityPreferences`, `BiometricHelper`, `SecurityManager`, `LockScreen`, `SecuritySettingsSheet`)** — 100% native Android device authentication using `BiometricPrompt` with `BIOMETRIC_STRONG` / `DEVICE_CREDENTIAL` (Fingerprint, Face, PIN, or Pattern).
21. **Full Database Backup & Restore Engine (`BackupData`, `BackupRepository`, `BackupRestoreSheet`)** — JSON export/import via Storage Access Framework (SAF) with atomic transactional restore.
22. **Excel & CSV / PDF Financial Statement Export (`CsvExporter`, `PdfStatementExporter`, `ExportStatementSheet`)** — RFC 4180 CSV export and native multi-page PDF generation with summary metrics and ledger tables.
23. **Search & Multi-Criteria Filtering Engine (`SearchFilterModels`, `SearchViewModel`, `SearchFilterSheet`, `SearchScreen`)** — zero-latency full-text search and multi-criteria filtering across dates, amounts, categories, and sources.
24. **Category Detail View with Trend Line (`CategoryDetailScreen`, `CategoryTrendLineChart`)** — 6-month cubic bezier trend line chart, month-over-month deltas, and top payee rankings.
25. **Truecaller-Style Instant Popup (`InstantPopupService`, `InstantPopupCard`, `InstantPopupSheet`)** — system overlay card (`TYPE_APPLICATION_OVERLAY`) with 8-second auto-dismiss and 1-tap quick categorization, plus heads-up notification fallback.
26. **Receive-Side Notification Detection (`KanriNotificationListenerService`)** — background listener for incoming payment notifications (Google Pay, Amazon Pay, FamPay, Paytm) with amount/timestamp deduplication.
27. **All-Time Multi-Month Trends View (`AllTimeTrendsView`, `MultiMonthTrendsChart`)** — multi-month trajectory with interactive Canvas line chart, touch scrubbing, and monthly drill-down.
28. **Recurring Payments & Subscription Tracker (`RecurringPaymentsSheet`, `RecurringPaymentEntity`)** — auto-detects repeating monthly spends with renewal countdowns and due date management.
29. **Android Home Screen Widget (`KanriAppWidgetProvider`, `widget_small.xml`, `widget_medium.xml`, `widget_large.xml`)** — glanceable home screen widgets with responsive breakpoints.
30. **Health-Check Warning Banner & Diagnostics (`HealthCheckBanner`, `DiagnosticsSheet`)** — monitors SMS permissions, notification listener binding, overlay permissions, and battery optimization exemptions.
31. **Savings Goals Tracker (`GoalsScreen`, `CircularGoalProgressGauge`, `SavingsGoalEntity`)** — target milestone tracker with circular gauges, deposit/withdraw dialogs, and progress tracking.
32. **Data Encryption at Rest (`KanriDatabase`, `DatabaseKeyManager`, `BackupCryptoHelper`)** — AES-256 SQLCipher encrypted database with Android Keystore passphrase management and password-protected encrypted JSON backups.
33. **Bank SMS Coverage Expansion (`BankSmsPatterns.kt`, `SmsParser.kt`)** — dedicated regex patterns for 15+ Indian banks and UPI providers.
34. **Onboarding / First-Run Flow (`OnboardingScreen`, `OnboardingPreferences`)** — 4-page walkthrough explaining offline-first architecture with deferred in-context permission requests.
35. **Settings Screen Consolidation (`SettingsScreen.kt`)** — centralized preferences destination covering Security, Data, Detection, Appearance, Statements, and About.
36. **Dynamic Theming: Light, Dark & AMOLED (`ThemePreferences`, `Color.kt`)** — pure AMOLED black (`#000000`), minimalist dark (`#1E1E1E`), and cream light mode (`#FEFAEC`).
37. **Automated Export Scheduling (`ScheduledExportWorker`, `WorkManager`)** — background periodic PDF/CSV export worker with completion notifications.
38. **Responsive Widget Resize Support** — dynamic RemoteViews layouts adapting smoothly across 1x1, 2x2, and 4x2+ widget dimensions.
39. **Production ProGuard/R8 & Release Signing** — release keystore (`kanri-release.jks`) with optimized R8 keep rules.
40. **Local-Only Crash Reporting (`CrashLogger`, `CrashLogsDialog`)** — zero-cloud crash logger recording uncaught exceptions with an in-app viewer and system sharing.
41. **Irreversible Factory Reset Flow (`FactoryResetManager`)** — typed confirmation ("DELETE") with atomic database wipe, key rotation, and app restart.
42. **16 KB Memory Page Size Compatibility** — upgraded SQLCipher 4.6.1 and 16 KB ELF boundary alignment for Android 15/16.
43. **Panchang Brand Icons & Startup Banners** — calibrated adaptive launcher icons and theme-strict splash screens.
44. **Simplified Theme & App Icon Control** — independent launcher icon selection with targeted restart and clean handoff.
45. **Seamless App Auto-Relaunch** — distinct task affinities (`.dark` / `.light`) preventing OS task destruction during theme restart.
46. **Full Monochrome Palette Overhaul** — unified AMOLED black and cream luxury styling with elimination of extraneous colors.
47. **Real Vector SVG Category Icons** — replaced raw emojis with crisp, scalable vector SVG icons across all categories and bottom sheets.
48. **Frosted Glass Bottom Sheets & Popups** — standardized frosted glass appearance across all sheets with backdrop blur and calibrated dark mode text contrast (`contentColor = MaterialTheme.colorScheme.onSurface`).
49. **Dual-Tone Transaction Amount Accents** — income amounts highlighted in balanced green (`#34C759`) and expense amounts in crisp red (`#E54D2E`), while preserving strict monochrome styling on tags, icons, and cards.
50. **Floating Frosted Glass Dock (`KanriFloatingDock.kt`)** — liquid frosted dock with blur effect, elevated border, centered "+ Add" button, and 4 tabs: **Home**, **Insights**, **Lend/Borrow**, and **Goals**.
51. **Goals Tab on Far Right of Dock** — moved Goals to the far-right dock tab with a custom concentric target SVG icon (`GoalsDockIcon`), providing direct access to the savings tracker.
52. **Recent Transactions 3-Item Preview & Smooth History Transition** — cleaned up Recent Transactions section to show the 3 most recent entries with a minimal "See more ›" button that smoothly transitions via `AnimatedContent` into `SearchScreen` (Transaction History).
53. **Multi-Style Expense Visualizer (`ExpenseVisualizer.kt`)** — 5 selectable visualization styles (Full Ring, Half-Ring Speedometer, Progress Bars, Category Donut, Digital Typography Counter) with frosted month selector and category preview chips.
54. **Daily "Safe-to-Spend" & Pace Widget (`DailySafeToSpendCard`)** — added directly below Recent Transactions, featuring clean hero safe-to-spend allowance per day, days remaining countdown, progress bar, and budget exhaustion metrics.
55. **Animated Support & Issue Reporting Footer (`SupportContactFooter`)** — subtle *"Found an issue? Contact us →"* footer with breathing pulse micro-animation and animated hover arrow, hyperlinked directly to `support.kanri.app@gmail.com`.
56. **Renamed Analytics to Insights** — updated the second dock tab and screen header to **Insights** (`Monthly Insights`), harmonizing naming across navigation and headers.
57. **Rolling Digit Slot-Machine Animation (`AnimatedNumber.kt`)** — rolling digit counter animation applied to Total Spent across all visualizers, Cash Flow (Income, Spent, Net Cash), and Set Budget prompt with immediate display.
58. **Soft Balanced Color Palette (`Color.kt`)** — replaced harsh neon green with Radix sage green (`#30A46C`) for income amounts and net cash; balanced against expense red (`#E54D2E`).
59. **Subtle Category Color Palette (`ExpenseVisualizer.kt`)** — 8 distinct, desaturated, eye-friendly matte pastel colors (Sage, Terracotta, Soft Lavender, Warm Amber, Dusty Teal, Mauve, Slate Blue, Sand) for pie chart slices and legend indicators.
60. **Focused 4-Style Expense Visualizer** — streamlined to 4 essential styles: Full Ring, Bar Graph, Category Pie Chart, and Full Digital.
61. **Cash Flow Summary Card (`CashFlowCard.kt`)** — monthly Income, Spent, and Net Cash Flow with rolling digit animations and softened income green.
62. **Lend & Borrow Quick Pulse Card** — direct status card linking to the Lend/Borrow ledger.
63. **Removed Accounts & Balance Section** — kept Home Screen clean and strictly focused on expenses, cash flow, and debt pulse.
64. **3D FlipFadeText Animation (`FlipFadeText.kt`)** — 3D perspective letter rotation and translation animation with staggered character entrance and cubic-bezier easing.
65. **Dynamic Context-Aware Greeting & Subtitle System (`GreetingResolver.kt`, `GreetingHeader.kt`, `GreetingDataStore.kt`)** — 5 local time buckets (Morning, Afternoon, Evening, Night, Late night), 60/40 weighted variants, deterministic seeding from `(epochDay, timeBucket)`, 11 prioritized finance/calendar subtitle rules, DataStore anti-repetition, 300ms crossfade without layout jump, tone coloring (Amber Warning, Green Positive, Muted Neutral), and contextual click actions.
66. **100% Passing Unit Tests (`GreetingResolverTest.kt`)** — validating time boundaries, seed determinism, priority order, budget guards, blank names, anti-repetition suppression, and month edge cases.
67. **Redesigned Floating '+' Action & Frosted Glass Menu (`AddExpenseIncomePopup.kt`, `KanriFloatingDock.kt`)** — replaced heavy bottom sheet with a smooth frosted glass popup menu card floating above the dock. Fluid 45° rotation on '+' button to morph into '×', smooth spring scale/fade entrance, crisp red 'Expense' and sage green 'Income' with dedicated financial flow vector SVGs, and zero 'debit'/'credit' wording.
68. **Unified Transaction Dialog with In-Popup Category & Note (`AddTransactionDialog.kt`)** — unified Expense & Income dialog modeled after the budget setup popup: central animated digit with `[-]` and `[+]` stepping buttons, tap-on-number to edit manually with autofocus numeric keyboard, small uniformly arranged buttons (`+10`, `+50`, `+100`, `+500`), sorted in-popup category selector with real SVG icons, and note/payee input with zero extra categorization steps.
69. **Updated Budget Setup Dialog (`SetBudgetDialog.kt`)** — tap-on-number to edit manually with autofocus numeric keyboard, removed old bulky preset chips, and added small uniformly arranged buttons (`+10`, `+50`, `+100`, `+500`).

---

## 2. Home Screen Architecture & Comprehensive Description 📱 [COMPLETED & LOCKED]

> **LOCK NOTICE:** The Kanri Home Screen (`HomeScreen.kt`) and all its sub-components are **100% COMPLETED and FINALIZED**. No further code changes are to be made to this screen.

The Kanri Home Screen is the central command center of the application, meticulously crafted around a **minimalist, clutter-free monochrome luxury aesthetic** that delivers maximum financial clarity with zero cognitive overload.

### 2.1 Choreographed App Launch Entrance
Every component enters with a fluid, staggered vertical translation and alpha fade-in on app launch using `Animatable` and `FastOutSlowInEasing`:
1. **Top Header Row** (0ms delay, 400ms duration)
2. **Greeting & Subtitle** (60ms delay, 420ms duration)
3. **Expense Visualizer** (130ms delay, 460ms duration)
4. **Cash Flow Card** (180ms delay, 460ms duration)
5. **Lend & Borrow Pulse Card** (220ms delay, 460ms duration)
6. **Recent Transactions** (260ms delay, 480ms duration)
7. **Daily Safe-to-Spend Card** (300ms delay, 480ms duration)
8. **Support Contact Footer** (340ms delay, 480ms duration)

### 2.2 Top Header Row
- **Brand Wordmark (Left):** Custom bold typography rendering **`KANRI`** in the Panchang font family with uppercase styling and `2.4.sp` letter spacing.
- **Profile & Settings Button (Right):** A 38.dp circular button with `surfaceVariant` background and a subtle `outlineVariant` border enclosing a centered Person icon. Tapping opens the consolidated **Settings** screen.

### 2.3 Dynamic Context-Aware Greeting Header (`GreetingHeader.kt`)
- **3D Staggered Greeting:** Rendered via `FlipFadeText` with 3D rotation (`rotationX: 90° -> 0°`) and character stagger.
- **Time Buckets & Deterministic Variants:** Morning, Afternoon, Evening, Night, Late night with 60% plain / 40% playful weighting, seeded by `(epochDay, timeBucket)` so it stays stable within the bucket.
- **Intelligent Financial Subtitle:** 11 prioritized rules (Over budget, Near limit, Ahead of pace, Milestone streak, Under pace, Better than last month, No spends yet, Review pending, Month start, Month end, Date fallback).
- **Tone-Based Styling & Actions:** Amber `#F5A524` for Warning (taps open Insights), Green `#34C759` for Positive, Muted for Neutral (Review pending taps open Review sheet). 300ms crossfade without layout jump.
- **Anti-Repetition:** DataStore prevents repeating finance rules on consecutive days and limits warnings to once per bucket.

### 2.4 Multi-Style Expense Visualizer (`ExpenseVisualizer.kt`)
The hero visualizer card anchors the upper half of the screen with real-time financial tracking:
- **Card Header:** `EXPENSES OVERVIEW` with frosted month dropdown selector.
- **Hero Total Spent:** Rolling digit slot-machine animation (`AnimatedNumberText`) displaying immediately on load.
- **4 Selectable Visualizer Styles:**
  1. **Full Ring Gauge:** 300° sweeping circular Canvas arc with under-track and budget reference.
  2. **Bar Graph:** Daily / weekly spending bars with clean day indicators.
  3. **Category Pie Chart:** Multi-segment pie chart using `SubtleCategoryPalette` (8 soft matte pastel colors).
  4. **Full Digital:** High-contrast digital metrics with clean typography.
- **Category Distribution Strip:** Horizontally scrollable strip displaying top categories with distinct palette dots, icons, and percentages.
- **Action Buttons:** "Edit" (opens budget dialog) and "Data Type" (opens visualizer picker sheet).

### 2.5 Cash Flow Card (`CashFlowCard.kt`)
Positioned directly below the hero visualizer:
- Monthly **Income** (`+₹...`), **Spent** (`-₹...`), and **Net Cash** (`+₹...`).
- Animated with rolling digit counters.
- Income and positive net cash styled in soft Radix sage green (`#30A46C`).

### 2.6 Lend & Borrow Quick Pulse Card
Direct glanceable pulse:
- Displays real-time debt status ("All settled • No pending dues" or active lent/borrowed amounts).
- Tapping navigates directly to the Lend & Borrow screen.

### 2.7 Recent Transactions Section
- **Header:** "Recent Transactions" with "See more ›" button that smoothly transitions into Transaction History (`SearchScreen`).
- **3-Item Transaction Preview:**
  - Displays the 3 most recent transactions directly on the Home Screen.
  - **Dual-Tone Amount Highlighting:** Expense amounts render in crisp red (`#E54D2E`), and income amounts render in balanced sage green (`#30A46C`).
  - **Monochrome Tags & Details:** Counterparty/payee name, payment source badge (UPI, Card, ATM, Cash, Bank Transfer), bank name, timestamp, and category badge.
  - **Interactive Re-Categorization:** Tapping any transaction card opens `CategoryPickerSheet` to change or assign its category on the spot.

### 2.8 Daily "Safe-to-Spend" & Financial Pace Widget (`DailySafeToSpendCard`)
Positioned directly below Recent Transactions, this card gives the user an actionable daily spending guide:
- **Header:** `DAILY SAFE-TO-SPEND` uppercase label.
- **Hero Metric:** Prominently formatted safe-to-spend allowance per day (`formatCurrency(safeToSpendPerDay)`).
- **Contextual Subtitle:** *"per day for the rest of [Month]"* alongside a countdown of remaining days in the month (*"[X] days left"*).
- **Budget Utilization Bar:** Minimalist `LinearProgressIndicator` showing overall monthly budget progress (transitions to red if over budget).
- **Footer:** Sub-row displaying `Remaining: ₹...` and `Budget: ₹...`.
- **Card Aesthetics:** 20.dp rounded corners, `surfaceVariant.copy(alpha = 0.35f)` background, and subtle `outlineVariant` border.

### 2.9 Animated Support & Issue Reporting Footer (`SupportContactFooter`)
Centered at the bottom of the scrollable column:
- **Text:** *"Found an issue? "* in subdued, soft secondary color.
- **Hyperlink:** *"Contact us →"* with `TextDecoration.Underline` in high-contrast off-white.
- **Micro-Animations:**
  - Subtle breathing pulse on the link's opacity (`0.7f` to `1.0f`).
  - Animated horizontal hover nudge on the `→` arrow icon (`0.dp` to `3.dp`).
- **Direct Email Action:** Tapping opens the device email client targeting **`support.kanri.app@gmail.com`** with subject `"Kanri Support / Issue Report"`.

### 2.10 Floating Frosted Glass Dock (`KanriFloatingDock.kt`)
Floats above the bottom edge with a liquid frosted glass backdrop blur and rounded border:
- **Tab 1: Home** — Active dashboard view with home vector icon.
- **Tab 2: Insights** (formerly Analytics) — Monthly breakdown, category donut chart, daily spend bar chart, and all-time trends.
- **Tab 3: Lend/Borrow** — Peer-to-peer debt ledger and group bill splitting.
- **Tab 4: Goals** — Savings milestones tracker with target circular progress gauges and deposit/withdraw controls.
- **Centered "+ Add" Floating Action Button:** Direct manual transaction entry trigger.
- **Scroll Clearance:** 115.dp spacer at the bottom of the Home screen ensures all content scrolls completely clear of the floating dock.

---

## 3. Current App State 🚀

- **Compilation & Build:** Compiles with zero errors, 100% Kotlin Compose code, 16 KB memory page size compliant, and release signing configured.
- **Theme & Appearance:** 
  - Standard Dark Mode (`#1E1E1E`) and AMOLED Dark Mode (`#000000`).
  - Cream Monochrome Light Mode (`#FEFAEC`).
  - Independent launcher app icon switching with prompt and seamless restart.
- **Navigation Structure:**
  - `KanriTab.HOME` $\rightarrow$ `HomeScreen`
  - `KanriTab.INSIGHTS` $\rightarrow$ `MonthViewScreen`
  - `KanriTab.LEND_BORROW` $\rightarrow$ `LendingScreen`
  - `KanriTab.GOALS` $\rightarrow$ `GoalsScreen`
- **Testing & Verification:** Verified live on Pixel 8 virtual device (API 34/35/37) with interactive screenshots across all screens and sheets.

---

## 4. Technical Stack

- **Language:** Kotlin 2.2.10
- **UI Toolkit:** Jetpack Compose (BOM 2026.02.01) + Material 3
- **Database:** Room 2.8.5 with **SQLCipher 4.6.1** AES-256 encryption at rest (16 KB page-size compliant)
- **Security:** Android Keystore + `EncryptedSharedPreferences` + Tink AES-GCM + PBKDF2 HMAC-SHA256
- **Biometrics:** AndroidX Biometric 1.2.0 (`BiometricPrompt` with `BIOMETRIC_STRONG` / `DEVICE_CREDENTIAL`)
- **Background SMS Detection:** Manifest-registered `BroadcastReceiver` with multi-part concatenation & deduplication
- **Receive-Side Notification Detection:** `NotificationListenerService`
- **Background Tasks & Scheduling:** AndroidX `WorkManager` 2.9.1 (periodic PDF/CSV statement exports)
- **Widgets:** Android `AppWidgetProvider` with Small / Medium / Large RemoteViews responsive breakpoints
- **Crash Reporting:** Zero-cloud local disk crash logger with in-app viewer and system sharing
- **PDF & CSV Export:** Android native `PdfDocument` + RFC 4180 CSV engine
- **Minification & Signing:** AGP 9.4.0 + ProGuard / R8 with release signing keystore (`useLegacyPackaging = false`)

---

## 5. Active Database Schema

**transactions**
- `id`, `type` (DEBIT/CREDIT), `amount`, `source_type` (UPI/ATM/CARD/BANK_TRANSFER/CASH), `counterparty`, `display_name`, `bank`, `ref_no`, `timestamp`, `category_id`, `raw_sms`, `is_duplicate`, `is_manual_entry`

**categories**
- `id`, `name`, `colorHex`, `iconName`

**counterparty_category_map**
- `counterparty` (Primary Key), `category_id`

**budgets**
- `id`, `month`, `category_id` (nullable = overall monthly budget), `amount`

**lending**
- `id`, `person_name`, `amount`, `type` (LENT/BORROWED), `date`, `return_date`, `is_settled`, `notes`

**recurring_payments**
- `id`, `title`, `amount`, `billing_cycle` (MONTHLY/YEARLY/WEEKLY), `next_due_date`, `is_auto_detected`, `is_active`, `category_id`

**savings_goals**
- `id`, `name`, `target_amount`, `current_amount`, `icon_emoji`, `created_at`, `is_completed`

---

## 6. What This App Deliberately Does NOT Do

- **No Loans or Credit Lines:** Pure personal expense tracking without predatory credit offerings.
- **No Investment Products:** No fixed deposits, mutual funds, or stock trading integrations.
- **Zero Cloud Leakage:** No third-party servers, no remote tracking, and no external data sync — all financial data remains 100% offline and encrypted on the user's device.
- **No Forced Generic Categories:** Users have full freedom to customize, rename, and assign categories.
