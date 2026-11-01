# KANRI — UI CORRECTION & COMPONENT PORT (STRICT, NO CREATIVE LIBERTY)

You are working in the Kanri Android project (Kotlin, Jetpack Compose, Material 3, Room + SQLCipher, minSdk 26, package `com.omkarnub.kanri`). Kanri is 100% offline and privacy-first. Do not add network calls, analytics, or cloud features.

A previous pass at this task was done badly. **Treat everything the previous pass produced for the components below as untrusted.** Do not "tweak" it. For every component below, compare against the original source (Appendix) and the specs in this document, and if it does not match exactly, **delete it and rewrite it from the spec**. Use `git log` / `git diff` to see what the previous pass changed.

## 0. GROUND RULES (read twice)

1. **The original source in the Appendix is the single source of truth.** The components are React + Tailwind + `motion/react` (kokonutui). Kanri is native Compose, so each one must be **ported 1:1**: same layout, same sizes, same colors, same text styles, same physics constants, same springs, same easing curves, same durations, same state machine, same gestures, same thresholds. Do not simplify, do not "improve", do not substitute Material components, do not restyle, do not swap an animation for a "similar" one, do not skip an animation because it is hard.
2. Unit mapping: Tailwind px → dp, font px → sp. 1:1. Keep the app's existing typeface, but enable tabular numerals (`fontFeatureSettings = "tnum"`) wherever the source uses `tabular-nums`.
3. The only things you may change are: **strings/labels** (adapted to Kanri's context, listed per component), **currency** (₹), and **data bindings** (Kanri's real data instead of demo data). Everything else is frozen.
4. Theme mapping: Light → the Tailwind light classes. Dark, AMOLED, Glass Dark → the `dark:` classes (AMOLED: use pure black `#000000` where the source uses zinc-950 as a page/card background; Glass themes: surfaces follow the existing glass system, but sizes, radii, borders, motion are unchanged). Kanri's UI is monochrome: the **only** non-monochrome colors allowed are the ones the source components themselves use (ring segment colors, emerald "done" states, red danger, rose/emerald change indicators, orange avatar chip in Amount Pad).
5. Honor reduced motion (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` → treat as reduced motion) exactly where the source branches on `useReducedMotion` / `prefers-reduced-motion`.
6. Where the source uses Framer `type:"spring", duration, bounce:0`: Compose `spring(dampingRatio = 1f, stiffness = (2π/duration)²)`. Where it uses `stiffness, damping, mass`: Compose `stiffness = k/m`, `dampingRatio = c / (2·√(k·m))`. Values given below for `duration/bounce/visualDuration` springs are **provisional starting points only** (`duration`-based: `stiffness≈(2π/d)²`; `visualDuration`-based: `stiffness=(2π/(1.2·vd))²`, `dampingRatio=1−bounce`). **Verify every one against the real motion library:** in Node, import `spring` from `motion`, sample each original transition (e.g. `{type:"spring",duration:0.42,bounce:0}`) at 60 Hz, and write a unit test that the Compose `SpringSpec` curve you chose matches the sampled curve within 1% of the travel at every frame. Adjust stiffness/damping until it does. Stiffness/damping-based springs (e.g. 520/42) map exactly with the formula above. Where the source runs its **own integrator** (Spending Ring, Tear Button), port the integrator itself — do not replace it with Compose springs.
7. Easings: `cubic-bezier(0.23,1,0.32,1)` = `CubicBezierEasing(0.23f,1f,0.32f,1f)`; `(0.32,0.72,0,1)`; `(0.65,0,0.35,1)`; "ease-out" = `CubicBezierEasing(0f,0f,0.58f,1f)`.
8. Tailwind zinc palette (use these exact hex values): 50 `#FAFAFA`, 100 `#F4F4F5`, 200 `#E4E4E7`, 300 `#D4D4D8`, 400 `#A1A1AA`, 500 `#71717A`, 600 `#52525B`, 700 `#3F3F46`, 800 `#27272A`, 900 `#18181B`, 950 `#09090B`. Others: emerald-600 `#059669`, 500 `#10B981`, 400 `#34D399`, 950 `#022C22`; red-600 `#DC2626`, 400 `#F87171`; rose-600 `#E11D48`, 400 `#FB7185`; orange-100 `#FFEDD5`, orange-700 `#C2410C`, orange-300 `#FDBA74`.
9. Write unit tests for every pure function listed per component (they are listed in the specs). Port the functions character-for-character in logic.

---

## 1. BUGS TO FIX FROM THE PREVIOUS PASS

### Bug 1 — Wrong chart replaced, wrong tap behavior
- The Spending Ring was given **to replace the existing "Category Pie Chart" in Insights — and nothing else.** The previous pass wrongly replaced/changed the **full ring chart**. **Restore the full ring chart to exactly how it was before the previous pass** (use git history), and do not touch it again.
- Replace only the Category Pie Chart (Insights) with the Spending Ring (Section 4.1), bound to Kanri's real category totals for the selected period.
- **Tapping, dragging, flicking or keyboard-navigating the ring must NEVER open "Category Distribution".** Tapping a slice only selects it (the ring turns it under the top marker) — that is the component's own behavior. The middle of the ring is a non-interactive readout. Legend rows also only select.
- **"Category Distribution" opens ONLY when the user taps the existing "Category" pill/chip.** Find that pill, keep it as the sole trigger, and remove every other navigation trigger on the chart (click handlers on the ring, on the card, on the legend, on the center readout). After the fix, search the codebase for every place that navigates to Category Distribution and confirm the pill is the only one.

### Bug 2 — Entry popups open from the center
Every entry popup (Amount Pad, details, summary/confirm) must be a **bottom sheet that rises from the bottom edge of the screen**, anchored flush to the bottom, never a centered dialog. This applies to Expense, Income, Lend and Borrow, add and edit. No centered `Dialog`/`AlertDialog` anywhere in these flows.

### Bug 3 — Components are not faithful
The previous pass produced look-alikes. Re-port every component from Section 4 exactly. A reviewer will compare each constant in the tables below against your code.

---

## 2. NEW FEATURES TO ADD

1. **Slide to Confirm** (Section 4.4) as the final confirm action for saving every transaction entry: Expense, Income, Lend, Borrow (add manually). It lives inside the summary drawer (Section 4.3).
2. **Tear Button animation** (Section 4.5) on Lend/Borrow entries: the user tears the stub off the end of an **unsettled** entry to mark it **settled**.

---

## 3. ENTRY FLOW (applies to Expense, Income, Lend, Borrow — when the user adds an entry manually)

All steps live in ONE bottom sheet that rises from the bottom, with a scrim. Steps:

1. **Amount Pad** (Section 4.2) — user types the amount. Button = "Continue ₹X" (see 4.2 for label changes). Tapping it pushes step 2.
2. **Details page** — all the existing fields for that entry type (type, category, payee/person, account Cash/Online, date & time, note, photo, expected return date for Lend/Borrow, etc. — **keep every existing field and validation**). The amount is shown at the top as a chip; tapping it goes back to step 1 with the typed amount preserved. Step transition between pad ↔ details uses the **same push/pop transition as the Stack Panel** (Section 4.6: spring duration 0.42 bounce 0, incoming page from the right, outgoing page parallax at 30%, shade 0.06→0; back = reverse). Primary button "Review".
3. **Summary Drawer** (Section 4.3) — peeks up from the bottom with the title, entry type/description line, rolling total, the **Slide to Confirm** (Section 4.4) in the place of the "Pay" button, and the "Details" toggle that expands to the full breakdown of every field the user entered.
4. On slide completion: write the entry to Room (same code path as the existing save), show the "Saved" done state for ~700 ms, then close the sheet. If saving throws, the thumb springs back to the start (the source's rejected-promise behavior) and an error is shown.

Editing an existing entry reuses the same flow, pre-filled.

---

## 4. COMPONENT SPECS (port exactly)

### 4.1 Spending Ring (Insights → replaces Category Pie Chart)

Constants: `SIZE=240, C=120, R=90, STROKE=22 (selected: 30), GAP=2.4°, RATE=11, DAMPING=0.78, PROJECT=0.2s, TAP_SLOP=5px`. Ring max width 240dp, square, centered.

- Geometry: 0° at top, angles clockwise. `polar(deg,r)=(C+r·sin, C−r·cos)`. Each category slice spans `amount/total·360°` (equal split if total=0). Arc drawn from `from+GAP/2` to `to−GAP/2`, stroke only, no cap rounding (butt), `large-arc` flag if sweep>180.
- The whole ring group rotates by `a` degrees about the center; resting `a = −mid(selected slice)` so the selected slice sits under the marker at the top.
- Marker: filled triangle `(C−5,2) (C+5,2) (C,8)`, zinc-900 (dark: zinc-100).
- Non-selected slices at opacity 0.4 (dark 0.5). Selected stroke width 30, animated 200 ms `cubic-bezier(0.23,1,0.32,1)` for stroke-width and opacity.
- Segment colors (light / dark): Housing sky-500 `#0EA5E9` / sky-400 `#38BDF8`; Groceries emerald-500 `#10B981` / 400 `#34D399`; Shopping rose-500 `#F43F5E` / 400 `#FB7185`; Dining out amber-400 `#FBBF24` / 300 `#FCD34D`; Transport violet-500 `#8B5CF6` / 400 `#A78BFA`; Subscriptions zinc-400 `#A1A1AA` / zinc-500 `#71717A`. For Kanri's real categories, assign colors by cycling this exact palette in order (the legend dot uses the same color).
- Physics (custom integrator, port exactly): `springStep(state{a,v}, target, dt)`: `slices=ceil(dt/(1/240)); h=dt/slices; repeat: accel=−RATE²·(a−target)−2·DAMPING·RATE·v; v+=accel·h; a+=v·h;` returns settled when `|a−target|<0.05 && |v|<2`. Per frame `dt=min((now−last)/1000, 1/30)` (first frame 1/60). On settle snap `a=target, v=0`.
- `wrap(deg) = ((((deg+180)%360)+360)%360)−180`. `sliceAt(spans,deg)`: `a=((deg%360)+360)%360`, first span with `from<=a<to`, else 0.
- `turnTo(index, rest=a, coast=false)`: `target = rest + wrap(goal − rest)` where `goal=−mid(index)`; short way round. If not coasting, set selection immediately. Reduced motion → jump to target.
- Drag: pointer angle = `atan2(x, −y)` in degrees relative to ring center. On press: stop animation, `v=0`, record angle/time/position. On move: track max moved distance; below TAP_SLOP do nothing; else `delta=wrap(angle−lastAngle)`, `dt=max((t−lastT)/1000, 1/240)`, `a+=delta`, `v=v·0.6+(delta/dt)·0.4`, update lastAngle/lastT, set dragging (cursor grab → n/a on touch), repaint, and **selection tracks the slice under the marker live** (`sliceAt(spans, −a)`).
- Release: if moved < TAP_SLOP → it's a tap: only if touch radius > `((R−STROKE)/SIZE)·width` (i.e. on the ring band, not the middle) call `turnTo(sliceAt(spans, pointerAngle − a))`; middle tap does nothing. Else (flick): if `now − lastMoveTime > 80ms` then `v=0`; `v=clamp(v,−1440,1440)`; `rest=a+v·PROJECT`; `turnTo(sliceAt(spans,−rest), rest, coast=true)`. While coasting the readout follows whichever slice passes under the marker each frame; at settle, coasting ends.
- Data change: keep the same selected index under the marker; if the spans are identical (same numbers) do not disturb an in-progress spin/drag.
- Accessibility: slider semantics ("Spending by category. Use the arrow keys to turn the ring."), value = index+1 of N, text = "Name, amount"; Left/Up = previous, Right/Down = next, Home/End, wrapping; provide custom accessibility actions for next/previous.
- Center readout (non-interactive, centered): row `[6dp dot in the slice color] [name 12sp zinc-500/400]`; amount `24sp semibold tabular tracking −0.025em` with 2 decimals (`en-US` currency formatting with ₹); `"{share}% of total"` 12sp zinc-500/400 (share rounded to 0 decimals); change line 12sp: `change>0` rose-600 (dark rose-400) `"▲ x.x% vs last month"`, `change<0` emerald-600 (dark emerald-400) `"▼ x.x% vs last month"`, `0` zinc-400 (dark zinc-500) `"Same as last month"`. Margins 2dp between lines. On selection change the readout block re-enters: translateY 4dp→0 and opacity 0→1, 200 ms ease-out. Compute "change vs last month" from Kanri's real previous-month totals (omit the line if there is no previous data).
- Card: full width, rounded 24dp, 1dp border zinc-200 (dark zinc-800), bg white (dark zinc-950), padding 20. Header row (baseline aligned, space-between, gap 16): title 14sp zinc-500/400 (e.g. "October spending" → the selected period label) and total `14sp medium tabular`. Ring `mt 12`. Legend `mt 16`: 2-column grid, gap 4; each row `px 8, py 6`, rounded 8, 12sp, gap 8: `[8dp dot][name flex 1 ellipsize][amount rounded to whole units tabular]`; selected row bg zinc-100 (dark zinc-900), others zinc-500/400 text with hover/press bg zinc-50 (dark zinc-900 @60%). 150 ms color transition. Tapping a legend row = `turnTo(i)`.
- Pure functions to unit-test: `wrap`, `polar`, `arc`, `sliceAt`, `springStep`.

### 4.2 Amount Pad (step 1 of the entry flow, as the sheet content)

Card style kept: max width 320dp, rounded 24dp, border 1dp zinc-200/800, bg white/zinc-950, padding 20. Inside the bottom sheet it is the sheet's surface (sheet top corners rounded 24).

- Header: 36dp circle chip (bg orange-100 / dark orange-500 @15%, text orange-700 / dark orange-300, 12sp medium) with initials; beside it a two-line block: `12sp zinc-500` label, `14sp medium` name (+ optional handle in zinc-400 / zinc-500, ml 6). Kanri strings: label = "Expense" / "Income" / "Lending to" / "Borrowing from"; name = the person/payee if already known, else the entry type name; initials accordingly.
- Amount area: `mt 24`, height 80dp, centered. Font **56sp semibold**, tabular, tracking −0.025em, line-height 1, baseline aligned, no wrap. Currency sign `₹` with `mr 2dp`, color zinc-300 (dark zinc-700) while empty, zinc-400 (dark zinc-500) otherwise, 200 ms color transition. Empty state shows a faint "0" (zinc-300/700), fade-in 150 ms, exit 80 ms.
- Typing logic (port `typeKey`, `layout`, `toNumber`, `money` exactly): max 7 whole digits, max 2 decimals, a lone leading "0" is replaced by the next digit, "." on empty gives "0.", second "." refused, commas every 3 whole digits (en-US grouping, as in the source), faint ghost zeros fill the missing cents after the point (up to 2), ghost color zinc-300/700, separators zinc-400/500. Each character has a stable id so commas animate as digits are added.
- Glyph animation per character: enter `opacity 0→1, y −22px→0, blur 2→0` with spring (Framer k=520, c=30, m=0.8 → Compose **stiffness 650, dampingRatio 0.7355**), blur over 160 ms; exit `opacity→0, y→+18px, blur→2` 140 ms ease-out (0.23,1,0.32,1); neighbors reflow with layout spring (k=500,c=40 → **stiffness 500, dampingRatio 0.894**). (Blur: `Modifier.blur` on API 31+, alpha-only fallback below.)
- Fit-to-width: the amount scales down (never up) to stay on one line: `scale=min(1, frameWidth/contentWidth)`, spring k=380,c=34 → **stiffness 380, dampingRatio 0.872**, re-measured on size change.
- Refusal shake (cap exceeded / invalid key / empty confirm): x keyframes `0,−10,9,−6,4,0` over 360 ms ease-out.
- Cap: if the new value > max, refuse and show the cap line in red-600 (dark red-400) for 1800 ms: "Up to ₹{max} per entry". Otherwise the line under the amount shows the subtitle (Kanri: e.g. "Cash balance ₹x" or "Online balance ₹x" if relevant, else the entry type). The two lines swap with `y 6→0`/`−6`, opacity, 180 ms ease-out. Line height 20dp, 12sp, centered.
- Presets: row centered, gap 8, `mt 16`; each pill height 32dp, px 12, fully rounded, 1dp border zinc-200/800, 12sp medium tabular, text zinc-700 (dark zinc-300); tapping sets the amount to the preset (clamped to max). Presets for Kanri: 100, 500, 1000 (₹).
- Keypad: `mt 16`, 3-column grid gap 4: `1 2 3 4 5 6 7 8 9 . 0 ⌫`. Each key: height 56dp, rounded 16, 24sp medium tabular, text zinc-900/100; pressed bg zinc-100 (dark zinc-800) instantly (0 ms), hover/idle transition 100 ms ease-out; backspace icon 24dp, stroke 1.75, zinc-500. Hardware keyboard: digits, "." and "," (as "."), Backspace.
- Action button: `mt 12`, height 48, rounded 16, 14sp medium, centered `[ArrowUp icon 16dp][label]`. Empty: bg zinc-100 / dark zinc-900, text zinc-400 / dark zinc-600, label "Enter an amount". Filled: bg zinc-900 / dark zinc-100, text white / dark zinc-900, label "Continue ₹X". Label swap animation `y 10→0` in, `−10` out, opacity, 200 ms ease-out. Pressing while empty = refusal shake. (The source's "Sent" success phase is not used because the button advances to step 2.)
- Pure functions to unit-test: `typeKey`, `layout`, `toNumber`, `money`.

### 4.3 Smooth Drawer (step 3: the summary, bottom sheet with two snap points)

- Sheet: centered, max width 420dp, rounded top corners 16dp (rounded-2xl), 1dp border zinc-200/800 (no bottom border), bg white/zinc-950, sits flush on the bottom edge. Height `sheetHeight = max(200, min(680, viewportHeight − 24))`. Snap points: **peek** (visible height 200dp) and **full**. Scrim fades in from the first snap point (fadeFromIndex 0). Drag handle: row height 24dp with a 36×4dp pill (zinc-300 / dark zinc-700). Swipe down past peek dismisses. Opens at peek.
- Peek content (px 20, pt 4): row space-between: left = title `15sp semibold` (tracking tight) + description `13sp zinc-500/400` (mt 2); right (end aligned) = "Total" `13sp zinc-500/400` and the **Rolling amount** `18sp semibold`, line-height 24, mt 2. Kanri strings: title = "Review expense" / "Review income" / "Review loan" / "Review borrowing"; description = person/category + date; Total = the amount.
- **Rolling amount**: per character; digits animate with direction = +1 if value grew else −1: enter `y = dir·70%`, opacity 0→1; exit `y = −dir·70%`, opacity→0; 240 ms ease-out (0.23,1,0.32,1); non-digit characters are static; slots are keyed from the right so cents stay put when whole digits change; clipped overflow. Reduced motion → opacity-only fade.
- **Primary action (mt 16)**: the **Slide to Confirm** (Section 4.4), full width, in the position of the source's "Pay" button. Label: "Slide to save expense" / "Slide to save income" / "Slide to lend ₹X" / "Slide to record borrowing".
- **Details toggle (mt 4)**: full width, height 40, rounded 8, 13sp zinc-500/400 (hover zinc-900/100). Text "Details" / "Hide details" + ChevronUp 14dp that rotates 180° when expanded, 300 ms `cubic-bezier(0.32,0.72,0,1)`.
- **Details area** (fills the rest, px 20, pb 24): top border zinc-200/800, pt 16. Opacity 0 + translateY 12dp + not interactive when collapsed; opacity 1 + translateY 0 + scrollable when expanded; 300 ms `cubic-bezier(0.32,0.72,0,1)`. **While the sheet is being dragged between snap points the details' opacity and translateY track the drag progress directly** (`progress=clamp((peekTop−top)/(peekTop−fullTop))`, opacity=progress, translateY=round((1−progress)·12)dp, no transition during the drag; on release/cancel hand back to the animated state).
- Details content, same structure as the source, with Kanri data: (a) a list of rows (40dp rounded-8 bordered square with the first letter, name `14sp medium`, sub-line `13sp`, value `14sp tabular` on the right) — one row per entered field group (Category, Person/Payee, Account, Date & time, Note, Expected return date); (b) in place of the Delivery radio cards, none (omit); (c) a `dl` block 14sp tabular with rows: Amount, plus any fee/adjustments if they exist, then a top-bordered "Total" row `medium` zinc-900/100; (d) a bordered rounded-12 row (px 12, py 10) with an icon, "Cash" or "Online" account text 14sp, and "Default"-style right label 13sp zinc-500 (e.g. the balance after this entry).
- Tapping the details toggle or dragging between snaps stays in sync (single source of truth for the active snap point).
- Pure function to unit-test: the progress/clamp math and the rolling-slot keying.

### 4.4 Slide to Confirm (inside the summary drawer)

Constants: `HEIGHT=56, INSET=4, THUMB=48 (HEIGHT−2·INSET), COMMIT_AT=0.9, FLICK_SPEED=900 px/s, FLICK_FROM=0.45, OVERDRAG=8, TAP_SLOP=4`. Max width 384dp, full width in the sheet.

- Track: height 56, fully rounded, 1dp border zinc-200/800, bg zinc-100 / dark zinc-900, clipped. Disabled → 50% opacity.
- Empty-track label: centered, `14sp medium`, zinc-500/400, `pl 24dp` nudge, opacity `max(0, 1 − progress·0.55)` where `progress = x/(trackWidth−THUMB−2·INSET)`.
- Ink layer: bg zinc-900 / dark zinc-100 (turns emerald-600 / dark emerald-500 when done, 300 ms ease-out), clipped by `inset(0, 0, 0, right=(1−cover)·max(0, width−x−INSET−THUMB/2), fully rounded)`; its opacity `min(1, x/20)`. It contains the same label in white (dark zinc-900; done: white / dark emerald-950), centered, 14sp medium, gap 8, `pl 24dp` while idle/dragging and `pr 24dp` while busy/done. The label cross-fades between stages (slide/busy/done) with `y 8→0`, blur 2→0 in; `y 0→−8`, blur out; 260 ms ease-out. Stage labels: slide = label; busy = "Saving…"; done = "Saved".
- Thumb: 48dp circle, left offset `INSET−1`, top `INSET−1`, bg white / dark zinc-800, shadow `0 1px 2px rgba(0,0,0,.12)` + 1dp ring rgba(0,0,0,.06) (dark: `0 1px 2px rgba(0,0,0,.5)` + ring white @8%). Contains a 20dp icon stack: ArrowRight (when armed and not busy/done, translateX +2dp; when busy/done translateX +6dp, opacity 0, blur 2, 200 ms ease-out), Check (emerald-600 / dark emerald-400, stroke 2.5, appears with scale .5→1, opacity, blur, 200 ms), and a 24dp spinner (rotates 700 ms linear infinite, appears with opacity 200 ms and 100 ms delay when busy; track circle r=9 stroke 2.5 zinc-200/700; arc dash `14 43`, round caps, current color).
- Phases `idle → dragging → confirming → done`. `max = trackWidth − THUMB − 2·INSET`.
- Drag: only when idle and primary pointer; stop animations, remember pointer x and thumb x. Move: `raw = from + delta`; `x = band(raw, max)` with `band(raw,max)=raw>max ? max+OVERDRAG·(1−e^(−(raw−max)/40)) : raw<0 ? −OVERDRAG·(1−e^(raw/40)) : raw`. Armed when `raw ≥ max·0.9`. Movement > 4px counts as moved.
- Release: if not moved → back to idle. Else `velocity=x velocity`, `progress=x/max`; commit if `progress ≥ 0.9` or `(velocity>900 && progress>0.45)`; otherwise settle back: cover→0 over 300 ms ease-out, x→0 with spring (k=520,c=42 → **stiffness 520, dampingRatio 0.921**, initial velocity = release velocity).
- Tap (no drag): nudge thumb to 28dp over 180 ms ease-out, then spring back (skip in reduced motion). Accessibility "click" (TalkBack double-tap / keyboard Enter) commits immediately with the **glide** (thumb to end over 420 ms `cubic-bezier(0.65,0,0.35,1)`).
- Commit: phase=confirming, armed. In parallel: thumb to `max` (spring with release velocity, or the 420 ms glide), cover 0→1 over 360 ms ease-out, and the real `onConfirm` (save to Room) — wait for `all of {onConfirm, 800 ms minimum, thumb landed, cover flooded}`. If `onConfirm` throws → settle back. On success phase=done (emerald). In Kanri `resetAfter = null` (stay done; the sheet closes ~700 ms later). Keep the rewind animation (600 ms ease-in-out, cover over 0.6× that) implemented for completeness.
- A re-entrancy token (`runRef`) cancels stale completions. Live region announces "Saving…" / "Saved".
- Pure function to unit-test: `band`, commit decision.

### 4.5 Tear Button (Lend/Borrow entries → mark as settled)

Constants (port all): `HEIGHT=68, STUB_WIDTH=76, TEAR_ANGLE=34°, HOLES=7, HOLD=0.55, HOLD_SHARE=0.3, SWING_STIFFNESS=900, SWING_DAMPING=42, RETURN_STIFFNESS=520, RETURN_DAMPING=22, GRAVITY=2600, FALL_DISTANCE=220, STEP=1/120, NOTCH=7`, `FIBRES=[1.6,0.4,2.2,0.9,1.8,0.2,2.4,1.1,0.5,2,1.4,0.3,2.3,0.8,1.7,0.1,2.1,1.2,0.6,1.9]`.

- **Placement**: every **unsettled** Lend/Borrow entry in the ledger is rendered as this ticket. `BODY_WIDTH = entryWidth − 76` (the body flexes; the stub stays 76 wide; total height 68). Body contents replace the demo text: round 36dp chip (icon = handshake/arrow icon; becomes a check when done), title = person name (14sp medium), sub-line = amount · date · expected return (12sp, muted per tone). Stub shows the tear icon (16dp) and label "Tear". Settled entries render as the normal non-ticket card.
- **Tone**: use `default` for lent, `muted` for borrowed (tones table from the source: paper/chip/chipDone/sub/stub/perforation classes — port the exact colors).
- Shape: body has rounded left corners (16dp), stub rounded right corners (16dp). Notches: radial cutouts of radius 7 (edge 7.5) at the body's top-right and bottom-right corners and the stub's top-left and bottom-left corners (implement with an offscreen layer + `BlendMode.Clear`). Perforation: 2dp dotted vertical line on the stub's left edge, inset 10dp top/bottom.
- Tear geometry: `tip = clamp(angle/34,0,1)·68`; when `tip>0.5` body and stub are clipped by the ragged polygon from `tornEdge(tip, width, side)` (port exactly using FIBRES; side +1 for body right edge, −1 for stub left edge). Stub rotates by `angle` degrees about its **bottom-left corner** (`transformOrigin = 0, 100%`), hinge point at (bodyRight, bottom).
- Gesture: pointer-down on the stub (primary pointer only, not when flying/torn): capture, `grabAngle = atan2(y−hingeY, x−hingeX) in deg − angle`, `held=true`, reset hand-velocity samples, phase=tearing. Move: hand velocity `pv = pv·0.5 + (Δpos/Δt)·500`; `raw = atan2(...) deg − grabAngle`; `pull = raw<0 ? raw·0.15 : raw`; `target = pull ≥ 34 ? 34·1.05 : stickSlip(max(pull,0)/34)·34 + min(pull,0)`. Release before tearing through: `held=false, target=0` (swings back on the RETURN spring; the perforation holds).
- `stickSlip(pull)`: `scaled=clamp(pull,0,1)·7; hole=min(floor(scaled),6); f=scaled−hole; given = f<0.55 ? (f/0.55)·0.3 : 0.3 + ((f−0.55)/(0.45))·0.7; return (hole+given)/7`.
- Integrator at fixed `STEP=1/120` with carry (`carry += min(dt, 0.05)`; run while `carry ≥ STEP`): attached: `stiffness,damping = (held||auto) ? (900,42) : (520,22)`; `force=(target−angle)·stiffness − speed·damping; speed+=force·STEP; angle+=speed·STEP`; finished when `angle ≥ 34·0.995`. Haptic: 3 ms tick each time a new hole index (`floor(clamp(angle/34,0,1)·7)`) is exceeded.
- Detach (when finished while held or auto): `held=false`, haptic 14 ms; `rad=angle·π/180; tangential=(speed·π/180)·68; vx=cos(rad)·tangential + clamp(pvx,−900,900)·0.6 + 90; vy=sin(rad)·tangential + clamp(pvy,−900,900)·0.6 − 120; spin=speed·0.6+140`; start flight; call `onConfirm` (mark settled in Room, with the existing undo/trash snackbar so it can be undone).
- Flight (per step): `vy+=2600·STEP; x+=vx·STEP; y+=vy·STEP; angle+=spin·STEP`; finished when `y>220`; draw stub with translate(x,y)+rotate(angle) and opacity `clamp(1 − (y−110)/110, 0, 1)`.
- After the stub is gone the body cross-fades its text to the done copy ("Settled" / "Marked as settled" sub-line) with `translateY 4px, blur 3dp, opacity`, 300 ms `cubic-bezier(0.23,1,0.32,1)`, chip → check (chipDone colors), **no auto-reset** (`resetAfter = 0`); then the entry transitions into the settled list.
- Stub entrance: from `translateX −12dp, opacity 0` over 300 ms same curve.
- Accessibility: stub is a button "Mark as settled: Tear off the stub to confirm"; activation by keyboard/TalkBack double-tap runs the **auto tear** (`auto=1`: `target=min(target+STEP·60, 34·1.05)` per step on the fast spring) — same animation hole by hole. Reduced motion: stub doesn't fall; it fades out in 180 ms where it tore, then done. Focus returns to the ticket when the stub is removed.
- Pure functions to unit-test: `stickSlip`, `tornEdge` (point list), stepping math.

### 4.6 Stack Panel (Settings — complete redesign)

Port the component and its page model exactly: `StackRow {id,label,detail?,to?,trailing?,onSelect?,selected?,checked?,tone}`, `StackSection {title?,rows,footer?}`, `StackPage {title,sections}`, `StackPanel(pages, root)`.

- Panel: width = screen width minus 16dp margins (the component's `max-w-sm`=384dp cap on tablets), rounded 16, 1dp border zinc-200/800, bg white/zinc-950, clipped. Header: height 48, px 8, bottom border zinc-100 / dark zinc-900, z above the body. Panel height = height of the top page content, animated with spring duration 0.38 → **stiffness ≈ 273, dampingRatio 1**. If the page is taller than the available screen height, the page scrolls inside the panel (the header stays fixed).
- Springs: push = duration 0.42 → **stiffness ≈ 223.8, ζ=1**; pop = duration 0.34 → **stiffness ≈ 341.5, ζ=1**.
- Push: the tapped row's label rect is captured (left/top/height/fontSize relative to the panel). The new page starts at `x = width` and animates to 0. The underlying page is shown beneath with `x = −0.3·(width − x)` (parallax 0.3) and a black shade overlay with opacity `lerp(0.06→0, x: 0→width)`; the top page casts a left shadow `−8px 0 24px −12px rgba(0,0,0,.18)` when stacked. The header title **flies from the tapped row's label** to the header center: scale = `rowFontSize / titleFontSize`, transform-origin left-center, spring = push; if there's no origin (pop/first) it slides `±24dp` + fades in over 280 ms ease-out (0 ms in reduced motion). Exiting title: opacity→0 and `x = direction·(−40)` over 180 ms ease-out. Back button (visible when stack>1): chevron-left 16dp + parent title, 14sp, zinc-600/400, max width 30%, px (1,8) rounded 8, fades 180 ms.
- Swipe back: the top page (when stack>1 and not busy) is draggable horizontally, right only (elastic factor 1 to the right, none to the left), direction-locked, no momentum; on release, pop if `x > 0.35·width` or `velocity.x > 500 px/s`, else spring back to 0. System Back / predictive back = pop when stack>1 (this replaces Escape). On pop, focus lands on the row that opened the page. Block interactions while `busy`.
- Page layout: column, gap 20dp, `px 12, pt 8, pb 16`. Section title `12sp medium zinc-500/400`, `mb 6, px 12`. Section card: rounded 12, bg zinc-50 / dark zinc-900, 1dp dividers zinc-100 / dark zinc-800 @70%. Footer: `12sp zinc-400 / dark zinc-500`, line-height 20, `mt 6, px 12`.
- Row: min height 44, px 12, py 8, gap 8; label `15sp`, zinc-900/100 (danger: red-600 / dark red-400), single line ellipsized, flex 1; detail `14sp zinc-500/400`, max 45% width, ellipsized; trailing slot; switch (`checked != null`) = **Toggle** 36×22dp fully rounded, off bg zinc-200 / dark zinc-700, on bg zinc-900 / dark zinc-100, 200 ms color; thumb 18dp at offset 2dp, white (dark zinc-950), shadow-sm, translateX 14dp when on, 200 ms `cubic-bezier(0.23,1,0.32,1)`; `selected != null` = 16dp check (invisible if not selected); `to != null` = chevron-right 16dp zinc-400/500 with −4dp end margin. Pressed/focused row bg zinc-100 / dark zinc-800, hover 60%. Switch rows expose `role=switch`, others button semantics.
- Pure behavior to unit-test: stack push/pop and focus bookkeeping (`openers`).

**Redesign the whole Settings screen using this Stack Panel.** The current settings screen is cluttered. First inventory **every existing setting and its behavior** (do not drop or break any). Then regroup into this page tree (adjust names only if an existing setting doesn't fit; every setting must still exist and work):
- Root "Settings": section 1 → Profile (detail = name; opens Profile page), Appearance (detail = current theme), Privacy & Security; section 2 → Budget & Wallet, Notifications, SMS & Detection; section 3 → Data & Backup, About.
- Profile: Name, avatar (opens avatar picker, Section 6).
- Appearance: "Theme" section (Light / Dark / AMOLED / Glass Dark / Glass Light as `selected` rows); any existing display options as switch/select rows.
- Privacy & Security: App lock / biometric (switch), **Hide wallet balance (switch — see Section 5)**, any other lock settings.
- Budget & Wallet: monthly budget, cash balance, online balance, currency, categories management.
- Notifications, SMS & Detection (supported UPI apps, parsing toggles), Data & Backup (backup/restore, export Excel/PDF, trash), About (version, repo).
- Destructive actions use `tone = danger`. Footnotes use `footer`.

---

## 5. HIDE / SHOW WALLET BALANCE (Spoiler)

Port the Spoiler component exactly and add the setting **Settings → Privacy & Security → "Hide wallet balance"** (switch row, persisted in the existing settings store, default off).

- When ON: every place that displays the wallet balance (cash balance, online balance, total/wallet balance on Home and anywhere else it appears — search the whole codebase) is wrapped in the Spoiler. When OFF: shown normally, no spoiler.
- Spoiler constants: `DENSITY=1/6` specks per px², `MAX_SPECKS=2400` (scale density down if the area is larger), `DRIFT=7 px/s`, `WAVE_MS=520`, `FADE_MS=360`, `THROW=70`. Speck: random position inside its line box, velocity of magnitude `7·(0.3+rand·0.7)` in a random direction, `phase=rand`, `rate=0.35+rand·0.9`, size `1` (or `1.5` with 18% probability), bounces inside its own line's rect until hit. Color = the text color.
- Frame step: if a wave exists and the speck isn't hit and `distance(speck, origin) ≤ radius`, mark hit at `now`, push away from origin by `70·(0.5+rand)`. `fade = hit ? 1−(now−hit)/360 : 1`; alpha 0 when `fade≤0`. Move `x+=vx·dt; y+=vy·dt; phase=(phase+rate·dt)%1` (in reduced motion only hit specks move). `twinkle=0.35+0.65·|sin(phase·π)|; alpha = twinkle·fade·0.9`. `dt=min(0.05,…)`.
- Wave radius = `farthestCorner(origin,w,h)·easeOut(min(1,(now−start)/520))` with `easeOut(t)=1−(1−t)³`. When all specks are dead, drop the canvas.
- Hidden state: text `opacity 0, blur 6dp, not selectable`; chip bg zinc-900 @6% (dark white @8%), pressed 10% / 12%, corner radius 3dp; accessibility label "Hidden text. Activate to reveal." Revealed: text opacity 1, blur 0 over 450 ms `cubic-bezier(0.23,1,0.32,1)`. Reveal starts at the touch point (keyboard/TalkBack: start of the first line + 4dp). Specks stay drawn while the wave clears them.
- Pause the shimmer when off-screen. Reveal is per session: re-hide whenever the app goes to the background or the setting is turned off then on. Never persist "revealed".
- Pure functions to unit-test: `farthestCorner`, speck step, wave radius.

---

## 6. AVATAR PICKER (first-time setup + Edit Profile) — port exactly, plus a custom photo option

**Replace the existing avatar selector entirely** with a faithful port of the Avatar Picker (Appendix A8). Used in **first-time setup** (title "Pick your avatar", submit "Continue") and **Edit Profile** (inside a bottom sheet, submit "Save"). Prefill the display name from the existing profile name; migrate any previously stored avatar to `variant 0` of that name. Persist only `displayName` and `variant` (the seed `"${displayName.trim().lowercase()}:${variant}"`) and regenerate the face from the seed wherever an avatar appears (render the static face at any size with the same drawing code, no animation).

**Face generation — must be bit-for-bit deterministic with the original** (same seed → same face):
- Port `hashString` (FNV-1a: init `2166136261`, `h ^= char`, `h = imul(h, 16777619)`, result unsigned 32-bit) and `mulberry32` with Kotlin `Int` wraparound arithmetic and `ushr`; final value `(t xor (t ushr 14)).toUInt().toLong() / 4294967296.0`.
- `createFace` must call the PRNG **in exactly the original order**: background pick, then the shape fields in object-literal order (`headW, headH, headSquare, chin, hairVolume, hairFringe, hairPart, hairLength (r()**1.6), eyeGap, eyeY, eyeRx, eyeRy, browTilt, browLift, browWeight (draws a 2nd value only when the first r()<0.65), mouthW, mouthCurve, mouthOpen (2nd value only when r()<0.3), nose, glasses (r()<0.25), cheeks, shoulders`), then skin, hair, shirt picks. Round every shape value to 2 decimals. Same palettes (`BACKGROUNDS, SKINS, HAIRS, SHIRTS`), `INK=#18181B`, same `describe()` strings.
- Port `lerp`, `lerpRgb`, `lerpFace` (shapes may overshoot with the spring, colors use `min(1,t)`), `blobPath`, `hairFrontPath`, `mouthPath`, and the `FaceArt` drawing in the **same z-order** inside a `76×76` viewBox starting at `(12,14)` (scale the Compose `Canvas` accordingly): bg rect, long hair blob (×0.9 brightness), shirt/shoulders path, neck rect (skin ×0.9), ears (skin ×0.95), head blob, front hair path, cheeks (INK @ `0.045·cheeks`), brows (stroke width `max(0,browWeight)`, round caps, 0.85 alpha), eyes group (offset by the look values, blink group scaled in Y), nose (INK @0.35, 1.2 stroke), mouth (INK fill+stroke 1.6, round joins), glasses (opacity=`clamp(glasses)`, stroke 1.2).
- **Golden-vector tests (mandatory):** run the original TypeScript in Node (e.g. `npx tsx`) to dump `createFace(seed)` JSON and the path strings for at least 30 seeds (include `"robin vale:0"`, `":0"`, unicode names, negative variants, large variants), commit it as test fixtures, and assert the Kotlin output matches (exact for shape/rgb/description; numeric tolerance 0.01 for path coordinates).

**Behavior and visuals (all constants from the source):**
- Card: max width 360dp, rounded 16, 1dp border zinc-200/800, bg white/zinc-950, padding 20. Title 16sp semibold tracking-tight; description 14sp zinc-500/400 ("Made from your name. Spin it or pick one."), 4dp apart.
- Stage (mt 20, centered): bezel box **176dp** with 60 tick marks (every 5th is long: r1=`176/2−3`, r0=`r1−7` long / `r1−4` short, stroke 1.5 / 1, alpha 1 / 0.6, round caps, color zinc-300 / dark zinc-700); the bezel rotates `angle/15` degrees as the coin spins. A fixed index mark at the top center (2dp wide, 12dp tall, rounded, zinc-900 / dark zinc-100). Coin **128dp** centered.
- Under the stage (mt 8): `[display name 12sp medium, ellipsized, max 180dp, "Anonymous" if empty]` and `["No. {pad3(variant)}" monospace 12sp tabular zinc-400/500]`, gap 8, baseline aligned. `pad3(v) = ((v%1000)+1000)%1000` zero-padded to 3.
- **Coin is a real 3D coin**: angle `a` in degrees, one face per 180°. Visible tilt = `a − 180·round(a/180)` (always −90..90). Draw 6 back discs (zinc-300 / dark zinc-600) at depths 6,5,4,3,2,1 behind the front face, all rotated about the vertical axis by the tilt with camera distance equivalent to CSS `perspective: 600` (in a single graphics layer per disc use `rotationY = tilt`, `translationX = −z·sin(tilt)` and a matching `cameraDistance`; the stack must give the coin visible thickness when it turns). The front face is a circle clipped face with `ring-1` black @10% inset, containing the morphing face and a black overlay with opacity `|tilt|/90·0.35`. Face index shown = `variant + round(a/180)`, so faces swap as the coin passes edge-on (no crossfade). Face morphing is disabled while spinning (`morph = !spinning`).
- **Gestures (Compose `pointerInput`, horizontal only so vertical scrolls pass through):** press stops any running spin and marks `spinning`; drag sets `a = start + dx·(180/128)`; keep the last 6 `(x,t)` samples; tap slop 4px. Release: if it moved → `velocity = ((x − samples[0].x)/max(16ms, t−samples[0].t)/1000)·(180/128)`, `start=round(startAngle/180)`, `projected=round((current + velocity·0.28)/180)`, target = projected clamped to `start±10`; if it was a tap → `target=round(current/180)+1`, `velocity=900`. Animate `a` to `target·180` with the drag spring (**stiffness 140, damping 22 → dampingRatio ≈0.930**), `restDelta 0.5`, `restSpeed 4`, initial velocity as computed. Pointer-cancel: settle to `round(a/180)` with velocity 0. Reduced motion: jump without animation. On landing: `s=round(a/180)`; reset `a` to 0; if `s≠0`, `variant += s`, update the selected slot, emit change, announce the description for accessibility.
- **Shuffle button** (40dp circle, 1dp border zinc-200/800, icon dice 16dp zinc-600/400, pressed bg zinc-100 / dark zinc-900; the icon rotates `a/4` degrees): `steps = 5 + floor(rand·4)`; spin to `current+steps` with the shuffle spring (**stiffness 50, damping 11 → dampingRatio ≈0.778**) and initial velocity `steps·180·1.6`.
- **Five slots** (toolbar, 40dp circles, gap 8, variants `start + i·7`): the selected slot shows a dashed circle socket (border zinc-300 / dark zinc-700, bg zinc-50 / dark zinc-900) with a 6dp dot (zinc-400/500); the other slots show their morphing face with `ring-1` black @10% inset (pressed: 2dp ring zinc-300 / dark zinc-700). **Shared-element fly**: choosing a slot flies its face into the coin position while the previous face flies back to its slot, with the fly spring (Framer `visualDuration 0.45, bounce 0.18`, see spring note below). Arrow/Home/End navigation among slots with roving focus, and an accessibility label per slot (`"Option N: {description}"`). Choosing a slot mid-spin stops and lands the spin first.
- **Living eyes** (only while the picker is on screen and the app is foregrounded, not in reduced motion): the eye group follows the touch/pointer position anywhere on screen: `reach=min(1, dist/220dp)`, target `x=(dx/dist)·reach·1.8`, `y=(dy/dist)·reach·1.3` in face units, smoothed with a spring (**stiffness 180, damping 18 → dampingRatio ≈0.671**). Blink: first blink after 1800 ms, then every `2600 + rand·3400` ms; eyes scaleY 0.1 for 120 ms (75 ms ease-out transform transition).
- **Live morph**: whenever the seed changes (typing the name, picking a slot, landing a spin) every shape value and color eases from what is currently on screen to the new face with the morph spring (Framer `visualDuration 0.42, bounce 0.22`, see spring note below). Skip the morph on first composition and when the seed already equals the shown face.
- Display name field (mt 20): label "Display name" 14sp medium; input height 40, rounded 8, 1dp border zinc-200/800, px 12, 14sp, placeholder "Your name" zinc-400, max 32 chars, no spell-check; focused: border zinc-400 / dark zinc-600 + 2dp ring zinc-900 @10% (dark zinc-100 @10%). Submit button (mt 16): height 40, rounded 8, bg zinc-900 / dark zinc-100, text white / dark zinc-900, 14sp medium, label + ArrowRight 16dp that nudges +2dp on press; disabled (40% opacity) when `name.trim().length < 2`. Live region announces avatar changes.
- Pure functions to unit-test: everything in the golden-vector list above plus `avatarSeed`, `pad3`, `releaseTarget`, tick geometry.

**Custom photo option (the only addition):**
- Add one full-width secondary button directly under the slot toolbar (mt 12): same style as the shuffle button (height 40, rounded 8, 1dp border zinc-200/800, text 14sp zinc-600/400, pressed bg zinc-100 / dark zinc-900), with a 16dp image-plus icon and the text "Use your own photo". (It is a separate row because the toolbar has no room for another 40dp circle.)
- Tapping opens the Android Photo Picker (`ActivityResultContracts.PickVisualMedia`, no storage permission). Crop to a centered square, downscale to max 512×512, save to app-private storage (`filesDir`), store the file path in the profile next to `displayName`/`variant`.
- When a photo is active, the coin's front face shows the photo (circle-clipped, same ring and tilt shading) and the button reads "Remove photo". Any generated-face action (spinning/tapping the coin, shuffle, choosing a slot) leaves photo mode and returns to generated faces. The photo is shown as a circle everywhere the avatar appears; delete the old file when replaced or removed. It never leaves the device.

---

## 7. VERIFICATION (mandatory before you say "done")

1. Produce a table `Component | Source constant/behavior | Your file:line | Matches (Y/N)` covering **every** constant, spring, easing, size, color and state in Section 4. Fix every N. Do not claim completion with any N.
2. Run all unit tests listed above; they must pass.
3. Run the app on the emulator and verify by hand: ring tap never opens Category Distribution but the Category pill does; the full ring chart is back to its original; every entry popup rises from the bottom; Amount Pad → Details → Summary drawer → Slide to Confirm saves the entry for all four entry types; unsettled Lend/Borrow entries can be torn (slow hole-by-hole tear, release halfway springs back, full tear/flick detaches and flies, entry becomes settled, undo works); Settings push/pop/swipe-back/title-fly works on every page and every old setting still works; Hide wallet balance toggles the Spoiler everywhere balance appears; the Avatar Picker (coin spin/flick/tap, shuffle, slot fly, live name morph, eyes, blink, custom photo) works in setup and edit profile and produces the same faces as the original for the same names; all of it in Light, Dark, AMOLED, Glass Dark, Glass Light.
4. Report back: what was reverted, what was rewritten, anything you could not match exactly and why. If something cannot be ported exactly, **stop and ask — do not approximate silently.**

---

## APPENDIX — ORIGINAL SOURCE (single source of truth)

Place the original TSX files in the repo at `reference/kokonut/` (read-only, do not compile) and/or paste them below. Port from these.

### A1. spending-ring.tsx
"use client";

/**
 * @author: @dorianbaffier
 * @description: Spending Ring
 * @version: 1.0.0
 * @date: 2026-10-15
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import {
  type KeyboardEvent,
  type PointerEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { cn } from "@/lib/utils";

const SIZE = 240;
const C = SIZE / 2;
const R = 90;
const STROKE = 22;
// Degrees left empty between categories.
const GAP = 2.4;
// Spring for the ring's angle: rate in rad/s and damping ratio. A touch under
// critical so a flick settles onto its category with a small give.
const RATE = 11;
const DAMPING = 0.78;
// How far ahead a flick is projected when choosing where it lands, in seconds.
const PROJECT = 0.2;
// Movement under this many pixels is a tap, not a spin.
const TAP_SLOP = 5;

export interface SpendingCategory {
  name: string;
  amount: number;
  /** Change from last month, in percent. */
  change?: number;
  /** Tailwind classes for the segment stroke and the legend dot. */
  stroke: string;
  dot: string;
}

export interface SpendingRingProps {
  title?: string;
  categories?: SpendingCategory[];
  currency?: string;
  className?: string;
}

const DEFAULT_CATEGORIES: SpendingCategory[] = [
  {
    name: "Housing",
    amount: 1250,
    change: 0,
    stroke: "stroke-sky-500 dark:stroke-sky-400",
    dot: "bg-sky-500 dark:bg-sky-400",
  },
  {
    name: "Groceries",
    amount: 486.2,
    change: -6.1,
    stroke: "stroke-emerald-500 dark:stroke-emerald-400",
    dot: "bg-emerald-500 dark:bg-emerald-400",
  },
  {
    name: "Shopping",
    amount: 398.1,
    change: 22.4,
    stroke: "stroke-rose-500 dark:stroke-rose-400",
    dot: "bg-rose-500 dark:bg-rose-400",
  },
  {
    name: "Dining out",
    amount: 312.75,
    change: 12.8,
    stroke: "stroke-amber-400 dark:stroke-amber-300",
    dot: "bg-amber-400 dark:bg-amber-300",
  },
  {
    name: "Transport",
    amount: 214.4,
    change: -3.5,
    stroke: "stroke-violet-500 dark:stroke-violet-400",
    dot: "bg-violet-500 dark:bg-violet-400",
  },
  {
    name: "Subscriptions",
    amount: 86.97,
    change: 0,
    stroke: "stroke-zinc-400 dark:stroke-zinc-500",
    dot: "bg-zinc-400 dark:bg-zinc-500",
  },
];

/** Wraps an angle into -180..180. */
const wrap = (deg: number) => ((((deg + 180) % 360) + 360) % 360) - 180;

/** A point on the ring, with 0° at the top and angles running clockwise. */
function polar(deg: number, r = R) {
  const rad = (deg * Math.PI) / 180;
  return [C + r * Math.sin(rad), C - r * Math.cos(rad)] as const;
}

function arc(from: number, to: number) {
  const [x0, y0] = polar(from);
  const [x1, y1] = polar(to);
  const large = to - from > 180 ? 1 : 0;
  return `M${x0.toFixed(2)} ${y0.toFixed(2)} A${R} ${R} 0 ${large} 1 ${x1.toFixed(2)} ${y1.toFixed(2)}`;
}

/** The category whose slice covers an angle on the ring. */
function sliceAt(spans: { from: number; to: number }[], deg: number) {
  const a = ((deg % 360) + 360) % 360;
  const index = spans.findIndex((s) => a >= s.from && a < s.to);
  return index === -1 ? 0 : index;
}

/** One step of a damped spring, in small slices for stability. */
function springStep(
  state: { a: number; v: number },
  target: number,
  dt: number
) {
  const slices = Math.ceil(dt / (1 / 240));
  const h = dt / slices;
  for (let i = 0; i < slices; i++) {
    const accel =
      -RATE * RATE * (state.a - target) - 2 * DAMPING * RATE * state.v;
    state.v += accel * h;
    state.a += state.v * h;
  }
  return Math.abs(state.a - target) < 0.05 && Math.abs(state.v) < 2;
}

export default function SpendingRing({
  title = "October spending",
  categories = DEFAULT_CATEGORIES,
  currency = "USD",
  className,
}: SpendingRingProps) {
  const [selected, setSelected] = useState(0);
  const [dragging, setDragging] = useState(false);
  const ringRef = useRef<SVGGElement>(null);
  const selectedRef = useRef(0);

  const total = categories.reduce((sum, c) => sum + c.amount, 0);
  const spans = useMemo(() => {
    let at = 0;
    return categories.map((c) => {
      const sweep = total ? (c.amount / total) * 360 : 360 / categories.length;
      const span = { from: at, to: at + sweep, mid: at + sweep / 2 };
      at += sweep;
      return span;
    });
  }, [categories, total]);

  // The ring turns so the selected slice sits under the marker at the top.
  const sim = useRef({
    a: -(spans[0]?.mid ?? 0),
    v: 0,
    target: -(spans[0]?.mid ?? 0),
    raf: 0,
    last: 0,
    // While a flick coasts, the readout follows whatever passes the marker.
    coast: false,
    drag: null as null | {
      id: number;
      angle: number;
      time: number;
      moved: number;
      x: number;
      y: number;
    },
  });

  const money = useMemo(
    () =>
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency,
        maximumFractionDigits: 2,
        minimumFractionDigits: 2,
      }),
    [currency]
  );
  // The legend rounds to whole units.
  const wholeMoney = useMemo(
    () =>
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency,
        maximumFractionDigits: 0,
        minimumFractionDigits: 0,
      }),
    [currency]
  );

  const paint = useCallback(() => {
    ringRef.current?.setAttribute(
      "transform",
      `rotate(${sim.current.a.toFixed(3)} ${C} ${C})`
    );
  }, []);

  /** Show whichever slice is under the marker right now. */
  const track = useCallback(() => {
    const index = sliceAt(spans, -sim.current.a);
    if (index !== selectedRef.current) {
      selectedRef.current = index;
      setSelected(index);
    }
  }, [spans]);

  const tick = useCallback(
    (now: number) => {
      const s = sim.current;
      s.raf = 0;
      if (s.drag) {
        return;
      }
      const dt = s.last ? Math.min((now - s.last) / 1000, 1 / 30) : 1 / 60;
      s.last = now;
      const state = { a: s.a, v: s.v };
      const settled = springStep(state, s.target, dt);
      s.a = settled ? s.target : state.a;
      s.v = settled ? 0 : state.v;
      paint();
      if (s.coast) {
        track();
      }
      if (settled) {
        s.last = 0;
        s.coast = false;
      } else {
        s.raf = requestAnimationFrame(tick);
      }
    },
    [paint, track]
  );

  /**
   * Turn the ring to a slice, the short way round from where it would come to
   * rest on its own (a flick keeps going the way it was thrown).
   */
  const turnTo = useCallback(
    (index: number, rest = sim.current.a, coast = false) => {
      const s = sim.current;
      const goal = -(spans[index]?.mid ?? 0);
      s.target = rest + wrap(goal - rest);
      s.coast = coast;
      if (!coast) {
        selectedRef.current = index;
        setSelected(index);
      }
      if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        s.a = s.target;
        s.v = 0;
        s.coast = false;
        paint();
        track();
        return;
      }
      if (!s.raf) {
        s.last = 0;
        s.raf = requestAnimationFrame(tick);
      }
    },
    [paint, spans, tick, track]
  );

  // New data keeps the same slice under the marker. A re-render with the same
  // numbers in a fresh array leaves a spin or drag in progress alone.
  useEffect(() => {
    const s = sim.current;
    const index = Math.max(0, Math.min(selectedRef.current, spans.length - 1));
    const goal = -(spans[index]?.mid ?? 0);
    if (Math.abs(wrap(goal - s.target)) < 0.001) {
      return;
    }
    s.a = goal;
    s.target = goal;
    s.v = 0;
    selectedRef.current = index;
    setSelected(index);
    paint();
  }, [spans, paint]);

  useEffect(() => () => cancelAnimationFrame(sim.current.raf), []);

  const pointerAngle = (event: PointerEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = event.clientX - rect.left - rect.width / 2;
    const y = event.clientY - rect.top - rect.height / 2;
    return (Math.atan2(x, -y) * 180) / Math.PI;
  };

  const handlePointerDown = (event: PointerEvent<HTMLDivElement>) => {
    const s = sim.current;
    if (s.drag || (event.pointerType === "mouse" && event.button !== 0)) {
      return;
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    cancelAnimationFrame(s.raf);
    s.raf = 0;
    s.v = 0;
    s.drag = {
      id: event.pointerId,
      angle: pointerAngle(event),
      time: event.timeStamp,
      moved: 0,
      x: event.clientX,
      y: event.clientY,
    };
  };

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const s = sim.current;
    const drag = s.drag;
    if (!drag || drag.id !== event.pointerId) {
      return;
    }
    drag.moved = Math.max(
      drag.moved,
      Math.hypot(event.clientX - drag.x, event.clientY - drag.y)
    );
    if (drag.moved < TAP_SLOP) {
      return;
    }
    const angle = pointerAngle(event);
    const delta = wrap(angle - drag.angle);
    const dt = Math.max((event.timeStamp - drag.time) / 1000, 1 / 240);
    s.a += delta;
    // Smoothed angular speed, for the flick on release.
    s.v = s.v * 0.6 + (delta / dt) * 0.4;
    drag.angle = angle;
    drag.time = event.timeStamp;
    setDragging(true);
    paint();
    track();
  };

  const release = (event: PointerEvent<HTMLDivElement>) => {
    const s = sim.current;
    const drag = s.drag;
    if (!drag || drag.id !== event.pointerId) {
      return;
    }
    s.drag = null;
    setDragging(false);
    if (drag.moved < TAP_SLOP) {
      // A tap on the ring picks the slice under the finger; the middle is
      // only a readout.
      const rect = event.currentTarget.getBoundingClientRect();
      const r = Math.hypot(
        event.clientX - rect.left - rect.width / 2,
        event.clientY - rect.top - rect.height / 2
      );
      if (r > ((R - STROKE) / SIZE) * rect.width) {
        turnTo(sliceAt(spans, pointerAngle(event) - s.a));
      }
      return;
    }
    // A stale drag (held still before letting go) has no flick left in it.
    if (event.timeStamp - drag.time > 80) {
      s.v = 0;
    }
    s.v = Math.max(-1440, Math.min(1440, s.v));
    const rest = s.a + s.v * PROJECT;
    turnTo(sliceAt(spans, -rest), rest, true);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (categories.length === 0) {
      return;
    }
    const step: Record<string, number> = {
      ArrowRight: 1,
      ArrowDown: 1,
      ArrowLeft: -1,
      ArrowUp: -1,
    };
    let next: number | null = null;
    if (event.key in step) {
      next =
        (selected + step[event.key] + categories.length) % categories.length;
    } else if (event.key === "Home") {
      next = 0;
    } else if (event.key === "End") {
      next = categories.length - 1;
    }
    if (next !== null) {
      event.preventDefault();
      turnTo(next);
    }
  };

  const current = categories[selected];
  const share = total && current ? (current.amount / total) * 100 : 0;
  const change = current?.change ?? 0;

  return (
    <div
      className={cn(
        "w-full max-w-sm rounded-3xl border border-zinc-200 bg-white p-5 text-zinc-900 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-100",
        className
      )}
    >
      <div className="flex items-baseline justify-between gap-4">
        <p className="text-sm text-zinc-500 dark:text-zinc-400">{title}</p>
        <p className="font-medium text-sm tabular-nums">
          {money.format(total)}
        </p>
      </div>

      <div
        aria-label="Spending by category. Use the arrow keys to turn the ring."
        aria-valuemax={categories.length}
        aria-valuemin={1}
        aria-valuenow={selected + 1}
        aria-valuetext={
          current ? `${current.name}, ${money.format(current.amount)}` : ""
        }
        className={cn(
          "relative mx-auto mt-3 aspect-square w-full max-w-[240px] touch-none select-none rounded-full outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-4 dark:focus-visible:ring-offset-zinc-950",
          dragging ? "cursor-grabbing" : "cursor-grab"
        )}
        onKeyDown={handleKeyDown}
        onLostPointerCapture={release}
        onPointerCancel={release}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={release}
        role="slider"
        tabIndex={0}
      >
        <svg
          aria-hidden="true"
          className="absolute inset-0 size-full"
          viewBox={`0 0 ${SIZE} ${SIZE}`}
        >
          <g ref={ringRef} transform={`rotate(${sim.current.a} ${C} ${C})`}>
            {spans.map((span, i) => {
              const category = categories[i];
              const on = i === selected;
              return (
                <path
                  className={cn(
                    "transition-[stroke-width,opacity] duration-200 ease-[cubic-bezier(0.23,1,0.32,1)]",
                    category.stroke,
                    !on && "opacity-40 dark:opacity-50"
                  )}
                  d={arc(span.from + GAP / 2, span.to - GAP / 2)}
                  fill="none"
                  key={category.name}
                  strokeWidth={on ? STROKE + 8 : STROKE}
                />
              );
            })}
          </g>
          {/* The marker the ring turns slices to. */}
          <path
            className="fill-zinc-900 dark:fill-zinc-100"
            d={`M${C - 5} 2 L${C + 5} 2 L${C} 8 Z`}
          />
        </svg>

        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center text-center">
          <div
            className="starting:translate-y-1 starting:opacity-0 transition-[opacity,translate] duration-200 ease-out"
            key={current?.name}
          >
            <p className="flex items-center justify-center gap-1.5 text-xs text-zinc-500 dark:text-zinc-400">
              <span className={cn("size-1.5 rounded-full", current?.dot)} />
              {current?.name}
            </p>
            <p className="mt-0.5 font-semibold text-2xl tabular-nums tracking-tight">
              {current ? money.format(current.amount) : ""}
            </p>
            <p className="mt-0.5 text-xs text-zinc-500 tabular-nums dark:text-zinc-400">
              {share.toFixed(0)}% of total
            </p>
            <p
              className={cn(
                "mt-0.5 text-xs tabular-nums",
                change > 0 && "text-rose-600 dark:text-rose-400",
                change < 0 && "text-emerald-600 dark:text-emerald-400",
                change === 0 && "text-zinc-400 dark:text-zinc-500"
              )}
            >
              {change === 0
                ? "Same as last month"
                : `${change > 0 ? "▲" : "▼"} ${Math.abs(change).toFixed(1)}% vs last month`}
            </p>
          </div>
        </div>
      </div>

      <ul className="mt-4 grid grid-cols-2 gap-1">
        {categories.map((category, i) => (
          <li key={category.name}>
            <button
              aria-pressed={i === selected}
              className={cn(
                "flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs outline-none transition-colors duration-150 ease-out focus-visible:ring-2 focus-visible:ring-zinc-400",
                i === selected
                  ? "bg-zinc-100 dark:bg-zinc-900"
                  : "text-zinc-500 hover:bg-zinc-50 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-900/60 dark:hover:text-zinc-100"
              )}
              onClick={() => turnTo(i)}
              type="button"
            >
              <span
                aria-hidden="true"
                className={cn("size-2 shrink-0 rounded-full", category.dot)}
              />
              <span className="min-w-0 flex-1 truncate">{category.name}</span>
              <span className="tabular-nums">
                {wholeMoney.format(category.amount)}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}


### A2. amount-pad.tsx
"use client";

/**
 * @author: @dorianbaffier
 * @description: Amount Pad
 * @version: 1.0.0
 * @date: 2026-10-21
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import { ArrowUp, Check, Delete } from "lucide-react";
import {
  AnimatePresence,
  animate,
  motion,
  useMotionValue,
  useReducedMotion,
} from "motion/react";
import {
  type KeyboardEvent,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { cn } from "@/lib/utils";

const EASE_OUT = [0.23, 1, 0.32, 1] as const;
const MAX_WHOLE_DIGITS = 7;
const DIGIT = /^\d$/;
const KEY_LABELS: Record<string, string> = {
  back: "Delete",
  ".": "Decimal point",
};
const KEYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "back"];

export interface AmountPadProps {
  /** Who the money goes to. */
  recipient?: { name: string; handle?: string };
  /** Currency sign drawn before the amount. */
  currency?: string;
  /** Largest amount allowed in one go. */
  max?: number;
  /** Shown under the amount until you hit the cap. */
  balance?: number;
  /** One-tap amounts. */
  presets?: number[];
  onSend?: (amount: number) => void;
  className?: string;
}

/** One typed character, with an id that follows it as commas move around. */
interface Char {
  id: number;
  char: string;
}

interface Glyph {
  key: string;
  char: string;
  kind: "digit" | "sep" | "ghost";
}

const toNumber = (chars: Char[]) =>
  Number.parseFloat(chars.map((c) => c.char).join("") || "0");

const money = (value: number, currency: string) =>
  `${currency}${value.toLocaleString("en-US", {
    minimumFractionDigits: value % 1 === 0 ? 0 : 2,
    maximumFractionDigits: 2,
  })}`;

/** What typing `key` does to the amount: the new characters, or null if it can't take it. */
function typeKey(
  chars: Char[],
  key: string,
  make: (char: string) => Char
): Char[] | null {
  const point = chars.findIndex((c) => c.char === ".");
  if (key === ".") {
    if (point !== -1) {
      return null;
    }
    return chars.length === 0 ? [make("0"), make(".")] : [...chars, make(".")];
  }
  const centsFull = point !== -1 && chars.length - point > 2;
  const wholeFull = point === -1 && chars.length >= MAX_WHOLE_DIGITS;
  if (centsFull || wholeFull) {
    return null;
  }
  // A lone leading zero is replaced, not followed.
  return chars.length === 1 && chars[0].char === "0"
    ? [make(key)]
    : [...chars, make(key)];
}

/**
 * Lays typed characters out as an amount: commas every three whole digits,
 * keyed by the digit they sit in front of, and faint zeros after the point
 * for the cents still to come.
 */
function layout(chars: Char[]): Glyph[] {
  const point = chars.findIndex((c) => c.char === ".");
  const whole = point === -1 ? chars : chars.slice(0, point);
  const glyphs: Glyph[] = [];

  whole.forEach((c, index) => {
    const fromRight = whole.length - index;
    if (index > 0 && fromRight % 3 === 0) {
      glyphs.push({ key: `comma-${c.id}`, char: ",", kind: "sep" });
    }
    glyphs.push({ key: `d-${c.id}`, char: c.char, kind: "digit" });
  });

  if (point !== -1) {
    const cents = chars.slice(point + 1);
    glyphs.push({ key: `d-${chars[point].id}`, char: ".", kind: "sep" });
    for (const c of cents) {
      glyphs.push({ key: `d-${c.id}`, char: c.char, kind: "digit" });
    }
    for (let i = cents.length; i < 2; i++) {
      glyphs.push({ key: `ghost-${i}`, char: "0", kind: "ghost" });
    }
  }
  return glyphs;
}

export default function AmountPad({
  recipient = { name: "Maya Chen", handle: "@maya" },
  currency = "$",
  max = 2500,
  balance = 3204.18,
  presets = [20, 50, 100],
  onSend,
  className,
}: AmountPadProps) {
  const [chars, setChars] = useState<Char[]>([]);
  const [capped, setCapped] = useState(false);
  const [sent, setSent] = useState<number | null>(null);
  const reduceMotion = useReducedMotion();

  const nextId = useRef(1);
  const frameRef = useRef<HTMLDivElement>(null);
  const amountRef = useRef<HTMLDivElement>(null);
  const capTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const sentTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const fit = useMotionValue(1);
  const shakeX = useMotionValue(0);

  const value = toNumber(chars);
  const empty = chars.length === 0;

  useEffect(
    () => () => {
      clearTimeout(capTimer.current);
      clearTimeout(sentTimer.current);
    },
    []
  );

  // The amount shrinks to stay on one line as it grows, and grows back as
  // digits are deleted.
  useLayoutEffect(() => {
    const frame = frameRef.current;
    const amount = amountRef.current;
    if (!(frame && amount)) {
      return;
    }
    const measure = () => {
      const target = Math.min(1, frame.clientWidth / amount.scrollWidth);
      animate(
        fit,
        target,
        reduceMotion
          ? { duration: 0 }
          : { type: "spring", stiffness: 380, damping: 34 }
      );
    };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(frame);
    observer.observe(amount);
    return () => observer.disconnect();
  }, [fit, reduceMotion]);

  const refuse = useCallback(
    (showCap: boolean) => {
      if (showCap) {
        setCapped(true);
        clearTimeout(capTimer.current);
        capTimer.current = setTimeout(() => setCapped(false), 1800);
      }
      if (!reduceMotion) {
        animate(shakeX, [0, -10, 9, -6, 4, 0], {
          duration: 0.36,
          ease: "easeOut",
        });
      }
    },
    [reduceMotion, shakeX]
  );

  const make = (char: string): Char => {
    const id = nextId.current;
    nextId.current += 1;
    return { id, char };
  };

  // Touching the pad after a send keeps what you type: the pending reset
  // would otherwise wipe it.
  const clearSent = () => {
    clearTimeout(sentTimer.current);
    setSent(null);
  };

  const press = (key: string) => {
    clearSent();
    if (key === "back") {
      setChars((current) => current.slice(0, -1));
      setCapped(false);
      return;
    }

    const next = typeKey(chars, key, make);
    if (!next) {
      refuse(false);
    } else if (toNumber(next) > max) {
      refuse(true);
    } else {
      setChars(next);
    }
  };

  const preset = (amount: number) => {
    clearSent();
    setCapped(false);
    setChars([...String(Math.min(amount, max))].map(make));
  };

  const send = () => {
    if (sent !== null) {
      return;
    }
    if (empty || value <= 0) {
      refuse(false);
      return;
    }
    onSend?.(value);
    setSent(value);
    clearTimeout(sentTimer.current);
    sentTimer.current = setTimeout(() => {
      setChars([]);
      setSent(null);
    }, 1600);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.metaKey || event.ctrlKey || event.altKey) {
      return;
    }
    let key: string | null = null;
    if (DIGIT.test(event.key) || event.key === ".") {
      key = event.key;
    } else if (event.key === ",") {
      key = ".";
    } else if (event.key === "Backspace") {
      key = "back";
    }
    // Enter is left alone: on a focused key or Send it already clicks it.
    if (key) {
      event.preventDefault();
      press(key);
    }
  };

  const glyphs = layout(chars);
  let sendTone =
    "bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-white";
  if (sent !== null) {
    sendTone =
      "bg-emerald-600 text-white dark:bg-emerald-500 dark:text-emerald-950";
  } else if (empty) {
    sendTone = "bg-zinc-100 text-zinc-400 dark:bg-zinc-900 dark:text-zinc-600";
  }
  const digitIn = reduceMotion
    ? { duration: 0 }
    : { type: "spring" as const, stiffness: 520, damping: 30, mass: 0.8 };
  const slide = reduceMotion
    ? { duration: 0 }
    : { type: "spring" as const, stiffness: 500, damping: 40 };

  const initials = recipient.name
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2);

  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: typing anywhere in the pad goes to the amount; the keys themselves are buttons.
    // biome-ignore lint/a11y/noNoninteractiveElementInteractions: same as above
    <div
      className={cn(
        "flex w-full max-w-xs flex-col rounded-3xl border border-zinc-200 bg-white p-5 dark:border-zinc-800 dark:bg-zinc-950",
        className
      )}
      onKeyDown={onKeyDown}
    >
      <div className="flex items-center gap-3">
        <span className="flex size-9 items-center justify-center rounded-full bg-orange-100 font-medium text-orange-700 text-xs dark:bg-orange-500/15 dark:text-orange-300">
          {initials}
        </span>
        <div className="min-w-0 leading-tight">
          <p className="text-xs text-zinc-500 dark:text-zinc-400">Send to</p>
          <p className="truncate font-medium text-sm text-zinc-900 dark:text-zinc-100">
            {recipient.name}
            {recipient.handle && (
              <span className="ml-1.5 font-normal text-zinc-400 dark:text-zinc-500">
                {recipient.handle}
              </span>
            )}
          </p>
        </div>
      </div>

      <div
        className="mt-6 flex h-20 items-center justify-center"
        ref={frameRef}
      >
        <output aria-live="polite" className="sr-only">
          {money(value, currency)}
        </output>
        <motion.div
          aria-hidden="true"
          className="flex origin-center items-baseline whitespace-nowrap font-semibold text-[56px] text-zinc-900 tabular-nums leading-none tracking-tight dark:text-zinc-50"
          ref={amountRef}
          style={{ scale: fit, x: shakeX }}
        >
          <span
            className={cn(
              "mr-0.5 transition-colors duration-200",
              empty
                ? "text-zinc-300 dark:text-zinc-700"
                : "text-zinc-400 dark:text-zinc-500"
            )}
          >
            {currency}
          </span>
          <AnimatePresence initial={false} mode="popLayout">
            {empty && (
              <motion.span
                animate={{ opacity: 1 }}
                className="text-zinc-300 dark:text-zinc-700"
                exit={{ opacity: 0, transition: { duration: 0.08 } }}
                initial={{ opacity: 0 }}
                key="placeholder"
                transition={{ duration: 0.15 }}
              >
                0
              </motion.span>
            )}
            {glyphs.map((glyph) => (
              <motion.span
                animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
                className={cn(
                  "inline-block",
                  glyph.kind === "ghost" && "text-zinc-300 dark:text-zinc-700",
                  glyph.kind === "sep" && "text-zinc-400 dark:text-zinc-500"
                )}
                exit={{
                  opacity: 0,
                  y: 18,
                  filter: "blur(2px)",
                  transition: { duration: 0.14, ease: EASE_OUT },
                }}
                initial={{ opacity: 0, y: -22, filter: "blur(2px)" }}
                key={glyph.key}
                layout="position"
                transition={{
                  default: digitIn,
                  layout: slide,
                  filter: { duration: 0.16 },
                }}
              >
                {glyph.char}
              </motion.span>
            ))}
          </AnimatePresence>
        </motion.div>
      </div>

      <div className="relative h-5 text-center text-xs">
        <AnimatePresence initial={false} mode="popLayout">
          <motion.p
            animate={{ opacity: 1, y: 0 }}
            className={cn(
              capped
                ? "text-red-600 dark:text-red-400"
                : "text-zinc-500 dark:text-zinc-400"
            )}
            exit={{ opacity: 0, y: -6 }}
            initial={{ opacity: 0, y: 6 }}
            key={capped ? "cap" : "balance"}
            transition={{ duration: 0.18, ease: EASE_OUT }}
          >
            {capped
              ? `Up to ${money(max, currency)} per transfer`
              : `Balance ${currency}${balance.toLocaleString("en-US", { minimumFractionDigits: 2 })}`}
          </motion.p>
        </AnimatePresence>
      </div>

      <div className="mt-4 flex justify-center gap-2">
        {presets.map((amount) => (
          <button
            className="h-8 rounded-full border border-zinc-200 px-3 font-medium text-xs text-zinc-700 tabular-nums transition-colors duration-150 hover:bg-zinc-50 active:bg-zinc-100 dark:border-zinc-800 dark:text-zinc-300 dark:active:bg-zinc-800 dark:hover:bg-zinc-900"
            key={amount}
            onClick={() => preset(amount)}
            type="button"
          >
            {money(amount, currency)}
          </button>
        ))}
      </div>

      <div className="mt-4 grid grid-cols-3 gap-1">
        {KEYS.map((key) => (
          <button
            aria-label={KEY_LABELS[key] ?? key}
            className={cn(
              "flex h-14 items-center justify-center rounded-2xl font-medium text-2xl text-zinc-900 tabular-nums outline-none",
              "transition-colors duration-100 ease-out hover:bg-zinc-50 active:bg-zinc-100 active:duration-0",
              "focus-visible:ring-2 focus-visible:ring-zinc-400 dark:text-zinc-100 dark:active:bg-zinc-800 dark:hover:bg-zinc-900"
            )}
            key={key}
            onClick={() => press(key)}
            type="button"
          >
            {key === "back" ? (
              <Delete
                aria-hidden
                className="size-6 text-zinc-500"
                strokeWidth={1.75}
              />
            ) : (
              key
            )}
          </button>
        ))}
      </div>

      <button
        aria-disabled={empty}
        className={cn(
          "relative mt-3 flex h-12 items-center justify-center gap-2 overflow-hidden rounded-2xl font-medium text-sm outline-none",
          "transition-colors duration-200 ease-out focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 dark:focus-visible:ring-offset-zinc-950",
          sendTone
        )}
        onClick={send}
        type="button"
      >
        <AnimatePresence initial={false} mode="popLayout">
          <motion.span
            animate={{ opacity: 1, y: 0 }}
            className="inline-flex items-center gap-2"
            exit={{ opacity: 0, y: -10 }}
            initial={{ opacity: 0, y: 10 }}
            key={sent === null ? "send" : "sent"}
            transition={{ duration: 0.2, ease: EASE_OUT }}
          >
            {sent === null ? (
              <>
                <ArrowUp aria-hidden className="size-4" />
                {empty ? "Enter an amount" : `Send ${money(value, currency)}`}
              </>
            ) : (
              <>
                <Check aria-hidden className="size-4" strokeWidth={2.5} />
                Sent {money(sent, currency)}
              </>
            )}
          </motion.span>
        </AnimatePresence>
      </button>
    </div>
  );
}


