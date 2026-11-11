# Changelog

All notable changes to Kanri will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.18.13] - 2026-11-11

### Fixed (Phase 6 Configuration-Change & Stress Verification Pass)
- **Post-Onboarding & Configuration-Change Prompt Restraint**:
  - Updated `OnboardingScreen.finishOnboarding()` to record `hasPromptedPostOnboardingSms = true` and `BatteryOptimizationHelper.setPromptedOnce(context, true)` upon onboarding completion so users who intentionally skip optional permissions during onboarding are not re-prompted immediately upon entering the home ledger.
  - Restricted legacy upgrader post-onboarding SMS/battery prompts in `MainActivity.onCreate` to `savedInstanceState == null` so device rotation, font scaling (`200%`), dark/light switches, and RTL configuration changes never launch external system settings over the active screen.
- Bumped version to `1.18.13` (Build 64).

## [1.18.12] - 2026-11-11

### Security & Pre-Publish Hardening (Phase 5 Audit)
- **Network, Backup & WebView Hardening**:
  - Added `res/xml/network_security_config.xml` (`cleartextTrafficPermitted="false"`) and declared `android:usesCleartextTraffic="false"` in `AndroidManifest.xml`.
  - Tightened `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` to exclude `kanri_cloud_backup_secure_prefs.xml` and `crash_logs` from cloud backup and non-transferable Keystore-backed shared preferences from device transfer.
  - Restricted `WebView` sandboxes in `GlassBackgroundView` and `OnboardingScreen` (`allowFileAccess = false`, `allowContentAccess = false`, `allowFileAccessFromFileURLs = false`, `allowUniversalAccessFromFileURLs = false`, plus `shouldOverrideUrlLoading` blocking non-asset URLs) and added explicit `@JavascriptInterface` R8 keep rules in `app/proguard-rules.pro`.
- **Biometric Lock & Resilience**:
  - Updated `QuickEntryActivity` to extend `FragmentActivity`, enforce `SecurityManager` / `SecurityPreferences` biometric/PIN unlock before exposing categories or lending contacts, and insert split-bill lending records inside an atomic `db.withTransaction` block.
  - Guarded external system/share `startActivity` launches across `HealthCheckHelper`, `SettingsComponents`, `OnboardingScreen`, `LendingPersonDetailView`, `LendingTransactionDetailDialog`, `CrashLogsDialog`, and `AppUpdateDialog` against `ActivityNotFoundException`.
  - Updated `ExportWidgetPreviewsTest` output path to app-scoped external Downloads/cache storage for Android 13–17 scoped storage compatibility.
- Bumped version to `1.18.12` (Build 63).

## [1.18.11] - 2026-11-11

### Changed & Fixed (Phase 4 Build Configuration & Compiler/Lint Hygiene)
- **Zero Compiler Warnings & Actionable Release Lint Cleanup**:
  - Updated `CrashLogger` thread ID capture for Android 16+ (`Build.VERSION_CODES.BAKLAVA`) and migrated `TransactionDetailScreen` clipboard copy actions from deprecated `LocalClipboardManager` to `LocalClipboard` + `ClipEntry`.
  - Scoped legacy `EncryptedSharedPreferences` / `MasterKey` deprecation suppressions cleanly with retention notes in `DatabaseKeyManager` and `CloudBackupPreferences` to preserve existing encrypted keys across upgrades (`0` Kotlin compiler warnings across debug, release, and unit-test compilations).
  - Fixed `InlinedApi` crash risk in `InstantPopupService` by gating `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` behind `Build.VERSION_CODES.UPSIDE_DOWN_CAKE` (API 34+).
  - Added launcher shortcut activation tracking (`AppShortcutManager.reportShortcutUsage`) from `MainActivity`, fixed `ClickableViewAccessibility` (`performClick()`) on `GlassBackgroundView` and `OnboardingScreen` WebViews, extracted hardcoded `widget_quick_actions.xml` content descriptions into string resources, set `android:baselineAligned="false"`, removed redundant `AndroidManifest` activity labels, eliminated `VisibleForTests` constructor leakage in `LendingHubViewModel`, removed `UnnecessaryArrayInit` lambdas in `WidgetDataSource`/`WidgetState`, and removed 26 redundant `ObsoleteSdkInt` (`< M/N/O`) checks under `minSdk = 26`.
- **Version Catalog & Build Caching Hygiene**:
  - Upgraded `androidx.biometric` (`1.2.0-alpha05` → `1.1.0`) and `androidx.security:security-crypto` (`1.1.0-alpha06` → `1.1.0`) to stable releases and migrated all inline dependencies (`haze`, `datastore-preferences`, `org.json:json`) into `gradle/libs.versions.toml`.
  - Enabled `org.gradle.caching=true` in `gradle.properties` alongside `org.gradle.configuration-cache=true`, and declared explicit `debug` (`isDebuggable = true`) and `release` (`isDebuggable = false`) build type blocks in `app/build.gradle.kts`.
- Bumped version to `1.18.11` (Build 62).

## [1.18.10] - 2026-11-11

### Changed & Optimized (Phase 3 Pre-Release Size & Performance Pass)
- **APK/AAB Size, Assets & R8 Rules**:
  - Replaced blanket `-keep` directives in `app/proguard-rules.pro` with targeted keep rules (relying on AAR consumer rules and AAPT2 `aapt_rules.txt`), shrinking `classes.dex` from `47,710` to `31,726` method IDs (`-33.5%`) and `7,091` to `5,876` class definitions (`-17.1%`).
  - Added `androidResources { localeFilters += listOf("en") }` in `app/build.gradle.kts`, reducing `resources.arsc` from `610,388 B` to `90,088 B` (`-85.2%`).
  - Converted `assets/backgrounds/cherry_canyon.html` embedded `736x1308` PNG (`1,434,530 B`) to `q=98` WebP (`278,388 B`), converted all 12 `res/drawable-nodpi/widget_preview_*.png` drawables to lossless WebP (`253,180 B` → `92,912 B`), and removed 30 redundant legacy PNG/WebP launcher mipmap density files superseded by `<adaptive-icon>` vectors.
  - Added optional `-PenableAbiSplits=true` ABI splits in `app/build.gradle.kts` (yielding `~11.66 MB` `arm64-v8a` APKs alongside AAB/universal release builds: universal APK `26.85 MB` / `28.16 MB` → `-2.32 MB`; release AAB `20.97 MB` / `21.99 MB` → `-2.22 MB`).
- **Startup, Baseline Profile & Runtime Performance**:
  - Added explicit `androidx.profileinstaller:profileinstaller:1.4.0` dependency and `app/src/main/baseline-prof.txt` compiled into `assets/dexopt/baseline.prof`.
  - Moved non-UI scheduler and notification worker initializations off the main startup path onto `Dispatchers.IO` inside `KanriApplication` and removed duplicate initializers from `MainActivity`.
  - Upgraded Room schema to **v13** (`MIGRATION_12_13`) adding performance indexes on `transactions` (`timestamp`, `category_id`, `is_duplicate`, `needs_review`, `counterparty`, `(type, timestamp)`) and `lending_records` (`is_settled`, `linked_transaction_id`, `person_name`, `date`) with unit tests verifying migration SQL and indexes (`55 suites`, `343 tests` passing).
  - Resolved all 8 `StaticFieldLeak` warnings, converted `ThemePreferences` init to async `.apply()`, moved expensive timeline/search transformations onto `Dispatchers.Default`, added `@Immutable` annotations, stable Lazy list `key`s, `mutableIntStateOf`, and lambda `Modifier.offset` optimizations.
- Bumped version to `1.18.10` (Build 61).

## [1.18.9] - 2026-11-11

### Removed & Cleaned (Phase 2 Pre-Release Dead Code & Hygiene Pass)
- **Dead Kotlin Screens, Cards, Charts & Composables**:
  - Removed 18 proven unreferenced UI files (`InstantPopupSheet.kt`, `SavingsGoalsSheet.kt`, `SplitExpenseScreen.kt`, `FinancialHealthCard.kt`, `FinancialHealthDetailsSheet.kt`, `CheckFinancialHealthCard.kt`, `PersonalRecordsSection.kt`, `SmallSpendsSection.kt`, `SplitExpensesSummarySection.kt`, `SuggestedBudgetSection.kt`, `WeekendVsWeekdaySection.kt`, `GrainGradientBackground.kt`, `FoldText.kt`, `KokonutStackPanel.kt`, `DotMatrixEngine.kt`, `FinancialSkyScene.kt`, `AnimatedMonochromeRingChart.kt`, `RecurringPaymentsSheet.kt`).
  - Removed unreferenced symbols and unused imports across `WheresMyMoneyCard.kt` (`WheresMyMoneyCard` wrapper), `AnimatedNumber.kt` (`AnimatedScore`), `CategoryDonutChart.kt` (`CategoryBreakdownRow`), `AddExpenseIncomePopup.kt` (`LendBorrowFlowIcon`, `SplitBillFlowIcon`), `KanriDockIcons.kt` (`HistoryDockIcon`), `SettingsComponents.kt` (`getShaderGradientBrush`, `ShaderPreviewCard`, `GlassQuickPresetsRow`, `CloudBackupDialog`), `KanriGlassTokens.kt` (unused `GlassPresets` entries), and `ScheduledExportWorker.kt` (`copyFile` helper).
- **Duplicate Assets, Unused Resources & Dependencies**:
  - Removed duplicate `assets/fonts/LastoriaBoldRegular.otf` and updated `SignatureText.kt` to load `R.font.lastoria_bold_regular` directly.
  - Removed 6 unused drawable XMLs (`ic_widget_arrow_right`, `ic_widget_split`, `preview_budget_ring`, `preview_goal_ring`, `splash_background_dark`, `splash_background_light`) and 6 unused onboarding strings (`UnusedResources` reduced from `13` to `0`).
  - Removed unused explicit `androidx.graphics.path` dependency from `app/build.gradle.kts` and `gradle/libs.versions.toml` (retained transitively where Compose requires it).
- **Release Logging Hygiene**:
  - Gated residual `Log.d` calls across workers, receivers, services, onboarding WebView, and notification helpers behind `BuildConfig.DEBUG`.
- Bumped version to `1.18.9` (Build 60).

## [1.18.8] - 2026-11-11

### Fixed & Verified (Phase 1 Pre-Release Correctness Audit)
- **Multi-Currency, Rounding & Indian Grouping Consistency**:
  - Updated `CurrencyUtils.formatCurrency` and `formatCompactCurrency` to apply explicit `BigDecimal` `HALF_UP` rounding, prevent signed `-0` / `-0.00` outputs on near-zero negative values or non-finite inputs, and enforce Indian `2,2,3` digit grouping consistently for `INR` alongside global `B`/`M`/`k` compact suffixes for non-INR currencies.
  - Raised `KokonutAmountPad` manual input ceiling to `10,000,000.0` to match high-denomination currencies and replaced residual hardcoded `"₹"` strings and `.toInt()` truncations across analytics, search, lending, recurring payments, goals, and export UIs.
- **Financial Calculators, Exports & DAO Accuracy**:
  - Fixed DST-safe remaining cycle day counting (`BudgetCalculator.getDaysRemainingInCycle`), calendar-instance cloning, duplicate/transfer transaction exclusion across `TransactionDao` review queries, `HomeViewModel`, `SearchFilterModels`, `DeltaCalculator`, `StreakCalculator`, `AllTimeTrendsCalculator`, `CategoryDetailUtils`, and `CsvExporter`.
  - Made date/number formatters thread-safe in CSV/PDF statement exporters, added `CurrencyUtils.getCurrencyCode()` headers, and propagated coroutine work off main threads via `Dispatchers.Default`/`Dispatchers.IO`.
- **Widget Engine Multi-Currency & Thread Safety**:
  - Synchronized `CurrencyPreferences` with `CurrencyUtils` inside `WidgetDataSource`, wired `BudgetPreferences.budgetCycleStartDay` into `Today`/`Budget`/`SafeToSpend` widgets, converted shared `Paint` instances to `ThreadLocal` across `DotVisuals`, `PixelFont`, `DotMatrixFont`, and `WidgetRenderer`, and capped widget bitmap allocations safely.
- **Database Safety, Backup Completeness & Process/State Preservation**:
  - Gated Room destructive migration fallback strictly to `BuildConfig.DEBUG` and aligned legacy unencrypted DB migrations (`MIGRATION_5_6..MIGRATION_11_12`) with all schema tables (`lending_records`, `lending_repayments`, `wallet_balances`).
  - Added `recurringPayments`, `savingsGoals`, and `savingsGoalContributions` to encrypted/JSON backup & restore (`BackupData`, `BackupRepository`, `CloudBackupManager`) and cleaned up `FactoryResetManager` deletion order.
  - Made category deletion atomic inside a Room transaction (`SettingsScreen`), preserved safe UI dialog states with `rememberSaveable`, routed launcher intents through `AppIconManager.createLauncherIntent`, removed hardcoded `15000.0` fallback balance in `InstantPopupCard`/`InstantPopupService`, escaped Google Drive query literals, resolved all 11 Compose `[NonObservableLocale]` lint errors, and gated sensitive financial logcat output behind `BuildConfig.DEBUG`.
- **Unit Test Coverage**:
  - Added `CurrencyUtilsTest` and expanded unit test suites across `DotMatrixFontTest`, `PixelFontTest`, `BackupJsonParserTest`, `DatabaseMigrationTest`, `BudgetCalculatorTest`, `DeltaCalculatorTest`, `StreakCalculatorTest`, `AllTimeTrendsTest`, `CategoryDetailTest`, and `CsvExporterTest` (`55 suites`, `342 tests` passing).
- Bumped version to `1.18.8` (Build 59).

## [1.18.7] - 2026-11-11

