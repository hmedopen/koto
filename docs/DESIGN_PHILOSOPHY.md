# Koto Design Philosophy, Architecture & Project Guidelines

This document codifies the non-negotiable design principles, spatial layout rules, component styling standards, and architectural roadmap for Koto. Every screen, component, dialog, and interaction flow in the application must strictly adhere to these rules.

---

## 1. Product Scope & Roadmap Horizon (100% Total Vision)

To prevent scope creep and ensure we never reinvent the wheel, the overall application is divided into 4 core pillars:

| Pillar | Total Scope % | Current Status % | Description |
| :--- | :---: | :---: | :--- |
| **Cards System** | **20%** | **~19% Done** | Bulky foundation is complete: Room database, SRS engine (Again/Hard/Good/Easy), 50 master decks, dual Kanji/Kana toggle, bulk spreadsheet import/export (.xlsx / .csv), custom decks. Minor polish remaining. |
| **The Map** | **30%** | **~1% Done** | Vast progression world, interactive stages, milestones, node paths, and gamified mastery. |
| **The Learn Tab** | **40%** | **~5% Done** | Offline/hybrid translation engine complete. Major curriculum modules ahead: grammar library, sentence mechanics, listening lab, kanji stroke canvas, structured lessons. |
| **Buffer / Polish** | **10%** | **Ongoing** | Room to breathe: edge cases, performance optimizations, OEM voice fallbacks, app store packaging. |
| **Overall App** | **100%** | **~25% Done** | Solid, satisfying foundation ready to showcase, with clear paths forward. |

---

## 2. The Anti-Bubble Principle (Zero "Bubble Design")

Koto is designed with a **clean, refined Japanese editorial aesthetic**. We explicitly reject cartoonish, bubbly, and rounded-blob UI designs.

### Prohibited Patterns
* ❌ **No nested bubble cards**: Do not wrap content sections inside multiple layers of rounded border containers with background fills.
* ❌ **No pastel blob chips / pill badges**: Do not enclose stats, labels, or numbers inside rounded pastel-tinted pill containers.
* ❌ **No decorative bubble clutter**: Visual appeal must never rely on decorative cards, bubbles, or container nesting.

### Mandatory Standards
* ✅ **Pure White Surfaces**: Dialogs, content panels, and screen backgrounds must use pure white (`Color.White` / `CardsColors.Surface`).
* ✅ **Impact Through Layout & Typography**: Visual hierarchy must be driven exclusively by:
  - **Typographic hierarchy**: Font weight, size, letter-spacing, and line height.
  - **Disciplined whitespace & layout**: Clean margins, generous breathing room, and structured alignment.
  - **Crisp hairline dividers**: Subtle 1dp lines (`CardsColors.Edge`) instead of nested boxes.
  - **Theme-driven accent colors**: Direct, purposeful text/icon coloring (e.g., Blue for Due, Coral for Weak, Green for Mastered) directly on the white background.
* ⚠️ **Sole Exception for Slidable Carousel Cards**:
  - Bubble containers are strictly forbidden throughout the app, with the single exception of **moving, slidable carousel cards** (such as sentence examples inside `HorizontalPager`). Here, an independent card shape is functionally required for swiping.
  - All static notes, grammar explanations, definitions, and metadata must sit directly on the pure white surface as clean text without enclosing cards, borders, or backgrounds.

---

## 3. Dual-Mode Kanji/Kana Invariant

Japanese learning requires both Kanji and Kana. Beginners start with Kana, but mastering Japanese demands Kanji.
* **Never Strip Kanji**: In flashcards, question flows, and contextual popups (`?`), Japanese text must **NEVER** strip or lose Kanji.
* **App-Wide Display Toggle**:
  - **Kanji Mode**: Full kanji with ruby furigana (or kanji only based on user preferences).
  - **Kana Mode**: Pure hiragana/katakana representation.
* Both modes must stay perfectly populated across all decks and dynamic dialogs.

---

## 4. Romaji Placement Rule

* Romaji must **ALWAYS** sit directly **under** the Kana/Kanji in a dedicated vertical block (`Column`).
* Never place Romaji side-by-side in a horizontal row next to Japanese text, where dynamic length causes broken, awkward wrapping across lines.

---

## 5. Glued Spatial Layouts (Zero Jumping UI)

Interactive elements and controls must remain glued in place.
* **Header & Title Slots**: Dynamic text length (e.g., a 1-line title vs. a 3-line title) must **NEVER** push, displace, or jump buttons located below it.
  - Fixed-height header slots must be used (e.g., `heightIn(min = 64.dp)`, `maxLines = 2`, `TextOverflow.Ellipsis`).
  - Actions and primary buttons must stay anchored at fixed coordinates or fixed screen regions.
* **Non-Scrollable Action Screens**: Dedicated interaction and selection screens (such as `DeckDetailScreen`) must remain **completely non-scrollable**. Users must never experience accidental vertical dragging when trying to tap buttons or study controls.

---

## 6. Square & Crisp Geometry

Geometric forms in Koto must project precision and craft.
* **Corner Radii**: Avoid bulbous or circular pill shapes. Containers, cards, and badges must use disciplined, subtle corner rounding:
  - **6.dp to 8.dp** for badges, icons, and small containers.
  - **14.dp to 16.dp** maximum for large surfaces and dialog windows.
* **Deck Artwork & Miniatures**:
  - Miniatures and icons must be housed in **crisp square badges** (e.g., 52×52 dp or 56×56 dp with `RoundedCornerShape(8.dp)` and `1.dp border`).
  - Icons must sit comfortably in their own dedicated space with breathing room, never awkwardly floating or squeezed against multi-line text.

---

## 7. App-Wide Button Filling & Tactile Depth

* **Pure White Button Fills (No Grey Fillings)**:
  - Neutral and secondary buttons across the entire app must have a **pure white filling** (`CardsColors.Surface` / `Color.White`) with tactile edge depth (`CardsColors.Edge`).
  - Never give neutral buttons grey, ice, or murky fillings (`CardsColors.Ice` or grey backgrounds are forbidden for buttons).
  - Primary/accent buttons retain their theme colors (such as primary Blue `CardsColors.Blue` or Green).
* **Tactile 3D Physical Feedback**:
  - Buttons use physical depth cues (`TactileButton`, `CardsPressable`): 3-4dp base depth edge and downward press travel displacement.

---

## 8. Unified Custom Deck Standard

* All custom-created or imported decks share a single, unified custom deck icon to clearly distinguish them.
* Remove redundant icon selection pickers during custom deck creation to eliminate friction and speed up deck building.

---

## 9. Native Lightweight Engineering Principle

* Avoid heavy, bloated external dependencies (such as Apache POI) that inflate APK size or cause JVM runtime conflicts.
* Leverage native platform APIs: JDK XML DOM (`DocumentBuilderFactory`), RFC-4180 streaming CSV with UTF-8 BOM, Room database, and offline-first design.

---

## 10. Navigation & Dismiss Standards

* **Top Dismiss Controls**:
  - Popups and dialogs place a tactile `[X]` dismiss button at the **top-right** (or top-left `[←]` back button for screens).
  - Never duplicate dismiss controls with redundant bottom "Close" buttons. The bottom of modals is reserved for primary forward-moving actions.
* **Dedicated Full Screens**:
  - Structured content lists (such as viewing all cards in a deck) must be presented as **dedicated full screens** with a top-right `[X]` dismiss button and horizontal slide-and-fade motion (`slideInHorizontally + fadeIn`), completely avoiding bottom-sheet anchor glitches, drag buffering, or void gaps.
