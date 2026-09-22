package com.koto.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.theme.KotoColors

/** Stable 24-unit illustrations: inactive icons are symbols; active icons reveal detail. */
@Composable
internal fun KotoNavIcon(
    destination: KotoDestination,
    color: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val active = progress.coerceIn(0f, 1f)
        val stroke = Stroke(1.65f + 0.25f * active, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            when (destination) {
                KotoDestination.Map -> drawMap(color, active, stroke)
                KotoDestination.Learn -> drawLearn(color, active, stroke)
                KotoDestination.Cards -> drawCards(color, active, stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMap(navy: Color, active: Float, stroke: Stroke) {
    val outline = Path().apply {
        moveTo(3f, 5f); lineTo(9f, 3f); lineTo(15f, 5f); lineTo(21f, 3f)
        lineTo(21f, 19f); lineTo(15f, 21f); lineTo(9f, 19f); lineTo(3f, 21f); close()
    }
    drawPath(outline, navy, style = stroke)
    drawLine(navy, Offset(9f, 3f), Offset(9f, 19f), stroke.width, StrokeCap.Round)
    drawLine(navy, Offset(15f, 5f), Offset(15f, 21f), stroke.width, StrokeCap.Round)
    if (active > 0f) {
        val route = Path().apply {
            moveTo(5f, 16f); cubicTo(8f, 14f, 9f, 10f, 13f, 11f); cubicTo(15f, 12f, 16f, 9f, 19f, 8f)
        }
        drawPath(route, KotoColors.QuietInk, style = stroke, alpha = active)
        drawPath(outline, KotoColors.SoftGrey.copy(alpha = 0.5f * active), style = Stroke(2.2f), alpha = active)
        val markerY = 7.5f + (1f - active) * 2.8f
        drawCircle(KotoColors.Gold, radius = 2.1f * active, center = Offset(19f, markerY))
        drawCircle(Color.White.copy(alpha = active), radius = 0.65f * active, center = Offset(19f, markerY))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLearn(navy: Color, active: Float, stroke: Stroke) {
    val book = Path().apply {
        moveTo(12f, 6f); cubicTo(9f, 3.8f, 6f, 3.3f, 3f, 4.5f); lineTo(3f, 19f)
        cubicTo(6f, 17.8f, 9f, 18.2f, 12f, 20f); cubicTo(15f, 18.2f, 18f, 17.8f, 21f, 19f)
        lineTo(21f, 4.5f); cubicTo(18f, 3.3f, 15f, 3.8f, 12f, 6f); close()
    }
    drawPath(book, navy, style = stroke)
    drawLine(navy, Offset(12f, 6f), Offset(12f, 20f), stroke.width, StrokeCap.Round)
    if (active > 0f) {
        drawPath(book, KotoColors.SoftGrey.copy(alpha = 0.72f * active))
        drawLine(KotoColors.QuietInk, Offset(12f, 7f), Offset(12f, 19f), stroke.width, StrokeCap.Round, alpha = active)
        val top = 5.5f + (1f - active) * 3f
        val bookmark = Path().apply {
            moveTo(14.5f, top); lineTo(18f, top); lineTo(18f, top + 8f); lineTo(16.25f, top + 6.7f); lineTo(14.5f, top + 8f); close()
        }
        drawPath(bookmark, KotoColors.Gold.copy(alpha = active))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCards(navy: Color, active: Float, stroke: Stroke) {
    val rear = Path().apply {
        moveTo(4f, 16f); lineTo(2.5f, 5.5f); quadraticTo(2.3f, 3.5f, 4.4f, 3.3f)
        lineTo(14.5f, 2f); quadraticTo(16.5f, 1.8f, 16.8f, 4f)
    }
    drawPath(rear, navy, style = stroke)
    val offset = (1f - active) * 1.2f
    val front = Path().apply {
        moveTo(7f + offset, 7f); lineTo(18f + offset, 7f); quadraticTo(20f + offset, 7f, 20f + offset, 9f)
        lineTo(20f + offset, 20f); quadraticTo(20f + offset, 22f, 18f + offset, 22f); lineTo(9f + offset, 22f)
        quadraticTo(7f + offset, 22f, 7f + offset, 20f); close()
    }
    if (active > 0f) drawPath(front, KotoColors.SoftGrey.copy(alpha = 0.8f * active))
    drawPath(front, navy, style = stroke)
    drawLine(navy, Offset(11f + offset, 12f), Offset(17f + offset, 12f), stroke.width, StrokeCap.Round)
    drawLine(navy, Offset(11f + offset, 16f), Offset(15f + offset, 16f), stroke.width, StrokeCap.Round)
    if (active > 0f) {
        drawCircle(KotoColors.Gold.copy(alpha = active), radius = 1.35f * active, center = Offset(17f + offset, 18.5f))
        drawRoundRect(KotoColors.QuietInk.copy(alpha = 0.6f * active), Offset(4.5f, 5f), Size(9f, 2.2f), CornerRadius(1.1f))
    }
}


