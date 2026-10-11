package com.koto.app.feature.kana.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.kana.model.KanaScript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PracticeWordPoolTest {

    @Test
    fun verifyFallbackPoolIntegrity() {
        val fallback = PracticeWordPool.fallbackWords
        assertTrue("Fallback pool must contain at least 200 words", fallback.size >= 200)

        for (word in fallback) {
            assertTrue("ID must not be blank", word.id.isNotBlank())
            assertTrue("Kana must not be blank", word.kana.isNotBlank())
            assertTrue("Romaji must not be blank", word.romaji.isNotBlank())
            assertTrue("English definition must not be blank", word.english.isNotBlank())
            assertNotNull(word.script)
            assertTrue("Category must not be blank", word.category.isNotBlank())
        }
    }

    @Test
    fun verifyFull5000WordAssetPool() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val words = PracticeWordPool.getWords(context)

        assertEquals("Full Practice pool must contain exactly 5,000 words", 5000, words.size)

        var hiraganaCount = 0
        var katakanaCount = 0
        var yoonCount = 0
        var dakutenCount = 0

        for (word in words) {
            assertTrue("ID must not be blank for word: ${word.kana}", word.id.isNotBlank())
            assertTrue("Kana must not be blank for ID: ${word.id}", word.kana.isNotBlank())
            assertTrue("Romaji must not be blank for word: ${word.kana}", word.romaji.isNotBlank())
            assertTrue("English definition must not be blank for word: ${word.kana}", word.english.isNotBlank())

            // Romaji must not contain raw untransliterated Kana or question marks
            assertFalse(
                "Romaji '${word.romaji}' for '${word.kana}' must not contain untransliterated Kana or '?'",
                word.romaji.any { it in '\u3040'..'\u30FF' || it == '?' }
            )

            if (word.script == KanaScript.Hiragana) hiraganaCount++
            if (word.script == KanaScript.Katakana) katakanaCount++
            if (word.category == "Yoon") yoonCount++
            if (word.category == "Dakuten") dakutenCount++
        }

        assertTrue("Must contain significant Hiragana words", hiraganaCount > 3000)
        assertTrue("Must contain significant Katakana loanwords", katakanaCount > 200)
        assertTrue("Must contain Yoon combinations", yoonCount > 100)
        assertTrue("Must contain Dakuten/Handakuten words", dakutenCount > 500)
    }

    @Test
    fun verifyKanaWordMatcherWith5000Words() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val allWords = KanaWordMatcher.getAllWords(context)
        assertEquals(5000, allWords.size)

        val vocabPairs = KanaWordMatcher.getVocabulary(context)
        assertEquals(5000, vocabPairs.size)
    }
}
