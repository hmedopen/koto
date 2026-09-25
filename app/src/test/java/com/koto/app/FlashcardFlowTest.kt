package com.koto.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    @Test fun cardsHomeUsesDecksAndMixesWithoutHeroCopy() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.onNodeWithText("Small practice.\nLasting progress.").assertDoesNotExist()
        compose.onNodeWithText("Choose a deck. Make the words your own.").assertDoesNotExist()
        compose.onNodeWithTag("cards_section_decks").assertIsSelected()
        compose.onNodeWithTag("deck_deck_01").assertExists()
        compose.onNodeWithTag("cards_section_mixes").performClick().assertIsSelected()
        compose.onNodeWithTag("deck_deck_01").assertDoesNotExist()
    }

    @Test fun fixtureAndFullSessionFlow() {
        val decks = loadFlashcardDecks(compose.activity)
        assertEquals(41, decks.size)
        assertEquals(609, decks.sumOf { it.cards.size })
        assertEquals(609, decks.flatMap { it.cards }.map { it.id }.toSet().size)
        assertEquals("Greetings & Courtesy", decks.first().title)
        assertEquals("Work, Office & Business", decks[31].title)
        assertEquals("Essential Verbs: Interactions & Transactions", decks[13].title)
        openDeck()
        screenshot("flashcards-detail")
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("rate_good").assertIsNotEnabled()
        compose.onNodeWithText("おはようございます").assertIsDisplayed()
        screenshot("flashcards-study-front")
        val ratings = List(decks.first().cards.size) { index ->
            listOf("again", "hard", "good", "easy")[index % 4]
        }
        ratings.forEachIndexed { index, rating ->
            compose.onNodeWithTag("study_card").performClick()
            if (index == 0) {
                compose.onNodeWithText("Good morning (formal)").assertIsDisplayed()
                screenshot("flashcards-study-answer")
            }
            compose.onNodeWithTag("rate_$rating").performClick()
        }
        compose.onNodeWithTag("study_complete").assertIsDisplayed()
        screenshot("flashcards-complete")
        compose.onNodeWithTag("back_to_deck").performClick()
        compose.onNodeWithTag("stat_weak").assertTextContains("14")
        compose.onNodeWithTag("stat_mastered").assertTextContains("12")
    }

    @Test fun studyCloseReturnsToDeck() {
        openDeck()
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("study_card").performClick()
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
        (1..41).forEach { number ->
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
        compose.onNodeWithText("おはようございます").assertIsDisplayed()
        compose.onNodeWithTag("rate_good").assertIsEnabled()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("rate_good").assertIsEnabled().performClick()
        compose.onNodeWithText("おはよう").assertIsDisplayed()
        compose.onNodeWithTag("rate_good").assertIsNotEnabled()
    }

    @Test fun startButtonCompressesAndCancelledPressDoesNotStartSession() {
        openDeck()
        val label = compose.onNodeWithText("START FLASHCARDS", useUnmergedTree = true)
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
        detailNode("preview_card_01_026").assertIsDisplayed()
        compose.onNodeWithTag("start_flashcards").assertIsDisplayed().performClick()
        compose.onNodeWithTag("flashcard_study").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun compactLayoutHasReachableStudyControls() {
        openDeck()
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
        compose.onNodeWithText("おはよう").performScrollTo().assertIsDisplayed()
        screenshot("flashcards-landscape")
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun doubleFontScaleCanStartRevealAndRate() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        openDeck()
        detailNode("start_flashcards").performClick()
        compose.onNodeWithTag("study_card").performScrollTo().performClick()
        compose.onNodeWithTag("rate_easy").performScrollTo().assertIsDisplayed()
        screenshot("flashcards-large-text")
        compose.onNodeWithTag("rate_easy").performClick()
        compose.onNodeWithText("おはよう").performScrollTo().assertIsDisplayed()
    }

    private fun detailNode(tag: String): SemanticsNodeInteraction {
        // The session CTA is anchored below the scrolling previews.
        if (tag != "start_flashcards") compose.onNodeWithTag("deck_detail").performScrollToNode(hasTestTag(tag))
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