### Changed & Refined
- **Centered Lower-River Permission Placement (`PaintingPermissionView`, Steps 2–7)**:
  - Shifted painting-integrated permission titles, captions, frosted glass Enable pill, and Skip action from the asymmetric top-left sky down to a centered band in the calm lower river foreground below the sampan boat.
  - Elevated `boatY` across Scenes 2–7 (`1272..1295`) and raised Scene 6's cove lotus pads (`y = 1385..1440`) so the entire mountain vista, birds, calligraphy, boat, and Easter eggs sit completely unobstructed above the permission prompt.
- Bumped version to `1.18.7` (Build 58).

## [1.18.6] - 2026-11-11

### Added
- **Step 8 Final "You're all set" Sanctuary Slide (`FinalAllSetSlideView`)**:
  - Added a dedicated 9th Shan Shui dawn scene (`buildScenery8_BaseWashes`) with low open mountain horizons, shoreline reeds, and an anchored serene sampan boat.
  - Displays the pure black Kanri logo (`KanriLogoLockup`), `"You're all set"` completion heading, privacy tagline, and a frosted dark glass `"Finish"` button.
- **Minimalist Shan Shui Permission Easter Eggs**:
  - **Step 2 (SMS Detection)**: One seated traveler on the sampan holds an illuminated handset with a tiny brushed SMS envelope rising above it.
  - **Step 3 (Payment Apps)**: Subtle gold-cinnabar chime sparks glow softly above the second traveler beneath the 4 swallows.
  - **Step 4 (Instant Overlay)**: Minimalist translucent floating miniature washi card hovers gracefully above the boat.
  - **Step 5 (Background Endurance)**: Warm bamboo fireflies trace a quiet infinity loop (`∞`) above the boat canopy.
  - **Step 6 (System Alerts)**: A hanging temple wind-bell (`furin`) gently sways beneath the cliff pavilion roof eave with soft acoustic arcs.
  - **Step 7 (Biometric Security)**: Delicate concentric biometric fingerprint ridges pulse inside the cinnabar red seal at `(928, 460)`.
- **Automatic Progression on Permission Grant**:
  - Tapping Enable on Steps 2–7 arms auto-advance (`awaitAutoAdvanceForStep`) across OS runtime permission dialogs, system Settings returns (`ON_RESUME`), and in-app App Lock activation, briefly displaying the emerald confirmation state before gliding cleanly to the next scene.

### Changed & Refined
- **True Top-Left Painting-Integrated Permission Layout (`PaintingPermissionView`)**:
  - Removed the centered circular icon badge and two-button stack; anchored each permission's title and caption directly in the open top-left sky (`fillMaxWidth(0.78f)`) clear of the top-right vertical calligraphy and seal.
  - Replaced stacked actions with a single frosted dark glass Enable/Continue pill button and a compact `"Skip"` button right beside it.
- **Responsive Calm Pacing**:
  - Tuned Shan Shui living scroll pacing from `3500ms / 3000ms` down to `glideDuration: 1850ms`, `paintDuration: 1650ms`, and `startupDuration: 1800ms`, with matching `240ms` UI fades and `1750ms`–`1800ms` Compose transition choreography.
- **Default Live Shader Changed to Sumi-e Mist**:
  - Changed the first-launch and preference-reset default background in `ThemePreferences` and `GlassBackgroundType.fromId()` from `Gemini Aurora` to `Sumi-e Mist`.
- Bumped version to `1.18.6` (Build 57).

## [1.18.5] - 2026-11-11

### Changed & Redesigned
- **Painting-Integrated Permission Prompts (`PaintingPermissionView`, Steps 2–7)**:
  - Removed floating permission cards on slides 2 through 7 and integrated each permission prompt directly into the Shan Shui living scroll canvas.
  - Added translucent circular ink seal badges (turning serene jade green with a checkmark once granted), deep sumi ink typography with delicate parchment radial glow, frosted dark glass Allow & Continue buttons, and subtle `"Skip for now"` links.
- **Educational Permission Skip Warning Modals (`PermissionSkipWarningDialog`)**:
  - Attempting to skip or advance past any ungranted permission (Steps 2–7) displays a contextual warning popup explaining why that specific permission is critical (SMS offline tracking, UPI notification listener, instant overlay, background endurance, budget alerts, and biometric security).
  - Provides `"Enable Permission"` as the primary action and `"Skip Anyway"` as the secondary confirmation action.

### Fixed & Optimized
- **Removed Horizontal White Strokes on Mountain Washes**:
  - Removed the rectangular `fillRect` mist strokes `[995, 1045, 1092, 1125]` in `buildScenery0_BaseWashes` and disabled `renderLiveMistVeils` in `shan_shui_onboarding.html` which were creating sharp horizontal pale bars across the mountain washes behind the Kanri logo.
- **60fps Hardware-Accelerated Canvas Rendering**:
  - Disabled heavy CPU-bound SVG `feDisplacementMap` filters (`#xuan-capillary-bleed`, `#sumi-dry-brush`) inside Android `webview-mode`.
  - Replaced per-frame offscreen `destination-in` scratch canvas clearing in `drawCapillaryWashReveal` with GPU-accelerated alpha compositing and upward capillary settle translation to eliminate animation stuttering.
- Bumped version to `1.18.5` (Build 56).

## [1.18.4] - 2026-11-11

### Fixed & Cleaned
- **Pure Black Kanri Monogram Emblem**:
  - Changed the center landing monogram emblem from cinnabar red to deep solid black (`#000000`), matching the Panchang "KANRI" vector wordmark.
- **Removed Financial Caption & Subtitle**:
  - Removed "FINANCIAL SERENITY / Know where every rupee went" tagline completely from Slide 0.
- **Removed White Tint Box Overlay**:
  - Removed `#mock-android-ui` and all residual `.dev-hud` elements from `shan_shui_onboarding.html`, eliminating the white frosted box from displaying behind the hero lockup.

### Changed & Refined
- **Meditative, Calm & Slow Painting Pacing**:
  - Tuned Shan Shui living scroll pacing to `glideDuration: 3500ms`, `paintDuration: 3000ms`, and `startupDuration: 3500ms`.
  - Extended transition step delay to 3200ms across all slide changes, giving the painting plenty of time to glide, bloom, and settle in ink.
- **Top Symmetrical Progress Bar & Bottom-Left Back Button**:
  - Removed the back button from the top row and centered the progress bar symmetrically in the middle of the screen.
  - Positioned a circular frosted glass back button at the bottom-left on setup slides 1 through 7.
- **Elevated Card Placement**:
  - Shifted all slide cards upwards with `BiasAlignment(0f, -0.38f)`, giving greater breathing room to the panoramic living scroll below.
- Bumped version to `1.18.4` (Build 55).

## [1.18.3] - 2026-11-11

### Fixed
- **Shan Shui Living Scroll Painting Engine**:
  - Fixed runtime initialization in `shan_shui_onboarding.html`, resolving undefined step parameter reference so living scroll paintings render and animate reliably.
  - Locked background permanently to light Xuan paper theme (`currentTheme = 'light'`), disabling dark mode.

### Changed & Refined
- **Minimalist Top Progress Bar**:
  - Replaced card stepper and checkboxes with a clean top bar: Back navigation icon, `"STEP X OF 7"` bold indicator, percentage completion, and smooth 4dp `LinearProgressIndicator`.
- **Frosted Glass Cards, Glass Buttons & White Typography**:
  - Styled all onboarding cards (`PermissionCardView`, `KokonutAvatarPicker`, and `OnboardingPrivacySheet`) in dark frosted glass (`#141416` @ 74% - 94% opacity) with specular linear gradient borders (`Brush.linearGradient`).
  - Switched all card typography, titles, descriptions, and labels to pure white (`Color.White`).
  - Implemented translucent frosted glass buttons with subtle white borders and white text across all cards and modals.
  - Upgraded `AvatarCropDialog` action buttons to frosted glass with white text.
- Bumped version to `1.18.3` (Build 54).

## [1.18.2] - 2026-11-11

### Added
- **1:1 Aspect Ratio Avatar Cropper (`AvatarCropDialog`)**:
  - Integrated interactive 1:1 avatar cropping modal with pinch-to-zoom (0.5x to 4.0x) and drag-to-pan.
  - Features circular avatar guideline mask and rule-of-thirds grid.
  - Automatically exports a 512x512 high-resolution bitmap and saves it to user preferences.
- **Setup Progress Pattern (`SetupProgressPattern`)**:
  - Replaced dot-and-line stepper with the modern Setup Progress pattern.
  - Includes progress title header, completion count, animated `LinearProgressIndicator`, and `CheckCircle`/`Circle` step status items.

### Changed & Fixed
- **Strict Light Mode for Onboarding**:
  - Enforced light mode exclusively across the onboarding flow, disabling dark mode and forcing Xuan paper light aesthetics.
- **Black Kanri Logo & Emblem**:
  - Configured pure black (`#000000`) for both the Kanri monogram emblem and Panchang letter wordmark.
- **Glass Transparency & Backdrop Decoupling**:
  - Decoupled Onboarding and `OnboardingPrivacySheet` from `KanriBackdropHost` to eliminate background shader bleeding through glass frost.
  - Set frosted cards in `ProfileSetupStepView` and `PermissionCardView` to a clean light translucent surface (`#FFFFFF` at 90% alpha with 1dp border).
- **Sequential Step Choreography**:
  - Standardized transition sequence: active page components fade out (260ms) $\to$ background living scroll glides and blooms in ink (1350ms) $\to$ next card fades in (300ms).
- **High-Contrast Button Styling**:
  - Fixed white-on-white button text issues by enforcing solid black containers (`#0F0E0C`) with crisp white text for primary CTA buttons, and soft green (`#E8F5E9`) with dark green text (`#1B5E20`) for granted permissions.
- Bumped version to `1.18.2` (Build 53).

## [1.18.1] - 2026-11-11

### Fixed
- **Shan Shui Living Scroll Canvas Initialization & WebView Runtime**:
  - Fixed JavaScript `SyntaxError: Identifier 'container' has already been declared` and unclosed block in `shan_shui_onboarding.html` caused by duplicate stepper variable declarations.
  - Removed query parameters from `file:///android_asset/backgrounds/shan_shui_onboarding.html` preventing Android AssetManager `ERR_FILE_NOT_FOUND` resolution failure.
  - Added null-safety checks to all HUD and mockup element selectors (`theme-label`, `theme-dot`, `paper-vignette`, `hud-brush-status`, `pace-label`, `mock-android-ui`, etc.) preventing unhandled DOM exceptions.
  - Guarded Java-to-Compose bridge callbacks with `coroutineScope.launch` for thread-safe state mutations.
  - Bumped version to `1.18.1` (Build 52).

## [1.18.0] - 2026-11-11

### Added
- **Rebuilt Onboarding Workflow & 100% ChapterPlayer Intro Animation**:
  - Rebuilt the entire first-time user onboarding slider workflow from scratch in `OnboardingScreen.kt` and `KanriIntroLogo.kt`.
  - **Screen Fade-In & Synchronized Painting**:
    - The screen dissolves from darkness/paper upon launch without any sudden UI flashing.
    - The Shan Shui living scroll background (`shan_shui_onboarding.html`) executes its 1:1 master landscape painting (Twin Peaks & Misty Promontory) with foreground UI hidden until the ink bloom finishes.
    - Integrated a two-way JavaScript bridge (`AndroidBridge.onPaintingFinished` and `AndroidBridge.onGlideFinished`) plus an autonomous safety timeout ensuring Compose and the HTML5 canvas stay in perfect lockstep.
  - **100% Identical Louis Studios ChapterPlayer Intro Animation**:
    - Implemented ChapterPlayer's exact mathematical formulas on Jetpack Compose Canvas:
      1. *Mark Landing*: Kanri monogram emblem (`kanriicon.svg`) lands at center with exponential ease (`eExpo(seg(lt, 0, 0.60))`, scale $1.55\times \to 1.0\times$).
      2. *Settle to Lockup*: Smoothly glides to left lockup position with cubic Hermite ease (`eInOut(seg(lt, 0.85, 1.70))`).
      3. *Kinetic Typography*: Panchang letters K-A-N-R-I rise and sharpen beside the emblem with $35\text{ms}$ stagger and $700\text{ms}$ duration (`dy = (1 - eExpo(p)) * size * 0.55`).
      4. *Masked Subtitle*: "FINANCIAL SERENITY / Know where every rupee went" reveals from behind a clipped mask (`p = seg(lt, 2.15, 2.95)`).
      5. *Living Frame Push-In*: Continuous slow push-in ($1 + 0.03 \cdot eInOut(seg(lt, 0, 5))$) keeping the held frame alive.
    - Synchronized "Get Started" button entrance at $lt \ge 2.95\text{s}$.
  - **Sequential Two-Phase Transitions**:
    - Tapping "Get Started" triggers ChapterPlayer's exit easing ($\Delta y = -eInOut(out) \cdot 14\text{px}$, $\alpha = 1 - eInOut(out)$), fading out the hero UI *first*.
    - Only once the UI is completely clear does the living scroll background progress to Scene 2 (`setOnboardingStep(1)`).
    - Avatar Picker card (`KokonutAvatarPicker`) fades in smoothly when arriving at Scene 2.
    - Unified the same card fade-out $\to$ background glide $\to$ card fade-in choreography across all remaining steps (Steps 1–7) with bidirectional back-navigation.
- Bumped version to `1.18.0` (Build 51).

## [1.17.1] - 2026-11-11

### Added
- **Hold & Drag Reordering in Category Order Sheet (`CategoryReorderSheet`)**:
  - Implemented tactile hold & drag reordering in `CategoryReorderSheet`:
    - **Long-Press Card Grab**: Long-pressing anywhere on a category card triggers haptic tick feedback, floating the card with elevation (`10.dp`), scale (`1.03f`), and primary highlight border.
    - **Direct Drag Handle**: Added a dedicated `Icons.Default.DragHandle` icon on the left of each item for instant touch-and-drag.
    - **Real-Time Animated Reorder**: As the dragged item passes neighboring items, the list updates smoothly in real time with `Modifier.animateItem()` placement transitions.
    - **Viewport Edge Auto-Scroll**: Dragging an item near the top or bottom of the sheet smoothly auto-scrolls the `LazyColumn`.
    - **Persistent Batch Updates**: Releasing the item immediately commits the new order sequence to Room database `categories.display_order`.
    - Preserved existing Move Up / Move Down buttons for single-step bumping and accessibility.