### A3. smooth-drawer.tsx
"use client";

/**
 * @author: @dorianbaffier
 * @description: Smooth Drawer
 * @version: 2.0.0
 * @date: 2026-10-16
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import { ChevronUp, CreditCard } from "lucide-react";
import {
  AnimatePresence,
  motion,
  useReducedMotion,
  type Variants,
} from "motion/react";
import {
  type PointerEvent,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";
import { Button } from "@/components/ui/button";
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer";
import { cn } from "@/lib/utils";

export interface OrderItem {
  id: string;
  name: string;
  /** Variant line, e.g. "Sand · M". */
  detail?: string;
  /** Unit price. */
  price: number;
  quantity?: number;
}

export type DeliverySpeed = "standard" | "express";

export interface OrderSummary {
  delivery: DeliverySpeed;
  subtotal: number;
  shipping: number;
  tax: number;
  total: number;
}

export interface SmoothDrawerProps {
  title?: string;
  /** Line under the title. Defaults to the item count and arrival estimate. */
  description?: string;
  items?: OrderItem[];
  /** Tax rate applied to the subtotal. */
  taxRate?: number;
  /** ISO 4217 currency code. */
  currency?: string;
  triggerLabel?: string;
  confirmLabel?: string;
  /** Called with the final breakdown; the sheet closes afterwards. */
  onConfirm?: (summary: OrderSummary) => void;
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
}

