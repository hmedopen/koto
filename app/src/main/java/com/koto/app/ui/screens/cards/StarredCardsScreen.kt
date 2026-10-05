package com.koto.app.ui.screens.cards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.audio.SpeechStatus
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.feature.lesson.ui.SpeakerButton
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone

@Composable
internal fun StarredCardsScreen(
    decks: List<FlashcardDeck>,
    state: FlashcardState,
    onToggleFavorite: (String) -> Unit,
    onStartReview: (List<Flashcard>) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val audio = remember(context) { JapaneseTtsController.get(context) }
    var playingCardId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(audio.isSpeaking) {
        if (!audio.isSpeaking) {
            playingCardId = null
        }
    }

    // Aggregate all starred cards across all decks upon entering screen (deferred removal)
    val starredCards = remember(decks) {
        val allCards = decks.flatMap { it.cards }
        // Keep unique cards by id that are initially in favorites
        val seen = mutableSetOf<String>()
        allCards.filter { card ->
            card.id in state.favorites && seen.add(card.id)
        }
    }

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .testTag("starred_cards_screen"),
    ) {

        if (starredCards.isEmpty()) {
            Box(
                Modifier
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
                        tint = CardsColors.Muted.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "No Starred Words Yet",
                        color = CardsColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Tap the star icon on any card during study or deck content preview to save it here for targeted review.",
                        color = CardsColors.Muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("starred_words_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                itemsIndexed(starredCards, key = { _, card -> card.id }) { index, card ->
                    if (index > 0) {
                        HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.5f), thickness = 0.8.dp)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                            .testTag("starred_row_${card.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: Kana + Romaji (Romaji directly under Kana, Rule 2)
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

                        // Audio button
                        SpeakerButton(
                            text = JapaneseText(card.japanese, card.romaji),
                            speechReady = audio.enabled && audio.status == SpeechStatus.Ready,
                            isPlaying = (playingCardId == card.id) && audio.isSpeaking,
                            speak = {
                                playingCardId = card.id
                                audio.speak(it)
                            },
                            modifier = Modifier.size(38.dp, 42.dp),
                            description = "Play pronunciation: ${card.romaji}",
                        )

                        Spacer(Modifier.width(4.dp))

                        MarkButton(
                            kind = "star",
                            active = card.id in state.favorites,
                            label = "Star ${card.english}",
                            tag = "favorite_${card.id}",
                        ) {
                            onToggleFavorite(card.id)
                        }
                    }
                }
            }

            val activeStarredCards = remember(starredCards, state.favorites) {
                starredCards.filter { it.id in state.favorites }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                CardsButton(
                    label = "START REVIEW (${activeStarredCards.size})",
                    onClick = { onStartReview(activeStarredCards) },
                    enabled = activeStarredCards.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("start_starred_review"),
                )
            }
        }
    }
}
