package com.koto.app.feature.translator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.translator.data.TranslationHistoryItem
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.theme.KotoType
import java.text.DateFormat
import java.util.Date

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.koto.app.feature.translator.data.TranslatorCardStore
import com.koto.app.feature.translator.model.SavedTranslationCard
import com.koto.app.ui.screens.cards.CardsPressable

@Composable
fun TranslationHistoryDialog(
    history: List<TranslationHistoryItem>,
    isStarred: (String, String) -> Boolean = { _, _ -> false },
    savedCards: List<SavedTranslationCard> = emptyList(),
    isSpeaking: Boolean = false,
    onSelect: (TranslationHistoryItem) -> Unit,
    onToggleStar: (TranslationHistoryItem) -> Unit,
    onSpeak: (String, TranslationLanguage) -> Unit,
    onCopy: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cardStore = remember(context) { TranslatorCardStore(context) }
    var playingHistoryId by remember { mutableStateOf<String?>(null) }

    BackHandler { onDismiss() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures { /* Absorb pointer clicks to prevent click-through */ }
            }
            .testTag("translation_history_dialog"),
    ) {
        // Top App Bar with Symmetrical Layout / Rule 6 top-right [X]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "History",
                    style = KotoType.Brand,
                    color = CardsColors.Ink,
                    fontSize = 20.sp,
                )
                Text(
                    text = "${history.size} recent ${if (history.size == 1) "query" else "queries"}",
                    fontSize = 12.sp,
                    color = CardsColors.Muted,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (history.isNotEmpty()) {
                    CardsPressable(
                        onClick = onClearAll,
                        modifier = Modifier.height(38.dp),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CardsColors.Coral,
                        )
                    }
                }

                // Tactile Top-Right [X] dismiss button (Rule 6)
                CardsPressable(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("history_dismiss_button"),
                    face = CardsColors.Surface,
                    depth = CardsColors.Edge,
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close history",
                        tint = CardsColors.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_document),
                        contentDescription = null,
                        tint = CardsColors.Muted.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "No Translation History",
                        color = CardsColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Translations you look up will appear here for easy reference.",
                        color = CardsColors.Muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(history, key = { _, item -> item.id }) { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = CardsColors.Edge.copy(alpha = 0.5f),
                            thickness = 0.8.dp,
                        )
                    }

                    val starred = if (savedCards.isNotEmpty()) {
                        cardStore.isStarred(item.sourceText, item.targetText, savedCards)
                    } else {
                        isStarred(item.sourceText, item.targetText)
                    }
                    val formattedTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                            .testTag("history_row_${item.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Column: Source and Target with Romaji underneath (Rule 2)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = item.sourceText,
                                color = CardsColors.Muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                            )
                            Text(
                                text = item.targetText,
                                color = CardsColors.Ink,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (item.targetLang == TranslationLanguage.Japanese && item.targetRomaji.isNotBlank()) {
                                Text(
                                    text = item.targetRomaji,
                                    color = CardsColors.Blue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                            Text(
                                text = formattedTime,
                                color = CardsColors.Muted.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                            )
                        }

                        // Right Action cluster: Audio, Copy, Star
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Audio (🔊) with animated waves
                            TranslatorAudioButton(
                                onClick = {
                                    playingHistoryId = item.id
                                    onSpeak(item.targetText, item.targetLang)
                                },
                                isPlaying = isSpeaking && playingHistoryId == item.id,
                                size = 36.dp,
                                tint = CardsColors.Blue,
                                description = "Speak translation",
                            )

                            // Copy (📋)
                            TranslatorCopyButton(
                                onClick = { onCopy(item.targetText) },
                                size = 36.dp,
                                tint = CardsColors.Ink,
                                description = "Copy translation",
                            )

                            // Star (⭐)
                            TranslatorStarButton(
                                onClick = { onToggleStar(item) },
                                isStarred = starred,
                                size = 36.dp,
                                description = if (starred) "Unstar translation" else "Star translation",
                            )
                        }
                    }
                }
            }
        }
    }
}
