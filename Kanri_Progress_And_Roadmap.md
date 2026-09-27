# Kanri — Master Progress Report & Engineering Roadmap

**App Name:** Kanri  
**Package Name:** `com.omkarnub.kanri`  
**Purpose:** 100% offline, privacy-first personal financial operating system — automatically detects UPI transactions, ATM withdrawals, card payments, and bank transfers (sent & received), tracks daily and monthly cash flow, budgets, lending/borrowing ledgers, group split expenses, financial health scoring, savings milestones, and encrypted data exports.  
**Target Platform:** Android 8.0+ (Min SDK 26, Target SDK 37, 16 KB memory page size compliant).  
**Tested On:** Pixel 8 virtual & physical devices (API 34/35/37) and real hardware (Android 14+).  
**Current Release:** v1.0.1 (versionCode 2)  
**Tech Stack:** Kotlin 2.2.10, Jetpack Compose (BOM 2026.02.01), Material 3, Room 2.8.5, SQLCipher 4.6.1 (AES-256), AndroidX Biometric 1.2.0, WorkManager 2.9.1, Haze Glassmorphism.  
**Design Philosophy:** Strict minimalist monochrome luxury aesthetic (pure AMOLED black `#000000`, refined dark `#1E1E1E`, cream light `#FEFAEC`, frosted glass accents), zero visual clutter, surgical dual-tone amount accents (Expense Red `#E54D2E`, Radix Sage Green `#30A46C`), Panchang brand typography, Google Sans Flex UI typography, and fluid choreographic micro-animations.

---

## 1. What's Done So Far ✅ (Complete Chronological Changelog)

