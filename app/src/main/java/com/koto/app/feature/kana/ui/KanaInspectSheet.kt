package com.koto.app.feature.kana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.theme.KotoType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaInspectSheet(
    character: KanaCharacter,
    script: KanaScript,
    audio: JapaneseTtsController?,
    onDismiss: () -> Unit,
    onScriptChanged: (KanaScript) -> Unit,
    onSelectCharacter: (KanaCharacter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentChar = character.characterFor(script)
    val strokeSteps = character.strokeStepsFor(script)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = null,
        modifier = modifier.testTag("kana_inspect_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // 1. Top Header: 3D Tactile Circular [X] Dismiss Button & Script Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                // Top-Left: Raised, tactile 3D circular "X" button
                CardsPressable(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .align(Alignment.CenterStart)
                        .testTag("btn_close_kana_inspect"),
                    face = CardsColors.Surface,
                    depth = CardsColors.Edge,
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = CardsColors.Ink,
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Centered Script label ("Hiragana" or "Katakana")
                Text(
                    text = script.displayName,
                    style = KotoType.Brand,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardsColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("kana_inspect_script_label"),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 2. Romaji Callout: Bold Latin Title
                Text(
                    text = character.romaji.uppercase(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CardsColors.Ink,
                    modifier = Modifier.testTag("kana_inspect_romaji_title"),
                )

                // 3. Hero Glyph Surface (Pristine White Background - No Bounding Box Card)
                // Overlaid with numbered directional stroke arrows directly on character paths
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Central Kana Glyph in Solid Black (replacing red)
                    Text(
                        text = currentChar,
                        fontSize = if (currentChar.length > 1) 74.sp else 96.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalJapaneseFont.current,
                        color = CardsColors.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("kana_inspect_large_glyph"),
                    )

                    // Directional Stroke Arrows Overlaid Directly on Character Area
                    OverlaidStrokeArrows(strokeSteps = strokeSteps)
                }

                // 4. Secondary Action Row: Pronounce Audio + Tracing Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Audio Pronunciation Trigger
                    CardsPressable(
                        onClick = {
                            audio?.speak(
                                JapaneseText(
                                    kana = currentChar,
                                    romaji = character.romaji.lowercase(),
                                    tts = currentChar,
                                ),
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_kana_inspect_audio"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_speaker),
                                contentDescription = "Pronounce",
                                tint = CardsColors.Ink,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Tracing Action Button
                    CardsPressable(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_kana_inspect_trace"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = "Trace",
                                tint = CardsColors.Muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // 5. Standardized Bottom Navigation Toolbar: [<] [Center Script Flip Pill] [>]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left Arrow (<): Jumps to preceding kana
                    CardsPressable(
                        onClick = {
                            val prev = KanaRepository.findPrevious(character)
                            onSelectCharacter(prev)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_kana_inspect_prev"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Previous",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // Center Pill: Instant Script Flip Toggle (Hiragana ⇄ Katakana)
                    CardsPressable(
                        onClick = {
                            onScriptChanged(script.opposite())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .testTag("btn_kana_inspect_flip_script"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    ) {
                        val flipText = if (script == KanaScript.Hiragana) "Katakana" else "Hiragana"
                        Text(
                            text = flipText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardsColors.Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Right Arrow (>): Advances to next kana
                    CardsPressable(
                        onClick = {
                            val next = KanaRepository.findNext(character)
                            onSelectCharacter(next)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_kana_inspect_next"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Next",
                            tint = CardsColors.Ink,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = 180f },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Numbered directional stroke arrows overlaid directly onto the character path.
 * Renders directional cues (1 →, 2 ↓, etc.) at key spatial positions around the glyph canvas.
 */
@Composable
private fun BoxScope.OverlaidStrokeArrows(
    strokeSteps: List<com.koto.app.feature.kana.model.StrokeStep>,
) {
    val positions = listOf(
        Alignment.TopStart to Pair(18.dp, 16.dp),
        Alignment.TopCenter to Pair(0.dp, 8.dp),
        Alignment.TopEnd to Pair((-18).dp, 16.dp),
        Alignment.CenterStart to Pair(12.dp, 0.dp),
        Alignment.BottomStart to Pair(18.dp, (-14).dp),
        Alignment.BottomEnd to Pair((-18).dp, (-14).dp),
    )

    strokeSteps.take(6).forEachIndexed { index, step ->
        val (alignment, offset) = positions[index % positions.size]
        Row(
            modifier = Modifier
                .align(alignment)
                .offset(x = offset.first, y = offset.second)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "${step.stepNumber}",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF8D6E63),
            )
            Text(
                text = step.arrowSymbol,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8D6E63),
            )
        }
    }
}
