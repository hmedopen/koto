package com.koto.app.feature.translator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.feature.translator.data.TranslatorCardStore
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.ui.screens.cards.CardContextLoader
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TranslatorCardStoreTest {

    private lateinit var store: TranslatorCardStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = TranslatorCardStore(context)
        store.clearAll()
    }

    @Test
    fun testStarringPreservesKanjiInSavedCard() {
        val starred = store.toggleStar(
            sourceText = "teacher",
            targetText = "先生", // Contains kanji
            targetRomaji = "sensei",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )
        assertTrue(starred)

        val cards = store.loadStarredCards()
        assertEquals(1, cards.size)
        val card = cards.first()

        assertEquals("teacher", card.sourceText)
        assertEquals("先生", card.targetText)
        assertEquals("sensei", card.targetRomaji)
    }

    @Test
    fun testStarringSentencePopulatesCardContextExample() {
        val sentenceEn = "I eat bread every day."
        val sentenceJp = "私は毎日パンを食べます。"

        val starred = store.toggleStar(
            sourceText = sentenceEn,
            targetText = sentenceJp,
            targetRomaji = "watashi wa mainichi pan o tabemasu.",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )
        assertTrue(starred)

        val cards = store.loadStarredCards()
        assertEquals(1, cards.size)
        val card = cards.first()

        // CardContextLoader should have registered context
        val context = CardContextLoader.getContext(card.id, card.targetText, card.targetRomaji)
        assertNotNull(context)
        assertEquals("私は毎日パンを食べます。", context?.kana)
        assertEquals(1, context?.examples?.size)
        assertEquals("I eat bread every day.", context?.examples?.first()?.english)
    }

    @Test
    fun testBidirectionalStarStatusChecking() {
        store.toggleStar(
            sourceText = "water",
            targetText = "水",
            targetRomaji = "mizu",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )

        // Japanese -> English check
        assertTrue(store.isStarred("水", "water"))
        // English -> Japanese check
        assertTrue(store.isStarred("water", "水"))
        // Pure kana check
        assertTrue(store.isStarred("water", "みず"))
        assertTrue(store.isStarred("みず", "water"))

        // Unrelated query
        assertFalse(store.isStarred("cat", "neko"))
    }

    @Test
    fun testToggleStarRemovesCardWhenAlreadyStarred() {
        store.toggleStar(
            sourceText = "book",
            targetText = "本",
            targetRomaji = "hon",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )
        assertEquals(1, store.loadStarredCards().size)

        // Toggle again to remove
        val starred = store.toggleStar(
            sourceText = "book",
            targetText = "本",
            targetRomaji = "hon",
            sourceLang = TranslationLanguage.English,
            targetLang = TranslationLanguage.Japanese,
        )
        assertFalse(starred)
        assertEquals(0, store.loadStarredCards().size)
    }
}
