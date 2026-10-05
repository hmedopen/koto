package com.koto.app.feature.translator.data

import android.content.Context
import android.content.SharedPreferences
import com.koto.app.feature.translator.model.TranslationLanguage
import org.json.JSONArray
import org.json.JSONObject

data class TranslationHistoryItem(
    val id: String,
    val sourceText: String,
    val targetText: String,
    val targetRomaji: String,
    val sourceLang: TranslationLanguage,
    val targetLang: TranslationLanguage,
    val timestamp: Long,
)

class TranslationHistoryStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getHistory(): List<TranslationHistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<TranslationHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TranslationHistoryItem(
                        id = obj.optString("id", "hist_$i"),
                        sourceText = obj.optString("sourceText", ""),
                        targetText = obj.optString("targetText", ""),
                        targetRomaji = obj.optString("targetRomaji", ""),
                        sourceLang = if (obj.optString("sourceLang") == "Japanese") {
                            TranslationLanguage.Japanese
                        } else {
                            TranslationLanguage.English
                        },
                        targetLang = if (obj.optString("targetLang") == "English") {
                            TranslationLanguage.English
                        } else {
                            TranslationLanguage.Japanese
                        },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    ),
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun recordQuery(
        sourceText: String,
        targetText: String,
        targetRomaji: String,
        sourceLang: TranslationLanguage,
        targetLang: TranslationLanguage,
    ) {
        val s = sourceText.trim()
        val t = targetText.trim()
        if (s.isBlank() || t.isBlank()) return

        val current = getHistory().toMutableList()
        // Deduplicate existing identical query
        current.removeAll { it.sourceText.equals(s, ignoreCase = true) && it.targetText.equals(t, ignoreCase = true) }

        val newItem = TranslationHistoryItem(
            id = "hist_${System.currentTimeMillis()}_${(s + t).hashCode()}",
            sourceText = s,
            targetText = t,
            targetRomaji = targetRomaji.trim(),
            sourceLang = sourceLang,
            targetLang = targetLang,
            timestamp = System.currentTimeMillis(),
        )
        current.add(0, newItem)

        // Keep maximum 50 recent items
        val trimmed = current.take(50)
        val array = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("sourceText", item.sourceText)
                put("targetText", item.targetText)
                put("targetRomaji", item.targetRomaji)
                put("sourceLang", item.sourceLang.name)
                put("targetLang", item.targetLang.name)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun removeItem(id: String) {
        val current = getHistory().filterNot { it.id == id }
        val array = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("sourceText", item.sourceText)
                put("targetText", item.targetText)
                put("targetRomaji", item.targetRomaji)
                put("sourceLang", item.sourceLang.name)
                put("targetLang", item.targetLang.name)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val PREFS_NAME = "koto_translation_history"
        private const val KEY_HISTORY = "history_entries"
    }
}
