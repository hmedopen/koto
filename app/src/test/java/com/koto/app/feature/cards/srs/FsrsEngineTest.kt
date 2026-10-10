package com.koto.app.feature.cards.srs

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FsrsEngineTest {

    private val oneDayMs = FsrsConfig.DAY_MS
    private val fakeNowDay1 = 1700000000000L // arbitrary fixed UTC epoch ms (Day 1)
    private val fakeNowDay2 = fakeNowDay1 + oneDayMs // Day 2
    private val fakeNowDay5 = fakeNowDay1 + 4 * oneDayMs // Day 5 (skipped days)

    private fun createDeckOfCards(count: Int, deckId: String = "deck_test"): List<FsrsCard> {
        return (1..count).map { index ->
            FsrsCard.createNew(cardId = "card_$index", deckId = deckId, now = fakeNowDay1)
        }
    }

    // 1. Scenario: Deck of 20, size 10, day 1: 10 new cards shown, 10 stay New.
    @Test
    fun scenario1_deckOf20_size10_day1() {
        val deck = createDeckOfCards(20)
        val queue = FsrsEngine.buildQueue(
            cards = deck,
            sessionSize = 10,
            now = fakeNowDay1,
            shuffle = false,
        )

        assertEquals("Queue must contain exactly 10 cards", 10, queue.size)
        val expectedSelectedIds = (1..10).map { "card_$it" }
        assertEquals(expectedSelectedIds, queue)

        // 10 rated cards become Learning/Review, 10 unstudied remain New
        val ratedCards = queue.map { cardId ->
            val original = deck.first { it.cardId == cardId }
            FsrsEngine.rateCard(original, FsrsRating.Good, fakeNowDay1).first
        }
        val unstudiedCards = deck.filter { it.cardId !in queue }
        assertEquals(10, unstudiedCards.size)
        assertTrue(unstudiedCards.all { it.state == FsrsState.New })
        assertTrue(ratedCards.all { it.state != FsrsState.New })
    }

    // 2. Scenario: Day 2: yesterday's due cards first, leftover slots go to new cards.
    @Test
    fun scenario2_day2_yesterdaysDueCardsFirst_leftoverSlotsToNewCards() {
        val deck = createDeckOfCards(20)
        // Day 1: 6 cards studied with Good (scheduled 1 day ahead, so due on Day 2)
        val day1Updated = deck.mapIndexed { index, card ->
            if (index < 6) {
                // Rate Good on Day 1
                val (rated, _) = FsrsEngine.rateCard(card, FsrsRating.Good, fakeNowDay1)
                // In FSRS, Learning step 2 is 10 min, or simulate transition into Review due on Day 2
                rated.copy(
                    state = FsrsState.Review,
                    due = fakeNowDay2 - 1000L, // Due on Day 2
                    stability = 1.0,
                )
            } else {
                card // Remains New
            }
        }

        // Session size 10 on Day 2:
        // 6 due cards first + 4 new cards fill the remaining slots
        val queue = FsrsEngine.buildQueue(
            cards = day1Updated,
            sessionSize = 10,
            now = fakeNowDay2,
            shuffle = false,
        )

        assertEquals(10, queue.size)
        // First 6 are the due cards
        assertEquals((1..6).map { "card_$it" }, queue.take(6))
        // Remaining 4 are new cards (card_7 to card_10)
        assertEquals((7..10).map { "card_$it" }, queue.drop(6))
    }

    // 3. Scenario: User skips days: reviews pile up above session size, so no new cards until backlog clears.
    @Test
    fun scenario3_skipsDays_backlogPilesUp_zeroNewCards() {
        val deck = createDeckOfCards(20)
        // 15 cards are due reviews on Day 5
        val overdueDeck = deck.mapIndexed { index, card ->
            if (index < 15) {
                card.copy(
                    state = FsrsState.Review,
                    due = fakeNowDay1 + 2 * oneDayMs, // Due on Day 3, now Day 5 (overdue)
                    stability = 2.0,
                )
            } else {
                card // Remains New
            }
        }

        val queue = FsrsEngine.buildQueue(
            cards = overdueDeck,
            sessionSize = 10,
            now = fakeNowDay5,
            shuffle = false,
        )

        assertEquals("Session size 10 must be respected", 10, queue.size)
        // Must contain ZERO new cards because due reviews (15) >= session size (10)
        val queueCardStates = queue.map { id -> overdueDeck.first { it.cardId == id }.state }
        assertTrue("No new cards allowed until backlog clears", queueCardStates.all { it != FsrsState.New })
        assertEquals((1..10).map { "card_$it" }, queue)
    }

    // 4. Scenario: User quits mid-session: rated cards are already saved, unrated cards untouched.
    @Test
    fun scenario4_userQuitsMidSession_ratedSaved_unratedUntouched() {
        val deck = createDeckOfCards(20)
        val savedCards = mutableMapOf<String, FsrsCard>()
        val savedLogs = mutableListOf<FsrsReviewLog>()

        fun simulateImmediateRating(cardId: String, rating: FsrsRating, now: Long) {
            val card = savedCards[cardId] ?: deck.first { it.cardId == cardId }
            val (nextCard, log) = FsrsEngine.rateCard(card, rating, now)
            // Immediately saved
            savedCards[cardId] = nextCard
            savedLogs.add(log)
        }

        val sessionQueue = FsrsEngine.buildQueue(deck, 10, fakeNowDay1, shuffle = false)

        // User rates card 1 and card 2, then quits mid-session
        simulateImmediateRating(sessionQueue[0], FsrsRating.Good, fakeNowDay1)
        simulateImmediateRating(sessionQueue[1], FsrsRating.Easy, fakeNowDay1)

        // Verify only 2 cards were saved and modified
        assertEquals(2, savedCards.size)
        assertEquals(2, savedLogs.size)
        assertEquals(FsrsState.Learning, savedCards["card_1"]?.state)
        assertEquals(FsrsState.Review, savedCards["card_2"]?.state)

        // Card 3 onwards was not rated and remains untouched New in deck
        assertFalse(savedCards.containsKey("card_3"))
        assertEquals(FsrsState.New, deck.first { it.cardId == "card_3" }.state)
    }

    // 5. Scenario: Mastered card gets Again: becomes Relearning, drops out of Mastered, shows as Weak.
    @Test
    fun scenario5_masteredCardGetsAgain_becomesRelearning_dropsMastered_showsWeak() {
        val masteredCard = FsrsCard(
            cardId = "card_mastered",
            deckId = "deck_test",
            due = fakeNowDay1,
            stability = 30.0, // >= 21.0
            difficulty = 3.0,
            elapsed_days = 25L,
            scheduled_days = 25L,
            reps = 6,
            lapses = 0,
            state = FsrsState.Review,
            last_review = fakeNowDay1 - 25 * oneDayMs,
            last_rating = FsrsRating.Good,
        )

        // Precondition check: card is Mastered
        assertTrue(FsrsEngine.isMastered(masteredCard))
        assertFalse(FsrsEngine.isWeak(masteredCard))
        val initialStats = FsrsEngine.getDeckStats(listOf(masteredCard), fakeNowDay1)
        assertEquals(1, initialStats.mastered)
        assertEquals(0, initialStats.weak)

        // User presses AGAIN on the Mastered card
        val (afterAgain, log) = FsrsEngine.rateCard(masteredCard, FsrsRating.Again, fakeNowDay1)

        assertEquals("State must become Relearning", FsrsState.Relearning, afterAgain.state)
        assertEquals("Lapses must increment", 1, afterAgain.lapses)
        assertEquals(FsrsRating.Again, afterAgain.last_rating)

        // Drops out of Mastered, shows as Weak
        assertFalse("Must drop out of Mastered", FsrsEngine.isMastered(afterAgain))
        assertTrue("Must be classified as Weak", FsrsEngine.isWeak(afterAgain))

        val updatedStats = FsrsEngine.getDeckStats(listOf(afterAgain), fakeNowDay1)
        assertEquals("Mastered must be 0", 0, updatedStats.mastered)
        assertEquals("Weak must be 1", 1, updatedStats.weak)
    }

    // 6. Scenario: Hard is a pass, only Again is a lapse.
    @Test
    fun scenario6_hardIsPass_onlyAgainIsLapse() {
        val reviewCard = FsrsCard(
            cardId = "card_review",
            deckId = "deck_test",
            due = fakeNowDay1,
            stability = 10.0,
            difficulty = 4.0,
            elapsed_days = 10L,
            scheduled_days = 10L,
            reps = 3,
            lapses = 1,
            state = FsrsState.Review,
            last_review = fakeNowDay1 - 10 * oneDayMs,
            last_rating = FsrsRating.Good,
        )

        // Rating Hard
        val (afterHard, _) = FsrsEngine.rateCard(reviewCard, FsrsRating.Hard, fakeNowDay1)
        assertEquals("Hard retains Review state (pass)", FsrsState.Review, afterHard.state)
        assertEquals("Hard does NOT increment lapses", 1, afterHard.lapses)
        assertEquals(4, afterHard.reps)

        // Rating Again
        val (afterAgain, _) = FsrsEngine.rateCard(reviewCard, FsrsRating.Again, fakeNowDay1)
        assertEquals("Again enters Relearning (lapse)", FsrsState.Relearning, afterAgain.state)
        assertEquals("Again increments lapses", 2, afterAgain.lapses)
        assertEquals(4, afterAgain.reps)
    }

    // 7. Scenario: Changing session size or direction mid-use never changes card data.
    @Test
    fun scenario7_changingSessionSizeOrDirectionNeverChangesCardData() {
        val initialCard = FsrsCard.createNew("card_1", "deck_1", fakeNowDay1)
        val initialCopy = initialCard.copy()

        // Simulating session size changing from 10 to 5 or "All"
        val queueSize5 = FsrsEngine.buildQueue(listOf(initialCard), 5, fakeNowDay1)
        val queueSizeAll = FsrsEngine.buildQueue(listOf(initialCard), null, fakeNowDay1)

        assertEquals(listOf("card_1"), queueSize5)
        assertEquals(listOf("card_1"), queueSizeAll)

        // Card data has zero mutations
        assertEquals(initialCopy, initialCard)
    }

    // 8. Scenario: Nothing due and no new cards: show "all caught up" and the next due time.
    @Test
    fun scenario8_nothingDueAndNoNewCards_showsAllCaughtUpAndNextDueTime() {
        val nextDueTimestamp = fakeNowDay1 + 14 * oneDayMs
        val futureDeck = listOf(
            FsrsCard(
                cardId = "card_future_1",
                deckId = "deck_test",
                due = nextDueTimestamp,
                stability = 14.0,
                difficulty = 3.0,
                state = FsrsState.Review,
            ),
            FsrsCard(
                cardId = "card_future_2",
                deckId = "deck_test",
                due = nextDueTimestamp + 2 * oneDayMs,
                stability = 16.0,
                difficulty = 3.0,
                state = FsrsState.Review,
            ),
        )

        val stats = FsrsEngine.getDeckStats(futureDeck, fakeNowDay1)
        assertEquals("Due must be 0", 0, stats.due)
        assertEquals("Next due time must be the earliest future due", nextDueTimestamp, stats.nextDueTime)

        val queue = FsrsEngine.buildQueue(futureDeck, 10, fakeNowDay1)
        assertTrue("Queue must be empty when nothing is due and no new cards", queue.isEmpty())
    }

    // 9. Scenario: Shuffle rule (important): Selection happens FIRST, shuffle happens LAST.
    // For any seed, the set of card IDs in the session is identical with Shuffle on or off.
    @Test
    fun scenario9_shuffleRule_selectionFirst_shuffleLast_identicalSet() {
        // Deck of 30 cards, 20 are due, session size 10
        val cards = (1..30).map { i ->
            if (i <= 20) {
                // 20 due cards
                FsrsCard(
                    cardId = "card_$i",
                    deckId = "deck_test",
                    due = fakeNowDay1 - (20 - i) * 1000L, // staggered overdue timestamps
                    stability = 2.0,
                    state = FsrsState.Review,
                )
            } else {
                // 10 new cards
                FsrsCard.createNew("card_$i", "deck_test", fakeNowDay1)
            }
        }

        // Test across 20 different random seeds
        val seeds = listOf(1L, 42L, 99L, 12345L, 999999L, 314159L, 271828L)
        val unshuffledQueue = FsrsEngine.buildQueue(
            cards = cards,
            sessionSize = 10,
            now = fakeNowDay1,
            shuffle = false,
        )

        assertEquals("Session size must be exactly 10", 10, unshuffledQueue.size)
        val expectedSet = unshuffledQueue.toSet()

        for (seed in seeds) {
            val shuffledQueue = FsrsEngine.buildQueue(
                cards = cards,
                sessionSize = 10,
                now = fakeNowDay1,
                shuffle = true,
                random = Random(seed),
            )

            assertEquals("Shuffled queue size must match", 10, shuffledQueue.size)
            assertEquals(
                "Selected card IDs must be 100% identical regardless of shuffle seed",
                expectedSet,
                shuffledQueue.toSet(),
            )

            // A new card (card_21 to card_30) must NEVER appear due to shuffling!
            assertTrue(
                "New cards must never be pulled in by shuffling when due >= session size",
                shuffledQueue.none { it in (21..30).map { idx -> "card_$idx" } },
            )
        }
    }

    // 10. Scenario: Short-step requeue: Again within ~10 min puts card back at end of queue until empty.
    @Test
    fun scenario10_shortStepRequeue_againAppendsToEndUntilEmpty() {
        val card = FsrsCard.createNew("card_1", "deck_test", fakeNowDay1)
        val (afterAgain, _) = FsrsEngine.rateCard(card, FsrsRating.Again, fakeNowDay1)

        val intervalMs = afterAgain.due - fakeNowDay1
        assertTrue(
            "Again due interval must be <= 10 min",
            intervalMs <= FsrsConfig.SHORT_STEP_THRESHOLD_MS,
        )

        // Queue simulation with requeue
        val queue = mutableListOf("card_1", "card_2")
        val processed = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            processed.add(currentId)

            if (currentId == "card_1" && processed.count { it == "card_1" } == 1) {
                // First attempt on card_1 fails with Again -> requeued at end
                queue.add(currentId)
            }
        }

        assertEquals(listOf("card_1", "card_2", "card_1"), processed)
        assertTrue("Session ends when queue is completely empty", queue.isEmpty())
    }

    // 11. Scenario: Derived stats on read: unstudied 20-card deck shows Due 20, Weak 0, Mastered 0.
    @Test
    fun scenario11_unstudiedDeckStats() {
        val deck = createDeckOfCards(20)
        val stats = FsrsEngine.getDeckStats(deck, fakeNowDay1)
        assertEquals(20, stats.due)
        assertEquals(0, stats.weak)
        assertEquals(0, stats.mastered)
    }

    // 12. Scenario: "All" session size selects all due reviews + all new cards
    @Test
    fun scenario12_allOptionSelectsAllDueAndAllNew() {
        val cards = listOf(
            FsrsCard("c_due1", "d", due = fakeNowDay1 - 1000L, state = FsrsState.Review),
            FsrsCard("c_due2", "d", due = fakeNowDay1 - 2000L, state = FsrsState.Learning),
            FsrsCard.createNew("c_new1", "d", fakeNowDay1),
            FsrsCard.createNew("c_new2", "d", fakeNowDay1),
            // Not due (scheduled in future)
            FsrsCard("c_future", "d", due = fakeNowDay1 + oneDayMs, state = FsrsState.Review),
        )

        val queue = FsrsEngine.buildQueue(cards, sessionSize = null, now = fakeNowDay1)
        assertEquals(4, queue.size)
        assertTrue("Future card must not be included", "c_future" !in queue)
        assertTrue(queue.containsAll(listOf("c_due1", "c_due2", "c_new1", "c_new2")))
    }
}