1. **Development Environment & Setup** — Android Studio configured on Windows, Kotlin 2.2.10 + Jetpack Compose initialized with Empty Activity template.
2. **Virtual Device Baseline** — Pixel 8 virtual device created, configured, and verified launching Jetpack Compose successfully.
3. **SMS Permissions Declared** — `RECEIVE_SMS` and `READ_SMS` declared in `AndroidManifest.xml` with dynamic in-context runtime permission handling.
4. **SmsReceiver Implementation** — Manifest-registered `BroadcastReceiver` (`SmsReceiver.kt`) listening for `android.provider.Telephony.SMS_RECEIVED`.
5. **Multi-Part SMS Concatenation** — Reassembles fragmented PDU segments into cohesive single SMS payloads prior to parsing.
6. **Regex Financial Transaction Parser (`SmsParser.kt`)** — High-precision extraction of transaction amount, type (DEBIT/CREDIT), source (UPI, ATM, CARD, BANK_TRANSFER), merchant/counterparty, bank institution, and reference number. Backed by `SmsParserTest.kt`.
7. **Database Architecture** — Room 2.8.5 with KSP 2.2.10, SQLCipher 4.6.1 AES-256 database encryption at rest (`KanriDatabase.kt`).
8. **Deduplication Engine** — Reference number and timestamp deduplication protecting against duplicate bank alerts and network retries.
9. **Live End-to-End SMS Pipeline Verification** — Validated live on Pixel 8 with real bank alerts from Bank of Baroda, Kotak Mahindra, HDFC, SBI, ICICI, and manual cash entries.
10. **Categorization Engine with Counterparty Memory** — 19 default system categories, tap-to-label interactive bottom sheet (`CategoryPickerSheet.kt`), and persistent memory auto-mapping (`counterparty_category_map`).
11. **Budget Engine & Safe-to-Spend Allowance** — Room persistence (`budgets` table), mathematical safe-to-spend allocation (`BudgetCalculator.kt`), and interactive modal dialog (`SetBudgetDialog.kt`).
12. **Refund, Reversal & Declined Spends Support** — Accurate parsing of refund, chargeback, and reversal alerts crediting or balancing ledgers cleanly.
13. **Monthly Analytics & Custom Canvas Visualizers** — `MonthViewScreen.kt`, custom Canvas `CategoryDonutChart`, and daily bar chart with touch interaction.
14. **Lend & Borrow Peer-to-Peer Ledger** — Debt ledger (`LendingEntity`, `LendingDao`) tracking 'Money I Lent' vs 'Money I Borrowed', net balance calculation, return date presets, and settled toggles.
15. **Split Bill Engine (`SplitExpenseSheet.kt`, `SplitExpenseScreen.kt`)** — Interactive bill splitting dynamically dividing expenses among 2 to 10+ participants with dual-role ledger integration.
16. **Native Biometric App Lock (`BiometricHelper.kt`, `SecurityManager.kt`, `LockScreen.kt`)** — 100% native Android authentication using `BiometricPrompt` supporting `BIOMETRIC_STRONG` / `DEVICE_CREDENTIAL` (Fingerprint, Face, PIN, Pattern).
17. **Database Backup & SAF Restore Engine (`BackupRepository.kt`, `BackupData.kt`)** — JSON import/export via Storage Access Framework with atomic transactional restoration (`withTransaction`).
18. **Financial Statement Exporters (`CsvExporter.kt`, `PdfStatementExporter.kt`)** — RFC 4180 standard CSV exporter and native multi-page vector PDF statement generator with summary cards and ledger tables.
19. **Search & Multi-Criteria Filtering Engine (`SearchScreen.kt`, `SearchViewModel.kt`)** — Instant full-text search, type filtering, date range presets, amount range filters, category chips, source badges, and sort options.
20. **Category Deep-Dive & Trendline Chart (`CategoryDetailScreen.kt`)** — 6-month cubic bezier spend trendline, month-over-month percentage deltas, top merchant rankings, and scoped transaction list.
21. **Truecaller-Style Instant Popup Overlay (`InstantPopupService.kt`, `InstantPopupCard.kt`)** — System overlay card (`TYPE_APPLICATION_OVERLAY`) with 8-second auto-dismiss countdown, 1-tap quick categorization, and heads-up notification fallback.
22. **Receive-Side Notification Listener (`KanriNotificationListenerService.kt`)** — Background listener capturing incoming payment push notifications from Google Pay, PhonePe, Paytm, Amazon Pay, FamPay, and BHIM with cross-channel deduplication.
23. **All-Time Multi-Month Historical Trends (`AllTimeTrendsView.kt`)** — Multi-year financial trajectory with touch-scrubbing Canvas curves, monthly breakdown chips, and averages.
24. **Recurring Subscriptions Tracker (`RecurringPaymentsSheet.kt`, `RecurringPaymentEntity`)** — Auto-detection of repeating monthly subscriptions (Netflix, Spotify, Rent, WiFi) with renewal countdowns and due date management.
25. **Android Glanceable Home Screen Widgets (`KanriAppWidgetProvider`)** — Small (1x1), Medium (2x2), and Large (4x2+) responsive RemoteViews widgets for home screen balance and quick add.
26. **Health Diagnostics Dashboard (`HealthCheckBanner.kt`, `DiagnosticsSheet.kt`)** — Live system monitor tracking SMS permissions, notification listener binding, overlay permissions, and battery optimization exemptions.
27. **Savings Goals System v1 (`GoalsScreen.kt`)** — Target milestone tracker with circular gauges and progress metrics.
28. **Cryptographic Data Protection at Rest (`DatabaseKeyManager.kt`, `BackupCryptoHelper.kt`)** — SQLCipher 4.6.1 AES-256 database encryption with Android Keystore passphrase management and password-protected encrypted JSON backups (Tink AES-GCM + PBKDF2 HMAC-SHA256).
29. **Expanded Indian Banking Regex Patterns (`BankSmsPatterns.kt`)** — Dedicated regex patterns covering 15+ major Indian banks and UPI handles (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, Canara, IndusInd, Federal, Union, Paytm Payments Bank, GPay, Cred).
30. **Onboarding / First-Run Walkthrough (`OnboardingScreen.kt`)** — 4-step onboarding explaining 100% offline security, deferred in-context permission requests, and user personalization.
31. **Centralized Settings Destination (`SettingsScreen.kt`)** — Unified management for Security, Data Backup/Restore, Detection Engines, Appearance, Statements, Crash Logs, Diagnostics, and Factory Reset.
32. **Dynamic Tri-Theme Palette (`ThemePreferences.kt`, `Color.kt`)** — AMOLED Dark (`#000000`), Standard Dark (`#1E1E1E`), and Cream Monochrome Light (`#FEFAEC`).
33. **Automated Export Scheduling (`ScheduledExportWorker.kt`)** — AndroidX WorkManager periodic background worker delivering automated weekly or monthly PDF/CSV financial statements.
34. **Release Optimization & ProGuard/R8 Signing** — Production release signing keystore (`kanri-release.jks`) with optimized R8 keep rules (`useLegacyPackaging = false`).
35. **Zero-Cloud Local Crash Logger (`CrashLogger.kt`, `CrashLogsDialog.kt`)** — Uncaught exception handler logging errors to encrypted local disk with in-app stack trace viewer and system sharing.
36. **Irreversible Atomic Factory Reset (`FactoryResetManager.kt`)** — Safe data sanitization requiring typed confirmation ("DELETE"), database wipe, cryptographic key rotation, and clean restart.
37. **16 KB Memory Page Boundary Alignment** — Upgraded native libraries and SQLCipher 4.6.1 to guarantee zero crash compatibility on Android 15 and 16.
38. **Launcher Icon Switching Engine (`AppIconManager.kt`)** — 4 distinct manifest activity aliases (`MainActivityDarkDark`, `MainActivityDarkLight`, `MainActivityLightDark`, `MainActivityLightLight`) with unique task affinities allowing seamless launcher icon switching without OS task destruction.
39. **Monochrome Design Overhaul** — Cleaned all harsh colors, standardizing on pure AMOLED black, crisp off-whites, and frosted glass.
40. **Vector SVG Category Icons (`CategoryIcon.kt`)** — Replaced all emojis with 19 scalable vector icons rendered with theme-aware tints.
41. **Frosted Glass Bottom Sheets & Popups (`KanriGlass.kt`)** — Haze-powered backdrop blur across all bottom sheets and dialogs with high-contrast text typography.
42. **Surgical Dual-Tone Amount Accents** — Radix Sage Green (`#30A46C`) for income amounts and Net Cash; Crisp Red (`#E54D2E`) for expense amounts.
43. **Floating Frosted Glass Dock (`KanriFloatingDock.kt`)** — Liquid frosted dock with centered morphing "+ Add" FAB and 4 core navigation destinations: **Home**, **Insights**, **Lend/Borrow**, and **Goals**.
44. **3-Item Recent Transactions Preview** — High-clarity preview of the 3 latest transactions on Home with seamless "See more ›" transition into full Transaction History (`SearchScreen.kt`).
45. **Multi-Style Expense Visualizer (`ExpenseVisualizer.kt`)** — 4 selectable visualization styles: Full Ring Gauge, Bar Graph, Category Pie Chart, and Full Digital Typography with frosted month selector and category distribution strip.
46. **Daily Safe-to-Spend & Pace Card (`DailySafeToSpendCard.kt`)** — Prominent safe daily spend allowance, days remaining in month countdown, and utilization progress indicator.
47. **Animated Support Footer (`SupportContactFooter.kt`)** — Breathing pulse animation on contact link hyperlinking directly to `support.kanri.app@gmail.com`.
48. **Rolling Digit Slot Machine Counter (`AnimatedNumber.kt`)** — Dynamic rolling number animation applied across Total Spent, Cash Flow metrics, and Budget dialog.
49. **3D Perspective FlipFadeText (`FlipFadeText.kt`)** — 3D rotation and translation animation with staggered character entrance and cubic-bezier easing.
50. **Context-Aware Dynamic Greeting Engine (`GreetingResolver.kt`, `GreetingHeader.kt`)** — 5 local time buckets (Morning, Afternoon, Evening, Night, Late night), 60/40 weighted variants, deterministic daily seeding, 11 prioritized financial subtitle rules, and DataStore anti-repetition.
51. **Unified Transaction Add Dialog (`AddTransactionDialog.kt`)** — Interactive Expense & Income builder with central animated digit, stepping buttons (`+10`, `+50`, `+100`, `+500`), tap-on-number autofocus keyboard, sorted category selector, and note input.
52. **Tap-to-Edit Budget Setup Dialog (`SetBudgetDialog.kt`)** — Interactive monthly limit editor with autofocus numeric input and stepping buttons.
53. **No-Spend Streak Tracker & Milestone Celebrations (`NoSpendStreakCard.kt`, `StreakCalculator.kt`, `StreakDataStore.kt`)** — Real-time calculation of consecutive zero-expense days with celebratory badges at 3, 7, 14, 30, and 100 days.
54. **Full-Page Transaction Detail Inspector (`TransactionDetailScreen.kt`)** — Deep inspector and editor for every transaction: amount hero card, counterparty, category picker, date/time pickers, source, bank, ref number, editable notes, raw SMS viewer with copy, review resolution, direct linkage to goals or lend/borrow, and deletion.
55. **Needs-Review Queue & Banner (`NeedsReviewCard.kt`, `NeedsReviewSheet.kt`)** — Flags low-confidence SMS alerts, unknown counterparties, or zero-amount transfers for 1-tap categorization.
56. **User Profile System & Preferences (`ProfileScreen.kt`, `UserProfilePreferences.kt`)** — Local user profile management with photo avatar upload/capture, user name, date of birth, and direct navigation hubs to Settings, Export, Recurring Spends, Backups, and Account Reset.
57. **Financial Health Score & Diagnostics Engine (`FinancialHealthCalculator.kt`, `FinancialHealthCard.kt`, `FinancialHealthDetailsSheet.kt`)** — Proprietary 0–100 algorithm analyzing 4 financial pillars (Savings Rate, Budget Adherence, Spend Velocity, Debt Burden) generating letter grades (A+ to F), pacing comparisons, vitals grid, and actionable observations.
58. **Insights Deep-Dive Modular Architecture (`MonthViewScreen.kt`, `InsightsViewModel.kt`)** — Complete overhaul into 18+ modular analytical sections:
    - `InsightsRangeBar` (7 range presets + Custom) & Mode Selector (Standard / Deep Dive)
    - `InsightsToolsRow` (Compare Months, Monthly Recap, All-Time Trends, Export Statement)
    - `MonthSummaryStripSection` (Spend, Income, Net Cash, Safe Spend, Daily Avg)
    - `CheckFinancialHealthCard` (Direct gateway to Health Score)
    - `MonthEndProjectionSection` (Burn-rate projection vs monthly budget)
    - `CumulativeSpendLineSection` (Current month vs prior month spend trajectory curves)
    - `BudgetVsActualSection` (Per-category budget vs actual spend progress bars)
    - `CalendarHeatmapSection` (GitHub-style calendar matrix of daily spend intensity)
    - `YearHeatmapSection` (365-day annual spending heatmap)
    - `CategoryMoversSection` (Biggest percentage jumps or drops)
    - `TopPayeesSection` (Ranked merchants with drill-down to `PayeeDetailScreen.kt`)
    - `BiggestTransactionsSection` (Top debits and credits of the period)
    - `PaymentMethodSplitSection` (Distribution across UPI, Card, ATM, Bank Transfer, Cash)
    - `WeekendVsWeekdaySection` (Weekend splurge vs weekday burn analysis)
    - `SmallSpendsSection` (Latte Factor micro-transactions analysis)
    - `IncomeSourcesSection` (Salary, Reimbursements, UPI credits breakdown)
    - `SplitExpensesSummarySection` (Split expense ledger summary)
    - `SuggestedBudgetSection` (Intelligent recommendations based on 3-month trailing spends)
    - `PersonalRecordsSection` (Highest/lowest spend days and single biggest transactions)