const DEFAULT_ITEMS: OrderItem[] = [
  { id: "overshirt", name: "Linen overshirt", detail: "Sand · M", price: 88 },
  {
    id: "socks",
    name: "Merino crew socks",
    detail: "Charcoal · 2 pairs",
    price: 14,
    quantity: 2,
  },
  { id: "tote", name: "Canvas tote", detail: "Natural", price: 32 },
];

const DELIVERY = {
  standard: { label: "Standard", price: 0, eta: "3–5 business days" },
  express: { label: "Express", price: 12, eta: "Tomorrow" },
} as const;

/** Visible height of the sheet at its first snap point. */
const PEEK_HEIGHT = 200;
/** Tallest the sheet gets, and the gap it keeps from the top of the screen. */
const MAX_SHEET_HEIGHT = 680;
const TOP_GAP = 24;

const EASE_OUT = [0.23, 1, 0.32, 1] as const;

/** Each digit slides in the direction the total moved. */
const digitVariants = {
  enter: (direction: number) => ({ y: `${direction * 70}%`, opacity: 0 }),
  center: { y: "0%", opacity: 1 },
  exit: (direction: number) => ({ y: `${direction * -70}%`, opacity: 0 }),
} satisfies Variants;

const fadeVariants = {
  enter: { opacity: 0 },
  center: { opacity: 1 },
  exit: { opacity: 0 },
} satisfies Variants;

