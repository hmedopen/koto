package com.koto.app.ui.screens.cards

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

private val Navy = Color(0xFF314963)
private val Blue = Color(0xFF639EDC)
private val Mint = Color(0xFF67B89A)
private val Gold = Color(0xFFF0BC58)
private val Coral = Color(0xFFE68779)
private val Violet = Color(0xFFA18ACB)
private val Paper = Color(0xFFF0F5FA)

/** Decorative, density-independent miniatures. The adjacent deck title supplies semantics.
 * Stable deck IDs keep artwork separate from vocabulary and session data.
 */
@Composable
internal fun DeckArtwork(deck: FlashcardDeck, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 32f, size.height / 32f, pivot = Offset.Zero) {
            when (deck.id) {
                "deck_01" -> { // Two people exchanging greetings.
                    person(8f, 12f, Mint); person(24f, 12f, Blue)
                    box(5f, 2f, 16f, 10f, Mint); polygon(Mint, 9f, 10f, 9f, 15f, 14f, 10f)
                    line(9f, 7f, 17f, 7f, Color.White)
                }
                "deck_02" -> { box(3f, 5f, 26f, 23f, Paper); person(10f, 10f, Violet); line(19f, 13f, 25f, 13f); line(19f, 18f, 24f, 18f); box(12f, 2f, 8f, 5f, Gold) }
                "deck_03" -> { // Abacus.
                    box(3f, 3f, 26f, 26f, Paper)
                    for (y in listOf(9f, 16f, 23f)) { line(6f, y, 26f, y); for (x in listOf(10f, 15f, if (y == 16f) 23f else 20f)) box(x - 2f, y - 3f, 4f, 6f, if (y == 16f) Coral else Gold) }
                }
                "deck_04" -> { person(8f, 4f, Blue); box(19f, 7f, 9f, 10f, Gold); box(4f, 23f, 9f, 6f, Mint); box(19f, 23f, 9f, 6f, Violet); line(16f, 4f, 16f, 29f, Navy) }
                "deck_05" -> { disc(16f, 16f, 13f, Blue); disc(16f, 16f, 10f, Paper); line(16f, 16f, 16f, 9f); line(16f, 16f, 22f, 19f); disc(16f, 16f, 1.5f, Coral) }
                "deck_06" -> { calendar(); repeat(7) { i -> box(6f + (i % 4) * 5f, 14f + (i / 4) * 6f, 3f, 4f, if (i == 2) Coral else Blue) } }
                "deck_07" -> { box(7f, 7f, 24f, 24f, Gold); calendar(); line(12f, 15f, 16f, 13f); line(16f, 13f, 16f, 24f); line(12f, 24f, 20f, 24f) }
                "deck_08" -> { disc(16f, 16f, 8f, Paper); line(16f, 16f, 16f, 11f); line(16f, 16f, 20f, 18f); drawArc(Mint, 205f, 265f, false, Offset(3f, 3f), Size(26f, 26f), style = Stroke(3f, cap = StrokeCap.Round)); polygon(Mint, 2f, 7f, 11f, 7f, 6f, 14f) }
                "deck_09" -> { person(7f, 5f, Blue); person(25f, 5f, Coral); person(16f, 13f, Gold) }
                "deck_10" -> { person(11f, 3f, Violet); box(17f, 18f, 13f, 11f, Gold); line(21f, 18f, 21f, 15f); line(21f, 15f, 26f, 15f); line(26f, 15f, 26f, 18f) }
                "deck_11" -> { disc(9f, 9f, 6f, Gold); box(12f, 15f, 15f, 13f, Mint); drawCircle(Mint, 4f, Offset(27f, 20f), style = Stroke(2f)); line(16f, 11f, 16f, 5f, Coral); line(22f, 11f, 22f, 7f, Coral) }
                "deck_12" -> { disc(18f, 5f, 3f, Blue); line(16f, 10f, 13f, 18f, Blue, 4f); line(15f, 12f, 23f, 15f); line(14f, 18f, 22f, 26f); line(14f, 18f, 7f, 26f); arrow(3f, 12f, 9f, 12f, Mint) }
                "deck_13" -> { polygon(Violet, 6f, 20f, 3f, 15f, 7f, 8f, 19f, 6f, 25f, 12f, 23f, 25f, 12f, 28f, 12f, 21f); disc(17f, 12f, 6f, Gold); line(14f, 20f, 20f, 20f); line(15f, 23f, 19f, 23f) }
                "deck_14" -> { box(10f, 11f, 12f, 11f, Gold); line(16f, 11f, 16f, 22f, Coral); arrow(3f, 6f, 26f, 6f, Blue); arrow(29f, 27f, 6f, 27f, Mint) }
                "deck_15" -> { box(3f, 7f, 12f, 21f, Blue); box(21f, 18f, 8f, 10f, Mint); arrow(25f, 14f, 25f, 4f, Coral) }
                "deck_16" -> { face(11f, 12f, Gold, true); face(22f, 22f, Blue, false) }
                "deck_17" -> { disc(16f, 16f, 12f, Mint); line(9f, 16f, 14f, 21f, Color.White, 3f); line(14f, 21f, 23f, 11f, Color.White, 3f); disc(26f, 5f, 3f, Gold) }
                "deck_18" -> { box(7f, 13f, 19f, 16f, Gold); polygon(Coral, 2f, 15f, 16f, 3f, 30f, 15f); box(14f, 20f, 6f, 9f, Blue); box(9f, 17f, 4f, 4f, Paper) }
                "deck_19" -> { box(3f, 8f, 18f, 21f, Blue); line(7f, 9f, 7f, 27f, Paper); line(12f, 14f, 17f, 14f, Paper); line(25f, 24f, 29f, 5f, Gold, 4f); polygon(Navy, 23f, 28f, 24f, 22f, 28f, 23f) }
                "deck_20" -> { disc(10f, 12f, 7f, Coral); line(10f, 6f, 13f, 3f, Mint, 3f); polygon(Gold, 20f, 10f, 29f, 14f, 18f, 29f); line(25f, 11f, 28f, 5f, Mint, 3f); line(23f, 10f, 22f, 4f, Mint, 3f) }
                "deck_21" -> { drawCircle(Mint, 5f, Offset(25f, 18f), style = Stroke(3f)); box(5f, 11f, 19f, 15f, Mint); line(4f, 28f, 27f, 28f, Navy); line(10f, 7f, 12f, 3f, Coral); line(17f, 7f, 19f, 3f, Coral) }
                "deck_22" -> { person(11f, 3f, Blue); box(20f, 16f, 7f, 15f, Coral); box(16f, 20f, 15f, 7f, Coral) }
                "deck_23" -> polygon(Violet, 10f, 4f, 13f, 8f, 19f, 8f, 22f, 4f, 30f, 11f, 25f, 16f, 23f, 14f, 23f, 29f, 9f, 29f, 9f, 14f, 7f, 16f, 2f, 11f)
                "deck_24" -> { disc(10f, 10f, 8f, Coral); box(17f, 13f, 13f, 14f, Blue); polygon(Gold, 3f, 29f, 11f, 15f, 19f, 29f) }
                "deck_25" -> { disc(11f, 10f, 7f, Gold); for (x in listOf(12f, 20f, 26f)) disc(x, 19f, 5f, Blue); disc(19f, 15f, 6f, Blue); line(13f, 27f, 11f, 30f, Blue); line(23f, 27f, 21f, 30f, Blue) }
                "deck_26" -> { polygon(Blue, 2f, 28f, 14f, 6f, 27f, 28f); polygon(Paper, 10f, 13f, 14f, 6f, 18f, 13f); line(25f, 20f, 25f, 30f); polygon(Mint, 17f, 23f, 25f, 7f, 32f, 23f) }
                "deck_27" -> { polygon(Gold, 5f, 16f, 5f, 3f, 14f, 9f, 20f, 9f, 28f, 3f, 28f, 18f); disc(16f, 19f, 12f, Gold); disc(11f, 17f, 1.5f, Navy); disc(22f, 17f, 1.5f, Navy); polygon(Coral, 13f, 22f, 19f, 22f, 16f, 25f) }
                "deck_28" -> { box(3f, 12f, 12f, 18f, Coral); box(17f, 3f, 12f, 27f, Blue); for (y in listOf(8f, 15f, 22f)) { box(20f, y, 3f, 3f, Paper); box(25f, y, 2f, 3f, Paper) }; box(7f, 17f, 4f, 5f, Paper) }
                "deck_29" -> { line(16f, 3f, 16f, 29f, Navy, 3f); polygon(Mint, 3f, 6f, 22f, 6f, 28f, 11f, 22f, 16f, 3f, 16f); polygon(Gold, 5f, 19f, 27f, 19f, 27f, 26f, 5f, 26f, 1f, 22f) }
                "deck_30" -> { box(6f, 2f, 20f, 25f, Blue); box(9f, 6f, 14f, 10f, Paper); disc(11f, 22f, 2f, Gold); disc(21f, 22f, 2f, Gold); line(10f, 27f, 7f, 31f); line(22f, 27f, 25f, 31f) }
                "deck_31" -> { polygon(Blue, 2f, 8f, 13f, 6f, 16f, 9f, 19f, 6f, 30f, 8f, 30f, 28f, 19f, 26f, 16f, 29f, 13f, 26f, 2f, 28f); line(16f, 10f, 16f, 26f, Paper); line(6f, 13f, 12f, 12f, Paper); line(20f, 12f, 26f, 13f, Paper) }
                "deck_32" -> { box(10f, 3f, 12f, 9f, Navy); box(13f, 6f, 6f, 6f, Paper); box(2f, 10f, 28f, 20f, Gold); line(3f, 18f, 29f, 18f, Navy); box(14f, 16f, 4f, 6f, Paper) }
                "deck_33" -> { box(3f, 11f, 19f, 19f, Coral); drawArc(Navy, 180f, 180f, false, Offset(7f, 3f), Size(11f, 15f), style = Stroke(2f)); disc(24f, 24f, 7f, Gold); line(24f, 20f, 24f, 28f) }
                "deck_34" -> { line(13f, 22f, 13f, 7f, Violet, 3f); line(13f, 7f, 27f, 4f, Violet, 4f); line(27f, 4f, 27f, 20f, Violet, 3f); disc(8f, 23f, 5f, Blue); disc(23f, 21f, 5f, Coral) }
                "deck_35" -> { box(5f, 2f, 18f, 28f, Navy); box(8f, 5f, 12f, 21f, Paper); box(13f, 9f, 17f, 11f, Mint); polygon(Mint, 17f, 18f, 17f, 24f, 23f, 18f); line(17f, 14f, 26f, 14f, Color.White) }
                "deck_36" -> { disc(16f, 16f, 14f, Violet); drawArc(Color.White, 185f, 270f, false, Offset(11f, 6f), Size(10f, 10f), style = Stroke(3f, cap = StrokeCap.Round)); line(16f, 16f, 16f, 20f, Color.White, 3f); disc(16f, 25f, 1.7f, Color.White) }
                "deck_37" -> { drawRoundRect(Blue, Offset(2f, 7f), Size(18f, 11f), CornerRadius(5f), style = Stroke(3f)); drawRoundRect(Mint, Offset(12f, 16f), Size(18f, 11f), CornerRadius(5f), style = Stroke(3f)); line(12f, 13f, 21f, 22f, Navy, 3f) }
                "deck_38" -> { line(3f, 8f, 29f, 8f); line(3f, 16f, 29f, 16f); line(3f, 25f, 29f, 25f); disc(10f, 8f, 4f, Blue); disc(23f, 16f, 4f, Coral); disc(16f, 25f, 4f, Mint) }
                "deck_39" -> { box(3f, 21f, 7f, 9f, Mint); box(13f, 13f, 7f, 17f, Gold); box(23f, 3f, 7f, 27f, Coral) }
                "deck_40" -> { box(3f, 4f, 26f, 20f, Blue); polygon(Blue, 8f, 22f, 8f, 30f, 17f, 22f); line(10f, 10f, 8f, 16f, Paper, 3f); line(21f, 10f, 19f, 16f, Paper, 3f) }
                "deck_41" -> { polygon(Mint, 3f, 5f, 16f, 2f, 29f, 5f, 27f, 22f, 16f, 31f, 5f, 22f); box(13f, 8f, 6f, 16f, Color.White); box(8f, 13f, 16f, 6f, Color.White) }
                else -> { box(4f, 5f, 24f, 23f, Blue); line(16f, 7f, 16f, 26f, Paper) }
            }
        }
    }
}

