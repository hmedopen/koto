package com.koto.app.feature.translator.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_cards")
data class TranslationCardEntity(
    @PrimaryKey
    val id: String,
    val japanese: String, // Strictly pure Kana (Hiragana/Katakana)
    val romaji: String,
    val english: String,
    val isSentence: Boolean = false,
    val contextNote: String = "",
    val exampleKana: String = "",
    val exampleRomaji: String = "",
    val exampleEnglish: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
