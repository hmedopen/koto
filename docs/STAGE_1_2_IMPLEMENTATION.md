# Stage 1–2 implementation and source audit

Scope: Stage 1 / FIRST CONTACT (01–05), Stage 2 / BUILDING SENTENCES (06–12).
No Stage 3–8 lesson definitions or questions are included.
Local rollback baseline: `ceb4f33` (clean working tree before this task). No push performed.

## Source decisions

Read all 11 markdown files in `C:\Users\HMED OPEN\Documents\koto documents\staages from 1 to 50` before implementation, including the blueprint, specification, schema, formula, and every stage database.

The user's current 12-level scope overrides the documents' 50-level implementation instructions. The user explicitly approved reusing authored questions in review slots, with Production taking precedence over FULL where they conflict.

`FoundationLessons.sources` is the per-level source manifest. `P01Q1` means Level 01 / Q1 in `Koto_Stage1_Stage2_Production_Database.md`; `F02Q3` means Level 02 / Q3 in `Koto_Stage1_Stage2_FULL_Question_Database.md`. Level 08's unnumbered Production example is referenced as `P08Q1`. Every deployed question has a stable destination ID (`L01_Q01`, etc.). The manifest retains its original source even when used for review in another level.

- 28 authored non-listening question templates, 116 playable question slots including approved review.
- Production answer keys and distractors retained. Options are deterministically rotated with identity-based scoring; the answer position is not hardcoded to A.
- FULL adds distinct authored items: thank-you meaning, cat meaning, book builder, “I” meaning, teacher cloze, “I watch,” sleep meaning, and the simple-question meaning.
- Kana and romaji supplied for romaji-only source questions and choices; no kanji. Names use ケン / Ken and ユキ / Yuki. The authored ぱん spelling is retained.
- Sentence builders split the authored sentences into selectable words/particles without adding new distractor tiles.
- One necessary prompt clarification: P04Q2 says “Choose: This is a book.” Its original “Choose correct” is ambiguous because “hon wa neko desu” is also grammatical. All four choices and the answer key remain intact.
- Historical synthetic content lives only in `src/test/.../PrototypeLessons.kt` for existing mechanics regression coverage; it is removed from the shipped application.

## Levels

| Level | Title | Stage | Questions |
|---|---|---|---:|
| 01 | Japanese First Steps | 1 | 7 |
| 02 | First Words | 1 | 7 |
| 03 | Introducing Yourself | 1 | 10 |
| 04 | The Desu Pattern | 1 | 10 |
| 05 | First Integration | 1 | 10 |
| 06 | Actions Begin | 2 | 10 |
| 07 | Food And Drinks | 2 | 10 |
| 08 | Similar Verb Training | 2 | 10 |
| 09 | Daily Actions | 2 | 10 |
| 10 | Simple Questions | 2 | 10 |
| 11 | Listening Practice | 2 | 10 |
| 12 | Stage Integration | 2 | 12 |

The introductory levels use the formula's seven-question short-level option. Level 01 has five distinct written non-listening questions followed by two reviews. Level 05 uses the FULL database's explicit 10-question count; Level 12 uses its explicit 12-question count. Other review slots reuse earlier written questions without importing material from later levels.

## Missing data and content limitations

These are source limitations, not silently generated replacements:

- Neither Stage 1–2 file contains the promised complete question sets. Many normal levels contain only 1–3 written questions; integration levels provide goals rather than actual banks. The approved reuse supplies playable reviews but cannot achieve the full new/practice/listening/conversation distribution.
- Vowels and konbanwa are listed as Level 01 learning goals, but no complete questions assess them. No new assessments invented.
- Level 09 supplies a sleep question in FULL, but no complete wake-up or study questions.
- Level 10 supplies a meaning question in FULL, but no complete conversation-response question for its new grammar.
- Listening is not a supported `Question` subtype. Existing Japanese TTS is optional playback, not an audio-only quiz. P01Q3/F01Q3 remain deferred. Level 11 currently contains approved reviews of known greetings, nouns, verbs and a conversation, using the existing speaker buttons; it is **not a completed listening assessment**. A dedicated listening game and authored listening bank are needed to fulfill that objective.
- Several supplied distractors conflict with the documents' own realism rules (for example a cat among greetings, animals among drinks, and malformed grammar options). They are preserved rather than replaced with invented choices; they need editorial revision.
- Full per-question difficulty/skill/review metadata is not authored in the sources. This change retains the current question model and a traceable source manifest, without adding an adaptive system or claiming generated pedagogical metadata.

No Stage 3+ work should proceed without a separate request. The above gaps remain even though all 12 scoped levels have playable data.

## Architecture and performance

- `LessonRepository.levels` contains only 12 small summaries (ID, title, stage, count). Map and unlock checks do not call `lesson()`.
- `FoundationLessons.lesson(id)` constructs and validates only the requested level. It retains no loaded-question cache and returns null outside 01–12.
- `KotoApp` remembers the active definition by level ID; ordinary recomposition does not rebuild questions.
- Existing keyed `LazyColumn` map rows and visual components remain. Stage boundaries now follow the blueprint (01–05 / 06–12), and popup counts come from metadata.
- No new dependencies, database, network requests, audio assets, background loops, or renderer/mechanics changes.
- Current debug unlock-all behavior is retained. Sequential unlocking remains active without the debug override and is tested independently.

## Progress and rollback

Completed real lessons use SharedPreferences `koto_foundation_v1`. Old `koto_lessons` placeholder progress is retained for rollback but does not unlock or complete the new curriculum. Invalid/out-of-scope IDs cannot be completed. A versioned lesson composition key prevents restoring an old placeholder attempt into the new question sequence. Real attempts retain the existing saved-instance-state behavior, mistake review, replay, and exit handling.

## Validation

Level 01 and Level 02 were tested first through the map and lesson UI. Focused checks cover their completion, answer scoring, activity recreation, and saved progress. Further checks cover all authored answer keys, kana/romaji rendering, all level sessions, review-before-completion, bounds, sequential progression, and map metadata access without loading questions.

Final validation (2026-09-24):

- `:app:assembleDebug`: passed; APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `:app:lintDebug`: passed with 0 errors and 22 warnings (dependency/tool versions, existing API/style advisories, and unused resources).
- `:app:testDebugUnitTest`: **77 tests passed, 0 failures, 0 errors, 0 skipped**, including the full map-to-completion walkthrough of all 12 levels.
- Level 01 / 02 focused completion and persistence tests passed before the full suite.
- All five game types rendered and completed on the 320 dp Robolectric phone; activity recreation, mistake review, replay, progress persistence and sequential unlock checks passed.
- Inspected native-rendered meaning-choice, conversation and sentence-builder screenshots. Longer four-answer conversations and sentence boards use the existing vertical scrolling at narrow sizes; no renderer layout was changed. Large-text reachability tests passed.
- `git diff --check`: clean.

Local Robolectric checks do not establish physical-device frame pacing, battery usage, or installed Japanese voice availability. Existing minimum Android API remains 26. No physical phone benchmark was performed.
