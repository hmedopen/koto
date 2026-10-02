package com.koto.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.theme.KotoColors
import kotlin.math.PI
import kotlin.math.sin

/** High-craft navigation illustrations with tactile 3D depth, dynamic reveals, and micro-animations. */
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

private fun DrawScope.drawMap(color: Color, active: Float, stroke: Stroke) {
    val leftPanel = Path().apply {
        moveTo(3f, 5.5f); lineTo(9f, 3.5f); lineTo(9f, 18.5f); lineTo(3f, 20.5f); close()
    }
    val centerPanel = Path().apply {
        moveTo(9f, 3.5f); lineTo(15f, 5.5f); lineTo(15f, 20.5f); lineTo(9f, 18.5f); close()
    }
    val rightPanel = Path().apply {
        moveTo(15f, 5.5f); lineTo(21f, 3.5f); lineTo(21f, 18.5f); lineTo(15f, 20.5f); close()
    }

    if (active > 0f) {
        // Shaded 3D paper accordion folds
        drawPath(leftPanel, Color(0xFFF9FBFE).copy(alpha = active))
        drawPath(centerPanel, Color(0xFFE2EEFB).copy(alpha = active))
        drawPath(rightPanel, Color(0xFFEEF5FD).copy(alpha = active))

        // Japanese mountain peak on the left panel
        val mountain = Path().apply {
            moveTo(4.5f, 17f)
            lineTo(6.5f, 13f)
            lineTo(8.5f, 17f)
            close()
        }
        drawPath(mountain, Color(0xFFCADDF2).copy(alpha = active * 0.85f))
        drawPath(mountain, color.copy(alpha = active * 0.5f), style = Stroke(0.9f))

        // Dotted discovery pathway
        val route = Path().apply {
            moveTo(6.5f, 15f)
            cubicTo(8.5f, 14f, 10.5f, 12f, 12.5f, 13f)
            cubicTo(14.5f, 14f, 16.2f, 11f, 18.2f, 9.5f)
        }
        drawPath(
            route,
            KotoColors.LessonBlue.copy(alpha = active),
            style = Stroke(
                width = 1.3f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f), 0f),
            ),
        )
    }

    // Outer map outlines and crease lines
    val outline = Path().apply {
        moveTo(3f, 5.5f); lineTo(9f, 3.5f); lineTo(15f, 5.5f); lineTo(21f, 3.5f)
        lineTo(21f, 18.5f); lineTo(15f, 20.5f); lineTo(9f, 18.5f); lineTo(3f, 20.5f); close()
    }
    drawPath(outline, color, style = stroke)
    drawLine(color, Offset(9f, 3.5f), Offset(9f, 18.5f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(15f, 5.5f), Offset(15f, 20.5f), stroke.width, StrokeCap.Round)

    if (active > 0f) {
        // Elastic bounce for golden pin drop
        val dropProgress = (active * 1.25f).coerceIn(0f, 1f)
        val bounceOffset = if (dropProgress < 0.7f) {
            (1f - dropProgress / 0.7f) * -5f
        } else {
            val t = (dropProgress - 0.7f) / 0.3f
            -sin(t * PI.toFloat()) * 1.6f
        }
        val pinX = 18.2f
        val pinCenterY = 6.8f + bounceOffset
        val pinTipY = pinCenterY + 3.2f

        // Ground shadow beneath pin
        drawOval(
            color = Color(0x331A3761).copy(alpha = active),
            topLeft = Offset(pinX - 1.6f, 9.4f),
            size = Size(3.2f, 1.2f),
        )

        // Pulsing radar ripple
        if (active > 0.4f) {
            val rippleAlpha = (1f - (active - 0.4f) / 0.6f) * 0.7f
            drawOval(
                color = KotoColors.Gold.copy(alpha = rippleAlpha),
                topLeft = Offset(pinX - 2.8f * active, 9.4f - 0.6f * active),
                size = Size(5.6f * active, 2.4f * active),
                style = Stroke(0.8f),
            )
        }

        // Teardrop pin body
        val pinPath = Path().apply {
            moveTo(pinX, pinTipY)
            cubicTo(pinX - 2f, pinCenterY + 1.2f, pinX - 2.2f, pinCenterY - 0.2f, pinX - 2.2f, pinCenterY - 1f)
            cubicTo(pinX - 2.2f, pinCenterY - 2.6f, pinX - 1.2f, pinCenterY - 3.4f, pinX, pinCenterY - 3.4f)
            cubicTo(pinX + 1.2f, pinCenterY - 3.4f, pinX + 2.2f, pinCenterY - 2.6f, pinX + 2.2f, pinCenterY - 1f)
            cubicTo(pinX + 2.2f, pinCenterY - 0.2f, pinX + 2f, pinCenterY + 1.2f, pinX, pinTipY)
            close()
        }
        drawPath(pinPath, KotoColors.Gold.copy(alpha = active))
        drawPath(pinPath, color.copy(alpha = active), style = Stroke(1.1f, join = StrokeJoin.Round))
        drawCircle(Color.White.copy(alpha = active), radius = 0.8f * active, center = Offset(pinX, pinCenterY - 1f))
    }
}

