package com.koto.app.ui.screens.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

@Composable
internal fun FlashcardStudyScreen(deck: FlashcardDeck, state: FlashcardState, update: (FlashcardState) -> Unit) {
    if (state.complete) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("study_complete"),
            verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            DeckBadge(deck)
            Text("A little stronger.", color = CardsColors.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Deck complete", color = CardsColors.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("You reviewed ${state.order.size} cards in ${deck.title}.", color = CardsColors.Muted, textAlign = TextAlign.Center)
            CardsPanel(Modifier.fillMaxWidth()) { DeckStats(state.counts(deck)) }
            CardsButton("Practice again", { update(state.start(deck)) }, Modifier.fillMaxWidth().testTag("practice_again"))
            CardsButton("Back to deck", { update(state.back()) }, Modifier.fillMaxWidth().testTag("back_to_deck"),
                background = CardsColors.Ice, ink = CardsColors.Ink, depth = CardsColors.IceDepth)
        }
        return
    }
    val card = deck.cards.firstOrNull { it.id == state.currentId } ?: return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollWholeScreen = maxHeight < 420.dp || LocalDensity.current.fontScale > 1.5f
        val cardHeight = when {
            maxHeight < 520.dp -> 190.dp
            maxHeight < 600.dp -> 250.dp
            else -> 310.dp
        }
        Column(Modifier.fillMaxSize().let { if (scrollWholeScreen) it.verticalScroll(rememberScrollState()) else it }
            .padding(20.dp).testTag("flashcard_study"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardsButton("‹  Deck", { update(state.back()) }, Modifier.testTag("cards_back"),
                    background = CardsColors.Ice, ink = CardsColors.Ink, depth = CardsColors.IceDepth)
                Text("${state.index + 1} / ${state.order.size}", color = CardsColors.Ink, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            LinearProgressIndicator(progress = { state.index.toFloat() / state.order.size },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = CardsColors.Blue, trackColor = CardsColors.IceDepth, drawStopIndicator = {})
            Column(if (scrollWholeScreen) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(deck.title, color = CardsColors.Muted, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                key(card.id) { StudyCard(card, state, cardHeight, { update(state.flip()) }) }
            }
            Text(if (state.revealed) "How well did you remember?" else "Reveal the answer, then choose a rating.",
                color = CardsColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            ResponseBar(state.revealed) { rating -> update(state.rate(card.id, rating)) }
        }
    }
}

@Composable
private fun StudyCard(card: Flashcard, state: FlashcardState, minHeight: Dp, flip: () -> Unit) {
    val rotation by animateFloatAsState(if (state.revealed) 180f else 0f, tween(320), label = "Flashcard flip")
    val backVisible = rotation > 90f
    val japaneseVisible = state.japaneseFirst != backVisible
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().padding(bottom = 5.dp)
        .graphicsLayer { rotationY = rotation; cameraDistance = 16 * density }
        .clickable(role = Role.Button, onClickLabel = "Flip flashcard", onClick = flip)
        .testTag("study_card").semantics { stateDescription = if (state.revealed) "Answer revealed" else "Question" }) {
        Box(Modifier.matchParentSize().graphicsLayer { translationY = 5.dp.toPx() }.background(CardsColors.IceDepth, shape))
        Column(Modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(CardsColors.Surface)
            .border(1.dp, CardsColors.Edge, shape).padding(20.dp)
            .graphicsLayer { rotationY = if (backVisible) 180f else 0f },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (japaneseVisible) "日本語" else "ENGLISH", color = CardsColors.Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(if (backVisible) "ANSWER" else "PROMPT", color = CardsColors.Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (japaneseVisible) card.japanese else card.english, color = CardsColors.Ink,
                    fontSize = if (japaneseVisible) 36.sp else 30.sp, lineHeight = 48.sp,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.testTag("card_word"))
                if (japaneseVisible && state.showRomaji) Text(card.romaji, color = CardsColors.Muted, fontSize = 16.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.testTag("card_romaji"))
            }
            Text(if (backVisible) "↻  Tap to see the front" else "↻  Tap to reveal answer", color = CardsColors.Blue,
                fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ResponseBar(enabled: Boolean, rate: (CardRating) -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val groups = CardRating.entries.chunked(if (maxWidth < 260.dp || largeText) 2 else 4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            groups.forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.forEach { rating ->
                        val background = when (rating) {
                            CardRating.Again -> Color(0xFFFCEDEC)
                            CardRating.Hard -> Color(0xFFFFF3DC)
                            CardRating.Good -> Color(0xFFE6F4EC)
                            CardRating.Easy -> CardsColors.Blue
                        }
                        val ink = when (rating) {
                            CardRating.Again -> CardsColors.Coral
                            CardRating.Hard -> CardsColors.Yellow
                            CardRating.Good -> CardsColors.Green
                            CardRating.Easy -> Color.White
                        }
                        val depth = when (rating) {
                            CardRating.Again -> Color(0xFFD99B97)
                            CardRating.Hard -> Color(0xFFD6B16C)
                            CardRating.Good -> Color(0xFF90BDA4)
                            CardRating.Easy -> CardsColors.BlueDepth
                        }
                        CardsButton(rating.name, { rate(rating) }, Modifier.weight(1f).testTag("rate_${rating.name.lowercase()}"),
                            background = background, ink = ink, depth = depth, enabled = enabled)
                    }
                }
            }
        }
    }
}
