package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.koto.app.feature.lesson.LessonScreen
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.data.PrototypeLessons
import com.koto.app.feature.lesson.model.Question
import com.koto.app.ui.theme.KotoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuizFeedbackOverlayTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val questions = listOf(
        PrototypeLessons.lesson(3)!!.questions[0],
        PrototypeLessons.lesson(9)!!.questions[0],
        PrototypeLessons.lesson(3)!!.questions[2],
        PrototypeLessons.lesson(3)!!.questions[3],
    )

    private fun show(items: List<Question>, rtl: Boolean = false, fontScale: Float = 1f) {
        val lesson = PrototypeLessons.lesson(3)!!.copy(questions = items)
        val audio = JapaneseTtsController.get(compose.activity)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(density.density, fontScale),
            ) { KotoTheme { LessonScreen(lesson, audio, {}, {}) } }
        }
        compose.waitForIdle()
    }

    private fun positions(q: Question): Map<Int, Rect> {
        val quizNodes = compose.onAllNodes(hasAnyAncestor(hasTestTag("question_${q.id}")), useUnmergedTree = true)
            .fetchSemanticsNodes()
        val fixedNodes = listOf("lesson_close", "lesson_progress", "lesson_settings", "lesson_action", "question_scroll")
            .map { compose.onNodeWithTag(it).fetchSemanticsNode() }
        return (quizNodes + fixedNodes).associate { it.id to it.boundsInRoot }
    }

    private fun assertPositions(q: Question, before: Map<Int, Rect>) {
        val after = positions(q)
        assertEquals("Underlying nodes must survive feedback", before.keys, after.keys)
        before.forEach { (id, bounds) -> assertEquals("${q.id}: node $id moved", bounds, after[id]) }
    }

    private fun verifyFlow(correct: Boolean) {
        show(questions)
        questions.forEach { q ->
            val action = compose.onNodeWithTag("lesson_action")
            action.assertIsDisplayed().assertIsNotEnabled().performClick()
            compose.onNodeWithTag("lesson_feedback").assertDoesNotExist()
            val manual = q is Question.Cloze || q is Question.SentenceBuilder
            if (q is Question.Cloze) {
                val answer = if (correct) q.correctId else q.options.first { it.id != q.correctId }.id
                compose.onNodeWithTag("answer_$answer").performClick()
                action.assertIsEnabled().assertTextContains("CHECK")
            } else if (q is Question.SentenceBuilder) {
                val chosen = if (correct) q.correctOrder else q.correctOrder.dropLast(1) + q.tiles.first { it.id !in q.correctOrder }.id
                chosen.forEach { compose.onNodeWithTag("tile_$it").performScrollTo().performClick() }
                action.assertIsEnabled().assertTextContains("CHECK")
            } else action.assertTextContains("CONTINUE")

            if (q is Question.SentenceBuilder && !correct) {
                val extra = q.tiles.first { it.id !in q.correctOrder }.id
                val chosen = q.correctOrder.dropLast(1) + extra
                action.performClick()
                compose.onNodeWithTag("sentence_feedback").assertIsDisplayed()
                compose.onNodeWithTag("lesson_feedback").assertDoesNotExist()
                action.assertIsEnabled().assertTextContains("CHECK")
                chosen.forEach { compose.onNodeWithTag("assembled_$it").assertIsEnabled() }
                // Correct the one mistaken tile without rebuilding the rest of the sentence.
                compose.onNodeWithTag("assembled_$extra").performScrollTo().performClick()
                q.correctOrder.dropLast(1).forEach { compose.onNodeWithTag("assembled_$it").assertExists() }
                compose.onNodeWithTag("tile_${q.correctOrder.last()}").performScrollTo().performClick()
            }

            compose.waitForIdle()
            val before = positions(q)
            val scrollBefore = compose.onNodeWithTag("question_scroll").fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange].value()
            saveRenderedScreenshot(compose.activity, "feedback-${q.id}-$correct-before")
            compose.mainClock.autoAdvance = false
            if (manual) action.performClick() else {
                val (options, correctId) = when (q) {
                    is Question.MeaningChoice -> q.options to q.correctId
                    is Question.ConversationResponse -> q.responses to q.correctId
                    else -> error("Unexpected quiz")
                }
                val answer = if (correct) correctId else options.first { it.id != correctId }.id
                compose.onNodeWithTag("answer_$answer").performClick()
            }
            compose.mainClock.advanceTimeBy(128)
            compose.waitForIdle()
            assertPositions(q, before)
            val enteringTop = compose.onNodeWithTag("lesson_feedback").fetchSemanticsNode().positionInRoot.y
            compose.mainClock.advanceTimeBy(250)
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            assertPositions(q, before)
            val overlay = compose.onNodeWithTag("lesson_feedback").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Only the overlay slides upward", enteringTop > overlay.top)
            val screen = compose.onNodeWithTag("lesson_screen").fetchSemanticsNode().boundsInRoot
            assertEquals(screen.left, overlay.left, 0f)
            assertEquals(screen.right, overlay.right, 0f)
            assertEquals(screen.bottom, overlay.bottom, 0f)
            val button = action.assertIsEnabled().assertTextContains("CONTINUE").fetchSemanticsNode().boundsInRoot
            assertTrue(button.bottom < screen.bottom)
            val accepted = correct || q is Question.SentenceBuilder
            compose.onNodeWithText(if (accepted) "Correct!" else "Incorrect").assertIsDisplayed()
            if (!accepted) compose.onNodeWithText("Correct answer:").assertIsDisplayed()
            assertEquals(scrollBefore, compose.onNodeWithTag("question_scroll").fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange].value(), 0f)
            compose.onAllNodes(hasAnyAncestor(hasTestTag("question_${q.id}")) and hasClickAction())
                .fetchSemanticsNodes().filter { it.config.contains(SemanticsProperties.Selected) }.forEach {
                    assertTrue("Answers are frozen", it.config.contains(SemanticsProperties.Disabled))
                }
            saveRenderedScreenshot(compose.activity, "feedback-${q.id}-$correct-after")
            // Exercise the real pointer path at the side of the full-width action target.
            action.performTouchInput { click(Offset(8f, center.y)) }
            compose.onNodeWithTag("lesson_feedback").assertDoesNotExist()
        }
        compose.onNodeWithTag("lesson_complete").assertIsDisplayed()
        compose.onNodeWithText(if (correct) "4 / 4" else "0 / 4").assertIsDisplayed()
    }

    @Test fun correctResultsNeverMoveTheQuizAndContinueReallyWorks() = verifyFlow(true)
    @Test fun wrongResultsNeverMoveTheQuizAndContinueReallyWorks() = verifyFlow(false)

    @Test fun staleContinueCannotAdvanceAnAlreadyAnsweredNextQuestion() {
        val q1 = questions[0] as Question.MeaningChoice
        val q2 = questions[1] as Question.ConversationResponse
        show(questions)
        compose.onNodeWithTag("answer_${q1.correctId}").performClick()
        val staleContinue = compose.onNodeWithTag("lesson_action").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.onNodeWithTag("lesson_action").performTouchInput { doubleClick() }
        compose.onNodeWithTag("question_${q2.id}").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsNotEnabled()
        compose.onNodeWithTag("answer_${q2.correctId}").performClick()
        compose.runOnIdle { staleContinue() }
        compose.onNodeWithTag("question_${q2.id}").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsEnabled().performClick()
        compose.onNodeWithTag("question_${questions[2].id}").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsNotEnabled().assertTextContains("CHECK")
    }

    @Test fun matchingPairsCompletionOverlaysTheBoardUntilContinue() {
        val pairs = PrototypeLessons.lesson(3)!!.questions.first { it is Question.PairMatch } as Question.PairMatch
        val next = questions.first()
        show(listOf(pairs, next))
        val boardBefore = compose.onNodeWithTag("question_scroll").fetchSemanticsNode().boundsInRoot
        pairs.pairs.forEach { pair ->
            compose.onNodeWithTag("pair_ja_${pair.id}").performClick()
            compose.onNodeWithTag("pair_en_${pair.id}").performClick()
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("question_${pairs.id}").assertIsDisplayed()
        compose.onNodeWithTag("lesson_feedback").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsEnabled().assertTextContains("CONTINUE")
        assertEquals(boardBefore, compose.onNodeWithTag("question_scroll").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("lesson_action").performClick()
        compose.onNodeWithTag("question_${next.id}").assertIsDisplayed()
    }

    @Test fun rtlLargeTextKeepsCorrectionAndContinueInsideTheSafeArea() {
        val q = questions[1] as Question.ConversationResponse
        show(listOf(q, questions[2]), rtl = true, fontScale = 2f)
        compose.onNodeWithTag("answer_${q.responses.first { it.id != q.correctId }.id}").performScrollTo().performClick()
        compose.onNodeWithText("Incorrect").assertIsDisplayed()
        compose.onNodeWithText("Correct answer:").assertIsDisplayed()
        val screen = compose.onNodeWithTag("lesson_screen").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("lesson_action").assertIsDisplayed().assertIsEnabled().fetchSemanticsNode().boundsInRoot
        assertTrue(button.bottom < screen.bottom)
        saveRenderedScreenshot(compose.activity, "feedback-rtl-large-text")
        compose.onNodeWithTag("lesson_action").performClick()
        compose.onNodeWithTag("question_${questions[2].id}").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsNotEnabled()
    }
}
