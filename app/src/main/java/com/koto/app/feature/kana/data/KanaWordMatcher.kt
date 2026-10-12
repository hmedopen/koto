package com.koto.app.feature.kana.data

import android.content.Context
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.kana.model.PracticeWord

/**
 * Vocabulary matching engine:
 * Evaluates words against the user's currently selected Kana set.
 * Powered directly by the dedicated PracticeWordPool, decoupled from the cards database.
 * Strictly enforces script separation: Hiragana words match only selected Hiragana;
 * Katakana words match only selected Katakana.
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
     * Finds vocabulary words that can be spelled using the separated selected Hiragana and Katakana sets.
     * Prevents script cross-bleeding:
     * - If selectedHiragana is empty, 0 Hiragana words are returned.
     * - If selectedKatakana is empty, 0 Katakana words are returned.
     */
    fun findAvailablePracticeWords(
        words: List<PracticeWord>,
        selectedHiragana: Set<KanaCharacter>,
        selectedKatakana: Set<KanaCharacter>,
    ): List<PracticeWord> {
        if (selectedHiragana.isEmpty() && selectedKatakana.isEmpty()) return emptyList()

        val hiraAllowed = if (selectedHiragana.isNotEmpty()) buildAllowedHiraganaGlyphs(selectedHiragana) else null
        val kataAllowed = if (selectedKatakana.isNotEmpty()) buildAllowedKatakanaGlyphs(selectedKatakana) else null

        return words.filter { word ->
            when (word.script) {
                KanaScript.Hiragana -> hiraAllowed != null && canSpell(word.kana, hiraAllowed)
                KanaScript.Katakana -> kataAllowed != null && canSpell(word.kana, kataAllowed)
            }
        }
    }

    /**
     * Backward-compatible overload when a single selectedKana set is passed.
     */
    fun findAvailablePracticeWords(words: List<PracticeWord>, selectedKana: Set<KanaCharacter>): List<PracticeWord> {
        return findAvailablePracticeWords(words, selectedKana, selectedKana)
    }

    /**
     * Counts how many vocabulary words can be formed using the separated Hiragana and Katakana selections.
     */
    fun countAvailableWords(
        words: List<PracticeWord>,
        selectedHiragana: Set<KanaCharacter>,
        selectedKatakana: Set<KanaCharacter>,
    ): Int {
        return findAvailablePracticeWords(words, selectedHiragana, selectedKatakana).size
    }

    fun countAvailableWords(words: List<PracticeWord>, selectedKana: Set<KanaCharacter>): Int {
        return findAvailablePracticeWords(words, selectedKana, selectedKana).size
    }

    fun findAvailableWords(
        vocabulary: List<Pair<String, String>>,
        selectedHiragana: Set<KanaCharacter>,
        selectedKatakana: Set<KanaCharacter>,
    ): List<Pair<String, String>> {
        if (selectedHiragana.isEmpty() && selectedKatakana.isEmpty()) return emptyList()

        val hiraAllowed = if (selectedHiragana.isNotEmpty()) buildAllowedHiraganaGlyphs(selectedHiragana) else null
        val kataAllowed = if (selectedKatakana.isNotEmpty()) buildAllowedKatakanaGlyphs(selectedKatakana) else null

        return vocabulary.filter { (kana, _) ->
            val isKatakana = isKatakanaWord(kana)
            if (isKatakana) {
                kataAllowed != null && canSpell(kana, kataAllowed)
            } else {
                hiraAllowed != null && canSpell(kana, hiraAllowed)
            }
        }
    }

    fun findAvailableWords(vocabulary: List<Pair<String, String>>, selectedKana: Set<KanaCharacter>): List<Pair<String, String>> {
        return findAvailableWords(vocabulary, selectedKana, selectedKana)
    }

    @JvmName("countAvailableWordsPairs")
    fun countAvailableWords(vocabulary: List<Pair<String, String>>, selectedKana: Set<KanaCharacter>): Int {
        return findAvailableWords(vocabulary, selectedKana, selectedKana).size
    }

    private fun isKatakanaWord(s: String): Boolean {
        return s.any { it in '\u30A0'..'\u30FF' }
    }

    private fun buildAllowedHiraganaGlyphs(selectedKana: Set<KanaCharacter>): AllowedGlyphs {
        val singles = HashSet<Char>(selectedKana.size * 2 + 8)
        val doubles = HashSet<String>(selectedKana.size * 2)

        for (kana in selectedKana) {
            val h = kana.hiragana
            if (h.length == 1) singles.add(h[0]) else if (h.isNotEmpty()) doubles.add(h)
        }
        singles.add(' ')
        singles.add('・')
        singles.add('ー')
        singles.add('っ')

        return AllowedGlyphs(singles, doubles)
    }

    private fun buildAllowedKatakanaGlyphs(selectedKana: Set<KanaCharacter>): AllowedGlyphs {
        val singles = HashSet<Char>(selectedKana.size * 2 + 8)
        val doubles = HashSet<String>(selectedKana.size * 2)

        for (kana in selectedKana) {
            val k = kana.katakana
            if (k.length == 1) singles.add(k[0]) else if (k.isNotEmpty()) doubles.add(k)
        }
        singles.add(' ')
        singles.add('・')
        singles.add('ー')
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
