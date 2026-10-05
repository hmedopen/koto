package com.koto.app.feature.cards.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_decks")
data class CustomDeckEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val icon: String,
    val category: String = "Custom",
    val number: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
