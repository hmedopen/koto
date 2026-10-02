package com.koto.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoDimens
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

    // Bouncy spring activation for lively pop and tactile settling
    val illustration by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = if (selected) 0.58f else 0.85f,
            stiffness = if (selected) 340f else 480f,
        ),
        label = "${destination.name} illustration",
    )

    // Snappy tactile compression on touch down, spring release on touch up
    val compression by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) {
            tween(durationMillis = 75, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "${destination.name} press",
    )

    val animatedIconColor by animateColorAsState(
        targetValue = if (selected) KotoColors.Navy else KotoColors.NavySoft,
        animationSpec = tween(durationMillis = 180),
        label = "${destination.name} icon color",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) KotoColors.Navy else KotoColors.NavySoft.copy(alpha = 0.72f),
        animationSpec = tween(durationMillis = 180),
        label = "${destination.name} text color",
    )

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
            .border(
                KotoDimens.FocusStroke,
                if (focused) KotoColors.Navy else Color.Transparent,
                RoundedCornerShape(KotoDimens.FocusRadius),
            )
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
                    // Tactile mechanical keypress depression
                    translationY = (KotoDimens.PressDisplacement.toPx() + 1.2f) * compression
                    scaleX = (1f - 0.06f * compression) * (1f + 0.05f * (illustration - 0.5f).coerceAtLeast(0f))
                    scaleY = (1f - 0.12f * compression) * (1f + 0.05f * (illustration - 0.5f).coerceAtLeast(0f))
                },
            contentAlignment = Alignment.Center,
        ) {
            // Tactile Active Pill / Capsule behind the icon
            if (illustration > 0.005f) {
                val pillShape = RoundedCornerShape(17.dp)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = illustration.coerceIn(0f, 1f)
                            val s = 0.76f + 0.24f * illustration
                            scaleX = s
                            scaleY = s
                        }
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF3F8FE),
                                    Color(0xFFE2EDFB),
                                ),
                            ),
                            shape = pillShape,
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFCBE0F5).copy(alpha = illustration.coerceIn(0f, 1f)),
                            shape = pillShape,
                        )
                        .drawBehind {
                            val strokeW = 1.6.dp.toPx()
                            val r = 17.dp.toPx()
                            drawLine(
                                color = Color(0xFFB5CCE8).copy(alpha = illustration.coerceIn(0f, 1f)),
                                start = Offset(r, size.height - strokeW / 2),
                                end = Offset(size.width - r, size.height - strokeW / 2),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round,
                            )
                        },
                )
            }

            // Lively micro-bounce when activating
            KotoNavIcon(
                destination = destination,
                color = animatedIconColor,
                progress = illustration,
                modifier = Modifier
                    .size(KotoDimens.IconSize)
                    .graphicsLayer {
                        val bounce = if (illustration > 0f && illustration < 1f) {
                            1f + 0.14f * kotlin.math.sin(illustration * Math.PI.toFloat())
                        } else {
                            1f
                        }
                        scaleX = bounce
                        scaleY = bounce
                    },
            )
        }

        Text(
            text = stringResource(destination.label),
            color = animatedTextColor,
            style = KotoType.Navigation,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 1f + 0.04f * illustration
                    scaleX = s
                    scaleY = s
                    alpha = 0.68f + 0.32f * illustration
                },
        )

        // Active indicator pip beneath the text label
        Box(
            Modifier
                .height(2.5.dp)
                .width(14.dp)
                .graphicsLayer {
                    alpha = illustration.coerceIn(0f, 1f)
                    scaleX = illustration.coerceIn(0f, 1f)
                }
                .background(
                    color = KotoColors.Navy,
                    shape = RoundedCornerShape(1.25.dp),
                ),
        )
    }
}
