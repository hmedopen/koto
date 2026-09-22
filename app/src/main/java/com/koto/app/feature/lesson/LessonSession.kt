package com.koto.app.feature.lesson

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import com.koto.app.feature.lesson.model.*

enum class QuizFeedback { None, WarningMissing, WarningExtra, WarningOrder, Wrong }

data class SessionState(
    val index: Int = 0,
    val selected: String? = null,
    val tiles: List<String> = emptyList(),
    val matched: Set<String> = emptySet(),
    val left: String? = null,
    val right: String? = null,
    val mismatch: Boolean = false,
    val failedAnswers: Set<String> = emptySet(),
    val feedback: QuizFeedback = QuizFeedback.None,
    val feedbackEpoch: Int = 0,
    val solved: Boolean = false,
    val hadMistake: Boolean = false,
    val results: List<Boolean> = emptyList(),
)

/** One deterministic, saveable session. No question creates a route or owns audio. */
class LessonSession(val lesson: LessonDefinition, initial: SessionState = SessionState()) {
    var state by mutableStateOf(initial)
        private set
    val finished get() = state.index >= lesson.questions.size
    val question get() = lesson.questions.getOrNull(state.index)
    val checked get() = state.solved
    val correct get() = state.results.getOrNull(state.index)
    val progress get() = state.results.size.toFloat() / lesson.questions.size
    val canCheck get() = !checked && when (val q = question) {
        is Question.Cloze -> state.selected != null
        is Question.SentenceBuilder -> state.tiles.isNotEmpty()
        else -> false
    }
    fun select(id: String) {
        if (checked || finished) return
        when (val q = question) {
            is Question.MeaningChoice -> if (q.options.any { it.id == id } && id !in state.failedAnswers) {
                state = state.copy(selected = id)
                if (id == q.correctId) evaluate(!state.hadMistake) else rejectAnswer(id)
            }
            is Question.ConversationResponse -> if (q.responses.any { it.id == id } && id !in state.failedAnswers) {
                state = state.copy(selected = id)
                if (id == q.correctId) evaluate(!state.hadMistake) else rejectAnswer(id)
            }
            is Question.Cloze -> if (q.options.any { it.id == id }) state = state.copy(selected = id, feedback = QuizFeedback.None)
            else -> Unit
        }
    }
    fun toggleTile(id: String) {
        val q = question as? Question.SentenceBuilder ?: return
        if (checked || q.tiles.none { it.id == id }) return
        state = when {
            id in state.tiles -> state.copy(tiles = state.tiles - id, feedback = QuizFeedback.None)
            else -> state.copy(tiles = state.tiles + id, feedback = QuizFeedback.None)
        }
    }
    fun check(): JapaneseText? {
        if (!canCheck) return null
        return when (val q = question) {
            is Question.Cloze -> {
                val correct = state.selected == q.correctId
                if (correct) { evaluate(!state.hadMistake); q.filled(q.correctId) }
                else { setFeedback(QuizFeedback.Wrong); null }
            }
            is Question.SentenceBuilder -> {
                when (val result = sentenceFeedback(state.tiles, q.correctOrder)) {
                    QuizFeedback.None -> { evaluate(!state.hadMistake); q.sentence }
                    else -> { setFeedback(result); null }
                }
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
                // Completing the board is always a successful result. A prior rejected
                // pairing only controls transient mismatch feedback, not the final outcome.
                if (state.matched.size == q.pairs.size) evaluate(true)
            } else state = state.copy(mismatch = true)
        }
    }
    fun clearMismatch() { if (state.mismatch) state = state.copy(left = null, right = null, mismatch = false) }
    private fun rejectAnswer(id: String) {
        state = state.copy(selected = id, failedAnswers = state.failedAnswers + id,
            feedback = QuizFeedback.Wrong, feedbackEpoch = state.feedbackEpoch + 1, hadMistake = true)
        evaluate(false)
    }
    private fun setFeedback(value: QuizFeedback) {
        state = state.copy(feedback = value, feedbackEpoch = state.feedbackEpoch + 1, hadMistake = true)
        evaluate(false)
    }
    private fun evaluate(correct: Boolean) { state = state.copy(solved = true, results = state.results + correct) }
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
                    putBooleanArray("results", results.toBooleanArray())
                    putStringArrayList("failed", ArrayList(failedAnswers)); putString("feedback", feedback.name); putInt("feedbackEpoch", feedbackEpoch)
                    putBoolean("solved", solved); putBoolean("hadMistake", hadMistake)
                }
            } },
            restore = { b -> LessonSession(lesson, SessionState(b.getInt("index"), b.getString("selected"),
                b.getStringArrayList("tiles")?.toList().orEmpty(), b.getStringArrayList("matched")?.toSet().orEmpty(),
                b.getString("left"), b.getString("right"), b.getBoolean("mismatch"),
                b.getStringArrayList("failed")?.toSet().orEmpty(),
                b.getString("feedback")?.let { runCatching { QuizFeedback.valueOf(it) }.getOrDefault(QuizFeedback.None) } ?: QuizFeedback.None,
                b.getInt("feedbackEpoch"), b.getBoolean("solved"), b.getBoolean("hadMistake"),
                b.getBooleanArray("results")?.toList().orEmpty())) },
        )
    }
}

private fun sentenceFeedback(actual: List<String>, expected: List<String>): QuizFeedback {
    if (actual == expected) return QuizFeedback.None
    if (actual.size + 1 == expected.size && expected.indices.any { index ->
            actual == expected.filterIndexed { expectedIndex, _ -> expectedIndex != index }
        }) return QuizFeedback.WarningMissing
    if (actual.size == expected.size + 1 && actual.indices.any { index ->
            actual.filterIndexed { actualIndex, _ -> actualIndex != index } == expected
        }) return QuizFeedback.WarningExtra
    if (actual.size == expected.size && actual.toSet() == expected.toSet()) {
        val differences = actual.indices.filter { actual[it] != expected[it] }
        if (differences.size in 2..3) return QuizFeedback.WarningOrder
    }
    return QuizFeedback.Wrong
}
