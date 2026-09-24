package com.koto.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import com.koto.app.feature.lesson.LessonScreen
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.data.LessonProgress
import com.koto.app.feature.lesson.data.FoundationLessons
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.koto.app.ui.components.KotoBottomBar
import com.koto.app.ui.components.KotoTopBar
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.navigation.KotoNavigation
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoTheme
import com.koto.app.ui.screens.map.SettingsSheet

@Composable
fun KotoApp() {
    // Saved instance state preserves the current task through rotation/recreation.
    // A new task starts on Learn; only completion and audio preferences persist to disk.
    var selected by rememberSaveable { mutableStateOf(KotoDestination.Learn) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var lessonId by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val progress = remember { LessonProgress(context) }
    val audio = remember { JapaneseTtsController.get(context) }
    val shellState = rememberSaveableStateHolder()

    // Top-level section changes don't accumulate a history of tab taps.
    BackHandler(enabled = lessonId == null && selected != KotoDestination.Learn) {
        selected = KotoDestination.Learn
    }

    val lesson = remember(lessonId) { lessonId?.let(FoundationLessons::lesson) }
    if (lesson != null) {
        // Old placeholder attempts must not restore selections/results into new questions.
        key("foundation_v1", lesson.id) {
            LessonScreen(lesson, audio, progress::complete, onExit = { lessonId = null; selected = KotoDestination.Map })
        }
    } else {
        shellState.SaveableStateProvider("main_shell") {
            Column(Modifier.fillMaxSize().background(KotoColors.Background)) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    ),
                ) {
                    KotoTopBar(title = selected.title, onSettings = { settingsOpen = true })
                    KotoNavigation(selected, Modifier.weight(1f).fillMaxWidth(), progress.completed,
                        onPlay = { lessonId = it })
                }
                KotoBottomBar(selected = selected, onSelect = { selected = it })
            }
        }
    }
    if (settingsOpen) SettingsSheet(onDismiss = { settingsOpen = false }, audio = audio)
}

private val KotoDestination.title: String
    get() = when (this) {
        KotoDestination.Map -> "Map"
        KotoDestination.Learn -> "Learn"
        KotoDestination.Cards -> "Cards"
    }

@Preview(name = "Koto — phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Koto — large text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Preview(name = "Koto — landscape", widthDp = 800, heightDp = 360)
@Composable
private fun KotoPreview() {
    KotoTheme { KotoApp() }
}

