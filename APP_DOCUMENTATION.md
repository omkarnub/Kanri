# Kanri — Comprehensive Application Architecture & Technical Specification

> **Version**: 1.0.3-production  
> **Target SDK**: Android 15 (API 35) | **Min SDK**: Android 8.0 (API 26)  
> **Design Pattern**: MVVM + Clean Architecture + Unidirectional Data Flow (UDF)  
> **UI Engine**: 100% Jetpack Compose with Frosted Glassmorphism (Haze)  
> **Security Tier**: Zero-Telemetry, Offline-First, SQLCipher AES-256 GCM (Android Keystore)

---

## Table of Contents
1. [Executive Overview & Design Philosophy](#1-executive-overview--design-philosophy)
2. [Technology Stack & Core Dependencies](#2-technology-stack--core-dependencies)
3. [System Architecture & Ingestion Flowcharts](#3-system-architecture--ingestion-flowcharts)
4. [Complete Project File & Directory Map](#4-complete-project-file--directory-map)
5. [Room Database Architecture & Entity Schemas](#5-room-database-architecture--entity-schemas)
6. [Complete DAO Function & Query Reference](#6-complete-dao-function--query-reference)
7. [Automated Dual-Pipeline Ingestion Engine](#7-automated-dual-pipeline-ingestion-engine)
8. [Dual-Wallet Subsystem (Cash vs. Online)](#8-dual-wallet-subsystem-cash-vs-online)
9. [Budgeting, Analytics & Financial Intelligence Engines](#9-budgeting-analytics--financial-intelligence-engines)
10. [Lending & Debt Hub Subsystem](#10-lending--debt-hub-subsystem)
11. [Savings Goals & Recurring Subscriptions](#11-savings-goals--recurring-subscriptions)
12. [Cryptographic Security, Backups & Cloud Sync](#12-cryptographic-security-backups--cloud-sync)
13. [Document Export Engine (Vector PDF & CSV)](#13-document-export-engine-vector-pdf--csv)
14. [Comprehensive UI Screen & Component Catalog](#14-comprehensive-ui-screen--component-catalog)
15. [Home Screen Widgets Subsystem](#15-home-screen-widgets-subsystem)
16. [Background Workers, Services & Broadcast Receivers](#16-background-workers-services--broadcast-receivers)
17. [Security Hardening & Vulnerability Mitigation Matrix](#17-security-hardening--vulnerability-mitigation-matrix)
18. [Build, Installation, Verification & Troubleshooting](#18-build-installation-verification--troubleshooting)

---

## 1. Executive Overview & Design Philosophy

**Kanri** (Japanese: 管理, *governance / management*) is a sovereign, offline-first personal financial operating system natively engineered for Android. Unlike conventional expense trackers that transmit sensitive banking data to remote aggregation APIs (Plaid, Yodlee, MX) or proprietary server databases, Kanri functions entirely on-device under a zero-telemetry paradigm.

### Core Architectural Pillars
- **Zero-Knowledge Privacy**: No personal financial records, SMS bodies, counterparty names, account numbers, or debt ledgers ever leave the local hardware sandbox, except during explicit user-initiated encrypted exports.
- **Hardware-Backed Cryptographic Storage**: SQLite persistence is managed through SQLCipher with AES-256 GCM encryption. The encryption passphrase is generated via a cryptographically secure pseudo-random number generator (CSPRNG) and safeguarded inside the hardware-backed **Android Keystore System**.
- **Autonomous Dual-Stream Ingestion**: Financial records are automatically captured from both carrier-level SMS messages (telephony PDUs) and real-time payment notifications via an Android `NotificationListenerService`. A time-bounded deduplication engine unifies these streams into single, verified financial transactions.
- **Physical Liquidity Modeling (Dual Wallets)**: The app explicitly differentiates between physical cash in hand and digital online accounts (bank/UPI/card). ATM withdrawals are governed by a dedicated state machine that distinguishes pure cash consumption from balance transfers.
- **Tactile, Glassmorphic UI**: Built with Jetpack Compose, dynamic Material 3 color palettes, and a comprehensive frosted glass component library (`KanriGlass.kt`) using Haze GPU-accelerated blur. All dialogs, bottom sheets, popups, date pickers, and dropdown menus apply native Android 12+ `FLAG_BLUR_BEHIND` window blur with frosted glass card surfaces. A dedicated primary button color system (`kanriPrimaryButtonColors`) prevents white-on-white contrast glitches across all theme modes. Google Sans typography and physical vibration feedback through `KanriHaptics` complete the tactile experience.
- **Minimal Monochrome Home Screen Widgets**: Eight glanceable widgets provide balance visibility, budget gauges, and instant action shortcuts directly from the home screen, strictly adhering to RemoteViews compatibility across OEM operating systems (Samsung One UI, Google Pixel launcher, Xiaomi HyperOS, Oppo ColorOS, Realme UI, Infinix XOS).

---

## 2. Technology Stack & Core Dependencies

| Category | Component / Library | Implementation Details |
|---|---|---|
| **Language & Runtimes** | Kotlin 2.0.21, JVM 17 | Coroutines, Flow, StateFlow, Channels, Serialization |
| **Android Architecture** | Android Jetpack (AndroidX) | Lifecycle, ViewModel, Navigation Compose, WorkManager 2.9+, Security Crypto |
| **User Interface** | Jetpack Compose BOM 2024.10.00 | Material 3, Foundation, Animation, Vector Graphics |
| **Visual Styling** | Haze (dev.chrisbanes.haze) | GPU-accelerated blur & glassmorphic frosted surface shaders with three-tier glass levels (Blur, Flat, Strong). Native Android 12+ `FLAG_BLUR_BEHIND` window blur on all dialogs and sheets. |
| **Typography** | Google Sans (Custom TTF) | Google Sans Regular, Medium, and Bold |
| **Encrypted Database** | Room 2.6.1 + SQLCipher 4.5.5 | Zetetic SQLCipher open helper factory, hardware-backed Keystore key |
| **Key Management** | AndroidX Security Crypto 1.1.0-alpha06 | `MasterKey`, `EncryptedSharedPreferences` (AES256-SIV + AES256-GCM) |
| **Biometrics** | AndroidX Biometric 1.2.0-alpha05 | `BiometricPrompt` supporting Class 3 biometrics (Fingerprint/Face/PIN) |
| **Work Scheduling** | AndroidX WorkManager 2.9.1 | `CoroutineWorker`, periodic constraints, background watchdog workers |
| **Cloud Integration** | Google Drive REST API v3 | User-scoped OAuth token (`drive.file`), direct RFC-compliant multipart HTTP streaming |
| **Document Generation** | Native `android.graphics.pdf.PdfDocument` | Multi-page vector PDF Canvas layout; RFC 4180 CSV serializer |
| **System Integrations** | Android AppWidgets, Quick Settings Tile | RemoteViews, `TileService`, Dynamic App Shortcuts (`ShortcutManager`) |

---

## 3. System Architecture & Ingestion Flowcharts

### 3.1 High-Level Architectural Flow

```
                                  +-----------------------------------------------------+
                                  |                 ANDROID OPERATING SYSTEM            |
                                  |                                                     |
                                  |  [SMS Broadcast]   [Notification Stream]  [QS Tile] |
                                  +---------+-------------------+----------------+------+
                                            |                   |                |
                                            v                   v                v
+-----------------------+         +------------------+ +------------------+ +------------------+
|      USER INTERACTION |         |   SmsReceiver    | | KanriNotification| | QuickAddTile     |
| [Home/Ledger]         |         |  (TRAI DLT Bank) | | ListenerService  | | Service          |
| [Insights & Heatmaps] |         +--------+---------+ +--------+---------+ +--------+---------+
| [Lending Hub & UPI]   |                  |                    |                    |
| [Goals & Wallets]     |                  |                    | (Credits/Rewards)  |
+-----------+-----------+                  v                    v                    |
            |                     +---------------------------------------+          |
            |                     |       INGESTION & PARSING ENGINE      |          |
            |                     |  - BankSmsPatterns (50+ banks)        |          |
            |                     |  - SmsParser (Safe bounded regexes)   |          |
            |                     |  - NotificationParser (Signature ver.)|          |
            |                     |  - BrandFormatter (Merchant cleaner)  |          |
            |                     +-------------------+-------------------+          |
            |                                         |                              |
            |                                         v                              |
            |                     +---------------------------------------+          |
            |                     |     DEDUPLICATION & MERGE HELPER      |          |
            |                     |  - Exact refNo / UTR matching         |          |
            |                     |  - 5-minute sliding window comparison |          |
            |                     +-------------------+-------------------+          |
            |                                         |                              |
            |                                         v                              |
            |                     +---------------------------------------+          |
            +-------------------->|        ENCRYPTED ROOM DATABASE        |<---------+
            |                     |     (SQLCipher AES-256 GCM)           |
            |                     |  Transactions | Budgets | Wallets     |
            |                     |  Lending | Savings | Categories       |
            |                     +-------------------+-------------------+
            |                                         |
            v                                         v
+-----------------------+         +---------------------------------------+
|  HOME SCREEN WIDGETS  |         |     EXPORTS & BACKGROUND WORKERS      |
|  - 8 Glanceable Views |<--------+  - Vector PDF & CSV Exporters         |
|  - FrameLayout Divs   |         |  - ScheduledExportWorker (Internal)   |
|  - Monochrome Gauges  |         |  - CloudBackupWorker (Encrypted AES)  |
+-----------------------+         +---------------------------------------+
```

### 3.2 Dual-Pipeline Deduplication & Merge State Diagram

```
                 Incoming Ingestion Event
                            |
           +----------------+----------------+
           |                                 |
      Source: SMS                      Source: Notification
           |                                 |
   Reassemble multi-part             Verify package certificate
   PDUs into body string             & verify Credit / Reward type
           |                                 |
   Validate sender via                       |
   isTrustedSender()                         |
           |                                 |
           +----------------+----------------+
                            |
                            v
              Parse monetary amount, type,
              bank, counterparty, and refNo
                            |
                            v
             Does refNo match existing record?
                            |
               +------------+------------+
               | YES                     | NO
               v                         v
       Enrich missing fields     Is there an entry within
       (Counterparty / Bank /    +/- 5 minutes with identical
       raw SMS payload)          type and amount?
               |                         |
               |                +--------+--------+
               |                | YES             | NO
               |                v                 v
               |          Merge streams     Insert as brand-new
               |          into single txn   transaction record
               |                |                 |
               +----------------+-----------------+
                                |
                                v
                   Invalidate active widgets via
                 KanriWidgetsUpdater.updateAll()
```

---

## 4. Complete Project File & Directory Map

```text
app/src/main/java/com/omkarnub/kanri/
|-- KanriApplication.kt                 # Application class: initializers, notification channels, watchdog setup
|-- MainActivity.kt                      # Main Activity router, handles icon aliases, dynamic shortcuts & deep links
|
|-- data/                                # CORE DATA & BUSINESS LOGIC LAYER
|   |-- analytics/
|   |   |-- DeltaCalculator.kt           # Month-over-month percentage & absolute delta arithmetic
|   |   |-- FinancialHealthCalculator.kt # 100-point financial health index evaluated across 4 core pillars
|   |   |-- StreakCalculator.kt          # Continuous no-spend streak calculator with 5-year traversal bounds
|   |   |-- StreakDataStore.kt           # Proto/Preferences store for streak celebration milestones
|   |-- backup/
|   |   |-- BackupCryptoHelper.kt        # PBKDF2 (65,536 iters) + AES-256 GCM engine with KANRI_ENC_v1 magic header
|   |   |-- BackupData.kt                # Structured JSON serialization schema for database backups
|   |   |-- BackupRepository.kt          # Export, import, and safe-merge database restoration logic
|   |-- budget/
|   |   |-- BudgetCalculator.kt          # Month-to-date spending, pace calculation, safe-to-spend per day
|   |-- cloud/
|   |   |-- CloudBackupManager.kt        # Orchestrates encrypted cloud uploads and downloads via Google Drive
|   |   |-- CloudBackupPreferences.kt    # Secure store for cloud backup metadata and CSPRNG device secret
|   |   |-- CloudBackupWorker.kt         # Periodic WorkManager job for scheduled automated cloud backups
|   |   |-- GoogleAuthManager.kt         # Google Identity token refresh and account lifecycle manager
|   |   |-- GoogleAuthModels.kt          # Data models for cloud sync statuses, auth states, and results
|   |   |-- GoogleDriveService.kt        # RFC-compliant multipart HTTP streaming client for Google Drive API v3
|   |-- crash/
|   |   |-- CrashLogger.kt               # Local on-device exception recorder (replaces third-party crash trackers)
|   |-- db/
|   |   |-- BudgetDao.kt                 # Data Access Object for monthly budget configurations
|   |   |-- BudgetEntity.kt              # Room entity schema for table: budgets
|   |   |-- CategoryDao.kt               # Data Access Object for custom categories & smart merchant mappings
|   |   |-- CategoryEntity.kt            # Room entity schema for table: categories
|   |   |-- CounterpartyCategoryMapEntity.kt # Room entity schema for table: counterparty_category_map
|   |   |-- DatabaseKeyManager.kt        # Keystore-backed AES-256 key generator for SQLCipher encryption
|   |   |-- KanriDatabase.kt             # Room database definition with SQLCipher factory & migrations 1..11
|   |   |-- LendingDao.kt                # Data Access Object for loans, debts, partial repayments & settlements
|   |   |-- LendingEntity.kt             # Room entity schema for table: lending_records
|   |   |-- LendingRepaymentEntity.kt    # Room entity schema for table: lending_repayments
|   |   |-- LendingWithRepayments.kt     # Relational aggregate model linking lending records with repayments
|   |   |-- RecurringPaymentDao.kt       # Data Access Object for recurring subscriptions & bills
|   |   |-- RecurringPaymentEntity.kt    # Room entity schema for table: recurring_payments
|   |   |-- SavingsGoalContributionEntity.kt # Room entity schema for table: savings_goal_contributions
|   |   |-- SavingsGoalDao.kt            # Data Access Object for savings milestones & incremental deposits
|   |   |-- SavingsGoalEntity.kt         # Room entity schema for table: savings_goals
|   |   |-- TransactionDao.kt            # Data Access Object for core ledger transactions & wallet balances
|   |   |-- TransactionEntity.kt         # Room entity schema for table: transactions
|   |   |-- TransactionWithCategory.kt   # Relational aggregate model linking transactions with category entities
|   |   |-- WalletBalanceEntity.kt       # Room entity schema for table: wallet_balances
|   |   |-- WalletDao.kt                 # Data Access Object for cash & online wallet baseline records
|   |-- export/
|   |   |-- CsvExporter.kt               # RFC 4180 CSV serializer with spreadsheet formula injection prevention
|   |   |-- ExportScheduler.kt           # Configures scheduled monthly statement WorkManager jobs
|   |   |-- PdfStatementExporter.kt      # Native multi-page vector PDF Canvas drawing engine
|   |   |-- ScheduledExportWorker.kt     # Periodic worker generating monthly statements to private internal storage
|   |   |-- StatementExportRepository.kt # High-level exporter interface coordinating file outputs and ranges
|   |-- greeting/
|   |   |-- GreetingDataStore.kt         # Persistent storage for anti-repetition greeting states
|   |-- lending/
|   |   |-- LendingMoneyEngine.kt        # Net balance calculation for debts, receivables, and group splits
|   |   |-- LendingReminderScheduler.kt  # Enqueues alerts for upcoming debt and loan due dates
|   |   |-- LendingReminderWorker.kt     # Background worker evaluating overdue and approaching debts
|   |   |-- LendingTransactionSyncHelper.kt # Automatically links bank transactions with loan records
|   |-- notification/
|   |   |-- DailyReminderScheduler.kt    # Enqueues 9:00 PM evening digest reminders
|   |   |-- DailyReminderWorker.kt       # Evaluates daily uncategorized spend and alerts the user
|   |-- parser/
|   |   |-- BankSmsPatterns.kt           # 50+ public/private Indian bank profiles & TRAI sender verifiers
|   |   |-- NotificationDeduplicationHelper.kt # 5-minute sliding window merger between SMS and app notifications
|   |   |-- NotificationParser.kt        # Ingests credit alerts from allowlisted UPI payment apps
|   |   |-- SmsParser.kt                 # ReDoS-hardened financial regex parser for debits, credits & ATMs
|   |-- profile/
|   |   |-- UserProfilePreferences.kt    # Secure preferences for user profile, name, avatar, and currency
|   |-- reset/
|   |   |-- FactoryResetManager.kt       # Complete atomic reset: wipes database, Keystore, files & workers
|   |-- security/
|   |   |-- BiometricHelper.kt           # AndroidX BiometricPrompt authentication wrapper
|   |   |-- SecurityManager.kt           # App lock coordinator: handles background timeouts and screen-off events
|   |   |-- SecurityPreferences.kt       # Encrypted preferences for app lock status and biometrics
|   |-- update/
|   |   |-- AppUpdateInfo.kt             # Data models for GitHub Releases semantic version payloads
|   |   |-- AppUpdateManager.kt          # Queries GitHub API for OTA updates and triggers APK downloads
|   |   |-- AppUpdatePreferences.kt      # Stores update checking frequency and dismissed release tags
|   |   |-- AppUpdateWorker.kt           # Daily background worker querying for new GitHub releases
|   |   |-- SemanticVersionComparator.kt # Compares Major.Minor.Patch strings with pre-release tag handling
|   |-- wallet/
|       |-- AtmWithdrawalMode.kt         # Enum defining ATM behavior: SPENDING vs WALLET_TRANSFER
|       |-- WalletBalanceCalculator.kt   # Mathematical engine computing live Cash, Online, and Total balances
|       |-- WalletModels.kt              # Data classes for wallet balances, transactions, and transfer rows
|       |-- WalletPreferences.kt         # User settings for wallet configuration and ATM resolution mode
|       |-- WalletRepository.kt          # Wallet data coordinator: manages opening balances and transfers
|       |-- WalletResolver.kt            # Decides target wallet and transfer routing for incoming transactions
|
|-- receiver/                            # SYSTEM BROADCAST RECEIVERS
|   |-- BootReceiver.kt                  # Listens to BOOT_COMPLETED; re-initializes workers, alarms, and widgets
|   |-- SmsReceiver.kt                   # Listens to SMS_RECEIVED; parses banking PDUs asynchronously
|
|-- service/                             # ANDROID BACKGROUND SERVICES
|   |-- QuickAddTileService.kt           # Quick Settings dropdown tile to launch instant transaction creation
|
|-- ui/                                  # USER INTERFACE PRESENTATION LAYER
|   |-- backup/                          # Passphrase entry dialogs, cloud sync sheets, backup restore prompts
|   |-- category/
|   |   |-- CategoryDetailModels.kt      # UI state models for single-category trend views
|   |   |-- CategoryDetailScreen.kt      # Detailed historical drill-down for an individual category
|   |   |-- CategoryDetailViewModel.kt   # Computes 6-month spending trends and MoM changes per category
|   |   |-- CategoryTrendLineChart.kt    # Smooth line chart showing category monthly spend progression
|   |-- common/
|   |   |-- AnimatedNumber.kt            # Spring-animated rolling number counters for currency values
|   |   |-- CategoryIcon.kt              # Visual category badge renderer with dynamic colors and icons
|   |   |-- FlipFadeText.kt              # Smooth text animation transitions
|   |   |-- GlassBackdrop.kt             # Curated ambient glass backdrop: shader WebView + contrast scrim + procedural micro-grain overlay
|   |   |-- GlassBackgroundView.kt       # Hardware-accelerated WebView host for 8 dynamic WebGL/Canvas background shaders with lifecycle pause/resume
|   |   |-- GrainGradientBackground.kt   # Tactile noise-grain visual surface shaders
|   |   |-- KanriGlass.kt                # Comprehensive frosted glass component library: kanriGlass() modifier, KanriGlassCard, KanriGlassPill, KanriGlassButton, KanriGlassDialog, KanriGlassAlertDialog, KanriGlassDatePickerDialog, KanriModalBottomSheet, KanriDropdownMenu, kanriPrimaryButtonColors, native window blur
|   |   |-- KanriWobbleLoader.kt         # Minimal organic loading spinner
|   |-- export/
|   |   |-- ExportStatementSheet.kt      # Date range and format selector for PDF/CSV generation
|   |-- greeting/
|   |   |-- GreetingModels.kt            # Greeting contexts, rules, tone styles, and anti-repetition seeds
|   |   |-- GreetingResolver.kt          # Time-of-day greeting engine with dynamic financial observations
|   |-- health/
|   |   |-- DiagnosticsSheet.kt          # Internal diagnostic panel displaying database and cache status
|   |   |-- HealthCheckBanner.kt         # Banner alerting users when critical permissions or setups are missing
|   |   |-- HealthCheckHelper.kt         # Evaluates system health, notification listener state, and battery status
|   |-- home/
|   |   |-- AddTransactionDialog.kt      # Manual entry dialog for income, expenses, and wallet transfers
|   |   |-- CashFlowCard.kt              # Glass card displaying monthly income, spend, and net cash flow
|   |   |-- CategoryPickerSheet.kt       # Bottom sheet to select or create a category for transactions
|   |   |-- ExpenseVisualizer.kt         # Segmented spending bar visualizing category distributions
|   |   |-- FinancialHealthCard.kt       # Primary scorecard displaying the 100-point Financial Health Index
|   |   |-- FinancialHealthDetailsSheet.kt # Comprehensive sheet detailing the 4 financial pillars and scores
|   |   |-- HomeScreen.kt                # Main dashboard: balance cards, streak pill, and transaction feed
|   |   |-- HomeViewModel.kt             # Primary ViewModel managing ledger states, greetings, and balances
|   |   |-- LendingQuickPulse.kt         # Home dashboard widget summarizing pending loans and debts
|   |   |-- MonthDeltaChip.kt            # Pill indicator showing spending velocity compared to prior month
|   |   |-- NeedsReviewCard.kt           # Banner indicating untrusted or uncategorized transactions
|   |   |-- NeedsReviewSheet.kt          # Review queue allowing one-tap bulk confirmation of flagged entries
|   |   |-- NoSpendStreakCard.kt         # Card tracking consecutive no-spend days and weekly calendar dots
|   |   |-- SetBudgetDialog.kt           # Dialog allowing the user to configure the current monthly budget
|   |   |-- TransactionDetailScreen.kt   # Full-screen inspector for a transaction: metadata, raw SMS, notes
|   |   |-- WheresMyMoneyCard.kt         # Visual breakdown highlighting top spending areas of the month
|   |-- insights/
|   |   |-- InsightsCalculators.kt       # Analytics math: payees, payment methods, heatmaps, projections
|   |   |-- InsightsModels.kt            # Comprehensive state data models for the Insights tab
|   |   |-- InsightsPeriods.kt           # Time range definitions: Current Month, Last Month, Custom, Year
|   |   |-- InsightsViewModel.kt         # State manager aggregating all analytical reports and chart feeds
|   |   |-- screens/
|   |   |   |-- CompareMonthsScreen.kt   # Side-by-side comparison between two historical calendar months
|   |   |   |-- MonthlyRecapCard.kt      # Shareable summary card recapping the month's financial behavior
|   |   |   |-- MonthlyRecapScreen.kt    # Full-screen storytelling recap of the previous month's finances
|   |   |   |-- PayeeDetailScreen.kt     # Deep-dive screen into transactions associated with a single payee
|   |   |-- sections/
|   |       |-- BiggestTransactionsSection.kt # Lists the 5 highest monetary expenses in the period
|   |       |-- BudgetVsActualSection.kt      # Horizontal bar comparing budgeted limits against actual spend
|   |       |-- CalendarHeatmapSection.kt     # Daily calendar grid shaded by transaction density
|   |       |-- CategoryMoversSection.kt      # Highlights categories with the largest positive/negative delta
|   |       |-- CheckFinancialHealthCard.kt   # Shortcut card to trigger the financial health diagnosis
|   |       |-- CumulativeSpendLineSection.kt # Smooth line chart plotting cumulative spending trajectory
|   |       |-- IncomeSourcesSection.kt       # Distribution chart of income streams
|   |       |-- InsightsRangeBar.kt           # Segmented pill selector for time ranges (Month / Year / All)
|   |       |-- InsightsToolsRow.kt           # Quick actions: Compare Months, Year Heatmap, Export
|   |       |-- MonthEndProjectionSection.kt  # Statistical spend projection estimating end-of-month totals
|   |       |-- MonthSummaryStripSection.kt   # Summary strip showing total debits, credits, and net savings
|   |       |-- PaymentMethodSplitSection.kt  # Donut chart splitting UPI vs Card vs ATM vs Cash
|   |       |-- PersonalRecordsSection.kt     # Records: Highest Spend Day, Longest Streak, Lowest Spend Day
|   |       |-- SmallSpendsSection.kt         # Micro-spend analysis (expenses under configurable threshold)
|   |       |-- SplitExpensesSummarySection.kt# Summary of group bill splits logged in the period
|   |       |-- SuggestedBudgetSection.kt     # Intelligent budget suggestion based on 3-month trailing median
|   |       |-- TopPayeesSection.kt           # Ranked list of top merchants by total money spent
|   |       |-- WeekendVsWeekdaySection.kt    # Comparison of weekday spending versus weekend spending
|   |       |-- YearHeatmapSection.kt         # GitHub-style 365-day annual spending activity heatmap
|   |-- lending/
|   |   |-- AddLendingDialog.kt          # Dialog to record a new loan or borrowed balance
|   |   |-- AnimatedMonochromeRingChart.kt# Monochrome circular gauge for settled vs outstanding debts
|   |   |-- LendingDateFormatters.kt     # Relative and absolute date formatters for debt due dates
|   |   |-- LendingEntryDialog.kt        # Combined entry dialog for single and split group debts
|   |   |-- LendingHeroCard.kt           # Top dashboard card displaying Net Outstanding, Lent, and Borrowed
|   |   |-- LendingHubState.kt           # Immutable UI state models for the Lending and Debt Hub
|   |   |-- LendingHubViewModel.kt       # State manager coordinating people lists, timelines, and settlements
|   |   |-- LendingPeopleView.kt         # Contact-centric list grouping loans by individual people
|   |   |-- LendingPersonDetailView.kt   # Complete transaction history and active balances with a contact
|   |   |-- LendingReminderUtils.kt      # Generates formatted reminder SMS and WhatsApp messages
|   |   |-- LendingRenameDialog.kt       # Allows renaming a contact across all historical loan records
|   |   |-- LendingRepaymentDialog.kt    # Dialog to log partial or total repayment of a debt
|   |   |-- LendingScreen.kt             # Main Lending tab container with People and Timeline view tabs
|   |   |-- LendingSegmentedControl.kt   # Monochrome pill control switching between Lent, Borrowed, and All
|   |   |-- LendingSettleAllDialog.kt    # Confirmation sheet to batch-settle all active records with a contact
|   |   |-- LendingSplitGroupCard.kt     # Card displaying group bill-splitting participants and balances
|   |   |-- LendingTimelineView.kt       # Chronological stream of all loans, repayments, and settlements
|   |   |-- LendingTransactionBridgeDialog.kt # Links a bank transaction to an active loan
|   |   |-- LendingTransactionDetailDialog.kt # Details of an individual loan record with repayment history
|   |   |-- LendingUpiHelper.kt          # Constructs RFC-compliant UPI payment intent URLs
|   |   |-- LendingUpiPayDialog.kt       # Launches external UPI app to settle a debt with prefilled VPA & amount
|   |   |-- LendingViewModel.kt          # Legacy ViewModel wrapper maintaining compatibility
|   |   |-- SplitExpenseScreen.kt        # Dedicated full-screen bill-splitting calculator
|   |   |-- SplitExpenseSheet.kt         # Quick bill-splitter bottom sheet for fast group bill calculations
|   |-- month/
|   |   |-- AllTimeTrendsCalculator.kt   # Computes multi-year spending patterns and rolling averages
|   |   |-- AllTimeTrendsModels.kt       # State models for historical ledger trend views
|   |   |-- AllTimeTrendsView.kt         # Visual presentation of multi-year financial trends
|   |   |-- CategoryDonutChart.kt        # Monochrome frosted donut chart breaking down category expenses
|   |   |-- DailySpendBarChart.kt        # Bar chart showing daily spending amounts across the calendar month
|   |   |-- MonthViewModel.kt            # ViewModel driving the historical calendar month ledger
|   |   |-- MonthViewScreen.kt           # Full calendar month ledger screen with day-by-day drill-downs
|   |   |-- MultiMonthTrendsChart.kt     # Bar chart comparing expenses across consecutive calendar months
|   |-- navigation/
|   |   |-- AddExpenseIncomePopup.kt     # Floating quick-add action sheet with options for Expense, Income, Split
|   |   |-- KanriDockIcons.kt            # Custom monochrome vector icons for the bottom navigation dock
|   |   |-- KanriFloatingDock.kt         # Floating glassmorphic bottom navigation dock
|   |   |-- MainNavigationScreen.kt      # Root application container managing bottom navigation and deep links
|   |-- notification/
|   |   |-- KanriNotificationListenerService.kt # Android NotificationListenerService capturing credit notifications
|   |   |-- NotificationAccessHelper.kt  # Checks and prompts for Notification Listener system permission
|   |   |-- NotificationListenerWatchdogWorker.kt # 15-minute watchdog worker keeping the listener alive
|   |-- onboarding/
|   |   |-- DemoScenario.kt              # Pre-populated mock data for the interactive onboarding demonstration
|   |   |-- FoldText.kt                  # 3D perspective text folding animation for onboarding transitions
|   |   |-- OnboardingHeroDemo.kt        # Interactive visual preview showcasing automated SMS parsing
|   |   |-- OnboardingMiniGauge.kt       # Animated progress indicator used during onboarding tutorials
|   |   |-- OnboardingPreferences.kt    # Tracks onboarding completion status
|   |   |-- OnboardingPrivacySheet.kt    # Privacy disclosure explaining zero-telemetry and on-device processing
|   |   |-- OnboardingScreen.kt          # 3-step modern onboarding flow introducing app capabilities
|   |-- popup/
|   |   |-- InstantPopupCard.kt          # Truecaller-style floating transaction card
|   |   |-- InstantPopupNotificationHelper.kt # Posts interactive notifications for immediate transaction categorization
|   |   |-- InstantPopupReceiver.kt      # Broadcast receiver handling inline category actions from notifications
|   |   |-- InstantPopupService.kt       # System overlay service rendering floating transaction confirmation cards
|   |   |-- InstantPopupSheet.kt         # Bottom sheet variant of the instant transaction confirmation card
|   |   |-- OverlayPermissionHelper.kt   # Checks and requests SYSTEM_ALERT_WINDOW overlay permission
|   |-- profile/
|   |   |-- ProfileScreen.kt             # User profile screen: data management, encrypted backup exports & imports
|   |-- recurring/
|   |   |-- RecurringPaymentCalculator.kt# Calculates upcoming subscription charges and monthly recurring commitments
|   |   |-- RecurringPaymentsSheet.kt    # Bottom sheet managing subscriptions, rent, utility bills, and insurance
|   |-- savings/
|   |   |-- CircularGoalProgressGauge.kt # Canvas-rendered circular progress ring for savings milestones
|   |   |-- GoalDialogs.kt               # Dialogs for creating, editing, and contributing funds to savings goals
|   |   |-- GoalIconHelper.kt            # Icon picker mapping goals to relevant visual badges
|   |   |-- GoalsScreen.kt               # Main Savings Goals tab displaying active and completed targets
|   |   |-- SavingsGoalCalculator.kt     # Computes savings completion percentage and target pace
|   |   |-- SavingsGoalsSheet.kt         # Bottom sheet interface for savings goals management
|   |-- search/
|   |   |-- SearchFilterModels.kt        # Filter criteria: keyword, date bounds, amount ranges, wallet, category
|   |   |-- SearchFilterSheet.kt         # Filter configuration bottom sheet for transaction search
|   |   |-- SearchScreen.kt              # Full-text transaction search with multi-parameter filter chips
|   |   |-- SearchViewModel.kt           # ViewModel executing reactive filtering on the transaction ledger
|   |-- security/
|   |   |-- LockScreen.kt                # Biometric and PIN authentication challenge screen
|   |   |-- SecuritySettingsSheet.kt     # Configuration sheet to toggle App Lock and biometric requirements
|   |-- settings/
|   |   |-- CrashLogsDialog.kt           # Viewer for locally captured diagnostic logs
|   |   |-- GlassCustomizerScreen.kt     # Real-time glass physics and optics tuner with live interactive preview, preset selector, and granular sliders
|   |   |-- PrivacyPolicyDialog.kt       # In-app offline privacy policy and architectural guarantees
|   |   |-- SettingsScreen.kt            # Application settings: theme, currency, cloud sync, resets
|   |   |-- SmartMerchantRulesScreen.kt  # Rule manager mapping merchant keyword patterns to categories
|   |-- theme/
|   |   |-- AppIconManager.kt            # Dynamic launcher icon manager switching activity aliases
|   |   |-- Color.kt                     # Frosted glass, dark mode, and monochrome color palettes
|   |   |-- GlassCapabilities.kt         # Hardware capability detector checking Android 12+ (API 31+), RAM, power save mode, and reduced motion
|   |   |-- KanriGlassTokens.kt          # Glass design tokens, GlassCustomization models, 5 curated presets, and dynamic token calculation
|   |   |-- Theme.kt                     # KanriTheme Jetpack Compose theme provider (MaterialTheme + LocalKanriGlass injection)
|   |   |-- ThemePreferences.kt          # Manages ThemeMode (Light/Dark/System/AMOLED/Glass), GlassCustomization, dynamic color, and reduce transparency
|   |   |-- Type.kt                      # Google Sans typography definitions (Title, Body, Label)
|   |-- update/
|   |   |-- AppUpdateDialog.kt           # In-app dialog presenting new version details and changelogs
|   |-- wallet/
|       |-- CorrectBalanceDialog.kt      # Reconciles ledger discrepancy by adjusting wallet opening balances
|       |-- MoveMoneyDialog.kt           # Logs a manual balance transfer between Cash and Online wallets
|       |-- WalletSetupDialog.kt         # Initial onboarding dialog to set Cash and Online baseline balances
|       |-- WalletSheet.kt               # Bottom sheet providing complete wallet management and transfer history
|
|-- util/                                # GENERAL UTILITY MODULES
|   |-- AppCurrency.kt                   # Currency definitions, symbols, and formatting rules
|   |-- AppShortcutManager.kt            # Android dynamic home screen shortcuts (Expense, Income, Split, Lending)
|   |-- BatteryOptimizationHelper.kt     # Guides users through OEM battery saver whitelisting settings
|   |-- BrandFormatter.kt                # Normalizes raw merchant strings (e.g. "UPI/CR/4256/SWIGGY" -> "Swiggy")
|   |-- CurrencyPreferences.kt           # User currency preference storage (INR, USD, EUR, GBP, JPY, etc.)
|   |-- CurrencyUtils.kt                 # Compact and standard currency formatters (e.g., ₹1.5K, ₹1,500.00)
|   |-- DevCaptureLogger.kt              # Local debug capture logger for SMS and notification testing
|   |-- KanriHaptics.kt                  # Tactile vibration patterns (click, success, warning, error, impact)
|   |-- SmsPermissionHelper.kt           # Runtime SMS permission check & request helper
|
|-- widget/                              # HOME SCREEN WIDGET SUBSYSTEM
    |-- BudgetProgressWidgetProvider.kt  # 4x1 horizontal budget progress bar widget
    |-- BudgetRingWidgetProvider.kt      # 2x2 frosted circular budget remaining ring widget
    |-- GoalsRingWidgetProvider.kt       # 2x2 circular savings goal tracker widget
    |-- KanriWidgetActions.kt            # Action router dispatching widget taps to MainNavigationScreen
    |-- KanriWidgetsUpdater.kt           # Central invalidation dispatcher to refresh all active widgets
    |-- LendBorrowWidgetProvider.kt      # 4x2 pending lending & debts list widget
    |-- MidnightWidgetResetWorker.kt     # Midnight cron worker to reset "Spent Today" counter
    |-- MoneySpentTodayWidgetProvider.kt # 2x1 daily spent tracker widget
    |-- QuickAddWidgetProvider.kt        # 3x1 quick expense/income recording widget
    |-- SplitBillWidgetProvider.kt       # 3x1 quick split bill calculator launcher widget
    |-- WalletBalanceWidgetProvider.kt   # 4x2 dual-column Cash Balance & Online Balance widget
    |-- WidgetCanvasRenderer.kt          # Pure Canvas bitmap drawer for vector rings and gauges
```

---

## 5. Room Database Architecture & Entity Schemas

Kanri uses AndroidX Room backed by **SQLCipher 4.5.5+** with full database-level encryption (Database version 11).

### 5.1 Database Definition (`KanriDatabase.kt`)
The database is initialized via `Room.databaseBuilder` using `SupportOpenHelperFactory(passphrase)` where `passphrase` is derived from `DatabaseKeyManager.getOrCreatePassphrase(context)`.

```kotlin
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CounterpartyCategoryMapEntity::class,
        BudgetEntity::class,
        LendingEntity::class,
        LendingRepaymentEntity::class,
        RecurringPaymentEntity::class,
        SavingsGoalEntity::class,
        SavingsGoalContributionEntity::class,
        WalletBalanceEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class KanriDatabase : RoomDatabase()
```

### 5.2 Database Migration History
- **Migration 5 → 6**: Added `needs_review` (INTEGER NOT NULL DEFAULT 0) and `review_reason` (TEXT DEFAULT NULL) to table `transactions`.
- **Migration 6 → 7**: Added `notes` (TEXT DEFAULT NULL) to table `transactions`.
- **Migration 7 → 8**: Added `is_custom` (INTEGER NOT NULL DEFAULT 0) to table `categories`.
- **Migration 8 → 9**: Added `original_amount` (REAL DEFAULT NULL) to `lending_records`; created table `lending_repayments` with foreign key cascade to `lending_records(id)`.
- **Migration 9 → 10**: Created table `savings_goal_contributions` with foreign key cascade to `savings_goals(id)`.
- **Migration 10 → 11**: Created table `wallet_balances` (columns: `wallet_id` PK, `opening_amount`, `opening_timestamp`); added columns `wallet` (TEXT NOT NULL DEFAULT 'ONLINE') and `transfer_to_wallet` (TEXT DEFAULT NULL) to `transactions`. Added index `index_transactions_wallet`. Backfilled existing records based on `source_type = 'CASH'`.

### 5.3 Entity Specifications

#### 1. `transactions` (`TransactionEntity.kt`)
```kotlin
@Entity(tableName = "transactions", indices = [Index(value = ["wallet"])])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                       // "DEBIT" or "CREDIT"
    val amount: Double,                     // Monetary transaction value
    val sourceType: String,                 // "UPI", "ATM", "CARD", "BANK_TRANSFER", "UNKNOWN", "CASH"
    val counterparty: String? = null,       // Raw merchant, payee, or VPA
    val displayName: String? = null,        // Clean human-readable merchant title
    val bank: String? = null,               // Bank or financial institution name
    val refNo: String? = null,              // UTR / UPI reference number (unique transaction identifier)
    val timestamp: Long,                    // Unix epoch timestamp in milliseconds
    val categoryId: Long? = null,           // Foreign key reference to categories.id
    val rawSms: String = "",                // Original SMS or notification body (truncated to 2,000 chars)
    val isDuplicate: Boolean = false,       // Flag indicating duplicate or superseded entry
    val isManualEntry: Boolean = false,     // True if entered manually by user
    val needsReview: Boolean = false,       // True if low-confidence or untrusted sender
    val reviewReason: String? = null,       // Human-readable reason for review flag
    val notes: String? = null,              // User-authored annotations
    val wallet: String = "ONLINE",          // Source wallet ("CASH" or "ONLINE")
    val transferToWallet: String? = null    // Destination wallet for internal transfers
)
```

#### 2. `budgets` (`BudgetEntity.kt`)
```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val monthKey: String,       // Key formatted as "yyyy-MM" or "yyyy-MM_cat_<id>"
    val monthlyLimit: Double                // Target maximum expenditure limit
)
```

#### 3. `categories` (`CategoryEntity.kt`)
```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // Category title (e.g. "Food & Dining")
    val colorHex: String,                   // Hex color code (e.g. "#FF7043")
    val iconName: String,                   // Material icon identifier
    val isCustom: Boolean = false           // True if user-created
)
```

#### 4. `counterparty_category_map` (`CounterpartyCategoryMapEntity.kt`)
```kotlin
@Entity(tableName = "counterparty_category_map")
data class CounterpartyCategoryMapEntity(
    @PrimaryKey val counterparty: String,   // Clean counterparty name or keyword
    val categoryId: Long                    // Assigned category foreign key
)
```

#### 5. `lending_records` (`LendingEntity.kt`)
```kotlin
@Entity(tableName = "lending_records")
data class LendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,                 // Contact or debtor/creditor name
    val amount: Double,                     // Current outstanding balance
    val type: String,                       // "LENT" (receivable) or "BORROWED" (payable)
    val date: Long,                         // Initial loan timestamp
    val dueDate: Long? = null,              // Expected repayment deadline
    val notes: String? = null,              // Loan conditions or memo
    val isSettled: Boolean = false,         // True if fully settled
    val settledDate: Long? = null,          // Settlement completion timestamp
    val originalAmount: Double? = null,     // Principal amount before partial repayments
    val linkedTransactionId: Long? = null   // Associated transaction ID in the ledger
)
```

#### 6. `lending_repayments` (`LendingRepaymentEntity.kt`)
```kotlin
@Entity(
    tableName = "lending_repayments",
    foreignKeys = [
        ForeignKey(
            entity = LendingEntity::class,
            parentColumns = ["id"],
            childColumns = ["lending_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lending_id"])]
)
data class LendingRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lending_id") val lendingId: Long,
    val amount: Double,                     // Repaid installment amount
    @ColumnInfo(name = "paid_at") val paidAt: Long, // Payment timestamp
    val note: String? = null                // Repayment memo
)
```

#### 7. `savings_goals` (`SavingsGoalEntity.kt`)
```kotlin
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // Goal title (e.g. "Emergency Fund")
    @ColumnInfo(name = "target_amount") val targetAmount: Double,
    @ColumnInfo(name = "current_amount") val currentAmount: Double = 0.0,
    @ColumnInfo(name = "target_date") val targetDate: Long? = null,
    @ColumnInfo(name = "color_hex") val colorHex: String = "#58A6FF",
    val emoji: String = "🎯",               // Visual icon identifier
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false
)
```

#### 8. `savings_goal_contributions` (`SavingsGoalContributionEntity.kt`)
```kotlin
@Entity(
    tableName = "savings_goal_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goal_id"])]
)
data class SavingsGoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "goal_id") val goalId: Long,
    val amount: Double,                     // Deposit amount
    val timestamp: Long,                    // Deposit timestamp
    val note: String? = null,               // Memo
    @ColumnInfo(name = "source_transaction_id") val sourceTransactionId: Long? = null
)
```

#### 9. `recurring_payments` (`RecurringPaymentEntity.kt`)
```kotlin
@Entity(tableName = "recurring_payments")
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,                      // Subscription name (e.g. "Netflix", "Rent")
    val amount: Double,                     // Recurring charge amount
    val frequency: String,                  // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    @ColumnInfo(name = "next_due_timestamp") val nextDueTimestamp: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    val wallet: String = "ONLINE",          // Wallet source
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
```

#### 10. `wallet_balances` (`WalletBalanceEntity.kt`)
```kotlin
@Entity(tableName = "wallet_balances")
data class WalletBalanceEntity(
    @PrimaryKey @ColumnInfo(name = "wallet_id") val walletId: String, // "CASH" or "ONLINE"
    @ColumnInfo(name = "opening_amount") val openingAmount: Double,    // Baseline opening balance
    @ColumnInfo(name = "opening_timestamp") val openingTimestamp: Long // Timestamp when opening balance was configured
)
```

---

## 6. Complete DAO Function & Query Reference

### 6.1 `TransactionDao` (`TransactionDao.kt`)

| Method Signature | Return Type | Operational SQL Query / Function Behavior |
|---|---|---|
| `insert(transaction: TransactionEntity)` | `Long` | `INSERT OR REPLACE INTO transactions ...` Returns newly inserted row ID. |
| `getAllTransactions()` | `Flow<List<TransactionEntity>>` | `SELECT * FROM transactions ORDER BY timestamp DESC` Reactive stream of all transactions. |
| `getTransactionsBetween(startTime: Long, endTime: Long)` | `Flow<List<TransactionEntity>>` | `SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC` |
| `findByRefNo(refNo: String)` | `TransactionEntity?` | `SELECT * FROM transactions WHERE ref_no = :refNo LIMIT 1` Fast lookup for UTR deduplication. |
| `getTransactionById(id: Long)` | `TransactionEntity?` | `SELECT * FROM transactions WHERE id = :id LIMIT 1` |
| `observeTransactionWithCategory(id: Long)` | `Flow<TransactionWithCategory?>` | Reactive relational query joining transaction with its category. |
| `getTransactionWithCategoryById(id: Long)` | `TransactionWithCategory?` | Synchronous relational lookup by primary key ID. |
| `deleteById(id: Long)` | `Unit` | `DELETE FROM transactions WHERE id = :id` |
| `deleteByRefNo(refNo: String)` | `Unit` | `DELETE FROM transactions WHERE ref_no = :refNo` |
| `deleteByRefNoPattern(pattern: String)` | `Unit` | `DELETE FROM transactions WHERE ref_no LIKE :pattern` |
| `getTransactionsByRefNoPattern(pattern: String)` | `List<TransactionEntity>` | `SELECT * FROM transactions WHERE ref_no LIKE :pattern` |
| `updateCounterpartyForLending(oldName: String, newName: String)` | `Unit` | Renames counterparty on transactions tagged with `source_type = 'LENDING'`. |
| `getTransactionsWithCategory()` | `Flow<List<TransactionWithCategory>>` | Reactive stream of all transactions joined with category metadata. |
| `getTransactionsWithCategoryBetweenSync(start: Long, end: Long)` | `List<TransactionWithCategory>` | Synchronous time-bounded transaction retrieval with categories. |
| `getAllTransactionsWithCategorySync()` | `List<TransactionWithCategory>` | Synchronous full ledger dump with categories. |
| `observeReviewCount()` | `Flow<Int>` | `SELECT COUNT(*) FROM transactions WHERE (needs_review = 1 OR category_id IS NULL) AND transfer_to_wallet IS NULL` |
| `observeReviewQueue()` | `Flow<List<TransactionEntity>>` | Stream of all transactions requiring user review or categorization. |
| `clearReviewFlag(transactionId: Long)` | `Unit` | Clears `needs_review = 0` and `review_reason = NULL` for single entry. |
| `clearReviewFlags(transactionIds: List<Long>)` | `Unit` | Bulk clears review flags for a list of transaction IDs. |
| `clearAllReviewFlags()` | `Unit` | Clears all pending review flags across the entire database. |
| `updateCategoryId(transactionId: Long, categoryId: Long)` | `Unit` | Assigns category ID and marks `needs_review = 0`. |
| `updateCategoryAndNotes(id: Long, catId: Long, notes: String?)`| `Unit` | Updates both category ID and user annotation notes. |
| `updateNotes(transactionId: Long, notes: String?)` | `Unit` | Updates user memo without altering category. |
| `updateCategoryForCounterparty(counterparty: String, catId: Long)`| `Unit` | Batch-updates category for all transactions with identical counterparty. |
| `applyCategoryToMatchingTransactions(keyword: String, catId: Long)`| `Int` | Batch-updates transactions matching keyword pattern; returns updated row count. |
| `countTransactionsMatchingKeyword(keyword: String)` | `Int` | Counts total records containing keyword in counterparty or display name. |
| `reassignTransactionsCategory(deletedId: Long, fallbackId: Long?)`| `Unit` | Reassigns transactions pointing to a deleted category to a fallback category. |
| `findRecentMatching(type: String, amount: Double, min: Long, max: Long)`| `TransactionEntity?` | Finds matching transaction within `[min, max]` window where `ABS(amount - :amount) < 0.01`. |
| `enrichCounterpartyIfEmpty(id: Long, name: String, display: String)`| `Unit` | Enriches empty or generic counterparties with validated merchant strings. |
| `enrichRefNoIfEmpty(id: Long, refNo: String)` | `Unit` | Enriches empty reference number with newly discovered UTR. |
| `enrichBankIfGeneric(id: Long, bank: String)` | `Unit` | Overwrites generic payment app banks (e.g. "Google Pay") with authentic bank names. |
| `enrichRawSmsIfFromNotification(id: Long, rawSms: String)` | `Unit` | Replaces short notification text with authoritative bank SMS body. |
| `getCount()` | `Int` | `SELECT COUNT(*) FROM transactions` Total row count. |
| `getAllTransactionsSync()` | `List<TransactionEntity>` | Synchronous list of all transactions ordered by timestamp descending. |
| `getRecentTransactionsSync(limit: Int = 3)` | `List<TransactionEntity>` | Retrieves the top `limit` most recent transactions. |
| `insertAll(transactions: List<TransactionEntity>)` | `Unit` | Batch insert or replace for restore/sync engines. |
| `deleteAllTransactions()` | `Unit` | `DELETE FROM transactions` Purges all ledger entries. |
| `delete(transaction: TransactionEntity)` | `Unit` | Deletes specific entity instance. |
| `observeWalletTransactions()` | `Flow<List<WalletTxnRow>>` | Stream of confirmed non-duplicate transactions for live wallet balance computation. |
| `getWalletTransactionsSync()` | `List<WalletTxnRow>` | Synchronous list of confirmed non-duplicate transactions for wallet balance calculation. |
| `rewriteAtmTransfers(targetTransferWallet: String?)` | `Int` | Converts historical ATM debits to wallet transfers or expenses based on mode. |
| `convertTransactionToTransfer(id: Long, wallet: String, toWallet: String)`| `Int` | Converts a regular expense into an internal wallet-to-wallet transfer. |
| `updateWallet(id: Long, wallet: String)` | `Unit` | Changes source wallet tag of a transaction ("CASH" or "ONLINE"). |
| `updateWalletAndTransfer(id: Long, wallet: String, toWallet: String?)`| `Unit` | Updates both source wallet and transfer destination wallet tags. |
| `getTransactionsBetweenSync(start: Long, end: Long)` | `List<TransactionEntity>` | Synchronous time-bounded transaction retrieval (excluding transfers). |
| `getTransactionsByTypeBetweenSync(type: String, start: Long, end: Long)`| `List<TransactionEntity>` | Time-bounded retrieval filtered strictly by `type` ("DEBIT" or "CREDIT"). |
| `getDailySpendAggregates(type: String, start: Long, end: Long)` | `List<DailySpendAggregate>` | Groups expenditures by calendar day (`strftime('%Y-%m-%d')`) with sum and count. |
| `getTopPayeeAggregates(type: String, start: Long, end: Long, limit: Int)`| `List<PayeeSpendAggregate>` | Groups spending by counterparty; returns top spenders ranked by total amount. |
| `getSourceTypeAggregates(type: String, start: Long, end: Long)` | `List<SourceTypeAggregate>` | Groups spending by source type (UPI, CARD, ATM, etc.) with totals. |
| `getCategorySpendAggregates(type: String, start: Long, end: Long)` | `List<CategorySpendAggregate>` | Groups spending by category foreign key ID with sums and counts. |
| `getBiggestTransactionsWithCategory(type: String, start: Long, end: Long, limit: Int)`| `List<TransactionWithCategory>` | Retrieves top `limit` largest monetary transactions in the interval. |
| `getBiggestSingleExpenseSync()` | `TransactionEntity?` | Retrieves the single largest non-transfer DEBIT transaction in the database. |

### 6.2 `WalletDao` (`WalletDao.kt`)

| Method Signature | Return Type | Operational SQL Query / Function Behavior |
|---|---|---|
| `observeBalances()` | `Flow<List<WalletBalanceEntity>>` | Reactive stream of Cash and Online opening balance records. |
| `getBalancesSync()` | `List<WalletBalanceEntity>` | Synchronous retrieval of wallet opening balance records. |
| `getBalance(walletId: String)` | `WalletBalanceEntity?` | `SELECT * FROM wallet_balances WHERE wallet_id = :walletId LIMIT 1` |
| `insertAll(balances: List<WalletBalanceEntity>)` | `Unit` | Inserts or replaces a list of wallet balance records. |
| `insert(balance: WalletBalanceEntity)` | `Unit` | Inserts or replaces a single wallet opening balance record. |
| `updateOpeningAmount(walletId: String, amount: Double)` | `Unit` | `UPDATE wallet_balances SET opening_amount = :amount WHERE wallet_id = :walletId` |
| `deleteAll()` | `Unit` | `DELETE FROM wallet_balances` Purges all wallet records. |
| `getCount()` | `Int` | `SELECT COUNT(*) FROM wallet_balances` |

### 6.3 `BudgetDao` (`BudgetDao.kt`)

| Method Signature | Return Type | Operational SQL Query / Function Behavior |
|---|---|---|
| `getBudgetForMonth(monthKey: String)` | `Flow<BudgetEntity?>` | Reactive stream observing monthly budget for specified key (e.g. "2026-10"). |
| `getBudgetForMonthSync(monthKey: String)` | `BudgetEntity?` | Synchronous retrieval of budget record by month key. |
| `getLatestBudget()` | `Flow<BudgetEntity?>` | `SELECT * FROM budgets ORDER BY month_key DESC LIMIT 1` Most recently set budget. |
| `setBudget(budget: BudgetEntity)` | `Unit` | Inserts or replaces budget record. |
| `getAllBudgetsSync()` | `List<BudgetEntity>` | Synchronous dump of all configured monthly budgets. |
| `insertAllBudgets(budgets: List<BudgetEntity>)` | `Unit` | Batch insert or replace for budget records. |
| `deleteAllBudgets()` | `Unit` | `DELETE FROM budgets` Purges all budget records. |
| `getCategoryBudgetsForMonth(prefix: String)` | `Flow<List<BudgetEntity>>` | Stream observing category-specific sub-budgets matching `month_key LIKE :prefix`. |
| `getCategoryBudgetsForMonthSync(prefix: String)` | `List<BudgetEntity>` | Synchronous retrieval of category sub-budgets matching prefix. |

### 6.4 `CategoryDao` (`CategoryDao.kt`)

| Method Signature | Return Type | Operational SQL Query / Function Behavior |
|---|---|---|
| `getAllCategories()` | `Flow<List<CategoryEntity>>` | `SELECT * FROM categories ORDER BY id ASC` Reactive stream of categories. |
| `getAllCategoriesSync()` | `List<CategoryEntity>` | Synchronous retrieval of all categories. |
| `getCategoryByName(name: String)` | `CategoryEntity?` | `SELECT * FROM categories WHERE name = :name LIMIT 1` |
| `getCategoryById(id: Long)` | `CategoryEntity?` | `SELECT * FROM categories WHERE id = :id LIMIT 1` |
| `insertCategories(categories: List<CategoryEntity>)` | `Unit` | Batch insert or replace for category definitions. |
| `insertCategory(category: CategoryEntity)` | `Long` | Inserts or replaces category; returns primary key ID. |
| `getCategoryCount()` | `Int` | `SELECT COUNT(*) FROM categories` |
| `getMappingForCounterparty(counterparty: String)` | `CounterpartyCategoryMapEntity?` | Exact lookup in `counterparty_category_map`. |
| `setMapping(mapping: CounterpartyCategoryMapEntity)` | `Unit` | Inserts or replaces a smart merchant category mapping. |
| `getAllMappings()` | `List<CounterpartyCategoryMapEntity>` | Synchronous list of all smart merchant rules. |
| `getAllMappingsFlow()` | `Flow<List<CounterpartyCategoryMapEntity>>` | Stream of all smart merchant rules ordered alphabetically. |
| `findSmartRuleForCounterparty(counterparty: String)` | `CounterpartyCategoryMapEntity?` | Finds longest matching substring rule: `WHERE :counterparty LIKE '%' \|\| counterparty \|\| '%' ORDER BY length(counterparty) DESC LIMIT 1`. |
| `deleteMapping(counterparty: String)` | `Unit` | Deletes specific counterparty rule. |
| `insertMappings(mappings: List<CounterpartyCategoryMapEntity>)`| `Unit` | Batch insert or replace for smart merchant mappings. |
| `deleteAllMappings()` | `Unit` | `DELETE FROM counterparty_category_map` |
| `deleteAllCategories()` | `Unit` | `DELETE FROM categories` |
| `updateCategory(category: CategoryEntity)` | `Unit` | Updates category metadata (name, color, icon). |
| `deleteCategoryById(id: Long)` | `Unit` | Deletes category by ID. |
| `deleteMappingsForCategory(categoryId: Long)` | `Unit` | Cleans up rules referencing deleted category ID. |

### 6.5 `LendingDao` (`LendingDao.kt`)

| Method Signature | Return Type | Operational SQL Query / Function Behavior |
|---|---|---|
| `getAllRecords()` | `Flow<List<LendingEntity>>` | Stream of all loan records ordered by date descending. |
| `getRecordsByType(type: String)` | `Flow<List<LendingEntity>>` | Stream filtered by type ("LENT" or "BORROWED"). |
| `insert(record: LendingEntity)` | `Long` | Inserts or replaces lending entity. |
| `update(record: LendingEntity)` | `Unit` | Updates lending entity fields. |
| `delete(record: LendingEntity)` | `Unit` | Deletes lending record. |
| `setSettled(id: Long, settled: Boolean)` | `Unit` | Updates settlement status boolean. |
| `getTotalLentPending()` | `Flow<Double?>` | `SELECT SUM(amount) FROM lending_records WHERE type = 'LENT' AND is_settled = 0` |
| `getTotalBorrowedPending()` | `Flow<Double?>` | `SELECT SUM(amount) FROM lending_records WHERE type = 'BORROWED' AND is_settled = 0` |
| `getAllRecordsSync()` | `List<LendingEntity>` | Synchronous dump of all lending records. |
| `insertAll(records: List<LendingEntity>)` | `Unit` | Batch insert or replace for lending records. |
| `deleteAllRecords()` | `Unit` | `DELETE FROM lending_records` |
| `getSplitRecordsBetweenSync(start: Long, end: Long)` | `List<LendingEntity>` | Retrieves records originating from group split calculations. |
| `getRecordById(id: Long)` | `LendingEntity?` | Lookup loan record by primary key ID. |
| `getRecordByLinkedTransactionId(txId: Long)` | `LendingEntity?` | Retrieves loan record linked to a specific transaction ID. |
| `getAllRecordsWithRepayments()` | `Flow<List<LendingWithRepayments>>` | Relational stream of loan records aggregated with repayment histories. |
| `getRecordWithRepaymentsById(id: Long)` | `LendingWithRepayments?` | Relational lookup by loan ID. |
| `getRecordWithRepaymentsByLinkedTransactionId(txId: Long)` | `LendingWithRepayments?` | Relational lookup by linked transaction ID. |
| `getAllRecordsWithRepaymentsSync()` | `List<LendingWithRepayments>` | Synchronous list of all loans with repayment histories. |
| `getOpenRecordsByTypeSync(type: String)` | `List<LendingEntity>` | Retrieves unsettled loans of specified type ordered chronologically. |
| `insertRepayment(repayment: LendingRepaymentEntity)` | `Long` | Records incremental repayment installment. |
| `insertAllRepayments(repayments: List<LendingRepaymentEntity>)`| `Unit` | Batch insert or replace for repayments. |
| `deleteRepayment(repayment: LendingRepaymentEntity)` | `Unit` | Deletes single repayment installment. |
| `deleteRepaymentById(id: Long)` | `Unit` | Deletes repayment by ID. |
| `deleteRepaymentsForLending(lendingId: Long)` | `Unit` | Deletes all repayments attached to specified loan. |
| `getRepaymentsForLending(lendingId: Long)` | `List<LendingRepaymentEntity>` | Retrieves chronological repayment history for a loan. |
| `getAllRepaymentsSync()` | `List<LendingRepaymentEntity>` | Synchronous dump of all repayment installments. |
| `deleteAllRepayments()` | `Unit` | `DELETE FROM lending_repayments` |
| `updatePersonName(oldName: String, newName: String)` | `Unit` | Batch renames contact across all loan records. |
| `repayRecord(recordId, repaymentAmount, paidAt, note)` | `Result<Long>` | **Transaction method**: Validates amount <= outstanding, inserts repayment installment, deducts remaining balance, and flags `is_settled = true` if remaining balance reaches zero. |
| `settleAllRecord(recordId, paidAt, note)` | `Result<Long>` | **Transaction method**: Settles remaining outstanding balance in full by delegating to `repayRecord`. |
| `undoLastRepayment(recordId)` | `Result<Unit>` | **Transaction method**: Reverts the most recent repayment, restoring the outstanding balance and un-settling the record if previously completed. |
| `reopenLegacyRecord(recordId)` | `Result<Unit>` | **Transaction method**: Reopens a legacy settled record with no repayment history. |
| `deleteRecordWithRepayments(recordId)` | `Unit` | **Transaction method**: Deletes all associated repayments before deleting parent loan. |
| `restoreRecordWithRepayments(record, repayments)` | `Unit` | **Transaction method**: Restores loan record and all historical repayments atomically. |
| `allocatePersonRepayment(personName, type, amount, paidAt, note)`| `Result<Double>` | **Transaction method**: Allocates lump-sum repayment across multiple unsettled records for a contact in chronological order. |

### 6.6 `SavingsGoalDao` (`SavingsGoalDao.kt`)

| Method Signature | Return Type | Operational SQL Query / Function Behavior |
|---|---|---|
| `insert(goal: SavingsGoalEntity)` | `Long` | Inserts or replaces a savings goal. |
| `update(goal: SavingsGoalEntity)` | `Unit` | Updates goal metadata (title, target amount, date, color). |
| `delete(goal: SavingsGoalEntity)` | `Unit` | Deletes savings goal. |
| `getAllGoals()` | `Flow<List<SavingsGoalEntity>>` | Stream of all goals ordered by active status first, then ID descending. |
| `getAllGoalsSync()` | `List<SavingsGoalEntity>` | Synchronous retrieval of all goals. |
| `getById(id: Long)` | `SavingsGoalEntity?` | Lookup goal by ID. |
| `updateProgress(id: Long, newAmount: Double, isCompleted: Boolean)`| `Unit` | Updates current accumulated funds and completion status. |
| `getActiveGoals()` | `Flow<List<SavingsGoalEntity>>` | Stream of unsettled goals (`is_completed = 0`). |
| `getActiveGoalsSync()` | `List<SavingsGoalEntity>` | Synchronous retrieval of active goals. |
| `getCount()` | `Int` | `SELECT COUNT(*) FROM savings_goals` |
| `deleteAll()` | `Unit` | `DELETE FROM savings_goals` |
| `insertContribution(contribution: SavingsGoalContributionEntity)`| `Long` | Records incremental deposit into a savings goal. |
| `getContributionsForGoal(goalId: Long)` | `Flow<List<SavingsGoalContributionEntity>>` | Reactive stream of deposit history for specified goal. |
| `getContributionsForGoalSync(goalId: Long)` | `List<SavingsGoalContributionEntity>` | Synchronous list of deposit history for goal. |

### 6.7 `RecurringPaymentDao` (`RecurringPaymentDao.kt`)

| Method Signature | Return Type | Operational SQL Query / Function Behavior |
|---|---|---|
| `insert(recurringPayment: RecurringPaymentEntity)` | `Long` | Inserts or replaces a subscription or recurring bill. |
| `update(recurringPayment: RecurringPaymentEntity)` | `Unit` | Updates recurring payment parameters. |
| `delete(recurringPayment: RecurringPaymentEntity)` | `Unit` | Deletes recurring payment. |
| `getAllRecurringPayments()` | `Flow<List<RecurringPaymentEntity>>` | Stream of all subscriptions ordered by next due date ascending. |
| `getAllRecurringPaymentsSync()` | `List<RecurringPaymentEntity>` | Synchronous dump of all recurring payments. |
| `getDueSoon(cutoffTime: Long)` | `List<RecurringPaymentEntity>` | Retrieves active subscriptions where `next_due_timestamp <= :cutoffTime`. |
| `getById(id: Long)` | `RecurringPaymentEntity?` | Lookup subscription by ID. |
| `getCount()` | `Int` | `SELECT COUNT(*) FROM recurring_payments` |
| `deleteAll()` | `Unit` | `DELETE FROM recurring_payments` |

---

## 7. Automated Dual-Pipeline Ingestion Engine

### 7.1 SMS Ingestion Pipeline (`SmsReceiver.kt` & `BankSmsPatterns.kt`)
1. **Trigger & Privilege**: Listens to Android system broadcast `Telephony.Sms.Intents.SMS_RECEIVED_ACTION` configured with high priority (`999`) and `android.permission.BROADCAST_SMS`.
2. **PDU Assembly**: Android fragments long bank SMS into multiple Protocol Data Units (PDUs). `SmsReceiver` reassembles these PDUs into a continuous string before processing to avoid truncating reference numbers or amounts.
3. **Sender Originator Validation**:
   - The sender header is validated against Indian TRAI DLT regulations using `BankSmsPatterns.extractSenderHeader(sender)`.
   - Leading circle prefixes (e.g., `VM-`, `AD-`, `BW-`, `BZ-`) and trailing service codes (e.g., `-S`, `-T`) are stripped.
   - If the originator is a standard 10-digit mobile number, `isTrustedSender()` flags `false`.
   - Validated against 50+ banking profiles (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, Canara, Union, Federal, IDFC, Paytm Payments Bank, etc.).
   - Untrusted SMS are marked `needs_review = true` and `review_reason = "Unverified sender"`. They are quarantined and excluded from wallet balances until approved by the user.

### 7.2 Safe Bounded Parsing Engine (`SmsParser.kt`)
- **ReDoS Prevention (CWE-1333)**: All regular expressions use non-overlapping linear patterns. Text input is clamped to a maximum length of 300 characters, and regex execution runs on `Dispatchers.Default` to prevent freezing the UI thread.
- **Negative Filtering**: Messages containing OTPs, collect requests, mandate approvals, credit card promotional offers, or declined transactions are immediately rejected via `isNonTransactional(body)`.
- **Classification Engine**:
  - Differentiates Debits vs Credits vs Refunds vs ATM withdrawals.
  - Resolves directional ambiguity when messages mention both debit and credit (e.g., *"Debited from A/C ... and credited to VPA swiggy@hdfc"* is classified as a DEBIT).
- **Balance & Limit Isolation**: Matches and isolates available balance patterns (`avl bal`, `a/c bal`, `available limit`) before extracting the primary transaction amount, ensuring the account balance is never mistaken for the expense amount.
- **Merchant Cleaning (`BrandFormatter.kt`)**: Cleans raw gateway strings (e.g., `UPI/CR/425612/SWIGGY/HDFC` → `Swiggy`; `POS 421598 DMART MUMBAI` → `DMart`).

### 7.3 Notification Ingestion Pipeline (`KanriNotificationListenerService.kt`)
1. **Signature Verification (CWE-346)**: Validates incoming notification packages against allowlisted payment apps (Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, FamPay, CRED). Verifies package signing certificates via `PackageManager.getPackageInfo(GET_SIGNING_CERTIFICATES)` to prevent spoofed notification attacks.
2. **Credit-Only Filter**: Scoped **exclusively to CREDIT transactions** (peer payments received, merchant cashbacks, refunds). Debits from notifications are ignored because the bank SMS serves as the authoritative ledger record.

### 7.4 Sliding-Window Deduplication Engine (`NotificationDeduplicationHelper.kt`)
- Matches transactions within a 5-minute sliding window (`DEDUPE_WINDOW_MS = 300_000L`).
- **Match Criteria**: Identical transaction type + identical amount + timestamp within $\pm 5$ minutes OR identical exact `refNo` (UTR).
- **Merge Logic**:
  - If a notification arrived first, a subsequent bank SMS enriches the authentic bank code, account mask, UTR, and raw SMS payload.
  - If a bank SMS arrived first, a subsequent notification enriches the clean counterparty name.

---

## 8. Dual-Wallet Subsystem (Cash vs. Online)

### 8.1 Architectural Model
Money is tracked across two liquidity pools:
1. **Cash Wallet**: Physical cash in hand.
2. **Online Wallet**: Digital accounts (Bank A/Cs, UPI balances, Credit Cards).

### 8.2 Balance Calculation Formula (`WalletBalanceCalculator.kt`)
Balances are computed dynamically by combining configured opening balances with confirmed, non-duplicate ledger records (`is_duplicate = 0 AND (needs_review = 0 OR is_manual_entry = 1)`):

$$\text{Cash Balance} = \text{Cash Opening} + \sum \text{Cash Credits} - \sum \text{Cash Debits} + \sum \text{Transfers to Cash} - \sum \text{Transfers from Cash}$$

$$\text{Online Balance} = \text{Online Opening} + \sum \text{Online Credits} - \sum \text{Online Debits} + \sum \text{Transfers to Online} - \sum \text{Transfers from Online}$$

$$\text{Total Balance} = \text{Cash Balance} + \text{Online Balance}$$

Unreviewed transactions (`needs_review = 1`) are excluded from wallet balance calculations to protect balance integrity against untrusted SMS spoofing.

### 8.3 ATM Withdrawal State Machine (`WalletResolver.kt`)
When an ATM withdrawal alert is ingested:
- **`SPENDING` Mode**: Treated as an immediate expense categorized under `Cash & ATM`. The Online wallet is debited, and the Cash wallet is unchanged.
- **`WALLET_TRANSFER` Mode**: Recorded as an internal transfer. ₹X is deducted from the Online wallet and ₹X is credited to the Cash wallet. Monthly expense totals are not inflated.

---

## 9. Budgeting, Analytics & Financial Intelligence Engines

### 9.1 Monthly Budget & Safe-to-Spend (`BudgetCalculator.kt`)
- Tracks month-to-date spending against the user-configured monthly budget limit.
- **Safe-to-Spend Per Day**:

$$\text{Safe-to-Spend} = \frac{\text{Monthly Limit} - \text{Month-to-Date Spend}}{\text{Days Left in Month}}$$

### 9.2 No-Spend Streak Engine (`StreakCalculator.kt`)
- Tracks consecutive days with zero non-transfer debit expenses.
- Loops backward day-by-day from today.
- **Bounded Safety (CWE-834)**: Loop iterations are clamped to a 5-year maximum lookback limit with safety counters to prevent UI thread hangs if corrupted historical timestamps exist.

### 9.3 100-Point Financial Health Index (`FinancialHealthCalculator.kt`)
Evaluates financial stability across 4 mathematical pillars:
1. **Savings Pillar (30 pts)**: Evaluates monthly savings rate:

$$\text{Savings Rate} = \frac{\text{Income} - \text{Spend}}{\text{Income}} \times 100$$

2. **Budget Pillar (30 pts)**: Ratio of actual spend to allocated budget, adjusted for days elapsed in the month.
3. **Velocity Pillar (20 pts)**: Compares daily spending burn rate against the sustainable daily budget allocation.
4. **Debt Pillar (20 pts)**: Evaluates debt-to-income exposure based on net outstanding borrowing versus total receivables.

### 9.4 Pacing Status & Health Grades
- **Grades**: $A+$ (90–100, Exceptional), $A$ (80–89, Healthy), $B$ (65–79, Balanced), $C$ (50–64, Caution), $D$ (<50, At Risk).
- **Pacing**: `UNDER_PACING` (Frugal), `ON_TRACK` (Aligned), `ACCELERATED` (Fast burn), `CRITICAL_BURN` (Over budget).

---

## 10. Lending & Debt Hub Subsystem

### 10.1 Debt Tracking Engine (`LendingMoneyEngine.kt`)
- **Lent**: Money owed *to* the user (receivables).
- **Borrowed**: Money the user *owes* to others (payables).
- **Net Position**: $\sum \text{Pending Lent} - \sum \text{Pending Borrowed}$.

### 10.2 Settlement Workflows
- **Partial Repayments**: Logs incremental payments (`LendingRepaymentEntity`) with timestamps and optional notes, deducting from the loan's outstanding amount.
- **Batch Settlement (`settleAllRecord`)**: Marks all open loans for a specific contact as settled in a single database transaction.
- **Undo Repayment (`undoLastRepayment`)**: Restores previous balances and un-settles records if a payment was logged accidentally.
- **UPI Deep-Linking (`LendingUpiHelper.kt`)**: Generates RFC-compliant UPI payment intent URLs launching installed payment apps with prefilled VPA, amount, and reference notes:
  `upi://pay?pa=recipient@upi&pn=Name&am=500.00&cu=INR&tn=Debt Settlement via Kanri`

---

## 11. Savings Goals & Recurring Subscriptions

### 11.1 Savings Goals (`SavingsGoalCalculator.kt`)
- Tracks target savings goals with target amounts, target dates, and custom color badges.
- **Incremental Contributions**: Deposits can be linked to either Cash or Online wallets.
- **Progress Gauge**: Calculates completion percentage and indicates whether the user is on pace to meet the target date based on remaining days.

### 11.2 Recurring Subscriptions (`RecurringPaymentCalculator.kt`)
- Tracks recurring commitments across four frequencies: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`.
- Computes upcoming due dates and project recurring monthly overhead.
- Posts notification alerts ahead of upcoming subscription deductions.

---

## 12. Cryptographic Security, Backups & Cloud Sync

### 12.1 Local Encrypted Backups (`BackupCryptoHelper.kt`)
- Exports all database tables into a structured JSON document.
- **Key Derivation**: Uses **PBKDF2WithHmacSHA256** with **65,536 iterations** and a 128-bit (16-byte) random salt.
- **Cipher**: **AES-256 GCM** with a 128-bit authentication tag and a 96-bit (12-byte) random IV.
- **Container Structure**: Prepend magic header bytes `KANRI_ENC_v1` followed by salt (16 bytes), IV (12 bytes), and the GCM ciphertext payload.
- **Safe Merge Restore**: Restores data without deleting existing device records by default (`clearExisting = false`), preventing accidental data loss during imports.

### 12.2 Google Drive Cloud Sync (`CloudBackupManager.kt`)
- Uses user-scoped OAuth tokens (`https://www.googleapis.com/auth/drive.file`).
- Backups are encrypted before upload. The encryption passphrase incorporates a 256-bit CSPRNG device secret stored in `EncryptedSharedPreferences` (`getOrCreateDeviceSecret`).
- Plaintext cloud restores are strictly rejected to ensure file integrity.

### 12.3 Application Lock & Session Lifecycle (`SecurityManager.kt`)
- Supports Fingerprint, Face Unlock, and device PIN via AndroidX `BiometricPrompt`.
- **Session Expiry (CWE-613)**: Invalidates session state on `onAppBackgrounded()`, enforcing immediate authentication when the app is resumed.
- **Screen Off Receiver**: Registers an `ACTION_SCREEN_OFF` broadcast receiver that immediately locks the session when the physical device screen turns off.
- **Temporary Exemption**: Scoped strictly to system document pickers (`isTemporarilyExempt = true`), preventing accidental session preservation when navigating outside the app.

---

## 13. Document Export Engine (Vector PDF & CSV)

### 13.1 Vector PDF Statements (`PdfStatementExporter.kt`)
- Uses native `android.graphics.pdf.PdfDocument`.
- **Clean Minimal Design & Official Kanri Logo**: Editorial letterhead styling with high-resolution vector extraction of the authentic `KANRI` Panchang wordmark (`R.drawable.panchang_dark`), sharp hairline rules (`#E2E8F0`), and multi-page pagination.
- **Dynamic Date Ranges**: Supports full custom date ranges from any start date to any end date chosen via Material 3 `DateRangePicker` in `KanriGlassDatePickerDialog`.
- **Currency-Aware**: Formats all transactions, executive summary cards, and category distributions using the user's primary currency (`CurrencyUtils.formatCurrency`).
- Renders in under 500ms on-device without external dependencies or cloud conversion APIs.

### 13.2 RFC 4180 CSV Statements (`CsvExporter.kt`)
- Serializes transactions to standard CSV format.
- **Formula Neutralization (CWE-1236)**: Neutralizes all leading spreadsheet formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`) by prepending a single quote (`'`), preventing CSV formula injection attacks when opened in spreadsheet software.

### 13.3 Automated Scheduled Statements (`ScheduledExportWorker.kt`)
- WorkManager periodic job running on the 1st of every month at 08:00 AM.
- Saves reports to private internal storage (`filesDir/auto_exports`), isolated from other applications (CWE-312).
- Delivers statements via system notifications using secure `FileProvider` URIs.

---

## 14. Comprehensive UI Screen & Component Catalog

### 14.1 Navigation Subsystem
- **`MainNavigationScreen.kt`**: Root container hosting the `NavHost` and managing bottom navigation state, backstack routing, deep-link handling, and root `HazeState` backdrop source propagated via `LocalHazeState`.
- **`KanriFloatingDock.kt`**: Floating glassmorphic dock with icon-only selection highlighting (no rectangular or circular selection indicator). Selected tab icons are rendered at full opacity; unselected icons are dimmed. Smooth animated transitions between Home, Month, Insights, Lending, and Goals.
- **`AddExpenseIncomePopup.kt`**: Floating action sheet for rapid entry of expenses, income, split bills, and transfers. Uses native window blur backdrop.

### 14.2 Home Tab
- **`HomeScreen.kt`**: Main dashboard containing balance cards, greeting headers, streak pills, and recent transaction feeds.
- **`HomeViewModel.kt`**: Coordinates data streams between DAOs, analytics calculators, and UI states.
- **`CashFlowCard.kt`**: Visual card displaying month-to-date income, expenses, and net cash flow.
- **`FinancialHealthCard.kt`**: Scorecard displaying the overall 100-point Financial Health score and grade.
- **`NoSpendStreakCard.kt`**: Displays the active no-spend streak count with weekly dot status indicators.
- **`ExpenseVisualizer.kt`**: Segmented bar displaying proportional category spending distributions with full glassmorphic rendering.
- **`NeedsReviewCard.kt`**: Alert card indicating unverified or uncategorized transactions requiring review.
- **`SetBudgetDialog.kt`**: Glass dialog for configuring the monthly budget, using `kanriPrimaryButtonColors()` to ensure high-contrast button text.
- **`AddTransactionDialog.kt`**: Glass dialog for manual entry of expenses, income, and wallet transfers with frosted backdrop blur.
- **`TransactionDetailScreen.kt`**: Full-screen glass inspector for transaction metadata, raw SMS payloads, and notes with native window blur background.

### 14.3 Insights Tab
- **`InsightsViewModel.kt`**: Computes analytical aggregations, category shifts, daily spending burn rates, and forecasts.
- **`InsightsRangeBar.kt`**: Segmented control switching time ranges between Month, Quarter, Year, and Custom.
- **`BudgetVsActualSection.kt`**: Compares budget limits against actual spend.
- **`CumulativeSpendLineSection.kt`**: Smooth line chart displaying cumulative spending trajectory across the billing cycle.
- **`CategoryMoversSection.kt`**: Highlights categories with significant spending increases or decreases.
- **`TopPayeesSection.kt`**: Ranked list of top counterparties by total expenditure.
- **`WeekendVsWeekdaySection.kt`**: Compares average weekday spending with weekend spending.
- **`YearHeatmapSection.kt`**: 365-day annual spending activity heatmap.
- **`CompareMonthsScreen.kt`**: Side-by-side comparison screen between two calendar months.
- **`PayeeDetailScreen.kt`**: Inspector screen focusing on transactions with a specific merchant.

### 14.4 Lending & Debt Hub
- **`LendingScreen.kt`**: Tabbed interface switching between Contact-Centric (People) and Chronological (Timeline) views. The "+ Add Record" button uses full glassmorphic styling.
- **`LendingHeroCard.kt`**: Summary card displaying Net Position, Total Lent, and Total Borrowed.
- **`LendingPeopleView.kt`**: Contact-centric list grouping loans by individual people with glass filter chips.
- **`LendingPersonDetailView.kt`**: Detailed view displaying all historical transactions and open balances with a contact.
- **`LendingEntryDialog.kt`**: Combined entry glass dialog for single and split group debts with frosted window blur.
- **`LendingRepaymentDialog.kt`**: Glass dialog to log partial or complete loan repayments.
- **`LendingUpiPayDialog.kt`**: Launches external UPI apps with prefilled VPA and amount for instant settlement.
- **`SplitExpenseScreen.kt`**: Bill-splitting calculator dividing shared bills among participants with custom split ratios.

### 14.5 Month Ledger Tab
- **`MonthViewScreen.kt`**: Full historical monthly ledger displaying day-by-day transaction breakdowns.
- **`CategoryDonutChart.kt`**: Frosted monochrome donut chart displaying category proportions.
- **`DailySpendBarChart.kt`**: Daily spend bar chart showing spending distribution across the calendar month.
- **`MultiMonthTrendsChart.kt`**: Bar chart comparing spending across consecutive calendar months.

### 14.6 Savings Goals & Subscriptions
- **`GoalsScreen.kt`**: Dashboard displaying active and completed savings goals.
- **`CircularGoalProgressGauge.kt`**: Circular progress ring indicating savings goal completion.
- **`RecurringPaymentsSheet.kt`**: Bottom sheet managing recurring subscriptions, rent, and utility bills.

### 14.7 Profile, Settings & Security
- **`ProfileScreen.kt`**: User profile management, avatar customization, and encrypted backup exports/imports. All edit dialogs use frosted glass with window blur.
- **`SettingsScreen.kt`**: App settings: theme toggles, currency selection, cloud sync settings, and factory resets. Currency picker, export sheet, and profile editor use `KanriGlassDialog` / `KanriModalBottomSheet`.
- **`SmartMerchantRulesScreen.kt`**: Manager interface for automated merchant keyword-to-category rules.
- **`LockScreen.kt`**: Biometric and PIN authentication challenge screen with frosted glass card surface.

### 14.8 Frosted Glass & Dynamic Theming Subsystem

The Frosted Glass subsystem in Kanri is an end-to-end, multi-layered visual architecture providing GPU-accelerated glassmorphism, mathematical color contrast adaptation, ambient WebGL/Canvas backdrop shaders, and a real-time optical customization engine.

#### 14.8.1 Glass Level System (`KanriGlass.kt`)
Three hierarchy tiers control the visual intensity and rendering performance of glassmorphism:

| Level | Target Usage | Blur Strategy | Tint Fill | Shadow & Border |
|---|---|---|---|---|
| `GlassLevel.Blur` | Primary cards, dashboard widgets, floating navigation dock | Live GPU backdrop blur via Haze (`hazeEffect`) | `tintBlur` from `KanriGlassTokens` | 4dp elevation shadow + luminous top-down gradient border |
| `GlassLevel.Flat` | High-frequency list rows, category chips, inline badges | Blur disabled (zero GPU overhead in scrollable lists) | `tintFlat` translucent fill | No shadow + subtle border hairline |
| `GlassLevel.Strong` | Modal dialogs, bottom sheets, context popups | Enhanced blur (1.45× blur radius, min 36dp) | `tintStrong` with white frost overlay | 8dp elevation shadow + high-contrast rim |

#### 14.8.2 Core Composable Primitives (`KanriGlass.kt`)

| Component | Responsibility & Implementation |
|---|---|
| `Modifier.kanriGlass()` | Unified modifier binding Haze blur, surface tint, shape clipping, gradient border brush, and elevation shadow according to `GlassLevel`. Integrates spring-animated press scale (`animateFloatAsState`) and haptic feedback on click. Gracefully falls back to solid `surfaceContainer` colors when non-glass themes are active. |
| `KanriGlassCard` | Box container wrapper applying `Modifier.kanriGlass()`. Accepts custom shapes, padding, levels, and optional tap handlers. |
| `KanriGlassPill` | Circular glass capsule for inline tags, streak badges, and status pills. |
| `KanriGlassButton` | Frosted glass interactive button with spring scale-down animation, tactile click haptics, and luminous border highlight. Provides text and icon overloads. |
| `KanriGlassDialog` | Full-screen modal dialog wrapper applying native Android 12+ `FLAG_BLUR_BEHIND` window blur, dim scrim, and `GlassLevel.Strong` card surface. |
| `KanriGlassAlertDialog` | Drop-in glass replacement for Material3 `AlertDialog` featuring title, text, icon slots, and confirm/dismiss button rows with frosted surfaces. |
| `KanriGlassDatePickerDialog` | Frosted replacement for `DatePickerDialog` wrapping Material3 date pickers with window blur and frosted surfaces. |
| `KanriModalBottomSheet` | Edge-to-edge frosted `ModalBottomSheet` with transparent sheet container, native window blur, ergonomic drag handle, and automated navigation bar insets. |
| `KanriDropdownMenu` | Frosted glass popup menu with live backdrop blur, rounded corners, and gradient border. |
| `kanriPrimaryButtonColors()` | Specialized `ButtonColors` factory preventing white-on-white text glitches in glass mode. Returns light container + dark text (inverted for light themes) ensuring contrast ratio compliance across all glass lightness levels. |

#### 14.8.3 Native Window Blur Engine
- **Platform Integration (`FLAG_BLUR_BEHIND`)**: On Android 12+ (API level 31+), dialogs and bottom sheets inject `WindowManager.LayoutParams.FLAG_BLUR_BEHIND` and invoke `Window.setBlurBehindRadius(clampedRadius)` with radii clamped between 50px and 160px.
- **Compose View Hierarchy Traversal (`findDialogWindow()`)**: Traverses the Compose internal view tree and unwraps the `ContextWrapper` chain to locate the underlying platform `Window` hosting the dialog or popup.
- **Low-API & OEM Fallback**: On Android versions below 12, or on devices where hardware blur is restricted, the window blur call is omitted cleanly and visual contrast is maintained via semi-opaque tinted scrims.

#### 14.8.4 Glass Design Tokens & Mathematical Color Engine (`KanriGlassTokens.kt`)
Glass aesthetics are driven by immutable `KanriGlassTokens` propagated through Compose via `LocalKanriGlass`:

- **Backdrop Tokens**: `backdropGradient` (vertical multi-stop brush), `blobs` (list of `GlassBlob` ambient radial glows with fraction-based offsets and blur radii), and `grainAlpha` (analogue noise density).
- **Surface & Rim Tokens**: `blurRadius` (dp), `tintBlur` (fill over blur), `tintFlat` (fill without blur), `tintStrong` (transient surface fill), `fallbackSurface` (no-blur solid fallback), `borderStart` / `borderEnd` (gradient border stops), `edgeHairline`, and `shadow` (elevation shadow color).
- **Content & Semantic Tokens**: `textPrimary`, `textSecondary`, `textTertiary`, `accent`, `onAccent`, `credit` (settled/income), `debit` (due/expense), `track` (gauge tracks), `expenseColor`, `incomeColor`, `expenseTintOpacity`, and `incomeTintOpacity`.
- **Dynamic Luminance Compensation (`computeAdaptedAccentColor`)**:
  When users adjust `tintLightness` from `-1.0` (deep smoky obsidian) to `+1.0` (pristine luminous white frost), semantic colors dynamically scale their RGB luminance. Under dark tints, colors shift toward deep, saturated jewel tones; under light frost tints, colors boost red/green luminance to prevent wash-out against bright frosted backgrounds.
- **Parametric Token Derivation (`createCustomGlassTokens`)**: Computes real-time tokens by lerping between dark obsidian (`#0F0F14`) and pure white frost based on user configuration.

#### 14.8.5 Curated Optical Presets (`GlassPresets`)
Kanri includes 5 hand-tuned optical presets catering to different aesthetic tastes and hardware capabilities:

| Preset Name | Blur Radius | Surface Opacity | Tint Lightness | Border Opacity / Width | Button Opacity | Semantic Palette | Character |
|---|---|---|---|---|---|---|---|
| **Default** | 24dp | 8% | 0.0 (Neutral) | 28% / 1.0dp | 16% | Vivid Red (`#E54D2E`) / Emerald (`#30A46C`) | Balanced contemporary frosted glass |
| **Ultra Sheer** | 36dp | 3% | +0.15 (Slight Frost) | 16% / 0.8dp | 8% | Rose (`#F43F5E`) / Mint (`#10B981`) | Ethereal, hyper-translucent, liquid glass |
| **Deep Obsidian** | 20dp | 20% | -0.75 (Smoky Dark) | 35% / 1.0dp | 22% | Crimson (`#DC2626`) / Deep Teal (`#059669`) | High-contrast, dark tinted obsidian glass |
| **Luminous Frost** | 28dp | 16% | +0.70 (Crisp Ice) | 42% / 1.2dp | 24% | Coral (`#FB7185`) / Pastel Green (`#34D399`) | Bright, illuminated, crystalline frosty glass |
| **Neon Crystal** | 14dp | 5% | 0.0 (Neutral) | 58% / 1.6dp | 32% | Neon Hot Pink (`#FF3366`) / Lime (`#84CC16`) | Cyberpunk aesthetic with glowing luminous rims |

#### 14.8.6 Ambient Shader Backdrop System (`GlassBackdrop.kt` & `GlassBackgroundView.kt`)
Behind all frosted surfaces runs a 3-layer visual backdrop composition:

```
+-------------------------------------------------------+
|  Layer 3: Procedural Micro-Grain Overlay              |  Cached 64x64 ARGB_8888 ImageShader (analogue texture)
+-------------------------------------------------------+
|  Layer 2: Translucent Tone-Balancing Scrim            |  20% Black (dark glass) / 30% White (light glass)
+-------------------------------------------------------+
|  Layer 1: Hardware-Accelerated WebGL/Canvas Shader    |  AndroidView WebView running non-interactive HTML/CSS/JS
+-------------------------------------------------------+
```

##### 22 Dynamic Background Shaders (`GlassBackgroundType`)
Users can choose between 22 hardware-accelerated animated backgrounds loaded from `android_asset/backgrounds/`:

1. **Gemini Aurora (`aurora.html`)**: Mobile Google Gemini neural aurora featuring simultaneous multi-color fluid orbs smoothly morphing between Cycle 1 (`#7cff67`, `#22d3c5`, `#8f5cff`) and Cycle 2 (`#ff8a5c`, `#ff5c8a`, `#8a5cff`) across the screen.
2. **Grain Gradient (`grain_gradient.html`)**: Diffuse multi-point color blurs with heavy organic analogue noise.
3. **Hero Geometric (`hero_geometric.html`)**: Futuristic isometric geometric planes and floating translucent polygons.
4. **Prism Swirl (`prism_gradient.html`)**: High-saturation chromatic dispersion swirls with fluid motion.
5. **Cloudscape (`cloud_shader.html`)**: Volumetric procedural cloud density drift shaders.
6. **Ink Ridges (`ink_ridges.html`)**: Topographic contour lines with pulsing fluid interference patterns.
7. **Fuji & Sakura (`fuji_sakura.html`)**: 9:16 pixel hanging scroll artwork with Mt. Fuji, golden mist, pagoda, torii gate, koi fish, cranes, cat, and cherry blossom physics.
8. **Sumi-e Mist (`sumi_e_mist.html`)**: Traditional monochrome ink wash painting with drifting watercolor mist.
9. **Thistle Portico (`thistle_portico.html`)**: Gothic stone portico in wisteria bloom with castle vista & drifting petals.
10. **Still Sunset Meadow (`sunset_meadow.html`)**: 3D voxel meadow with interactive grass, voxel flora & dynamic photorealistic 24-hour day/sunset/night cycle driven by local time.
11. **Twilight Meadow (`twilight_meadow.html`)**: Pixel art twilight landscape: crescent moon, jagged alpine peaks reflected in a still mountain lake, and sloping meadow with purple wildflower asters.
12. **Tokyo Night (`tokyo_night.html`)**: Pixel art Tokyo night skyline vista with mountain bench, companion cat, neon city lights, and drifting petals.
13. **Pixel Kitchen (`pixel_kitchen.html`)**: Cozy 24-hour pixel kitchen with warm kettle, day/night window lighting & subtle steam.
14. **Snowy Mountain Road (`snowy_mountain_road.html`)**: Atmospheric 24-hour winter mountain road with drifting snow, headlight beams & real-time sky.
15. **Sunlit Balcony (`sunlit_balcony.html`)**: Quiet 24-hour sunlit balcony with potted plants, gentle breeze & evening fireflies.
16. **Alpine Valley (`alpine_valley.html`)**: Animated 9:16 pixel art mountain valley with winding river, wildflower meadow & soaring birds.
17. **Shiganshina Windmill (`shiganshina_windmill.html`)**: 4-bit animated pixel art windmill with calm wind gusts & 24-hour dawn-to-night lighting cycle.
18. **Chibi Coffee Shop (`chibi_coffee_shop.html`)**: Cozy chibi cafe window on a rainy evening with steaming mugs, warm lanterns & cat companion.
19. **Fuji Torii Gate (`fuji_torii.html`)**: Japanese vermilion torii gate before Mount Fuji with drifting sakura petals & glowing valley mist.
20. **Cherry Blossom Canyon (`cherry_canyon.html`)**: 3D voxel cherry blossom canyon with drifting petals, celestial day/sunset/night traversal & volumetric clouds.
21. **Pixel Misty Alley (`pixel_misty_alley.html`)**: Atmospheric 24-hour neon city alley with gentle rain, companion cat & dynamic sun/moon cycle.
22. **Glowing Bamboo Forest (`glowing_bamboo_forest.html`)**: Steam train crossing a bridge through swaying bamboo with glowing lanterns & 24-hour atmosphere.

##### Lifecycle & Performance Safeguards
- **Lifecycle Awareness**: Observes `LocalLifecycleOwner`. The underlying `WebView` automatically invokes `onPause()` when Kanri enters the background or the screen turns off, suspending WebGL/requestAnimationFrame execution to achieve **zero background CPU/battery consumption**. Calls `onResume()` when returning to the foreground.
- **Hardware Layering**: `WebView` runs with `LAYER_TYPE_HARDWARE` and `WebSettings.LOAD_NO_CACHE`.
- **Gesture Passthrough**: `setOnTouchListener { _, _ -> true }` ensures the backdrop never intercepts touch, scroll, or swipe gestures intended for the UI.

#### 14.8.7 Hardware Capability & Accessibility Engine (`GlassCapabilities.kt`)
To guarantee fluid 60/120 FPS performance across diverse Android hardware tiers, Kanri dynamically inspects device capabilities before activating GPU shaders and blurs:

1. **Android 12+ API Check**: `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` (API 31) required for platform RenderEffect / window blur.
2. **Low-RAM Device Gating**: Queries `ActivityManager.isLowRamDevice`. On Android Go or low-memory devices, heavy blurs and shader animations are disabled automatically.
3. **Power Save Mode Gating**: Queries `PowerManager.isPowerSaveMode`. If battery saver is active, live blurs and animated WebGL shaders pause immediately to conserve power.
4. **Reduce Transparency Accessibility Toggle**: User preference in Settings allowing users sensitive to translucency to replace glass surfaces with opaque tinted surfaces.
5. **Reduced Motion Compliance**: Checks system `Settings.Global.ANIMATOR_DURATION_SCALE > 0f`. If animations are disabled in Android accessibility settings, animated WebGL backgrounds fall back to a static vertical gradient (`#0F172A` to `#020617`).

#### 14.8.8 Real-Time Glass Customizer Screen (`GlassCustomizerScreen.kt`)
Accessible directly from Settings, the Glass Customizer allows users to fine-tune optical physics with live visual feedback:

- **Interactive Preview Card**: Anchored at the top with live `HazeState` backdrop blur, showing simulated balance numbers, transaction badges, and glass buttons reacting in real time to slider adjustments.
- **Preset Switcher Carousel**: Horizontal pill bar to switch between Default, Ultra Sheer, Deep Obsidian, Luminous Frost, and Neon Crystal with tactile haptic clicks.
- **Granular Optical Sliders**:
  - *Blur Radius*: `0dp` (flat transparent) to `60dp` (deep frosted diffusion).
  - *Surface Opacity*: `1%` to `60%`.
  - *Tint Lightness*: `-1.0` (deep smoky dark) to `+1.0` (pristine luminous white).
  - *Border Opacity & Width*: `0%` to `85%` opacity, `0.5dp` to `4.0dp` width.
  - *Corner Radius*: `8dp` to `40dp`.
  - *Button Opacity*: `4%` to `50%`.
  - *Shadow Intensity*: `0%` to `70%`.
  - *Expense / Income Custom Colors & Tint Opacity*: Custom hex color picker and tint fill opacity sliders.
- **Reactive Persistence**: Adjustments write immediately to `ThemePreferences`, propagating instantly across all screens via Kotlin StateFlows without requiring an app restart.
- **Factory Reset**: Header action button to restore defaults at any time.

#### 14.8.9 Theming Pipeline Integration (`Theme.kt` & `ThemePreferences.kt`)
When `ThemeMode.GLASS` is active:
- `KanriTheme` sets `background = Color.Transparent` in the Material3 `darkColorScheme`, allowing the ambient `GlassBackdrop` to show through.
- Surface containers (`surfaceContainer`, `surfaceContainerHigh`, `surfaceVariant`) are mapped directly to `tintBlur`, `tintStrong`, and `tintFlat`.
- System status bar and navigation bar insets are configured via `WindowCompat.getInsetsController(window, view)` with dark content icons.
- `LocalKanriGlass` provides the active token instance to any composable in the composition hierarchy.

---

## 15. Home Screen Widgets Subsystem

Kanri includes 8 home screen widgets designed with a **monochrome, minimal glassmorphism aesthetic**:

| Widget Name | Size | Provider Class | Description |
|---|---|---|---|
| **Cash & Online Balance** | 4x2 | `WalletBalanceWidgetProvider` | Dual-column display showing Cash, Online, and Total balances with a one-tap deep link to the wallet manager. |
| **Spent Today** | 2x1 | `MoneySpentTodayWidgetProvider` | Daily spending monitor that automatically resets at midnight via `MidnightWidgetResetWorker`. |
| **Budget Progress** | 4x1 | `BudgetProgressWidgetProvider` | Horizontal progress bar displaying monthly percentage used, remaining budget, and daily safe spend. |
| **Budget Ring** | 2x2 | `BudgetRingWidgetProvider` | Frosted circular ring chart drawn via Canvas with remaining budget centered. |
| **Savings Goal Ring** | 2x2 | `GoalsRingWidgetProvider` | Circular Canvas progress gauge tracking the primary savings goal with bold progress indicators. |
| **Lend & Borrow** | 4x2 | `LendBorrowWidgetProvider` | Overview of pending receivables and payables with due dates and overdue flags. |
| **Quick Record** | 3x1 | `QuickAddWidgetProvider` | Instant one-tap shortcut buttons to record Expenses or Income. |
| **Split the Bill** | 3x1 | `SplitBillWidgetProvider` | Rapid group bill-splitting calculator launcher. |

### 15.1 OEM Launcher Preview Compatibility
Widget pickers on certain OEM operating systems (Infinix XOS, Xiaomi/Redmi HyperOS, Oppo ColorOS, Realme UI) fail to render previews when referencing `<adaptive-icon>` drawables. Kanri resolves this across all 8 widgets by using dedicated standalone vector drawables (`res/drawable/preview_widget_*.xml`) with explicit dp dimensions.

### 15.2 RemoteViews Inflation Compatibility
RemoteViews supports only an allowlisted subset of Android views. Generic `<View>` elements trigger `InflateException: Class not allowed to be inflated android.view.View`, causing launcher errors. Kanri uses `<FrameLayout>` for all divider lines and card borders to ensure reliable layout inflation across all Android versions.

### 15.3 Vector Canvas Graphics (`WidgetCanvasRenderer.kt`)
Circular gauges and progress rings are drawn dynamically onto in-memory Bitmaps using Android `Canvas` and `Paint` with anti-aliasing. Rendered rings use pure white tones (`#FFFFFF`, `#33FFFFFF`) and Google Sans typography.

### 15.4 Invalidation & Action Dispatching
- **Central Coordinator (`KanriWidgetsUpdater.kt`)**: Updates all active widgets simultaneously whenever transactions are added, edited, or deleted.
- **Midnight Cron (`MidnightWidgetResetWorker.kt`)**: Periodic WorkManager job resetting the "Spent Today" counter at midnight.
- **Deep-Link Action Routing (`KanriWidgetActions.kt`)**: Widget click events broadcast custom pending intents (`com.omkarnub.kanri.action.*`) routed through `MainNavigationScreen` to open specific features (e.g., Quick Add, Split Bill, Wallet Manager).

---

## 16. Background Workers, Services & Broadcast Receivers

| Component | Type | Responsibility & Operational Details |
|---|---|---|
| `BootReceiver.kt` | `BroadcastReceiver` | Listens to `ACTION_BOOT_COMPLETED`. Reschedules periodic watchdog workers, rebinds notification services, and refreshes home screen widgets after device restart. |
| `SmsReceiver.kt` | `BroadcastReceiver` | Listens to `SMS_RECEIVED_ACTION`. Reassembles SMS PDUs and extracts banking transaction details asynchronously via `goAsync()` on `Dispatchers.IO`. |
| `QuickAddTileService.kt` | `TileService` | Quick Settings pull-down tile allowing instant transaction creation from anywhere in the OS. |
| `KanriNotificationListenerService.kt`| `NotificationListenerService` | Captures incoming credit notifications from allowlisted payment apps with publisher certificate verification. |
| `NotificationListenerWatchdogWorker.kt`| `CoroutineWorker` | 15-minute watchdog job verifying whether `KanriNotificationListenerService` is actively bound, rebinding if disconnected by OEM battery savers. |
| `ScheduledExportWorker.kt` | `CoroutineWorker` | Generates monthly PDF and CSV statements on the 1st of every month at 08:00 AM, saving files to private internal app storage (`filesDir/auto_exports`). |
| `DailyReminderWorker.kt` | `CoroutineWorker` | Runs daily at 9:00 PM to alert users about uncategorized transactions or review items. |
| `LendingReminderWorker.kt` | `CoroutineWorker` | Evaluates approaching and overdue loan due dates, delivering alert notifications. |
| `CloudBackupWorker.kt` | `CoroutineWorker` | Runs periodic 24-hour background cloud backups when connected to an unmetered network. |
| `MidnightWidgetResetWorker.kt` | `CoroutineWorker` | Runs daily at midnight to reset the "Spent Today" widget counter. |
| `AppUpdateWorker.kt` | `CoroutineWorker` | Checks GitHub Releases daily for new app updates and posts update notifications. |
| `InstantPopupService.kt` | `Service` | Renders floating transaction confirmation overlays when configured by the user. |
| `InstantPopupReceiver.kt` | `BroadcastReceiver` | Handles inline categorization actions from transaction notifications. |

---

## 17. Security Hardening & Vulnerability Mitigation Matrix

| Security / Reliability Finding | CWE Reference | Threat Model | Mitigation Implemented in Kanri |
|---|---|---|---|
| **Account Regex ReDoS** | **CWE-1333** | Catastrophic backtracking in nested whitespace repetitions freezing the UI thread. | Replaced overlapping quantifier patterns in `ACCOUNT_PATTERN`, capped input to 300 characters, and offloaded execution to `Dispatchers.Default`. |
| **Notification Regex ReDoS** | **CWE-1333** | Overlapping repetitions in notification sender extraction causing callback hang. | Refactored `SENDER_PATTERNS` to non-overlapping linear expressions with 500-character input bounds. |
| **CSV Formula Injection** | **CWE-1236** | Crafted payee names beginning with `=CMD` or `+` executing code when exported to Excel. | In `CsvExporter.kt`, all formula-leading characters (`=`, `+`, `-`, `@`, `\t`, `\r`) are prepended with an apostrophe (`'`). |
| **Plaintext Backup Export** | **CWE-312** | Export Data action releasing financial history and raw SMS in plaintext. | Enforced mandatory passphrase input dialog and **AES-256 GCM** encryption before opening destination output streams in `BackupRepository.kt`. |
| **App Lock Session Bypass** | **CWE-613** | Reopening the app from recents without biometric challenge after backgrounding. | `SecurityManager.kt` invalidates active sessions on `onAppBackgrounded()`, enforcing immediate authentication upon return. |
| **Automated Statements in External Storage** | **CWE-312** | PDF/CSV monthly statements accessible by third-party apps on Android 8–9. | Moved all automated statement generation to private internal storage (`filesDir/auto_exports`), serving files via scoped `FileProvider` grants. |
| **AppWidget Component Authorization Bypass** | **CWE-863** | External broadcasts targeting financial widget IDs to capture private data. | All 8 `WidgetProvider` classes validate supplied IDs against `AppWidgetManager.getAppWidgetIds()` before reading ledger data or updating views. |
| **Unauthenticated Notification / Overlay Mutation** | **CWE-862** | Background overlays or notification actions modifying categories while the phone is locked. | Added checks to `InstantPopupReceiver` and `InstantPopupService` ensuring mutations are rejected while the app is locked. |
| **Unverified Notification Publisher** | **CWE-346** | Spoofed app package publishing fake credit notifications to fabricate income. | Verified installed package signatures using `PackageManager.getPackageInfo(GET_SIGNING_CERTIFICATES)`. Unverified sources are flagged for user review. |
| **Hardcoded Crypto Key Fallback** | **CWE-636** | Swallowing Keystore failure and falling back to a static embedded key or plaintext SQLite. | Removed static fallback passphrases; requires authentic Keystore generation and SQLCipher native initialization. |
| **Plaintext Migration Database Residue** | **CWE-459** | Temporary `kanri_database_plain.db` file remaining on disk after encrypted migration or reset. | Added automated rollback/cleanup in `KanriDatabase.kt`, deleted file during factory resets, and added exclusions in `backup_rules.xml`. |
| **Destructive Default Profile Import** | **CWE-000** | Importing partial backups deleting existing records when merge was requested. | Changed `clearExisting` default to `false` in `BackupRepository.kt` to preserve existing records unless explicit deletion is confirmed. |
| **Unverified SMS Sender Balance Contamination** | **CWE-345** | Spoofed SMS from standard phone numbers altering Cash or Online balances. | Validated originating sender headers using `BankSmsPatterns.isTrustedSender`. Unverified senders are marked with `needs_review = true` and filtered from wallet queries. |
| **Cloud Restore Plaintext Fallback** | **CWE-345** | Replacing cloud backup with unencrypted JSON bypassing passphrase verification. | Replaced plaintext fallback in `CloudBackupManager.kt` with strict container verification (`BackupCryptoHelper.isEncryptedBackup`). |
| **Factory Reset Artifact Retention** | **CWE-459** | Reset leaving statement files and persistent notifications active on device. | `FactoryResetManager.kt` cancels scheduled WorkManager jobs, dismisses all notifications, and deletes export files and cached statements. |
| **Unbounded Streak Calculation Loop** | **CWE-834** | Ancient transaction timestamps triggering billions of daily calendar loop iterations. | Clamped transaction lookback in `StreakCalculator.kt` to 5 years with safety loop counters; bounded timestamps during JSON backup parsing. |
| **Predictable Cloud Passphrase Derivation** | **CWE-331** | Deriving default cloud encryption key solely from user email without a secret. | Incorporated a 256-bit CSPRNG device secret stored in Keystore-backed `EncryptedSharedPreferences` (`getOrCreateDeviceSecret`). |

---

## 18. Build, Installation, Verification & Troubleshooting

### 18.1 Build Prerequisites
- **Android Studio**: Ladybug / Meerkat (2024.2+)
- **JDK**: Java 17 (Recommended: Bundled Android Studio JBR at `C:\Program Files\Android\Android Studio\jbr`)
- **Android SDK Components**: Compile SDK 35, Build Tools 35.0.0, Platform Tools 35.0.0+

### 18.2 Essential Build Commands (PowerShell)

```powershell
# Set Java Environment to bundled JBR
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Check code syntax and compile Kotlin sources
.\gradlew.bat compileDebugKotlin

# Execute comprehensive unit test suite
.\gradlew.bat testDebugUnitTest

# Assemble production-ready debug APK
.\gradlew.bat assembleDebug
# Generated APK: app/build/outputs/apk/debug/app-debug.apk

# Install debug build directly onto running device or emulator
.\gradlew.bat installDebug

# Launch the primary activity on device via ADB
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.omkarnub.kanri/.MainActivityDarkDark
```

### 18.3 Troubleshooting Common Issues

1. **"Can't load widget" Error**:
   - *Cause*: RemoteViews encountered an unsupported View tag during XML inflation (e.g. `<View>` tag used as a divider line).
   - *Fix*: Replace `<View>` tags with `<FrameLayout>` (which is on the RemoteViews allowlist).
2. **Missing Widget Previews on OEM Launchers**:
   - *Cause*: Launcher does not decode `<adaptive-icon>` references in `android:previewImage`.
   - *Fix*: Use dedicated vector drawables with explicit dp dimensions (`res/drawable/preview_widget_*.xml`).
3. **`ActivityNotFoundException` when launching via ADB**:
   - *Cause*: Targeting `.MainActivity` instead of the active launcher alias.
   - *Fix*: Launch `com.omkarnub.kanri/.MainActivityDarkDark`.
4. **`SecurityException` when broadcasting `APPWIDGET_UPDATE` via ADB**:
   - *Cause*: `android.appwidget.action.APPWIDGET_UPDATE` is a system-protected broadcast.
   - *Fix*: Trigger updates by opening the app or using internal dispatchers (`KanriWidgetsUpdater.updateAllWidgets(context)`).
