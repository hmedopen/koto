package com.koto.app.feature.cards.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deck_bookmarks")
data class DeckBookmarkEntity(
    @PrimaryKey
    val deckId: String,
    val bookmarkedAt: Long = System.currentTimeMillis(),
)
