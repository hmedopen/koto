package com.koto.app.ui.screens.cards

import androidx.compose.runtime.saveable.listSaver
import kotlin.random.Random

enum class CardRating { Again, Hard, Good, Easy }
data class DeckCounts(val new: Int, val weak: Int, val mastered: Int)

/** Task-local mock study state. Survives tab changes and recreation; no SRS scheduler or disk store. */
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
) {
    val complete get() = studying && index >= order.size
    val currentId get() = order.getOrNull(index)
    fun open(deck: FlashcardDeck) = copy(deckId = deck.id, studying = false, order = emptyList(), index = 0, revealed = false)
    fun back() = if (studying) copy(studying = false, revealed = false) else copy(deckId = null)
    fun start(deck: FlashcardDeck, random: Random = Random.Default): FlashcardState {
        val ids = deck.cards.map { it.id }
        return copy(deckId = deck.id, studying = true, order = if (shuffle) ids.shuffled(random) else ids,
            index = 0, revealed = false)
    }
    fun flip() = if (studying && !complete) copy(revealed = !revealed) else this
    // The expected ID also guards callbacks from a card that has already advanced.
    fun rate(expectedId: String, rating: CardRating, now: Long = System.currentTimeMillis()): FlashcardState {
        if (!studying || !revealed || currentId != expectedId || deckId == null) return this
        return copy(ratings = ratings + (expectedId to rating), index = index + 1, revealed = false,
            practiced = practiced + (deckId to now))
    }
    fun favorite(id: String) = copy(favorites = favorites.toggle(id))
    fun pin(id: String) = copy(pinned = pinned.toggle(id))
    fun counts(deck: FlashcardDeck): DeckCounts {
        val weak = deck.cards.count { ratings[it.id] == CardRating.Again || ratings[it.id] == CardRating.Hard }
        val mastered = deck.cards.count { ratings[it.id] == CardRating.Good || ratings[it.id] == CardRating.Easy }
        return DeckCounts(deck.cards.size - weak - mastered, weak, mastered)
    }

    companion object {
        val Saver = listSaver<FlashcardState, Any>(save = {
            listOf(it.deckId.orEmpty(), it.studying, it.showRomaji, it.shuffle, it.japaneseFirst,
                it.order.joinToString(","), it.index, it.revealed,
                it.ratings.entries.joinToString(",") { entry -> "${entry.key}:${entry.value.name}" },
                it.favorites.joinToString(","), it.pinned.joinToString(","),
                it.practiced.entries.joinToString(",") { entry -> "${entry.key}:${entry.value}" })
        }, restore = {
            FlashcardState((it[0] as String).ifEmpty { null }, it[1] as Boolean, it[2] as Boolean,
                it[3] as Boolean, it[4] as Boolean, split(it[5]), it[6] as Int, it[7] as Boolean,
                split(it[8]).associate { entry -> entry.substringBefore(':') to CardRating.valueOf(entry.substringAfter(':')) },
                split(it[9]).toSet(), split(it[10]).toSet(),
                split(it[11]).associate { entry -> entry.substringBefore(':') to entry.substringAfter(':').toLong() })
        })
        private fun split(value: Any) = (value as String).split(',').filter { it.isNotEmpty() }
    }
}

private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id
