# Kanri App - Complete QA Inventory

## 1. Architectural Overview

- **Modules:** Single-module Android application (`:app`) targeting Android 14/15/16 (compileSdk/targetSdk 37, minSdk 26).
- **UI Framework:** Jetpack Compose with Material 3 + Custom Apple-fluid and Kokonut UI component system.
- **Navigation:** Single-activity architecture (`MainActivity` with launcher aliases for dynamic app icons & splash backgrounds). Compose state-driven navigation with hierarchical depth transitions (`KanriMotion`) and predictive back handlers.
- **State Management:** MVVM architecture with Kotlin Coroutines `StateFlow` and Compose `collectAsState()`. ViewModels: `HomeViewModel`, `InsightsViewModel`, `LendingViewModel`, `SearchViewModel`, `CategoryDetailViewModel`.
- **Data & Persistence Layer:**
  - Room DB with SQLCipher encryption (`KanriDatabase`, 256-bit passphrase managed securely by `DatabaseKeyManager` via Android Keystore).
  - SharedPreferences and DataStore for user preferences (`ThemePreferences`, `OnboardingPreferences`, `AppUpdatePreferences`, `SecurityPreferences`).
  - Google Drive REST API for encrypted cloud backups.
- **Theme System:**
  - Central theme engine: `KanriTheme` (`Theme.kt`).
  - Color Tokens: `Color.kt`, `KanriGlassTokens.kt`.
  - Font System: `Type.kt` using Google Sans Flex variable font (`GoogleSansFlex`) and Panchang display font (`Panchang`).
  - Motion tokens: `KanriMotion.kt` with Apple fluid springs (`StiffSpring`, `SnappySpring`, `GentleSpring`, `SmoothSpring`, `BouncySpring`).
  - Blur & Glass System: Integrated Haze library (`HazeState`, `hazeSource`, `KanriGlassCard`) with custom live WebGL shaders (`GlassShaderBackground`), micro-grain procedural shader, and user-customizable glass physics (blur radius, opacity, tints, border width).
  - Status & Navigation bar handling: `WindowCompat.getInsetsController` syncing appearance light/dark flags dynamically with theme mode.

---

## 2. Supported Themes & Theme Variants

| Theme Identifier | Base Color Scheme | System Chrome | Glass / Surface Characteristics |
|---|---|---|---|
| **ThemeMode.DARK** | Minimalist Dark Charcoal (`#121212`) | Dark (Light text/icons) | Solid charcoal cards, subtle border outlines (`#2A2A2A`), high contrast. |
| **ThemeMode.AMOLED** | Pure True Black (`#000000`) | Dark (Light text/icons) | Pure black background (`#000000`), minimal battery consumption, sharp borders. |
| **ThemeMode.LIGHT** | Monochrome Soft Cream (`#FEFAEC`) | Light (Dark text/icons) | Warm cream background, deep charcoal text (`#1F1E1A`), soft borders. |
| **ThemeMode.GLASS** | Glass Translucent with live backdrops | Adaptive to live backdrop | Transparent surfaces, Haze blur, frosted glass cards (`KanriGlassTokens`), specular borders. |
| **ThemeMode.SYSTEM** | Auto-switches between Light & Dark | Follows system day/night | Follows Android system night mode setting. |
| **Dynamic Color** | Material You wallpaper-extracted | Follows system colors | Available on Android 12+ (API 31+); adapts Dark or Light scheme from system wallpaper. |

### Live Glass Shaders Supported (8 Variants):
1. **Gradient Aura (`aurora`)**: Flowing multi-color ambient auroras.
2. **Grain Gradient (`grain_gradient`)**: Deep monochromatic textural gradient.
3. **Hero Geometric (`hero_geometric`)**: Deep blue geometric ambient field.
4. **Prism Swirl (`prism_gradient`)**: Prismatic light diffraction.
5. **Cloudscape (`cloud_shader`)**: Ethereal volumetric cloud motion.
6. **Ink Ridges (`ink_ridges`)**: Fluid topographic monochrome waves.
7. **Fuji & Sakura (`fuji_sakura`)**: Japanese aesthetic cherry blossom gradients.
8. **Sumi-e Mist (`sumi_e_mist`)**: Traditional Japanese ink-wash atmospheric shader.

---

## 3. Screen Inventory

### Primary Navigation Destinations
1. **Home Screen (`HomeScreen.kt`)**: Greeting header, financial health summary card, cash flow balance overview, quick action buttons, spending visualizer, recent transactions list.
2. **Insights Screen (`InsightsViewModel.kt`, `sections/`)**: Comprehensive analytics dashboard including Month Summary Strip, Budget vs Actual, Biggest Transactions, Category Movers, Calendar Heatmap, Year Heatmap, Cumulative Spend Line, Top Payees, Income Sources, Payment Method Split, Weekend vs Weekday, Month-End Projections, Personal Records, Small Spends, and Suggested Budgets.
3. **Lend & Borrow Screen (`LendingScreen.kt`)**: Net balance banner, filter tabs (All, Lent, Borrowed), person summary cards, transaction timelines, repayment recording, reminder scheduler.
4. **Goals Screen (`GoalsScreen.kt`)**: Savings goals cards with circular progress gauges (`CircularGoalProgressGauge`), target tracking, milestone alerts.

