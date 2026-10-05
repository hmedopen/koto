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
import com.koto.app.feature.translator.model.SavedTranslationCard
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.theme.KotoType

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import com.koto.app.ui.screens.cards.CardsPressable

@Composable
fun SavedTranslationsDialog(
    cards: List<SavedTranslationCard>,
    onDismiss: () -> Unit,
    onSpeak: (String, TranslationLanguage) -> Unit,
    onCopy: (String) -> Unit,
    onRemove: (String) -> Unit,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var playingSavedId by remember { mutableStateOf<String?>(null) }
    var stagedRemovedIds by remember { mutableStateOf(setOf<String>()) }

    val handleDismiss = {
        stagedRemovedIds.forEach { onRemove(it) }
        onDismiss()
    }

    BackHandler { handleDismiss() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures { /* Absorb pointer clicks to prevent click-through */ }
            }
            .testTag("saved_translations_screen"),
    ) {
        // Top App Bar with Centered "Bookmarks" Title and Top-Right [X] dismiss button (Design Rule 6)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Balanced spacer on left to mathematically center the title
            Spacer(modifier = Modifier.size(44.dp))

            Text(
                text = "Bookmarks",
                style = KotoType.Brand,
                color = CardsColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .testTag("saved_translations_title"),
            )

            // Tactile Top-Right [X] button with crisp squircle geometry (Rule 4 & 5)
            CardsPressable(
                onClick = handleDismiss,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("saved_translations_dismiss"),
                face = CardsColors.Surface,
                depth = CardsColors.Edge,
                padding = PaddingValues(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close bookmarks",
                    tint = CardsColors.Ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

        if (cards.isEmpty()) {
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
                        painter = painterResource(R.drawable.ic_review_star),
                        contentDescription = null,
                        tint = CardsColors.Muted.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "No Starred Translations Yet",
                        color = CardsColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Tap the star icon (⭐) during any translation to save it directly into your flashcards collection.",
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
                    .testTag("saved_translations_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = CardsColors.Edge.copy(alpha = 0.5f),
                            thickness = 1.dp,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Column: Japanese text + Romaji directly underneath (Rule 2)
                        val isJpTarget = card.targetLanguage == TranslationLanguage.Japanese
                        val jpText = if (isJpTarget) card.targetText else card.sourceText
                        val romajiText = if (isJpTarget) card.targetRomaji else ""
                        val englishText = if (isJpTarget) card.sourceText else card.targetText

                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = jpText,
                                color = CardsColors.Ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (romajiText.isNotBlank()) {
                                Text(
                                    text = romajiText,
                                    color = CardsColors.Blue,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        // English meaning
                        Text(
                            text = englishText,
                            color = CardsColors.Muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        )

                        // Audio button with animated waves
                        TranslatorAudioButton(
                            onClick = {
                                playingSavedId = card.id
                                onSpeak(card.targetText, card.targetLanguage)
                            },
                            isPlaying = isSpeaking && playingSavedId == card.id,
                            size = 36.dp,
                            tint = CardsColors.Blue,
                            description = "Play audio",
                        )

                        Spacer(Modifier.width(6.dp))

                        // Copy button
                        TranslatorCopyButton(
                            onClick = { onCopy(card.targetText) },
                            size = 36.dp,
                            tint = CardsColors.Ink,
                            description = "Copy text",
                        )

                        Spacer(Modifier.width(6.dp))

                        val isItemStarred = card.id !in stagedRemovedIds
                        // Star button to toggle bookmark (deferred removal on dismiss)
                        TranslatorStarButton(
                            onClick = {
                                stagedRemovedIds = if (card.id in stagedRemovedIds) {
                                    stagedRemovedIds - card.id
                                } else {
                                    stagedRemovedIds + card.id
                                }
                            },
                            isStarred = isItemStarred,
                            size = 36.dp,
                            description = if (isItemStarred) "Remove bookmark" else "Restore bookmark",
                        )
                    }
                }
            }
        }
    }
}