function RollingAmount({ value, format }: { value: number; format: string }) {
  const reduceMotion = useReducedMotion();
  const previous = useRef(value);
  const direction = value >= previous.current ? 1 : -1;

  useEffect(() => {
    previous.current = value;
  }, [value]);

  const chars = format.split("");

  return (
    <span className="relative inline-flex tabular-nums">
      <span className="sr-only">{format}</span>
      <span aria-hidden="true" className="inline-flex">
        {chars.map((char, index) => {
          // Keyed from the right so cents stay put when dollars gain a digit.
          const slot = chars.length - index;
          if (char < "0" || char > "9") {
            return <span key={`s${slot}`}>{char}</span>;
          }
          return (
            <span
              className="relative inline-flex overflow-hidden"
              key={`d${slot}`}
            >
              <AnimatePresence
                custom={direction}
                initial={false}
                mode="popLayout"
              >
                <motion.span
                  animate="center"
                  className="inline-block"
                  custom={direction}
                  exit="exit"
                  initial="enter"
                  key={char}
                  transition={{ duration: 0.24, ease: EASE_OUT }}
                  variants={reduceMotion ? fadeVariants : digitVariants}
                >
                  {char}
                </motion.span>
              </AnimatePresence>
            </span>
          );
        })}
      </span>
    </span>
  );
}

const clamp01 = (n: number) => Math.min(1, Math.max(0, n));

export default function SmoothDrawer({
  title = "Order summary",
  description,
  items = DEFAULT_ITEMS,
  taxRate = 0.08,
  currency = "USD",
  triggerLabel = "Review order",
  confirmLabel = "Pay",
  onConfirm,
  open,
  defaultOpen = false,
  onOpenChange,
}: SmoothDrawerProps) {
  const [internalOpen, setInternalOpen] = useState(defaultOpen);
  const isOpen = open ?? internalOpen;
  const [expanded, setExpanded] = useState(false);
  const [delivery, setDelivery] = useState<DeliverySpeed>("standard");
  const [viewportHeight, setViewportHeight] = useState(0);
  const detailsRef = useRef<HTMLDivElement>(null);
  const frame = useRef(0);
  const reduceMotion = useReducedMotion();
  const deliveryName = useId();

  useEffect(() => {
    const measure = () => setViewportHeight(window.innerHeight);
    measure();
    window.addEventListener("resize", measure);
    return () => window.removeEventListener("resize", measure);
  }, []);

  const money = useMemo(
    () => new Intl.NumberFormat("en-US", { style: "currency", currency }),
    [currency]
  );

  const subtotal = items.reduce(
    (sum, item) => sum + item.price * (item.quantity ?? 1),
    0
  );
  const shipping = DELIVERY[delivery].price;
  const tax = Math.round(subtotal * taxRate * 100) / 100;
  const total = subtotal + shipping + tax;
  const itemCount = items.reduce((sum, item) => sum + (item.quantity ?? 1), 0);
  const arrival =
    delivery === "express" ? "Arrives tomorrow" : "Arrives in 3–5 days";

  // Vaul measures snap points from the top of the window, so the sheet is
  // given an explicit height and the peek point is offset by the gap above it.
  const sheetHeight = Math.max(
    PEEK_HEIGHT,
    Math.min(MAX_SHEET_HEIGHT, viewportHeight - TOP_GAP)
  );
  const snapPoints = useMemo(
    () => [`${PEEK_HEIGHT + Math.max(0, viewportHeight - sheetHeight)}px`, 1],
    [viewportHeight, sheetHeight]
  );

  const handleOpenChange = (next: boolean) => {
    if (open === undefined) {
      setInternalOpen(next);
    }
    if (next) {
      setExpanded(false);
    }
    onOpenChange?.(next);
  };

  const clearDragStyles = useCallback(() => {
    cancelAnimationFrame(frame.current);
    const details = detailsRef.current;
    if (details) {
      details.style.transition = "";
      details.style.opacity = "";
      details.style.translate = "";
    }
  }, []);

  // Hand the details back to their class-driven transition.
  const setSnap = (next: boolean) => {
    clearDragStyles();
    setExpanded(next);
  };

  // While Vaul drags the sheet, the details track its position directly.
  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const sheet = event.currentTarget;
    cancelAnimationFrame(frame.current);
    frame.current = requestAnimationFrame(() => {
      const details = detailsRef.current;
      if (!(details && sheet.classList.contains("vaul-dragging"))) {
        return;
      }
      const peekTop = window.innerHeight - PEEK_HEIGHT;
      const fullTop = window.innerHeight - sheetHeight;
      const top = sheet.getBoundingClientRect().top;
      const progress = clamp01((peekTop - top) / (peekTop - fullTop || 1));
      details.style.transition = "none";
      details.style.opacity = String(progress);
      details.style.translate = reduceMotion
        ? ""
        : `0 ${Math.round((1 - progress) * 12)}px`;
    });
  };

  const handleConfirm = () => {
    onConfirm?.({ delivery, subtotal, shipping, tax, total });
    handleOpenChange(false);
  };

  return (
    <Drawer
      activeSnapPoint={snapPoints[expanded ? 1 : 0]}
      fadeFromIndex={0}
      onOpenChange={handleOpenChange}
      open={isOpen}
      setActiveSnapPoint={(point) => setSnap(point === 1)}
      snapPoints={snapPoints}
    >
      <DrawerTrigger asChild>
        <Button className="h-10 rounded-lg px-4" variant="outline">
          {triggerLabel}
        </Button>
      </DrawerTrigger>
      <DrawerContent
        className={cn(
          "mx-auto w-full max-w-[420px] rounded-t-2xl border border-zinc-200 border-b-0 bg-white outline-none dark:border-zinc-800 dark:bg-zinc-950",
          "mb-0! data-[vaul-drawer-direction=bottom]:mt-0 data-[vaul-drawer-direction=bottom]:max-h-none data-[vaul-drawer-direction=bottom]:rounded-t-2xl",
          // Sit flush on the bottom edge even if the host styles dialogs with a
          // margin. Swap the stock wide handle for a small pill.
          "[&>div:first-child]:hidden"
        )}
        onPointerCancel={clearDragStyles}
        onPointerMove={handlePointerMove}
        onPointerUp={clearDragStyles}
        style={{ height: sheetHeight }}
      >
        <div className="flex h-full flex-col">
          <div
            aria-hidden="true"
            className="flex h-6 shrink-0 items-center justify-center"
          >
            <span className="h-1 w-9 rounded-full bg-zinc-300 dark:bg-zinc-700" />
          </div>

          <div className="shrink-0 px-5 pt-1">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <DrawerTitle className="font-semibold text-[15px] text-zinc-900 tracking-tight dark:text-zinc-50">
                  {title}
                </DrawerTitle>
                <DrawerDescription className="mt-0.5 text-[13px] text-zinc-500 dark:text-zinc-400">
                  {description ??
                    `${itemCount} ${itemCount === 1 ? "item" : "items"} · ${arrival}`}
                </DrawerDescription>
              </div>
              <div className="shrink-0 text-right">
                <p className="text-[13px] text-zinc-500 dark:text-zinc-400">
                  Total
                </p>
                <p className="mt-0.5 font-semibold text-lg text-zinc-900 leading-6 tracking-tight dark:text-zinc-50">
                  <RollingAmount format={money.format(total)} value={total} />
                </p>
              </div>
            </div>

            <button
              className="mt-4 flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-zinc-900 font-medium text-sm text-white outline-none transition-colors duration-150 ease-out hover:bg-zinc-800 focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 focus-visible:ring-offset-white active:bg-zinc-700 dark:bg-zinc-50 dark:text-zinc-900 dark:active:bg-zinc-300 dark:focus-visible:ring-offset-zinc-950 dark:hover:bg-zinc-200"
              onClick={handleConfirm}
              type="button"
            >
              {confirmLabel}
            </button>

            <button
              aria-expanded={expanded}
              className="mt-1 flex h-10 w-full items-center justify-center gap-1.5 rounded-lg text-[13px] text-zinc-500 outline-none transition-colors duration-150 hover:text-zinc-900 focus-visible:ring-2 focus-visible:ring-zinc-400 dark:text-zinc-400 dark:hover:text-zinc-100"
              onClick={() => setSnap(!expanded)}
              type="button"
            >
              {expanded ? "Hide details" : "Order details"}
              <ChevronUp
                aria-hidden="true"
                className={cn(
                  "size-3.5 transition-transform duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] motion-reduce:transition-none",
                  expanded && "rotate-180"
                )}
              />
            </button>
          </div>

          <div
            className={cn(
              "min-h-0 flex-1 px-5 pb-6",
              "transition-[opacity,translate] duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] motion-reduce:translate-y-0",
              expanded
                ? "translate-y-0 overflow-y-auto opacity-100"
                : "translate-y-3 overflow-hidden opacity-0"
            )}
            inert={!expanded}
            ref={detailsRef}
          >
            <div className="border-zinc-200 border-t pt-4 dark:border-zinc-800">
              <ul className="space-y-3">
                {items.map((item) => (
                  <li className="flex items-center gap-3" key={item.id}>
                    <span className="flex size-10 shrink-0 items-center justify-center rounded-lg border border-zinc-200 bg-zinc-50 font-medium text-sm text-zinc-500 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-400">
                      {item.name.charAt(0)}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium text-sm text-zinc-900 dark:text-zinc-100">
                        {item.name}
                      </p>
                      {item.detail ? (
                        <p className="truncate text-[13px] text-zinc-500 dark:text-zinc-400">
                          {item.detail}
                        </p>
                      ) : null}
                    </div>
                    <span className="text-sm text-zinc-900 tabular-nums dark:text-zinc-100">
                      {money.format(item.price * (item.quantity ?? 1))}
                    </span>
                  </li>
                ))}
              </ul>

              <fieldset className="mt-5">
                <legend className="font-medium text-[13px] text-zinc-500 dark:text-zinc-400">
                  Delivery
                </legend>
                <div className="mt-2 grid grid-cols-2 gap-2">
                  {(Object.keys(DELIVERY) as DeliverySpeed[]).map((speed) => {
                    const option = DELIVERY[speed];
                    const checked = delivery === speed;
                    return (
                      <label
                        className={cn(
                          "relative flex cursor-pointer flex-col rounded-xl border px-3 py-2.5 transition-colors duration-150 has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-zinc-400",
                          checked
                            ? "border-zinc-900 bg-zinc-50 dark:border-zinc-200 dark:bg-zinc-900"
                            : "border-zinc-200 hover:border-zinc-300 dark:border-zinc-800 dark:hover:border-zinc-700"
                        )}
                        key={speed}
                      >
                        <input
                          checked={checked}
                          className="sr-only"
                          name={deliveryName}
                          onChange={() => setDelivery(speed)}
                          type="radio"
                          value={speed}
                        />
                        <span className="flex items-baseline justify-between gap-2">
                          <span className="font-medium text-sm text-zinc-900 dark:text-zinc-100">
                            {option.label}
                          </span>
                          <span className="text-[13px] text-zinc-900 tabular-nums dark:text-zinc-100">
                            {option.price === 0
                              ? "Free"
                              : money.format(option.price)}
                          </span>
                        </span>
                        <span className="mt-0.5 text-[13px] text-zinc-500 dark:text-zinc-400">
                          {option.eta}
                        </span>
                      </label>
                    );
                  })}
                </div>
              </fieldset>

              <dl className="mt-5 space-y-1.5 text-sm tabular-nums">
                <div className="flex justify-between text-zinc-500 dark:text-zinc-400">
                  <dt>Subtotal</dt>
                  <dd>{money.format(subtotal)}</dd>
                </div>
                <div className="flex justify-between text-zinc-500 dark:text-zinc-400">
                  <dt>Delivery</dt>
                  <dd>{shipping === 0 ? "Free" : money.format(shipping)}</dd>
                </div>
                <div className="flex justify-between text-zinc-500 dark:text-zinc-400">
                  <dt>Estimated tax</dt>
                  <dd>{money.format(tax)}</dd>
                </div>
                <div className="flex justify-between border-zinc-200 border-t pt-2 font-medium text-zinc-900 dark:border-zinc-800 dark:text-zinc-100">
                  <dt>Total</dt>
                  <dd>{money.format(total)}</dd>
                </div>
              </dl>

              <div className="mt-5 flex items-center gap-3 rounded-xl border border-zinc-200 px-3 py-2.5 dark:border-zinc-800">
                <CreditCard
                  aria-hidden="true"
                  className="size-4 text-zinc-500 dark:text-zinc-400"
                />
                <span className="flex-1 text-sm text-zinc-900 dark:text-zinc-100">
                  Visa ending 4242
                </span>
                <span className="text-[13px] text-zinc-500 dark:text-zinc-400">
                  Default
                </span>
              </div>
            </div>
          </div>
        </div>
      </DrawerContent>
    </Drawer>
  );
}


### A4. slide-to-confirm.tsx
"use client";

/**
 * @author: @dorianbaffier
 * @description: Slide to Confirm
 * @version: 1.0.0
 * @date: 2026-10-21
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import { ArrowRight, Check } from "lucide-react";
import {
  AnimatePresence,
  animate,
  motion,
  useMotionValue,
  useReducedMotion,
  useTransform,
} from "motion/react";
import {
  type MouseEvent,
  type PointerEvent,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { cn } from "@/lib/utils";

const EASE_OUT = [0.23, 1, 0.32, 1] as const;
const EASE_IN_OUT = [0.65, 0, 0.35, 1] as const;
const HEIGHT = 56;
const INSET = 4;
const THUMB = HEIGHT - INSET * 2;
/** Share of the track the thumb must cross before letting go commits. */
const COMMIT_AT = 0.9;
/** A flick this fast (px/s) commits from FLICK_FROM of the way along. */
const FLICK_SPEED = 900;
const FLICK_FROM = 0.45;
/** How far past either end the thumb stretches, at most. */
const OVERDRAG = 8;
/** A press that moves less than this is a tap. */
const TAP_SLOP = 4;

type Phase = "idle" | "dragging" | "confirming" | "done";

export interface SlideToConfirmProps {
  /** What sliding does, e.g. "Slide to pay $48.00". */
  label?: string;
  /** Read out while `onConfirm` runs. */
  confirmingLabel?: string;
  /** Shown once it's done, e.g. "Paid". */
  doneLabel?: string;
  /**
   * The real work. Return a promise to keep the spinner up until it settles;
   * a rejected promise sends the thumb back to the start.
   */
  onConfirm?: () => Promise<unknown> | unknown;
  /** Go back to the start this many ms after it's done. `null` stays done. */
  resetAfter?: number | null;
  disabled?: boolean;
  className?: string;
}

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

// Past the ends the thumb follows less and less, so it stretches instead of
// stopping dead against the edge.
function band(raw: number, max: number) {
  if (raw > max) {
    return max + OVERDRAG * (1 - Math.exp(-(raw - max) / 40));
  }
  if (raw < 0) {
    return -OVERDRAG * (1 - Math.exp(raw / 40));
  }
  return raw;
}