### Sub-screens & Dedicated Destinations
5. **Month View Screen (`MonthViewScreen.kt`)**: Monthly calendar grid, daily expense badges, selected day transaction breakdown.
6. **Profile Screen (`ProfileScreen.kt`)**: Procedural avatar display, avatar customizer launcher, monthly budget limit, stats overview, currency selector, export shortcuts.
7. **Settings Screen (`SettingsScreen.kt`)**: Comprehensive configuration hub for themes, live backgrounds, notifications, security, smart categorizer rules, diagnostics, data reset, and about.
8. **Transaction Detail Screen (`TransactionDetailScreen.kt`)**: Complete transaction inspection, note editing, category reassignment, deletion, and split bill actions.
9. **Category Detail Screen (`CategoryDetailScreen.kt`)**: Specific category history, monthly spending trends line chart (`CategoryTrendLineChart`), average transaction size, and transaction list.
10. **Compare Months Screen (`CompareMonthsScreen.kt`)**: Side-by-side comparative analysis of income, expenses, and category variance between any two selected months.
11. **Monthly Recap Screen (`MonthlyRecapScreen.kt`)**: Visualized month recap card (`MonthlyRecapCard`) with high-level spending narrative, top categories, and savings performance.
12. **Payee Detail Screen (`PayeeDetailScreen.kt`)**: Per-merchant financial history, total spent, transaction frequency, and category affiliations.
13. **Themes & Live Backgrounds Screen (`ThemesCustomizationScreen.kt`)**: Theme mode selector (System, Dark, AMOLED, Light, Glass), 8 Live Shader preview grid, Dynamic Colors toggle.
14. **Glass Customizer Screen (`GlassCustomizerScreen.kt`)**: Precision glass physics tuner (blur radius, opacity, tint lightness, border width, corner radius, shadow intensity).
15. **Smart Merchant Rules Screen (`SmartMerchantRulesScreen.kt`)**: Merchant auto-categorization keyword rule creation and priority management.
16. **Onboarding Screen (`OnboardingScreen.kt`)**: Animated introductory walkthrough (`FoldText`), currency choice, initial budget, and SMS read permissions.
17. **Lock Screen (`LockScreen.kt`)**: Biometric prompt and PIN entry pad with security lockout timers.

### Dialogs & Bottom Sheets
18. **Add Transaction Dialog (`AddTransactionDialog.kt` / `AddExpenseIncomePopup.kt`)**: Kokonut Amount Pad, category selector, wallet selector, slide-to-confirm action.
19. **Category Picker Sheet (`CategoryPickerSheet.kt`)**: Full category grid with icons and custom color badges.
20. **Add Lending Dialog (`AddLendingDialog.kt` / `LendingEntryDialog.kt`)**: Create lent/borrowed debt record with contact picker, due date, and amount pad.
21. **Split Expense Sheet (`SplitExpenseSheet.kt`)**: Bill splitting calculator across multiple people with unequal shares.
22. **Lending Detail Sheet**: Timeline of repayments, add repayment dialog, settle debt action.
23. **Goal Dialogs (`GoalDialogs.kt`)**: Create new savings goal, add deposit/contribution dialog, withdraw from goal.
24. **Savings Goals Sheet (`SavingsGoalsSheet.kt`)**: Expanded savings roadmap and list.
25. **Recurring Payments Sheet (`RecurringPaymentsSheet.kt`)**: Subscriptions list, billing frequencies, next payment due dates, and projections.
26. **Wallet Sheet (`WalletSheet.kt`)**: Cash vs Online wallet account overview with balance audit.
27. **Wallet Setup Dialog (`WalletSetupDialog.kt`)**: Initial balance configuration for wallets.
28. **Move Money Dialog (`MoveMoneyDialog.kt`)**: Transfer funds between Cash and Online balances.
29. **Correct Balance Dialog (`CorrectBalanceDialog.kt`)**: Recalibrate tracked balance against physical wallet.
30. **Set Budget Dialog (`SetBudgetDialog.kt`)**: Monthly budget allocation modal.
31. **Search Filter Sheet (`SearchFilterSheet.kt`)**: Multi-attribute transaction query filters (type, date range, amount range, categories, wallets).
32. **Export Statement Sheet (`ExportStatementSheet.kt`)**: Custom date range selection, CSV and PDF format generators.
33. **Backup & Restore Sheet (`BackupRestoreSheet.kt`)**: Google Drive cloud backup, local JSON/encrypted backup, and import restore.
34. **Security Settings Sheet (`SecuritySettingsSheet.kt`)**: PIN configuration, biometric unlock toggle, auto-lock timeouts (immediate, 30s, 1m, 5m).
35. **Diagnostics Sheet (`DiagnosticsSheet.kt`)**: Health check diagnostics: SMS permissions, battery optimization whitelist, notification listener status.
36. **Crash Logs Dialog (`CrashLogsDialog.kt`)**: View recorded crash stack traces with one-click clipboard export.
37. **Privacy Policy Dialog (`PrivacyPolicyDialog.kt`)**: Built-in offline privacy terms viewer.
38. **App Update Dialog (`AppUpdateDialog.kt`)**: In-app APK update release notes and download installer.
39. **Instant Popup Sheet (`InstantPopupSheet.kt`)**: Floating system overlay window triggered on SMS payment detection.

