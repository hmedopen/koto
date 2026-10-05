package com.koto.app.feature.translator

import com.koto.app.feature.translator.data.KanaConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KanaConverterTest {

    @Test
    fun toPureKana_convertsKanjiToHiraganaAndEnforcesZeroKanji() {
        val testCases = listOf(
            "先生" to "せんせい",
            "水" to "みず",
            "友よ" to "ともよ",
            "学校" to "がっこう",
            "日本語" to "にほんご",
            "私はパンを食べます" to "わたしはパンをたべます",
            "猫" to "ねこ",
            "犬" to "いぬ",
            "明日" to "あした",
        )

        for ((input, expectedKana) in testCases) {
            val converted = KanaConverter.toPureKana(input)
            assertFalse("Output for '$input' must contain ZERO kanji, was: $converted", KanaConverter.containsKanji(converted))
            assertEquals("Expected reading for '$input' was $expectedKana", expectedKana, converted)
        }
    }

    @Test
    fun toPureKana_preservesPureHiraganaAndKatakana() {
        val hiragana = "こんにちは"
        val katakana = "コンピューター"

        assertEquals(hiragana, KanaConverter.toPureKana(hiragana))
        assertEquals(katakana, KanaConverter.toPureKana(katakana))
        assertFalse(KanaConverter.containsKanji(KanaConverter.toPureKana(katakana)))
    }

    @Test
    fun toRomaji_generatesAccurateHepburnRomaji() {
        // Greetings
        assertEquals("konnichiwa", KanaConverter.toRomaji("こんにちは"))
        assertEquals("konbanwa", KanaConverter.toRomaji("こんばんは"))

        // Common vocabulary
        assertEquals("arigatou", KanaConverter.toRomaji("ありがとう"))
        assertEquals("sayounara", KanaConverter.toRomaji("さようなら"))
        assertEquals("sensei", KanaConverter.toRomaji("せんせい"))
        assertEquals("mizu", KanaConverter.toRomaji("みず"))

        // Sokuon (geminate consonants)
        assertEquals("gakkou", KanaConverter.toRomaji("がっこう"))
        assertEquals("matcha", KanaConverter.toRomaji("まっちゃ"))
        assertEquals("kitte", KanaConverter.toRomaji("きって"))

        // Digraphs
        assertEquals("kyou", KanaConverter.toRomaji("きょう"))
        assertEquals("shinkansen", KanaConverter.toRomaji("しんかんせん"))
        assertEquals("tokyo", KanaConverter.toRomaji("とうきょう").replace("ou", "o")) // base check
    }

    @Test
    fun containsKanji_accuratelyDetectsKanjiCharacters() {
        assertTrue(KanaConverter.containsKanji("日"))
        assertTrue(KanaConverter.containsKanji("本"))
        assertTrue(KanaConverter.containsKanji("今日"))
        assertTrue(KanaConverter.containsKanji("私は"))

        assertFalse(KanaConverter.containsKanji("ひらがな"))
        assertFalse(KanaConverter.containsKanji("カタカナ"))
        assertFalse(KanaConverter.containsKanji("English text 123 !?"))
        assertFalse(KanaConverter.containsKanji(""))
    }

    @Test
    fun katakanaHiraganaConversions() {
        assertEquals("あいうえお", KanaConverter.katakanaToHiragana("アイウエオ"))
        assertEquals("アイウエオ", KanaConverter.hiraganaToKatakana("あいうえお"))
        assertEquals("らーめん", KanaConverter.katakanaToHiragana("ラーメン")) // keeps chōonpu
    }
}
