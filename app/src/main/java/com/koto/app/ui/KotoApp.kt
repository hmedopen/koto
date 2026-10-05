package com.koto.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import com.koto.app.feature.lesson.LessonScreen
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.data.LessonProgress
import com.koto.app.feature.lesson.data.FoundationLessons
import com.koto.app.feature.translator.ui.TranslatorScreen
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
    var cardsStudying by rememberSaveable { mutableStateOf(false) }
    var cardsDeckOpen by rememberSaveable { mutableStateOf(false) }
    var cardsCreateDeckOpen by rememberSaveable { mutableStateOf(false) }
    var cardsContentOpen by rememberSaveable { mutableStateOf(false) }
    var randomDeckTrigger by rememberSaveable { mutableStateOf(false) }
    var starredWordsTrigger by rememberSaveable { mutableStateOf(false) }
    var createDeckTrigger by rememberSaveable { mutableStateOf(false) }
    var translatorOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val progress = remember { LessonProgress(context) }
    val audio = remember { JapaneseTtsController.get(context) }
    val shellState = rememberSaveableStateHolder()

    // Top-level section changes don't accumulate a history of tab taps.
    BackHandler(enabled = lessonId == null && !translatorOpen && selected != KotoDestination.Learn) {
        selected = KotoDestination.Learn
    }

    val lesson = remember(lessonId) { lessonId?.let(FoundationLessons::lesson) }
    if (lesson != null) {
        // Old placeholder attempts must not restore selections/results into new questions.
        key("foundation_v1", lesson.id) {
            LessonScreen(lesson, audio, progress::complete, onExit = { lessonId = null; selected = KotoDestination.Map })
        }
    } else {
        AnimatedContent(
            targetState = translatorOpen,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } +
                        fadeIn(tween(250, easing = LinearOutSlowInEasing))).togetherWith(
                        slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } +
                            fadeOut(tween(200, easing = FastOutLinearInEasing)),
                    )
                } else {
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } +
                        fadeIn(tween(250, easing = LinearOutSlowInEasing))).togetherWith(
                        slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } +
                            fadeOut(tween(200, easing = FastOutLinearInEasing)),
                    )
                }
            },
            label = "Translator dedicated screen transition",
        ) { isTranslator ->
            if (isTranslator) {
                TranslatorScreen(onDismiss = { translatorOpen = false }, onSettings = { settingsOpen = true })
            } else {
                shellState.SaveableStateProvider("main_shell") {
                    Column(Modifier.fillMaxSize().background(KotoColors.Background)) {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                            ),
                        ) {
                            if ((selected != KotoDestination.Cards || !cardsStudying) && !cardsCreateDeckOpen) {
                                val topBarTitle = if (selected == KotoDestination.Cards && cardsContentOpen) {
                                    "Deck Content"
                                } else {
                                    selected.title
                                }
                                KotoTopBar(
                                    title = topBarTitle,
                                    onSettings = { settingsOpen = true },
                                    onBack = if (selected == KotoDestination.Cards && cardsDeckOpen) {
                                        { backDispatcher?.onBackPressed() }
                                    } else null,
                                    onRandomDeck = if (selected == KotoDestination.Cards && !cardsDeckOpen) {
                                        { randomDeckTrigger = true }
                                    } else null,
                                    onStarredWords = if (selected == KotoDestination.Cards && !cardsDeckOpen) {
                                        { starredWordsTrigger = true }
                                    } else null,
                                    onCreateDeck = if (selected == KotoDestination.Cards && !cardsDeckOpen) {
                                        { createDeckTrigger = true }
                                    } else null,
                                )
                            }
                            KotoNavigation(
                                selected = selected,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                completed = progress.completed,
                                onPlay = { lessonId = it },
                                onCardsStudyModeChanged = { cardsStudying = it },
                                onCardsDeckOpenChanged = { cardsDeckOpen = it },
                                onCardsCreateDeckModeChanged = { cardsCreateDeckOpen = it },
                                onCardsContentOpenChanged = { cardsContentOpen = it },
                                randomDeckTrigger = randomDeckTrigger,
                                onRandomDeckHandled = { randomDeckTrigger = false },
                                starredWordsTrigger = starredWordsTrigger,
                                onStarredWordsHandled = { starredWordsTrigger = false },
                                createDeckTrigger = createDeckTrigger,
                                onCreateDeckHandled = { createDeckTrigger = false },
                                onOpenTranslator = { translatorOpen = true },
                            )
                        }
                        if (!cardsStudying && !cardsCreateDeckOpen && !(selected == KotoDestination.Cards && cardsDeckOpen)) {
                            KotoBottomBar(selected = selected, onSelect = { selected = it })
                        }
                    }
                }
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

