package com.koto.app.ui.screens.cards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CardsScreen(
    onStudyModeChanged: (Boolean) -> Unit = {},
    onDeckOpenChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val decks = remember(context) { loadFlashcardDecks(context) }
    var state by rememberSaveable(stateSaver = FlashcardState.Saver) { mutableStateOf(FlashcardState()) }
    val deck = decks.find { it.id == state.deckId }
    val deckDetailOpen = deck != null && !state.studying
    LaunchedEffect(state.studying) { onStudyModeChanged(state.studying) }
    LaunchedEffect(deckDetailOpen) { onDeckOpenChanged(deckDetailOpen) }
    DisposableEffect(Unit) {
        onDispose {
            onStudyModeChanged(false)
            onDeckOpenChanged(false)
        }
    }
    BackHandler(enabled = deck != null) { state = state.back() }
    Box(Modifier.fillMaxSize().background(CardsColors.Background).testTag("screen_cards")) {
        when {
            deck == null -> CategoryGrid(decks, state, { state = state.open(it) }, { state = state.pin(it) })
            state.studying -> FlashcardStudyScreen(deck, state, { state = it })
            else -> DeckDetailScreen(deck, state, { state = it })
        }
    }
}

@Composable
private fun CategoryGrid(decks: List<FlashcardDeck>, state: FlashcardState,
    onOpen: (FlashcardDeck) -> Unit, onFavorite: (String) -> Unit) {
    val sorted = decks.sortedByDescending { it.id in state.pinned }
    val largeText = LocalDensity.current.fontScale > 1.3f
    var selectedSection by rememberSaveable { mutableStateOf("decks") }
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().testTag("cards_grid"),
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CardsSectionControl(selectedSection) { selectedSection = it }
                if (selectedSection == "decks") FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR DECKS", color = CardsColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${decks.size} decks · ${decks.sumOf { it.cards.size }} words", color = CardsColors.Muted, fontSize = 12.sp)
                }
            }
        }
        if (selectedSection == "decks") items(sorted, key = { it.id }) { deck ->
            val counts = state.counts(deck)
            CardsPressable({ onOpen(deck) }, Modifier.fillMaxWidth().testTag("deck_${deck.id}"),
                padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().heightIn(min = maxWidth - 30.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            if (largeText) DeckBadge(deck) else {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    DeckBadge(deck)
                                    Text("${deck.cards.size} words", color = CardsColors.Muted, fontSize = 10.sp)
                                }
                            }
                            MarkButton("heart", deck.id in state.pinned, "Favorite ${deck.title}", "favorite_deck_${deck.id}") {
                                onFavorite(deck.id)
                            }
                        }
                        Text(deck.title, color = CardsColors.Ink,
                            fontSize = if (largeText) 11.sp else 14.sp,
                            lineHeight = if (largeText) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.heightIn(min = if (largeText) 46.dp else 52.dp))
                        if (largeText) Text("${deck.cards.size} words", color = CardsColors.Muted, fontSize = 10.sp)
                        if (largeText) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                StatLine(counts.new, "New", CardsColors.Blue)
                                StatLine(counts.weak, "Weak", CardsColors.Coral)
                                StatLine(counts.mastered, "Mastered", CardsColors.Green)
                            }
                        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TileCount(counts.new, "New", CardsColors.Blue, Modifier.weight(1f))
                            TileCount(counts.weak, "Weak", CardsColors.Coral, Modifier.weight(1f))
                            TileCount(counts.mastered, "Mastered", CardsColors.Green, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardsSectionControl(selectedSection: String, select: (String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(CardsColors.Surface)
        .border(1.dp, CardsColors.Edge, shape).padding(3.dp).testTag("cards_sections")) {
        listOf("decks" to "Decks", "mixes" to "Mixes").forEach { (id, label) ->
            val selected = selectedSection == id
            Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                .background(if (selected) CardsColors.Blue else Color.Transparent)
                .clickable(role = Role.Tab) { select(id) }
                .testTag("cards_section_$id").semantics { this.selected = selected; role = Role.Tab }
                .padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                Text(label, color = if (selected) Color.White else CardsColors.Ink,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatLine(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = CardsColors.Ink, fontSize = 8.sp, modifier = Modifier.weight(1f))
        Text("$count", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TileCount(count: Int, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text("$count", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = CardsColors.Ink, fontSize = 8.sp, maxLines = 1)
    }
}
