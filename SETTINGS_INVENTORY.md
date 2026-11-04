# Kanri Settings & Preferences Inventory

Generated as part of the Kanri Settings Experience Redesign. Single source of truth compiled from codebase analysis.

---

## 1. Complete Inventory of User-Configurable Settings & Actions

| # | Name | Storage Key | Type | Default Value | Storage Location | Consumed In | Current UI Location | New UI Planned Location | Status / Notes |
|---|------|-------------|------|---------------|------------------|-------------|---------------------|-------------------------|----------------|
| 1 | **Theme Mode** | `key_theme_mode` | Choice (5 options: `SYSTEM`, `LIGHT`, `DARK`, `AMOLED`, `GLASS`) | `ThemeMode.DARK` | SharedPreferences (`kanri_theme_prefs`) | `Theme.kt` (`KanriTheme`), `AppIconManager.kt`, `MainActivity.kt` | `ThemesCustomizationScreen.kt` (horizontal cards), dead `ThemePreviewCard` in `SettingsScreen.kt` | Appearance > Theme Selector | Active |
| 2 | **Dynamic System Color** | `key_dynamic_color` | Toggle (Boolean) | `false` | SharedPreferences (`kanri_theme_prefs`) | `Theme.kt` (`KanriTheme`) on Android 12+ (API 31+) | `ThemesCustomizationScreen.kt` | Appearance > Dynamic Color | Active |
| 3 | **Reduce Transparency** | `key_reduce_transparency` | Toggle (Boolean) | `false` | SharedPreferences (`kanri_theme_prefs`) | `GlassCapabilities.kt` (`isBlurSupported`, `isMotionAllowed`) | *Hidden / Never exposed in UI* | Appearance > Glass Customization (Glass only) / Accessibility | Active (Surfacing hidden setting) |
| 4 | **Launcher App Icon** | `key_app_icon_style` | Choice (2 options: `DARK`, `LIGHT`) | `AppIconStyle.DARK` | SharedPreferences (`kanri_theme_prefs`) | `AppIconManager.kt` (enables/disables activity aliases in PackageManager) | `ThemesCustomizationScreen.kt` | Appearance > App Icon | Active |
| 5 | **Glass Animated Background** | `key_glass_background` | Choice (8 options: `AURORA`, `GRAIN_GRADIENT`, `HERO_GEOMETRIC`, `PRISM_GRADIENT`, `CLOUD_SHADER`, `INK_RIDGES`, `FUJI_SAKURA`, `SUMI_E_MIST`) | `GlassBackgroundType.AURORA` | SharedPreferences (`kanri_theme_prefs`) | `GlassBackdrop.kt`, `GlassShaderBackground.kt` (loads WebGL asset in WebView) | `ThemesCustomizationScreen.kt` (always visible, even outside Glass) | Appearance > Live Backgrounds (Glass only) | Active (Only visible in Glass) |
| 6 | **Glass Quick Preset** | Multiple keys | Preset Choice (5 presets: `DEFAULT`, `ULTRA_SHEER`, `DEEP_OBSIDIAN`, `LUMINOUS_FROST`, `NEON_CRYSTAL`) | `GlassPresets.DEFAULT` | SharedPreferences (`kanri_theme_prefs`) | `Theme.kt` (`createCustomGlassTokens`) | `GlassCustomizerScreen.kt` | Appearance > Glass Engine > Presets (Glass only) | Active |
| 7 | **Glass Frosted Blur Strength** | `key_glass_blur` | Slider (0.0f .. 50.0f dp) | `24.0f` dp | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `KanriGlass.kt` (`hazeEffect`) | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 8 | **Glass Surface Opacity** | `key_glass_opacity` | Slider (0.02f .. 0.40f) | `0.08f` (8%) | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `createCustomGlassTokens` | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 9 | **Glass Tint & Tone Lightness** | `key_glass_tint_lightness` | Slider (-1.0f .. 1.0f) + 3 Quick Buttons | `0.0f` (Neutral) | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `createCustomGlassTokens` | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 10 | **Glass Expense (Red) Tint Color** | `key_glass_expense_color` | Color Choice (5 swatches: Crimson, Rose, Flame, Coral, Ruby) | `0xFFE54D2EL` | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `KanriGlassButton.kt` | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 11 | **Glass Expense Tint Strength** | `key_glass_expense_tint_opacity` | Slider (0.04f .. 0.45f) + 4 Quick Buttons | `0.18f` (18%) | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `createCustomGlassTokens` | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 12 | **Glass Income (Green) Tint Color** | `key_glass_income_color` | Color Choice (5 swatches: Emerald, Jade, Lime, Spring, Teal) | `0xFF30A46CL` | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `KanriGlassButton.kt` | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 13 | **Glass Income Tint Strength** | `key_glass_income_tint_opacity` | Slider (0.04f .. 0.45f) + 4 Quick Buttons | `0.18f` (18%) | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `createCustomGlassTokens` | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 14 | **Glass Border Shine Opacity** | `key_glass_border_opacity` | Slider (0.0f .. 0.70f) | `0.28f` (28%) | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `createCustomGlassTokens` | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 15 | **Glass Border Width** | `key_glass_border_width` | Float (0.8f .. 2.0f dp) | `1.0f` dp | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `createCustomGlassTokens` | Auto-adjusted with border opacity in `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 16 | **Glass Corner Roundedness** | `key_glass_corner_radius` | Slider (10.0f .. 32.0f dp) | `20.0f` dp | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, cards, dock, dialogs | `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 17 | **Glass Button Opacity** | `key_glass_button_opacity` | Float (0.05f .. 0.40f) | `0.16f` | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt`, `KanriGlass.kt` (Floating Dock & buttons) | Loaded from preset in `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 18 | **Glass Shadow Intensity** | `key_glass_shadow` | Float (0.0f .. 0.80f) | `0.25f` | SharedPreferences (`kanri_theme_prefs`) | `KanriGlassTokens.kt` | Loaded from preset in `GlassCustomizerScreen.kt` | Appearance > Glass Engine (Glass only) | Active |
| 19 | **Primary App Currency** | `key_app_currency` | Searchable Choice (Enum `AppCurrency`, 40+ currencies with symbols & flags) | `AppCurrency.INR` (`INR`, `₹`) | SharedPreferences (`kanri_currency_prefs`) | `CurrencyUtils.kt`, throughout the whole app | `SettingsScreen.kt` > `SelectCurrencyDialog` | Wallets & Currency > Primary Currency | Active |
| 20 | **Cash Opening Balance Correction** | Room DB `wallet_balances` (account `CASH`) | Decimal Amount Input | `0.0` | Room Database (`WalletEntity`) | `WalletRepository.kt`, `HomeViewModel.kt`, `FloatingDock` | `SettingsScreen.kt` > `CorrectBalanceDialog` | Wallets & Currency > Cash Balance | Active |
| 21 | **Online Opening Balance Correction** | Room DB `wallet_balances` (account `ONLINE`) | Decimal Amount Input | `0.0` | Room Database (`WalletEntity`) | `WalletRepository.kt`, `HomeViewModel.kt`, `FloatingDock` | `SettingsScreen.kt` > `CorrectBalanceDialog` | Wallets & Currency > Online Balance | Active |
| 22 | **Wallet Full Reconfiguration** | Room DB `wallet_balances` + `WalletPreferences` | Multi-step Dialog (Cash, Online, ATM Mode) | Unconfigured / `0.0` | Room DB + `WalletPreferences` | `WalletRepository.kt` | `SettingsScreen.kt` > `WalletSetupDialog` | Wallets & Currency > Reconfigure Wallets | Active |
| 23 | **Hide Balances (Privacy Mask)** | `key_balance_spoiler_enabled` | Toggle (Boolean) | `false` | SharedPreferences (`kanri_wallet_prefs`) | `FloatingDock.kt`, `WalletCards.kt` (blurs/masks balance numbers) | `SettingsScreen.kt` | Security & Privacy > Privacy Mask | Active |
| 24 | **ATM Withdrawal Accounting Mode** | `key_atm_withdrawal_mode` | Choice (2 options: `SPENDING`, `TRANSFER`) | `AtmWithdrawalMode.SPENDING` | SharedPreferences (`kanri_wallet_prefs`) | `SmsTransactionParser.kt`, `WalletRepository.kt` | `SettingsScreen.kt` | Wallets & Currency > ATM Flow | Active |
| 25 | **Device Screen / Biometric Lock** | `key_lock_enabled` | Toggle (Boolean) | `false` | SharedPreferences (`kanri_security_prefs`) | `SecurityManager.kt`, `MainNavigationScreen.kt` | `SettingsScreen.kt`, `SecuritySettingsSheet.kt` | Security & Privacy > App Lock | Active |
| 26 | **Test Lock Screen** | Action / Trigger | Action Button | N/A | In-memory trigger | `SecurityManager.lockImmediately()` | `SettingsScreen.kt`, `SecuritySettingsSheet.kt` | Security & Privacy > Test Lock Screen | Active |
| 27 | **Privacy Policy** | Action / Modal | Dialog viewer | N/A | Static / Read-only | `PrivacyPolicyDialog.kt` | `SettingsScreen.kt` | Security & Privacy > Privacy Policy | Active |
| 28 | **Smart Merchant Rules** | Room DB table `counterparty_category_map` | Sub-screen CRUD (keyword, target category, past apply) | Initial presets | Room Database (`CounterpartyCategoryMapEntity`) | `CategoryDao.kt`, `SmsTransactionParser.kt` | `SettingsScreen.kt` > `SmartMerchantRulesScreen.kt` | Rules & Categories > Smart Merchant Rules | Active |
| 29 | **Custom Categories CRUD** | Room DB table `categories` (`isCustom = true`) | Dialog CRUD (Name, Icon picker from 30+ icons) | Built-in default categories | Room Database (`CategoryEntity`) | `CategoryDao.kt`, Transaction categorization | `SettingsScreen.kt` (Add / Edit / Delete dialogs) | Rules & Categories > Custom Categories | Active |
| 30 | **Bank SMS Detection Permission** | System Runtime Permission (`RECEIVE_SMS`, `READ_SMS`) | Action Button / Status Badge | Denied until granted | Android OS Permission Manager | `SmsReceiver.kt`, `SmsPermissionHelper.kt` | `SettingsScreen.kt` > `PermissionsStatusOverview` | Permissions & System Detection | Active |
| 31 | **Payment Notification Listener Permission** | System Setting (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Action Button / Status Badge | Denied until granted | Android OS Notification Settings | `PaymentNotificationListenerService.kt` | `SettingsScreen.kt` > `PermissionsStatusOverview` | Permissions & System Detection | Active |
| 32 | **Transaction Notifications Permission** | System Runtime Permission (`POST_NOTIFICATIONS`) | Status Badge | Granted on install / API < 33 | Android OS Permission Manager | `NotificationManagerCompat` | `SettingsScreen.kt` > `PermissionsStatusOverview` | Permissions & System Detection | Active |
| 33 | **Floating Quick Category Overlay** | System Setting (`SYSTEM_ALERT_WINDOW`) | Status Badge | Denied until granted | Android OS Settings (`canDrawOverlays`) | `InstantFloatingOverlayService.kt` | `SettingsScreen.kt` > `PermissionsStatusOverview` | Permissions & System Detection | Active |
| 34 | **Battery Optimization Exemption** | System Setting (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | Action Button / Status Badge | Optimized (default) | Android OS PowerManager (`isIgnoringBatteryOptimizations`) | `BatteryOptimizationHelper.kt` | `SettingsScreen.kt` > `PermissionsStatusOverview` | Permissions & System Detection | Active |
| 35 | **Google Drive Cloud Backup Mode** | `key_backup_mode` | Choice (`OFFLINE`, `CLOUD`) | `BackupMode.OFFLINE` | EncryptedSharedPreferences (`kanri_cloud_backup_prefs`) | `DriveBackupWorker.kt`, `CloudBackupRepository.kt` | `SettingsScreen.kt` (Toast only), `OnboardingScreen.kt` | Data & Backup > Google Drive Cloud Backup | Active (Fully wired) |
| 36 | **Cloud Auto-Backup Toggle** | `key_auto_backup_enabled` | Toggle (Boolean) | `true` | EncryptedSharedPreferences (`kanri_cloud_backup_prefs`) | `DriveBackupWorker.kt` | *Previously only in background worker* | Data & Backup > Cloud Auto-Backup | Active |
| 37 | **Cloud Account Sign-In / Disconnect** | `key_account_email`, `key_account_name` | Action Buttons / Status | `null` | EncryptedSharedPreferences (`kanri_cloud_backup_prefs`) | `GoogleAuthHelper.kt`, `CloudBackupManager.kt` | `SettingsScreen.kt` (Previously simulated toast) | Data & Backup > Google Drive Cloud Backup | Active |
| 38 | **Direct Offline Backup Export** | Action / File Save | File Export (AES-256 GCM encrypted JSON or plain JSON) | N/A | File Provider / External Storage | `BackupRepository.kt` | `ProfileScreen.kt` > `BackupRestoreSheet.kt` | Data & Backup > Export Offline Backup | Active |
| 39 | **Direct Offline Backup Restore** | Action / File Pick | File Import (Decryption passphrase + Merge / Overwrite) | N/A | Room Database | `BackupRepository.kt` | `ProfileScreen.kt` > `BackupRestoreSheet.kt` | Data & Backup > Restore Backup | Active |
| 40 | **Automated Monthly Statement** | `scheduled_export_frequency` | Choice (`OFF`, `MONTHLY`, `WEEKLY`) | `ExportSchedule.OFF` | SharedPreferences (`kanri_export_scheduler_prefs`) | `ExportScheduler.kt`, `ScheduledExportWorker.kt` | `ProfileScreen.kt` | Data & Backup > Automated Statement Export | Active |
| 41 | **Generate Statement (PDF / CSV)** | Action / Sheet | Modal Bottom Sheet (`ExportStatementSheet.kt`) | N/A | Statement generator | `ProfileScreen.kt` | `SettingsScreen.kt` (Toast only), `ProfileScreen.kt` | Data & Backup > Generate Statement | Active |
| 42 | **Replay Intro Walkthrough** | `has_completed_onboarding` | Action Button | `false` | SharedPreferences (`kanri_onboarding_prefs`) | `MainActivity.kt`, `OnboardingScreen.kt` | `SettingsScreen.kt` | Data & Backup / System > Replay Intro Walkthrough | Active |
| 43 | **App Version & Check Updates** | `key_last_check`, `key_pending_update_json` | Action Button / Info | v1.0.2 | SharedPreferences (`kanri_update_prefs`) | `AppUpdateManager.kt`, `AppUpdateDialog.kt` | `SettingsScreen.kt` | About & Support > Kanri Version & Updates | Active |
| 44 | **App Health & Diagnostic Logs** | Files in cache / filesDir (`fatal_crash_*.txt`, `error_log_*.txt`) | Viewer Dialog + Clear + Share | Empty | Internal storage | `CrashLogger.kt` | `SettingsScreen.kt` (`ConsumerCrashLogsDialog` & `CrashLogsDialog.kt`) | About & Support > App Health & Diagnostic Logs | Active |
| 45 | **Suggest a Feature** | Action / Dialog | Dialog Form (Category, Title, Details) -> Email Intent | N/A | Email Client Intent | `SettingsScreen.kt` > `SuggestFeatureDialog` | `SettingsScreen.kt` | About & Support > Suggest a Feature | Active |
| 46 | **Contact Support** | Action / Dialog | Dialog (Copy email / Open mailto Intent) | N/A | Clipboard & Email Intent | `SettingsScreen.kt` > `ContactUsDialog` | `SettingsScreen.kt` | About & Support > Contact Support | Active |
| 47 | **Reset Opening Balances** | Database wallet balances reset | Danger Action Dialog | N/A | Room DB (`WalletRepository.reset()`) | `WalletRepository.kt` | `SettingsScreen.kt` > Danger Zone | Danger Zone > Reset Balances | Active |
| 48 | **Terminate App Process** | Process kill | Danger Action Dialog | N/A | In-memory process | `Activity.finishAffinity()`, `Process.killProcess()` | `SettingsScreen.kt` > Danger Zone | Danger Zone > Terminate App | Active |
| 49 | **Factory Reset App** | Database wipe, keystore rotate, preferences clear | Danger Action Dialog (Type "DELETE") | N/A | Entire app storage | `FactoryResetManager.executeFactoryReset()` | `SettingsScreen.kt` > Danger Zone | Danger Zone > Factory Reset Entire App | Active |

---

## 2. Non-Settings & Internal State Store Audit (Proof of Exclusions)

1. **`StreakDataStore.kt`**:
   - `last_celebrated_milestone` (`intPreferencesKey("last_celebrated_milestone")`): Internal gamification milestone tracker (3, 7, 14, 30 days) to prevent duplicate celebrations. Not a user-configurable setting.
2. **`GreetingDataStore.kt`**:
   - `last_subtitle_rule_id`, `last_shown_epoch_day`, `last_warning_bucket`, `last_warning_epoch_day`: Anti-repetition algorithm state for home screen dynamic greetings. Not a user-configurable setting.
3. **`BatteryOptimizationHelper.kt`**:
   - `has_prompted_battery_opt_once`: Flag preventing annoying repeated prompts on app launch. The actual user permission is surfaced cleanly via `isIgnoringBatteryOptimizations` in Permissions.
4. **`ThemePreviewCard` in `SettingsScreen.kt` (lines 1447–1662)**:
   - Dead composable code in old `SettingsScreen.kt`. It was defined at the bottom of the file but never referenced in the composable tree because `SettingsScreen` just launched `ThemesCustomizationScreen` via a button row. It is now replaced by the proper interactive preview cards in the new design.

---

## 3. Architecture of Current Theme System & Audit of Flaws

### How Theme Modes are Stored, Resolved, and Applied
1. **Storage**: Mode is persisted in `kanri_theme_prefs` under `key_theme_mode` (`SYSTEM`, `LIGHT`, `DARK`, `AMOLED`, `GLASS`).
2. **Resolution (`Theme.kt`)**:
   - `effectiveThemeMode` resolves `SYSTEM` against `isSystemInDarkTheme()`.
   - `isDark` is calculated: `true` for `DARK`, `AMOLED`, `GLASS`, and `SYSTEM` when system is dark.
   - `glassTokens`: Non-null ONLY when `resolvedThemeMode == ThemeMode.GLASS`.
   - `colorScheme`:
     - If `glassTokens != null` -> `dynamicGlassColorScheme` (translucent surfaces).
     - Else if `dynamicColor` enabled on Android 12+ -> `dynamicDarkColorScheme` / `dynamicLightColorScheme`.
     - Else if `AMOLED` -> `KanriAmoledColorScheme` (pitch black `#000000` background).
     - Else if `DARK` -> `KanriDarkColorScheme` (charcoal `#121212` background).
     - Else -> `KanriLightColorScheme` (soft cream `#FBF8EE` background).
3. **System Bars & Window Backdrop**:
   - Status bar and navigation bar icon light/dark mode are set via `WindowCompat.getInsetsController(window, view)`.
   - Native Window background drawable is tinted to prevent black/white flashes during keyboard or window transitions.

### Problems & Inconsistencies in the Current Theme UI
1. **Fragmented UI & Navigation**:
   - Settings had a "Themes & Live Backgrounds" row that pushed `ThemesCustomizationScreen`.
   - Settings also had a separate "Glass Surface Engine" row that opened `GlassCustomizerScreen`.
   - Inside `ThemesCustomizationScreen`, there was yet another link to `GlassCustomizerScreen`.
   - `ThemePreviewCard` was copy-pasted dead code inside `SettingsScreen.kt`.
2. **Violation of Theme Dependency Rules**:
   - All 8 live shader backgrounds were displayed and selectable when Light, Dark, or AMOLED was selected!
   - Selecting a shader while in Light/Dark/AMOLED silently changed the theme mode to Glass without warning.
   - Glass customization options were accessible when not in Glass mode.
3. **Lack of Visual Representation**:
   - The theme mode selector in `ThemesCustomizationScreen` was a horizontal strip of plain icon cards with no visual clue of how Light, Dark, AMOLED, or Glass actually look.
4. **Hardcoded Colors & Contrast Defects**:
   - `SettingsDivider` used hardcoded `Color.White.copy(alpha = 0.08f)` fallback, which was unreadable/dirty in Light theme.
   - `SettingsSwitchRow` used hardcoded white thumbs on switches regardless of theme.
   - Card container colors had hardcoded alphas (`surfaceVariant.copy(alpha = 0.35f)`) causing contrast problems against AMOLED black.
   - Sub-dialogs like `SelectCurrencyDialog` had hardcoded text color fallbacks.
5. **Orphaned Capabilities**:
   - `isReduceTransparency` was wired into `GlassCapabilities.isBlurSupported` but had no toggle in the UI.

---

## 4. Verification Checklist & Theme Matrix



### 4.1 Final Setting Verification Table (All 49 Inventory Items)

| # | Setting / Action | New Location in Settings | Runtime Effect & Persistence | Status |
|---|------------------|--------------------------|------------------------------|--------|
| 1 | Theme Mode | Appearance > Theme Selector (5 visual preview cards) | Persists in `kanri_theme_prefs`; switches theme instantly without restart | **VERIFIED** |
| 2 | Dynamic System Color | Appearance > Dynamic System Colors switch | Persists in `kanri_theme_prefs`; applies Android 12+ wallpaper palette | **VERIFIED** |
| 3 | Reduce Transparency | Appearance > Glass Engine > Reduce Transparency switch | Persists in `kanri_theme_prefs`; disables blur and heavy shaders | **VERIFIED** |
| 4 | Launcher App Icon | Appearance > App Launcher Icon row | Enables/disables activity aliases in `PackageManager`; prompt to restart | **VERIFIED** |
| 5 | Glass Animated Background | Appearance > Live Backgrounds (8 shader preview cards) | Only composed when Glass theme active; updates active shader key | **VERIFIED** |
| 6 | Glass Quick Presets | Appearance > Glass Engine > 5 Quick Preset swatches | Updates blur, opacity, tint, borders, shadows simultaneously | **VERIFIED** |
| 7 | Glass Blur Strength | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Live slider 0-50dp; real-time haze blur update | **VERIFIED** |
| 8 | Glass Surface Opacity | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Live slider 2-40%; adjusts surface alpha | **VERIFIED** |
| 9 | Glass Tint Lightness | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Live slider -1.0 to +1.0; neutral/bright/dark quick buttons | **VERIFIED** |
| 10 | Glass Expense Color | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | 5 swatches (Crimson, Rose, Flame, Coral, Ruby) | **VERIFIED** |
| 11 | Glass Expense Tint Strength | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Live slider 4-45%; modifies expense badge & button background | **VERIFIED** |
| 12 | Glass Income Color | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | 5 swatches (Emerald, Jade, Lime, Spring, Teal) | **VERIFIED** |
| 13 | Glass Income Tint Strength | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Live slider 4-45%; modifies income badge & button background | **VERIFIED** |
| 14 | Glass Border Shine Opacity | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Live slider 0-70%; hairline border illumination | **VERIFIED** |
| 15 | Glass Border Width | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Float 0.8-2.0dp; linked with border opacity | **VERIFIED** |
| 16 | Glass Corner Roundedness | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Live slider 10-32dp; adjusts card, dock, and dialog corner radii | **VERIFIED** |
| 17 | Glass Button Opacity | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Presets & tuning; modifies floating dock & action button alpha | **VERIFIED** |
| 18 | Glass Shadow Intensity | Appearance > Glass Engine > Advanced Glass Tuning (`GlassCustomizerScreen`) | Presets & tuning; adjusts ambient elevation drop-shadow | **VERIFIED** |
| 19 | Primary App Currency | Wallets & Currency > Primary Currency (`SelectCurrencyDialog`) | Searchable dialog across 40+ currencies; updates `AppCurrency` app-wide | **VERIFIED** |
| 20 | Cash Balance Correction | Wallets & Currency > Cash Balance (`CorrectBalanceDialog`) | Direct decimal update in `WalletEntity`; recalculates totals | **VERIFIED** |
| 21 | Online Balance Correction | Wallets & Currency > Online Balance (`CorrectBalanceDialog`) | Direct decimal update in `WalletEntity`; recalculates totals | **VERIFIED** |
| 22 | Reconfigure Wallets | Wallets & Currency > Reconfigure Wallets (`WalletSetupDialog`) | Multi-step setup for Cash, Online, and ATM mode | **VERIFIED** |
| 23 | Hide Balances (Privacy Mask) | Security & Privacy > Privacy Mask switch | Blurs/masks balance numbers across Home, Wallets, and Dock | **VERIFIED** |
| 24 | ATM Flow Accounting | Wallets & Currency > ATM Flow (Spending vs Transfer) | Determines whether ATM withdrawals debit online as expense or transfer | **VERIFIED** |
| 25 | Biometric / App Lock | Security & Privacy > Biometric Lock switch | Enables biometric / PIN gate via `SecurityManager` on resume | **VERIFIED** |
| 26 | Test Lock Screen | Security & Privacy > Test Lock Screen action row | Immediately triggers `SecurityManager.lockImmediately()` and presents gate | **VERIFIED** |
| 27 | Privacy Policy | Security & Privacy > Privacy Policy row | Opens `PrivacyPolicyDialog` with offline security commitments | **VERIFIED** |
| 28 | Smart Merchant Rules | Rules & Categories > Smart Merchant Rules | Shows rule count; opens full CRUD sub-screen | **VERIFIED** |
| 29 | Custom Categories CRUD | Rules & Categories > Custom Categories | Expandable list, Add dialog with 30+ icons, Edit, and Delete dialogs | **VERIFIED** |
| 30 | SMS Permission | Permissions & System Detection > Status row | Real-time status badge (`RECEIVE_SMS`, `READ_SMS`); triggers grant flow | **VERIFIED** |
| 31 | Notification Listener | Permissions & System Detection > Status row | Real-time status badge; launches Android Notification Listener Settings | **VERIFIED** |
| 32 | Post Notifications | Permissions & System Detection > Status row | Real-time status badge (`POST_NOTIFICATIONS`); opens app notification settings | **VERIFIED** |
| 33 | Floating Quick Category Overlay | Permissions & System Detection > Status row | Real-time status badge (`SYSTEM_ALERT_WINDOW`); opens overlay permission settings | **VERIFIED** |
| 34 | Battery Optimization Exemption | Permissions & System Detection > Status row | Real-time status badge; opens power management exemption request | **VERIFIED** |
| 35 | Google Drive Cloud Backup Mode | Data & Backup > Cloud Backup (`CloudBackupDialog`) | Toggle between Offline Only and Google Drive Sync | **VERIFIED** |
| 36 | Cloud Auto-Backup Toggle | Data & Backup > Cloud Backup (`CloudBackupDialog`) | Daily background synchronization toggle | **VERIFIED** |
| 37 | Google Account Status & Sync Now | Data & Backup > Cloud Backup (`CloudBackupDialog`) | Displays signed-in Google account; Sync Now triggers `DriveBackupWorker` | **VERIFIED** |
| 38 | Export Offline Backup | Data & Backup > Export Offline Backup | AES-256 GCM encrypted JSON or plain JSON file export | **VERIFIED** |
| 39 | Restore Offline Backup | Data & Backup > Restore Offline Backup | File picker + decryption password prompt + Merge/Overwrite options | **VERIFIED** |
| 40 | Automated Monthly Statement | Data & Backup > Automated Monthly Statement | Dropdown selector (Off, Monthly, Weekly) for background statement worker | **VERIFIED** |
| 41 | Generate Statement | Data & Backup > Generate Statement (`ExportStatementSheet`) | Generates custom date-range PDF or CSV financial statement | **VERIFIED** |
| 42 | Replay Intro Walkthrough | Data & Backup > Replay Intro Walkthrough | Clears `has_completed_onboarding` and re-launches onboarding flow | **VERIFIED** |
| 43 | App Version & Updates | About & Support > Kanri Version row | Shows v1.1.0 (Build 4); opens `AppUpdateDialog` to check GitHub releases | **VERIFIED** |
| 44 | App Health & Diagnostic Logs | About & Support > App Health & Diagnostic Logs | Displays error count; opens `ConsumerCrashLogsDialog` (view, clear, share) | **VERIFIED** |
| 45 | Suggest a Feature | About & Support > Suggest a Feature | Opens `SuggestFeatureDialog` (category chips, title, details -> email intent) | **VERIFIED** |
| 46 | Contact Support | About & Support > Contact Support | Opens `ContactUsDialog` (email copy & mailto intent) | **VERIFIED** |
| 47 | Reset Opening Balances | Danger Zone > Reset Balances action row | Confirmation dialog; clears wallet balances safely | **VERIFIED** |
| 48 | Terminate App Process | Danger Zone > Terminate App action row | Confirmation dialog; terminates activity affinity and kills process | **VERIFIED** |
| 49 | Factory Reset App | Danger Zone > Factory Reset Entire App | Destructive modal requiring user to type "DELETE"; clears DB, keys & prefs | **VERIFIED** |

---

### 4.2 Theme Matrix Verification

| Theme Mode | Window / Backdrop | Card / Group Surface | Hairline Divider | Primary Text Contrast | Accent / Highlight | Status & Nav Bar Icons | Status |
|------------|-------------------|----------------------|------------------|-----------------------|--------------------|------------------------|--------|
| **Light** | `#FBF8EE` (Cream) | `#FFFFFF` (100% white card) | `#E2DEC9` (Muted cream border) | `#1A1C19` (High contrast black, > 12:1) | `#30A46C` (Forest emerald) | Dark icons (`isAppearanceLightStatusBars = true`) | **PASS** |
| **Dark** | `#121212` (Charcoal) | `#1E1E1E` (Dark elevated surface) | `rgba(255,255,255,0.08)` (Subtle hairline) | `#FFFFFF` (> 15:1 contrast) | `#30A46C` (Emerald) | Light icons (`isAppearanceLightStatusBars = false`) | **PASS** |
| **AMOLED** | `#000000` (True Pitch Black) | `#0D0D0D` (Distinguishable dark surface) | `rgba(255,255,255,0.12)` (Crisp edge border) | `#FFFFFF` (21:1 max contrast) | `#30A46C` (Emerald) | Light icons (`isAppearanceLightStatusBars = false`) | **PASS** |
| **Glass** | Active WebGL Shader (`WebView`) | Translucent blur tint (`rgba(255,255,255, 0.08)` + blur) | `rgba(255,255,255, 0.28)` (Prism edge shine) | `#FFFFFF` over frosted backdrop | `#30A46C` / Custom Tint | Adaptive light icons | **PASS** |
| **System (Light)** | `#FBF8EE` | `#FFFFFF` | `#E2DEC9` | `#1A1C19` | Dynamic/Emerald | Dark icons | **PASS** |
| **System (Dark)** | `#121212` | `#1E1E1E` | `rgba(255,255,255,0.08)` | `#FFFFFF` | Dynamic/Emerald | Light icons | **PASS** |

---

### 4.3 Theme Switching & Resource Lifecycle Verification

| Transition Pair | Visual Transition | Background Lifecycle | Shader / WebView Cost Outside Glass | State Preservation |
|-----------------|-------------------|----------------------|-------------------------------------|--------------------|
| **Dark -> Glass** | Smooth fade-in of frosted surfaces | `GlassBackdrop` mounts; WebGL shader starts rendering | N/A (Glass active) | Preserves previous shader selection & custom glass tokens |
| **Glass -> AMOLED** | Instant switch to true pitch black | `GlassBackdrop` unmounts; `stopLoading()` and `destroy()` called on WebView | **ZERO** (No WebView, no canvas, no GPU load) | Glass presets & shader preference remembered in preferences |
| **AMOLED -> Light** | Clean switch to soft cream palette | Solid background; no glass tokens composed | **ZERO** | Instant |
| **Light -> Glass** | Frosted blur smoothly animates in | `GlassBackdrop` mounts with selected shader | Active only during Glass | All glass adjustments intact |
| **Glass -> Dark** | Transitions to charcoal `#121212` | `GlassBackdrop` unmounts; WebView destroyed | **ZERO** | Glass settings preserved |
