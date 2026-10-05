package com.koto.app.ui.screens.cards

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.feature.translator.data.MlKitTranslationEngine
import com.koto.app.feature.translator.model.TranslationLanguage
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import com.koto.app.R
import com.koto.app.feature.cards.csv.DeckCsvEngine
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.cards.data.CustomDeckStore
import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone
import com.koto.app.ui.theme.KotoColors

private enum class CreateDeckStage {
    Overview,
    CardDetail,
}

val CANON_DECK_ICONS = listOf(
    "chatbubble",
    "hashtag",
    "calendar",
    "clock",
    "notebook",
    "home",
    "utensils",
    "train",
    "star",
)

@Composable
fun CreateDeckScreen(
    deckIdToEdit: String? = null,
    onBack: () -> Unit,
    onDeckSaved: (FlashcardDeck) -> Unit,
) {
    val context = LocalContext.current
    val customDeckStore = remember(context) { CustomDeckStore(context) }

    var deckTitle by rememberSaveable { mutableStateOf("") }
    var selectedIcon by rememberSaveable { mutableStateOf("chatbubble") }
    var cardsList by remember { mutableStateOf<List<CustomCardItem>>(emptyList()) }
    var stage by rememberSaveable { mutableStateOf(CreateDeckStage.Overview) }

    // Pre-load deck to edit if an ID was passed
    LaunchedEffect(deckIdToEdit) {
        if (!deckIdToEdit.isNullOrBlank()) {
            val existingCards = customDeckStore.getDeckCardsDetails(deckIdToEdit)
            val allDecks = customDeckStore.loadCustomDecks()
            val deckEntity = allDecks.find { it.id == deckIdToEdit }
            if (deckEntity != null) {
                deckTitle = deckEntity.title
                selectedIcon = deckEntity.icon
                cardsList = existingCards
            }
        }
    }

    // Card Editor buffer
    var editingCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorKana by rememberSaveable { mutableStateOf("") }
    var editorEnglish by rememberSaveable { mutableStateOf("") }

    // Quick add buffer
    var quickKana by rememberSaveable { mutableStateOf("") }
    var quickEnglish by rememberSaveable { mutableStateOf("") }

    // Discard & Delete confirmations
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var cardIdToDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val handleBackPress = {
        if (stage == CreateDeckStage.CardDetail) {
            stage = CreateDeckStage.Overview
        } else {
            val hasChanges = deckTitle.isNotBlank() || cardsList.isNotEmpty() || quickKana.isNotBlank() || quickEnglish.isNotBlank()
            if (hasChanges) {
                showDiscardDialog = true
            } else {
                onBack()
            }
        }
    }

    BackHandler { handleBackPress() }

    val layoutSign = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("create_deck_screen")
            .testTag("create_deck_placeholder_screen"),
    ) {
        AnimatedContent(
            targetState = stage,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState == CreateDeckStage.CardDetail
                val direction = if (forward) layoutSign else -layoutSign
                (slideInHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> direction * fullWidth },
                ) + fadeIn(
                    animationSpec = tween(250, easing = LinearOutSlowInEasing),
                )).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -direction * fullWidth },
                    ) + fadeOut(
                        animationSpec = tween(200, easing = FastOutLinearInEasing),
                    ),
                ).using(null)
            },
            label = "Create deck screen flow",
        ) { currentStage ->
            when (currentStage) {
                CreateDeckStage.Overview -> {
                    CreateDeckOverviewView(
                        deckTitle = deckTitle,
                        onTitleChange = { deckTitle = it },
                        selectedIcon = selectedIcon,
                        onIconSelect = { selectedIcon = it },
                        cards = cardsList,
                        quickKana = quickKana,
                        onQuickKanaChange = { quickKana = it },
                        quickEnglish = quickEnglish,
                        onQuickEnglishChange = { quickEnglish = it },
                        onQuickAdd = {
                            val jp = quickKana.trim()
                            val en = quickEnglish.trim()
                            if (jp.isNotBlank() && en.isNotBlank()) {
                                val rom = KanaConverter.toRomaji(KanaConverter.toPureKana(jp))
                                val newCard = CustomCardItem(
                                    id = "card_${System.currentTimeMillis()}_${cardsList.size}",
                                    japanese = jp,
                                    romaji = rom,
                                    english = en,
                                )
                                cardsList = cardsList + newCard
                                quickKana = ""
                                quickEnglish = ""
                            }
                        },
                        onEditCard = { card ->
                            editingCardId = card.id
                            editorKana = card.japanese
                            editorEnglish = card.english
                            stage = CreateDeckStage.CardDetail
                        },
                        onDeleteCard = { cardId ->
                            cardIdToDelete = cardId
                        },
                        onImportCsvCards = { importedCards ->
                            cardsList = cardsList + importedCards
                        },
                        onBack = handleBackPress,
                        onSaveDeck = {
                            val title = deckTitle.trim()
                            if (title.isNotBlank() && cardsList.isNotEmpty()) {
                                val savedDeck = customDeckStore.saveDeck(
                                    deckId = deckIdToEdit,
                                    title = title,
                                    icon = selectedIcon,
                                    cards = cardsList,
                                )
                                onDeckSaved(savedDeck)
                            }
                        },
                    )
                }
                CreateDeckStage.CardDetail -> {
                    CardEditorDetailView(
                        kana = editorKana,
                        onKanaChange = { editorKana = it },
                        english = editorEnglish,
                        onEnglishChange = { editorEnglish = it },
                        onBack = { stage = CreateDeckStage.Overview },
                        onSave = {
                            val jp = editorKana.trim()
                            val en = editorEnglish.trim()
                            if (jp.isNotBlank() && en.isNotBlank()) {
                                val rom = KanaConverter.toRomaji(KanaConverter.toPureKana(jp))
                                val updatedCard = CustomCardItem(
                                    id = editingCardId ?: "card_${System.currentTimeMillis()}_${cardsList.size}",
                                    japanese = jp,
                                    romaji = rom,
                                    english = en,
                                )
                                cardsList = if (editingCardId != null) {
                                    cardsList.map { if (it.id == editingCardId) updatedCard else it }
                                } else {
                                    cardsList + updatedCard
                                }
                            }
                            stage = CreateDeckStage.Overview
                        },
                    )
                }
            }
        }

        // Delete confirmation dialog
        if (cardIdToDelete != null) {
            AlertDialog(
                onDismissRequest = { cardIdToDelete = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        "Delete Card",
                        color = CardsColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to remove this card from the deck?",
                        color = CardsColors.Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CardsButton(
                            label = "Cancel",
                            onClick = { cardIdToDelete = null },
                            modifier = Modifier.weight(1f),
                            background = CardsColors.Surface,
                            ink = CardsColors.Ink,
                            depth = CardsColors.Edge,
                        )
                        CardsButton(
                            label = "Delete",
                            onClick = {
                                cardsList = cardsList.filterNot { it.id == cardIdToDelete }
                                cardIdToDelete = null
                            },
                            modifier = Modifier.weight(1f),
                            background = CardsColors.Coral,
                            depth = Color(0xFF8B2B2B),
                        )
                    }
                },
                dismissButton = null,
            )
        }

        // Discard unsaved changes confirmation dialog
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        "Discard Deck?",
                        color = CardsColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                text = {
                    Text(
                        "You have unsaved changes. Exiting now will discard this deck.",
                        color = CardsColors.Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CardsButton(
                            label = "Keep Editing",
                            onClick = { showDiscardDialog = false },
                            modifier = Modifier.weight(1f),
                            background = CardsColors.Surface,
                            ink = CardsColors.Ink,
                            depth = CardsColors.Edge,
                        )
                        CardsButton(
                            label = "Discard",
                            onClick = {
                                showDiscardDialog = false
                                onBack()
                            },
                            modifier = Modifier.weight(1f),
                            background = CardsColors.Coral,
                            depth = Color(0xFF8B2B2B),
                        )
                    }
                },
                dismissButton = null,
            )
        }
    }
}

