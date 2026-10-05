package com.koto.app.feature.cards

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.cards.data.CustomDeckStore
import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.ui.screens.cards.CardContextLoader
import com.koto.app.ui.screens.cards.FlashcardState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomDeckStoreTest {

    private lateinit var store: CustomDeckStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = CustomDeckStore(context)
        // Clean any existing custom decks
        val existing = store.loadCustomDecks()
        for (deck in existing) {
            store.deleteDeck(deck.id)
        }
    }

    @Test
    fun testSaveCustomDeckAndVerifyStatsAndContext() {
        val cards = listOf(
            CustomCardItem(
                japanese = "おはよう",
                english = "Good morning",
                exampleKana = "せんせい おはよう",
                exampleEnglish = "Good morning teacher",
                notes = "Casual morning greeting",
            ),
            CustomCardItem(
                japanese = "ありがとう",
                english = "Thank you",
                exampleKana = "どうも ありがとう",
                exampleEnglish = "Thank you very much",
                notes = "Casual thank you",
            ),
        )

        val savedDeck = store.saveDeck(
            deckId = null,
            title = "Morning Greetings",
            icon = "chatbubble",
            cards = cards,
        )

        assertNotNull(savedDeck)
        assertEquals("Morning Greetings", savedDeck.title)
        assertEquals("chatbubble", savedDeck.icon)
        assertEquals(2, savedDeck.cards.size)

        // Check Room persistence
        val loadedDecks = store.loadCustomDecks()
        assertEquals(1, loadedDecks.size)
        val loadedDeck = loadedDecks[0]
        assertEquals(savedDeck.id, loadedDeck.id)
        assertEquals("Morning Greetings", loadedDeck.title)
        assertEquals(2, loadedDeck.cards.size)

        // Verify initial counts: 2 words, 2 Due, 0 Weak, 0 Mastered
        val state = FlashcardState()
        val counts = state.counts(loadedDeck)
        assertEquals(2, counts.due)
        assertEquals(0, counts.weak)
        assertEquals(0, counts.mastered)

        // Verify CardContextLoader registration
        val firstCard = loadedDeck.cards[0]
        val context = CardContextLoader.getContext(firstCard.id, firstCard.japanese, firstCard.romaji)
        assertNotNull(context)
        assertEquals("おはよう", context?.kana)
        assertEquals("Casual morning greeting", context?.usageNote)
        assertEquals(1, context?.examples?.size)
        assertEquals("せんせい おはよう", context?.examples?.first()?.kana)
        assertEquals("Good morning teacher", context?.examples?.first()?.english)
    }

    @Test
    fun testSavePreservesKanjiInCards() {
        val cards = listOf(
            CustomCardItem(
                japanese = "先生", // Kanji
                english = "Teacher",
                exampleKana = "先生、こんにちは", // Kanji
                exampleEnglish = "Hello teacher",
                notes = "School context",
            ),
        )

        val savedDeck = store.saveDeck(
            deckId = null,
            title = "Classroom Words",
            icon = "notebook",
            cards = cards,
        )

        val savedCard = savedDeck.cards[0]
        assertEquals("先生", savedCard.japanese)
        assertEquals("sensei", savedCard.romaji)

        val context = CardContextLoader.getContext(savedCard.id, savedCard.japanese, savedCard.romaji)
        assertNotNull(context)
        assertEquals("先生、こんにちは", context?.examples?.first()?.kana)
    }

    @Test
    fun testUpdateExistingDeck() {
        val cards = listOf(
            CustomCardItem(
                japanese = "みず",
                english = "Water",
            ),
        )

        val originalDeck = store.saveDeck(
            deckId = null,
            title = "Drinks",
            icon = "utensils",
            cards = cards,
        )

        val updatedCards = listOf(
            CustomCardItem(
                id = originalDeck.cards[0].id,
                japanese = "みず",
                english = "Cold water",
            ),
            CustomCardItem(
                japanese = "おちゃ",
                english = "Green tea",
            ),
        )

        val updatedDeck = store.saveDeck(
            deckId = originalDeck.id,
            title = "Hot & Cold Drinks",
            icon = "utensils",
            cards = updatedCards,
        )

        assertEquals(originalDeck.id, updatedDeck.id)
        assertEquals("Hot & Cold Drinks", updatedDeck.title)
        assertEquals(2, updatedDeck.cards.size)

        val loadedDecks = store.loadCustomDecks()
        assertEquals(1, loadedDecks.size)
        assertEquals("Hot & Cold Drinks", loadedDecks[0].title)
        assertEquals(2, loadedDecks[0].cards.size)
    }

    @Test
    fun testDeleteDeckRemovesFromDatabaseAndContext() {
        val cards = listOf(
            CustomCardItem(
                japanese = "ねこ",
                english = "Cat",
            ),
        )

        val deck = store.saveDeck(
            deckId = null,
            title = "Pets",
            icon = "home",
            cards = cards,
        )

        assertEquals(1, store.loadCustomDecks().size)

        store.deleteDeck(deck.id)

        assertEquals(0, store.loadCustomDecks().size)
    }
}
