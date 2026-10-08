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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.koto.app.BuildConfig
import com.koto.app.feature.translator.data.DeepLApiClient
import com.koto.app.feature.translator.data.NetworkMonitor
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
import com.koto.app.feature.translator.data.HybridTranslationEngine
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
import com.koto.app.feature.cards.spreadsheet.ExportFormat
import com.koto.app.feature.cards.spreadsheet.SkippedRow
import com.koto.app.feature.cards.spreadsheet.SpreadsheetEngine
import com.koto.app.feature.cards.spreadsheet.SpreadsheetValidationResult
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.koto.app.ui.theme.KotoColors

private enum class CreateDeckStage {
    Overview,
    CardDetail,
    CardContext,
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
    onDeleteDeck: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val customDeckStore = remember(context) { CustomDeckStore(context) }

    var deckTitle by rememberSaveable { mutableStateOf("") }
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
                cardsList = existingCards
            }
        }
    }

    // Card Editor buffer
    var editingCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorKana by rememberSaveable { mutableStateOf("") }
    var editorEnglish by rememberSaveable { mutableStateOf("") }
    var editorNotes by rememberSaveable { mutableStateOf("") }
    var editorEx1Jp by rememberSaveable { mutableStateOf("") }
    var editorEx1Romaji by rememberSaveable { mutableStateOf("") }
    var editorEx1En by rememberSaveable { mutableStateOf("") }
    var editorEx2Jp by rememberSaveable { mutableStateOf("") }
    var editorEx2Romaji by rememberSaveable { mutableStateOf("") }
    var editorEx2En by rememberSaveable { mutableStateOf("") }
    var editorEx3Jp by rememberSaveable { mutableStateOf("") }
    var editorEx3Romaji by rememberSaveable { mutableStateOf("") }
    var editorEx3En by rememberSaveable { mutableStateOf("") }

    // Quick add buffer
    var quickKana by rememberSaveable { mutableStateOf("") }
    var quickEnglish by rememberSaveable { mutableStateOf("") }

    // Discard & Delete confirmations
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var cardIdToDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val handleBackPress = {
        if (stage == CreateDeckStage.CardContext) {
            stage = CreateDeckStage.CardDetail
        } else if (stage == CreateDeckStage.CardDetail) {
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
                val forward = targetState.ordinal > initialState.ordinal
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
                            editorNotes = card.notes ?: ""
                            editorEx1Jp = card.example1Kana ?: ""
                            editorEx1Romaji = card.example1Romaji ?: ""
                            editorEx1En = card.example1English ?: ""
                            editorEx2Jp = card.example2Kana ?: ""
                            editorEx2Romaji = card.example2Romaji ?: ""
                            editorEx2En = card.example2English ?: ""
                            editorEx3Jp = card.example3Kana ?: ""
                            editorEx3Romaji = card.example3Romaji ?: ""
                            editorEx3En = card.example3English ?: ""
                            stage = CreateDeckStage.CardDetail
                        },
                        onDeleteCard = { cardId ->
                            cardIdToDelete = cardId
                        },
                        onImportCsvCards = { importedCards ->
                            cardsList = cardsList + importedCards
                        },
                        onAddNewCard = {
                            editingCardId = null
                            editorKana = ""
                            editorEnglish = ""
                            editorNotes = ""
                            editorEx1Jp = ""
                            editorEx1Romaji = ""
                            editorEx1En = ""
                            editorEx2Jp = ""
                            editorEx2Romaji = ""
                            editorEx2En = ""
                            editorEx3Jp = ""
                            editorEx3Romaji = ""
                            editorEx3En = ""
                            stage = CreateDeckStage.CardDetail
                        },
                        onBack = handleBackPress,
                        onSaveDeck = {
                            val title = deckTitle.trim()
                            if (title.isNotBlank() && cardsList.isNotEmpty()) {
                                val savedDeck = customDeckStore.saveDeck(
                                    deckId = deckIdToEdit,
                                    title = title,
                                    icon = CustomDeckStore.CUSTOM_DECK_ICON,
                                    cards = cardsList,
                                )
                                onDeckSaved(savedDeck)
                            }
                        },
                        onDeleteDeck = if (!deckIdToEdit.isNullOrBlank() && onDeleteDeck != null) {
                            {
                                customDeckStore.deleteDeck(deckIdToEdit)
                                onDeleteDeck(deckIdToEdit)
                            }
                        } else null,
                        isEditing = !deckIdToEdit.isNullOrBlank(),
                    )
                }
                CreateDeckStage.CardDetail -> {
                    CardEditorDetailView(
                        kana = editorKana,
                        onKanaChange = { editorKana = it },
                        english = editorEnglish,
                        onEnglishChange = { editorEnglish = it },
                        onOpenContext = { stage = CreateDeckStage.CardContext },
                        onBack = { stage = CreateDeckStage.Overview },
                        onSave = {
                            val jp = editorKana.trim()
                            val en = editorEnglish.trim()
                            if (jp.isNotBlank() && en.isNotBlank()) {
                                val rom = KanaConverter.toRomaji(KanaConverter.toPureKana(jp))
                                val updatedCard = CustomCardItem(
                                    id = editingCardId ?: "card_${System.currentTimeMillis()}_${cardsList.size}",
                                    japanese = jp,
                                    furigana = "",
                                    romaji = rom,
                                    english = en,
                                    notes = editorNotes.trim(),
                                    exampleKana = editorEx1Jp.trim(),
                                    exampleRomaji = editorEx1Romaji.trim().ifEmpty {
                                        editorEx1Jp.trim().takeIf { it.isNotEmpty() }?.let { KanaConverter.toRomaji(KanaConverter.toPureKana(it)) } ?: ""
                                    },
                                    exampleEnglish = editorEx1En.trim(),
                                    example2Kana = editorEx2Jp.trim(),
                                    example2Romaji = editorEx2Romaji.trim().ifEmpty {
                                        editorEx2Jp.trim().takeIf { it.isNotEmpty() }?.let { KanaConverter.toRomaji(KanaConverter.toPureKana(it)) } ?: ""
                                    },
                                    example2English = editorEx2En.trim(),
                                    example3Kana = editorEx3Jp.trim(),
                                    example3Romaji = editorEx3Romaji.trim().ifEmpty {
                                        editorEx3Jp.trim().takeIf { it.isNotEmpty() }?.let { KanaConverter.toRomaji(KanaConverter.toPureKana(it)) } ?: ""
                                    },
                                    example3English = editorEx3En.trim(),
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
                CreateDeckStage.CardContext -> {
                    CardContextEditorView(
                        notes = editorNotes,
                        onNotesChange = { editorNotes = it },
                        ex1Jp = editorEx1Jp,
                        onEx1JpChange = { editorEx1Jp = it },
                        ex1Romaji = editorEx1Romaji,
                        onEx1RomajiChange = { editorEx1Romaji = it },
                        ex1En = editorEx1En,
                        onEx1EnChange = { editorEx1En = it },
                        ex2Jp = editorEx2Jp,
                        onEx2JpChange = { editorEx2Jp = it },
                        ex2Romaji = editorEx2Romaji,
                        onEx2RomajiChange = { editorEx2Romaji = it },
                        ex2En = editorEx2En,
                        onEx2EnChange = { editorEx2En = it },
                        ex3Jp = editorEx3Jp,
                        onEx3JpChange = { editorEx3Jp = it },
                        ex3Romaji = editorEx3Romaji,
                        onEx3RomajiChange = { editorEx3Romaji = it },
                        ex3En = editorEx3En,
                        onEx3EnChange = { editorEx3En = it },
                        onBack = { stage = CreateDeckStage.CardDetail },
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
sealed class ImportState {
    object Idle : ImportState()
    object Validating : ImportState()
    data class Summary(
        val validCards: List<CustomCardItem>,
        val skippedRows: List<SkippedRow>,
    ) : ImportState()
    data class Error(val reason: String) : ImportState()
}

@Composable
private fun CreateDeckOverviewView(
    deckTitle: String,
    onTitleChange: (String) -> Unit,
    cards: List<CustomCardItem>,
    quickKana: String,
    onQuickKanaChange: (String) -> Unit,
    quickEnglish: String,
    onQuickEnglishChange: (String) -> Unit,
    onQuickAdd: () -> Unit,
    onEditCard: (CustomCardItem) -> Unit,
    onDeleteCard: (String) -> Unit,
    onImportCsvCards: (List<CustomCardItem>) -> Unit,
    onAddNewCard: () -> Unit = {},
    onBack: () -> Unit,
    onSaveDeck: () -> Unit,
    onDeleteDeck: (() -> Unit)? = null,
    isEditing: Boolean = false,
) {
    val context = LocalContext.current
    val canSave = deckTitle.trim().isNotBlank() && cards.isNotEmpty()
    val quickAddValid = quickKana.trim().isNotBlank() && quickEnglish.trim().isNotBlank()

    var importState by remember { mutableStateOf<ImportState>(ImportState.Idle) }
    var showTemplateDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDeckDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Native Spreadsheet / CSV document picker launcher (Dispatchers.IO parsing)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    importState = ImportState.Validating
                }
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val result = SpreadsheetEngine.parseSpreadsheet(stream)
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is SpreadsheetValidationResult.CriticalError -> {
                                    importState = ImportState.Error(result.message)
                                }
                                is SpreadsheetValidationResult.Success -> {
                                    val validCardItems = result.validCards.map { it.toCustomCardItem() }
                                    if (result.skippedRows.isEmpty()) {
                                        // Scenario A (100% Valid): Insert cards directly into deck database, show snackbar
                                        onImportCsvCards(validCardItems)
                                        importState = ImportState.Idle
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Imported ${validCardItems.size} cards successfully.")
                                        }
                                        Toast.makeText(context, "Imported ${validCardItems.size} cards successfully.", Toast.LENGTH_SHORT).show()
                                    } else if (validCardItems.isNotEmpty()) {
                                        // Scenario B (Partial Valid): Show ImportSummaryDialog
                                        importState = ImportState.Summary(validCardItems, result.skippedRows)
                                    } else {
                                        // All rows were invalid
                                        importState = ImportState.Error("No valid cards found in file. All rows were missing required columns.")
                                    }
                                }
                            }
                        }
                    } ?: withContext(Dispatchers.Main) {
                        importState = ImportState.Error("Could not open selected file.")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        importState = ImportState.Error("Failed to read file: ${e.message}")
                    }
                }
            }
        }
    }

    val translationEngine = remember(context) { HybridTranslationEngine.getInstance(context) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
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
            // Left: Back button & Template Download button (moved to the left)
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TactileButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp, 48.dp)
                        .testTag("create_deck_back")
                        .testTag("cards_detail_back"),
                    tone = TactileTone.Quiet,
                    description = "Back",
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = null,
                        tint = KotoColors.Navy,
                        modifier = Modifier.size(24.dp),
                    )
                }

                TactileButton(
                    onClick = { showTemplateDialog = true },
                    modifier = Modifier
                        .size(44.dp, 48.dp)
                        .testTag("create_deck_template_download_button"),
                    tone = TactileTone.Quiet,
                    description = "Download Template",
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_template_download),
                        contentDescription = "Download Template",
                        tint = KotoColors.Navy,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Center: Screen Title
            Text(
                text = if (isEditing) "Edit Deck" else "Create Deck",
                color = CardsColors.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )

            // Right: Export & Import Buttons
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Export Button (replaces +)
                TactileButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .size(44.dp, 48.dp)
                        .testTag("create_deck_export_button")
                        .testTag("btn_export_deck")
                        .testTag("create_deck_add_card_button"),
                    tone = TactileTone.Quiet,
                    description = "Export Deck",
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_export),
                        contentDescription = "Export Deck",
                        tint = KotoColors.Navy,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Import Button (triggers ImportOptionsDialog)
                TactileButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier
                        .size(44.dp, 48.dp)
                        .testTag("create_deck_import_button"),
                    tone = TactileTone.Quiet,
                    description = "Import Deck",
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_import),
                        contentDescription = "Import Deck",
                        tint = KotoColors.Navy,
                        modifier = Modifier.size(22.dp),
                    )
                }
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
                    DeckEditorCardItem(
                        index = index + 1,
                        card = card,
                        onEdit = { onEditCard(card) },
                        onDelete = { onDeleteCard(card.id) },
                    )
                }
            }
        }

        // Sticky Bottom Bar: Fixed at the very bottom (Zero jumping, glued spatial layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardsColors.Surface)
                .drawBehind {
                    drawLine(CardsColors.Edge, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                }
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (isEditing && onDeleteDeck != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardsButton(
                        label = "Delete Deck",
                        onClick = { showDeleteDeckDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_delete_deck"),
                        background = CardsColors.Surface,
                        ink = CardsColors.Coral,
                        depth = CardsColors.Edge,
                    )
                    CardsButton(
                        label = "Save Deck",
                        onClick = onSaveDeck,
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("create_deck_save"),
                        background = if (canSave) CardsColors.Blue else CardsColors.Ice,
                        ink = if (canSave) Color.White else CardsColors.Muted,
                        depth = if (canSave) CardsColors.BlueDepth else CardsColors.Edge,
                    )
                }
            } else {
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

        // Snackbar Host for import notifications
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp),
            )
        }
    }

    // Template Download Dialog
    if (showTemplateDialog) {
        TemplateDownloadDialog(
            onDismiss = { showTemplateDialog = false },
        )
    }

    // Import Options Dialog (guide & rules)
    if (showImportDialog) {
        ImportOptionsDialog(
            onSelectFile = {
                showImportDialog = false
                openDocumentLauncher.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/csv",
                        "text/comma-separated-values",
                        "*/*",
                    ),
                )
            },
            onDownloadTemplate = {
                showImportDialog = false
                downloadTemplateFile(context)
            },
            onDismiss = { showImportDialog = false },
        )
    }

    // Export Deck Dialog (options & rules)
    if (showExportDialog) {
        ExportDeckDialog(
            deckTitle = deckTitle.ifBlank { "Untitled Deck" },
            cardCount = cards.size,
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                showExportDialog = false
                if (cards.isEmpty()) {
                    Toast.makeText(context, "Add at least 1 card to export", Toast.LENGTH_SHORT).show()
                } else {
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                            val cleanName = (deckTitle.ifBlank { "Deck" }).replace("[^a-zA-Z0-9_-]".toRegex(), "_").ifEmpty { "Deck" }
                            val filename = "${cleanName}_KotobaExport_${dateStr}.${format.extension}"
                            val bytes = SpreadsheetEngine.exportDeck(deckTitle.ifBlank { "Deck" }, cards, format)
                            val uri = SpreadsheetEngine.saveToDownloads(context, filename, format.mimeType, bytes)
                            if (uri != null) {
                                SpreadsheetEngine.ExportResult(
                                    uri = uri,
                                    filename = filename,
                                    mimeType = format.mimeType,
                                    deckTitle = deckTitle,
                                    cardCount = cards.size,
                                )
                            } else null
                        }
                        if (result != null) {
                            val action = snackbarHostState.showSnackbar(
                                message = "Exported ${result.filename} successfully.",
                                actionLabel = "Share",
                                duration = androidx.compose.material3.SnackbarDuration.Long,
                            )
                            Toast.makeText(context, "Exported ${result.filename} to Downloads", Toast.LENGTH_SHORT).show()
                            if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                SpreadsheetEngine.shareExportedFile(context, result.uri, result.mimeType, result.deckTitle)
                            }
                        } else {
                            snackbarHostState.showSnackbar("Failed to export deck.")
                        }
                    }
                }
            },
        )
    }

    // Delete Deck Confirmation Dialog
    if (showDeleteDeckDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDeckDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    "Delete Deck?",
                    color = CardsColors.Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${deckTitle.ifBlank { "this deck" }}\"? All cards in this deck will be permanently removed.",
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
                        onClick = { showDeleteDeckDialog = false },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cancel_delete_deck"),
                        background = CardsColors.Surface,
                        ink = CardsColors.Ink,
                        depth = CardsColors.Edge,
                    )
                    CardsButton(
                        label = "Delete",
                        onClick = {
                            showDeleteDeckDialog = false
                            onDeleteDeck?.invoke()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_confirm_delete_deck"),
                        background = CardsColors.Coral,
                        ink = Color.White,
                        depth = Color(0xFF8B2B2B),
                    )
                }
            },
            dismissButton = null,
        )
    }

    // Import Dialogs
    when (val currentImport = importState) {
        is ImportState.Summary -> {
            ImportSummaryDialog(
                validCount = currentImport.validCards.size,
                skippedRows = currentImport.skippedRows,
                onImportValid = {
                    onImportCsvCards(currentImport.validCards)
                    val count = currentImport.validCards.size
                    importState = ImportState.Idle
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Imported $count cards successfully.")
                    }
                    Toast.makeText(context, "Imported $count cards successfully.", Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    importState = ImportState.Idle
                },
            )
        }
        is ImportState.Error -> {
            ImportErrorDialog(
                reason = currentImport.reason,
                onDownloadTemplate = {
                    importState = ImportState.Idle
                    showTemplateDialog = true
                },
                onTryAgain = {
                    importState = ImportState.Idle
                    openDocumentLauncher.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "text/csv",
                            "text/comma-separated-values",
                            "*/*",
                        ),
                    )
                },
                onDismiss = {
                    importState = ImportState.Idle
                },
            )
        }
        else -> Unit
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
    onOpenContext: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val canSave = kana.trim().isNotBlank() && english.trim().isNotBlank()
    val context = LocalContext.current
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
                .verticalScroll(rememberScrollState())
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

            // Action Buttons: Add Context & Translate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Add Context Button (matching white outlined style)
                CardsButton(
                    label = "Add Context",
                    onClick = onOpenContext,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_edit_card_add_context"),
                    background = CardsColors.Surface,
                    ink = CardsColors.Ink,
                    depth = CardsColors.Edge,
                )

                // Translate Button (DeepL Only, Toast when offline)
                CardsButton(
                    label = if (isTranslatingEditor) "Translating..." else "Translate",
                    onClick = {
                        if (!isTranslatingEditor && canTranslateEditor) {
                            coroutineScope.launch {
                                if (!NetworkMonitor.isOnline(context)) {
                                    Toast.makeText(
                                        context,
                                        "Internet connection required for high-accuracy translation.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@launch
                                }
                                isTranslatingEditor = true
                                try {
                                    val apiKey = BuildConfig.DEEPL_API_KEY.trim()
                                    val deepLApiClient = DeepLApiClient()
                                    if (kana.isNotBlank() && english.isBlank()) {
                                        val res = deepLApiClient.translate(
                                            kana,
                                            com.koto.app.feature.translator.model.TranslationLanguage.Japanese,
                                            com.koto.app.feature.translator.model.TranslationLanguage.English,
                                            apiKey,
                                        )
                                        if (!res.isNullOrBlank()) {
                                            onEnglishChange(res.trim().replaceFirstChar { it.uppercase() })
                                        }
                                    } else if (english.isNotBlank() && kana.isBlank()) {
                                        val res = deepLApiClient.translate(
                                            english,
                                            com.koto.app.feature.translator.model.TranslationLanguage.English,
                                            com.koto.app.feature.translator.model.TranslationLanguage.Japanese,
                                            apiKey,
                                        )
                                        if (!res.isNullOrBlank()) {
                                            onKanaChange(res.trim())
                                        }
                                    } else if (kana.isNotBlank()) {
                                        val res = deepLApiClient.translate(
                                            kana,
                                            com.koto.app.feature.translator.model.TranslationLanguage.Japanese,
                                            com.koto.app.feature.translator.model.TranslationLanguage.English,
                                            apiKey,
                                        )
                                        if (!res.isNullOrBlank()) {
                                            onEnglishChange(res.trim().replaceFirstChar { it.uppercase() })
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

        // Bottom Full-Width Blue Action Bar: Save
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardsColors.Surface)
                .drawBehind {
                    drawLine(CardsColors.Edge, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                }
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            CardsButton(
                label = "Save",
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_edit_card_save"),
                background = if (canSave) CardsColors.Blue else CardsColors.Ice,
                ink = if (canSave) Color.White else CardsColors.Muted,
                depth = if (canSave) CardsColors.BlueDepth else CardsColors.Edge,
            )
        }
    }
}

/**
 * Screen 3: Dedicated Context & Example Sentences Editor (Specification 3.2 Task 6.4)
 * Allows specifying Nuance / Context notes and up to 3 rich example sentences.
 */
@Composable
private fun CardContextEditorView(
    notes: String,
    onNotesChange: (String) -> Unit,
    ex1Jp: String,
    onEx1JpChange: (String) -> Unit,
    ex1Romaji: String,
    onEx1RomajiChange: (String) -> Unit,
    ex1En: String,
    onEx1EnChange: (String) -> Unit,
    ex2Jp: String,
    onEx2JpChange: (String) -> Unit,
    ex2Romaji: String,
    onEx2RomajiChange: (String) -> Unit,
    ex2En: String,
    onEx2EnChange: (String) -> Unit,
    ex3Jp: String,
    onEx3JpChange: (String) -> Unit,
    ex3Romaji: String,
    onEx3RomajiChange: (String) -> Unit,
    ex3En: String,
    onEx3EnChange: (String) -> Unit,
    onBack: () -> Unit,
) {
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
                    .testTag("card_context_back"),
                tone = TactileTone.Quiet,
                description = "Back to card editor",
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
                text = "Add Context",
                color = CardsColors.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Form Body
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // General Context / Nuance Notes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "GENERAL CONTEXT / NUANCE",
                    color = CardsColors.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                CleanInputBox(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = "Usage notes, cultural context, tone nuance...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_context_notes"),
                    singleLine = false,
                    minLines = 3,
                )
            }

            // Example Sentence 1
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXAMPLE SENTENCE 1",
                    color = CardsColors.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                CleanInputBox(
                    value = ex1Jp,
                    onValueChange = onEx1JpChange,
                    placeholder = "Japanese / Kana",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex1_jp"),
                )
                CleanInputBox(
                    value = ex1Romaji,
                    onValueChange = onEx1RomajiChange,
                    placeholder = "Romaji (optional)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex1_romaji"),
                )
                CleanInputBox(
                    value = ex1En,
                    onValueChange = onEx1EnChange,
                    placeholder = "English translation",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex1_en"),
                )
            }

            // Example Sentence 2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXAMPLE SENTENCE 2",
                    color = CardsColors.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                CleanInputBox(
                    value = ex2Jp,
                    onValueChange = onEx2JpChange,
                    placeholder = "Japanese / Kana",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex2_jp"),
                )
                CleanInputBox(
                    value = ex2Romaji,
                    onValueChange = onEx2RomajiChange,
                    placeholder = "Romaji (optional)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex2_romaji"),
                )
                CleanInputBox(
                    value = ex2En,
                    onValueChange = onEx2EnChange,
                    placeholder = "English translation",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex2_en"),
                )
            }

            // Example Sentence 3
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXAMPLE SENTENCE 3",
                    color = CardsColors.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                CleanInputBox(
                    value = ex3Jp,
                    onValueChange = onEx3JpChange,
                    placeholder = "Japanese / Kana",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex3_jp"),
                )
                CleanInputBox(
                    value = ex3Romaji,
                    onValueChange = onEx3RomajiChange,
                    placeholder = "Romaji (optional)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex3_romaji"),
                )
                CleanInputBox(
                    value = ex3En,
                    onValueChange = onEx3EnChange,
                    placeholder = "English translation",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ex3_en"),
                )
            }
        }

        // Bottom Full-Width Blue Action Bar: Done
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardsColors.Surface)
                .drawBehind {
                    drawLine(CardsColors.Edge, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                }
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            CardsButton(
                label = "Done",
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_card_context_done"),
                background = CardsColors.Blue,
                ink = Color.White,
                depth = CardsColors.BlueDepth,
            )
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