- Bumped version to `1.17.1` (Build 50).

## [1.17.0] - 2026-11-11

### Added
- **Official Kanri Monogram Emblem Vector Assets**:
  - Replaced legacy launcher vector assets with the official geometric Kanri monogram emblem (`kanriicon.svg`).
  - Updated `panchang_light.xml`, `panchang_dark.xml`, `ic_launcher_monochrome.xml`, and `ic_notification.xml` with mathematically centered vector transforms (`scaleX=0.0350665`, `translateX=18.092`, `translateY=18.092`) fitting within Android adaptive icon safe-zones.
- **ChapterPlayer Scene 0 Kinetic Logo Reveal (`KanriIntroLogo`)**:
  - Re-choreographed the onboarding logo entrance using the official Kanri monogram emblem:
    1. *Landing*: Emblem lands large in the center with exponential ease (`scale 1.85 -> 1.50`, `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`).
    2. *Settle & Extend*: Smoothly translates from center to left lockup (`scale 1.50 -> 1.00`, `CubicBezierEasing(0.25f, 1f, 0.35f, 1f)`).
    3. *Wordmark Emergence*: Staggered kinetic rise and sharpening of the 5 Panchang letters (`K - A - N - R - I`, 65ms intervals).
    4. *Tagline Reveal*: Elegant fade-in of `"FINANCIAL SERENITY / Know where every rupee went."`
    5. *Organic Breathing*: Subtle living push-in (`scale 1.0 -> 1.02`) breathing with the living scroll background.
- **Direct-to-Onboarding First Launch Flow**:
  - Initialized `showSplashScreen = onboardingPrefs.hasCompletedOnboarding` in `MainActivity.kt`.
  - First-time users immediately see Scene 0 of the animated Shan Shui background painting while the ChapterPlayer logo animation begins.
  - Returning users continue to receive the quick splash screen animation.
- **Unobstructed Living Scroll Wallpaper on Slide 0**:
  - Removed demo card container from Slide 0 (Hero slide), allowing the East-Asian Shan Shui living scroll landscape to paint unobstructed across the entire screen.
  - Attached `WebViewClient` on `shan_shui_onboarding.html` to guarantee instant step 0 trigger as soon as the HTML canvas is ready.
- **Frosted Window Blur for Category Order Sheet (`CategoryReorderSheet`)**:
  - Replaced standard `ModalBottomSheet` with `KanriModalBottomSheet` in `SettingsComponents.kt`.
  - Added native `FLAG_BLUR_BEHIND` window blur and frosted glassmorphic styling (`kanriGlass(level = GlassLevel.Strong)`) to the Category Order sheet.
- Bumped version to `1.17.0` (Build 49).

## [1.16.0] - 2026-11-11

### Fixed
- **Promotional Cashback & Offer Notification Hallucinations**:
  - Filtered promotional cashback offers, marketing banners, and coupon notifications from Amazon, Flipkart, PhonePe, Google Pay, and Paytm (e.g., "Cashback offer: Flat ₹250 on Amazon Pay on recharge", "Win scratch cards", "Offer valid till midnight").
  - Enforced `isConfirmedCashbackOrReward` verification in `NotificationParser.kt`: cashback and rewards are only parsed as `CREDIT` when explicit, unambiguous credit verbs are present ("credited", "received", "added to your", etc.). Bare marketing keywords without verified deposits are rejected.
  - Added `PKG_FLIPKART` (`com.flipkart.android`) support with strict marketing offer filtering in `NotificationParser.kt` and `SmsParser.kt`.

### Added
- **Custom Category Ordering in Settings (`CategoryReorderSheet`)**:
  - Added new **"Category Order"** control under *Settings > Rules & Categories*.
  - Users can prioritize categories with tactile Move Up / Move Down buttons, reset back to factory defaults at any time, or add new custom categories.
  - Added `display_order` column to Room database `categories` table with seamless database migration `MIGRATION_11_12` (Database v11 → v12).
  - Updated category pickers across transaction entry and search sheets to sort with user-defined priority.
- **8-Step Panoramic Shan Shui Living Scroll Onboarding Background**:
  - Added edge-to-edge animated East-Asian living scroll (`shan_shui_onboarding.html`) with 8 sceneries synchronized with onboarding steps:
    - Step 0: *Twin Peaks & Dawn Clouds* (Hero)
    - Step 1: *Scholar's Pine & Mountain Brook* (Profile)
    - Step 2: *Cloud Gorge Waterfalls & Rising Mist* (SMS Detection)
    - Step 3: *Cinnabar Plum with 4 Swallows* (Payment Apps)
    - Step 4: *Glowing Moonbridge with Silver Lanterns* (Overlay)
    - Step 5: *Bamboo Sanctuary & Fireflies* (Battery Optimization)
    - Step 6: *Sunset Pavilion Ripples & Gold Horizon* (Spending Alerts)
    - Step 7: *Sacred Pagoda & Mountain Constellations* (Biometric Lock)
  - Completely stripped HUD elements, controls, and audio listeners for a pure wallpaper loop with transparent background touch passthrough.
  - Added live paper theme synchronization matching app light/dark appearance.
- **ChapterPlayer Kinetic Intro Logo Animation (`KanriIntroLogo`)**:
  - Replaced static hero icon with ChapterPlayer-inspired kinetic monogram mark landing large (`scale 2.2 -> 1.0`) with exponential ease (`CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`).
  - Animated Panchang typography with staggered letter rises (`K-A-N-R-I`, 75ms delay each) and tagline reveal ("FINANCIAL SERENITY / Know where every rupee went.").
- Bumped version to `1.16.0` (Build 48).

## [1.15.0] - 2026-11-11

### Added
- **10 New Animated Glass Backgrounds Added**:
  - `Pixel Kitchen` (24h): Cozy pixel kitchen with warm kettle, day/night window lighting & subtle steam.
  - `Snowy Mountain Road` (24h): Atmospheric winter mountain road with drifting snow, headlight beams & real-time sky.
  - `Sunlit Balcony` (24h): Quiet sunlit balcony with potted plants, gentle breeze & evening fireflies.
  - `Alpine Valley`: Animated 9:16 pixel art mountain valley with winding river, wildflower meadow & soaring birds.
  - `Shiganshina Windmill` (24h): 4-bit animated pixel art windmill with calm wind gusts & 24-hour dawn-to-night lighting cycle.
  - `Chibi Coffee Shop`: Cozy chibi cafe window on a rainy evening with steaming mugs, warm lanterns & cat companion.
  - `Fuji Torii Gate`: Japanese vermilion torii gate before Mount Fuji with drifting sakura petals & glowing valley mist.
  - `Cherry Blossom Canyon` (24h): 3D voxel cherry blossom canyon with drifting petals, celestial day/sunset/night traversal & volumetric clouds.
  - `Pixel Misty Alley` (24h): Atmospheric neon city alley with gentle rain, companion cat & dynamic sun/moon cycle.
  - `Glowing Bamboo Forest` (24h): Steam train crossing a bridge through swaying bamboo with glowing lanterns & 24-hour atmosphere.
- **Pure Wallpaper Loop Without HUD & Controls**:
  - Stripped all HUD elements (pixel digital clocks, time badges, audio hints, quality badges) and removed audio/click control listeners from all backgrounds so only the pure wallpaper plays in a seamless loop.
  - Sized backgrounds to fill the viewport seamlessly (`object-fit: cover`) with touch passthrough to the UI.
- **Local Time 24-Hour Cycle Synchronization**:
  - Connected all 24-hour cycle backgrounds (`Pixel Kitchen`, `Snowy Mountain Road`, `Sunlit Balcony`, `Shiganshina Windmill`, `Cherry Blossom Canyon`, `Pixel Misty Alley`, `Glowing Bamboo Forest`) to the user's real-time local clock (`new Date()`), matching `Still Sunset Meadow`.
- **Offline Three.js Support & Video Autoplay**:
  - Bundled local `three.min.js` reference in `cherry_canyon.html` for zero-network offline loading.
  - Configured `mediaPlaybackRequiresUserGesture = false` in `GlassBackgroundView.kt` for seamless video loop playback.
- Bumped version to `1.15.0` (Build 47).

## [1.14.4] - 2026-11-11

### Fixed & Restored
- **Glass Mode Cards Tint & Backdrop Blur Restored**:
  - Removed accidental `hazeSource` modifier from `MainNavigationScreen.kt` tabs container, restoring pure capture of `GlassBackdrop` and ensuring all `KanriGlassCard`s render with their full frosted blur, custom tint, and specular rim reflections.
- **Frosted Translucent Dock for Light & Dark Modes**:
  - Rebuilt non-glass dock container in `KanriFloatingDock.kt` with dedicated frosted translucent vertical gradients (`0.82f - 0.90f` opacity), luminous top edge highlights, and soft shadows, making the dock frosted and translucent in Light and Dark mode without conflicting with Glass mode shaders.
- Bumped version to `1.14.4` (Build 46).

## [1.14.3] - 2026-11-11

### Fixed & Changed
- **Default App Theme to Glass Mode**:
  - Configured `ThemeMode.GLASS` as the default theme in `ThemePreferences.kt` on new installs and resets.
- **Full Ring Visibility in Dark & AMOLED Modes**:
  - Fixed background track visibility in `RingVisualizer` (`ExpenseVisualizer.kt`) so that the complete 360-degree circle is clearly visible in Dark and AMOLED mode rather than blending into black.
  - Added subtle boundary hairlines for clean edge definition and updated `AnimatedMonochromeRingChart.kt` background track to match.
- **Frosted Translucent Floating Dock in Light and Dark Modes**:
  - Increased dock surface opacity to authentic frosted glass levels (`0.76f - 0.82f`) in `KanriGlass.kt` for Light, Dark, and AMOLED modes.
  - Connected `Modifier.hazeSource` to `MainNavigationScreen.kt` tabs container so underlying content is sampled and blurred in real time.
  - Added backing translucent background in `KanriFloatingDock.kt` so the dock has frosted substance and is never see-through.
- Bumped version to `1.14.3` (Build 45).

## [1.14.2] - 2026-11-11

### Refined & Polished
- **Complete Final UI/UX Consistency Across All 4 Themes (Light, Dark, AMOLED, Glass)**:
  - **Floating Dock & Navigation Transitions**:
    - Purged cool blue undertones (`#14151E`, `#06070A`, `#121318`) from `KanriFloatingDock.kt`, standardizing on pure neutral monochrome `#141414`, `#060606`, and `#121212`.
    - Harmonized theme lerping in dock background, icons, and text across Light mode cream (`#FAF9F6`), Dark mode, AMOLED true black, and Glass frost blur.
  - **Instant Popup Card & Floating Overlay**:
    - Made `InstantPopupCard.kt` dynamically reactive to theme mode changes via `themePrefs.themeModeFlow.collectAsState()`, properly resolving `ThemeMode.SYSTEM` with `isSystemInDarkTheme()`.
    - Replaced hardcoded cool blue backgrounds with neutral monochrome `#141414` (Dark/Glass) and `#FAF9F5` (Light mode).
  - **Instant Transaction Popup Sheet**:
    - Replaced hardcoded GitHub dark colors (`#E6EDF3`, `#8B949E`, `#30363D`) with semantic `MaterialTheme.colorScheme.onSurface`, `onSurfaceVariant`, `outlineVariant`, and `primary` for flawless legibility in Light mode.
  - **Recurring Payments & Export Statement Sheets**:
    - Replaced hardcoded white `#F0F6FC` on candidate titles and item amounts with `MaterialTheme.colorScheme.onSurface`, eliminating invisible text in Light mode.
    - Standardized billing cycles, due date indicators, and delete buttons to `MaterialTheme.colorScheme.onSurfaceVariant`.
    - Updated `ExportStatementSheet.kt` records summary, transaction counts, and calculation placeholders to adaptive theme text colors.
  - **Split Expense & Group Bill Splitting**:
    - Replaced bright cyan `#00D09C` and bright red `#FF6B6B` with `SplitPositiveGreen` (`#30A46C`) and `SplitNegativeRed` (`#E54D2E`), ensuring accessible contrast against both light cream and dark backgrounds.
    - Updated profile badges and icons to adapt across Light, Dark, AMOLED, and Glass.
  - **Widget Gallery & Live DotTiles Previews**:
    - Wrapped `WidgetGalleryCard` in adaptive `KanriGlassCard`, converting the preview cards from hardcoded dark containers to theme-adaptive surfaces with clean borders and typography.
    - Upgraded size selection filter chips and Pin buttons to theme-adaptive primary colors.
  - **Health Check & Diagnostics Sheet**:
    - Converted action buttons in `DiagnosticsSheet.kt` to semantic `primary` and `error` theme colors.
    - Replaced hardcoded GitHub dark fallback text tints in `HealthCheckBanner.kt` with `MaterialTheme.colorScheme.onSurfaceVariant`.
  - **Onboarding Hero Interactive Demo**:
    - Updated `DemoSmsCard` and `DemoParsedCard` in `OnboardingHeroDemo.kt` to use `MaterialTheme.colorScheme.outlineVariant`, `surfaceContainerHighest`, and `onSurfaceVariant`, fixing light mode contrast on the onboarding screen.
  - **Savings Goals & Category Detail**:
    - Swapped hardcoded `DropdownMenu` in `GoalsScreen.kt` for `KanriDropdownMenu` with glass blur and surface adaptation.
    - Updated month-over-month delta badges in `CategoryDetailScreen.kt` to accessible Radix/Material semantic colors.
- Bumped version to `1.14.2` (Build 44).

