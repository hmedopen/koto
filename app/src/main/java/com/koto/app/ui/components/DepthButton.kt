package com.koto.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koto.app.ui.theme.KotoColors

/** A solid lower edge gives depth without a blurred/elevated shadow. */
@Composable
fun DepthButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val displacement = animateFloatAsState(if (pressed) 3f else 0f, tween(90), label = "Button depth")
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.padding(bottom = 4.dp)) {
        Box(Modifier.matchParentSize().graphicsLayer { translationY = 4.dp.toPx() }
            .background(if (enabled) KotoColors.NavyDepth else KotoColors.Hairline, shape))
        Box(Modifier.fillMaxWidth().graphicsLayer { translationY = displacement.value.dp.toPx() }
            .clip(shape).background(if (enabled) KotoColors.Navy else KotoColors.SoftGrey)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction,
                indication = ripple(), onClick = onClick)
            .heightIn(min = 52.dp).padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("level_action"), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge,
                color = if (enabled) KotoColors.Background else KotoColors.QuietInk)
        }
    }
}
