package com.koto.app

import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.feature.lesson.model.LessonText
import com.koto.app.feature.lesson.model.Question
import com.koto.app.feature.lesson.data.PrototypeLessons
import com.koto.app.feature.lesson.ui.kanaReadingUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseReadingTest {
    @Test fun alignsRomajiToEachKana() {
        val units = kanaReadingUnits(JapaneseText("わたし", "watashi"))
        assertEquals(listOf("わ" to "wa", "た" to "ta", "し" to "shi"),
            units.map { it.kana to it.romaji })
    }

    @Test fun preservesAuthoredParticleReadingsAndWordBreaks() {
        val units = kanaReadingUnits(JapaneseText("これは ほんです", "kore wa hon desu"))
        assertEquals("wa", units.single { it.kana == "は" }.romaji)
        assertTrue(units.single { it.kana == "ほ" }.breakBefore)
        assertTrue(units.single { it.kana == "で" }.breakBefore)
    }

    @Test fun handlesSmallKanaGeminationKatakanaAndPunctuation() {
        assertEquals(listOf("ga", "k", "ko", "u"),
            kanaReadingUnits(JapaneseText("がっこう", "gakkou")).map { it.romaji })
        assertEquals(listOf("お" to "o", "ちゃ" to "cha"),
            kanaReadingUnits(JapaneseText("おちゃ", "ocha")).map { it.kana to it.romaji })
        assertEquals(listOf("pa", "n"),
            kanaReadingUnits(JapaneseText("パン", "pan")).map { it.romaji })
        val phrase = kanaReadingUnits(JapaneseText("はい、げんきです", "hai, genki desu"))
        assertEquals("", phrase.single { it.kana == "、" }.romaji)
        assertTrue(phrase.single { it.kana == "げ" }.breakBefore)
    }

    @Test fun everyAuthoredJapaneseDisplayKeepsItsFullKanaAndRomajiReading() {
        val readings = PrototypeLessons.lessons.flatMap { lesson ->
            lesson.questions.flatMap { question ->
                fun japanese(text: LessonText) = (text as? LessonText.Japanese)?.value?.let(::listOf).orEmpty()
                when (question) {
                    is Question.MeaningChoice -> japanese(question.prompt) + question.options.flatMap { japanese(it.text) }
                    is Question.SentenceBuilder -> listOf(question.sentence) + question.tiles.flatMap { japanese(it.text) }
                    is Question.Cloze -> question.options.flatMap { japanese(it.text) } + question.filled(question.correctId)
                    is Question.ConversationResponse -> listOf(question.incoming) + question.responses.flatMap { japanese(it.text) }
                    is Question.PairMatch -> question.pairs.map { it.japanese }
                }
            }
        }.distinct()

        readings.forEach { reading ->
            val units = kanaReadingUnits(reading)
            assertEquals(reading.kana.filterNot(Char::isWhitespace), units.joinToString("") { it.kana })
            assertEquals(reading.romaji.lowercase().filter(Char::isLetter), units.joinToString("") { it.romaji })
            assertTrue("Missing aligned reading for $reading", units
                .filterNot { it.kana.first() in setOf('。', '、', '？', '！', '・', '「', '」', '（', '）') }
                .all { it.romaji.isNotBlank() })
        }
    }
}
