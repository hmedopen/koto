package com.koto.app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.audio.SpeechStatus
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.screens.settings.DisplayMode
import com.koto.app.ui.screens.settings.DisplayPreferences
import com.koto.app.ui.screens.settings.RubyToken
import com.koto.app.ui.theme.KotoType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(
    onDismiss: () -> Unit,
    onOpenAdvanced: () -> Unit = {},
    audio: JapaneseTtsController = JapaneseTtsController.get(LocalContext.current),
    preferences: DisplayPreferences = DisplayPreferences.get(LocalContext.current),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_sheet"),
        ) {
            // 1. Header Layout: Centered "Settings" Title & Top-Right [✕] Close Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                // Left spacer to mathematically center title
                Spacer(modifier = Modifier.size(44.dp).align(Alignment.CenterStart))

                // Centered "Settings" Title
                Text(
                    text = "Settings",
                    style = KotoType.Brand,
                    fontSize = 20.sp,
                    color = CardsColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("settings_title")
                        .semantics { heading() },
                )

                // Top-Right [✕] Close Button (Design Rule 6)
                CardsPressable(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterEnd)
                        .testTag("settings_close"),
                    face = CardsColors.Surface,
                    depth = CardsColors.Edge,
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close settings",
                        tint = CardsColors.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

            // Content Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 2. Japanese Display Preference Cards (Two Squares)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "How do you prefer to display Japanese text?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardsColors.Ink,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Kana Card
                        DisplayPreferenceSquareCard(
                            title = "Kana",
                            previewContent = {
                                Text(
                                    text = "みず",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (preferences.displayMode == DisplayMode.KANA) CardsColors.Blue else CardsColors.Ink,
                                )
                            },
                            subtitle = "Plain Kana",
                            isSelected = preferences.displayMode == DisplayMode.KANA,
                            onClick = { preferences.setDisplayMode(DisplayMode.KANA) },
                            modifier = Modifier.weight(1f),
                            tag = "settings_display_kana",
                        )

                        // Kanji Card (Kanji with reading assist / Furigana)
                        DisplayPreferenceSquareCard(
                            title = "Kanji",
                            previewContent = {
                                com.koto.app.ui.components.RubyText(
                                    tokens = listOf(RubyToken("水", "みず")),
                                    mode = DisplayMode.KANJI_FURIGANA,
                                    baseFontSize = 22.sp,
                                    baseColor = if (preferences.displayMode.isKanji) CardsColors.Blue else CardsColors.Ink,
                                    furiganaColor = CardsColors.Blue,
                                    horizontalArrangement = Arrangement.Center,
                                )
                            },
                            subtitle = "Kanji + Reading",
                            isSelected = preferences.displayMode.isKanji,
                            onClick = { preferences.setDisplayMode(DisplayMode.KANJI_FURIGANA) },
                            modifier = Modifier.weight(1f),
                            tag = "settings_display_kanji",
                        )
                    }
                }

                HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.6f), thickness = 1.dp)

                // 3. Romaji Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Romaji Pronunciation",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardsColors.Ink,
                    )

                    QuickSettingsRowToggle(
                        checked = preferences.romajiEnabled,
                        onCheckedChange = { preferences.setRomajiEnabled(it) },
                        testTag = "settings_romaji",
                    )
                }

                HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.6f), thickness = 1.dp)

                // 4. Text to Speech Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Text to Speech",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardsColors.Ink,
                    )

                    QuickSettingsRowToggle(
                        checked = audio.enabled,
                        onCheckedChange = { audio.setSpeechEnabled(it) },
                        testTag = "settings_sound",
                    )
                }

                HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.6f), thickness = 1.dp)

                // 4. Advanced Settings Button (Outline / tonal tactile button)
                CardsPressable(
                    onClick = {
                        onDismiss()
                        onOpenAdvanced()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_advanced"),
                    face = CardsColors.Surface,
                    depth = CardsColors.Edge,
                    padding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                            tint = CardsColors.Blue,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Advanced",
                            color = CardsColors.Blue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Selectable Square Card for Japanese Display Preferences (Kana vs Kanji).
 * Uses pure white background, crisp squircle geometry (12dp), and highlighted border when selected.
 */
@Composable
private fun DisplayPreferenceSquareCard(
    title: String,
    previewContent: @Composable () -> Unit,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String,
) {
    val shape = RoundedCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    val displacement = if (isPressed) 3.dp else 0.dp

    Box(
        modifier = modifier
            .padding(bottom = 4.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag(tag)
            .semantics {
                selected = isSelected
                role = Role.RadioButton
            },
    ) {
        // 3D Depth layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { translationY = 4.dp.toPx() }
                .background(if (isSelected) CardsColors.BlueDepth else CardsColors.Edge, shape),
        )

        // Face layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 114.dp)
                .graphicsLayer { translationY = displacement.toPx() }
                .clip(shape)
                .background(CardsColors.Surface)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) CardsColors.Blue else CardsColors.Edge,
                    shape = shape,
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) CardsColors.Blue else CardsColors.Ink,
                    textAlign = TextAlign.Center,
                )

                Box(
                    modifier = Modifier.height(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    previewContent()
                }

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isSelected) CardsColors.Blue else CardsColors.Muted,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun QuickSettingsRowToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val displacement = if (isPressed) 2.dp else 0.dp

    Box(
        modifier = modifier
            .padding(bottom = 2.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            )
            .testTag(testTag)
            .semantics {
                role = Role.Switch
                selected = checked
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            },
    ) {
        // 3D Depth layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { translationY = 3.dp.toPx() }
                .background(if (checked) CardsColors.BlueDepth else CardsColors.Edge, shape),
        )

        // Face layer
        Box(
            modifier = Modifier
                .widthIn(min = 68.dp)
                .heightIn(min = 34.dp)
                .graphicsLayer { translationY = displacement.toPx() }
                .clip(shape)
                .background(if (checked) CardsColors.Blue else CardsColors.Surface)
                .border(
                    width = 1.dp,
                    color = if (checked) CardsColors.BlueDepth else CardsColors.Edge,
                    shape = shape,
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (checked) "ON" else "OFF",
                color = if (checked) Color.White else CardsColors.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