59. **Compare Months Screen (`CompareMonthsScreen.kt`)** — Full side-by-side comparative analysis of any two arbitrary months with delta percentages across totals and categories.
60. **Monthly Recap Visual Story (`MonthlyRecapScreen.kt`, `MonthlyRecapCard.kt`)** — Spotify/Instagram Wrapped-style interactive card deck summarizing monthly spending, top categories, peak days, and financial wins.
61. **Payee Detail Deep-Dive (`PayeeDetailScreen.kt`)** — Dedicated inspector for any individual merchant/payee showing total spent, visit frequency, average ticket size, and historical transaction log.
62. **Lend & Borrow Hub v2 Architecture (`LendingScreen.kt`, `LendingHubViewModel.kt`)** — Comprehensive peer-to-peer debt ledger overhaul:
    - Hero Overview Card with `AnimatedMonochromeRingChart` (Net balance, lent vs borrowed amounts, debt ratio)
    - People View (`LendingPeopleView.kt`) with individual person cards, net balances, overdue chips, and settled counts
    - Person Detail View (`LendingPersonDetailView.kt`) with full transaction timeline, partial repayment ledger, settle-all dues, rename person, delete, WhatsApp polite reminder generator, and direct UPI payment trigger
    - Timeline View (`LendingTimelineView.kt`) with chronological debt entries, return-date presets, overdue badges, and filter chips (All, Lent, Borrowed, Settled, Overdue)
    - Split Groups Card (`LendingSplitGroupCard.kt`) linking directly to group bill splitting
63. **Partial Repayment Engine (`LendingRepaymentEntity`, `LendingRepaymentDialog.kt`, Room Migration `8_9`)** — Support for logging partial repayments against any debt entry with timestamps, notes, and remaining balance recalculation.
64. **Native UPI Deep-Link Payments (`LendingUpiHelper.kt`, `LendingUpiPayDialog.kt`)** — 1-tap settlement opening installed UPI apps (GPay, PhonePe, Paytm, BHIM) with recipient VPA and amount pre-filled.
65. **Automated Debt Reminders (`LendingReminderScheduler.kt`, `LendingReminderWorker.kt`)** — WorkManager scheduled background worker dispatching system notifications for approaching and overdue return dates.
66. **Transaction to Lending Bridge (`LendingTransactionBridgeDialog.kt`, `LendingTransactionSyncHelper.kt`)** — 1-tap conversion of any bank/UPI debit or credit transaction into a tracked Lend/Borrow entry.
67. **Savings Goals System v2 Overhaul (`GoalsScreen.kt`, `GoalDialogs.kt`, `SavingsGoalsSheet.kt`)** —
    - 16 custom scalable vector SVG icons (`ic_goal_target`, `ic_goal_savings`, `ic_goal_laptop`, `ic_goal_car`, `ic_goal_home`, `ic_goal_travel`, `ic_goal_shield`, `ic_goal_education`, `ic_goal_gift`, `ic_goal_trophy`, `ic_goal_investment`, `ic_goal_star`, `ic_goal_deposit`, `ic_goal_withdraw`, `ic_goal_history`, `ic_goal_calendar`)
    - Contribution History Ledger backed by `SavingsGoalContributionEntity` (Room migration `9_10`) tracking deposit/withdrawal logs with notes and source transactions
    - Daily & Monthly Pace Calculator (`SavingsGoalCalculator.kt`) based on target deadlines and remaining amounts
    - Monetary Steppers dialogs (`+₹500`, `+₹1000`, `+₹2000`, `+₹5000`, `+₹10000`)
    - Direct Income Allocation: toggle in `CategoryPickerSheet`, `AddTransactionDialog`, and `NeedsReviewSheet` to allocate income transactions directly into any active savings goal
    - Status filtering (In Progress, Completed, All) and 4-way sorting
68. **Haptic Feedback Utility (`KanriHaptics.kt`)** — Unified haptic response engine delivering light clicks, medium clicks, double clicks, and error shakes across tabs, steppers, and dialogs.
69. **Custom Category Management (`CategoryPickerSheet.kt`, `CategoryDao.kt`)** — Full user-created custom expense/income categories with custom naming, hex color selection, and vector SVG icon assignment (`is_custom = 1`).
70. **Redesigned 5-Step Luxury Onboarding & Mesh Gradient Engine (`OnboardingScreen.kt`, `GrainGradientBackground.kt`)** — Reimagined onboarding flow featuring a dynamic Canvas film-grain noise and multi-point animated mesh gradient background, interactive selection cards, deferred respectful permission handling, offline vs cloud mode selection, and personalized financial baseline setup (name, avatar, currency, monthly budget).
71. **Optional End-to-End Encrypted Cloud Backup via Google Drive AppData (`GoogleDriveBackupManager.kt`, `CloudBackupPreferences.kt`, `CloudSyncCard.kt`)** — 100% private, zero third-party cloud infrastructure. Backups are encrypted locally with AES-GCM (Tink PBKDF2) using the user's master key and uploaded strictly to the user's hidden Google Drive `appDataFolder` (`drive.appdata` scope), with automated background sync, manual restore, and one-tap offline-only fallback.
72. **Dedicated Privacy Policy Center & Transparency Architecture (`PRIVACY_POLICY.md`, `PrivacyPolicyDialog.kt`)** — Transparent in-app privacy center detailing local storage guarantees, encryption mechanisms, zero third-party trackers, and scoped permission usage, backed by full root documentation in `PRIVACY_POLICY.md`.
73. **7 Dedicated Glanceable Android Home Screen Widgets & Live Canvas Engine (`KanriWidgetsUpdater.kt`, `WidgetCanvasRenderer.kt`, `KanriWidgetActions.kt`)** — Comprehensive suite of 7 specialized Android AppWidgets:
    - *Money Spent Today Widget* (`MoneySpentTodayWidgetProvider`) — Today's spend total, transaction count, and instant '+' launch action.
    - *Budget Progress Widget* (`BudgetProgressWidgetProvider`) — Linear budget progress gauge, daily safe-to-spend allowance, and month pace indicator.
    - *Budget Ring Widget* (`BudgetRingWidgetProvider`) — Custom Canvas high-res antialiased circular donut ring showing budget consumption percentage, safe spend, and days left.
    - *Goals Ring Widget* (`GoalsRingWidgetProvider`) — Circular Canvas progress ring tracking top savings goal progress, milestone target, and accumulated savings.
    - *Lend & Borrow Widget* (`LendBorrowWidgetProvider`) — At-a-glance net debt ledger (to collect vs to pay) and overdue alert chips.
    - *Split Bill Widget* (`SplitBillWidgetProvider`) — Quick-access group bill split calculator with 1-tap participant splits.
    - *Quick Add Widget* (`QuickAddWidgetProvider`) — Floating action bar for instant Expense, Income, and Split Bill logging.
    All widgets dynamically redraw via `KanriWidgetsUpdater` on every transaction, budget, or goal event.
74. **Android Quick Settings Tile (`QuickAddTileService.kt`)** — Native Android notification shade tile ("Quick Add" / "Log Expense") registered with `BIND_QUICK_SETTINGS_TILE`. Features Android 14+ `PendingIntent` collapse support, legacy fallbacks, and `unlockAndRun` handler for seamless transaction logging directly from lock screens or within any app.
75. **Smart Merchant Rule Editor & Real-Time Simulator (`SmartMerchantRulesScreen.kt`)** — Comprehensive keyword auto-categorization engine:
    - Interactive Rule Simulator / Match Tester to test live inputs against saved rules.
    - Substring matching algorithm with longest-match precedence (`findSmartRuleForCounterparty`).
    - Pre-populated starter chips for popular merchants (Swiggy, Zomato, Starbucks, Uber, Ola, Netflix, Amazon, etc.).
    - Retroactive categorization engine (`applyCategoryToMatchingTransactions`) updating historical transactions in 1 tap.
    - Full management hub in Settings with rule counts, category badges, and edit/delete modal dialogs.
