package com.koto.app.ui.screens.map

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors

@Composable
internal fun StageLabel(@StringRes title: Int) {
    Box(Modifier.fillMaxWidth().heightIn(min = 72.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(1.dp).height(72.dp).background(KotoColors.Hairline))
        Text(stringResource(title), color = KotoColors.QuietInk,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(KotoColors.SoftGrey)
                .padding(horizontal = 18.dp, vertical = 9.dp).semantics { heading() })
    }
}

// Each lazy row draws just a few simple shapes, never a map-sized canvas.
@Composable
internal fun LevelRow(level: MapLevel, onOpen: (Int) -> Unit, isLast: Boolean) {
    Box(Modifier.widthIn(max = 360.dp).fillMaxWidth().height(124.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(1.dp).fillMaxHeight(if (isLast) .5f else 1f)
            .align(Alignment.TopCenter).background(KotoColors.Hairline))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            AdjacentBranch(level.left, level, Modifier.weight(1f), before = true)
            MainLevelNode(level, onOpen)
            AdjacentBranch(level.right, level, Modifier.weight(1f), before = false)
        }
    }
}

@Composable
private fun MainLevelNode(level: MapLevel, onOpen: (Int) -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed) .95f else 1f, tween(100), label = "Level press")
    val current = level.state == LevelState.Current
    val locked = level.state == LevelState.Locked
    val completed = level.state == LevelState.Completed
    val description = stringResource(R.string.map_level, level.displayNumber)
    val stateLabel = stringResource(level.state.label)
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        if (current) Box(Modifier.size(86.dp).border(2.dp, KotoColors.Gold, CircleShape))
        Column(
            Modifier.size(72.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .clip(CircleShape)
                .background(when { locked -> KotoColors.Background; completed -> KotoColors.SoftGrey; else -> KotoColors.Navy })
                .border(1.dp, if (locked || completed) KotoColors.Hairline else KotoColors.Navy, CircleShape)
                .clickable(interactionSource = interactions, indication = ripple(), role = Role.Button) { onOpen(level.number) }
                .testTag("level_${level.number}")
                .semantics(mergeDescendants = true) { contentDescription = description; stateDescription = stateLabel },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(level.displayNumber, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = when { locked -> KotoColors.Locked; completed -> KotoColors.Navy; else -> KotoColors.Background },
                modifier = Modifier.clearAndSetSemantics {})
            if (completed) Icon(painterResource(R.drawable.ic_check), null,
                tint = KotoColors.Navy, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AdjacentBranch(type: AdjacentType?, level: MapLevel, modifier: Modifier, before: Boolean) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (before) Arrangement.End else Arrangement.Start) {
        if (type != null) {
            if (!before) Connector()
            AdjacentNode(type, level)
            if (before) Connector()
        }
    }
}

@Composable
private fun Connector() = Box(Modifier.width(28.dp).height(1.dp).background(KotoColors.Hairline))

@Composable
private fun AdjacentNode(type: AdjacentType, level: MapLevel) {
    val description = stringResource(R.string.map_adjacent_description,
        stringResource(type.label), level.displayNumber)
    val locked = level.state == LevelState.Locked
    val color = if (locked) KotoColors.Locked else if (type == AdjacentType.Review) KotoColors.Gold else KotoColors.QuietInk
    Box(Modifier.size(44.dp).semantics { contentDescription = description }
        .testTag("adjacent_${level.number}_${type.name}"), contentAlignment = Alignment.Center) {
        when (type) {
            AdjacentType.Review -> Icon(painterResource(R.drawable.ic_review_star), null, tint = color, modifier = Modifier.size(32.dp))
            AdjacentType.Mini -> Box(Modifier.size(25.dp).rotate(45f).border(1.5.dp, color, RoundedCornerShape(3.dp)))
            AdjacentType.PhaseTwo -> Box(Modifier.size(34.dp).border(1.5.dp, color, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(25.dp).border(1.5.dp, color, androidx.compose.foundation.shape.CircleShape))
            }
            AdjacentType.Special -> Box(Modifier.size(30.dp).border(1.5.dp, color, RoundedCornerShape(8.dp)))
        }
    }
}
