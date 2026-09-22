# Koto Minigame Prototype v1

Implemented from `Koto_Minigame_Prototype_v1.docx`, supplied on 2026-09-22. The Word document is the product brief; its embedded screenshots are composition references. The document itself was not modified.

## Delivered behavior

- Map levels 01–10 open ten data-defined lessons containing all 60 specified questions. The existing node positions, 124 dp rows, path, side nodes, title area, and bottom navigation geometry remain unchanged. The three chips read FIRST CONTACT, BUILD & USE, and CONVERSATION TEST. Existing future nodes 11 and 12 remain locked.
- One `LessonScreen` renders Meaning Choice, Sentence Builder, Cloze, Conversation Response, and Pair Match. It replaces the main shell, with only close, progress, and settings in its utility row.
- Choice and conversation check on selection, then wait for CONTINUE. Builder and cloze allow edits until CHECK, then show feedback and CONTINUE. Incorrect answers reveal the correct answer and count as a first-try miss. Pair mismatches clear after 200 ms; completed boards advance after 450 ms. Opening an overlay or backgrounding the activity pauses pending pair transitions.
- `LessonSession` owns selections, tile order, pair state, results, and progress. Its Bundle saver preserves an active attempt across activity recreation and Android saved-state restoration. Replay clears the entire attempt. Close/system Back confirms discarding unfinished work and returns to Map. Map scroll position survives lessons.
- Completion IDs persist locally; partial/abandoned attempts never complete a level. The single `UNLOCK_ALL_PROTOTYPE_LEVELS = true` constant is effective only when `BuildConfig.DEBUG` is true. Set it to false in `feature/lesson/data/LessonRepository.kt` to exercise sequential unlocking; release builds are sequential regardless of the constant.
- One application-context Japanese TTS controller is reused across lessons and rotations. Only `JapaneseText.tts` reaches speech; romaji and English do not. Japanese selections use QUEUE_FLUSH, prompts have replay controls, and correct manual sentences speak once. The controller selects an installed Japanese voice that does not require a network connection. Without one, speakers disable and one unobtrusive notice appears per process; gameplay remains functional. Settings persist the real Japanese-audio toggle.
- `TactileButton` supplies a solid face, 4 dp lower edge, 90 ms press travel, and 180 ms feedback for lesson controls. The existing map action delegates to this implementation. No image assets, runtime content/font downloads, blur, or continuous animation were added.
- M PLUS Rounded 1c weights 400, 500, and 700 are bundled in `res/font` and applied to every Material typography role and the app's custom text tokens. The full fonts support both Latin and Japanese.

## Extending content

`feature/lesson/model/LessonDefinition.kt` defines the five question types, Japanese text, stable answer/pair IDs, and content validation. `LessonRepository` is the content boundary; `PrototypeLessons` currently supplies immutable Kotlin data. UI and speech objects are never stored in question definitions. Correctness compares IDs or ordered ID lists.

To add a lesson, add its `LessonDefinition` to the repository and reference its ID from a map node. The existing node for 11 already has that reference and becomes playable when a definition is added. No extra player or question route is required. Questions within a lesson reuse the same shell and renderer.

The validation pass rejects Han/kanji, missing romaji, invalid or duplicated answer IDs, invalid builder order, and duplicated pair IDs. Options and tiles use deterministic non-answer-order presentation; replay is repeatable.

## Local verification

Run from the checkout with the installed Android Studio JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --gradle-user-home '.gradle-user-home' '-Pkotlin.compiler.execution.strategy=in-process' :app:assembleDebug :app:lintDebug :app:testDebugUnitTest --console=plain
```

The full local suite passes: 29 tests, 0 failures. Debug APK assembly succeeds. Lint reports 0 errors and 17 warnings (dependency update notices, unused legacy preview strings, and KTX convenience suggestions).

Coverage includes all 60 questions through the actual Map/player/completion flow at 320 × 640 dp, with assertions that each unanswered question fits without scrolling at normal font scale. Tests also cover manual completeness, reversing tiles, wrong answers and first-try scoring, pair rejection, debug versus sequential unlock, replay, settings, close/Back, recreation with an answered choice and partially built sentence, and 2× text scale with reachable actions. Content and speech-boundary validation have unit coverage. Existing navigation, map geometry, RTL, and large-text shell tests remain included.

Native Robolectric/API 35 screenshots were visually reviewed for all five game families, the four-pair board, completion, and large text. Screenshots are in `app/build/reports/koto/screenshots/lesson-*.png`. At larger font scales, the question body can scroll while the utility row and manual action remain accessible.

Installable debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Device checks still required

`adb devices` reported no connected device. These local checks do not establish audible Japanese pronunciation/voice availability, TalkBack behavior, gesture insets on the user's phone, real frame pacing, or tactile feel on the target older hardware. Validate rapid Japanese taps, mute, suspend/resume, all five games, and the final four-pair board on that phone before accepting the performance requirement.

## Font provenance

Unmodified TTF files were downloaded from [Google Fonts M PLUS Rounded 1c](https://github.com/google/fonts/tree/main/ofl/mplusrounded1c). The SIL Open Font License and copyright notice are packaged in `assets/licenses/MPLUSRounded1c-OFL.txt`, sourced from the [original Rounded M+ directory](https://github.com/google/fonts/blob/main/ofl/roundedmplus1c/OFL.txt). Japanese speech uses the platform [TextToSpeech API](https://developer.android.com/reference/android/speech/tts/TextToSpeech).
