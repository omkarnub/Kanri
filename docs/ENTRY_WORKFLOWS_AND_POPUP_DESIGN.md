# Entry Workflows & Action Popup Design

**Current Version:** 1.5.1 (Build 22)  
**Last Updated:** 2026-11-07  

---

## 1. Overview
Kanri provides a unified, fluid financial logging workflow for all core entry types:
1. **Expense**: Logging spent money across categories and accounts.
2. **Income**: Recording received money with optional savings goal contribution.
3. **Lend & Borrow**: Personal debt ledger tracking funds lent to or borrowed from contacts.
4. **Split Expense**: Splitting group bills between multiple friends with automatic ledger synchronization.
5. **Instant Payment Detection Popup**: Floating interactive overlay triggered when SMS or UPI payment notifications arrive.

Every entry follows the identical 3-step Apple-grade pop-up workflow:
1. Zero-start Amount Pad (`KokonutAmountPad`)
2. Half-screen Fill Details popup with uniformly arranged fields, Back, Cancel, and Proceed buttons
3. Down-to-up small half-screen summary drawer with rolling numbers and slide-to-confirm slider (`KokonutSmoothDrawer` + `KokonutSlideToConfirm`).

In Glass Mode, all entry surfaces enforce a fixed 50 dp blur and 10% dark tint via `GlassLevel.EntrySheet`. In Solid Modes (Light, Dark, AMOLED, and System), all entry surfaces use crisp, fully opaque theme container backgrounds without blur or transparency.

---

## 2. Navigation Z-Order Elevation & Floating Dock Behavior
To eliminate any possibility of bottom dialogs appearing behind the bottom floating dock (`KanriFloatingDock`), all modal dialogs are elevated to the root navigation overlay container in `MainNavigationScreen.kt`:

- **Root Z-Order Elevation**: `AddTransactionDialog`, `LendingEntryDialog`, `SplitExpenseEntryDialog`, and `LendingTransactionBridgeDialog` are rendered in the root Box of `MainNavigationScreen.kt`, *above* the tab contents and above `KanriFloatingDock`.
- **Dock Auto-Slide Transition**: When any entry sheet is active (`isAnyEntryModalOpen`), `KanriFloatingDock` smoothly slides down off-screen via `slideOutVertically + fadeOut`. When the sheet is confirmed or dismissed, the dock smoothly slides back into position via `slideInVertically + fadeIn`.
- **Delegated Entry Callbacks**: `LendingScreen` and `HomeScreen` expose `onOpenAddEntry`, `onOpenSplitExpense`, and `onOpenLendBorrowTransaction` callbacks so that all user actions (e.g. "Add Record", "Track Loan", "Edit", "Split a Group Expense", "Bridge from Transaction") trigger the top-level elevated container.
- **Pure In-Compose Architecture**: `SplitExpenseEntryDialog` was converted to a pure in-compose sheet (matching `LendingEntryDialog` and `AddTransactionDialog`), ensuring direct sampling of the backdrop `HazeState` without native dialog window boundary limitations.

---

## 3. Floating Dock Action Popup (`AddExpenseIncomePopup`)
The floating dock '+' button displays a hardware-accelerated frosted glass card (`KanriGlassCard`) elevated directly above the bottom navigation dock.

### Core Options
| Option | Subtitle | Accent Color | Custom Flow Icon | Triggered Dialog |
|---|---|---|---|---|
| **Expense** | Log spent money | Vibrant Red (`#E54D2E`) | `ExpenseFlowIcon` | `AddTransactionDialog(initialType = "DEBIT")` |
| **Income** | Log received money | Sage Green (`#30A46C`) | `IncomeFlowIcon` | `AddTransactionDialog(initialType = "CREDIT")` |

*Note:* Lend & Borrow and Split Bill are initiated from the Lending Hub screen and transaction category flows, keeping the floating dock action popup focused and uncluttered.

### Visual & Interactive Behavior
- **Fluid Spring Animation**: Enters with snappy scale-in (`0.90 -> 1.0f`) and vertical spring translation (`KanriMotion.SmoothSlideSpring`).
- **Glassmorphic Adaptation**: Dynamically consumes `tokens.tintLightness`, border gradients, and elevation shadows matching the current glass theme.
- **Dismissal**: Dismisses on tap outside or upon selecting an entry option.

---

## 4. The 3-Step Unified Pop-up Workflow
Every manual entry is rendered as a bottom-anchored modal popup sheet rather than a centered or full-page dialog.

```mermaid
flowchart LR
    A["Step 1: Amount Pad<br/>(KokonutAmountPad)"] -->|"Continue"| B["Step 2: Fill Details<br/>(KanriGlassCard 26dp)"]
    B -->|"Proceed"| C["Step 3: Summary Drawer<br/>(KokonutSmoothDrawer)"]
    C -->|"Slide to Confirm"| D["Save Record<br/>& Haptic Feedback"]
    B -->|"Tap Amount / Back"| A
```

### Step 1: Zero-Start Amount Pad (`KokonutAmountPad`)
- Large, zero-start fluid keypad.
- Formatted currency display with live rolling digits.
- Clean pull/dismiss drag handle at the top.
- Proceeding advances directly to the Fill Details step with the entered amount preserved.
- Calibrated to `GlassLevel.EntrySheet` (frosted 50 dp blur in Glass Mode; crisp opaque container background in Solid Light/Dark/AMOLED modes).

