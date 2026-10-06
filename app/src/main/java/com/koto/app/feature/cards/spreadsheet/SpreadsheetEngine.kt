package com.koto.app.feature.cards.spreadsheet

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.cards.data.CustomDeckStore
import com.koto.app.ui.screens.cards.FlashcardDeck
import com.koto.app.feature.translator.data.KanaConverter
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import java.io.*
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class SpreadsheetCard(
    val japanese: String,
    val english: String,
    val furigana: String? = null,
    val romaji: String? = null,
    val notes: String? = null,
) {
    fun toCustomCardItem(idPrefix: String = "import"): CustomCardItem {
        val calculatedRomaji = romaji?.takeIf { it.isNotBlank() }
            ?: KanaConverter.toRomaji(KanaConverter.toPureKana(japanese))
        return CustomCardItem(
            id = "${idPrefix}_${System.currentTimeMillis()}_${(1000..9999).random()}",
            japanese = japanese,
            romaji = calculatedRomaji,
            english = english,
            furigana = furigana ?: "",
            exampleKana = "",
            exampleRomaji = "",
            exampleEnglish = "",
            notes = notes ?: "",
        )
    }
}

data class SkippedRow(
    val rowIndex: Int, // 1-based index
    val reason: String,
)

sealed class SpreadsheetValidationResult {
    data class Success(
        val validCards: List<SpreadsheetCard>,
        val skippedRows: List<SkippedRow>,
    ) : SpreadsheetValidationResult()

    data class CriticalError(
        val message: String,
    ) : SpreadsheetValidationResult()
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv");
}

object SpreadsheetEngine {

    val REQUIRED_HEADERS = listOf("Japanese", "English", "Furigana", "Romaji", "Notes")

    val SEED_ROWS = listOf(
        SpreadsheetCard(
            japanese = "病院",
            english = "Hospital",
            furigana = "びょういん",
            romaji = "byouin",
            notes = "Healthcare / Facilities",
        ),
        SpreadsheetCard(
            japanese = "食べる",
            english = "To eat",
            furigana = "たべる",
            romaji = "taberu",
            notes = "Group 2 verb",
        ),
        SpreadsheetCard(
            japanese = "ありがとう",
            english = "Thank you",
            furigana = null, // Blank in template architecture
            romaji = "arigatou",
            notes = "Casual expression",
        ),
    )

    // =========================================================================
    // 1. TEMPLATE GENERATION (.xlsx)
    // =========================================================================

    fun generateTemplateXlsx(): ByteArray {
        val rows = mutableListOf<List<String>>()
        rows.add(REQUIRED_HEADERS)
        for (seed in SEED_ROWS) {
            rows.add(
                listOf(
                    seed.japanese,
                    seed.english,
                    seed.furigana ?: "",
                    seed.romaji ?: "",
                    seed.notes ?: "",
                ),
            )
        }
        return generateXlsxFromRows(rows)
    }

    // =========================================================================
    // 2. PARSING & VALIDATION ENGINE (.xlsx & .csv)
    // =========================================================================

    fun parseSpreadsheet(inputStream: InputStream): SpreadsheetValidationResult {
        val buffered = if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream)
        buffered.mark(1024)
        val header = ByteArray(4)
        val readCount = buffered.read(header)
        buffered.reset()

        val isZipXlsx = readCount >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

        val rows: List<List<String>> = try {
            if (isZipXlsx) {
                parseXlsxRows(buffered)
            } else {
                parseCsvRows(buffered)
            }
        } catch (e: Exception) {
            return SpreadsheetValidationResult.CriticalError("Failed to parse file: ${e.message ?: "Invalid file format."}")
        }

