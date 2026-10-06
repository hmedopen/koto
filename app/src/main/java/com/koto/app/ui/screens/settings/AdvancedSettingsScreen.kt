package com.koto.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koto.app.R
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.theme.KotoFont
import com.koto.app.ui.theme.KotoType
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private val SquircleShape = RoundedCornerShape(8.dp)
private val DialogShape = RoundedCornerShape(12.dp)

@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdvancedSettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    AdvancedSettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onFuriganaChange = viewModel::setFuriganaDisplay,
        onRomajiChange = viewModel::setRomajiDisplay,
        onJapaneseFontChange = viewModel::setJapaneseFont,
        onEnglishFontChange = viewModel::setEnglishFont,
        onTtsChange = viewModel::setTextToSpeech,
        onSpeechSpeedChange = viewModel::setSpeechSpeed,
        onInstallVoicePack = { launchVoicePackInstaller(context) },
        onRevealFuriganaChange = viewModel::setRevealFuriganaOnTap,
        onKanjiLookupChange = viewModel::setKanjiLookupOnHold,
        onCardFlipChange = viewModel::setCardFlipAnimation,
        onRetryPolicyChange = viewModel::setRetryFailedCards,
        onAutoFillChange = viewModel::setAutoFillOtherSide,
        onAutoGenerateFuriganaChange = viewModel::setAutoGenerateFurigana,
        onClearCache = {
            scope.launch {
                val freedBytes = viewModel.clearCache()
                val freedFormatted = AdvancedSettingsViewModel.formatCacheSize(freedBytes)
                snackbarHostState.showSnackbar(
                    message = "Cache cleared ($freedFormatted freed)",
                    duration = SnackbarDuration.Short,
                )
            }
        },
        onOpenUrl = { url ->
            openExternalUrl(context, url)
        },
        modifier = modifier,
    )
}

