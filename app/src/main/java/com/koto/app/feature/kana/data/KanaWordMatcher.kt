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

    private class AllowedGlyphs(
        val singleGlyphs: Set<Char>,
        val doubleGlyphs: Set<String>,
    )

    /**
     * Counts how many vocabulary words can be formed exclusively using the given set of selected Kana characters.
     */
    fun countAvailableWords(words: List<PracticeWord>, selectedKana: Set<KanaCharacter>): Int {
        if (selectedKana.isEmpty()) return 0
        val allowed = buildAllowedGlyphs(selectedKana)
        return words.count { canSpell(it.kana, allowed) }
    }

    @JvmName("countAvailableWordsPairs")
    fun countAvailableWords(vocabulary: List<Pair<String, String>>, selectedKana: Set<KanaCharacter>): Int {
        if (selectedKana.isEmpty()) return 0
        val allowed = buildAllowedGlyphs(selectedKana)
        return vocabulary.count { canSpell(it.first, allowed) }
    }

    /**
     * Finds vocabulary words that can be spelled using the given selected Kana.
     */
    fun findAvailablePracticeWords(words: List<PracticeWord>, selectedKana: Set<KanaCharacter>): List<PracticeWord> {
        if (selectedKana.isEmpty()) return emptyList()
        val allowed = buildAllowedGlyphs(selectedKana)
        return words.filter { canSpell(it.kana, allowed) }
    }

    fun findAvailableWords(vocabulary: List<Pair<String, String>>, selectedKana: Set<KanaCharacter>): List<Pair<String, String>> {
        if (selectedKana.isEmpty()) return emptyList()
        val allowed = buildAllowedGlyphs(selectedKana)
        return vocabulary.filter { canSpell(it.first, allowed) }
    }

    private fun buildAllowedGlyphs(selectedKana: Set<KanaCharacter>): AllowedGlyphs {
        val singles = HashSet<Char>(selectedKana.size * 2 + 8)
        val doubles = HashSet<String>(selectedKana.size * 2)

        for (kana in selectedKana) {
            val h = kana.hiragana
            if (h.length == 1) singles.add(h[0]) else if (h.isNotEmpty()) doubles.add(h)
            val k = kana.katakana
            if (k.length == 1) singles.add(k[0]) else if (k.isNotEmpty()) doubles.add(k)
        }
        singles.add(' ')
        singles.add('・')
        singles.add('ー')
        singles.add('っ')
        singles.add('ッ')

        return AllowedGlyphs(singles, doubles)
    }

    private fun isSmallKana(c: Char): Boolean =
        c == 'ゃ' || c == 'ゅ' || c == 'ょ' ||
            c == 'ャ' || c == 'ュ' || c == 'ョ' ||
            c == 'ぁ' || c == 'ぃ' || c == 'ぅ' || c == 'ぇ' || c == 'ぉ' ||
            c == 'ァ' || c == 'ィ' || c == 'ゥ' || c == 'ェ' || c == 'ォ' ||
            c == 'ゎ' || c == 'ヮ'

    private fun canSpell(word: String, allowed: AllowedGlyphs): Boolean {
        var i = 0
        val len = word.length
        while (i < len) {
            // Check 2-character contracted digraph (e.g. きゃ, シュ) only when small kana follows
            if (i + 1 < len && isSmallKana(word[i + 1])) {
                val sub2 = word.substring(i, i + 2)
                if (allowed.doubleGlyphs.contains(sub2)) {
                    i += 2
                    continue
                }
            }
            if (allowed.singleGlyphs.contains(word[i])) {
                i += 1
            } else {
                return false
            }
        }
        return true
    }
}