private fun DrawScope.drawLearn(color: Color, active: Float, stroke: Stroke) {
    val book = Path().apply {
        moveTo(12f, 5.5f)
        cubicTo(9f, 3.8f, 6f, 3.4f, 3f, 4.5f)
        lineTo(3f, 18.5f)
        cubicTo(6f, 17.4f, 9f, 17.8f, 12f, 19.8f)
        cubicTo(15f, 17.8f, 18f, 17.4f, 21f, 18.5f)
        lineTo(21f, 4.5f)
        cubicTo(18f, 3.4f, 15f, 3.8f, 12f, 5.5f)
        close()
    }

    if (active > 0f) {
        // Lower leaf rim showing book thickness / stacked pages
        val lowerRim = Path().apply {
            moveTo(3f, 18.5f); lineTo(3f, 19.8f)
            cubicTo(6f, 18.7f, 9f, 19.1f, 12f, 21f)
            cubicTo(15f, 19.1f, 18f, 18.7f, 21f, 19.8f)
            lineTo(21f, 18.5f)
            cubicTo(18f, 17.4f, 15f, 17.8f, 12f, 19.8f)
            cubicTo(9f, 17.8f, 6f, 17.4f, 3f, 18.5f)
            close()
        }
        drawPath(lowerRim, Color(0xFFE4EDF7).copy(alpha = active))
        drawPath(lowerRim, color.copy(alpha = active * 0.4f), style = Stroke(0.8f))

        // Main page fills: warm paper on left, crisp on right
        val leftPage = Path().apply {
            moveTo(12f, 5.5f); cubicTo(9f, 3.8f, 6f, 3.4f, 3f, 4.5f); lineTo(3f, 18.5f)
            cubicTo(6f, 17.4f, 9f, 17.8f, 12f, 19.8f); close()
        }
        val rightPage = Path().apply {
            moveTo(12f, 5.5f); cubicTo(15f, 3.8f, 18f, 3.4f, 21f, 4.5f); lineTo(21f, 18.5f)
            cubicTo(18f, 17.4f, 15f, 17.8f, 12f, 19.8f); close()
        }
        drawPath(leftPage, Color(0xFFFDFAF5).copy(alpha = active))
        drawPath(rightPage, Color(0xFFF6FAFD).copy(alpha = active))

        // Spine shadow
        drawLine(
            Color(0x221A3761).copy(alpha = active),
            Offset(12f, 6f),
            Offset(12f, 19.5f),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )

        // Left page study lines
        drawLine(
            KotoColors.NavySoft.copy(alpha = 0.75f * active),
            Offset(6.5f, 9.5f),
            Offset(10f, 9.5f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
        drawLine(
            KotoColors.NavySoft.copy(alpha = 0.75f * active),
            Offset(6.5f, 13f),
            Offset(9.5f, 13f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
        drawCircle(KotoColors.LessonBlue.copy(alpha = active), radius = 0.6f, center = Offset(5.5f, 9.5f))
        drawCircle(KotoColors.LessonBlue.copy(alpha = active), radius = 0.6f, center = Offset(5.5f, 13f))

        // Right page Japanese wisdom glyph strokes
        drawLine(
            KotoColors.LessonBlue.copy(alpha = 0.8f * active),
            Offset(14f, 10.5f),
            Offset(18.5f, 10.5f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
        drawLine(
            KotoColors.LessonBlue.copy(alpha = 0.8f * active),
            Offset(16.2f, 9.2f),
            Offset(16.2f, 14.5f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
        drawLine(
            KotoColors.NavySoft.copy(alpha = 0.6f * active),
            Offset(15.2f, 12f),
            Offset(13.8f, 14.8f),
            strokeWidth = 1.1f,
            cap = StrokeCap.Round,
        )
        drawLine(
            KotoColors.NavySoft.copy(alpha = 0.6f * active),
            Offset(17.2f, 12f),
            Offset(18.6f, 14.8f),
            strokeWidth = 1.1f,
            cap = StrokeCap.Round,
        )
    }

    // Book outline
    drawPath(book, color, style = stroke)
    drawLine(color, Offset(12f, 5.5f), Offset(12f, 19.8f), stroke.width, StrokeCap.Round)

    if (active > 0f) {
        // Golden bookmark with elastic drop
        val drop = (active * 1.2f).coerceIn(0f, 1f)
        val ribbonBounce = if (drop < 0.75f) drop / 0.75f else 1f + sin((drop - 0.75f) / 0.25f * PI.toFloat()) * 0.15f
        val ribbonLen = 7.5f * ribbonBounce
        val rTop = 4.8f
        val rBottom = rTop + ribbonLen
        val ribbon = Path().apply {
            moveTo(14.5f, rTop)
            lineTo(17.5f, rTop)
            lineTo(17.5f, rBottom)
            lineTo(16f, rBottom - 1.8f) // swallowtail notch
            lineTo(14.5f, rBottom)
            close()
        }
        drawPath(ribbon, KotoColors.Gold.copy(alpha = active))
        drawPath(ribbon, color.copy(alpha = active), style = Stroke(1f, join = StrokeJoin.Round))

        // Spark of knowledge / 4-point star above the spine
        val starSize = 2.1f * active
        if (starSize > 0.3f) {
            val star = Path().apply {
                moveTo(12f, 2.8f - starSize)
                quadraticTo(12f, 2.8f, 12f + starSize, 2.8f)
                quadraticTo(12f, 2.8f, 12f, 2.8f + starSize)
                quadraticTo(12f, 2.8f, 12f - starSize, 2.8f)
                quadraticTo(12f, 2.8f, 12f, 2.8f - starSize)
                close()
            }
            drawPath(star, KotoColors.Gold.copy(alpha = active))
            drawCircle(Color.White.copy(alpha = active), radius = 0.6f * active, center = Offset(12f, 2.8f))
        }
    }
}

private fun DrawScope.drawCards(color: Color, active: Float, stroke: Stroke) {
    val backDx = -1.8f * active
    val backDy = -0.5f * active
    val frontDy = -1.8f * active

    // Back card (slightly tilted to left)
    val rear = Path().apply {
        moveTo(4f + backDx, 16.5f + backDy)
        lineTo(2.5f + backDx, 5.5f + backDy)
        quadraticTo(2.3f + backDx, 3.5f + backDy, 4.4f + backDx, 3.3f + backDy)
        lineTo(14.5f + backDx, 2f + backDy)
        quadraticTo(16.5f + backDx, 1.8f + backDy, 16.8f + backDx, 4f + backDy)
    }

    if (active > 0f) {
        val rearFull = Path().apply {
            moveTo(4f + backDx, 16.5f + backDy)
            lineTo(2.5f + backDx, 5.5f + backDy)
            quadraticTo(2.3f + backDx, 3.5f + backDy, 4.4f + backDx, 3.3f + backDy)
            lineTo(14.5f + backDx, 2f + backDy)
            quadraticTo(16.5f + backDx, 1.8f + backDy, 16.8f + backDx, 4f + backDy)
            lineTo(16f + backDx, 14f + backDy)
            close()
        }
        drawPath(rearFull, Color(0xFFE5EFFB).copy(alpha = active))
    }
    drawPath(rear, color.copy(alpha = if (active > 0f) 0.85f else 1f), style = stroke)

    // Front card geometry
    val frontX = 6.8f
    val frontY = 6.2f + frontDy
    val frontW = 13.4f
    val frontH = 15.6f
    val frontR = 2.4f

    if (active > 0f) {
        // Elevation drop shadow under front card
        drawRoundRect(
            color = Color(0x241A3761).copy(alpha = active),
            topLeft = Offset(frontX + 0.8f, frontY + 1.2f),
            size = Size(frontW, frontH),
            cornerRadius = CornerRadius(frontR),
        )
        // Opaque white card body
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(frontX, frontY),
            size = Size(frontW, frontH),
            cornerRadius = CornerRadius(frontR),
        )
        // Top category header strip
        drawRoundRect(
            color = Color(0xFFE8F1FC),
            topLeft = Offset(frontX + 1.5f, frontY + 1.5f),
            size = Size(frontW - 3f, 2.6f),
            cornerRadius = CornerRadius(1.3f),
        )
    }

    // Front card outline
    drawRoundRect(
        color = color,
        topLeft = Offset(frontX, frontY),
        size = Size(frontW, frontH),
        cornerRadius = CornerRadius(frontR),
        style = stroke,
    )

    if (active > 0f) {
        // Center Hiragana 「あ」
        val hiragana = Path().apply {
            // Horizontal stroke
            moveTo(frontX + 3.4f, frontY + 6.8f)
            lineTo(frontX + 8.8f, frontY + 6.8f)
            // Vertical downward curve
            moveTo(frontX + 6.2f, frontY + 5.2f)
            cubicTo(
                frontX + 6.2f, frontY + 7.5f,
                frontX + 5.8f, frontY + 9.5f,
                frontX + 4.8f, frontY + 11.2f,
            )
            // Looping flourish
            moveTo(frontX + 4.5f, frontY + 9f)
            cubicTo(
                frontX + 3.8f, frontY + 10.8f,
                frontX + 8f, frontY + 11.8f,
                frontX + 8.6f, frontY + 10f,
            )
            cubicTo(
                frontX + 8.8f, frontY + 9f,
                frontX + 7.8f, frontY + 8f,
                frontX + 6.5f, frontY + 8.6f,
            )
        }
        drawPath(
            hiragana,
            color,
            style = Stroke(1.35f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // SRS study dots at bottom
        drawCircle(KotoColors.LessonBlue.copy(alpha = active), radius = 0.75f, center = Offset(frontX + 4f, frontY + 13.5f))
        drawCircle(KotoColors.LessonBlue.copy(alpha = active), radius = 0.75f, center = Offset(frontX + 6.2f, frontY + 13.5f))

        // Golden mastery star seal in top-right corner
        val badgeProgress = (active * 1.3f).coerceIn(0f, 1f)
        val badgeScale = if (badgeProgress < 0.7f) {
            badgeProgress / 0.7f * 1.2f
        } else {
            1.2f - (badgeProgress - 0.7f) / 0.3f * 0.2f
        }
        val badgeR = 2.4f * badgeScale
        val bX = frontX + frontW - 1.2f
        val bY = frontY + 1.2f

        drawCircle(KotoColors.Gold.copy(alpha = active), radius = badgeR, center = Offset(bX, bY))
        drawCircle(color.copy(alpha = active), radius = badgeR, center = Offset(bX, bY), style = Stroke(0.9f))
        drawCircle(Color.White.copy(alpha = active), radius = 0.75f * badgeScale, center = Offset(bX, bY))

        // Tiny star sparkle burst
        if (badgeProgress > 0.8f) {
            val p = (badgeProgress - 0.8f) / 0.2f
            drawCircle(KotoColors.Gold.copy(alpha = (1f - p) * active), radius = 0.5f, center = Offset(bX - 2.8f * p, bY - 2.4f * p))
            drawCircle(KotoColors.Gold.copy(alpha = (1f - p) * active), radius = 0.45f, center = Offset(bX + 2.5f * p, bY - 2.5f * p))
        }
    } else {
        // Inactive preview lines
        drawLine(color, Offset(frontX + 3.5f, frontY + 6.5f), Offset(frontX + 9.5f, frontY + 6.5f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(frontX + 3.5f, frontY + 10.5f), Offset(frontX + 7.5f, frontY + 10.5f), stroke.width, StrokeCap.Round)
    }
}



