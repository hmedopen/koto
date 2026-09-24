package com.koto.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.data.*
import com.koto.app.feature.lesson.model.*
import com.koto.app.ui.screens.map.MapFixtures
import com.koto.app.ui.screens.map.MapRow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FoundationContentTest {
    @Test fun onlyStagesOneAndTwoHaveContentAndEverySourceStaysWithinTheLearnedRange() {
        assertEquals((1..12).toList(), FoundationLessons.levels.map { it.id })
        assertEquals(List(5) { 1 } + List(7) { 2 }, FoundationLessons.levels.map { it.stage })
        assertNull(FoundationLessons.lesson(0))
        assertNull(FoundationLessons.lesson(13))
        FoundationLessons.levels.forEach { summary ->
            val lesson = FoundationLessons.lesson(summary.id)!!
            lesson.validate()
            assertEquals(summary.questionCount, lesson.questions.size)
            FoundationLessons.sources[summary.id - 1].forEach { reference ->
                val originalLevel = reference.substring(1, 3).toInt()
                assertTrue("No future material: $reference in ${summary.id}", originalLevel <= summary.id)
            }
        }
    }

    @Test fun sourceAnswerKeysArePreservedAfterChoiceArrangement() {
        val answers = mapOf(
            "P01Q1" to "hello", "P01Q4" to "arigatou", "P01Q5" to "ohayou", "F01Q2" to "thank you",
            "P02Q1" to "water", "P02Q3" to "neko", "P02Q4" to "kore wa neko desu", "F02Q2" to "cat",
            "P03Q1" to "watashi wa gakusei desu", "P03Q2" to "watashi wa Yuki desu", "P03Q3" to "friend", "F03Q1" to "I",
            "P04Q1" to "desu", "P04Q2" to "kore wa hon desu", "F04Q1" to "desu",
            "P06Q1" to "eat", "P06Q2" to "watashi wa mizu o nomimasu", "P06Q3" to "mimasu",
            "P07Q1" to "watashi wa sushi o tabemasu", "P07Q2" to "ocha", "P08Q1" to "ongaku o kikimasu",
            "F08Q2" to "mimasu", "F09Q1" to "sleep", "F10Q1" to "What do you do?",
            "P02Q2" to "kore wa mizu desu", "F02Q3" to "kore wa hon desu",
            "P01Q2" to "ohayou=good morning|konnichiwa=hello|arigatou=thank you",
            "P02Q5" to "inu=dog|hon=book|pan=bread",
        )
        fun text(value: LessonText) = when (value) {
            is LessonText.Japanese -> value.value.romaji
            is LessonText.English -> value.value
        }
        FoundationLessons.levels.forEach { level ->
            val lesson = FoundationLessons.lesson(level.id)!!
            lesson.questions.forEachIndexed { index, q ->
                val actual = when (q) {
                    is Question.MeaningChoice -> text(q.options.single { it.id == q.correctId }.text)
                    is Question.ConversationResponse -> text(q.responses.single { it.id == q.correctId }.text)
                    is Question.Cloze -> text(q.options.single { it.id == q.correctId }.text)
                    is Question.SentenceBuilder -> q.sentence.romaji
                    is Question.PairMatch -> q.pairs.joinToString("|") { "${it.japanese.romaji}=${it.english}" }
                }
                assertEquals(q.id, answers.getValue(FoundationLessons.sources[level.id - 1][index]), actual)
            }
        }
    }

    @Test fun allQuestionsScoreOnceCompleteAndReplay() {
        val seen = mutableSetOf<String>()
        FoundationLessons.levels.forEach { level ->
            val session = LessonSession(FoundationLessons.lesson(level.id)!!)
            session.lesson.questions.forEachIndexed { index, q ->
                assertTrue(seen.add(q.id))
                session.next()
                assertEquals(index, session.state.index)
                solve(session, q)
                assertEquals(true, session.correct)
                session.check()
                assertEquals(index + 1, session.state.results.size)
                session.next()
            }
            assertTrue(session.finished)
            assertEquals(1f, session.progress)
            session.replay()
            assertFalse(session.finished)
            assertEquals(0, session.state.index)
            assertTrue(session.state.results.isEmpty())
        }
    }

    @Test fun wrongAnswersRequireReviewBeforeCompletion() {
        val session = LessonSession(FoundationLessons.lesson(1)!!)
        val first = session.question as Question.MeaningChoice
        session.select(first.options.first { it.id != first.correctId }.id)
        session.check()
        session.next()
        while (session.question != null) {
            solve(session, session.question!!)
            session.next()
        }
        assertFalse(session.finished)
        assertTrue(session.state.reviewPending)
        session.startReview()
        assertEquals(first.id, session.question!!.id)
        solve(session, first)
        session.next()
        assertTrue(session.finished)
        assertEquals(1, session.state.results.count { !it })
    }

    @Test fun mapAndUnlockChecksNeverRequestQuestionData() {
        val metadataOnly = object : LessonRepository {
            override val levels = FoundationLessons.levels
            override fun lesson(id: Int): LessonDefinition = error("Map loaded questions for level $id")
        }
        val rows = MapFixtures.rows(emptySet(), metadataOnly)
        assertEquals(2, rows.filterIsInstance<MapRow.Stage>().size)
        assertEquals(12, rows.filterIsInstance<MapRow.Level>().size)
        assertTrue(isLessonUnlocked(1, emptySet(), metadataOnly, unlockAll = false))
        assertFalse(isLessonUnlocked(2, emptySet(), metadataOnly, unlockAll = false))
        assertTrue(isLessonUnlocked(2, setOf(1), metadataOnly, unlockAll = false))
        assertTrue(isLessonUnlocked(12, setOf(11), metadataOnly, unlockAll = false))
        assertFalse(isLessonUnlocked(13, (1..12).toSet(), metadataOnly, unlockAll = true))
    }

    private fun solve(session: LessonSession, q: Question) {
        when (q) {
            is Question.MeaningChoice -> session.select(q.correctId)
            is Question.ConversationResponse -> session.select(q.correctId)
            is Question.Cloze -> session.select(q.correctId)
            is Question.SentenceBuilder -> q.correctOrder.forEach(session::addSentenceTile)
            is Question.PairMatch -> q.pairs.forEach { session.pair(it.id, true); session.pair(it.id, false) }
        }
        session.check()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FoundationProgressTest {
    @Test fun placeholderProgressStaysSeparateAndRealCompletionSurvivesReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val old = context.getSharedPreferences("koto_lessons", Context.MODE_PRIVATE)
        val current = context.getSharedPreferences("koto_foundation_v1", Context.MODE_PRIVATE)
        current.edit().clear().commit()
        old.edit().putStringSet("completed", setOf("1", "2", "10")).commit()
        val progress = LessonProgress(context)
        assertTrue(progress.completed.isEmpty())
        progress.complete(1)
        progress.complete(2)
        progress.complete(2)
        progress.complete(13)
        assertEquals(setOf(1, 2), LessonProgress(context).completed)
        assertEquals(setOf("1", "2", "10"), old.getStringSet("completed", null))
        assertTrue(isLessonUnlocked(3, LessonProgress(context).completed, unlockAll = false))
    }
}
