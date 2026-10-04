# Koto Design Philosophy & UI Guidelines

This document codifies the non-negotiable design principles, spatial layout rules, and component styling standards for Koto. Every screen, component, dialog, and interaction flow in the application must strictly adhere to these rules.

---

## 1. The Anti-Bubble Principle (Zero "Bubble Design")

Koto is designed with a **clean, refined Japanese editorial aesthetic**. We explicitly reject cartoonish, bubbly, and rounded-blob UI designs (e.g., standard gamified "Duolingo-like" styling).

### Prohibited Patterns
* ❌ **No nested bubble cards**: Do not wrap content sections inside multiple layers of rounded border containers with background fills.
* ❌ **No pastel blob chips / pill badges**: Do not enclose stats, labels, or numbers inside rounded pastel-tinted pill containers (e.g., colored background boxes with `RoundedCornerShape(10.dp)`).
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

## 2. Glued Spatial Layouts (Zero Jumping UI)

Interactive elements and controls must remain glued in place. The user interface must provide visual stability and eliminate confusion.

### Rules
* **Header & Title Slots**: Dynamic text length (e.g., a 1-line title vs. a 3-line title) must **NEVER** push, displace, or jump buttons located below it.
  - Fixed-height header slots must be used (e.g., `heightIn(min = 64.dp)`, `maxLines = 2`, `TextOverflow.Ellipsis`).
  - Actions and primary buttons must stay anchored at fixed coordinates or fixed screen regions.
* **Non-Scrollable Action Screens**: Dedicated interaction and selection screens (such as `DeckDetailScreen`) must remain **completely non-scrollable**. Users must never experience accidental vertical dragging when trying to tap buttons or study controls.

---

## 3. Square & Crisp Geometry

Geometric forms in Koto must project precision and craft.

* **Corner Radii**: Avoid bulbous or circular pill shapes. Containers, cards, and badges must use disciplined, subtle corner rounding:
  - **6.dp to 8.dp** for badges, icons, and small containers.
  - **14.dp to 16.dp** maximum for large surfaces and dialog windows.
* **Deck Artwork & Miniatures**:
  - Miniatures and icons must be housed in **crisp square badges** (e.g., 52×52 dp or 56×56 dp with `RoundedCornerShape(8.dp)` and `1.dp border`).
  - Icons must sit comfortably in their own dedicated space with breathing room, never awkwardly floating or squeezed against multi-line text.

---

## 4. App-Wide Button Filling & Tactile Depth

* **Pure White Button Fills (No Grey Fillings)**:
  - Neutral and secondary buttons across the entire app must have a **pure white filling** (`CardsColors.Surface` / `Color.White`) with tactile edge depth (`CardsColors.Edge`).
  - Never give neutral buttons grey, ice, or murky fillings (`CardsColors.Ice` or grey backgrounds are forbidden for buttons).
  - **Accent Buttons**: Do not alter buttons with intentional primary theme colors (such as primary Blue `CardsColors.Blue` or Green).
* **Tactile 3D Physical Feedback**:
  - Buttons use physical depth cues (`TactileButton`, `CardsPressable`): 4dp base depth edge and 3–4dp downward press travel displacement.
  - Remove unnecessary flat decorative bars under primary action buttons.

---

## 5. Navigation & Dismiss Standards

* **Top Dismiss Controls**:
  - Popups and dialogs place a tactile `[X]` dismiss button at the **top-right** (or top-left `[←]` back button for screens).
  - **Never duplicate dismiss controls** with redundant bottom "Close" buttons. The bottom of modals is reserved for primary forward-moving actions (e.g., "START REVIEW", "Roll Again").
  - Structured content lists (such as viewing all cards in a deck) must be presented as **dedicated full screens** with a top-right `[X]` dismiss button and horizontal slide-and-fade motion, completely avoiding bottom-sheet anchor glitches, drag buffering, or void gaps.
  - Transitions between screens and cards must use clean **horizontal slide and fade** (`slideInHorizontally + fadeIn`), strictly avoiding vertical corner-scaling, zooming, or upward lifting.

---

## 6. Study Flow & Content Granularity

* **Romaji Placement Rule**:
  - Romaji must **ALWAYS** sit directly **under** the Kana/Kanji in a dedicated vertical block (`Column`).
  - Never place Romaji side-by-side in a horizontal row next to Japanese text, where dynamic length causes broken, awkward wrapping.
* **Anti-Cheat Controls**:
  - Context and auxiliary help buttons (such as the `?` information button) must sit outside the card surface.
  - They must remain locked/unclickable (`enabled = false`, dimmed alpha) until the card is flipped/revealed, preventing players from cheating before answering.
* **One Item at a Time (Slidable Cards)**:
  - Multi-item reference material (e.g., sentence examples) must never be crammed into long vertical text blocks.
  - Use dedicated, full-screen slidable cards (e.g., Compose `HorizontalPager`), presenting one example at a time with clean top hierarchy.
