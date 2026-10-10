package com.koto.app.feature.cards.srs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Pure functions implementing the official FSRS algorithm, queue construction,
 * and derived statistics.
 */
object FsrsEngine {

    private val w: DoubleArray get() = FsrsConfig.DEFAULT_W
    private val DECAY: Double = -w[20]
    private val FACTOR: Double = (0.9).pow(1.0 / DECAY) - 1.0

    // Interval modifier: for target retention 0.9, intervalModifier is 1.0
    val intervalModifier: Double =
        ((FsrsConfig.REQUEST_RETENTION.pow(1.0 / DECAY) - 1.0) / FACTOR)

    fun initStability(rating: FsrsRating): Double {
        return max(w[rating.value - 1], 0.1)
    }

    fun initDifficulty(rating: FsrsRating): Double {
        val d = w[4] - exp((rating.value - 1) * w[5]) + 1.0
        return d.coerceIn(1.0, 10.0)
    }

    fun nextDifficulty(currentD: Double, rating: FsrsRating): Double {
        val deltaD = -w[6] * (rating.value - 3)
        val nextD = currentD + deltaD * (10.0 - currentD) / 9.0
        val meanReversion = w[7] * initDifficulty(FsrsRating.Easy) + (1.0 - w[7]) * nextD
        return meanReversion.coerceIn(1.0, 10.0)
    }