@Composable
fun AdvancedSettingsContent(
    state: AdvancedSettingsState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFuriganaChange: (Boolean) -> Unit,
    onRomajiChange: (Boolean) -> Unit,
    onJapaneseFontChange: (String) -> Unit,
    onEnglishFontChange: (String) -> Unit,
    onTtsChange: (Boolean) -> Unit,
    onSpeechSpeedChange: (Float) -> Unit,
    onInstallVoicePack: () -> Unit,
    onRevealFuriganaChange: (Boolean) -> Unit,
    onKanjiLookupChange: (Boolean) -> Unit,
    onCardFlipChange: (Boolean) -> Unit,
    onRetryPolicyChange: (RetryFailedCardsPolicy) -> Unit,
    onAutoFillChange: (Boolean) -> Unit,
    onAutoGenerateFuriganaChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onBack() }

    var fontPickerTarget by remember { mutableStateOf<FontPickerType?>(null) }
    var displayedCacheBytes by remember { mutableFloatStateOf(state.cacheSizeBytes.toFloat()) }

    LaunchedEffect(state.cacheSizeBytes) {
        displayedCacheBytes = state.cacheSizeBytes.toFloat()
    }

    val animatedCacheBytes by animateFloatAsState(
        targetValue = displayedCacheBytes,
        animationSpec = tween(durationMillis = 400),
        label = "cache_bytes_animation",
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("advanced_settings_screen"),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    CardsPressable(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterStart)
                            .testTag("advanced_settings_back"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Text(
                        text = "Advanced Settings",
                        style = KotoType.Brand,
                        color = CardsColors.Ink,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("advanced_settings_title"),
                    )

                    Spacer(
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterEnd),
                    )
                }
                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)
            }
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // ================= 1. DISPLAY & TYPOGRAPHY =================
            CategoryHeader(title = "DISPLAY & TYPOGRAPHY")

            SettingsRow(
                title = "Furigana Display",
                testTag = "row_furigana_display",
            ) {
                BinaryToggleControl(
                    checked = state.furiganaDisplay,
                    onCheckedChange = onFuriganaChange,
                    testTag = "toggle_furigana_display",
                )
            }

            HairlineRowDivider()

            SettingsRow(
                title = "Romaji Display",
                testTag = "row_romaji_display",
            ) {
                BinaryToggleControl(
                    checked = state.romajiDisplay,
                    onCheckedChange = onRomajiChange,
                    testTag = "toggle_romaji_display",
                )
            }

            HairlineRowDivider()

            val currentJpFont = FontCatalog.findJapaneseFont(state.japaneseFontId)
            SettingsRow(
                title = "Japanese Font",
                testTag = "row_japanese_font",
            ) {
                FontBadgePill(
                    sampleGlyph = currentJpFont.sampleGlyph,
                    fontFamily = currentJpFont.fontFamily,
                    onClick = { fontPickerTarget = FontPickerType.JAPANESE },
                    testTag = "badge_japanese_font",
                )
            }

            HairlineRowDivider()

            val currentEnFont = FontCatalog.findEnglishFont(state.englishFontId)
            SettingsRow(
                title = "English Font",
                testTag = "row_english_font",
            ) {
                FontBadgePill(
                    sampleGlyph = currentEnFont.sampleGlyph,
                    fontFamily = currentEnFont.fontFamily,
                    onClick = { fontPickerTarget = FontPickerType.ENGLISH },
                    testTag = "badge_english_font",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================= 2. AUDIO & SPEECH =================
            CategoryHeader(title = "AUDIO & SPEECH")

            SettingsRow(
                title = "Text to Speech",
                testTag = "row_tts",
            ) {
                BinaryToggleControl(
                    checked = state.textToSpeech,
                    onCheckedChange = onTtsChange,
                    testTag = "toggle_tts",
                )
            }

            // Conditional Warning Card: Only if TTS == ON and engine lacks Japanese data
            if (state.textToSpeech && !state.isJapaneseVoiceAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                VoicePackWarningCard(
                    onInstallClick = onInstallVoicePack,
                    modifier = Modifier.testTag("card_voice_pack_warning"),
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                HairlineRowDivider()
            }

            SpeechSpeedControl(
                speed = state.speechSpeed,
                onSpeedChange = onSpeechSpeedChange,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ================= 3. STUDY & CARD INTERACTION =================
            CategoryHeader(title = "STUDY & CARD INTERACTION")

            SettingsRow(
                title = "Reveal Furigana on Tap",
                testTag = "row_reveal_furigana",
            ) {
                BinaryToggleControl(
                    checked = state.revealFuriganaOnTap,
                    onCheckedChange = onRevealFuriganaChange,
                    testTag = "toggle_reveal_furigana",
                )
            }

            HairlineRowDivider()

            SettingsRow(
                title = "Kanji Lookup on Hold",
                testTag = "row_kanji_lookup",
            ) {
                BinaryToggleControl(
                    checked = state.kanjiLookupOnHold,
                    onCheckedChange = onKanjiLookupChange,
                    testTag = "toggle_kanji_lookup",
                )
            }

            HairlineRowDivider()

            SettingsRow(
                title = "Card Flip Animation",
                testTag = "row_card_flip",
            ) {
                BinaryToggleControl(
                    checked = state.cardFlipAnimation,
                    onCheckedChange = onCardFlipChange,
                    testTag = "toggle_card_flip",
                )
            }

            HairlineRowDivider()

            SettingsRow(
                title = "Retry Failed Cards",
                testTag = "row_retry_failed",
            ) {
                SegmentedChoiceToggle(
                    options = listOf("SOON", "END"),
                    selectedIndex = if (state.retryFailedCards == RetryFailedCardsPolicy.SOON) 0 else 1,
                    onSelect = { index ->
                        onRetryPolicyChange(if (index == 0) RetryFailedCardsPolicy.SOON else RetryFailedCardsPolicy.END)
                    },
                    testTag = "toggle_retry_failed",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================= 4. CUSTOM DECK CREATION =================
            CategoryHeader(title = "CUSTOM DECK CREATION")

            SettingsRow(
                title = "Auto-Fill Other Side",
                testTag = "row_auto_fill",
            ) {
                BinaryToggleControl(
                    checked = state.autoFillOtherSide,
                    onCheckedChange = onAutoFillChange,
                    testTag = "toggle_auto_fill",
                )
            }

            HairlineRowDivider()

            SettingsRow(
                title = "Auto-Generate Furigana",
                testTag = "row_auto_generate",
            ) {
                BinaryToggleControl(
                    checked = state.autoGenerateFurigana,
                    onCheckedChange = onAutoGenerateFuriganaChange,
                    testTag = "toggle_auto_generate",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================= 5. STORAGE & DATA =================
            CategoryHeader(title = "STORAGE & DATA")

            SettingsRow(
                title = "Clear Cache",
                testTag = "row_clear_cache",
                modifier = Modifier.clickable {
                    displayedCacheBytes = 0f
                    onClearCache()
                },
            ) {
                val formattedSize = AdvancedSettingsViewModel.formatCacheSize(animatedCacheBytes.toLong())
                Text(
                    text = formattedSize,
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = CardsColors.Ink,
                    ),
                    modifier = Modifier.testTag("text_clear_cache_size"),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================= 6. ABOUT & LEGAL =================
            CategoryHeader(title = "ABOUT & LEGAL")

            SettingsRow(
                title = "App Version",
                testTag = "row_app_version",
            ) {
                Text(
                    text = "v1.0.0",
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = CardsColors.Muted,
                    ),
                )
            }

            HairlineRowDivider()

            SettingsRow(
                title = "Privacy Policy",
                testTag = "row_privacy_policy",
                modifier = Modifier.clickable {
                    onOpenUrl("https://koto.app/privacy")
                },
            ) {
                Text(
                    text = "↗",
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CardsColors.Blue,
                    ),
                )
            }

            HairlineRowDivider()

            SettingsRow(
                title = "Terms of Service",
                testTag = "row_terms_of_service",
                modifier = Modifier.clickable {
                    onOpenUrl("https://koto.app/terms")
                },
            ) {
                Text(
                    text = "↗",
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CardsColors.Blue,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 3x3 Font Picker Dialog
    fontPickerTarget?.let { type ->
        FontPickerDialog(
            type = type,
            selectedFontId = if (type == FontPickerType.JAPANESE) state.japaneseFontId else state.englishFontId,
            onFontSelected = { selectedId ->
                if (type == FontPickerType.JAPANESE) {
                    onJapaneseFontChange(selectedId)
                } else {
                    onEnglishFontChange(selectedId)
                }
                fontPickerTarget = null
            },
            onDismiss = { fontPickerTarget = null },
        )
    }
}

enum class FontPickerType {
    JAPANESE,
    ENGLISH;
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = TextStyle(
            fontFamily = KotoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            color = CardsColors.Muted,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    control: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = KotoFont,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = CardsColors.Ink,
            ),
            modifier = Modifier.weight(1f, fill = false),
        )

        Spacer(modifier = Modifier.width(16.dp))

        control()
    }
}

@Composable
private fun HairlineRowDivider() {
    HorizontalDivider(
        color = CardsColors.Edge.copy(alpha = 0.6f),
        thickness = 1.dp,
    )
}

@Composable
private fun BinaryToggleControl(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    SegmentedChoiceToggle(
        options = listOf("OFF", "ON"),
        selectedIndex = if (checked) 1 else 0,
        onSelect = { index -> onCheckedChange(index == 1) },
        testTag = testTag,
        modifier = modifier,
    )
}

@Composable
private fun SegmentedChoiceToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(SquircleShape)
            .border(1.dp, CardsColors.Edge, SquircleShape)
            .background(CardsColors.Surface)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(SquircleShape)
                    .background(if (isSelected) CardsColors.Blue else CardsColors.Surface)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isSelected) Color.White else CardsColors.Muted,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FontBadgePill(
    sampleGlyph: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(SquircleShape)
            .border(1.dp, CardsColors.Edge, SquircleShape)
            .background(CardsColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "{ $sampleGlyph }",
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CardsColors.Ink,
            ),
        )
    }
}

@Composable
private fun SpeechSpeedControl(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var lastSnappedAnchor by remember { mutableStateOf<Float?>(null) }
    val anchors = remember { listOf(0.75f, 1.0f, 1.25f) }
    val snapThreshold = 0.04f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("row_speech_speed"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Speech Speed",
                style = TextStyle(
                    fontFamily = KotoFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = CardsColors.Ink,
                ),
            )

            Text(
                text = String.format(Locale.US, "%.2fx", speed),
                style = TextStyle(
                    fontFamily = KotoFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = CardsColors.Ink,
                ),
                modifier = Modifier.testTag("text_speech_speed_value"),
            )
        }

        // Continuous slider from 0.5f to 2.0f with magnetic snapping
        Slider(
            value = speed,
            onValueChange = { rawValue ->
                var resolved = rawValue
                var isSnapped = false
                for (anchor in anchors) {
                    if (abs(rawValue - anchor) <= snapThreshold) {
                        resolved = anchor
                        isSnapped = true
                        break
                    }
                }

                if (isSnapped && lastSnappedAnchor != resolved) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastSnappedAnchor = resolved
                } else if (!isSnapped) {
                    lastSnappedAnchor = null
                }

                onSpeedChange(resolved)
            },
            valueRange = 0.5f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = CardsColors.Blue,
                activeTrackColor = CardsColors.Blue,
                inactiveTrackColor = CardsColors.Edge,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_speech_speed"),
        )

        // Fixed tick anchors: 0.75x, 1.0x, 1.25x
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0.75x",
                style = TextStyle(
                    fontFamily = KotoFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (abs(speed - 0.75f) < 0.02f) CardsColors.Blue else CardsColors.Muted,
                ),
            )
            Text(
                text = "1.0x",
                style = TextStyle(
                    fontFamily = KotoFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (abs(speed - 1.0f) < 0.02f) CardsColors.Blue else CardsColors.Muted,
                ),
            )
            Text(
                text = "1.25x",
                style = TextStyle(
                    fontFamily = KotoFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (abs(speed - 1.25f) < 0.02f) CardsColors.Blue else CardsColors.Muted,
                ),
            )
        }
    }
}