76. **Centralized Widget Event Bus (`KanriWidgetActions.kt`, `KanriWidgetsUpdater.kt`)** — Dispatches targeted broadcast intents (`ACTION_EXPENSE_ADDED`, `ACTION_UPDATE_ALL`) and synchronizes AppWidgetManager state across all 7 providers.
77. **36 Comprehensive Unit Test Suites (232+ Unit Tests)** — Complete coverage of all business logic, parsers, cryptographic routines, cloud sync states, merchant rules, widget layouts, deduplication logic, and financial health calculators with 100% pass rate.
78. **Hardened SMS Receiver Pipeline (`SmsReceiver.kt`)** — Upgraded to `goAsync()` with priority 999 intent filter, ensuring Kanri's SMS processor runs before other SMS apps. Multi-part PDU concatenation now operates within a `goAsync()` coroutine scope for reliable background processing.
79. **Boot Persistence Engine (`BootReceiver.kt`)** — Manifest-registered `BOOT_COMPLETED` BroadcastReceiver that re-initializes all background monitors (NotificationListenerService watchdog, widget midnight reset, lending reminder scheduler, daily reminder scheduler) after device reboot, ensuring zero-gap detection without requiring manual app launch.
80. **Notification Listener Watchdog (`NotificationListenerWatchdogWorker.kt`)** — 15-minute periodic WorkManager watchdog that detects silently disconnected `NotificationListenerService` (common on MIUI, ColorOS, FunTouchOS battery savers) and triggers automatic rebind to restore payment notification capture without user intervention.
81. **Midnight Widget Auto-Reset (`MidnightWidgetResetWorker.kt`)** — WorkManager periodic worker firing 5 seconds past midnight to refresh all 7 home screen widgets, ensuring "Money Spent Today" and daily counters reset cleanly at the start of each new day.
82. **Daily 9 PM Smart Reminder Notifications (`DailyReminderScheduler.kt`, `DailyReminderWorker.kt`)** — End-of-day WorkManager worker that checks for uncategorized transactions, zero-transaction days, and approaching lending dues, dispatching contextual notification reminders to maintain financial tracking discipline.
83. **Cross-Channel 5-Minute Sliding Window Deduplication (`NotificationDeduplicationHelper.kt`)** — Enhanced two-way deduplication engine using a 5-minute sliding timestamp window to prevent duplicate entries when both an SMS alert and a UPI app notification arrive for the same transaction, with cross-channel metadata enrichment.
84. **Battery Optimization Exemption Helper (`BatteryOptimizationHelper.kt`)** — Guided prompt requesting battery optimization exemption for Kanri so that background services (SMS receiver, notification listener, widget updater) are not silently killed by Android Doze or OEM battery savers.
85. **Development Capture Logger (`DevCaptureLogger.kt`)** — Structured development-time logging utility that captures SMS and notification payloads to local encrypted files for debugging parser failures and missed transaction patterns.

---

## 2. Screen-by-Screen Architecture & System Breakdown 📱

### 2.1 Main Navigation Shell (`MainNavigationScreen.kt` & `KanriFloatingDock.kt`)
The app utilizes a custom **CompositionLocal** state architecture wrapped in `LocalHazeState` for fluid blur propagation:
- **4 Main Tabs:**
  - `KanriTab.HOME` $\rightarrow$ `HomeScreen`
  - `KanriTab.INSIGHTS` $\rightarrow$ `MonthViewScreen`
  - `KanriTab.LEND_BORROW` $\rightarrow$ `LendingScreen`
  - `KanriTab.GOALS` $\rightarrow$ `GoalsScreen`
- **Floating Glass Dock:** Liquid frosted dock hovering above the bottom edge with 115.dp content scroll clearance.
- **Center Morphing '+' Action:** Rotates 45° into an '×' to summon `AddExpenseIncomePopup.kt` (frosted menu card floating above dock with Sage Green 'Income' and Crisp Red 'Expense' triggers).
- **Sub-Screen Transitions:** Hardware back-handler and horizontal slide/fade animations for `SearchScreen` (Transaction History), `TransactionDetailScreen`, `ProfileScreen`, and `SettingsScreen`.

---

### 2.2 Home Screen (`HomeScreen.kt`) [LOCKED & FINALIZED]
The central dashboard delivering instant financial clarity with zero cognitive load:
1. **Choreographed Staggered Launch:** 9-step entrance animation easing each card into view on app launch.
2. **Top Header Row:** KANRI bold wordmark in Panchang font (left) and 38.dp circular User Profile avatar with image bitmap or initial (right) opening `ProfileScreen`.
3. **Dynamic 3D Greeting Header (`GreetingHeader.kt`):**
   - 3D FlipFadeText letter rotation and character stagger.
   - 5 local time buckets (Morning, Afternoon, Evening, Night, Late night) with 60/40 weighted variants and deterministic daily seeding.
   - 11 prioritized financial subtitle rules with tone coloring (Amber Warning `#F5A524`, Green Positive `#34C759`, Muted Neutral) and DataStore anti-repetition.
4. **Needs Review Badge:** Appears dynamically when unassigned or low-confidence transactions require verification, opening `NeedsReviewSheet`.
5. **Multi-Style Expense Visualizer (`ExpenseVisualizer.kt`):**
   - Header with frosted month selector dropdown and month-over-month delta chip.
   - Rolling digit slot-machine counter displaying total monthly spent.
   - 4 selectable styles: Full Ring Gauge, Bar Graph, Category Pie Chart, Full Digital Counter.
   - Category distribution strip with matte pastel dots and percentages.
6. **Cash Flow Summary Card (`CashFlowCard.kt`):** Monthly Income (`+₹...`), Spent (`-₹...`), and Net Cash Flow with rolling digits and Radix sage green styling.
7. **Lend & Borrow Quick Pulse Card:** Real-time glanceable debt balance ("₹X to collect", "₹Y you owe", or "All settled") navigating directly to Lend/Borrow.
8. **Recent Transactions Preview:** Shows the 3 latest transactions with dual-tone amounts (+sage green / -crisp red), payment method pills, category SVG badges, and "See more ›" transition into full Transaction History.
9. **Daily Safe-to-Spend Card (`DailySafeToSpendCard.kt`):** Daily spending allowance, days remaining countdown, and utilization progress indicator.
10. **No-Spend Streak Card (`NoSpendStreakCard.kt`):** Calculates consecutive zero-spend days with celebratory animations at 3, 7, 14, 30, and 100-day milestones.
11. **Support & Issue Reporting Footer (`SupportContactFooter.kt`):** Pulsing contact link with animated hover arrow hyperlinking directly to `support.kanri.app@gmail.com`.

---

### 2.3 Insights Tab (`MonthViewScreen.kt` & `InsightsViewModel.kt`)
The advanced analytics and financial intelligence suite:
- **Range & Mode Selector:**
  - Presets: This Month, Last Month, 30 Days, 90 Days, This Year, All Time, Custom Range.
  - Mode toggle: Standard Mode vs Deep Dive Mode.
- **Insights Tools Row:** Direct quick-actions for Compare Months, Monthly Recap, All-Time Trends, and Export Statement.
- **Financial Health Score Card (`CheckFinancialHealthCard.kt`):** Direct gateway to `FinancialHealthDetailsSheet.kt` presenting a 0–100 score, letter grade (A+ to F), 4 pillar breakdown, pacing comparison, and actionable tips.
- **18+ Modular Analytical Sections:**
  - *Month Summary Strip:* Spend, Income, Net Cash, Safe Spend, Daily Avg.
  - *Month-End Projection:* Trajectory burn rate predicting end-of-month spend vs budget.
  - *Cumulative Spend Line Chart:* Current month spend curve plotted against previous month.
  - *Budget vs Actual:* Per-category spend bars against category limits.
  - *Calendar Spend Heatmap:* Interactive GitHub-style matrix of daily spend intensity.
  - *Year Heatmap:* 365-day annual spending matrix with intensity shading.
  - *Category Movers:* Highlights categories with largest percentage increases or drops.
  - *Top Payees Ranking:* Ranked merchant table with direct drill-down to `PayeeDetailScreen.kt`.
  - *Biggest Transactions:* Top debit and credit outlays of the period.
  - *Payment Method Split:* Breakdown across UPI, Card, ATM, Bank Transfer, and Cash.
  - *Weekend vs Weekday:* Spending pattern comparison between weekdays and weekends.
  - *Small Spends (Latte Factor):* Aggregates micro-transactions under ₹200.
  - *Income Sources:* Origin breakdown of all inflows.
  - *Split Expenses Summary:* Integrated group bill split ledger status.
  - *Suggested Budgets:* Algorithmic budget recommendations based on 3-month trailing averages.
  - *Personal Records:* All-time highest spend day, lowest spend day, and peak transaction.
