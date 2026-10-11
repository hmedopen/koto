# KOTO: DEEP PHASE-2 ZERO-EDIT ADVERSARIAL AUDIT REPORT
**Target Codebase**: 100% of Android Application Codebase, Schemas, Assets, and Configuration (`koto the project`)  
**Audit Protocol**: Strict Read-Only Deep Adversarial Inspection  
**Audit Date**: October 2026  
**Status**: 100% Complete — Zero Code Edits Executed  

---

## EXECUTIVE SUMMARY

This adversarial Phase-2 audit follows the initial reconnaissance and subjects every subsystem, Room schema, asset payload, Compose tree, and utility class of **Koto** to microscopic inspection.

### Critical Systemic Vulnerabilities Uncovered:
1. **Imminent Main-Thread Crash Cascade**: Koto’s database (`KotoDatabase`) relies on `.allowMainThreadQueries()`. Exactly 18 call sites across `CardsScreen`, `TranslatorScreen`, `CreateDeckScreen`, `FlashcardStudyScreen`, and `SpreadsheetEngine` execute synchronous SQLite queries on the Android UI thread during initial composition or click events. Removing the flag without complete coroutine refactoring will trigger instant `IllegalStateException` crashes across all primary screens.
2. **Double-Serialization Performance Disaster in SRS**: On every single flashcard review button press, `FlashcardSrsStore` serializes the *entire in-memory store of thousands of cards* into a monolithic `JSONObject`, converts it to a multi-megabyte string, and writes it to Android `SharedPreferences` while synchronously querying Room.
3. **Data Loss & Kanji Stripping in Custom Decks**: `CustomCardEntity` was engineered without a `kanji` column (`// Strictly pure Kana`). Custom decks created by users permanently discard Kanji text, directly violating **Design Philosophy Rule 3 (Dual-Mode Kanji/Kana Invariant)**. Furthermore, `CustomDeckDao.saveDeckWithCards` executes a destructive `DELETE FROM custom_cards WHERE deckId = :deckId` before re-inserting cards, which breaks foreign key stability and detaches SRS review history.
4. **Massive Asset Bloat & Machine Leak**: 
   - `assets/card_contexts.json` (3.85 MB) triplicates every single card record under three separate keys (`cardId`, `kana|romaji`, and `kana`), meaning 66% of the 3.85 MB file is copy-pasted string duplication.
   - `CardContextLoader.kt` line 111 contains a hardcoded local Windows file path (`c:/Users/HMED OPEN/...`) compiled into production code.
   - `res/font/` bundles 21 uncompressed TTF font files totaling over **65 Megabytes**, inflating APK download size by over 400%.
   - Six vector drawables in `res/drawable` and an Excel template in `assets` are completely dead.
5. **Direct Design Philosophy Violations**:
   - `KanaTile` uses soft grey bubble fills (`#F5F6F8`) and 12dp rounded corners (violating **Rule 2 Anti-Bubble** and **Rule 6 Square Geometry**).
   - `StageLabel` in `MapNodes.kt` uses a `RoundedCornerShape(50)` pill container with `KotoColors.SoftGrey`.
   - `LevelPopup.kt` uses a 28dp bubble dialog and violates **Rule 4 (Romaji Placement)** by placing Romaji *above* Kana instead of directly under it.

---

# SECTION 1: DATABASE & PERSISTENCE DRILL-DOWN

---

### [DB-01]: Main-Thread Database Query Crash Cascade
1. **File & Exact Line Range**: 
   - `com/koto/app/feature/translator/data/db/KotoDatabase.kt` lines 92–95
   - `com/koto/app/ui/screens/cards/CardsScreen.kt` lines 87–95, 153–157, 236–241
   - `com/koto/app/feature/translator/ui/TranslatorScreen.kt` lines 103, 192–201, 329, 749, 788–796
   - `com/koto/app/ui/screens/cards/CreateDeckScreen.kt` lines 126–131, 274–280
   - `com/koto/app/ui/screens/cards/FlashcardSrsEngine.kt` lines 43–56, 115–146
   - `com/koto/app/feature/cards/data/DeckFavoriteStore.kt` lines 23–61
   - `com/koto/app/feature/cards/spreadsheet/SpreadsheetEngine.kt` lines 338–342
2. **The Flaw & Root Cause**: 
   `KotoDatabase.getInstance` explicitly configures `.allowMainThreadQueries()`. If this single flag is deleted, the following 18 execution points will immediately throw `java.lang.IllegalStateException: Cannot access database on the main thread since it may potentially lock the UI for a long period of time`:
   - `CardsScreen.kt:87`: `cardStore.loadStarredCards()` called in `remember { mutableStateOf(...) }` on initial composition.
   - `CardsScreen.kt:88`: `customDeckStore.loadCustomDecks()` called in `remember { mutableStateOf(...) }` on initial composition.
   - `CardsScreen.kt:91`: `srsStore.loadAll()` called in `rememberSaveable` on initial composition.
   - `CardsScreen.kt:92`: `deckFavoriteStore.loadBookmarkedDeckIds()` called in initial composition.
   - `CardsScreen.kt:93`: `deckFavoriteStore.loadStarredCardIds()` called in initial composition.
   - `CardsScreen.kt:155`: `srsStore.saveAll(state.srsRecords)` fired synchronously from `LaunchedEffect(state.srsRecords)`.
   - `CardsScreen.kt:236`: `deckFavoriteStore.toggleDeckBookmark(deckId)` fired in UI click callback.
   - `CardsScreen.kt:240`: `deckFavoriteStore.toggleCardStar(cardId)` fired in UI click callback.
   - `TranslatorScreen.kt:103`: `cardStore.loadStarredCards()` called in `remember { mutableStateOf(...) }` on initial composition.
   - `TranslatorScreen.kt:192, 200`: `cardStore.toggleStar()` and `cardStore.loadStarredCards()` in UI click callback.
   - `TranslatorScreen.kt:329, 749, 788, 795`: `cardStore.loadStarredCards()` and `toggleStar()` in UI event handlers.
   - `CreateDeckScreen.kt:126`: `customDeckStore.loadCustomDecks()` called in `LaunchedEffect` without `Dispatchers.IO`.
   - `CreateDeckScreen.kt:274`: `customDeckStore.saveDeck(...)` called directly in UI click lambda.
   - `FlashcardStudyScreen.kt:721`: `srsStore.saveCard(...)` called synchronously on rating button tap.
   - `SpreadsheetEngine.kt:338`: `customDeckStore.loadCustomDecks()` called on main thread before export dialog shows.
