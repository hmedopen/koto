package com.koto.app

import androidx.compose.runtime.saveable.SaverScope
import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.data.PrototypeLessons
import com.koto.app.feature.lesson.model.LessonDefinition
import com.koto.app.feature.lesson.model.Question
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MistakeReviewTest {
    private val source: LessonDefinition = PrototypeLessons.lesson(3)!!
    private val choices: List<Question.MeaningChoice> = source.questions.filterIsInstance<Question.MeaningChoice>().take(2)
    private val lesson: LessonDefinition = source.copy(questions = choices)
    private fun answer(session: LessonSession, correct: Boolean) {
        val q = session.question as Question.MeaningChoice
        session.select(if (correct) q.correctId else q.options.first { it.id != q.correctId }.id)
        assertFalse(session.checked)
        session.check()
        assertEquals(correct, session.correct)
        session.check() // A repeated submission cannot duplicate a queue entry or score.
    }
    private fun restore(session: LessonSession): LessonSession {
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saver = LessonSession.saver(lesson)
        return saver.restore(with(saver) { scope.save(session) }!!)!!
    }

    @Test fun wrongReviewsRotateUntilSolvedAndNeverChangeFirstAttemptScores() {
        var session = LessonSession(lesson)
        repeat(2) { answer(session, false); session.next() }
        assertTrue(session.state.reviewPending)
        assertFalse(session.finished)
        assertEquals(listOf(0, 1), session.state.mistakes)
        assertEquals(1f, session.progress)
        session = restore(session)
        session.startReview()
        session.startReview()
        assertEquals(0, session.state.index)
        answer(session, false)
        session = restore(session)
        assertEquals(false, session.correct)
        session.next()
        assertEquals(listOf(1, 0), session.state.mistakes)
        assertEquals(1, session.state.index)
        answer(session, true)
        session = restore(session)
        assertEquals(true, session.correct)
        session.next()
        assertEquals(listOf(0), session.state.mistakes)
        repeat(2) {
            val attempt = session.state.attempt
            answer(session, false)
            session.next()
            assertEquals(0, session.state.index)
            assertEquals(attempt + 1, session.state.attempt)
            assertFalse(session.checked)
            assertNull(session.state.selected)
        }
        answer(session, true)
        session.next()
        assertTrue(session.finished)
        assertTrue(session.state.mistakes.isEmpty())
        assertEquals(listOf(false, false), session.state.results)
        session = restore(session)
        assertTrue(session.finished)
        session.replay()
        assertFalse(session.state.reviewing)
        assertTrue(session.state.results.isEmpty())
    }

    @Test fun everyChoiceTypeAllowsReplacingWrongSelectionBeforeCheck() {
        PrototypeLessons.lessons.flatMap { it.questions }.forEach { q ->
            val (options, correctId) = when (q) {
                is Question.MeaningChoice -> q.options to q.correctId
                is Question.ConversationResponse -> q.responses to q.correctId
                is Question.Cloze -> q.options to q.correctId
                else -> return@forEach
            }
            val session = LessonSession(source.copy(questions = listOf(q)))
            session.select(options.first { it.id != correctId }.id)
            assertFalse(session.checked)
            assertTrue(session.state.results.isEmpty())
            session.select(correctId)
            session.check()
            assertEquals(true, session.correct)
            session.next()
            assertTrue(session.finished)
            assertFalse(session.state.reviewPending)
        }
    }
}
