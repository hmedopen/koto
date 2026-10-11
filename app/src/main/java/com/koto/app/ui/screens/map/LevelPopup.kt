package com.koto.app.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koto.app.R
import com.koto.app.ui.screens.cards.CardsButton
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.theme.KotoColors

@Composable
internal fun LevelPopup(level: MapLevel, onDismiss: () -> Unit, onPlay: () -> Unit) {
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    var playRequested by remember { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismiss)
    val play by rememberUpdatedState(onPlay)
    // Keep the dialog attached until its exit finishes, including Back/outside dismissal.
    LaunchedEffect(visibility.isIdle, visibility.currentState, visibility.targetState) {
        if (visibility.isIdle && !visibility.currentState && !visibility.targetState) {
            dismiss()
            if (playRequested) play()
        }
    }
    Dialog(onDismissRequest = { visibility.targetState = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(visibleState = visibility,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = .96f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = .96f)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CardsColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardsColors.Edge),
                modifier = Modifier.padding(24.dp).widthIn(max = 340.dp).fillMaxWidth().testTag("level_popup"),
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.map_level, level.displayNumber),
                            style = MaterialTheme.typography.titleLarge, color = CardsColors.Ink,
                            modifier = Modifier.weight(1f).semantics { heading() })
                        IconButton(onClick = { visibility.targetState = false }, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.map_close),
                                tint = CardsColors.Ink, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(level.lessonTitle?.let { "$it · ${level.questionCount} questions" } ?: "More lessons are coming later.", color = CardsColors.Muted,
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    level.preview?.let { preview ->
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = preview.kana,
                            color = CardsColors.Ink,
                            fontSize = 28.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontFamily = LocalJapaneseFont.current,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("kana_preview"),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = preview.romaji,
                            color = CardsColors.Muted,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (level.state == LevelState.Locked) {
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.map_locked_hint), style = MaterialTheme.typography.labelMedium,
                            color = CardsColors.Muted, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(24.dp))
                    CardsButton(
                        label = stringResource(if (level.state == LevelState.Completed) R.string.map_replay else R.string.map_play),
                        onClick = { playRequested = true; visibility.targetState = false },
                        enabled = level.state != LevelState.Locked && visibility.targetState,
                        background = CardsColors.Blue,
                        ink = androidx.compose.ui.graphics.Color.White,
                        depth = CardsColors.BlueDepth,
                        modifier = Modifier.fillMaxWidth().testTag("level_action"),
                    )
                }
            }
        }
    }
}
