package com.koto.app.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DeckDetailScreen(deck: FlashcardDeck, state: FlashcardState, update: (FlashcardState) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).testTag("deck_detail"), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                CardsButton("‹  All decks", { update(state.back()) }, Modifier.testTag("cards_back"),
                    background = CardsColors.Ice, ink = CardsColors.Ink, depth = CardsColors.IceDepth)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DeckBadge(deck)
                        Text(deck.title, color = CardsColors.Ink, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                    }
                    Text(deck.description, color = CardsColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
                    Text("${deck.cards.size} cards · ${practiceLabel(state.practiced[deck.id])}", color = CardsColors.Muted, fontSize = 12.sp)
                }
            }
            item { DeckStats(state.counts(deck)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    OptionSwitch("Show Romaji", "Reading hints", "romaji_toggle", state.showRomaji) { update(state.copy(showRomaji = it)) }
                    OptionSwitch("Shuffle", "Mix up the order", "shuffle_toggle", state.shuffle) { update(state.copy(shuffle = it)) }
                }
            }
            item {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IN THIS DECK", color = CardsColors.Ink, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text("${deck.cards.size} words", color = CardsColors.Muted, fontSize = 12.sp)
                }
            }
            items(deck.cards, key = { it.id }) { card ->
                CardsPanel(Modifier.fillMaxWidth().testTag("preview_${card.id}")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(card.japanese, color = CardsColors.Ink, fontSize = 22.sp, lineHeight = 30.sp)
                            if (state.showRomaji) Text(card.romaji, color = CardsColors.Blue, fontSize = 12.sp)
                            Text(card.english, color = CardsColors.Muted, fontSize = 14.sp)
                        }
                        MarkButton("star", card.id in state.favorites, "Favorite ${card.english}", "favorite_${card.id}") {
                            update(state.favorite(card.id))
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(CardsColors.Surface).padding(horizontal = 20.dp, vertical = 12.dp)) {
            CardsButton("START FLASHCARDS", { update(state.start(deck)) },
                Modifier.fillMaxWidth().testTag("start_flashcards"))
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
private fun OptionSwitch(label: String, subtitle: String, tag: String, checked: Boolean, change: (Boolean) -> Unit) {
    CardsPressable({ change(!checked) }, Modifier.fillMaxWidth().testTag(tag).semantics {
        role = Role.Switch; toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
    }, padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, color = CardsColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = CardsColors.Muted, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(
                checkedTrackColor = CardsColors.Blue, checkedThumbColor = Color.White,
                checkedBorderColor = CardsColors.Blue, uncheckedTrackColor = CardsColors.Ice,
                uncheckedThumbColor = CardsColors.Muted, uncheckedBorderColor = CardsColors.IceDepth))
        }
    }
}

private fun practiceLabel(timestamp: Long?): String = if (timestamp == null) "Not studied yet" else
    "Last studied ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))}"
