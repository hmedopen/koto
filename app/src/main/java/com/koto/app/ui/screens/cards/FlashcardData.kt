package com.koto.app.ui.screens.cards

import android.content.Context
import org.json.JSONArray

data class Flashcard(
    val id: String,
    val japanese: String,
    val romaji: String,
    val english: String,
    val dueTimestamp: Long = 0L,
    val intervalDays: Int = 0,
    val state: String = "NEW",
    val kanji: String? = null,
    val furigana: String? = null,
) {
    val displayKanji: String get() = kanji?.takeIf { it.isNotBlank() } ?: japanese
}

data class FlashcardDeck(
    val id: String,
    val title: String,
    val icon: String,
    val cards: List<Flashcard>,
    val number: Int = 0,
    val category: String = "",
    val tier: Int = 1,
)

/** The test fixture is kept verbatim in assets, independent of lesson content. */
fun loadFlashcardDecks(jsonString: String): List<FlashcardDeck> {
    val parsed = runCatching {
        val json = JSONArray(jsonString)
        List(json.length()) { index ->
            val deck = json.getJSONObject(index)
            val cards = deck.getJSONArray("cards")
            FlashcardDeck(
                id = deck.getString("id"),
                title = deck.getString("title"),
                icon = deck.optString("icon", "chatbubble"),
                cards = List(cards.length()) { cardIndex ->
                    val card = cards.getJSONObject(cardIndex)
                    Flashcard(
                        id = card.getString("id"),
                        japanese = card.getString("japanese"),
                        romaji = card.getString("romaji"),
                        english = card.getString("english"),
                        dueTimestamp = card.optLong("dueTimestamp", 0L),
                        intervalDays = card.optInt("intervalDays", 0),
                        state = card.optString("state", "NEW"),
                        kanji = card.optString("kanji", "").takeIf { it.isNotBlank() },
                        furigana = card.optString("furigana", "").takeIf { it.isNotBlank() },
                    )
                },
                number = deck.optInt("number", index + 1),
                category = deck.optString("category", ""),
                tier = deck.optInt("tier", 1),
            )
        }
    }.getOrNull()

    if (parsed != null && parsed.isNotEmpty()) {
        return parsed
    }
    return loadFlashcardDecksFallback(jsonString)
}

internal fun loadFlashcardDecksFallback(jsonString: String): List<FlashcardDeck> {
    val decks = mutableListOf<FlashcardDeck>()
    var currentDeckId = ""
    var currentTitle = ""
    var currentIcon = "chatbubble"
    var currentNumber = 0
    var currentCategory = ""
    var currentTier = 1
    var currentCards = mutableListOf<Flashcard>()
    var inCards = false

    var currentCardId = ""
    var currentJapanese = ""
    var currentRomaji = ""
    var currentEnglish = ""
    var currentDue = 0L
    var currentInterval = 0
    var currentState = "NEW"
    var currentKanji: String? = null
    var currentFurigana: String? = null

    fun unescape(str: String): String = str.replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\")

    for (line in jsonString.lineSequence()) {
        val trimmed = line.trim().removeSuffix(",")
        if (trimmed.startsWith("\"id\":")) {
            val value = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
            if (!inCards) {
                currentDeckId = value
            } else {
                currentCardId = value
            }
        } else if (trimmed.startsWith("\"title\":")) {
            currentTitle = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
        } else if (trimmed.startsWith("\"number\":")) {
            currentNumber = trimmed.substringAfter(":").trim().toIntOrNull() ?: 0
        } else if (trimmed.startsWith("\"category\":")) {
            currentCategory = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
        } else if (trimmed.startsWith("\"tier\":")) {
            currentTier = trimmed.substringAfter(":").trim().toIntOrNull() ?: 1
        } else if (trimmed.startsWith("\"icon\":")) {
            currentIcon = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
        } else if (trimmed.startsWith("\"cards\":")) {
            inCards = true
            currentCards = mutableListOf()
        } else if (trimmed.startsWith("\"japanese\":")) {
            currentJapanese = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
        } else if (trimmed.startsWith("\"kanji\":")) {
            currentKanji = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\"")).takeIf { it.isNotBlank() }
        } else if (trimmed.startsWith("\"furigana\":")) {
            currentFurigana = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\"")).takeIf { it.isNotBlank() }
        } else if (trimmed.startsWith("\"romaji\":")) {
            currentRomaji = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
        } else if (trimmed.startsWith("\"english\":")) {
            currentEnglish = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
        } else if (trimmed.startsWith("\"dueTimestamp\":")) {
            currentDue = trimmed.substringAfter(":").trim().toLongOrNull() ?: 0L
        } else if (trimmed.startsWith("\"intervalDays\":")) {
            currentInterval = trimmed.substringAfter(":").trim().toIntOrNull() ?: 0
        } else if (trimmed.startsWith("\"state\":")) {
            currentState = unescape(trimmed.substringAfter(":").trim().removeSurrounding("\""))
        } else if (trimmed == "}" && inCards && currentCardId.isNotEmpty()) {
            currentCards.add(
                Flashcard(
                    id = currentCardId,
                    japanese = currentJapanese,
                    romaji = currentRomaji,
                    english = currentEnglish,
                    dueTimestamp = currentDue,
                    intervalDays = currentInterval,
                    state = currentState,
                    kanji = currentKanji,
                    furigana = currentFurigana,
                ),
            )
            currentCardId = ""
            currentJapanese = ""
            currentRomaji = ""
            currentEnglish = ""
            currentDue = 0L
            currentInterval = 0
            currentState = "NEW"
            currentKanji = null
            currentFurigana = null
        } else if (trimmed == "]") {
            inCards = false
        } else if (trimmed == "}" && !inCards && currentDeckId.isNotEmpty()) {
            decks.add(FlashcardDeck(currentDeckId, currentTitle, currentIcon, currentCards, currentNumber, currentCategory, currentTier))
            currentDeckId = ""
            currentTitle = ""
            currentCards = mutableListOf()
        }
    }
    return decks
}

fun loadFlashcardDecks(context: Context): List<FlashcardDeck> {
    val jsonString = context.assets.open("flashcard_decks.json").bufferedReader().use { it.readText() }
    return loadFlashcardDecks(jsonString)
}

val FlashcardDeck.description: String get() = when {
    category == "Custom" || id.startsWith("custom_") || icon == "custom" -> "Personal custom flashcard deck."
    icon == "chatbubble" -> "Small words to start a conversation."
    icon == "hashtag" -> "Build confidence with everyday numbers."
    icon == "home" -> "Get to know the things around you."
    icon == "utensils" -> "Everyday words for the table."
    icon == "train" -> "Find your way, one word at a time."
    else -> "Useful actions for everyday life."
}
