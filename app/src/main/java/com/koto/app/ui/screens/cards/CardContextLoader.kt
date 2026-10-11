package com.koto.app.ui.screens.cards

import android.content.Context
import org.json.JSONObject
import java.io.File

data class CardExample(
    val kana: String,
    val romaji: String,
    val english: String,
    val kanji: String? = null,
) {
    val displayKanji: String get() = kanji?.takeIf { it.isNotBlank() } ?: kana
}

data class CardContext(
    val cardId: String = "",
    val kana: String,
    val romaji: String,
    val english: String,
    val usageNote: String,
    val examples: List<CardExample>,
    val kanji: String? = null,
) {
    val displayKanji: String get() = kanji?.takeIf { it.isNotBlank() } ?: kana
    val notes: String get() = usageNote
}

/**
 * Data pipeline for card context and usage notes.
 *
 * Loads authored context notes and up to 3 sentence examples per card from
 * the parsed database asset (card_contexts.json). Supports exact card ID
 * lookup to prevent homonym collisions across decks.
 */
object CardContextLoader {
    private const val ASSET_FILE = "card_contexts.json"
    private var cachedData: Map<String, CardContext>? = null
    private var appContext: Context? = null

    private val dynamicContexts = java.util.concurrent.ConcurrentHashMap<String, CardContext>()

    fun registerContext(cardId: String, context: CardContext) {
        dynamicContexts[cardId] = context
        val composite = "${context.kana.trim()}|${context.romaji.trim()}"
        dynamicContexts[composite] = context
        dynamicContexts[context.kana.trim()] = context
    }

    fun removeContext(cardId: String) {
        val ctx = dynamicContexts.remove(cardId)
        if (ctx != null) {
            val composite = "${ctx.kana.trim()}|${ctx.romaji.trim()}"
            dynamicContexts.remove(composite)
            dynamicContexts.remove(ctx.kana.trim())
        }
    }

    fun getContext(card: Flashcard, context: Context? = null): CardContext? {
        return getContext(card.id, card.japanese, card.romaji, context)
    }

    fun getContext(japanese: String, romaji: String, context: Context? = null): CardContext? {
        return getContext(null, japanese, romaji, context)
    }

    fun getContext(cardId: String?, japanese: String, romaji: String, context: Context? = null): CardContext? {
        if (!cardId.isNullOrEmpty()) {
            dynamicContexts[cardId]?.let { return it }
        }
        val normalizedKana = japanese.trim()
        val normalizedRomaji = romaji.trim()
        val compositeKey = "$normalizedKana|$normalizedRomaji"
        dynamicContexts[compositeKey]?.let { return it }
        dynamicContexts[normalizedKana]?.let { return it }

        val data = loadData(context)
        if (!cardId.isNullOrEmpty()) {
            data[cardId]?.let { return it }
        }

        return data[compositeKey] ?: data[normalizedKana]
    }

    @Synchronized
    fun loadData(context: Context? = null): Map<String, CardContext> {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        cachedData?.let { return it }

        val stream = openInputStream(context ?: appContext) ?: return emptyMap()
        val parsed = runCatching {
            stream.bufferedReader(Charsets.UTF_8).use { reader ->
                parseJsonStream(reader)
            }
        }.getOrNull() ?: emptyMap()

        cachedData = parsed
        return parsed
    }

    private fun openInputStream(context: Context?): java.io.InputStream? {
        val ctx = context ?: appContext
        if (ctx != null) {
            runCatching {
                ctx.assets.open(ASSET_FILE)
            }.getOrNull()?.let { return it }
        }

        // Relative path candidates for tests running in JVM
        val candidates = listOf(
            File("src/main/assets/$ASSET_FILE"),
            File("app/src/main/assets/$ASSET_FILE"),
            File("../app/src/main/assets/$ASSET_FILE"),
        )
        for (f in candidates) {
            if (f.exists() && f.isFile) {
                return runCatching { f.inputStream() }.getOrNull()
            }
        }

        return javaClass.classLoader?.getResourceAsStream(ASSET_FILE)
            ?: javaClass.classLoader?.getResourceAsStream("assets/$ASSET_FILE")
    }

    private fun parseJsonStream(reader: java.io.Reader): Map<String, CardContext> {
        val jsonReader = android.util.JsonReader(reader).apply { isLenient = true }
        val result = HashMap<String, CardContext>(3800)
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            if (jsonReader.peek() == android.util.JsonToken.END_OBJECT) break
            val key = jsonReader.nextName()
            val ctx = readCardContext(jsonReader)
            result[key] = ctx
        }
        jsonReader.endObject()
        return result
    }

    private fun readCardContext(reader: android.util.JsonReader): CardContext {
        var cardId = ""
        var kana = ""
        var kanji: String? = null
        var romaji = ""
        var english = ""
        var usageNote = ""
        val examples = ArrayList<CardExample>(3)

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "cardId" -> cardId = reader.nextString()
                "kana" -> kana = reader.nextString()
                "kanji" -> {
                    val k = reader.nextString()
                    if (k.isNotBlank()) kanji = k
                }
                "romaji" -> romaji = reader.nextString()
                "english" -> english = reader.nextString()
                "usageNote" -> usageNote = reader.nextString()
                "examples" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        examples.add(readCardExample(reader))
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return CardContext(
            cardId = cardId,
            kana = kana,
            romaji = romaji,
            english = english,
            usageNote = usageNote,
            examples = examples,
            kanji = kanji,
        )
    }

    private fun readCardExample(reader: android.util.JsonReader): CardExample {
        var kana = ""
        var romaji = ""
        var english = ""
        var kanji: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "kana" -> kana = reader.nextString()
                "romaji" -> romaji = reader.nextString()
                "english" -> english = reader.nextString()
                "kanji" -> {
                    val k = reader.nextString()
                    if (k.isNotBlank()) kanji = k
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return CardExample(
            kana = kana,
            romaji = romaji,
            english = english,
            kanji = kanji,
        )
    }
}