export default function SlideToConfirm({
  label = "Slide to pay $48.00",
  confirmingLabel = "Paying",
  doneLabel = "Paid",
  onConfirm = () => wait(1200),
  resetAfter = 2600,
  disabled = false,
  className,
}: SlideToConfirmProps) {
  const [phase, setPhase] = useState<Phase>("idle");
  const [armed, setArmed] = useState(false);
  const reduceMotion = useReducedMotion();

  const trackRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef({ pointer: 0, from: 0, moved: false });
  const runRef = useRef(0);
  const confirmRef = useRef(onConfirm);
  confirmRef.current = onConfirm;

  const x = useMotionValue(0);
  const width = useMotionValue(0);
  // 0: the ink ends at the thumb. 1: it fills the whole track, thumb and all.
  const cover = useMotionValue(0);

  const max = () => Math.max(0, width.get() - THUMB - INSET * 2);

  // The ink fill runs from the left edge to just past the thumb, so the label
  // turns white as the thumb wipes over it. Once committed it floods the rest.
  const fillClip = useTransform(
    [x, width, cover],
    ([value, total, covered]: number[]) =>
      `inset(0px ${(1 - covered) * Math.max(0, total - value - INSET - THUMB / 2)}px 0px 0px round ${HEIGHT}px)`
  );
  // Hidden at rest, so no ink shows round the thumb before it moves.
  const fillOpacity = useTransform(x, (value) => Math.min(1, value / 20));
  const labelOpacity = useTransform([x, width], ([value, total]: number[]) => {
    const progress = value / Math.max(1, total - THUMB - INSET * 2);
    return Math.max(0, 1 - progress * 0.55);
  });

  useLayoutEffect(() => {
    const track = trackRef.current;
    if (!track) {
      return;
    }
    const observer = new ResizeObserver(() => {
      const before = width.get();
      width.set(track.offsetWidth);
      // Keep a thumb that sits at the end at the end when the track resizes.
      if (before > 0 && x.get() >= before - THUMB - INSET * 2) {
        x.set(track.offsetWidth - THUMB - INSET * 2);
      }
    });
    observer.observe(track);
    return () => observer.disconnect();
  }, [width, x]);

  useEffect(
    () => () => {
      runRef.current += 1;
    },
    []
  );

  const spring = (velocity = 0) =>
    reduceMotion
      ? { duration: 0.15, ease: EASE_OUT }
      : { type: "spring" as const, stiffness: 520, damping: 42, velocity };

  // The ink drains back behind the thumb as it springs home.
  const settleBack = (velocity = 0) => {
    setArmed(false);
    setPhase("idle");
    animate(cover, 0, { duration: reduceMotion ? 0 : 0.3, ease: EASE_OUT });
    animate(x, 0, spring(velocity));
  };

  // The reset nobody asked for runs slower, so it reads as a rewind.
  const rewind = () => {
    setArmed(false);
    setPhase("idle");
    const duration = reduceMotion ? 0 : 0.6;
    animate(cover, 0, { duration: duration * 0.6, ease: EASE_OUT });
    animate(x, 0, { duration, ease: EASE_IN_OUT });
  };

  // One motion from release to result: the thumb carries on to the end while
  // the ink floods the track and the label turns to "Paying", and the work
  // starts at once instead of waiting for the thumb to land.
  const commit = async (velocity: number, glide = false) => {
    const run = ++runRef.current;
    const alive = () => runRef.current === run;
    setPhase("confirming");
    setArmed(true);

    const landing = animate(
      x,
      max(),
      glide && !reduceMotion
        ? { duration: 0.42, ease: EASE_IN_OUT }
        : spring(velocity)
    );
    const flooding = animate(cover, 1, {
      duration: reduceMotion ? 0 : 0.36,
      ease: EASE_OUT,
    });

    try {
      // A floor on the wait, so a fast handler doesn't flash the spinner.
      await Promise.all([confirmRef.current(), wait(800), landing, flooding]);
    } catch {
      if (alive()) {
        settleBack();
      }
      return;
    }
    if (!alive()) {
      return;
    }
    setPhase("done");
    if (resetAfter === null) {
      return;
    }
    await wait(resetAfter);
    if (alive()) {
      rewind();
    }
  };

  const onPointerDown = (event: PointerEvent<HTMLButtonElement>) => {
    if (disabled || phase !== "idle" || event.button !== 0) {
      return;
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    x.stop();
    dragRef.current = { pointer: event.clientX, from: x.get(), moved: false };
    setPhase("dragging");
  };

  const onPointerMove = (event: PointerEvent<HTMLButtonElement>) => {
    if (phase !== "dragging") {
      return;
    }
    const drag = dragRef.current;
    const delta = event.clientX - drag.pointer;
    if (Math.abs(delta) > TAP_SLOP) {
      drag.moved = true;
    }
    const raw = drag.from + delta;
    x.set(band(raw, max()));
    const isArmed = raw >= max() * COMMIT_AT;
    if (isArmed !== armed) {
      setArmed(isArmed);
    }
  };

  const onPointerUp = () => {
    if (phase !== "dragging") {
      return;
    }
    if (!dragRef.current.moved) {
      setPhase("idle");
      return;
    }
    const velocity = x.getVelocity();
    const progress = x.get() / Math.max(1, max());
    const flicked = velocity > FLICK_SPEED && progress > FLICK_FROM;
    if (progress >= COMMIT_AT || flicked) {
      commit(velocity);
    } else {
      settleBack(velocity);
    }
  };

  const onClick = async (event: MouseEvent<HTMLButtonElement>) => {
    if (disabled || phase !== "idle") {
      return;
    }
    // Enter and Space click with no pointer, and confirm in one go.
    if (event.detail === 0) {
      commit(0, true);
      return;
    }
    if (dragRef.current.moved || reduceMotion) {
      return;
    }
    // A tap shows which way to go: a short nudge that springs back.
    await animate(x, 28, { duration: 0.18, ease: EASE_OUT });
    if (phase === "idle") {
      animate(x, 0, spring());
    }
  };

  const done = phase === "done";
  const busy = phase === "confirming";
  let stage = "slide";
  let stageLabel = label;
  if (busy) {
    stage = "busy";
    stageLabel = `${confirmingLabel}…`;
  } else if (done) {
    stage = "done";
    stageLabel = doneLabel;
  }

  return (
    <div
      className={cn("relative w-full max-w-sm select-none", className)}
      data-phase={phase}
    >
      <motion.div
        className={cn(
          "relative overflow-hidden rounded-full border",
          "border-zinc-200 bg-zinc-100 dark:border-zinc-800 dark:bg-zinc-900",
          disabled && "opacity-50"
        )}
        ref={trackRef}
        style={{ height: HEIGHT }}
      >
        {/* The label as it reads on the empty track. Nudged off the thumb by
            half its width, so it sits between the track and free-space
            centers and reads as centered. */}
        <motion.span
          aria-hidden
          className="absolute inset-0 flex items-center justify-center whitespace-nowrap pl-6 font-medium text-sm text-zinc-500 dark:text-zinc-400"
          style={{ opacity: labelOpacity }}
        >
          {label}
        </motion.span>

        {/* The ink the thumb leaves behind, with the label in white on it. It
            turns emerald in place when the work is done. */}
        <motion.div
          aria-hidden
          className={cn(
            "absolute inset-0 transition-colors duration-300 ease-out motion-reduce:transition-none",
            done
              ? "bg-emerald-600 dark:bg-emerald-500"
              : "bg-zinc-900 dark:bg-zinc-100"
          )}
          style={{ clipPath: fillClip, opacity: fillOpacity }}
        >
          <AnimatePresence initial={false} mode="popLayout">
            <motion.span
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              className={cn(
                "absolute inset-0 flex items-center justify-center gap-2 whitespace-nowrap font-medium text-sm",
                done
                  ? "text-white dark:text-emerald-950"
                  : "text-white dark:text-zinc-900",
                phase === "idle" || phase === "dragging" ? "pl-6" : "pr-6"
              )}
              exit={{ opacity: 0, y: -8, filter: "blur(2px)" }}
              initial={{ opacity: 0, y: 8, filter: "blur(2px)" }}
              key={stage}
              transition={
                reduceMotion
                  ? { duration: 0 }
                  : { duration: 0.26, ease: EASE_OUT }
              }
            >
              {stageLabel}
            </motion.span>
          </AnimatePresence>
        </motion.div>

        <motion.button
          aria-disabled={disabled || phase !== "idle"}
          aria-label={`${label}. Press Enter to confirm.`}
          className={cn(
            "absolute flex touch-none items-center justify-center rounded-full outline-none",
            "bg-white text-zinc-900 shadow-[0_1px_2px_rgb(0_0_0/0.12),0_0_0_1px_rgb(0_0_0/0.06)]",
            "dark:bg-zinc-800 dark:text-zinc-100 dark:shadow-[0_1px_2px_rgb(0_0_0/0.5),0_0_0_1px_rgb(255_255_255/0.08)]",
            "focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-100 dark:focus-visible:ring-offset-zinc-900",
            phase === "idle" && !disabled && "cursor-grab",
            phase === "dragging" && "cursor-grabbing"
          )}
          onClick={onClick}
          onPointerCancel={onPointerUp}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          // Offsets are from inside the 1px border.
          style={{
            x,
            left: INSET - 1,
            top: INSET - 1,
            width: THUMB,
            height: THUMB,
          }}
          type="button"
        >
          <span className="relative size-5">
            <ArrowRight
              aria-hidden
              className={cn(
                "absolute inset-0 size-5 transition-[opacity,translate,filter] duration-200 ease-out motion-reduce:transition-none",
                armed && !busy && !done && "translate-x-0.5",
                (busy || done) && "translate-x-1.5 opacity-0 blur-[2px]"
              )}
            />
            <Check
              aria-hidden
              className={cn(
                "absolute inset-0 size-5 text-emerald-600 transition-[opacity,scale,filter] duration-200 ease-out motion-reduce:transition-none dark:text-emerald-400",
                done ? "scale-100 opacity-100" : "scale-50 opacity-0 blur-[2px]"
              )}
              strokeWidth={2.5}
            />
            <svg
              aria-hidden
              className={cn(
                "absolute -inset-0.5 size-6 animate-spin transition-opacity duration-200 [animation-duration:700ms]",
                // Paused, not removed, so it fades out without snapping back.
                busy
                  ? "opacity-100 delay-100"
                  : "opacity-0 [animation-play-state:paused]"
              )}
              viewBox="0 0 24 24"
            >
              <title>Working</title>
              <circle
                className="stroke-zinc-200 dark:stroke-zinc-700"
                cx="12"
                cy="12"
                fill="none"
                r="9"
                strokeWidth="2.5"
              />
              <circle
                className="stroke-current"
                cx="12"
                cy="12"
                fill="none"
                r="9"
                strokeDasharray="14 43"
                strokeLinecap="round"
                strokeWidth="2.5"
              />
            </svg>
          </span>
        </motion.button>
      </motion.div>

      <span aria-live="polite" className="sr-only">
        {busy && `${confirmingLabel}…`}
        {done && doneLabel}
      </span>
    </div>
  );
}


### A5. tear-button.tsx
"use client";

/**
 * @author: @dorianbaffier
 * @description: Tear Button
 * @version: 1.0.0
 * @date: 2026-10-14
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import {
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { cn } from "@/lib/utils";

const HEIGHT = 68;
const BODY_WIDTH = 236;
const STUB_WIDTH = 76;
// How far (deg) the stub swings, about the foot of the perforation, before
// the last fibre gives.
const TEAR_ANGLE = 34;
// The perforation tears hole by hole: each one holds for a moment, then
// gives. That stick-slip is what makes it feel like paper.
const HOLES = 7;
const HOLD = 0.55;
const HOLD_SHARE = 0.3;
// The stub's swing chases the hand on a stiff spring, so the slips snap.
const SWING_STIFFNESS = 900;
const SWING_DAMPING = 42;
const RETURN_STIFFNESS = 520;
const RETURN_DAMPING = 22;
// Once free: px and seconds.
const GRAVITY = 2600;
const FALL_DISTANCE = 220;
const STEP = 1 / 120;
// Ragged edge of the tear, as offsets in px along the perforation.
const FIBRES = [
  1.6, 0.4, 2.2, 0.9, 1.8, 0.2, 2.4, 1.1, 0.5, 2, 1.4, 0.3, 2.3, 0.8, 1.7, 0.1,
  2.1, 1.2, 0.6, 1.9,
];
const EASE_OUT = "cubic-bezier(0.23, 1, 0.32, 1)";

type Phase = "idle" | "tearing" | "flying" | "torn";

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function haptic(ms: number) {
  if (typeof navigator !== "undefined" && "vibrate" in navigator) {
    navigator.vibrate(ms);
  }
}

/** Raw pull (0..1+) to how far the tear has run, holding at each hole. */
function stickSlip(pull: number) {
  const scaled = clamp(pull, 0, 1) * HOLES;
  const hole = Math.min(Math.floor(scaled), HOLES - 1);
  const f = scaled - hole;
  const given =
    f < HOLD
      ? (f / HOLD) * HOLD_SHARE
      : HOLD_SHARE + ((f - HOLD) / (1 - HOLD)) * (1 - HOLD_SHARE);
  return (hole + given) / HOLES;
}

/**
 * One side of the tear as a clip-path: ragged down to `tip` (px from the
 * top), straight below. `side` is 1 for the body's right edge, -1 for the
 * stub's left edge.
 */
function tornEdge(tip: number, width: number, side: 1 | -1) {
  const points: string[] = [];
  const edge = side === 1 ? width : 0;
  const far = side === 1 ? 0 : width;
  points.push(`${far}px 0px`);
  const rows = Math.ceil((tip / HEIGHT) * FIBRES.length);
  for (let k = 0; k <= rows; k++) {
    const y = Math.min((k / FIBRES.length) * HEIGHT, tip);
    const x = edge - side * FIBRES[k % FIBRES.length];
    points.push(`${x.toFixed(1)}px ${y.toFixed(1)}px`);
  }
  points.push(`${edge}px ${tip.toFixed(1)}px`, `${edge}px ${HEIGHT}px`);
  points.push(`${far}px ${HEIGHT}px`);
  return `polygon(${points.join(",")})`;
}

// Ticket notches, cut where the perforation meets the top and bottom edges.
const NOTCH = 7;
const bodyMask = `radial-gradient(circle at 100% 0, #0000 ${NOTCH}px, #000 ${NOTCH + 0.5}px) top / 100% 51% no-repeat, radial-gradient(circle at 100% 100%, #0000 ${NOTCH}px, #000 ${NOTCH + 0.5}px) bottom / 100% 51% no-repeat`;
const stubMask = `radial-gradient(circle at 0 0, #0000 ${NOTCH}px, #000 ${NOTCH + 0.5}px) top / 100% 51% no-repeat, radial-gradient(circle at 0 100%, #0000 ${NOTCH}px, #000 ${NOTCH + 0.5}px) bottom / 100% 51% no-repeat`;

const TONES = {
  default: {
    paper: "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900",
    chip: "bg-white/10 dark:bg-zinc-900/10",
    chipDone: "bg-white text-zinc-900 dark:bg-zinc-900 dark:text-white",
    sub: "text-white/55 dark:text-zinc-900/55",
    stub: "text-white/60 dark:text-zinc-900/60",
    perforation: "border-white/30 dark:border-zinc-900/30",
  },
  muted: {
    paper: "bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100",
    chip: "bg-zinc-900/5 dark:bg-white/10",
    chipDone: "bg-zinc-900 text-white dark:bg-white dark:text-zinc-900",
    sub: "text-zinc-500 dark:text-zinc-400",
    stub: "text-zinc-500 dark:text-zinc-400",
    perforation: "border-zinc-900/20 dark:border-white/25",
  },
  danger: {
    paper: "bg-red-600 text-white",
    chip: "bg-white/15",
    chipDone: "bg-white text-red-600",
    sub: "text-white/70",
    stub: "text-white/75",
    perforation: "border-white/40",
  },
} as const;

const TRASH = (
  <svg
    aria-hidden="true"
    className="size-4"
    fill="none"
    stroke="currentColor"
    strokeLinecap="round"
    strokeLinejoin="round"
    strokeWidth={2}
    viewBox="0 0 24 24"
  >
    <path d="M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3" />
  </svg>
);

export interface TearButtonProps {
  /** Paper colour. */
  tone?: keyof typeof TONES;
  /** Shown in the round chip until the stub is torn; a trash can by default. */
  icon?: ReactNode;
  label?: string;
  description?: string;
  stubLabel?: string;
  confirmedLabel?: string;
  confirmedDescription?: string;
  /** Called once the stub comes away. */
  onConfirm?: () => void;
  /** Print a fresh stub this many ms after confirming. Off when 0. */
  resetAfter?: number;
  className?: string;
}

export default function TearButton({
  tone = "default",
  icon = TRASH,
  label = "Delete workspace",
  description = "Tear off the stub to confirm",
  stubLabel = "Tear",
  confirmedLabel = "Workspace deleted",
  confirmedDescription = "Kept in trash for 30 days",
  onConfirm,
  resetAfter = 2400,
  className,
}: TearButtonProps) {
  const [phase, setPhase] = useState<Phase>("idle");
  const [generation, setGeneration] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);
  const bodyRef = useRef<HTMLDivElement>(null);
  const stubRef = useRef<HTMLButtonElement>(null);
  // Set when the stub tears away with focus on it, so focus can follow.
  const hadFocusRef = useRef(false);

  const sim = useRef({
    angle: 0,
    speed: 0,
    target: 0,
    held: false,
    grabAngle: 0,
    hole: 0,
    raf: 0,
    last: 0,
    carry: 0,
    // Free flight, once torn.
    x: 0,
    y: 0,
    vx: 0,
    vy: 0,
    spin: 0,
    // The hand's recent movement, for the throw.
    px: 0,
    py: 0,
    pt: 0,
    pvx: 0,
    pvy: 0,
    auto: 0,
    flying: false,
    reduceMotion: false,
  });

  const latest = useRef({ onConfirm });
  latest.current = { onConfirm };

  const hinge = useCallback(() => {
    const rect = rootRef.current?.getBoundingClientRect();
    return rect
      ? { x: rect.left + BODY_WIDTH, y: rect.top + HEIGHT }
      : { x: 0, y: 0 };
  }, []);

  const drawTear = useCallback(() => {
    const s = sim.current;
    const tip = clamp(s.angle / TEAR_ANGLE, 0, 1) * HEIGHT;
    const stub = stubRef.current;
    const body = bodyRef.current;
    if (body) {
      body.style.clipPath = tip > 0.5 ? tornEdge(tip, BODY_WIDTH, 1) : "";
    }
    if (stub) {
      stub.style.clipPath = tip > 0.5 ? tornEdge(tip, STUB_WIDTH, -1) : "";
      stub.style.transform = `rotate(${s.angle.toFixed(2)}deg)`;
    }
  }, []);

  const drawFlight = useCallback(() => {
    const s = sim.current;
    const stub = stubRef.current;
    if (!stub) {
      return;
    }
    const fade = clamp(
      1 - (s.y - FALL_DISTANCE * 0.5) / (FALL_DISTANCE * 0.5),
      0,
      1
    );
    stub.style.transform = `translate(${s.x.toFixed(1)}px, ${s.y.toFixed(1)}px) rotate(${s.angle.toFixed(1)}deg)`;
    stub.style.opacity = fade.toFixed(3);
  }, []);

  const detach = useCallback(() => {
    const s = sim.current;
    s.held = false;
    haptic(14);
    // The stub leaves with the swing it had, plus the hand's throw.
    const rad = (s.angle * Math.PI) / 180;
    const tangential = ((s.speed * Math.PI) / 180) * HEIGHT;
    s.x = 0;
    s.y = 0;
    s.vx = Math.cos(rad) * tangential + clamp(s.pvx, -900, 900) * 0.6 + 90;
    s.vy = Math.sin(rad) * tangential + clamp(s.pvy, -900, 900) * 0.6 - 120;
    s.spin = s.speed * 0.6 + 140;
    s.flying = true;
    if (stubRef.current) {
      stubRef.current.style.transition = "none";
      hadFocusRef.current = document.activeElement === stubRef.current;
    }
    setPhase("flying");
    latest.current.onConfirm?.();
  }, []);

  // While attached: the swing chases its target, held or springing home.
  const stepSwing = useCallback(() => {
    const s = sim.current;
    const stiffness = s.held || s.auto ? SWING_STIFFNESS : RETURN_STIFFNESS;
    const damping = s.held || s.auto ? SWING_DAMPING : RETURN_DAMPING;
    const force = (s.target - s.angle) * stiffness - s.speed * damping;
    s.speed += force * STEP;
    s.angle += s.speed * STEP;
    // A tick for each hole that gives.
    const hole = Math.floor(clamp(s.angle / TEAR_ANGLE, 0, 1) * HOLES);
    if (hole > s.hole) {
      haptic(3);
    }
    s.hole = hole;
    return s.angle >= TEAR_ANGLE * 0.995;
  }, []);

  const stepFlight = useCallback(() => {
    const s = sim.current;
    s.vy += GRAVITY * STEP;
    s.x += s.vx * STEP;
    s.y += s.vy * STEP;
    s.angle += s.spin * STEP;
    return s.y > FALL_DISTANCE;
  }, []);

  // Runs the fixed steps owed since the last frame; true once this phase is
  // over (the stub tore through, or it has fallen out of sight).
  const advance = useCallback(
    (elapsed: number) => {
      const s = sim.current;
      s.carry += elapsed;
      let finished = false;
      while (s.carry >= STEP && !finished) {
        s.carry -= STEP;
        if (s.flying) {
          finished = stepFlight();
        } else if (s.auto) {
          s.target = Math.min(s.target + STEP * 60, TEAR_ANGLE * 1.05);
          finished = stepSwing();
        } else {
          finished = stepSwing() && s.held;
        }
      }
      return finished;
    },
    [stepFlight, stepSwing]
  );

  const tick = useCallback(
    (now: number) => {
      const s = sim.current;
      s.raf = 0;
      const elapsed = s.last ? Math.min((now - s.last) / 1000, 0.05) : STEP;
      s.last = now;
      const { flying } = s;
      const finished = advance(elapsed);
      if (flying) {
        drawFlight();
        if (finished) {
          s.last = 0;
          setPhase("torn");
          return;
        }
        s.raf = requestAnimationFrame(tick);
        return;
      }
      drawTear();
      if (finished) {
        s.auto = 0;
        detach();
        s.raf = requestAnimationFrame(tick);
        return;
      }
      const resting =
        !(s.held || s.auto) &&
        Math.abs(s.angle) < 0.02 &&
        Math.abs(s.speed) < 0.05;
      if (!resting) {
        s.raf = requestAnimationFrame(tick);
        return;
      }
      s.angle = 0;
      s.speed = 0;
      s.last = 0;
      drawTear();
      setPhase("idle");
    },
    [advance, detach, drawFlight, drawTear]
  );

  const wake = useCallback(() => {
    const s = sim.current;
    if (!s.raf) {
      s.raf = requestAnimationFrame(tick);
    }
  }, [tick]);

  useEffect(() => {
    const s = sim.current;
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    s.reduceMotion = query.matches;
    const onChange = () => {
      s.reduceMotion = query.matches;
    };
    query.addEventListener("change", onChange);
    return () => {
      query.removeEventListener("change", onChange);
      cancelAnimationFrame(s.raf);
      s.raf = 0;
    };
  }, []);

  // A fresh stub, a little while after the last one came away.
  useEffect(() => {
    if (phase !== "torn" || !resetAfter) {
      return;
    }
    const id = window.setTimeout(() => {
      const s = sim.current;
      s.angle = 0;
      s.speed = 0;
      s.target = 0;
      s.hole = 0;
      s.flying = false;
      setGeneration((g) => g + 1);
      setPhase("idle");
    }, resetAfter);
    return () => window.clearTimeout(id);
  }, [phase, resetAfter]);

  // The torn stub unmounts, which would drop focus on <body>. Hold it on the
  // ticket meanwhile and hand it to the fresh stub when one is printed.
  useEffect(() => {
    if (!hadFocusRef.current) {
      return;
    }
    if (phase === "torn") {
      rootRef.current?.focus({ preventScroll: true });
    } else if (phase === "idle") {
      hadFocusRef.current = false;
      stubRef.current?.focus({ preventScroll: true });
    }
  }, [phase]);

  // In reduced motion the stub doesn't fall: it fades where it tore.
  useEffect(() => {
    if (phase !== "flying" || !sim.current.reduceMotion) {
      return;
    }
    const s = sim.current;
    cancelAnimationFrame(s.raf);
    s.raf = 0;
    s.last = 0;
    const stub = stubRef.current;
    if (stub) {
      stub.style.transition = `opacity 180ms ${EASE_OUT}`;
      stub.style.opacity = "0";
    }
    const id = window.setTimeout(() => setPhase("torn"), 180);
    return () => window.clearTimeout(id);
  }, [phase]);

  const handlePointerDown = (event: PointerEvent<HTMLButtonElement>) => {
    const s = sim.current;
    if (phase === "flying" || phase === "torn") {
      return;
    }
    if (event.pointerType === "mouse" && event.button !== 0) {
      return;
    }
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    const h = hinge();
    s.grabAngle =
      (Math.atan2(event.clientY - h.y, event.clientX - h.x) * 180) / Math.PI -
      s.angle;
    s.held = true;
    s.px = event.clientX;
    s.py = event.clientY;
    s.pt = event.timeStamp;
    s.pvx = 0;
    s.pvy = 0;
    setPhase("tearing");
    wake();
  };

  const handlePointerMove = (event: PointerEvent<HTMLButtonElement>) => {
    const s = sim.current;
    if (!s.held) {
      return;
    }
    const dt = event.timeStamp - s.pt;
    if (dt > 0) {
      s.pvx = s.pvx * 0.5 + ((event.clientX - s.px) / dt) * 500;
      s.pvy = s.pvy * 0.5 + ((event.clientY - s.py) / dt) * 500;
    }
    s.px = event.clientX;
    s.py = event.clientY;
    s.pt = event.timeStamp;
    const h = hinge();
    const raw =
      (Math.atan2(event.clientY - h.y, event.clientX - h.x) * 180) / Math.PI -
      s.grabAngle;
    // Pushing the stub back into the body only gives a little.
    const pull = raw < 0 ? raw * 0.15 : raw;
    s.target =
      pull >= TEAR_ANGLE
        ? TEAR_ANGLE * 1.05
        : stickSlip(Math.max(pull, 0) / TEAR_ANGLE) * TEAR_ANGLE +
          Math.min(pull, 0);
    wake();
  };

  const handlePointerUp = () => {
    const s = sim.current;
    if (!s.held) {
      return;
    }
    s.held = false;
    // Not torn through: the stub swings back and the perforation holds.
    s.target = 0;
    wake();
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (event.key !== "Enter" && event.key !== " ") {
      return;
    }
    event.preventDefault();
    const s = sim.current;
    if (phase !== "idle" || event.repeat) {
      return;
    }
    // From the keyboard the stub tears itself, hole by hole.
    s.auto = 1;
    s.target = 0;
    setPhase("tearing");
    wake();
  };

  const done = phase === "flying" || phase === "torn";
  const colors = TONES[tone];

  return (
    <div
      className={cn("relative select-none outline-none", className)}
      ref={rootRef}
      style={{ width: BODY_WIDTH + STUB_WIDTH, height: HEIGHT }}
      tabIndex={-1}
    >
      <div
        className={cn(
          "absolute inset-y-0 left-0 flex items-center gap-3 rounded-l-2xl px-4",
          colors.paper
        )}
        ref={bodyRef}
        style={{ width: BODY_WIDTH, mask: bodyMask, WebkitMask: bodyMask }}
      >
        <span
          aria-hidden="true"
          className={cn(
            "flex size-9 shrink-0 items-center justify-center rounded-full transition-colors duration-300 ease-out",
            done ? colors.chipDone : colors.chip
          )}
        >
          {done ? (
            <svg
              aria-hidden="true"
              className="size-4"
              fill="none"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              viewBox="0 0 24 24"
            >
              <path d="M5 12.5l4.5 4.5L19 7.5" />
            </svg>
          ) : (
            icon
          )}
        </span>
        <span className="relative grid min-w-0 flex-1">
          {[
            { key: "ask", on: !done, title: label, sub: description },
            {
              key: "done",
              on: done,
              title: confirmedLabel,
              sub: confirmedDescription,
            },
          ].map((line) => (
            <span
              aria-hidden={!line.on}
              className={cn(
                "col-start-1 row-start-1 min-w-0 transition-[opacity,filter,translate] duration-300 ease-[cubic-bezier(0.23,1,0.32,1)] motion-reduce:transition-opacity",
                line.on
                  ? "translate-y-0 opacity-100 blur-0"
                  : "pointer-events-none translate-y-1 opacity-0 blur-[3px]"
              )}
              key={line.key}
            >
              <span className="block truncate font-medium text-sm">
                {line.title}
              </span>
              <span className={cn("block truncate text-xs", colors.sub)}>
                {line.sub}
              </span>
            </span>
          ))}
        </span>
      </div>

      {phase === "torn" ? null : (
        <button
          aria-label={`${label}: ${description}`}
          className={cn(
            "absolute inset-y-0 flex touch-none flex-col items-center justify-center gap-1 rounded-r-2xl outline-none",
            // The notch mask clips anything drawn outside the stub, so the
            // focus ring sits inside it.
            "focus-visible:ring-2 focus-visible:ring-current focus-visible:ring-inset",
            colors.paper,
            colors.stub,
            "starting:-translate-x-3 starting:opacity-0 transition-[opacity,translate] duration-300 ease-[cubic-bezier(0.23,1,0.32,1)]",
            phase === "tearing" ? "cursor-grabbing" : "cursor-grab"
          )}
          // A new stub per tear, so no inline style from the last one lingers.
          key={generation}
          onKeyDown={handleKeyDown}
          onLostPointerCapture={handlePointerUp}
          onPointerCancel={handlePointerUp}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          ref={stubRef}
          style={{
            left: BODY_WIDTH,
            width: STUB_WIDTH,
            mask: stubMask,
            WebkitMask: stubMask,
            transformOrigin: "0 100%",
          }}
          type="button"
        >
          {/* The perforation. */}
          <span
            aria-hidden="true"
            className={cn(
              "absolute inset-y-2.5 left-0 border-l-2 border-dotted",
              colors.perforation
            )}
          />
          <svg
            aria-hidden="true"
            className="size-4"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            viewBox="0 0 24 24"
          >
            <path d="M6 6c6 0 12 3 12 11M14 14l4 4 4-4" />
          </svg>
          <span className="font-medium text-xs">{stubLabel}</span>
        </button>
      )}
      <span aria-live="polite" className="sr-only">
        {done ? confirmedLabel : ""}
      </span>
    </div>
  );
}


