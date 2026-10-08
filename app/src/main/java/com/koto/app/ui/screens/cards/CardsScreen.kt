package com.koto.app.ui.screens.cards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.cards.data.CustomDeckStore
import com.koto.app.feature.translator.data.TranslatorCardStore
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class CardsStage {
    Browser,
    Detail,
    Content,
    Study,
    Starred,
    CreateDeck,
}

@Composable
fun CardsScreen(
    onStudyModeChanged: (Boolean) -> Unit = {},
    onDeckOpenChanged: (Boolean) -> Unit = {},
    onCreateDeckModeChanged: (Boolean) -> Unit = {},
    randomDeckTrigger: Boolean = false,
    onRandomDeckHandled: () -> Unit = {},
    starredWordsTrigger: Boolean = false,
    onStarredWordsHandled: () -> Unit = {},
    createDeckTrigger: Boolean = false,
    onCreateDeckHandled: () -> Unit = {},
    onContentOpenChanged: (Boolean) -> Unit = {},
    targetDeckId: String? = null,
    onTargetDeckIdHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val decks = remember(context) { loadFlashcardDecks(context) }
    val srsStore = remember(context) { FlashcardSrsStore(context) }
    val cardStore = remember(context) { TranslatorCardStore(context) }
    val customDeckStore = remember(context) { CustomDeckStore(context) }
    val coroutineScope = rememberCoroutineScope()
    var starredTranslationCards by remember { mutableStateOf(cardStore.loadStarredCards()) }
    var customDecks by remember { mutableStateOf(customDeckStore.loadCustomDecks()) }

    LaunchedEffect(Unit) {
        launch {
            cardStore.starredCardsFlow.collect { cards ->
                starredTranslationCards = cards
            }
        }
        launch {
            customDeckStore.customDecksFlow.collect { cDecks ->
                customDecks = cDecks
            }
        }
    }

    val quickTranslationsDeck = remember(starredTranslationCards) {
        if (starredTranslationCards.isEmpty()) null
        else FlashcardDeck(
            id = "deck_quick_translations",
            title = "Quick Translations",
            icon = "chatbubble",
            cards = starredTranslationCards.map { card ->
                Flashcard(
                    id = card.id,
                    japanese = card.targetText,
                    romaji = card.targetRomaji,
                    english = card.sourceText,
                )
            },
            number = 0,
            category = "Quick Translations",
            tier = 1,
        )
    }

    // Decoupled: Quick Translations belongs exclusively to Translation Bookmarks, not the main grid
    val allDecks = remember(decks, customDecks) {
        val list = mutableListOf<FlashcardDeck>()
        list.addAll(customDecks)
        list.addAll(decks)
        list
    }

    var state by rememberSaveable(stateSaver = FlashcardState.Saver) {
        val initialSrs = srsStore.loadAll()
        mutableStateOf(FlashcardState(srsRecords = initialSrs))
    }

    // Persist SRS updates to local database
    LaunchedEffect(state.srsRecords) {
        if (state.srsRecords.isNotEmpty()) {
            srsStore.saveAll(state.srsRecords)
        }
    }

    var showSurpriseMeDialog by rememberSaveable { mutableStateOf(false) }
    var showStarred by rememberSaveable { mutableStateOf(false) }
    var showCreateDeck by rememberSaveable { mutableStateOf(false) }
    var editingCustomDeckId by rememberSaveable { mutableStateOf<String?>(null) }
    var showContent by rememberSaveable { mutableStateOf(false) }
    var dynamicStarredDeck by remember { mutableStateOf<FlashcardDeck?>(null) }

    LaunchedEffect(showCreateDeck) {
        onCreateDeckModeChanged(showCreateDeck)
    }

    LaunchedEffect(randomDeckTrigger) {
        if (randomDeckTrigger) {
            showSurpriseMeDialog = true
            onRandomDeckHandled()
        }
    }

    LaunchedEffect(starredWordsTrigger) {
        if (starredWordsTrigger) {
            showStarred = true
            showCreateDeck = false
            onStarredWordsHandled()
        }
    }

    LaunchedEffect(createDeckTrigger) {
        if (createDeckTrigger) {
            editingCustomDeckId = null
            showCreateDeck = true
            showStarred = false
            onCreateDeckHandled()
        }
    }

    val deck = if (state.deckId == "starred_review") {
        dynamicStarredDeck
    } else if (state.deckId == "deck_quick_translations") {
        quickTranslationsDeck
    } else {
        allDecks.find { it.id == state.deckId }
    }
    var lastActiveDeck by remember { mutableStateOf<FlashcardDeck?>(null) }
    if (deck != null) {
        lastActiveDeck = deck
    }
    val activeDeck = deck ?: lastActiveDeck

    val currentStage = when {
        showStarred -> CardsStage.Starred
        showCreateDeck -> CardsStage.CreateDeck
        deck == null -> CardsStage.Browser
        state.studying -> CardsStage.Study
        showContent -> CardsStage.Content
        else -> CardsStage.Detail
    }

    val subScreenOpen = (deck != null && !state.studying) || showStarred || showCreateDeck
    val openDeck: (FlashcardDeck) -> Unit = remember { {
        showContent = false
        state = state.open(it)
    } }

    LaunchedEffect(targetDeckId, quickTranslationsDeck) {
        if (targetDeckId != null) {
            val target = if (targetDeckId == "deck_quick_translations") {
                quickTranslationsDeck
            } else {
                allDecks.find { it.id == targetDeckId }
            }
            if (target != null) {
                openDeck(target)
            }
            onTargetDeckIdHandled()
        }
    }
    val pinDeck: (String) -> Unit = remember { { state = state.pin(it) } }
    val favoriteCard: (String) -> Unit = remember { { state = state.favorite(it) } }

    LaunchedEffect(state.studying) { onStudyModeChanged(state.studying) }
    LaunchedEffect(subScreenOpen) { onDeckOpenChanged(subScreenOpen) }
    LaunchedEffect(showContent) { onContentOpenChanged(showContent) }
    DisposableEffect(Unit) {
        onDispose {
            onStudyModeChanged(false)
            onDeckOpenChanged(false)
            onCreateDeckModeChanged(false)
            onContentOpenChanged(false)
        }
    }
    BackHandler(enabled = deck != null || showStarred || showCreateDeck) {
        when {
            showStarred -> showStarred = false
            showCreateDeck -> {
                showCreateDeck = false
                editingCustomDeckId = null
            }
            showContent -> showContent = false
            else -> state = state.back()
        }
    }

    val layoutSign = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1

    Box(Modifier.fillMaxSize().background(Color.White).testTag("screen_cards")) {
        AnimatedContent(
            targetState = currentStage,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val direction = if (forward) layoutSign else -layoutSign
                (slideInHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> direction * fullWidth },
                ) + fadeIn(
                    animationSpec = tween(250, easing = LinearOutSlowInEasing),
                )).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -direction * fullWidth },
                    ) + fadeOut(
                        animationSpec = tween(200, easing = FastOutLinearInEasing),
                    ),
                ).using(null)
            },
            label = "Cards flow navigation",
        ) { stage ->
            when (stage) {
                CardsStage.Browser -> CategoryGrid(allDecks, state, openDeck, pinDeck)
                CardsStage.Detail -> {
                    if (activeDeck != null) {
                        DeckDetailScreen(
                            deck = activeDeck,
                            state = state,
                            update = { state = it },
                            favorite = favoriteCard,
                            onShowContent = { showContent = true },
                            onEditDeck = { deckId ->
                                editingCustomDeckId = deckId
                                showCreateDeck = true
                            },
                            onDeleteDeck = { deckId ->
                                coroutineScope.launch {
                                    if (deckId == "deck_quick_translations") {
                                        cardStore.clearAll()
                                    } else {
                                        customDeckStore.deleteDeck(deckId)
                                    }
                                    state = state.back()
                                }
                            },
                            onAddCard = { deckId ->
                                editingCustomDeckId = deckId
                                showCreateDeck = true
                            },
                        )
                    }
                }
                CardsStage.Content -> {
                    if (activeDeck != null) {
                        DeckContentScreen(activeDeck, state, favoriteCard, onClose = { showContent = false })
                    }
                }
                CardsStage.Study -> {
                    if (activeDeck != null) {
                        FlashcardStudyScreen(activeDeck, state, { state = it })
                    }
                }
                CardsStage.Starred -> {
                    StarredCardsScreen(
                        decks = allDecks,
                        state = state,
                        onToggleFavorite = favoriteCard,
                        onViewDeck = { starredCards ->
                            val dynamicDeck = FlashcardDeck(
                                id = "starred_review",
                                title = "Starred Words",
                                category = "Favorites",
                                icon = "star",
                                cards = starredCards,
                            )
                            dynamicStarredDeck = dynamicDeck
                            showStarred = false
                            openDeck(dynamicDeck)
                        },
                        onBack = { showStarred = false },
                    )
                }
                CardsStage.CreateDeck -> {
                    CreateDeckScreen(
                        deckIdToEdit = editingCustomDeckId,
                        onBack = {
                            showCreateDeck = false
                            editingCustomDeckId = null
                        },
                        onDeckSaved = { savedDeck ->
                            showCreateDeck = false
                            editingCustomDeckId = null
                            openDeck(savedDeck)
                        },
                        onDeleteDeck = { deckId ->
                            coroutineScope.launch {
                                customDeckStore.deleteDeck(deckId)
                                showCreateDeck = false
                                editingCustomDeckId = null
                                state = state.back()
                            }
                        },
                    )
                }
            }
        }

        if (showSurpriseMeDialog && deck == null) {
            val counts = remember(allDecks, state.ratings, state.srsRecords) {
                allDecks.associate { it.id to state.counts(it) }
            }
            SurpriseMeDialog(
                decks = allDecks,
                counts = counts,
                onDismiss = { showSurpriseMeDialog = false },
                onOpenDeck = openDeck,
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    decks: List<FlashcardDeck>,
    state: FlashcardState,
    onOpen: (FlashcardDeck) -> Unit,
    onFavorite: (String) -> Unit,
) {
    val sorted = remember(decks, state.pinned) { decks.sortedByDescending { it.id in state.pinned } }
    val counts = remember(decks, state.ratings, state.srsRecords) { decks.associate { it.id to state.counts(it) } }
    val density = LocalDensity.current
    val largeText = density.fontScale > 1.3f
    val columns = remember { GridCells.Fixed(2) }
    var masteredExpanded by rememberSaveable { mutableStateOf(false) }

    val (masteredDecks, activeDecks) = remember(sorted, counts) {
        sorted.partition { deck ->
            val c = counts.getValue(deck.id)
            c.mastered == deck.cards.size && deck.cards.isNotEmpty()
        }
    }
    val wordCount = remember(activeDecks) { activeDecks.sumOf { it.cards.size } }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        val tileMinHeights = remember(constraints.maxWidth, density) {
            with(columns) {
                with(density) {
                    calculateCrossAxisCellSizes(
                        constraints.maxWidth - 2 * 16.dp.roundToPx(), 10.dp.roundToPx(),
                    ).map { ((it - 2 * 10.dp.roundToPx()).toDp() - 30.dp).coerceAtLeast(0.dp) }
                }
            }
        }
        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize().testTag("cards_grid"),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "decks_header", contentType = "header", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("YOUR DECKS", color = CardsColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${activeDecks.size} active · $wordCount words", color = CardsColors.Muted, fontSize = 12.sp)
                }
            }
                // Active Decks
                itemsIndexed(
                    activeDecks,
                    key = { _, deck -> deck.id },
                    contentType = { _, _ -> "deck" },
                ) { index, deck ->
                    DeckTile(
                        deck = deck,
                        counts = counts.getValue(deck.id),
                        pinned = deck.id in state.pinned,
                        largeText = largeText,
                        minHeight = tileMinHeights[index % 2],
                        isMastered = false,
                        onOpen = onOpen,
                        onFavorite = onFavorite,
                    )
                }

                // Mastered Decks Section (Separated & Collapsible)
                if (masteredDecks.isNotEmpty()) {
                    item(key = "mastered_header", contentType = "mastered_header", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { masteredExpanded = !masteredExpanded }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("mastered_decks_header"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_trophy),
                                    contentDescription = null,
                                    tint = CardsColors.Green,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "MASTERED DECKS (${masteredDecks.size})",
                                    color = CardsColors.Green,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                )
                            }
                            Text(
                                if (masteredExpanded) "Hide" else "Show",
                                color = CardsColors.Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    if (masteredExpanded) {
                        itemsIndexed(
                            masteredDecks,
                            key = { _, deck -> "mastered_${deck.id}" },
                            contentType = { _, _ -> "deck" },
                        ) { index, deck ->
                            DeckTile(
                                deck = deck,
                                counts = counts.getValue(deck.id),
                                pinned = deck.id in state.pinned,
                                largeText = largeText,
                                minHeight = tileMinHeights[index % 2],
                                isMastered = true,
                                onOpen = onOpen,
                                onFavorite = onFavorite,
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun DeckTile(
    deck: FlashcardDeck,
    counts: DeckCounts,
    pinned: Boolean,
    largeText: Boolean,
    minHeight: Dp,
    isMastered: Boolean = false,
    onOpen: (FlashcardDeck) -> Unit,
    onFavorite: (String) -> Unit,
) {
    if (isMastered) {
        CardsPressable(
            onClick = { onOpen(deck) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deck_${deck.id}"),
            face = CardsColors.Green,
            depth = CardsColors.GreenDepth,
            padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DeckBadge(deck)
                    MarkButton("heart", pinned, "Favorite ${deck.title}", "favorite_deck_${deck.id}") {
                        onFavorite(deck.id)
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        deck.title,
                        color = Color.White,
                        fontSize = if (largeText) 12.sp else 15.sp,
                        lineHeight = if (largeText) 15.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    Text(
                        "${deck.cards.size} words",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .testTag("indicator_mastered_${deck.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = "Mastered",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("badge_trophy_${deck.id}"),
                    )
                }
            }
        }
    } else {
        CardsPressable(
            onClick = { onOpen(deck) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deck_${deck.id}"),
            padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (largeText) {
                        DeckBadge(deck)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DeckBadge(deck)
                            Text("${deck.cards.size} words", color = CardsColors.Muted, fontSize = 10.sp)
                        }
                    }
                    MarkButton("heart", pinned, "Favorite ${deck.title}", "favorite_deck_${deck.id}") {
                        onFavorite(deck.id)
                    }
                }
                Text(
                    deck.title,
                    color = CardsColors.Ink,
                    fontSize = if (largeText) 11.sp else 14.sp,
                    lineHeight = if (largeText) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.heightIn(min = if (largeText) 46.dp else 52.dp),
                )
                if (largeText) {
                    Text("${deck.cards.size} words", color = CardsColors.Muted, fontSize = 10.sp)
                }
                if (largeText) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        StatLine(counts.due, "Due", CardsColors.Blue)
                        StatLine(counts.weak, "Weak", CardsColors.Coral)
                        StatLine(counts.mastered, "Mastered", CardsColors.Green)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TileCount(counts.due, "Due", CardsColors.Blue, Modifier.weight(1f))
                        TileCount(counts.weak, "Weak", CardsColors.Coral, Modifier.weight(1f))
                        TileCount(counts.mastered, "Mastered", CardsColors.Green, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SurpriseMeDialog(
    decks: List<FlashcardDeck>,
    counts: Map<String, DeckCounts>,
    onDismiss: () -> Unit,
    onOpenDeck: (FlashcardDeck) -> Unit,
) {
    var seed by rememberSaveable { mutableIntStateOf(0) }

    val eligibleDecks = remember(decks, counts) {
        decks.filter { deck ->
            val c = counts[deck.id]
            c == null || c.mastered < deck.cards.size || deck.cards.isEmpty()
        }
    }

    val selectedDeck = remember(eligibleDecks, seed) {
        if (eligibleDecks.isNotEmpty()) eligibleDecks.random(Random(System.currentTimeMillis() + seed)) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("surprise_me_dialog"),
        shape = RoundedCornerShape(16.dp),
        containerColor = CardsColors.Surface,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DAILY SHUFFLE",
                    color = CardsColors.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                TactileButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).testTag("close_dialog"),
                    tone = TactileTone.Quiet,
                    padding = PaddingValues(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = CardsColors.Ink,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (selectedDeck != null) {
                    val deckCount = counts[selectedDeck.id]

                    // Crisp square deck icon badge with breathing room
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardsColors.Surface)
                            .border(1.dp, CardsColors.Edge, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        DeckArtwork(selectedDeck, Modifier.size(34.dp))
                    }

                    // Deck title and word count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = selectedDeck.title,
                            color = CardsColors.Ink,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 23.sp,
                        )
                        Text(
                            text = "${selectedDeck.cards.size} words",
                            color = CardsColors.Muted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Zero-bubble typographic stats
                    if (deckCount != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${deckCount.due}",
                                    color = CardsColors.Blue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "DUE",
                                    color = CardsColors.Muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                )
                            }
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(CardsColors.Edge)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${deckCount.weak}",
                                    color = CardsColors.Coral,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "WEAK",
                                    color = CardsColors.Muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                )
                            }
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(CardsColors.Edge)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${deckCount.mastered}",
                                    color = CardsColors.Green,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "MASTERED",
                                    color = CardsColors.Muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "All active decks are 100% mastered!",
                            color = CardsColors.Muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selectedDeck != null) {
                    CardsButton(
                        label = "START REVIEW",
                        onClick = {
                            onDismiss()
                            onOpenDeck(selectedDeck)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_random_deck"),
                    )
                }
                if (eligibleDecks.size > 1) {
                    CardsButton(
                        label = "Roll Again",
                        onClick = { seed += 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("roll_again"),
                        background = CardsColors.Surface,
                        ink = CardsColors.Ink,
                        depth = CardsColors.Edge,
                    )
                }
            }
        },
    )
}


@Composable
private fun StatLine(count: Int, label: String, color: Color) {
    Row(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = CardsColors.Ink, fontSize = 8.sp, modifier = Modifier.weight(1f))
        Text("$count", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TileCount(count: Int, label: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text("$count", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = CardsColors.Ink, fontSize = 8.sp, maxLines = 1)
    }
}
