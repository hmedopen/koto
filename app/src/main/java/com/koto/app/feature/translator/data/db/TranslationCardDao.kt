package com.koto.app.feature.translator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationCardDao {
    @Query("SELECT * FROM translation_cards ORDER BY timestamp DESC")
    fun getAllCardsFlow(): Flow<List<TranslationCardEntity>>

    @Query("SELECT * FROM translation_cards ORDER BY timestamp DESC")
    fun getAllCards(): List<TranslationCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCard(card: TranslationCardEntity): Long

    @Query("DELETE FROM translation_cards WHERE id = :id")
    fun deleteById(id: String): Int

    @Query("DELETE FROM translation_cards WHERE (TRIM(japanese) = TRIM(:japanese) AND TRIM(english) = TRIM(:english)) OR (TRIM(japanese) = TRIM(:english) AND TRIM(english) = TRIM(:japanese))")
    fun deleteByPair(japanese: String, english: String): Int

    @Query("DELETE FROM translation_cards")
    fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM translation_cards WHERE (TRIM(japanese) = TRIM(:japanese) AND TRIM(english) = TRIM(:english)) OR (TRIM(japanese) = TRIM(:english) AND TRIM(english) = TRIM(:japanese)))")
    fun isStarred(japanese: String, english: String): Boolean
}
