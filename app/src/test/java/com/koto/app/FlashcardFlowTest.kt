package com.koto.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import com.koto.app.ui.screens.cards.loadFlashcardDecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlashcardFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun cardsHomeShowsDecksCatalogWithoutMixesBar() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithText("Small practice.\nLasting progress.").assertDoesNotExist()
        compose.onNodeWithText("Choose a deck. Make the words your own.").assertDoesNotExist()
        compose.onNodeWithTag("cards_section_decks").assertDoesNotExist()
        compose.onNodeWithTag("cards_section_mixes").assertDoesNotExist()
        compose.onNodeWithTag("deck_deck_01").assertExists()
        compose.onNodeWithTag("cards_starred_words").assertExists()
        compose.onNodeWithTag("cards_create_deck").assertExists()
        compose.onNodeWithTag("cards_random_deck").assertExists()
    }

    @Test fun starredWordsScreenOpensAndCloses() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithTag("cards_starred_words").performClick()
        compose.onNodeWithTag("starred_cards_screen").assertIsDisplayed()
        compose.onNodeWithTag("cards_detail_back").performClick()
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()
    }

    @Test fun starredWordsScreenDeferredUnstarring() {
        // 1. Open Deck 01 and favorite card_01_001
        openDeck()
        compose.waitForIdle()
        detailNode("favorite_card_01_001").performClick()
        compose.waitForIdle()

        // 2. Go back to cards grid
        back()
        if (compose.onAllNodesWithTag("deck_detail").fetchSemanticsNodes().isNotEmpty()) {
            back()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()

        // 3. Open Starred Words screen
        compose.onNodeWithTag("cards_starred_words").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("starred_cards_screen").assertIsDisplayed()
        compose.onNodeWithTag("favorite_card_01_001").assertIsDisplayed()

        // 4. Unstar the card: it must remain visible in the list during the active session!
        compose.onNodeWithTag("favorite_card_01_001").performClick()
        compose.waitForIdle()
        // Card is STILL displayed in the list (deferred unstarring)
        compose.onNodeWithTag("favorite_card_01_001").assertIsDisplayed()

        // 5. Navigate back to grid
        compose.onNodeWithTag("cards_detail_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()

        // 6. Re-open Starred Words screen: unstarred card is now removed
        compose.onNodeWithTag("cards_starred_words").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("favorite_card_01_001").assertDoesNotExist()
    }

    @Test fun createDeckScreenOpensAndCloses() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithTag("cards_create_deck").performClick()
        compose.onNodeWithTag("create_deck_back").assertIsDisplayed()
        compose.onNodeWithText("Create Deck").assertIsDisplayed()
        compose.onNodeWithTag("create_deck_save").assertIsDisplayed()
        compose.onNodeWithTag("create_deck_back").performClick()
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()
    }

    @Test fun createCustomDeckPersistsAndAppearsInGrid() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithTag("cards_create_deck").performClick()
        compose.onNodeWithTag("create_deck_save").assertIsNotEnabled()

        // Input title
        compose.onNodeWithTag("input_deck_title").performTextInput("Custom Colors")

        // Quick add card
        compose.onNodeWithTag("input_quick_kana").performTextInput("あか")
        compose.onNodeWithTag("input_quick_english").performTextInput("Red")
        compose.onNodeWithTag("btn_quick_add_save").performClick()

        // Verify card added to list and Save button is enabled
        compose.onNodeWithText("あか").assertIsDisplayed()
        compose.onNodeWithText("Red").assertIsDisplayed()
        compose.onNodeWithTag("create_deck_save").assertIsEnabled()

        // Save deck
        compose.onNodeWithTag("create_deck_save").performClick()
        compose.waitForIdle()

        // Navigates directly into the new deck detail or grid with the new deck
        compose.onNodeWithText("Custom Colors").assertIsDisplayed()
    }

    @Test fun continuousCardAdditionAndEditCardFlow() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithTag("cards_create_deck").performClick()

        // 1. First card addition
        compose.onNodeWithTag("input_quick_kana").performTextInput("みどり")
        compose.onNodeWithTag("input_quick_english").performTextInput("Green")
        compose.onNodeWithTag("btn_quick_add_save").performClick()

        // Verify first card in deck
        compose.onNodeWithText("みどり").assertIsDisplayed()
        compose.onNodeWithText("Green").assertIsDisplayed()

        // 2. Continuous second card addition on the spot
        compose.onNodeWithTag("input_quick_kana").performTextInput("くろ")
        compose.onNodeWithTag("input_quick_english").performTextInput("Black")
        compose.onNodeWithTag("btn_quick_add_save").performClick()

        // Verify both cards exist
        compose.onNodeWithText("みどり").assertIsDisplayed()
        compose.onNodeWithText("くろ").assertIsDisplayed()
        compose.onNodeWithText("Black").assertIsDisplayed()

        // 3. Edit single card flow
        compose.onAllNodesWithContentDescription("Edit Card")[0].performClick()
        compose.waitForIdle()

        // Verify on Edit Card screen
        compose.onNodeWithText("Edit Card").assertIsDisplayed()
        compose.onNodeWithTag("input_editor_english").performTextClearance()
        compose.onNodeWithTag("input_editor_english").performTextInput("Emerald Green")
        compose.onNodeWithTag("btn_edit_card_save").performClick()
        compose.waitForIdle()

        // Verify returned to overview and edited text is displayed
        compose.onNodeWithText("Create Deck").assertIsDisplayed()
        compose.onNodeWithText("Emerald Green").assertIsDisplayed()
    }

    @Test fun deckContentScreenBackArrowTakesToDeckDetailNotCatalog() {
        openDeck()
        compose.onNodeWithTag("deck_detail").assertIsDisplayed()
        try {
            compose.onNodeWithTag("deck_detail").performScrollToNode(hasTestTag("show_content"))
        } catch (_: Throwable) {}
        compose.onNodeWithTag("show_content").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Deck Content")
        compose.onNodeWithTag("content_table").assertIsDisplayed()
        compose.onNodeWithTag("cards_detail_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Cards")
        compose.onNodeWithTag("deck_detail").assertIsDisplayed()
        compose.onNodeWithTag("cards_detail_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()
    }

    @Test fun deckDetailHidesTabsAndKeepsStartAboveTheBottomEdge() {
        openDeck()
        compose.onNodeWithTag("tab_map").assertDoesNotExist()
        compose.onNodeWithTag("tab_learn").assertDoesNotExist()
        compose.onNodeWithTag("tab_cards").assertDoesNotExist()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Cards")
        val rootBottom = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val startBottom = compose.onNodeWithTag("start_flashcards").fetchSemanticsNode().boundsInRoot.bottom
        with(compose.activity.resources.displayMetrics) {
            assertTrue("Start button needs comfortable bottom clearance", rootBottom - startBottom >= 24f * density)
        }
    }

    @Test fun fixtureAndFullSessionFlow() {
        val decks = loadFlashcardDecks(compose.activity)
        assertEquals(50, decks.size)
        assertEquals(1257, decks.sumOf { it.cards.size })
        assertEquals(1257, decks.flatMap { it.cards }.map { it.id }.toSet().size)
        assertEquals("Greetings & Essential Courtesy", decks.first().title)
        assertEquals("Primary Adjectives II (Sensory & Living)", decks[31].title)
        assertEquals("Fundamental Action Verbs II (Daily Routine)", decks[13].title)
        openDeck()
        screenshot("flashcards-detail")
        detailNode("preview_card_01_001").assertIsDisplayed()
        screenshot("flashcards-preview")
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("rate_good").assertIsNotEnabled()
        compose.onNodeWithText("おはよう").assertIsDisplayed()
        screenshot("flashcards-study-front")
        val ratings = List(decks.first().cards.size) { index ->
            listOf("again", "hard", "good", "easy")[index % 4]
        }
        ratings.forEachIndexed { index, rating ->
            compose.onNodeWithTag("study_card").performClick()
            if (index == 0) {
                compose.onNodeWithText("Good morning (casual)").assertIsDisplayed()
                screenshot("flashcards-study-answer")
            }
            compose.onNodeWithTag("rate_$rating").performClick()
        }
        compose.onNodeWithTag("study_complete").assertIsDisplayed()
        screenshot("flashcards-complete")
        compose.onNodeWithTag("back_to_deck").performClick()
        compose.onNodeWithTag("stat_weak").assertTextContains("13")
        compose.onNodeWithTag("stat_mastered").assertTextContains("12")
    }

    @Test fun studyCloseReturnsToDeck() {
        openDeck()
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("study_card").assertHasClickAction()
        compose.onNodeWithTag("study_card").performTouchInput { click(Offset(24f, 24f)) }
        compose.onNodeWithTag("rate_again").performClick()
        compose.onNodeWithTag("cards_back").performClick()
        compose.onNodeWithText("Quit this session?").assertIsDisplayed()
        compose.onNodeWithText("Your card stats will not be saved.").assertIsDisplayed()
        compose.onNodeWithTag("keep_studying").performClick()
        compose.onNodeWithTag("study_card").assertIsDisplayed()
        compose.onNodeWithTag("cards_back").performClick()
        compose.onNodeWithTag("quit_session").performClick()
        compose.onNodeWithTag("deck_detail").assertIsDisplayed()
        compose.onNodeWithTag("top_bar_title").assertIsDisplayed()
        compose.onNodeWithTag("stat_weak").assertTextContains("1")
    }

    @Test fun optionsFavoritesAndSessionSurviveRecreation() {
        openDeck()
        val romaji = detailNode("romaji_toggle").fetchSemanticsNode().boundsInRoot
        val shuffle = detailNode("shuffle_toggle").fetchSemanticsNode().boundsInRoot
        assertTrue("Session options should share one row", kotlin.math.abs(romaji.top - shuffle.top) < 2f)
        assertEquals(romaji.width, shuffle.width, 2f)
        compose.onNodeWithText("Show Romaji: On").assertIsDisplayed()
        compose.onNodeWithText("Shuffle: Off").assertIsDisplayed()
        detailNode("romaji_toggle").performClick()
        detailNode("shuffle_toggle").performClick()
        detailNode("english_first").performClick()
        detailNode("favorite_card_01_001").performClick()
        compose.onNodeWithTag("favorite_card_01_001").assertIsSelected()
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("top_bar_title").assertDoesNotExist()
        compose.onNodeWithTag("cards_back").assertIsDisplayed()
        compose.onNodeWithTag("cards_settings").assertIsDisplayed()
        val progress = compose.onNodeWithTag("study_progress").fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithTag("cards_back").fetchSemanticsNode().boundsInRoot
        val settings = compose.onNodeWithTag("cards_settings").fetchSemanticsNode().boundsInRoot
        assertTrue("Progress should sit between the two controls", close.right <= progress.left && progress.right <= settings.left)
        assertEquals(close.top, settings.top, 2f)
        assertEquals(close.width, settings.width, 2f)
        assertEquals(close.height, settings.height, 2f)
        val audio = compose.onNodeWithTag("play_audio").fetchSemanticsNode().boundsInRoot
        assertEquals(close.width, audio.width, 2f)
        assertEquals(close.height, audio.height, 2f)
        val card = compose.onNodeWithTag("study_card").fetchSemanticsNode().boundsInRoot
        assertTrue("Audio should sit below the card", audio.top > card.bottom)
        compose.onNodeWithTag("cards_settings").performClick()
        compose.onNodeWithTag("settings_sheet").assertIsDisplayed()
        compose.onNodeWithTag("settings_done").performClick()
        compose.onNodeWithTag("settings_sheet").assertDoesNotExist()
        val firstWord = compose.onNodeWithTag("card_word", useUnmergedTree = true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        compose.onNodeWithTag("study_card").performClick()
        compose.onNodeWithTag("card_romaji", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("rate_good").assertIsEnabled()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("rate_good").assertIsEnabled()
        compose.onNodeWithTag("card_romaji", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("study_card").performClick()
        compose.onNodeWithText(firstWord).assertIsDisplayed()
        back()
        compose.onNodeWithTag("deck_detail").assertIsDisplayed()
        detailNode("romaji_toggle").assertIsOff()
        compose.onNodeWithTag("shuffle_toggle").assertIsOn()
        compose.onNodeWithTag("english_first").assertIsSelected()
        detailNode("favorite_card_01_001").assertIsSelected()
        back()
        compose.onNodeWithTag("deck_detail").assertIsDisplayed()
        back()
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()
        back()
        compose.onNodeWithTag("tab_learn").assertIsSelected()
    }

    @Test fun deckHeartDoesNotOpenDeckAndSurvivesRecreation() {
        compose.onNodeWithTag("tab_cards").performClick()
        val greetings = compose.onNodeWithTag("deck_deck_01").fetchSemanticsNode().boundsInRoot
        val numbers = compose.onNodeWithTag("deck_deck_02").fetchSemanticsNode().boundsInRoot
        assertTrue("Decks should share a grid row", kotlin.math.abs(greetings.top - numbers.top) < 2f)
        assertTrue("Decks should occupy separate columns", greetings.right <= numbers.left)
        screenshot("flashcards-grid-square")
        assertTrue("Deck tiles should be close to square: ${greetings.width} × ${greetings.height}",
            greetings.height / greetings.width in 0.9f..1.18f)
        compose.onAllNodesWithText("Tap to practice").assertCountEquals(0)
        compose.onNodeWithTag("favorite_deck_deck_02").performClick()
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("favorite_deck_deck_02").assertIsSelected()
        screenshot("flashcards-grid")
    }

    @Test fun deckTilesKeepOneHeightAcrossTitleLengths() {
        compose.onNodeWithTag("tab_cards").performClick()
        val grid = compose.onNodeWithTag("cards_grid")
        fun height(id: String): Float {
            grid.performScrollToNode(hasTestTag("deck_$id"))
            return compose.onNodeWithTag("deck_$id").fetchSemanticsNode().boundsInRoot.height
        }
        val expected = height("deck_14")
        (1..50).forEach { number ->
            val id = "deck_${number.toString().padStart(2, '0')}"
            assertEquals("$id should match the three-line tile", expected, height(id), 2f)
        }
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun twoColumnGridKeepsProgressLabelsReadableAtDoubleFontScale() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithTag("cards_grid").performScrollToNode(hasTestTag("deck_deck_01"))
        compose.onNodeWithTag("deck_deck_01").assertIsDisplayed()
        val heart = compose.onNodeWithTag("favorite_deck_deck_01").fetchSemanticsNode().boundsInRoot
        val minTarget = with(compose.density) { 48.dp.toPx() }
        assertTrue("Heart touch target should remain full-size", heart.width >= minTarget && heart.height >= minTarget)
        screenshot("flashcards-grid-large-text")
        val mastered = compose.onAllNodesWithText("Mastered", useUnmergedTree = true).onFirst()
        mastered.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { getLayout ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            assertTrue(getLayout(layouts))
            assertTrue(layouts.none { it.hasVisualOverflow })
        }
    }

    @Test fun gradingStaysAvailableAfterFlipBackAndResetsOnNextCard() {
        openDeck()
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("play_audio").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rate_good").assertIsNotEnabled()
        compose.onNodeWithTag("study_card").performClick()
        compose.onNodeWithTag("rate_good").assertIsEnabled()
        compose.onNodeWithTag("study_card").performClick()
        compose.onNodeWithText("おはよう").assertIsDisplayed()
        compose.onNodeWithTag("rate_good").assertIsEnabled()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("rate_good").assertIsEnabled().performClick()
        compose.onNodeWithText("おはようございます").assertIsDisplayed()
        compose.onNodeWithTag("rate_good").assertIsNotEnabled()
    }

    @Test fun recycledPreviewFavoritesKeepTheLatestOptionsAndOtherFavorites() {
        openDeck()
        detailNode("favorite_card_01_001").performClick()
        detailNode("preview_card_01_025").assertIsDisplayed()
        detailNode("romaji_toggle").performClick()
        detailNode("shuffle_toggle").performClick()
        detailNode("english_first").performClick()
        detailNode("favorite_card_01_002").performClick()
        detailNode("favorite_card_01_001").assertIsSelected()
        detailNode("favorite_card_01_002").assertIsSelected()
        detailNode("romaji_toggle").assertIsOff()
        detailNode("shuffle_toggle").assertIsOn()
        detailNode("english_first").assertIsSelected()
    }

    @Test fun fastScrollStartingOnFavoritesDoesNotActivateThem() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithTag("favorite_deck_deck_01").performTouchInput {
            swipe(center, Offset(center.x, center.y - 400f), durationMillis = 130)
        }
        compose.onNodeWithTag("cards_grid").assertIsDisplayed()
        compose.onNodeWithTag("cards_grid").performScrollToNode(hasTestTag("deck_deck_01"))
        compose.onNodeWithTag("favorite_deck_deck_01").assertIsNotSelected()
        compose.onNodeWithTag("deck_deck_01").performClick()
        detailNode("favorite_card_01_001").performTouchInput {
            swipe(center, Offset(center.x, center.y - 400f), durationMillis = 130)
        }
        detailNode("favorite_card_01_001").assertIsNotSelected()
        back()
        compose.onNodeWithTag("deck_detail").assertIsDisplayed()
        compose.onNodeWithTag("start_flashcards").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
    fun landscapeGridAndPreviewKeepTheirLayout() {
        compose.onNodeWithTag("tab_cards").performClick()
        screenshot("flashcards-landscape-grid")
        compose.onNodeWithTag("deck_deck_01").performClick()
        detailNode("preview_card_01_001").assertIsDisplayed()
        screenshot("flashcards-landscape-preview")
        if (compose.onAllNodesWithTag("content_drawer").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("content_drawer").performSemanticsAction(SemanticsActions.Dismiss)
            compose.waitForIdle()
        }
        try {
            compose.onNodeWithTag("deck_detail").performScrollToNode(hasTestTag("start_flashcards"))
        } catch (_: Throwable) {}
        compose.onNodeWithTag("start_flashcards").assertIsDisplayed()
    }

    @Test fun startButtonCompressesAndCancelledPressDoesNotStartSession() {
        openDeck()
        val label = compose.onNodeWithText("START REVIEW", useUnmergedTree = true)
        val restingTop = label.fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("start_flashcards").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(250)
        val pressedTop = label.fetchSemanticsNode().boundsInRoot.top
        assertTrue("The face must move down on press", pressedTop > restingTop + 1f)
        screenshot("flashcards-start-pressed")
        compose.onNodeWithTag("start_flashcards").performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(500)
        assertEquals(restingTop, label.fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.onNodeWithTag("deck_detail").assertIsDisplayed()
        compose.mainClock.autoAdvance = true
        // The CTA remains reachable after scrolling to the final preview.
        detailNode("preview_card_01_025").assertIsDisplayed()
        if (compose.onAllNodesWithTag("content_drawer").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("content_drawer").performSemanticsAction(SemanticsActions.Dismiss)
            compose.waitForIdle()
        }
        compose.onNodeWithTag("start_flashcards").assertIsDisplayed().performClick()
        compose.onNodeWithTag("flashcard_study").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun compactLayoutHasReachableStudyControls() {
        openDeck()
        detailNode("preview_card_01_001").assertIsDisplayed()
        screenshot("flashcards-preview-compact")
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("study_card").performClick()
        listOf("again", "hard", "good", "easy").forEach { compose.onNodeWithTag("rate_$it").assertIsDisplayed() }
        screenshot("flashcards-compact")
    }

    @Test @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
    fun landscapeCanRevealAndRate() {
        openDeck()
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("study_card").performScrollTo().performClick()
        compose.onNodeWithTag("rate_good").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("おはようございます").performScrollTo().assertIsDisplayed()
        screenshot("flashcards-landscape")
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun doubleFontScaleCanStartRevealAndRate() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        openDeck()
        detailNode("preview_card_01_001").assertIsDisplayed()
        screenshot("flashcards-preview-large-text")
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("study_card").performScrollTo().performClick()
        compose.onNodeWithTag("rate_easy").performScrollTo().assertIsDisplayed()
        screenshot("flashcards-large-text")
        compose.onNodeWithTag("rate_easy").performClick()
        compose.onNodeWithText("おはようございます").performScrollTo().assertIsDisplayed()
    }

    @Test fun englishSideAudioPlaysWithoutIssue() {
        openDeck()
        detailNode("english_first").performClick()
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("play_audio").assertIsDisplayed()
        compose.onNodeWithTag("play_audio").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("cards_back").performClick()
        compose.onNodeWithTag("quit_session").performClick()
    }

    @Test fun quickTranslationsDeckShowsInfoAndCanBeDeleted() {
        val context = compose.activity
        val store = com.koto.app.feature.translator.data.TranslatorCardStore(context)
        store.toggleStar(
            sourceText = "spoon",
            targetText = "スプーン",
            targetRomaji = "supuun",
            sourceLang = com.koto.app.feature.translator.model.TranslationLanguage.English,
            targetLang = com.koto.app.feature.translator.model.TranslationLanguage.Japanese,
        )

        compose.onNodeWithTag("tab_cards").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Quick Translations").assertIsDisplayed()
        compose.onNodeWithText("Quick Translations").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Auto-synced from your starred translations in Learn → Translate").assertIsDisplayed()
        compose.onNodeWithTag("btn_delete_deck").performClick()
        compose.onNodeWithText("Delete Quick Translations?").assertIsDisplayed()
        compose.onNodeWithTag("btn_confirm_delete_deck").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Quick Translations").assertDoesNotExist()
    }

    @Test fun deleteDeckDialogCancelLeavesDeckIntact() {
        val context = compose.activity
        com.koto.app.feature.translator.data.TranslatorCardStore(context).toggleStar(
            sourceText = "fork",
            targetText = "フォーク",
            targetRomaji = "fooku",
            sourceLang = com.koto.app.feature.translator.model.TranslationLanguage.English,
            targetLang = com.koto.app.feature.translator.model.TranslationLanguage.Japanese,
        )

        compose.onNodeWithTag("tab_cards").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Quick Translations").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("btn_delete_deck").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Delete Quick Translations?").assertIsDisplayed()
        compose.onNodeWithTag("btn_cancel_delete_deck").assertIsDisplayed()
        compose.onNodeWithTag("btn_confirm_delete_deck").assertIsDisplayed()

        // Cancel
        compose.onNodeWithTag("btn_cancel_delete_deck").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Delete Quick Translations?").assertDoesNotExist()
        compose.onNodeWithText("Quick Translations").assertIsDisplayed()
    }

    private fun detailNode(tag: String): SemanticsNodeInteraction {
        if (tag.startsWith("preview_") || tag.startsWith("favorite_card_")) {
            if (compose.onAllNodesWithTag("content_table").fetchSemanticsNodes().isEmpty()) {
                try {
                    compose.onNodeWithTag("deck_detail").performScrollToNode(hasTestTag("show_content"))
                } catch (_: Throwable) {}
                compose.onNodeWithTag("show_content").performClick()
                compose.waitForIdle()
            }
            if (compose.onAllNodesWithTag("content_table").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("content_table").performScrollToNode(hasTestTag(tag))
            }
            return compose.onNodeWithTag(tag)
        }
        if (compose.onAllNodesWithTag("content_drawer").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("content_drawer").performSemanticsAction(SemanticsActions.Dismiss)
            compose.waitForIdle()
        }
        try {
            compose.onNodeWithTag("deck_detail").performScrollToNode(hasTestTag(tag))
        } catch (_: Throwable) {}
        return compose.onNodeWithTag(tag)
    }

    private fun openDeck() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithTag("cards_grid").performScrollToNode(hasTestTag("deck_deck_01"))
        compose.onNodeWithTag("deck_deck_01").performClick()
    }
    private fun back() = compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    private fun screenshot(name: String) { compose.waitForIdle(); saveRenderedScreenshot(compose.activity, name) }
}