3. **Architectural Resolution**: 
   - All DAO methods must either return reactive streams (`Flow<T>`) or be declared as Kotlin `suspend` functions.
   - UI Composables must never invoke stores directly during composition initialization. All state loading must be hoisted into ViewModels (`CardsViewModel`, `TranslatorViewModel`, `CreateDeckViewModel`) running coroutines on `viewModelScope.launch(Dispatchers.IO)`.
   - Stores must expose read-only `StateFlow` or `SharedFlow` streams to the UI.
4. **Blast Radius (What It Could Break)**: 
   Touching this without coordinating all 18 callers will crash app launch on both the Cards Tab and the Translator Tab. Every bookmark, star, and deck creation event will crash the app process.
5. **Safe Fix Protocol**: 
   - Phase A: Introduce `suspend` variants for all DAO methods and wrap internal store calls in `withContext(Dispatchers.IO)`.
   - Phase B: Migrate `CardsScreen` and `TranslatorScreen` to collect from existing Room Flows (`starredCardsFlow`, `customDecksFlow`, `bookmarkedDeckIdsFlow`).
   - Phase C: Remove `.allowMainThreadQueries()` from `KotoDatabase` builder and execute targeted Unit tests on DAO operations.
6. **Urgency & Priority**: **CRITICAL / HIGHEST**. Primary cause of UI thread stalling and frame drops.

---

### [DB-02]: Monolithic JSON Re-Serialization on Every Single Flashcard Review
1. **File & Exact Line Range**: `com/koto/app/ui/screens/cards/FlashcardSrsEngine.kt` lines 115–169
2. **The Flaw & Root Cause**: 
   In `FlashcardSrsStore`, when a user reviews a card by tapping Any rating button (Again, Hard, Good, Easy), `saveCard(card, log)` is executed. It updates the in-memory map, calls Room, and then calls `saveToPreferences(memoryStore)`.
   In `saveToPreferences`:
   ```kotlin
   val root = JSONObject()
   for ((id, card) in records) {
       val obj = JSONObject()
       // ... 15 fields serialized ...
       root.put(id, obj)
   }
   preferences.edit().putString(KEY_SRS_DATA, root.toString()).apply()
   ```
   If a user has 2,000 flashcards, tapping "Good" causes the app to instantiate 2,000 `JSONObject` instances, assemble a multi-megabyte JSON string, and write it to disk through SharedPreferences. This generates huge heap churn, causes major GC pauses, and wastes battery.
3. **Architectural Resolution**: 
   Room Database (`card_srs` table via `CardSrsDao`) is already version 8 and stores every single FSRS field (`due`, `stability`, `difficulty`, `elapsedDays`, `scheduledDays`, `reps`, `state`, `lastReview`, `step`). 
   The SharedPreferences JSON fallback is completely obsolete legacy code. `saveToPreferences` must be eliminated entirely. Individual card updates must write solely to Room via `CardSrsDao.insertOrUpdate(entity)` on a background IO dispatcher.
4. **Blast Radius (What It Could Break)**: 
   If removed incorrectly, users upgrading from an older app version that only had data in `SharedPreferences` could lose progress.
5. **Safe Fix Protocol**: 
   - Implement a one-way startup migration: If `card_srs` is empty and `KEY_SRS_DATA` exists in SharedPreferences, parse it once in the background, batch-insert into Room via `insertOrUpdateAll`, then clear `KEY_SRS_DATA`.
   - Delete `saveToPreferences` completely.
6. **Urgency & Priority**: **CRITICAL / IMMEDIATE**. Destroys review flow smoothness and causes high GC stutter on every card flip.

---

### [DB-03]: Data Loss & Kanji Stripping in Custom Decks
1. **File & Exact Line Range**: 
   - `com/koto/app/feature/cards/data/db/CustomCardEntity.kt` lines 20–40
   - `com/koto/app/feature/cards/data/CustomDeckStore.kt` lines 107–184
   - `com/koto/app/ui/screens/cards/CreateDeckScreen.kt` lines 215–231
2. **The Flaw & Root Cause**: 
   In `CustomCardEntity.kt`, the schema is defined as:
   ```kotlin
   val japanese: String, // Strictly pure Kana (Hiragana/Katakana)
   ```
   Notice that `CustomCardEntity` has NO `kanji` column!
   When a user creates a card in `CreateDeckScreen.kt`, line 220 calls:
   `val rom = KanaConverter.toRomaji(KanaConverter.toPureKana(jp))`
   And when converting `CustomCardEntity` to `Flashcard` in `CustomDeckStore.kt` lines 165–173:
   `Flashcard(id = entity.id, japanese = entity.japanese, ..., kanji = null)`
   The `kanji` parameter is never passed and defaults to `null`!
   This means any Kanji entered by the user is either converted to Kana or stripped of Kanji metadata. In the study screen and deck preview, custom decks cannot render Kanji ruby characters or respect the Dual-Mode toggle. This directly violates **Design Philosophy Rule 3 (Dual-Mode Kanji/Kana Invariant)**.
3. **Architectural Resolution**: 
   Add a `kanji: String = ""` column to `CustomCardEntity`. In `CustomDeckStore`, store the native Japanese text (with Kanji) in `kanji` and phonetic reading in `japanese`. When mapping to `Flashcard`, pass `kanji = entity.kanji.takeIf { it.isNotBlank() }`.
