package com.koto.app.feature.kana.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.kana.data.KanaMasteryStore
import com.koto.app.feature.kana.data.KanaTestScoringEngine
import com.koto.app.feature.kana.data.KanaWordMatcher
import com.koto.app.feature.kana.data.PracticeWordPool
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.kana.model.PracticeWord
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.ui.components.SessionControlBar
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone
import com.koto.app.ui.screens.cards.CardsButton
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

/**
 * Unified target for Kana quizzes, preserving whether Hiragana or Katakana is being tested.
 */
data class QuizKanaTarget(
    val character: KanaCharacter,
    val script: KanaScript,
) {
    val glyph: String get() = character.characterFor(script)
    val romaji: String get() = character.romaji
    val id: String get() = character.id
    val scriptName: String get() = script.displayName
}

private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = context.getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    } catch (_: Exception) {}
}

@Composable
fun KanaQuizSessionView(
    mode: KanaQuizMode,
    selectedKana: List<KanaCharacter> = emptyList(),
    selectedHiragana: List<KanaCharacter> = emptyList(),
    selectedKatakana: List<KanaCharacter> = emptyList(),
    vocabulary: List<Pair<String, String>>,
    masteryStore: KanaMasteryStore,
    audio: JapaneseTtsController?,
    onExit: () -> Unit,
    onSettings: () -> Unit = {},
    activeScript: KanaScript? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val totalQuestions = 20
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var sessionFinished by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var sessionStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sessionDurationSeconds by remember { mutableLongStateOf(0L) }

    // Intercept back navigation if quiz is in progress to show confirmation dialog
    BackHandler(enabled = !sessionFinished) {
        showExitConfirmation = true
    }

    // Build script-aware quiz targets list based on selected Hiragana and Katakana
    val quizTargets = remember(selectedHiragana, selectedKatakana, selectedKana, activeScript, mode) {
        val list = mutableListOf<QuizKanaTarget>()
        // For Test quiz mode, prioritize the active script to keep evaluation focused
        if (mode == KanaQuizMode.Test && activeScript != null) {
            if (activeScript == KanaScript.Hiragana && selectedHiragana.isNotEmpty()) {
                selectedHiragana.forEach { list.add(QuizKanaTarget(it, KanaScript.Hiragana)) }
            } else if (activeScript == KanaScript.Katakana && selectedKatakana.isNotEmpty()) {
                selectedKatakana.forEach { list.add(QuizKanaTarget(it, KanaScript.Katakana)) }
            }
        }
        if (list.isEmpty()) {
            if (selectedHiragana.isNotEmpty() || selectedKatakana.isNotEmpty()) {
                selectedHiragana.forEach { list.add(QuizKanaTarget(it, KanaScript.Hiragana)) }
                selectedKatakana.forEach { list.add(QuizKanaTarget(it, KanaScript.Katakana)) }
            } else {
                selectedKana.forEach { char ->
                    val hSelected = masteryStore.isCharacterSelected(char.id, isHiragana = true)
                    val kSelected = masteryStore.isCharacterSelected(char.id, isHiragana = false)
                    if (hSelected) list.add(QuizKanaTarget(char, KanaScript.Hiragana))
                    if (kSelected) list.add(QuizKanaTarget(char, KanaScript.Katakana))
                    if (!hSelected && !kSelected) list.add(QuizKanaTarget(char, KanaScript.Hiragana))
                }
            }
        }
        if (list.isEmpty() && selectedKana.isNotEmpty()) {
            selectedKana.forEach { list.add(QuizKanaTarget(it, KanaScript.Hiragana)) }
        }
        list
    }

    // Curated Practice Words for Word Selection, Listening Challenge, and Word Building drills
    val allPracticeWords = remember(context) { PracticeWordPool.getWords(context) }
    val availablePracticeWords = remember(selectedHiragana, selectedKatakana, allPracticeWords) {
        val hiraSet = selectedHiragana.toSet()
        val kataSet = selectedKatakana.toSet()
        val found = KanaWordMatcher.findAvailablePracticeWords(allPracticeWords, hiraSet, kataSet)
        if (found.size >= 10) found else (found + PracticeWordPool.fallbackWords).distinctBy { it.id }
    }

    var sessionKey by remember { mutableIntStateOf(0) }
    // Pre-resolve drill types for all questions in the session for stable animated transitions
    val drillsPerQuestion = remember(mode, sessionKey) {
        if (mode == KanaQuizMode.Mixed) {
            val drillOptions = listOf(
                KanaQuizMode.Listening,
                KanaQuizMode.WordSelection,
                KanaQuizMode.MatchingPairs,
                KanaQuizMode.WordBuilding,
                KanaQuizMode.Typing,
            )
            (0 until totalQuestions).map { drillOptions[Random.nextInt(drillOptions.size)] }
        } else {
            List(totalQuestions) { mode }
        }
    }

    // Pre-resolve balanced targets for Test mode across the 20 questions
    val testTargetsPerQuestion = remember(quizTargets, sessionKey) {
        if (quizTargets.isEmpty()) emptyList()
        else {
            val shuffled = quizTargets.shuffled()
            (0 until totalQuestions).map { i -> shuffled[i % shuffled.size] }
        }
    }

    // Hybrid Mastery Engine for Test mode
    val scoringEngine = remember(sessionKey) {
        KanaTestScoringEngine(
            initialScoreProvider = { kanaId, isHiragana ->
                masteryStore.getCharacterMastery(kanaId, isHiragana)
            }
        )
    }
    var isInRetryPhase by remember(sessionKey) { mutableStateOf(false) }
    var retryCurrentTarget by remember(sessionKey) { mutableStateOf<QuizKanaTarget?>(null) }
    var retryStepCount by remember(sessionKey) { mutableIntStateOf(0) }

    val handleTestQuestionResult: (Boolean, QuizKanaTarget) -> Unit = { isCorrect, target ->
        if (!isInRetryPhase) {
            scoringEngine.recordAttempt(target = target, isCorrect = isCorrect, isRetryPhase = false)
            if (isCorrect) {
                correctCount++
                streak++
                if (streak > bestStreak) bestStreak = streak
            } else {
                streak = 0
            }
            if (currentQuestionIndex + 1 < totalQuestions) {
                currentQuestionIndex++
            } else {
                // Initial 20 questions completed; check for retry queue
                if (scoringEngine.hasPendingRetries()) {
                    isInRetryPhase = true
                    retryCurrentTarget = scoringEngine.nextRetryTarget()
                } else {
                    masteryStore.commitTestSessionOutcomes(scoringEngine.computeFinalOutcomes())
                    sessionDurationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).coerceAtLeast(1)
                    sessionFinished = true
                }
            }
        } else {
            // In Retry Phase (awards 0 points; strictly for pedagogical reinforcement)
            scoringEngine.recordAttempt(target = target, isCorrect = isCorrect, isRetryPhase = true)
            retryStepCount++
            if (scoringEngine.hasPendingRetries()) {
                retryCurrentTarget = scoringEngine.nextRetryTarget()
            } else {
                masteryStore.commitTestSessionOutcomes(scoringEngine.computeFinalOutcomes())
                sessionDurationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).coerceAtLeast(1)
                sessionFinished = true
            }
        }
    }

    val onQuestionCompleted: (Boolean) -> Unit = { isCorrect ->
        if (isCorrect) {
            correctCount++
            streak++
            if (streak > bestStreak) bestStreak = streak
        } else {
            streak = 0
        }
        if (currentQuestionIndex + 1 >= totalQuestions) {
            sessionDurationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).coerceAtLeast(1)
            sessionFinished = true
        } else {
            currentQuestionIndex++
        }
    }

    val onRestartSession: () -> Unit = {
        currentQuestionIndex = 0
        correctCount = 0
        streak = 0
        bestStreak = 0
        isInRetryPhase = false
        retryCurrentTarget = null
        retryStepCount = 0
        sessionStartTime = System.currentTimeMillis()
        sessionFinished = false
        sessionKey++
    }

    if (sessionFinished) {
        QuizFinishedScreen(
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            bestStreak = bestStreak,
            durationSeconds = sessionDurationSeconds,
            onRestart = onRestartSession,
            onExit = onExit,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .testTag("kana_quiz_session_view"),
    ) {
        // Shared 3D Session Control Bar (identical to Cards study & Lesson study)
        val sessionProgress = if (isInRetryPhase) 1f else (currentQuestionIndex.toFloat() / totalQuestions).coerceIn(0f, 1f)
        SessionControlBar(
            progress = sessionProgress,
            onClose = { showExitConfirmation = true },
            onSettings = onSettings,
            closeTag = "btn_quiz_exit",
            progressTag = "quiz_progress",
            settingsTag = "btn_quiz_settings",
            closeDescription = "Exit quiz",
            settingsDescription = "Settings",
        )

        if (isInRetryPhase) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Review Missed Items • ${scoringEngine.pendingRetryCount} remaining",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardsColors.Blue,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Drill Content Area - Animated horizontal slide-and-fade transition between questions
        val contentTransitionKey = if (isInRetryPhase) 1000 + retryStepCount else currentQuestionIndex
        AnimatedContent(
            targetState = contentTransitionKey,
            transitionSpec = {
                if (targetState >= initialState) {
                    (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } +
                            fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 } +
                                    fadeOut(animationSpec = tween(180))
                        )
                } else {
                    (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 } +
                            fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } +
                                    fadeOut(animationSpec = tween(180))
                        )
                }
            },
            label = "quiz_question_transition",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) { qIndex ->
            key(qIndex) {
                val activeDrill = drillsPerQuestion.getOrElse(qIndex) { mode }
                when (activeDrill) {
                    KanaQuizMode.Test -> {
                        val currentTarget = if (isInRetryPhase) {
                            retryCurrentTarget ?: scoringEngine.nextRetryTarget() ?: quizTargets.first()
                        } else {
                            testTargetsPerQuestion.getOrElse(qIndex) { quizTargets.random() }
                        }
                        TestQuizDrill(
                            target = currentTarget,
                            allTargets = quizTargets,
                            onResult = { isCorrect, _, target ->
                                handleTestQuestionResult(isCorrect, target)
                            },
                        )
                    }

                    KanaQuizMode.Listening -> {
                        ListeningQuizDrill(
                            words = availablePracticeWords,
                            audio = audio,
                            onResult = onQuestionCompleted,
                        )
                    }

                    KanaQuizMode.WordSelection -> {
                        WordSelectionQuizDrill(
                            words = availablePracticeWords,
                            onResult = onQuestionCompleted,
                        )
                    }

                    KanaQuizMode.MatchingPairs -> {
                        MatchingPairsQuizDrill(
                            questionIndex = qIndex,
                            targets = quizTargets,
                            onResult = onQuestionCompleted,
                        )
                    }

                    KanaQuizMode.WordBuilding -> {
                        WordBuildingQuizDrill(
                            words = availablePracticeWords,
                            onResult = onQuestionCompleted,
                        )
                    }

                    KanaQuizMode.Typing -> {
                        TypingQuizDrill(
                            targets = quizTargets,
                            onResult = onQuestionCompleted,
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    // Confirmation dialog on exit (identical to the Cards tab study exit dialog)
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Quit this session?") },
            text = { Text("Your quiz stats will not be saved.") },
            confirmButton = {
                TactileButton(
                    onClick = {
                        showExitConfirmation = false
                        onExit()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("quit_session"),
                    tone = TactileTone.Primary,
                ) {
                    Text(
                        text = "Quit session",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            },
            dismissButton = {
                CardsButton(
                    label = "Keep studying",
                    onClick = { showExitConfirmation = false },
                    modifier = Modifier.fillMaxWidth().testTag("keep_studying"),
                    background = CardsColors.Surface,
                    ink = CardsColors.Ink,
                    depth = CardsColors.Edge,
                )
            },
        )
    }
}

// ==========================================
// DRILL 1: TEST MODE (CRITICAL MASTERY ENGINE)
// ==========================================
@Composable
private fun TestQuizDrill(
    target: QuizKanaTarget,
    allTargets: List<QuizKanaTarget>,
    onResult: (isCorrect: Boolean, elapsedFraction: Float, target: QuizKanaTarget) -> Unit,
) {
    val targetChar = target
    val isRomajiPrompt = remember(targetChar) { Random.nextBoolean() }
    val options = remember(targetChar, allTargets) {
        val sameScriptTargets = allTargets.filter { it.script == targetChar.script && it.id != targetChar.id }
        val others = sameScriptTargets.shuffled().take(3)
        (others + targetChar).shuffled()
    }

    val totalTimeMs = 8000L
    var timeRemainingMs by remember { mutableLongStateOf(totalTimeMs) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Keyed on targetChar so it is never prematurely cancelled by isAnswered mutation
    LaunchedEffect(targetChar) {
        val interval = 100L
        while (timeRemainingMs > 0 && !isAnswered) {
            delay(interval)
            timeRemainingMs -= interval
        }
        if (!isAnswered) {
            isAnswered = true
            // Reveal the correct answer in green for 1200ms, then advance to next question
            delay(1200L)
            onResult(false, 1.0f, targetChar)
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
        val promptText = if (isRomajiPrompt) targetChar.romaji.uppercase() else targetChar.glyph
        val promptSub = if (isRomajiPrompt) "Romaji" else targetChar.scriptName
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
                        val isCorrect = opt.id == targetChar.id && opt.script == targetChar.script
                        val isSelected = selectedOptionId == opt.id
                        val bgColor = when {
                            isAnswered && isCorrect -> Color(0xFF43A047) // Solid Green
                            isAnswered && isSelected -> Color(0xFFE53935) // Solid Red
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
                                        if (isCorrect) {
                                            delay(450L)
                                        } else {
                                            delay(1000L)
                                        }
                                        onResult(isCorrect, elapsedFraction, targetChar)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val optionText = if (isRomajiPrompt) opt.glyph else opt.romaji.uppercase()
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
// DRILL 2: LISTENING CHALLENGE (WORDS IN STRICT KANA, 5 CHOICES)
// ==========================================
@Composable
private fun ListeningQuizDrill(
    words: List<PracticeWord>,
    audio: JapaneseTtsController?,
    onResult: (Boolean) -> Unit,
) {
    val targetWord = remember(words) { words.random() }
    val options = remember(targetWord, words) {
        val sameScriptWords = words.filter { it.script == targetWord.script && it.id != targetWord.id }
        val pool = if (sameScriptWords.size >= 4) sameScriptWords else words.filter { it.id != targetWord.id }
        val distractors = pool.shuffled().take(4)
        (distractors + targetWord).shuffled()
    }
    var answered by remember { mutableStateOf(false) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Auto-play audio each time quiz starts and with every new question
    LaunchedEffect(targetWord) {
        delay(150L)
        audio?.speak(JapaneseText(kana = targetWord.kana, romaji = targetWord.romaji, tts = targetWord.kana))
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Listen and select the word in Kana",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = CardsColors.Muted,
        )

        Spacer(Modifier.height(28.dp))

        // Normal 3D Tactile Audio Button (Anti-Bubble, Squircle Geometry, Pure White Fill)
        CardsPressable(
            onClick = {
                audio?.speak(JapaneseText(kana = targetWord.kana, romaji = targetWord.romaji, tts = targetWord.kana))
            },
            modifier = Modifier
                .width(170.dp)
                .height(52.dp),
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
                    modifier = Modifier.size(22.dp),
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

        Spacer(Modifier.weight(1f))

        // 5 Answer choices in strictly Kana (no romaji, no English meaning)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { opt ->
                val isCorrect = opt.id == targetWord.id
                val isSelected = selectedOptionId == opt.id
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
                            selectedOptionId = opt.id
                            coroutineScope.launch {
                                if (isCorrect) {
                                    delay(450L)
                                } else {
                                    delay(1000L)
                                }
                                onResult(isCorrect)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    face = faceColor,
                    depth = depthColor,
                    padding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = opt.kana,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalJapaneseFont.current,
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
// DRILL 3: WORD SELECTION (KANA QUESTION + ENGLISH UNDER IT, ROMAJI CHOICES)
// ==========================================
@Composable
private fun WordSelectionQuizDrill(
    words: List<PracticeWord>,
    onResult: (Boolean) -> Unit,
) {
    val targetWord = remember(words) { words.random() }
    val options = remember(targetWord, words) {
        val others = words.filter { it.romaji.lowercase() != targetWord.romaji.lowercase() }
            .distinctBy { it.romaji.lowercase() }
            .shuffled().take(3)
        (others + targetWord).shuffled().map { it.romaji.lowercase() }
    }
    var answered by remember { mutableStateOf(false) }
    var selectedRomaji by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Select the correct reading",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = CardsColors.Muted,
        )

        Spacer(Modifier.height(20.dp))

        // Question: Kana as primary question, strictly NO romaji above or over it
        Text(
            text = targetWord.kana,
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = LocalJapaneseFont.current,
            color = CardsColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        // English word directly under it in smaller font
        Text(
            text = targetWord.english,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = CardsColors.Muted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        // Answer choices: Romaji readings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEach { optRomaji ->
                val isCorrect = optRomaji == targetWord.romaji.lowercase()
                val isSelected = selectedRomaji == optRomaji
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
                            selectedRomaji = optRomaji
                            coroutineScope.launch {
                                if (isCorrect) {
                                    delay(450L)
                                } else {
                                    delay(1000L)
                                }
                                onResult(isCorrect)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    face = faceColor,
                    depth = depthColor,
                    padding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(
                        text = optRomaji,
                        fontSize = 18.sp,
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
// DRILL 4: MATCHING PAIRS (DYNAMIC 3-7 PAIRS, BOTH WAYS SELECTABLE, SOLID COLORS, SHAKE & POP ANIMATIONS)
// ==========================================
@Composable
private fun MatchingPairsQuizDrill(
    questionIndex: Int,
    targets: List<QuizKanaTarget>,
    onResult: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Dynamic number of selections from 3 to 7 per quiz round
    val pairCount = remember(questionIndex, targets) {
        val dynamicCount = 3 + (questionIndex % 5) // alternates 3, 4, 5, 6, 7
        dynamicCount.coerceIn(3, 7).coerceAtMost(targets.size.coerceAtLeast(3))
    }

    val pairs = remember(pairCount, targets) {
        targets.shuffled().take(pairCount).map { it.glyph to it.romaji.uppercase() }
    }
    val shuffledKanaList = remember(pairs) { pairs.map { it.first }.shuffled() }
    val shuffledRomajiList = remember(pairs) { pairs.map { it.second }.shuffled() }

    var selectedKana by remember { mutableStateOf<String?>(null) }
    var selectedRomaji by remember { mutableStateOf<String?>(null) }
    var matchedKanaSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var matchedRomajiSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var flashingWrongKana by remember { mutableStateOf<String?>(null) }
    var flashingWrongRomaji by remember { mutableStateOf<String?>(null) }
    var recentlyMatchedKana by remember { mutableStateOf<String?>(null) }
    var recentlyMatchedRomaji by remember { mutableStateOf<String?>(null) }
    var mistakesInRound by remember { mutableIntStateOf(0) }

    val shakeOffset = remember { Animatable(0f) }
    val popScale = remember { Animatable(1f) }

    val evaluatePair: (String, String) -> Unit = { kana, romaji ->
        val expectedRomaji = pairs.firstOrNull { it.first == kana }?.second
        if (expectedRomaji == romaji) {
            // Correct match
            matchedKanaSet = matchedKanaSet + kana
            matchedRomajiSet = matchedRomajiSet + romaji
            recentlyMatchedKana = kana
            recentlyMatchedRomaji = romaji
            selectedKana = null
            selectedRomaji = null

            coroutineScope.launch {
                popScale.snapTo(1f)
                popScale.animateTo(1.15f, animationSpec = tween(110))
                popScale.animateTo(1f, animationSpec = tween(110))
                recentlyMatchedKana = null
                recentlyMatchedRomaji = null
            }

            if (matchedKanaSet.size == pairs.size) {
                coroutineScope.launch {
                    delay(550L)
                    onResult(mistakesInRound == 0)
                }
            }
        } else {
            // Wrong match - vibrate and flash red with shake animation
            mistakesInRound++
            flashingWrongKana = kana
            flashingWrongRomaji = romaji
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            triggerVibration(context)

            coroutineScope.launch {
                shakeOffset.snapTo(0f)
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 350
                        -12f at 50
                        12f at 100
                        -8f at 150
                        8f at 200
                        -4f at 250
                        4f at 300
                        0f at 350
                    },
                )
                flashingWrongKana = null
                flashingWrongRomaji = null
                selectedKana = null
                selectedRomaji = null
            }
        }
    }

    val onSelectKana: (String) -> Unit = { kana ->
        if (flashingWrongKana == null && !matchedKanaSet.contains(kana)) {
            if (selectedKana == kana) {
                selectedKana = null
            } else {
                selectedKana = kana
                if (selectedRomaji != null) {
                    evaluatePair(kana, selectedRomaji!!)
                }
            }
        }
    }

    val onSelectRomaji: (String) -> Unit = { romaji ->
        if (flashingWrongRomaji == null && !matchedRomajiSet.contains(romaji)) {
            if (selectedRomaji == romaji) {
                selectedRomaji = null
            } else {
                selectedRomaji = romaji
                if (selectedKana != null) {
                    evaluatePair(selectedKana!!, romaji)
                }
            }
        }
    }

    val cardHeight = if (pairs.size <= 4) 58.dp else if (pairs.size <= 5) 52.dp else 46.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Match Kana with Romaji (${pairs.size} pairs)",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = CardsColors.Ink,
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            pairs.indices.forEach { index ->
                val kana = shuffledKanaList[index]
                val romaji = shuffledRomajiList[index]

                val isKanaMatched = matchedKanaSet.contains(kana)
                val isRomajiMatched = matchedRomajiSet.contains(romaji)
                val isKanaSelected = selectedKana == kana
                val isRomajiSelected = selectedRomaji == romaji
                val isKanaFlashingWrong = flashingWrongKana == kana
                val isRomajiFlashingWrong = flashingWrongRomaji == romaji
                val isKanaPopping = recentlyMatchedKana == kana
                val isRomajiPopping = recentlyMatchedRomaji == romaji

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Kana card
                    val kanaBg = when {
                        isKanaFlashingWrong -> Color(0xFFE53935) // Solid Red
                        isKanaMatched -> Color(0xFF2E7D32) // Solid Green
                        isKanaSelected -> CardsColors.Blue // Solid Blue
                        else -> Color(0xFFF9FAFB)
                    }
                    val kanaTextColor = if (isKanaFlashingWrong || isKanaMatched || isKanaSelected) Color.White else CardsColors.Ink

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cardHeight)
                            .graphicsLayer {
                                if (isKanaFlashingWrong) translationX = shakeOffset.value
                                if (isKanaPopping) {
                                    scaleX = popScale.value
                                    scaleY = popScale.value
                                }
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(kanaBg)
                            .border(1.dp, if (isKanaMatched) Color(0xFF1B5E20) else CardsColors.Edge, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isKanaMatched) { onSelectKana(kana) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = kana,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalJapaneseFont.current,
                            color = kanaTextColor,
                        )
                    }

                    // Romaji card
                    val romajiBg = when {
                        isRomajiFlashingWrong -> Color(0xFFE53935) // Solid Red
                        isRomajiMatched -> Color(0xFF2E7D32) // Solid Green
                        isRomajiSelected -> CardsColors.Blue // Solid Blue
                        else -> Color(0xFFF9FAFB)
                    }
                    val romajiTextColor = if (isRomajiFlashingWrong || isRomajiMatched || isRomajiSelected) Color.White else CardsColors.Ink

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cardHeight)
                            .graphicsLayer {
                                if (isRomajiFlashingWrong) translationX = shakeOffset.value
                                if (isRomajiPopping) {
                                    scaleX = popScale.value
                                    scaleY = popScale.value
                                }
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(romajiBg)
                            .border(1.dp, if (isRomajiMatched) Color(0xFF1B5E20) else CardsColors.Edge, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRomajiMatched) { onSelectRomaji(romaji) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = romaji,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = romajiTextColor,
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
@Composable
private fun WordBuildingQuizDrill(
    words: List<PracticeWord>,
    onResult: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val targetWord = remember(words) {
        val suitable = words.filter { it.kana.length in 2..5 }
        if (suitable.isNotEmpty()) {
            suitable.random()
        } else if (words.isNotEmpty()) {
            words.random()
        } else {
            PracticeWordPool.fallbackWords.filter { it.kana.length in 2..5 }.random()
        }
    }

    val word = targetWord.kana
    val meaning = targetWord.english
    val romaji = targetWord.romaji.lowercase()
    val charList = remember(word) { word.map { it.toString() } }
    val scrambledPool = remember(charList) {
        var scrambled = charList.shuffled()
        var attempts = 0
        while (scrambled == charList && attempts < 10 && charList.distinct().size > 1) {
            scrambled = charList.shuffled()
            attempts++
        }
        if (scrambled == charList && charList.size > 1 && charList.distinct().size > 1) {
            scrambled = charList.reversed()
        }
        scrambled
    }

    var assembled by remember { mutableStateOf<List<String>>(emptyList()) }
    var availablePool by remember { mutableStateOf(scrambledPool) }
    var isCompleted by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(assembled) {
        if (!isCompleted && !isFailed && assembled.size == charList.size) {
            if (assembled.joinToString("") == word) {
                isCompleted = true
                delay(500L)
                onResult(true)
            } else {
                isFailed = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                triggerVibration(context)
                shakeOffset.snapTo(0f)
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 350
                        -12f at 50
                        12f at 100
                        -8f at 150
                        8f at 200
                        -4f at 250
                        4f at 300
                        0f at 350
                    },
                )
                delay(650L)
                onResult(false)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Select Kana in correct order",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = CardsColors.Muted,
        )
        Spacer(Modifier.height(20.dp))
        // Question: Romaji as primary question in large typography
        Text(
            text = romaji,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CardsColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        // English meaning directly below
        Text(
            text = meaning,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = CardsColors.Muted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        // Slots to assemble Kana
        Row(
            modifier = Modifier.graphicsLayer { translationX = shakeOffset.value },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (i in 0 until charList.size) {
                val filled = assembled.getOrNull(i)
                val slotBg = when {
                    isCompleted -> Color(0xFFE8F5E9)
                    isFailed -> Color(0xFFFFEBEE)
                    filled != null -> CardsColors.Surface
                    else -> Color(0xFFF1F3F5)
                }
                val slotBorder = when {
                    isCompleted -> Color(0xFF43A047)
                    isFailed -> Color(0xFFE53935)
                    else -> CardsColors.Edge
                }
                val slotTextColor = when {
                    isCompleted -> Color(0xFF2E7D32)
                    isFailed -> Color(0xFFC62828)
                    else -> CardsColors.Ink
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(slotBg)
                        .border(1.dp, slotBorder, RoundedCornerShape(8.dp))
                        .clickable(enabled = filled != null && !isCompleted && !isFailed) {
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
                        color = slotTextColor,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Scrambled pool tiles (Kana answers to assemble)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            availablePool.forEachIndexed { idx, char ->
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    CardsPressable(
                        onClick = {
                            if (!isCompleted && !isFailed) {
                                availablePool = availablePool.toMutableList().also { it.removeAt(idx) }
                                assembled = assembled + char
                            }
                        },
                        enabled = !isCompleted && !isFailed,
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalJapaneseFont.current,
                            color = CardsColors.Ink,
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// DRILL 6: TYPING QUIZ (EXACT UNDERSCORES & AUTO-CHECK WITH PER-LETTER RED/GREEN)
// ==========================================
@Composable
private fun TypingQuizDrill(
    targets: List<QuizKanaTarget>,
    onResult: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val target = remember(targets) { targets.random() }
    val expectedRomaji = remember(target) { target.romaji.lowercase() }
    val requiredLength = expectedRomaji.length

    var typedInput by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var evaluatedErrors by remember { mutableStateOf<List<Boolean>?>(null) } // true = correct, false = wrong

    // Auto-check immediately when all gaps/underscores are filled
    LaunchedEffect(typedInput) {
        if (!isChecking && typedInput.length == requiredLength) {
            isChecking = true
            val errors = (0 until requiredLength).map { i ->
                typedInput[i].lowercaseChar() == expectedRomaji[i].lowercaseChar()
            }
            evaluatedErrors = errors
            val allCorrect = errors.all { it }

            if (allCorrect) {
                coroutineScope.launch {
                    delay(500L)
                    onResult(true)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                triggerVibration(context)
                coroutineScope.launch {
                    delay(1200L)
                    onResult(false)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = target.glyph,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = LocalJapaneseFont.current,
            color = CardsColors.Ink,
        )
        Text(
            text = target.scriptName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = CardsColors.Muted,
        )

        Spacer(Modifier.height(28.dp))

        // Underscores row matching exact length of Romaji reading
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until requiredLength) {
                val charTyped = typedInput.getOrNull(i)
                val isEvaluated = evaluatedErrors != null
                val isCharCorrect = evaluatedErrors?.getOrNull(i)

                val boxBorderColor = when {
                    isEvaluated && isCharCorrect == true -> Color(0xFF43A047)
                    isEvaluated && isCharCorrect == false -> Color(0xFFE53935)
                    charTyped != null -> CardsColors.Blue
                    else -> CardsColors.Edge
                }
                val boxBgColor = when {
                    isEvaluated && isCharCorrect == true -> Color(0xFFE8F5E9)
                    isEvaluated && isCharCorrect == false -> Color(0xFFFFEBEE)
                    else -> CardsColors.Surface
                }
                val textColor = when {
                    isEvaluated && isCharCorrect == true -> Color(0xFF2E7D32)
                    isEvaluated && isCharCorrect == false -> Color(0xFFC62828)
                    charTyped != null -> CardsColors.Ink
                    else -> CardsColors.Muted
                }

                Box(
                    modifier = Modifier
                        .size(48.dp, 54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(boxBgColor)
                        .border(2.dp, boxBorderColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = charTyped?.toString()?.lowercase() ?: "_",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }
            }
        }

        // Educational feedback if user makes an error
        if (evaluatedErrors != null && evaluatedErrors?.any { !it } == true) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Correct reading: $expectedRomaji",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32),
            )
        }

        Spacer(Modifier.weight(1f))

        // On-Screen QWERTY Virtual Keyboard
        CustomQwertyKeyboard(
            onKeyPress = { key ->
                if (!isChecking && typedInput.length < requiredLength) {
                    typedInput += key.lowercase()
                }
            },
            onBackspace = {
                if (!isChecking && typedInput.isNotEmpty()) {
                    typedInput = typedInput.dropLast(1)
                }
            },
            onDone = {
                // Auto-check triggers automatically, but user can submit if complete
                if (!isChecking && typedInput.length == requiredLength) {
                    // Handled reactively by LaunchedEffect
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

// ==========================================
// SESSION STATS COMPLETION SCREEN (ANTI-BUBBLE ARCHITECTURE)
// ==========================================
@Composable
private fun QuizFinishedScreen(
    totalQuestions: Int,
    correctCount: Int,
    bestStreak: Int,
    durationSeconds: Long,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val accuracyPct = ((correctCount * 100f) / totalQuestions.coerceAtLeast(1)).toInt()
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

    val title = when {
        correctCount == totalQuestions -> "Flawless Mastery!"
        accuracyPct >= 80 -> "Excellent Work!"
        accuracyPct >= 60 -> "Solid Progress!"
        else -> "Good Practice!"
    }

    val subtitle = when {
        correctCount == totalQuestions -> "Perfect score across all 20 questions! Outstanding Kana fluency."
        accuracyPct >= 80 -> "High accuracy across 20 questions. Keep the momentum going!"
        accuracyPct >= 60 -> "Good drill. Regular practice sharpens your recognition speed."
        else -> "Every practice builds muscle memory. Review missed items and try again!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = null,
            tint = if (accuracyPct >= 80) CardsColors.Green else CardsColors.Blue,
            modifier = Modifier.size(68.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            style = KotoType.Brand,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = CardsColors.Ink,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = CardsColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(36.dp))

        // 4-Column Anti-Bubble Stats Row with crisp 1dp hairline vertical dividers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val statsList = listOf(
                Triple("Score", "$correctCount/$totalQuestions", CardsColors.Blue),
                Triple("Accuracy", "$accuracyPct%", CardsColors.Green),
                Triple("Streak", "$bestStreak", Color(0xFFF57F17)),
                Triple("Time", formattedDuration, CardsColors.Ink),
            )

            statsList.forEachIndexed { index, (label, value, color) ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(CardsColors.Edge),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = value,
                        color = color,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = label,
                        color = CardsColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        // Action Buttons: Practice Again (Primary) & Done (Secondary White)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardsPressable(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().testTag("btn_quiz_restart"),
                face = CardsColors.Blue,
                depth = CardsColors.BlueDepth,
                padding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    text = "Practice Again",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            CardsButton(
                label = "Return to Dashboard",
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().testTag("btn_quiz_return_dashboard"),
                background = CardsColors.Surface,
                ink = CardsColors.Ink,
                depth = CardsColors.Edge,
            )
        }
    }
}
