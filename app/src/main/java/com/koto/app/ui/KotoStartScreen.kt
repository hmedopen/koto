package com.koto.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KotoEntryScreen() {
    val isTesting = remember {
        System.getProperty("robolectric.graphicsMode") != null ||
            android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }

    var showStartScreen by remember { mutableStateOf(!isTesting) }

    LaunchedEffect(Unit) {
        if (!isTesting) {
            // Choreographed 1450ms motion timeline before seamless dissolve
            delay(1450)
            showStartScreen = false
        }
    }

    AnimatedContent(
        targetState = showStartScreen,
        transitionSpec = {
            if (!targetState) {
                (fadeIn(tween(260, delayMillis = 40)) + scaleIn(tween(280), initialScale = 0.98f)).togetherWith(
                    fadeOut(tween(220)),
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
    // 1. Primary Hero Animation: Emblem Scale & Alpha
    val logoScale = remember { Animatable(0.76f) }
    val logoAlpha = remember { Animatable(0f) }

    // 2. Secondary Motion: Dual Concentric Acoustic Ripples
    val rippleScale1 = remember { Animatable(0.85f) }
    val rippleAlpha1 = remember { Animatable(0f) }
    val rippleScale2 = remember { Animatable(0.85f) }
    val rippleAlpha2 = remember { Animatable(0f) }

    // 3. Kinetic Typography: Title & Subtitle Stagger
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(16f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(12f) }

    // 4. Subtle Diagonal Gleam Progress across Emblem
    val gleamProgress = remember { Animatable(-0.3f) }

    LaunchedEffect(Unit) {
        // --- Hero Emblem Entrance ---
        launch {
            logoAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            // Branded overshoot: scales to 1.04f with custom ease, then settles to 1.0f
            logoScale.animateTo(
                targetValue = 1.04f,
                animationSpec = tween(380, easing = CubicBezierEasing(0.34f, 1.35f, 0.64f, 1f)),
            )
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
            )
        }

        // --- Concentric Resonance Wave 1 ---
        launch {
            delay(110)
            launch {
                rippleAlpha1.snapTo(0.40f)
                rippleAlpha1.animateTo(0f, tween(650, easing = FastOutSlowInEasing))
            }
            rippleScale1.animateTo(1.35f, tween(650, easing = FastOutSlowInEasing))
        }

        // --- Concentric Resonance Wave 2 ---
        launch {
            delay(220)
            launch {
                rippleAlpha2.snapTo(0.28f)
                rippleAlpha2.animateTo(0f, tween(680, easing = FastOutSlowInEasing))
            }
            rippleScale2.animateTo(1.52f, tween(680, easing = FastOutSlowInEasing))
        }

        // --- Kinetic Typography Stagger: "Koto" Title ---
        launch {
            delay(260)
            launch {
                titleAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            }
            titleOffsetY.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }

        // --- Kinetic Typography Stagger: Japanese Subtitle ---
        launch {
            delay(380)
            launch {
                subtitleAlpha.animateTo(0.85f, tween(320, easing = FastOutSlowInEasing))
            }
            subtitleOffsetY.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
        }

        // --- Micro Gleam Sheen Sweep ---
        launch {
            delay(600)
            gleamProgress.animateTo(1.3f, tween(550, easing = FastOutSlowInEasing))
        }
    }

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
            // Center emblem container with concentric resonance ripples
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Outer Ripple Ring 2 (Hairline)
                if (rippleAlpha2.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .graphicsLayer {
                                scaleX = rippleScale2.value
                                scaleY = rippleScale2.value
                                alpha = rippleAlpha2.value
                            }
                            .border(
                                width = 1.dp,
                                color = KotoColors.BlueEdge.copy(alpha = rippleAlpha2.value),
                                shape = CircleShape,
                            ),
                    )
                }

                // Inner Ripple Ring 1 (Hairline)
                if (rippleAlpha1.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .graphicsLayer {
                                scaleX = rippleScale1.value
                                scaleY = rippleScale1.value
                                alpha = rippleAlpha1.value
                            }
                            .border(
                                width = 1.dp,
                                color = KotoColors.LessonBlue.copy(alpha = rippleAlpha1.value),
                                shape = CircleShape,
                            ),
                    )
                }

                // Large engraved emblem on pure white with subtle depth and sheen
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Color(0x181A3761),
                            spotColor = Color(0x241A3761),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.koto_app_icon),
                        contentDescription = "Koto Logo",
                        modifier = Modifier
                            .size(220.dp)
                            .drawWithContent {
                                drawContent()
                                // Diagonal gleam sweep across emblem surface
                                val p = gleamProgress.value
                                if (p in -0.2f..1.2f) {
                                    val sizePx = size.width
                                    val xOffset = p * sizePx * 1.6f - sizePx * 0.3f
                                    val gleamBrush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.38f),
                                            Color.Transparent,
                                        ),
                                        start = Offset(xOffset, 0f),
                                        end = Offset(xOffset + sizePx * 0.35f, sizePx),
                                    )
                                    drawRect(brush = gleamBrush, blendMode = BlendMode.SrcAtop)
                                }
                            },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Primary Brand Typography
            Text(
                text = "Koto",
                style = KotoType.Brand,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = KotoColors.Navy,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = titleAlpha.value
                    }
                    .offset(y = titleOffsetY.value.dp)
                    .testTag("start_screen_title"),
            )

            Spacer(Modifier.height(4.dp))

            // Japanese Subtitle: Dedicated vertical block, Kana/Kanji integrity
            Text(
                text = "こと • 言葉",
                style = KotoType.Body,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.4.sp,
                color = KotoColors.NavySoft,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = subtitleAlpha.value
                    }
                    .offset(y = subtitleOffsetY.value.dp),
            )
        }
    }
}
