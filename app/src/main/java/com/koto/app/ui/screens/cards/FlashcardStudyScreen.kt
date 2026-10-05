package com.koto.app.ui.screens.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.audio.SpeechStatus
import com.koto.app.feature.lesson.ui.SpeakerButton
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.ui.components.SessionControlBar
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.screens.map.SettingsSheet

private enum class StudyStage {
    Active,
    Context,
    Complete,
}

@Composable
internal fun FlashcardStudyScreen(deck: FlashcardDeck, state: FlashcardState, update: (FlashcardState) -> Unit) {
    val currentCard = deck.cards.firstOrNull { it.id == state.currentId }
    var lastActiveCard by remember { mutableStateOf<Flashcard?>(null) }
    if (currentCard != null) {
        lastActiveCard = currentCard
    }
    val card = currentCard ?: lastActiveCard

    if (card == null && !state.complete) return

    val context = LocalContext.current
    val audio = remember(context) { JapaneseTtsController.get(context) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var exitRequested by rememberSaveable { mutableStateOf(false) }
    var contextCard by remember { mutableStateOf<Flashcard?>(null) }
    var lastRatedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastRatingName by rememberSaveable { mutableStateOf<String?>(null) }
    val lastRating = lastRatingName?.let { name -> CardRating.entries.firstOrNull { it.name == name } }

    val currentStage = when {
        contextCard != null -> StudyStage.Context
        state.complete -> StudyStage.Complete
        else -> StudyStage.Active
    }

    if (card != null) {
        DisposableEffect(card.id) { onDispose { audio.stop() } }
    }

    AnimatedContent(
        targetState = currentStage,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            when {
                // Active ➔ Context (forward inspector push)
                initialState == StudyStage.Active && targetState == StudyStage.Context -> {
                    (slideInHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth },
                    ) + fadeIn(
                        animationSpec = tween(240, easing = LinearOutSlowInEasing),
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> -fullWidth },
                        ) + fadeOut(
                            animationSpec = tween(180, easing = FastOutLinearInEasing),
                        ),
                    ).using(null)
                }
                // Context ➔ Active (reverse pop return)
                initialState == StudyStage.Context && targetState == StudyStage.Active -> {
                    (slideInHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -fullWidth },
                    ) + fadeIn(
                        animationSpec = tween(240, easing = LinearOutSlowInEasing),
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> fullWidth },
                        ) + fadeOut(
                            animationSpec = tween(200, easing = FastOutLinearInEasing),
                        ),
                    ).using(null)
                }
                // Active ➔ Complete (clean horizontal forward slide & fade)
                initialState == StudyStage.Active && targetState == StudyStage.Complete -> {
                    (slideInHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth },
                    ) + fadeIn(
                        animationSpec = tween(250, easing = LinearOutSlowInEasing),
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> -fullWidth },
                        ) + fadeOut(
                            animationSpec = tween(200, easing = FastOutLinearInEasing),
                        ),
                    ).using(null)
                }
                // Complete ➔ Active (clean horizontal return slide & fade)
                initialState == StudyStage.Complete && targetState == StudyStage.Active -> {
                    (slideInHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -fullWidth },
                    ) + fadeIn(
                        animationSpec = tween(250, easing = LinearOutSlowInEasing),
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> fullWidth },
                        ) + fadeOut(
                            animationSpec = tween(200, easing = FastOutLinearInEasing),
                        ),
                    ).using(null)
                }
                // Default crossfade
                else -> {
                    (fadeIn(tween(250)) togetherWith fadeOut(tween(200))).using(null)
                }
            }
        },
        label = "Study session stage transition",
    ) { stage ->
        when (stage) {
            StudyStage.Active -> {
                if (card != null) {
                    val isCustomOrImported = deck.category == "Custom" ||
                        deck.id.startsWith("custom_") ||
                        deck.id == "deck_quick_translations" ||
                        deck.id == "starred_review" ||
                        deck.number == 0
                    ActiveStudyContent(
                        card = card,
                        state = state,
                        audio = audio,
                        update = update,
                        lastRatedCardId = lastRatedCardId,
                        lastRating = lastRating,
                        onRate = { rating ->
                            lastRatedCardId = card.id
                            lastRatingName = rating.name
                            update(state.rate(card.id, rating))
                        },
                        onOpenContext = { contextCard = card },
                        onOpenSettings = { settings = true },
                        onRequestExit = { exitRequested = true },
                        isCustomDeck = isCustomOrImported,
                    )
                }
            }
            StudyStage.Context -> {
                val targetCard = contextCard ?: card
                if (targetCard != null) {
                    CardContextScreen(
                        card = targetCard,
                        audio = audio,
                        onBack = { contextCard = null },
                    )
                }
            }
            StudyStage.Complete -> {
                StudyCompletionScreen(
                    deck = deck,
                    state = state,
                    update = update,
                )
            }
        }
    }

    if (settings) SettingsSheet(onDismiss = { settings = false }, audio = audio)
    if (exitRequested) AlertDialog(
        onDismissRequest = { exitRequested = false },
        title = { Text("Quit this session?") },
        text = { Text("Your card stats will not be saved.") },
        confirmButton = {
            TactileButton(
                { update(state.back()); exitRequested = false },
                Modifier.fillMaxWidth().testTag("quit_session"), tone = TactileTone.Primary,
            ) {
                Text("Quit session", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
        dismissButton = {
            TactileButton(
                { exitRequested = false }, Modifier.fillMaxWidth().testTag("keep_studying"),
                tone = TactileTone.Default,
            ) {
                Text("Keep studying", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
    )
}

@Composable
private fun StudyCompletionScreen(
    deck: FlashcardDeck,
    state: FlashcardState,
    update: (FlashcardState) -> Unit,
) {
    val counts = state.counts(deck)
    val isMastered = counts.mastered == deck.cards.size && deck.cards.isNotEmpty()

    val ceremony = remember { Animatable(if (isMastered) 0f else 1f) }
    LaunchedEffect(isMastered) {
        if (isMastered) {
            ceremony.animateTo(1f, animationSpec = tween(durationMillis = 2400, easing = LinearEasing))
        } else {
            ceremony.snapTo(1f)
        }
    }
    val t = ceremony.value

    // Animation Choreography:
    // Phase 1 (0..0.30): Large trophy cup in the center of pure white screen
    // Phase 2 (0.30..0.62): Trophy glides slowly up to top header badge and scales down
    // Phase 3 (0.58..0.80): Text ("DECK MASTERED!", title, description, stats) fades in slowly
    // Phase 4 (0.78..1.00): Action buttons ("Practice again", "Back to deck") fade in after text
    val trophyTransit = if (!isMastered) 1f else {
        val moveT = ((t - 0.30f) / 0.32f).coerceIn(0f, 1f)
        FastOutSlowInEasing.transform(moveT)
    }
    val textAlpha = if (!isMastered) 1f else {
        val textT = ((t - 0.58f) / 0.22f).coerceIn(0f, 1f)
        FastOutSlowInEasing.transform(textT)
    }
    val buttonAlpha = if (!isMastered) 1f else {
        val buttonT = ((t - 0.78f) / 0.22f).coerceIn(0f, 1f)
        FastOutSlowInEasing.transform(buttonT)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("study_complete")
            .then(if (isMastered) Modifier.testTag("mastery_celebration") else Modifier),
    ) {
        if (isMastered) {
            ConfettiParticleEffect(
                Modifier
                    .matchParentSize()
                    .alpha(trophyTransit)
            )
        }

        val trophyStartOffsetY = (maxHeight * 0.28f).coerceAtLeast(140.dp)
        val currentOffsetY = trophyStartOffsetY * (1f - trophyTransit)
        val currentScale = 1.0f + 0.85f * (1f - trophyTransit)

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isMastered) {
                Box(
                    Modifier
                        .graphicsLayer {
                            translationY = currentOffsetY.toPx()
                            scaleX = currentScale
                            scaleY = currentScale
                        }
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardsColors.Green.copy(alpha = 0.15f))
                        .border(2.dp, CardsColors.Green, RoundedCornerShape(22.dp))
                        .testTag("celebration_trophy_badge"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = "Mastered",
                        tint = CardsColors.Green,
                        modifier = Modifier.size(42.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = textAlpha
                            translationY = ((1f - textAlpha) * 16.dp.toPx())
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "DECK MASTERED!",
                        color = CardsColors.Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${deck.title} is 100% Mastered",
                        color = CardsColors.Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Every single card in this deck has reached mastery.",
                        color = CardsColors.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    "Deck complete",
                    color = CardsColors.Ink,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = textAlpha
                    },
            ) {
                CompletionStats(counts)
            }
            Spacer(Modifier.height(40.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = buttonAlpha
                        translationY = ((1f - buttonAlpha) * 20.dp.toPx())
                    },
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CardsButton(
                    "Practice again",
                    { update(state.start(deck, state.sessionSize)) },
                    Modifier.fillMaxWidth().testTag("practice_again"),
                )
                CardsButton(
                    "Back to deck",
                    { update(state.back()) },
                    Modifier.fillMaxWidth().testTag("back_to_deck"),
                    background = CardsColors.Surface,
                    ink = CardsColors.Ink,
                    depth = CardsColors.Edge,
                )
            }
        }
    }
}

@Composable
private fun ActiveStudyContent(
    card: Flashcard,
    state: FlashcardState,
    audio: JapaneseTtsController,
    update: (FlashcardState) -> Unit,
    lastRatedCardId: String?,
    lastRating: CardRating?,
    onRate: (CardRating) -> Unit,
    onOpenContext: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestExit: () -> Unit,
    isCustomDeck: Boolean = false,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollWholeScreen = maxHeight < 420.dp || LocalDensity.current.fontScale > 1.5f
        val cardHeight = when {
            maxHeight < 520.dp -> 170.dp
            maxHeight < 600.dp -> 230.dp
            else -> 310.dp
        }
        Column(
            Modifier.fillMaxSize().background(Color.White)
                .let { if (scrollWholeScreen) it.verticalScroll(rememberScrollState()) else it }
                .testTag("flashcard_study"),
        ) {
            SessionControlBar(
                state.index.toFloat() / state.order.size.coerceAtLeast(1),
                { audio.stop(); onRequestExit() },
                { audio.stop(); onOpenSettings() },
                "cards_back", "study_progress", "cards_settings",
                "Close cards", "Cards settings",
            )
            Column(
                (if (scrollWholeScreen) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()))
                    .fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
                    .offset { IntOffset(0, (-56).dp.roundToPx()) },
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val flipTransition = updateTransition(state.revealed, label = "Card flip")
                val currentRotation by flipTransition.animateFloat(transitionSpec = { tween(360) }, label = "Card rotation") {
                    if (it) 180f else 0f
                }
                val japaneseVisible = state.japaneseFirst != (currentRotation > 90f)
                val allowAudio = !flipTransition.isRunning
                LaunchedEffect(allowAudio) { if (!allowAudio) audio.stop() }

                // Anti-Cheat Context Action Button ("?"): Reserved solely for built-in curriculum decks
                // Locked and unclickable until card is revealed so the user cannot cheat
                val canInspectContext = state.revealed || state.hasBeenRevealed
                if (!isCustomDeck) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TactileButton(
                            onClick = { if (canInspectContext) onOpenContext() },
                            enabled = canInspectContext,
                            modifier = Modifier
                                .size(44.dp, 48.dp)
                                .testTag("card_context_button")
                                .alpha(if (canInspectContext) 1f else 0.38f),
                            tone = TactileTone.Quiet,
                            description = if (canInspectContext) "Card context & examples" else "Reveal card first to unlock context",
                            padding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = "?",
                                color = if (canInspectContext) CardsColors.Ink else CardsColors.Muted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val cardsLeft = state.order.size - state.index
                    if (cardsLeft > 1) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.96f)
                                .height(cardHeight - 40.dp)
                                .offset(y = 5.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                                .border(1.dp, CardsColors.Edge.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                        )
                    }

                    AnimatedContent(
                        targetState = card,
                        transitionSpec = {
                            val duration = 260
                            (slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 400f,
                                ),
                                initialOffsetX = { fullWidth -> (fullWidth * 0.92f).toInt() },
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> (-fullWidth * 0.92f).toInt() },
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = duration - 40, easing = FastOutLinearInEasing),
                                ),
                            ).using(null)
                        },
                        label = "Card slide transition",
                    ) { targetCard ->
                        val isCurrent = targetCard.id == state.currentId
                        val isExiting = targetCard.id == lastRatedCardId && !isCurrent
                        val cardRotation = if (isCurrent) currentRotation else 180f

                        StudyCard(
                            card = targetCard,
                            state = state,
                            minHeight = cardHeight,
                            rotation = cardRotation,
                            isFlipping = isCurrent && flipTransition.isRunning,
                            isCurrent = isCurrent,
                            isExiting = isExiting,
                            rating = if (isExiting) lastRating else null,
                        ) {
                            if (isCurrent) {
                                audio.stop()
                                update(state.flip())
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                AudioButton(card, audio, japaneseVisible, allowAudio)
            }
            Box(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                    .offset { IntOffset(0, (-24).dp.roundToPx()) },
            ) {
                ResponseBar(state.hasBeenRevealed) { rating ->
                    onRate(rating)
                }
            }
        }
    }
}

@Composable
private fun ConfettiParticleEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "Confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "Confetti progress",
    )

    val particles = remember {
        val colors = listOf(
            CardsColors.Green,
            CardsColors.Blue,
            CardsColors.Yellow,
            CardsColors.Coral,
            Color(0xFF10B981),
            Color(0xFF3B82F6),
        )
        List(48) { i ->
            val startX = (i * 37 % 100) / 100f
            val cycles = 1 + (i % 3)
            val offset = (i * 19 % 100) / 100f
            val size = 5f + (i * 7 % 7)
            val color = colors[i % colors.size]
            val swingCycles = 1 + (i % 2)
            ConfettiParticle(startX, cycles, offset, size, color, swingCycles)
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (p in particles) {
            val rawY = (progress * p.cycles + p.offset) % 1f
            val y = rawY * (h + 40f) - 20f
            val swingPhase = (progress * p.swingCycles + p.offset) * (2f * Math.PI.toFloat())
            val xOffset = kotlin.math.sin(swingPhase) * 18f
            val x = (p.startX * w + xOffset).coerceIn(8f, w - 8f)
            drawCircle(
                color = p.color.copy(alpha = 0.85f),
                radius = p.size,
                center = Offset(x, y),
            )
        }
    }
}

