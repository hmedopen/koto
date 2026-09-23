package com.koto.app

import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.QuizFeedback
import com.koto.app.feature.lesson.SentenceGameState
import com.koto.app.feature.lesson.SentenceValidation
import com.koto.app.feature.lesson.SessionState
import com.koto.app.feature.lesson.data.PrototypeLessons
import com.koto.app.feature.lesson.model.LessonDefinition
import com.koto.app.feature.lesson.model.Question
import org.junit.Assert.*
import org.junit.Test

class SentenceBuilderStateTest {
    private val source: LessonDefinition = PrototypeLessons.lesson(4)!!
    private val question: Question.SentenceBuilder = source.questions.filterIsInstance<Question.SentenceBuilder>().single()
    private val lesson: LessonDefinition = source.copy(questions = listOf(question))

    private fun assertPartition(session: LessonSession, q: Question.SentenceBuilder = question) {
        val game = session.sentenceGame
        val authored = q.tiles.map { it.id }
        assertEquals(game.sentenceTileIds, session.state.tiles)
        assertEquals(game.sentenceTileIds.distinct(), game.sentenceTileIds)
        assertEquals(authored.filterNot { it in game.sentenceTileIds }, game.availableTileIds)
        assertEquals(authored.toSet(), (game.availableTileIds + game.sentenceTileIds).toSet())
    }

    @Test fun rapidEditsPreserveIdsAndOriginalPoolPositionsWithoutWaitingForAnimation() {
        val session = LessonSession(lesson)
        val ids = question.tiles.map { it.id }
        repeat(30) {
            ids.forEach { id ->
                session.addSentenceTile(id)
                session.addSentenceTile(id) // A duplicate input cannot duplicate a tile.
                assertPartition(session)
            }
            assertEquals(ids, session.sentenceGame.sentenceTileIds)
            ids.reversed().forEach { id ->
                session.removeSentenceTile(id)
                session.removeSentenceTile(id)
                assertPartition(session)
            }
            assertEquals(ids, session.sentenceGame.availableTileIds)
        }
    }

    @Test fun atomicMovesUseFinalIndicesInBothDirectionsAndClampAtTheEdges() {
        val session = LessonSession(lesson)
        val ids = question.correctOrder
        ids.forEach(session::addSentenceTile)
        session.moveSentenceTile(ids.first(), 2)
        assertEquals(listOf(ids[1], ids[2], ids[0]) + ids.drop(3), session.state.tiles)
        session.moveSentenceTile(ids.first(), 0)
        assertEquals(ids, session.state.tiles)
        session.moveSentenceTile(ids.first(), Int.MAX_VALUE)
        assertEquals(ids.drop(1) + ids.first(), session.state.tiles)
        session.moveSentenceTile(ids.first(), Int.MIN_VALUE)
        assertEquals(ids, session.state.tiles)
        assertPartition(session)
        val before = session.state
        session.moveSentenceTile(ids.first(), 0)
        session.moveSentenceTile("not-authored", 1)
        session.removeSentenceTile("not-authored")
        session.addSentenceTile("not-authored")
        assertEquals(before, session.state)
    }

    @Test fun structuralMistakesSubmitAndAreSolvedDuringReview() {
        val builds = listOf(question.correctOrder.dropLast(1), question.correctOrder.reversed(),
            question.correctOrder + question.tiles.first { it.id !in question.correctOrder }.id)
        builds.forEach { ids ->
            val session = LessonSession(lesson)
            ids.forEach(session::addSentenceTile)
            assertFalse(session.checked)
            session.check()
            assertTrue(session.checked)
            assertEquals(false, session.correct)
            assertEquals(QuizFeedback.Wrong, session.state.feedback)
            session.removeSentenceTile(ids.first())
            assertEquals(ids, session.state.tiles)
            session.next()
            assertTrue(session.state.reviewPending)
            assertFalse(session.finished)
            session.startReview()
            question.correctOrder.forEach(session::addSentenceTile)
            assertEquals(question.sentence, session.check())
            assertEquals(true, session.correct)
            assertEquals(listOf(false), session.state.results)
            session.next()
            assertTrue(session.finished)
            assertTrue(session.state.mistakes.isEmpty())
        }
    }
    @Test fun wrongReplacementIsASubmittedFailureAndCannotBeSilentlyCorrected() {
        val session = LessonSession(lesson)
        val distractor = question.tiles.first { it.id !in question.correctOrder }
        question.correctOrder.dropLast(1).forEach(session::addSentenceTile)
        session.addSentenceTile(distractor.id)
        val submitted = session.state.tiles

        assertNull(session.check())

        assertTrue(session.checked)
        assertEquals(false, session.correct)
        assertEquals(SentenceValidation.WrongTiles, session.sentenceGame.validation)
        assertEquals(QuizFeedback.Wrong, session.state.feedback)
        assertEquals(listOf(false), session.state.results)
        session.removeSentenceTile(distractor.id)
        session.addSentenceTile(question.correctOrder.last())
        assertEquals(submitted, session.state.tiles)
    }

