package com.koto.app

import com.koto.app.ui.screens.cards.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FlashcardUxRefinementsTest {
    private val deck1 = FlashcardDeck(
        id = "deck_01",
        title = "Greetings & Courtesy",
        icon = "chatbubble",
        cards = listOf(
            Flashcard("c1_1", "おはよう", "ohayou", "Good morning (casual)"),
            Flashcard("c1_2", "おはようございます", "ohayou gozaimasu", "Good morning (formal)"),
            Flashcard("c1_3", "こんにちは", "konnichiwa", "Hello / Good afternoon"),
        ),
    )

    private val deck2 = FlashcardDeck(
        id = "deck_02",
        title = "Numbers",
        icon = "hashtag",
        cards = listOf(
            Flashcard("c2_1", "いち", "ichi", "One"),
            Flashcard("c2_2", "に", "ni", "Two"),
        ),
    )

    // =========================================================================
    // 1. Random Deck Selection ("Surprise Me / Daily Shuffle")
    // =========================================================================
    @Test
    fun randomDeckSelectionFiltersOutMasteredDecksByDefault() {
        val now = 1000000000L
        val oneDay = FlashcardSrsScheduler.ONE_DAY_MS

        // Deck 1 is 100% mastered
        val records = mapOf(
            "c1_1" to CardSrsRecord("c1_1", deck1.id, now + 7 * oneDay, CardRating.Easy, 2, isMastered = true, now),
            "c1_2" to CardSrsRecord("c1_2", deck1.id, now + 7 * oneDay, CardRating.Easy, 2, isMastered = true, now),
            "c1_3" to CardSrsRecord("c1_3", deck1.id, now + 7 * oneDay, CardRating.Easy, 2, isMastered = true, now),
        )
        val state = FlashcardState(srsRecords = records)
        val allDecks = listOf(deck1, deck2)
        val counts = allDecks.associate { it.id to state.counts(it, now) }

        // Mastered filter: default (includeMastered = false)
        val defaultEligible = allDecks.filter { deck ->
            val c = counts[deck.id]
            c == null || c.mastered < deck.cards.size || deck.cards.isEmpty()
        }
        assertEquals(1, defaultEligible.size)
        assertEquals(deck2.id, defaultEligible.first().id)

        // Include mastered toggle: On (includeMastered = true)
        val allEligible = allDecks
        assertEquals(2, allEligible.size)
    }

    // =========================================================================
    // 2. Mastered Deck State & Completion Celebration
    // =========================================================================
    @Test
    fun masteredDeckStateDetectionAndSeparation() {
        val now = 1000000000L
        val oneDay = FlashcardSrsScheduler.ONE_DAY_MS

        // Rate all cards in deck 2 as Easy twice to master it
        var state = FlashcardState()
        val r1 = FlashcardSrsScheduler.scheduleNext(null, "c2_1", deck2.id, CardRating.Easy, now)
        val r1Mastered = FlashcardSrsScheduler.scheduleNext(r1, "c2_1", deck2.id, CardRating.Easy, now + 7 * oneDay)

        val r2 = FlashcardSrsScheduler.scheduleNext(null, "c2_2", deck2.id, CardRating.Easy, now)
        val r2Mastered = FlashcardSrsScheduler.scheduleNext(r2, "c2_2", deck2.id, CardRating.Easy, now + 7 * oneDay)

        state = state.copy(srsRecords = mapOf("c2_1" to r1Mastered, "c2_2" to r2Mastered))
        val counts = state.counts(deck2, now)

        assertEquals(2, counts.mastered)
        assertEquals(0, counts.weak)
        assertEquals(0, counts.due)
        assertTrue("Every card reaching mastered triggers 100% deck mastery", counts.mastered == deck2.cards.size)

        // Separation into active vs mastered decks
        val allDecks = listOf(deck1, deck2)
        val allCounts = allDecks.associate { it.id to state.counts(it, now) }
        val (masteredDecks, activeDecks) = allDecks.partition { deck ->
            val c = allCounts.getValue(deck.id)
            c.mastered == deck.cards.size && deck.cards.isNotEmpty()
        }

        assertEquals(listOf(deck2), masteredDecks)
        assertEquals(listOf(deck1), activeDecks)
    }

    // =========================================================================
    // 3. Card Context Modal / Screen ({?} Button)
    // =========================================================================
    @Test
    fun cardContextPipelineAndGracefulFallback() {
        // Query card with authored content from card_contexts.json
        val context = CardContextLoader.getContext("おはよう", "ohayou")
        assertNotNull(context)
        assertEquals("おはよう", context?.kana)
        assertEquals("ohayou", context?.romaji)
        assertTrue("Usage note must be non-empty", context?.usageNote?.isNotEmpty() == true)
        assertEquals("Must load 3 sentence examples", 3, context?.examples?.size)

        // Verify example sentence fields
        val ex1 = context?.examples?.first()
        assertNotNull(ex1)
        assertTrue(ex1!!.kana.isNotEmpty())
        assertTrue(ex1.romaji.isNotEmpty())
        assertTrue(ex1.english.isNotEmpty())

        // Graceful fallback when card context is not present
        val fallbackContext = CardContextLoader.getContext("non_existent_card_xyz", "romaji")
        assertNull("Non-authored cards must return null for graceful fallback", fallbackContext)
    }

    // =========================================================================
    // 4. Review Session Size Selector
    // =========================================================================
    @Test
    fun sessionSizeSelectorConstrainsSessionOrder() {
        val largeDeck = FlashcardDeck(
            id = "deck_large",
            title = "Large Deck",
            icon = "star",
            cards = List(25) { Flashcard("card_$it", "日$it", "hi$it", "day $it") },
        )

        // Segment [ 5 ]
        val state5 = FlashcardState().start(largeDeck, size = 5)
        assertEquals(5, state5.order.size)
        assertEquals(5, state5.sessionSize)

        // Segment [ 10 ]
        val state10 = FlashcardState().start(largeDeck, size = 10)
        assertEquals(10, state10.order.size)
        assertEquals(10, state10.sessionSize)

        // Segment [ 15 ]
        val state15 = FlashcardState().start(largeDeck, size = 15)
        assertEquals(15, state15.order.size)
        assertEquals(15, state15.sessionSize)

        // Segment [ All ]
        val stateAll = FlashcardState().start(largeDeck, size = null)
        assertEquals(25, stateAll.order.size)
        assertNull(stateAll.sessionSize)
    }

    // =========================================================================
    // 5. Spaced Repetition System (SRS) Daily Engine
    // =========================================================================
    @Test
    fun srsIntervalSchedulingRules() {
        val now = 1000000000L
        val oneDay = FlashcardSrsScheduler.ONE_DAY_MS

        // Again: Due today
        val againRec = FlashcardSrsScheduler.scheduleNext(null, "c1", "d", CardRating.Again, now)
        assertEquals(now, againRec.dueTimestamp)
        assertEquals(CardRating.Again, againRec.lastRating)
        assertFalse(againRec.isMastered)

        // Hard: Today + 1 Day
        val hardRec = FlashcardSrsScheduler.scheduleNext(null, "c2", "d", CardRating.Hard, now)
        assertEquals(now + 1 * oneDay, hardRec.dueTimestamp)
        assertEquals(CardRating.Hard, hardRec.lastRating)
        assertFalse(hardRec.isMastered)

        // Good: Today + 3 Days
        val goodRec = FlashcardSrsScheduler.scheduleNext(null, "c3", "d", CardRating.Good, now)
        assertEquals(now + 3 * oneDay, goodRec.dueTimestamp)
        assertEquals(CardRating.Good, goodRec.lastRating)
        assertFalse(goodRec.isMastered)

        // Easy: Today + 7 Days
        val easyRec1 = FlashcardSrsScheduler.scheduleNext(null, "c4", "d", CardRating.Easy, now)
        assertEquals(now + 7 * oneDay, easyRec1.dueTimestamp)
        assertEquals(CardRating.Easy, easyRec1.lastRating)
        assertEquals(1, easyRec1.consecutiveEasyCount)
        assertFalse(easyRec1.isMastered)

        // Consecutive Easy -> Flagged as Mastered
        val easyRec2 = FlashcardSrsScheduler.scheduleNext(easyRec1, "c4", "d", CardRating.Easy, now + 7 * oneDay)
        assertEquals(now + 14 * oneDay, easyRec2.dueTimestamp)
        assertEquals(2, easyRec2.consecutiveEasyCount)
        assertTrue(easyRec2.isMastered)
    }

    @Test
    fun againRequeuesInActivePoolDuringSession() {
        val now = 1000000000L
        var state = FlashcardState().start(deck1)
        assertEquals(3, state.order.size)
        val firstCardId = state.currentId!!

        // Flip and rate Again with requeueAgain = true
        state = state.flip().rate(firstCardId, CardRating.Again, now, requeueAgain = true)

        // Session order should now have 4 cards, with firstCardId at the end
        assertEquals(4, state.order.size)
        assertEquals(firstCardId, state.order.last())
        assertEquals(1, state.index)
        assertFalse(state.revealed)
        assertFalse(state.complete)
    }

    @Test
    fun dayBoundaryRefreshSyncsDueTimestamps() {
        val now = 1000000000L
        val oneDay = FlashcardSrsScheduler.ONE_DAY_MS

        val cardA = CardSrsRecord("c1_1", deck1.id, dueTimestamp = now - 100L, CardRating.Hard, 0, isMastered = false, now - oneDay)
        val cardB = CardSrsRecord("c1_2", deck1.id, dueTimestamp = now + oneDay, CardRating.Good, 0, isMastered = false, now)
        val cardC = CardSrsRecord("c1_3", deck1.id, dueTimestamp = now + 7 * oneDay, CardRating.Easy, 2, isMastered = true, now)

        val state = FlashcardState(srsRecords = mapOf("c1_1" to cardA, "c1_2" to cardB, "c1_3" to cardC))

        val counts = state.counts(deck1, now)
        assertEquals(1, counts.mastered)
        assertEquals(1, counts.weak)
        assertEquals(1, counts.due)
    }
}
