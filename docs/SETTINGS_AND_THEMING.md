# Settings & Theming Architecture

## 1. Overview
The Settings experience in Kanri provides a centralized, cohesive control center for configuring appearance, account balances, security gates, automated rules, system permissions, and offline data backups. 

The settings architecture adheres to:
- **Clean Architecture & Unidirectional Data Flow**: State hoisted to ViewModels and reactive Preference stores (`ThemePreferences`, `WalletPreferences`, `SecurityPreferences`, `UserProfilePreferences`), observing via StateFlow / MutableState.
- **Strict Theme Decoupling**: Glass-specific UI and live WebGL backgrounds are strictly mounted only when the `GLASS` theme mode is active. In solid modes (`LIGHT`, `DARK`, `AMOLED`, and `SYSTEM`), zero glass surfaces, WebViews, or background shaders run.
- **100% Offline-First Data**: Financial data, backups, and profile information stay securely on-device with AES-256 GCM encryption.

---

## 2. Information Architecture: 8 Functional Sections

1. **Appearance**:
   - **Themes Sub-Window** (`ThemeSettingsScreen`):
     - Visual phone preview cards for `System`, `Light`, `Dark`, `AMOLED`, and `Glass`.
     - When `Glass` is selected:
       - **Live Shader Backgrounds**: Minimalist, single-column vertical list view of 10 animated WebGL / Canvas backgrounds (`Gemini Aurora`, `Grain Gradient`, `Hero Geometric`, `Prism Swirl`, `Cloudscape`, `Ink Ridges`, `Fuji & Sakura`, `Sumi-e Mist`, `Thistle Portico`, `Still Sunset Meadow`).
         - **No Previews**: Removed gradient preview swatches for a clean, distraction-free typographic list view.
         - **Apply / Applied Button**: Each row features a dedicated right-aligned action button ("Apply" with outline when unselected, or filled "Applied" with a checkmark when active), allowing instant selection alongside direct row-tapping.
         - **Thistle Portico**: High-performance Canvas 2D scene with pointed Gothic arch, castle view, wisteria racemes swaying in the wind, falling petals, and luminous light shafts.
         - **Still Sunset Meadow**: 3D voxel landscape with interactive grass parting, blocky flowers, rolling hills, and a 15-keyframe photorealistic 24-hour day cycle (Golden Rose sunrise, crisp morning azure, real-life Rayleigh daylight blue, fiery sunset peak, and starry cosmic night) driven by the user's local time zone.
        - **Customise Glass Components** action row navigating to `GlassCustomizerScreen`:
          - **Frosted Blur (0 to 50 dp)**: Directly scales hardware backdrop blur through Haze / RenderNode without restrictive minimum clamping, allowing clean transitions from crystal clear glass (0 dp) up to silky, heavy frosted diffusion (50 dp).
          - **Glass Tint (-1.0 to +1.0)**: Center-neutral slider (default `0.0f` = "No tint") allowing smooth adjustment to dark smoky obsidian glass (sliding left) or luminous crystalline white frost glass (sliding right), complete with quick-snap buttons (`Dark -50%`, `No tint 0%`, `Light +50%`). Both the content cards and the floating navigation dock actively follow this tint in real time.
          - **Border & Edge Shine (0% to 70%)**: Controls specular edge reflection intensity from fully off (0%) up to intense luminance across cards and dock.
          - **Corner Curvature (10 to 32 dp)**: Modifies corner radius dynamically across cards.
   - **App Launcher Icon**: Toggle between Dark and Light activity aliases with confirmation dialog.

2. **Wallets & Currency**:
   - Primary App Currency selection (52 international currencies with standard national flag indicators, symbols, and currency-scaled default monthly budgets & quick-entry presets).
   - **Budget Cycle Start Day**: Configurable day of month (1 to 28/31) to align monthly budget cycles and safe-to-spend calculations with the user's monthly salary / payday rather than forcing the 1st of the month.
   - Cash Opening Balance direct correction.
   - Online Opening Balance direct correction.
   - Reconfigure Wallets wizard.
   - ATM Withdrawal Accounting toggle (`SPENDING` vs `TRANSFER`).

3. **Security & Privacy**:
   - Biometric / Screen Lock switch.
   - Privacy Mask (Balance Spoiler) switch.
   - Offline Privacy Policy dialog.

4. **Rules & Categories**:
   - Smart Merchant Rules sub-screen (keyword -> category auto-mapping).
   - Custom Categories management (create, edit, delete with 30+ custom icons).

5. **Permissions & System Detection**:
   - Modern **toggle switches** with real-time status and system deep-links for:
     - Bank SMS Detection (`RECEIVE_SMS`, `READ_SMS`).
     - Payment Notification Listener (`BIND_NOTIFICATION_LISTENER_SERVICE`).
     - App Notifications (`POST_NOTIFICATIONS`).
     - Floating Overlay Quick Category (`SYSTEM_ALERT_WINDOW`).
     - Background Battery Exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

