# Kanri Onboarding & Stepper System

Current Version: **1.18.4** (Build 55)

## 1. Overview
The Kanri onboarding experience guides new users through permissions, personal profile configuration, and security settings with transparency, privacy assurances, and immediate tactile feedback.

In version 1.18.4:
- **Pure Black Kanri Monogram Emblem & Clean Hero**:
  - The kinetic emblem is deep black (`#000000`), matching the Panchang vector wordmark.
  - Subtitle tagline and HTML mock UI cards removed completely, giving an unobstructed view of the ink-painted landscape.
- **Slow, Meditative Pacing**:
  - Transition duration set to 3500ms glide / 3000ms paint with a 3200ms choreography interval so users can enjoy the calm flow of ink and scenery.
- **Centered Top Progress Bar & Bottom-Left Back Navigation**:
  - Symmetrically centered progress bar (`STEP X OF 7` + `LinearProgressIndicator`) at the top.
  - Circular frosted glass back button anchored at the bottom-left.
- **Elevated Slide Cards**:
  - All cards shifted upwards (`BiasAlignment(0f, -0.38f)`), providing an open view of the water and living scroll below.
- **Frosted Dark Glass Cards with Glass Buttons & White Typography**:
  - `PermissionCardView`: Dark frosted glass (`#141416` @ 74% opacity) with specular linear gradient border (`Brush.linearGradient`), glass buttons, and white text (`Color.White`).
  - `KokonutAvatarPicker`: Dark frosted glass container, white typography, glass photo picker & dice buttons, and glass CTA button with white text.
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
| 7 | **Lock** | *App Lock & Biometrics* | **Sacred Pagoda & Mountain Constellations** | Enables optional biometric or device PIN authentication on app resume. |

---

## 3. Panoramic Shan Shui Living Scroll (`shan_shui_onboarding.html`)
- **Zero HUD & Controls**: All developer bars, chapter switchers, and audio controls are stripped for a pure wallpaper experience (`display: none !important`).
- **Hardware-Accelerated 2D Canvas**: Renders ink wash mountains, pine branches, drifting mist, waterfall particles, and water reflections smoothly at 60fps.
- **Dynamic Step Synchronization**: In `OnboardingScreen.kt`, the background `WebView` listens to step updates and page load completions via `WebViewClient`:
  ```kotlin
  webView.evaluateJavascript("window.setOnboardingStep($currentSlide);", null)
  ```
  The canvas translates with smooth cubic bezier camera easing between sceneries.
- **Light & Dark Paper Adaptation**: Automatically responds to the active theme via `window.setPaperTheme('dark' | 'light')`.
- **Touch Passthrough**: Pointer events are set to `none` on the interaction layer, allowing native Compose touches, swipes, and button taps to pass through without interference.

---

## 4. ChapterPlayer Kinetic Intro Logo (`KanriIntroLogo.kt`)
On Slide 0 (Hero slide), the logo presents an expressive, physics-driven kinetic entrance inspired by ChapterPlayer:
1. **Monogram Emblem Center Landing**: The official Kanri geometric mark lands large in the center with exponential ease (`scale 1.85 -> 1.50`, `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`).
2. **Settle & Extend to Left**: Translates horizontally from canvas center to lockup left (`scale 1.50 -> 1.00`, `CubicBezierEasing(0.25f, 1f, 0.35f, 1f)`).
3. **Kinetic Typography Stagger**: The letters **K - A - N - R - I** in Panchang font rise and sharpen beside the emblem (`+16dp` offset, staggered at 65ms intervals).
4. **Subhead & Tagline Reveal**: Follows with a smooth fade & upward slide of `"FINANCIAL SERENITY / Know where every rupee went."`
5. **Organic Breathing**: A subtle, slow push-in (`scale 1.0 -> 1.02`) keeps the lockup alive with the flowing landscape.

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

## 4. State Management & Navigation Flow
- Managed in `OnboardingScreen.kt` using `rememberSaveable` state (`currentSlide: Int`).
- Slide 0 is the initial Hero screen.
- Slides 1 through 7 display the fixed top `Stepper` header with back button support.
- Clicking the back button or tapping an earlier step in the stepper decrements `currentSlide`.
- Once Slide 7 is completed (or skipped), `onFinished()` writes `is_onboarding_completed = true` to `DataStore` and launches `MainActivity`.