### A6. stack-panel.tsx
"use client";

/**
 * @author: @dorianbaffier
 * @description: Stack Panel
 * @version: 1.0.0
 * @date: 2026-10-16
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import { Check, ChevronLeft, ChevronRight } from "lucide-react";
import {
  AnimatePresence,
  animate,
  MotionConfig,
  motion,
  type PanInfo,
  useAnimate,
  useMotionValue,
  useReducedMotion,
  useTransform,
} from "motion/react";
import {
  type KeyboardEvent,
  type ReactNode,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { flushSync } from "react-dom";
import { cn } from "@/lib/utils";

export interface StackRow {
  id: string;
  label: string;
  /** Grey text on the right, like the current value of a setting. */
  detail?: string;
  /** Id of the page this row opens. */
  to?: string;
  /** Anything else on the right: a switch, a badge. */
  trailing?: ReactNode;
  onSelect?: () => void;
  /** Shows a check, for rows that pick one option of a section. */
  selected?: boolean;
  /** Shows a switch in this state, for rows that turn something on. */
  checked?: boolean;
  tone?: "default" | "danger";
}

export interface StackSection {
  title?: string;
  rows: StackRow[];
  /** Small print under the section. */
  footer?: string;
}

export interface StackPage {
  title: string;
  sections: StackSection[];
}

export interface StackPanelProps {
  pages: Record<string, StackPage>;
  /** The page the panel starts on. */
  root: string;
  className?: string;
}

// iOS pushes with a curve close to this; the page settles without bounce.
const PUSH = { type: "spring", duration: 0.42, bounce: 0 } as const;
const HEIGHT = { type: "spring", duration: 0.38, bounce: 0 } as const;
const EASE_OUT = [0.23, 1, 0.32, 1] as const;
// The page underneath moves a third as far as the one on top.
const PARALLAX = 0.3;
// Let go past this share of the width, or flick faster, and the page pops.
const POP_DISTANCE = 0.35;
const POP_VELOCITY = 500;

interface Origin {
  /** The tapped row's label, relative to the panel. */
  left: number;
  top: number;
  height: number;
  fontSize: number;
}

/**
 * The header title. Pushed in from a row, it flies from that row's label
 * up into the header; otherwise it slides a little and fades.
 */
function Title({
  text,
  origin,
  direction,
  panel,
}: {
  text: string;
  origin: Origin | null;
  direction: 1 | -1;
  panel: HTMLElement | null;
}) {
  const [scope, run] = useAnimate<HTMLSpanElement>();
  const reduced = useReducedMotion();

  useLayoutEffect(() => {
    const el = scope.current;
    if (!(el && panel)) {
      return;
    }
    if (!origin || reduced) {
      run(
        el,
        { opacity: [0, 1], x: [direction * 24, 0] },
        { duration: reduced ? 0 : 0.28, ease: EASE_OUT }
      );
      return;
    }
    const box = panel.getBoundingClientRect();
    const own = el.getBoundingClientRect();
    const fontSize = Number.parseFloat(getComputedStyle(el).fontSize);
    const scale = origin.fontSize / fontSize;
    const dx = origin.left - (own.left - box.left);
    const dy =
      origin.top + origin.height / 2 - (own.top - box.top + own.height / 2);
    // Placed at the row before the first frame, so it never flashes in the
    // header first.
    el.style.transform = `translateX(${dx}px) translateY(${dy}px) scale(${scale})`;
    run(
      el,
      { x: [dx, 0], y: [dy, 0], scale: [scale, 1], opacity: [1, 1] },
      PUSH
    );
  }, [scope, run, origin, direction, panel, reduced]);

  return (
    <motion.span
      className="absolute inset-x-0 top-1/2 mx-auto w-fit max-w-[55%] -translate-y-1/2 truncate font-semibold text-[15px] text-zinc-900 dark:text-zinc-100"
      exit={{
        opacity: 0,
        x: direction * -40,
        transition: { duration: 0.18, ease: EASE_OUT },
      }}
      ref={scope}
      style={{ transformOrigin: "left center" }}
    >
      {text}
    </motion.span>
  );
}

function Page({
  page,
  onOpen,
  measureRef,
}: {
  page: StackPage;
  onOpen: (row: StackRow, label: HTMLElement) => void;
  measureRef?: (el: HTMLDivElement | null) => void;
}) {
  return (
    <div className="flex flex-col gap-5 px-3 pt-2 pb-4" ref={measureRef}>
      {page.sections.map((section, index) => (
        // biome-ignore lint/suspicious/noArrayIndexKey: sections are a fixed, ordered list
        <section key={index}>
          {section.title && (
            <h3 className="mb-1.5 px-3 font-medium text-xs text-zinc-500 dark:text-zinc-400">
              {section.title}
            </h3>
          )}
          <ul className="divide-y divide-zinc-100 overflow-hidden rounded-xl bg-zinc-50 dark:divide-zinc-800/70 dark:bg-zinc-900">
            {section.rows.map((row) => (
              <li key={row.id}>
                <RowView onOpen={onOpen} row={row} />
              </li>
            ))}
          </ul>
          {section.footer && (
            <p className="mt-1.5 px-3 text-xs text-zinc-400 leading-5 dark:text-zinc-500">
              {section.footer}
            </p>
          )}
        </section>
      ))}
    </div>
  );
}

function RowView({
  row,
  onOpen,
}: {
  row: StackRow;
  onOpen: (row: StackRow, label: HTMLElement) => void;
}) {
  const labelRef = useRef<HTMLSpanElement>(null);
  const interactive = Boolean(row.to || row.onSelect);
  const content = (
    <>
      <span
        className={cn(
          "min-w-0 flex-1 truncate text-left text-[15px]",
          row.tone === "danger"
            ? "text-red-600 dark:text-red-400"
            : "text-zinc-900 dark:text-zinc-100"
        )}
        ref={labelRef}
      >
        {row.label}
      </span>
      {row.detail && (
        <span className="max-w-[45%] shrink-0 truncate text-sm text-zinc-500 dark:text-zinc-400">
          {row.detail}
        </span>
      )}
      {row.trailing}
      {row.checked !== undefined && <Toggle on={row.checked} />}
      {row.selected !== undefined && (
        <Check
          aria-hidden="true"
          className={cn(
            "size-4 shrink-0 text-zinc-900 dark:text-zinc-100",
            !row.selected && "invisible"
          )}
        />
      )}
      {row.to && (
        <ChevronRight
          aria-hidden="true"
          className="-mr-1 size-4 shrink-0 text-zinc-400 dark:text-zinc-500"
        />
      )}
    </>
  );
  const base = "flex min-h-11 w-full items-center gap-2 px-3 py-2";
  if (!interactive) {
    return <div className={base}>{content}</div>;
  }
  const className = cn(
    base,
    "outline-none transition-colors duration-150 hover:bg-zinc-100 focus-visible:bg-zinc-100 active:bg-zinc-100 dark:active:bg-zinc-800 dark:focus-visible:bg-zinc-800 dark:hover:bg-zinc-800/60"
  );
  const onClick = () => {
    if (labelRef.current) {
      onOpen(row, labelRef.current);
    }
  };
  if (row.checked !== undefined) {
    return (
      <button
        aria-checked={row.checked}
        className={className}
        data-row={row.id}
        onClick={onClick}
        role="switch"
        type="button"
      >
        {content}
      </button>
    );
  }
  return (
    <button
      aria-pressed={row.selected}
      className={className}
      data-row={row.id}
      onClick={onClick}
      type="button"
    >
      {content}
    </button>
  );
}

/**
 * A panel that drills into pages, like settings on a phone. A row opens its
 * page from the right while its label flies up to become the title; swipe
 * the page right, or press Back or Escape, to return.
 */
export function StackPanel({ pages, root, className }: StackPanelProps) {
  const [stack, setStack] = useState<string[]>([root]);
  const [origin, setOrigin] = useState<Origin | null>(null);
  const [direction, setDirection] = useState<1 | -1>(1);
  // "auto" until the first page is measured, so the server render has a
  // height too.
  const [height, setHeight] = useState<number | "auto">("auto");
  const [busy, setBusy] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const bodyRef = useRef<HTMLDivElement>(null);
  const backRef = useRef<HTMLButtonElement>(null);
  const observer = useRef<ResizeObserver | null>(null);
  // The row that opened each page, so Back can hand focus to it again.
  const openers = useRef<string[]>([]);
  // The top page's offset; the page underneath is derived from it, so a
  // push, a pop and a swipe all move both pages together.
  const x = useMotionValue(0);
  const [width, setWidth] = useState(360);
  const underX = useTransform(x, (v) => -PARALLAX * (width - v));
  const shade = useTransform(x, [0, width], [0.06, 0]);

  const top = stack.at(-1) ?? root;
  const under = stack.length > 1 ? stack.at(-2) : undefined;
  const parent = under ? pages[under] : undefined;

  useEffect(() => {
    const body = bodyRef.current;
    if (!body) {
      return;
    }
    const watch = new ResizeObserver(() => setWidth(body.clientWidth));
    watch.observe(body);
    return () => {
      watch.disconnect();
      observer.current?.disconnect();
    };
  }, []);

  // The panel's height follows the page on top.
  const measure = (el: HTMLDivElement | null) => {
    observer.current?.disconnect();
    if (!el) {
      return;
    }
    observer.current = new ResizeObserver(() => setHeight(el.offsetHeight));
    observer.current.observe(el);
  };

  const push = (row: StackRow, label: HTMLElement) => {
    row.onSelect?.();
    if (!(row.to && pages[row.to]) || busy) {
      return;
    }
    const box = panelRef.current?.getBoundingClientRect();
    const rect = label.getBoundingClientRect();
    setOrigin(
      box
        ? {
            left: rect.left - box.left,
            top: rect.top - box.top,
            height: rect.height,
            fontSize: Number.parseFloat(getComputedStyle(label).fontSize),
          }
        : null
    );
    setDirection(1);
    setBusy(true);
    x.set(width);
    const to = row.to;
    openers.current.push(row.id);
    flushSync(() => setStack((s) => [...s, to]));
    animate(x, 0, PUSH).then(() => setBusy(false));
    backRef.current?.focus({ preventScroll: true });
  };

  const pop = () => {
    if (stack.length < 2 || busy) {
      return;
    }
    setOrigin(null);
    setDirection(-1);
    setBusy(true);
    const opener = openers.current.pop();
    const hadFocus = panelRef.current?.contains(document.activeElement);
    animate(x, width, { ...PUSH, duration: 0.34 }).then(() => {
      // Both in one frame: the old top unmounts as the offset resets.
      flushSync(() => setStack((s) => s.slice(0, -1)));
      x.set(0);
      setBusy(false);
      // The focused Back button or row just unmounted; land on the row that
      // opened the page instead of dropping focus to the document.
      if (hadFocus && opener) {
        const rows = bodyRef.current?.querySelectorAll<HTMLElement>(
          `[data-row="${CSS.escape(opener)}"]`
        );
        const row = [...(rows ?? [])].find((el) => !el.closest("[inert]"));
        row?.focus({ preventScroll: true });
      }
    });
  };

  const onDragEnd = (_: unknown, info: PanInfo) => {
    if (x.get() > width * POP_DISTANCE || info.velocity.x > POP_VELOCITY) {
      pop();
    } else {
      animate(x, 0, PUSH);
    }
  };

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === "Escape" && stack.length > 1) {
      event.preventDefault();
      pop();
    }
  };

  return (
    <MotionConfig reducedMotion="user">
      {/* biome-ignore lint/a11y/noStaticElementInteractions lint/a11y/noNoninteractiveElementInteractions: Escape is a shortcut for the Back button */}
      <div
        className={cn(
          "relative w-full max-w-sm overflow-hidden rounded-2xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950",
          className
        )}
        onKeyDown={onKeyDown}
        ref={panelRef}
      >
        <div className="relative z-10 flex h-12 items-center border-zinc-100 border-b bg-white px-2 dark:border-zinc-900 dark:bg-zinc-950">
          <AnimatePresence initial={false}>
            {parent && (
              <motion.button
                animate={{ opacity: 1 }}
                className="relative z-10 flex max-w-[30%] items-center rounded-lg py-1.5 pr-2 pl-1 text-sm text-zinc-600 outline-none transition-colors duration-150 hover:bg-zinc-100 hover:text-zinc-900 focus-visible:ring-2 focus-visible:ring-zinc-400 dark:text-zinc-400 dark:hover:bg-zinc-900 dark:hover:text-zinc-100"
                exit={{ opacity: 0 }}
                initial={{ opacity: 0 }}
                key="back"
                onClick={pop}
                ref={backRef}
                transition={{ duration: 0.18 }}
                type="button"
              >
                <ChevronLeft aria-hidden="true" className="size-4 shrink-0" />
                <span className="truncate">{parent.title}</span>
              </motion.button>
            )}
          </AnimatePresence>
          <AnimatePresence initial={false}>
            <Title
              direction={direction}
              key={`${top}-${stack.length}`}
              origin={origin}
              panel={panelRef.current}
              text={pages[top]?.title ?? ""}
            />
          </AnimatePresence>
        </div>

        <motion.div
          animate={{ height }}
          className="relative"
          ref={bodyRef}
          transition={HEIGHT}
        >
          {under && pages[under] && (
            <motion.div
              aria-hidden="true"
              className="pointer-events-none absolute inset-x-0 top-0"
              inert
              key={`${under}-${stack.length - 1}`}
              style={{ x: underX }}
            >
              <Page onOpen={() => undefined} page={pages[under]} />
              <motion.div
                className="absolute inset-0 bg-black"
                style={{ opacity: shade }}
              />
            </motion.div>
          )}
          {pages[top] && (
            <motion.div
              className="relative bg-white dark:bg-zinc-950"
              drag={stack.length > 1 && !busy ? "x" : false}
              dragConstraints={{ left: 0, right: 0 }}
              dragDirectionLock
              dragElastic={{ left: 0, right: 1 }}
              dragMomentum={false}
              key={`${top}-${stack.length}`}
              onDragEnd={onDragEnd}
              style={{
                x,
                boxShadow:
                  stack.length > 1 ? "-8px 0 24px -12px rgb(0 0 0 / 0.18)" : "",
              }}
            >
              <Page measureRef={measure} onOpen={push} page={pages[top]} />
            </motion.div>
          )}
        </motion.div>
      </div>
    </MotionConfig>
  );
}

function Toggle({ on }: { on: boolean }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "relative h-[22px] w-9 shrink-0 rounded-full transition-colors duration-200",
        on ? "bg-zinc-900 dark:bg-zinc-100" : "bg-zinc-200 dark:bg-zinc-700"
      )}
    >
      <span
        className={cn(
          "absolute top-0.5 left-0.5 size-[18px] rounded-full bg-white shadow-sm transition-transform duration-200 ease-[cubic-bezier(0.23,1,0.32,1)] dark:bg-zinc-950",
          on && "translate-x-3.5"
        )}
      />
    </span>
  );
}

/** Workspace settings, three levels deep. */
export default function StackPanelDemo({ className }: { className?: string }) {
  const [theme, setTheme] = useState("System");
  const [density, setDensity] = useState("Comfortable");
  const [alerts, setAlerts] = useState({
    mentions: true,
    digest: false,
    deploys: true,
  });
  const [quiet, setQuiet] = useState("22:00 – 07:00");

  const flip = (key: keyof typeof alerts) => () =>
    setAlerts((a) => ({ ...a, [key]: !a[key] }));
  const toggleRow = (id: keyof typeof alerts, label: string): StackRow => ({
    id,
    label,
    onSelect: flip(id),
    checked: alerts[id],
  });
  const choice = (
    value: string,
    current: string,
    set: (v: string) => void
  ): StackRow => ({
    id: value,
    label: value,
    selected: value === current,
    onSelect: () => set(value),
  });

  const pages: Record<string, StackPage> = {
    settings: {
      title: "Settings",
      sections: [
        {
          rows: [
            {
              id: "account",
              label: "Account",
              detail: "Maya Chen",
              to: "account",
            },
            {
              id: "appearance",
              label: "Appearance",
              detail: theme,
              to: "appearance",
            },
            {
              id: "notifications",
              label: "Notifications",
              to: "notifications",
            },
          ],
        },
        {
          rows: [
            { id: "billing", label: "Billing", detail: "Pro", to: "billing" },
            { id: "shortcuts", label: "Keyboard shortcuts", detail: "34" },
          ],
        },
      ],
    },
    account: {
      title: "Account",
      sections: [
        {
          rows: [
            { id: "name", label: "Name", detail: "Maya Chen" },
            { id: "email", label: "Email", detail: "maya@acme.com" },
            { id: "security", label: "Password & security", to: "security" },
          ],
        },
        {
          rows: [
            {
              id: "sign-out",
              label: "Sign out",
              tone: "danger",
              onSelect: () => undefined,
            },
          ],
        },
      ],
    },
    security: {
      title: "Password & security",
      sections: [
        {
          rows: [
            {
              id: "password",
              label: "Password",
              detail: "Changed 3 months ago",
            },
            { id: "2fa", label: "Two-factor", detail: "Authenticator app" },
            { id: "sessions", label: "Active sessions", detail: "3" },
          ],
          footer: "Signing out of a session takes effect on its next request.",
        },
      ],
    },
    appearance: {
      title: "Appearance",
      sections: [
        {
          title: "Theme",
          rows: ["Light", "Dark", "System"].map((v) =>
            choice(v, theme, setTheme)
          ),
        },
        {
          title: "Density",
          rows: ["Comfortable", "Compact"].map((v) =>
            choice(v, density, setDensity)
          ),
        },
      ],
    },
    notifications: {
      title: "Notifications",
      sections: [
        {
          rows: [
            toggleRow("mentions", "Mentions"),
            toggleRow("deploys", "Failed deploys"),
            toggleRow("digest", "Weekly digest"),
          ],
        },
        {
          rows: [
            { id: "quiet", label: "Quiet hours", detail: quiet, to: "quiet" },
          ],
          footer: "Nothing but failed deploys gets through in quiet hours.",
        },
      ],
    },
    quiet: {
      title: "Quiet hours",
      sections: [
        {
          rows: ["Off", "22:00 – 07:00", "20:00 – 09:00", "Weekends"].map((v) =>
            choice(v, quiet, setQuiet)
          ),
        },
      ],
    },
    billing: {
      title: "Billing",
      sections: [
        {
          rows: [
            { id: "plan", label: "Plan", detail: "Pro, yearly" },
            { id: "next", label: "Next invoice", detail: "Nov 1 · $96" },
            { id: "card", label: "Card", detail: "Visa ·· 4242" },
          ],
        },
      ],
    },
  };

  return <StackPanel className={className} pages={pages} root="settings" />;
}


### A7. spoiler-text.tsx
"use client";

/**
 * @author: @dorianbaffier
 * @description: Spoiler Text
 * @version: 1.0.0
 * @date: 2026-10-16
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import {
  type KeyboardEvent,
  type MouseEvent,
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { cn } from "@/lib/utils";

export interface SpoilerProps {
  children: ReactNode;
  /** Controlled state. Leave it out and the spoiler keeps its own. */
  revealed?: boolean;
  defaultRevealed?: boolean;
  onRevealedChange?: (revealed: boolean) => void;
  /** What screen readers hear while the text is hidden. */
  label?: string;
  className?: string;
}

// Specks per square pixel of hidden text. About one every 6px², like the
// grain on a scratch card, so even one short word reads as a solid patch.
const DENSITY = 1 / 6;
const MAX_SPECKS = 2400;
// How far a speck wanders, in px per second, while it shimmers in place.
const DRIFT = 7;
// The reveal wave crosses the whole spoiler in this long, whatever its size.
const WAVE_MS = 520;
// Once the wave passes a speck it is thrown out and fades over this long.
const FADE_MS = 360;
const THROW = 70;

interface Speck {
  x: number;
  y: number;
  vx: number;
  vy: number;
  /** Where in its twinkle the speck is, 0 to 1. */
  phase: number;
  /** Twinkles per second. */
  rate: number;
  size: number;
  /** The rectangle (one per line) the speck is kept inside. */
  box: number;
  /** When the reveal wave reached it, in ms; 0 while hidden. */
  hit: number;
}

