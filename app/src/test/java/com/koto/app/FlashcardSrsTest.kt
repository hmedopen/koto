package com.koto.app

import com.koto.app.ui.screens.cards.*
import org.junit.Assert.*
import org.junit.Test

class FlashcardSrsTest {
    private val deck = FlashcardDeck(
        id = "test_deck",
        title = "Test Deck",
        icon = "chatbubble",
        cards = List(10) { Flashcard("c_$it", "かな$it", "kana$it", "meaning $it") }
    )

    @Test
    fun srsIntervalSchedulingAndConsecutiveEasyMastery() {
        val now = 1000000000L
        val oneDay = FlashcardSrsScheduler.ONE_DAY_MS

        // Card rated Hard -> Today + 1 Day
        val hardRecord = FlashcardSrsScheduler.scheduleNext(null, "c_0", deck.id, CardRating.Hard, now)
        assertEquals(now + oneDay, hardRecord.dueTimestamp)
        assertEquals(CardRating.Hard, hardRecord.lastRating)
        assertFalse(hardRecord.isMastered)

        // Card rated Good -> Today + 3 Days
        val goodRecord = FlashcardSrsScheduler.scheduleNext(null, "c_1", deck.id, CardRating.Good, now)
        assertEquals(now + 3 * oneDay, goodRecord.dueTimestamp)
        assertEquals(CardRating.Good, goodRecord.lastRating)
        assertFalse(goodRecord.isMastered)

        // Card rated Easy once -> Today + 7 Days, not yet mastered
        val easyRecord1 = FlashcardSrsScheduler.scheduleNext(null, "c_2", deck.id, CardRating.Easy, now)
        assertEquals(now + 7 * oneDay, easyRecord1.dueTimestamp)
        assertEquals(1, easyRecord1.consecutiveEasyCount)
        assertFalse(easyRecord1.isMastered)

        // Card rated Easy a second consecutive time -> Flagged as Mastered!
        val easyRecord2 = FlashcardSrsScheduler.scheduleNext(easyRecord1, "c_2", deck.id, CardRating.Easy, now + 7 * oneDay)
        assertEquals(now + 14 * oneDay, easyRecord2.dueTimestamp)
        assertEquals(2, easyRecord2.consecutiveEasyCount)
        assertTrue("Consecutive Easy rating must flag card as Mastered", easyRecord2.isMastered)

        // Card rated Hard after Easy -> consecutive reset and not mastered
        val hardAfterEasy = FlashcardSrsScheduler.scheduleNext(easyRecord1, "c_2", deck.id, CardRating.Hard, now + 7 * oneDay)
        assertEquals(0, hardAfterEasy.consecutiveEasyCount)
        assertFalse(hardAfterEasy.isMastered)
    }

    @Test
    fun againRequeuesInCurrentSessionAndStaysDueToday() {
        val now = 1000000000L
        var state = FlashcardState().start(deck)
        assertEquals(10, state.order.size)

        // Rate first card as Again with requeueAgain = true
        state = state.flip().rate("c_0", CardRating.Again, now, requeueAgain = true)
        assertEquals("Session should append missed card to the end", 11, state.order.size)
        assertEquals("c_0", state.order.last())
        assertEquals(now, state.srsRecords["c_0"]?.dueTimestamp)
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
    fun dayBoundaryRefreshAndActiveQueueExclusion() {
        val now = 1000000000L
        val oneDay = FlashcardSrsScheduler.ONE_DAY_MS

        // Set up records: 3 mastered, 2 weak, 5 due
        val records = mutableMapOf<String, CardSrsRecord>()
        // 3 mastered
        for (i in 0..2) {
            records["c_$i"] = CardSrsRecord("c_$i", deck.id, now + 7 * oneDay, CardRating.Easy, 2, isMastered = true, now)
        }
        // 2 weak (due in 1 day)
        records["c_3"] = CardSrsRecord("c_3", deck.id, now + oneDay, CardRating.Hard, 0, isMastered = false, now)
        records["c_4"] = CardSrsRecord("c_4", deck.id, now + oneDay, CardRating.Hard, 0, isMastered = false, now)

        val state = FlashcardState(srsRecords = records)
        val counts = state.counts(deck, now)
        assertEquals(3, counts.mastered)
        assertEquals(2, counts.weak)
        assertEquals(5, counts.due)

        // When all 10 cards are mastered:
        val allMasteredRecords = (0..9).associate {
            "c_$it" to CardSrsRecord("c_$it", deck.id, now + 7 * oneDay, CardRating.Easy, 2, isMastered = true, now)
        }
        val allMasteredState = FlashcardState(srsRecords = allMasteredRecords)
        val masteredCounts = allMasteredState.counts(deck, now)
        assertEquals(10, masteredCounts.mastered)
        assertEquals(0, masteredCounts.weak)
        assertEquals(0, masteredCounts.due)
        assertTrue(masteredCounts.mastered == deck.cards.size)
    }
}
