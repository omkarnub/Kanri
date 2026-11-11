# Settings & Theming Architecture

## 1. Overview
The Settings experience in Kanri provides a centralized, cohesive control center for configuring appearance, account balances, security gates, automated rules, system permissions, and offline data backups. 

The settings architecture adheres to:
- **Clean Architecture & Unidirectional Data Flow**: State hoisted to ViewModels and reactive Preference stores (`ThemePreferences`, `WalletPreferences`, `SecurityPreferences`, `UserProfilePreferences`), observing via StateFlow / MutableState.
- **Strict Theme Decoupling**: Glass-specific UI and live WebGL backgrounds are strictly mounted only when the `GLASS` theme mode is active. In solid modes (`LIGHT`, `DARK`, `AMOLED`, and `SYSTEM`), zero glass surfaces, WebViews, or background shaders run.
- **100% Offline-First Data**: Financial data, backups, and profile information stay securely on-device with AES-256 GCM encryption.

---

## 2. Information Architecture: 8 Functional Sections

1. **Customisation**:
   - **Themes Sub-Window** (`ThemeSettingsScreen`):
     - Visual phone preview cards for `System`, `Light`, `Dark`, `AMOLED`, and `Glass`.
     - When `Glass` is selected:
       - **Live Shader Backgrounds**: Minimalist, single-column vertical list view of 22 animated WebGL, Three.js, and Canvas 2D backgrounds:
         - `Gemini Aurora`: Dual-cycle neural aurora morphing between lime, turquoise, violet, coral, and pink.
         - `Grain Gradient`: Film-grade atmospheric organic grain.
         - `Hero Geometric`: Precision isometric low-poly geometry.
         - `Prism Swirl`: Dynamic refractive glass caustics.
         - `Cloudscape`: Volumetric cumulus cloudscape.
         - `Ink Ridges`: Japanese ink wash topographical flow.
         - `Fuji & Sakura`: 9:16 pixel hanging scroll (Mt. Fuji, golden mist, pagoda, torii gate & sakura blossoms).
         - `Sumi-e Mist`: Minimalist monochrome ink diffusion.
         - `Thistle Portico`: Gothic stone portico in wisteria bloom with castle vista & drifting petals.
         - `Still Sunset Meadow` (24h): 3D voxel meadow with interactive grass, voxel flora & dynamic photorealistic 24-hour day/sunset/night cycle driven by local time.
         - `Twilight Meadow`: Pixel art twilight alpine lake with wildflower asters & crescent moon.
         - `Tokyo Night`: Pixel art Tokyo night skyline vista with mountain bench & companion cat.
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
         - **Pure Loop & No HUD**: All HUD elements (digital clocks, time badges, audio hints, controls) are completely stripped so only the pure wallpaper plays seamlessly in the background. Backgrounds with 24-hour cycles automatically synchronize with the user's real-time clock.
        - **Customise Glass Components** action row navigating to `GlassCustomizerScreen`:
          - **Frosted Blur (0 to 50 dp)**: Directly scales hardware backdrop blur through Haze / RenderNode without restrictive minimum clamping, allowing clean transitions from crystal clear glass (0 dp) up to silky, heavy frosted diffusion (50 dp).
          - **Glass Tint (-1.0 to +1.0)**: Center-neutral slider (default `0.0f` = "No tint") allowing smooth adjustment to dark smoky obsidian glass (sliding left) or luminous crystalline white frost glass (sliding right), complete with quick-snap buttons (`Dark -50%`, `No tint 0%`, `Light +50%`). Both the content cards and the floating navigation dock actively follow this tint in real time.
          - **Border & Edge Shine (0% to 70%)**: Controls specular edge reflection intensity from fully off (0%) up to intense luminance across cards and dock.
          - **Corner Curvature (10 to 32 dp)**: Modifies corner radius dynamically across cards.
   - **Widgets Sub-Window** (`WidgetsScreen`):
     - Dedicated gallery for customising Android home screen widgets with 1:1 live interactive previews across three design styles:
       1. **iPhone Fluid**: Apple HIG squircle geometry (`24dp`), Cupertino vibrant health tints (Coral, Mint, Electric Blue), activity rings with overlapping shadow depth on Canvas, and fluid spring animations.
       2. **100x Liquid Glass**: Apple WWDC25 Liquid Glass specular reflections, top-rim shine gradient (60% to 5%), ambient radial colored backlight bloom, and glowing neon ring arcs.
       3. **Pixel DotTiles**: Complete native port of the Louis Studios DotTiles specification, featuring 7-row dot-matrix font numerals, live jitter drift, tick sliders with marker blocks, stepped histograms, and dot timelines.
     - **Actions**:
       - Segmented style tab switcher (` iPhone Fluid` | `✦ 100x Glass` | `⠶ Pixel DotTiles`).
       - "Set as Home Screen Widget Style" button applying the selected style across all 8 active home screen widgets.
       - One-tap "Pin to Home Screen" shortcut on every preview card.
       - Tap-to-replay animation trigger on any widget card.
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
   - **Category Order Customization** (`CategoryReorderSheet`):
     - Interactive modal bottom sheet styled with KanriModalBottomSheet providing native FLAG_BLUR_BEHIND window blur and heavy frosted glassmorphic styling (kanriGlass(level = GlassLevel.Strong)).
     - Move Up / Move Down buttons for quick reordering.
     - "Reset" button to restore factory standard category sequence.
     - Backed by Room DB `display_order` column on `CategoryEntity` (`MIGRATION_11_12`).
     - Directly powers category ordering across `CategoryPickerSheet` and transaction detail screens.

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

