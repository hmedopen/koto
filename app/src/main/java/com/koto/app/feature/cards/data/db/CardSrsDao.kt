package com.koto.app.feature.cards.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardSrsDao {
    @Query("SELECT * FROM card_srs")
    fun getAll(): List<CardSrsEntity>

    @Query("SELECT * FROM card_srs")
    fun getAllFlow(): Flow<List<CardSrsEntity>>

    @Query("SELECT * FROM card_srs WHERE deckId = :deckId")
    fun getByDeckId(deckId: String): List<CardSrsEntity>

    @Query("SELECT * FROM card_srs WHERE cardId = :cardId")
    fun getByCardId(cardId: String): CardSrsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(entity: CardSrsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(entities: List<CardSrsEntity>)

    @Query("DELETE FROM card_srs WHERE cardId = :cardId")
    fun deleteByCardId(cardId: String)

    @Query("DELETE FROM card_srs WHERE deckId = :deckId")
    fun deleteByDeckId(deckId: String)

    @Query("DELETE FROM card_srs")
    fun clearAll()
}
