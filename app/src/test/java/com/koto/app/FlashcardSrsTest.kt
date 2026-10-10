package com.koto.app

import com.koto.app.feature.cards.srs.FsrsCard
import com.koto.app.feature.cards.srs.FsrsConfig
import com.koto.app.feature.cards.srs.FsrsEngine
import com.koto.app.feature.cards.srs.FsrsRating
import com.koto.app.feature.cards.srs.FsrsState
import com.koto.app.ui.screens.cards.*
import org.junit.Assert.*
import org.junit.Test

class FlashcardSrsTest {
    private val deck = FlashcardDeck(
        id = "test_deck",
        title = "Test Deck",
        icon = "chatbubble",
        cards = List(10) { Flashcard("c_$it", "かな$it", "kana$it", "meaning $it") },
    )

    private val now = 1700000000000L

    @Test
    fun unstudiedCardsImmediatelyDue() {
        val unstudiedRecord = FsrsCard.createNew("c_new", deck.id, now)
        assertTrue("New cards must be immediately due", unstudiedRecord.isDue(now))

        val nullRecord: FsrsCard? = null
        assertTrue("Null records must be immediately due", nullRecord.isDue(now))
    }

    @Test
    fun sessionBuilderPrioritizesDueReviewsOverUnstudied() {
        val srsMap = mapOf(
            "c_0" to FsrsCard("c_0", deck.id, due = now - 1000L, state = FsrsState.Review),
            "c_1" to FsrsCard("c_1", deck.id, due = now - 2000L, state = FsrsState.Review), // overdue
            "c_5" to FsrsCard("c_5", deck.id, due = now + 10 * FsrsConfig.DAY_MS, state = FsrsState.Review), // not due
        )

        val queue = FlashcardSessionBuilder.buildSessionQueue(
            deck = deck,
            srsRecords = srsMap,
            sessionSize = 5,
            shuffle = false,
            now = now,
        )

        assertEquals(5, queue.size)
        // Due reviews come first
        assertEquals("c_1", queue[0]) // more overdue (due - 2000L)
        assertEquals("c_0", queue[1])
        // Remaining cards must be unstudied cards
        assertTrue(queue[2] in listOf("c_2", "c_3", "c_4", "c_6", "c_7", "c_8", "c_9"))
        assertFalse("Not-due card c_5 must not be included when due cards are available", "c_5" in queue)
    }

    @Test
    fun sessionSizeSelectorLimitsOrderSize() {
        val state5 = FlashcardState().start(deck, size = 5)
        assertEquals(5, state5.order.size)

        val state10 = FlashcardState().start(deck, size = 10)
        assertEquals(10, state10.order.size)

        val stateAll = FlashcardState().start(deck, size = null)
        assertEquals(10, stateAll.order.size)
    }

    @Test
    fun againRequeuesInCurrentSession() {
        var state = FlashcardState().start(deck)
        assertEquals(10, state.order.size)

        state = state.flip().rate("c_0", FsrsRating.Again, now, requeueAgain = true)
        assertEquals("Session should append missed card to the end for practice", 11, state.order.size)
        assertEquals("c_0", state.order.last())
        assertEquals(1, state.srsRecords["c_0"]?.lapses)
        assertEquals(FsrsState.Learning, state.srsRecords["c_0"]?.state)
    }

    @Test
    fun getDeckStatsDerivedCorrectly() {
        val state = FlashcardState()
        val stats = state.counts(deck, now)
        assertEquals("All unstudied cards show as Due", 10, stats.due)
        assertEquals(0, stats.weak)
        assertEquals(0, stats.mastered)
    }
}
