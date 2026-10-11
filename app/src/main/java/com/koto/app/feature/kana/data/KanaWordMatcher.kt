package com.koto.app.feature.kana.data

import android.content.Context
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.PracticeWord

/**
 * Vocabulary matching engine:
 * Evaluates words against the user's currently selected Kana set.
 * Powered directly by the dedicated PracticeWordPool, decoupled from the cards database.
 */
object KanaWordMatcher {

    /**
     * Returns all curated practice words from the dedicated pool (5,000 words if Context is provided).
     */
    fun getAllWords(context: Context? = null): List<PracticeWord> = PracticeWordPool.getWords(context)

    /**
     * Backward-compatible helper returning (KanaReading, EnglishDefinition) pairs.
     */
    fun getVocabulary(context: Context? = null): List<Pair<String, String>> {
        return getAllWords(context).map { it.kana to it.english }
    }

    /**
     * Counts how many vocabulary words can be formed exclusively using the given set of selected Kana characters.
     */
    fun countAvailableWords(words: List<PracticeWord>, selectedKana: Set<KanaCharacter>): Int {
        if (selectedKana.isEmpty()) return 0
        val allowedGlyphs = buildAllowedGlyphSet(selectedKana)
        return words.count { canSpell(it.kana, allowedGlyphs) }
    }

    @JvmName("countAvailableWordsPairs")
    fun countAvailableWords(vocabulary: List<Pair<String, String>>, selectedKana: Set<KanaCharacter>): Int {
        if (selectedKana.isEmpty()) return 0
        val allowedGlyphs = buildAllowedGlyphSet(selectedKana)
        return vocabulary.count { canSpell(it.first, allowedGlyphs) }
    }

    /**
     * Finds vocabulary words that can be spelled using the given selected Kana.
     */
    fun findAvailablePracticeWords(words: List<PracticeWord>, selectedKana: Set<KanaCharacter>): List<PracticeWord> {
        if (selectedKana.isEmpty()) return emptyList()
        val allowedGlyphs = buildAllowedGlyphSet(selectedKana)
        return words.filter { canSpell(it.kana, allowedGlyphs) }
    }

    fun findAvailableWords(vocabulary: List<Pair<String, String>>, selectedKana: Set<KanaCharacter>): List<Pair<String, String>> {
        if (selectedKana.isEmpty()) return emptyList()
        val allowedGlyphs = buildAllowedGlyphSet(selectedKana)
        return vocabulary.filter { canSpell(it.first, allowedGlyphs) }
    }

    private fun buildAllowedGlyphSet(selectedKana: Set<KanaCharacter>): Set<String> = buildSet {
        selectedKana.forEach {
            add(it.hiragana)
            add(it.katakana)
        }
        add(" ")
        add("・")
        add("ー")
        add("っ")
        add("ッ")
    }

    private fun canSpell(word: String, allowedGlyphs: Set<String>): Boolean {
        var i = 0
        while (i < word.length) {
            // Check 2-character contracted digraph (e.g. きゃ, シュ)
            if (i + 1 < word.length && allowedGlyphs.contains(word.substring(i, i + 2))) {
                i += 2
            } else if (allowedGlyphs.contains(word.substring(i, i + 1))) {
                i += 1
            } else {
                return false
            }
        }
        return true
    }
}
