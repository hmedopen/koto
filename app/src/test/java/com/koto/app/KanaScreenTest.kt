package com.koto.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.kana.data.KanaMasteryStore
import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.model.KanaScript
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KanaScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("koto_kana_store", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun kanaRepositoryIntegrity() {
        val allRows = KanaRepository.rows
        assertEquals("Must contain 27 rows across all 4 groups", 27, allRows.size)

        val basicRows = KanaRepository.basicRows
        assertEquals("Must contain exactly 11 Basic Kana rows", 11, basicRows.size)

        val expectedBasicLabels = listOf("A-", "K-", "S-", "T-", "N-", "H-", "M-", "Y-", "R-", "W-", "N")
        assertEquals(expectedBasicLabels, basicRows.map { it.label })

        val allChars = KanaRepository.allCharacters
        assertEquals("Must contain 104 total syllabary characters", 104, allChars.size)

        val basicChars = basicRows.flatMap { it.columns.filterNotNull() }
        assertEquals("Must contain exactly 46 basic Gojuon characters", 46, basicChars.size)

        // Verify gaps in Y-row (3 characters: ya, yu, yo)
        val yRow = basicRows.first { it.id == "row_y" }
        assertEquals(5, yRow.columns.size)
        assertNotNull(yRow.columns[0]) // YA
        assertNull(yRow.columns[1])    // gap
        assertNotNull(yRow.columns[2]) // YU
        assertNull(yRow.columns[3])    // gap
        assertNotNull(yRow.columns[4]) // YO

        // Verify gaps in W-row (2 characters: wa, wo)
        val wRow = basicRows.first { it.id == "row_w" }
        assertEquals(5, wRow.columns.size)
        assertNotNull(wRow.columns[0]) // WA
        assertNull(wRow.columns[1])    // gap
        assertNull(wRow.columns[2])    // gap
        assertNull(wRow.columns[3])    // gap
        assertNotNull(wRow.columns[4]) // WO

        // Verify character navigation (next / previous)
        val firstChar = allChars.first() // A
        val secondChar = allChars[1]     // I
        val lastChar = allChars.last()
        assertEquals(secondChar, KanaRepository.findNext(firstChar))
        assertEquals(lastChar, KanaRepository.findPrevious(firstChar))
        assertEquals(firstChar, KanaRepository.findNext(lastChar))

        // Verify stroke breakdown steps
        assertTrue(firstChar.strokeStepsHira.isNotEmpty())
        assertTrue(firstChar.strokeStepsKata.isNotEmpty())
        assertEquals("A", firstChar.romaji)
        assertEquals("あ", firstChar.hiragana)
        assertEquals("ア", firstChar.katakana)
    }

    @Test
    fun kanaMasteryStorePersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = KanaMasteryStore(context)

        // Default seed is 46 Hiragana characters mastered
        assertEquals(46, store.masteredCount)

        // Toggle Katakana A to mastered
        val newState = store.toggleMastered("kana_a", isHiragana = false)
        assertTrue(newState)
        assertEquals(47, store.masteredCount)

        // Toggle back off
        val stateOff = store.toggleMastered("kana_a", isHiragana = false)
        assertFalse(stateOff)
        assertEquals(46, store.masteredCount)

        // Test character granular selection
        store.selectAllCharacters()
        assertEquals(104, store.selectedKanaIds.size)

        store.clearAllCharacters()
        assertTrue(store.selectedKanaIds.isEmpty())

        store.toggleCharacterSelected("kana_a", isHiragana = true)
        assertTrue(store.isCharacterSelected("kana_a", isHiragana = true))
        assertFalse("Katakana selection must be separate from Hiragana", store.isCharacterSelected("kana_a", isHiragana = false))
        assertEquals(1, store.selectedHiraganaIds.size)
        assertEquals(0, store.selectedKatakanaIds.size)
    }

    @Test
    fun kanaLearnCardAndScreenFlow() {
        // 1. Verify Kana Card on Learn screen
        compose.onNodeWithTag("learn_kana_button").assertIsDisplayed()
        compose.onNodeWithTag("learn_kana_title", useUnmergedTree = true).assertTextEquals("Kana")

        // 2. Click to open KanaScreen
        compose.onNodeWithTag("learn_kana_button").performClick()
        compose.waitForIdle()

        // 3. Verify Kana Top App Bar and Tabs
        compose.onNodeWithTag("screen_kana").assertIsDisplayed()
        compose.onNodeWithTag("kana_screen_title").assertTextEquals("Kana")
        compose.onNodeWithTag("btn_kana_back").assertIsDisplayed()
        compose.onNodeWithTag("btn_kana_settings").assertIsDisplayed()

        // 4. Verify Chart Tab Elements
        compose.onNodeWithTag("toggle_script_hiragana").assertIsDisplayed()
        compose.onNodeWithTag("toggle_script_katakana").assertIsDisplayed()
        compose.onNodeWithTag("kana_category_header_basic").assertIsDisplayed()
        compose.onNodeWithTag("kana_column_header_basic_-A").assertIsDisplayed()
        compose.onNodeWithTag("kana_column_header_basic_-O").assertIsDisplayed()

        // 5. Click Tile to Open Inspection Modal
        compose.onNodeWithTag("kana_tile_kana_a").performClick()
        compose.waitForIdle()

        // Modal should display character details with raised 3D Close button
        compose.onNodeWithTag("kana_inspect_sheet").assertIsDisplayed()
        compose.onNodeWithTag("kana_inspect_romaji_title").assertTextEquals("A")
        compose.onNodeWithTag("kana_inspect_large_glyph").assertTextEquals("あ")
        compose.onNodeWithTag("btn_kana_inspect_audio").assertIsDisplayed()
        compose.onNodeWithTag("btn_kana_inspect_trace").assertIsDisplayed()

        // Flip Script in Modal
        compose.onNodeWithTag("btn_kana_inspect_flip_script").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_inspect_large_glyph").assertTextEquals("ア")

        // Next character in Modal
        compose.onNodeWithTag("btn_kana_inspect_next").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_inspect_romaji_title").assertTextEquals("I")

        // Close Modal
        compose.onNodeWithTag("btn_close_kana_inspect").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_inspect_sheet").assertDoesNotExist()

        // 6. Switch to Practice Tab
        compose.onNodeWithTag("tab_kana_practice").performClick()
        compose.waitForIdle()

        // Practice Dashboard controls & metrics
        compose.onNodeWithTag("kana_practice_tab").assertIsDisplayed()
        compose.onNodeWithTag("practice_header_title").assertIsDisplayed()
        compose.onNodeWithTag("card_stat_selected_kana").assertIsDisplayed()
        compose.onNodeWithTag("card_stat_available_words").assertIsDisplayed()

        // Solid-colored Quiz Catalog cards
        compose.onNodeWithTag("quiz_card_mixed").assertExists()
        compose.onNodeWithTag("quiz_card_test").assertExists()
        compose.onNodeWithTag("quiz_card_listening").assertExists()
        compose.onNodeWithTag("quiz_card_word_selection").assertExists()
        compose.onNodeWithTag("quiz_card_matching_pairs").assertExists()
        compose.onNodeWithTag("quiz_card_word_building").assertExists()
        compose.onNodeWithTag("quiz_card_typing").assertExists()

        // 7. Verify Granular Filter Modal interaction via 3D Selected Kana button
        compose.onNodeWithTag("card_stat_selected_kana").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_filter_modal").assertIsDisplayed()
        compose.onNodeWithTag("btn_close_kana_filter").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_filter_modal").assertDoesNotExist()

        // Verify Word Bank interaction via 3D Available Words button
        compose.onNodeWithTag("card_stat_available_words").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_word_bank_view").assertIsDisplayed()
        compose.onNodeWithTag("btn_word_bank_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_word_bank_view").assertDoesNotExist()

        // 8. Launch Quiz Mode and verify separate screen transition
        compose.onNodeWithTag("quiz_card_test").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_quiz_session_view").assertIsDisplayed()
        compose.onNodeWithTag("quiz_progress", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("btn_quiz_exit").assertIsDisplayed()

        // Exit Quiz back to Practice Dashboard
        compose.onNodeWithTag("btn_quiz_exit").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("kana_quiz_session_view").assertDoesNotExist()
        compose.onNodeWithTag("kana_practice_tab").assertIsDisplayed()

        // 9. Test Back navigation from KanaScreen
        compose.onNodeWithTag("btn_kana_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("screen_kana").assertDoesNotExist()
        compose.onNodeWithTag("learn_kana_button").assertIsDisplayed()
    }
}