## [1.14.1] - 2026-11-11

### Refined & Perfected
- **Adaptive Two-Column Layouts & Zero Dead Space**:
  - Engineered adaptive geometry engine in `WidgetRenderer.kt` utilizing dynamic aspect ratio and width breakpoints (`isWide = width > 220dp`, `isCompactH = height < 115dp`).
  - Wide 4x2 widgets (`Today`, `Budget`, `Wallets`, `Dues`, `Month Delta`) now render in balanced two-column designs:
    - **Today Spend (4x2)**: Left column displays title, expense count, hero spent amount, and daily limit badge; right column renders the pacing bar and 4 checkpoint cards (`MORN · AFTN · EVE · NIGHT`).
    - **Budget (4x2)**: Left column displays title, cycle days remaining, hero remaining amount, and spent of total badge; right column renders dual progress bars and daily safe allowance.
    - **Wallets (4x2)**: Left column displays title, subtitle, mint net worth hero, and allocation ratio; right column renders two recessed cards for Cash and Bank with balances and dot meters.
    - **Lend & Borrow (4x2)**: Left column displays title, net balance hero, and total get/owe breakdown; right column renders top contacts in individual cards with `+₹` / `-₹` amounts in Ndot57 numerals.
    - **Month Delta (4x2)**: Left column displays title, comparison day subtitle, hero delta %, and status indicator; right column renders two comparison tiles for this month to date vs last month same day.
  - **Wide 4x1 Compact Widgets** (`Goals`, `Streak`, `Safe to Spend`): Replaced left-stacked empty space with balanced side-by-side layouts where hero metrics sit on the left and full-width interactive sliders or 10-flame timelines fill the right half.
  - **Compact 2x1 Widgets** (`Budget`, `Review Inbox`, `Quick Actions`): Balanced side-by-side layouts preventing text collisions and keeping metrics legible at low vertical heights.
  - **Square 2x2 Widgets**: Adjusted vertical zone spacing and font auto-scaling to prevent overlap between headers, hero metrics, and bottom visual primitives.
- Bumped version to `1.14.1` (Build 43).

## [1.14.0] - 2026-11-11

### Added & Rebuilt
- **Authentic Frosted Glass Blur for Floating Quick Entry**:
  - Configured native API 31+ window blur behind on `QuickEntryActivity` (`WindowManager.LayoutParams.FLAG_BLUR_BEHIND` with `blurBehindRadius = 120` and `FLAG_DIM_BEHIND` with `0.35f` dim).
  - Adjusted `GlassLevel.EntrySheet` in `KanriGlass.kt` to use translucent frosted glass fills (`0.52f` alpha in dark mode, `0.58f` in light mode) so the launcher wallpaper shines through with authentic blur.
  - Tuned scrim alpha in `AddTransactionDialog.kt` and `SplitExpenseEntryDialog.kt` to `0.28f` in glass mode for optimal contrast and luminosity.
- **Purposeful & Meaningful Widget Visuals**:
  - **Today Spend Widget**: Replaced the ambiguous 24-dot line with an intuitive Spending Pacing Indicator (`SPENT ₹X / LIMIT ₹Y` with percentage used) and 4-session checkpoint breakdown (`MORN · AFTN · EVE · NIGHT`).
  - **Budget Widget**: Implemented dual progress bars — Top Bar displays used budget (`USED: ₹X (Y%)`) while Bottom Bar displays the total budget scale (`TOTAL: ₹Z · ₹W LEFT`) with expected time pacing marker.
  - **Wallets Widget**: Eliminated the arbitrary mini-rise graph; elevated `NET BALANCE` to prominent hero metric in Ndot57 numerals, followed by crisp Cash and Bank account rows with allocation percentages (`CASH (10%)`, `BANK (90%)`).
  - **Lend & Borrow Widget**: Eliminated the arbitrary bottom rise graph; displays top lend/borrow contacts with name and `+₹amount` (lent / receivable) or `-₹amount` (borrowed / payable) in tabular Ndot57 numerals.
  - **Financial Sky Widget**: Ported the official `Weather.Sky` dot-matrix recipe on an 8-pitch grid with sun/moon arc, astronomical moon phases, rays, dawn/dusk horizon glow, stars, drifting clouds, and horizon hills; paired with live financial data (today spent, safe to spend per day, and 7-day spending pacing track).
  - **Month Heatmap Widget**: Redesigned into a true GitHub contribution graph (7 rows Mon..Sun x 5 columns for weeks) with 4 discrete spending intensity levels, day labels (`M`, `W`, `F`), and active today cell indicator.
  - **Streak Widget**: Replaced progress bar with a row of 7 pixelated flame icons (lit for no-spend days, dim for spend days) alongside large Ndot57 streak counter (`XX DAYS NO SPEND`).
  - **Quick Actions Widget**: Replaced generic icons with authentic pixelated dot-matrix glyphs: Expense (pixel minus `-`), Income (pixel plus `+`), and Split (pixel division `÷`).
- Bumped version to `1.14.0` (Build 42).

## [1.13.2] - 2026-11-11

### Fixed & Refined
- **Quick Entry Theme Synchronization & Transparency**:
  - `QuickEntryActivity` respects the active app theme (`ThemeMode.DARK`, `AMOLED`, `LIGHT`, `GLASS`, `SYSTEM`) and keeps the window background fully transparent (`ColorDrawable(Color.TRANSPARENT)`).
  - Dimmed scrim (`0.55f`) overlays directly on top of the user's home screen wallpaper without an opaque backdrop override from `KanriTheme`.
- **Eliminated Cool Blue/Slate Undertones Across Dark Mode**:
  - Replaced Tailwind Zinc palette in `KokonutSpendingRing.kt` with a pure neutral monochrome scale (equal RGB: `#0A0A0A` through `#FAFAFA`).
  - Neutralized `DarkOffWhite` in `Color.kt` and `PixelTheme.kt` from `0xFFF2F2F5` (cool slate) to `0xFFF2F2F2` (pure monochrome off-white).
  - Neutralized `AVATAR_BACKGROUNDS` (replaced slate/mist with charcoal/silver neutral monochrome pairs), `AVATAR_HAIRS`, `AVATAR_SHIRTS`, and `AVATAR_INK` (`0xFF121212`) in `KokonutAvatarPicker.kt`.
  - Added pure monochrome `cursorBrush` to the Display Name `BasicTextField` in `KokonutAvatarPicker.kt`.
  - Provided `LocalTextSelectionColors` globally in `KanriTheme` with pure monochrome handle and highlight colors.
  - Specified explicit monochrome text field colors (`cursorColor`, `focusedBorderColor`, `focusedLabelColor`) in `AddTransactionDialog.kt` and `SplitExpenseEntryDialog.kt`.
  - Neutralized dark base and fallback surfaces in `KanriGlassTokens.kt`, `KanriGlass.kt`, `WidgetsScreen.kt`, and `SettingsComponents.kt`.
- Bumped version to `1.13.2` (Build 41).

## [1.13.1] - 2026-11-11

### Changed & Refined
- **Removed Widget Outlines & White Border Strokes**:
  - Removed all white border stroke and inner rim specular highlights from `WidgetRenderer.kt`, drawing pure solid Obsidian `#08080A` tile backgrounds across all 12 widgets.
  - Removed stroke outline from `app/src/main/res/drawable/widget_bg_pixel.xml`.
  - Re-exported all 12 launcher preview assets (`widget_preview_*.png`) in `res/drawable-nodpi/` to reflect the borderless aesthetic in the home screen widget picker.
- Bumped version to `1.13.1` (Build 40).

## [1.13.0] - 2026-11-11

### Added & Enhanced
- **Interactive Stepper Component & Onboarding Slider Integration**:
  - Created `com.omkarnub.kanri.ui.common.stepper.Stepper.kt` implementing the React Stepper design system in idiomatic Jetpack Compose.
  - Supports `StepItem`, `StepperVariant` (`CIRCLE`, `CIRCLE_ALT`, `LINE`), `StepperOrientation` (`HORIZONTAL`, `VERTICAL`), and `StepperSize` (`SM`, `MD`, `LG`).
  - Features smooth animated checkmark transitions (`scaleIn + fadeIn`), animated horizontal connector lines (`animateColorAsState`), interactive step clicks with haptic feedback, and error/loading states.
  - Integrated persistent top Stepper navigation header across all onboarding slides (Profile, SMS, Apps, Overlay, Battery, Alerts, Lock) in `OnboardingScreen.kt` with live step counter (`STEP X OF 7 · LABEL`), back button, and removed redundant bottom dots.
- **Widget Corner Border Highlight Engine**:
  - Updated `PixelTheme.kt` with `COLOR_BORDER_HIGHLIGHT_INT = 0x75FFFFFF` (46% opacity) and `COLOR_BORDER_LOWLIGHT_INT = 0x24FFFFFF`.
  - In `WidgetRenderer.kt`, inset `tileRect` by `strokeWidth / 2f + 0.8f` to eliminate canvas edge clipping on rounded corners, applied a top-to-bottom specular `LinearGradient` shader, and added a secondary inner specular rim along upper corners for luminous edge visibility.
- **Launcher System Widget Picker Previews**:
  - Exported authentic 1:1 previews for all 12 widgets into `res/drawable-nodpi/` (`widget_preview_*.png`).
  - Configured `android:previewImage` across all 12 `<appwidget-provider>` XML info files.
  - Cleaned up `widget_tile_canvas.xml` and removed `android:previewLayout` so the launcher displays the unique authentic preview for every widget.
- **Home Screen Floating Quick Entry Modal**:
  - Created `QuickEntryActivity.kt` styled with transparent `Theme.Kanri.QuickEntry`, `excludeFromRecents="true"`, and `taskAffinity=""`.
  - Tapping "Expense" / "Income" / "Split" on `QuickActionsWidgetProvider`, or tapping the `Today` or `SafeToSpend` widgets directly, brings up the tactile numpad and manual transaction dialog immediately over the user's home screen without opening `MainActivity`.
  - Saves directly to Room database, plays burst animation update across all widgets, displays quick toast, and dismisses back to the launcher instantly.
- Bumped version to `1.13.0` (Build 39).

## [1.12.2] - 2026-11-10

### Fixed & Enhanced
- **Zero-Overlap Widget Geometry & Fluid Animation System**:
  - **Eliminated All Text-Visual Overlaps**:
    - Architected strict adaptive vertical zones partitioning Header, Hero Metric, and Bottom Visual.
    - Added compact height detection (`isCompactH`) for `2x1` and `4x1` widget configurations, dynamically scaling hero metric typography and omitting bottom axis labels to guarantee ample breathing room.
    - Completely resolved Budget `2x1` overlap where remaining amount collided with slider ticks.
    - Resolved Review Inbox `2x1` overlap where "Clear" collided with session blocks.
    - Resolved Financial Sky celestial arc overflow by calculating strictly bounded parabolic trajectories `x(t), y(t)` inside the recessed well.
  - **Comprehensive Fluid Animation Engine**:
    - Added interactive `progress: Float = 1.0f` driving all `DotVisuals` primitives:
      - **Slider**: Marker block slides from 0 to target position; lit ticks sweep in from left to right.
      - **Steps**: Histogram bars grow fluidly upward from the baseline.
      - **Dots**: 24-hour timeline dots sweep across with an expanding cyan ring for the active hour.
      - **Blocks**: Review session blocks illuminate sequentially.
      - **Rise**: Proportional stepped tiers rise cleanly step by step.
      - **Grid**: Month calendar cells illuminate with animated intensity.
      - **Arc**: Celestial sun/moon traverses its sky path smoothly.
    - Enabled 60fps animations across all interactive preview cards in `WidgetsScreen`.
    - Maintained 8-frame burst animation on home screen placement and updates in `BasePixelWidgetProvider`.
  - **Dues (Lend & Borrow) Visual Polish**:
    - Replaced 2-dot steps with a 6-tier stepped `DotVisuals.drawRise` gauge displaying Mint receivable ratio.
  - **Today Dot Timeline Polish**:
    - Refined vertical clearances between 24-hour timeline dots and bottom "00H" / "23H" labels.
  - **Autonomous Emulator Verification**:
    - Captured and visually verified live screenshots across all 12 widgets in the in-app gallery and on the real Android home screen.
- Bumped version to `1.12.2` (Build 38).

## [1.12.1] - 2026-11-10

### Fixed & Enhanced
- **Pixel DotTiles Typography & Visual Polish Overhaul**:
  - **Authentic Ndot57 Font Integration**: Embedded official Nothing `Ndot57-Regular.otf` (`res/font/ndot57.otf`) for all hero numerals, currency amounts, percentages, and metrics.
  - **Louis Studios & Weather Hierarchy**:
    - Replaced crude manual 5x7 dot circle loops with crisp system sans (`sans-serif-medium`) for titles, subtitles, currency signs (`₹`), and axis labels.
    - Sized and baseline-aligned currency prefix (`₹`) with Ndot57 numerals without tofu boxes or glyph truncation.
  - **Eliminated Text Elongation & Distortion**:
    - Changed `widget_tile_canvas.xml` and `widget_quick_actions.xml` from `scaleType="fitXY"` to `scaleType="fitCenter"` with `adjustViewBounds="true"`, strictly preventing launcher stretching.
    - Updated `BasePixelWidgetProvider` to use orientation-aware target dimensions (`minW` in portrait, `maxH` in landscape) with density scaling.
  - **Refined Data Visuals**:
    - `Slider`: 44 fine vertical ticks with rounded block marker (GLOW/FILL modes) and clean sans endpoints.
    - `Steps`: Equal-width histogram bars with faint slot guide lines.
    - `Dots`: 24 hourly dots with density-scaled radii and active current hour indicator ring.
    - `Vector Icons`: Integrated official Louis Studios SVG path icons (`spark`, `flame`, `clock`, `pulse`, `bolt`, `battery`, `drop`, `moon`, `sun`, `heart`, `plus`, `inbox`, `split`).
    - `Sky Scene`: 8-pitch dot well featuring astronomical sun arc with rays, synodic moon phase, twinkling night stars, rolling hill horizon terrain, and 7-day spending pulse.
  - **Unit Testing**: Added `PixelFontTest` covering Indian currency formatting, prefix splitting, tabular width measurement, and JVM fallback safety.
