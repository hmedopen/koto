# KOTOBA & KOTO UI/UX REFACTOR CHECKLIST

Tracked against `Kotoba_and_Koto_Refactor_Plan_Complete.docx` (Refactor Spec v2.0).

---

### Phase 1: Learn Tab Layout Restructure
- [x] 1.1 Rework top layout: Translate button converted into a full-width banner at the top of the content list.
- [x] 1.2 Side-by-side modules: Position "Kana" and "Kanji Canvas" beneath Translate as two equal-width 1:1 square buttons.
- [x] 1.3 Solid red background: Change the Kana button container from faint pink/tinted red to a solid, vibrant red matching the other cards.
- [x] 1.4 Icon styling: Remove the rounded square background container behind leading card icons. Render crisp white vector lines directly on the solid button background.
- [x] 1.5 Remaining hierarchy: Maintain "Phrasebook" and "Audio Lab" below the squares as full-width horizontal cards.

### Phase 2: Navigation Transitions & Global Theme
- [x] 2.1 Segmented control styling: Normalize Chart/Practice and Hiragana/Katakana segmented switches to match the Blue theme established in the Cards tab (Session Size selector).
- [x] 2.2 Top app bar action: Remove the red top-right audio button on the Kana screen. Replace it with the neutral, rounded Settings gear button matching the Cards screen.
- [x] 2.3 Universal slide transition: Replicate the coordinated navigation transition from the Translate module (parent screen shifts out while destination slides in) across all child routes launched from the Learn tab.

### Phase 3: Kana Chart View & Rating Engine
- [x] 3.1 Cell geometry: Change all Gojuon grid cells from tall rectangles into symmetrical 1:1 squares.
- [x] 3.2 Visual cleanup: Remove clutter badges ("46 Characters", "Gojuon Matrix").
- [x] 3.3 Typography: Increase the font size and visual hierarchy of group section titles (Basic, Dakuon, Handakuon, Yoon).
- [x] 3.4 Full character coverage: Add complete datasets for both Hiragana and Katakana (Basic 46, Dakuon 20, Handakuon 5, Yoon digraphs).
- [x] 3.5 Rating progress engine: Replace color-coded dots with a dynamic mini progress bar under each character tile that fills (0–100%) and changes color (Red -> Yellow -> Green -> Blue) based on Practice performance.

### Phase 4: Kana Detail View (Character Modal)
- [x] 4.1 Surface cleanup: Remove the outlined bounding box card; render all elements seamlessly on a clean white background.
- [x] 4.2 3D Close button: Replace the text "Close" with a raised, tactile 3D circular "X" button in the top-left corner.
- [x] 4.3 Glyph styling: Set the central Kana glyph to solid black (replacing red).
- [x] 4.4 Stroke order: Remove textual stroke explanations ("1 -> Horizontal stroke..."). Overlay numbered directional arrows directly onto the character path.
- [x] 4.5 Toolbar sizing: Standardize the bottom navigation row (Left arrow `<`, Center script flip pill, Right arrow `>`).

### Phase 5: Practice Dashboard & Selection Logic
- [x] 5.1 Deprecate old layout: Completely purge the legacy Practice tab layout, character checkboxes, and the bottom "Start Practice Session" button.
- [x] 5.2 Header controls: Add the neutral Settings gear and an elevated 3D Filter Button (icon with 2 slider lines and 2 dots).
- [x] 5.3 Metric cards: Add two prominent header stats: "Selected Kana" (count of selected characters) and "Available Words" (computed vocabulary count).
- [x] 5.4 Dynamic word count engine: Query the decks database, convert Kanji words to Kana, and count words that can be spelled exclusively using the currently selected Kana set.
- [x] 5.5 Granular filter drawer: Filter button opens a bottom-sheet with batch controls (`+` / `-` buttons to toggle entire rows/columns) and individual Kana toggles.
- [x] 5.6 Minimum validation constraint: Enforce a strict minimum of 15 selected Kana to start any quiz. Disable quiz launch and warn the user if selection is below 15.

### Phase 6: Quiz Catalog & Test Mastery Algorithm
- [x] 6.1 Quiz list UI: Redesign quiz options into solid-color cards matching the Learn tab (solid background, white line vector icon, title, subtitle, right arrow).
- [x] 6.2 Scope exclusion: Strictly omit the "Drawing" quiz mode.
- [x] 6.3 Test Quiz Engine (Mastery Benchmark): Configure Test mode as the ONLY quiz mode that modifies Kana progress bars and mastery states.
- [x] 6.4 Scoring logic: Implement scoring algorithm (+5 base points, +2 speed bonus for <50% time taken, 1.2x streak multiplier for 3+ consecutive correct, -4 penalty on wrong answer with streak reset).
- [x] 6.5 Supporting drill engines: Implement Listening Challenge, Word Selection, Matching Pairs, Word Building, and Typing Quiz (with custom virtual on-screen QWERTY keyboard).
