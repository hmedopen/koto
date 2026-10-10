package com.koto.app

import com.koto.app.feature.cards.srs.FsrsConfig
import com.koto.app.feature.cards.srs.FsrsRating
import com.koto.app.ui.screens.cards.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FlashcardStateTest {
    private val deck = FlashcardDeck("deck", "Test", "home", List(6) { Flashcard("card_$it", "日$it", "romaji$it", "English $it") })

    @Test fun ratingsRequireRevealAndCannotSkipOrOverrunCards() {
        var state = FlashcardState().start(deck)
        assertEquals(state, state.rate("card_0", CardRating.Again))
        state = state.flip().rate("card_0", CardRating.Again, 123L)
        assertEquals(1, state.index)
        assertFalse(state.revealed)
        assertFalse(state.hasBeenRevealed)
        assertEquals(state, state.rate("card_0", CardRating.Easy))
        val nextFlipped = state.flip()
        assertEquals(nextFlipped, nextFlipped.rate("card_0", CardRating.Easy))
        for (index in 1..5) state = state.flip().rate("card_$index", CardRating.Easy, 124L)
        // With card_0 requeued at end of queue due to Again, state completes after card_0 is reviewed
        state = state.flip().rate("card_0", CardRating.Easy, 125L)
        assertTrue(state.complete)
        assertNull(state.currentId)
        assertEquals(125L, state.practiced[deck.id])
    }

    @Test fun restartShuffleAndBackKeepPreferencesAndProgress() {
        var state = FlashcardState(shuffle = true, showRomaji = false, japaneseFirst = false)
            .favorite("card_0").pin(deck.id).start(deck, Random(42))
        assertEquals(deck.cards.map { it.id }.toSet(), state.order.toSet())
        assertNotEquals(deck.cards.map { it.id }, state.order)
        state = state.flip().rate(state.currentId!!, CardRating.Again, 100L)
        val restored = state.back().start(deck, Random(43))
        assertEquals(0, restored.index)
        assertFalse(restored.revealed)
        assertFalse(restored.showRomaji)
        assertFalse(restored.japaneseFirst)
        assertEquals(1, restored.counts(deck, 100L).weak)
        assertTrue("card_0" in restored.favorites)
        assertTrue(deck.id in restored.pinned)
        assertNull(restored.back().back().deckId)
    }

    @Test fun reratingReplacesPreviousClassification() {
        val weak = FlashcardState().start(deck).flip().rate("card_0", CardRating.Again, 100L)
        assertEquals(1, weak.counts(deck, 100L).weak)
        // Rating Good replaces Again
        val good = weak.start(deck).flip().rate("card_0", CardRating.Good, 101L)
        assertEquals(0, good.counts(deck, 101L).weak)
    }

    @Test fun flipBackRetainsGradingPermissionOnlyForCurrentTurn() {
        var state = FlashcardState().start(deck)
        assertFalse(state.hasBeenRevealed)
        assertEquals(state, state.rate("card_0", CardRating.Good))
        state = state.flip()
        assertTrue(state.revealed)
        assertTrue(state.hasBeenRevealed)
        state = state.flip()
        assertFalse(state.revealed)
        assertTrue(state.hasBeenRevealed)
        state = state.rate("card_0", CardRating.Good, 10L)
        assertEquals(1, state.index)
        assertFalse(state.hasBeenRevealed)
        assertEquals(state, state.rate("card_1", CardRating.Good))
        assertFalse(state.start(deck).hasBeenRevealed)
    }
}
