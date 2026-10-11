package com.koto.app.feature.cards.data

import android.content.Context
import com.koto.app.feature.cards.data.db.DeckBookmarkEntity
import com.koto.app.feature.cards.data.db.StarredCardEntity
import com.koto.app.feature.translator.data.db.KotoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Local Room-backed persistent store for:
 * 1. Bookmarked / Hearted decks (pinned status across app restarts)
 * 2. Starred cards in decks (matching the persistence pattern of Quick Translations)
 */
class DeckFavoriteStore(context: Context) {
    private val db = KotoDatabase.getInstance(context.applicationContext)
    private val bookmarkDao = db.deckBookmarkDao()
    private val starredCardDao = db.starredCardDao()

    val bookmarkedDeckIdsFlow: Flow<Set<String>> = bookmarkDao.getAllBookmarkedDeckIdsFlow().map { it.toSet() }
    val starredCardIdsFlow: Flow<Set<String>> = starredCardDao.getAllStarredCardIdsFlow().map { it.toSet() }

    fun loadBookmarkedDeckIds(): Set<String> = runBlocking(Dispatchers.IO) {
        runCatching { bookmarkDao.getAllBookmarkedDeckIds().toSet() }.getOrElse { emptySet() }
    }

    fun loadStarredCardIds(): Set<String> = runBlocking(Dispatchers.IO) {
        runCatching { starredCardDao.getAllStarredCardIds().toSet() }.getOrElse { emptySet() }
    }

    fun isDeckBookmarked(deckId: String): Boolean = runBlocking(Dispatchers.IO) {
        runCatching { bookmarkDao.isBookmarked(deckId) }.getOrDefault(false)
    }

    fun isCardStarred(cardId: String): Boolean = runBlocking(Dispatchers.IO) {
        runCatching { starredCardDao.isStarred(cardId) }.getOrDefault(false)
    }

    fun toggleDeckBookmark(deckId: String): Boolean = runBlocking(Dispatchers.IO) {
        if (deckId.isBlank()) return@runBlocking false
        val currentlyBookmarked = runCatching { bookmarkDao.isBookmarked(deckId) }.getOrDefault(false)
        if (currentlyBookmarked) {
            bookmarkDao.deleteBookmark(deckId)
            false
        } else {
            bookmarkDao.insertBookmark(DeckBookmarkEntity(deckId))
            true
        }
    }

    fun toggleCardStar(cardId: String, deckId: String? = null): Boolean = runBlocking(Dispatchers.IO) {
        if (cardId.isBlank()) return@runBlocking false
        val currentlyStarred = runCatching { starredCardDao.isStarred(cardId) }.getOrDefault(false)
        if (currentlyStarred) {
            starredCardDao.deleteStarredCard(cardId)
            false
        } else {
            starredCardDao.insertStarredCard(StarredCardEntity(cardId, deckId))
            true
        }
    }
}