/**
 * Screen 1: Create / Edit Deck Overview (Clean Main View)
 */
@Composable
private fun CreateDeckOverviewView(
    deckTitle: String,
    onTitleChange: (String) -> Unit,
    selectedIcon: String,
    onIconSelect: (String) -> Unit,
    cards: List<CustomCardItem>,
    quickKana: String,
    onQuickKanaChange: (String) -> Unit,
    quickEnglish: String,
    onQuickEnglishChange: (String) -> Unit,
    onQuickAdd: () -> Unit,
    onEditCard: (CustomCardItem) -> Unit,
    onDeleteCard: (String) -> Unit,
    onImportCsvCards: (List<CustomCardItem>) -> Unit,
    onBack: () -> Unit,
    onSaveDeck: () -> Unit,
) {
    val context = LocalContext.current
    val canSave = deckTitle.trim().isNotBlank() && cards.isNotEmpty()
    val quickAddValid = quickKana.trim().isNotBlank() && quickEnglish.trim().isNotBlank()

    // Native CSV document picker launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val result = DeckCsvEngine.parseImportStream(context, stream)
                    if (result.cards.isNotEmpty()) {
                        onImportCsvCards(result.cards)
                        val summary = if (result.skippedRows > 0) {
                            "Imported ${result.cards.size} cards (${result.skippedRows} skipped)"
                        } else {
                            "Imported ${result.cards.size} cards"
                        }
                        Toast.makeText(context, summary, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No valid cards found in file", Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                Toast.makeText(context, "Failed to read file: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val translationEngine = remember(context) { MlKitTranslationEngine.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        // Canon Top App Bar
        Box(
            Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .shadow(
                    elevation = 4.dp,
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    clip = false,
                    ambientColor = Color(0x201A3761),
                    spotColor = Color(0x301A3761),
                )
                .background(Color.White)
                .heightIn(min = 64.dp)
                .drawBehind {
                    drawLine(KotoColors.Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                }
                .padding(horizontal = 16.dp),
        ) {
            // Left: Back button
            TactileButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp, 52.dp)
                    .align(Alignment.CenterStart)
                    .testTag("create_deck_back")
                    .testTag("cards_detail_back"),
                tone = TactileTone.Quiet,
                description = "Back",
                padding = PaddingValues(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = null,
                    tint = KotoColors.Navy,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Center: Screen Title
            Text(
                text = "Create Deck",
                color = CardsColors.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )

            // Right: Import Deck Icon Button
            TactileButton(
                onClick = {
                    openDocumentLauncher.launch(
                        arrayOf(
                            "text/*",
                            "text/comma-separated-values",
                            "text/csv",
                            "application/vnd.ms-excel",
                            "application/zip",
                            "application/octet-stream",
                            "application/x-apkg",
                            "*/*",
                        ),
                    )
                },
                modifier = Modifier
                    .size(48.dp, 52.dp)
                    .align(Alignment.CenterEnd)
                    .testTag("create_deck_import_button"),
                tone = TactileTone.Quiet,
                description = "Import Deck",
                padding = PaddingValues(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_import),
                    contentDescription = "Import Deck",
                    tint = KotoColors.Navy,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Screen Body: LazyColumn with metadata, utility row, quick add, and card rows
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("create_deck_scroll_content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Deck Title Input Section
            item(key = "section_metadata") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DECK TITLE",
                        color = CardsColors.Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    CleanInputBox(
                        value = deckTitle,
                        onValueChange = onTitleChange,
                        placeholder = "Deck Title",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_deck_title"),
                    )
                }
            }

            // Category Icon Selector Carousel
            item(key = "section_icons") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CHOOSE ICON",
                        color = CardsColors.Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.testTag("deck_icon_carousel"),
                    ) {
                        items(CANON_DECK_ICONS) { iconKey ->
                            val isSelected = iconKey == selectedIcon
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) CardsColors.Blue else CardsColors.Edge,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable { onIconSelect(iconKey) }
                                    .testTag("icon_select_$iconKey"),
                                contentAlignment = Alignment.Center,
                            ) {
                                CanonIconArtwork(iconKey, Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            // Card Adder (Stacked Layout: Japanese, English, [Save] [Translate])
            item(key = "section_quick_add") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "ADD CARD",
                        color = CardsColors.Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )

                    // Target Japanese (with Romaji preview underneath)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        CleanInputBox(
                            value = quickKana,
                            onValueChange = onQuickKanaChange,
                            placeholder = "Japanese / Kana",
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_quick_kana"),
                        )
                        val quickRomaji = remember(quickKana) {
                            if (quickKana.isNotBlank()) KanaConverter.toRomaji(KanaConverter.toPureKana(quickKana)) else ""
                        }
                        if (quickRomaji.isNotBlank()) {
                            Text(
                                text = quickRomaji,
                                color = CardsColors.Blue,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }

                    // English Meaning
                    CleanInputBox(
                        value = quickEnglish,
                        onValueChange = onQuickEnglishChange,
                        placeholder = "English",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_quick_english"),
                    )

                    // Action Buttons: Save & Translate
                    var isTranslatingQuick by remember { mutableStateOf(false) }
                    val canTranslateQuick = quickKana.isNotBlank() || quickEnglish.isNotBlank()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Save Button
                        CardsButton(
                            label = "Save",
                            onClick = onQuickAdd,
                            enabled = quickAddValid,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_add_save"),
                            background = if (quickAddValid) CardsColors.Blue else CardsColors.Surface,
                            ink = if (quickAddValid) Color.White else CardsColors.Muted,
                            depth = if (quickAddValid) CardsColors.BlueDepth else CardsColors.Edge,
                        )

                        // Translate Button
                        CardsButton(
                            label = if (isTranslatingQuick) "Translating..." else "Translate",
                            onClick = {
                                if (!isTranslatingQuick && canTranslateQuick) {
                                    coroutineScope.launch {
                                        isTranslatingQuick = true
                                        try {
                                            if (quickKana.isNotBlank() && quickEnglish.isBlank()) {
                                                val res = translationEngine.translate(
                                                    quickKana,
                                                    TranslationLanguage.Japanese,
                                                    TranslationLanguage.English,
                                                )
                                                if (res.translatedText.isNotBlank()) {
                                                    onQuickEnglishChange(res.translatedText)
                                                }
                                            } else if (quickEnglish.isNotBlank() && quickKana.isBlank()) {
                                                val res = translationEngine.translate(
                                                    quickEnglish,
                                                    TranslationLanguage.English,
                                                    TranslationLanguage.Japanese,
                                                )
                                                if (res.translatedText.isNotBlank()) {
                                                    onQuickKanaChange(res.translatedText)
                                                }
                                            } else if (quickKana.isNotBlank()) {
                                                val res = translationEngine.translate(
                                                    quickKana,
                                                    TranslationLanguage.Japanese,
                                                    TranslationLanguage.English,
                                                )
                                                if (res.translatedText.isNotBlank()) {
                                                    onQuickEnglishChange(res.translatedText)
                                                }
                                            }
                                        } catch (_: Throwable) {
                                        } finally {
                                            isTranslatingQuick = false
                                        }
                                    }
                                }
                            },
                            enabled = canTranslateQuick && !isTranslatingQuick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_auto_translate"),
                            background = CardsColors.Surface,
                            ink = if (canTranslateQuick) CardsColors.Blue else CardsColors.Muted,
                            depth = CardsColors.Edge,
                        )
                    }
                }
            }

            // Divider before card list
            item(key = "divider_before_cards") {
                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)
            }

            // Header for Cards in Deck
            item(key = "header_cards_in_deck") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CARDS IN DECK (${cards.size})",
                        color = CardsColors.Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            if (cards.isEmpty()) {
                item(key = "cards_empty_placeholder") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "No cards added yet",
                                color = CardsColors.Ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Use Quick Add above or import cards from a CSV file.",
                                color = CardsColors.Muted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                    if (index > 0) {
                        HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.6f), thickness = 0.8.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .testTag("deck_card_row_${card.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Column: Japanese + Romaji strictly underneath
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = card.japanese,
                                color = CardsColors.Ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (card.romaji.isNotBlank()) {
                                Text(
                                    text = card.romaji,
                                    color = CardsColors.Blue,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        // Center: English
                        Text(
                            text = card.english,
                            color = CardsColors.Ink,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(horizontal = 8.dp),
                        )

                        // Row Actions: Edit and Delete
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Edit button
                            TactileButton(
                                onClick = { onEditCard(card) },
                                modifier = Modifier
                                    .size(38.dp, 40.dp)
                                    .testTag("btn_edit_card_${card.id}"),
                                tone = TactileTone.Quiet,
                                description = "Edit Card",
                                padding = PaddingValues(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = null,
                                    tint = CardsColors.Blue,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            // Delete button
                            TactileButton(
                                onClick = { onDeleteCard(card.id) },
                                modifier = Modifier
                                    .size(38.dp, 40.dp)
                                    .testTag("btn_delete_card_${card.id}"),
                                tone = TactileTone.Quiet,
                                description = "Delete Card",
                                padding = PaddingValues(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_trash),
                                    contentDescription = null,
                                    tint = CardsColors.Coral,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar: Full-width 3D Save Deck Button (Zero jumping, glued spatial layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardsColors.Surface)
                .drawBehind {
                    drawLine(CardsColors.Edge, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            CardsButton(
                label = "Save Deck",
                onClick = onSaveDeck,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("create_deck_save"),
                background = if (canSave) CardsColors.Blue else CardsColors.Ice,
                ink = if (canSave) Color.White else CardsColors.Muted,
                depth = if (canSave) CardsColors.BlueDepth else CardsColors.Edge,
            )
        }
    }
}

/**
 * Screen 2: Dedicated Single-Card Editor (Streamlined Japanese + English + Save + Translate)
 */
@Composable
private fun CardEditorDetailView(
    kana: String,
    onKanaChange: (String) -> Unit,
    english: String,
    onEnglishChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val canSave = kana.trim().isNotBlank() && english.trim().isNotBlank()
    val context = LocalContext.current
    val translationEngine = remember(context) { MlKitTranslationEngine.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    var isTranslatingEditor by remember { mutableStateOf(false) }
    val canTranslateEditor = kana.isNotBlank() || english.isNotBlank()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        // Canon Top App Bar
        Box(
            Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .shadow(
                    elevation = 4.dp,
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    clip = false,
                    ambientColor = Color(0x201A3761),
                    spotColor = Color(0x301A3761),
                )
                .background(Color.White)
                .heightIn(min = 64.dp)
                .drawBehind {
                    drawLine(KotoColors.Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                }
                .padding(horizontal = 16.dp),
        ) {
            // Left: Back button
            TactileButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp, 52.dp)
                    .align(Alignment.CenterStart)
                    .testTag("card_detail_back"),
                tone = TactileTone.Quiet,
                description = "Back",
                padding = PaddingValues(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = null,
                    tint = KotoColors.Navy,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Center: Screen Title
            Text(
                text = "Edit Card",
                color = CardsColors.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Editor Form Body
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Japanese Word / Expression
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "JAPANESE / KANA",
                    color = CardsColors.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                CleanInputBox(
                    value = kana,
                    onValueChange = onKanaChange,
                    placeholder = "Japanese / Kana",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_editor_kana"),
                )
                val wordRomaji = remember(kana) {
                    if (kana.isNotBlank()) KanaConverter.toRomaji(KanaConverter.toPureKana(kana)) else ""
                }
                if (wordRomaji.isNotBlank()) {
                    Text(
                        text = wordRomaji,
                        color = CardsColors.Blue,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // English Meaning
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ENGLISH",
                    color = CardsColors.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                CleanInputBox(
                    value = english,
                    onValueChange = onEnglishChange,
                    placeholder = "English",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_editor_english"),
                )
            }

            // Action Buttons: Save & Translate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Save Button (Saves and navigates back to deck overview)
                CardsButton(
                    label = "Save",
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_edit_card_save"),
                    background = if (canSave) CardsColors.Blue else CardsColors.Surface,
                    ink = if (canSave) Color.White else CardsColors.Muted,
                    depth = if (canSave) CardsColors.BlueDepth else CardsColors.Edge,
                )

                // Translate Button
                CardsButton(
                    label = if (isTranslatingEditor) "Translating..." else "Translate",
                    onClick = {
                        if (!isTranslatingEditor && canTranslateEditor) {
                            coroutineScope.launch {
                                isTranslatingEditor = true
                                try {
                                    if (kana.isNotBlank() && english.isBlank()) {
                                        val res = translationEngine.translate(
                                            kana,
                                            TranslationLanguage.Japanese,
                                            TranslationLanguage.English,
                                        )
                                        if (res.translatedText.isNotBlank()) {
                                            onEnglishChange(res.translatedText)
                                        }
                                    } else if (english.isNotBlank() && kana.isBlank()) {
                                        val res = translationEngine.translate(
                                            english,
                                            TranslationLanguage.English,
                                            TranslationLanguage.Japanese,
                                        )
                                        if (res.translatedText.isNotBlank()) {
                                            onKanaChange(res.translatedText)
                                        }
                                    } else if (kana.isNotBlank()) {
                                        val res = translationEngine.translate(
                                            kana,
                                            TranslationLanguage.Japanese,
                                            TranslationLanguage.English,
                                        )
                                        if (res.translatedText.isNotBlank()) {
                                            onEnglishChange(res.translatedText)
                                        }
                                    }
                                } catch (_: Throwable) {
                                } finally {
                                    isTranslatingEditor = false
                                }
                            }
                        }
                    },
                    enabled = canTranslateEditor && !isTranslatingEditor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_btn_auto_translate"),
                    background = CardsColors.Surface,
                    ink = if (canTranslateEditor) CardsColors.Blue else CardsColors.Muted,
                    depth = CardsColors.Edge,
                )
            }
        }
    }
}

/**
 * Standard Koto crisp input box with pure white background and 1dp hairline border.
 */
@Composable
private fun CleanInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, CardsColors.Edge, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        textStyle = TextStyle(
            color = CardsColors.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        ),
        cursorBrush = SolidColor(CardsColors.Blue),
        singleLine = singleLine,
        minLines = minLines,
        decorationBox = { innerTextField ->
            Box(Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = CardsColors.Muted.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                innerTextField()
            }
        },
    )
}