@Composable
private fun VoicePackWarningCard(
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .border(1.dp, CardsColors.Coral.copy(alpha = 0.7f), SquircleShape)
            .background(CardsColors.Surface)
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "⚠",
                    fontSize = 16.sp,
                    color = CardsColors.Coral,
                )
                Text(
                    text = "Japanese Voice Pack Missing",
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = CardsColors.Coral,
                    ),
                )
            }

            CardsPressable(
                onClick = onInstallClick,
                face = CardsColors.Surface,
                depth = CardsColors.Edge,
                padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_install_voice_pack"),
            ) {
                Text(
                    text = "Install Voice Pack",
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CardsColors.Coral,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FontPickerDialog(
    type: FontPickerType,
    selectedFontId: String,
    onFontSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val fonts = if (type == FontPickerType.JAPANESE) FontCatalog.japaneseFonts else FontCatalog.englishFonts
    val dialogTitle = if (type == FontPickerType.JAPANESE) "Japanese Font" else "English Font"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DialogShape)
                    .border(1.dp, CardsColors.Edge, DialogShape)
                    .background(Color.White)
                    .testTag("font_picker_dialog"),
            ) {
                // Symmetrical Dialog Top Bar with Top-Right [X] (Design Rule 6)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.size(40.dp).align(Alignment.CenterStart))

                    Text(
                        text = dialogTitle,
                        style = KotoType.Brand,
                        fontSize = 18.sp,
                        color = CardsColors.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    CardsPressable(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                            .testTag("font_picker_close"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close font picker",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                // 3x3 Grid of 9 fonts
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(fonts, key = { it.id }) { font ->
                        val isSelected = font.id == selectedFontId
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(SquircleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CardsColors.Blue else CardsColors.Edge,
                                    shape = SquircleShape,
                                )
                                .background(CardsColors.Surface)
                                .clickable { onFontSelected(font.id) }
                                .padding(8.dp)
                                .testTag("font_item_${font.id}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = font.sampleGlyph,
                                    style = TextStyle(
                                        fontFamily = font.fontFamily,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CardsColors.Blue else CardsColors.Ink,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = font.name,
                                    style = TextStyle(
                                        fontFamily = font.fontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CardsColors.Blue else CardsColors.Ink,
                                        textAlign = TextAlign.Center,
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun launchVoicePackInstaller(context: Context) {
    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent("com.android.settings.TTS_SETTINGS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(installIntent)
    } catch (_: Exception) {
        try {
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            // Handled
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Handled
    }
}
