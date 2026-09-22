# Map V2.0 implementation and QA

Source of truth: `C:\\Users\\HMED OPEN\\Documents\\koto documents\\Koto_Map_V2_0_Implementation_Specification.md`.

## Scope and behavior

Map is implemented inside the existing three-tab shell. Learn and Cards remain their existing empty placeholders; the original Learn fresh-launch default is preserved. Select Map from the left tab. Shared backgrounds and navigation accents now follow the V2 white/navy/gold/grey palette.

Twelve static sample main levels span three stages. They illustrate completed, current, available, and locked states, not real learner progress or JLPT curriculum. Main circles keep their numbers, including the completed circle's additional checkmark. Optional star, diamond, double-ring, and rounded-square shapes attach to their parent main rows, with no optional numbering and at most two per parent.

Every main level opens a compact preview. Completed levels show Replay; available/current levels show Play. Locked levels can be inspected, with a disabled Play action and explanatory text. Play/Replay dismiss the preview and show an honest placeholder snackbar; they never launch lessons, unlock levels, or mutate progress. Back, outside-tap dismissal, and the close button use the dialog's exit transition.

The settings bottom sheet has a working dropdown, two switches, a slider, and Done. Values are local interaction samples and reset when the sheet leaves composition. There is no settings service, preference storage, database, network call, or account system.

## Architecture

- `ui/screens/map/MapData.kt`: immutable row, level, and kana models; explicit optional slots; static fixtures; resource-backed expectations and state descriptions.
- `MapScreen.kt`: header, remembered list state, selected level number, sheet visibility, and placeholder feedback. Saved instance state is UI restoration only, not progress persistence.
- `MapNodes.kt`: stage labels, centered numbered nodes, simple vertical/horizontal connectors, and optional shape rendering.
- `LevelPopup.kt`: small, width-capped, vertically scrollable dialog, kana with romaji above, and interruptible 180 ms entry / 160 ms exit. Exit completion controls disposal instead of an arbitrary delayed callback.
- `SettingsPlaceholderSheet.kt`: standard Material sheet with simple dimming and local controls.
- `ui/components/DepthButton.kt`: reusable rounded action with a solid lower edge and 3 dp press travel over 90 ms; no expensive blurred shadow.
- `ui/navigation/KotoNavigation.kt`: saveable state holder retains the Map scroll position across existing tab switches.
- `res/values/map_strings.xml` and four tiny vector drawables: UI copy and icons, with no map image assets or external fonts.

A ViewModel/repository layer is intentionally unnecessary for static data and ephemeral UI state. Real curriculum can later replace the fixture source without replacing the row components.

## Performance review

The progression is a LazyColumn with stable keys and stage/level content types. Off-screen levels leave composition. Immutable level values and stable callbacks let unchanged node components skip recomposition. Scroll position is not read by composition to drive effects. Press animation values are read in graphics-layer blocks. There are no perpetual animation loops, map-wide canvases, blur effects, gradients, particles, large bitmap backgrounds, database observers, or rendering engines.

All node connections are plain narrow rectangles. The optional star and settings/check/close icons use vectors. The current-level ring is static. Only interaction transitions run. The existing shell retains its short finite tab/icon transitions.

## Validation

Validation commands (PowerShell, repository root):

```powershell
$env:JAVA_HOME = 'C:\\Program Files\\Android\\Android Studio\\jbr'
.\\gradlew.bat --gradle-user-home '.gradle-user-home' '-Pkotlin.compiler.execution.strategy=in-process' :app:assembleDebug :app:lintDebug :app:testDebugUnitTest --console=plain
```

The in-process compiler option avoids a Kotlin daemon writing outside this workspace. Robolectric's `user.home` is isolated to `app/build/test-home` for its download lock; its existing temporary short-path Maven cache is retained. These are build/test accommodations and do not add app runtime code.

Local validation covers the original shell behavior and Map state semantics, lazy off-screen disposal, centered alignment, all optional shapes, compact targets, popup contents and kana order, Play/Replay feedback without progress changes, locked action disabling, actual dialog Back dispatch, activity recreation, dropdown/switch/slider behavior, settings reset, retained scroll position, landscape overlays, and double-font-scale popup access.

Rendered snapshots live under `app/build/reports/koto/screenshots/`, including `map-v2-phone.png`, `map-v2-compact.png`, `map-v2-popup.png`, `map-v2-settings.png`, `map-v2-popup-landscape.png`, and `map-v2-popup-large-text.png`. Overlay-only snapshots capture the top window, so their transparent/scrim areas do not include the underlying activity. These are local render checks, not physical-device screenshots.

## Device acceptance still required

No device was connected during implementation. Stable 60 FPS on an older phone has **not** been measured. Before claiming that target, run a release/profileable build on representative API 26+ low-end hardware, record frame timing during repeated full-path scrolls and rapid dialog/sheet interactions, and check TalkBack, system gesture insets, and animation scale 0. Verify that idle Map has no continuously scheduled animation work. Debug/Robolectric results cannot establish hardware frame pacing or battery behavior.

Installable development build: `app/build/outputs/apk/debug/app-debug.apk`.