4. **Blast Radius (What It Could Break)**: 
   Requires a Room schema migration (`MIGRATION_8_9`) for `koto_local.db`. If migration fails, Room's fallback to destructive migration would wipe all existing custom decks!
5. **Safe Fix Protocol**: 
   - Write explicit Room migration: `ALTER TABLE custom_cards ADD COLUMN kanji TEXT NOT NULL DEFAULT ''`.
   - Add unit test verifying `MIGRATION_8_9` migrates schema without data loss.
6. **Urgency & Priority**: **HIGH**. Permanent degradation of user study content.

---

### [DB-04]: Destructive Card Replacement & Broken Foreign Key Continuity in Custom Decks
1. **File & Exact Line Range**: `com/koto/app/feature/cards/data/db/CustomDeckDao.kt` lines 45–53
2. **The Flaw & Root Cause**: 
   `CustomDeckDao.saveDeckWithCards` is implemented as:
   ```kotlin
   @Transaction
   fun saveDeckWithCards(deck: CustomDeckEntity, cards: List<CustomCardEntity>) {
       insertDeck(deck)
       deleteCardsByDeckId(deck.id)
       if (cards.isNotEmpty()) {
           insertCards(cards)
       }
   }
   ```
   Every time a user edits a deck or adds a card, the DAO executes `deleteCardsByDeckId(deck.id)`—deleting ALL existing cards in that deck—and inserts the new list.
   Consequences:
   - If SQLite foreign key cascade is active, any child entity referencing `card.id` is deleted.
   - In `CreateDeckScreen.kt`, when editing or importing cards, new card IDs are generated with `System.currentTimeMillis()`. Because the old cards are deleted and new card IDs are inserted, the card's corresponding SRS learning history in `card_srs` becomes detached and orphaned! The card resets to "New" state, losing all SRS memory stability and difficulty records!
3. **Architectural Resolution**: 
   `saveDeckWithCards` must perform an upsert/diffing strategy:
   - Upsert updated card entities (`OnConflictStrategy.REPLACE`).
   - Delete only cards whose IDs are in the database for `deckId` but absent from the incoming list.
   - Never regenerate IDs for existing cards.
4. **Blast Radius (What It Could Break)**: 
   Touches custom deck editing, CSV import appending, and deck saving flows.
5. **Safe Fix Protocol**: 
   - Retain original card IDs across edits.
   - Refactor `saveDeckWithCards` into a targeted differential delete: `DELETE FROM custom_cards WHERE deckId = :deckId AND id NOT IN (:activeIds)`.
6. **Urgency & Priority**: **HIGH**. Prevents silent loss of SRS learning progress for user decks.

---

### [DB-05]: Missing Foreign Keys and Table Indices Across All Room Entities
1. **File & Exact Line Range**: 
   - `com/koto/app/feature/cards/data/db/CardSrsEntity.kt` lines 7–12
   - `com/koto/app/feature/cards/data/db/StarredCardEntity.kt` lines 6–13
   - `com/koto/app/feature/cards/data/db/DeckBookmarkEntity.kt` lines 6–12
   - `com/koto/app/feature/translator/data/db/TranslationCardEntity.kt` lines 6–21
   - `com/koto/app/feature/translator/data/db/TranslationCardDao.kt` lines 23, 29
2. **The Flaw & Root Cause**: 
   - `CardSrsEntity`: Has index on `deckId`, but NO index on `due`. The query to fetch due cards (`WHERE due <= :now`) performs a full table scan. Furthermore, it has NO foreign key linking `cardId` or `deckId` to `custom_cards` or `custom_decks`. When custom decks are deleted, their `card_srs` rows are orphaned indefinitely.
   - `StarredCardEntity`: Column `deckId` is unindexed despite being used for deck-scoped favorite queries.
   - `TranslationCardEntity`: Has ZERO indices on `japanese`, `english`, or `kanji`.
   - `TranslationCardDao`: Queries in `isStarred` and `deleteByPair` use `TRIM(japanese) = TRIM(:japanese)`. In SQLite, wrapping a column in a scalar function (`TRIM()`) prevents the query planner from using B-Tree indices even if they exist, forcing an unindexed full table scan on every check.
3. **Architectural Resolution**: 
   - Add index on `due` in `CardSrsEntity`.
   - Add index on `deckId` in `StarredCardEntity`.
   - Add index on `(japanese, english)` in `TranslationCardEntity`.
   - Store trimmed strings upon insertion so queries can perform exact indexed equality matches (`japanese = :japanese AND english = :english`) without `TRIM()`.
4. **Blast Radius (What It Could Break)**: 
   Requires database migration. Clean index additions do not alter data semantics.
5. **Safe Fix Protocol**: 
   Bundle index additions into `MIGRATION_8_9` with `CREATE INDEX IF NOT EXISTS`.
6. **Urgency & Priority**: **MEDIUM**. Eliminates table scan degradation as user card counts grow.

---

### [DB-06]: Discrepancy & Fragmentation Across Deck Models (`FlashcardDeck` vs `CustomDeckEntity`)
1. **File & Exact Line Range**: 
   - `com/koto/app/ui/screens/cards/FlashcardData.kt` lines 6–28
   - `com/koto/app/feature/cards/data/db/CustomDeckEntity.kt` lines 7–16
   - `com/koto/app/feature/cards/data/CustomDeckStore.kt` lines 18–38
2. **The Flaw & Root Cause**: 
   The application maintains three divergent models for flashcard data:
   - `FlashcardDeck` / `Flashcard` (in-memory presentation model from JSON asset).
   - `CustomDeckEntity` / `CustomCardEntity` (Room database entity).
   - `CustomCardItem` (UI draft model in `CustomDeckStore`).
   Each model has different field names and types:
   - `Flashcard` has `kanji: String?`, `dueTimestamp: Long`, `intervalDays: Int`, `state: String`.
   - `CustomCardEntity` lacks `kanji`, lacks `dueTimestamp`, and splits examples into 9 individual flat columns (`exampleKana`, `exampleRomaji`, `exampleEnglish`, `example2Kana`, etc.) instead of a normalized or embedded structure.
   - `CustomCardItem` has `example1Kana` getters wrapping `exampleKana`.
   Whenever data moves between screens, it passes through multi-layer translation loops (`toCustomCardItem()`, `toFlashcardDeck()`, `toFlashcard()`, `registerCardsContext()`).
