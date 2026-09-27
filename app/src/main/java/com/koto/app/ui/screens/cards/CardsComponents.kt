package com.koto.app.ui.screens.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object CardsColors {
    val Background = Color(0xFFF4F7FA)
    val Surface = Color.White
    val Edge = Color(0xFFE2E8F0)
    val Ink = Color(0xFF0F172A)
    val Muted = Color(0xFF64748B)
    val Blue = Color(0xFF1D63B8)
    val BlueDepth = Color(0xFF134688)
    val Ice = Color(0xFFEBF3FA)
    val IceDepth = Color(0xFFCFDEEC)
    val Coral = Color(0xFFB44242)
    val Green = Color(0xFF237451)
    val Yellow = Color(0xFF936013)
}

private val CardsShape = RoundedCornerShape(16.dp)
private val IconStroke = Stroke(1.8f, cap = StrokeCap.Round)

internal val FlashcardDeck.accent: Color get() = when (icon) {
    "chatbubble" -> CardsColors.Green
    "hashtag" -> CardsColors.Yellow
    "home" -> CardsColors.Coral
    "utensils" -> Color(0xFFAD4676)
    "train" -> CardsColors.Blue
    else -> Color(0xFF7854A3)
}

@Composable
internal fun CardsPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(CardsShape).background(CardsColors.Surface)
        .border(1.dp, CardsColors.Edge, CardsShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun CardsButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    background: Color = CardsColors.Blue, ink: Color = Color.White,
    depth: Color = CardsColors.BlueDepth, enabled: Boolean = true, isSelected: Boolean? = null) {
    CardsPressable(onClick, modifier.semantics {
        isSelected?.let { selected = it; role = Role.RadioButton }
    }, background, depth, enabled) {
        Text(label, color = if (enabled) ink else CardsColors.Muted, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** Fixed 4 dp base and moving face, matching the lesson/map physical button treatment.
 * Only presentation owns this transient press animation; card/session state stays upstream.
 */
@Composable
internal fun CardsPressable(onClick: () -> Unit, modifier: Modifier = Modifier,
    face: Color = CardsColors.Surface, depth: Color = CardsColors.IceDepth,
    enabled: Boolean = true, padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val semanticPressed by interaction.collectIsPressedAsState()
    // clickable already emits the press interaction and cancels it when a drag
    // takes over. Avoid a second pointer-input coroutine on every lazy-list row.
    val pressed = semanticPressed
    val displacement by animateFloatAsState(if (pressed && enabled) 3f else 0f,
        spring(dampingRatio = .65f, stiffness = 1100f), label = "Cards press depth")
    val shape = CardsShape
    Box(modifier.padding(bottom = 4.dp)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().graphicsLayer { translationY = 4.dp.toPx() }
            .background(if (enabled) depth else CardsColors.Edge, shape))
        Box(Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer { translationY = displacement.dp.toPx() }
            .clip(shape).background(if (enabled) face else CardsColors.Background)
            .border(1.dp, if (!enabled || face == CardsColors.Surface) CardsColors.Edge else depth, shape)
            .padding(padding), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
internal fun MarkButton(kind: String, active: Boolean, label: String, tag: String, onClick: () -> Unit) {
    CardsPressable(onClick, Modifier.size(width = 48.dp, height = 52.dp).testTag(tag).semantics {
        contentDescription = label; selected = active; role = Role.Checkbox
        toggleableState = if (active) ToggleableState.On else ToggleableState.Off
    }, face = if (active) CardsColors.Ice else CardsColors.Surface, padding = PaddingValues(12.dp)) {
        LineIcon(kind, if (active) CardsColors.Coral else CardsColors.Muted, Modifier.size(22.dp), active)
    }
}

@Composable
internal fun DeckBadge(deck: FlashcardDeck) {
    DeckArtwork(deck, Modifier.size(28.dp))
}

/** Geometry is built once and only read by drawing; recycled rows share the same paths. */
private val IconPaths = listOf("heart", "bookmark", "star", "chatbubble", "home", "utensils", "train")
    .associateWith { kind ->
        Path().apply {
            when (kind) {
                "heart" -> {
                    moveTo(12f, 21f)
                    cubicTo(9f, 18f, 3f, 14f, 3f, 9f)
                    cubicTo(3f, 3f, 9f, 2f, 12f, 7f)
                    cubicTo(15f, 2f, 21f, 3f, 21f, 9f)
                    cubicTo(21f, 14f, 15f, 18f, 12f, 21f)
                    close()
                }
                "bookmark" -> { moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 21f); lineTo(12f, 17f); lineTo(6f, 21f); close() }
                "star" -> {
                    repeat(10) { i ->
                        val angle = Math.PI * i / 5 - Math.PI / 2
                        val radius = if (i % 2 == 0) 10.0 else 4.5
                        val x = (12 + kotlin.math.cos(angle) * radius).toFloat()
                        val y = (12 + kotlin.math.sin(angle) * radius).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                "chatbubble" -> { moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 16f); lineTo(11f, 16f); lineTo(5f, 21f); lineTo(5f, 16f); lineTo(4f, 16f); close() }
                "home" -> { moveTo(3f, 11f); lineTo(12f, 3f); lineTo(21f, 11f); moveTo(6f, 10f); lineTo(6f, 21f); lineTo(18f, 21f); lineTo(18f, 10f); moveTo(10f, 21f); lineTo(10f, 15f); lineTo(14f, 15f); lineTo(14f, 21f) }
                "utensils" -> { moveTo(20f, 21f); lineTo(20f, 3f); quadraticTo(14f, 5f, 16f, 13f); lineTo(20f, 13f) }
                "train" -> { moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 18f); lineTo(6f, 18f); close() }
            }
        }
    }

/** Small local vectors keep the module independent of icon libraries and bitmap assets. */
@Composable
internal fun LineIcon(kind: String, color: Color, modifier: Modifier, filled: Boolean = false) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x, y), Offset(x2, y2), 1.8f, StrokeCap.Round)
            val path = IconPaths[kind]
            when (kind) {
                "heart", "bookmark", "star", "home" -> Unit
                "chatbubble" -> line(8f, 9f, 16f, 9f)
                "hashtag" -> { line(9f, 3f, 7f, 21f); line(17f, 3f, 15f, 21f); line(3f, 9f, 21f, 9f); line(3f, 15f, 21f, 15f) }
                "utensils" -> { line(4f, 3f, 4f, 10f); line(8f, 3f, 8f, 21f); line(12f, 3f, 12f, 10f); line(4f, 10f, 12f, 10f) }
                "train" -> { line(6f, 11f, 18f, 11f); line(8f, 18f, 5f, 22f); line(16f, 18f, 19f, 22f); drawCircle(color, 1f, Offset(9f, 15f)); drawCircle(color, 1f, Offset(15f, 15f)) }
                else -> { drawOval(color, Offset(3f, 3f), androidx.compose.ui.geometry.Size(6f, 10f), style = IconStroke); drawOval(color, Offset(14f, 9f), androidx.compose.ui.geometry.Size(6f, 10f), style = IconStroke); line(5f, 16f, 7f, 18f); line(16f, 22f, 18f, 22f) }
            }
            if (path != null) {
                if (filled) drawPath(path, color) else drawPath(path, color, style = IconStroke)
            }
        }
    }
}
