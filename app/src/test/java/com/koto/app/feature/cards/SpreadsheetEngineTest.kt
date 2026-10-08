package com.koto.app.feature.cards

import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.cards.spreadsheet.ExportFormat
import com.koto.app.feature.cards.spreadsheet.SpreadsheetCard
import com.koto.app.feature.cards.spreadsheet.SpreadsheetEngine
import com.koto.app.feature.cards.spreadsheet.SpreadsheetValidationResult
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

class SpreadsheetEngineTest {

    @Test
    fun testTemplateGenerationProducesValidXlsxWithSeedRows() {
        val bytes = SpreadsheetEngine.generateTemplateXlsx()
        assertTrue("Generated template XLSX bytes must not be empty", bytes.isNotEmpty())

        // Save to asset directory if running in repo workspace
        val assetDir = File("src/main/assets").takeIf { it.exists() }
            ?: File("app/src/main/assets").takeIf { it.exists() }
        if (assetDir != null) {
            val templateFile = File(assetDir, "kotoba_deck_template.xlsx")
            templateFile.writeBytes(bytes)
            assertTrue("Template file written to assets", templateFile.exists())
        }

        // Parse generated template bytes back
        val result = SpreadsheetEngine.parseSpreadsheet(ByteArrayInputStream(bytes))
        if (result is SpreadsheetValidationResult.CriticalError) {
            fail("CriticalError: ${result.message}")
        }
        assertTrue("Parsing generated template must succeed", result is SpreadsheetValidationResult.Success)

        val success = result as SpreadsheetValidationResult.Success
        assertEquals("Seed rows count must be 3", 3, success.validCards.size)
        assertEquals("Skipped rows count must be 0", 0, success.skippedRows.size)

        // Seed 1
        val card1 = success.validCards[0]
        assertEquals("病院", card1.japanese)
        assertEquals("Hospital", card1.english)
        assertEquals("びょういん", card1.furigana)
        assertEquals("byouin", card1.romaji)
        assertEquals("Essential healthcare vocabulary; used when visiting medical facilities.", card1.notes)

        // Seed 2
        val card2 = success.validCards[1]
        assertEquals("食べる", card2.japanese)
        assertEquals("To eat", card2.english)
        assertEquals("たべる", card2.furigana)
        assertEquals("taberu", card2.romaji)
        assertEquals("Group 2 (ichidan) verb; versatile base verb for dining and meals.", card2.notes)

        // Seed 3 (blank furigana in seed specification)
        val card3 = success.validCards[2]
        assertEquals("ありがとう", card3.japanese)
        assertEquals("Thank you", card3.english)
        assertNull(card3.furigana)
        assertEquals("arigatou", card3.romaji)
        assertEquals("Everyday polite gratitude expression; often expanded to 'ありがとうございます'.", card3.notes)
    }