- Bumped version to `1.12.1` (Build 37).

## [1.12.0] - 2026-11-10

### Rebuilt & Unified
- **Single Pixel DotTiles Widget System**:
  - Rebuilt the entire widget subsystem from scratch around ONE visual language: **Pixel DotTiles** (7-row dot-matrix).
  - Completely deleted legacy iPhone Fluid and 100x Liquid Glass renderers, layouts, drawables, enums (`WidgetStyle`), style preferences, and style switcher UI.
  - Established a unified design system in `com.omkarnub.kanri.widget.core`:
    - Strict palette: Obsidian tile `#08080A`, 1px border `#24FFFFFF`, corner radius `22dp`, dot on `#F2F2F5`, dot dim `#1E1E24`, dot mid `#6B6B76`, semantic accents (mint `#30D158`, amber `#FFB020`, coral `#FF453A`, cyan `#4DD0E1`).
    - 7-row compact array bitmap font (`DotMatrixFont`) with tabular monospace digits, reveal progress, and Indian compact currency formatting (`₹1.2k`, `₹12.4k`, `₹1.2L`, `₹1.5Cr`).
    - Shared visuals library (`DotVisuals`): Slider, Steps, Dots, Blocks, Rise, Grid, Arc, 7x7 Icons.
    - Pure Canvas engine (`WidgetRenderer`) shared 1:1 between home screen RemoteViews bitmaps and Jetpack Compose in-app previews.
  - Final 12-widget lineup:
    1. **Today Spend** (`TodayWidgetProvider`, 2x2, 4x2)
    2. **Budget** (`BudgetWidgetProvider`, 2x1, 2x2, 4x2)
    3. **Wallets** (`WalletsWidgetProvider`, 2x2, 4x2)
    4. **Savings Goals** (`GoalsWidgetProvider`, 2x2, 4x1)
    5. **Dues** (`DuesWidgetProvider`, 2x2, 4x2)
    6. **Financial Sky** (`SkyWidgetProvider`, 4x2)
    7. **Month Heatmap** (`HeatmapWidgetProvider`, 4x3, 4x2)
    8. **No-Spend Streak** (`StreakWidgetProvider`, 2x2, 4x1)
    9. **Safe to Spend** (`SafeToSpendWidgetProvider`, 2x2, 4x1)
    10. **Review Inbox** (`ReviewInboxWidgetProvider`, 2x1, 2x2)
    11. **Month Delta** (`MonthDeltaWidgetProvider`, 2x2, 4x2)
    12. **Quick Actions** (`QuickActionsWidgetProvider`, 4x1, 2x1)
  - Interactive Preview Gallery in `WidgetsScreen` categorized under *Money*, *Insights*, and *Actions* with size chips, 60fps animations, tap-to-replay, and pin-to-homescreen action.
  - Comprehensive unit test suite covering font tabular measurement, compact currency formatting, streak calculations, safe-to-spend allowance, same-day month delta comparison, heatmap quantization, and astronomical moon phase math.
- Bumped version to `1.12.0` (Build 36).

## [1.11.1] - 2026-11-09

### Added & Widgets
- **Financial Sky Weather Widget (`FinancialWeatherWidgetProvider` / `FinancialSkyScene`)**:
  - Implemented the 9th Kanri home screen widget based on the dot-matrix Weather slab specification, adapted specifically for financial ledger intelligence.
  - **Dot-Matrix Celestial Sky Well**:
    - Astronomical sun arc stepping with time of day (sunrise 7:30 to sunset 19:30), stepped rays, and flying birds.
    - True synodic astronomical moon phase calculations (`moonAge` and `isMoonLit`) with twinkling star field at night.
    - Dot-matrix terrain contour hills on the horizon.
    - Weather condition dynamically reflecting financial climate: *"Clear Horizons"*, *"Golden Sunrise"*, *"Partly Cloudy"*, *"Stormy Outflow"*, or *"Starry Calm"*.
  - **7-Day Spending Pulse**:
    - Replaces weather forecast with a 7-day financial pulse table.
    - Displays daily expenditure across past 7 days on a shared 20-dot scale, colored by spending intensity (Green under budget, Amber moderate, Red heavy spend).
  - **Available in All 3 Styles & Home Screen**:
    - Supported across `iPhone Fluid`, `100x Apple Liquid Glass`, and `Pixel DotTiles`.
    - Registered `FinancialWeatherWidgetProvider` for launcher placement.
    - Added 1:1 interactive preview to `WidgetsScreen` in Settings.
- Bumped version to `1.11.1` (Build 35).

## [1.11.0] - 2026-11-09

### Added & Widgets Recreation
- **Triple Widget Engine (iPhone Fluid, 100x Liquid Glass, Pixel DotTiles)**:
  - Recreated all 8 Kanri home screen widgets across three distinct design systems:
    1. **iPhone Fluid Style (`WidgetStyle.IPHONE`)**:
       - Apple Human Interface Guidelines (HIG) aesthetics with continuous squircle corners (`24dp`), concentric borders, and Cupertino vibrant accents (Coral `#FF2D55`, Mint `#30D158`, Electric Blue `#0A84FF`, Indigo `#5E5CE6`).
       - Apple Fitness Activity concentric progress rings with overlapping shadow depth on Canvas, Apple Health micro bar charts, and dual Apple Wallet card stack representation.
       - Fluid spring physics animations (`spring(dampingRatio = 0.75f, stiffness = 300f)`).
    2. **100x Liquid Glass Style (`WidgetStyle.GLASS_100X`)**:
       - Elevated our glass widgets to 100x utilizing Apple WWDC25 Liquid Glass specular reflections and deep translucent layering.
       - High-specular top-rim shine gradient (60% white top to 5% bottom edge), ambient radial colored backlight bloom behind main values and charts, shimmering reflection sweep animations, and glowing neon ring arcs (Cyan to Emerald, Violet to Indigo).
    3. **Pixel DotTiles Style (`WidgetStyle.PIXEL_MATRIX`)**:
       - Complete native Android & Compose port of the Louis Studios DotTiles specification.
       - 7-row dot-matrix font renderer for numbers and glyphs (`0-9`, `:`, `.`, `-`, `₹`, `$`, `%`, `k`, `M`, `/`).
       - Animated counting up dot-by-dot on entry, live organic jitter drifting every few seconds, and 5 distinct bottom data visuals: fine tick slider with marker block (glow/fill mode), stepped histogram, dot timeline, sparse blocks, and 4-tier rise gauge.
       - Matte obsidian black glass tile (`#08080A`) with subtle 1px border.
- **Dedicated Widgets Section in Settings (`WidgetsScreen`)**:
  - Added dedicated **"Widgets"** entry in `SettingsScreen` under **`CUSTOMISATION`**.
  - Opens `WidgetsScreen` featuring live 1:1 interactive previews of all 8 widgets:
    1. **Money Spent Today**
    2. **Cash & Online Wallet Balances**
    3. **Budget Ring Chart**
    4. **Budget Progress Bar**
    5. **Savings Goals Ring**
    6. **Lend & Borrow Dues**
    7. **Quick Record Action Tiles**
    8. **Split the Bill Calculator**
  - **Interactive Features**:
    - Top segmented style switcher (` iPhone Fluid` | `✦ 100x Glass` | `⠶ Pixel DotTiles`).
    - One-tap "Set as Home Screen Widget Style" button applying the selected style across all home screen widgets via `KanriWidgetsUpdater` and persistent `WidgetPreferences`.
    - One-tap "Pin to Home Screen" shortcut on every preview card utilizing `AppWidgetManager.requestPinAppWidget`.
    - Tap-to-replay animation on any widget card with haptic feedback.
- **Home Screen RemoteViews Dynamic Theming**:
  - Added background drawables: `widget_bg_iphone.xml`, `widget_bg_glass_100x.xml`, and `widget_bg_pixel.xml`.
  - Updated `WidgetCanvasRenderer` and all 8 `AppWidgetProvider` classes (`BudgetRingWidgetProvider`, `GoalsRingWidgetProvider`, `MoneySpentTodayWidgetProvider`, `WalletBalanceWidgetProvider`, `BudgetProgressWidgetProvider`, `LendBorrowWidgetProvider`, `QuickAddWidgetProvider`, `SplitBillWidgetProvider`) to dynamically apply background cards and canvas chart graphics based on `WidgetPreferences.widgetStyle`.
- Bumped version to `1.11.0` (Build 34).

## [1.10.0] - 2026-11-09

### Added & Auto-Categorization
- **Pre-Seeded Smart Auto-Categorization Engine (`PreseededSmartRules`)**:
  - Implemented an extensive offline directory of smart categorization rules pre-seeded directly into Room's `counterparty_category_map` on app launch/update.
  - Covers hundreds of domestic and international brands, apps, transport networks, and merchants across all major spending domains:
    - **Railways & Metros**: IRCTC, Indian Railways, CRIS, UTS, and all 17 Indian metro networks (DMRC, Mumbai Metro, Namma Metro, CMRL, Maha Metro, Pune Metro, Kochi Metro, etc.) auto-mapped to *Train & Metro*.
    - **Airlines & Airports**: IndiGo, Air India, Akasa, SpiceJet, Vistara, international carriers, major airport authorities (Adani, GMR, AAI), city airport codes (CSMIA Mumbai, IGI Delhi, KIA Bangalore, RGIA), duty free, and airport lounges auto-mapped to *Travel & Vacation*.
    - **Cabs & Transit**: Uber, Ola, Rapido, Namma Yatri, BluSmart, inDrive, Jugnoo, and state bus transport corporations (BEST, DTC, BMTC, MSRTC, KSRTC) auto-mapped to *Auto / Taxi Fare*.
    - **Fuel & Tolls**: Indian Oil, BPCL, HPCL, Shell, Nayara, Jio-bp, NETC FASTag, NHAI toll plazas, and EV charging networks auto-mapped to *Transport & Fuel*.
    - **Food & Dining**: Swiggy, Zomato, Domino's, McDonald's, Starbucks, KFC, Burger King, Chaayos, CCD, Third Wave, Blue Tokai, cloud kitchens, and regional eateries auto-mapped to *Food & Dining*.
    - **Groceries**: Blinkit, Zepto, BigBasket, Swiggy Instamart, DMart, Reliance Fresh/Smart, JioMart, Spencer's, Country Delight, Licious, and dairies (Amul, Mother Dairy, Nandini) auto-mapped to *Groceries*.
    - **Shopping**: Amazon, Flipkart, Myntra, Ajio, Meesho, Nykaa, Tata CLiQ, Croma, Reliance Digital, Zudio, Westside, Decathlon, Lenskart, Tanishq, IKEA auto-mapped to *Shopping*.
    - **Utilities & Bills**: Jio, Airtel, Vi, BSNL, power utilities (Tata Power, Adani Electricity, BESCOM, MSEDCL, BSES), piped gas (MGL, IGL), DTH, broadband, and water boards auto-mapped to *Bills & Utilities*.
    - **Entertainment**: Netflix, Prime Video, Hotstar, JioCinema, BookMyShow, PVR INOX, Spotify, YouTube, gaming platforms (Steam, PlayStation, Xbox, BGMI) auto-mapped to *Entertainment*.
    - **Health & Medical**: Apollo Pharmacy, 1mg, PharmEasy, Netmeds, MedPlus, diagnostics (Lal PathLabs, SRL, Thyrocare), and hospitals (Max, Fortis, Manipal, Medanta) auto-mapped to *Health & Medical*.
    - **Education**: PhysicsWallah, Unacademy, Byju's, Coursera, Udemy, Allen, Aakash auto-mapped to *Education*.
    - **Investments & Insurance**: Zerodha, Groww, Upstox, INDmoney, AMCs, LIC, health/general insurance (Star Health, Niva Bupa, HDFC ERGO) auto-mapped to *Investment & Savings*.
    - **Rent & Society**: NoBroker, MyGate, Housing.com, society maintenance auto-mapped to *Rent & Maintenance*.
    - **Personal Care**: Urban Company, Naturals, Lakme, Jawed Habib, Kaya, D2C grooming brands auto-mapped to *Personal Care*.
    - **Gifts & Donations**: FNP, IGP, Ketto, Milaap, PM CARES, temple trusts auto-mapped to *Gifts & Donations*.
  - **Full User Control**: Rules stay safely in the local Room database (`counterparty_category_map`). Users can search, filter, edit, or delete any auto-categorization rule inside the Merchant Rules screen (`SmartMerchantRulesScreen`). Deleted rules are never re-added automatically.
  - **Automatic Detection**: Incoming SMS alerts and payment notifications auto-resolve the exact category without requiring manual input.
- Bumped version to `1.10.0` (Build 33).

## [1.9.0] - 2026-11-09

### Added & Motion Design
- **Cinematic Logo Reveal Animated Splash Screen (`KanriSplashScreen`)**:
  - Implemented the signature Apple-grade vector logo reveal animation based on official brand assets (`panchang_light` / `panchang_dark`).
  - **Staggered Letter Slide-In**: Each of the 5 letters (`K`, `A`, `N`, `R`, `I`) animates up through an individual clip mask boundary from `translateY(+110%)` to `0%` with a smooth deceleration curve (`CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`) and staggered delay (220ms + index * 100ms).
  - **Animated Underline Sweep**: Thin accent rule draws across beneath the mark from left to right (origin left-center) and then wipes away cleanly to the right (origin right-center) with `CubicBezierEasing(0.7f, 0f, 0.3f, 1f)`.
  - **Render-Gated App Transition**: Splash screen stays active until the main application Compose hierarchy and databases are fully rendered and measured (via double `withFrameNanos`), seamlessly transitioning only once the screen underneath is ready with zero blank flickers.
  - **Fluid Staggered Exit & Content Scale-Up**: Upon render completion, the letters slide up and out staggered into the ceiling (`translateY(0%)` -> `-110%`) while the splash overlay dissolves (`alpha = 1f -> 0f`) and the main app smoothly scales in with `KanriMotion.SnappySpring`.
  - **Clean Unmount**: After exit transition completes, the splash composable is completely removed from the composition to ensure 0 CPU/GPU memory footprint during regular app usage.
  - **Theme-Aware Void**: Pure pitch black (`#000000`) void in Dark/Glass/AMOLED themes with warm cream ink (`#FDFAEE`), and soft cream (`#FEFAEC`) void with charcoal ink (`#121212`) in Light mode.
