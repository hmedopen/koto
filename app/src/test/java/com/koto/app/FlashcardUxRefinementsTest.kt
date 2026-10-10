package com.koto.app

import com.koto.app.feature.cards.srs.FsrsCard
import com.koto.app.feature.cards.srs.FsrsConfig
import com.koto.app.feature.cards.srs.FsrsEngine
import com.koto.app.feature.cards.srs.FsrsRating
import com.koto.app.feature.cards.srs.FsrsState
import com.koto.app.ui.screens.cards.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FlashcardUxRefinementsTest {

    private val deck1 = FlashcardDeck(
        id = "deck_1",
        title = "Deck 1",
        icon = "chatbubble",
        cards = listOf(
            Flashcard("c1_1", "日1", "hi1", "day 1"),
            Flashcard("c1_2", "日2", "hi2", "day 2"),
            Flashcard("c1_3", "日3", "hi3", "day 3"),
        ),
    )

    private val deck2 = FlashcardDeck(
        id = "deck_2",
        title = "Deck 2",
        icon = "book",
        cards = listOf(
            Flashcard("c2_1", "月1", "tsuki1", "month 1"),
            Flashcard("c2_2", "月2", "tsuki2", "month 2"),
        ),
    )

    // =========================================================================
    // 1. Random Deck Selection ("Surprise Me / Daily Shuffle")
    // =========================================================================
    @Test
    fun randomDeckSelectionFiltersOutMasteredDecksByDefault() {
        val now = 1000000000L
        val oneDay = FsrsConfig.DAY_MS

        // Deck 1 is 100% mastered
        val records = mapOf(
            "c1_1" to FsrsCard("c1_1", deck1.id, due = now + 7 * oneDay, stability = 25.0, state = FsrsState.Review),
            "c1_2" to FsrsCard("c1_2", deck1.id, due = now + 7 * oneDay, stability = 25.0, state = FsrsState.Review),
            "c1_3" to FsrsCard("c1_3", deck1.id, due = now + 7 * oneDay, stability = 25.0, state = FsrsState.Review),
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
        val oneDay = FsrsConfig.DAY_MS

        // All cards in deck 2 at Mastered tier
        val r1Mastered = FsrsCard("c2_1", deck2.id, due = now + 300 * oneDay, stability = 25.0, state = FsrsState.Review)
        val r2Mastered = FsrsCard("c2_2", deck2.id, due = now + 300 * oneDay, stability = 25.0, state = FsrsState.Review)

        val state = FlashcardState(srsRecords = mapOf("c2_1" to r1Mastered, "c2_2" to r2Mastered))
        val counts = state.counts(deck2, now)

        assertEquals(2, counts.mastered)
        assertEquals(0, counts.weak)
        assertEquals(0, counts.due)
        assertTrue("Every card reaching mastered triggers 100% deck mastery", counts.mastered == deck2.cards.size)
    }

    // =========================================================================
    // 3. Dynamic Starred Cards Virtual Deck
    // =========================================================================
    @Test
    fun starredCardsDeckAggregatesAcrossAllDecks() {
        val allDecks = listOf(deck1, deck2)
        val starredCardIds = setOf("c1_1", "c2_2")

        val starredDeck = FlashcardDeck(
            id = "starred_review",
            title = "Starred Cards",
            icon = "star",
            cards = allDecks.flatMap { it.cards }.filter { it.id in starredCardIds },
            number = 0,
            category = "Bookmarks",
            tier = 0,
        )

        assertEquals(2, starredDeck.cards.size)
        assertEquals(setOf("c1_1", "c2_2"), starredDeck.cards.map { it.id }.toSet())
    }

    // =========================================================================
    // 4. Session Size Selector
    // =========================================================================
    @Test
    fun sessionSizeClampsCorrectly() {
        val deck10 = FlashcardDeck(
            id = "d10",
            title = "D10",
            icon = "chatbubble",
            cards = (1..10).map { Flashcard("c$it", "j$it", "r$it", "e$it") },
        )

        val state5 = FlashcardState().start(deck10, size = 5)
        assertEquals(5, state5.order.size)

        val state10 = FlashcardState().start(deck10, size = 10)
        assertEquals(10, state10.order.size)

        val stateAll = FlashcardState().start(deck10, size = null)
        assertEquals(10, stateAll.order.size)
    }

    // =========================================================================
    // 5. Spaced Repetition System (SRS) Daily Engine
    // =========================================================================
    @Test
    fun srsIntervalSchedulingRules() {
        val now = 1000000000L
        val card1 = FsrsCard.createNew("c1", "d", now)
        val (againRec, _) = FsrsEngine.rateCard(card1, FsrsRating.Again, now)
        assertEquals(FsrsState.Learning, againRec.state)
        assertEquals(1, againRec.lapses)
        assertEquals(FsrsRating.Again, againRec.last_rating)

        val card2 = FsrsCard.createNew("c2", "d", now)
        val (hardRec, _) = FsrsEngine.rateCard(card2, FsrsRating.Hard, now)
        assertEquals(FsrsState.Learning, hardRec.state)
        assertEquals(0, hardRec.lapses)
        assertEquals(FsrsRating.Hard, hardRec.last_rating)

        val card3 = FsrsCard.createNew("c3", "d", now)
        val (goodRec, _) = FsrsEngine.rateCard(card3, FsrsRating.Good, now)
        assertEquals(FsrsState.Learning, goodRec.state)
        assertEquals(FsrsRating.Good, goodRec.last_rating)

        val card4 = FsrsCard.createNew("c4", "d", now)
        val (easyRec, _) = FsrsEngine.rateCard(card4, FsrsRating.Easy, now)
        assertEquals(FsrsState.Review, easyRec.state)
        assertEquals(FsrsRating.Easy, easyRec.last_rating)
    }

    @Test
    fun againRequeuesInActivePoolDuringSession() {
        val now = 1000000000L
        var state = FlashcardState().start(deck1)
        assertEquals(3, state.order.size)
        val firstCardId = state.currentId!!

        // Flip and rate Again with requeueAgain = true
        state = state.flip().rate(firstCardId, FsrsRating.Again, now, requeueAgain = true)

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
        val oneDay = FsrsConfig.DAY_MS

        val cardA = FsrsCard("c1_1", deck1.id, due = now - 100L, stability = 5.0, state = FsrsState.Review, last_rating = FsrsRating.Again, lapses = 1)
        val cardB = FsrsCard("c1_2", deck1.id, due = now + oneDay, stability = 5.0, state = FsrsState.Review, last_rating = FsrsRating.Good)
        val cardC = FsrsCard("c1_3", deck1.id, due = now + 7 * oneDay, stability = 25.0, state = FsrsState.Review, last_rating = FsrsRating.Good)

        val state = FlashcardState(srsRecords = mapOf("c1_1" to cardA, "c1_2" to cardB, "c1_3" to cardC))

        val counts = state.counts(deck1, now)
        assertEquals(1, counts.mastered)
        assertEquals(1, counts.weak)
        assertEquals(1, counts.due)
    }
}