- **Sub-Screens & Drill-Downs:**
  - `CompareMonthsScreen.kt`: Side-by-side comparative analysis of two selected months.
  - `MonthlyRecapScreen.kt`: Visual, shareable story deck summarizing monthly finances.
  - `PayeeDetailScreen.kt`: Complete historical spending profile for an individual merchant.
  - `CategoryDetailScreen.kt`: 6-month trendline and payee rankings for any category.
  - `AllTimeTrendsView.kt`: Multi-month Canvas historical trends with touch scrubbing.

---

### 2.4 Lend & Borrow Hub (`LendingScreen.kt` & `LendingHubViewModel.kt`)
A complete peer-to-peer credit and debt ledger:
- **Hero Overview Card:** High-contrast `AnimatedMonochromeRingChart` displaying Net Balance, Total Lent Pending, Total Borrowed Pending, and debt ratio.
- **Segmented View Controller:** Switch between **People View** and **Timeline View**.
- **People View (`LendingPeopleView.kt`):**
  - Aggregated contact cards showing net balance per person, overdue warning badges, and settled records.
  - Tapping opens `LendingPersonDetailView.kt`.
- **Person Detail View (`LendingPersonDetailView.kt`):**
  - Contact balance header (amount to collect vs amount you owe).
  - Complete chronological ledger of transactions with this person.
  - Partial repayment history log.
  - Settle all dues dialog (`LendingSettleAllDialog.kt`).
  - Record partial repayment dialog (`LendingRepaymentDialog.kt`).
  - Rename person across all records (`LendingRenameDialog.kt`).
  - Native UPI payment trigger (`LendingUpiPayDialog.kt`).
  - WhatsApp polite reminder generator (`LendingReminderUtils.kt`).
  - Deletion of individual entries or full contact profile.
- **Timeline View (`LendingTimelineView.kt`):**
  - Chronological debt entries with type badges (Lent / Borrowed), return date countdowns, overdue warnings, and linked transaction badges.
  - Filter chips: All, Lent, Borrowed, Settled, Overdue.
- **Split Bill Engine (`LendingSplitGroupCard.kt`, `SplitExpenseSheet.kt`, `SplitExpenseScreen.kt`):**
  - Dynamic bill splitting among 2 to 10+ people with dual-role ledger integration.
- **Partial Repayments:** Backed by `LendingRepaymentEntity` allowing multiple installments toward a debt.
- **Deep UPI Payments:** Integration with installed UPI apps (GPay, PhonePe, Paytm, BHIM) pre-filling VPA and amount.
- **Automated WorkManager Reminders:** `LendingReminderScheduler` and `LendingReminderWorker` notifying users of approaching or overdue debts.
- **Transaction Bridge Dialog (`LendingTransactionBridgeDialog.kt`):** Bridge any bank/UPI transaction into a tracked Lend/Borrow entry with prefilled amount and payee.

---

### 2.5 Savings Goals Hub (`GoalsScreen.kt` & `SavingsGoalCalculator.kt`)
Target milestone savings tracker:
- **Hero Savings Overview:** Total saved across all milestones, total target amount, and aggregated progress percentage.
- **16 Custom Scalable Vector SVG Icons:** Target, Savings, Laptop, Car, Home, Travel, Shield, Education, Gift, Trophy, Investment, Star, Deposit, Withdraw, History, Calendar.
- **Goal Cards:**
  - SVG icon badge and milestone title.
  - Circular / linear progress gauge with percentage.
  - Target deadline countdown and days remaining.
  - Required savings pace (Daily & Monthly pace needed to reach goal on time).
  - Quick action buttons: "+ Deposit", "- Withdraw", "Details", "Edit", "Delete".
- **Contribution History Ledger (`SavingsGoalContributionEntity`, Room Migration `9_10`):**
  - Complete audit trail of all deposits and withdrawals with timestamps, notes, and source transaction tags.
- **Deposit & Withdraw Steppers Dialog (`GoalDialogs.kt`):**
  - Monetary steppers (`+₹500`, `+₹1000`, `+₹2000`, `+₹5000`, `+₹10000`), custom amount field, and note.
- **Income Transaction Attribution:**
  - Direct toggle in `CategoryPickerSheet`, `AddTransactionDialog`, and `NeedsReviewSheet` to allocate incoming funds directly to an active goal.
- **Filter Tabs & Sorting:** In Progress, Completed, All; sorted by Recently Added, Highest Progress, Closest Deadline, or Target Amount.

---

### 2.6 Full-Page Transaction Detail Inspector (`TransactionDetailScreen.kt`)
Comprehensive inspector and modifier for any financial transaction:
- **Hero Amount Display:** Dual-tone styled amount with large typography and +/- indicator.
- **Editable Metadata Fields:**
  - Counterparty / merchant name.
  - Category selector with vector SVG icons.
  - Date and time pickers.
  - Payment source selector (UPI, Card, ATM, Bank Transfer, Cash).
  - Bank institution name.
  - Reference number.
  - Multi-line notes.
- **Raw SMS Payload Inspector:** Displays the original raw SMS body with one-tap copy-to-clipboard.
- **Review Resolution:** Accept or dismiss "Needs Review" flag.
- **Cross-Module Linkage:**
  - Convert/link to Lend & Borrow entry.
  - Allocate to Savings Goal (for income transactions).
- **Deletion:** Confirmation dialog with atomic removal and balance recalculation.

---

### 2.7 Search & Transaction History Engine (`SearchScreen.kt`)
Zero-latency full-text search and multi-dimensional filtering:
- **Instant Search:** Matches payee, bank, reference number, notes, and category name.
- **Filter Sheet (`SearchFilterSheet.kt`):**
  - Transaction Type: All, Debit (Spent), Credit (Received).
  - Date Presets: All Time, Today, This Week, This Month, Last 30 Days, This Year, Custom Date Range.
  - Amount Presets: All, Under ₹500, ₹500–₹2,000, ₹2,000–₹10,000, Over ₹10,000, Custom Range.
  - Category Multi-Select: Filter across any combination of categories.
  - Source Multi-Select: Filter across UPI, Card, ATM, Bank Transfer, and Cash.
  - Sort Options: Newest First, Oldest First, Highest Amount, Lowest Amount.
- **Summary Metrics Bar:** Dynamic real-time calculation of Total Spent, Total Received, and Net Amount for the filtered dataset.
- **Bulk Selection & Export:** Select multiple transactions and export directly to PDF or CSV.

---

### 2.8 User Profile & Account Center (`ProfileScreen.kt` & `UserProfilePreferences.kt`)
Central user identity and system preferences hub:
- **Profile Avatar:** Camera capture or gallery image picker with local storage persistence (`kanri_profile_avatar.jpg`), or fallback to user initial.
- **User Details:** Editable user name and date of birth with DatePickerDialog.
- **Navigation Shortcuts:** Direct links to Settings, Statements Export, Recurring Spends, Database Backup/Restore, and Factory Reset.

---

