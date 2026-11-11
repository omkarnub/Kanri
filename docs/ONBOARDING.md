# Kanri Onboarding & Stepper System

Current Version: **1.18.14** (Build 65)

## 1. Overview
The Kanri onboarding experience guides new users through permissions, personal profile configuration, and security settings with transparency, privacy assurances, and immediate tactile feedback.

In version 1.18.7:
- **Centered Lower-River Painting Integration (`PaintingPermissionView`)**:
  - Slides 2 through 7 center each permission's title, caption, frosted dark glass Enable pill (`Continue` if already granted), and compact `Skip` link cleanly over the calm lower river foreground (`Alignment.BottomCenter`), aligned with the centered progress header and comfortably within thumb reach.
  - Raised the sampan boat lane on Scenes 2–7 (`boatY = 1272..1295`) and Scene 6 cove lotus pads so the entire mountain panorama, birds, red seal, boat, and Easter eggs remain unobstructed above the prompt.

In version 1.18.6:
- **Painting-Integrated Permission Flow (`PaintingPermissionView`)**:
  - Replaces multi-button stacks with a single frosted dark glass Enable button (switching to `"Continue"` if returning on an already-granted step) paired cleanly beside a compact `"Skip"` action button.
- **Automatic Transition on Permission Grant**:
  - Tapping Enable on Slides 2–7 arms `awaitAutoAdvanceForStep`. Whether granted inline via OS permission dialog, enabled in Android Settings and returning via `ON_RESUME`, or toggled in-app on Step 7, Kanri briefly flashes the emerald checkmark feedback (`520ms`) and automatically advances to the next slide.
- **Minimalist Shan Shui Easter Eggs**:
  - Every permission slide subtly enriches the painting with a themed detail without cluttering minimalism: lit SMS handset & envelope (Step 2), cinnabar chime sparks (Step 3), hovering washi overlay card (Step 4), infinity firefly loop (Step 5), swaying pavilion wind-bell (Step 6), and pulsing biometric fingerprint ridges inside the red seal (Step 7).
- **Step 8 Final Sanctuary Slide (`FinalAllSetSlideView`)**:
  - Adds a 9th peaceful open-horizon scene (`buildScenery8_BaseWashes`) showcasing the pure black `KanriLogoLockup`, `"You're all set"` heading, subtitle, and a bottom frosted dark glass `"Finish"` button.
- **Balanced Calm Pacing & Default Sumi-e Mist Shader**:
  - Faster yet unhurried `1850ms` glide / `1650ms` paint (`1800ms` startup) pacing with `1750ms`–`1800ms` Compose transition synchronization.
  - First-launch live Glass shader defaults to `Sumi-e Mist` (`GlassBackgroundType.SUMI_E_MIST`).

In version 1.18.5:
- **Cardless, Painting-Integrated Permission Prompts (`PaintingPermissionView`)**:
  - Slides 2 through 7 remove rectangular card containers and seamlessly integrate permission prompts directly into the Shan Shui living scroll canvas using floating translucent ink seals, deep sumi typography with feathered Xuan parchment haloes, frosted glass action buttons, and `"Skip for now"` links.
- **Educational Permission Skip Warning Modals (`PermissionSkipWarningDialog`)**:
  - Attempting to skip or continue past an ungranted permission on Slides 2–7 presents an educational modal warning explaining why that permission is important for Kanri's core functionality, offering `"Enable Permission"` (primary) or `"Skip Anyway"`.
- **Clean Horizon & Smooth 60fps Canvas Engine**:
  - Removed horizontal rectangular pale mist strokes (`buildScenery0_BaseWashes` & `renderLiveMistVeils`) that intersected the mountains.
  - Optimized WebView rendering by bypassing CPU-bound SVG displacement filters (`feDisplacementMap`) and offscreen `destination-in` scratch compositing in `webview-mode`, delivering fluid GPU-accelerated 60fps canvas animation.

In version 1.18.4:
- **Pure Black Kanri Monogram Emblem & Clean Hero**:
  - The kinetic emblem is deep black (`#000000`), matching the Panchang vector wordmark.
  - Subtitle tagline and HTML mock UI cards removed completely, giving an unobstructed view of the ink-painted landscape.
