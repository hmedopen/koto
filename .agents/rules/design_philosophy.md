---
trigger: always_on
description: Non-negotiable UI and layout design philosophy rules for Koto
---

# Koto UI & Layout Design Philosophy Rules

When modifying or creating any UI in Koto, you MUST adhere to the following rules:

1. **Anti-Bubble Principle**:
   - Strictly NO bubble design: No nested rounded cards, no pastel pill chips, no rounded blob containers enclosing content.
   - Use pure white backgrounds (`Color.White` / `CardsColors.Surface`).
   - UI elements must stand out through **typography hierarchy, disciplined whitespace, crisp hairline dividers (1dp CardsColors.Edge), and theme-driven color accents**, NOT decorative containers.
   - **Sole Exception**: Moving, slidable carousel cards (e.g. `HorizontalPager` example cards) where an independent card boundary is functionally required to swipe. Static notes and explanations must remain pure text directly on white.

2. **Romaji Placement Rule**:
   - Romaji must **ALWAYS** be placed directly **under** the Kana/Kanji in a dedicated vertical block (`Column`).
   - Never place Romaji side-by-side in a horizontal row next to Japanese text where words wrap awkwardly across lines.

3. **Glued Spatial Layouts (Zero Jumping UI)**:
   - Interactive elements and buttons must remain glued to their places.
   - Dynamic text length (e.g. 1-line vs 3-line title) must NEVER displace or push buttons.
   - Use fixed header slots (e.g., `heightIn(min = 64.dp)`, `maxLines = 2`, `TextOverflow.Ellipsis`).
   - Action/detail screens must remain non-scrollable when displaying structured workflows.

4. **Square & Crisp Geometry**:
   - Avoid bulbous or circular pill shapes. Badges and containers must use crisp square/squircle geometry with subtle 6–8dp corners max and 1dp hairline edges.
   - Icons must sit comfortably in their own dedicated space with breathing room, never squeezed next to multi-line text.

5. **App-wide White Button Fills (No Grey Fillings)**:
   - Neutral and secondary buttons across the entire app must have pure white filling (`CardsColors.Surface`) with tactile edge depth (`CardsColors.Edge`).
   - Never use grey, ice, or murky fillings (`CardsColors.Ice` or grey backgrounds are forbidden for buttons).
   - Primary/accent buttons retain their theme colors (e.g., Primary Blue `CardsColors.Blue`).

6. **Dismiss Navigation & Dedicated Full Screens**:
   - Popups, dialogs, and secondary inspection views (such as Deck Content preview) must use a tactile top-right `[X]` dismiss button (or top-left `[←]` for screens).
   - Structured content lists (such as viewing all cards in a deck) must be presented as **dedicated full screens** with a top-right `[X]` dismiss button and horizontal slide-and-fade motion, completely avoiding bottom-sheet anchor glitches, drag buffering, or void gaps.
   - Never place redundant "Close" buttons at the bottom of modals. Bottom areas are reserved for forward-moving primary actions.
   - Transitions between screens and cards must use clean **horizontal slide and fade** (`slideInHorizontally + fadeIn`), strictly avoiding vertical corner-scaling, zooming, or upward lifting.

7. **Anti-Cheat & Granular Content**:
   - Auxiliary help/context buttons (e.g. `?`) must sit outside the card and remain locked until user interaction.
   - Multi-item content must be shown one at a time on dedicated slidable cards (e.g., `HorizontalPager`).
