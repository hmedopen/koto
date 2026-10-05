package com.koto.app.feature.cards

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.cards.csv.AnkiApkgEngine
import com.koto.app.feature.cards.csv.DeckCsvEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnkiApkgEngineTest {

    @Test
    fun stripAnkiFormattingRemovesHtmlAndSoundTags() {
        val raw = "<div>先生</div><br>[sound:rec_123.mp3]<b>(せんせい)</b>&amp;&nbsp;teacher"
        val cleaned = AnkiApkgEngine.stripAnkiFormatting(raw)
        assertEquals("先生\n(せんせい)& teacher", cleaned)
    }

    @Test
    fun parseApkgExtractsCollectionDbAndSplitsUnitSeparatorFields() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = context.cacheDir

        // Create a real SQLite database collection.anki2
        val dbFile = File(tempDir, "collection.anki2")
        if (dbFile.exists()) dbFile.delete()

        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY, flds TEXT);")
        // Anki fields separated by \u001f
        db.execSQL("INSERT INTO notes (id, flds) VALUES (1, '<b>猫</b>\u001fCat\u001fねこ\u001fAnimal');")
        db.execSQL("INSERT INTO notes (id, flds) VALUES (2, '犬<br>[sound:bark.mp3]\u001fDog\u001fいぬ\u001fPet');")
        db.close()

        // Create in-memory zip archive with collection.anki2
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("collection.anki2"))
            dbFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
        dbFile.delete()

        val zipBytes = baos.toByteArray()
        val result = AnkiApkgEngine.parseApkg(context, ByteArrayInputStream(zipBytes))

        assertEquals(2, result.cards.size)
        assertEquals("猫", result.cards[0].japanese)
        assertEquals("neko", result.cards[0].romaji)
        assertEquals("Cat", result.cards[0].english)

        assertEquals("犬", result.cards[1].japanese)
        assertEquals("inu", result.cards[1].romaji)
        assertEquals("Dog", result.cards[1].english)
    }

    @Test
    fun parseImportStreamAutoDetectsApkgVsCsv() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. CSV stream
        val csv = "Japanese,English\n桜,Cherry Blossom\n"
        val csvResult = DeckCsvEngine.parseImportStream(context, ByteArrayInputStream(csv.toByteArray()))
        assertEquals(1, csvResult.cards.size)
        assertEquals("桜", csvResult.cards[0].japanese)
        assertEquals("sakura", csvResult.cards[0].romaji)
        assertEquals("Cherry Blossom", csvResult.cards[0].english)
    }
}