- **Slow, Meditative Pacing**:
  - Transition duration set to 3500ms glide / 3000ms paint with a 3200ms choreography interval so users can enjoy the calm flow of ink and scenery.
- **Centered Top Progress Bar & Bottom-Left Back Navigation**:
  - Symmetrically centered progress bar (`STEP X OF 7` + `LinearProgressIndicator`) at the top.
  - Circular frosted glass back button anchored at the bottom-left.
- **Elevated Slide Layout**:
  - Profile card (`KokonutAvatarPicker`) and painting-integrated permissions shifted cleanly upward (`BiasAlignment(0f, -0.32f)` to `-0.38f`), keeping the river and sampan boat unobstructed below.
- **Frosted Dark Glass Cards & Buttons (`KokonutAvatarPicker`, `OnboardingPrivacySheet`, `AvatarCropDialog`)**:
  - `KokonutAvatarPicker` (Slide 1): Dark frosted glass container, white typography, glass photo picker & dice buttons, and glass CTA button with white text.
  - `OnboardingPrivacySheet`: Dark frosted glass sheet (`#141416` @ 94% opacity) with white typography, icon pills, and glass dismiss button.
  - **1:1 Avatar Cropper (`AvatarCropDialog`)**: Interactive 1:1 avatar cropping modal with pinch-to-zoom (0.5x to 4.0x), drag-to-pan, circular guide overlay, rule-of-thirds grid, and frosted glass action buttons with white text.

---

## 2. Onboarding Slide Structure & Panoramic Living Scroll
The onboarding sequence consists of a Hero intro followed by 7 setup slides, with each slide mapped directly to an individual hand-crafted panoramic scenery:

| Step | Slide | Title | Living Scroll Scenery | Purpose |
|------|-------|-------|-----------------------|---------|
| 0 | **Hero** | *Take Control of Your Money* | **Twin Peaks & Dawn Clouds** | ChapterPlayer kinetic logo landing, unobstructed Shan Shui landscape, offline-first philosophy, and "Get Started" CTA. |
| 1 | **Profile** | *Set Up Your Profile* | **Scholar's Pine & Mountain Brook** | Captures user display name, preferred avatar, and optional monthly budget cap. |
| 2 | **SMS** | *SMS Detection* | **Cloud Gorge Waterfalls & Rising Mist** | Requests `READ_SMS` and `RECEIVE_SMS` for offline bank debit/credit parsing. |
| 3 | **Apps** | *Payment App Notifications* | **Cinnabar Plum with 4 Swallows** | Guides user to grant Notification Listener access for PhonePe, GPay, Paytm, CRED, etc. |
| 4 | **Overlay** | *Quick Entry Overlay* | **Glowing Moonbridge & Silver Lanterns** | Prompts for `SYSTEM_ALERT_WINDOW` permission to enable the instant floating entry pad. |
| 5 | **Battery** | *Background Reliability* | **Bamboo Sanctuary & Floating Fireflies** | Requests battery optimization exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) for auto-rebind. |
| 6 | **Alerts** | *Spending Reminders* | **Sunset Pavilion Ripples & Gold Horizon** | Configures daily budget pacing alerts and scheduled digests (`POST_NOTIFICATIONS`). |
| 7 | **Lock** | *Biometric Security* | **Sacred Pagoda & Seal Fingerprint** | Enables biometric/device lock (`SecurityPreferences`); pulses subtle fingerprint ridges inside the cinnabar seal. |
| 8 | **Done** | *You're all set* | **Dawn Sanctuary & Open River** | Displays pure black Kanri logo lockup and `"Finish"` button to enter the main app. |

---

## 3. Panoramic Shan Shui Living Scroll (`shan_shui_onboarding.html`)
- **9 Synchronized Sceneries**: Expands across Hero (0), Profile (1), 6 permission scenes with contextual minimalist Easter eggs (2–7), and the Final Dawn Sanctuary scene (8).
- **Zero HUD & Controls**: All developer bars, chapter switchers, and audio controls are stripped for a pure wallpaper experience (`display: none !important`).
- **Hardware-Accelerated 2D Canvas**: Renders ink wash mountains, pine branches, waterfall particles, and water reflections smoothly at 60fps. In Android `webview-mode`, heavy CPU-bound SVG filters and scratch canvas masks are bypassed so hardware acceleration remains rock-solid.
- **Dynamic Step Synchronization**: In `OnboardingScreen.kt`, the background `WebView` listens to step updates and page load completions via `WebViewClient`:
  ```kotlin
  webView.evaluateJavascript("window.setOnboardingStep($currentSlide);", null)
  ```
  The canvas translates with smooth cubic bezier camera easing between sceneries.
