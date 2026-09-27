package com.koto.app.ui.screens.cards

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.screens.map.SettingsSheet

@Composable
internal fun FlashcardStudyScreen(deck: FlashcardDeck, state: FlashcardState, update: (FlashcardState) -> Unit) {
    if (state.complete) {
        BoxWithConstraints(Modifier.fillMaxSize().testTag("study_complete")) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight).padding(horizontal = 24.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Deck complete", color = CardsColors.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(36.dp))
                CompletionStats(state.counts(deck))
                Spacer(Modifier.height(44.dp))
                CardsButton("Practice again", { update(state.start(deck)) }, Modifier.fillMaxWidth().testTag("practice_again"))
                Spacer(Modifier.height(14.dp))
                CardsButton("Back to deck", { update(state.back()) }, Modifier.fillMaxWidth().testTag("back_to_deck"),
                    background = CardsColors.Ice, ink = CardsColors.Ink, depth = CardsColors.IceDepth)
            }
        }
        return
    }
    val card = deck.cards.firstOrNull { it.id == state.currentId } ?: return
    val context = LocalContext.current
    val audio = remember(context) { JapaneseTtsController.get(context) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var exitRequested by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(card.id) { onDispose { audio.stop() } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollWholeScreen = maxHeight < 420.dp || LocalDensity.current.fontScale > 1.5f
        val cardHeight = when {
            maxHeight < 520.dp -> 170.dp
            maxHeight < 600.dp -> 230.dp
            else -> 310.dp
        }
        Column(Modifier.fillMaxSize().let { if (scrollWholeScreen) it.verticalScroll(rememberScrollState()) else it }
            .testTag("flashcard_study")) {
            SessionControlBar(state.index.toFloat() / state.order.size, { audio.stop(); exitRequested = true },
                { audio.stop(); settings = true }, "cards_back", "study_progress", "cards_settings",
                "Close cards", "Cards settings")
            Column((if (scrollWholeScreen) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()))
                .fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
                .offset { IntOffset(0, (-56).dp.roundToPx()) },
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                key(card.id) {
                    val transition = updateTransition(state.revealed, label = "Card flip")
                    val rotation by transition.animateFloat(transitionSpec = { tween(360) }, label = "Card rotation") {
                        if (it) 180f else 0f
                    }
                    val japaneseVisible = state.japaneseFirst != (rotation > 90f)
                    // Wait until the Japanese face is settled; never reveal the
                    // answer through audio while English is visible or flipping.
                    val allowAudio = japaneseVisible && !transition.isRunning &&
                        (state.japaneseFirst != state.revealed)
                    LaunchedEffect(allowAudio) { if (!allowAudio) audio.stop() }
                    StudyCard(card, state, cardHeight, rotation, transition.isRunning) {
                        audio.stop()
                        update(state.flip())
                    }
                    Spacer(Modifier.height(12.dp))
                    AudioButton(card, audio, allowAudio)
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
private fun CompletionStats(counts: DeckCounts) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val stats = listOf(Triple("New", counts.new, CardsColors.Blue),
            Triple("Weak", counts.weak, CardsColors.Coral),
            Triple("Mastered", counts.mastered, CardsColors.Green))
        stats.forEachIndexed { index, (label, count, color) ->
            if (index > 0) Box(Modifier.width(1.dp).height(44.dp).background(CardsColors.Edge))
            Column(Modifier.weight(1f).padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$count", color = color, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("stat_${label.lowercase()}"))
                Text(label, color = CardsColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AudioButton(card: Flashcard, audio: JapaneseTtsController, japaneseVisible: Boolean) {
    val ready = japaneseVisible && audio.enabled && audio.status == SpeechStatus.Ready
    // The tactile button may defer a pointer callback. Recheck the latest face
    // before speaking so a queued tap cannot play after a flip to English.
    val canPlay by rememberUpdatedState(ready)
    SpeakerButton(JapaneseText(card.japanese, card.romaji), ready, audio.isSpeaking,
        { if (canPlay) audio.speak(it) }, Modifier.testTag("play_audio"),
        description = if (japaneseVisible) "Play Japanese: ${card.romaji}" else "Audio available on the Japanese side")
}

@Composable
private fun StudyCard(card: Flashcard, state: FlashcardState, minHeight: Dp,
    rotation: Float, isFlipping: Boolean,
    flip: () -> Unit) {
    val backVisible = rotation > 90f
    val japaneseVisible = state.japaneseFirst != backVisible
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("study_card")
        .clickable(interactionSource = null, indication = null, role = Role.Button,
            onClickLabel = if (state.revealed) "Show question" else "Reveal answer") {
            if (!isFlipping) flip()
        }.semantics {
        stateDescription = if (state.revealed) "Answer revealed" else "Question"
    }) {
        Column(Modifier.fillMaxWidth().graphicsLayer {
            rotationY = if (backVisible) rotation - 180f else rotation
            cameraDistance = 16 * density
        }.shadow(1.dp, shape, ambientColor = Color(0x3323334A), spotColor = Color(0x3323334A)).clip(shape)
            .background(CardsColors.Surface).border(1.dp, CardsColors.Edge, shape)
            .padding(20.dp).heightIn(min = minHeight - 40.dp),
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
                            CardRating.Again -> KotoColors.WrongButtonFace
                            CardRating.Hard -> Color(0xFFA66A0B)
                            CardRating.Good -> KotoColors.CorrectButtonFace
                            CardRating.Easy -> CardsColors.Blue
                        }
                        val ink = Color.White
                        val depth = when (rating) {
                            CardRating.Again -> KotoColors.WrongButtonDepth
                            CardRating.Hard -> Color(0xFF754A07)
                            CardRating.Good -> KotoColors.CorrectButtonDepth
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
