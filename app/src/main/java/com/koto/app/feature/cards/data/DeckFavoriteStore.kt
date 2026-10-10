package com.koto.app.feature.cards.data

import android.content.Context
import com.koto.app.feature.cards.data.db.DeckBookmarkEntity
import com.koto.app.feature.cards.data.db.StarredCardEntity
import com.koto.app.feature.translator.data.db.KotoDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    fun loadBookmarkedDeckIds(): Set<String> {
        return runCatching { bookmarkDao.getAllBookmarkedDeckIds().toSet() }.getOrElse { emptySet() }
    }

    fun loadStarredCardIds(): Set<String> {
        return runCatching { starredCardDao.getAllStarredCardIds().toSet() }.getOrElse { emptySet() }
    }

    fun isDeckBookmarked(deckId: String): Boolean {
        return runCatching { bookmarkDao.isBookmarked(deckId) }.getOrDefault(false)
    }

    fun isCardStarred(cardId: String): Boolean {
        return runCatching { starredCardDao.isStarred(cardId) }.getOrDefault(false)
    }

    fun toggleDeckBookmark(deckId: String): Boolean {
        if (deckId.isBlank()) return false
        val currentlyBookmarked = isDeckBookmarked(deckId)
        return if (currentlyBookmarked) {
            bookmarkDao.deleteBookmark(deckId)
            false
        } else {
            bookmarkDao.insertBookmark(DeckBookmarkEntity(deckId))
            true
        }
    }

    fun toggleCardStar(cardId: String, deckId: String? = null): Boolean {
        if (cardId.isBlank()) return false
        val currentlyStarred = isCardStarred(cardId)
        return if (currentlyStarred) {
            starredCardDao.deleteStarredCard(cardId)
            false
        } else {
            starredCardDao.insertStarredCard(StarredCardEntity(cardId, deckId))
            true
        }
    }
}
