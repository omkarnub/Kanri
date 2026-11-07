# Changelog

All notable changes to Kanri will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.1] - 2026-11-07

### Fixed
- **Opaque Backgrounds & Contrast for Entry Workflows in Dark and Light Modes**:
  - Fixed `KanriGlassCard` when rendering `GlassLevel.EntrySheet` in solid themes (Light, Dark, AMOLED, and System), replacing the missing-haze transparent fallback with clean, 100% opaque surface backgrounds (`surfaceContainerHigh` in Dark/AMOLED mode and pure `#FFFFFF` in Light mode).
  - Restored opaque tactile styling in `KokonutAmountPad` (`TactileKeypadKey` and `TactilePresetChip`) using `Zinc900`/`Zinc800` in Dark mode and `Zinc100`/`Zinc200` in Light mode, while preserving the 10% dark glass tint in Glass mode.
  - Standardized the Continue CTA button in `KokonutAmountPad` to render opaque `Zinc900`/`Zinc100` backgrounds in solid themes when empty, avoiding washed-out transparency.
  - Updated the metadata detail container in `KokonutSmoothDrawer` to use opaque `Zinc900`/`Zinc100` surfaces and solid dividers in non-glass modes.
  - Standardized the slide track container in `KokonutSlideToConfirm` to use opaque `Zinc900`/`Zinc100` with subtle border contrast in non-glass modes.
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
