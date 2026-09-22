package com.koto.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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

enum class TactileTone { Default, Primary, Selected, Correct, Wrong, Quiet }

/** Shared solid face and 4 dp edge, used by answers, tiles, utility and action buttons. */
@Composable
fun TactileButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    tone: TactileTone = TactileTone.Default, selected: Boolean = false,
    description: String? = null, stateLabel: String? = null,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val displacement by animateFloatAsState(if (pressed) 3f else 0f, tween(90), label = "Tactile depth")
    val emphasized = tone == TactileTone.Primary || tone == TactileTone.Selected
    val face by animateColorAsState(when {
        !enabled && tone == TactileTone.Default -> KotoColors.SoftGrey
        emphasized -> KotoColors.LessonBlue
        tone == TactileTone.Correct -> KotoColors.CorrectWash
        tone == TactileTone.Wrong -> KotoColors.WrongWash
        tone == TactileTone.Quiet -> Color.White
        else -> KotoColors.BlueWash
    }, tween(180), label = "Answer feedback")
    val edge = when (tone) {
        TactileTone.Correct -> KotoColors.Correct
        TactileTone.Wrong -> KotoColors.Wrong
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
        Box(Modifier.matchParentSize().graphicsLayer { translationY = 4.dp.toPx() }.background(edge, shape))
        Box(Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer { translationY = displacement.dp.toPx() }.clip(shape).background(face)
            .border(1.dp, edge.copy(alpha = .4f), shape)
            .padding(padding), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides ink, content = content)
        }
    }
}
