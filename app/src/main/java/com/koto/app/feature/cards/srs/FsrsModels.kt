package com.koto.app.feature.cards.srs

/**
 * Learning state of an FSRS card.
 */
enum class FsrsState(val value: Int) {
    New(0),
    Learning(1),
    Review(2),
    Relearning(3);

    companion object {
        fun fromInt(v: Int): FsrsState = entries.firstOrNull { it.value == v } ?: New
    }
}

/**
 * User rating response for a flashcard review (1 to 4).
 */
enum class FsrsRating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4);

    companion object {
        fun fromInt(v: Int): FsrsRating = entries.firstOrNull { it.value == v } ?: Good
    }
}

/**
 * Immutable state of a single card in the FSRS system.
 * All timestamp fields are UTC milliseconds since unix epoch.
 */
data class FsrsCard(
    val cardId: String,
    val deckId: String,
    val due: Long,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val elapsed_days: Long = 0L,
    val scheduled_days: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0,
    val state: FsrsState = FsrsState.New,
    val last_review: Long? = null,
    val last_rating: FsrsRating? = null,
    val step: Int = 0,
) {
    val lastRating: FsrsRating? get() = last_rating
    val lastReview: Long? get() = last_review
    val elapsedDays: Long get() = elapsed_days
    val scheduledDays: Long get() = scheduled_days

    companion object {
        fun createNew(cardId: String, deckId: String, now: Long): FsrsCard {
            return FsrsCard(
                cardId = cardId,
                deckId = deckId,
                due = now,
                stability = 0.0,
                difficulty = 0.0,
                elapsed_days = 0L,
                scheduled_days = 0L,
                reps = 0,
                lapses = 0,
                state = FsrsState.New,
                last_review = null,
                last_rating = null,
                step = 0,
            )
        }
    }
}

/**
 * Review log entry recorded immediately on every rating.
 */
data class FsrsReviewLog(
    val cardId: String,
    val rating: FsrsRating,
    val state: FsrsState,
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    val elapsed_days: Long,
    val scheduled_days: Long,
    val review: Long,
)

/**
 * Derived deck statistics computed on read, never stored.
 */
data class FsrsDeckStats(
    val due: Int,
    val weak: Int,
    val mastered: Int,
    val nextDueTime: Long? = null,
)