        return validateRows(rows)
    }

    fun validateRows(rows: List<List<String>>): SpreadsheetValidationResult {
        if (rows.isEmpty()) {
            return SpreadsheetValidationResult.CriticalError("Spreadsheet file is empty.")
        }

        // 1. Header Row: Inspect Row 1. Case-insensitively match Col A (Japanese) and Col B (English).
        val headerRow = rows[0]
        val colA = headerRow.getOrNull(0)?.trim() ?: ""
        val colB = headerRow.getOrNull(1)?.trim() ?: ""

        if (!colA.equals("Japanese", ignoreCase = true) || !colB.equals("English", ignoreCase = true)) {
            return SpreadsheetValidationResult.CriticalError("Missing required header columns 'Japanese' and 'English' in Row 1.")
        }

        // 2. Row Processing (1-based index)
        val validCards = mutableListOf<SpreadsheetCard>()
        val skippedRows = mutableListOf<SkippedRow>()
        var hasDataRows = false

        for (i in 1 until rows.size) {
            val rowIndex = i + 1 // 1-based index
            val row = rows[i]

            // Completely blank rows: Silently ignore (do not count as skipped)
            if (row.all { it.isBlank() }) {
                continue
            }
            hasDataRows = true

            val japanese = row.getOrNull(0)?.trim() ?: ""
            val english = row.getOrNull(1)?.trim() ?: ""
            val furigana = row.getOrNull(2)?.trim()?.ifEmpty { null }
            val romaji = row.getOrNull(3)?.trim()?.ifEmpty { null }
            val notes = row.getOrNull(4)?.trim()?.ifEmpty { null }

            when {
                japanese.isNotEmpty() && english.isNotEmpty() -> {
                    validCards.add(
                        SpreadsheetCard(
                            japanese = japanese,
                            english = english,
                            furigana = furigana,
                            romaji = romaji,
                            notes = notes,
                        ),
                    )
                }
                japanese.isEmpty() && english.isNotEmpty() -> {
                    skippedRows.add(SkippedRow(rowIndex, "Row $rowIndex: Missing Japanese text"))
                }
                japanese.isNotEmpty() && english.isEmpty() -> {
                    skippedRows.add(SkippedRow(rowIndex, "Row $rowIndex: Missing English translation"))
                }
                else -> {
                    skippedRows.add(SkippedRow(rowIndex, "Row $rowIndex: Missing required Japanese and English columns"))
                }
            }
        }

        if (!hasDataRows) {
            return SpreadsheetValidationResult.CriticalError("Spreadsheet contains no data rows.")
        }

        return SpreadsheetValidationResult.Success(
            validCards = validCards,
            skippedRows = skippedRows,
        )
    }

    // =========================================================================
    // 3. DECK EXPORT ENGINE
    // =========================================================================

    fun exportDeck(
        title: String,
        cards: List<CustomCardItem>,
        format: ExportFormat,
    ): ByteArray {
        val rows = mutableListOf<List<String>>()
        rows.add(REQUIRED_HEADERS)
        for (card in cards) {
            rows.add(
                listOf(
                    card.japanese,
                    card.english,
                    card.furigana,
                    card.romaji,
                    card.notes,
                ),
            )
        }

        return when (format) {
            ExportFormat.XLSX -> generateXlsxFromRows(rows)
            ExportFormat.CSV -> generateCsvBytes(rows)
        }
    }

    data class ExportResult(
        val uri: Uri,
        val filename: String,
        val mimeType: String,
        val deckTitle: String,
        val cardCount: Int,
    )

    fun exportDeck(
        deckId: Long,
        format: ExportFormat,
        context: Context,
    ): ExportResult? = exportDeck(deckId.toString(), format, context)

    fun exportDeck(
        deckId: String,
        format: ExportFormat,
        context: Context,
        customDeckStore: CustomDeckStore = CustomDeckStore(context),
    ): ExportResult? {
        val allDecks = customDeckStore.loadCustomDecks()
        val deck = allDecks.find { it.id == deckId || it.number.toString() == deckId }
            ?: return null
        return exportDeck(deck, format, context, customDeckStore)
    }

    fun exportDeck(
        deck: FlashcardDeck,
        format: ExportFormat,
        context: Context,
        customDeckStore: CustomDeckStore = CustomDeckStore(context),
    ): ExportResult? {
        val customCards = customDeckStore.getDeckCardsDetails(deck.id)
        val cards = if (customCards.isNotEmpty()) {
            customCards
        } else {
            deck.cards.map { card ->
                CustomCardItem(
                    id = card.id,
                    japanese = card.displayKanji,
                    romaji = card.romaji,
                    english = card.english,
                    furigana = card.furigana ?: "",
                    notes = "",
                )
            }
        }
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val cleanName = deck.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").ifEmpty { "Deck" }
        val filename = "${cleanName}_KotobaExport_${dateStr}.${format.extension}"

        val bytes = exportDeck(deck.title, cards, format)
        val uri = saveToDownloads(context, filename, format.mimeType, bytes) ?: return null
        return ExportResult(
            uri = uri,
            filename = filename,
            mimeType = format.mimeType,
            deckTitle = deck.title,
            cardCount = cards.size,
        )
    }

    fun saveToDownloads(
        context: Context,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, filename)
                FileOutputStream(file).use { out ->
                    out.write(bytes)
                    out.flush()
                }
                Uri.fromFile(file)
            }
        } catch (_: Exception) {
            // Fallback to cache directory for reliable sharing
            val cacheFile = File(context.cacheDir, filename)
            FileOutputStream(cacheFile).use { out ->
                out.write(bytes)
                out.flush()
            }
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            } catch (_: Exception) {
                Uri.fromFile(cacheFile)
            }
        }
    }

    fun shareExportedFile(
        context: Context,
        uri: Uri,
        mimeType: String,
        title: String,
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$title - Kotoba Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Deck: $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // =========================================================================
    // 4. LOW-LEVEL XLSX GENERATION (Streaming OpenXML)
    // =========================================================================

    private fun generateXlsxFromRows(rows: List<List<String>>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8),
            )
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8),
            )
            zip.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8),
            )
            zip.closeEntry()

            // 4. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Kotoba Deck" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8),
            )
            zip.closeEntry()

            // 5. xl/worksheets/sheet1.xml
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetWriter = OutputStreamWriter(zip, StandardCharsets.UTF_8)
            sheetWriter.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""",
            )

            for ((rowIndex0, row) in rows.withIndex()) {
                val rowNum = rowIndex0 + 1
                sheetWriter.write("""    <row r="$rowNum">""")
                for ((colIndex, cellValue) in row.withIndex()) {
                    val colLetter = indexToColumnLetter(colIndex)
                    val cellRef = "$colLetter$rowNum"
                    val escaped = escapeXml(cellValue)
                    sheetWriter.write("""<c r="$cellRef" t="inlineStr"><is><t>$escaped</t></is></c>""")
                }
                sheetWriter.write("</row>\n")
            }

            sheetWriter.write(
                """  </sheetData>
