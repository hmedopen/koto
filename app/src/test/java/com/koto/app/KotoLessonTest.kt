package com.koto.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.koto.app.feature.lesson.data.PrototypeLessons
import com.koto.app.feature.lesson.model.Question
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
class KotoLessonTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun start(level: Int) {
        compose.onNodeWithTag("tab_map").performClick()
        compose.onNodeWithTag("map_list").performScrollToNode(hasTestTag("level_$level"))
        compose.onNodeWithTag("level_$level").performClick()
        compose.onNodeWithTag("level_action").performClick()
        compose.onNodeWithTag("lesson_screen").assertIsDisplayed()
        compose.onNodeWithTag("bottom_bar").assertDoesNotExist()
        compose.onNodeWithTag("map_settings").assertDoesNotExist()
    }
    private fun solve(q: Question) {
        when (q) {
            is Question.MeaningChoice -> compose.onNodeWithTag("answer_${q.correctId}").performClick()
            is Question.ConversationResponse -> compose.onNodeWithTag("answer_${q.correctId}").performClick()
            is Question.Cloze -> {
                compose.onNodeWithTag("lesson_action").assertIsNotEnabled()
                compose.onNodeWithTag("answer_${q.correctId}").performClick()
                compose.onNodeWithTag("lesson_action").assertIsEnabled().performClick()
            }
            is Question.SentenceBuilder -> {
                compose.onNodeWithTag("lesson_action").assertIsNotEnabled()
                q.correctOrder.forEach { compose.onNodeWithTag("tile_$it").performClick() }
                compose.onNodeWithTag("lesson_action").performClick()
            }
            is Question.PairMatch -> q.pairs.forEach {
                compose.onNodeWithTag("pair_ja_${it.id}").performClick()
                compose.onNodeWithTag("pair_en_${it.id}").performClick()
            }
        }
        if (q !is Question.PairMatch) {
            compose.onNodeWithText("Correct!").assertIsDisplayed()
            compose.onNodeWithTag("lesson_action").performClick()
        } else {
            compose.mainClock.advanceTimeBy(500)
        }
        compose.waitForIdle()
    }

    @Test fun allFiveGamesCompleteOnNarrowPhoneAndReplayResets() {
        start(3)
        val captured = mutableSetOf<String>()
        PrototypeLessons.lesson(3)!!.questions.forEach { q ->
            compose.onNodeWithTag("question_${q.id}").assertIsDisplayed()
            // All normal question content fits the compact viewport without scrolling.
            if (q !is Question.SentenceBuilder) {
                val range = compose.onNodeWithTag("question_scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
                assertEquals("${q.id} must fit without scrolling", 0f, range.maxValue(), 1f)
            }
            if (captured.add(q.javaClass.simpleName)) saveRenderedScreenshot(compose.activity, "lesson-${q.javaClass.simpleName}-320")
            solve(q)
        }
        compose.onNodeWithTag("lesson_complete").assertIsDisplayed()
        compose.onNodeWithText("6 / 6").assertIsDisplayed()
        saveRenderedScreenshot(compose.activity, "lesson-completion-320")
        compose.onNodeWithTag("lesson_replay").performClick()
        compose.onNodeWithTag("question_3-1").assertIsDisplayed()
        compose.onNodeWithTag("lesson_close").performClick()
        compose.onNodeWithTag("confirm_exit").performClick()
        compose.onNodeWithTag("level_3").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Completed"))
    }

    @Test fun settingsRotationAndExitConfirmationPreserveTheAttempt() {
        start(1)
        val q = PrototypeLessons.lesson(1)!!.questions.first() as Question.MeaningChoice
        val wrong = q.options.first { it.id != q.correctId }
        compose.onNodeWithTag("answer_${wrong.id}").performClick()
        compose.onNodeWithTag("lesson_settings").performClick()
        compose.onNodeWithTag("settings_sound").performClick()
        compose.onNodeWithTag("settings_done").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("answer_${wrong.id}").assertIsSelected()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("cancel_exit").performClick()
        compose.onNodeWithTag("question_1-1").assertIsDisplayed()
        compose.onNodeWithText("Incorrect").assertIsDisplayed()
        compose.onNodeWithTag("answer_${q.correctId}").assertIsNotEnabled()
        compose.onNodeWithTag("lesson_action").performClick()
        compose.onNodeWithTag("question_1-2").assertIsDisplayed()
        compose.onNodeWithTag("lesson_close").performClick()
        compose.onNodeWithTag("confirm_exit").performClick()
        compose.onNodeWithTag("tab_map").assertIsSelected()
    }

    @Test fun longConversationAndFourPairsFitNarrowPhone() {
        start(10)
        PrototypeLessons.lesson(10)!!.questions.forEach { q ->
            if (q !is Question.SentenceBuilder) {
                val range = compose.onNodeWithTag("question_scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
                assertEquals("${q.id} fits", 0f, range.maxValue(), 1f)
            }
            if (q is Question.PairMatch) saveRenderedScreenshot(compose.activity, "lesson-four-pairs-320")
            solve(q)
        }
        compose.onNodeWithTag("lesson_map").performClick()
        compose.onNodeWithTag("tab_map").assertIsSelected()
        compose.onNodeWithTag("level_10").assertIsDisplayed()
    }

    @Test fun allSixtyAuthoredQuestionsFitAndCompleteThroughTheMap() {
        PrototypeLessons.lessons.forEach { lesson ->
            start(lesson.id)
            lesson.questions.forEach { q ->
                compose.onNodeWithTag("question_${q.id}").assertIsDisplayed()
                if (q !is Question.SentenceBuilder) {
                    val range = compose.onNodeWithTag("question_scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
                    assertEquals("Question ${q.id} fits at 320 dp", 0f, range.maxValue(), 1f)
                }
                solve(q)
            }
            compose.onNodeWithText("6 / 6").assertIsDisplayed()
            compose.onNodeWithTag("lesson_map").performClick()
        }
    }

    @Test fun builderSelectionSurvivesRecreationAndCanBeReturned() {
        start(2)
        val lesson = PrototypeLessons.lesson(2)!!
        lesson.questions.take(5).forEach(::solve)
        val builder = lesson.questions.last() as Question.SentenceBuilder
        val tile = builder.correctOrder.first()
        compose.onNodeWithTag("tile_$tile").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("assembled_$tile").assertIsDisplayed().performClick()
        compose.onNodeWithTag("assembled_$tile").assertDoesNotExist()
        compose.onNodeWithTag("tile_$tile").assertIsEnabled()
        solve(builder)
        compose.onNodeWithText("6 / 6").assertIsDisplayed()
    }

    @Test fun largeTextKeepsManualActionAndExitReachable() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        start(3)
        val questions = PrototypeLessons.lesson(3)!!.questions
        questions.take(2).forEach { q ->
            val choice = q as Question.MeaningChoice
            compose.onNodeWithTag("answer_${choice.correctId}").performScrollTo().performClick()
            compose.onNodeWithTag("lesson_action").assertIsDisplayed().performClick()
        }
        val cloze = questions[2] as Question.Cloze
        compose.onNodeWithTag("answer_${cloze.correctId}").performScrollTo().performClick()
        compose.onNodeWithTag("lesson_action").assertIsDisplayed().performClick()
        compose.onNodeWithTag("lesson_action").assertIsDisplayed()
        saveRenderedScreenshot(compose.activity, "lesson-large-text")
        compose.onNodeWithTag("lesson_close").performClick()
        compose.onNodeWithTag("confirm_exit").performClick()
        compose.onNodeWithTag("tab_map").assertIsSelected()
    }
}
