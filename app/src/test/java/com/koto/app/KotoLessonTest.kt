package com.koto.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.koto.app.feature.lesson.data.FoundationLessons
import com.koto.app.feature.lesson.data.LessonProgress
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
            is Question.MeaningChoice -> compose.onNodeWithTag("answer_${q.correctId}").performScrollTo().performClick()
            is Question.Listening -> compose.onNodeWithTag("answer_${q.correctId}").performScrollTo().performClick()
            is Question.ConversationResponse -> compose.onNodeWithTag("answer_${q.correctId}").performScrollTo().performClick()
            is Question.Cloze -> compose.onNodeWithTag("answer_${q.correctId}").performScrollTo().performClick()
            is Question.SentenceBuilder -> {
                compose.onNodeWithTag("lesson_action").assertIsNotEnabled()
                q.correctOrder.forEach {
                    compose.onNodeWithTag("tile_$it").performScrollTo().performClick()
                    compose.waitForIdle()
                }
            }
            is Question.PairMatch -> q.pairs.forEach {
                compose.onNodeWithTag("pair_ja_${it.id}").performScrollTo().performClick()
                compose.onNodeWithTag("pair_en_${it.id}").performScrollTo().performClick()
            }
        }
        if (q !is Question.PairMatch) compose.onNodeWithTag("lesson_action").assertIsEnabled().performClick()
        compose.onNodeWithText("Correct!").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsEnabled().assertTextContains("CONTINUE").performClick()
        compose.waitForIdle()
    }
    private fun complete(level: Int) {
        start(level)
        val lesson = FoundationLessons.lesson(level)!!
        lesson.questions.forEach { q ->
            compose.onNodeWithTag("question_${q.id}").assertIsDisplayed()
            solve(q)
        }
        compose.onNodeWithTag("lesson_complete").assertIsDisplayed()
        compose.onNodeWithText("${lesson.questions.size} / ${lesson.questions.size}").assertIsDisplayed()
        compose.runOnIdle { assertTrue(level in LessonProgress(compose.activity).completed) }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("lesson_complete").assertIsDisplayed()
        compose.onNodeWithTag("lesson_map").performClick()
        compose.onNodeWithTag("level_$level").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Completed"))
    }

    @Test fun level01CompletesAndSavesProgress() = complete(1)
    @Test fun level02CompletesAndSavesProgress() = complete(2)

    @Test fun allFiveGamesCompleteOnNarrowPhoneAndReplayResets() {
        start(12)
        val lesson = FoundationLessons.lesson(12)!!
        assertEquals(5, lesson.questions.map { it.javaClass }.distinct().size)
        val captured = mutableSetOf<String>()
        lesson.questions.forEach { q ->
            if (captured.add(q.javaClass.simpleName)) saveRenderedScreenshot(compose.activity, "foundation-${q.javaClass.simpleName}-320")
            solve(q)
        }
        compose.onNodeWithTag("lesson_complete").assertIsDisplayed()
        compose.onNodeWithTag("lesson_replay").performClick()
        compose.onNodeWithTag("question_${lesson.questions.first().id}").assertIsDisplayed()
        compose.onNodeWithTag("lesson_action").assertIsNotEnabled()
    }

    @Test fun settingsRotationAndExitConfirmationPreserveTheAttempt() {
        start(1)
        val q = FoundationLessons.lesson(1)!!.questions.first() as Question.MeaningChoice
        val wrong = q.options.first { it.id != q.correctId }
        compose.onNodeWithTag("answer_${wrong.id}").performClick()
        compose.onNodeWithTag("lesson_action").performClick()
        compose.onNodeWithTag("lesson_settings").performClick()
        compose.onNodeWithTag("settings_sound").performClick()
        compose.onNodeWithTag("settings_done").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("answer_${wrong.id}").assertIsSelected()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("cancel_exit").performClick()
        compose.onNodeWithTag("question_${q.id}").assertIsDisplayed()
        compose.onNodeWithText("Incorrect").assertIsDisplayed()
        compose.onNodeWithTag("answer_${q.correctId}").assertIsNotEnabled()
        compose.onNodeWithTag("lesson_action").performClick()
        compose.onNodeWithTag("question_L01_Q02").assertIsDisplayed()
        compose.onNodeWithTag("lesson_close").performClick()
        compose.onNodeWithTag("confirm_exit").performClick()
        compose.onNodeWithTag("tab_map").assertIsSelected()
    }

    @Test fun allAuthoredQuestionsCompleteThroughTheMap() {
        FoundationLessons.levels.forEach { complete(it.id) }
    }

    @Test fun builderSelectionSurvivesRecreationAndCanBeReturned() {
        start(2)
        val lesson = FoundationLessons.lesson(2)!!
        val index = lesson.questions.indexOfFirst { it is Question.SentenceBuilder }
        lesson.questions.take(index).forEach(::solve)
        val builder = lesson.questions[index] as Question.SentenceBuilder
        val tile = builder.correctOrder.first()
        compose.onNodeWithTag("tile_$tile").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("assembled_$tile").assertIsDisplayed().performClick()
        compose.onNodeWithTag("assembled_$tile").assertDoesNotExist()
        compose.onNodeWithTag("tile_$tile").assertIsEnabled()
        lesson.questions.drop(index).forEach(::solve)
        compose.onNodeWithTag("lesson_complete").assertIsDisplayed()
    }

    @Test fun largeTextKeepsManualActionAndExitReachable() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        start(4)
        val cloze = FoundationLessons.lesson(4)!!.questions.first() as Question.Cloze
        compose.onNodeWithTag("answer_${cloze.correctId}").performScrollTo().performClick()
        compose.onNodeWithTag("lesson_action").assertIsDisplayed().performClick()
        compose.onNodeWithTag("lesson_action").assertIsDisplayed()
        saveRenderedScreenshot(compose.activity, "foundation-large-text")
        compose.onNodeWithTag("lesson_close").performClick()
        compose.onNodeWithTag("confirm_exit").performClick()
        compose.onNodeWithTag("tab_map").assertIsSelected()
    }
}