</worksheet>""",
            )
            sheetWriter.flush()
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    // =========================================================================
    // 5. LOW-LEVEL XLSX PARSER (Streaming Zip + XmlPullParser)
    // =========================================================================

    private fun parseXlsxRows(inputStream: InputStream): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (zip.read(buffer).also { len = it } > 0) {
                        out.write(buffer, 0, len)
                    }
                    entries[entry.name] = out.toByteArray()
                }
                entry = zip.nextEntry
            }
        }

        // Shared strings if present
        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { ssBytes ->
            parseSharedStrings(ssBytes, sharedStrings)
        }

        // Find sheet entry
        val sheetEntryName = entries.keys.firstOrNull { it == "xl/worksheets/sheet1.xml" }
            ?: entries.keys.firstOrNull { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            ?: return emptyList()

        val sheetBytes = entries[sheetEntryName] ?: return emptyList()
        return parseSheetXml(sheetBytes, sharedStrings)
    }

    private fun parseSharedStrings(bytes: ByteArray, outList: MutableList<String>) {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isExpandEntityReferences = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(bytes))
        val siNodes = doc.getElementsByTagName("si")
        for (i in 0 until siNodes.length) {
            val siElement = siNodes.item(i) as? Element ?: continue
            val tNodes = siElement.getElementsByTagName("t")
            val textBuilder = StringBuilder()
            for (j in 0 until tNodes.length) {
                textBuilder.append(tNodes.item(j).textContent ?: "")
            }
            outList.add(textBuilder.toString())
        }
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = DocumentBuilderFactory.newInstance()
        factory.isExpandEntityReferences = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(bytes))

        val rowNodes = doc.getElementsByTagName("row")
        for (i in 0 until rowNodes.length) {
            val rowElement = rowNodes.item(i) as? Element ?: continue
            val currentRow = mutableMapOf<Int, String>()

            val cellNodes = rowElement.getElementsByTagName("c")
            for (j in 0 until cellNodes.length) {
                val cellElement = cellNodes.item(j) as? Element ?: continue
                val cellRef = cellElement.getAttribute("r")
                val cellType = cellElement.getAttribute("t")
                val col = columnLetterToIndex(cellRef)

                val value = when (cellType) {
                    "s" -> {
                        val vNode = cellElement.getElementsByTagName("v").item(0)
                        val idx = vNode?.textContent?.trim()?.toIntOrNull()
                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else (vNode?.textContent?.trim() ?: "")
                    }
                    "inlineStr" -> {
                        val tNode = cellElement.getElementsByTagName("t").item(0)
                        tNode?.textContent ?: ""
                    }
                    else -> {
                        val vNode = cellElement.getElementsByTagName("v").item(0)
                        val tNode = cellElement.getElementsByTagName("t").item(0)
                        vNode?.textContent?.trim() ?: tNode?.textContent ?: ""
                    }
                }
                currentRow[col] = value
            }

            val maxCol = if (currentRow.isEmpty()) 0 else currentRow.keys.maxOrNull()!! + 1
            val colsCount = maxOf(maxCol, REQUIRED_HEADERS.size)
            val rowList = (0 until colsCount).map { colIdx -> currentRow[colIdx] ?: "" }
            rows.add(rowList)
        }

        return rows
    }

    // =========================================================================
    // 6. CSV PARSER & GENERATOR (UTF-8 with BOM for Excel compatibility)
    // =========================================================================

    private fun generateCsvBytes(rows: List<List<String>>): ByteArray {
        val bos = ByteArrayOutputStream()
        // Write UTF-8 BOM (\uFEFF)
        bos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

        val writer = OutputStreamWriter(bos, StandardCharsets.UTF_8)
        for (row in rows) {
            val line = row.joinToString(",") { escapeCsvField(it) }
            writer.write(line)
            writer.write("\r\n")
        }
        writer.flush()
        return bos.toByteArray()
    }

    private fun parseCsvRows(inputStream: InputStream): List<List<String>> {
        val content = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { it.readText() }
        val cleaned = content.removePrefix("\uFEFF")
        return parseCsvRecords(cleaned)
    }

    private fun escapeCsvField(field: String): String {
        if (!field.contains(",") && !field.contains("\"") && !field.contains("\n") && !field.contains("\r")) {
            return field
        }
        val escaped = field.replace("\"", "\"\"")
        return "\"$escaped\""
    }

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
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
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

    // =========================================================================
    // 7. UTILITIES
    // =========================================================================

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun columnLetterToIndex(cellRef: String): Int {
        var index = 0
        for (char in cellRef) {
            if (char in 'A'..'Z') {
                index = index * 26 + (char - 'A' + 1)
            } else if (char in 'a'..'z') {
                index = index * 26 + (char.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return if (index > 0) index - 1 else 0
    }

    private fun indexToColumnLetter(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }
}
