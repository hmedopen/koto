package com.koto.app.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DeckDetailScreen(deck: FlashcardDeck, state: FlashcardState, update: (FlashcardState) -> Unit,
    favorite: (String) -> Unit) {
    val counts = remember(deck, state.ratings) { state.counts(deck) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).testTag("deck_detail"), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "header", contentType = "header") {
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DeckBadge(deck)
                        Text(deck.title, color = CardsColors.Ink, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                    }
                    Text(deck.description, color = CardsColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
                    Text("${deck.cards.size} cards · ${practiceLabel(state.practiced[deck.id])}", color = CardsColors.Muted, fontSize = 12.sp)
                }
            }
            item(key = "stats", contentType = "stats") {
                Box(Modifier.padding(bottom = 12.dp)) { DeckStats(counts) }
            }
            item(key = "options", contentType = "options") {
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MAKE IT YOUR SESSION", color = CardsColors.Ink, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text("Which side comes first?", color = CardsColors.Muted, fontSize = 13.sp)
                    BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
                        val stacked = maxWidth < 300.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
                        @Composable fun Mode(japanese: Boolean, modifier: Modifier) {
                            val selected = state.japaneseFirst == japanese
                            CardsButton(if (japanese) "Japanese First" else "English First", { update(state.copy(japaneseFirst = japanese)) },
                                modifier.testTag(if (japanese) "japanese_first" else "english_first"),
                                background = if (selected) CardsColors.Blue else CardsColors.Ice,
                                ink = if (selected) Color.White else CardsColors.Ink,
                                depth = if (selected) CardsColors.BlueDepth else CardsColors.IceDepth, isSelected = selected)
                        }
                        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Mode(true, Modifier.fillMaxWidth()); Mode(false, Modifier.fillMaxWidth())
                        } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Mode(true, Modifier.weight(1f)); Mode(false, Modifier.weight(1f))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OptionButton("Show Romaji", "romaji_toggle", state.showRomaji,
                            Modifier.weight(1f)) { update(state.copy(showRomaji = it)) }
                        OptionButton("Shuffle", "shuffle_toggle", state.shuffle,
                            Modifier.weight(1f)) { update(state.copy(shuffle = it)) }
                    }
                }
            }
            item(key = "preview_heading", contentType = "preview_heading") {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IN THIS DECK", color = CardsColors.Ink, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text("${deck.cards.size} words", color = CardsColors.Muted, fontSize = 12.sp)
                }
            }
            items(deck.cards, key = { it.id }, contentType = { "preview" }) { card ->
                PreviewRow(card, state.showRomaji, card.id in state.favorites, favorite)
            }
        }
        Column(Modifier.fillMaxWidth().background(CardsColors.Surface).padding(horizontal = 20.dp, vertical = 12.dp)) {
            CardsButton("START FLASHCARDS", { update(state.start(deck)) },
                Modifier.fillMaxWidth().testTag("start_flashcards"))
        }
    }
}

@Composable
private fun PreviewRow(card: Flashcard, showRomaji: Boolean, isFavorite: Boolean, favorite: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().testTag("preview_${card.id}")
        .clip(shape).background(CardsColors.Surface).border(1.dp, CardsColors.Edge, shape)
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.japanese, color = CardsColors.Ink, fontSize = 20.sp, lineHeight = 26.sp)
            Text(buildAnnotatedString {
                if (showRomaji) {
                    withStyle(SpanStyle(color = CardsColors.Blue, fontSize = 12.sp)) { append(card.romaji) }
                    append(" · ")
                }
                append(card.english)
            }, color = CardsColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        MarkButton("star", isFavorite, "Favorite ${card.english}", "favorite_${card.id}") {
            favorite(card.id)
        }
    }
}

@Composable
internal fun DeckStats(counts: DeckCounts) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("NEW", counts.new, CardsColors.Blue), Triple("WEAK", counts.weak, CardsColors.Coral),
            Triple("MASTERED", counts.mastered, CardsColors.Green)).forEach { (label, count, ink) ->
            Column(Modifier.weight(1f).testTag("stat_${label.lowercase()}").semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$count", color = ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(label, color = CardsColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun OptionButton(label: String, tag: String, checked: Boolean, modifier: Modifier,
    change: (Boolean) -> Unit) {
    CardsButton("$label: ${if (checked) "On" else "Off"}", { change(!checked) },
        modifier.testTag(tag).semantics {
            role = Role.Switch
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        }, background = if (checked) CardsColors.Blue else CardsColors.Surface,
        ink = if (checked) Color.White else CardsColors.Ink,
        depth = if (checked) CardsColors.BlueDepth else CardsColors.Edge)
}

private fun practiceLabel(timestamp: Long?): String = if (timestamp == null) "Not studied yet" else
    "Last studied ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))}"