private fun DrawScope.box(x: Float, y: Float, w: Float, h: Float, color: Color) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(2f))
private fun DrawScope.disc(x: Float, y: Float, r: Float, color: Color) = drawCircle(color, r, Offset(x, y))
private fun DrawScope.line(x: Float, y: Float, x2: Float, y2: Float, color: Color = Navy, width: Float = 2f) =
    drawLine(color, Offset(x, y), Offset(x2, y2), width, StrokeCap.Round)
private fun DrawScope.polygon(color: Color, vararg points: Float) {
    drawPath(Path().apply { moveTo(points[0], points[1]); for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1]); close() }, color)
}
private fun DrawScope.person(x: Float, y: Float, color: Color) {
    disc(x, y + 4f, 4f, Gold); box(x - 5f, y + 9f, 10f, 10f, color)
}
private fun DrawScope.calendar() {
    box(3f, 5f, 26f, 25f, Paper); box(3f, 5f, 26f, 7f, Coral)
    line(9f, 2f, 9f, 7f); line(23f, 2f, 23f, 7f)
}
private fun DrawScope.arrow(x: Float, y: Float, x2: Float, y2: Float, color: Color) {
    line(x, y, x2, y2, color, 2.5f)
    val dx = x2 - x; val dy = y2 - y
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    val ux = dx / length; val uy = dy / length
    polygon(color, x2, y2, x2 - ux * 5f - uy * 4f, y2 - uy * 5f + ux * 4f, x2 - ux * 5f + uy * 4f, y2 - uy * 5f - ux * 4f)
}
private fun DrawScope.face(x: Float, y: Float, color: Color, happy: Boolean) {
    disc(x, y, 9f, color); disc(x - 3f, y - 2f, 1f, Navy); disc(x + 3f, y - 2f, 1f, Navy)
    drawArc(Navy, if (happy) 0f else 180f, 180f, false, Offset(x - 4f, y + if (happy) 0f else 3f), Size(8f, 5f), style = Stroke(1.5f))
}
