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
    val mistakes: List<Int> = emptyList(),
    val reviewing: Boolean = false,
    val reviewPending: Boolean = false,
    val attempt: Int = 0,
    val submittedCorrect: Boolean? = null,
)

/** One deterministic, saveable session. No question creates a route or owns audio. */
class LessonSession(val lesson: LessonDefinition, initial: SessionState = SessionState()) {
    var state by mutableStateOf(initial)
        private set
    init { ensureSentenceGame() }
    val finished get() = state.index >= lesson.questions.size && !state.reviewPending
    val question get() = lesson.questions.getOrNull(state.index)
    val checked get() = state.solved
    val correct get() = state.submittedCorrect ?: state.results.getOrNull(state.index)
    val progress get() = state.results.size.toFloat() / lesson.questions.size
    val canCheck get() = !checked && !state.reviewPending && when (question) {
        is Question.PairMatch, null -> false
        is Question.SentenceBuilder -> sentenceGame.sentenceTileIds.isNotEmpty()
        else -> state.selected != null
    }
    val sentenceGame: SentenceGameState
        get() = state.sentenceGame ?: (question as? Question.SentenceBuilder)?.let(SentenceGameState::initial)
            ?: error("Sentence state requested outside Build a Sentence")
    fun select(id: String) {
        if (checked || finished) return
        when (val q = question) {
            is Question.MeaningChoice -> if (q.options.any { it.id == id }) state = state.copy(selected = id)
            is Question.ConversationResponse -> if (q.responses.any { it.id == id }) state = state.copy(selected = id)
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
    /** Every manual game submits through the same result and review lifecycle. */
    fun check(): JapaneseText? {
        if (!canCheck) return null
        val q = question ?: return null
        val correct = when (q) {
            is Question.MeaningChoice -> state.selected == q.correctId
            is Question.ConversationResponse -> state.selected == q.correctId
            is Question.Cloze -> state.selected == q.correctId
            is Question.SentenceBuilder -> {
                val game = sentenceGame
                val validation = sentenceValidation(game.sentenceTileIds, q)
                updateSentenceGame(game.copy(validation = validation,
                    validationEpoch = game.validationEpoch + 1, movingTileId = null))
                validation == SentenceValidation.Correct
            }
            is Question.PairMatch -> return null
        }
        evaluate(correct)
        return if (correct) when (q) {
            is Question.Cloze -> q.filled(q.correctId)
            is Question.SentenceBuilder -> q.sentence
            else -> null
        } else null
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
    private fun evaluate(correct: Boolean) {
        state = state.copy(solved = true, submittedCorrect = correct,
            feedback = if (correct) QuizFeedback.None else QuizFeedback.Wrong,
            feedbackEpoch = state.feedbackEpoch + if (correct) 0 else 1,
            hadMistake = !correct,
            results = if (state.reviewing) state.results else state.results + correct,
            mistakes = if (!state.reviewing && !correct && state.index !in state.mistakes)
                state.mistakes + state.index else state.mistakes)
    }
    fun next() {
        if (!checked || finished) return
        val queue = if (state.reviewing) {
            state.mistakes.drop(1) + if (correct == false) listOf(state.index) else emptyList()
        } else state.mistakes
        val nextIndex = if (state.reviewing) queue.firstOrNull() ?: lesson.questions.size else state.index + 1
        state = SessionState(index = nextIndex, results = state.results, mistakes = queue,
            reviewing = state.reviewing,
            reviewPending = !state.reviewing && nextIndex >= lesson.questions.size && queue.isNotEmpty(),
            attempt = state.attempt + 1)
        ensureSentenceGame()
    }
    fun startReview() {
        if (!state.reviewPending) return
        state = SessionState(index = state.mistakes.first(), results = state.results,
            mistakes = state.mistakes, reviewing = true, attempt = state.attempt + 1)
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
                    putIntegerArrayList("mistakes", ArrayList(mistakes))
                    putBoolean("reviewing", reviewing); putBoolean("reviewPending", reviewPending)
                    putInt("attempt", attempt)
                    submittedCorrect?.let { putBoolean("submittedCorrect", it) }
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
                ) },
                mistakes = b.getIntegerArrayList("mistakes")?.toList().orEmpty(),
                reviewing = b.getBoolean("reviewing"), reviewPending = b.getBoolean("reviewPending"),
                attempt = b.getInt("attempt"),
                submittedCorrect = if (b.containsKey("submittedCorrect")) b.getBoolean("submittedCorrect") else null,
            )) },
        )
    }
}

private fun sentenceValidation(actualIds: List<String>, question: Question.SentenceBuilder): SentenceValidation {
    // Tile identity controls movement; visible word content controls acceptance.
    // Two identical authored particles must be interchangeable to the learner.
    val textById = question.tiles.associate { it.id to it.text }
    val actual = actualIds.map { textById[it] }
    val expected = question.correctOrder.map { textById.getValue(it) }
    if (actual == expected) return SentenceValidation.Correct
    if (actual.groupingBy { it }.eachCount() == expected.groupingBy { it }.eachCount()) {
        return SentenceValidation.WrongOrder
    }
    val actualCounts = actual.groupingBy { it }.eachCount()
    val expectedCounts = expected.groupingBy { it }.eachCount()
    if (actual.size == expected.size - 1 && actualCounts.all { (word, count) -> count <= expectedCounts.getOrDefault(word, 0) }) {
        return SentenceValidation.Missing
    }
    if (actual.size == expected.size + 1 && expectedCounts.all { (word, count) -> count <= actualCounts.getOrDefault(word, 0) }) {
        return SentenceValidation.Extra
    }
    return SentenceValidation.WrongTiles
}
