package com.koto.app.feature.cards.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fsrs_review_logs",
    indices = [
        Index(value = ["cardId"]),
        Index(value = ["review"]),
    ],
)
data class FsrsReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cardId: String,
    val rating: String,
    val state: String,
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Long,
    val scheduledDays: Long,
    val review: Long,
)
