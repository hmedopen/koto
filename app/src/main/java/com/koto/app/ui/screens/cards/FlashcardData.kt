package com.koto.app.ui.screens.cards

import android.content.Context
import org.json.JSONArray

data class Flashcard(val id: String, val japanese: String, val romaji: String, val english: String)
data class FlashcardDeck(val id: String, val title: String, val icon: String, val cards: List<Flashcard>)

/** The test fixture is kept verbatim in assets, independent of lesson content. */
fun loadFlashcardDecks(context: Context): List<FlashcardDeck> {
    val json = context.assets.open("flashcard_decks.json").bufferedReader().use { JSONArray(it.readText()) }
    return List(json.length()) { index ->
        val deck = json.getJSONObject(index)
        val cards = deck.getJSONArray("cards")
        FlashcardDeck(deck.getString("id"), deck.getString("title"), deck.getString("icon"),
            List(cards.length()) { cardIndex ->
                val card = cards.getJSONObject(cardIndex)
                Flashcard(card.getString("id"), card.getString("japanese"), card.getString("romaji"), card.getString("english"))
            })
    }
}

val FlashcardDeck.description: String get() = when (icon) {
    "chatbubble" -> "Small words to start a conversation."
    "hashtag" -> "Build confidence with everyday numbers."
    "home" -> "Get to know the things around you."
    "utensils" -> "Everyday words for the table."
    "train" -> "Find your way, one word at a time."
    else -> "Useful actions for everyday life."
}