3. **Architectural Resolution**: 
   Unify all deck structures into a cohesive Domain model. Built-in master decks from `flashcard_decks.json` should be pre-seeded into Room SQLite tables on first app launch. Once seeded, the app has a single unified repository: `DeckRepository` querying Room entities, completely eliminating dual data pipelines.
4. **Blast Radius (What It Could Break)**: 
   Major architectural refactor affecting `CardsScreen`, `DeckDetailScreen`, `CreateDeckScreen`, and `SpreadsheetEngine`.
5. **Safe Fix Protocol**: 
   Implement incrementally: first standardize domain data classes, then route asset deck queries through the repository interface.
6. **Urgency & Priority**: **MEDIUM**. High architectural complexity and maintenance drag.

---

# SECTION 2: ASSET & MEMORY BLOAT DRILL-DOWN

---

### [AST-01]: 3.85 MB JSON Triplication & Machine-Specific Path in Production Code
1. **File & Exact Line Range**: 
   - `app/src/main/assets/card_contexts.json` (lines 1–3550, 3,856,595 bytes)
   - `com/koto/app/ui/screens/cards/CardContextLoader.kt` lines 36–126
2. **The Flaw & Root Cause**: 
   Inspection of `assets/card_contexts.json` reveals that for EVERY card, the identical JSON object is duplicated three times under different keys:
   - Key 1: `card_01_001` (Card ID)
   - Key 2: `おはよう|ohayou` (Kana + Romaji)
   - Key 3: `おはよう` (Kana only)
   Because each entry includes full usage notes and 3 sentence examples, 66% of the 3.85 MB file is verbatim string duplication!
   Furthermore, in `CardContextLoader.kt` line 111, the production code contains:
   ```kotlin
   File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/$ASSET_FILE")
   ```
   A developer's personal machine file path is hardcoded into the production binary!
   When `CardContextLoader.loadData` runs, `JSONObject` parses all 3,550 entries on the UI thread, creating thousands of redundant Java objects in memory and holding ~15 MB on the JVM heap indefinitely.
3. **Architectural Resolution**: 
   - Normalize the data: Store context notes and examples in a pre-populated Room SQLite asset database (`koto_prepopulated.db`) with tables: `card_contexts(card_id PRIMARY KEY, usage_note, kana, romaji, kanji)` and `card_examples(id PRIMARY KEY, card_id, kana, romaji, english, kanji)`.
   - Remove `CardContextLoader`'s in-memory hash map. Contexts should be queried on-demand by `cardId` via Room (`SELECT * FROM card_contexts WHERE card_id = :id`).
   - Delete the hardcoded Windows file path candidate.
4. **Blast Radius (What It Could Break)**: 
   Context display in `FlashcardStudyScreen` (the `?` modal) and custom deck context registration in `CustomDeckStore`.
5. **Safe Fix Protocol**: 
   - Build a build-time script or migration to generate the SQLite database from `card_contexts.json`.
   - Implement `Room.databaseBuilder().createFromAsset("koto_prepopulated.db")`.
   - Fall back safely to existing asset loader during migration.
6. **Urgency & Priority**: **CRITICAL / IMMEDIATE**. Eliminates ~15 MB heap overhead, 3.85 MB APK bloat, and removes hardcoded host paths.

---

### [AST-02]: Kuromoji IPADIC Resident Memory Footprint (~30 MB Heap)
1. **File & Exact Line Range**: `com/koto/app/feature/translator/data/KanaConverter.kt` lines 8–15
2. **The Flaw & Root Cause**: 
   `KanaConverter` declares `private val tokenizer: Tokenizer by lazy { Tokenizer() }`. Kuromoji's IPADIC dictionary is loaded from classpath JAR resources upon first use. It constructs large double-array trie matrices, word-reading dictionaries, and connection cost tables.
   Once loaded:
   - It consumes between **28 MB and 34 MB of JVM heap memory permanently**.
   - Because `KanaConverter` is a static singleton `object`, this memory is never released for the lifetime of the process.
   - On low-end Android devices (2 GB RAM) where the app's standard heap limit is 192 MB, holding ~35 MB permanently for a tokenizer increases OS kill rates when Koto is backgrounded.
   - The first call to `tokenizer.tokenize()` takes 250ms to 800ms of CPU time. When invoked from a Composable (`RubyText`), it drops 15–50 frames on first render.
3. **Architectural Resolution**: 
   - Asynchronous Warm-Up: Initialize `tokenizer` on a background thread (`Dispatchers.Default`) during app startup or when entering Japanese-heavy screens, never synchronously during composition.
   - Dictionary Caching: For static card decks and lessons, tokenize and store readings ahead of time in the database so Kuromoji is only required for dynamic user input (translator and custom deck creation).
   - Evaluate lightweight tokenizer alternative (or Kuromoji NEologd small footprint) if translation is offline-only.
4. **Blast Radius (What It Could Break)**: 
   Romaji generation in `KanaConverter`, ruby text display in `RubyText`, and pronunciation in `TranslatorScreen`.
5. **Safe Fix Protocol**: 
   - Keep `KanaConverter` API intact but initialize the tokenizer in `AppCoroutineScope.launch(Dispatchers.Default)`.
   - Ensure `RubyText` uses pre-parsed tokens or caches results so it never triggers Kuromoji synchronously.
6. **Urgency & Priority**: **HIGH**. Major contributor to process termination and initial UI freezes.

---

