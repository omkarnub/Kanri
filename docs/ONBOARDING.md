# Kanri Onboarding & Stepper System

Current Version: **1.13.0** (Build 39)

## 1. Overview
The Kanri onboarding experience guides new users through permissions, personal profile configuration, and security settings with transparency, privacy assurances, and immediate tactile feedback.

In version 1.13.0, the onboarding flow is powered by an interactive Jetpack Compose **Stepper** navigation architecture inspired by modern accessible Stepper design patterns.

---

## 2. Onboarding Slide Structure
The onboarding sequence consists of a Hero intro followed by 7 setup slides:

| Step | Slide | Title | Purpose |
|------|-------|-------|---------|
| — | **Hero** | *Take Control of Your Money* | Welcomes the user, explains offline-first philosophy, and provides the "Get Started" CTA. |
| 1 | **Profile** | *Set Up Your Profile* | Captures user display name, preferred avatar, and optional monthly budget cap. |
| 2 | **SMS** | *SMS Detection* | Requests `READ_SMS` and `RECEIVE_SMS` for offline bank debit/credit parsing. |
| 3 | **Apps** | *Payment App Notifications* | Guides user to grant Notification Listener access for PhonePe, GPay, Paytm, CRED, etc. |
| 4 | **Overlay** | *Quick Entry Overlay* | Prompts for `SYSTEM_ALERT_WINDOW` permission to enable the instant floating entry pad. |
| 5 | **Battery** | *Background Reliability* | Requests battery optimization exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) for auto-rebind. |
| 6 | **Alerts** | *Spending Reminders* | Configures daily budget pacing alerts and scheduled digests (`POST_NOTIFICATIONS`). |
| 7 | **Lock** | *App Lock & Biometrics* | Enables optional biometric or device PIN authentication on app resume. |

---

## 3. Interactive Stepper Architecture (`Stepper.kt`)

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

