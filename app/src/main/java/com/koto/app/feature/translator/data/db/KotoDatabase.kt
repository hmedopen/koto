package com.koto.app.feature.translator.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.koto.app.feature.cards.data.db.CustomCardEntity
import com.koto.app.feature.cards.data.db.CustomDeckDao
import com.koto.app.feature.cards.data.db.CustomDeckEntity

@Database(
    entities = [
        TranslationCardEntity::class,
        CustomDeckEntity::class,
        CustomCardEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class KotoDatabase : RoomDatabase() {
    abstract fun translationCardDao(): TranslationCardDao
    abstract fun customDeckDao(): CustomDeckDao

    companion object {
        @Volatile
        private var instance: KotoDatabase? = null

        fun getInstance(context: Context): KotoDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KotoDatabase::class.java,
                    "koto_local.db",
                )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
}
