package com.koto.app

import com.koto.app.ui.screens.cards.CardContextLoader
import org.junit.Assert.*
import org.junit.Test

class CardContextLoaderTest {
    @Test
    fun loadCardContextAndExamples() {
        val context1 = CardContextLoader.getContext("おはよう", "ohayou")
        assertNotNull("Context for おはよう should be found", context1)
        assertEquals("おはよう", context1?.kana)
        assertEquals("ohayou", context1?.romaji)
        assertTrue(context1?.usageNote?.isNotEmpty() == true)
        assertEquals(3, context1?.examples?.size)

        val context2 = CardContextLoader.getContext("こんにちは", "konnichiwa")
        assertNotNull("Context for こんにちは should be found", context2)
        assertEquals(3, context2?.examples?.size)

        val contextUnknown = CardContextLoader.getContext("unknown_word_xyz", "romaji")
        assertNull("Unknown word should return null for graceful fallback", contextUnknown)
    }
}