### 2.9 Settings Hub (`SettingsScreen.kt`)
Consolidated preferences and administrative tools:
- **Cloud Sync (`CloudSyncCard.kt`):** Optional Google Drive AppData backup integration with end-to-end client-side encryption (Tink AES-GCM + PBKDF2), automated daily background sync, account switcher, manual restore modal, and one-tap offline-only fallback.
- **Automations & Rules:** Direct entry point to Smart Merchant Rule Editor (`SmartMerchantRulesScreen.kt`) displaying real-time active rule count badge.
- **Security & Privacy:** Biometric lock toggle, auto-lock timeout (Immediate, 30s, 1m, 5m, 15m), native device credentials, and in-app Privacy Policy dialog (`PrivacyPolicyDialog.kt`).
- **Quick Settings Tile:** Tile status indicator and configuration shortcut for Android notification shade quick-add access.
- **Data Protection:** Password-protected encrypted JSON backup export and SAF restoration.
- **Scheduled Statement Worker:** Configure automated weekly or monthly PDF/CSV exports via WorkManager.
- **Recurring Payments:** Manage auto-detected and manual subscription charges.
- **Detection Preferences:** Status and configuration for SMS Receiver, Notification Listener Service, and System Overlay Permissions.
- **Diagnostics Dashboard (`DiagnosticsSheet.kt`):** Real-time permission health check and system service status.
- **Appearance & Theme:** AMOLED Dark, Standard Dark, and Cream Light; independent launcher icon selection.
- **Local Crash Logs (`CrashLogsDialog.kt`):** In-app crash log reader with copy and share.
- **Factory Reset (`FactoryResetManager.kt`):** Typed confirmation ("DELETE") sanitization.

---

### 2.10 Background Detection & Notification Services
- **SMS Receiver (`SmsReceiver.kt`):**
  - Priority 999 intent filter ensuring Kanri processes bank SMS before other apps.
  - `goAsync()` coroutine scope for reliable background processing within BroadcastReceiver time limits.
  - Multi-part PDU concatenation.
  - Regex patterns for 15+ Indian banks (`BankSmsPatterns.kt`).
  - Reference number deduplication.
  - Reversals, refunds, and declined payment handling.
- **Notification Listener Service (`KanriNotificationListenerService.kt`):**
  - Captures payment notifications from Google Pay, PhonePe, Paytm, Amazon Pay, FamPay, BHIM.
  - Cross-channel 5-minute sliding window deduplication with SMS alerts (`NotificationDeduplicationHelper.kt`).
  - Cross-channel metadata enrichment (merging counterparty, bank, and reference data from both sources).
- **Notification Listener Watchdog (`NotificationListenerWatchdogWorker.kt`):**
  - 15-minute periodic WorkManager health check.
  - Detects silently disconnected `NotificationListenerService` (common on MIUI, ColorOS, FunTouchOS).
  - Automatic rebind trigger via `KanriNotificationListenerService.rebindService()` without user intervention.
- **Boot Receiver (`BootReceiver.kt`):**
  - Manifest-registered `BOOT_COMPLETED` listener.
  - Re-initializes all background monitors after device reboot: notification watchdog, widget midnight reset, lending reminder scheduler, daily reminder scheduler.
  - Refreshes all active home screen widgets immediately.
- **Daily Reminder Engine (`DailyReminderScheduler.kt`, `DailyReminderWorker.kt`):**
  - WorkManager periodic worker scheduled around 9 PM daily.
  - Checks for uncategorized transactions and nudges user to categorize.
  - Detects zero-transaction days and prompts for missed expense/income logging.
  - Priority-based notification dispatch (uncategorized > no-transaction > lending dues).
- **Instant Popup Service (`InstantPopupService.kt`):**
  - Foreground service (`specialUse`) rendering a system overlay card (`TYPE_APPLICATION_OVERLAY`) upon payment detection.
  - 8-second auto-dismiss with countdown ring.
  - 1-tap category assignment without opening the app.
  - Heads-up notification fallback if overlay permission is missing.
- **Battery Optimization Exemption (`BatteryOptimizationHelper.kt`):**
  - Guided dialog requesting `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` exemption.
  - Prevents Android Doze and OEM battery savers from silently killing background services.

---

### 2.11 Smart Merchant Rules Hub (`SmartMerchantRulesScreen.kt`)
Intelligent keyword auto-categorization and simulation suite:
- **Interactive Rule Simulator / Match Tester:** Live test card where users type arbitrary merchant names or SMS strings to immediately see which saved rule matches and what category will be applied.
- **Smart Matching Algorithm:** SQLite substring matching with longest-match precedence (`findSmartRuleForCounterparty`) prioritizing specific keywords over generic ones.
- **Starter Suggestion Chips:** Pre-populated 1-tap chips for frequent merchants (Swiggy, Zomato, Starbucks, Uber, Ola, Netflix, Amazon, Blinkit, Zepto, Dunzo).
- **Retroactive Categorization Engine:** 1-tap "Apply to past transactions" executing atomic Room update (`applyCategoryToMatchingTransactions`) across all historical records.
- **Rule Management:** Add, edit, delete, and filter active rules with visual category badges and match count diagnostics.

---

### 2.12 Glanceable Home Screen Widgets Architecture (7 Dedicated Widgets)
Specialized Android AppWidget suite with dynamic RemoteViews Canvas rendering:
1. **Money Spent Today Widget (`MoneySpentTodayWidgetProvider`):** Today's spend total, transaction count badge, and instant '+' quick-add action.
2. **Budget Progress Widget (`BudgetProgressWidgetProvider`):** Linear budget progress bar, remaining daily allowance, and month pace indicator.
3. **Budget Ring Widget (`BudgetRingWidgetProvider`):** Custom Canvas antialiased circular donut ring showing budget consumption percentage, safe spend, and days left.
4. **Goals Ring Widget (`GoalsRingWidgetProvider`):** Circular Canvas progress ring tracking top savings goal with target milestone and accumulated amount.
5. **Lend & Borrow Widget (`LendBorrowWidgetProvider`):** Glanceable overview of net peer-to-peer balance (to collect vs to pay) and overdue debt badges.
6. **Split Bill Widget (`SplitBillWidgetProvider`):** 1-tap access to group bill split calculator with participant splits.
7. **Quick Add Widget (`QuickAddWidgetProvider`):** Floating action bar for instant Expense, Income, and Split Bill logging.
- **Centralized Event Bus (`KanriWidgetsUpdater.kt`):** Automated refresh across all active widgets triggered whenever transactions, budgets, or goals are created, updated, or deleted.
- **Canvas Renderer (`WidgetCanvasRenderer.kt`):** Off-screen antialiased circular gauge drawing generating crisp Bitmaps for RemoteViews display.
- **Midnight Auto-Reset (`MidnightWidgetResetWorker.kt`):** WorkManager periodic worker firing 5 seconds past midnight to refresh all widgets, ensuring daily counters ("Money Spent Today") reset cleanly.

---

### 2.13 Android Quick Settings Tile (`QuickAddTileService.kt`)
Native Android notification shade pull-down tile:
- **Quick Logging:** 1-tap tile ("Quick Add" / "Log Expense") opening the transaction creation dialog from anywhere in the OS.
- **Android 14+ Support:** Uses `startActivityAndCollapse(PendingIntent)` on API 34+ and fallback collapse methods on older Android releases.
- **Lock Screen Handler:** Automatically prompts for device unlock (`unlockAndRun`) before showing the dialog if triggered while device is locked.

---

### 2.14 Onboarding Experience (`OnboardingScreen.kt` & `GrainGradientBackground.kt`)
Luxury 5-step onboarding walkthrough:
- **Mesh Gradient Engine (`GrainGradientBackground.kt`):** Custom Canvas shader rendering continuous animated multi-point color gradient combined with film-grain noise.
- **Step 1 — Welcome & Philosophy:** Luxury monochrome wordmark, privacy manifesto, and 100% offline commitment.
- **Step 2 — Detection Power:** How SMS and notification detection work locally without cloud ingestion.
- **Step 3 — Mode Selection:** Strict Offline Mode vs Optional Encrypted Google Drive AppData Sync.
- **Step 4 — Permissions:** In-context explanations for SMS and Notification listener permissions with respectful skip options.
- **Step 5 — Personalization:** User name, optional profile avatar, currency display, and initial monthly budget target.

---

### 2.15 Optional Encrypted Cloud Backup Engine (`GoogleDriveBackupManager.kt`)
Zero-knowledge, client-side encrypted backup synchronization:
- **Private Drive AppData:** Operates strictly within Google Drive's hidden `appDataFolder` (`drive.appdata` scope) — invisible in standard Drive UI and inaccessible to other apps.
- **Client-Side Cryptography:** Payloads are encrypted with AES-GCM (Tink PBKDF2) using the user's master key before network transit.
- **Automated WorkManager Sync:** Periodic background worker performing scheduled backups when connected to unmetered Wi-Fi.
- **Instant Fallback:** Users can switch back to 100% offline mode at any time with a single toggle.

