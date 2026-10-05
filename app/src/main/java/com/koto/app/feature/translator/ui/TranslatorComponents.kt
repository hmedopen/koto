package com.koto.app.feature.translator.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koto.app.R
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import kotlin.math.PI
import kotlin.math.cos

/**
 * Animated speaker glyph matching the cards/lesson audio playback with pulsing sound waves.
 */
@Composable
fun TranslatorSpeakerWavesIcon(
    isPlaying: Boolean,
    tint: Color = CardsColors.Ink,
    modifier: Modifier = Modifier,
) {
    val phase = if (isPlaying) {
        rememberInfiniteTransition(label = "SpeakerPlayback").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing)),
            label = "SpeakerWavePulse",
        )
    } else rememberUpdatedState(0f)

    Box(
        modifier = modifier
            .testTag("audio_playback_waves")
            .drawWithCache {
                val unit = size.width / 24f
                val speaker = Path().apply {
                    moveTo(3f * unit, 9f * unit)
                    lineTo(7f * unit, 9f * unit)
                    lineTo(11f * unit, 5f * unit)
                    lineTo(11f * unit, 19f * unit)
                    lineTo(7f * unit, 15f * unit)
                    lineTo(3f * unit, 15f * unit)
                    close()
                }
                val stroke = Stroke(width = 1.8f * unit, cap = StrokeCap.Round)
                onDrawBehind {
                    drawPath(speaker, tint)
                    repeat(2) { index ->
                        val alpha = if (isPlaying) {
                            val pulse = (1f + cos((phase.value - index * 0.22f) * 2f * PI).toFloat()) / 2f
                            0.18f + 0.82f * pulse
                        } else 1f
                        val radius = (6f + index * 4f) * unit
                        drawArc(
                            color = tint.copy(alpha = tint.alpha * alpha),
                            startAngle = -46f,
                            sweepAngle = 92f,
                            useCenter = false,
                            topLeft = Offset(10f * unit - radius, 12f * unit - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = stroke,
                        )
                    }
                }
            },
    )
}

/**
 * Tactile, on-theme physical button for TTS playback with wave animation.
 */
@Composable
fun TranslatorAudioButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = CardsColors.Ink,
    tag: String = "translator_audio_button",
    description: String = "Play audio",
) {
    CardsPressable(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .testTag(tag),
        face = CardsColors.Surface,
        depth = CardsColors.Edge,
        padding = PaddingValues(8.dp),
    ) {
        TranslatorSpeakerWavesIcon(
            isPlaying = isPlaying,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * Tactile, on-theme physical button for copying text to clipboard.
 */
@Composable
fun TranslatorCopyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = CardsColors.Ink,
    tag: String = "translator_copy_button",
    description: String = "Copy text",
) {
    CardsPressable(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .testTag(tag),
        face = CardsColors.Surface,
        depth = CardsColors.Edge,
        padding = PaddingValues(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_copy),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

/**
 * Tactile, on-theme physical button for starring / bookmarking a translation.
 */
@Composable
fun TranslatorStarButton(
    onClick: () -> Unit,
    isStarred: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tag: String = "translator_star_button",
    description: String = if (isStarred) "Starred" else "Star translation",
) {
    CardsPressable(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .testTag(tag),
        face = CardsColors.Surface,
        depth = CardsColors.Edge,
        padding = PaddingValues(8.dp),
    ) {
        Icon(
            painter = painterResource(if (isStarred) R.drawable.ic_review_star else R.drawable.ic_star_outline),
            contentDescription = description,
            tint = if (isStarred) CardsColors.Yellow else CardsColors.Ink,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
