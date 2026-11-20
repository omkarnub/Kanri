# Kanri Motion Graphics — 32-Beat Timeline Specification

All measurements in design pixels (1× camera space). Timing is exact on 120 BPM grid (0.500 s per beat, 16.000 s total).

| Beat | t (s) | Shape W×H · radius · fill | Content / action | Cursor | Cam s | SFX |
|---|---|---|---|---|---|---|
| 1 | 0.0 | 220×56 · r28 · #121212 | Label "+ Add Transaction" settled at frame 0 | Rests at P0, drifts to pill, press 0.40 | 4.58 | kick |
| 2 | 0.5 | 440×200 · r36 · #121212 | Release fires: pill stretches into Amount field: label, "₹1,450", chips +₹500 / +₹1,000 | Releases on beat, glides to +₹500 chip | 2.29 | CLK_UP, THOCK |
| 3 | 1.0 | 440×200 · r36 · #121212 | Chip release fires odometer 1,450→1,950; chip press-in | Press 0.90, release 1.00 | 2.29 | CLK_DN/UP, TICK×5 |
| 4 | 1.5 | 440×288 · r40 · #121212 | Height grows; footer: [fork-knife Food & Dining] chip slides in from x−24, [Save] button (#30A46C bg, #F2F2F2 text) | Glides to Save | 2.29 | THOCK |
| 5 | 2.0 | 128×128 · r64 · #121212 | Save release: shape contracts to circle; green spinner | Release on beat, then drifts aside | 7.90 | CLK_UP, THOCK |
| 6 | 2.5 | 128×128 · r64 · #30A46C | Fill flashes green via OKLab; 2px checkmark draws on | — | 7.90 | CHIME |
| 7 | 3.0 | 400×72 · r36 · #121212 | Capsule: red 8px dot + "HDFC Bank • ₹1,950 Debited" (minus sign red) | Drifts toward lower right of capsule | 2.52 | THOCK, TOAST |
| 8 | 3.5 | 560×120 · r40 · #121212 | Opens: adds muted line "Auto-captured via Dual-Engine • 100% Offline" with offline icon, and a [Split] chip | Moves to Split chip | 1.80 | TICK, SWEEP |
| 9 | 4.0 | 480×264 · r40 · #121212 | Split click fires morph: "Split ₹1,950", avatars row, "3 people · ₹650 each", slider with notches 2–6 | Press 3.90, release 4.00 | 2.10 | CLK_UP, THOCK |
| 10 | 4.5 | 480×264 · r40 · #121212 | Cursor reaches the thumb; thumb scales 1→1.15 | Press 4.90 (grab) | 2.10 | CLK_DN |
| 11 | 5.0 | 480×264 · r40 · #121212 | Drag right: thumb passes notch 4 and stretches elastically (dual-spring) to ~4.4; per-person odometer 650→487.50 | Direct drag, spring lag | 2.10 | TICK×4 |
| 12 | 5.5 | 480×264 · r40 · #121212 | Release: slider recoils to notch 3; odometer back to ₹650; avatars settle | Release at 5.50 | 2.10 | CLK_UP, SNAP |
| 13 | 6.0 | 340×72 · r36 · #121212 | Segmented pill "Owe │ Owed" (Owe selected: off-white knob, dark text) | Glides to "Owed", press 6.40 | 2.96 | THOCK |
| 14 | 6.5 | 340×72 · r36 · #121212 | Release: liquid knob stretches Owe→Owed (leading, then trailing); knob fades to #30A46C; weight on "Owed" 500→700 | Release 6.50 | 2.96 | CLK_UP, SNAP |
| 15 | 7.0 | 460×248 · r40 · #121212 | Goal card: "MacBook Fund", "₹45,000 / ₹60,000", progress bar at 75%, [+₹5,000] chip | Glides to chip | 2.19 | THOCK |
| 16 | 7.5 | 460×248 · r40 · #121212 | Release: bar fills 75%→83.3% in #30A46C; numbers odometer to ₹50,000; subtle highlight sweep (1 pass, alpha only) | Press 7.40, release 7.50 | 2.19 | CLK_UP, TICK×3 |
| 17 | 8.0 | 560×400 · r44 · #121212 | Expands into "Kanri Insights" card, "This week", baseline, Mon–Sun labels | Drifts toward Saturday column | 1.80 | THOCK, SWEEP |
| 18 | 8.5 | 560×400 · r44 · #121212 | 7 bars sprout (Mon 620, Tue 340, Wed 780, Thu 450, Fri 1,120, Sat 1,950, Sun 880), 50 ms stagger | — | 1.80 | TICK×7 (rising) |
| 19 | 9.0 | 560×400 · r44 · #121212 | Hover Sat: bar → #30A46C; tooltip "Sat: ₹1,950" pops | Arrives on Sat bar | 1.80 | TOAST |
| 20 | 9.5 | 520×360 · r40 · #121212 | Tooltip tucks in; container condenses slightly | Lifts off | 1.94 | WHOOSH_SOFT |
| 21 | 10.0 | 440×72 · r36 · #121212 | Collapses horizontally into search pill: ⌘ icon, "Filter:" and a ⏎ keycap | Moves to field | 2.29 | THOCK |
| 22 | 10.5 | 440×144 · r32 · #121212 | Grows to show 4 rows (Swiggy −₹1,950, Spotify −₹119, Uber −₹186, Amazon −₹1,299); typing "Swiggy" at 83 ms/char; rows collapse live | Hovers ⏎ keycap | 2.29 | KEY ticks×6 |
| 23 | 11.0 | 380×80 · r40 · #121212 | ⏎ keycap presses (release 11.00): capsule snaps into badge: shield icon + "SQLCipher AES-256" + sub "100% Offline Vault" | Press 10.90, release 11.00 | 2.65 | CLK_UP, THOCK |
| 24 | 11.5 | 380×80 · r40 · #121212 | Lock glyph's shackle drops and clicks closed; 1px micro-haptic settle | — | 2.65 | LOCK |
| 25 | 12.0 | 480×64 · r32 · #121212 | Badge stretches into toast: ✓ + "Saved to Local Keystore • No Cloud"; camera pans down to follow (y +320) | Drifts | 2.10 | TOAST, SWEEP |
| 26 | 12.5 | 300×88 · r44 · #121212 | Contracts to balance badge "₹84,320" (world y returns to 0) | — | 3.36 | THOCK |
| 27 | 13.0 | 420×88 · r44 · #121212 | Digits micro-flip to "+₹12,400 this month" in #30A46C (flip = vertical reel swap, 40 ms stagger) | — | 2.40 | TICK×4 |
| 28 | 13.5 | 420×88 · r44 · #121212 | Hold with life: balance gently settles; cursor sweeps toward center | Sweeps, press 13.90 | 2.40 | SWEEP |
| 29 | 14.0 | 300×64 · r32 · #121212 | Release: balance content exits; neutral capsule | Release 14.00, then drifts to P0 | 3.36 | CLK_UP, THOCK |
| 30 | 14.5 | 220×56 · r28 · #121212 | Bounds settle to the initial capsule geometry (LAST geometry retarget) | Drifting to P0 | 4.58 | SNAP |
| 31 | 15.0 | 220×56 · r28 · #121212 | "+ Add Transaction" label reveals (icon, then text, 50 ms stagger) | Arrives at P0 by 15.4 | 4.58 | TICK |
| 32 | 15.5 | 220×56 · r28 · #121212 | Hold. State vector is identical to beat 1, frame 0 | At P0, velocity 0 | 4.58 | (kick on 16.0 = 0.0) |

## Camera Focus Fractions
- Target frame size: 1440×1440 px
- Formula: `scale = fraction × 1440 / max(focusW, focusH)`
- Fraction $0.70$ for normal cards; $0.65$ for small capsules/circles ($128\times 128$) to reduce whiplash.
- Camera retarget lags shape by $0.060\text{ s}$.
- Panned and zoomed smoothly via log-scale spring.

