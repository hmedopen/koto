package com.koto.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.translator.data.MockTranslationEngine
import com.koto.app.feature.translator.data.TranslatorCardStore
import com.koto.app.feature.translator.model.TranslationLanguage
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
class TranslatorScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TranslatorCardStore(context).clearAll()
    }

    @Test
    fun mockTranslationEngineBidirectionalAndRomajiIntegrity() {
        // English to Japanese
        val helloResult = MockTranslationEngine.translate(
            text = "hello",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )
        assertEquals("こんにちは", helloResult.translatedText)
        assertEquals("konnichiwa", helloResult.romaji)

        val friendResult = MockTranslationEngine.translate(
            text = "hello, friend",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )
        assertEquals("こんにちは、 友よ", friendResult.translatedText)
        assertEquals("konnichiwa, tomoyo", friendResult.romaji)

        // Japanese to English
        val jpResult = MockTranslationEngine.translate(
            text = "ありがとう",
            sourceLanguage = TranslationLanguage.Japanese,
            targetLanguage = TranslationLanguage.English,
        )
        assertEquals("Thanks", jpResult.translatedText)
        assertEquals("arigatou", jpResult.romaji)

        // Empty input returns blank
        val emptyResult = MockTranslationEngine.translate(
            text = "   ",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )
        assertTrue(emptyResult.translatedText.isEmpty())
    }

    @Test
    fun translatorCardStoreSavesAndTogglesStars() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = TranslatorCardStore(context)

        assertFalse(store.isStarred("hello", "こんにちは"))

        // Toggle on
        val starred = store.toggleStar(
            sourceText = "hello",
            targetText = "こんにちは",
            targetRomaji = "konnichiwa",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )
        assertTrue(starred)
        assertTrue(store.isStarred("hello", "こんにちは"))

        val cards = store.loadStarredCards()
        assertTrue(cards.any { it.sourceText == "hello" && it.targetText == "こんにちは" })

        // Toggle off
        val unstarred = store.toggleStar(
            sourceText = "hello",
            targetText = "こんにちは",
            targetRomaji = "konnichiwa",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )
        assertFalse(unstarred)
        assertFalse(store.isStarred("hello", "こんにちは"))
    }

    @Test
    fun learnTabHasProminentTranslateCardAndNavigatesToTranslator() {
        compose.onNodeWithTag("tab_learn").performClick()
        compose.onNodeWithTag("learn_translate_button").assertIsDisplayed()
        compose.onNodeWithTag("learn_translate_title", useUnmergedTree = true).assertTextEquals("Translate")
        compose.onNodeWithTag("learn_translate_subtitle", useUnmergedTree = true).assertTextEquals("English ⇄ Japanese")

        // Open Translator
        compose.onNodeWithTag("learn_translate_button").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("translator_screen").assertIsDisplayed()
        compose.onNodeWithTag("translator_header_title").assertIsDisplayed()
        compose.onNodeWithTag("translator_view_starred_button").assertIsDisplayed()
        compose.onNodeWithTag("translator_dismiss_button").assertIsDisplayed()
        compose.onNodeWithTag("translator_idle_input_field").assertIsDisplayed()
        compose.onNodeWithTag("translator_bottom_language_bar").assertIsDisplayed()

        // Dismiss back to Learn tab
        compose.onNodeWithTag("translator_dismiss_button").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("screen_learn").assertIsDisplayed()
        compose.onNodeWithTag("learn_translate_button").assertIsDisplayed()
    }

    @Test
    fun translatorScreenStateTransitionsAndInteractions() {
        // Open Translator from Learn tab
        compose.onNodeWithTag("tab_learn").performClick()
        compose.onNodeWithTag("learn_translate_button").performClick()
        compose.waitForIdle()

        // STATE A: Idle state verification
        compose.onNodeWithTag("translator_idle_input_field").assertIsDisplayed()
        compose.onNodeWithTag("translator_source_lang_text").assertTextEquals("English")
        compose.onNodeWithTag("translator_target_lang_text").assertTextEquals("Japanese")

        // Type text to enter STATE B: Active translation
        compose.onNodeWithTag("translator_idle_input_field").performTextInput("hello")
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()

        // Verify State B layout
        compose.onNodeWithTag("translator_active_back_button").assertIsDisplayed()
        compose.onNodeWithTag("translator_clear_button").assertIsDisplayed()
        compose.onNodeWithTag("translator_star_toggle_button").assertIsDisplayed()
        compose.onNodeWithTag("translator_overflow_button").assertIsDisplayed()

        // Source & Target section verification
        compose.onNodeWithTag("translator_source_label").assertTextEquals("ENGLISH")
        compose.onNodeWithTag("translator_target_label").assertTextEquals("JAPANESE")
        compose.onNodeWithTag("translator_offline_indicator").assertIsDisplayed()
        compose.onNodeWithTag("translator_target_text").assertTextEquals("こんにちは")
        compose.onNodeWithTag("translator_target_romaji").assertTextEquals("konnichiwa")

        // Audio & Copy buttons
        compose.onNodeWithTag("translator_source_audio").assertIsDisplayed()
        compose.onNodeWithTag("translator_source_copy").assertIsDisplayed()
        compose.onNodeWithTag("translator_target_audio").assertIsDisplayed()
        compose.onNodeWithTag("translator_target_copy").assertIsDisplayed()

        // Toggle Star
        compose.onNodeWithTag("translator_star_toggle_button").performClick()
        compose.waitForIdle()

        // Swap languages button
        compose.onNodeWithTag("translator_swap_languages_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("translator_source_lang_text").assertTextEquals("Japanese")
        compose.onNodeWithTag("translator_target_lang_text").assertTextEquals("English")
        compose.onNodeWithTag("translator_target_romaji").assertDoesNotExist()

        // Clear button resets back to State A
        compose.onNodeWithTag("translator_clear_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("translator_idle_input_field").assertIsDisplayed()
    }

    @Test
    fun savedTranslationsDialogOpensAndCloses() {
        compose.onNodeWithTag("tab_learn").performClick()
        compose.onNodeWithTag("learn_translate_button").performClick()
        compose.waitForIdle()

        // Tap Star (⭐) in State A
        compose.onNodeWithTag("translator_view_starred_button").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("saved_translations_screen").assertIsDisplayed()
        compose.onNodeWithTag("saved_translations_dismiss").assertIsDisplayed()

        // Dismiss
        compose.onNodeWithTag("saved_translations_dismiss").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("saved_translations_screen").assertDoesNotExist()
        compose.onNodeWithTag("translator_screen").assertIsDisplayed()
    }

    @Test
    fun topStarButtonAlwaysViewsSavedTranslationsAndHistoryStarToggles() {
        compose.onNodeWithTag("tab_learn").performClick()
        compose.onNodeWithTag("learn_translate_button").performClick()
        compose.waitForIdle()

        // In idle mode: top star is view-only
        compose.onNodeWithTag("translator_view_starred_button").assertIsDisplayed()

        // Type to enter active mode
        compose.onNodeWithTag("translator_idle_input_field").performTextInput("hello")
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()

        // In active mode: BOTH top star (view only) AND source star toggle button exist!
        compose.onNodeWithTag("translator_view_starred_button").assertIsDisplayed()
        compose.onNodeWithTag("translator_star_toggle_button").assertIsDisplayed()

        // Clicking top star opens saved translations dialog (view only)
        compose.onNodeWithTag("translator_view_starred_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("saved_translations_screen").assertIsDisplayed()
        compose.onNodeWithTag("saved_translations_dismiss").performClick()
        compose.waitForIdle()

        // Clicking source star toggle button stars the item
        compose.onNodeWithTag("translator_star_toggle_button").performClick()
        compose.waitForIdle()

        // Open history dialog
        compose.onNodeWithTag("translator_history_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("translation_history_dialog").assertIsDisplayed()

        // Dismiss history dialog
        compose.onNodeWithTag("history_dismiss_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("translator_screen").assertIsDisplayed()
    }

    @Test
    fun bookmarksScreenTitleAndDeferredUnstarring() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = TranslatorCardStore(context)
        store.toggleStar(
            sourceText = "apple",
            targetText = "りんご",
            targetRomaji = "ringo",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )

        compose.onNodeWithTag("tab_learn").performClick()
        compose.onNodeWithTag("learn_translate_button").performClick()
        compose.waitForIdle()

        // Open Bookmarks dialog
        compose.onNodeWithTag("translator_view_starred_button").performClick()
        compose.waitForIdle()

        // 1. Verify centered title is "Bookmarks"
        compose.onNodeWithTag("saved_translations_title").assertTextEquals("Bookmarks")
        compose.onNodeWithText("りんご").assertIsDisplayed()

        // 2. Click star to unstar - item MUST remain displayed in dialog (deferred removal)
        compose.onNodeWithContentDescription("Remove bookmark").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("りんご").assertIsDisplayed()
        compose.onNodeWithContentDescription("Restore bookmark").assertIsDisplayed()

        // 3. Dismiss dialog - removal is now finalized
        compose.onNodeWithTag("saved_translations_dismiss").performClick()
        compose.waitForIdle()

        assertFalse(store.isStarred("apple", "りんご"))
    }

    @Test
    fun targetEnglishNeverDisplaysRomaji() {
        compose.onNodeWithTag("tab_learn").performClick()
        compose.onNodeWithTag("learn_translate_button").performClick()
        compose.waitForIdle()

        // Swap to Japanese -> English direction
        compose.onNodeWithTag("translator_swap_languages_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("translator_source_lang_text").assertTextEquals("Japanese")
        compose.onNodeWithTag("translator_target_lang_text").assertTextEquals("English")

        // Enter Japanese input "ありがとう"
        compose.onNodeWithTag("translator_idle_input_field").performTextInput("ありがとう")
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()

        // Verify target is English
        compose.onNodeWithTag("translator_target_label").assertTextEquals("ENGLISH")
        compose.onNodeWithTag("translator_target_text").assertTextEquals("Thanks")

        // Romaji MUST NOT exist when target is English
        compose.onNodeWithTag("translator_target_romaji").assertDoesNotExist()
    }
}

