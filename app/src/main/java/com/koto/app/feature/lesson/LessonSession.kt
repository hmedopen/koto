package com.koto.app.feature.lesson

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import com.koto.app.feature.lesson.model.*

data class SessionState(
    val index: Int = 0,
    val selected: String? = null,
    val tiles: List<String> = emptyList(),
    val matched: Set<String> = emptySet(),
    val left: String? = null,
    val right: String? = null,
    val mismatch: Boolean = false,
    val pairMistake: Boolean = false,
    val results: List<Boolean> = emptyList(),
)

/** One deterministic, saveable session. No question creates a route or owns audio. */
class LessonSession(val lesson: LessonDefinition, initial: SessionState = SessionState()) {
    var state by mutableStateOf(initial)
        private set
    val finished get() = state.index >= lesson.questions.size
    val question get() = lesson.questions.getOrNull(state.index)
    val checked get() = state.results.size > state.index
    val correct get() = state.results.getOrNull(state.index)
    val progress get() = state.results.size.toFloat() / lesson.questions.size
    val canCheck get() = !checked && when (val q = question) {
        is Question.Cloze -> state.selected != null
        is Question.SentenceBuilder -> state.tiles.size == q.correctOrder.size
        else -> false
    }
    fun select(id: String) {
        if (checked || finished) return
        when (val q = question) {
            is Question.MeaningChoice -> if (q.options.any { it.id == id }) {
                state = state.copy(selected = id); evaluate(id == q.correctId)
            }
            is Question.ConversationResponse -> if (q.responses.any { it.id == id }) {
                state = state.copy(selected = id); evaluate(id == q.correctId)
            }
            is Question.Cloze -> if (q.options.any { it.id == id }) state = state.copy(selected = id)
            else -> Unit
        }
    }
    fun toggleTile(id: String) {
        val q = question as? Question.SentenceBuilder ?: return
        if (checked || q.tiles.none { it.id == id }) return
        state = when {
            id in state.tiles -> state.copy(tiles = state.tiles - id)
            state.tiles.size < q.correctOrder.size -> state.copy(tiles = state.tiles + id)
            else -> state
        }
    }
    fun check(): JapaneseText? {
        if (!canCheck) return null
        return when (val q = question) {
            is Question.Cloze -> {
                val correct = state.selected == q.correctId
                evaluate(correct)
                if (correct) q.filled(q.correctId) else null
            }
            is Question.SentenceBuilder -> {
                val correct = state.tiles == q.correctOrder
                evaluate(correct)
                if (correct) q.sentence else null
            }
            else -> null
        }
    }
    fun pair(id: String, japanese: Boolean) {
        val q = question as? Question.PairMatch ?: return
        if (checked || state.mismatch || id in state.matched || q.pairs.none { it.id == id }) return
        state = if (japanese) state.copy(left = id.takeUnless { state.left == id })
            else state.copy(right = id.takeUnless { state.right == id })
        if (state.left != null && state.right != null) {
            if (state.left == state.right) {
                state = state.copy(matched = state.matched + id, left = null, right = null)
                if (state.matched.size == q.pairs.size) evaluate(!state.pairMistake)
            } else state = state.copy(mismatch = true, pairMistake = true)
        }
    }
    fun clearMismatch() { if (state.mismatch) state = state.copy(left = null, right = null, mismatch = false) }
    private fun evaluate(correct: Boolean) { state = state.copy(results = state.results + correct) }
    fun next() {
        if (!checked || finished) return
        state = SessionState(index = state.index + 1, results = state.results)
    }
    fun replay() { state = SessionState() }

    companion object {
        fun saver(lesson: LessonDefinition) = Saver<LessonSession, Bundle>(
            save = { session -> with(session.state) {
                Bundle().apply {
                    putInt("index", index); putString("selected", selected)
                    putStringArrayList("tiles", ArrayList(tiles)); putStringArrayList("matched", ArrayList(matched))
                    putString("left", left); putString("right", right); putBoolean("mismatch", mismatch)
                    putBoolean("mistake", pairMistake); putBooleanArray("results", results.toBooleanArray())
                }
            } },
            restore = { b -> LessonSession(lesson, SessionState(b.getInt("index"), b.getString("selected"),
                b.getStringArrayList("tiles")?.toList().orEmpty(), b.getStringArrayList("matched")?.toSet().orEmpty(),
                b.getString("left"), b.getString("right"), b.getBoolean("mismatch"), b.getBoolean("mistake"),
                b.getBooleanArray("results")?.toList().orEmpty())) },
        )
    }
}
