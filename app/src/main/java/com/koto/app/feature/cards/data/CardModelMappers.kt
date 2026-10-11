package com.koto.app.feature.cards.data

import com.koto.app.feature.cards.data.db.CustomCardEntity
import com.koto.app.feature.cards.data.db.CustomDeckWithCards
import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.ui.screens.cards.CardContext
import com.koto.app.ui.screens.cards.CardExample
import com.koto.app.ui.screens.cards.Flashcard
import com.koto.app.ui.screens.cards.FlashcardDeck

/**
 * Bidirectional mapping extension functions unifying domain models
 * across CustomCardEntity (Room), CustomCardItem (UI Draft), and Flashcard (Study UI).
 */

fun CustomCardEntity.toCustomCardItem(): CustomCardItem = CustomCardItem(
    id = id,
    japanese = japanese,
    romaji = romaji,
    english = english,
    furigana = furigana,
    exampleKana = exampleKana,
    exampleRomaji = exampleRomaji,
    exampleEnglish = exampleEnglish,
    example2Kana = example2Kana,
    example2Romaji = example2Romaji,
    example2English = example2English,
    example3Kana = example3Kana,
    example3Romaji = example3Romaji,
    example3English = example3English,
    notes = notes,
    kanji = kanji,
)

fun CustomCardItem.toCustomCardEntity(
    deckId: String,
    orderIndex: Int = 0,
    createdAt: Long = System.currentTimeMillis(),
): CustomCardEntity {
    val cardId = if (id.isNotBlank()) id else "${deckId}_card_${orderIndex}_$createdAt"
    val cleanJapanese = japanese.trim()
    val pureKana = KanaConverter.toPureKana(cleanJapanese)
    val resolvedKanji = if (kanji.isNotBlank()) {
        kanji.trim()
    } else if (KanaConverter.containsKanji(cleanJapanese)) {
        cleanJapanese
    } else {
        ""
    }
    val resolvedRomaji = if (romaji.isNotBlank()) {
        romaji.trim()
    } else {
        KanaConverter.toRomaji(pureKana)
    }
    val pureEx1Kana = exampleKana.trim()
    val ex1Romaji = if (exampleRomaji.isNotBlank()) {
        exampleRomaji.trim()
    } else if (pureEx1Kana.isNotBlank()) {
        KanaConverter.toRomaji(KanaConverter.toPureKana(pureEx1Kana))
    } else {
        ""
    }
    val pureEx2Kana = example2Kana.trim()
    val ex2Romaji = if (example2Romaji.isNotBlank()) {
        example2Romaji.trim()
    } else if (pureEx2Kana.isNotBlank()) {
        KanaConverter.toRomaji(KanaConverter.toPureKana(pureEx2Kana))
    } else {
        ""
    }
    val pureEx3Kana = example3Kana.trim()
    val ex3Romaji = if (example3Romaji.isNotBlank()) {
        example3Romaji.trim()
    } else if (pureEx3Kana.isNotBlank()) {
        KanaConverter.toRomaji(KanaConverter.toPureKana(pureEx3Kana))
    } else {
        ""
    }

    return CustomCardEntity(
        id = cardId,
        deckId = deckId,
        japanese = cleanJapanese,
        romaji = resolvedRomaji,
        english = english.trim(),
        furigana = furigana.trim(),
        exampleKana = pureEx1Kana,
        exampleRomaji = ex1Romaji,
        exampleEnglish = exampleEnglish.trim(),
        example2Kana = pureEx2Kana,
        example2Romaji = ex2Romaji,
        example2English = example2English.trim(),
        example3Kana = pureEx3Kana,
        example3Romaji = ex3Romaji,
        example3English = example3English.trim(),
        notes = notes.trim(),
        orderIndex = orderIndex,
        createdAt = createdAt,
        kanji = resolvedKanji,
    )
}

fun CustomCardEntity.toFlashcard(): Flashcard = Flashcard(
    id = id,
    japanese = if (japanese.isNotBlank()) japanese else kanji,
    romaji = romaji,
    english = english,
    kanji = kanji.takeIf { it.isNotBlank() } ?: japanese.takeIf { KanaConverter.containsKanji(it) },
    furigana = furigana.takeIf { it.isNotBlank() },
)

fun Flashcard.toCustomCardItem(): CustomCardItem = CustomCardItem(
    id = id,
    japanese = japanese,
    romaji = romaji,
    english = english,
    furigana = furigana ?: "",
    kanji = kanji ?: "",
)

fun Flashcard.toCustomCardEntity(
    deckId: String,
    orderIndex: Int = 0,
    createdAt: Long = System.currentTimeMillis(),
): CustomCardEntity = toCustomCardItem().toCustomCardEntity(deckId, orderIndex, createdAt)

fun CustomCardItem.toFlashcard(): Flashcard = Flashcard(
    id = id,
    japanese = if (japanese.isNotBlank()) japanese else kanji,
    romaji = romaji,
    english = english,
    kanji = kanji.takeIf { it.isNotBlank() } ?: japanese.takeIf { KanaConverter.containsKanji(it) },
    furigana = furigana.takeIf { it.isNotBlank() },
)

fun CustomCardEntity.toCardContext(): CardContext {
    val examples = mutableListOf<CardExample>()
    if (exampleKana.isNotBlank() || exampleEnglish.isNotBlank()) {
        examples.add(
            CardExample(
                kana = exampleKana,
                romaji = exampleRomaji,
                english = exampleEnglish,
            )
        )
    }
    if (example2Kana.isNotBlank() || example2English.isNotBlank()) {
        examples.add(
            CardExample(
                kana = example2Kana,
                romaji = example2Romaji,
                english = example2English,
            )
        )
    }
    if (example3Kana.isNotBlank() || example3English.isNotBlank()) {
        examples.add(
            CardExample(
                kana = example3Kana,
                romaji = example3Romaji,
                english = example3English,
            )
        )
    }

    return CardContext(
        cardId = id,
        kana = japanese,
        romaji = romaji,
        english = english,
        usageNote = notes,
        examples = examples,
    )
}

fun CustomDeckWithCards.toFlashcardDeck(): FlashcardDeck {
    val sortedCards = cards.sortedBy { it.orderIndex }
    return FlashcardDeck(
        id = deck.id,
        title = deck.title,
        icon = CustomDeckStore.CUSTOM_DECK_ICON,
        cards = sortedCards.map { it.toFlashcard() },
        number = deck.number,
        category = deck.category,
        tier = 1,
    )
}
