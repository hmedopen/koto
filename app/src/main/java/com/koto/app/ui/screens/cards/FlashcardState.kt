package com.koto.app.ui.screens.cards

import androidx.compose.runtime.saveable.listSaver
import com.koto.app.feature.cards.srs.FsrsCard
import com.koto.app.feature.cards.srs.FsrsConfig
import com.koto.app.feature.cards.srs.FsrsEngine
import com.koto.app.feature.cards.srs.FsrsRating
import com.koto.app.feature.cards.srs.FsrsState
import kotlin.random.Random

data class DeckCounts(
    val due: Int,
    val weak: Int,
    val mastered: Int,
    val nextDueTime: Long? = null,
) {
    val new: Int get() = due
}

/** Task-local and persistent study state with FSRS scheduling and session size options. */
data class FlashcardState(
    val deckId: String? = null,
    val studying: Boolean = false,
    val showRomaji: Boolean = true,
    val shuffle: Boolean = false,
    val japaneseFirst: Boolean = true,
    val order: List<String> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val ratings: Map<String, FsrsRating> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val pinned: Set<String> = emptySet(),
    val practiced: Map<String, Long> = emptyMap(),
    val hasBeenRevealed: Boolean = false,
    val sessionSize: Int? = null,
    val srsRecords: Map<String, FsrsCard> = emptyMap(),
) {
    val fsrsCards: Map<String, FsrsCard> get() = srsRecords

    val complete get() = studying && index >= order.size
    val currentId get() = order.getOrNull(index)

    fun open(deck: FlashcardDeck) = copy(
        deckId = deck.id, studying = false, order = emptyList(), index = 0,
        revealed = false, hasBeenRevealed = false,
    )

    fun back() = if (studying) copy(studying = false, revealed = false, hasBeenRevealed = false) else copy(deckId = null)

    fun start(deck: FlashcardDeck, random: Random = Random.Default): FlashcardState = start(deck, sessionSize, random)

    fun start(
        deck: FlashcardDeck,
        size: Int?,
        random: Random = Random.Default,
        now: Long = System.currentTimeMillis(),
    ): FlashcardState {
        val cards = deck.cards.map { card ->
            fsrsCards[card.id] ?: FsrsCard.createNew(card.id, deck.id, now)
        }
        val sessionIds = FsrsEngine.buildQueue(
            cards = cards,
            sessionSize = size,
            now = now,
            shuffle = shuffle,
            random = random,
        )
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
        rating: FsrsRating,
        now: Long = System.currentTimeMillis(),
        requeueAgain: Boolean = false,
        requeueSoon: Boolean = false,
    ): FlashcardState {
        if (!studying || !hasBeenRevealed || currentId != expectedId || deckId == null) return this
        val currentCard = fsrsCards[expectedId] ?: FsrsCard.createNew(expectedId, deckId, now)
        val (nextCard, _) = FsrsEngine.rateCard(currentCard, rating, now)
        val isShortStep = (nextCard.due - now) <= FsrsConfig.SHORT_STEP_THRESHOLD_MS
        val nextOrder = when {
            rating == FsrsRating.Again && isShortStep && requeueAgain -> order + expectedId
            rating == FsrsRating.Again && requeueSoon -> {
                val list = order.toMutableList()
                val targetIndex = (index + 4).coerceAtMost(list.size)
                list.add(targetIndex, expectedId)
                list
            }
            else -> order
        }
        return copy(
            ratings = ratings + (expectedId to rating),
            srsRecords = srsRecords + (expectedId to nextCard),
            order = nextOrder,
            index = index + 1,
            revealed = false,
            hasBeenRevealed = false,
            practiced = practiced + (deckId to now),
        )
    }

    fun favorite(id: String) = copy(favorites = favorites.toggle(id))
    fun pin(id: String) = copy(pinned = pinned.toggle(id))

    fun dueCount(deck: FlashcardDeck, now: Long = System.currentTimeMillis()): Int {
        val cards = deck.cards.map { card ->
            fsrsCards[card.id] ?: FsrsCard.createNew(card.id, deck.id, now)
        }
        return FsrsEngine.getDeckStats(cards, now).due
    }

    fun counts(deck: FlashcardDeck, now: Long = System.currentTimeMillis()): DeckCounts {
        val cards = deck.cards.map { card ->
            fsrsCards[card.id] ?: FsrsCard.createNew(card.id, deck.id, now)
        }
        val stats = FsrsEngine.getDeckStats(cards, now)
        return DeckCounts(
            due = stats.due,
            weak = stats.weak,
            mastered = stats.mastered,
            nextDueTime = stats.nextDueTime,
        )
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
                    "${r.cardId}:${r.deckId}:${r.due}:${r.stability}:${r.difficulty}:${r.elapsed_days}:${r.scheduled_days}:${r.reps}:${r.lapses}:${r.state.name}:${r.last_review ?: -1L}:${r.last_rating?.name.orEmpty()}:${r.step}"
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

                    val card = if (p.size >= 12) {
                        // FSRS format
                        val due = p[2].toLongOrNull() ?: 0L
                        val stability = p[3].toDoubleOrNull() ?: 0.0
                        val difficulty = p[4].toDoubleOrNull() ?: 0.0
                        val elapsedDays = p[5].toLongOrNull() ?: 0L
                        val scheduledDays = p[6].toLongOrNull() ?: 0L
                        val reps = p[7].toIntOrNull() ?: 0
                        val lapses = p[8].toIntOrNull() ?: 0
                        val stateStr = p[9]
                        val state = runCatching { FsrsState.valueOf(stateStr) }.getOrDefault(FsrsState.New)
                        val lastReview = p[10].toLongOrNull()?.takeIf { l -> l >= 0 }
                        val rating = p[11].ifEmpty { null }?.let { name -> runCatching { FsrsRating.valueOf(name) }.getOrNull() }
                        val step = p.getOrNull(12)?.toIntOrNull() ?: 0
                        FsrsCard(
                            cardId = cardId,
                            deckId = deckId,
                            due = due,
                            stability = stability,
                            difficulty = difficulty,
                            elapsed_days = elapsedDays,
                            scheduled_days = scheduledDays,
                            reps = reps,
                            lapses = lapses,
                            state = state,
                            last_review = lastReview,
                            last_rating = rating,
                            step = step,
                        )
                    } else {
                        // Legacy ladder format migration
                        val stage = p.getOrNull(2)?.toIntOrNull() ?: 0
                        val dueEpochDay = p.getOrNull(3)?.toLongOrNull() ?: 0L
                        val lapses = p.getOrNull(4)?.toIntOrNull() ?: 0
                        val rating = p.getOrNull(5)?.ifEmpty { null }?.let { name -> runCatching { FsrsRating.valueOf(name) }.getOrNull() }
                        val lastReview = p.getOrNull(6)?.toLongOrNull()?.takeIf { l -> l >= 0 }
                        val due = dueEpochDay * 86_400_000L
                        val state = if (stage >= 8) FsrsState.Review else if (stage > 0) FsrsState.Review else FsrsState.New
                        FsrsCard(
                            cardId = cardId,
                            deckId = deckId,
                            due = due,
                            stability = if (stage >= 8) 25.0 else stage.toDouble(),
                            difficulty = 5.0,
                            elapsed_days = 0L,
                            scheduled_days = 0L,
                            reps = stage,
                            lapses = lapses,
                            state = state,
                            last_review = lastReview,
                            last_rating = rating,
                        )
                    }
                    cardId to card
                }
            }
            FlashcardState(
                deckId = (it[0] as String).ifEmpty { null },
                studying = it[1] as Boolean,
                showRomaji = it[2] as Boolean,
                shuffle = it[3] as Boolean,
                japaneseFirst = it[4] as Boolean,
                order = split(it[5]),
                index = it[6] as Int,
                revealed = it[7] as Boolean,
                ratings = split(it[8]).associate { entry -> entry.substringBefore(':') to FsrsRating.valueOf(entry.substringAfter(':')) },
                favorites = split(it[9]).toSet(),
                pinned = split(it[10]).toSet(),
                practiced = split(it[11]).associate { entry -> entry.substringBefore(':') to entry.substringAfter(':').toLong() },
                hasBeenRevealed = (it.getOrNull(12) as? Boolean) ?: (it[7] as Boolean),
                sessionSize = if (sizeInt >= 0) sizeInt else null,
                srsRecords = srsMap,
            )
        })

        private fun split(value: Any) = (value as String).split(',').filter { it.isNotEmpty() }
    }
}

private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id
