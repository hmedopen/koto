---
trigger: always_on
description: Non-negotiable UI, layout, architecture & project roadmap rules for Koto
---

# Koto UI, Architecture & Design Philosophy Rules

When modifying or creating any feature in Koto, you MUST adhere to the following rules:

---

## 1. Product Scope & Roadmap Horizon (100% Total Vision)
Keep this scope model locked in so we never reinvent the wheel or lose sight of where we are:
- **Cards System (20% total app scope — currently ~19% done)**:
  - Bulky foundation is complete: Room database, SRS rating engine, 50 master decks, dual Kanji/Kana mode toggle, bulk spreadsheet import/export (.xlsx / .csv), custom deck creation.
  - Only minor fine-tuning and polish remain.
- **The Map (30% total app scope — currently ~1% done)**:
  - Vast progression world, stages, milestones, node paths, and gamified mastery.
- **The Learn Tab (40% total app scope — currently ~5% done)**:
  - Offline/hybrid translation engine is complete.
  - The core curricula are ahead: grammar modules, sentence mechanics, listening lab, kanji stroke canvas, structured lessons.
- **Buffer / Room to Breathe (10% total app scope)**:
  - Reserved for edge cases, performance refinements, OEM voice fallbacks, polish.
- **Overall Completion: ~25%**.

---

## 2. Anti-Bubble Principle
- Strictly NO bubble design: No nested rounded cards, no pastel pill chips, no rounded blob containers enclosing content.
- Use pure white backgrounds (`Color.White` / `CardsColors.Surface`).
- UI elements must stand out through **typography hierarchy, disciplined whitespace, crisp hairline dividers (1dp CardsColors.Edge), and theme-driven color accents**, NOT decorative containers.
- **Sole Exception**: Moving, slidable carousel cards (e.g. `HorizontalPager` example cards) where an independent card boundary is functionally required to swipe. Static notes and explanations must remain pure text directly on white.

---

## 3. Dual-Mode Kanji/Kana Invariant
- Japanese learning requires both Kanji and Kana.
- In flashcards, question flows, and contextual popups (`?`), Japanese text must **NEVER** strip or lose Kanji.
- Always support the app-wide display mode toggle:
  - **Kanji Mode**: Full kanji with ruby furigana (or kanji only based on settings).
  - **Kana Mode**: Hiragana/katakana representation for beginners.
- Both modes must stay perfectly populated across all decks and dynamic dialogs.

---

## 4. Romaji Placement Rule
- Romaji must **ALWAYS** be placed directly **under** the Kana/Kanji in a dedicated vertical block (`Column`).
- Never place Romaji side-by-side in a horizontal row next to Japanese text where words wrap awkwardly across lines.

---

## 5. Glued Spatial Layouts (Zero Jumping UI)
- Interactive elements and buttons must remain glued to their places.
- Dynamic text length (e.g. 1-line vs 3-line title) must NEVER displace or push buttons.
- Use fixed header slots (e.g., `heightIn(min = 64.dp)`, `maxLines = 2`, `TextOverflow.Ellipsis`).
- Action/detail screens must remain non-scrollable when displaying structured workflows.

---

## 6. Square & Crisp Geometry
- Avoid bulbous or circular pill shapes. Badges and containers must use crisp square/squircle geometry with subtle 6–8dp corners max and 1dp hairline edges.
- Icons must sit comfortably in their own dedicated space with breathing room, never squeezed next to multi-line text.

---

## 7. App-Wide White Button Fills (No Grey Fillings) & 3D Tactile Depth
- Neutral and secondary buttons across the entire app must have pure white filling (`CardsColors.Surface`) with tactile edge depth (`CardsColors.Edge`).
- Never use grey, ice, or murky fillings (`CardsColors.Ice` or grey backgrounds are forbidden for buttons).
- Primary/accent buttons retain their theme colors (e.g., Primary Blue `CardsColors.Blue`).
- Every interactive button features 3D physical depth (3-4dp bottom bevel shadow) and downward depression upon tap.

---

## 8. Unified Custom Deck Standard
- All custom-created or imported decks share a single, unified custom deck icon.
- Remove redundant icon selection flows during custom deck creation; keep deck creation streamlined, rapid, and friction-free.

---

## 9. Native Lightweight Engineering Principle
- Avoid heavy bloat libraries (e.g. Apache POI, heavy external parsers) that inflate APK size or cause JVM runtime conflicts.
- Leverage native platform APIs: JDK XML DOM (`DocumentBuilderFactory`), RFC-4180 streaming CSV with UTF-8 BOM, standard Room database, and offline-first design.

---

## 10. Dismiss Navigation & Dedicated Full Screens
- Popups, dialogs, and secondary inspection views must use a tactile top-right `[X]` dismiss button (or top-left `[←]` for screens).
- Structured content lists (such as viewing all cards in a deck) must be presented as **dedicated full screens** with a top-right `[X]` dismiss button and horizontal slide-and-fade motion (`slideInHorizontally + fadeIn`), completely avoiding bottom-sheet anchor glitches, drag buffering, or void gaps.
- Never place redundant "Close" buttons at the bottom of modals. Bottom areas are reserved for forward-moving primary actions.
