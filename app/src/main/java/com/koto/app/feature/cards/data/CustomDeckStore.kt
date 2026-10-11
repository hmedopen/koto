package com.koto.app.feature.cards.data

import android.content.Context
import com.koto.app.feature.cards.data.db.CustomCardEntity
import com.koto.app.feature.cards.data.db.CustomDeckDao
import com.koto.app.feature.cards.data.db.CustomDeckEntity
import com.koto.app.feature.cards.data.db.CustomDeckWithCards
import com.koto.app.feature.translator.data.db.KotoDatabase
import com.koto.app.ui.screens.cards.CardContextLoader
import com.koto.app.ui.screens.cards.Flashcard
import com.koto.app.ui.screens.cards.FlashcardDeck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

data class CustomCardItem(
    val id: String = "",
    val japanese: String, // Strictly pure Kana or Kanji
    val romaji: String = "",
    val english: String,
    val furigana: String = "",
    val exampleKana: String = "",
    val exampleRomaji: String = "",
    val exampleEnglish: String = "",
    val example2Kana: String = "",
    val example2Romaji: String = "",
    val example2English: String = "",
    val example3Kana: String = "",
    val example3Romaji: String = "",
    val example3English: String = "",
    val notes: String = "",
    val kanji: String = "",
) {
    val example1Kana: String get() = exampleKana
    val example1Romaji: String get() = exampleRomaji
    val example1English: String get() = exampleEnglish
}

class CustomDeckStore(context: Context) {
    companion object {
        const val CUSTOM_DECK_ICON = "custom"
    }

    private val db = KotoDatabase.getInstance(context.applicationContext)
    private val dao: CustomDeckDao = db.customDeckDao()

    val customDecksFlow: Flow<List<FlashcardDeck>> = dao.getAllDecksWithCardsFlow().map { list ->
        list.map { withCards ->
            registerCardsContext(withCards.cards)
            withCards.toFlashcardDeck()
        }
    }

    fun loadCustomDecks(): List<FlashcardDeck> = runBlocking(Dispatchers.IO) {
        val list = runCatching { dao.getAllDecksWithCards() }.getOrElse { emptyList() }
        list.map { withCards ->
            registerCardsContext(withCards.cards)
            withCards.toFlashcardDeck()
        }
    }

    fun getDeckCardsDetails(deckId: String): List<CustomCardItem> = runBlocking(Dispatchers.IO) {
        val withCards = runCatching { dao.getDeckWithCards(deckId) }.getOrNull() ?: return@runBlocking emptyList()
        withCards.cards.sortedBy { it.orderIndex }.map { it.toCustomCardItem() }
    }

    fun saveDeck(
        deckId: String?,
        title: String,
        icon: String = CUSTOM_DECK_ICON,
        cards: List<CustomCardItem>,
    ): FlashcardDeck = runBlocking(Dispatchers.IO) {
        val effectiveDeckId = if (!deckId.isNullOrBlank()) deckId else "custom_deck_${System.currentTimeMillis()}"
        val cleanTitle = title.trim()
        val cleanIcon = CUSTOM_DECK_ICON
        val now = System.currentTimeMillis()

        val deckEntity = CustomDeckEntity(
            id = effectiveDeckId,
            title = cleanTitle,
            icon = cleanIcon,
            category = "Custom",
            number = 0,
            createdAt = now,
            updatedAt = now,
        )

        val cardEntities = cards.mapIndexed { index, item ->
            item.toCustomCardEntity(
                deckId = effectiveDeckId,
                orderIndex = index,
                createdAt = now,
            )
        }

        dao.saveDeckWithCards(deckEntity, cardEntities)
        registerCardsContext(cardEntities)

        val flashcards = cardEntities.map { it.toFlashcard() }

        FlashcardDeck(
            id = effectiveDeckId,
            title = cleanTitle,
            icon = cleanIcon,
            cards = flashcards,
            number = 0,
            category = "Custom",
            tier = 1,
        )
    }

    fun addCardsToDeck(deckId: String, newCards: List<CustomCardItem>): FlashcardDeck? = runBlocking(Dispatchers.IO) {
        val withCards = runCatching { dao.getDeckWithCards(deckId) }.getOrNull() ?: return@runBlocking null
        val existingCards = getDeckCardsDetails(deckId)
        val combined = existingCards + newCards
        saveDeck(
            deckId = deckId,
            title = withCards.deck.title,
            icon = CUSTOM_DECK_ICON,
            cards = combined,
        )
    }

    fun deleteDeck(deckId: String) = runBlocking(Dispatchers.IO) {
        val withCards = runCatching { dao.getDeckWithCards(deckId) }.getOrNull()
        if (withCards != null) {
            for (card in withCards.cards) {
                CardContextLoader.removeContext(card.id)
            }
        }
        dao.deleteDeckById(deckId)
    }

    private fun registerCardsContext(cards: List<CustomCardEntity>) {
        for (card in cards) {
            CardContextLoader.registerContext(
                cardId = card.id,
                context = card.toCardContext(),
            )
        }
    }
}
