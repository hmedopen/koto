package com.koto.app.feature.cards.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_cards",
    indices = [Index(value = ["deckId"])],
    foreignKeys = [
        ForeignKey(
            entity = CustomDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CustomCardEntity(
    @PrimaryKey
    val id: String,
    val deckId: String,
    val japanese: String, // Strictly pure Kana (Hiragana/Katakana)
    val romaji: String,
    val english: String,
    val exampleKana: String = "",
    val exampleRomaji: String = "",
    val exampleEnglish: String = "",
    val notes: String = "",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
