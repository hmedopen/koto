package com.koto.app

import com.koto.app.feature.lesson.*
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.data.*
import com.koto.app.feature.lesson.model.*
import org.junit.Assert.*
import org.junit.Test

class LessonSessionTest {
    @Test fun allSixtyQuestionsValidateCompleteAndReplayCleanly() {
        assertEquals(10, PrototypeLessons.lessons.size)
        assertEquals(60, PrototypeLessons.lessons.sumOf { it.questions.size })
        PrototypeLessons.lessons.forEach { lesson ->
            lesson.validate()
            val session = LessonSession(lesson)
            lesson.questions.forEachIndexed { index, q ->
                session.next() // Cannot skip an unanswered question.
                assertEquals(index, session.state.index)
                when (q) {
                    is Question.MeaningChoice -> session.select(q.correctId)
                    is Question.ConversationResponse -> session.select(q.correctId)
                    is Question.Cloze -> { session.select(q.correctId); assertFalse(session.checked); session.check() }
                    is Question.SentenceBuilder -> { q.correctOrder.forEach(session::toggleTile); session.check() }
                    is Question.PairMatch -> q.pairs.forEach { session.pair(it.id, true); session.pair(it.id, false) }
                }
                assertTrue("${q.id} correct", session.correct == true)
                session.check() // Duplicate taps must not double score.
                assertEquals(index + 1, session.state.results.size)
                session.next()
            }
            assertTrue(session.finished)
            assertEquals(6, session.state.results.count { it })
            session.next()
            session.replay()
            assertEquals(SessionState(), session.state)
        }
    }

    @Test fun manualChoicesAreEditableUntilCheckThenFreezeAndScoreOnce() {
        val lesson = PrototypeLessons.lesson(3)!!
        val session = LessonSession(lesson, SessionState(index = 2, results = listOf(true, true)))
        val cloze = session.question as Question.Cloze
        assertFalse(session.canCheck)
        assertNull(session.check())
        session.select(cloze.correctId)
        assertEquals("わたしは みず を のみます", cloze.filled(session.state.selected).kana)
        session.select(cloze.options.first { it.id != cloze.correctId }.id)
        session.check()
        assertTrue(session.checked)
        val submitted = session.state
        session.select(cloze.correctId)
        session.check()
        assertEquals(submitted, session.state)
        assertEquals(false, session.correct)
        session.next()
        session.next() // A second Continue cannot skip the unanswered question.
        val builder = session.question as Question.SentenceBuilder
        session.toggleTile(builder.correctOrder.first())
        session.toggleTile(builder.correctOrder.first())
        assertTrue(session.state.tiles.isEmpty())
        builder.correctOrder.reversed().forEach(session::toggleTile)
        assertTrue(session.canCheck)
        session.toggleTile("missing")
        session.check()
        assertTrue(session.checked)
        assertEquals(QuizFeedback.Wrong, session.state.feedback)
        val checkedBuilder = session.state
        session.toggleTile(builder.correctOrder.first())
        session.check()
        assertEquals(checkedBuilder, session.state)
    }

    @Test fun wrongAutoChecksFreezeTheSelectionAndContinueResetsFeedback() {
        listOf(PrototypeLessons.lesson(1)!!.questions.first(), PrototypeLessons.lesson(9)!!.questions.first()).forEach { q ->
            val (answers, correctId) = when (q) {
                is Question.MeaningChoice -> q.options to q.correctId
                is Question.ConversationResponse -> q.responses to q.correctId
                else -> error("Expected an auto-check question")
            }
            val session = LessonSession(PrototypeLessons.lesson(1)!!.copy(questions = listOf(q, q)))
            session.select(answers.first { it.id != correctId }.id)
            assertTrue(session.checked)
            assertEquals(false, session.correct)
            val submitted = session.state
            session.select(correctId)
            session.check()
            assertEquals(submitted, session.state)
            session.next()
            session.next()
            assertEquals(SessionState(index = 1, results = listOf(false)), session.state)
            session.select(correctId)
            assertTrue(session.checked)
            assertEquals(correctId, session.state.selected)
            assertEquals(listOf(false, true), session.state.results)
        }
    }