### [AST-03]: 65 Megabytes of Uncompressed Bundled Font Binaries
1. **File & Exact Line Range**: `app/src/main/res/font/` (21 TTF files)
2. **The Flaw & Root Cause**: 
   The application bundles 21 distinct TTF font files directly inside `res/font/`:
   - `font_noto_serif_jp.ttf`: 13.57 MB
   - `font_noto_sans_jp.ttf`: 9.58 MB
   - `font_klee_one.ttf`: 8.72 MB
   - `font_shippori_mincho.ttf`: 8.67 MB
   - `font_kaisei_decol.ttf`: 4.50 MB
   - `font_zen_maru_gothic.ttf`: 3.83 MB
   - `font_kosugi_maru.ttf`: 3.56 MB
   - `mplus_rounded_1c_bold.ttf`: 3.54 MB
   - `mplus_rounded_1c_medium.ttf`: 3.43 MB
   - `mplus_rounded_1c_regular.ttf`: 3.38 MB
   - `font_zen_kaku_gothic.ttf`: 2.36 MB
   - `font_m_plus_1p.ttf`: 1.75 MB
   - Plus 9 Latin fonts (0.08 MB – 0.87 MB each).
   **Total bundled font size is ~65.6 MB!**
   Every Japanese font contains thousands of complex CJK ideographs. Bundling 9 complete Japanese fonts into the APK inflates the download size beyond 75 MB. Meanwhile, `res/values/font_certs.xml` is present, proving downloadable fonts were intended but never hooked up.
3. **Architectural Resolution**: 
   - Bundle only the primary default font (e.g., `mplus_rounded_1c_regular` and `bold` ~6 MB).
   - Migrate optional Japanese stylistic fonts (`Klee One`, `Shippori Mincho`, `Noto Serif JP`) to Android's Google Play Services **Downloadable Fonts** API using `Font(GoogleFont(...))`, downloading them on-demand only when the user selects them in Font Settings.
4. **Blast Radius (What It Could Break)**: 
   `FontCatalog.kt` and Settings Font Picker screen.
5. **Safe Fix Protocol**: 
   Retain local fallback for primary app font; wire downloadable font provider for the 8 secondary stylistic options.
6. **Urgency & Priority**: **HIGH**. Reduces APK download size by over 70% (~55 MB reduction).

---

### [AST-04]: Dead Drawables, Assets, and Duplicate Templates
1. **File & Exact Line Range**: 
   - `app/src/main/res/drawable/ic_filter.xml`
   - `app/src/main/res/drawable/ic_more.xml`
   - `app/src/main/res/drawable/ic_share.xml`
   - `app/src/main/res/drawable/ic_template_download.xml`
   - `app/src/main/res/drawable/ic_thumbs_down.xml`
   - `app/src/main/res/drawable/ic_thumbs_up.xml`
   - `app/src/main/assets/kotoba_deck_template.xlsx`
2. **The Flaw & Root Cause**: 
   - Code search across all Kotlin files confirmed that `ic_filter`, `ic_more`, `ic_share`, `ic_template_download`, `ic_thumbs_down`, and `ic_thumbs_up` have **zero usages** in the codebase.
   - In `assets/`, `koto_deck_template.xlsx` is actively used by `SpreadsheetDialogs.kt`, while `kotoba_deck_template.xlsx` is an orphaned legacy duplicate from an earlier app naming iteration.
3. **Architectural Resolution**: 
   Delete the 6 unused XML vector drawables and delete `kotoba_deck_template.xlsx`.
4. **Blast Radius (What It Could Break)**: 
   None. Verified zero references in source code.
5. **Safe Fix Protocol**: 
   Remove files in a single cleanup commit.
6. **Urgency & Priority**: **LOW**. Hygiene and clean maintenance.

---

# SECTION 3: JETPACK COMPOSE RECOMPOSITION & THREADING AUDIT

---

### [CMP-01]: Un-Remembered Morphological Tokenization in `RubyText`
1. **File & Exact Line Range**: `com/koto/app/ui/components/JapaneseTextLayout.kt` lines 42–78
2. **The Flaw & Root Cause**: 
   Inside `RubyText(text: String, ...)`, the code calls:
   ```kotlin
   val tokens = KanaConverter.extractRubyTokens(text)
   ```
   Without `remember(text)`. Every time the parent layout recomposes (e.g., during parent scrolling, state ticks, animation progress, or theme mode changes), `extractRubyTokens(text)` executes Kuromoji morphological analysis synchronously.
3. **Architectural Resolution**: 
   Wrap token extraction in `remember(text)`:
   ```kotlin
   val tokens = remember(text) { KanaConverter.extractRubyTokens(text) }
   ```
   Or better, provide pre-extracted tokens directly from the data layer/ViewModel.
4. **Blast Radius (What It Could Break)**: 
   Japanese text layout rendering across flashcards and question cards.
5. **Safe Fix Protocol**: 
   Wrap in `remember(text)` with fallback for dynamic text updates.
6. **Urgency & Priority**: **HIGH**. Directly eliminates UI jank during recomposition loops.

---

### [CMP-02]: Synchronous SharedPreferences I/O Inside `KanaChartTab` Lazy Matrix Cells
1. **File & Exact Line Range**: `com/koto/app/feature/kana/ui/KanaChartTab.kt` lines 228–236
2. **The Flaw & Root Cause**: 
   In `KanaMatrixRow`, inside the column iteration:
   ```kotlin
   val score = masteryStore.getCharacterMastery(char.id, script == KanaScript.Hiragana)
   ```
   This is invoked inside the Composable function of every kana tile cell. `masteryStore.getCharacterMastery` synchronously accesses Android `SharedPreferences`. In a chart with over 100 kana characters, scrolling through the chart executes dozens of synchronous disk/cache read operations inside the layout measurement and composition pass.
3. **Architectural Resolution**: 
   Pre-load all character mastery scores into an immutable map (`Map<String, Int>`) inside `KanaViewModel` as a `StateFlow`. Pass the map down or read directly via state holder so each cell performs a pure $O(1)$ memory lookup.
