package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.koto.app.feature.lesson.LessonExercise
import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.SentenceValidation
import com.koto.app.feature.lesson.SessionState
import com.koto.app.feature.lesson.data.PrototypeLessons
import com.koto.app.feature.lesson.model.LessonText
import com.koto.app.feature.lesson.model.Question
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pointer-driven tests: semantics clicks do not exercise gesture ownership or moving hit targets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SentenceBuilderInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun question(level: Int) = PrototypeLessons.lesson(level)!!.questions
        .filterIsInstance<Question.SentenceBuilder>().single()

    private fun show(q: Question.SentenceBuilder, selected: List<String> = emptyList(),
        fontScale: Float = 1f, rtl: Boolean = false): LessonSession {
        val lesson = PrototypeLessons.lesson(2)!!.copy(questions = listOf(q))
        val session = LessonSession(lesson, SessionState(tiles = selected))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                KotoTheme {
                    Box(Modifier.fillMaxSize().background(KotoColors.Background).testTag("sentence_test_screen")) {
                        LessonExercise(session, false, false, {}, {}, Modifier.fillMaxSize())
                    }
                }
            }
        }
        compose.waitForIdle()
        return session
    }

    private fun tap(tag: String) = compose.onNodeWithTag(tag).performTouchInput {
        down(center)
        advanceEventTime(16)
        up()
    }

    private fun poolPositions(q: Question.SentenceBuilder): Map<String, Rect> {
        val origin = compose.onNodeWithTag("sentence_board").fetchSemanticsNode().positionInRoot
        return q.tiles.associate { tile ->
            val node = compose.onNodeWithTag("sentence_pool_slot_${tile.id}").fetchSemanticsNode()
            tile.id to Rect(node.positionInRoot - origin, Size(node.size.width.toFloat(), node.size.height.toFloat()))
        }
    }

    private fun boardPoint(tileId: String): Offset {
        val board = compose.onNodeWithTag("sentence_board").fetchSemanticsNode().positionInRoot
        return compose.onNodeWithTag("assembled_$tileId").fetchSemanticsNode().boundsInRoot.center - board
    }

    private fun drag(fromId: String, destinationId: String) {
        val source = boardPoint(fromId)
        val destination = boardPoint(destinationId)
        compose.onNodeWithTag("sentence_board").performTouchInput {
            down(source)
            advanceEventTime(650)
            moveTo(destination, delayMillis = 32)
            up()
        }
        compose.waitForIdle()
    }

    @Test fun quickTapsCommitImmediatelyAndCanReverseAnUnfinishedFlight() {
        val q = question(2)
        val session = show(q)
        val homes = poolPositions(q)
        val first = q.tiles[0].id
        val second = q.tiles[1].id
        compose.mainClock.autoAdvance = false

        tap("tile_$first")
        compose.runOnIdle { assertEquals(listOf(first), session.sentenceGame.sentenceTileIds) }
        compose.mainClock.advanceTimeBy(32)
        tap("assembled_$first")
        compose.runOnIdle {
            assertTrue("A returning tile is usable before its animation completes", session.sentenceGame.sentenceTileIds.isEmpty())
        }
        compose.mainClock.advanceTimeBy(32)
        tap("tile_$second")
        compose.runOnIdle { assertEquals(listOf(second), session.sentenceGame.sentenceTileIds) }

        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals("Pool slots must never reflow", homes, poolPositions(q))
        tap("tile_$first")
        compose.runOnIdle { assertEquals(listOf(second, first), session.sentenceGame.sentenceTileIds) }
        assertEquals(homes, poolPositions(q))
        saveRenderedScreenshot(compose.activity, "sentence-quick-taps-320")
    }

    @Test fun longPressDragMovesAcrossRowsAndCanInsertAtEitherEnd() {
        val q = question(10)
        val session = show(q, q.correctOrder)
        val homes = poolPositions(q)
        val source = boardPoint(q.correctOrder.first())
        val destination = boardPoint(q.correctOrder.last())
        assertTrue("Fixture must cross a wrapped row", destination.y > source.y)

        compose.onNodeWithTag("sentence_board").performTouchInput {
            down(source)
            advanceEventTime(650)
            moveTo(destination, delayMillis = 32)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("A drag preview must not corrupt committed order", q.correctOrder, session.sentenceGame.sentenceTileIds)
        }
        saveRenderedScreenshot(compose.activity, "sentence-drag-preview-320")
        compose.onNodeWithTag("sentence_board").performTouchInput { up() }
        compose.waitForIdle()
        val movedToEnd = q.correctOrder.drop(1) + q.correctOrder.first()
        compose.runOnIdle { assertEquals(movedToEnd, session.sentenceGame.sentenceTileIds) }

        drag(q.correctOrder.first(), q.correctOrder[1])
        compose.runOnIdle { assertEquals(q.correctOrder, session.sentenceGame.sentenceTileIds) }
        assertEquals(homes, poolPositions(q))
        saveRenderedScreenshot(compose.activity, "sentence-reordered-320")
    }

    @Test fun cancellingALiftPreservesTheSentenceAndNextTapStillWorks() {
        val q = question(10)
        val session = show(q, q.correctOrder)
        val source = boardPoint(q.correctOrder.first())
        val destination = boardPoint(q.correctOrder.last())
        compose.onNodeWithTag("sentence_board").performTouchInput {
            down(source)
            advanceEventTime(650)
            moveTo(destination, delayMillis = 32)
            cancel()
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(q.correctOrder, session.sentenceGame.sentenceTileIds) }
        tap("assembled_${q.correctOrder[2]}")
        compose.runOnIdle {
            assertEquals(q.correctOrder.filterIndexed { index, _ -> index != 2 }, session.sentenceGame.sentenceTileIds)
        }
    }

    @Test fun holdingWithoutMovingNeverReturnsTheWordAndNextTapWorks() {
        val q = question(2)
        val session = show(q, q.correctOrder)
        val first = q.correctOrder.first()
        val source = boardPoint(first)
        compose.onNodeWithTag("sentence_board").performTouchInput {
            down(source)
            advanceEventTime(650)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("A completed hold is not a remove tap", q.correctOrder, session.sentenceGame.sentenceTileIds)
        }
        tap("assembled_$first")
        compose.runOnIdle { assertEquals(q.correctOrder.drop(1), session.sentenceGame.sentenceTileIds) }
    }

    @Test fun holdingAtTheScrollEdgeReachesOffscreenWordsAndCancellationKeepsOrder() {
        val q = question(10)
        val session = show(q, q.correctOrder, fontScale = 2f)
        val first = q.correctOrder.first()
        val last = q.correctOrder.last()
        val scrollNode = compose.onNodeWithTag("question_scroll")
        val viewport = scrollNode.fetchSemanticsNode().boundsInRoot
        val initialLast = compose.onNodeWithTag("assembled_$last").fetchSemanticsNode()
        assertTrue("Large-text fixture must require edge scrolling",
            initialLast.positionInRoot.y + initialLast.size.height > viewport.bottom)

        fun scrollPosition() = scrollNode.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()

        fun beginEdgeDrag(tileId: String) {
            val source = boardPoint(tileId)
            val boardOrigin = compose.onNodeWithTag("sentence_board").fetchSemanticsNode().positionInRoot
            val edge = Offset(source.x, viewport.bottom - boardOrigin.y - 12f)
            compose.onNodeWithTag("sentence_board").performTouchInput {
                down(source)
                advanceEventTime(650)
                moveTo(edge, delayMillis = 32)
            }
        }

        val beforeScroll = scrollPosition()
        compose.mainClock.autoAdvance = false
        beginEdgeDrag(first)
        var lastWordReached = false
        repeat(100) {
            if (!lastWordReached) {
                compose.mainClock.advanceTimeBy(32)
                compose.waitForIdle()
                val node = compose.onNodeWithTag("assembled_$last").fetchSemanticsNode()
                lastWordReached = node.positionInRoot.y >= viewport.top &&
                    node.positionInRoot.y + node.size.height <= viewport.bottom
            }
        }
        assertTrue("The held edge drag must bring the last word into view", lastWordReached)
        assertTrue("Scrolling must advance while the pointer remains held", scrollPosition() > beforeScroll)
        compose.runOnIdle { assertEquals(q.correctOrder, session.sentenceGame.sentenceTileIds) }
        // Let the last insertion settle after the row enters the viewport, without another move event.
        compose.mainClock.advanceTimeBy(128)
        compose.waitForIdle()
        saveRenderedScreenshot(compose.activity, "sentence-edge-drag-large-text")
        compose.onNodeWithTag("sentence_board").performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val reordered = q.correctOrder.drop(1) + first
        compose.runOnIdle { assertEquals(reordered, session.sentenceGame.sentenceTileIds) }

        compose.onNodeWithTag("assembled_${reordered.first()}").performScrollTo()
        val beforeCancelScroll = scrollPosition()
        compose.mainClock.autoAdvance = false
        beginEdgeDrag(reordered.first())
        repeat(12) {
            compose.mainClock.advanceTimeBy(32)
            compose.waitForIdle()
        }
        assertTrue("Cancellation is exercised during an actual edge scroll", scrollPosition() > beforeCancelScroll)
        compose.onNodeWithTag("sentence_board").performTouchInput { cancel() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(reordered, session.sentenceGame.sentenceTileIds) }
    }

    @Test fun incompleteSubmissionLocksTilesAndShowsContinueWithoutMovingTheAction() {
        val q = question(3)
        val incomplete = q.correctOrder.dropLast(1)
        val session = show(q, incomplete)
        val actionBefore = compose.onNodeWithTag("lesson_action").fetchSemanticsNode().boundsInRoot
        val homes = poolPositions(q)
        tap("lesson_action")
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(incomplete, session.sentenceGame.sentenceTileIds)
            assertEquals(SentenceValidation.Missing, session.sentenceGame.validation)
            assertTrue(session.checked)
            assertEquals(listOf(0), session.state.mistakes)
        }
        compose.onNodeWithTag("lesson_feedback").assertIsDisplayed()
        incomplete.forEach { compose.onNodeWithTag("assembled_$it").assertIsNotEnabled() }
        compose.onNodeWithText("Incorrect").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertTextContains("CONTINUE").assertIsEnabled()
        assertEquals(actionBefore, compose.onNodeWithTag("lesson_action").fetchSemanticsNode().boundsInRoot)
        assertEquals(homes, poolPositions(q))
    }
    @Test fun visuallyIdenticalParticlesAreAcceptedWithoutMergingTheirIdentities() {
        val q = question(4)
        val particles = q.tiles.filter { (it.text as LessonText.Japanese).value.kana == "を" }
        val expectedParticle = particles.single { it.id in q.correctOrder }
        val equivalentParticle = particles.single { it.id !in q.correctOrder }
        val answer = q.correctOrder.map { if (it == expectedParticle.id) equivalentParticle.id else it }
        val session = show(q, answer)
        compose.onNodeWithTag("tile_${expectedParticle.id}").assertIsEnabled()
        compose.onNodeWithTag("assembled_${equivalentParticle.id}").assertIsEnabled()
        tap("lesson_action")
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(session.checked)
            assertEquals(true, session.correct)
            assertEquals(answer, session.sentenceGame.sentenceTileIds)
            assertTrue(expectedParticle.id in session.sentenceGame.availableTileIds)
        }
        compose.onNodeWithText("Correct!").assertIsDisplayed()
    }

    @Test fun largeTextAndRtlKeepEveryAuthoredTileAndCheckReachable() {
        val q = question(10)
        val session = show(q, fontScale = 2f, rtl = true)
        val screen = compose.onNodeWithTag("sentence_test_screen").fetchSemanticsNode().boundsInRoot
        q.tiles.forEach { tile ->
            val bounds = compose.onNodeWithTag("tile_${tile.id}").performScrollTo().assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            assertTrue("${tile.id} left edge", bounds.left >= screen.left)
            assertTrue("${tile.id} right edge", bounds.right <= screen.right)
        }
        val selected = q.correctOrder.dropLast(1)
        selected.forEach { id ->
            compose.onNodeWithTag("tile_$id").performScrollTo()
            tap("tile_$id")
        }
        compose.runOnIdle { assertEquals(selected, session.sentenceGame.sentenceTileIds) }
        compose.onNodeWithTag("lesson_action").assertIsDisplayed().assertIsEnabled()
        tap("lesson_action")
        compose.waitForIdle()
        compose.onNodeWithTag("lesson_feedback").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsDisplayed().assertTextContains("CONTINUE")
        saveRenderedScreenshot(compose.activity, "sentence-large-text-rtl")
    }
}
