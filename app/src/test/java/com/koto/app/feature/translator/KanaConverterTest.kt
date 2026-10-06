package com.koto.app.feature.translator

import com.koto.app.feature.translator.data.KanaConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun extractRubyTokens_segmentsKanjiAndPreservesKana() {
        // Pure kana
        val kanaTokens = KanaConverter.extractRubyTokens("こんにちは")
        assertEquals(1, kanaTokens.size)
        assertEquals("こんにちは", kanaTokens[0].surface)
        assertEquals(null, kanaTokens[0].reading)

        // Single Kanji word
        val mizuTokens = KanaConverter.extractRubyTokens("水")
        assertEquals(1, mizuTokens.size)
        assertEquals("水", mizuTokens[0].surface)
        assertEquals("みず", mizuTokens[0].reading)

        // Compound Kanji
        val senseiTokens = KanaConverter.extractRubyTokens("先生")
        assertEquals(1, senseiTokens.size)
        assertEquals("先生", senseiTokens[0].surface)
        assertEquals("せんせい", senseiTokens[0].reading)

        // Mixed sentence
        val sentenceTokens = KanaConverter.extractRubyTokens("私は水を飲みます")
        assertTrue("Tokens should not be empty", sentenceTokens.isNotEmpty())
        val watashi = sentenceTokens.firstOrNull { it.surface == "私" }
        assertNotNull(watashi)
        assertEquals("わたし", watashi?.reading)

        val mizu = sentenceTokens.firstOrNull { it.surface == "水" }
        assertNotNull(mizu)
        assertEquals("みず", mizu?.reading)
    }

    @Test
    fun extractRubyTokens_stripsOkuriganaCorrectly() {
        // Verb with okurigana: 行きます -> 行[い] + きます
        val ikimasuTokens = KanaConverter.extractRubyTokens("行きます")
        val ikimasuKanji = ikimasuTokens.firstOrNull { it.surface == "行" }
        assertNotNull("Should isolate Kanji ideograph '行'", ikimasuKanji)
        assertEquals("い", ikimasuKanji?.reading)

        val ikimasuOkurigana = ikimasuTokens.firstOrNull { it.surface == "きます" }
        assertNotNull("Should isolate okurigana 'きます'", ikimasuOkurigana)
        assertEquals(null, ikimasuOkurigana?.reading)

        // Adjective with okurigana: 面白い -> 面白[おもしろ] + い
        val omoshiroiTokens = KanaConverter.extractRubyTokens("面白い")
        val omoshiroiOkurigana = omoshiroiTokens.lastOrNull()
        assertEquals("い", omoshiroiOkurigana?.surface)
        assertEquals(null, omoshiroiOkurigana?.reading)

        // Compound verb with internal okurigana: 思い出す -> 思[おも] + い + 出[だ] + す
        val omoidasuTokens = KanaConverter.extractRubyTokens("思い出す")
        val omo = omoidasuTokens.firstOrNull { it.surface == "思" }
        assertNotNull(omo)
        assertEquals("おも", omo?.reading)

        val da = omoidasuTokens.firstOrNull { it.surface == "出" }
        assertNotNull(da)
        assertEquals("だ", da?.reading)

        // Honorific prefix: お茶 -> お + 茶[ちゃ]
        val ochaTokens = KanaConverter.extractRubyTokens("お茶")
        val o = ochaTokens.firstOrNull { it.surface == "お" }
        assertNotNull(o)
        assertEquals(null, o?.reading)
        val cha = ochaTokens.firstOrNull { it.surface == "茶" }
        assertNotNull(cha)
        assertEquals("ちゃ", cha?.reading)
    }

    @Test
    fun toSpacedRomaji_separatesWordsWithNaturalSpaces() {
        val complex = "私が部屋に入ったら、窓が開き、ヒーターオフになったが。"
        val spacedRomaji = KanaConverter.toSpacedRomaji(complex)
        println("SPACED ROMAJI: $spacedRomaji")
        assertTrue("Must contain space after 'watashi'", spacedRomaji.contains("watashi ga"))
        assertTrue("Must contain space after 'heya'", spacedRomaji.contains("heya ni"))
        assertTrue("Must contain space after 'mado'", spacedRomaji.contains("mado ga"))
        assertTrue("Must attach 'tara' to verb stem 'hait'", spacedRomaji.contains("haittara,"))
        assertTrue("Must attach 'ta' to verb stem 'nat'", spacedRomaji.contains("natta ga."))

        val basic = "私は水を飲みます。"
        val basicRomaji = KanaConverter.toSpacedRomaji(basic)
        assertEquals("watashi wa mizu o nomimasu.", basicRomaji)

        val greeting = "こんにちは、お元気ですか？"
        val greetingRomaji = KanaConverter.toSpacedRomaji(greeting)
        assertEquals("konnichiwa, ogenki desu ka?", greetingRomaji)
    }
}
