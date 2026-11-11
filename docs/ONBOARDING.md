# Kanri Onboarding & Stepper System

Current Version: **1.18.1** (Build 52)

## 1. Overview
The Kanri onboarding experience guides new users through permissions, personal profile configuration, and security settings with transparency, privacy assurances, and immediate tactile feedback.

In version 1.18.0, the onboarding flow is completely rebuilt from scratch:
- **Two-Way JavaScript Bridge**: `AndroidBridge.onPaintingFinished(step)` and `AndroidBridge.onGlideFinished(step)` synchronize the HTML5 canvas living scroll with Compose state.
- **Screen Fade-In & Synchronized Painting**: Background paints Scene 0 ink washes (3800ms) with foreground UI hidden until the ink bloom finishes.
- **100% Identical Louis Studios ChapterPlayer Intro Animation**: `KanriIntroLogo.kt` directly implements the mathematical equations and timing from Louis Studios' `ChapterPlayer` (emblem center landing, cubic Hermite lockup settle, kinetic Panchang letter emergence, masked subtitle reveal, and living push-in).
- **Sequential Two-Phase Transitions**: Tapping **Get Started** or **Continue** fades out the current UI first before progressing the background camera glide.
- **Unified Slider Rhythm Across All Steps**: Step cards (Avatar Picker, SMS, Apps, Overlay, Battery, Alerts, Lock) enter and exit with matching fade and subtle scale transforms.
- **Interactive Stepper Navigation**: Steps 1 through 7 feature the top `Stepper` header with haptic navigation and back button support.

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

