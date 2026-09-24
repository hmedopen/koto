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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
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
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(CardsColors.Surface)
        .border(1.dp, CardsColors.Edge, RoundedCornerShape(16.dp)).padding(16.dp),
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
    var pointerPressed by remember { mutableStateOf(false) }
    val pressed = semanticPressed || pointerPressed
    val displacement by animateFloatAsState(if (pressed && enabled) 3f else 0f,
        spring(dampingRatio = .65f, stiffness = 1100f), label = "Cards press depth")
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.padding(bottom = 4.dp)
        .pointerInput(enabled) {
            if (!enabled) { pointerPressed = false; return@pointerInput }
            try {
                awaitPointerEventScope {
                    while (true) {
                        // Observe down immediately, even inside a lazy list. Clickable still
                        // owns activation/cancellation, so dragging never opens a deck.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.changedToDown() }) pointerPressed = true
                        if (event.changes.none { it.pressed }) pointerPressed = false
                    }
                }
            } finally { pointerPressed = false }
        }
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
        LineIcon(kind, if (active) CardsColors.Yellow else CardsColors.Muted, Modifier.size(22.dp), active)
    }
}

@Composable
internal fun DeckBadge(deck: FlashcardDeck) {
    LineIcon(deck.icon, deck.accent, Modifier.size(28.dp))
}

/** Small local vectors keep the module independent of icon libraries and bitmap assets. */
@Composable
internal fun LineIcon(kind: String, color: Color, modifier: Modifier, filled: Boolean = false) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.8f, cap = StrokeCap.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x, y), Offset(x2, y2), 1.8f, StrokeCap.Round)
            val p = Path()
            when (kind) {
                "bookmark" -> { p.moveTo(6f, 3f); p.lineTo(18f, 3f); p.lineTo(18f, 21f); p.lineTo(12f, 17f); p.lineTo(6f, 21f); p.close() }
                "star" -> {
                    repeat(10) { i ->
                        val angle = Math.PI * i / 5 - Math.PI / 2
                        val radius = if (i % 2 == 0) 10.0 else 4.5
                        val x = (12 + kotlin.math.cos(angle) * radius).toFloat()
                        val y = (12 + kotlin.math.sin(angle) * radius).toFloat()
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }; p.close()
                }
                "chatbubble" -> { p.moveTo(4f, 4f); p.lineTo(20f, 4f); p.lineTo(20f, 16f); p.lineTo(11f, 16f); p.lineTo(5f, 21f); p.lineTo(5f, 16f); p.lineTo(4f, 16f); p.close(); line(8f, 9f, 16f, 9f) }
                "hashtag" -> { line(9f, 3f, 7f, 21f); line(17f, 3f, 15f, 21f); line(3f, 9f, 21f, 9f); line(3f, 15f, 21f, 15f) }
                "home" -> { p.moveTo(3f, 11f); p.lineTo(12f, 3f); p.lineTo(21f, 11f); p.moveTo(6f, 10f); p.lineTo(6f, 21f); p.lineTo(18f, 21f); p.lineTo(18f, 10f); p.moveTo(10f, 21f); p.lineTo(10f, 15f); p.lineTo(14f, 15f); p.lineTo(14f, 21f) }
                "utensils" -> { line(4f, 3f, 4f, 10f); line(8f, 3f, 8f, 21f); line(12f, 3f, 12f, 10f); line(4f, 10f, 12f, 10f); p.moveTo(20f, 21f); p.lineTo(20f, 3f); p.quadraticTo(14f, 5f, 16f, 13f); p.lineTo(20f, 13f) }
                "train" -> { p.moveTo(6f, 3f); p.lineTo(18f, 3f); p.lineTo(18f, 18f); p.lineTo(6f, 18f); p.close(); line(6f, 11f, 18f, 11f); line(8f, 18f, 5f, 22f); line(16f, 18f, 19f, 22f); drawCircle(color, 1f, Offset(9f, 15f)); drawCircle(color, 1f, Offset(15f, 15f)) }
                else -> { drawOval(color, Offset(3f, 3f), androidx.compose.ui.geometry.Size(6f, 10f), style = stroke); drawOval(color, Offset(14f, 9f), androidx.compose.ui.geometry.Size(6f, 10f), style = stroke); line(5f, 16f, 7f, 18f); line(16f, 22f, 18f, 22f) }
            }
            if (filled) drawPath(p, color) else drawPath(p, color, style = stroke)
        }
    }
}
