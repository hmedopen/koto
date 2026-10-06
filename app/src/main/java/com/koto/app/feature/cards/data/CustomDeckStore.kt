package com.koto.app.feature.cards.data

import android.content.Context
import com.koto.app.feature.cards.data.db.CustomCardEntity
import com.koto.app.feature.cards.data.db.CustomDeckDao
import com.koto.app.feature.cards.data.db.CustomDeckEntity
import com.koto.app.feature.cards.data.db.CustomDeckWithCards
import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.feature.translator.data.db.KotoDatabase
import com.koto.app.ui.screens.cards.CardContext
import com.koto.app.ui.screens.cards.CardContextLoader
import com.koto.app.ui.screens.cards.CardExample
import com.koto.app.ui.screens.cards.Flashcard
import com.koto.app.ui.screens.cards.FlashcardDeck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CustomCardItem(
    val id: String = "",
    val japanese: String, // Strictly pure Kana or Kanji
    val romaji: String = "",
    val english: String,
    val furigana: String = "",
    val exampleKana: String = "",
    val exampleRomaji: String = "",
    val exampleEnglish: String = "",
    val notes: String = "",
)

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

    fun loadCustomDecks(): List<FlashcardDeck> {
        val list = runCatching { dao.getAllDecksWithCards() }.getOrElse { emptyList() }
        return list.map { withCards ->
            registerCardsContext(withCards.cards)
            withCards.toFlashcardDeck()
        }
    }

    fun getDeckCardsDetails(deckId: String): List<CustomCardItem> {
        val withCards = runCatching { dao.getDeckWithCards(deckId) }.getOrNull() ?: return emptyList()
        return withCards.cards.sortedBy { it.orderIndex }.map { entity ->
            CustomCardItem(
                id = entity.id,
                japanese = entity.japanese,
                romaji = entity.romaji,
                english = entity.english,
                furigana = entity.furigana,
                exampleKana = entity.exampleKana,
                exampleRomaji = entity.exampleRomaji,
                exampleEnglish = entity.exampleEnglish,
                notes = entity.notes,
            )
        }
    }

    fun saveDeck(
        deckId: String?,
        title: String,
        icon: String = CUSTOM_DECK_ICON,
        cards: List<CustomCardItem>,
    ): FlashcardDeck {
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
            val cardId = if (item.id.isNotBlank()) item.id else "${effectiveDeckId}_card_${index}_${now}"
            val cleanJapanese = item.japanese.trim()
            val romaji = if (item.romaji.isNotBlank()) {
                item.romaji.trim()
            } else {
                KanaConverter.toRomaji(KanaConverter.toPureKana(cleanJapanese))
            }
            val pureExampleKana = item.exampleKana.trim()
            val exampleRomaji = if (item.exampleRomaji.isNotBlank()) {
                item.exampleRomaji.trim()
            } else if (pureExampleKana.isNotBlank()) {
                KanaConverter.toRomaji(KanaConverter.toPureKana(pureExampleKana))
            } else {
                ""
            }

                CustomCardEntity(
                    id = cardId,
                    deckId = effectiveDeckId,
                    japanese = cleanJapanese,
                    romaji = romaji,
                    english = item.english.trim(),
                    furigana = item.furigana.trim(),
                    exampleKana = pureExampleKana,
                    exampleRomaji = exampleRomaji,
                    exampleEnglish = item.exampleEnglish.trim(),
                    notes = item.notes.trim(),
                    orderIndex = index,
                    createdAt = now,
                )
            }

            dao.saveDeckWithCards(deckEntity, cardEntities)
            registerCardsContext(cardEntities)

            val flashcards = cardEntities.map { entity ->
                Flashcard(
                    id = entity.id,
                    japanese = entity.japanese,
                    romaji = entity.romaji,
                    english = entity.english,
                    furigana = entity.furigana.takeIf { it.isNotBlank() },
                )
            }

        return FlashcardDeck(
            id = effectiveDeckId,
            title = cleanTitle,
            icon = cleanIcon,
            cards = flashcards,
            number = 0,
            category = "Custom",
            tier = 1,
        )
    }

    fun addCardsToDeck(deckId: String, newCards: List<CustomCardItem>): FlashcardDeck? {
        val withCards = runCatching { dao.getDeckWithCards(deckId) }.getOrNull() ?: return null
        val existingCards = getDeckCardsDetails(deckId)
        val combined = existingCards + newCards
        return saveDeck(
            deckId = deckId,
            title = withCards.deck.title,
            icon = CUSTOM_DECK_ICON,
            cards = combined,
        )
    }

    fun deleteDeck(deckId: String) {
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
            val examples = if (card.exampleKana.isNotBlank()) {
                listOf(
                    CardExample(
                        kana = card.exampleKana,
                        romaji = card.exampleRomaji,
                        english = card.exampleEnglish,
                    ),
                )
            } else {
                emptyList()
            }

            CardContextLoader.registerContext(
                cardId = card.id,
                context = CardContext(
                    cardId = card.id,
                    kana = card.japanese,
                    romaji = card.romaji,
                    english = card.english,
                    usageNote = card.notes,
                    examples = examples,
                ),
            )
        }
    }

    private fun CustomDeckWithCards.toFlashcardDeck(): FlashcardDeck {
        val sortedCards = cards.sortedBy { it.orderIndex }
        return FlashcardDeck(
            id = deck.id,
            title = deck.title,
            icon = CUSTOM_DECK_ICON,
            cards = sortedCards.map { entity ->
                Flashcard(
                    id = entity.id,
                    japanese = entity.japanese,
                    romaji = entity.romaji,
                    english = entity.english,
                    furigana = entity.furigana.takeIf { it.isNotBlank() },
                )
            },
            number = deck.number,
            category = deck.category,
            tier = 1,
        )
    }
}
