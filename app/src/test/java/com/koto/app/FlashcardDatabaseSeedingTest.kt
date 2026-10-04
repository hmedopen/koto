package com.koto.app

import com.koto.app.ui.screens.cards.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FlashcardDatabaseSeedingTest {

    private fun loadDecksAsset(): List<FlashcardDeck> {
        val rootDir = File(".").canonicalFile
        val candidates = listOf(
            File(rootDir, "src/main/assets/flashcard_decks.json"),
            File(rootDir, "app/src/main/assets/flashcard_decks.json"),
            File("../app/src/main/assets/flashcard_decks.json"),
            File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/flashcard_decks.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("flashcard_decks.json must exist in assets", file)
        val json = file!!.readText(Charsets.UTF_8)
        return loadFlashcardDecks(json)
    }

    @Test
    fun verify50DecksAnd1257CardsIntegrity() {
        val decks = loadDecksAsset()

        // 1. Deck count verification
        assertEquals("Exact 50 decks must be imported", 50, decks.size)

        // 2. Card count verification
        val totalCards = decks.sumOf { it.cards.size }
        assertEquals("Exact 1257 cards must be present across all 50 decks", 1257, totalCards)

        val uniqueCardIds = decks.flatMap { it.cards }.map { it.id }.toSet()
        assertEquals("All 1257 card IDs must be strictly unique", 1257, uniqueCardIds.size)

        // 3. Deck metadata and Tier structure verification
        decks.forEachIndexed { index, deck ->
            val expectedNumber = index + 1
            val expectedId = "deck_%02d".format(expectedNumber)
            assertEquals("Deck ID must be correctly formatted", expectedId, deck.id)
            assertEquals("Deck number must match index", expectedNumber, deck.number)
            assertTrue("Deck title must be non-empty", deck.title.isNotEmpty())
            assertTrue("Deck icon must be non-empty", deck.icon.isNotEmpty())

            val expectedTier = when {
                expectedNumber <= 16 -> 1
                expectedNumber <= 34 -> 2
                else -> 3
            }
            assertEquals("Deck $expectedNumber tier must match blueprint", expectedTier, deck.tier)

            val expectedCategory = when (expectedTier) {
                1 -> "Survival, Anchor Vocabulary & Core Mechanics"
                2 -> "Real-World Living, Navigation & Daily Tasks"
                else -> "Complex Expression, Society, Nuance & Culture"
            }
            assertEquals("Deck $expectedNumber category must match tier", expectedCategory, deck.category)

            // Verify individual cards in deck
            deck.cards.forEachIndexed { cardIdx, card ->
                val expectedCardNum = cardIdx + 1
                val expectedCardId = "card_%02d_%03d".format(expectedNumber, expectedCardNum)
                assertEquals(expectedCardId, card.id)
                assertTrue("Kana must be populated for ${card.id}", card.japanese.isNotEmpty())
                assertTrue("Romaji must be populated for ${card.id}", card.romaji.isNotEmpty())
                assertTrue("English must be populated for ${card.id}", card.english.isNotEmpty())

                // Default initial state
                assertEquals("dueTimestamp must default to ready (0L)", 0L, card.dueTimestamp)
                assertEquals("intervalDays must default to 0", 0, card.intervalDays)
                assertEquals("state must default to NEW", "NEW", card.state)
            }
        }
    }

    @Test
    fun verifyCardContextsAndExamplesCoverage() {
        val decks = loadDecksAsset()

        decks.forEach { deck ->
            deck.cards.forEach { card ->
                val context = CardContextLoader.getContext(card.japanese, card.romaji)
                assertNotNull("Context must exist for '${card.japanese}' (${card.romaji}) in ${deck.id}", context)
                assertEquals("Context kana must match card kana", card.japanese.trim(), context?.kana?.trim())
                assertEquals(
                    "Context must provide exactly 3 example sentences for '${card.japanese}'",
                    3,
                    context?.examples?.size,
                )
                context?.examples?.forEachIndexed { exIdx, ex ->
                    assertTrue("Example $exIdx kana must not be blank in ${card.id}", ex.kana.isNotBlank())
                    assertTrue("Example $exIdx romaji must not be blank in ${card.id}", ex.romaji.isNotBlank())
                    assertTrue("Example $exIdx english must not be blank in ${card.id}", ex.english.isNotBlank())
                }
            }
        }
    }

    @Test
    fun verifyFreshImportCardStatesAreDueAndNotWeakOrMastered() {
        val decks = loadDecksAsset()
        val emptyState = FlashcardState()

        decks.forEach { deck ->
            val counts = emptyState.counts(deck)
            assertEquals("Deck ${deck.id} should have 0 weak cards initially", 0, counts.weak)
            assertEquals("Deck ${deck.id} should have 0 mastered cards initially", 0, counts.mastered)
            assertEquals("Deck ${deck.id} should have all cards due initially", deck.cards.size, counts.due)
            assertEquals("counts.new should equal counts.due", counts.due, counts.new)
        }
    }
}
