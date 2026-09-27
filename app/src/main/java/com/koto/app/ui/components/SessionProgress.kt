package com.koto.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.koto.app.ui.theme.KotoColors
import kotlin.math.PI
import kotlin.math.sin

/** Shared, label-free rail for Cards and Map lessons. */
@Composable
internal fun SessionProgress(progress: Float, modifier: Modifier = Modifier) {
    val target = progress.coerceIn(0f, 1f)
    val fill = animateFloatAsState(target,
        spring(dampingRatio = .9f, stiffness = 260f), label = "Session progress")
    val highlight = remember { Animatable(1f) }
    var previous by remember { mutableFloatStateOf(target) }
    LaunchedEffect(target) {
        val advanced = target > previous
        previous = target
        if (advanced) {
            highlight.snapTo(0f)
            highlight.animateTo(1f, tween(650))
        }
    }
    val face = if (target == 1f) KotoColors.CorrectButtonFace else KotoColors.LessonBlue
    val depth = if (target == 1f) KotoColors.CorrectButtonDepth else Color(0xFF134688)
    Box(modifier.semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
    }.drawWithCache {
        val railHeight = 10.dp.toPx()
        val radius = CornerRadius(railHeight / 2f)
        val recess = 2.dp.toPx()
        val markerRadius = 1.4.dp.toPx()
        onDrawBehind {
            drawRoundRect(Color(0xFFCFDEEC), size = Size(size.width, railHeight + recess),
                cornerRadius = radius)
            drawRoundRect(Color(0xFFE2E8F0), topLeft = Offset(0f, recess),
                size = Size(size.width, railHeight), cornerRadius = radius)
            val width = size.width * fill.value.coerceIn(0f, 1f)
            if (width > 0f) {
                drawRoundRect(depth, topLeft = Offset(0f, recess),
                    size = Size(width, railHeight), cornerRadius = radius)
                drawRoundRect(face, size = Size(width, railHeight), cornerRadius = radius)
                if (width > 8.dp.toPx()) {
                    drawLine(Color.White.copy(alpha = .24f),
                        Offset(4.dp.toPx(), 2.5.dp.toPx()),
                        Offset(width - 4.dp.toPx(), 2.5.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                }
                // One finite highlight after advancing; no idle animation.
                val glow = sin(highlight.value * PI).toFloat().coerceAtLeast(0f)
                val glintX = width * highlight.value
                val glintLength = minOf(12.dp.toPx(), width / 3f)
                drawLine(Color.White.copy(alpha = glow * .65f),
                    Offset((glintX - glintLength).coerceAtLeast(0f), railHeight / 2f),
                    Offset(glintX.coerceAtMost(width), railHeight / 2f),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Butt)
            }
            for (quarter in 1..3) {
                val x = size.width * quarter / 4f
                drawCircle(if (x <= width) Color.White.copy(alpha = .7f)
                    else KotoColors.QuietInk.copy(alpha = .32f), markerRadius,
                    Offset(x, (railHeight + recess) / 2f))
            }
        }
    })
}
