package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.koto.app.ui.screens.cards.*
import com.koto.app.ui.screens.settings.DisplayMode
import com.koto.app.ui.screens.settings.DisplayPreferences
import com.koto.app.ui.screens.settings.LocalJapaneseDisplayMode
import com.koto.app.ui.screens.settings.LocalRomajiVisibility
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeckDualModeDisplayTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun loadDecks(): List<FlashcardDeck> {
        val rootDir = File(".").canonicalFile
        val candidates = listOf(
            File(rootDir, "src/main/assets/flashcard_decks.json"),
            File(rootDir, "app/src/main/assets/flashcard_decks.json"),
            File("../app/src/main/assets/flashcard_decks.json"),
            File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/flashcard_decks.json"),
        )
        val file = candidates.firstOrNull { it.exists() } ?: error("Asset not found")
        return loadFlashcardDecks(file.readText(Charsets.UTF_8))
    }

    @Test
    fun flashcardAssetContainsHighCoverageKanjiAndFurigana() {
        val decks = loadDecks()
        val allCards = decks.flatMap { it.cards }

        val withKanji = allCards.filter { !it.kanji.isNullOrBlank() }
        val withFurigana = allCards.filter { !it.furigana.isNullOrBlank() }

        // Over 65% of all cards should now have verified Kanji and Furigana markup
        assertTrue("At least 800 cards must have Kanji annotations", withKanji.size >= 800)
        assertTrue("At least 800 cards must have Furigana markup", withFurigana.size >= 800)

        // Verify specific known cards
        val watashi = allCards.first { it.id == "card_02_001" }
        assertEquals("わたし", watashi.japanese)
        assertEquals("私", watashi.kanji)
        assertEquals("私{わたし}", watashi.furigana)
        assertEquals("私", watashi.displayKanji)

        val ohayou = allCards.first { it.id == "card_01_001" }
        assertEquals("おはよう", ohayou.japanese)
        assertNull(ohayou.kanji)
        assertEquals("おはよう", ohayou.displayKanji) // Fallback to kana
    }

    @Test
    fun deckContentScreen_switchesBetweenKanaAndKanjiModes() {
        val sampleDeck = FlashcardDeck(
            id = "deck_test",
            title = "Test Deck",
            icon = "chatbubble",
            cards = listOf(
                Flashcard(
                    id = "c1",
                    japanese = "みず",
                    kanji = "水",
                    furigana = "水{みず}",
                    romaji = "mizu",
                    english = "Water",
                ),
            ),
        )
        val state = FlashcardState()
        val currentMode = androidx.compose.runtime.mutableStateOf(DisplayMode.KANA)

        compose.setContent {
            CompositionLocalProvider(
                LocalJapaneseDisplayMode provides currentMode.value,
                LocalRomajiVisibility provides true,
            ) {
                DeckContentScreen(
                    deck = sampleDeck,
                    state = state,
                    favorite = {},
                )
            }
        }

        // 1. In Kana Mode
        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("水").assertDoesNotExist()

        // 2. In Kanji Only Mode
        currentMode.value = DisplayMode.KANJI_ONLY
        compose.waitForIdle()

        compose.onNodeWithText("水").assertIsDisplayed()
        compose.onNodeWithText("みず").assertDoesNotExist()

        // 3. In Furigana Mode
        currentMode.value = DisplayMode.KANJI_FURIGANA
        compose.waitForIdle()

        compose.onNodeWithText("水").assertIsDisplayed()
        compose.onNodeWithText("みず").assertIsDisplayed()
    }

    @Test
    fun starredCardsScreen_switchesBetweenKanaAndKanjiModes() {
        val sampleCard = Flashcard(
            id = "c1",
            japanese = "たべる",
            kanji = "食べる",
            furigana = "食{た}べる",
            romaji = "taberu",
            english = "To eat",
        )
        val sampleDeck = FlashcardDeck(
            id = "deck_test",
            title = "Test",
            icon = "chatbubble",
            cards = listOf(sampleCard),
        )
        val state = FlashcardState(favorites = setOf("c1"))
        val currentMode = androidx.compose.runtime.mutableStateOf(DisplayMode.KANA)

        compose.setContent {
            CompositionLocalProvider(
                LocalJapaneseDisplayMode provides currentMode.value,
                LocalRomajiVisibility provides true,
            ) {
                StarredCardsScreen(
                    decks = listOf(sampleDeck),
                    state = state,
                    onToggleFavorite = {},
                    onViewDeck = {},
                    onBack = {},
                )
            }
        }

        // Kana Mode
        compose.onNodeWithText("たべる").assertIsDisplayed()
        compose.onNodeWithText("食べる").assertDoesNotExist()

        // Kanji Mode
        currentMode.value = DisplayMode.KANJI_ONLY
        compose.waitForIdle()

        compose.onNodeWithText("食べる").assertIsDisplayed()
        compose.onNodeWithText("たべる").assertDoesNotExist()
    }

    @Test
    fun cardContextAssetContainsEnrichedKanjiAndExamples() {
        val card = Flashcard(
            id = "card_01_001",
            japanese = "おはよう",
            kanji = null,
            romaji = "ohayou",
            english = "Good morning (casual)",
        )
        val context = CardContextLoader.getContext(card)
        assertNotNull("Context for card_01_001 must exist", context)
        assertEquals("お早う", context?.kanji)
        assertTrue("Context must contain sentence examples", context?.examples?.isNotEmpty() == true)

        val firstExample = context!!.examples.first()
        assertNotNull("First example must have kanji", firstExample.kanji)
        assertTrue("First example must contain kanji characters", firstExample.displayKanji.contains("お早う"))
    }

    @Test
    fun cardContextScreen_displaysKanjiAndTogglesBetweenModes() {
        val sampleCard = Flashcard(
            id = "card_01_001",
            japanese = "おはよう",
            kanji = null,
            romaji = "ohayou",
            english = "Good morning (casual)",
        )
        val sampleDeck = FlashcardDeck(
            id = "deck_01",
            number = 1,
            title = "Greetings",
            category = "Basics",
            icon = "chatbubble",
            cards = listOf(sampleCard),
        )
        val state = FlashcardState(
            revealed = true,
            hasBeenRevealed = true,
            order = listOf("card_01_001"),
        )
        val preferences = DisplayPreferences.get(compose.activity)
        preferences.setDisplayMode(DisplayMode.KANJI_FURIGANA)

        compose.setContent {
            CompositionLocalProvider(
                LocalJapaneseDisplayMode provides preferences.displayMode,
                LocalRomajiVisibility provides true,
            ) {
                FlashcardStudyScreen(
                    deck = sampleDeck,
                    state = state,
                    update = {},
                )
            }
        }

        // 1. Click "?" button to open CardContextScreen
        compose.onNodeWithTag("card_context_button").performClick()
        compose.waitForIdle()

        // 2. Card context dialog is visible
        compose.onNodeWithTag("card_context_dialog").assertIsDisplayed()

        // 3. In KANJI_FURIGANA mode, kanji surface "早" and furigana reading "はよ" are rendered for card "お早う"
        compose.onAllNodesWithText("早").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("はよ").onFirst().assertIsDisplayed()

        // 4. Mode toggle button is present showing "あ" (clicking switches to Kana)
        compose.onNodeWithTag("card_context_display_mode_toggle").assertIsDisplayed()
        compose.onNodeWithText("あ").assertIsDisplayed()

        // 5. Click toggle button to switch to Kana mode
        compose.onNodeWithTag("card_context_display_mode_toggle").performClick()
        compose.waitForIdle()

        // 6. Mode toggle button now shows "漢"
        compose.onNodeWithText("漢").assertIsDisplayed()

        // 7. In Kana mode, pure kana "おはよう" is rendered
        compose.onAllNodesWithText("おはよう").onFirst().assertIsDisplayed()
    }
}
