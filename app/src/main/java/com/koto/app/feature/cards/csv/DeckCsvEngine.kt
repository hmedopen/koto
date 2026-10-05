package com.koto.app.feature.cards.csv

import android.content.Context
import android.content.Intent
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.translator.data.KanaConverter
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class CsvImportResult(
    val cards: List<CustomCardItem>,
    val skippedRows: Int,
    val totalRows: Int,
)

object DeckCsvEngine {

    const val CSV_HEADER = "Japanese,English,ExampleKana,ExampleEnglish,Notes"

    /**
     * Parses an import stream by auto-detecting APKG (ZIP magic bytes) vs UTF-8 CSV.
     */
    fun parseImportStream(context: Context, inputStream: InputStream): CsvImportResult {
        val buffered = if (inputStream is java.io.BufferedInputStream) inputStream else java.io.BufferedInputStream(inputStream)
        buffered.mark(1024)
        val header = ByteArray(4)
        val readCount = buffered.read(header)
        buffered.reset()

        if (readCount >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
            val ankiResult = AnkiApkgEngine.parseApkg(context, buffered)
            return CsvImportResult(
                cards = ankiResult.cards,
                skippedRows = ankiResult.skippedRows,
                totalRows = ankiResult.totalRows,
            )
        }

        return parseCsv(buffered)
    }

    /**
     * Parses a CSV input stream enforcing UTF-8 encoding,
     * skips headers, validates non-empty Japanese and English fields,
     * preserves Japanese Kanji/Kana, and auto-generates Romaji.
     */
    fun parseCsv(inputStream: InputStream): CsvImportResult {
        val content = java.io.BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { it.readText() }
        return parseCsvString(content)
    }

    fun parseCsvString(content: String): CsvImportResult {
        // Strip BOM if present
        val cleaned = content.removePrefix("\uFEFF")
        val records = parseCsvRecords(cleaned)

        if (records.isEmpty()) {
            return CsvImportResult(emptyList(), 0, 0)
        }

        val cards = mutableListOf<CustomCardItem>()
        var skipped = 0
        var isFirstRow = true

        for (record in records) {
            if (record.isEmpty() || record.all { it.isBlank() }) {
                continue
            }

            // Check if this row is the header
            if (isFirstRow) {
                isFirstRow = false
                val firstCol = record.getOrNull(0)?.trim()?.lowercase() ?: ""
                val secondCol = record.getOrNull(1)?.trim()?.lowercase() ?: ""
                if (firstCol in listOf("japanese", "kana", "front", "target") ||
                    secondCol in listOf("english", "meaning", "back", "definition")
                ) {
                    continue // Skip header row
                }
            }

            val rawJapanese = record.getOrNull(0)?.trim() ?: ""
            val english = record.getOrNull(1)?.trim() ?: ""
            val rawExampleKana = record.getOrNull(2)?.trim() ?: ""
            val exampleEnglish = record.getOrNull(3)?.trim() ?: ""
            val notes = record.getOrNull(4)?.trim() ?: ""

            if (rawJapanese.isBlank() || english.isBlank()) {
                skipped++
                continue
            }

            // Compute Romaji automatically in the background via Kuromoji
            val romaji = KanaConverter.toRomaji(KanaConverter.toPureKana(rawJapanese))
            val exampleRomaji = if (rawExampleKana.isNotBlank()) {
                KanaConverter.toRomaji(KanaConverter.toPureKana(rawExampleKana))
            } else {
                ""
            }

            cards.add(
                CustomCardItem(
                    id = "csv_${System.currentTimeMillis()}_${cards.size}",
                    japanese = rawJapanese,
                    romaji = romaji,
                    english = english,
                    exampleKana = rawExampleKana,
                    exampleRomaji = exampleRomaji,
                    exampleEnglish = exampleEnglish,
                    notes = notes,
                ),
            )
        }

        return CsvImportResult(
            cards = cards,
            skippedRows = skipped,
            totalRows = records.size,
        )
    }

    /**
     * Serializes a list of cards into an RFC-4180 CSV string with UTF-8 encoding.
     */
    fun exportToCsv(cards: List<CustomCardItem>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\r\n")

        for (card in cards) {
            val jp = escapeField(card.japanese)
            val en = escapeField(card.english)
            val exKana = escapeField(card.exampleKana)
            val exEn = escapeField(card.exampleEnglish)
            val notes = escapeField(card.notes)

            sb.append(jp).append(",")
                .append(en).append(",")
                .append(exKana).append(",")
                .append(exEn).append(",")
                .append(notes).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Opens Android's native share sheet (ACTION_SEND) to share the exported CSV.
     */
    fun shareDeckCsv(context: Context, deckTitle: String, cards: List<CustomCardItem>) {
        val csvData = exportToCsv(cards)
        val cleanTitle = deckTitle.ifBlank { "Custom Deck" }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "$cleanTitle - Koto Flashcards")
            putExtra(Intent.EXTRA_TEXT, csvData)
            putExtra(Intent.EXTRA_TITLE, "$cleanTitle.csv")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Export $cleanTitle").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeField(field: String): String {
        if (!field.contains(",") && !field.contains("\"") && !field.contains("\n") && !field.contains("\r")) {
            return field
        }
        val escaped = field.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * RFC-4180 CSV parser supporting multiline quoted fields, escaped quotes, and CRLF.
     */
    private fun parseCsvRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && text[i + 1] == '"') {
                        // Escaped quote
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        // End of quote
                        inQuotes = false
                        i++
                        continue
                    }
                } else {
                    currentField.append(c)
                    i++
                    continue
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                        i++
                    }
                    ',' -> {
                        currentRecord.add(currentField.toString().trim())
                        currentField.clear()
                        i++
                    }
                    '\r' -> {
                        if (i + 1 < len && text[i + 1] == '\n') {
                            i++
                        }
                        currentRecord.add(currentField.toString().trim())
                        currentField.clear()
                        records.add(currentRecord.toList())
                        currentRecord.clear()
                        i++
                    }
                    '\n' -> {
                        currentRecord.add(currentField.toString().trim())
                        currentField.clear()
                        records.add(currentRecord.toList())
                        currentRecord.clear()
                        i++
                    }
                    else -> {
                        currentField.append(c)
                        i++
                    }
                }
            }
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString().trim())
            records.add(currentRecord.toList())
        }

        return records
    }
}
