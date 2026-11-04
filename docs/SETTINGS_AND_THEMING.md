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
       - **Live Shader Backgrounds**: 8 animated WebGL shaders displayed **side by side in 2 columns** (`Gemini Aurora`, `Grain Gradient`, `Hero Geometric`, `Prism Gradient`, `Cloud Shader`, `Ink Ridges`, `Fuji Sakura`, `Sumi-e Mist`).
       - **Customise Glass Components** action row navigating to `GlassCustomizerScreen`.
   - **App Launcher Icon**: Toggle between Dark and Light activity aliases with confirmation dialog.

2. **Wallets & Currency**:
   - Primary App Currency selection (40+ international currencies with flags and symbols).
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
  - Header: `"Happy Birthday, {name}! 🎂"`
  - Subtitle: `"Wishing you joy, good health, and abundance in the year ahead ✨"`
- **Birthday Notifications**: `BirthdayHelper.checkAndTriggerBirthdayNotification` fires an annual high-priority celebration notification on their birthday.