6. **Data & Backup**:
   - Offline Backup Export (Encrypted AES-256 GCM or plain JSON).
   - Offline Backup Restore (File picker + passphrase decrypt + merge/overwrite).
   - Automated Monthly Statement frequency scheduler.
   - Generate Financial Statement sheet (PDF / CSV export).
   - Replay Intro Walkthrough trigger.

7. **About & Support**:
   - Kanri App Version & GitHub Releases update checker.
   - App Health & Diagnostic Logs (view, clear, share crash logs).
   - Suggest a Feature dialog (email intent).
   - Contact Support dialog (clipboard & email intent).

8. **Danger Zone**:
   - Reset Opening Balances (clears wallet entities).
   - Terminate App Process (`finishAffinity` + process kill).
   - Factory Reset Entire App (destructive dialog requiring typing "DELETE").

---

## 3. Customise Glass Components (`GlassCustomizerScreen`)

The glass customization interface has been refined into a minimal, focused tool:
- **No live preview container**: Changes apply immediately to the actual running app backdrop and UI cards.
- **No presets**: Eliminates confusing preset overrides.
- **4 Essential Sliders**:
  1. **Frosted blur** (`blurRadiusDp`: 0dp - 50dp)
  2. **Glass tint** (`surfaceOpacity`: 2% - 40%)
  3. **Border & edge shine** (`borderOpacity`: 0% - 70%)
  4. **Corner curvature** (`cornerRadiusDp`: 10dp - 32dp)
- **Actions**:
  - **Reset to default**: Reverts sliders to standard values.
  - **Save & Exit**: Saves configuration to preferences and navigates back.

---

## 4. User Birthday & Celebration System

- **Setting Birthday**: In `ProfileScreen`, user can tap the Date of Birth row to open `KanriGlassDatePickerDialog` and select their birthday (formatted as `dd MMM yyyy`).
- **Dynamic Greeting**: On the user's birthday, `HomeViewModel` and `GreetingResolver` display a festive greeting:
  - Header: `"Happy Birthday, {name}!"`
  - Subtitle: `"Wishing you joy, good health, and abundance in the year ahead."`
- **Birthday Notifications**: `BirthdayHelper.checkAndTriggerBirthdayNotification` fires an annual high-priority celebration notification on their birthday.

---

## 5. Clean, Professional Visual Language

Kanri adheres strictly to a modern, typographic, and vector-driven design language:
- **No Decorative Emojis**: Emojis are eliminated from user-facing app text, dialogs, widgets, greetings, and category lists.
- **Letter Monograms**: User avatars utilize bold uppercase letter monograms against rich gradient surfaces (`KokonutAvatarSelector`).
- **Vector Icons**: Actions, categories, reminders, and recurrent operations use Material Symbols / vector icons (`androidx.compose.material.icons`).
- **Standard Currency Flags**: Currencies in the currency selection sheet and settings subtitle utilize standard regional flag indicators for clear, intuitive international currency identification.

---

## 6. Splash Screen & Launch Flow Architecture

Kanri uses Google's official Jetpack Splash Screen library (`androidx.core:core-splashscreen`):
- **Theme Starting Styles**:
  - `Theme.Kanri.Starting` (Dark starting theme, inherits from `Theme.SplashScreen`)
  - `Theme.Kanri.Starting.Light` (Light starting theme, inherits from `Theme.SplashScreen`)
- **Attributes & Circular Artifact Elimination**:
  - `windowSplashScreenBackground`: Solid black (`#000000`) or cream (`#FEFAEC`).
  - `windowSplashScreenAnimatedIcon`: Centered vector branding (`@drawable/panchang_light` / `@drawable/panchang_dark`).
  - `android:windowSplashScreenIconBackgroundColor` is intentionally omitted, preventing the OS from rendering a circular disc / circular cutout behind the wordmark.
- **Fluid Exit & Handover Choreography**:
  - Initialized in `MainActivity.onCreate()` via `installSplashScreen()`.
  - Conditioned with `splashScreen.setKeepOnScreenCondition { !isAppReady }` to guarantee the splash screen stays mounted until Compose initializes the first frame.
  - Overridden via `splashScreen.setOnExitAnimationListener`:
    - Wordmark floats slightly upward (-16dp) and scales (1.10x) as it dissolves over 320ms (`cubic-bezier(0.23, 1, 0.32, 1)`).
    - Splash background cross-fades over 380ms.
    - Compose content executes an in-sync entrance reveal (`scale 0.98 -> 1.0` and `alpha 0.85 -> 1.0` over 380ms) for a continuous, cinematic handover from splash into the app.
- **Version**: Kanri `1.6.0` (Build 27).
