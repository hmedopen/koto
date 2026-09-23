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
    /** Build a Sentence owns its own state; [tiles] is a legacy save/UI mirror. */
    val sentenceGame: SentenceGameState? = null,
)

/** One deterministic, saveable session. No question creates a route or owns audio. */
class LessonSession(val lesson: LessonDefinition, initial: SessionState = SessionState()) {
    var state by mutableStateOf(initial)
        private set
    init { ensureSentenceGame() }
    val finished get() = state.index >= lesson.questions.size
    val question get() = lesson.questions.getOrNull(state.index)
    val checked get() = state.solved
    val correct get() = state.results.getOrNull(state.index)
    val progress get() = state.results.size.toFloat() / lesson.questions.size
    val canCheck get() = !checked && when (val q = question) {
        is Question.Cloze -> state.selected != null
        is Question.SentenceBuilder -> sentenceGame.sentenceTileIds.isNotEmpty()
        else -> false
    }
    val sentenceGame: SentenceGameState
        get() = state.sentenceGame ?: (question as? Question.SentenceBuilder)?.let(SentenceGameState::initial)
            ?: error("Sentence state requested outside Build a Sentence")
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
    /** Adds an unused authored tile to the end of the constructed sentence. */
    fun addSentenceTile(id: String) {
        val q = question as? Question.SentenceBuilder ?: return
        val game = sentenceGame
        if (checked || id !in game.availableTileIds || q.tiles.none { it.id == id }) return
        updateSentenceGame(game.copy(
            availableTileIds = game.availableTileIds - id,
            sentenceTileIds = game.sentenceTileIds + id,
            validation = SentenceValidation.Idle,
            movingTileId = null,
        ))
    }
    /** Returns one tile without disturbing the rest of the constructed sentence. */
    fun removeSentenceTile(id: String) {
        val q = question as? Question.SentenceBuilder ?: return
        val game = sentenceGame
        if (checked || id !in game.sentenceTileIds) return
        val selected = game.sentenceTileIds - id
        updateSentenceGame(game.copy(
            availableTileIds = q.tiles.map { it.id }.filterNot { it in selected },
            sentenceTileIds = selected,
            validation = SentenceValidation.Idle,
            movingTileId = null,
        ))
    }
    /**
     * Commits one reorder in a single update. [finalIndex] is the tile's index in
     * the resulting sentence, including when moving forward, and is clamped to
     * the sentence bounds. The UI can preview a drag without changing this state.
     */
    fun moveSentenceTile(id: String, finalIndex: Int) {
        if (question !is Question.SentenceBuilder || checked) return
        val game = sentenceGame
        val oldIndex = game.sentenceTileIds.indexOf(id)
        if (oldIndex < 0) return
        val destination = finalIndex.coerceIn(0, game.sentenceTileIds.lastIndex)
        if (destination == oldIndex) {
            cancelSentenceReorder()
            return
        }
        val reordered = game.sentenceTileIds.toMutableList().apply {
            removeAt(oldIndex)
            add(destination, id)
        }
        updateSentenceGame(game.copy(
            sentenceTileIds = reordered,
            validation = SentenceValidation.Idle,
            movingTileId = null,
        ))
    }
    /** Compatibility for older callers. New drag previews stay local to the UI. */
    fun startSentenceReorder(id: String) {
        if (question !is Question.SentenceBuilder) return
        val game = sentenceGame
        if (!checked && id in game.sentenceTileIds) {
            updateSentenceGame(game.copy(movingTileId = id), feedback = state.feedback)
        }
    }
    /** Places the picked-up tile immediately before [targetId]. */
    fun moveSentenceTileBefore(targetId: String) {
        if (question !is Question.SentenceBuilder) return
        val game = sentenceGame
        val moving = game.movingTileId ?: return
        if (checked || targetId !in game.sentenceTileIds || moving == targetId) return
        val withoutMoving = game.sentenceTileIds - moving
        val destination = withoutMoving.indexOf(targetId)
        moveSentenceTile(moving, destination)
    }
    fun moveSentenceTileToEnd() {
        if (question !is Question.SentenceBuilder) return
        val game = sentenceGame
        val moving = game.movingTileId ?: return
        moveSentenceTile(moving, game.sentenceTileIds.lastIndex)
    }
    fun cancelSentenceReorder() {
        if (question is Question.SentenceBuilder && state.sentenceGame?.movingTileId != null) {
            updateSentenceGame(sentenceGame.copy(movingTileId = null), feedback = state.feedback)
        }
    }
    /** Compatibility entry point for saved tests and callers from the earlier tile implementation. */
    fun toggleTile(id: String) {
        if (question !is Question.SentenceBuilder) return
        if (id in sentenceGame.sentenceTileIds) removeSentenceTile(id) else addSentenceTile(id)
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
                val game = sentenceGame
                when (val validation = sentenceValidation(game.sentenceTileIds, q)) {
                    SentenceValidation.Correct -> {
                        updateSentenceGame(game.copy(validation = validation, movingTileId = null))
                        evaluate(!state.hadMistake)
                        q.sentence
                    }
                    else -> {
                        // A wrong build is feedback, not a submission. Keep every tile in place
                        // so the learner can edit or reorder it immediately.
                        updateSentenceGame(game.copy(validation = validation, validationEpoch = game.validationEpoch + 1,
                            movingTileId = null), feedbackFor(validation), hadMistake = true)
                        null
                    }
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
        ensureSentenceGame()
    }
    fun replay() { state = SessionState(); ensureSentenceGame() }

    private fun updateSentenceGame(game: SentenceGameState, feedback: QuizFeedback = QuizFeedback.None, hadMistake: Boolean = state.hadMistake) {
        state = state.copy(sentenceGame = game, tiles = game.sentenceTileIds, feedback = feedback, hadMistake = hadMistake)
    }
    private fun ensureSentenceGame() {
        val builder = question as? Question.SentenceBuilder ?: return
        val authoredIds = builder.tiles.map { it.id }
        val restored = state.sentenceGame
        val selected = (restored?.sentenceTileIds ?: state.tiles).filter { it in authoredIds }.distinct()
        val game = (restored ?: SentenceGameState.initial(builder)).copy(
            availableTileIds = authoredIds.filterNot { it in selected },
            sentenceTileIds = selected,
            validation = if (state.solved && sentenceValidation(selected, builder) == SentenceValidation.Correct) {
                SentenceValidation.Correct
            } else restored?.validation ?: SentenceValidation.Idle,
            movingTileId = null,
        )
        state = state.copy(sentenceGame = game, tiles = selected)
    }

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
                    sentenceGame?.let { game ->
                        putStringArrayList("sentenceAvailable", ArrayList(game.availableTileIds))
                        putStringArrayList("sentenceSelected", ArrayList(game.sentenceTileIds))
                        putString("sentenceValidation", game.validation.name)
                        putInt("sentenceValidationEpoch", game.validationEpoch)
                    }
                }
            } },
            restore = { b -> LessonSession(lesson, SessionState(b.getInt("index"), b.getString("selected"),
                b.getStringArrayList("tiles")?.toList().orEmpty(), b.getStringArrayList("matched")?.toSet().orEmpty(),
                b.getString("left"), b.getString("right"), b.getBoolean("mismatch"),
                b.getStringArrayList("failed")?.toSet().orEmpty(),
                b.getString("feedback")?.let { runCatching { QuizFeedback.valueOf(it) }.getOrDefault(QuizFeedback.None) } ?: QuizFeedback.None,
                b.getInt("feedbackEpoch"), b.getBoolean("solved"), b.getBoolean("hadMistake"),
                b.getBooleanArray("results")?.toList().orEmpty(),
                b.getStringArrayList("sentenceAvailable")?.let { available -> SentenceGameState(
                    available, b.getStringArrayList("sentenceSelected")?.toList().orEmpty(),
                    b.getString("sentenceValidation")?.let { runCatching { SentenceValidation.valueOf(it) }.getOrDefault(SentenceValidation.Idle) }
                        ?: SentenceValidation.Idle,
                    b.getInt("sentenceValidationEpoch"),
                ) })) },
        )
    }
}

private fun feedbackFor(validation: SentenceValidation) = when (validation) {
    SentenceValidation.Missing -> QuizFeedback.WarningMissing
    SentenceValidation.WrongOrder -> QuizFeedback.WarningOrder
    SentenceValidation.WrongTiles -> QuizFeedback.WarningExtra
    SentenceValidation.Idle, SentenceValidation.Correct -> QuizFeedback.None
}

private fun sentenceValidation(actualIds: List<String>, question: Question.SentenceBuilder): SentenceValidation {
    // Tile identity controls movement; visible word content controls acceptance.
    // Two identical authored particles must be interchangeable to the learner.
    val textById = question.tiles.associate { it.id to it.text }
    val actual = actualIds.map { textById[it] }
    val expected = question.correctOrder.map { textById.getValue(it) }
    if (actual == expected) return SentenceValidation.Correct
    if (actual.size < expected.size) return SentenceValidation.Missing
    if (actual.groupingBy { it }.eachCount() == expected.groupingBy { it }.eachCount()) {
        return SentenceValidation.WrongOrder
    }
    return SentenceValidation.WrongTiles
}
