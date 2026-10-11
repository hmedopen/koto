package com.koto.app.feature.cards

import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.feature.cards.data.db.CustomCardEntity
import com.koto.app.feature.cards.data.db.CustomDeckEntity
import com.koto.app.feature.cards.data.db.CustomDeckWithCards
import com.koto.app.feature.cards.data.toCardContext
import com.koto.app.feature.cards.data.toCustomCardEntity
import com.koto.app.feature.cards.data.toCustomCardItem
import com.koto.app.feature.cards.data.toFlashcard
import com.koto.app.feature.cards.data.toFlashcardDeck
import com.koto.app.ui.screens.cards.Flashcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CardModelMappersTest {

    @Test
    fun testBidirectionalMappingEntityAndItem() {
        val originalEntity = CustomCardEntity(
            id = "card_101",
            deckId = "deck_01",
            japanese = "たべる",
            romaji = "taberu",
            english = "to eat",
            furigana = "たべる",
            exampleKana = "りんごを たべる",
            exampleRomaji = "ringo o taberu",
            exampleEnglish = "eat an apple",
            example2Kana = "ごはんを たべる",
            example2Romaji = "gohan o taberu",
            example2English = "eat rice",
            example3Kana = "パンを たべる",
            example3Romaji = "pan o taberu",
            example3English = "eat bread",
            notes = "Ichidan verb",
            orderIndex = 2,
            createdAt = 123456789L,
            kanji = "食べる",
        )

        val item = originalEntity.toCustomCardItem()

        assertEquals("card_101", item.id)
        assertEquals("たべる", item.japanese)
        assertEquals("taberu", item.romaji)
        assertEquals("to eat", item.english)
        assertEquals("たべる", item.furigana)
        assertEquals("食べる", item.kanji)
        assertEquals("りんごを たべる", item.exampleKana)
        assertEquals("ringo o taberu", item.exampleRomaji)
        assertEquals("eat an apple", item.exampleEnglish)
        assertEquals("ごはんを たべる", item.example2Kana)
        assertEquals("gohan o taberu", item.example2Romaji)
        assertEquals("eat rice", item.example2English)
        assertEquals("パンを たべる", item.example3Kana)
        assertEquals("pan o taberu", item.example3Romaji)
        assertEquals("eat bread", item.example3English)
        assertEquals("Ichidan verb", item.notes)

        // Map back to entity
        val reconstructedEntity = item.toCustomCardEntity(
            deckId = "deck_01",
            orderIndex = 2,
            createdAt = 123456789L,
        )

        assertEquals(originalEntity, reconstructedEntity)
    }

    @Test
    fun testEntityToFlashcardMapping() {
        val entity = CustomCardEntity(
            id = "c_1",
            deckId = "d_1",
            japanese = "みず",
            romaji = "mizu",
            english = "water",
            furigana = "みず",
            kanji = "水",
        )

        val flashcard = entity.toFlashcard()

        assertEquals("c_1", flashcard.id)
        assertEquals("みず", flashcard.japanese)
        assertEquals("mizu", flashcard.romaji)
        assertEquals("water", flashcard.english)
        assertEquals("水", flashcard.kanji)
        assertEquals("みず", flashcard.furigana)
        assertEquals("水", flashcard.displayKanji)
    }

    @Test
    fun testFlashcardToCustomCardItemAndEntity() {
        val flashcard = Flashcard(
            id = "fc_01",
            japanese = "ねこ",
            romaji = "neko",
            english = "cat",
            kanji = "猫",
            furigana = "ねこ",
        )

        val item = flashcard.toCustomCardItem()
        assertEquals("fc_01", item.id)
        assertEquals("ねこ", item.japanese)
        assertEquals("neko", item.romaji)
        assertEquals("cat", item.english)
        assertEquals("猫", item.kanji)
        assertEquals("ねこ", item.furigana)

        val entity = flashcard.toCustomCardEntity(deckId = "deck_animals", orderIndex = 0, createdAt = 1000L)
        assertEquals("fc_01", entity.id)
        assertEquals("deck_animals", entity.deckId)
        assertEquals("ねこ", entity.japanese)
        assertEquals("neko", entity.romaji)
        assertEquals("cat", entity.english)
        assertEquals("猫", entity.kanji)
    }

    @Test
    fun testCustomCardEntityToCardContext() {
        val entity = CustomCardEntity(
            id = "ctx_01",
            deckId = "d_ctx",
            japanese = "いく",
            romaji = "iku",
            english = "to go",
            exampleKana = "がっこうへ いく",
            exampleRomaji = "gakkou e iku",
            exampleEnglish = "go to school",
            notes = "Movement verb",
            kanji = "行く",
        )

        val context = entity.toCardContext()
        assertEquals("ctx_01", context.cardId)
        assertEquals("いく", context.kana)
        assertEquals("iku", context.romaji)
        assertEquals("to go", context.english)
        assertEquals("Movement verb", context.usageNote)
        assertEquals(1, context.examples.size)
        assertEquals("がっこうへ いく", context.examples[0].kana)
        assertEquals("gakkou e iku", context.examples[0].romaji)
        assertEquals("go to school", context.examples[0].english)
    }

    @Test
    fun testCustomDeckWithCardsToFlashcardDeck() {
        val deckEntity = CustomDeckEntity(
            id = "custom_deck_1",
            title = "My Custom Deck",
            icon = "custom",
            category = "Custom",
            number = 1,
        )
        val card1 = CustomCardEntity(
            id = "card_1",
            deckId = "custom_deck_1",
            japanese = "いぬ",
            romaji = "inu",
            english = "dog",
            orderIndex = 1,
            kanji = "犬",
        )
        val card0 = CustomCardEntity(
            id = "card_0",
            deckId = "custom_deck_1",
            japanese = "ねこ",
            romaji = "neko",
            english = "cat",
            orderIndex = 0,
            kanji = "猫",
        )

        val withCards = CustomDeckWithCards(
            deck = deckEntity,
            cards = listOf(card1, card0), // out of order
        )

        val flashcardDeck = withCards.toFlashcardDeck()
        assertEquals("custom_deck_1", flashcardDeck.id)
        assertEquals("My Custom Deck", flashcardDeck.title)
        assertEquals("custom", flashcardDeck.icon)
        assertEquals(2, flashcardDeck.cards.size)
        // verify sorted by orderIndex
        assertEquals("card_0", flashcardDeck.cards[0].id)
        assertEquals("card_1", flashcardDeck.cards[1].id)
        assertEquals("猫", flashcardDeck.cards[0].kanji)
        assertEquals("犬", flashcardDeck.cards[1].kanji)
    }
}