interface Box {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

/** How far the wave has to travel from where it started to clear it all. */
function farthestCorner(at: { x: number; y: number }, w: number, h: number) {
  return Math.max(
    Math.hypot(at.x, at.y),
    Math.hypot(w - at.x, at.y),
    Math.hypot(at.x, h - at.y),
    Math.hypot(w - at.x, h - at.y)
  );
}

function union(rects: DOMRect[]): Box {
  return {
    left: Math.min(...rects.map((r) => r.left)),
    top: Math.min(...rects.map((r) => r.top)),
    right: Math.max(...rects.map((r) => r.right)),
    bottom: Math.max(...rects.map((r) => r.bottom)),
  };
}

/**
 * Puts the canvas over the text and returns each line's box inside it.
 * An inline element split over lines has no single box to position a
 * canvas against, and browsers disagree on where its absolute children
 * go. So the canvas is parked at 0,0, measured, then moved by the
 * difference: it lands on the text's bounding box whatever the browser.
 */
function fitCanvas(
  canvas: HTMLCanvasElement,
  ctx: CanvasRenderingContext2D,
  text: HTMLElement
) {
  const lines = Array.from(text.getClientRects());
  if (lines.length === 0) {
    return null;
  }
  const bounds = union(lines);
  canvas.style.left = "0px";
  canvas.style.top = "0px";
  const origin = canvas.getBoundingClientRect();
  canvas.style.left = `${bounds.left - origin.left}px`;
  canvas.style.top = `${bounds.top - origin.top}px`;

  const width = bounds.right - bounds.left;
  const height = bounds.bottom - bounds.top;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  canvas.width = Math.round(width * dpr);
  canvas.height = Math.round(height * dpr);
  canvas.style.width = `${width}px`;
  canvas.style.height = `${height}px`;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

  const boxes: Box[] = lines.map((r) => ({
    left: r.left - bounds.left,
    top: r.top - bounds.top,
    right: r.right - bounds.left,
    bottom: r.bottom - bounds.top,
  }));
  return { width, height, boxes };
}

/** Draws every speck still showing; returns how many there were. */
function paint(ctx: CanvasRenderingContext2D, specks: Speck[], frame: Frame) {
  let alive = 0;
  for (const s of specks) {
    const alpha = step(s, frame);
    if (alpha > 0) {
      alive++;
      ctx.globalAlpha = alpha;
      ctx.fillRect(s.x, s.y, s.size, s.size);
    }
  }
  ctx.globalAlpha = 1;
  return alive;
}

const easeOut = (t: number) => 1 - (1 - t) ** 3;

function makeSpecks(boxes: Box[]): Speck[] {
  const specks: Speck[] = [];
  const area = boxes.reduce(
    (sum, box) => sum + (box.right - box.left) * (box.bottom - box.top),
    0
  );
  const scale = Math.min(1, MAX_SPECKS / Math.max(1, area * DENSITY));
  boxes.forEach((box, index) => {
    const width = box.right - box.left;
    const height = box.bottom - box.top;
    const count = Math.round(width * height * DENSITY * scale);
    for (let i = 0; i < count; i++) {
      const angle = Math.random() * Math.PI * 2;
      const speed = DRIFT * (0.3 + Math.random() * 0.7);
      specks.push({
        x: box.left + Math.random() * width,
        y: box.top + Math.random() * height,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        phase: Math.random(),
        rate: 0.35 + Math.random() * 0.9,
        size: Math.random() < 0.18 ? 1.5 : 1,
        box: index,
        hit: 0,
      });
    }
  });
  return specks;
}

interface Frame {
  now: number;
  dt: number;
  wave: { x: number; y: number } | null;
  radius: number;
  boxes: Box[];
  reduced: boolean;
}

/** Moves one speck a frame on; returns its opacity, 0 once it has faded. */
function step(s: Speck, { now, dt, wave, radius, boxes, reduced }: Frame) {
  if (wave && !s.hit && Math.hypot(s.x - wave.x, s.y - wave.y) <= radius) {
    s.hit = now;
    const dx = s.x - wave.x;
    const dy = s.y - wave.y;
    const length = Math.hypot(dx, dy) || 1;
    const push = THROW * (0.5 + Math.random());
    s.vx += (dx / length) * push;
    s.vy += (dy / length) * push;
  }
  const fade = s.hit ? 1 - (now - s.hit) / FADE_MS : 1;
  if (fade <= 0) {
    return 0;
  }
  if (!reduced || s.hit) {
    s.x += s.vx * dt;
    s.y += s.vy * dt;
    s.phase = (s.phase + s.rate * dt) % 1;
  }
  // Until it's hit, a speck bounces around inside its own line.
  const box = s.hit ? undefined : boxes[s.box];
  if (box) {
    if (s.x < box.left || s.x > box.right) {
      s.vx = -s.vx;
      s.x = Math.min(box.right, Math.max(box.left, s.x));
    }
    if (s.y < box.top || s.y > box.bottom) {
      s.vy = -s.vy;
      s.y = Math.min(box.bottom, Math.max(box.top, s.y));
    }
  }
  // Each speck twinkles between faint and full, out of step with the
  // others, so the patch shimmers instead of flashing.
  const twinkle = 0.35 + 0.65 * Math.abs(Math.sin(s.phase * Math.PI));
  return twinkle * fade * 0.9;
}

/**
 * Inline text hidden under a field of drifting specks, like a spoiler in a
 * chat app. Click it, or focus it and press Enter, and the specks are blown
 * away from the point you touched while the words come into focus.
 */
export function Spoiler({
  children,
  revealed: revealedProp,
  defaultRevealed = false,
  onRevealedChange,
  label = "Hidden text. Activate to reveal.",
  className,
}: SpoilerProps) {
  const [ownRevealed, setOwnRevealed] = useState(defaultRevealed);
  const revealed = revealedProp ?? ownRevealed;
  // The specks stay on screen while the wave clears them, after `revealed`.
  const [clearing, setClearing] = useState(false);
  const rootRef = useRef<HTMLSpanElement>(null);
  const textRef = useRef<HTMLSpanElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const waveRef = useRef<{ x: number; y: number; start: number } | null>(null);

  const reveal = useCallback(
    (clientX?: number, clientY?: number) => {
      if (revealed) {
        return;
      }
      const canvas = canvasRef.current;
      const rect = canvas?.getBoundingClientRect();
      const first = textRef.current?.getClientRects()[0];
      if (rect) {
        // A keyboard reveal starts from the start of the first line.
        const x = clientX ?? (first ? first.left + 4 : rect.left);
        const y = clientY ?? (first ? first.top + first.height / 2 : rect.top);
        waveRef.current = {
          x: x - rect.left,
          y: y - rect.top,
          start: performance.now(),
        };
        setClearing(true);
      }
      setOwnRevealed(true);
      onRevealedChange?.(true);
    },
    [revealed, onRevealedChange]
  );

  // Hiding again (a controlled parent can) forgets any wave in progress.
  useEffect(() => {
    if (!revealed) {
      waveRef.current = null;
      setClearing(false);
    }
  }, [revealed]);

  const showSpecks = !revealed || clearing;

  useEffect(() => {
    if (!showSpecks) {
      return;
    }
    const root = rootRef.current;
    const text = textRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!(root && text && canvas && ctx)) {
      return;
    }

    const reduced = window.matchMedia(
      "(prefers-reduced-motion: reduce)"
    ).matches;
    let boxes: Box[] = [];
    let specks: Speck[] = [];
    let width = 0;
    let height = 0;
    let color = "currentColor";
    let frame = 0;
    let last = 0;
    let visible = true;

    const layout = () => {
      const fitted = fitCanvas(canvas, ctx, text);
      if (!fitted) {
        return;
      }
      ({ width, height, boxes } = fitted);
      specks = makeSpecks(boxes);
      color = getComputedStyle(root).color;
    };

    const draw = (now: number) => {
      const dt = last ? Math.min(0.05, (now - last) / 1000) : 0;
      last = now;
      ctx.clearRect(0, 0, width, height);
      ctx.fillStyle = color;

      const wave = waveRef.current;
      const radius = wave
        ? farthestCorner(wave, width, height) *
          easeOut(Math.min(1, (now - wave.start) / WAVE_MS))
        : -1;
      const alive = paint(ctx, specks, {
        now,
        dt,
        wave,
        radius,
        boxes,
        reduced,
      });

      if (wave && alive === 0) {
        waveRef.current = null;
        setClearing(false);
        return;
      }
      if ((!reduced || wave) && visible) {
        frame = requestAnimationFrame(draw);
      }
    };

    const restart = () => {
      cancelAnimationFrame(frame);
      last = 0;
      frame = requestAnimationFrame(draw);
    };

    layout();
    restart();

    // Text rewraps when its container changes width; lay the specks out again.
    const resize = new ResizeObserver(() => {
      if (!waveRef.current) {
        layout();
        restart();
      }
    });
    const block = root.parentElement ?? root;
    resize.observe(block);

    // Off screen, the shimmer stops.
    const seen = new IntersectionObserver(([entry]) => {
      visible = entry?.isIntersecting ?? true;
      if (visible) {
        restart();
      } else {
        cancelAnimationFrame(frame);
      }
    });
    seen.observe(root);

    return () => {
      cancelAnimationFrame(frame);
      resize.disconnect();
      seen.disconnect();
    };
  }, [showSpecks]);

  const onClick = (event: MouseEvent<HTMLSpanElement>) => {
    // A keyboard "click" has no position; start from the first line instead.
    if (event.detail === 0) {
      reveal();
    } else {
      reveal(event.clientX, event.clientY);
    }
  };

  const onKeyDown = (event: KeyboardEvent<HTMLSpanElement>) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      reveal();
    }
  };

  const text = (
    <span
      aria-hidden={revealed ? undefined : true}
      className={cn(
        "transition-[opacity,filter] duration-[450ms] ease-[cubic-bezier(0.23,1,0.32,1)] motion-reduce:transition-opacity",
        revealed ? "opacity-100 blur-0" : "select-none opacity-0 blur-[6px]"
      )}
      ref={textRef}
    >
      {children}
    </span>
  );
  const specksLayer = showSpecks && (
    <canvas className="pointer-events-none absolute" ref={canvasRef} />
  );
  const shape =
    "relative rounded-[3px] [-webkit-box-decoration-break:clone] [box-decoration-break:clone]";

  if (revealed) {
    // Keeps tabIndex -1 so focus stays put when the spoiler was opened with
    // the keyboard, instead of falling back to the page.
    return (
      <span
        className={cn(shape, "outline-none", className)}
        ref={rootRef}
        tabIndex={-1}
      >
        {text}
        {specksLayer}
      </span>
    );
  }

  return (
    // biome-ignore lint/a11y/useSemanticElements: a <button> can't wrap across lines like inline text
    <span
      aria-label={label}
      className={cn(
        shape,
        "cursor-pointer bg-zinc-900/[0.06] outline-none transition-colors duration-150 hover:bg-zinc-900/10 focus-visible:ring-2 focus-visible:ring-zinc-400 dark:bg-white/[0.08] dark:hover:bg-white/12",
        className
      )}
      onClick={onClick}
      onKeyDown={onKeyDown}
      ref={rootRef}
      role="button"
      tabIndex={0}
    >
      {text}
      {specksLayer}
    </span>
  );
}

interface Message {
  id: string;
  name: string;
  initial: string;
  time: string;
  body: ReactNode;
  own?: boolean;
}

/**
 * A group chat where two messages hide a spoiler. Click a hidden patch to
 * reveal it; "Hide again" puts the specks back.
 */
export default function SpoilerText({ className }: { className?: string }) {
  // Remounting the spoilers is the simplest way to hide them all again.
  const [round, setRound] = useState(0);
  const [shown, setShown] = useState(0);

  const onReveal = () => setShown((count) => count + 1);

  const messages: Message[] = [
    {
      id: "maya",
      name: "Maya",
      initial: "M",
      time: "21:04",
      body: "Finished the finale last night. I'm still not over it.",
    },
    {
      id: "jun",
      name: "Jun",
      initial: "J",
      time: "21:06",
      body: (
        <>
          Same. When it turned out{" "}
          <Spoiler key={`a${round}`} onRevealedChange={onReveal}>
            the lighthouse keeper had been writing the letters all along
          </Spoiler>{" "}
          I had to pause it for a minute.
        </>
      ),
    },
    {
      id: "you",
      name: "You",
      initial: "Y",
      time: "21:07",
      own: true,
      body: (
        <>
          Don't. I'm two episodes behind and I know someone{" "}
          <Spoiler key={`b${round}`} onRevealedChange={onReveal}>
            doesn't make it off the island
          </Spoiler>{" "}
          already.
        </>
      ),
    },
  ];

  return (
    <div
      className={cn(
        "w-full max-w-md rounded-2xl border border-zinc-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-950",
        className
      )}
    >
      <div className="mb-3 flex items-center justify-between">
        <p className="font-medium text-sm text-zinc-900 dark:text-zinc-100">
          # the-finale
        </p>
        <button
          className={cn(
            "rounded-md px-2 py-1 text-xs text-zinc-500 transition-[color,background-color,opacity] duration-150 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-900 dark:hover:text-zinc-100",
            shown === 0 && "pointer-events-none opacity-0"
          )}
          onClick={() => {
            setRound((r) => r + 1);
            setShown(0);
          }}
          tabIndex={shown === 0 ? -1 : undefined}
          type="button"
        >
          Hide again
        </button>
      </div>
      <ul className="flex flex-col gap-3">
        {messages.map((message) => (
          <li className="flex gap-2.5" key={message.id}>
            <span
              aria-hidden="true"
              className={cn(
                "mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full font-medium text-xs",
                message.own
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
                  : "bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-300"
              )}
            >
              {message.initial}
            </span>
            <div className="min-w-0">
              <p className="flex items-baseline gap-2">
                <span className="font-medium text-sm text-zinc-900 dark:text-zinc-100">
                  {message.name}
                </span>
                <span className="text-xs text-zinc-400 tabular-nums dark:text-zinc-500">
                  {message.time}
                </span>
              </p>
              <p className="text-[15px] text-zinc-700 leading-6 dark:text-zinc-300">
                {message.body}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}


### A8. avatar-picker.tsx
// biome-ignore-all lint/suspicious/noBitwiseOperators: the seed hash and PRNG are bitwise by design.
"use client";

/**
 * @author: @dorianbaffier
 * @description: Avatar Picker
 * @version: 2.0.0
 * @date: 2026-10-14
 * @license: MIT
 * @website: https://kokonutui.com
 * @github: https://github.com/kokonut-labs/kokonutui
 */

import { ArrowRight, Dices } from "lucide-react";
import {
  type AnimationPlaybackControls,
  animate,
  LayoutGroup,
  MotionConfig,
  type MotionValue,
  motion,
  useMotionValue,
  useMotionValueEvent,
  useReducedMotion,
  useSpring,
  useTransform,
} from "motion/react";
import {
  type KeyboardEvent,
  type PointerEvent,
  type RefObject,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
} from "react";
import { cn } from "@/lib/utils";

/* ----------------------------------------------------------------------------
 * Face generation
 *
 * Every avatar is a pure function of a seed string. The seed is hashed into a
 * PRNG, which picks continuous feature values (head proportions, hair, eyes,
 * brows, mouth) and a few colors from small muted palettes. Because features
 * are numbers rather than parts picked from a list, two faces can be blended,
 * which is what makes the live morph possible.
 * ------------------------------------------------------------------------- */

type RGB = readonly [number, number, number];

const BACKGROUNDS: readonly { name: string; rgb: RGB }[] = [
  { name: "gray", rgb: [228, 228, 231] },
  { name: "sand", rgb: [233, 226, 214] },
  { name: "sage", rgb: [217, 225, 213] },
  { name: "slate", rgb: [218, 225, 233] },
  { name: "clay", rgb: [235, 221, 214] },
  { name: "mist", rgb: [226, 223, 233] },
];
const SKINS: readonly RGB[] = [
  [246, 233, 222],
  [236, 210, 187],
  [214, 176, 142],
  [176, 128, 96],
  [128, 88, 66],
];
const HAIRS: readonly RGB[] = [
  [39, 39, 42],
  [63, 45, 36],
  [113, 84, 62],
  [161, 161, 170],
  [82, 82, 91],
];
const SHIRTS: readonly RGB[] = [
  [39, 39, 42],
  [82, 82, 91],
  [113, 113, 122],
  [244, 244, 245],
];
const INK = "#18181b";

const SHAPE_KEYS = [
  "headW",
  "headH",
  "headSquare",
  "chin",
  "hairVolume",
  "hairFringe",
  "hairPart",
  "hairLength",
  "eyeGap",
  "eyeY",
  "eyeRx",
  "eyeRy",
  "browTilt",
  "browLift",
  "browWeight",
  "mouthW",
  "mouthCurve",
  "mouthOpen",
  "nose",
  "glasses",
  "cheeks",
  "shoulders",
] as const;

type ShapeKey = (typeof SHAPE_KEYS)[number];

export interface Face {
  shape: Record<ShapeKey, number>;
  bg: RGB;
  skin: RGB;
  hair: RGB;
  shirt: RGB;
  /** Plain-language summary, used for screen-reader labels. */
  description: string;
}

function hashString(input: string) {
  let h = 2_166_136_261;
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i);
    h = Math.imul(h, 16_777_619);
  }
  return h >>> 0;
}

function mulberry32(seed: number) {
  let a = seed;
  return () => {
    a = (a + 0x6d_2b_79_f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t ^= t + Math.imul(t ^ (t >>> 7), 61 | t);
    return ((t ^ (t >>> 14)) >>> 0) / 4_294_967_296;
  };
}

const round2 = (v: number) => Math.round(v * 100) / 100;

function pick<T>(list: readonly T[], r: number): T {
  return list[Math.min(list.length - 1, Math.floor(r * list.length))] as T;
}

function describe(shape: Face["shape"], bgName: string) {
  let hair = "short hair";
  if (shape.hairLength > 0.62) {
    hair = "long hair";
  } else if (shape.hairVolume < 0.22) {
    hair = "cropped hair";
  }
  let mouth = "a slight smile";
  if (shape.mouthOpen > 0.5) {
    mouth = "an open smile";
  } else if (shape.mouthCurve > 3.2) {
    mouth = "a wide smile";
  } else if (shape.mouthCurve < 1) {
    mouth = "a calm expression";
  }
  const face = shape.headSquare > 0.55 ? "Square face" : "Round face";
  const glasses = shape.glasses > 0.5 ? ", glasses" : "";
  return `${face}, ${hair}${glasses}, ${mouth}, on ${bgName}`;
}

/** Deterministic face for a seed. Same seed, same face, server and client. */
export function createFace(seed: string): Face {
  const r = mulberry32(hashString(seed));
  const bg = pick(BACKGROUNDS, r());
  const shape: Face["shape"] = {
    headW: 18 + r() * 5,
    headH: 22 + r() * 4,
    headSquare: r(),
    chin: 0.75 + r() * 0.25,
    hairVolume: r(),
    hairFringe: r(),
    hairPart: r() * 2 - 1,
    hairLength: r() ** 1.6,
    eyeGap: 7.5 + r() * 3,
    eyeY: -2 + r() * 4,
    eyeRx: 1.7 + r() * 0.9,
    eyeRy: 1.9 + r() * 1.4,
    browTilt: -2 + r() * 4,
    browLift: 4.2 + r() * 2,
    browWeight: r() < 0.65 ? 1.3 + r() * 0.8 : 0,
    mouthW: 3.5 + r() * 3,
    mouthCurve: r() * 4.5,
    mouthOpen: r() < 0.3 ? 0.6 + r() * 0.4 : 0,
    nose: r(),
    glasses: r() < 0.25 ? 1 : 0,
    cheeks: r(),
    shoulders: r(),
  };
  for (const key of SHAPE_KEYS) {
    shape[key] = round2(shape[key]);
  }
  return {
    shape,
    bg: bg.rgb,
    skin: pick(SKINS, r()),
    hair: pick(HAIRS, r()),
    shirt: pick(SHIRTS, r()),
    description: describe(shape, bg.name),
  };
}

const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

function lerpRgb(a: RGB, b: RGB, t: number): RGB {
  const c = (i: 0 | 1 | 2) =>
    Math.max(0, Math.min(255, Math.round(lerp(a[i], b[i], t))));
  return [c(0), c(1), c(2)];
}

function lerpFace(a: Face, b: Face, t: number): Face {
  const shape = { ...b.shape };
  for (const key of SHAPE_KEYS) {
    shape[key] = lerp(a.shape[key], b.shape[key], t);
  }
  // Colors stop at the target; only shapes may overshoot with the spring.
  const ct = Math.min(1, t);
  return {
    shape,
    bg: lerpRgb(a.bg, b.bg, ct),
    skin: lerpRgb(a.skin, b.skin, ct),
    hair: lerpRgb(a.hair, b.hair, ct),
    shirt: lerpRgb(a.shirt, b.shirt, ct),
    description: b.description,
  };
}

const rgb = (c: RGB, k = 1) =>
  `rgb(${Math.round(c[0] * k)} ${Math.round(c[1] * k)} ${Math.round(c[2] * k)})`;

const n = round2;

/** Rounded blob: `square` blends between an ellipse and a squircle. */
function blobPath(
  cx: number,
  cy: number,
  w: number,
  top: number,
  bottom: number,
  square: number,
  chin = 1
) {
  const k = 0.552 + square * 0.28;
  const kb = k * chin;
  return [
    `M${n(cx)} ${n(cy - top)}`,
    `C${n(cx + k * w)} ${n(cy - top)} ${n(cx + w)} ${n(cy - k * top)} ${n(cx + w)} ${n(cy)}`,
    `C${n(cx + w)} ${n(cy + kb * bottom)} ${n(cx + kb * w)} ${n(cy + bottom)} ${n(cx)} ${n(cy + bottom)}`,
    `C${n(cx - kb * w)} ${n(cy + bottom)} ${n(cx - w)} ${n(cy + kb * bottom)} ${n(cx - w)} ${n(cy)}`,
    `C${n(cx - w)} ${n(cy - k * top)} ${n(cx - k * w)} ${n(cy - top)} ${n(cx)} ${n(cy - top)}Z`,
  ].join("");
}

function hairFrontPath(s: Face["shape"], cx: number, cy: number) {
  const w = s.headW;
  const h = s.headH;
  const v = s.hairVolume;
  const temple = cy - h * 0.12;
  const crown = cy - h - 1.5 - v * 4;
  const side = w + 1.5 + v * 2.5;
  const k = 0.552 + s.headSquare * 0.28;
  const fringeY = cy - h * (0.66 - s.hairFringe * 0.36);
  const partX = cx + s.hairPart * w * 0.55;
  return [
    `M${n(cx - side)} ${n(temple)}`,
    `C${n(cx - side)} ${n(cy - h * k - v * 3)} ${n(cx - k * side)} ${n(crown)} ${n(cx)} ${n(crown)}`,
    `C${n(cx + k * side)} ${n(crown)} ${n(cx + side)} ${n(cy - h * k - v * 3)} ${n(cx + side)} ${n(temple)}`,
    `L${n(cx + w - 1)} ${n(temple)}`,
    `Q${n(cx + w * 0.75)} ${n(fringeY + 1 - s.hairPart * 5)} ${n(partX)} ${n(fringeY - 2.5)}`,
    `Q${n(cx - w * 0.75)} ${n(fringeY + 1 + s.hairPart * 5)} ${n(cx - w + 1)} ${n(temple)}Z`,
  ].join("");
}

function mouthPath(s: Face["shape"], cx: number, y: number) {
  const curve = s.mouthCurve * 2;
  const open = s.mouthOpen * 5;
  return [
    `M${n(cx - s.mouthW)} ${n(y)}`,
    `Q${n(cx)} ${n(y + curve)} ${n(cx + s.mouthW)} ${n(y)}`,
    `Q${n(cx)} ${n(y + curve + open)} ${n(cx - s.mouthW)} ${n(y)}Z`,
  ].join("");
}

interface FaceArtProps {
  face: Face;
  /** Pointer-follow offset for the eyes, in face units. */
  look?: { x: MotionValue<number>; y: MotionValue<number> };
  blinkRef?: RefObject<SVGGElement | null>;
  className?: string;
}

/** The face itself: one SVG, 100×100 units, every number rounded. */
function FaceArt({ face, look, blinkRef, className }: FaceArtProps) {
  const s = face.shape;
  const cx = 50;
  const cy = 50;
  const eyeY = cy + s.eyeY;
  const mouthY = cy + s.headH * 0.5;
  const glassR = Math.max(s.eyeRx, s.eyeRy) + 3.2;
  const neckTop = cy + s.headH - 6;
  const shoulderTop = cy + s.headH + 3;
  const sw = 30 + s.shoulders * 8;

  return (
    <svg
      aria-hidden="true"
      className={cn("block h-full w-full", className)}
      viewBox="12 14 76 76"
    >
      <rect fill={rgb(face.bg)} height="100" width="100" />
      {/* Long hair sits behind the head. */}
      <path
        d={blobPath(
          cx,
          cy - s.headH * 0.15,
          s.headW + 1 + s.hairVolume * 2.5,
          s.headH * 0.85 + s.hairVolume * 4,
          s.headH * 0.15 + s.hairLength * (s.headH + 14),
          s.headSquare * 0.6
        )}
        fill={rgb(face.hair, 0.9)}
      />
      <path
        d={`M${n(cx - sw)} 100C${n(cx - sw)} ${n(shoulderTop + 6)} ${n(cx - sw * 0.55)} ${n(shoulderTop)} ${cx} ${n(shoulderTop)}C${n(cx + sw * 0.55)} ${n(shoulderTop)} ${n(cx + sw)} ${n(shoulderTop + 6)} ${n(cx + sw)} 100Z`}
        fill={rgb(face.shirt)}
      />
      <rect
        fill={rgb(face.skin, 0.9)}
        height={n(shoulderTop - neckTop + 3)}
        rx="3"
        width="12"
        x={cx - 6}
        y={n(neckTop)}
      />
      <ellipse
        cx={n(cx - s.headW)}
        cy={n(cy + 2)}
        fill={rgb(face.skin, 0.95)}
        rx="3"
        ry="4.5"
      />
      <ellipse
        cx={n(cx + s.headW)}
        cy={n(cy + 2)}
        fill={rgb(face.skin, 0.95)}
        rx="3"
        ry="4.5"
      />
      <path
        d={blobPath(cx, cy, s.headW, s.headH, s.headH, s.headSquare, s.chin)}
        fill={rgb(face.skin)}
      />
      <path d={hairFrontPath(s, cx, cy)} fill={rgb(face.hair)} />
      <g fill={INK} opacity={n(0.045 * s.cheeks)}>
        <circle cx={n(cx - s.eyeGap - 3)} cy={n(eyeY + 7)} r="3.4" />
        <circle cx={n(cx + s.eyeGap + 3)} cy={n(eyeY + 7)} r="3.4" />
      </g>
      <g
        stroke={INK}
        strokeLinecap="round"
        strokeOpacity="0.85"
        strokeWidth={n(Math.max(0, s.browWeight))}
      >
        <path
          d={`M${n(cx - s.eyeGap - 3.2)} ${n(eyeY - s.browLift + s.browTilt)}L${n(cx - s.eyeGap + 3.2)} ${n(eyeY - s.browLift - s.browTilt)}`}
        />
        <path
          d={`M${n(cx + s.eyeGap - 3.2)} ${n(eyeY - s.browLift - s.browTilt)}L${n(cx + s.eyeGap + 3.2)} ${n(eyeY - s.browLift + s.browTilt)}`}
        />
      </g>
      <motion.g style={look ? { x: look.x, y: look.y } : undefined}>
        <g
          className="transition-transform duration-75 ease-out"
          ref={blinkRef}
          style={{ transformBox: "fill-box", transformOrigin: "center" }}
        >
          <ellipse
            cx={n(cx - s.eyeGap)}
            cy={n(eyeY)}
            fill={INK}
            rx={n(s.eyeRx)}
            ry={n(s.eyeRy)}
          />
          <ellipse
            cx={n(cx + s.eyeGap)}
            cy={n(eyeY)}
            fill={INK}
            rx={n(s.eyeRx)}
            ry={n(s.eyeRy)}
          />
        </g>
      </motion.g>
      <path
        d={`M${n(cx - 1.5 - s.nose)} ${n(eyeY + 8 + s.nose * 1.5)}Q${cx} ${n(eyeY + 9.5 + s.nose * 1.5)} ${n(cx + 1.5 + s.nose)} ${n(eyeY + 8 + s.nose * 1.5)}`}
        fill="none"
        stroke={INK}
        strokeLinecap="round"
        strokeOpacity="0.35"
        strokeWidth="1.2"
      />
      <path
        d={mouthPath(s, cx, mouthY)}
        fill={INK}
        stroke={INK}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
      <g
        fill="none"
        opacity={n(Math.max(0, Math.min(1, s.glasses)))}
        stroke={INK}
        strokeWidth="1.2"
      >
        <circle cx={n(cx - s.eyeGap)} cy={n(eyeY)} r={n(glassR)} />
        <circle cx={n(cx + s.eyeGap)} cy={n(eyeY)} r={n(glassR)} />
        <path
          d={`M${n(cx - s.eyeGap + glassR)} ${n(eyeY)}Q${cx} ${n(eyeY - 2)} ${n(cx + s.eyeGap - glassR)} ${n(eyeY)}`}
        />
      </g>
    </svg>
  );
}

const MORPH = { type: "spring", bounce: 0.22, visualDuration: 0.42 } as const;

/**
 * Renders the face for `seed` and, when the seed changes, eases every feature
 * from where it is now to the new values instead of crossfading.
 */
function MorphingFace({
  seed,
  morph = true,
  ...rest
}: Omit<FaceArtProps, "face"> & { seed: string; morph?: boolean }) {
  const [face, setFace] = useState(() => createFace(seed));
  const shownRef = useRef(face);
  // The seed whose face is fully on screen, or null mid-morph. Lets the
  // effect skip a no-op morph on mount and after every spin lands, each of
  // which would otherwise re-render the face every frame for nothing.
  const settledRef = useRef<string | null>(seed);
  const reduceMotion = useReducedMotion();

  useEffect(() => {
    if (settledRef.current === seed) {
      return;
    }
    const target = createFace(seed);
    if (!morph || reduceMotion) {
      shownRef.current = target;
      settledRef.current = seed;
      setFace(target);
      return;
    }
    const from = shownRef.current;
    settledRef.current = null;
    const controls = animate(0, 1, {
      ...MORPH,
      onUpdate: (t) => {
        const next = lerpFace(from, target, t);
        shownRef.current = next;
        setFace(next);
      },
      onComplete: () => {
        settledRef.current = seed;
      },
    });
    return () => controls.stop();
  }, [seed, morph, reduceMotion]);

  return <FaceArt face={face} {...rest} />;
}

/* ----------------------------------------------------------------------------
 * Picker
 * ------------------------------------------------------------------------- */

export interface AvatarValue {
  /** The name the faces are generated from. */
  displayName: string;
  /** Which face for that name. Any integer works; each one is a new face. */
  variant: number;
}

export interface AvatarPickerProps {
  /** Controlled value. */
  value?: AvatarValue;
  /** Initial value when uncontrolled. */
  defaultValue?: Partial<AvatarValue>;
  /** Called when the name changes or a face lands. */
  onChange?: (
    value: AvatarValue & { seed: string; description: string }
  ) => void;
  /** Called by the submit button. */
  onComplete?: (value: AvatarValue & { seed: string }) => void;
  /** Form field name; a hidden input carries the avatar seed. */
  name?: string;
  title?: string;
  description?: string;
  submitLabel?: string;
  className?: string;
}

const SLOT_COUNT = 5;
const COIN = 128;
const BEZEL = 176;
const TICKS = 60;
// One face per half turn. Dragging the coin its own width turns it half a turn.
const DEG_PER_PX = 180 / COIN;
const MAX_FLICK_STEPS = 10;
const TAP_SLOP = 4;
const DRAG_SPRING = { type: "spring", stiffness: 140, damping: 22 } as const;
const SHUFFLE_SPRING = { type: "spring", stiffness: 50, damping: 11 } as const;
const FLY = { type: "spring", bounce: 0.18, visualDuration: 0.45 } as const;
const MIN_NAME = 2;

/** The seed string for a name and variant; feed it to `createFace`. */
export function avatarSeed(displayName: string, variant: number) {
  return `${displayName.trim().toLowerCase()}:${variant}`;
}

const pad3 = (v: number) => String(((v % 1000) + 1000) % 1000).padStart(3, "0");

// Bezel tick marks, rounded so server and client markup match.
const TICK_LINES = Array.from({ length: TICKS }, (_, i) => {
  const a = (i / TICKS) * Math.PI * 2;
  const long = i % 5 === 0;
  const r1 = BEZEL / 2 - 3;
  const r0 = r1 - (long ? 7 : 4);
  const c = BEZEL / 2;
  return {
    key: i,
    long,
    x1: round2(c + Math.sin(a) * r0),
    y1: round2(c - Math.cos(a) * r0),
    x2: round2(c + Math.sin(a) * r1),
    y2: round2(c - Math.cos(a) * r1),
  };
});

function useInView(ref: RefObject<HTMLElement | null>) {
  const [active, setActive] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) {
      return;
    }
    let onScreen = false;
    const update = () => setActive(onScreen && !document.hidden);
    const io = new IntersectionObserver(([entry]) => {
      onScreen = Boolean(entry?.isIntersecting);
      update();
    });
    io.observe(el);
    document.addEventListener("visibilitychange", update);
    return () => {
      io.disconnect();
      document.removeEventListener("visibilitychange", update);
    };
  }, [ref]);
  return active;
}

