package com.koto.app.feature.cards.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FsrsReviewLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: FsrsReviewLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(logs: List<FsrsReviewLogEntity>)

    @Query("SELECT * FROM fsrs_review_logs WHERE cardId = :cardId ORDER BY review DESC")
    fun getLogsForCard(cardId: String): List<FsrsReviewLogEntity>

    @Query("SELECT * FROM fsrs_review_logs ORDER BY review DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): List<FsrsReviewLogEntity>

    @Query("DELETE FROM fsrs_review_logs WHERE cardId = :cardId")
    fun deleteByCardId(cardId: String)

    @Query("DELETE FROM fsrs_review_logs")
    fun clearAll()
}
