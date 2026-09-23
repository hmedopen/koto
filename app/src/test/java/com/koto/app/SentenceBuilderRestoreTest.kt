package com.koto.app

import androidx.compose.runtime.saveable.SaverScope
import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.SentenceValidation
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
class SentenceBuilderRestoreTest {
    private val source: LessonDefinition = PrototypeLessons.lesson(4)!!
    private val question: Question.SentenceBuilder = source.questions.filterIsInstance<Question.SentenceBuilder>().single()
    private val lesson: LessonDefinition = source.copy(questions = listOf(question))
    private val scope = object : SaverScope {
        override fun canBeSaved(value: Any) = true
    }

    @Test fun saverRetainsFeedbackAndFirstTryHistoryButNeverResumesATransientReorder() {
        val session = LessonSession(lesson)
        session.addSentenceTile(question.correctOrder.first())
        session.check()
        session.startSentenceReorder(question.correctOrder.first())
        val saver = LessonSession.saver(lesson)
        val bundle = with(saver) { scope.save(session) }!!
        assertFalse(bundle.containsKey("sentenceMoving"))
        // Old bundles may contain this value; restoration must still discard it.
        bundle.putString("sentenceMoving", question.correctOrder.first())
        val restored = saver.restore(bundle)!!
        assertEquals(session.state.tiles, restored.state.tiles)
        assertEquals(session.state.feedback, restored.state.feedback)
        assertEquals(SentenceValidation.Missing, restored.sentenceGame.validation)
        assertEquals(session.sentenceGame.validationEpoch, restored.sentenceGame.validationEpoch)
        assertTrue(restored.state.hadMistake)
        assertNull(restored.sentenceGame.movingTileId)
        question.correctOrder.drop(1).forEach(restored::addSentenceTile)
        assertEquals(question.sentence, restored.check())
        assertEquals(SentenceValidation.Correct, restored.sentenceGame.validation)
        assertEquals(listOf(false), restored.state.results)

        val completed = saver.restore(with(saver) { scope.save(restored) }!!)!!
        assertTrue(completed.checked)
        assertEquals(SentenceValidation.Correct, completed.sentenceGame.validation)
        assertEquals(restored.state, completed.state)
    }
}