/** Eyes drift toward the pointer and blink now and then, only while visible. */
function useLivingEyes(
  coinRef: RefObject<HTMLDivElement | null>,
  active: boolean
) {
  const reduceMotion = useReducedMotion();
  const x = useSpring(0, { stiffness: 180, damping: 18 });
  const y = useSpring(0, { stiffness: 180, damping: 18 });
  const blinkRef = useRef<SVGGElement>(null);

  useEffect(() => {
    if (!active || reduceMotion) {
      return;
    }
    const onMove = (e: globalThis.PointerEvent) => {
      const el = coinRef.current;
      if (!el) {
        return;
      }
      const box = el.getBoundingClientRect();
      const dx = e.clientX - (box.left + box.width / 2);
      const dy = e.clientY - (box.top + box.height / 2);
      const dist = Math.hypot(dx, dy) || 1;
      const reach = Math.min(1, dist / 220);
      x.set((dx / dist) * reach * 1.8);
      y.set((dy / dist) * reach * 1.3);
    };
    let timer = 0;
    let open = 0;
    const blink = () => {
      const g = blinkRef.current;
      if (g) {
        g.style.transform = "scaleY(0.1)";
        open = window.setTimeout(() => {
          g.style.transform = "";
        }, 120);
      }
      timer = window.setTimeout(blink, 2600 + Math.random() * 3400);
    };
    timer = window.setTimeout(blink, 1800);
    window.addEventListener("pointermove", onMove, { passive: true });
    return () => {
      window.clearTimeout(timer);
      window.clearTimeout(open);
      if (blinkRef.current) {
        blinkRef.current.style.transform = "";
      }
      window.removeEventListener("pointermove", onMove);
    };
  }, [active, reduceMotion, coinRef, x, y]);

  return { look: { x, y }, blinkRef };
}

interface CoinProps {
  layoutId: string;
  seed: string;
  angle: MotionValue<number>;
  spinning: boolean;
  label: string;
  look: { x: MotionValue<number>; y: MotionValue<number> };
  blinkRef: RefObject<SVGGElement | null>;
  coinRef: RefObject<HTMLDivElement | null>;
  onPointerDown: (e: PointerEvent<HTMLDivElement>) => void;
  onPointerMove: (e: PointerEvent<HTMLDivElement>) => void;
  onPointerUp: (e: PointerEvent<HTMLDivElement>) => void;
  onPointerCancel: (e: PointerEvent<HTMLDivElement>) => void;
}

/**
 * The preview is a coin with real thickness: a stack of discs in 3D. It turns
 * around its vertical axis and swaps faces as it passes edge-on, so a spin
 * reads like a slot reel without any crossfade.
 */
function Coin({
  layoutId,
  seed,
  angle,
  spinning,
  label,
  look,
  blinkRef,
  coinRef,
  ...handlers
}: CoinProps) {
  // Fold the running angle into -90..90 so the front always faces us.
  const tilt = useTransform(angle, (a) => a - 180 * Math.round(a / 180));
  const transform = useTransform(tilt, (t) => `rotateY(${round2(t)}deg)`);
  const shade = useTransform(tilt, (t) => round2((Math.abs(t) / 90) * 0.35));

  return (
    <motion.div
      className="relative z-10 cursor-grab touch-pan-y select-none rounded-full active:cursor-grabbing"
      layoutId={layoutId}
      ref={coinRef}
      style={{ width: COIN, height: COIN, perspective: 600 }}
      transition={FLY}
      {...handlers}
    >
      <motion.div
        aria-label={label}
        className="relative h-full w-full"
        role="img"
        style={{ transform, transformStyle: "preserve-3d" }}
      >
        {[6, 5, 4, 3, 2, 1].map((z) => (
          <div
            aria-hidden="true"
            className="absolute inset-0 rounded-full bg-zinc-300 dark:bg-zinc-600"
            key={z}
            style={{ transform: `translateZ(-${z}px)` }}
          />
        ))}
        <div className="absolute inset-0 overflow-hidden rounded-full ring-1 ring-black/10 ring-inset">
          <MorphingFace
            blinkRef={blinkRef}
            look={look}
            morph={!spinning}
            seed={seed}
          />
          <motion.div
            aria-hidden="true"
            className="absolute inset-0 bg-black"
            style={{ opacity: shade }}
          />
        </div>
      </motion.div>
    </motion.div>
  );
}

function useSpinner(onLand: (steps: number) => void) {
  const angle = useMotionValue(0);
  const [step, setStep] = useState(0);
  const [spinning, setSpinning] = useState(false);
  const controls = useRef<AnimationPlaybackControls | null>(null);
  const stepRef = useRef(0);
  const onLandRef = useRef(onLand);
  onLandRef.current = onLand;

  useMotionValueEvent(angle, "change", (a) => {
    const s = Math.round(a / 180);
    if (s !== stepRef.current) {
      stepRef.current = s;
      setStep(s);
    }
  });

  const land = useCallback(() => {
    controls.current = null;
    const s = Math.round(angle.get() / 180);
    angle.jump(0);
    stepRef.current = 0;
    setStep(0);
    setSpinning(false);
    if (s !== 0) {
      onLandRef.current(s);
    }
  }, [angle]);

  const stop = useCallback(() => {
    controls.current?.stop();
    controls.current = null;
  }, []);

  const spinTo = useCallback(
    (
      steps: number,
      transition: { stiffness: number; damping: number; velocity: number }
    ) => {
      stop();
      setSpinning(true);
      controls.current = animate(angle, steps * 180, {
        type: "spring",
        ...transition,
        restDelta: 0.5,
        restSpeed: 4,
        onComplete: land,
      });
    },
    [angle, land, stop]
  );

  const jumpBy = useCallback(
    (steps: number) => {
      stop();
      angle.jump(steps * 180);
      land();
    },
    [angle, land, stop]
  );

  return { angle, step, spinning, setSpinning, spinTo, jumpBy, stop, land };
}

type Spinner = ReturnType<typeof useSpinner>;

interface DragState {
  id: number;
  x: number;
  start: number;
  moved: boolean;
  samples: { x: number; t: number }[];
}

function releaseTarget(d: DragState, current: number, x: number, t: number) {
  const first = d.samples[0] ?? { x, t };
  const dt = Math.max(16, t - first.t) / 1000;
  const velocity = ((x - first.x) / dt) * DEG_PER_PX;
  const start = Math.round(d.start / 180);
  const projected = Math.round((current + velocity * 0.28) / 180);
  const target = Math.max(
    start - MAX_FLICK_STEPS,
    Math.min(start + MAX_FLICK_STEPS, projected)
  );
  return { target, velocity };
}

/** Drag the coin sideways to turn it; let go and it coasts with momentum. */
function useDragSpin(spinner: Spinner, reduceMotion: boolean | null) {
  const drag = useRef<DragState | null>(null);

  const onPointerDown = (e: PointerEvent<HTMLDivElement>) => {
    if (e.button !== 0) {
      return;
    }
    spinner.stop();
    spinner.setSpinning(true);
    e.currentTarget.setPointerCapture(e.pointerId);
    drag.current = {
      id: e.pointerId,
      x: e.clientX,
      start: spinner.angle.get(),
      moved: false,
      samples: [{ x: e.clientX, t: e.timeStamp }],
    };
  };

  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d || d.id !== e.pointerId) {
      return;
    }
    const dx = e.clientX - d.x;
    if (Math.abs(dx) > TAP_SLOP) {
      d.moved = true;
    }
    spinner.angle.set(d.start + dx * DEG_PER_PX);
    d.samples.push({ x: e.clientX, t: e.timeStamp });
    if (d.samples.length > 6) {
      d.samples.shift();
    }
  };

  const onPointerUp = (e: PointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d || d.id !== e.pointerId) {
      return;
    }
    drag.current = null;
    const current = spinner.angle.get();
    // A tap turns to the next face; a drag coasts on its release speed.
    const { target, velocity } = d.moved
      ? releaseTarget(d, current, e.clientX, e.timeStamp)
      : { target: Math.round(current / 180) + 1, velocity: 900 };
    if (reduceMotion) {
      spinner.jumpBy(target);
    } else {
      spinner.spinTo(target, { ...DRAG_SPRING, velocity });
    }
  };

  // The browser took the gesture (a vertical scroll that started on the
  // coin): settle on the nearest face instead of treating it as a tap.
  const onPointerCancel = (e: PointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d || d.id !== e.pointerId) {
      return;
    }
    drag.current = null;
    const target = Math.round(spinner.angle.get() / 180);
    if (reduceMotion) {
      spinner.jumpBy(target);
    } else {
      spinner.spinTo(target, { ...DRAG_SPRING, velocity: 0 });
    }
  };

  return { onPointerDown, onPointerMove, onPointerUp, onPointerCancel };
}

/**
 * Onboarding avatar picker. Faces are generated from the display name, morph
 * live as it is typed, and can be spun like a coin: drag or flick it sideways,
 * tap it, or press shuffle. Five slots keep options to compare; picking one
 * flies it into the preview and sends the previous face back to its slot.
 */
export default function AvatarPicker({
  value,
  defaultValue,
  onChange,
  onComplete,
  name,
  title = "Pick your avatar",
  description = "Made from your name. Spin it or pick one.",
  submitLabel = "Continue",
  className,
}: AvatarPickerProps) {
  const uid = useId();
  const reduceMotion = useReducedMotion();
  const startVariant = defaultValue?.variant ?? 0;
  const [internalName, setInternalName] = useState(
    defaultValue?.displayName ?? "Robin Vale"
  );
  const [slots, setSlots] = useState(() =>
    Array.from({ length: SLOT_COUNT }, (_, i) => startVariant + i * 7)
  );
  const [selected, setSelected] = useState(0);
  const [focusIndex, setFocusIndex] = useState(0);
  const [announcement, setAnnouncement] = useState("");
  const rootRef = useRef<HTMLDivElement>(null);
  const coinRef = useRef<HTMLDivElement>(null);
  const slotRefs = useRef<(HTMLButtonElement | null)[]>([]);

  const displayName = value?.displayName ?? internalName;
  const variant = value?.variant ?? slots[selected] ?? 0;
  const latest = useRef({ displayName, variant, selected });
  latest.current = { displayName, variant, selected };

  // Keep the selected slot in step with a controlled variant.
  const controlledVariant = value?.variant;
  useEffect(() => {
    if (controlledVariant === undefined) {
      return;
    }
    setSlots((prev) =>
      prev[selected] === controlledVariant
        ? prev
        : prev.map((v, i) => (i === selected ? controlledVariant : v))
    );
  }, [controlledVariant, selected]);

  const emit = (next: AvatarValue, announce: boolean) => {
    const seed = avatarSeed(next.displayName, next.variant);
    const face = createFace(seed);
    onChange?.({ ...next, seed, description: face.description });
    if (announce) {
      setAnnouncement(`Avatar: ${face.description}`);
    }
  };

  const spinner = useSpinner((steps) => {
    const {
      displayName: current,
      variant: from,
      selected: slot,
    } = latest.current;
    const next = from + steps;
    setSlots((prev) => prev.map((v, i) => (i === slot ? next : v)));
    emit({ displayName: current, variant: next }, true);
  });
  const dragHandlers = useDragSpin(spinner, reduceMotion);
  const active = useInView(rootRef);
  const { look, blinkRef } = useLivingEyes(coinRef, active);

  const shownVariant = variant + spinner.step;
  const seed = avatarSeed(displayName, shownVariant);

  const shuffle = () => {
    const current = Math.round(spinner.angle.get() / 180);
    const steps = 5 + Math.floor(Math.random() * 4);
    if (reduceMotion) {
      spinner.jumpBy(current + steps);
      return;
    }
    spinner.spinTo(current + steps, {
      ...SHUFFLE_SPRING,
      velocity: steps * 180 * 1.6,
    });
  };

  const choose = (index: number) => {
    if (index === selected) {
      return;
    }
    if (spinner.spinning) {
      spinner.stop();
      spinner.land();
    }
    setSelected(index);
    setFocusIndex(index);
    emit(
      { displayName: latest.current.displayName, variant: slots[index] ?? 0 },
      true
    );
  };

  const onSlotKeyDown = (e: KeyboardEvent<HTMLButtonElement>) => {
    const moves: Record<string, number> = {
      ArrowRight: focusIndex + 1,
      ArrowDown: focusIndex + 1,
      ArrowLeft: focusIndex - 1,
      ArrowUp: focusIndex - 1,
      Home: 0,
      End: SLOT_COUNT - 1,
    };
    const target = moves[e.key];
    if (target === undefined) {
      return;
    }
    e.preventDefault();
    const next = (target + SLOT_COUNT) % SLOT_COUNT;
    setFocusIndex(next);
    slotRefs.current[next]?.focus();
  };

  const setName = (next: string) => {
    if (value === undefined) {
      setInternalName(next);
    }
    emit({ displayName: next, variant }, false);
  };

  const nameOk = displayName.trim().length >= MIN_NAME;
  const bezelRotate = useTransform(
    spinner.angle,
    (a) => `rotate(${round2(a / 15)}deg)`
  );
  const diceRotate = useTransform(
    spinner.angle,
    (a) => `rotate(${round2(a / 4)}deg)`
  );

  return (
    <MotionConfig reducedMotion="user">
      <div
        className={cn(
          "mx-auto w-full max-w-[360px] rounded-2xl border border-zinc-200 bg-white p-5 text-zinc-900 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-100",
          className
        )}
        ref={rootRef}
      >
        <div className="space-y-1">
          <h2 className="font-semibold text-base tracking-tight">{title}</h2>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            {description}
          </p>
        </div>

        <LayoutGroup id={uid}>
          <div className="mt-5 flex flex-col items-center">
            <div
              className="relative grid place-items-center"
              style={{ width: BEZEL, height: BEZEL }}
            >
              <motion.svg
                aria-hidden="true"
                className="absolute inset-0 text-zinc-300 dark:text-zinc-700"
                style={{ transform: bezelRotate }}
                viewBox={`0 0 ${BEZEL} ${BEZEL}`}
              >
                {TICK_LINES.map((t) => (
                  <line
                    key={t.key}
                    stroke="currentColor"
                    strokeLinecap="round"
                    strokeOpacity={t.long ? 1 : 0.6}
                    strokeWidth={t.long ? 1.5 : 1}
                    x1={t.x1}
                    x2={t.x2}
                    y1={t.y1}
                    y2={t.y2}
                  />
                ))}
              </motion.svg>
              {/* Fixed index mark the bezel turns under. */}
              <div
                aria-hidden="true"
                className="absolute top-0 left-1/2 h-3 w-0.5 -translate-x-1/2 rounded-full bg-zinc-900 dark:bg-zinc-100"
              />
              <Coin
                angle={spinner.angle}
                blinkRef={blinkRef}
                coinRef={coinRef}
                key={selected}
                label={`Selected avatar: ${createFace(seed).description}`}
                layoutId={`face-${selected}`}
                look={look}
                seed={seed}
                spinning={spinner.spinning}
                {...dragHandlers}
              />
            </div>

            <div className="mt-2 flex items-baseline gap-2 text-xs">
              <span className="max-w-[180px] truncate font-medium">
                {displayName.trim() || "Anonymous"}
              </span>
              <span className="font-mono text-zinc-400 tabular-nums dark:text-zinc-500">
                No. {pad3(shownVariant)}
              </span>
            </div>
          </div>

          <div className="mt-5 flex items-center justify-between gap-3">
            <div
              aria-label="Avatar options"
              className="flex gap-2"
              role="toolbar"
            >
              {slots.map((slotVariant, i) => {
                const isSelected = i === selected;
                const slotSeed = avatarSeed(displayName, slotVariant);
                return (
                  <button
                    aria-label={`Option ${i + 1}: ${createFace(slotSeed).description}`}
                    aria-pressed={isSelected}
                    className="group/slot relative size-10 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2 focus-visible:ring-offset-white dark:focus-visible:ring-zinc-100 dark:focus-visible:ring-offset-zinc-950"
                    // biome-ignore lint/suspicious/noArrayIndexKey: slots are fixed positions.
                    key={i}
                    onClick={() => choose(i)}
                    onFocus={() => setFocusIndex(i)}
                    onKeyDown={onSlotKeyDown}
                    ref={(el) => {
                      slotRefs.current[i] = el;
                    }}
                    tabIndex={i === focusIndex ? 0 : -1}
                    type="button"
                  >
                    {/* The socket stays put; the face flies in and out of it. */}
                    <span
                      aria-hidden="true"
                      className={cn(
                        "absolute inset-0 rounded-full border transition-colors duration-150 ease-out",
                        isSelected
                          ? "border-zinc-300 border-dashed bg-zinc-50 dark:border-zinc-700 dark:bg-zinc-900"
                          : "border-transparent"
                      )}
                    />
                    {isSelected ? (
                      <span
                        aria-hidden="true"
                        className="absolute inset-0 m-auto size-1.5 rounded-full bg-zinc-400 dark:bg-zinc-500"
                      />
                    ) : (
                      <motion.span
                        className="absolute inset-0 overflow-hidden rounded-full ring-1 ring-black/10 ring-inset transition-shadow duration-150 ease-out group-hover/slot:shadow-[0_0_0_2px_rgb(212_212_216)] dark:group-hover/slot:shadow-[0_0_0_2px_rgb(63_63_70)]"
                        layoutId={`face-${i}`}
                        transition={FLY}
                      >
                        <MorphingFace seed={slotSeed} />
                      </motion.span>
                    )}
                  </button>
                );
              })}
            </div>

            <button
              aria-label="Shuffle avatar"
              className="grid size-10 shrink-0 place-items-center rounded-full border border-zinc-200 text-zinc-600 outline-none transition-colors duration-150 ease-out hover:bg-zinc-100 hover:text-zinc-900 focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2 focus-visible:ring-offset-white dark:border-zinc-800 dark:text-zinc-400 dark:focus-visible:ring-zinc-100 dark:focus-visible:ring-offset-zinc-950 dark:hover:bg-zinc-900 dark:hover:text-zinc-100"
              onClick={shuffle}
              title="Shuffle"
              type="button"
            >
              <motion.span
                className="grid place-items-center"
                style={{ transform: diceRotate }}
              >
                <Dices aria-hidden="true" className="size-4" />
              </motion.span>
            </button>
          </div>
        </LayoutGroup>

        <div className="mt-5 space-y-2">
          <label
            className="block font-medium text-sm"
            htmlFor={`${uid}-display-name`}
          >
            Display name
          </label>
          <input
            autoComplete="name"
            className="h-10 w-full rounded-lg border border-zinc-200 bg-transparent px-3 text-sm outline-none transition-colors duration-150 ease-out placeholder:text-zinc-400 focus-visible:border-zinc-400 focus-visible:ring-2 focus-visible:ring-zinc-900/10 dark:border-zinc-800 dark:focus-visible:border-zinc-600 dark:focus-visible:ring-zinc-100/10"
            id={`${uid}-display-name`}
            maxLength={32}
            onChange={(e) => setName(e.target.value)}
            placeholder="Your name"
            spellCheck={false}
            type="text"
            value={displayName}
          />
        </div>

        {name ? (
          <input
            name={name}
            type="hidden"
            value={avatarSeed(displayName, variant)}
          />
        ) : null}

        <button
          className="group/submit mt-4 flex h-10 w-full items-center justify-center gap-1.5 rounded-lg bg-zinc-900 font-medium text-sm text-white outline-none transition-colors duration-150 ease-out hover:bg-zinc-800 focus-visible:ring-2 focus-visible:ring-zinc-900 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-40 dark:bg-zinc-100 dark:text-zinc-900 dark:focus-visible:ring-zinc-100 dark:focus-visible:ring-offset-zinc-950 dark:hover:bg-white"
          disabled={!nameOk}
          onClick={() =>
            onComplete?.({
              displayName: displayName.trim(),
              variant,
              seed: avatarSeed(displayName, variant),
            })
          }
          type="button"
        >
          {submitLabel}
          <ArrowRight
            aria-hidden="true"
            className="size-4 transition-transform duration-150 ease-out group-hover/submit:translate-x-0.5"
          />
        </button>

        <p aria-live="polite" className="sr-only">
          {announcement}
        </p>
      </div>
    </MotionConfig>
  );
}

