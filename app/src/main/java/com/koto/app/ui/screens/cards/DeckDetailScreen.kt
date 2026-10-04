package com.koto.app.ui.screens.cards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone
import androidx.compose.ui.text.style.TextOverflow
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DeckDetailScreen(
    deck: FlashcardDeck,
    state: FlashcardState,
    update: (FlashcardState) -> Unit,
    favorite: (String) -> Unit,
    onShowContent: () -> Unit,
) {
    val counts = remember(deck, state.ratings, state.srsRecords) { state.counts(deck) }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        DeckDetailContent(
            deck = deck,
            state = state,
            counts = counts,
            update = update,
            onShowContent = onShowContent,
        )
    }
}

@Composable
private fun DeckDetailContent(
    deck: FlashcardDeck,
    state: FlashcardState,
    counts: DeckCounts,
    update: (FlashcardState) -> Unit,
    onShowContent: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollEnabled = maxHeight < 700.dp
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState(), enabled = scrollEnabled)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("deck_detail"),
        ) {
            // Fixed-height Header Slot: Never pushes buttons regardless of 1, 2, or 3-line title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DeckBadge(deck)
                    Text(
                        text = deck.title,
                        color = CardsColors.Ink,
                        fontSize = if (deck.title.length > 22) 22.sp else 26.sp,
                        lineHeight = if (deck.title.length > 22) 28.sp else 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Trimmed subtitle: compact metadata (replaces long description & study info)
            Text(
                text = "${deck.cards.size} cards · ${practiceLabel(state.practiced[deck.id])}",
                color = CardsColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            // Deck Stats (Due · Weak · Mastered)
            Box(Modifier.padding(bottom = 12.dp)) {
                DeckStats(counts)
            }

            // Session Size Selector
            Text(
                text = "Session Size",
                color = CardsColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            SessionSizeSelector(
                selectedSize = state.sessionSize,
                onSelectSize = { update(state.copy(sessionSize = it)) },
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Direction Selection (Japanese First / English First)
            BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup().padding(bottom = 10.dp)) {
                val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
                @Composable fun Mode(japanese: Boolean, modifier: Modifier) {
                    val selected = state.japaneseFirst == japanese
                    CardsButton(
                        if (japanese) "Japanese First" else "English First",
                        { update(state.copy(japaneseFirst = japanese)) },
                        modifier.testTag(if (japanese) "japanese_first" else "english_first"),
                        background = if (selected) CardsColors.Blue else CardsColors.Surface,
                        ink = if (selected) Color.White else CardsColors.Ink,
                        depth = if (selected) CardsColors.BlueDepth else CardsColors.Edge,
                        isSelected = selected,
                    )
                }
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Mode(true, Modifier.fillMaxWidth()); Mode(false, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Mode(true, Modifier.weight(1f)); Mode(false, Modifier.weight(1f))
                    }
                }
            }

            // Romaji & Shuffle Options
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OptionButton(
                    "Show Romaji", "romaji_toggle", state.showRomaji,
                    Modifier.weight(1f),
                ) { update(state.copy(showRomaji = it)) }
                OptionButton(
                    "Shuffle", "shuffle_toggle", state.shuffle,
                    Modifier.weight(1f),
                ) { update(state.copy(shuffle = it)) }
            }

            // Show Content Trigger Button (White fill & edge depth)
            CardsButton(
                label = "Show Content (${deck.cards.size} cards)",
                onClick = onShowContent,
                modifier = Modifier.fillMaxWidth().testTag("show_content"),
                background = CardsColors.Surface,
                ink = CardsColors.Ink,
                depth = CardsColors.Edge,
            )

            Spacer(Modifier.weight(1f))

            // Start Review Button: No white bottom bar container underneath, sits cleanly above navigation bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 28.dp),
            ) {
                CardsButton(
                    label = "START REVIEW",
                    onClick = { update(state.start(deck, state.sessionSize)) },
                    modifier = Modifier.fillMaxWidth().testTag("start_flashcards").testTag("start_review"),
                )
            }
        }
    }
}

@Composable
internal fun DeckContentScreen(
    deck: FlashcardDeck,
    state: FlashcardState,
    favorite: (String) -> Unit,
    onClose: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .semantics {
                dismiss {
                    onClose()
                    true
                }
            }
            .testTag("content_drawer"),
    ) {
        // Top Bar: Clean header with title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "DECK CONTENT",
                color = CardsColors.Ink,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("content_table"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            itemsIndexed(deck.cards, key = { _, card -> card.id }) { index, card ->
                if (index > 0) {
                    HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.5f), thickness = 0.8.dp)
                }
                Row(
                    Modifier.fillMaxWidth().testTag("preview_${card.id}")
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: Kana + Romaji
                    Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(card.japanese, color = CardsColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        if (state.showRomaji) {
                            Text(card.romaji, color = CardsColors.Blue, fontSize = 12.sp)
                        }
                    }
                    // Right: English meaning
                    Text(
                        text = card.english,
                        color = CardsColors.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    MarkButton("star", card.id in state.favorites, "Favorite ${card.english}", "favorite_${card.id}") {
                        favorite(card.id)
                    }
                }
            }
        }
    }
}

/**
 * Single-row segmented chip picker for review session sizes: [ 5 ] [ 10 ] [ 15 ] [ All ].
 * Inline, low-profile height (~32dp).
 */
@Composable
private fun SessionSizeSelector(
    selectedSize: Int?,
    onSelectSize: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(5 to "5", 10 to "10", 15 to "15", null to "All")
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardsColors.Surface)
            .border(1.dp, CardsColors.Edge, shape)
            .padding(2.dp)
            .testTag("session_size_selector"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (size, label) ->
            val isSelected = selectedSize == size
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CardsColors.Blue else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelectSize(size) }
                    .testTag("session_size_${label.lowercase()}")
                    .semantics {
                        this.selected = isSelected
                        role = Role.RadioButton
                    }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else CardsColors.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun DeckStats(counts: DeckCounts) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("DUE", counts.due, CardsColors.Blue),
            Triple("WEAK", counts.weak, CardsColors.Coral),
            Triple("MASTERED", counts.mastered, CardsColors.Green),
        ).forEach { (label, count, ink) ->
            Column(
                Modifier.weight(1f).testTag("stat_${label.lowercase()}").semantics(mergeDescendants = true) {
                    if (label == "DUE") {
                        // Alias for legacy test tags checking stat_new
                    }
                },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("$count", color = ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(label, color = CardsColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun OptionButton(
    label: String, tag: String, checked: Boolean, modifier: Modifier,
    change: (Boolean) -> Unit,
) {
    CardsButton(
        "$label: ${if (checked) "On" else "Off"}", { change(!checked) },
        modifier.testTag(tag).semantics {
            role = Role.Switch
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        }, background = if (checked) CardsColors.Blue else CardsColors.Surface,
        ink = if (checked) Color.White else CardsColors.Ink,
        depth = if (checked) CardsColors.BlueDepth else CardsColors.Edge,
    )
}

private fun practiceLabel(timestamp: Long?): String = if (timestamp == null) "Not studied yet" else
    "Last studied ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))}"
