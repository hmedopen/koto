package com.koto.app.feature.cards.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckBookmarkDao {

    @Query("SELECT deckId FROM deck_bookmarks ORDER BY bookmarkedAt DESC")
    fun getAllBookmarkedDeckIdsFlow(): Flow<List<String>>

    @Query("SELECT deckId FROM deck_bookmarks ORDER BY bookmarkedAt DESC")
    fun getAllBookmarkedDeckIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBookmark(bookmark: DeckBookmarkEntity)

    @Query("DELETE FROM deck_bookmarks WHERE deckId = :deckId")
    fun deleteBookmark(deckId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM deck_bookmarks WHERE deckId = :deckId)")
    fun isBookmarked(deckId: String): Boolean
}
