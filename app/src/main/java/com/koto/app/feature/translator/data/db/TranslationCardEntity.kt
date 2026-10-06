package com.koto.app.feature.translator.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_cards")
data class TranslationCardEntity(
    @PrimaryKey
    val id: String,
    val japanese: String, // Phonetic pure Kana (Hiragana/Katakana)
    val romaji: String,
    val english: String,
    val isSentence: Boolean = false,
    val contextNote: String = "",
    val exampleKana: String = "",
    val exampleRomaji: String = "",
    val exampleEnglish: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val kanji: String = "", // Native Japanese text containing Kanji (or same as japanese if kana-only)
)
