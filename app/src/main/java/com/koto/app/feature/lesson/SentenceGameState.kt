package com.koto.app.feature.lesson

import com.koto.app.feature.lesson.model.Question

/**
 * The committed state of a Build a Sentence attempt. Tile ids are the authored
 * [com.koto.app.feature.lesson.model.Answer.id] values, so repeated words remain
 * independently addressable. The pool always follows the authored tile order.
 * Animation positions and drag previews belong to the UI, never this state.
 */
data class SentenceGameState(
    val availableTileIds: List<String>,
    val sentenceTileIds: List<String> = emptyList(),
    val validation: SentenceValidation = SentenceValidation.Idle,
    val validationEpoch: Int = 0,
    /** Legacy reorder callers only. Never persisted or used by the drag UI. */
    val movingTileId: String? = null,
) {
    companion object {
        fun initial(question: Question.SentenceBuilder) = SentenceGameState(
            availableTileIds = question.tiles.map { it.id },
        )
    }
}

/** Correct describes the accepted build; session results still record first-try credit. */
enum class SentenceValidation { Idle, Missing, WrongOrder, WrongTiles, Correct }