private class ConfettiParticle(
    val startX: Float,
    val cycles: Int,
    val offset: Float,
    val size: Float,
    val color: Color,
    val swingCycles: Int,
)

@Composable
private fun CardContextScreen(
    card: Flashcard,
    audio: JapaneseTtsController,
    onBack: () -> Unit,
) {
    val context = remember(card) {
        CardContextLoader.getContext(card.japanese, card.romaji)
    }
    val examples = context?.examples ?: emptyList()
    val pagerState = rememberPagerState(pageCount = { examples.size })
    val coroutineScope = rememberCoroutineScope()

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("card_context_dialog"),
    ) {
        // 1. Top Bar: Crisp Back button, Screen Title, and Main Word Pronunciation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TactileButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp, 48.dp).testTag("close_card_context"),
                tone = TactileTone.Quiet,
                description = "Back to cards",
                padding = PaddingValues(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = CardsColors.Ink,
                    modifier = Modifier.size(22.dp),
                )
            }

            Text(
                text = "CARD CONTEXT",
                color = CardsColors.Ink,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
            )

            SpeakerButton(
                text = JapaneseText(card.japanese, card.romaji),
                speechReady = audio.enabled && audio.status == SpeechStatus.Ready,
                isPlaying = audio.isSpeaking,
                speak = { audio.speak(it) },
                modifier = Modifier.size(44.dp, 48.dp),
                description = "Play pronunciation: ${card.romaji}",
            )
        }
        HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

        // 2. Top Context Section: Organized, Editorial Japanese Hierarchy (No bubble curves)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Kana and Romaji: Romaji ALWAYS directly under Kana in a Column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = card.japanese,
                    color = CardsColors.Ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp,
                )
                Text(
                    text = card.romaji,
                    color = CardsColors.Blue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = card.english,
                color = CardsColors.Ink,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            )

            if (context?.usageNote?.isNotEmpty() == true) {
                Text(
                    text = context.usageNote,
                    color = CardsColors.Ink,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

        // 3. Middle / Bottom Section: Slidable Sentence Example Cards (One at a time)
        if (examples.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Section Header with pagination count and slide controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SENTENCE EXAMPLES",
                        color = CardsColors.Ink,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} of ${examples.size}",
                            color = CardsColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        TactileButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier
                                .size(32.dp)
                                .alpha(if (pagerState.currentPage > 0) 1f else 0.35f),
                            tone = TactileTone.Quiet,
                            padding = PaddingValues(0.dp),
                        ) {
                            Text("‹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CardsColors.Ink)
                        }
                        TactileButton(
                            onClick = {
                                if (pagerState.currentPage < examples.size - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < examples.size - 1,
                            modifier = Modifier
                                .size(32.dp)
                                .alpha(if (pagerState.currentPage < examples.size - 1) 1f else 0.35f),
                            tone = TactileTone.Quiet,
                            padding = PaddingValues(0.dp),
                        ) {
                            Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CardsColors.Ink)
                        }
                    }
                }

                // HorizontalPager: Displays ONE example card at a time with smooth swiping
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 14.dp,
                ) { page ->
                    val ex = examples[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, CardsColors.Edge, RoundedCornerShape(10.dp))
                            .background(Color(0xFFFAFCFE))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "EXAMPLE ${page + 1}",
                                color = CardsColors.Blue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            )
                            SpeakerButton(
                                text = JapaneseText(ex.kana, ex.romaji),
                                speechReady = audio.enabled && audio.status == SpeechStatus.Ready,
                                isPlaying = audio.isSpeaking,
                                speak = { audio.speak(it) },
                                modifier = Modifier.size(38.dp, 42.dp),
                                description = "Play example sentence ${page + 1}",
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = ex.kana,
                                color = CardsColors.Ink,
                                fontSize = 19.sp,
                                lineHeight = 27.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = ex.romaji,
                                color = CardsColors.Blue,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.6f), thickness = 1.dp)
                            Text(
                                text = ex.english,
                                color = CardsColors.Ink,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                // Dot pagination indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(examples.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isSelected) 18.dp else 6.dp, 5.dp)
                                .background(
                                    if (isSelected) CardsColors.Blue else CardsColors.Edge,
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                }
            }
        } else {
            // Clean Fallback
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Sentence examples coming soon",
                        color = CardsColors.Muted,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        // 4. Bottom Action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            CardsButton(
                label = "Back to Study",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().testTag("close_card_context_bottom"),
                background = CardsColors.Blue,
                ink = Color.White,
                depth = CardsColors.BlueDepth,
            )
        }
    }
}

