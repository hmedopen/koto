# Phase 1 verification

This record distinguishes implemented behavior, automated evidence, and the phone checks still required by the brief.

## Local checks

Verified on 21 September 2026:

| Check | Result |
| --- | --- |
| Debug APK assembly | Pass — Kotlin/Compose, application `com.koto.app`, version `0.1.0`, min API 26 / target API 37 |
| Android lint | Pass — no errors; only six version-update advisories for the deliberately pinned toolchain/libraries |
| Compose/Robolectric UI suite | **10 tests passed, 0 failures**, native graphics on API 35 |
| Exactly three accessible tabs, correct order and centered Learn | Pass |
| New task starts on Learn; recreation preserves current task | Pass |
| Back returns directly to Learn after repeated section switching | Pass |
| Icon illustration state changes immediately and remains stable after rapid interruptions | Pass |
| Cancelled contact leaves the current destination selected | Pass |
| 320 dp width, 360 dp phone, landscape, 200% font scale | Pass; targets and rendered text checked |
| RTL equal touch-slot alignment | Pass |
| APK permission review | No INTERNET permission; only AndroidX's app-scoped receiver permission |

Generated reports:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`
- `app/build/reports/koto/screenshots/learn-phone.png`
- `app/build/reports/koto/screenshots/cards-phone.png`
- `app/build/reports/koto/screenshots/cards-compact.png`
- `app/build/reports/koto/screenshots/cards-large-text.png`
- `app/build/reports/koto/screenshots/map-landscape.png`

All five rendered snapshots were inspected for alignment, hierarchy and clipping. These are local native-rendered Android windows, not photographs or screenshots of a physical device. No device-system-bar appearance is inferred from them.

Text contrast calculated from the actual color tokens:

| Pair | Contrast |
| --- | --- |
| Primary text / background | 11.77:1 |
| Secondary text / background | 5.01:1 |
| Unselected navigation text / bar | 5.29:1 |
| Active navy navigation foreground / light background | 8.56:1 |

## Phone acceptance pass

No Android device was connected and no emulator system image was configured at implementation time. These checks remain open; local rendering is not a substitute for them.

1. Install the debug APK, launch it, and confirm Learn is selected. Select Cards, dismiss the task from Recents, and relaunch: Learn must be selected. Background/resume and rotation should preserve the current task.
2. Repeatedly tap `Map → Learn → Cards → Learn → Map`, slowly and then as quickly as possible. Confirm immediate contact feedback, each icon's controlled activation/deactivation, no visible icon wobble, and no input queuing. Also press and drag off a tab to cancel, and repeatedly tap the selected tab.
3. Switch among tabs, then use Back. Map/Cards should go to Learn once; the next Back should perform Android's root behavior rather than revisit tap history. Check hardware/three-button Back and gesture Back, including cancellation of the gesture.
4. Verify portrait, landscape, a display cutout, gesture navigation, and three-button navigation. Content and controls must stay out of system areas, while the bar background extends behind the navigation area. Check gesture-contrast and system-icon visibility on the phone.
5. Set font size and display size to their largest values. Confirm all three labels remain readable and all touch targets remain usable. Check a narrow split-screen window where available.
6. Enable TalkBack and navigate the bar. Each destination should be announced once, as a tab with its selected state. Confirm activation works and the screen heading is reachable. Check keyboard/D-pad focus where available.
7. Enable Remove animations / set animator duration scale to 0. Tab selection and destination changes should still work immediately with unmistakable selected state.
8. Judge frame pacing on real hardware and tune only the centralized motion, color, and dimension tokens. No feature scope should be added during this pass.

## Acceptance status

The source implements the requested Kotlin/Compose shell, exact tab order and default, three placeholders, custom logo-colored bottom bar, unique interruptible icon illustrations, restrained content transition, top-level Back behavior, accessible tab semantics, edge-to-edge inset handling, and centralized tuning tokens. No Phase 2 feature is included.

Implementation, APK assembly, lint and local UI checks are complete. Final acceptance remains conditional on the phone pass above. In particular, reliable launch on a physical device, gesture/three-button system-bar appearance, TalkBack behavior, reduced-motion behavior, and subjective interaction/performance quality have **not** been device-verified.
