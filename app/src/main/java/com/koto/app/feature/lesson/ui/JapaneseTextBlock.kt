package com.koto.app.feature.lesson.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.feature.lesson.model.*
import com.koto.app.ui.components.*
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.settings.DisplayMode
import com.koto.app.ui.screens.settings.LocalJapaneseDisplayMode
import com.koto.app.ui.screens.settings.LocalRomajiVisibility
import kotlin.math.PI
import kotlin.math.cos

@Composable
fun JapaneseTextBlock(
    text: JapaneseText,
    modifier: Modifier = Modifier,
    size: TextUnit = 22.sp,
    alignReading: Boolean = true,
    mode: DisplayMode = LocalJapaneseDisplayMode.current,
    showRomaji: Boolean = LocalRomajiVisibility.current,
) {
    val ink = LocalContentColor.current
    val isWhite = ink == androidx.compose.ui.graphics.Color.White
    val fontColor = if (isWhite) androidx.compose.ui.graphics.Color.White else CardsColors.Ink
    val furiganaColor = if (isWhite) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f) else CardsColors.Blue
    val romajiColor = if (isWhite) androidx.compose.ui.graphics.Color(0xFFE3E8EE) else KotoColors.QuietInk

    JapaneseWordDisplay(
        kanji = text.displayKanji,
        kana = text.kana,
        romaji = text.romaji,
        modifier = modifier,
        mode = mode,
        showRomaji = showRomaji,
        fontSize = size,
        fontColor = fontColor,
        furiganaColor = furiganaColor,
        romajiColor = romajiColor,
        horizontalAlignment = Alignment.CenterHorizontally,
    )
}

@Composable
internal fun ContentText(
    text: LessonText,
    size: TextUnit = 22.sp,
    alignReading: Boolean = true,
    mode: DisplayMode = LocalJapaneseDisplayMode.current,
    showRomaji: Boolean = LocalRomajiVisibility.current,
) {
    when (text) {
        is LessonText.Japanese -> JapaneseTextBlock(
            text = text.value,
            size = size,
            alignReading = alignReading,
            mode = mode,
            showRomaji = showRomaji,
        )
        is LessonText.English -> Text(
            text = text.value,
            fontSize = size,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun SpeakerButton(text: JapaneseText, speechReady: Boolean, isPlaying: Boolean,
    speak: (JapaneseText) -> Unit, modifier: Modifier = Modifier,
    description: String = "Play Japanese: ${text.romaji}") {
    TactileButton({ speak(text) }, modifier.size(48.dp, 52.dp), enabled = speechReady,
        tone = TactileTone.Quiet, description = description,
        stateLabel = if (isPlaying) "Playing" else null, padding = PaddingValues(8.dp)) {
        PlaybackSpeakerIcon(isPlaying && speechReady, speechReady)
    }
}

@Composable
private fun PlaybackSpeakerIcon(isPlaying: Boolean, enabled: Boolean) {
    // Only run a clock during playback. InfiniteTransition also respects the
    // system animation scale without a zero-duration coroutine loop.
    val phase = if (isPlaying) {
        rememberInfiniteTransition(label = "Speaker playback").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing)),
            label = "Speaker wave pulse",
        )
    } else rememberUpdatedState(0f)
    val ink = if (enabled) KotoColors.LessonBlue else KotoColors.QuietInk.copy(alpha = .45f)
    Box(Modifier.size(26.dp).testTag("audio_playback_waves").drawWithCache {
        // One 24-unit glyph: the arcs share an origin and never move across the
        // speaker body. Cache geometry; read animation state only while drawing.
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
            drawPath(speaker, ink)
            repeat(2) { index ->
                // The outer arc follows the inner arc; pulse the actual waves,
                // never a second overlay. Keep a faint outline at the trough.
                val alpha = if (isPlaying) {
                    val pulse = (1f + cos((phase.value - index * .22f) * 2f * PI).toFloat()) / 2f
                    .18f + .82f * pulse
                } else 1f
                val radius = (6f + index * 4f) * unit
                drawArc(
                    color = ink.copy(alpha = ink.alpha * alpha),
                    startAngle = -46f,
                    sweepAngle = 92f,
                    useCenter = false,
                    topLeft = Offset(10f * unit - radius, 12f * unit - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = stroke,
                )
            }
        }
    })
}