Kanri combines Google's Jetpack Splash Screen library (`androidx.core:core-splashscreen`) with an Apple-grade Compose logo reveal overlay (`KanriSplashScreen`):
- **Theme Starting Styles**:
  - `Theme.Kanri.Starting` (Dark starting theme, inherits from `Theme.SplashScreen`)
  - `Theme.Kanri.Starting.Light` (Light starting theme, inherits from `Theme.SplashScreen`)
- **Attributes & Circular Artifact Elimination**:
  - `windowSplashScreenBackground`: Solid black (`#000000`) or cream (`#FEFAEC`).
  - `windowSplashScreenAnimatedIcon`: Centered vector branding (`@drawable/panchang_light` / `@drawable/panchang_dark`).
  - `android:windowSplashScreenIconBackgroundColor` is intentionally omitted, preventing the OS from rendering a circular disc / circular cutout behind the wordmark.
- **Cinematic Logo Reveal Animation (`KanriSplashScreen`)**:
  - **Staggered Letter Slide-In**: The 5 vector letters (`K`, `A`, `N`, `R`, `I`) animate up through isolated clip rect boundaries from `+110%` to `0%` using `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)` with staggered delays (220ms + index * 100ms).
  - **Underline Rule Sweep**: An accent rule draws across beneath the mark from left to right (origin left-center) and then wipes away cleanly to the right (origin right-center) over 1250ms with `CubicBezierEasing(0.7f, 0f, 0.3f, 1f)`.
  - **Render-Gated App Transition**: The splash screen stays mounted until the main application Compose hierarchy and databases are fully measured and rendered (confirmed via double `withFrameNanos`), guaranteeing zero blank flickers or layout jumps.
  - **Fluid Staggered Exit**: Once rendering and the reveal conclude, the letters slide up and out staggered into the ceiling (`0%` -> `-110%`) while the splash overlay cross-fades out and the main app smoothly scales up with `KanriMotion.SnappySpring`.
  - **Clean Unmount**: After exit transition completes, the splash composable is unmounted from the tree for zero runtime memory footprint.
- **Version**: Kanri `1.9.0` (Build 32).
