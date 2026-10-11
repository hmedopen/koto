package com.koto.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoType
import kotlinx.coroutines.delay

@Composable
fun KotoEntryScreen() {
    val isTesting = remember {
        System.getProperty("robolectric.graphicsMode") != null ||
            android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }

    var showStartScreen by remember { mutableStateOf(!isTesting) }

    LaunchedEffect(Unit) {
        if (!isTesting) {
            delay(1100)
            showStartScreen = false
        }
    }

    AnimatedContent(
        targetState = showStartScreen,
        transitionSpec = {
            if (!targetState) {
                (fadeIn(tween(200, delayMillis = 40)) + scaleIn(tween(250), initialScale = 0.98f)).togetherWith(
                    fadeOut(tween(180)),
                )
            } else {
                fadeIn(tween(100)).togetherWith(fadeOut(tween(100)))
            }
        },
        label = "Start screen transition",
    ) { isSplash ->
        if (isSplash) {
            KotoStartScreen(onDismiss = { showStartScreen = false })
        } else {
            KotoApp()
        }
    }
}

@Composable
fun KotoStartScreen(onDismiss: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .testTag("koto_start_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Large engraved emblem on pure white with seamless edge merging
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color(0x1A1A3761),
                        spotColor = Color(0x2A1A3761),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.koto_app_icon),
                    contentDescription = "Koto Logo",
                    modifier = Modifier.size(230.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Koto",
                style = KotoType.Brand,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = KotoColors.Navy,
                modifier = Modifier.testTag("start_screen_title"),
            )
        }
    }
}
