# Koto · Minigame Prototype v1

Native Android navigation shell for learning Japanese. Exactly three sections — **Map | Learn | Cards** — with Learn in the middle. Map opens 10 playable lessons containing 60 questions across five reusable minigames. Learn and Cards remain quiet placeholders, and Learn remains the fresh-launch default. Lessons provide Japanese TTS, first-try scoring, saved completion, and replay. Fonts and question content are bundled locally. See `docs/MINIGAME_PROTOTYPE_QA.md` for the current implementation and validation status.

## Build and run

Requirements for this checkout: **JDK 25** (the existing `gradle/gradle-daemon-jvm.properties` selects it), Android SDK platform **37**, and build tools **36.0.0**. The bundled Android Studio JDK on this machine satisfies that setting; application bytecode targets Java 17. Dependency versions are pinned in `gradle/libs.versions.toml`; the Gradle distribution is SHA-256 verified. Android Gradle plugin 9.1.1 supplies Kotlin 2.2.10, and the Compose compiler plugin uses the same version.

On this Windows machine, from the project directory:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --gradle-user-home '.gradle-user-home' '-Pkotlin.compiler.execution.strategy=in-process' :app:assembleDebug :app:lintDebug :app:testDebugUnitTest --console=plain
```

`local.properties` points at the SDK already installed on this machine. It is deliberately ignored by version control; on another machine, set `sdk.dir` to that machine's SDK directory or set `ANDROID_HOME`.

The installable APK is `app/build/outputs/apk/debug/app-debug.apk`. With a USB-debugging-enabled Android phone connected and authorized:

```powershell
& 'C:\Users\HMED OPEN\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& 'C:\Users\HMED OPEN\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell am start -n com.koto.app/.MainActivity
```

Minimum supported device: Android 8.0 / API 26. This is a development APK, signed with the local debug key, for installation and interaction tuning.

## Structure and state

One `MainActivity` hosts Compose. `KotoApp` owns a single, explicit `KotoDestination` value with `rememberSaveable`. A fresh task initializes it to **Learn**. Rotation or Android restoring the existing task preserves that task's selection; reopening an already-running task is a resume, not a fresh launch. No tab value is written to preferences, DataStore, or a database.

`KotoNavigation` replaces content with `AnimatedContent`. There is no stack of tab taps. Back from Map or Cards selects Learn; at Learn, Back is left to Android's normal root-activity behavior. Internal destination stacks can be added when real screens need them.

`MapScreen` owns the unchanged keyed LazyColumn and level preview dialog. Its scroll position survives tab switches and lessons through SaveableStateHolder. `KotoApp` holds one optional lesson ID; `LessonScreen` replaces the main shell while active. A saveable `LessonSession` owns question state, and completion IDs persist in local preferences. One shared settings sheet controls Japanese audio. Learn and Cards retain their placeholder treatment.

## The custom bar

`KotoBottomBar` draws a persistent full-width surface with three equal invisible touch slots. Each slot uses `KotoBottomBarItem`; the entire slot is selectable, with `Role.Tab`, selected state, a merged text label, and keyboard focus indication. Icons are decorative within the labeled tab, so a screen reader does not announce the name twice. There is no traveling selection surface, pill, underline, or stock Material navigation component.

`KotoNavIcon` contains three small, replaceable vector-style drawings with common bounds. Selected icons gain stroke and fill emphasis. Selection also has position, shape and depth cues, so color is not the only signal.

The bar is 80 dp minimum, grows with text, and caps its internal width at 560 dp on wide windows. Visual motion never moves the hit targets. Bottom and side safe-area insets protect the controls while the surface extends beneath system navigation. The content separately consumes top and side safe-area insets. Dark system-bar icons are explicitly configured for the light-first theme.

## Motion and tuning

Compose's interruptible animation primitives follow the latest target; there are no delayed navigation jobs or animation queues. The content and bar transition concurrently. Compose's animation duration scale follows the Android setting, including disabled animations.

| Control | Initial value | Location |
| --- | --- | --- |
| Color hierarchy and active detail | Deep navy, muted gold, supporting grey, pure white | `ui/theme/Color.kt` |
| Spacing and icon geometry | 80 dp minimum bar; stable 58 × 34 dp icon wells | `ui/theme/Dimens.kt` |
| Press travel | 2 dp icon displacement; 0.94 press scale | `ui/theme/Dimens.kt` and `Motion.kt` |
| Press | 90 ms tween; icon scale 0.94 | `ui/theme/Motion.kt` |
| Icon activation/deactivation | 420 ms / 210 ms controlled easing | `ui/theme/Motion.kt` |
| Content replacement | 180 ms in, 100 ms out, 6 dp travel | `ui/theme/Motion.kt` and `Dimens.kt` |
| Typography | Fixed 12 sp navigation labels, stable weight | `ui/theme/Type.kt` |

The launch-window background and accent in `res/values/colors.xml` mirror `Color.kt`; keep these two XML values in sync when adjusting the palette. M PLUS Rounded 1c is bundled for Latin and Japanese. No runtime font or image download, shader, blur, or custom animation engine is required.

Each icon has an inactive line-art state and a unique active illustration state. The existing icon geometry remains; detail uses muted gold and soft grey to match the Map V2 palette. The icon animations are interruptible and the destination changes immediately; Compose's duration scaling handles reduced-motion settings without delaying navigation.

## Foundation files

The foundation includes:

- Root Gradle configuration, pinned version catalog, checksum-verified wrapper, and `.gitignore`.
- `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml`.
- `MainActivity.kt`, `ui/KotoApp.kt`, and the two `ui/navigation` files.
- `ui/components/KotoBottomBar.kt`, `KotoBottomBarItem.kt`, and `KotoNavIcon.kt`.
- The three destination screens and shared `PlaceholderScreen.kt`.
- Five `ui/theme` files for colors, dimensions, motion, typography, and Material theme wiring.
- Basic string, launch-theme, and adaptive launcher-icon resources.
- Supplied fox-and-butterfly launcher artwork in `artwork/launcher`, with reproducible adaptive foreground and monochrome assets generated by `tools/generate_launcher_icon.py`.
- `KotoShellTest.kt`, `KotoAccessibilityLayoutTest.kt`, and `RenderedScreenshot.kt` for local Android/Compose checks and native-rendered snapshots.
- This handoff and `docs/PHASE_1_QA.md` for evidence and remaining device checks.

## Platform choices

API 26 is a deliberately small baseline with light navigation-bar icon support. Compile/target API 37 uses the SDK already installed here. This phase is light-only, including when the device uses dark mode. Standard Android/Compose APIs handle edge-to-edge insets, root Back, and animation scaling. No experimental navigation framework is necessary for three static destinations.

Local Robolectric tests run against an API 35 Android runtime with native graphics. Map interaction coverage and additional overlay renders extend the original 10 shell tests. Current results are recorded in `docs/MINIGAME_PROTOTYPE_QA.md`; older QA documents describe superseded prototypes. They can check state, rendering, semantics, and geometry, but cannot establish physical-device frame pacing, gesture-navigation appearance, or the subjective feel of presses. The minigame prototype still needs the physical-phone checks listed in the current QA document.

Test-tool compatibility: Robolectric 4.16.1's native loader mishandles spaces in Maven cache paths. The test task uses a `koto-robolectric-maven` cache beneath the system temporary directory (a short path on this Windows installation). If that directory contains spaces on another machine, pass `-ProbolectricMavenCache=<a-path-without-spaces>`. Screenshots use native `PixelCopy` directly to avoid Compose's device-only frame-commit wait in this runtime. These accommodations are test-only and add nothing to the APK.

Implementation references: [Android state restoration](https://developer.android.com/develop/ui/compose/state-saving), [system insets](https://developer.android.com/develop/ui/compose/system/insets-ui), [accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [AGP 9.1.1 compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes).
