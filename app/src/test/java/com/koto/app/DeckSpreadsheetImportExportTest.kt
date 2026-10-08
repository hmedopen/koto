package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.cards.spreadsheet.ExportFormat
import com.koto.app.feature.cards.spreadsheet.SkippedRow
import com.koto.app.ui.screens.cards.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeckSpreadsheetImportExportTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun deckEditorCardItemRendersCorrectlyWithFuriganaRomajiNotes() {
        var editedIndex = -1
        var deletedIndex = -1

        val card = CustomCardItem(
            id = "c1",
            japanese = "病院",
            romaji = "byouin",
            english = "Hospital",
            furigana = "びょういん",
            notes = "Healthcare / Facilities",
        )

        compose.setContent {
            DeckEditorCardItem(
                index = 1,
                card = card,
                onEdit = { editedIndex = 0 },
                onDelete = { deletedIndex = 0 },
            )
        }

        compose.onNodeWithTag("deck_card_index_c1").assertIsDisplayed().assertTextEquals("01")
        compose.onNodeWithTag("deck_card_furigana_c1").assertIsDisplayed().assertTextEquals("びょういん")
        compose.onNodeWithTag("deck_card_japanese_c1").assertIsDisplayed().assertTextEquals("病院")
        compose.onNodeWithTag("deck_card_romaji_c1").assertIsDisplayed().assertTextEquals("byouin")
        compose.onNodeWithTag("deck_card_english_c1").assertIsDisplayed().assertTextEquals("Hospital")
        compose.onNodeWithTag("deck_card_notes_c1").assertIsDisplayed().assertTextEquals("• Healthcare / Facilities")

        compose.onNodeWithTag("deck_card_edit_c1").assertIsDisplayed().performClick()
        assertEquals(0, editedIndex)

        compose.onNodeWithTag("deck_card_delete_c1").assertIsDisplayed().performClick()
        assertEquals(0, deletedIndex)
    }

    @Test
    fun deckEditorCardItemCollapsesEmptyFields() {
        val card = CustomCardItem(
            id = "c2",
            japanese = "水",
            romaji = "",
            english = "Water",
            furigana = "",
            notes = "",
        )

        compose.setContent {
            DeckEditorCardItem(
                index = 2,
                card = card,
                onEdit = {},
                onDelete = {},
            )
        }

        compose.onNodeWithTag("deck_card_index_c2").assertIsDisplayed().assertTextEquals("02")
        compose.onNodeWithTag("deck_card_japanese_c2").assertIsDisplayed().assertTextEquals("水")
        compose.onNodeWithTag("deck_card_english_c2").assertIsDisplayed().assertTextEquals("Water")

        // Collapsed fields must not exist
        compose.onNodeWithTag("deck_card_furigana_c2").assertDoesNotExist()
        compose.onNodeWithTag("deck_card_romaji_c2").assertDoesNotExist()
        compose.onNodeWithTag("deck_card_notes_c2").assertDoesNotExist()
    }

    @Test
    fun templateDownloadDialogDisplaysGuideDisclaimerAndActions() {
        var dismissed = false

        compose.setContent {
            TemplateDownloadDialog(
                onDismiss = { dismissed = true },
            )
        }

        compose.onNodeWithTag("template_download_dialog").assertIsDisplayed()
        compose.onNodeWithTag("btn_download_template").assertExists()
        compose.onNodeWithTag("btn_close_template_dialog").assertIsDisplayed().performClick()
        assertTrue("Dialog should be dismissed on close click", dismissed)
    }

    @Test
    fun importSummaryDialogDisplaysCountsAndSkippedRows() {
        var imported = false
        var cancelled = false

        val skipped = listOf(
            SkippedRow(14, "Row 14: Missing English translation"),
            SkippedRow(22, "Row 22: Missing Japanese text"),
        )

        compose.setContent {
            ImportSummaryDialog(
                validCount = 10,
                skippedRows = skipped,
                onImportValid = { imported = true },
                onCancel = { cancelled = true },
            )
        }

        compose.onNodeWithTag("import_summary_dialog").assertIsDisplayed()
        compose.onNodeWithTag("text_import_summary_subtitle")
            .assertIsDisplayed()
            .assertTextEquals("Ready to import: 10 cards | Skipped: 2 invalid rows")

        compose.onNodeWithTag("skipped_row_14").assertIsDisplayed()
        compose.onNodeWithTag("skipped_row_22").assertIsDisplayed()

        compose.onNodeWithTag("btn_summary_import").assertExists().performClick()
        assertTrue("Import confirmation should be invoked", imported)

        compose.onNodeWithTag("btn_summary_cancel").assertExists().performClick()
        assertTrue("Cancel should be invoked", cancelled)
    }

    @Test
    fun importErrorDialogDisplaysRootCauseAndActions() {
        var downloadTemplateClicked = false
        var tryAgainClicked = false

        compose.setContent {
            ImportErrorDialog(
                reason = "Missing required header columns 'Japanese' and 'English' in Row 1.",
                onDownloadTemplate = { downloadTemplateClicked = true },
                onTryAgain = { tryAgainClicked = true },
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("import_error_dialog").assertIsDisplayed()
        compose.onNodeWithTag("text_import_error_reason")
            .assertIsDisplayed()
            .assertTextEquals("Missing required header columns 'Japanese' and 'English' in Row 1.")

        compose.onNodeWithTag("btn_error_download_template").assertIsDisplayed().performClick()
        assertTrue(downloadTemplateClicked)

        compose.onNodeWithTag("btn_error_try_again").assertIsDisplayed().performClick()
        assertTrue(tryAgainClicked)
    }

    @Test
    fun exportDeckDialogOffersExcelAndCsvOptions() {
        var selectedFormat: ExportFormat? = null
        var dismissed = false

        compose.setContent {
            ExportDeckDialog(
                deckTitle = "Essential Verbs",
                cardCount = 25,
                onExport = { selectedFormat = it },
                onDismiss = { dismissed = true },
            )
        }

        compose.onNodeWithTag("export_deck_dialog").assertIsDisplayed()

        compose.onNodeWithTag("btn_export_xlsx").assertIsDisplayed().performClick()
        assertEquals(ExportFormat.XLSX, selectedFormat)

        compose.onNodeWithTag("btn_export_csv").assertIsDisplayed().performClick()
        assertEquals(ExportFormat.CSV, selectedFormat)

        compose.onNodeWithTag("btn_close_export_dialog").assertIsDisplayed().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun createDeckScreenDisplaysExportButtonAndOpensDialog() {
        compose.setContent {
            CreateDeckScreen(
                onBack = {},
                onDeckSaved = {},
            )
        }

        compose.onNodeWithTag("create_deck_export_button").assertIsDisplayed().performClick()
        compose.onNodeWithTag("export_deck_dialog").assertIsDisplayed()
        compose.onNodeWithTag("btn_export_xlsx").assertIsDisplayed()
        compose.onNodeWithTag("btn_export_csv").assertIsDisplayed()
    }
}
