package com.koto.app.feature.kana.model

enum class KanaScript(val displayName: String) {
    Hiragana("Hiragana"),
    Katakana("Katakana");

    fun opposite(): KanaScript = when (this) {
        Hiragana -> Katakana
        Katakana -> Hiragana
    }
}

/**
 * Individual stroke order step with sequence number, directional arrow, and descriptive motion.
 */
data class StrokeStep(
    val stepNumber: Int,
    val arrowSymbol: String,
    val description: String,
)

/**
 * Japanese Kana character entity containing both Hiragana and Katakana representations,
 * standard Hepburn Romaji, grid coordinates, and vector stroke order breakdowns.
 */
data class KanaCharacter(
    val id: String,
    val hiragana: String,
    val katakana: String,
    val romaji: String,
    val rowLabel: String,
    val colIndex: Int,
    val strokeStepsHira: List<StrokeStep>,
    val strokeStepsKata: List<StrokeStep>,
) {
    fun characterFor(script: KanaScript): String = when (script) {
        KanaScript.Hiragana -> hiragana
        KanaScript.Katakana -> katakana
    }

    fun strokeStepsFor(script: KanaScript): List<StrokeStep> = when (script) {
        KanaScript.Hiragana -> strokeStepsHira
        KanaScript.Katakana -> strokeStepsKata
    }
}

/**
 * Grid row in the Kana matrix (e.g. A-Row, K-Row).
 * Holds exactly 5 column positions corresponding to -A, -I, -U, -E, -O.
 * Empty positions (e.g. Y-I, W-I) are represented as null for strict alignment.
 */
data class KanaRow(
    val id: String,
    val label: String,
    val displayName: String,
    val columns: List<KanaCharacter?>,
)

data class KanaGroup(
    val id: String,
    val title: String,
    val rows: List<KanaRow>,
)
