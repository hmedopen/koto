package com.koto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoDimens

@Composable
fun KotoBottomBar(
    selected: KotoDestination,
    onSelect: (KotoDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = KotoDestination.entries
    val interactions = remember { destinations.associateWith { MutableInteractionSource() } }
    // The surface extends under system navigation; only interactive content is inset.
    Box(
        modifier
            .fillMaxWidth()
            .background(KotoColors.BarSurface)
            .drawBehind { drawLine(KotoColors.Hairline, Offset.Zero, Offset(size.width, 0f), 1f) }
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        Row(
            Modifier
                .widthIn(max = KotoDimens.BarMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = KotoDimens.BarHorizontalPadding)
                .testTag("bottom_bar"),
        ) {
            destinations.forEach { destination ->
                KotoBottomBarItem(
                    destination = destination,
                    selected = destination == selected,
                    interactionSource = interactions.getValue(destination),
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