- Bumped version to `1.9.0` (Build 32).

## [1.8.2] - 2026-11-09

### Fixed & UI Polish
- **Removed Preview Payment Detection Popup from Settings**:
  - Removed "Preview Payment Detection Popup" and testing triggers from `AutomationSettingsCard` in `SettingsComponents.kt` and cleaned up testing artifacts to ensure production polish.
- **Frosted Blur Added to Live Shader Backgrounds & Theme Preview Cards**:
  - Upgraded unselected live shader items in `ThemeSettingsScreen.kt` from `GlassLevel.Flat` (0 blur) to `GlassLevel.Blur` with subtle glass specular border, providing rich frosted blur to the entire live background list.
  - Upgraded all 5 theme preview cards (`ThemePreviewCard` in `SettingsComponents.kt`) to utilize `KanriGlassCard(level = if (isSelected) GlassLevel.Strong else GlassLevel.Blur)` with specular borders and shadows in Glass theme.
- **Added Frosted Glass Blur to Recent Transactions**:
  - Updated `RecentTransactionsSection` on `HomeScreen.kt` to consistently wrap the transaction list in `KanriGlassCard(level = GlassLevel.Blur, shape = RoundedCornerShape(tokens?.cornerRadius ?: 20.dp))` across all themes with live frosted blur in Glass theme and elevated surface container card in non-glass modes.
  - Wrapped recent transactions in `WalletSheet.kt` in `KanriGlassCard(level = GlassLevel.Blur)` for consistent frosted glass styling.
- **Dynamic Variable Tint for Category Distribution Pill**:
  - Replaced hardcoded `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)` in `CategoryPreviewPill` and legend in `ExpenseVisualizer.kt` with variable tokens `tokens?.tintFlat` and `tokens?.borderEnd`, ensuring the pill dynamically follows the user's custom glass tint and lighting settings.
- **Cleaned Budget Cycle Start Day Dialog**:
  - Removed "POPULAR START DAYS" preset chips and `quickDays` list from `BudgetCycleStartDayDialog` in `SettingsComponents.kt`, simplifying the dialog to a clean selected day summary card and 1..28 slider.
- **Lend & Borrow Search Contacts Bar Blur & Circular Edges**:
  - Applied full circular pill radius (`RoundedCornerShape(26.dp)`) and frosted glass blur (`kanriGlass(level = GlassLevel.Blur)`) to the contact search bar in `LendingPeopleView.kt` with transparent container color and dynamic border.
- Bumped version to `1.8.2` (Build 31).

## [1.8.1] - 2026-11-09

### Fixed
- **Custom Statement Date Range Picker Apply Button Visibility & UX**:
  - Constrained `KanriGlassDatePickerDialog` to `fillMaxHeight(0.85f)` and added `Modifier.weight(1f)` to `DateRangePicker`, preventing the calendar grid from expanding off-screen and pushing action buttons below the display.
  - Replaced subtle `TextButton` with prominent, high-contrast Material 3 `Button` (`containerColor = MaterialTheme.colorScheme.primary`) for "Apply" and `OutlinedButton` for "Cancel".
  - Pre-populated default start date (first day of active month) and end date (today) upon opening dialog so date ranges are active immediately.
  - Added single-date support (`end = selectedEndDateMillis ?: start`) and enabled Apply button whenever a valid start date is picked.
- Bumped version to `1.8.1` (Build 30).

## [1.8.0] - 2026-11-09

### Added & Financial Statements
- **Custom Date Range Financial Statements**:
  - Added `ExportRange.Custom(startMillis, endMillis)` to `StatementExportRepository`.
  - Added dedicated "Custom Range" chip in `ExportStatementSheet` with date range formatting (`dd MMM – dd MMM`).
  - Integrated `KanriGlassDatePickerDialog` wrapping Material 3 `DateRangePicker` allowing users to pick custom date ranges from any start date to any end date.
  - Generates custom period titles and safe filenames (e.g. `kanri_statement_custom_YYYYMMDD_YYYYMMDD.pdf`).
- **Clean Minimal PDF Statements with Kanri Logo**:
  - Re-engineered `PdfStatementExporter` with an Apple-grade, editorial letterhead design on a clean white canvas with subtle `#E2E8F0` hairline rules.
  - High-res Kanri Vector Logo: Vector-renders the official `KANRI` Panchang mark from `R.drawable.panchang_dark` with dynamic non-transparent bounding box cropping for crisp vector branding in the statement header.
  - Currency-Aware Formatting: Replaced hardcoded currency symbols with user's selected primary currency via `CurrencyUtils.formatCurrency`.
  - Executive KPI Cards: Clean summary cards for Total Received (emerald), Total Spent (crimson), and Net Cash Flow with transaction counts and surplus/deficit badges.
  - Expense Breakdown: Horizontal category cards with percentage progress bars.
  - Ledger Table: Sleek transaction table with transfers (`⇄`), debits, credits, and multi-page pagination.
- **New Pixel Art Animated Backgrounds**:
  - Replaced `fuji_sakura.html` with new 9:16 pixel hanging scroll artwork featuring Mt. Fuji, golden mist, pagoda, torii gate, koi fish, cranes, and cherry blossom physics.
  - Added `twilight_meadow.html` ("Twilight Meadow"): Pixel art twilight alpine lake with jagged moonlit peaks and swaying wildflower asters.
  - Added `tokyo_night.html` ("Tokyo Night"): Pixel art Tokyo night skyline vista with mountain bench, companion cat, and city lights.
  - Registered `TWILIGHT_MEADOW` and `TOKYO_NIGHT` in `GlassBackgroundType` and added custom color gradients and descriptions in `SettingsComponents.kt`.
  - Tuned CSS with `object-fit: cover; width: 100vw; height: 100vh; image-rendering: pixelated;` for sharp, full-screen coverage across all phone aspect ratios.
- Bumped version to `1.8.0` (Build 29).

## [1.7.0] - 2026-11-09

### Added & Fluid Motion Polish
- **Apple-Grade Fluid Spring Animations & Layout Transitions**:
  - **Transaction List Item Transitions (`HomeScreen`)**:
    - Wrapped recent transactions in `AnimatedContent` (`SnappySpring` + `EaseOut` crossfade) so toggling between empty and populated states transitions seamlessly.
    - Added `.animateContentSize(spring(0.82f, Spring.StiffnessMediumLow))` to transaction columns, enabling smooth layout gliding when transactions are inserted or deleted without jarring jumps.
    - Upgraded `TransactionListItem` with `kanriPressFeedback(pressedScale = 0.98f, haptic = true)` for tactile Apple-style spring press feedback on touch.
  - **Needs-Review Card Collapse (`HomeScreen` & `NeedsReviewCard`)**:
    - Replaced hard conditional render with smooth `expandVertically(spring(0.82f, Spring.StiffnessMediumLow))` on appearance and `shrinkVertically(tween(200, EaseOut))` on resolution, preventing feed jumping.
  - **Directional Month Navigation Transitions (`MonthViewScreen`)**:
    - Wrapped month title ("MMMM yyyy") in `AnimatedContent` with directional horizontal slide (`slideInHorizontally` + `slideOutHorizontally` + `EaseOut`) based on forward vs backward month selection.
    - Added `kanriPressFeedback(pressedScale = 0.88f)` to month arrow buttons.
  - **Physical Spring Gauge & Celebration Transitions (`GoalsScreen`)**:
    - Upgraded `CircularGoalProgressGauge` from arbitrary 800ms tween to physical `spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)`.
    - Added celebratory `scaleIn(KanriMotion.BouncySpring)` + `fadeIn` entrance animation for the "Achieved!" milestone badge.
  - **Fluid Segmented Indicator & Direction Toggle (`MoveMoneyDialog`)**:
    - Implemented animated color transitions (`animateColorAsState` with `KanriMotion.EaseOut`) across background, border, text, and subtext when toggling between ATM/Withdraw and Deposit.
  - **Tactile Quick Amount & Stepper Feedback (`SetBudgetDialog`)**:
    - Added `kanriPressFeedback(pressedScale = 0.94f)` to all quick increment pills (`+10`, `+50`, `+500`, etc.) and `kanriPressFeedback(pressedScale = 0.88f)` to `[-]` and `[+]` stepping buttons.
  - **Directional Tab Switching & Press Feedback (`LendingScreen`, `LendingPeopleView`, `LendingTimelineView`)**:
    - Integrated `KanriMotion.tabTransition` across "People" and "Timeline" tabs for directional slide transitions with momentum.
    - Added `kanriPressFeedback(pressedScale = 0.96f)` to `LendingSegmentedControl` tab options and `KanriMotion.EaseOut` color interpolation.
    - Upgraded settled section expand/collapse in `LendingPeopleView` to spring-driven `expandVertically` / `shrinkVertically`.
    - Added `kanriPressFeedback(pressedScale = 0.98f)` to `PersonRowItem` and timeline transaction cards.
  - **Expense Visualizer Style Transitions & Tactile Controls (`ExpenseVisualizer`)**:
    - Upgraded visualizer style switcher crossfade to `fadeIn(220, EaseOut) + scaleIn(0.96f, SnappySpring)` together with `fadeOut(160, EaseOut) + scaleOut(0.98f)`.
    - Added `kanriPressFeedback(pressedScale = 0.95f)` to "Edit Budget", "View by", and month dropdown pills.
- Bumped version to `1.7.0` (Build 28).

## [1.6.0] - 2026-11-09

### Added & Dynamic Enhancements
- **Dynamic Salary / Budget Cycle Start Day**:
  - Unlocked monthly budgeting from rigid calendar Day 1 (`set(Calendar.DAY_OF_MONTH, 1)`) via new `BudgetPreferences`.
  - Added user-configurable **Budget Cycle Start Day** setting (1 to 28/31) under *Settings -> Wallets & Currency*, allowing salaried users to align their budget directly with their monthly payday (e.g. Day 25 or Day 28).
  - Enhanced `BudgetCalculator` with `getStartOfCycleMillis`, `getEndOfCycleMillis`, `getDaysRemainingInCycle`, and `getCurrentCycleDisplayName` so daily safe-to-spend, days remaining, and burn rates dynamically track the active cycle.
- **Currency-Aware Dynamic Default Monthly Budget**:
  - Replaced hardcoded `DEFAULT_MONTHLY_BUDGET = 20000.0` with dynamic per-currency defaults across all 52 supported global currencies in `AppCurrency` (e.g., $2,500 for USD, €2,200 for EUR, ¥300,000 for JPY, ₹25,000 for INR, 700 KD for KWD).
  - Synchronized `SetBudgetDialog` and `FinancialHealthCalculator` to scale default inputs and step increments dynamically.
- **Currency-Tailored Numpad Quick Amount Presets**:
  - Replaced hardcoded `listOf(100, 500, 1000, 2000)` presets in `KokonutAmountPad` with `CurrencyUtils.getCurrency().quickPresets`.
  - Presets dynamically adjust to appropriate denominations: USD/EUR/GBP/CHF (`+5, +10, +20, +50`), CAD/AUD/SGD (`+10, +20, +50, +100`), JPY (`+500, +1000, +5000, +10000`), INR (`+100, +500, +1000, +2000`), KRW (`+1000, +5000, +10000, +50000`), etc.
- **Dynamic Contextual Category Suggestions in Instant Payment Popup**:
  - Replaced static database-order category pills in `InstantPopupService` and `InstantPopupCard` with a dual-factor dynamic ranking engine:
    1. **Historical Frequency**: Prioritizes categories the user spends on most frequently based on recent ledger transactions.
    2. **Time-of-Day Context**: Contextually weights categories according to transaction time (e.g., Morning -> Food/Cafe/Transit/Fuel; Midday -> Food/Groceries/Shopping; Evening -> Transit/Dining/Groceries; Night -> Dining/Entertainment).
    3. Resolved/merchant-mapped category is placed at index 0 with selected checkmark, followed by the dynamically ranked pills.
- Bumped version to `1.6.0` (Build 27).

## [1.5.5] - 2026-11-09

### Fixed
- **Eliminated Faint Purplish/Pinkish Radial Glow Circle on Backgrounds**:
  - Removed hardcoded legacy ambient radial glow mesh (`Color(0xFF6366F1)` indigo/purple and `Color(0xFFEC4899)` pink circles) from `GlassBackdrop.kt`.
  - Restored pure, vibrant, uncluttered color fidelity across all live WebGL shader backgrounds (Prism Swirl, Gemini Aurora, Sunset Meadow, Thistle Portico, Cloudscape, etc.).
- Bumped version to `1.5.5` (Build 26).

## [1.5.4] - 2026-11-09

### Changed
- **Live Shader Backgrounds Refactored to Minimalist List View**:
  - **Single-Column List View**: Replaced the 2-column grid layout in `ThemeSettingsScreen` with a clean, full-width vertical list layout for all 10 live WebGL shader backgrounds.
  - **Removed Previews**: Eliminated heavy gradient swatch previews / boxes in favor of a clean, distraction-free typographic layout showing shader title and description.
  - **Dedicated "Apply" Action Button**: Placed a responsive Apply button beside each shader item:
    - Active shader displays "Applied" in primary container color with a checkmark icon (`Icons.Default.Check`).
    - Inactive shaders display an "Apply" outlined button that immediately activates the shader and triggers subtle haptics.
    - Whole row remains interactive and clickable for seamless one-tap selection.