---

## 3. Active Database Architecture (Room Version 10) 🗄️

Kanri uses **Room 2.8.5** secured with **SQLCipher 4.6.1** AES-256 encryption at rest. The database passphrase is automatically generated and protected inside the **Android Keystore** (`DatabaseKeyManager.kt`).

### 3.1 Entity Schemas

#### 1. `transactions`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INTEGER (PK) | No | Auto | Primary key |
| `type` | TEXT | No | — | "DEBIT" or "CREDIT" |
| `amount` | REAL | No | — | Transaction amount |
| `source_type` | TEXT | No | — | "UPI", "ATM", "CARD", "BANK_TRANSFER", "CASH", "UNKNOWN" |
| `counterparty` | TEXT | Yes | NULL | VPA, merchant name, account number, or ATM TID |
| `display_name` | TEXT | Yes | NULL | Normalized display merchant name |
| `bank` | TEXT | Yes | NULL | Bank institution name (e.g. "HDFC", "SBI", "KOTAK") |
| `ref_no` | TEXT | Yes | NULL | Bank reference or UPI UTR number (used for deduplication) |
| `timestamp` | INTEGER | No | — | Epoch milliseconds |
| `category_id` | INTEGER | Yes | NULL | Foreign key reference to `categories.id` |
| `raw_sms` | TEXT | No | — | Original SMS body or notification text |
| `is_duplicate` | INTEGER | No | 0 | Flag indicating duplicate alert suppression |
| `is_manual_entry` | INTEGER | No | 0 | Flag indicating manual entry |
| `needs_review` | INTEGER | No | 0 | Flag indicating transaction requires user verification |
| `review_reason` | TEXT | Yes | NULL | Reason for review (e.g. "Unknown payee", "Zero amount") |
| `notes` | TEXT | Yes | NULL | User-provided notes |

#### 2. `categories`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INTEGER (PK) | No | Auto | Primary key |
| `name` | TEXT | No | — | Category name (e.g. "Food & Dining", "Shopping") |
| `color_hex` | TEXT | No | — | Hex color code |
| `icon_name` | TEXT | No | — | Vector SVG icon key |
| `is_custom` | INTEGER | No | 0 | Flag indicating user-created custom category |

#### 3. `counterparty_category_map`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `counterparty` | TEXT (PK) | No | — | Merchant / counterparty identifier |
| `category_id` | INTEGER | No | — | Mapped category ID for automated classification |

#### 4. `budgets`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `month_key` | TEXT (PK) | No | — | Month identifier in `yyyy-MM` format (e.g. "2026-09") |
| `monthly_limit` | REAL | No | — | Budget spending limit for that month |

#### 5. `lending_records`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INTEGER (PK) | No | Auto | Primary key |
| `person_name` | TEXT | No | — | Contact / counterparty name |
| `amount` | REAL | No | — | Current remaining balance |
| `type` | TEXT | No | — | "LENT" (I lent to someone) or "BORROWED" (Someone lent to me) |
| `date` | INTEGER | No | Current | Date record was created (Epoch ms) |
| `due_date` | INTEGER | Yes | NULL | Agreed return date deadline (Epoch ms) |
| `is_settled` | INTEGER | No | 0 | Flag indicating debt is fully settled |
| `notes` | TEXT | Yes | NULL | Optional notes or purpose of loan |
| `linked_transaction_id`| INTEGER | Yes | NULL | ID of source transaction bridged into this record |
| `original_amount` | REAL | Yes | NULL | Original total principal amount before partial repayments |

#### 6. `lending_repayments`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INTEGER (PK) | No | Auto | Primary key |
| `lending_id` | INTEGER | No | — | Foreign key cascade linking to `lending_records.id` |
| `amount` | REAL | No | — | Repayment installment amount |
| `paid_at` | INTEGER | No | Current | Repayment timestamp (Epoch ms) |
| `note` | TEXT | Yes | NULL | Optional repayment note |

*Index: `index_lending_repayments_lending_id` on `lending_id`.*

#### 7. `recurring_payments`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INTEGER (PK) | No | Auto | Primary key |
| `title` | TEXT | No | — | Subscription name (e.g. "Netflix", "Rent") |
| `amount` | REAL | No | — | Recurring amount |
| `billing_cycle` | TEXT | No | "MONTHLY" | "MONTHLY", "YEARLY", "WEEKLY" |
| `category_id` | INTEGER | Yes | NULL | Linked category ID |
| `counterparty` | TEXT | Yes | NULL | Matched merchant handle |
| `next_due_timestamp` | INTEGER | No | — | Next billing timestamp (Epoch ms) |
| `auto_detect_keyword`| TEXT | Yes | NULL | Keyword used for automatic matching |
| `is_active` | INTEGER | No | 1 | Active tracking status |
| `last_paid_timestamp`| INTEGER | Yes | NULL | Timestamp of last recorded payment |

#### 8. `savings_goals`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INTEGER (PK) | No | Auto | Primary key |
| `title` | TEXT | No | — | Goal title (e.g. "Emergency Fund", "New Laptop") |
| `target_amount` | REAL | No | — | Target milestone savings amount |
| `current_amount` | REAL | No | 0.0 | Currently accumulated savings amount |
| `target_date` | INTEGER | Yes | NULL | Optional target deadline (Epoch ms) |
| `color_hex` | TEXT | No | "#06D6A0"| Display color |
| `emoji` | TEXT | No | "🎯" | Vector SVG icon key or legacy emoji |
| `is_completed` | INTEGER | No | 0 | Completion status |

#### 9. `savings_goal_contributions`
| Column | Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INTEGER (PK) | No | Auto | Primary key |
| `goal_id` | INTEGER | No | — | Foreign key cascade linking to `savings_goals.id` |
| `amount` | REAL | No | — | Contribution amount (+ for deposit, - for withdrawal) |
| `timestamp` | INTEGER | No | Current | Timestamp of contribution (Epoch ms) |
| `note` | TEXT | Yes | NULL | Optional contribution note |
| `source_transaction_id`| INTEGER | Yes | NULL | Linked income transaction ID (if attributed) |

*Index: `index_savings_goal_contributions_goal_id` on `goal_id`.*

---

### 3.2 Migration History

- **`MIGRATION_5_6`:** Added `needs_review` (`INTEGER NOT NULL DEFAULT 0`) and `review_reason` (`TEXT DEFAULT NULL`) to `transactions`.
- **`MIGRATION_6_7`:** Added `notes` (`TEXT DEFAULT NULL`) to `transactions`.
- **`MIGRATION_7_8`:** Added `is_custom` (`INTEGER NOT NULL DEFAULT 0`) to `categories`.
- **`MIGRATION_8_9`:** Added `original_amount` (`REAL DEFAULT NULL`) to `lending_records`. Created `lending_repayments` table with cascade foreign key and index on `lending_id`.
- **`MIGRATION_9_10`:** Created `savings_goal_contributions` table with cascade foreign key and index on `goal_id`.

---

## 4. Comprehensive Test Suite (36 Test Suites) 🧪

The repository maintains **36 unit and integration test suites** (232+ individual tests) verifying financial accuracy, cryptographic integrity, background services, deduplication logic, and UI state:

