package com.koto.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoDimens
import com.koto.app.ui.theme.KotoMotion
import com.koto.app.ui.theme.KotoType

@Composable
internal fun KotoBottomBarItem(
    destination: KotoDestination,
    selected: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val compression = animateFloatAsState(
        if (pressed) 1f else 0f,
        KotoMotion.press(),
        label = "${destination.name} press",
    )
    val illustration = animateFloatAsState(
        if (selected) 1f else 0f,
        KotoMotion.iconActivation(selected),
        label = "${destination.name} illustration",
    )
    val color = if (selected) KotoColors.Navy else KotoColors.NavySoft

    // The entire slot is the target. Only the visual content moves, never its hit area.
    Column(
        modifier
            .testTag("tab_${destination.name.lowercase()}")
            .heightIn(min = KotoDimens.BarMinHeight)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .border(KotoDimens.FocusStroke, if (focused) KotoColors.Navy else Color.Transparent, RoundedCornerShape(KotoDimens.FocusRadius))
            .padding(
                horizontal = KotoDimens.ItemHorizontalPadding,
                vertical = KotoDimens.ItemVerticalPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KotoDimens.IconLabelGap),
    ) {
        Box(
            Modifier
                .size(KotoDimens.IconWellWidth, KotoDimens.IconWellHeight)
                .graphicsLayer {
                    translationY = KotoDimens.PressDisplacement.toPx() * compression.value
                    scaleX = 1f - (1f - KotoMotion.PressScale) * compression.value
                    scaleY = scaleX
                },
            contentAlignment = Alignment.Center,
        ) {
            KotoNavIcon(destination, color, illustration.value, Modifier.size(KotoDimens.IconSize))
        }
        Text(
            text = stringResource(destination.label),
            color = color,
            style = KotoType.Navigation,
            textAlign = TextAlign.Center,
            // No singleLine/fixed-height constraint: the bar can grow for accessible text.
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0.72f + 0.28f * illustration.value },
        )
    }
}
