package com.koto.app.feature.kana.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.koto.app.feature.kana.data.KanaMasteryStore
import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.data.KanaWordMatcher
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.ui.components.SessionControlBar
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.theme.KotoType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class KanaQuizMode(val displayName: String, val subtitle: String) {
    Mixed("Mixed", "All at once"),
    Test("Test", "Choose an answer against the clock"),
    Listening("Listening", "Select the correct answer"),
    WordSelection("Word Selection", "Choose the correct answer"),
    MatchingPairs("Matching Pairs", "Match word pairs"),
    WordBuilding("Word Building", "Form a word"),
    Typing("Typing", "Type the syllable"),
}

@Composable
fun KanaQuizSessionView(
    mode: KanaQuizMode,
    selectedKana: List<KanaCharacter>,
    vocabulary: List<Pair<String, String>>,
    masteryStore: KanaMasteryStore,
    audio: JapaneseTtsController?,
    onExit: () -> Unit,
    onSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val totalQuestions = 16
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var sessionFinished by remember { mutableStateOf(false) }

    // Resolve active drill type for question
    val activeDrill = remember(currentQuestionIndex, mode) {
        if (mode == KanaQuizMode.Mixed) {
            val drillOptions = listOf(
                KanaQuizMode.Test,
                KanaQuizMode.Listening,
                KanaQuizMode.WordSelection,
                KanaQuizMode.MatchingPairs,
                KanaQuizMode.WordBuilding,
                KanaQuizMode.Typing,
            )
            drillOptions[Random.nextInt(drillOptions.size)]
        } else {
            mode
        }
    }

    if (sessionFinished) {
        QuizFinishedScreen(onExit = onExit)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // Absorb any click events so they never bleed into background screens
            )
            .testTag("kana_quiz_session_view"),
    ) {
        // Shared 3D Session Control Bar (identical to Cards study & Lesson study)
        val sessionProgress = (currentQuestionIndex.toFloat() / totalQuestions).coerceIn(0f, 1f)
        SessionControlBar(
            progress = sessionProgress,
            onClose = onExit,
            onSettings = onSettings,
            closeTag = "btn_quiz_exit",
            progressTag = "quiz_progress",
            settingsTag = "btn_quiz_settings",
            closeDescription = "Exit quiz",
            settingsDescription = "Settings",
        )

        // Drill Content Area - Keyed by currentQuestionIndex so every question resets cleanly
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            key(currentQuestionIndex) {
                when (activeDrill) {
                    KanaQuizMode.Test -> {
                        TestQuizDrill(
                            selectedKana = selectedKana,
                            onResult = { isCorrect, elapsedFraction, char ->
                                if (isCorrect) {
                                    val speedBonus = if (elapsedFraction < 0.5f) 2 else 0
                                    streak++
                                    val streakMultiplier = if (streak >= 3) 1.2f else 1.0f
                                    val points = ((5 + speedBonus) * streakMultiplier).toInt()
                                    masteryStore.updateMasteryScore(char.id, isHiragana = true, delta = points)
                                } else {
                                    streak = 0
                                    masteryStore.updateMasteryScore(char.id, isHiragana = true, delta = -4)
                                }
                                if (currentQuestionIndex + 1 >= totalQuestions) {
                                    sessionFinished = true
                                } else {
                                    currentQuestionIndex++
                                }
                            },
                        )
                    }

                    KanaQuizMode.Listening -> {
                        ListeningQuizDrill(
                            selectedKana = selectedKana,
                            audio = audio,
                            onNext = {
                                if (currentQuestionIndex + 1 >= totalQuestions) sessionFinished = true else currentQuestionIndex++
                            },
                        )
                    }

                    KanaQuizMode.WordSelection -> {
                        WordSelectionQuizDrill(
                            vocabulary = vocabulary,
                            selectedKana = selectedKana,
                            onNext = {
                                if (currentQuestionIndex + 1 >= totalQuestions) sessionFinished = true else currentQuestionIndex++
                            },
                        )
                    }

                    KanaQuizMode.MatchingPairs -> {
                        MatchingPairsQuizDrill(
                            selectedKana = selectedKana,
                            onNext = {
                                if (currentQuestionIndex + 1 >= totalQuestions) sessionFinished = true else currentQuestionIndex++
                            },
                        )
                    }

                    KanaQuizMode.WordBuilding -> {
                        WordBuildingQuizDrill(
                            vocabulary = vocabulary,
                            selectedKana = selectedKana,
                            onNext = {
                                if (currentQuestionIndex + 1 >= totalQuestions) sessionFinished = true else currentQuestionIndex++
                            },
                        )
                    }

                    KanaQuizMode.Typing -> {
                        TypingQuizDrill(
                            selectedKana = selectedKana,
                            onNext = {
                                if (currentQuestionIndex + 1 >= totalQuestions) sessionFinished = true else currentQuestionIndex++
                            },
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

// ==========================================
// DRILL 1: TEST MODE (CRITICAL MASTERY ENGINE)
// ==========================================
@Composable
private fun TestQuizDrill(
    selectedKana: List<KanaCharacter>,
    onResult: (isCorrect: Boolean, elapsedFraction: Float, char: KanaCharacter) -> Unit,
) {
    val targetChar = remember(selectedKana) { selectedKana.random() }
    val isRomajiPrompt = remember { Random.nextBoolean() }
    val options = remember(targetChar, selectedKana) {
        val others = selectedKana.filter { it.id != targetChar.id }.shuffled().take(3)
        (others + targetChar).shuffled()
    }

    val totalTimeMs = 8000L
    var timeRemainingMs by remember { mutableLongStateOf(totalTimeMs) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isAnswered) {
        if (!isAnswered) {
            val interval = 100L
            while (timeRemainingMs > 0 && !isAnswered) {
                delay(interval)
                timeRemainingMs -= interval
            }
            if (!isAnswered) {
                isAnswered = true
                delay(300)
                onResult(false, 1.0f, targetChar)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Countdown timer bar & time display
        val timeFraction = (timeRemainingMs / totalTimeMs.toFloat()).coerceIn(0f, 1f)
        val seconds = (timeRemainingMs / 1000).coerceAtLeast(0)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LinearProgressIndicator(
                progress = { timeFraction },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (timeFraction < 0.3f) Color(0xFFE53935) else Color(0xFFFBC02D),
                trackColor = Color(0xFFF1F3F5),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "00:0$seconds",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CardsColors.Ink,
            )
        }

        Spacer(Modifier.height(48.dp))

        // Prompt
        val promptText = if (isRomajiPrompt) targetChar.romaji.uppercase() else targetChar.hiragana
        val promptSub = if (isRomajiPrompt) "Romaji" else "Hiragana"
        Text(
            text = promptText,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = if (!isRomajiPrompt) LocalJapaneseFont.current else null,
            color = CardsColors.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = promptSub,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = CardsColors.Muted,
        )

        Spacer(Modifier.weight(1f))

        // 4 Options Grid (2x2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    for (col in 0..1) {
                        val opt = options[row * 2 + col]
                        val isCorrect = opt.id == targetChar.id
                        val isSelected = selectedOptionId == opt.id
                        val bgColor = when {
                            isAnswered && isCorrect -> Color(0xFF43A047) // Green
                            isAnswered && isSelected -> Color(0xFFE53935) // Red
                            else -> Color(0xFFF9FAFB)
                        }
                        val textColor = if (isAnswered && (isCorrect || isSelected)) Color.White else CardsColors.Ink

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .border(1.dp, CardsColors.Edge, RoundedCornerShape(16.dp))
                                .clickable(enabled = !isAnswered) {
                                    isAnswered = true
                                    selectedOptionId = opt.id
                                    val elapsedFraction = 1f - (timeRemainingMs / totalTimeMs.toFloat())
                                    coroutineScope.launch {
                                        delay(450)
                                        onResult(isCorrect, elapsedFraction, targetChar)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val optionText = if (isRomajiPrompt) opt.hiragana else opt.romaji.uppercase()
                            Text(
                                text = optionText,
                                fontSize = if (isRomajiPrompt) 28.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isRomajiPrompt) LocalJapaneseFont.current else null,
                                color = textColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DRILL 2: LISTENING CHALLENGE
// ==========================================
@Composable
private fun ListeningQuizDrill(
    selectedKana: List<KanaCharacter>,
    audio: JapaneseTtsController?,
    onNext: () -> Unit,
) {
    val target = remember { selectedKana.random() }
    val options = remember(target) {
        val others = selectedKana.filter { it.id != target.id }.shuffled().take(3)
        (others + target).shuffled()
    }
    var answered by remember { mutableStateOf(false) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(target) {
        delay(200)
        audio?.speak(JapaneseText(kana = target.hiragana, romaji = target.romaji, tts = target.hiragana))
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.35f))

        // Normal 3D Tactile Audio Button (Anti-Bubble, Squircle Geometry, Pure White Fill)
        CardsPressable(
            onClick = {
                audio?.speak(JapaneseText(kana = target.hiragana, romaji = target.romaji, tts = target.hiragana))
            },
            modifier = Modifier
                .width(170.dp)
                .height(56.dp),
            face = CardsColors.Surface,
            depth = CardsColors.Edge,
            padding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_speaker),
                    contentDescription = "Play Audio",
                    tint = CardsColors.Ink,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Play Audio",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardsColors.Ink,
                )
            }
        }

        Spacer(Modifier.weight(0.65f))

        // 4 Options (2x2 Grid)
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (row in 0..1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (col in 0..1) {
                        val opt = options[row * 2 + col]
                        val isCorrect = opt.id == target.id
                        val isSelected = selectedOptionId == opt.id
                        val bgColor = when {
                            answered && isCorrect -> Color(0xFF43A047)
                            answered && isSelected -> Color(0xFFE53935)
                            else -> Color(0xFFF9FAFB)
                        }
                        val textColor = if (answered && (isCorrect || isSelected)) Color.White else CardsColors.Ink

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .border(1.dp, CardsColors.Edge, RoundedCornerShape(16.dp))
                                .clickable(enabled = !answered) {
                                    answered = true
                                    selectedOptionId = opt.id
                                    coroutineScope.launch {
                                        delay(450)
                                        onNext()
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = opt.hiragana, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = LocalJapaneseFont.current, color = textColor)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DRILL 3: WORD SELECTION
// ==========================================
@Composable
private fun WordSelectionQuizDrill(
    vocabulary: List<Pair<String, String>>,
    selectedKana: List<KanaCharacter>,
    onNext: () -> Unit,
) {
    val availableWords = remember {
        val filtered = KanaWordMatcher.findAvailableWords(vocabulary, selectedKana.toSet())
            .filter { it.first.length in 2..6 }
        if (filtered.size >= 4) filtered
        else if (vocabulary.size >= 4) vocabulary.filter { it.first.length in 2..6 }
        else listOf(
            "ねこ" to "cat",
            "いぬ" to "dog",
            "さくら" to "cherry blossom",
            "みず" to "water",
            "やま" to "mountain",
            "ほん" to "book",
            "くるま" to "car",
            "つき" to "moon",
        )
    }
    val targetPair = remember { availableWords.random() }
    val options = remember(targetPair) {
        val others = availableWords.filter { it.second != targetPair.second }.shuffled().take(3)
        (others + targetPair).shuffled().map { it.second }
    }
    var answered by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = "Select the correct translation", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CardsColors.Muted)
        Spacer(Modifier.height(16.dp))
        Text(text = targetPair.first, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, fontFamily = LocalJapaneseFont.current, color = CardsColors.Ink)
        Spacer(Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { opt ->
                val isCorrect = opt == targetPair.second
                val isSelected = selectedOptionText == opt
                val faceColor = when {
                    answered && isCorrect -> Color(0xFF43A047)
                    answered && isSelected -> Color(0xFFE53935)
                    else -> CardsColors.Surface
                }
                val depthColor = when {
                    answered && isCorrect -> Color(0xFF2E7D32)
                    answered && isSelected -> Color(0xFFC62828)
                    else -> CardsColors.Edge
                }
                val textColor = if (answered && (isCorrect || isSelected)) Color.White else CardsColors.Ink

                CardsPressable(
                    onClick = {
                        if (!answered) {
                            answered = true
                            selectedOptionText = opt
                            coroutineScope.launch {
                                delay(450)
                                onNext()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    face = faceColor,
                    depth = depthColor,
                    padding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(
                        text = opt,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ==========================================
// DRILL 4: MATCHING PAIRS
// ==========================================
@Composable
private fun MatchingPairsQuizDrill(
    selectedKana: List<KanaCharacter>,
    onNext: () -> Unit,
) {
    val pairs = remember {
        selectedKana.shuffled().take(3).map { it.hiragana to it.romaji.uppercase() }
    }
    val shuffledRomaji = remember(pairs) { pairs.map { it.second }.shuffled() }
    var selectedKanaCard by remember { mutableStateOf<String?>(null) }
    var matchedKanaSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var matchedRomajiSet by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(matchedKanaSet.size) {
        if (matchedKanaSet.size == pairs.size && pairs.isNotEmpty()) {
            delay(500)
            onNext()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = "Match Kana with transliteration", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CardsColors.Ink)
        Spacer(Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            pairs.indices.forEach { index ->
                val kana = pairs[index].first
                val romaji = shuffledRomaji[index]
                val isKanaMatched = matchedKanaSet.contains(kana)
                val isRomajiMatched = matchedRomajiSet.contains(romaji)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Kana card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedKanaCard == kana) CardsColors.Blue
                                else if (isKanaMatched) Color(0xFFE8F5E9)
                                else Color(0xFFF9FAFB),
                            )
                            .border(1.dp, CardsColors.Edge, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isKanaMatched) {
                                selectedKanaCard = kana
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = kana,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalJapaneseFont.current,
                            color = if (selectedKanaCard == kana) Color.White else if (isKanaMatched) Color(0xFF2E7D32) else CardsColors.Ink,
                        )
                    }

                    // Romaji card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isRomajiMatched) Color(0xFFE8F5E9)
                                else Color(0xFFF9FAFB),
                            )
                            .border(1.dp, CardsColors.Edge, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRomajiMatched && selectedKanaCard != null) {
                                val requiredRomaji = pairs.firstOrNull { it.first == selectedKanaCard }?.second
                                if (requiredRomaji == romaji) {
                                    matchedKanaSet = matchedKanaSet + selectedKanaCard!!
                                    matchedRomajiSet = matchedRomajiSet + romaji
                                }
                                selectedKanaCard = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = romaji,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRomajiMatched) Color(0xFF2E7D32) else CardsColors.Ink,
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// DRILL 5: WORD BUILDING
// ==========================================
private data class WordBuildingTarget(val word: String, val meaning: String, val romaji: String)

private val FallbackWordBuildingPool = listOf(
    WordBuildingTarget("ねこ", "cat", "neko"),
    WordBuildingTarget("いぬ", "dog", "inu"),
    WordBuildingTarget("さくら", "cherry blossom", "sakura"),
    WordBuildingTarget("みず", "water", "mizu"),
    WordBuildingTarget("やま", "mountain", "yama"),
    WordBuildingTarget("かわ", "river", "kawa"),
    WordBuildingTarget("ほん", "book", "hon"),
    WordBuildingTarget("くるま", "car", "kuruma"),
    WordBuildingTarget("つき", "moon", "tsuki"),
    WordBuildingTarget("あめ", "rain", "ame"),
    WordBuildingTarget("はな", "flower", "hana"),
    WordBuildingTarget("そら", "sky", "sora"),
    WordBuildingTarget("ゆき", "snow", "yuki"),
    WordBuildingTarget("ひかり", "light", "hikari"),
    WordBuildingTarget("ともだち", "friend", "tomodachi"),
    WordBuildingTarget("ハイキング", "hiking", "haikingu"),
    WordBuildingTarget("カメラ", "camera", "kamera"),
    WordBuildingTarget("パン", "bread", "pan"),
    WordBuildingTarget("バス", "bus", "basu"),
    WordBuildingTarget("ラジオ", "radio", "rajio"),
)

@Composable
private fun WordBuildingQuizDrill(
    vocabulary: List<Pair<String, String>>,
    selectedKana: List<KanaCharacter>,
    onNext: () -> Unit,
) {
    val candidate = remember {
        val available = KanaWordMatcher.findAvailableWords(vocabulary, selectedKana.toSet())
            .filter { it.first.length in 2..5 }
        if (available.isNotEmpty()) {
            val picked = available.random()
            WordBuildingTarget(picked.first, picked.second, picked.first)
        } else {
            FallbackWordBuildingPool.random()
        }
    }

    val word = candidate.word
    val meaning = candidate.meaning
    val romaji = candidate.romaji
    val charList = remember(word) { word.map { it.toString() } }
    val scrambledPool = remember(charList) {
        var scrambled = charList.shuffled()
        if (scrambled == charList && charList.size > 1) {
            scrambled = charList.reversed()
        }
        scrambled
    }

    var assembled by remember { mutableStateOf<List<String>>(emptyList()) }
    var availablePool by remember { mutableStateOf(scrambledPool) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(assembled) {
        if (!isCompleted && assembled.joinToString("") == word) {
            isCompleted = true
            delay(500)
            onNext()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = "Select Kana in correct order", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CardsColors.Muted)
        Spacer(Modifier.height(16.dp))
        Text(text = romaji, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = CardsColors.Ink)
        Text(text = meaning, fontSize = 16.sp, color = CardsColors.Muted)

        Spacer(Modifier.height(36.dp))

        // Slots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until charList.size) {
                val filled = assembled.getOrNull(i)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompleted) Color(0xFFE8F5E9) else if (filled != null) CardsColors.Surface else Color(0xFFF1F3F5))
                        .border(1.dp, if (isCompleted) Color(0xFF43A047) else CardsColors.Edge, RoundedCornerShape(8.dp))
                        .clickable(enabled = filled != null && !isCompleted) {
                            // Return back to pool
                            if (filled != null) {
                                assembled = assembled.toMutableList().also { it.removeAt(i) }
                                availablePool = availablePool + filled
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = filled ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalJapaneseFont.current,
                        color = if (isCompleted) Color(0xFF2E7D32) else CardsColors.Ink,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Scrambled pool tiles
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            availablePool.forEachIndexed { idx, char ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F3F5))
                        .border(1.dp, CardsColors.Edge, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isCompleted) {
                            availablePool = availablePool.toMutableList().also { it.removeAt(idx) }
                            assembled = assembled + char
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = char, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = LocalJapaneseFont.current, color = CardsColors.Ink)
                }
            }
        }
    }
}

// ==========================================
// DRILL 6: TYPING QUIZ (VIRTUAL QWERTY KEYBOARD)
// ==========================================
@Composable
private fun TypingQuizDrill(
    selectedKana: List<KanaCharacter>,
    onNext: () -> Unit,
) {
    val target = remember { selectedKana.random() }
    var typedInput by remember { mutableStateOf("") }
    var isSubmittedCorrect by remember { mutableStateOf(false) }
    var showErrorFeedback by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(typedInput) {
        if (!isSubmittedCorrect && typedInput.equals(target.romaji, ignoreCase = true)) {
            isSubmittedCorrect = true
            delay(400)
            onNext()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = target.hiragana, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, fontFamily = LocalJapaneseFont.current, color = CardsColors.Ink)
        Text(text = "Hiragana", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = CardsColors.Muted)

        Spacer(Modifier.height(24.dp))

        // Input Slot with dashed indicator
        val boxBorderColor = when {
            isSubmittedCorrect -> Color(0xFF43A047)
            showErrorFeedback -> Color(0xFFE53935)
            else -> CardsColors.Blue
        }
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(48.dp)
                .border(2.dp, boxBorderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = typedInput.ifEmpty { "_ _" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (showErrorFeedback) Color(0xFFE53935) else if (isSubmittedCorrect) Color(0xFF43A047) else CardsColors.Ink,
                letterSpacing = 4.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        // On-Screen Custom QWERTY Virtual Keyboard
        CustomQwertyKeyboard(
            onKeyPress = { key ->
                if (!isSubmittedCorrect && typedInput.length < 5) typedInput += key
            },
            onBackspace = {
                if (!isSubmittedCorrect && typedInput.isNotEmpty()) typedInput = typedInput.dropLast(1)
            },
            onDone = {
                if (typedInput.equals(target.romaji, ignoreCase = true)) {
                    isSubmittedCorrect = true
                    coroutineScope.launch {
                        delay(400)
                        onNext()
                    }
                } else {
                    showErrorFeedback = true
                    coroutineScope.launch {
                        delay(400)
                        showErrorFeedback = false
                        typedInput = ""
                    }
                }
            },
        )
    }
}

@Composable
private fun CustomQwertyKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit,
) {
    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFECEFF1))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row1.forEach { key ->
                KeyButton(label = key, onClick = { onKeyPress(key) }, modifier = Modifier.weight(1f))
            }
        }
        // Row 2
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row2.forEach { key ->
                KeyButton(label = key, onClick = { onKeyPress(key) }, modifier = Modifier.weight(1f))
            }
        }
        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Spacer(Modifier.width(18.dp))
            row3.forEach { key ->
                KeyButton(label = key, onClick = { onKeyPress(key) }, modifier = Modifier.weight(1f))
            }
            // Backspace key
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .clickable(onClick = onBackspace),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⌫", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CardsColors.Ink)
            }
        }
        // Row 4: Space & Done
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .clickable { onKeyPress(" ") },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Space", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CardsColors.Ink)
            }
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardsColors.Blue)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CardsColors.Ink)
    }
}

@Composable
private fun QuizFinishedScreen(onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painter = painterResource(R.drawable.ic_trophy), contentDescription = null, tint = CardsColors.Blue, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(text = "Session Complete!", style = KotoType.Brand, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CardsColors.Ink)
        Spacer(Modifier.height(8.dp))
        Text(text = "Great job sharpening your Kana mastery.", fontSize = 14.sp, color = CardsColors.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        CardsPressable(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
            face = CardsColors.Blue,
            depth = CardsColors.BlueDepth,
            padding = PaddingValues(vertical = 14.dp),
        ) {
            Text(text = "Return to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