### App Widgets (Home Screen)
40. **Budget Ring Widget (`BudgetRingWidgetProvider.kt`)**: Circular radial gauge showing monthly budget burn rate.
41. **Budget Progress Widget (`BudgetProgressWidgetProvider.kt`)**: Linear progress indicator of spend vs budget limit.
42. **Lend & Borrow Widget (`LendBorrowWidgetProvider.kt`)**: Summary of receivables and payables.
43. **Goals Ring Widget (`GoalsRingWidgetProvider.kt`)**: Visual savings ring progress.
44. **Split Bill Widget (`SplitBillWidgetProvider.kt`)**: Quick shortcut to split expenses on the go.
45. **Quick Add Widget (`QuickAddWidgetProvider.kt`)**: One-touch shortcut to log expense or income.
46. **Money Spent Today Widget (`MoneySpentTodayWidgetProvider.kt`)**: Real-time ticker of today's total outflow.
47. **Wallet Balance Widget (`WalletBalanceWidgetProvider.kt`)**: Aggregated cash and digital account balances.

### Background Tasks, Receivers, Services & Notifications
48. **SMS Transaction Receiver (`SmsReceiver.kt`)**: Listens for bank debit/credit SMS alerts (SBI, HDFC, ICICI, Axis, Paytm, GPay, etc.).
49. **Notification Listener Service (`KanriNotificationListenerService.kt`)**: Detects UPI and banking push notifications for instant logging.
50. **Instant Popup Service (`InstantPopupService.kt`)**: Foreground service displaying floating transaction prompt.
51. **Boot Receiver (`BootReceiver.kt`)**: Reschedules alarm managers and workers after device reboot.
52. **Quick Settings Tile (`QuickAddTileService.kt`)**: System pull-down quick settings tile for fast expense logging.
53. **Daily Reminder Worker (`DailyReminderWorker.kt`)**: Nightly push reminder notification to review expenses.
54. **Midnight Widget Reset Worker (`MidnightWidgetResetWorker.kt`)**: Refreshes widget canvas renderers at 00:00 daily.
55. **Scheduled Export Worker (`ScheduledExportWorker.kt`)**: Automates monthly statement generation.

---

## 4. Reusable Component Inventory

### Core UI Design System Components
- `KanriFloatingDock`: Apple-style floating navigation pill with elastic selection indicators and smooth springs.
- `KanriDockIcons`: Custom vector iconography designed for Home, Insights, Lend/Borrow, and Goals tabs.
- `KanriGlassCard`: Adaptable card rendering with either Haze blur (in Glass mode) or solid elevated surfaces (Dark/AMOLED/Light).
- `KanriGlassSurface`: Low-level translucent glass box with customizable border strokes and specular highlights.
- `KanriGlassButton`: Tactile push button supporting glass styling, press scaling, and themed haptic feedback.
- `KanriGlassTextField`: Styled text input field matching glass and solid themes.
- `CategoryIcon`: Universal category glyph container with customizable tint palettes and shapes.
- `AnimatedNumber`: Animated tabular currency numbers with rolling transition.
- `FlipFadeText`: Text switcher with vertical flip and alpha fade choreography.
- `SignatureText`: Custom branded typography renderer using Google Sans Flex and Panchang.
- `KanriWobbleLoader`: Minimalist custom 3-dot undulating loading indicator.

### Kokonut Component Suite (`ui/common/kokonut/`)
- `KokonutAmountPad`: Comprehensive tactile number pad with zero-start logic, backspace, and quick presets.
- `KokonutSlideToConfirm`: Interactive spring-damped slider with velocity-sensitive threshold confirmation.
- `KokonutSmoothDrawer`: Physics-based draggable bottom drawer.
- `KokonutAvatarPicker`: Procedural vector avatar editor with customizable skin, hair, expressions, and accessories.
- `KokonutSpendingRing`: Custom canvas polar coordinate spending distribution donut chart.
- `KokonutSpoilerText`: Tap-to-reveal obfuscated text with sparkling particle effect.
- `KokonutStackPanel`: Cascading deck of expandable cards.
- `KokonutTearButton`: Pull-down elastic tear interaction button.