- **Light Xuan Paper Lock**: Locked permanently to the warm handmade Xuan light paper theme (`currentTheme = 'light'`).
- **Touch Passthrough**: Pointer events are set to `none` on the interaction layer, allowing native Compose touches, swipes, and button taps to pass through without interference.

---

## 4. ChapterPlayer Kinetic Intro Logo (`KanriIntroLogo.kt`)
On Slide 0 (Hero slide), the logo presents an expressive, physics-driven kinetic entrance inspired by ChapterPlayer:
1. **Monogram Emblem Center Landing**: The pure black official Kanri geometric mark lands large in the center with exponential ease (`scale 1.85 -> 1.50`, `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`).
2. **Settle & Extend to Left**: Translates horizontally from canvas center to lockup left (`scale 1.50 -> 1.00`, `CubicBezierEasing(0.25f, 1f, 0.35f, 1f)`).
3. **Kinetic Typography Stagger**: The pure black letters **K - A - N - R - I** in Panchang font rise and sharpen beside the emblem (`+16dp` offset, staggered at 65ms intervals).
4. **Organic Breathing**: A subtle, slow push-in (`scale 1.0 -> 1.02`) keeps the lockup alive with the flowing landscape.

---

## 5. Interactive Stepper Architecture (`Stepper.kt`)

Package: `com.omkarnub.kanri.ui.common.stepper`

### Core Interfaces & Models
- **`StepItem`**:
  - `title`: Display label for the step (e.g. "Profile", "SMS", "Lock").
  - `description`: Optional supporting subtitle.
  - `icon`: Optional custom vector icon or painter.
  - `state`: `StepState.DEFAULT`, `StepState.LOADING`, or `StepState.ERROR`.
- **`StepperVariant`**:
  - `CIRCLE`: Numbered circle step indicator with animated checkmark on completion.
  - `CIRCLE_ALT`: Inset subtle circle indicator variant.
  - `LINE`: Minimalist horizontal bar indicator.
- **`StepperOrientation`**:
  - `HORIZONTAL`: Standard horizontal step progression bar with connector lines.
  - `VERTICAL`: Vertical timeline step progression.
- **`StepperSize`**:
  - `SM` (28dp), `MD` (34dp), `LG` (42dp).

### Animation & Interactivity
- **Checkmark Transition**: Completed steps animate from number to checkmark with `AnimatedContent` using `scaleIn(spring(dampingRatio = 0.6f))` and `fadeIn`.
- **Connector Fill**: Horizontal line dividers animate their fill color between inactive and primary mint/accent colors via `animateColorAsState`.
- **Haptic Touch Targets**: Clicking any previously completed step triggers `HapticFeedbackType.TextHandleMove` and navigates directly back to that step.
- **Header Badge**: Displays `STEP {currentStep} OF {totalSteps} · {stepTitle}` with dynamic animated fade transition when switching steps.

---

## 6. State Management & Navigation Flow
- Managed in `OnboardingScreen.kt` using `currentSlide: Int` (`0..8`) and `awaitAutoAdvanceForStep: Int`.
- Slide 0 is the initial Hero screen (`HeroSlideView`).
- Slides 1 through 7 display the centered top linear progress bar (`STEP 1..7 OF 7`); Slide 8 hides the progress bar for an open dawn arrival (`FinalAllSetSlideView`).
- A circular frosted glass Back button remains anchored at bottom-left across Slides 1 through 8 (`stepBack()`).
- Granting any permission on Slides 2 through 7 triggers an automatic `520ms` confirmation beat followed by smooth progression to the next slide; tapping `"Finish"` on Slide 8 writes `hasCompletedOnboarding = true` and launches the home ledger.

