package com.koto.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

object KotoMotion {
    const val PressDuration = 90
    const val ContentDuration = 180
    const val ContentExitDuration = 100
    const val IconActivationDuration = 420
    const val IconDeactivationDuration = 210
    const val PressScale = 0.94f

    fun <T> press() = tween<T>(PressDuration, easing = FastOutSlowInEasing)

    fun iconActivation(selected: Boolean): FiniteAnimationSpec<Float> = tween(
        durationMillis = if (selected) IconActivationDuration else IconDeactivationDuration,
        easing = FastOutSlowInEasing,
    )
}
