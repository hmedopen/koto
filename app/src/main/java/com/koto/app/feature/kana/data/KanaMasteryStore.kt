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

        fun snapToMilestone(score: Int): Int {
            return (Math.round(score / 25f).toInt() * 25).coerceIn(0, 100)
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
        // 1. Load explicit scores from SharedPreferences into cache
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("score_") && value is Int) {
                scoresCache[key] = value
            }
        }

        // 2. Load and sanitize masteredSet: purge legacy seeds that lack explicit score >= 100
        val loadedMastered = prefs.getStringSet(KEY_MASTERED, null) ?: emptySet()
        val validMastered = loadedMastered.filter { key ->
            val isHira = key.startsWith("hira_")
            val id = key.removePrefix("hira_").removePrefix("kata_")
            val scoreKey = (if (isHira) "score_hira_" else "score_kata_") + id
            scoresCache[scoreKey]?.let { it >= 100 } ?: (prefs.getInt(scoreKey, 0) >= 100)
        }.toSet()

        if (validMastered.size != loadedMastered.size) {
            prefs.edit().putStringSet(KEY_MASTERED, validMastered).apply()
        }
        masteredSet.addAll(validMastered)
        masteredCount = masteredSet.size

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

    fun getCharacterMastery(kanaId: String, isHiragana: Boolean): Int {
        val prefix = if (isHiragana) "score_hira_" else "score_kata_"
        val key = prefix + kanaId
        val cached = scoresCache[key]
        if (cached != null) {
            return cached
        }
        val stored = if (prefs.contains(key)) prefs.getInt(key, 0) else 0
        scoresCache[key] = stored
        return stored
    }

    /**
     * Resets all individual Kana scores and mastered states to 0 (clean slate).
     */
    fun clearAllMasteryScores() {
        scoresCache.clear()
        masteredSet.clear()
        masteredCount = 0
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("score_") }.forEach { editor.remove(it) }
        editor.putStringSet(KEY_MASTERED, emptySet())
        editor.apply()
        scoreVersion++
    }

    /**
     * Commits final Test session outcomes calculated by the Hybrid Mastery Engine.
     * Updates scoresCache, synchronizes masteredSet (removing items dropping < 100 and
     * adding items reaching 100), writes to SharedPreferences, and bumps scoreVersion.
     */
    fun commitTestSessionOutcomes(outcomes: List<KanaMasteryOutcome>) {
        if (outcomes.isEmpty()) return
        val editor = prefs.edit()
        for (outcome in outcomes) {
            val key = (if (outcome.isHiragana) "score_hira_" else "score_kata_") + outcome.kanaId
            val masteredKey = if (outcome.isHiragana) "hira_${outcome.kanaId}" else "kata_${outcome.kanaId}"
            val clampedScore = snapToMilestone(outcome.newScore)

            scoresCache[key] = clampedScore
            editor.putInt(key, clampedScore)

            if (clampedScore >= 100) {
                masteredSet.add(masteredKey)
            } else {
                masteredSet.remove(masteredKey)
            }
        }
        editor.putStringSet(KEY_MASTERED, masteredSet.toSet()).apply()
        masteredCount = masteredSet.size
        scoreVersion++
    }

    /**
     * Directly updates a Kana character's mastery score, snapped to the nearest 25-point milestone.
     */
    fun setCharacterMastery(kanaId: String, isHiragana: Boolean, score: Int): Int {
        val clampedScore = snapToMilestone(score)
        val prefix = if (isHiragana) "score_hira_" else "score_kata_"
        val key = prefix + kanaId
        scoresCache[key] = clampedScore
        prefs.edit().putInt(key, clampedScore).apply()

        val masteredKey = if (isHiragana) "hira_$kanaId" else "kata_$kanaId"
        if (clampedScore >= 100) {
            masteredSet.add(masteredKey)
        } else {
            masteredSet.remove(masteredKey)
        }
        prefs.edit().putStringSet(KEY_MASTERED, masteredSet.toSet()).apply()
        masteredCount = masteredSet.size
        scoreVersion++
        return clampedScore
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
        val newScore = if (newState) 100 else 0
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