### Step 2: Fill Details (`KanriGlassCard`)
- Bottom-docked half-screen card with 26 dp rounded top corners (NO full-screen stretching).
- **Uniform Info Fields Arrangement**:
  - Drag handle and header title/subtitle.
  - **Tappable Amount Card**: Displays the entered amount with an "Edit" affordance; tapping returns to Step 1 without data loss.
  - Role/Type toggle (e.g. "I Lent" vs "I Borrowed").
  - Target/Person Name field with integrated Profile and Contacts pickers, plus quick suggestion chips.
  - Date & Time row (Date chip + Time chip).
  - Expected Return / Due date chips with quick presets and calendar picker.
  - Wallet impact chips ("Cash", "Online", "Not from balances").
  - Contextual Note field.
- **Action Buttons**: "Back" text button on the left, "Cancel" text button and bold "Proceed" primary button on the right.
- Calibrated to `GlassLevel.EntrySheet` (frosted 50 dp blur in Glass Mode; crisp opaque container background in Solid Light/Dark/AMOLED modes).

### Step 3: Summary Drawer & Slide to Confirm (`KokonutSmoothDrawer`)
- Down-to-up small half-screen summary drawer with detailed breakdown rows.
- Rolling total amount display.
- Semantic confirmation color and label (`SlideConfirmationType.EXPENSE`, `INCOME`, `LEND`, or `BORROW`).
- **`KokonutSlideToConfirm`**: Zero-latency drag gesture with progressive haptic feedback, snap animation, loading spinner, and success checkmark before automatically dismissing the dialog.
- Calibrated to `GlassLevel.EntrySheet` (frosted 50 dp blur in Glass Mode; crisp opaque container background in Solid Light/Dark/AMOLED modes).

---

## 5. Instant Payment Detection Floating Popup Card (`InstantPopupCard` & `InstantPopupService`)
When an incoming transaction SMS or UPI notification is received, Kanri presents an interactive floating card directly over the screen:

### Visual Architecture & Glassmorphic Optics
- **Android WindowManager Compositor Blur**: On Android 12+ (API 31+), the overlay window enables `FLAG_BLUR_BEHIND` with `blurBehindRadius = 45.dp` in Glass mode.
- **45% Dark Scrim Backdrop**: A full-screen translucent scrim (`Color.Black.copy(alpha = 0.45f)`) dims the underlying screen with coordinated `fadeIn`/`fadeOut` transitions.
- **Translucent Frosted Card Container**: The card is rendered with a 35% dark tint (`#141622` at 0.35 alpha), a vertical white sheen gradient, a 0.8 dp specular gradient border, and an inner 1 dp top specular highlight rim, allowing the hardware-accelerated 45 dp blur of the underlying screen to shine through cleanly with real optical depth.
- **Theme-Adaptive Surface**: Unified across all themes:
  - **Glass Mode**: Hardware-accelerated cross-window blur with translucent specular glass card.
  - **Light Mode**: Pure crisp white elevated surface (`#FFFFFF`) with subtle border and elevation shadow.
  - **Dark Mode**: High-contrast dark charcoal surface (`#161618`) with elevation shadow.
  - **AMOLED Mode**: Deep pure black surface (`#000000`) with high-contrast border.

### Interaction & UX Layout
1. **Header**: Clean, centered dim grey date & timestamp (`07 Nov, 05:45 AM`) in `GoogleSansFlex` with an aligned close icon button.
2. **Amount**: Large 40 sp bold currency typography in Vibrant Red (`#E54D2E`) for expense and Sage Green (`#30A46C`) for income.
3. **Merchant & Tag**: Clean merchant name and bank/wallet brand tag.
4. **Left-Aligned Search**: Plain text hyperlink with search icon on the left to quickly expand the search input box.
5. **Top 5 Category Pills**: Displays only the top 5 relevant/smart-resolved categories in capsule pills (`RoundedCornerShape(50)`).
6. **Capsule Notes Prompt**: Pill-shaped notes input box (`RoundedCornerShape(50)`) with clear button.
7. **Single Capsule Done Button**: Full-width primary CTA with circular edges (`RoundedCornerShape(50)`).
8. **Split Expense Action**: Direct hyperlink at the bottom ("Split expense ›") opening the split expense flow with pre-filled amount and merchant.

### Coordinated Pop-up & Pop-down Motion
The popup card shares the exact signature spring motion from `AddExpenseIncomePopup`:
- **Pop-up (Entrance)**: `fadeIn(180ms)` + `scaleIn(0.90f, KanriMotion.SnappySpring)` + `slideInVertically(10%, KanriMotion.SmoothSlideSpring)`.
- **Pop-down (Exit)**: `fadeOut(140ms)` + `scaleOut(0.94f)` + `slideOutVertically(8%)`.
- Coordinated through `dismissWithAnimation` across outside dismiss, close button, Done button, and Split expense action.

---

## 6. Modules & Files Involved
- `app/src/main/java/com/omkarnub/kanri/ui/popup/InstantPopupCard.kt`: Themed floating payment detection popup card.
- `app/src/main/java/com/omkarnub/kanri/ui/popup/InstantPopupService.kt`: Overlay service managing `WindowManager.LayoutParams`, cross-window blur, background scrim, and lifecycle.
- `app/src/main/java/com/omkarnub/kanri/ui/popup/InstantPopupSheet.kt`: In-app quick categorization bottom sheet and demo popup triggers.
- `app/src/main/java/com/omkarnub/kanri/ui/settings/SettingsComponents.kt`: Settings screen item for previewing the detection popup.
- `app/src/main/java/com/omkarnub/kanri/ui/common/KanriGlass.kt`: Core glassmorphism tokens, `KanriGlassCard`, and `GlassLevel.EntrySheet`.
- `app/src/main/java/com/omkarnub/kanri/ui/navigation/AddExpenseIncomePopup.kt`: Action popup menu and signature spring motion curves (`KanriMotion`).
- `app/src/main/java/com/omkarnub/kanri/ui/navigation/MainNavigationScreen.kt`: Navigation host wiring floating dock popup and dialog triggers.