    @Test fun pairErrorsResetAndCompletionIsStillCorrect() {
        val session = LessonSession(PrototypeLessons.lesson(1)!!, SessionState(index = 2, results = listOf(true, true)))
        val q = session.question as Question.PairMatch
        session.pair(q.pairs[0].id, false)
        session.pair(q.pairs[1].id, true)
        assertTrue(session.state.mismatch)
        session.pair(q.pairs[0].id, true) // Rapid taps during rejection are ignored.
        assertTrue(session.state.matched.isEmpty())
        session.clearMismatch()
        q.pairs.forEach { session.pair(it.id, false); session.pair(it.id, true) }
        assertEquals(true, session.correct)
        assertEquals(3, session.state.matched.size)
        assertEquals(3, session.state.results.size)
    }

    @Test fun placesPairsCompleteCorrectlyAfterHomeToSchoolMismatch() {
        val question = PrototypeLessons.lesson(8)!!.questions
            .filterIsInstance<Question.PairMatch>().single()
        val session = LessonSession(PrototypeLessons.lesson(8)!!.copy(questions = listOf(question)))
        val home = question.pairs.single { it.english == "Home" }
        val school = question.pairs.single { it.english == "School" }

        session.pair(home.id, false)
        session.pair(school.id, true)
        assertTrue(session.state.mismatch)
        assertTrue(session.state.matched.isEmpty())

        session.clearMismatch()
        question.pairs.forEach { pair ->
            session.pair(pair.id, true)
            session.pair(pair.id, false)
        }

        assertEquals(question.pairs.size, session.state.matched.size)
        assertEquals(true, session.correct)
    }

    @Test fun progressionIsSequentialWithoutDebugOverrideAndFutureNodesStayLocked() {
        assertTrue(isLessonUnlocked(1, emptySet(), unlockAll = false))
        assertFalse(isLessonUnlocked(2, emptySet(), unlockAll = false))
        assertTrue(isLessonUnlocked(2, setOf(1), unlockAll = false))
        assertFalse(isLessonUnlocked(3, setOf(1), unlockAll = false))
        assertTrue(isLessonUnlocked(10, emptySet(), unlockAll = true))
        assertFalse(isLessonUnlocked(11, (1..10).toSet(), unlockAll = true))
        // An eleventh definition needs no new renderer or route.
        val repository = object : LessonRepository {
            override val lessons = PrototypeLessons.lessons + PrototypeLessons.lessons.last().copy(id = 11)
        }
        assertTrue(isLessonUnlocked(11, setOf(10), repository, unlockAll = false))
    }

    @Test fun validatorsRejectKanjiMissingRomajiAndInvalidAnswerIds() {
        val original = PrototypeLessons.lesson(1)!!
        val first = original.questions.first() as Question.MeaningChoice
        fun rejected(question: Question) {
            assertThrows(IllegalArgumentException::class.java) { original.copy(questions = listOf(question)).validate() }
        }
        rejected(first.copy(prompt = LessonText.Japanese(JapaneseText("猫", "neko"))))
        rejected(first.copy(prompt = LessonText.Japanese(JapaneseText("ねこ", ""))))
        rejected(first.copy(correctId = "missing"))
        rejected(first.copy(options = first.options + first.options.first()))
        val builder = PrototypeLessons.lesson(2)!!.questions.last() as Question.SentenceBuilder
        rejected(builder.copy(correctOrder = listOf("missing")))
        rejected(builder.copy(correctOrder = builder.correctOrder + builder.correctOrder.first()))
        val pairs = original.questions[2] as Question.PairMatch
        rejected(pairs.copy(pairs = pairs.pairs + pairs.pairs.first()))
    }

    @Test fun speechBoundaryRejectsEnglishAndKanjiEvenInMalformedJapaneseObjects() {
        assertTrue(JapaneseTtsController.canSpeak(JapaneseText("ねこ", "neko")))
        assertFalse(JapaneseTtsController.canSpeak(JapaneseText("Cat", "cat")))
        assertFalse(JapaneseTtsController.canSpeak(JapaneseText("ねこ", "neko", "Cat")))
        assertFalse(JapaneseTtsController.canSpeak(JapaneseText("猫です", "neko desu")))
    }
}
