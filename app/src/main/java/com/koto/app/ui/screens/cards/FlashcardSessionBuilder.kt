package com.koto.app.ui.screens.cards

import com.koto.app.feature.cards.srs.FsrsCard
import com.koto.app.feature.cards.srs.FsrsEngine
import kotlin.random.Random

/**
 * Session builder delegating directly to FSRS queue builder.
 */
object FlashcardSessionBuilder {

    fun buildSessionQueue(
        deck: FlashcardDeck,
        srsRecords: Map<String, FsrsCard>,
        sessionSize: Int?,
        shuffle: Boolean = false,
        now: Long = System.currentTimeMillis(),
        random: Random = Random.Default,
    ): List<String> {
        val cards = deck.cards.map { card ->
            srsRecords[card.id] ?: FsrsCard.createNew(card.id, deck.id, now)
        }
        return FsrsEngine.buildQueue(
            cards = cards,
            sessionSize = sessionSize,
            now = now,
            shuffle = shuffle,
            random = random,
        )
    }
}
