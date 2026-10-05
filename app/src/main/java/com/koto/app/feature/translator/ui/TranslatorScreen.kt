package com.koto.app.feature.translator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.translator.audio.TranslatorTtsController
import com.koto.app.feature.translator.data.MlKitTranslationEngine
import com.koto.app.feature.translator.data.TranslationHistoryStore
import com.koto.app.feature.translator.data.TranslatorCardStore
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.screens.map.SettingsSheet
import com.koto.app.ui.theme.KotoType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TranslatorScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val cardStore = remember(context) { TranslatorCardStore(context) }
    val historyStore = remember(context) { TranslationHistoryStore(context) }
    val tts = remember(context) { TranslatorTtsController.get(context) }
    val translationEngine = remember(context) { MlKitTranslationEngine.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sourceLang by rememberSaveable { mutableStateOf(TranslationLanguage.English) }
    var targetLang by rememberSaveable { mutableStateOf(TranslationLanguage.Japanese) }
    var inputText by rememberSaveable { mutableStateOf("") }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var translatedRomaji by rememberSaveable { mutableStateOf("") }
    var isStarred by rememberSaveable { mutableStateOf(false) }
    var isTranslating by rememberSaveable { mutableStateOf(false) }
    var skipDebounceNext by rememberSaveable { mutableStateOf(false) }
    var swapRotation by rememberSaveable { mutableStateOf(0f) }
    var showSavedDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showInternalSettings by rememberSaveable { mutableStateOf(false) }
    var savedCards by remember { mutableStateOf(cardStore.loadStarredCards()) }
    var historyItems by remember { mutableStateOf(historyStore.getHistory()) }
    var playingSpeakerTag by remember { mutableStateOf<String?>(null) }

    val inputFocusRequester = remember { FocusRequester() }

    // Model Pack Management: Check if Japanese offline pack (~60MB) is downloaded.
    LaunchedEffect(Unit) {
        val isReady = translationEngine.checkModelStatus()
        if (!isReady) {
            translationEngine.downloadModel()
        }
    }

    // Synchronize translation with input text, language direction, and 1.0s debounce
    LaunchedEffect(inputText, sourceLang, targetLang) {
        val query = inputText.trim()
        if (query.isBlank()) {
            translatedText = ""
            translatedRomaji = ""
            isStarred = false
            isTranslating = false
            return@LaunchedEffect
        }

        isTranslating = true

        if (!skipDebounceNext) {
            delay(1000L) // STRICT 1.0s Debounce
        } else {
            skipDebounceNext = false
        }

        val result = translationEngine.translate(query, sourceLang, targetLang)
        translatedText = result.translatedText
        translatedRomaji = result.romaji
        isStarred = cardStore.isStarred(query, result.translatedText)
        isTranslating = false

        if (result.translatedText.isNotBlank()) {
            historyStore.recordQuery(
                sourceText = query,
                targetText = result.translatedText,
                targetRomaji = result.romaji,
                sourceLang = sourceLang,
                targetLang = targetLang,
            )
            historyItems = historyStore.getHistory()
        }
    }

    fun showToast(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    fun copyToClipboard(text: String, label: String = "Text") {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        showToast("$label copied to clipboard")
    }

    fun swapLanguages() {
        swapRotation += 180f
        val tempLang = sourceLang
        sourceLang = targetLang
        targetLang = tempLang

        if (inputText.isNotBlank() && translatedText.isNotBlank()) {
            val oldTranslated = translatedText
            skipDebounceNext = true
            inputText = oldTranslated
        }
    }

    fun toggleStar() {
        if (inputText.isBlank() || translatedText.isBlank()) return
        val currentlyStarred = cardStore.toggleStar(
            sourceText = inputText,
            targetText = translatedText,
            targetRomaji = translatedRomaji,
            sourceLang = sourceLang,
            targetLang = targetLang,
        )
        isStarred = currentlyStarred
        savedCards = cardStore.loadStarredCards()
        showToast(if (currentlyStarred) "Saved to Starred Cards" else "Removed from Starred Cards")
    }

    fun clearInput() {
        inputText = ""
        translatedText = ""
        translatedRomaji = ""
        isStarred = false
        isTranslating = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val isActiveMode = inputText.isNotBlank()

    // Back button behavior
    BackHandler {
        when {
            showHistoryDialog -> showHistoryDialog = false
            showSavedDialog -> showSavedDialog = false
            showInternalSettings -> showInternalSettings = false
            isActiveMode -> clearInput()
            else -> onDismiss()
        }
    }

    val animatedSwapRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SwapRotation",
    )

    val starScale by animateFloatAsState(
        targetValue = if (isStarred) 1.2f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "StarScale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .testTag("translator_screen"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // UNIVERSAL SYMMETRICAL TOP APP BAR (2-Title-2 Standard, Module 4)
            // Layout: [Back Arrow] [History Button] | Translate | [Star Button] [Settings Button]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp),
            ) {
                // Left Side: [Back] [History]
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardsPressable(
                        onClick = {
                            if (isActiveMode) clearInput() else onDismiss()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag(if (isActiveMode) "translator_active_back_button" else "translator_dismiss_button"),
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

                    CardsPressable(
                        onClick = {
                            historyItems = historyStore.getHistory()
                            showHistoryDialog = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("translator_history_button"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_document),
                            contentDescription = "History",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Center: 'Translate' (1-word clean title)
                Text(
                    text = "Translate",
                    style = KotoType.Brand,
                    color = CardsColors.Ink,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("translator_header_title"),
                )

                // Right Side: [Star] [Settings]
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardsPressable(
                        onClick = {
                            savedCards = cardStore.loadStarredCards()
                            showSavedDialog = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("translator_view_starred_button"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(if (savedCards.isNotEmpty()) R.drawable.ic_review_star else R.drawable.ic_star_outline),
                            contentDescription = "Starred Cards",
                            tint = if (savedCards.isNotEmpty()) CardsColors.Yellow else CardsColors.Ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    CardsPressable(
                        onClick = {
                            if (onSettings != null) {
                                onSettings()
                            } else {
                                showInternalSettings = true
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("translator_overflow_button"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

            // CONTENT AREA
            // UNIFIED SEAMLESS CONTENT AREA: Persistent TextField prevents keyboard drop
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // SOURCE SECTION (Glued spatial slot)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = sourceLang.displayName.uppercase(),
                        color = CardsColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("translator_source_label"),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("translator_active_source_text"),
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inputFocusRequester)
                                .padding(end = if (inputText.isNotEmpty()) 40.dp else 0.dp)
                                .testTag("translator_idle_input_field"),
                            textStyle = TextStyle(
                                color = CardsColors.Ink,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 30.sp,
                            ),
                            cursorBrush = SolidColor(CardsColors.Blue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = "Translate text",
                                        color = CardsColors.Muted.copy(alpha = 0.6f),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Normal,
                                    )
                                }
                                innerTextField()
                            },
                        )

                        if (inputText.isNotEmpty()) {
                            CardsPressable(
                                onClick = { clearInput() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .align(Alignment.CenterEnd)
                                    .testTag("translator_clear_button"),
                                face = CardsColors.Surface,
                                depth = CardsColors.Edge,
                                padding = PaddingValues(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Clear input",
                                    tint = CardsColors.Ink,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Aux Controls under input field
                    if (!isActiveMode) {
                        // Paste button if clipboard contains text (Tactile 3D CardsPressable)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        if (clipText.isNotBlank()) {
                            CardsPressable(
                                onClick = { inputText = clipText },
                                modifier = Modifier.testTag("translator_paste_button"),
                                face = CardsColors.Surface,
                                depth = CardsColors.Edge,
                                padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_copy),
                                        contentDescription = null,
                                        tint = CardsColors.Blue,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Paste from clipboard",
                                        color = CardsColors.Blue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    } else {
                        // Source Audio, Copy & Star (Star button placed on the top section only)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TranslatorAudioButton(
                                onClick = {
                                    playingSpeakerTag = "source"
                                    tts.speak(inputText, sourceLang)
                                },
                                isPlaying = tts.isSpeaking && playingSpeakerTag == "source",
                                tint = CardsColors.Ink,
                                tag = "translator_source_audio",
                                description = "Speak source text",
                            )

                            TranslatorCopyButton(
                                onClick = { copyToClipboard(inputText, "Source text") },
                                tint = CardsColors.Ink,
                                tag = "translator_source_copy",
                                description = "Copy source text",
                            )

                            TranslatorStarButton(
                                onClick = { toggleStar() },
                                isStarred = isStarred,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = starScale
                                    scaleY = starScale
                                },
                                tag = "translator_star_toggle_button",
                                description = if (isStarred) "Unstar translation" else "Star translation",
                            )
                        }
                    }
                }

                if (isActiveMode) {
                    HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                    // TARGET SECTION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = targetLang.displayName.uppercase(),
                            color = CardsColors.Blue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("translator_target_label"),
                        )

                        // Output Block: Skeleton Shimmer vs Translation Output
                        if (isTranslating) {
                            TranslatorSkeletonShimmer(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .testTag("translator_skeleton_shimmer"),
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("translator_target_block"),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = translatedText,
                                    color = CardsColors.Ink,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 32.sp,
                                    modifier = Modifier.testTag("translator_target_text"),
                                )

                                if (translatedRomaji.isNotBlank()) {
                                    Text(
                                        text = translatedRomaji,
                                        color = CardsColors.Blue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.testTag("translator_target_romaji"),
                                    )
                                }
                            }
                        }

                        // Target Audio & Copy (Bloat eliminated: thumbs up/down/share removed, star on top only)
                        if (!isTranslating && translatedText.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TranslatorAudioButton(
                                    onClick = {
                                        playingSpeakerTag = "target"
                                        tts.speak(translatedText, targetLang)
                                    },
                                    isPlaying = tts.isSpeaking && playingSpeakerTag == "target",
                                    tint = CardsColors.Blue,
                                    tag = "translator_target_audio",
                                    description = "Speak translation",
                                )

                                TranslatorCopyButton(
                                    onClick = { copyToClipboard(translatedText, "Translation") },
                                    tint = CardsColors.Ink,
                                    tag = "translator_target_copy",
                                    description = "Copy translation",
                                )
                            }
                        }
                    }
                }
            }

            // KEYBOARD-ANCHORED FLOATING TOGGLE BAR (Modules 2 & 3)
            // Floats smoothly right above the soft keyboard using Modifier.imePadding()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
            ) {
                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("translator_bottom_language_bar"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Left: Source Language (tactile clickable)
                    CardsPressable(
                        onClick = { swapLanguages() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("translator_source_lang_text"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                    ) {
                        Text(
                            text = sourceLang.displayName,
                            color = CardsColors.Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Center: ⇄ Swap button
                    CardsPressable(
                        onClick = { swapLanguages() },
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                rotationZ = animatedSwapRotation
                            }
                            .testTag("translator_swap_languages_button"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_swap),
                            contentDescription = "Swap Languages",
                            tint = CardsColors.Blue,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Right: Target Language (tactile clickable)
                    CardsPressable(
                        onClick = { swapLanguages() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("translator_target_lang_text"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                    ) {
                        Text(
                            text = targetLang.displayName,
                            color = CardsColors.Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // Starred Cards Inspection Dialog (Bookmarks)
        AnimatedVisibility(
            visible = showSavedDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            SavedTranslationsDialog(
                cards = savedCards,
                onDismiss = {
                    showSavedDialog = false
                    savedCards = cardStore.loadStarredCards()
                    isStarred = cardStore.isStarred(inputText, translatedText, savedCards)
                },
                onSpeak = { text, lang -> tts.speak(text, lang) },
                onCopy = { copyToClipboard(it, "Translation") },
                onRemove = { id ->
                    cardStore.removeCard(id)
                },
                isSpeaking = tts.isSpeaking,
            )
        }

        // Translation History Dialog
        AnimatedVisibility(
            visible = showHistoryDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            TranslationHistoryDialog(
                history = historyItems,
                savedCards = savedCards,
                isSpeaking = tts.isSpeaking,
                onSelect = { item ->
                    sourceLang = item.sourceLang
                    targetLang = item.targetLang
                    skipDebounceNext = true
                    inputText = item.sourceText
                    translatedText = item.targetText
                    translatedRomaji = item.targetRomaji
                    isStarred = cardStore.isStarred(item.sourceText, item.targetText, savedCards)
                    showHistoryDialog = false
                },
                onToggleStar = { item ->
                    cardStore.toggleStar(
                        sourceText = item.sourceText,
                        targetText = item.targetText,
                        targetRomaji = item.targetRomaji,
                        sourceLang = item.sourceLang,
                        targetLang = item.targetLang,
                    )
                    savedCards = cardStore.loadStarredCards()
                    historyItems = historyStore.getHistory()
                    if (inputText.equals(item.sourceText, ignoreCase = true) || inputText.equals(item.targetText, ignoreCase = true)) {
                        isStarred = cardStore.isStarred(inputText, translatedText, savedCards)
                    }
                },
                onSpeak = { text, lang -> tts.speak(text, lang) },
                onCopy = { copyToClipboard(it, "Translation") },
                onClearAll = {
                    historyStore.clearAll()
                    historyItems = emptyList()
                    showToast("History cleared")
                },
                onDismiss = { showHistoryDialog = false },
            )
        }

        // Settings Sheet if opened directly from top bar
        if (showInternalSettings) {
            SettingsSheet(
                onDismiss = { showInternalSettings = false },
                audio = remember(context) { JapaneseTtsController.get(context) },
            )
        }

        // Feedback Toasts
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }
}

/**
 * Pulsing skeleton shimmer placeholder during 1.0s debounce and API latency.
 */
@Composable
private fun TranslatorSkeletonShimmer(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SkeletonTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "SkeletonAlpha",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CardsColors.Edge.copy(alpha = alpha)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CardsColors.Edge.copy(alpha = alpha * 0.8f)),
        )
    }
}