    @Test fun substantiallyWrongShortSentenceIsARealFailureNotAMissingWordWarning() {
        val session = LessonSession(lesson)
        question.tiles.filter { it.id !in question.correctOrder }.forEach { session.addSentenceTile(it.id) }

        session.check()

        assertTrue(session.checked)
        assertEquals(false, session.correct)
        assertEquals(SentenceValidation.WrongTiles, session.sentenceGame.validation)
        assertEquals(QuizFeedback.Wrong, session.state.feedback)
    }

    @Test fun identicalAuthoredWordsRemainIndependentButAreInterchangeableWhenChecked() {
        val session = LessonSession(lesson)
        val duplicates = question.tiles.groupBy { it.text }.values.single { it.size > 1 }
        val expected = duplicates.single { it.id in question.correctOrder }
        val equivalent = duplicates.single { it.id !in question.correctOrder }
        session.addSentenceTile(expected.id)
        session.addSentenceTile(equivalent.id)
        session.removeSentenceTile(expected.id)
        assertEquals(listOf(equivalent.id), session.state.tiles)
        assertTrue(expected.id in session.sentenceGame.availableTileIds)
        session.removeSentenceTile(equivalent.id)
        question.correctOrder.map { if (it == expected.id) equivalent.id else it }.forEach(session::addSentenceTile)
        assertEquals(question.sentence, session.check())
        assertEquals(SentenceValidation.Correct, session.sentenceGame.validation)
        assertEquals(listOf(true), session.state.results)
        assertPartition(session)
    }

    @Test fun submittedSentenceCannotBeEditedOrScoredTwice() {
        val session = LessonSession(lesson)
        question.correctOrder.forEach(session::addSentenceTile)
        session.check()
        val submitted = session.state
        session.check()
        session.moveSentenceTile(question.correctOrder.first(), 2)
        session.removeSentenceTile(question.correctOrder.first())
        session.addSentenceTile(question.tiles.first { it.id !in question.correctOrder }.id)
        session.startSentenceReorder(question.correctOrder.first())
        assertEquals(submitted, session.state)
        assertEquals(listOf(true), session.state.results)
    }

    @Test fun restoreNormalizesIdsAndPoolAndDiscardsInterruptedLegacyReorder() {
        val id = question.correctOrder.first()
        val restored = LessonSession(lesson, SessionState(
            tiles = listOf("stale-mirror"),
            sentenceGame = SentenceGameState(
                availableTileIds = listOf(id, "removed-id"),
                sentenceTileIds = listOf(id, id, "removed-id"),
                movingTileId = id,
            ),
        ))
        assertEquals(listOf(id), restored.state.tiles)
        assertNull(restored.sentenceGame.movingTileId)
        assertPartition(restored)
        restored.addSentenceTile(question.correctOrder[1])
        assertEquals(question.correctOrder.take(2), restored.state.tiles)
    }

    @Test fun legacySavedTileOrderIsRecoveredWithoutReorderingTheSentence() {
        val ids = question.correctOrder.take(3).reversed()
        val restored = LessonSession(lesson, SessionState(tiles = ids))
        assertEquals(ids, restored.state.tiles)
        assertPartition(restored)
    }

    @Test fun everyExistingBuildCanBeCompletedWithItsAuthoredAnswerAndReset() {
        PrototypeLessons.lessons.forEach { sourceLesson: LessonDefinition ->
            sourceLesson.questions.filterIsInstance<Question.SentenceBuilder>().forEach { q ->
                val session = LessonSession(sourceLesson.copy(questions = listOf(q)))
                q.correctOrder.forEach(session::addSentenceTile)
                assertEquals(q.sentence, session.check())
                assertEquals(SentenceValidation.Correct, session.sentenceGame.validation)
                assertEquals(listOf(true), session.state.results)
                assertPartition(session, q)
                session.replay()
                assertTrue(session.state.tiles.isEmpty())
                assertEquals(q.tiles.map { it.id }, session.sentenceGame.availableTileIds)
            }
        }
    }
}