1. **`DeltaCalculatorTest.kt`** — Validates month-over-month spend delta calculations, zero spending baselines, and percentage bounds.
2. **`FinancialHealthCalculatorTest.kt`** — Validates the 4 financial health pillars (Savings Rate, Budget Utilization, Spend Velocity, Debt Burden), edge cases (zero income, over-budget), letter grade assignments, and diagnostic feedback generation.
3. **`StreakCalculatorTest.kt`** — Tests calculation of consecutive no-spend days, timezone boundaries, same-day multiple spend events, and milestone celebration logic.
4. **`BackupCryptoTest.kt`** — Verifies AES-GCM encryption/decryption of backup JSON payloads, PBKDF2 key derivation, wrong password rejection, and payload tampering detection.
5. **`BackupJsonParserTest.kt`** — Tests full JSON serialization and deserialization across all entities (transactions, categories, budgets, lending, goals, contributions).
6. **`LendingBackupTest.kt`** — Validates backup/restore integrity specifically for lending records, partial repayments, and foreign key relationships.
7. **`BudgetCalculatorTest.kt`** — Tests safe-to-spend allowance per day calculations, days remaining countdowns, and over-budget transition thresholds.
8. **`CloudBackupTest.kt`** — Tests optional encrypted Google Drive backup serialization, AES-GCM Tink envelope encryption, and local fallback states.
9. **`DriveSyncTest.kt`** — Tests Google Drive REST v3 AppData folder synchronization, conflict detection, and network error recovery.
10. **`GoogleAuthTest.kt`** — Validates Google Sign-In intent handling, token acquisition, and permission scope boundaries (`drive.appdata`).
11. **`CrashLoggerTest.kt`** — Validates local disk crash file writing, exception stack formatting, file pruning, and sensitive data redaction.
12. **`DatabaseMigrationTest.kt`** — Verifies Room migrations 5$\rightarrow$6, 6$\rightarrow$7, 7$\rightarrow$8, 8$\rightarrow$9, and 9$\rightarrow$10 under SQLite and SQLCipher.
13. **`MerchantRulesTest.kt`** — Validates smart keyword substring matching, case insensitivity, longest-match priority, retroactive Room update contracts, and tile action contracts.
14. **`CsvExporterTest.kt`** — Verifies RFC 4180 CSV generation, column headers, currency formatting, multiline note escaping, and special character sanitization.
15. **`ExportSchedulerTest.kt`** — Tests WorkManager export job scheduling, recurring intervals, and constraint validation.
16. **`LendingMoneyRulesTest.kt`** — Validates financial calculations for lent vs borrowed balances, net debt position, partial repayments, and overdue status rules.
17. **`LendingTransactionSyncTest.kt`** — Verifies bridging standard bank transactions into lending records without data corruption or duplicate ledger entries.
18. **`NotificationParserTest.kt`** — Tests payment notification extraction across Google Pay, PhonePe, Paytm, Amazon Pay, and FamPay push notifications.
19. **`SmsParserTest.kt`** — Comprehensive regex parser test verifying 50+ real-world SMS alerts across 15+ Indian banks, debit/credit detection, and reference number extraction.
20. **`FactoryResetTest.kt`** — Verifies atomic database wipe, preferences reset, encryption key rotation, and clean state restoration.
21. **`CategoryDetailTest.kt`** — Tests 6-month category aggregation, merchant ranking calculations, and monthly spend trajectories.
22. **`CategoryIconTest.kt`** — Validates SVG icon mapping, fallback logic, and theme tint resolution across all categories.
23. **`GreetingResolverTest.kt`** — Validates greeting time boundaries (Morning to Late Night), seed determinism, 11 prioritized financial subtitle rules, and anti-repetition guards.
24. **`HealthCheckTest.kt`** — Validates permission inspection logic (SMS, Notification Listener, System Overlay, Battery Optimization).
25. **`InsightsCalculatorsTest.kt`** — Validates mathematical calculators for cumulative spend lines, weekend vs weekday splits, latte factor micro-transactions, and projection models.
26. **`InsightsPeriodsTest.kt`** — Tests date range math for This Month, Last Month, 30 Days, 90 Days, This Year, and Custom Range presets.
27. **`LendingHubViewModelTest.kt`** — Tests `LendingHubViewModel` StateFlow emissions, person aggregation, repayment processing, overdue calculations, and settle-all actions.
28. **`SplitExpenseLogicTest.kt`** — Tests group bill splitting algorithms (equal split, custom participant counts, remainder cent/paisa distribution).
29. **`AllTimeTrendsTest.kt`** — Tests multi-year financial trends aggregation, monthly data point generation, and average burn calculations.
30. **`InstantPopupTest.kt`** — Tests overlay popup creation, 8-second auto-dismiss timeout, and 1-tap category assignment dispatch.
31. **`RecurringPaymentTest.kt`** — Validates subscription auto-detection rules, billing cycle calculations, and upcoming due date countdowns.
32. **`SavingsGoalsTest.kt`** — Tests `SavingsGoalCalculator` daily and monthly savings pace formulas, progress percentages, and contribution logging.
33. **`SearchFilterTest.kt`** — Validates multi-criteria search filtering across text, dates, amounts, categories, and payment sources.
34. **`WidgetBreakpointTest.kt`** — Verifies RemoteViews layout selection across Small (1x1), Medium (2x2), Large (4x2+), and all 7 dedicated widget providers.
35. **`DeduplicationLogicTest.kt`** — Validates the cross-channel 5-minute sliding window deduplication engine, two-way SMS/notification matching, enrichment merging, duplicate suppression, and edge cases around timing boundaries.
36. **`ExampleUnitTest.kt`** — Base environment validation test.

---

## 5. Architectural & Privacy Principles (What Kanri Deliberately Does NOT Do) 🛡️

- **100% Offline & Zero Cloud Leakage:** Kanri does not have a backend server, analytics telemetry, or remote storage. All transaction data, notes, and profile details remain encrypted on the local device.
- **No Ads, Tracking, or Data Monetization:** Zero third-party ad SDKs, trackers, or behavioral profiling.
- **No Predatory Credit or Loan Offers:** Unlike commercial expense apps, Kanri contains no personal loan marketing, credit score queries, or insurance cross-selling.
- **No Banking Credentials Required:** Kanri never asks for net banking passwords, UPI PINs, or bank account logins. Detection operates solely on SMS alerts and system notifications.
- **Strict User Sovereignty:** Data can be exported to standard CSV or PDF at any time, backed up to an encrypted file, or wiped permanently via typed Factory Reset.

---

## 6. Forward Engineering Roadmap & Upcoming Horizons 🗺️

### Phase 1: Core Experience Refinements
- [x] **Custom Category Management:** Allow users to create custom categories with custom SVG icons and colors directly in `CategoryPickerSheet` and `SettingsScreen`.
- [ ] **Multi-Currency Support:** Support international currency symbols ($, €, £, ¥, AED) with user-selectable locale formatting in settings.
- [x] **Quick Add Tiles in Android Quick Settings:** Add a Quick Settings tile in the Android notification shade to trigger manual expense or income logging instantly from anywhere in the OS.

### Phase 2: Intelligence & Advanced Automations
- [x] **Smart Merchant Rule Editor:** Expose a screen in Settings where users can view and edit custom keyword auto-categorization rules (e.g. "If merchant contains 'Starbucks', categorize as 'Food & Dining'").
- [x] **Glanceable Multi-Widget Suite (7 Dedicated Widgets):** Replaced legacy widget with 7 dedicated Android AppWidgets for Today's Spend, Budget Progress, Budget Ring, Goals Ring, Lend/Borrow, Split Bill, and Quick Add.
- [x] **Optional Encrypted Google Drive AppData Backup:** 100% private, client-side encrypted backup sync via Google Drive hidden AppData folder with automated background sync.
- [ ] **Predictive Cash Flow Forecasting:** Expand the Month-End Projection model into a 30-day forward-looking cash flow forecast that accounts for upcoming recurring subscriptions and expected debt repayments.
- [ ] **Bill Splitting via QR Code / Local Share:** Generate offline QR codes or use Android Nearby Share to transfer bill split breakdowns directly to other Kanri users without cloud intermediation.

### Phase 3: Hardware & Ecosystem Integrations
- [ ] **Wear OS Companion App:** Glanceable today's spend counter, safe-to-spend allowance, and voice/numpad quick transaction entry on Wear OS smartwatches.
- [ ] **Android 15 Edge-to-Edge Compliance & Predictive Back:** Deepen predictive back gesture animations across all sub-screens and dialogs.
- [ ] **Automated Bi-Directional Backup Sync via SAF Local Provider:** Support user-configured local folder auto-sync (e.g. syncing encrypted backups to a local Syncthing or SD card folder automatically upon change).
