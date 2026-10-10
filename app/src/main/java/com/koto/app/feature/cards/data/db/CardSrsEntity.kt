package com.koto.app.feature.cards.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_srs",
    indices = [Index(value = ["deckId"])],
)
data class CardSrsEntity(
    @PrimaryKey
    val cardId: String,
    val deckId: String,
    val due: Long = 0L,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val elapsedDays: Long = 0L,
    val scheduledDays: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0,
    val state: String = "New",
    val lastReview: Long? = null,
    val lastRating: String? = null,
    val step: Int = 0,
    // Preserved for backwards compatibility with legacy schema version 7
    val stage: Int = 0,
    val dueDateEpochDay: Long = 0L,
    val lastReviewedTimestamp: Long? = null,
)