@Composable
private fun CompletionStats(counts: DeckCounts) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val stats = listOf(
            Triple("Due", counts.due, CardsColors.Blue),
            Triple("Weak", counts.weak, CardsColors.Coral),
            Triple("Mastered", counts.mastered, CardsColors.Green),
        )
        stats.forEachIndexed { index, (label, count, color) ->
            if (index > 0) Box(Modifier.width(1.dp).height(44.dp).background(CardsColors.Edge))
            Column(
                Modifier.weight(1f).padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "$count", color = color, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("stat_${label.lowercase()}"),
                )
                Text(label, color = CardsColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AudioButton(
    card: Flashcard,
    audio: JapaneseTtsController,
    japaneseVisible: Boolean,
    enabled: Boolean = true,
) {
    val ready = enabled && audio.enabled && audio.status == SpeechStatus.Ready
    val canPlay by rememberUpdatedState(ready)
    SpeakerButton(
        text = JapaneseText(card.japanese, card.romaji),
        speechReady = ready,
        isPlaying = audio.isSpeaking,
        speak = {
            if (canPlay) {
                if (japaneseVisible) {
                    audio.speak(it)
                } else {
                    audio.speakEnglish(card.english)
                }
            }
        },
        modifier = Modifier.testTag("play_audio"),
        description = if (japaneseVisible) "Play Japanese: ${card.romaji}" else "Play English: ${card.english}",
    )
}

@Composable
private fun StudyCard(
    card: Flashcard,
    state: FlashcardState,
    minHeight: Dp,
    rotation: Float,
    isFlipping: Boolean,
    isCurrent: Boolean = true,
    isExiting: Boolean = false,
    rating: CardRating? = null,
    flip: () -> Unit,
) {
    val backVisible = rotation > 90f
    val japaneseVisible = state.japaneseFirst != backVisible
    val shape = RoundedCornerShape(16.dp)

    val exitTilt by animateFloatAsState(
        targetValue = if (isExiting) -3.5f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "Exit tilt",
    )

    val ratingColor = when (rating) {
        CardRating.Again -> KotoColors.WrongButtonFace
        CardRating.Hard -> Color(0xFFA66A0B)
        CardRating.Good -> KotoColors.CorrectButtonFace
        CardRating.Easy -> CardsColors.Blue
        null -> null
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .testTag(if (isCurrent) "study_card" else "study_card_exiting")
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClickLabel = if (state.revealed) "Show question" else "Reveal answer",
            ) {
                if (!isFlipping && !isExiting) flip()
            }
            .semantics {
                stateDescription = if (state.revealed) "Answer revealed" else "Question"
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = if (backVisible) rotation - 180f else rotation
                    rotationZ = exitTilt
                    cameraDistance = 16 * density
                }
                .shadow(
                    elevation = if (isExiting) 4.dp else 1.dp,
                    shape = shape,
                    ambientColor = Color(0x3323334A),
                    spotColor = Color(0x3323334A),
                )
                .clip(shape)
                .background(CardsColors.Surface)
                .border(
                    width = if (isExiting && ratingColor != null) 2.dp else 1.dp,
                    color = if (isExiting && ratingColor != null) ratingColor.copy(alpha = 0.85f) else CardsColors.Edge,
                    shape = shape,
                )
                .padding(20.dp)
                .heightIn(min = minHeight - 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val primaryText = if (japaneseVisible) card.japanese else card.english
                val maxFontSize = if (japaneseVisible) 36.sp else 30.sp
                val minFontSize = 13.sp
                val reservedRomajiHeight = if (japaneseVisible && state.showRomaji) {
                    with(density) { 36.dp.roundToPx() }
                } else 0
                val availableHeightPx = (constraints.maxHeight - reservedRomajiHeight).coerceAtLeast(80)

                val computedFontSize = remember(primaryText, constraints.maxWidth, availableHeightPx, maxFontSize) {
                    if (constraints.maxWidth <= 0 || availableHeightPx <= 0) return@remember maxFontSize
                    val maxStyle = TextStyle(
                        fontSize = maxFontSize,
                        lineHeight = (maxFontSize.value * 1.32f).sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    val maxResult = textMeasurer.measure(
                        text = AnnotatedString(primaryText),
                        style = maxStyle,
                        constraints = Constraints(maxWidth = constraints.maxWidth),
                    )
                    if (maxResult.size.height <= availableHeightPx) {
                        maxFontSize
                    } else {
                        var low = minFontSize.value
                        var high = maxFontSize.value
                        var best = minFontSize.value
                        repeat(7) {
                            val mid = (low + high) / 2f
                            val midStyle = TextStyle(
                                fontSize = mid.sp,
                                lineHeight = (mid * 1.32f).sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                            val measure = textMeasurer.measure(
                                text = AnnotatedString(primaryText),
                                style = midStyle,
                                constraints = Constraints(maxWidth = constraints.maxWidth),
                            )
                            if (measure.size.height <= availableHeightPx) {
                                best = mid
                                low = mid + 0.5f
                            } else {
                                high = mid - 0.5f
                            }
                        }
                        best.sp
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = primaryText,
                        color = CardsColors.Ink,
                        fontSize = computedFontSize,
                        lineHeight = (computedFontSize.value * 1.32f).sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("card_word"),
                    )
                    if (japaneseVisible && state.showRomaji) {
                        Text(
                            text = card.romaji,
                            color = CardsColors.Muted,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("card_romaji"),
                        )
                    }
                }

                // If this card is exiting after rating, stamp the rating badge
                if (isExiting && rating != null && ratingColor != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .graphicsLayer { rotationZ = 6f }
                            .background(ratingColor, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = rating.name.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Text(
                if (backVisible) "↻  Tap to see the front" else if (state.hasBeenRevealed) "↻  Tap to see the answer again" else "↻  Tap to reveal answer",
                color = CardsColors.Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
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
                        CardsButton(
                            rating.name, { rate(rating) }, Modifier.weight(1f).testTag("rate_${rating.name.lowercase()}"),
                            background = background, ink = ink, depth = depth, enabled = enabled,
                        )
                    }
                }
            }
        }
    }
}
