package com.koto.app.feature.cards.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starred_cards")
data class StarredCardEntity(
    @PrimaryKey
    val cardId: String,
    val deckId: String? = null,
    val starredAt: Long = System.currentTimeMillis(),
)
