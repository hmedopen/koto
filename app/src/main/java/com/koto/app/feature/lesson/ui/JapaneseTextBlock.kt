package com.koto.app.feature.lesson.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.lesson.model.*
import com.koto.app.ui.components.*
import com.koto.app.ui.theme.KotoColors

@Composable
fun JapaneseTextBlock(text: JapaneseText, modifier: Modifier = Modifier, size: TextUnit = 22.sp,
    alignReading: Boolean = true) {
    val ink = LocalContentColor.current
    if (!alignReading) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text.romaji, fontSize = size * .7f, lineHeight = size * .9f,
                color = if (ink == androidx.compose.ui.graphics.Color.White) androidx.compose.ui.graphics.Color(0xFFE3E8EE) else KotoColors.QuietInk,
                textAlign = TextAlign.Center, fontWeight = FontWeight.Normal)
            Text(text.kana, fontSize = size, lineHeight = size * 1.35f,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        return
    }
    val units = remember(text.kana, text.romaji) { kanaReadingUnits(text) }
    val groups = remember(units) {
        buildList {
            var current = mutableListOf<KanaReadingUnit>()
            units.forEach { unit ->
                if (unit.breakBefore && current.isNotEmpty()) {
                    add(current)
                    current = mutableListOf()
                }
                current += unit
            }
            if (current.isNotEmpty()) add(current)
        }
    }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        groups.forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                group.forEach { unit ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(unit.romaji.ifEmpty { " " }, fontSize = size * .7f, lineHeight = size * .7f,
                            color = if (ink == androidx.compose.ui.graphics.Color.White) androidx.compose.ui.graphics.Color(0xFFE3E8EE) else KotoColors.QuietInk,
                            textAlign = TextAlign.Center, fontWeight = FontWeight.Normal, maxLines = 1)
                        Text(unit.kana, fontSize = size, lineHeight = size * 1.1f,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ContentText(text: LessonText, size: TextUnit = 22.sp, alignReading: Boolean = true) {
    when (text) {
        is LessonText.Japanese -> JapaneseTextBlock(text.value, size = size, alignReading = alignReading)
        is LessonText.English -> Text(text.value, fontSize = size, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
    }
}

@Composable
internal fun SpeakerButton(text: JapaneseText, speechReady: Boolean, isPlaying: Boolean,
    speak: (JapaneseText) -> Unit, modifier: Modifier = Modifier) {
    TactileButton({ speak(text) }, modifier.size(48.dp, 52.dp), enabled = speechReady,
        tone = TactileTone.Quiet, description = "Play Japanese: ${text.romaji}",
        stateLabel = if (isPlaying) "Playing" else null, padding = PaddingValues(8.dp)) {
        PlaybackSpeakerIcon(isPlaying)
    }
}

@Composable
private fun PlaybackSpeakerIcon(isPlaying: Boolean) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            phase.snapTo(0f)
        } else {
            while (true) {
                phase.snapTo(0f)
                phase.animateTo(1f, tween(900, easing = LinearEasing))
            }
        }
    }
    Box(Modifier.size(30.dp, 24.dp), contentAlignment = Alignment.CenterStart) {
        Icon(painterResource(R.drawable.ic_speaker), null, modifier = Modifier.size(22.dp), tint = KotoColors.LessonBlue)
        Canvas(Modifier.matchParentSize().testTag("audio_playback_waves")) {
            if (isPlaying) repeat(3) { index ->
                val progress = (phase.value + index / 3f) % 1f
                val radius = 7.dp.toPx() + progress * 9.dp.toPx()
                drawArc(
                    color = KotoColors.LessonBlue.copy(alpha = (1f - progress) * .55f),
                    startAngle = -42f,
                    sweepAngle = 84f,
                    useCenter = false,
                    topLeft = Offset(4.dp.toPx() - radius, size.height / 2f - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }
        }
    }
}
