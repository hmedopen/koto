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

        val jsonString = readJsonString(context ?: appContext) ?: return emptyMap()
        val parsed = parseJson(jsonString)
        cachedData = parsed
        return parsed
    }

    private fun readJsonString(context: Context?): String? {
        val ctx = context ?: appContext
        if (ctx != null) {
            runCatching {
                ctx.assets.open(ASSET_FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrNull()?.let { return it }
        }

        // Relative path candidates for tests running in JVM
        val candidates = listOf(
            File("src/main/assets/$ASSET_FILE"),
            File("app/src/main/assets/$ASSET_FILE"),
            File("../app/src/main/assets/$ASSET_FILE"),
            File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/$ASSET_FILE"),
        )
        for (f in candidates) {
            if (f.exists() && f.isFile) {
                return runCatching { f.readText(Charsets.UTF_8) }.getOrNull()
            }
        }

        val stream = javaClass.classLoader?.getResourceAsStream(ASSET_FILE)
            ?: javaClass.classLoader?.getResourceAsStream("assets/$ASSET_FILE")
        if (stream != null) {
            return runCatching { stream.bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrNull()
        }

        return null
    }

    private fun parseJson(jsonString: String): Map<String, CardContext> {
        val jsonResult = runCatching {
            val root = JSONObject(jsonString)
            val result = mutableMapOf<String, CardContext>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                val cardId = obj.optString("cardId", "")
                val kana = obj.optString("kana")
                val kanji = obj.optString("kanji", "").takeIf { it.isNotBlank() }
                val romaji = obj.optString("romaji")
                val english = obj.optString("english")
                val usageNote = obj.optString("usageNote")
                val examplesArray = obj.optJSONArray("examples")
                val examples = mutableListOf<CardExample>()
                if (examplesArray != null) {
                    for (i in 0 until examplesArray.length()) {
                        val ex = examplesArray.getJSONObject(i)
                        examples.add(
                            CardExample(
                                kana = ex.optString("kana"),
                                romaji = ex.optString("romaji"),
                                english = ex.optString("english"),
                                kanji = ex.optString("kanji", "").takeIf { it.isNotBlank() },
                            ),
                        )
                    }
                }
                result[key] = CardContext(
                    cardId = cardId,
                    kana = kana,
                    romaji = romaji,
                    english = english,
                    usageNote = usageNote,
                    examples = examples,
                    kanji = kanji,
                )
            }
            result
        }.getOrNull()

        if (jsonResult != null && jsonResult.isNotEmpty()) {
            return jsonResult
        }

        return parseJsonFallback(jsonString)
    }

    private fun parseJsonFallback(jsonString: String): Map<String, CardContext> {
        val result = mutableMapOf<String, CardContext>()

        for (line in jsonString.lineSequence()) {
            val trimmed = line.trim().removeSuffix(",")
            if (!trimmed.startsWith("\"") || !trimmed.contains("\": {")) continue

            val keyEnd = trimmed.indexOf("\": {")
            if (keyEnd == -1) continue
            val key = unescapeJson(trimmed.substring(1, keyEnd))
            val body = trimmed.substring(keyEnd + 3)

            val cardId = extractJsonString(body, "cardId")
            val kana = extractJsonString(body, "kana")
            val kanji = extractJsonString(body, "kanji").takeIf { it.isNotBlank() }
            val romaji = extractJsonString(body, "romaji")
            val english = extractJsonString(body, "english")
            val usageNote = extractJsonString(body, "usageNote")

            val examples = mutableListOf<CardExample>()
            val exStart = body.indexOf("\"examples\":[")
            if (exStart != -1) {
                val exContent = body.substring(exStart + "\"examples\":[".length).substringBefore("]")
                var searchFrom = 0
                while (searchFrom < exContent.length) {
                    val objStart = exContent.indexOf("{", searchFrom)
                    if (objStart == -1) break
                    val objEnd = exContent.indexOf("}", objStart)
                    if (objEnd == -1) break
                    val exStr = exContent.substring(objStart, objEnd + 1)
                    examples.add(
                        CardExample(
                            kana = extractJsonString(exStr, "kana"),
                            romaji = extractJsonString(exStr, "romaji"),
                            english = extractJsonString(exStr, "english"),
                            kanji = extractJsonString(exStr, "kanji").takeIf { it.isNotBlank() },
                        ),
                    )
                    searchFrom = objEnd + 1
                }
            }

            result[key] = CardContext(cardId, kana, romaji, english, usageNote, examples, kanji)
        }
        return result
    }

    private fun extractJsonString(source: String, key: String): String {
        val search = "\"$key\":\""
        val startIdx = source.indexOf(search)
        if (startIdx == -1) return ""
        val valStart = startIdx + search.length
        val sb = StringBuilder()
        var i = valStart
        while (i < source.length) {
            val c = source[i]
            if (c == '\\' && i + 1 < source.length) {
                when (val next = source[i + 1]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> { sb.append('\\'); sb.append(next) }
                }
                i += 2
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun unescapeJson(str: String): String {
        return str.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}
