package com.koto.app.feature.kana.model

/**
 * Dedicated data model for Kana practice vocabulary words.
 * Completely decoupled from the Flashcards system for high-performance,
 * zero-lag offline Kana practice and transliteration drills.
 */
data class PracticeWord(
    val id: String,
    val kana: String,
    val romaji: String,
    val english: String,
    val script: KanaScript = KanaScript.Hiragana,
    val category: String = "Basic",
)
