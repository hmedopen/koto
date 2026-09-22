package com.koto.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.koto.app.ui.theme.KotoColors
import kotlinx.coroutines.delay

enum class TactileTone { Default, Primary, Selected, Correct, Wrong, Warning, Quiet }

@Composable
fun Modifier.feedbackWiggle(active: Boolean): Modifier {
    var phase by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (active) { phase = true; delay(70); phase = false; delay(70); phase = true; delay(70); phase = false }
    }
    val offset by animateFloatAsState(if (phase) 5f else 0f, tween(55), label = "Feedback wiggle")
    return graphicsLayer { translationX = offset.dp.toPx() }
}

@Composable
fun Modifier.feedbackWiggle(trigger: Int): Modifier {
    var phase by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) { phase = true; delay(70); phase = false; delay(70); phase = true; delay(70); phase = false }
    }
    val offset by animateFloatAsState(if (phase) 5f else 0f, tween(55), label = "Feedback wiggle")
    return graphicsLayer { translationX = offset.dp.toPx() }
}

/** Shared solid face and 4 dp edge, used by answers, tiles, utility and action buttons. */
@Composable
fun TactileButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    tone: TactileTone = TactileTone.Default, selected: Boolean = false,
    description: String? = null, stateLabel: String? = null,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Press state is applied synchronously on down; only the return uses a short transition.
    val displacement by animateFloatAsState(if (pressed) 4f else 0f,
        if (pressed) snap() else tween(90), label = "Tactile depth")
    val edgeDisplacement by animateFloatAsState(if (pressed) 4f else 4f,
        if (pressed) snap() else tween(90), label = "Tactile edge")
    val emphasized = tone == TactileTone.Primary || tone == TactileTone.Selected
    val face by animateColorAsState(when {
        !enabled && tone == TactileTone.Default -> KotoColors.SoftGrey
        emphasized -> KotoColors.LessonBlue
        tone == TactileTone.Correct -> KotoColors.CorrectWash
        tone == TactileTone.Wrong -> KotoColors.WrongWash
        tone == TactileTone.Warning -> KotoColors.WarningWash
        tone == TactileTone.Quiet -> Color.White
        else -> KotoColors.BlueWash
    }, tween(180), label = "Answer feedback")
    val edge = when (tone) {
        TactileTone.Correct -> KotoColors.Correct
        TactileTone.Wrong -> KotoColors.Wrong
        TactileTone.Warning -> KotoColors.Warning
        TactileTone.Quiet -> KotoColors.Hairline
        else -> if (emphasized) KotoColors.Navy else KotoColors.BlueEdge
    }
    val ink = if (emphasized) Color.White else KotoColors.Navy
    val shape = RoundedCornerShape(17.dp)
    Box(modifier.padding(bottom = 4.dp)
        .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)
        .semantics(mergeDescendants = true) {
            this.selected = selected
            description?.let { contentDescription = it }
            stateLabel?.let { stateDescription = it }
        }, propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().graphicsLayer { translationY = edgeDisplacement.dp.toPx() }.background(edge, shape))
        Box(Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer { translationY = displacement.dp.toPx() }.clip(shape).background(face)
            .border(1.dp, edge.copy(alpha = .4f), shape)
            .padding(padding), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides ink, content = content)
        }
    }
}
