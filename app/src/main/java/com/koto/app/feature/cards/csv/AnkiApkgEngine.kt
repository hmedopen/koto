package com.koto.app.feature.cards.csv

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.translator.data.KanaConverter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class AnkiImportResult(
    val cards: List<CustomCardItem>,
    val skippedRows: Int,
    val totalRows: Int,
)

object AnkiApkgEngine {

    /**
     * Unzips an Anki package (.apkg), extracts collection.anki2 SQLite database,
     * queries the 'notes' table, splits fields by \u001f, strips HTML/sound tags,
     * auto-generates Romaji, and constructs clean flashcards.
     */
    fun parseApkg(context: Context, inputStream: InputStream): AnkiImportResult {
        val cacheDir = context.cacheDir
        val tempDbFile = File(cacheDir, "anki_import_${System.currentTimeMillis()}_${(0..99999).random()}.anki2")

        try {
            var foundDb = false
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "collection.anki2" || name == "collection.anki21" || name.endsWith(".anki2")) {
                        FileOutputStream(tempDbFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        foundDb = true
                        break
                    }
                    entry = zis.nextEntry
                }
            }

            if (!foundDb || !tempDbFile.exists() || tempDbFile.length() == 0L) {
                return AnkiImportResult(emptyList(), 0, 0)
            }

            return parseSQLiteDatabase(tempDbFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return AnkiImportResult(emptyList(), 0, 0)
        } finally {
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }
        }
    }

    /**
     * Directly parses an SQLite collection file (collection.anki2).
     */
    fun parseSQLiteDatabase(dbFile: File): AnkiImportResult {
        var db: SQLiteDatabase? = null
        val cards = mutableListOf<CustomCardItem>()
        var skipped = 0
        var total = 0

        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT id, flds FROM notes", null)

            cursor.use { c ->
                val idIndex = c.getColumnIndex("id")
                val fldsIndex = c.getColumnIndex("flds")

                while (c.moveToNext()) {
                    total++
                    val noteId = if (idIndex >= 0) c.getString(idIndex) else "note_$total"
                    val flds = if (fldsIndex >= 0) c.getString(fldsIndex) else ""

                    if (flds.isNullOrBlank()) {
                        skipped++
                        continue
                    }

                    // Anki fields are delimited by 0x1F unit separator
                    val fields = flds.split("\u001f")
                    val rawFront = fields.getOrNull(0)?.let { stripAnkiFormatting(it) } ?: ""
                    val rawBack = fields.getOrNull(1)?.let { stripAnkiFormatting(it) } ?: ""

                    if (rawFront.isBlank() || rawBack.isBlank()) {
                        skipped++
                        continue
                    }

                    val romaji = KanaConverter.toRomaji(KanaConverter.toPureKana(rawFront))
                    val exampleField = fields.getOrNull(2)?.let { stripAnkiFormatting(it) } ?: ""
                    val notesField = fields.getOrNull(3)?.let { stripAnkiFormatting(it) } ?: ""

                    cards.add(
                        CustomCardItem(
                            id = "anki_${noteId}_${cards.size}",
                            japanese = rawFront,
                            romaji = romaji,
                            english = rawBack,
                            exampleKana = exampleField,
                            exampleRomaji = if (exampleField.isNotBlank()) {
                                KanaConverter.toRomaji(KanaConverter.toPureKana(exampleField))
                            } else "",
                            exampleEnglish = "",
                            notes = notesField,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }

        return AnkiImportResult(cards, skipped, total)
    }

    /**
     * Sanitizes Anki fields by stripping HTML tags, sound references, and HTML entities.
     */
    fun stripAnkiFormatting(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("\\[sound:[^]]+\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<div[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("[ \t]+"), " ")
            .replace(Regex("\n+"), "\n")
            .trim()
    }
}
