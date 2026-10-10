package com.koto.app.feature.translator.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.koto.app.feature.cards.data.db.CardSrsDao
import com.koto.app.feature.cards.data.db.CardSrsEntity
import com.koto.app.feature.cards.data.db.CustomCardEntity
import com.koto.app.feature.cards.data.db.CustomDeckDao
import com.koto.app.feature.cards.data.db.CustomDeckEntity
import com.koto.app.feature.cards.data.db.DeckBookmarkDao
import com.koto.app.feature.cards.data.db.DeckBookmarkEntity
import com.koto.app.feature.cards.data.db.FsrsReviewLogDao
import com.koto.app.feature.cards.data.db.FsrsReviewLogEntity
import com.koto.app.feature.cards.data.db.StarredCardDao
import com.koto.app.feature.cards.data.db.StarredCardEntity

@Database(
    entities = [
        TranslationCardEntity::class,
        CustomDeckEntity::class,
        CustomCardEntity::class,
        DeckBookmarkEntity::class,
        StarredCardEntity::class,
        CardSrsEntity::class,
        FsrsReviewLogEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class KotoDatabase : RoomDatabase() {
    abstract fun translationCardDao(): TranslationCardDao
    abstract fun customDeckDao(): CustomDeckDao
    abstract fun deckBookmarkDao(): DeckBookmarkDao
    abstract fun starredCardDao(): StarredCardDao
    abstract fun cardSrsDao(): CardSrsDao
    abstract fun fsrsReviewLogDao(): FsrsReviewLogDao

    companion object {
        @Volatile
        private var instance: KotoDatabase? = null

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add FSRS columns to card_srs
                db.execSQL("ALTER TABLE card_srs ADD COLUMN due INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN stability REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN difficulty REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN elapsedDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN scheduledDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN reps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN state TEXT NOT NULL DEFAULT 'New'")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN lastReview INTEGER")
                db.execSQL("ALTER TABLE card_srs ADD COLUMN step INTEGER NOT NULL DEFAULT 0")

                // Create fsrs_review_logs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fsrs_review_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cardId` TEXT NOT NULL,
                        `rating` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `due` INTEGER NOT NULL,
                        `stability` REAL NOT NULL,
                        `difficulty` REAL NOT NULL,
                        `elapsedDays` INTEGER NOT NULL,
                        `scheduledDays` INTEGER NOT NULL,
                        `review` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fsrs_review_logs_cardId` ON `fsrs_review_logs` (`cardId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fsrs_review_logs_review` ON `fsrs_review_logs` (`review`)")

                // Migrate existing records from legacy epochDay to UTC milliseconds if any
                db.execSQL("UPDATE card_srs SET due = dueDateEpochDay * 86400000 WHERE dueDateEpochDay > 0 AND due = 0")
            }
        }

        fun getInstance(context: Context): KotoDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KotoDatabase::class.java,
                    "koto_local.db",
                )
                .addMigrations(MIGRATION_7_8)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
}
