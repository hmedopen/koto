package com.koto.app.feature.kana.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.kana.data.KanaMasteryStore
import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.data.KanaWordMatcher
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.theme.KotoType

/**
 * Modern Practice Dashboard:
 * - Deprecated legacy layout (checkboxes and bottom CTA purged).
 * - Header controls: Settings gear and elevated 3D Filter button.
 * - Prominent metric cards: "Selected Kana" & "Available Words" dynamically computed.
 * - 15-character minimum validation constraint before launching quizzes.
 * - Solid-colored Quiz Catalog matching Learn tab cards (Drawing mode strictly omitted).
 */
@Composable
fun KanaPracticeTab(
    masteryStore: KanaMasteryStore,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
    audio: JapaneseTtsController? = null,
    onOpenFilter: (() -> Unit)? = null,
    onOpenWordBank: (() -> Unit)? = null,
    onStartQuiz: ((KanaQuizMode) -> Unit)? = null,
) {
    val context = LocalContext.current
    var showFilterModal by remember { mutableStateOf(false) }
    var showWordBank by remember { mutableStateOf(false) }
    var internalActiveQuizMode by remember { mutableStateOf<KanaQuizMode?>(null) }

    val allCharacters = remember { KanaRepository.allCharacters }
    // Observe selection reactively for both Hiragana and Katakana
    val selectedHiraganaIds = masteryStore.selectedHiraganaIds
    val selectedKatakanaIds = masteryStore.selectedKatakanaIds
    val totalSelectedCount = selectedHiraganaIds.size + selectedKatakanaIds.size
    val selectedChars = remember(selectedHiraganaIds, selectedKatakanaIds) {
        allCharacters.filter { masteryStore.isCharacterSelected(it.id) }.toSet()
    }
    val allWords = remember(context) { KanaWordMatcher.getAllWords(context) }
    val availablePracticeWords = remember(selectedChars, allWords) {
        KanaWordMatcher.findAvailablePracticeWords(allWords, selectedChars)
    }
    val availableWordsCount = availablePracticeWords.size
    val vocabulary = remember(allWords) { allWords.map { it.kana to it.english } }

    // Intercept back navigation if internal quiz or word bank is active
    BackHandler(enabled = internalActiveQuizMode != null || (showWordBank && onOpenWordBank == null)) {
        if (internalActiveQuizMode != null) {
            internalActiveQuizMode = null
        } else if (showWordBank) {
            showWordBank = false
        }
    }

    // Dedicated Word Bank Screen (fallback when not hosted by full screen parent)
    if (showWordBank && onOpenWordBank == null) {
        KanaWordBankView(
            availableWords = availablePracticeWords,
            allWords = allWords,
            audio = audio,
            onDismiss = { showWordBank = false },
            onSettings = onSettings,
            onOpenFilter = {
                showWordBank = false
                if (onOpenFilter != null) onOpenFilter() else showFilterModal = true
            },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    // If running quiz internally (when no parent host callback is provided)
    if (internalActiveQuizMode != null && onStartQuiz == null) {
        KanaQuizSessionView(
            mode = internalActiveQuizMode!!,
            selectedKana = selectedChars.toList(),
            vocabulary = vocabulary,
            masteryStore = masteryStore,
            audio = audio,
            onExit = { internalActiveQuizMode = null },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val handleLaunchQuiz: (KanaQuizMode) -> Unit = { mode ->
        if (totalSelectedCount < 15) {
            Toast.makeText(
                context,
                "Select at least 15 Kana to start quizzes (currently $totalSelectedCount/15)",
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            if (onStartQuiz != null) {
                onStartQuiz(mode)
            } else {
                internalActiveQuizMode = mode
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("kana_practice_tab"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Section Header: Practice Drills
            Text(
                text = "Practice Drills",
                style = KotoType.Brand,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CardsColors.Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("practice_header_title"),
            )

            // 2. Prominent Metric Cards (Selected Kana & Available Words)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("practice_metrics_row"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Metric 1: Selected Kana
                PracticeMetricCard(
                    title = "Selected Kana",
                    value = "$totalSelectedCount",
                    onClick = { onOpenFilter?.invoke() ?: run { showFilterModal = true } },
                    modifier = Modifier.weight(1f),
                    cardTag = "card_stat_selected_kana",
                    valueTag = "text_stat_selected_kana_value",
                )

                // Metric 2: Available Words
                PracticeMetricCard(
                    title = "Available Words",
                    value = "$availableWordsCount",
                    onClick = { onOpenWordBank?.invoke() ?: run { showWordBank = true } },
                    modifier = Modifier.weight(1f),
                    cardTag = "card_stat_available_words",
                    valueTag = "text_stat_available_words_value",
                )
            }

            // 3. Minimum Selection Warning Banner (if < 15 characters selected)
            if (totalSelectedCount < 15) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("kana_selection_warning_banner"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Select at least 15 Kana to unlock quizzes ($totalSelectedCount/15)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Edit Filter",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardsColors.Blue,
                            modifier = Modifier
                                .clickable { onOpenFilter?.invoke() ?: run { showFilterModal = true } }
                                .padding(start = 8.dp)
                                .testTag("btn_warning_open_filter"),
                        )
                    }
                }
            }

            // 4. Section Title: Quizzes & Drills Catalog
            Text(
                text = "Study Modes",
                style = KotoType.Brand,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CardsColors.Ink,
                modifier = Modifier.padding(top = 4.dp),
            )

            // 5. Quiz Cards Catalog (Matching Learn Tab solid-color cards, subtitles removed)

            // 1. Mixed Mode (Indigo)
            PracticeQuizCard(
                title = "Mixed",
                iconRes = R.drawable.ic_swap,
                faceColor = Color(0xFF3F51B5),
                depthColor = Color(0xFF283593),
                onClick = { handleLaunchQuiz(KanaQuizMode.Mixed) },
                testTag = "quiz_card_mixed",
            )

            // 2. Test Mode (Deep Blue - ONLY mode that drives Mastery engine)
            PracticeQuizCard(
                title = "Test",
                iconRes = R.drawable.ic_trophy,
                faceColor = CardsColors.Blue,
                depthColor = CardsColors.BlueDepth,
                onClick = { handleLaunchQuiz(KanaQuizMode.Test) },
                testTag = "quiz_card_test",
            )

            // 3. Listening Challenge (Teal)
            PracticeQuizCard(
                title = "Listening Challenge",
                iconRes = R.drawable.ic_speaker,
                faceColor = Color(0xFF18758C),
                depthColor = Color(0xFF0F4E5E),
                onClick = { handleLaunchQuiz(KanaQuizMode.Listening) },
                testTag = "quiz_card_listening",
            )

            // 4. Word Selection (Emerald)
            PracticeQuizCard(
                title = "Word Selection",
                iconRes = R.drawable.ic_document,
                faceColor = Color(0xFF1E7E55),
                depthColor = Color(0xFF145338),
                onClick = { handleLaunchQuiz(KanaQuizMode.WordSelection) },
                testTag = "quiz_card_word_selection",
            )

            // 5. Matching Pairs (Purple)
            PracticeQuizCard(
                title = "Matching Pairs",
                iconRes = R.drawable.ic_swap,
                faceColor = Color(0xFF5B4393),
                depthColor = Color(0xFF3E2D64),
                onClick = { handleLaunchQuiz(KanaQuizMode.MatchingPairs) },
                testTag = "quiz_card_matching_pairs",
            )

            // 6. Word Building (Coral)
            PracticeQuizCard(
                title = "Word Building",
                iconRes = R.drawable.ic_edit,
                faceColor = Color(0xFFC04B33),
                depthColor = Color(0xFF833323),
                onClick = { handleLaunchQuiz(KanaQuizMode.WordBuilding) },
                testTag = "quiz_card_word_building",
            )

            // 7. Typing Quiz (Dark Slate)
            PracticeQuizCard(
                title = "Typing Quiz",
                iconRes = R.drawable.ic_edit,
                faceColor = Color(0xFF37474F),
                depthColor = Color(0xFF263238),
                onClick = { handleLaunchQuiz(KanaQuizMode.Typing) },
                testTag = "quiz_card_typing",
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Granular Filter Bottom Sheet Modal (fallback when not hosted by full screen parent)
    if (showFilterModal && onOpenFilter == null) {
        KanaFilterModal(
            masteryStore = masteryStore,
            onDismiss = { showFilterModal = false },
            onSettings = onSettings,
        )
    }
}

/**
 * Metric stat card complying with the Anti-Bubble principle:
 * Pure white background, 3D tactile bottom depth, hairline 1dp border, crisp squircle geometry.
 */
@Composable
private fun PracticeMetricCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardTag: String = "",
    valueTag: String = "",
) {
    CardsPressable(
        onClick = onClick,
        modifier = modifier.testTag(cardTag),
        face = CardsColors.Surface,
        depth = CardsColors.Edge,
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CardsColors.Muted,
            )
            Text(
                text = value,
                style = KotoType.Brand,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CardsColors.Ink,
                modifier = Modifier.testTag(valueTag),
            )
        }
    }
}

/**
 * Solid-color quiz drill card matching the Learn tab cards:
 * Solid face color, 3D bottom bevel depth, crisp white line vector icon, and right arrow indicator.
 */
@Composable
private fun PracticeQuizCard(
    title: String,
    iconRes: Int,
    faceColor: Color,
    depthColor: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    CardsPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        face = faceColor,
        depth = depthColor,
        padding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flat crisp white vector line icon directly on solid background
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )

            Spacer(Modifier.width(18.dp))

            // Title centered vertically with icon and arrow
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )

            // Arrow forward indicator (crisp line art)
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Start",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = 180f },
            )
        }
    }
}