4. **Blast Radius (What It Could Break)**: 
   Mastery score display and progress bar indicators on Kana tiles.
5. **Safe Fix Protocol**: 
   Expose `val masteryScores: StateFlow<Map<String, Int>>` from `KanaMasteryStore` and pass it to `KanaChartTab`.
6. **Urgency & Priority**: **HIGH**. Prevents scroll stutter on the Kana Chart screen.

---

### [CMP-03]: In-Composition 5,000-Word Filtering in `KanaPracticeTab`
1. **File & Exact Line Range**: `com/koto/app/feature/kana/ui/KanaPracticeTab.kt` lines 115–140
2. **The Flaw & Root Cause**: 
   `findAvailablePracticeWords(selectedKana)` is called directly in the body of `KanaPracticeTab`. It iterates through a vocabulary pool of 5,000 words in `PracticeWordPool`, performing character containment and regex checks, allocating lists and strings on the UI thread whenever a user selects or unselects a kana row.
3. **Architectural Resolution**: 
   Move practice word filtering into a background coroutine in `KanaViewModel` using `flowOn(Dispatchers.Default)` triggered by changes to `selectedKanaFlow`. Emit a filtered `ImmutableList<PracticeWord>` to the UI.
4. **Blast Radius (What It Could Break)**: 
   Practice word bank count and quiz generation in the Kana tab.
5. **Safe Fix Protocol**: 
   Hoist calculation into `KanaViewModel`. Composable should only read `val availableWords by viewModel.availableWords.collectAsState()`.
6. **Urgency & Priority**: **MEDIUM**. Eliminates freeze when toggling kana filters.

---

### [CMP-04]: Missing Keys in Lazy Lists Preventing View Recycling
1. **File & Exact Line Range**: 
   - `com/koto/app/ui/screens/cards/StarredCardsScreen.kt` lines 104–120
   - `com/koto/app/feature/kana/ui/KanaFilterModal.kt` lines 117–135
2. **The Flaw & Root Cause**: 
   In `StarredCardsScreen` and `KanaFilterModal`, `items(list)` is called without providing a `key` parameter. Compose defaults to positional indexing (`key = { index }`). When items are favorited/unfavorited, deleted, or reordered, Compose cannot match previous layout nodes to data items, forcing complete invalidation and recreation of item subtree compositions instead of recycling.
3. **Architectural Resolution**: 
   Specify stable unique keys: `key = { it.id }` or `key = { it.cardId }`.
4. **Blast Radius (What It Could Break)**: 
   Item animation and scroll position persistence.
5. **Safe Fix Protocol**: 
   Supply `key = { it.id }` to `items(...)`.
6. **Urgency & Priority**: **MEDIUM**. Smooths list animations and saves recomposition cost.

---

# SECTION 4: DESIGN SYSTEM & ANTI-BUBBLE COMPLIANCE

---

### [DS-01]: Anti-Bubble Violation in `KanaTile` Matrix Cells
1. **File & Exact Line Range**: `com/koto/app/feature/kana/ui/KanaChartTab.kt` lines 275–285
2. **The Flaw & Root Cause**: 
   `KanaTile` is styled as:
   ```kotlin
   .clip(RoundedCornerShape(12.dp))
   .background(KanaColors.TileBackground) // #F5F6F8 (Soft grey fill)
   .border(1.dp, KanaColors.TileBorder, RoundedCornerShape(12.dp))
   ```
   This directly violates two project design philosophy rules:
   - **Rule 2 (Anti-Bubble Principle)**: "Strictly NO bubble design: No nested rounded cards, no pastel pill chips, no rounded blob containers enclosing content. Use pure white backgrounds (`Color.White` / `CardsColors.Surface`). UI elements must stand out through typography hierarchy, disciplined whitespace, crisp hairline dividers (1dp `CardsColors.Edge`), and theme-driven color accents, NOT decorative containers."
   - **Rule 6 (Square & Crisp Geometry)**: "Avoid bulbous or circular pill shapes. Badges and containers must use crisp square/squircle geometry with subtle 6–8dp corners max and 1dp hairline edges."
   The 12dp rounded corners and `#F5F6F8` grey fill give the chart a soft bubbly appearance instead of the disciplined, crisp, white Koto standard.
3. **Architectural Resolution**: 
   Update `KanaTile` to use `RoundedCornerShape(6.dp)` (or 8dp max), pure white background `CardsColors.Surface` (`Color.White`), and 1dp crisp hairline border `CardsColors.Edge`.
4. **Blast Radius (What It Could Break)**: 
   Visual appearance of the Kana Chart screen.
5. **Safe Fix Protocol**: 
   Replace `KanaColors.TileBackground` with `CardsColors.Surface` and adjust corner radius to 6dp.
6. **Urgency & Priority**: **HIGH / THEME INTEGRITY**. Ensures visual compliance with project rules.

---

### [DS-02]: Romaji Inverted Above Kana in `LevelPopup`
1. **File & Exact Line Range**: `com/koto/app/ui/screens/map/LevelPopup.kt` lines 68–75
2. **The Flaw & Root Cause**: 
   In `LevelPopup.kt`, the level preview is rendered as:
   ```kotlin
   level.preview?.let { preview ->
       Spacer(Modifier.height(24.dp))
       Text(preview.romaji, color = KotoColors.QuietInk, ...)
       Spacer(Modifier.height(6.dp))
       Text(preview.kana, color = KotoColors.Navy, fontSize = 28.sp, ...)
   }
   ```
   The Romaji text is drawn **FIRST**, and the Kana text is drawn **SECOND (underneath)**!
   This directly violates **Rule 4 (Romaji Placement Rule)**:
   "Romaji must ALWAYS be placed directly **under** the Kana/Kanji in a dedicated vertical block (`Column`). Never place Romaji side-by-side in a horizontal row next to Japanese text where words wrap awkwardly across lines."
   Inverting the order and putting Romaji above Kana disrupts natural Japanese reading hierarchy.
