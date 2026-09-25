package com.koto.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.koto.app.ui.screens.cards.CardsScreen
import com.koto.app.ui.screens.learn.LearnScreen
import com.koto.app.ui.screens.map.MapScreen
import com.koto.app.ui.theme.KotoDimens
import com.koto.app.ui.theme.KotoMotion

@Composable
fun KotoNavigation(selected: KotoDestination, modifier: Modifier = Modifier, completed: Set<Int> = emptySet(),
    onPlay: (Int) -> Unit = {}, onCardsStudyModeChanged: (Boolean) -> Unit = {},
    onCardsDeckOpenChanged: (Boolean) -> Unit = {}) {
    val stateHolder = rememberSaveableStateHolder()
    val distance = with(LocalDensity.current) { KotoDimens.ContentDisplacement.roundToPx() }
    val layoutSign = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    // Target changes interrupt this transition; taps never wait on a coroutine or queue.
    // Compose's animation clock honors Android's animator duration scale (including 0).
    AnimatedContent(
        targetState = selected,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) layoutSign else -layoutSign
            (fadeIn(tween(KotoMotion.ContentDuration)) +
                slideInHorizontally(tween(KotoMotion.ContentDuration)) { direction * distance }) togetherWith
                (fadeOut(tween(KotoMotion.ContentExitDuration)) +
                    slideOutHorizontally(tween(KotoMotion.ContentExitDuration)) { -direction * distance })
        },
        label = "Primary destination",
    ) { destination ->
        stateHolder.SaveableStateProvider(destination.name) {
            when (destination) {
                KotoDestination.Map -> MapScreen(completed, onPlay)
                KotoDestination.Learn -> LearnScreen()
                KotoDestination.Cards -> CardsScreen(
                    onStudyModeChanged = onCardsStudyModeChanged,
                    onDeckOpenChanged = onCardsDeckOpenChanged,
                )
            }
        }
    }
}

