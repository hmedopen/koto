package com.koto.app.feature.cards

import com.koto.app.feature.cards.csv.DeckCsvEngine
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.translator.data.KanaConverter
import org.junit.Assert.*
import org.junit.Test

class DeckCsvEngineTest {

    @Test
    fun testParseStandardCsvWithHeader() {
        val csv = """
            Japanese,English,ExampleKana,ExampleEnglish,Notes
            おはよう,Good morning,せんせい おはよう ございます,Good morning teacher,Polite
            ありがとう,Thank you,どうも ありがとう,Thank you very much,Casual
        """.trimIndent()

        val result = DeckCsvEngine.parseCsvString(csv)
        assertEquals(2, result.cards.size)
        assertEquals(0, result.skippedRows)

        val card1 = result.cards[0]
        assertEquals("おはよう", card1.japanese)
        assertEquals("ohayou", card1.romaji)
        assertEquals("Good morning", card1.english)
        assertEquals("せんせい おはよう ございます", card1.exampleKana)
        assertEquals("sensei ohayou gozaimasu", card1.exampleRomaji)
        assertEquals("Good morning teacher", card1.exampleEnglish)
        assertEquals("Polite", card1.notes)

        val card2 = result.cards[1]
        assertEquals("ありがとう", card2.japanese)
        assertEquals("arigatou", card2.romaji)
        assertEquals("Thank you", card2.english)
        assertEquals("どうも ありがとう", card2.exampleKana)
        assertEquals("doumo arigatou", card2.exampleRomaji)
        assertEquals("Thank you very much", card2.exampleEnglish)
        assertEquals("Casual", card2.notes)
    }

    @Test
    fun testParsePreservesKanjiAndAutoGeneratesRomaji() {
        // Japanese input contains Kanji; parser preserves Kanji in japanese field and auto-computes Romaji
        val csv = """
            Japanese,English,ExampleKana,ExampleEnglish,Notes
            先生,Teacher,先生、こんにちは,Hello, teacher!,School context
            水,Water,水を飲みます,I drink water,Everyday
        """.trimIndent()

        val result = DeckCsvEngine.parseCsvString(csv)
        assertEquals(2, result.cards.size)

        val card1 = result.cards[0]
        assertEquals("先生", card1.japanese)
        assertEquals("sensei", card1.romaji)
        assertEquals("Teacher", card1.english)

        val card2 = result.cards[1]
        assertEquals("水", card2.japanese)
        assertEquals("mizu", card2.romaji)
        assertEquals("Water", card2.english)
    }

    @Test
    fun testParseHandlesBomAndQuotedFieldsWithCommas() {
        val bomCsv = "\uFEFF" + """
            "Japanese","English","ExampleKana","ExampleEnglish","Notes"
            "こんにちは","Hello, good afternoon","こんにちは、おげんきですか？","Hello, how are you?","Standard, polite"
        """.trimIndent()

        val result = DeckCsvEngine.parseCsvString(bomCsv)
        assertEquals(1, result.cards.size)

        val card = result.cards[0]
        assertEquals("こんにちは", card.japanese)
        assertEquals("Hello, good afternoon", card.english)
        assertEquals("Standard, polite", card.notes)
    }

    @Test
    fun testParseSkipsInvalidRows() {
        val csv = """
            Japanese,English,ExampleKana,ExampleEnglish,Notes
            ,,Example,No front or back,Invalid
            おはよう,,,No English,Invalid
            ,Thank you,,,No Japanese,Invalid
            ありがとう,Thank you,,,Valid
        """.trimIndent()

        val result = DeckCsvEngine.parseCsvString(csv)
        assertEquals(1, result.cards.size)
        assertEquals(3, result.skippedRows)
        assertEquals("ありがとう", result.cards[0].japanese)
        assertEquals("Thank you", result.cards[0].english)
    }

    @Test
    fun testExportToCsvProducesCompliantRfc4180() {
        val cards = listOf(
            CustomCardItem(
                id = "c1",
                japanese = "おはよう",
                romaji = "ohayou",
                english = "Good morning, everyone",
                exampleKana = "せんせい おはよう",
                exampleRomaji = "sensei ohayou",
                exampleEnglish = "Good morning, teacher",
                notes = "Casual / friendly",
            ),
            CustomCardItem(
                id = "c2",
                japanese = "ありがとう",
                romaji = "arigatou",
                english = "Thank you \"very much\"",
                exampleKana = "",
                exampleRomaji = "",
                exampleEnglish = "",
                notes = "",
            ),
        )

        val exported = DeckCsvEngine.exportToCsv(cards)
        assertTrue(exported.startsWith("Japanese,English,ExampleKana,ExampleEnglish,Notes"))

        // Round-trip parse
        val reimported = DeckCsvEngine.parseCsvString(exported)
        assertEquals(2, reimported.cards.size)
        assertEquals("おはよう", reimported.cards[0].japanese)
        assertEquals("Good morning, everyone", reimported.cards[0].english)
        assertEquals("Good morning, teacher", reimported.cards[0].exampleEnglish)
        assertEquals("Casual / friendly", reimported.cards[0].notes)

        assertEquals("ありがとう", reimported.cards[1].japanese)
        assertEquals("Thank you \"very much\"", reimported.cards[1].english)
    }
}