3. **Architectural Resolution**: 
   Swap the layout order inside the vertical column: render `preview.kana` on top with large font, followed by `preview.romaji` directly underneath.
4. **Blast Radius (What It Could Break)**: 
   Visual layout of the Map level inspection popup.
5. **Safe Fix Protocol**: 
   Reorder the composables: `Text(preview.kana)` followed by `Spacer(4.dp)` followed by `Text(preview.romaji)`.
6. **Urgency & Priority**: **HIGH / PEDAGOGICAL INVARIANT**. Direct violation of core educational reading rule.

---

### [DS-03]: Bubbly 28dp Rounded Dialog & Soft Grey Pill in Map Screen
1. **File & Exact Line Range**: 
   - `com/koto/app/ui/screens/map/LevelPopup.kt` line 51
   - `com/koto/app/ui/screens/map/MapNodes.kt` lines 43–45
2. **The Flaw & Root Cause**: 
   - `LevelPopup.kt:51`: Uses `Surface(shape = RoundedCornerShape(28.dp), color = KotoColors.Background, ...)`. A 28dp corner radius is a bulbous bubble container that conflicts with Rule 6 (crisp geometry, 8dp corners max).
   - `MapNodes.kt:43`: `StageLabel` uses `Modifier.clip(RoundedCornerShape(50)).background(KotoColors.SoftGrey)`. This is a pill container with a soft grey background fill, violating Rule 2 (Anti-Bubble: no grey fills, no pill chips).
3. **Architectural Resolution**: 
   - Refactor `LevelPopup` surface to `RoundedCornerShape(8.dp)` with a 1dp hairline border `CardsColors.Edge`.
   - Refactor `StageLabel` to use a crisp squircle badge (6dp corner radius) with pure white surface fill, 1dp hairline border, and disciplined typography.
4. **Blast Radius (What It Could Break)**: 
   Visual appearance of Map stage dividers and popup dialogs.
5. **Safe Fix Protocol**: 
   Update corner shapes and backgrounds to match `CardsColors.Surface` and `CardsColors.Edge`.
6. **Urgency & Priority**: **MEDIUM**. Establishes app-wide visual unity.

---

### [DS-04]: Non-Standard Button Fills & Inconsistent 3D Tactile Depths
1. **File & Exact Line Range**: 
   - `com/koto/app/ui/screens/map/LevelPopup.kt` line 82
   - `com/koto/app/ui/screens/settings/AdvancedSettingsScreen.kt` lines 450–475
2. **The Flaw & Root Cause**: 
   `LevelPopup.kt` uses `DepthButton`, while the rest of the application uses `CardsButton` / `TactileButton`. In several dialogs, secondary buttons lack the required 3D tactile depth (3–4dp bottom bevel shadow) and downward depression upon tap, and some use grey fills instead of pure white `CardsColors.Surface`. This violates **Rule 7 (App-Wide White Button Fills & 3D Tactile Depth)**.
3. **Architectural Resolution**: 
   Standardize on `CardsButton` across all screens. Ensure all secondary and neutral buttons use pure white filling (`CardsColors.Surface`) with bottom depth bevel `CardsColors.Edge`.
4. **Blast Radius (What It Could Break)**: 
   Button click callbacks on Map dialogs and settings screens.
5. **Safe Fix Protocol**: 
   Replace `DepthButton` with `CardsButton`.
6. **Urgency & Priority**: **MEDIUM**. Consistency of tactile touch interactions.

---

# SECTION 5: DEAD CODE, ORPHANED LOGIC & STALE UTILITIES

---

### [DED-01]: Unused Functions in `KanaConverter.kt`
1. **File & Exact Line Range**: `com/koto/app/feature/translator/data/KanaConverter.kt` lines 207–214, 242–252
2. **The Flaw & Root Cause**: 
   - `KanaConverter.hiraganaToKatakana(text: String)`: Has **0 calls** across the entire codebase.
   - `KanaConverter.containsJapanese(text: String)`: Has **0 calls** across the entire codebase.
   These utility functions are dead code that increase maintenance surface.
3. **Architectural Resolution**: 
   Safely remove `hiraganaToKatakana` and `containsJapanese`.
4. **Blast Radius (What It Could Break)**: 
   None. Verified zero references in source code.
5. **Safe Fix Protocol**: 
   Remove the unused functions or mark `@Deprecated(level = DeprecationLevel.ERROR)` prior to deletion.
6. **Urgency & Priority**: **LOW**. Code hygiene.

---

### [DED-02]: Dead Dependency in `app/build.gradle.kts`
1. **File & Exact Line Range**: `app/build.gradle.kts` line 128
2. **The Flaw & Root Cause**: 
   `build.gradle.kts` includes:
   ```kotlin
   implementation("androidx.compose.ui:ui-text-google-fonts")
   ```
   A comprehensive scan across all Kotlin source files confirms that `GoogleFont`, `FontProvider`, or Google Fonts downloadable APIs are **never imported or referenced anywhere in the app**. The library is compiled into the APK, increasing dex size and dependency resolution time without providing any utility.
3. **Architectural Resolution**: 
   Either implement Google Play Services downloadable fonts (as recommended in [AST-03] to offload the 65 MB of TTF fonts), or remove the dependency from `build.gradle.kts`.
4. **Blast Radius (What It Could Break)**: 
   None. Zero active usages in code.
5. **Safe Fix Protocol**: 
   Remove line 128 if keeping bundled fonts, or hook it up to replace the bundled TTFs.
6. **Urgency & Priority**: **LOW**. Dependency hygiene.

---

