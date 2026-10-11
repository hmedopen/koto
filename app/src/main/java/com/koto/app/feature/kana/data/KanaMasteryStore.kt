package com.koto.app.feature.kana.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class KanaMasteryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("koto_kana_store", Context.MODE_PRIVATE)

    companion object {
        const val TOTAL_KANA_COUNT = 92 // 46 Hiragana + 46 Katakana
        private const val KEY_MASTERED = "mastered_kana_set"
        private const val KEY_SELECTED_ROWS = "selected_practice_rows"
        private const val KEY_SELECTED_HIRAGANA = "selected_practice_hiragana"
        private const val KEY_SELECTED_KATAKANA = "selected_practice_katakana"
        private const val KEY_SELECTED_KANA = "selected_practice_kana"

        @Volatile
        private var instance: KanaMasteryStore? = null

        fun get(context: Context): KanaMasteryStore = instance ?: synchronized(this) {
            instance ?: KanaMasteryStore(context).also { instance = it }
        }
    }

    var masteredCount by mutableIntStateOf(0)
        private set

    var selectedPracticeRows by mutableStateOf<Set<String>>(emptySet())
        private set

    var selectedHiraganaIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var selectedKatakanaIds by mutableStateOf<Set<String>>(emptySet())
        private set

    val selectedKanaIds: Set<String>
        get() = selectedHiraganaIds + selectedKatakanaIds

    private val masteredSet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val scoresCache = java.util.concurrent.ConcurrentHashMap<String, Int>()

    var scoreVersion by mutableIntStateOf(0)
        private set

    init {
        val loadedMastered = prefs.getStringSet(KEY_MASTERED, null) ?: defaultMasteredSeed()
        masteredSet.addAll(loadedMastered)
        masteredCount = masteredSet.size

        prefs.all.forEach { (key, value) ->
            if (key.startsWith("score_") && value is Int) {
                scoresCache[key] = value
            }
        }

        val loadedRows = prefs.getStringSet(KEY_SELECTED_ROWS, null) ?: setOf("row_a", "row_k")
        selectedPracticeRows = loadedRows

        val defaultHira = KanaRepository.basicRows.flatMap { it.columns.filterNotNull() }.map { it.id }.toSet()
        val defaultKata = KanaRepository.basicRows.flatMap { it.columns.filterNotNull() }.map { it.id }.toSet()
        selectedHiraganaIds = prefs.getStringSet(KEY_SELECTED_HIRAGANA, null)
            ?: prefs.getStringSet(KEY_SELECTED_KANA, null)
            ?: defaultHira
        selectedKatakanaIds = prefs.getStringSet(KEY_SELECTED_KATAKANA, null)
            ?: prefs.getStringSet(KEY_SELECTED_KANA, null)
            ?: defaultKata
    }

    private fun defaultMasteredSeed(): Set<String> {
        return KanaRepository.allCharacters.take(46).map { "hira_${it.id}" }.toSet()
    }

    fun getCharacterMastery(kanaId: String, isHiragana: Boolean): Int {
        val prefix = if (isHiragana) "score_hira_" else "score_kata_"
        val key = prefix + kanaId
        val cached = scoresCache[key]
        if (cached != null) {
            return cached
        }
        val defaultScore = if (isMastered(kanaId, isHiragana)) 95 else 10
        scoresCache[key] = defaultScore
        return defaultScore
    }

    fun updateMasteryScore(kanaId: String, isHiragana: Boolean, delta: Int): Int {
        val current = getCharacterMastery(kanaId, isHiragana)
        val newScore = (current + delta).coerceIn(0, 100)
        val prefix = if (isHiragana) "score_hira_" else "score_kata_"
        val key = prefix + kanaId
        scoresCache[key] = newScore
        prefs.edit().putInt(key, newScore).apply()

        val masteredKey = if (isHiragana) "hira_$kanaId" else "kata_$kanaId"
        if (newScore >= 90) {
            masteredSet.add(masteredKey)
        } else {
            masteredSet.remove(masteredKey)
        }
        prefs.edit().putStringSet(KEY_MASTERED, masteredSet.toSet()).apply()
        masteredCount = masteredSet.size
        scoreVersion++
        return newScore
    }

    fun isMastered(kanaId: String, isHiragana: Boolean): Boolean {
        val key = if (isHiragana) "hira_$kanaId" else "kata_$kanaId"
        return masteredSet.contains(key)
    }

    fun toggleMastered(kanaId: String, isHiragana: Boolean): Boolean {
        val key = if (isHiragana) "hira_$kanaId" else "kata_$kanaId"
        val newState = if (masteredSet.contains(key)) {
            masteredSet.remove(key)
            false
        } else {
            masteredSet.add(key)
            true
        }
        prefs.edit().putStringSet(KEY_MASTERED, masteredSet.toSet()).apply()
        masteredCount = masteredSet.size
        val scoreKey = (if (isHiragana) "score_hira_" else "score_kata_") + kanaId
        val newScore = if (newState) 95 else 20
        scoresCache[scoreKey] = newScore
        prefs.edit().putInt(scoreKey, newScore).apply()
        scoreVersion++
        return newState
    }

    fun setRowSelected(rowId: String, selected: Boolean) {
        val updated = selectedPracticeRows.toMutableSet()
        if (selected) {
            updated.add(rowId)
        } else {
            updated.remove(rowId)
        }
        selectedPracticeRows = updated
        prefs.edit().putStringSet(KEY_SELECTED_ROWS, updated).apply()
    }

    fun selectAllRows() {
        val allRowIds = KanaRepository.rows.map { it.id }.toSet()
        selectedPracticeRows = allRowIds
        prefs.edit().putStringSet(KEY_SELECTED_ROWS, allRowIds).apply()
    }

    fun clearAllRows() {
        selectedPracticeRows = emptySet()
        prefs.edit().putStringSet(KEY_SELECTED_ROWS, emptySet()).apply()
    }

    fun isCharacterSelected(kanaId: String, isHiragana: Boolean): Boolean {
        return if (isHiragana) selectedHiraganaIds.contains(kanaId) else selectedKatakanaIds.contains(kanaId)
    }

    fun isCharacterSelected(kanaId: String): Boolean {
        return selectedHiraganaIds.contains(kanaId) || selectedKatakanaIds.contains(kanaId)
    }

    fun toggleCharacterSelected(kanaId: String, isHiragana: Boolean): Boolean {
        return if (isHiragana) {
            val updated = selectedHiraganaIds.toMutableSet()
            val newState = if (updated.contains(kanaId)) {
                updated.remove(kanaId)
                false
            } else {
                updated.add(kanaId)
                true
            }
            selectedHiraganaIds = updated
            prefs.edit().putStringSet(KEY_SELECTED_HIRAGANA, updated).apply()
            newState
        } else {
            val updated = selectedKatakanaIds.toMutableSet()
            val newState = if (updated.contains(kanaId)) {
                updated.remove(kanaId)
                false
            } else {
                updated.add(kanaId)
                true
            }
            selectedKatakanaIds = updated
            prefs.edit().putStringSet(KEY_SELECTED_KATAKANA, updated).apply()
            newState
        }
    }

    fun toggleCharacterSelected(kanaId: String): Boolean {
        return toggleCharacterSelected(kanaId, isHiragana = true)
    }

    fun setCharactersSelected(kanaIds: Collection<String>, isHiragana: Boolean, selected: Boolean) {
        if (isHiragana) {
            val updated = selectedHiraganaIds.toMutableSet()
            if (selected) updated.addAll(kanaIds) else updated.removeAll(kanaIds.toSet())
            selectedHiraganaIds = updated
            prefs.edit().putStringSet(KEY_SELECTED_HIRAGANA, updated).apply()
        } else {
            val updated = selectedKatakanaIds.toMutableSet()
            if (selected) updated.addAll(kanaIds) else updated.removeAll(kanaIds.toSet())
            selectedKatakanaIds = updated
            prefs.edit().putStringSet(KEY_SELECTED_KATAKANA, updated).apply()
        }
    }

    fun setCharactersSelected(kanaIds: Collection<String>, selected: Boolean) {
        setCharactersSelected(kanaIds, isHiragana = true, selected = selected)
        setCharactersSelected(kanaIds, isHiragana = false, selected = selected)
    }

    fun selectAllCharacters(isHiragana: Boolean) {
        val all = KanaRepository.allCharacters.map { it.id }.toSet()
        if (isHiragana) {
            selectedHiraganaIds = all
            prefs.edit().putStringSet(KEY_SELECTED_HIRAGANA, all).apply()
        } else {
            selectedKatakanaIds = all
            prefs.edit().putStringSet(KEY_SELECTED_KATAKANA, all).apply()
        }
    }

    fun selectAllCharacters() {
        selectAllCharacters(isHiragana = true)
        selectAllCharacters(isHiragana = false)
    }

    fun clearAllCharacters(isHiragana: Boolean) {
        if (isHiragana) {
            selectedHiraganaIds = emptySet()
            prefs.edit().putStringSet(KEY_SELECTED_HIRAGANA, emptySet()).apply()
        } else {
            selectedKatakanaIds = emptySet()
            prefs.edit().putStringSet(KEY_SELECTED_KATAKANA, emptySet()).apply()
        }
    }

    fun clearAllCharacters() {
        clearAllCharacters(isHiragana = true)
        clearAllCharacters(isHiragana = false)
    }
}
