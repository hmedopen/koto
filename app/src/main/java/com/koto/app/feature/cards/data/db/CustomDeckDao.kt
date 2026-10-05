package com.koto.app.feature.cards.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class CustomDeckWithCards(
    @Embedded val deck: CustomDeckEntity,
    @Relation(parentColumn = "id", entityColumn = "deckId")
    val cards: List<CustomCardEntity>,
)

@Dao
interface CustomDeckDao {

    @Transaction
    @Query("SELECT * FROM custom_decks ORDER BY createdAt DESC")
    fun getAllDecksWithCardsFlow(): Flow<List<CustomDeckWithCards>>

    @Transaction
    @Query("SELECT * FROM custom_decks ORDER BY createdAt DESC")
    fun getAllDecksWithCards(): List<CustomDeckWithCards>

    @Transaction
    @Query("SELECT * FROM custom_decks WHERE id = :deckId LIMIT 1")
    fun getDeckWithCards(deckId: String): CustomDeckWithCards?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeck(deck: CustomDeckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCards(cards: List<CustomCardEntity>)

    @Query("DELETE FROM custom_cards WHERE deckId = :deckId")
    fun deleteCardsByDeckId(deckId: String)

    @Query("DELETE FROM custom_decks WHERE id = :deckId")
    fun deleteDeckById(deckId: String)

    @Transaction
    fun saveDeckWithCards(deck: CustomDeckEntity, cards: List<CustomCardEntity>) {
        insertDeck(deck)
        deleteCardsByDeckId(deck.id)
        if (cards.isNotEmpty()) {
            insertCards(cards)
        }
    }
}
