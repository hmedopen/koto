package com.koto.app.ui.screens.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.audio.SpeechStatus
import com.koto.app.feature.lesson.ui.SpeakerButton
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.ui.components.SessionControlBar
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone
import com.koto.app.ui.screens.map.SettingsSheet

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
    val context = LocalContext.current
    val audio = remember(context) { JapaneseTtsController.get(context) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var exitRequested by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(card.id) { onDispose { audio.stop() } }
    val progress by animateFloatAsState(state.index.toFloat() / state.order.size, tween(220), label = "Cards progress")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollWholeScreen = maxHeight < 420.dp || LocalDensity.current.fontScale > 1.5f
        val cardHeight = when {
            maxHeight < 520.dp -> 170.dp
            maxHeight < 600.dp -> 230.dp
            else -> 310.dp
        }
        Column(Modifier.fillMaxSize().let { if (scrollWholeScreen) it.verticalScroll(rememberScrollState()) else it }
            .testTag("flashcard_study")) {
            SessionControlBar(progress, { audio.stop(); exitRequested = true },
                { audio.stop(); settings = true }, "cards_back", "study_progress", "cards_settings",
                "Close cards", "Cards settings")
            Column((if (scrollWholeScreen) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()))
                .fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
                .offset { IntOffset(0, (-56).dp.roundToPx()) },
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                key(card.id) {
                    StudyCard(card, state, cardHeight, { update(state.flip()) })
                    Spacer(Modifier.height(12.dp))
                    AudioButton(card, audio)
                }
            }
            Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .offset { IntOffset(0, (-24).dp.roundToPx()) }) {
                ResponseBar(state.hasBeenRevealed) { rating -> update(state.rate(card.id, rating)) }
            }
        }
    }
    if (settings) SettingsSheet(onDismiss = { settings = false }, audio = audio)
    if (exitRequested) AlertDialog(
        onDismissRequest = { exitRequested = false },
        title = { Text("Quit this session?") },
        text = { Text("Your card stats will not be saved.") },
        confirmButton = {
            TactileButton({ update(state.back()); exitRequested = false },
                Modifier.fillMaxWidth().testTag("quit_session"), tone = TactileTone.Primary) {
                Text("Quit session", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
        dismissButton = {
            TactileButton({ exitRequested = false }, Modifier.fillMaxWidth().testTag("keep_studying"),
                tone = TactileTone.Default) {
                Text("Keep studying", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
    )
}

@Composable
private fun AudioButton(card: Flashcard, audio: JapaneseTtsController) {
    val ready = audio.enabled && audio.status == SpeechStatus.Ready
    SpeakerButton(JapaneseText(card.japanese, card.romaji), ready, audio.isSpeaking,
        audio::speak, Modifier.testTag("play_audio"))
}

@Composable
private fun StudyCard(card: Flashcard, state: FlashcardState, minHeight: Dp,
    flip: () -> Unit) {
    val transition = updateTransition(state.revealed, label = "Flashcard flip")
    val rotation by transition.animateFloat(transitionSpec = { tween(420) }, label = "Card rotation") {
        if (it) 180f else 0f
    }
    val backVisible = rotation > 90f
    val japaneseVisible = state.japaneseFirst != backVisible
    // Keep both faces upright as the solid card turns through its edge. The shared
    // button supplies the same physical press depth and ripple-free face as the header.
    TactileButton(flip, Modifier.fillMaxWidth().graphicsLayer {
        rotationY = if (backVisible) rotation - 180f else rotation
        cameraDistance = 16 * density
    }.testTag("study_card"), enabled = !transition.isRunning,
        tone = TactileTone.Quiet,
        stateLabel = if (state.revealed) "Answer revealed" else "Question",
        padding = PaddingValues(20.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(min = minHeight - 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (japaneseVisible) card.japanese else card.english, color = CardsColors.Ink,
                        fontSize = if (japaneseVisible) 36.sp else 30.sp, lineHeight = 48.sp,
                        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.testTag("card_word"))
                    if (japaneseVisible && state.showRomaji) Text(card.romaji, color = CardsColors.Muted, fontSize = 16.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.testTag("card_romaji"))
                }
            }
            Text(if (backVisible) "↻  Tap to see the front" else if (state.hasBeenRevealed) "↻  Tap to see the answer again" else "↻  Tap to reveal answer", color = CardsColors.Blue,
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
