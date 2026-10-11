package com.koto.app.feature.kana.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.kana.data.KanaMasteryStore
import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.data.KanaWordMatcher
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.theme.KotoType

enum class KanaTab {
    Chart,
    Practice,
}

@Composable
fun KanaScreen(
    onDismiss: () -> Unit,
    onSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    audio: JapaneseTtsController? = null,
) {
    val context = LocalContext.current
    val masteryStore = remember(context) { KanaMasteryStore.get(context) }
    var currentTab by rememberSaveable { mutableStateOf(KanaTab.Chart) }
    var currentScript by rememberSaveable { mutableStateOf(KanaScript.Hiragana) }
    var inspectingCharacter by remember { mutableStateOf<KanaCharacter?>(null) }
    var activeSubScreen by rememberSaveable { mutableStateOf("dashboard") }
    var activeQuizMode by remember { mutableStateOf<KanaQuizMode?>(null) }

    // Intercept back navigation if on a sub-page, modal, or quiz
    BackHandler {
        if (activeSubScreen != "dashboard") {
            activeSubScreen = "dashboard"
            activeQuizMode = null
        } else if (inspectingCharacter != null) {
            inspectingCharacter = null
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("screen_kana"),
    ) {
        // Main Screen Area: Animated transitions between Dashboard and Dedicated Sub-Pages (Word Bank, Filter, Quiz)
        AnimatedContent(
            targetState = activeSubScreen,
            transitionSpec = {
                if (initialState == "dashboard") {
                    (slideInHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(280)) { fullWidth -> -fullWidth / 4 } + fadeOut(animationSpec = tween(180))
                        )
                } else if (targetState == "dashboard") {
                    (slideInHorizontally(animationSpec = tween(280)) { fullWidth -> -fullWidth / 4 } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth } + fadeOut(animationSpec = tween(180))
                        )
                } else {
                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(180)))
                }
            },
            label = "KanaScreenPageTransition",
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            when (screen) {
                "dashboard" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Universal Symmetrical Top App Bar
                        // [Back Arrow] | Kana | [Neutral Settings Gear]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 16.dp),
                        ) {
                            // Left: Back button
                            CardsPressable(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.CenterStart)
                                    .testTag("btn_kana_back"),
                                face = CardsColors.Surface,
                                depth = CardsColors.Edge,
                                padding = PaddingValues(10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back),
                                    contentDescription = "Back",
                                    tint = CardsColors.Ink,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            // Center: Title "Kana"
                            Text(
                                text = "Kana",
                                style = KotoType.Brand,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CardsColors.Ink,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .testTag("kana_screen_title"),
                            )

                            // Right: Controls (Settings gear)
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CardsPressable(
                                    onClick = onSettings,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("btn_kana_settings"),
                                    face = CardsColors.Surface,
                                    depth = CardsColors.Edge,
                                    padding = PaddingValues(10.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_settings),
                                        contentDescription = "Settings",
                                        tint = CardsColors.Ink,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        // 2. Primary Segmented Switch: Chart / Practice (Normalized Blue Theme)
                        val segmentShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(segmentShape)
                                    .background(CardsColors.Surface)
                                    .border(1.dp, CardsColors.Edge, segmentShape)
                                    .padding(2.dp)
                                    .testTag("kana_tab_segmented_switch"),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                // Segment 1: Chart
                                val isChart = currentTab == KanaTab.Chart
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChart) CardsColors.Blue else Color.Transparent)
                                        .clickable { currentTab = KanaTab.Chart }
                                        .padding(vertical = 8.dp)
                                        .testTag("tab_kana_chart"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Chart",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChart) Color.White else CardsColors.Ink,
                                    )
                                }

                                // Segment 2: Practice
                                val isPractice = currentTab == KanaTab.Practice
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPractice) CardsColors.Blue else Color.Transparent)
                                        .clickable { currentTab = KanaTab.Practice }
                                        .padding(vertical = 8.dp)
                                        .testTag("tab_kana_practice"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Practice",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPractice) Color.White else CardsColors.Ink,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                        // Content Area: Switch between Chart and Practice
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                            },
                            modifier = Modifier.weight(1f),
                            label = "KanaTabContent",
                        ) { tab ->
                            when (tab) {
                                KanaTab.Chart -> {
                                    KanaChartTab(
                                        script = currentScript,
                                        onScriptChanged = { currentScript = it },
                                        onSelectCharacter = { inspectingCharacter = it },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                KanaTab.Practice -> {
                                    KanaPracticeTab(
                                        masteryStore = masteryStore,
                                        onSettings = onSettings,
                                        audio = audio,
                                        onOpenFilter = { activeSubScreen = "filter" },
                                        onOpenWordBank = { activeSubScreen = "word_bank" },
                                        onStartQuiz = { mode ->
                                            activeQuizMode = mode
                                            activeSubScreen = "quiz"
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
                "word_bank" -> {
                    val allChars = remember { KanaRepository.allCharacters }
                    val selectedChars = remember(masteryStore.selectedHiraganaIds, masteryStore.selectedKatakanaIds) {
                        allChars.filter { masteryStore.isCharacterSelected(it.id) }.toSet()
                    }
                    val allWords = remember(context) { KanaWordMatcher.getAllWords(context) }
                    val availablePracticeWords = remember(selectedChars, allWords) {
                        KanaWordMatcher.findAvailablePracticeWords(allWords, selectedChars)
                    }
                    KanaWordBankView(
                        availableWords = availablePracticeWords,
                        allWords = allWords,
                        audio = audio,
                        onDismiss = { activeSubScreen = "dashboard" },
                        onSettings = onSettings,
                        onOpenFilter = { activeSubScreen = "filter" },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                "filter" -> {
                    KanaFilterView(
                        masteryStore = masteryStore,
                        onDismiss = { activeSubScreen = "dashboard" },
                        onSettings = onSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                "quiz" -> {
                    val currentQuiz = activeQuizMode ?: KanaQuizMode.Test
                    val allChars = remember { KanaRepository.allCharacters }
                    val selectedChars = remember(masteryStore.selectedKanaIds) {
                        allChars.filter { masteryStore.isCharacterSelected(it.id) }
                    }
                    val vocabulary = remember(context) { KanaWordMatcher.getVocabulary(context) }
                    KanaQuizSessionView(
                        mode = currentQuiz,
                        selectedKana = selectedChars,
                        vocabulary = vocabulary,
                        masteryStore = masteryStore,
                        audio = audio,
                        onExit = {
                            activeQuizMode = null
                            activeSubScreen = "dashboard"
                        },
                        onSettings = onSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Modal Character Inspection Sheet
        val activeChar = inspectingCharacter
        if (activeChar != null) {
            KanaInspectSheet(
                character = activeChar,
                script = currentScript,
                audio = audio,
                onDismiss = { inspectingCharacter = null },
                onScriptChanged = { currentScript = it },
                onSelectCharacter = { inspectingCharacter = it },
            )
        }
    }
}