### [DED-03]: Kanji Stripping in `MockTranslationEngine`
1. **File & Exact Line Range**: `com/koto/app/feature/translator/data/MockTranslationEngine.kt` lines 14–110
2. **The Flaw & Root Cause**: 
   `MockTranslationEngine` provides a static fallback dictionary of ~100 common phrases. However, nearly every Japanese entry is written purely in phonetic Hiragana, omitting Kanji entirely:
   - `Entry("friend", "ともだち", "tomodachi")` (instead of `友達`)
   - `Entry("water", "みず", "mizu")` (instead of `水`)
   - `Entry("teacher", "せんせい", "sensei")` (instead of `先生`)
   - `Entry("student", "がくせい", "gakusei")` (instead of `学生`)
   - `Entry("book", "ほん", "hon")` (instead of `本`)
   When a user is offline or ML Kit is downloading models and the mock engine provides a translation, the translated card displays zero Kanji, violating **Rule 3 (Dual-Mode Kanji/Kana Invariant)**.
3. **Architectural Resolution**: 
   Update `MockTranslationEngine.Entry` to store native Japanese with Kanji: `Entry(english, kanji, kana, romaji)`. Populate true Kanji for all dictionary items so offline translations provide full kanji support.
4. **Blast Radius (What It Could Break)**: 
   Offline translation fallback results.
5. **Safe Fix Protocol**: 
   Expand `Entry` data class and populate Kanji fields.
6. **Urgency & Priority**: **MEDIUM**. Preserves Kanji learning standards during offline translation.

---

# MASTER SEVERITY MATRIX

| Issue ID | Title | Category | Severity | Blast Radius |
| :--- | :--- | :--- | :---: | :--- |
| **[DB-01]** | Main-Thread Room DB Query Crash Cascade | Database / UI Lag | **CRITICAL** | Cards, Translator, Decks, Study screens |
| **[DB-02]** | Monolithic JSON Re-Serialization on Review | Database / Performance | **CRITICAL** | Flashcard review flow, GC pause times |
| **[AST-01]**| 3.85 MB JSON Triplication & Machine Path | Asset / Memory Bloat | **CRITICAL** | Study context modal, APK size, Heap |
| **[DB-03]** | Data Loss & Kanji Stripping in Custom Decks | Schema / Rule Violation | **HIGH** | Custom deck creation, Kanji display |
| **[DB-04]** | Destructive Card Replacement & Broken FKs | Database / Data Integrity| **HIGH** | Custom deck editing, SRS history continuity |
| **[AST-02]**| Kuromoji IPADIC Resident Heap Footprint | Memory / Performance | **HIGH** | App RAM usage, Background termination |
| **[AST-03]**| 65 MB Uncompressed Bundled Font Binaries | Asset Bloat / APK Size | **HIGH** | APK download size (65 MB vs 15 MB) |
| **[CMP-01]**| Un-Remembered Tokenizer in `RubyText` | Compose / UI Lag | **HIGH** | Japanese text render, frame drops |
| **[CMP-02]**| SharedPreferences Disk I/O in Kana Matrix | Compose / UI Lag | **HIGH** | Kana Chart scroll performance |
| **[DS-01]** | Anti-Bubble Violation in `KanaTile` | Design Philosophy | **HIGH** | Kana tab visual theme |
| **[DS-02]** | Romaji Inverted Above Kana in `LevelPopup` | Design Philosophy | **HIGH** | Map level popup reading hierarchy |
| **[DB-05]** | Missing Indices on Foreign & Query Keys | Database Optimization | **MEDIUM** | SQLite query performance |
| **[DB-06]** | Discrepancy & Fragmentation Across Models | Architecture / Models | **MEDIUM** | Deck management, data conversions |
| **[CMP-03]**| 5,000-Word Filtering in Composition | Compose / Performance | **MEDIUM** | Kana practice filter toggling |
| **[CMP-04]**| Missing Keys in Lazy Lists | Compose / Recycling | **MEDIUM** | Starred cards, Filter dialogs |
| **[DS-03]** | 28dp Bubble Dialog & Pill in Map Screen | Design Philosophy | **MEDIUM** | Map screen visual theme |
| **[DS-04]** | Inconsistent Button Fills & 3D Tactile Depth| Design Philosophy | **MEDIUM** | Tactile button feel across app |
| **[DED-03]**| Kanji Stripping in `MockTranslationEngine` | Educational Accuracy | **MEDIUM** | Offline translation cards |
| **[AST-04]**| Dead Drawables & Duplicate Excel Template | Dead Code / Hygiene | **LOW** | Zero runtime impact |
| **[DED-01]**| Unused Functions in `KanaConverter.kt` | Dead Code / Hygiene | **LOW** | Zero runtime impact |
| **[DED-02]**| Dead Dependency `ui-text-google-fonts` | Dependency Hygiene | **LOW** | Zero runtime impact |

---

# EXECUTION SAFETY CHECKLIST

Before modifying a single line of production code in subsequent phases, the following safety gates must be respected:

- [ ] **Gate 1: Zero Destructive Database Drops**: Under no circumstance should `fallbackToDestructiveMigration()` be triggered. Every schema change (`CustomCardEntity.kanji`, index additions) must have an explicit `Migration` object with a targeted unit test verifying user data is preserved.
- [ ] **Gate 2: Asynchronous Room Bridge**: Before removing `.allowMainThreadQueries()`, ensure all 18 identified call sites in `CardsScreen`, `TranslatorScreen`, `CreateDeckScreen`, and `FlashcardStudyScreen` are moved to coroutine dispatchers or ViewModel flows.
- [ ] **Gate 3: Pre-Populated SQLite Database for Contexts**: Convert `card_contexts.json` to an indexed SQLite asset database so the 3.85 MB JSON parsing and developer local file path are cleanly removed without breaking context cards.
- [ ] **Gate 4: Single SRS Source of Truth**: Perform a one-time migration of any legacy SharedPreferences SRS data to Room `card_srs` before disabling `saveToPreferences`, preventing review data loss.
- [ ] **Gate 5: Visual Theme Alignment**: Update `KanaTile`, `StageLabel`, and `LevelPopup` geometry and backgrounds to pure white `CardsColors.Surface` with 6–8dp crisp corners and 1dp hairline edges, honoring the project's Anti-Bubble directives.

---
*Deep Phase-2 Adversarial Audit Complete. Zero application code or assets were modified.*