    @Test
    fun testParseCsvIngestion100PercentValid() {
        val csv = """
            Japanese,English,Furigana,Romaji,Notes
            猫,Cat,ねこ,neko,Domestic animal
            犬,Dog,いぬ,inu,Loyal animal
        """.trimIndent()

        val result = SpreadsheetEngine.parseSpreadsheet(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertTrue(result is SpreadsheetValidationResult.Success)

        val success = result as SpreadsheetValidationResult.Success
        assertEquals(2, success.validCards.size)
        assertEquals(0, success.skippedRows.size)

        assertEquals("猫", success.validCards[0].japanese)
        assertEquals("Cat", success.validCards[0].english)
        assertEquals("ねこ", success.validCards[0].furigana)
        assertEquals("neko", success.validCards[0].romaji)
        assertEquals("Domestic animal", success.validCards[0].notes)

        assertEquals("犬", success.validCards[1].japanese)
        assertEquals("Dog", success.validCards[1].english)
        assertEquals("いぬ", success.validCards[1].furigana)
        assertEquals("inu", success.validCards[1].romaji)
        assertEquals("Loyal animal", success.validCards[1].notes)
    }

    @Test
    fun testParseCsvWithPartialValidRowsAndSkippedIndices() {
        val csv = """
            Japanese,English,Furigana,Romaji,Notes
            本,Book,ほん,hon,Reading
            
            ,Hospital,びょういん,byouin,Missing japanese
            水,,みず,mizu,Missing english
            月,Moon,つき,tsuki,Night sky
        """.trimIndent()

        val result = SpreadsheetEngine.parseSpreadsheet(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertTrue(result is SpreadsheetValidationResult.Success)

        val success = result as SpreadsheetValidationResult.Success
        assertEquals(2, success.validCards.size)
        assertEquals(2, success.skippedRows.size)

        // Row 1 is header
        // Row 2 is valid (本)
        // Row 3 is completely blank (silently ignored)
        // Row 4 is missing Japanese (index 4)
        // Row 5 is missing English (index 5)
        // Row 6 is valid (月)
        assertEquals(4, success.skippedRows[0].rowIndex)
        assertTrue(success.skippedRows[0].reason.contains("Missing Japanese"))

        assertEquals(5, success.skippedRows[1].rowIndex)
        assertTrue(success.skippedRows[1].reason.contains("Missing English"))

        assertEquals("本", success.validCards[0].japanese)
        assertEquals("月", success.validCards[1].japanese)
    }

    @Test
    fun testHeaderValidationRejectionMissingRequiredHeaders() {
        // Missing Japanese / English in first two columns
        val invalidCsv = """
            Word,Meaning,Reading
            猫,Cat,neko
        """.trimIndent()

        val result = SpreadsheetEngine.parseSpreadsheet(ByteArrayInputStream(invalidCsv.toByteArray(StandardCharsets.UTF_8)))
        assertTrue(result is SpreadsheetValidationResult.CriticalError)

        val error = result as SpreadsheetValidationResult.CriticalError
        assertTrue(error.message.contains("Missing required header columns"))
    }

    @Test
    fun testHeaderValidationCaseInsensitiveAcceptance() {
        val csv = """
            JAPANESE,english,FURIGANA,romaji,notes
            海,Ocean,うみ,umi,Nature
        """.trimIndent()

        val result = SpreadsheetEngine.parseSpreadsheet(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertTrue(result is SpreadsheetValidationResult.Success)
        val success = result as SpreadsheetValidationResult.Success
        assertEquals(1, success.validCards.size)
        assertEquals("海", success.validCards[0].japanese)
    }

    @Test
    fun testExportDeckToCsvWithBom() {
        val cards = listOf(
            CustomCardItem(
                id = "c1",
                japanese = "空",
                romaji = "sora",
                english = "Sky",
                furigana = "そら",
                notes = "Blue sky",
            ),
            CustomCardItem(
                id = "c2",
                japanese = "山",
                romaji = "yama",
                english = "Mountain",
                furigana = "やま",
                notes = "High mountain",
            ),
        )

        val bytes = SpreadsheetEngine.exportDeck(
            title = "Nature",
            cards = cards,
            format = ExportFormat.CSV,
        )

        // Check UTF-8 BOM
        assertTrue("Byte array must contain at least BOM", bytes.size >= 3)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])

        // Parse back
        val result = SpreadsheetEngine.parseSpreadsheet(ByteArrayInputStream(bytes))
        assertTrue(result is SpreadsheetValidationResult.Success)
        val success = result as SpreadsheetValidationResult.Success
        assertEquals(2, success.validCards.size)
        assertEquals("空", success.validCards[0].japanese)
        assertEquals("Sky", success.validCards[0].english)
        assertEquals("そら", success.validCards[0].furigana)
        assertEquals("sora", success.validCards[0].romaji)
        assertEquals("Blue sky", success.validCards[0].notes)
    }

    @Test
    fun testExportDeckToXlsxAndParseBack() {
        val cards = listOf(
            CustomCardItem(
                id = "c1",
                japanese = "火",
                romaji = "hi",
                english = "Fire",
                furigana = "ひ",
                notes = "Hot",
            ),
        )

        val bytes = SpreadsheetEngine.exportDeck(
            title = "Elements",
            cards = cards,
            format = ExportFormat.XLSX,
        )

        assertTrue(bytes.isNotEmpty())

        val result = SpreadsheetEngine.parseSpreadsheet(ByteArrayInputStream(bytes))
        if (result is SpreadsheetValidationResult.CriticalError) {
            fail("CriticalError: ${result.message}")
        }
        assertTrue(result is SpreadsheetValidationResult.Success)
        val success = result as SpreadsheetValidationResult.Success
        assertEquals(1, success.validCards.size)
        assertEquals("火", success.validCards[0].japanese)
        assertEquals("Fire", success.validCards[0].english)
    }

    @Test
    fun testSpreadsheetCardConversionToCustomCardItem() {
        val cardWithRomaji = SpreadsheetCard(
            japanese = "花",
            english = "Flower",
            furigana = "はな",
            romaji = "hana",
            notes = "Spring",
        )
        val item1 = cardWithRomaji.toCustomCardItem()
        assertEquals("花", item1.japanese)
        assertEquals("hana", item1.romaji)
        assertEquals("Flower", item1.english)
        assertEquals("はな", item1.furigana)
        assertEquals("Spring", item1.notes)

        // Card without romaji auto-computes from pure kana
        val cardWithoutRomaji = SpreadsheetCard(
            japanese = "さくら",
            english = "Cherry blossom",
            furigana = null,
            romaji = null,
            notes = null,
        )
        val item2 = cardWithoutRomaji.toCustomCardItem()
        assertEquals("さくら", item2.japanese)
        assertEquals("sakura", item2.romaji)
        assertEquals("Cherry blossom", item2.english)
        assertEquals("", item2.furigana)
        assertEquals("", item2.notes)
    }
}
