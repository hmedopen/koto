package com.koto.app.ui.screens.cards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CardsScreen() {
    val context = LocalContext.current
    val decks = remember(context) { loadFlashcardDecks(context) }
    var state by rememberSaveable(stateSaver = FlashcardState.Saver) { mutableStateOf(FlashcardState()) }
    val deck = decks.find { it.id == state.deckId }
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
    onOpen: (FlashcardDeck) -> Unit, onPin: (String) -> Unit) {
    val sorted = decks.sortedByDescending { it.id in state.pinned }
    LazyVerticalGrid(columns = GridCells.Adaptive(280.dp), modifier = Modifier.fillMaxSize().testTag("cards_grid"),
        contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("単語  /  YOUR WORD COLLECTION", color = CardsColors.Blue, fontSize = 10.sp,
                    letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Small practice.\nLasting progress.", color = CardsColors.Ink, fontSize = 30.sp,
                    lineHeight = 37.sp, fontWeight = FontWeight.Bold)
                Text("Choose a deck. Make the words your own.", color = CardsColors.Muted, fontSize = 14.sp)
                FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR DECKS", color = CardsColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${decks.size} decks · ${decks.sumOf { it.cards.size }} words", color = CardsColors.Muted, fontSize = 12.sp)
                }
            }
        }
        items(sorted, key = { it.id }) { deck ->
            val counts = state.counts(deck)
            CardsPressable({ onOpen(deck) }, Modifier.fillMaxWidth().testTag("deck_${deck.id}"), padding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DeckBadge(deck)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(deck.title, color = CardsColors.Ink, fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                            Text("${deck.cards.size} words · Tap to practice", color = CardsColors.Muted, fontSize = 12.sp)
                        }
                        MarkButton("bookmark", deck.id in state.pinned, "Pin ${deck.title}", "pin_${deck.id}") { onPin(deck.id) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TileCount(counts.new, "New", CardsColors.Blue, Modifier.weight(1f))
                        TileCount(counts.weak, "Weak", CardsColors.Coral, Modifier.weight(1f))
                        TileCount(counts.mastered, "Mastered", CardsColors.Green, Modifier.weight(1f))
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(CardsColors.Ice)) {
                        if (counts.mastered > 0 && deck.cards.isNotEmpty()) Box(Modifier.fillMaxWidth(counts.mastered.toFloat() / deck.cards.size)
                            .fillMaxHeight().background(CardsColors.Green))
                    }
                }
            }
        }
    }
}

@Composable
private fun TileCount(count: Int, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("$count", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = CardsColors.Muted, fontSize = 11.sp)
    }
}