- Bumped version to `1.5.4` (Build 25).

## [1.5.3] - 2026-11-09

### Added
- **Post-Confirmation "Current Balance" View with Rolling Text Reel Animation & Bracketed Delta**:
  - **Down-to-Up Transition**: On slide confirmation completion (`onSuccessComplete`), `InstantPopupCard` executes a smooth physical down-to-up transition (`slideInVertically` from bottom + `fadeIn` paired with the exiting form's `slideOutVertically` upward + `fadeOut` and animated `SizeTransform`) into the Current Balance view.
  - **GSAP-Grade `RollingText` Reel Component**:
    - Ported slot-machine/rolling digits physics to Jetpack Compose using deterministic `mulberry32` PRNG.
    - Configured per-character vertical reels (`copies = minCycles + cycleVariance + 1`) that roll through rotating digits before locking into the exact target character.
    - Applied exponential deceleration easing (`CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)`) with individual character duration variance.
    - Strictly enforced `GoogleSansFlex` font across all text, numbers, and symbols in the popup.
  - **Bracketed Added/Deducted Amount Display**: Renders the transaction delta right beside the balance in brackets `[Amount with rolling animation](+amount/-amount)` with Vibrant Red (`#E54D2E`) for expense and Sage Green (`#30A46C`) for income.
  - **Live Dynamic Balance Extraction**: Resolves balance directly from the transaction's bank SMS (`SmsParser.extractBalance`), recent account ledger history, or wallet calculations.
  - **Timed Auto-Dismissal**: Automatically dismisses the popup overlay after 3 seconds, while retaining instant manual drag-down to dismiss at any moment.
- **Natural Drag-Down to Dismiss for Payment Detection Popup Card**:
  - Implemented 1:1 Apple Fluid Design direct touch manipulation on `InstantPopupCard` allowing the user to naturally swipe/drag down anywhere on the card to dismiss.
  - **Tactile Drag Handle Affordance**: Added a centered 36×4 dp rounded capsule pull handle (`RoundedCornerShape(50)`) at the top of the card in subtle dim grey (`dimGrey.copy(alpha = 0.35f)`).
  - **Rubberband Upward Elasticity**: When the user drags upwards, exponential decay resistance asymptotically limits movement to -24 dp and springs back to 0 on release.
  - **Fluid Dismiss & Flick Thresholds**:
    - Commits to dismiss when pulled down past 90 dp, smoothly springing off-screen (`animateTo(450.dp)`) with subtle tactile haptic confirmation (`haptics.click()`).
    - Downward velocity flick detection (> 1000 px/s with offset > 20 dp) triggers instant animated dismissal.
    - If released before threshold, card snaps back to resting position using an interruptible spring (`Spring.StiffnessMediumLow`, `dampingRatio = 0.82f`).
  - **Directional Gesture Disambiguation**: Horizontal gestures (`abs(dx) > abs(dy) * 1.15f`) are gracefully handed off to category pill horizontal scrolling and the `KokonutSlideToConfirm` slider without interference or accidental dismissal.
  - Dynamic opacity fading (`graphicsLayer`) as card is pulled down for enhanced spatial depth.
- Bumped version to `1.5.3` (Build 24).

## [1.5.2] - 2026-11-09

### Added & Refined
- **Slide-to-Confirm Slider in Payment Detection Popup**:
  - Replaced the static "Done" button in `InstantPopupCard` with Kanri's signature 1:1 Apple Fluid Design `KokonutSlideToConfirm` slider.
  - **Dynamic State Lifecycle**:
    - **Idle**: Displays "Slide to save expense" (for debits) or "Slide to save income" (for credits) with tactile draggable thumb.
    - **Confirming**: Seamless ink flooding across the track with a centered spinner and "Saving…".
    - **Done**: Morphs into vibrant Emerald Green with checkmark icon and confirmation label ("Expense saved!" / "Income added!").
    - **Success Confirmation**: Triggers `haptics.success()` and holds the emerald confirmation state for 850ms before smoothly initiating the pop-down dismissal animation (`dismissWithAnimation`).
  - Seamlessly integrated with asynchronous Room database persistence in `InstantPopupService.kt` on `Dispatchers.IO`.
- Bumped version to `1.5.2` (Build 23).

## [1.5.1] - 2026-11-07

### Fixed
- **Opaque Backgrounds & Contrast for Entry Workflows in Dark and Light Modes**:
  - Fixed `KanriGlassCard` when rendering `GlassLevel.EntrySheet` in solid themes (Light, Dark, AMOLED, and System), replacing the missing-haze transparent fallback with clean, 100% opaque surface backgrounds (`surfaceContainerHigh` in Dark/AMOLED mode and pure `#FFFFFF` in Light mode).
  - Swapped out cool blueish-slate tones in Dark mode across numpad buttons (`TactileKeypadKey`, `TactilePresetChip`), Continue CTA button, summary list container (`KokonutSmoothDrawer`), and slider track (`KokonutSlideToConfirm`) in favor of Kanri's pure neutral dark charcoal palette (`#161616` tile background, `#282828` border/pressed, `#383838` drag handle, `#A6A6A6` secondary text, and `#F2F2F5` primary text), eliminating blueish tint.
  - Kept the verified, high-contrast light mode styling (`Zinc100`/`Zinc200`/`Zinc900`) and the 10% dark glass tint in Glass mode intact.
  - Bumped version to `1.5.1` (Build 22).

## [1.5.0] - 2026-11-07

### Added
- **Redesigned Instant Payment Detection Popup Card**:
  - Re-architected `InstantPopupCard` and `InstantPopupService` into a unified, high-polish floating card overlay triggered automatically on detected SMS and UPI payments.
  - **Frosted Glass Optics (Glass Mode)**: Enabled Android WindowManager compositor blur (`FLAG_BLUR_BEHIND` with `blurBehindRadius = 45.dp`) on Android 12+ (API 31+). Layered with a 35% dark glass tint, vertical white sheen gradient, 0.8 dp specular gradient border, and 1 dp top specular highlight rim so the underlying blurred screen shines through with optical depth.
  - **Theme-Adaptive Surfaces**: Unified card layout across all themes with semantic lighting—pure white elevated card (`#FFFFFF`) for Light mode, high-contrast dark charcoal (`#161618`) for Dark mode, and deep black (`#000000`) for AMOLED mode.
  - **Streamlined Layout & Information Hierarchy**:
    - Top centered dim grey timestamp (`GoogleSansFlex`, Medium, `#8E8E93`) with aligned close icon button.
    - Large 40 sp bold currency typography in Vibrant Red (`#E54D2E`) for expense and Sage Green (`#30A46C`) for income.
    - Clean merchant name and bank/wallet brand tag.
    - Unboxed, left-aligned "Search categories" text hyperlink button to quickly expand the search input box.
    - Top 5 category pill capsules (`RoundedCornerShape(50)`) with icons and checkmark feedback.
    - Capsule-shaped notes input prompt (`RoundedCornerShape(50)`).
    - Single capsule "Done" primary CTA (`RoundedCornerShape(50)`).
    - "Split expense ›" action replacing "Open in Kanri", launching the split bill flow with pre-filled amount and merchant.
  - **Coordinated Spring Pop-up & Pop-down Motion**:
    - Replicated the signature entrance/exit spring curves from `AddExpenseIncomePopup.kt` (`fadeIn` + `scaleIn` + `slideInVertically` with `KanriMotion.SnappySpring` and `SmoothSlideSpring`).
    - Smooth exit transition (`fadeOut` + `scaleOut` + `slideOutVertically`) before service termination across all dismissal actions (tap outside, close '✕', Done button, Split expense).
  - **In-App Popup Preview**: Added "Preview Payment Detection Popup" to Settings under Floating Quick Category to allow instant testing and visual verification.
- Bumped application version to `1.5.0` (Build 21).

## [1.4.9] - 2026-11-07

### Fixed
- **Seamless App Launch & Fluid Splash Screen Handover**:
  - Integrated official AndroidX Jetpack Splash Screen API (`androidx.core:core-splashscreen:1.0.1`).
  - Added dedicated starting themes (`Theme.Kanri.Starting` and `Theme.Kanri.Starting.Light`) inheriting from `Theme.SplashScreen` with seamless `postSplashScreenTheme` transitions.
  - Eliminated the split-second circular CRT-style mask and center circular disc by removing the redundant `android:windowSplashScreenIconBackgroundColor` attribute.
  - Integrated `setKeepOnScreenCondition` to hold the launch splash precisely until the root Compose composition has initialized, completely eliminating blank/unrendered frame flashes.
  - Choreographed an ultra-smooth exit motion: the centered branding wordmark floats gently upward (-16dp) and scales slightly (1.10x) as it dissolves over 320ms, while the splash window cross-fades over 380ms using an Apple/Emil-inspired cubic-bezier ease-out curve (`cubic-bezier(0.23, 1, 0.32, 1)`).
  - Coordinated an in-sync entrance reveal for Compose app content (`0.98 -> 1.0` scale and `0.85 -> 1.0` alpha over 380ms) for a continuous, cinematic handover from splash into the app.
  - Standardized window backgrounds across dark and light post-splash themes.
- Bumped application version to `1.4.9` (Build 20).

## [1.4.8] - 2026-11-07

### Changed & Refined
- **Variable Glass Blur & Tint Alignment**:
  - Restored `LendingSplitGroupCard` (Split Group Expense card) and `LendingSegmentedControl` ([People | Timeline] toggle bar) to use `GlassLevel.Blur` with `LocalHazeState.current`, aligning with all other variable glass cards in the app.
  - Restored "Add Record" action pill button and "Add First Entry" button in `LendingScreen.kt` to use `GlassLevel.Blur` with `LocalHazeState.current`.
  - Updated `InsightsRangeBar` (Time Period Selector: Month, 30D, 3M, Year, Custom) from `GlassLevel.Flat` to `GlassLevel.Blur` with `LocalHazeState.current`, allowing the live backdrop blur and dynamic theme tint to shine through.
- **Fixed 10% Dark Tint Consistency across Numpad and Summary Popup**:
  - Unified `TactileKeypadKey` digits and `TactilePresetChip` in `KokonutAmountPad` to strictly adhere to the fixed 10% dark tint (`Color.Black.copy(alpha = 0.10f)`) and subtle border (`Color.White.copy(alpha = 0.10f)`), seamlessly matching the numpad sheet container.
  - Standardized the Continue CTA in `KokonutAmountPad` to use the 10% dark tint when empty.
  - Standardized the metadata detail cards container and dividers in `KokonutSmoothDrawer` (entry summary popup) to use `Color.Black.copy(alpha = 0.10f)` and `Color.White.copy(alpha = 0.10f)` borders.
  - Standardized the slider track background in `KokonutSlideToConfirm` to follow `Color.Black.copy(alpha = 0.10f)` with matching border.
- Bumped application version to `1.4.8` (Build 19).

## [1.4.7] - 2026-11-06

### Added & Refined
- **Slider Saving Loader & Emerald Green Success Message**:
  - Decoupled asynchronous database persistence from modal dialog dismissal in `MainNavigationScreen.kt` and `LendingScreen.kt`.
  - The slide-to-confirm slider (`KokonutSlideToConfirm`) now executes its full fluid lifecycle upon completion:
    1. Ink floods across the slider track with a centered spinner and "Saving…".
    2. Slider smoothly transitions to vibrant Emerald Green with checkmark icon and confirmation label ("Loan saved!" / "Borrowing saved!" / "Expense saved!").
    3. Tactile haptic pulse triggers, holding the green success confirmation for 850ms before smoothly dismissing the sheet.
- **Glassmorphic Fixed 50dp Blur & 10% Dark Tint on Hub Controls**:
  - Updated `LendingSplitGroupCard` (Split a Group Expense card) to use `GlassLevel.EntrySheet` with `LocalHazeState.current`.
  - Updated `LendingSegmentedControl` ([People | Timeline] toggle bar) to use `GlassLevel.EntrySheet` with `LocalHazeState.current`.
  - Updated the "Add Record" pill action button and "Add First Entry" button in `LendingScreen.kt` to adopt `GlassLevel.EntrySheet` with `LocalHazeState.current`, removing legacy opaque borders and backgrounds.
- Bumped application version to `1.4.7` (Build 18).

## [1.4.6] - 2026-11-06

### Fixed
- **Lend & Borrow and Split Bill Popup Elevation Above Floating Dock**:
  - Fixed Z-order glitch where `LendingEntryDialog` and `SplitExpenseEntryDialog` could be rendered behind `KanriFloatingDock`.
  - Elevated all modal entry dialogs (`LendingEntryDialog`, `SplitExpenseEntryDialog`, `AddTransactionDialog`, and `LendingTransactionBridgeDialog`) to the root Box in `MainNavigationScreen.kt`, above all tabs and docks.
  - Added delegated callbacks `onOpenAddEntry` and `onOpenSplitExpense` in `LendingScreen`, and `onOpenLendBorrowTransaction` in `HomeScreen`, linking all trigger buttons directly to the root-level modal host.
  - Wired `KanriFloatingDock` visibility to automatically animate down off-screen (`slideOutVertically + fadeOut`) when any entry sheet is open, and smoothly slide back up (`slideInVertically + fadeIn`) upon dismissal or completion.
  - Converted `SplitExpenseEntryDialog` to pure in-compose sheet architecture (eliminating `Dialog` window wrapping) for unified backdrop `HazeState` live blur sampling.
  - Bottom-anchored all `AnimatedContent` step transitions (`contentAlignment = Alignment.BottomCenter`) across Amount Pad, Fill Details, and Summary Drawer for seamless vertical alignment.
- Bumped application version to `1.4.6` (Build 17).

## [1.4.5] - 2026-11-05

### Changed
- **Dock Floating '+' Menu Cleaned Up**:
  - Removed "Lend & Borrow" and "Split Expense" from the dock floating '+' action popup (`AddExpenseIncomePopup.kt`).
  - The menu now cleanly surfaces the 2 core spending actions: "Expense" and "Income".
- **Lend & Borrow Half-Screen Popup Refinement (`LendingEntryDialog.kt`)**:
  - Removed fullscreen Android Dialog window wrapper and forced `fillMaxHeight(0.92f)`.
  - Transformed `LendingEntryDialog` into a bottom-anchored, half-screen popup sheet with uniformly arranged info fields:
    - Top drag handle and header title/subtitle.
    - Sleek Amount Summary Card with instant Edit affordance.
    - "I Lent" vs "I Borrowed" segmented toggle.
    - Person Name field with Profile and Contacts pickers and quick-select chips.
    - Uniform Date & Time row matching `AddTransactionDialog`.
    - Expected Return Date chips (presets and custom calendar picker).
    - Wallet Impact chips ("Cash", "Online", "Not from balances").
    - Notes input.
    - Action Buttons: "Back" text button on left, "Cancel" and bold "Proceed" primary button on right.
  - Proceed button triggers a fluid spring slide-up to the small half-screen summary drawer (`KokonutSmoothDrawer`) with rolling total and `KokonutSlideToConfirm` slider.
- **Fixed 50dp Blur & 10% Dark Tint (`GlassLevel.EntrySheet`)**:
  - Added `GlassLevel.EntrySheet` to `KanriGlass.kt` providing a calibrated fixed 50 dp blur radius and 10% dark tint (`Color.Black.copy(alpha = 0.10f)`).
  - Applied `GlassLevel.EntrySheet` across:
    - Amount pads (`KokonutAmountPad`).
    - Transaction details cards (`FILL_DETAILS` in `AddTransactionDialog`, `LendingEntryDialog`, and `SplitExpenseEntryDialog`).
    - Summary drawers with slider (`KokonutSmoothDrawer`).
- Bumped application version to `1.4.5` (Build 16).

## [1.4.4] - 2026-11-04

### Added
- **Restored Unified 4-Option Action Dock Popup**:
  - Expanded `AddExpenseIncomePopup.kt` to present all 4 core transaction entry points:
    - **Expense**: "Log spent money" (Vibrant Red with `ExpenseFlowIcon`)
    - **Income**: "Log received money" (Sage Green with `IncomeFlowIcon`)
    - **Lend & Borrow**: "Lent or borrowed money" (Indigo/Violet with custom bidirectional `LendBorrowFlowIcon`)
    - **Split Expense**: "Split a bill with friends" (Amber/Orange with custom diverging `SplitBillFlowIcon`)
  - Added custom procedural SVG composable `SplitBillFlowIcon` matching the geometric aesthetic of Kanri flow icons.
- **Unified Kokonut Pop-up Workflow for Split Expense (`SplitExpenseEntryDialog`)**:
  - Introduced `SplitExpenseEntryDialog.kt` adopting the exact 3-step modern pop-up paradigm:
    1. **Amount Pad (`KokonutAmountPad`)**: Fluid zero-start amount keypad with rolling numbers and currency formatting.
    2. **Fill Details**: Sleek bottom-anchored frosted glass modal card (`KanriGlassCard` with 26 dp rounded corners) featuring an edit amount affordance chip, "I Paid (I Lent)" vs "Friend Paid (I Owe)" role toggle, event description, interactive people counter stepper (2–20 people) with dynamic per-person share calculations, and smart contact suggestion chips.
    3. **Summary Drawer (`KokonutSmoothDrawer`)**: Review drawer displaying event details, people breakdown, per-person share, and interactive haptic `KokonutSlideToConfirm` with confirmation types (`SlideConfirmationType.LEND` and `SlideConfirmationType.BORROW`).
  - Updated `SplitExpenseSheet.kt` to delegate directly to `SplitExpenseEntryDialog` for backwards-compatibility.
  - Extracted centralized `enum class SplitRole` into `SplitRole.kt`.

### Changed
- **Lend & Borrow Entry Dialog Refinement (`LendingEntryDialog.kt`)**:
  - Refactored `LendingEntryStep.FILL_DETAILS` from a full-page Scaffold into a sleek, bottom-docked frosted glass card (`KanriGlassCard` with top drag handle and 26 dp rounded top corners), matching the exact pop-up workflow of `AddTransactionDialog`.
  - Wired both `onAddLendBorrow` and `onSplitExpense` in `MainNavigationScreen.kt` to trigger the unified entry dialogs seamlessly.
  - Updated `LendingScreen.kt` to invoke `SplitExpenseEntryDialog` when clicking "Split a Group Expense".
- Bumped application version to `1.4.4` (Build 15).

## [1.4.3] - 2026-11-04

### Changed
- **Greeting Subtitle High-Contrast Typography**:
  - Updated greeting dynamic subtitle color in `GreetingHeader.kt` to luminous light grey (`#E8E8ED`) in dark and glass themes with medium font weight (`FontWeight.Medium`) for optimal readability against animated WebGL backdrops and frosted glass surfaces.
- **Developer Signature Intro**:
  - Changed developer signature introduction text from `"This app was built by"` to `"Made by"` in `HomeScreen.kt` and `SignatureText.kt`.
- Bumped application version to `1.4.3` (Build 14).

## [1.4.2] - 2026-11-04

### Fixed
- **Floating Navigation Dock Tint and Blur Responsiveness**:
  - Connected the navigation dock capsule and floating Add (+) button directly to `tokens.tintLightness` in `KanriFloatingDock.kt` and `KanriGlass.kt`.
  - The dock now smoothly reflects the customizer's glass tint: dark smoky obsidian when sliding left, neutral crystal clear glass at center (0.0f), and bright crystalline frost when sliding right.
  - Dock borders dynamically follow `tokens.borderStart` and `tokens.borderEnd` specular edge reflectance.
  - Tab icon and label colors smoothly transition to dark charcoal `Color(0xFF121318)` at high frost lightness levels for crisp readability and contrast.
  - Removed restrictive 24 dp minimum clamp on dock blur, allowing seamless scaling down to 0 dp (crystal-clear dock glass).
- **Streamlined Glass Customization Screen**:
  - Removed the bulky live preview card, preview stat chips, and mock backdrop pattern from `GlassCustomizerScreen.kt`.
  - Customizer screen now features a clean, minimal interface that directly previews changes on the live app backdrop and running UI cards.
- **Restored Standard National Currency Flags**:
  - Re-enabled standard national flag indicators in `AppCurrency.kt`, `SettingsScreen.kt`, and `SettingsComponents.kt` (`SelectCurrencyDialog`) so regional currencies display their customary flags.
- Bumped application version to `1.4.2` (Build 13).

## [1.4.1] - 2026-11-04

### Fixed
- **Glass Components Blur and Opacity across All Menus**:
  - Eliminated Haze recursion cycle caused by `AnimatedContent` applying `hazeSource` on top of child cards applying `hazeEffect(effectiveHazeState)` in `MainNavigationScreen.kt`. Foreground navigation content now floats cleanly above the `GlassBackdrop` source.
  - Restored base opacity for `GlassLevel.Blur`: default neutral tint (`0.0f`) now provides lush `frostBgAlpha = 0.32f` and `frostTint = Color.White.copy(alpha = 0.10f)` so glass cards remain visible, translucent, and frosted across Home, Insights, Lend & Borrow, and Goals screens.
  - Smoothly scales `frostBgAlpha` up to `0.88f` when sliding left to dark smoky obsidian tint (`Color(0xFF06070A)`) or right to luminous crystalline white frost (`Color(0xFFFAF9F6)`).
  - Restored proper opacities and tint interpolation for `GlassLevel.Dock` and `GlassLevel.Strong`.
- **Complete Emoji Removal across Entire Application**:
  - Removed all emoji characters throughout the entire application, adopting a sleek, typographic, and vector-driven interface:
    - `KokonutAvatarSelector`: Replaced emoji avatars with sleek, uppercase letter monograms over gradient circles.
    - `SettingsScreen` & `SettingsComponents`: Replaced emoji flags in currency subtitle and currency selection picker with styled currency symbol chips (`Text(currency.symbol)`). Removed flag emoji properties from `AppCurrency.kt`.
    - `MonthViewModel` & `CategoryDetailModels`: Removed category emojis.
    - `GreetingResolver` & `BirthdayHelper`: Removed cake and sparkle emojis from birthday greetings and celebration notifications.
    - `GoalDialogs`, `GoalsScreen`, `SavingsGoalsSheet`, `SavingsGoalEntity`, `GoalIconHelper`: Removed celebration and target emojis.
    - `AddLendingDialog`, `LendingUpiPayDialog`, `LendingReminderWorker`: Removed handshake, inbox, and warning emojis.
    - `InstantPopupNotificationHelper`: Removed payment banner emojis.
    - `DiagnosticsSheet`: Removed shield emoji from system status.
    - `RecurringPaymentsSheet`: Replaced repeat emoji with `Icons.Default.Repeat`.
    - `WalletSetupDialog`: Removed cash and bank emojis from wallet type options.
- Bumped application version to `1.4.1` (Build 12).

## [1.4.0] - 2026-11-04

### Added
- **Photorealistic 24-Hour Day Cycle for Still Sunset Meadow**:
  - Implemented a 15-keyframe 24-hour celestial environment cycle synchronized with the user's local device time zone.
  - **Sunrise**: Golden rose dawn palette (`#B47290`, `#FFA085`, `#FFE2BE`) with warm atmospheric light shafts and dawn-tinted clouds.
  - **Morning Azure**: Crisp, fresh morning blue sky (`#3B8AE8`, `#A8D4F8`, `#FFF8E7`).
  - **Daytime Blue**: True-to-life vibrant daylight blue with realistic Rayleigh scattering fading to a bright horizon (`#2563EB`, `#93C5FD`, `#FFFFFF`).
  - **Sunset Peak**: Fiery golden amber and twilight magenta sunset during dusk hours (`#D87093`, `#F97316`, `#FFC078`).
  - **Starry Cosmic Night**: Deep night sky (`#040716`, `#0E1738`, `#9DB0F0`) with twinkling stars and blocky voxel moon.
  - Smooth cosine interpolation (`easeT`) across all 15 keyframes for seamless transitions between hours.
- **Center-Neutral Glass Tint Slider**:
  - Centered by default at `0.0f` ("No tint"), providing pure crystal-clear glass without artificial dark or light cast.
  - Sliding left applies a **dark smoky obsidian tint** (e.g. "Dark 50%").
  - Sliding right applies a **luminous crystalline white frost tint** (e.g. "Light 50%").
  - Added 3 quick-snap buttons below the slider (`Dark (-50%)`, `No tint (0%)`, `Light (+50%)`) for instant 1-tap resets to center or preset intensities.
- **Interactive High-Contrast Live Glass Preview Lens**:
  - Added a dedicated, high-contrast preview canvas with glowing violet, rose-pink, emerald, and amber gradient orbs and cross lines directly beneath `LiveGlassPreviewCard` in a local `HazeState`.
  - When sliding Frosted Blur from 0 dp to 50 dp, users experience a dramatic, unmistakable transition from razor-sharp clarity (0 dp clear glass) to silky, soft blur (24 dp) to heavy frosted diffusion (50 dp).
  - Added Compose-rendered ambient radial mesh orbs to `GlassBackdrop` and connected screen content in `MainNavigationScreen` to `hazeSource` so `KanriFloatingDock` and glass surfaces blur live content as it scrolls underneath.
- Bumped application version to `1.4.0` (Build 11).

## [1.3.1] - 2026-11-04

### Fixed
- **Glass Blur Slider Responsiveness**:
  - Removed restrictive hardcoded blur clamps (`coerceAtLeast(20.dp)` on Frosted Blur and `coerceAtLeast(36.dp)` on Strong Blur) in `KanriGlass.kt`, allowing the blur slider to smoothly scale blur depth across the full 0–50 dp range down to 0 dp (crystal clear glass).
  - Scaled backdrop frost background and tint alphas dynamically with `surfaceOpacity` so low tint percentages produce crisp, transparent surfaces instead of being artificially clamped.
  - Expanded `borderOpacity` range in `KanriGlassTokens.kt` from `0.12f..0.35f` to `0.0f..0.85f`, allowing border highlights to be completely turned off or made intensely specular.
  - Connected `rememberKanriGlassTheme` reactively to `LocalKanriGlass.current`, ensuring dialogs, popups, and dropdown menus dynamically mirror the user's custom blur and glass preferences.
  - Upgraded `GlassCustomizerScreen.kt` control cards from `GlassLevel.Flat` to `GlassLevel.Blur`, and added an interactive **Live Glass Preview Card** at the top that responds in real time to slider adjustments.
- Bumped application version to `1.3.1` (Build 10).

## [1.3.0] - 2026-11-04

### Added
- **Thistle Portico Live Glass Background**: High-fidelity procedural Canvas 2D scene featuring a stone Gothic portico, wisteria and hydrangea foliage, drifting and landing blossom petals, racemes swaying in the wind, a pale distant castle, and animated sun shafts and dust motes.
- **Still Sunset Meadow Live Glass Background**: Full-featured Three.js 3D voxel landscape with interactive grass parting on touch, blooming block flowers, cube clouds, and a real-time day cycle that automatically synchronizes with the user's local time zone (daylight, golden sunset, and starry moonlit night).
- **10-Shader Glass Customization Grid**: Expanded the Theme Settings glass background picker from 8 to 10 choices in a balanced 2-column grid with dedicated gradient swatches and descriptions.
- Bumped application version to `1.3.0` (Build 9).

### Fixed
- **Thistle Portico Initialization Glitch**: Resolved variable shadowing in the render loop frame dispatcher and added immediate WebView canvas layout sizing on launch.

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