    fun forgettingCurve(elapsedDays: Double, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + FACTOR * (elapsedDays / stability)).pow(DECAY)
    }

    fun nextRecallStability(d: Double, s: Double, r: Double, rating: FsrsRating): Double {
        val hardPenalty = if (rating == FsrsRating.Hard) w[15] else 1.0
        val easyBonus = if (rating == FsrsRating.Easy) w[16] else 1.0
        val factor = exp(w[8]) * (11.0 - d) * s.pow(-w[9]) * (exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus
        return (s * (1.0 + factor)).coerceIn(0.001, FsrsConfig.MAXIMUM_INTERVAL_DAYS.toDouble())
    }

    fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val sForget = w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) * exp((1.0 - r) * w[14])
        val sMin = s / exp(w[17] * w[18])
        return sForget.coerceIn(min(sMin, 0.001), max(sForget, 0.001))
            .coerceIn(0.001, FsrsConfig.MAXIMUM_INTERVAL_DAYS.toDouble())
    }

    fun nextShortTermStability(s: Double, rating: FsrsRating): Double {
        val baseS = max(s, 0.001)
        val sinc = baseS.pow(-w[19]) * exp(w[17] * (rating.value - 3 + w[18]))
        val maskedSinc = if (rating.value >= FsrsRating.Hard.value) max(sinc, 1.0) else sinc
        return (baseS * maskedSinc).coerceIn(0.001, FsrsConfig.MAXIMUM_INTERVAL_DAYS.toDouble())
    }

    fun nextInterval(stability: Double): Long {
        val days = (stability * intervalModifier).roundToLong()
        return max(1L, days).coerceAtMost(FsrsConfig.MAXIMUM_INTERVAL_DAYS.toLong())
    }

    /**
     * Pure function: Rate a card, returning an updated immutable card and review log.
     * The input card is never mutated.
     */
    fun rateCard(card: FsrsCard, rating: FsrsRating, now: Long): Pair<FsrsCard, FsrsReviewLog> {
        val prevElapsed = if (card.last_review != null) {
            max(0L, (now - card.last_review) / FsrsConfig.DAY_MS)
        } else 0L

        val (nextCard, scheduledDays) = when (card.state) {
            FsrsState.New -> {
                val d = initDifficulty(rating)
                val s = initStability(rating)
                val reps = card.reps + 1

                when (rating) {
                    FsrsRating.Again -> {
                        val due = now + FsrsConfig.LEARNING_STEPS_MINUTES[0] * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = 0L,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses + 1,
                            state = FsrsState.Learning,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to 0L
                    }
                    FsrsRating.Hard -> {
                        val due = now + 5 * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = 0L,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Learning,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to 0L
                    }
                    FsrsRating.Good -> {
                        val due = now + FsrsConfig.LEARNING_STEPS_MINUTES[1] * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = 0L,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Learning,
                            last_review = now,
                            last_rating = rating,
                            step = 1,
                        ) to 0L
                    }
                    FsrsRating.Easy -> {
                        val ivl = nextInterval(s)
                        val due = now + ivl * FsrsConfig.DAY_MS
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = 0L,
                            scheduled_days = ivl,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Review,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to ivl
                    }
                }
            }

            FsrsState.Learning -> {
                val reps = card.reps + 1
                when (rating) {
                    FsrsRating.Again -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextShortTermStability(card.stability, rating)
                        val due = now + FsrsConfig.LEARNING_STEPS_MINUTES[0] * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses + 1,
                            state = FsrsState.Learning,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to 0L
                    }
                    FsrsRating.Hard -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextShortTermStability(card.stability, rating)
                        val due = now + 5 * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Learning,
                            last_review = now,
                            last_rating = rating,
                        ) to 0L
                    }
                    FsrsRating.Good -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextShortTermStability(card.stability, rating)
                        val ivl = nextInterval(s)
                        val due = now + ivl * FsrsConfig.DAY_MS
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = ivl,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Review,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to ivl
                    }
                    FsrsRating.Easy -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextShortTermStability(card.stability, rating)
                        val ivl = max(nextInterval(s), 2L)
                        val due = now + ivl * FsrsConfig.DAY_MS
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = ivl,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Review,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to ivl
                    }
                }
            }

            FsrsState.Review -> {
                val reps = card.reps + 1
                val r = forgettingCurve(prevElapsed.toDouble(), card.stability)

                when (rating) {
                    FsrsRating.Again -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextForgetStability(card.difficulty, card.stability, r)
                        val due = now + FsrsConfig.RELEARNING_STEPS_MINUTES[0] * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses + 1,
                            state = FsrsState.Relearning,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to 0L
                    }
                    FsrsRating.Hard -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextRecallStability(card.difficulty, card.stability, r, rating)
                        val ivl = nextInterval(s)
                        val due = now + ivl * FsrsConfig.DAY_MS
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = ivl,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Review,
                            last_review = now,
                            last_rating = rating,
                        ) to ivl
                    }
                    FsrsRating.Good -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextRecallStability(card.difficulty, card.stability, r, rating)
                        val ivl = nextInterval(s)
                        val due = now + ivl * FsrsConfig.DAY_MS
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = ivl,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Review,
                            last_review = now,
                            last_rating = rating,
                        ) to ivl
                    }
                    FsrsRating.Easy -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextRecallStability(card.difficulty, card.stability, r, rating)
                        val ivl = nextInterval(s)
                        val due = now + ivl * FsrsConfig.DAY_MS
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = ivl,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Review,
                            last_review = now,
                            last_rating = rating,
                        ) to ivl
                    }
                }
            }

            FsrsState.Relearning -> {
                val reps = card.reps + 1
                when (rating) {
                    FsrsRating.Again -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextShortTermStability(card.stability, rating)
                        val due = now + FsrsConfig.RELEARNING_STEPS_MINUTES[0] * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses + 1,
                            state = FsrsState.Relearning,
                            last_review = now,
                            last_rating = rating,
                        ) to 0L
                    }
                    FsrsRating.Hard -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextShortTermStability(card.stability, rating)
                        val due = now + 15 * 60_000L
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = 0L,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Relearning,
                            last_review = now,
                            last_rating = rating,
                        ) to 0L
                    }
                    FsrsRating.Good, FsrsRating.Easy -> {
                        val d = nextDifficulty(card.difficulty, rating)
                        val s = nextShortTermStability(card.stability, rating)
                        val ivl = nextInterval(s)
                        val due = now + ivl * FsrsConfig.DAY_MS
                        card.copy(
                            due = due,
                            stability = s,
                            difficulty = d,
                            elapsed_days = prevElapsed,
                            scheduled_days = ivl,
                            reps = reps,
                            lapses = card.lapses,
                            state = FsrsState.Review,
                            last_review = now,
                            last_rating = rating,
                            step = 0,
                        ) to ivl
                    }
                }
            }
        }

        val reviewLog = FsrsReviewLog(
            cardId = card.cardId,
            rating = rating,
            state = card.state,
            due = nextCard.due,
            stability = nextCard.stability,
            difficulty = nextCard.difficulty,
            elapsed_days = prevElapsed,
            scheduled_days = scheduledDays,
            review = now,
        )

        return nextCard to reviewLog
    }

    /**
     * Checks if a card is in the "Mastered" bucket.
     * Rule: state == Review AND stability >= 21.0 days.
     */
    fun isMastered(card: FsrsCard): Boolean {
        return card.state == FsrsState.Review && card.stability >= FsrsConfig.MASTERED_STABILITY_DAYS
    }

    /**
     * Checks if a card is in the "Weak" bucket.
     * Rule: state != New AND (state == Relearning OR lapses >= 2 OR last_rating == Again).
     */
    fun isWeak(card: FsrsCard): Boolean {
        if (card.state == FsrsState.New) return false
        return card.state == FsrsState.Relearning ||
            card.lapses >= FsrsConfig.WEAK_LAPSE_THRESHOLD ||
            card.last_rating == FsrsRating.Again
    }

    /**
     * Pure function: Computes derived deck stats on read.
     * Each card falls into exactly ONE bucket:
     * 1. Mastered
     * 2. Weak
     * 3. Normal
     * Due is separate: New cards + cards with due <= now.
     */
    fun getDeckStats(cards: List<FsrsCard>, now: Long): FsrsDeckStats {
        var masteredCount = 0
        var weakCount = 0
        var dueCount = 0
        var earliestFutureDue: Long? = null

        for (card in cards) {
            // Bucket check in strict order
            if (isMastered(card)) {
                masteredCount++
            } else if (isWeak(card)) {
                weakCount++
            }

            // Due count is separate: New cards or cards due <= now
            if (card.state == FsrsState.New || card.due <= now) {
                dueCount++
            } else {
                if (earliestFutureDue == null || card.due < earliestFutureDue) {
                    earliestFutureDue = card.due
                }
            }
        }

        return FsrsDeckStats(
            due = dueCount,
            weak = weakCount,
            mastered = masteredCount,
            nextDueTime = earliestFutureDue,
        )
    }

    /**
     * Pure function: Builds the study session queue.
     * Selection happens FIRST, shuffle happens LAST:
     * 1. Due reviews first: Learning/Relearning, then Weak, then most overdue (earliest due).
     * 2. New cards fill remaining slots (deck order).
     * 3. If due reviews >= sessionSize, add ZERO new cards.
     * 4. Session size counts unique cards, not repeats.
     * 5. If shuffle is enabled, shuffle only the ALREADY SELECTED set.
     */
    fun buildQueue(
        cards: List<FsrsCard>,
        sessionSize: Int?,
        now: Long,
        shuffle: Boolean = false,
        random: Random = Random.Default,
    ): List<String> {
        if (cards.isEmpty()) return emptyList()

        // 1. Due reviews (already studied and due now or overdue)
        val dueReviews = cards.filter { it.state != FsrsState.New && it.due <= now }
            .sortedWith(
                compareBy<FsrsCard> {
                    // Priority 1: Learning or Relearning comes first
                    if (it.state == FsrsState.Learning || it.state == FsrsState.Relearning) 0 else 1
                }.thenBy {
                    // Priority 2: Weak cards come next
                    if (isWeak(it)) 0 else 1
                }.thenBy {
                    // Priority 3: Most overdue (smallest due timestamp)
                    it.due
                }
            )

        // 2. New cards (never studied, in natural deck order)
        val newCards = cards.filter { it.state == FsrsState.New }

        // 3. Selection
        val selectedCards: List<FsrsCard> = if (sessionSize == null) {
            // "All" option: all due reviews + all new cards
            dueReviews + newCards
        } else {
            if (dueReviews.size >= sessionSize) {
                // If due reviews >= session size, add ZERO new cards
                dueReviews.take(sessionSize)
            } else {
                // Take all due reviews, fill remaining slots with new cards in deck order
                val leftoverSlots = sessionSize - dueReviews.size
                dueReviews + newCards.take(leftoverSlots)
            }
        }

        // 4. Shuffle: Selection happens FIRST, shuffle happens LAST
        val orderedCards = if (shuffle) {
            selectedCards.shuffled(random)
        } else {
            selectedCards
        }

        return orderedCards.map { it.cardId }
    }
}
