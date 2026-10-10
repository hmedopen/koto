package com.koto.app.feature.cards.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StarredCardDao {

    @Query("SELECT cardId FROM starred_cards ORDER BY starredAt DESC")
    fun getAllStarredCardIdsFlow(): Flow<List<String>>

    @Query("SELECT cardId FROM starred_cards ORDER BY starredAt DESC")
    fun getAllStarredCardIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStarredCard(card: StarredCardEntity)

    @Query("DELETE FROM starred_cards WHERE cardId = :cardId")
    fun deleteStarredCard(cardId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM starred_cards WHERE cardId = :cardId)")
    fun isStarred(cardId: String): Boolean
}
