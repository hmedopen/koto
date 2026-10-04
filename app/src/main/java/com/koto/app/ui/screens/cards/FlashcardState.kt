package com.koto.app.ui.screens.cards

import androidx.compose.runtime.saveable.listSaver
import kotlin.random.Random

enum class CardRating { Again, Hard, Good, Easy }
data class DeckCounts(val due: Int, val weak: Int, val mastered: Int) {
    val new: Int get() = due
}

/** Task-local and persistent study state with SRS scheduling and session size options. */
data class FlashcardState(
    val deckId: String? = null,
    val studying: Boolean = false,
    val showRomaji: Boolean = true,
    val shuffle: Boolean = false,
    val japaneseFirst: Boolean = true,
    val order: List<String> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val ratings: Map<String, CardRating> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val pinned: Set<String> = emptySet(),
    val practiced: Map<String, Long> = emptyMap(),
    val hasBeenRevealed: Boolean = false,
    val sessionSize: Int? = null,
    val srsRecords: Map<String, CardSrsRecord> = emptyMap(),
) {
    val complete get() = studying && index >= order.size
    val currentId get() = order.getOrNull(index)

    fun open(deck: FlashcardDeck) = copy(
        deckId = deck.id, studying = false, order = emptyList(), index = 0,
        revealed = false, hasBeenRevealed = false,
    )

    fun back() = if (studying) copy(studying = false, revealed = false, hasBeenRevealed = false) else copy(deckId = null)

    fun start(deck: FlashcardDeck, random: Random = Random.Default): FlashcardState = start(deck, sessionSize, random)

    fun start(deck: FlashcardDeck, size: Int?, random: Random = Random.Default): FlashcardState {
        val allIds = deck.cards.map { it.id }
        val orderedIds = if (shuffle) allIds.shuffled(random) else allIds
        val sessionIds = if (size != null && size in 1 until orderedIds.size) {
            orderedIds.take(size)
        } else {
            orderedIds
        }
        return copy(
            deckId = deck.id,
            studying = true,
            sessionSize = size,
            order = sessionIds,
            index = 0,
            revealed = false,
            hasBeenRevealed = false,
        )
    }

    fun flip() = if (studying && !complete) copy(
        revealed = !revealed,
        hasBeenRevealed = hasBeenRevealed || !revealed,
    ) else this

    // The expected ID also guards callbacks from a card that has already advanced.
    fun rate(
        expectedId: String,
        rating: CardRating,
        now: Long = System.currentTimeMillis(),
        requeueAgain: Boolean = false,
    ): FlashcardState {
        if (!studying || !hasBeenRevealed || currentId != expectedId || deckId == null) return this
        val currentSrs = srsRecords[expectedId]
        val nextSrs = FlashcardSrsScheduler.scheduleNext(currentSrs, expectedId, deckId, rating, now)
        val nextOrder = if (requeueAgain && rating == CardRating.Again) order + expectedId else order
        return copy(
            ratings = ratings + (expectedId to rating),
            srsRecords = srsRecords + (expectedId to nextSrs),
            order = nextOrder,
            index = index + 1,
            revealed = false,
            hasBeenRevealed = false,
            practiced = practiced + (deckId to now),
        )
    }

    fun favorite(id: String) = copy(favorites = favorites.toggle(id))
    fun pin(id: String) = copy(pinned = pinned.toggle(id))

    fun counts(deck: FlashcardDeck, now: Long = System.currentTimeMillis()): DeckCounts {
        val weak = deck.cards.count {
            ratings[it.id] == CardRating.Again || ratings[it.id] == CardRating.Hard ||
                (srsRecords[it.id]?.lastRating == CardRating.Hard && srsRecords[it.id]?.isMastered != true)
        }
        val mastered = deck.cards.count {
            ratings[it.id] == CardRating.Good || ratings[it.id] == CardRating.Easy ||
                srsRecords[it.id]?.isMastered == true
        }
        val due = (deck.cards.size - weak - mastered).coerceAtLeast(0)
        return DeckCounts(due = due, weak = weak, mastered = mastered)
    }

    companion object {
        val Saver = listSaver<FlashcardState, Any>(save = {
            listOf(
                it.deckId.orEmpty(),
                it.studying,
                it.showRomaji,
                it.shuffle,
                it.japaneseFirst,
                it.order.joinToString(","),
                it.index,
                it.revealed,
                it.ratings.entries.joinToString(",") { entry -> "${entry.key}:${entry.value.name}" },
                it.favorites.joinToString(","),
                it.pinned.joinToString(","),
                it.practiced.entries.joinToString(",") { entry -> "${entry.key}:${entry.value}" },
                it.hasBeenRevealed,
                it.sessionSize ?: -1,
                it.srsRecords.entries.joinToString(";") { entry ->
                    val r = entry.value
                    "${r.cardId}:${r.deckId}:${r.dueTimestamp}:${r.lastRating?.name.orEmpty()}:${r.consecutiveEasyCount}:${r.isMastered}:${r.lastReviewedTimestamp ?: -1L}"
                },
            )
        }, restore = {
            val sizeInt = it.getOrNull(13) as? Int ?: -1
            val srsStr = it.getOrNull(14) as? String
            val srsMap = if (srsStr.isNullOrEmpty()) emptyMap() else {
                srsStr.split(';').filter { s -> s.isNotEmpty() }.associate { token ->
                    val p = token.split(':')
                    val cardId = p[0]
                    val deckId = p[1]
                    val due = p[2].toLong()
                    val rating = p.getOrNull(3)?.ifEmpty { null }?.let { name -> runCatching { CardRating.valueOf(name) }.getOrNull() }
                    val consecutiveEasy = p.getOrNull(4)?.toIntOrNull() ?: 0
                    val isMastered = p.getOrNull(5)?.toBooleanStrictOrNull() ?: false
                    val lastReviewed = p.getOrNull(6)?.toLongOrNull()?.takeIf { l -> l >= 0 }
                    cardId to CardSrsRecord(cardId, deckId, due, rating, consecutiveEasy, isMastered, lastReviewed)
                }
            }
            FlashcardState(
                (it[0] as String).ifEmpty { null },
                it[1] as Boolean,
                it[2] as Boolean,
                it[3] as Boolean,
                it[4] as Boolean,
                split(it[5]),
                it[6] as Int,
                it[7] as Boolean,
                split(it[8]).associate { entry -> entry.substringBefore(':') to CardRating.valueOf(entry.substringAfter(':')) },
                split(it[9]).toSet(),
                split(it[10]).toSet(),
                split(it[11]).associate { entry -> entry.substringBefore(':') to entry.substringAfter(':').toLong() },
                (it.getOrNull(12) as? Boolean) ?: (it[7] as Boolean),
                if (sizeInt >= 0) sizeInt else null,
                srsMap,
            )
        })

        private fun split(value: Any) = (value as String).split(',').filter { it.isNotEmpty() }
    }
}

private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id
