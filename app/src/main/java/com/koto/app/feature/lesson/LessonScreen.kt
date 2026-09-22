package com.koto.app.feature.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.koto.app.R
import com.koto.app.feature.lesson.audio.*
import com.koto.app.feature.lesson.model.*
import com.koto.app.feature.lesson.ui.*
import com.koto.app.ui.components.*
import com.koto.app.ui.screens.map.SettingsSheet
import com.koto.app.ui.theme.KotoColors
import kotlinx.coroutines.delay

@Composable
fun LessonScreen(lesson: LessonDefinition, audio: JapaneseTtsController, onComplete: (Int) -> Unit, onExit: () -> Unit) {
    val session = rememberSaveable(lesson.id, saver = LessonSession.saver(lesson)) { LessonSession(lesson) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var exitRequested by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_STOP) audio.stop()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); audio.stop() }
    }
    val exit: () -> Unit = { audio.stop(); onExit() }
    val requestExit: () -> Unit = { if (session.finished) exit() else { audio.stop(); exitRequested = true } }
    BackHandler { requestExit() }
    LaunchedEffect(audio.status) {
        if (audio.takeUnavailableNotice()) {
            snackbar.showSnackbar("Japanese voice unavailable. You can keep playing.")
        }
    }
    LaunchedEffect(session.finished) { if (session.finished) onComplete(lesson.id) }
    LaunchedEffect(session.state.mismatch, session.state.index, settings, exitRequested, resumed) {
        if (session.state.mismatch && !settings && !exitRequested && resumed) { delay(200); session.clearMismatch() }
    }
    LaunchedEffect(session.checked, session.state.index, settings, exitRequested, resumed) {
        if (session.question is Question.PairMatch && session.checked && !settings && !exitRequested && resumed) {
            delay(450); session.next()
        }
    }
    val progress by animateFloatAsState(session.progress, tween(220), label = "Lesson progress")
    Box(Modifier.fillMaxSize().background(KotoColors.Background).windowInsetsPadding(WindowInsets.safeDrawing).testTag("lesson_screen")) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().align(Alignment.TopCenter).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TactileButton(requestExit, Modifier.size(48.dp, 52.dp).testTag("lesson_close"), tone = TactileTone.Quiet,
                    description = "Close lesson", padding = PaddingValues(12.dp)) {
                    Icon(painterResource(R.drawable.ic_close), null, Modifier.size(24.dp))
                }
                LinearProgressIndicator(progress = { progress }, Modifier.weight(1f).height(7.dp).testTag("lesson_progress"),
                    color = KotoColors.LessonBlue, trackColor = KotoColors.Hairline, gapSize = 0.dp, drawStopIndicator = {})
                TactileButton({ audio.stop(); settings = true }, Modifier.size(48.dp, 52.dp).testTag("lesson_settings"),
                    tone = TactileTone.Quiet, description = "Lesson settings", padding = PaddingValues(12.dp)) {
                    Icon(painterResource(R.drawable.ic_settings), null, Modifier.size(24.dp))
                }
            }
            if (session.finished) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Level ${lesson.id.toString().padStart(2, '0')} complete", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("lesson_complete").semantics { heading() })
                    Spacer(Modifier.height(12.dp))
                    Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(28.dp))
                    Text("${session.state.results.count { it }} / ${lesson.questions.size}", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text("correct on the first try", color = KotoColors.QuietInk)
                    Spacer(Modifier.height(36.dp))
                    ActionButton("Replay Level", "lesson_replay") { session.replay() }
                    Spacer(Modifier.height(12.dp))
                    ActionButton("Back to Map", "lesson_map", tone = TactileTone.Default, onClick = exit)
                }
            } else {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val available = maxHeight
                    key(session.state.index) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("question_scroll")) {
                            QuestionRenderer(session, available, audio.enabled && audio.status == SpeechStatus.Ready, audio::speak)
                        }
                    }
                }
                val manual = session.question is Question.SentenceBuilder || session.question is Question.Cloze
                if (session.question !is Question.PairMatch && (manual || session.checked)) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)) {
                        if (session.checked) Feedback(session)
                        ActionButton(if (session.checked) "CONTINUE" else "CHECK", "lesson_action",
                            enabled = session.checked || session.canCheck) {
                            if (session.checked) { audio.stop(); session.next() } else session.check()?.let(audio::speak)
                        }
                    }
                } else Spacer(Modifier.height(12.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (settings) SettingsSheet(onDismiss = { settings = false }, audio = audio)
    if (exitRequested) AlertDialog(onDismissRequest = { exitRequested = false },
        title = { Text("Leave this lesson?") },
        text = { Text("This attempt will be discarded. Completed levels stay saved.") },
        confirmButton = { ActionButton("Leave", "confirm_exit", onClick = exit) },
        dismissButton = { ActionButton("Keep playing", "cancel_exit", tone = TactileTone.Default) { exitRequested = false } })
}

@Composable
internal fun ActionButton(text: String, tag: String, enabled: Boolean = true,
    tone: TactileTone = TactileTone.Primary, onClick: () -> Unit) {
    TactileButton(onClick, Modifier.fillMaxWidth().testTag(tag), enabled,
        if (enabled) tone else TactileTone.Default) {
        Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun Feedback(session: LessonSession) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(if (session.correct == true) "Correct!" else "Not quite. Correct answer:", fontWeight = FontWeight.Bold,
            color = if (session.correct == true) KotoColors.Correct else KotoColors.Wrong)
        if (session.correct == false) {
            val answer = when (val q = session.question) {
                is Question.MeaningChoice -> q.options.first { it.id == q.correctId }.text
                is Question.ConversationResponse -> LessonText.Japanese(q.responses.first { it.id == q.correctId }.let { (it.text as LessonText.Japanese).value })
                is Question.Cloze -> LessonText.Japanese(q.filled(q.correctId))
                is Question.SentenceBuilder -> LessonText.Japanese(q.sentence)
                else -> null
            }
            answer?.let { ContentText(it, 16.sp) }
        }
    }
}
