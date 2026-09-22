package com.koto.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KotoMapTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun openMap() { compose.onNodeWithTag("tab_map").performClick(); compose.waitForIdle() }
    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test fun progressionIsCenteredLazyAndShowsAllStates() {
        openMap()
        val center = compose.onNodeWithTag("map_list").fetchSemanticsNode().boundsInRoot.center.x
        listOf(1 to "Current level", 2 to "Available", 3 to "Available", 11 to "Locked").forEach { (number, state) ->
            compose.onNodeWithTag("map_list").performScrollToNode(hasTestTag("level_$number"))
            val node = compose.onNodeWithTag("level_$number").assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))
                .fetchSemanticsNode()
            assertEquals(center, node.boundsInRoot.center.x, 1f)
        }
        compose.onNodeWithTag("map_list").performScrollToIndex(0)
        compose.onNodeWithTag("level_12").assertDoesNotExist()
        compose.onNodeWithTag("map_list").performScrollToNode(hasTestTag("level_12"))
        compose.onNodeWithTag("level_12").assertIsDisplayed()
        compose.onNodeWithTag("level_1").assertDoesNotExist()
        compose.onNodeWithTag("map_list").performScrollToIndex(0)
        saveRenderedScreenshot(compose.activity, "map-v2-phone")
    }

    @Test fun playableLevelOpensFocusedLesson() {
        openMap()
        compose.onNodeWithTag("level_1").performClick()
        compose.onNodeWithText("Greetings · 6 questions").assertIsDisplayed()
        compose.onNodeWithTag("level_action").performClick()
        compose.onNodeWithTag("lesson_screen").assertIsDisplayed()
        compose.onNodeWithTag("bottom_bar").assertDoesNotExist()
    }
    @Test fun lockedLevelCanBeInspectedButCannotPlay() {
        openMap()
        compose.onNodeWithTag("map_list").performScrollToNode(hasTestTag("level_11"))
        compose.onNodeWithTag("level_11").performClick()
        compose.onNodeWithText("Level 11").assertIsDisplayed()
        compose.onNodeWithTag("level_action").assertIsNotEnabled()
        compose.onNodeWithTag("kana_preview").assertDoesNotExist()
        compose.onNodeWithContentDescription("Close level preview").performClick()
        compose.onNodeWithTag("level_popup").assertDoesNotExist()
        compose.onNodeWithTag("tab_map").assertIsSelected()
    }

    @Test fun popupBackDismissesBeforeShellBackAndSurvivesRecreation() {
        openMap()
        compose.onNodeWithTag("level_2").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Level 02").assertIsDisplayed()
        compose.onNodeWithText("Play").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close level preview").performClick()
        compose.onNodeWithTag("level_popup").assertDoesNotExist()
        compose.onNodeWithTag("tab_map").assertIsSelected()
        back()
        compose.onNodeWithTag("tab_learn").assertIsSelected()
    }

    @Test fun settingsAudioPreferencePersistsWhenReopened() {
        compose.runOnUiThread {
            com.koto.app.feature.lesson.audio.JapaneseTtsController.get(compose.activity).setSpeechEnabled(true)
        }
        openMap()
        compose.onNodeWithTag("map_settings").performClick()
        compose.onNodeWithTag("settings_sound").assertIsSelected().performClick().assertIsNotSelected()
        compose.onNodeWithTag("settings_done").performClick()
        compose.onNodeWithTag("map_settings").performClick()
        compose.onNodeWithTag("settings_sound").assertIsNotSelected()
    }
    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun compactMapKeepsOptionalShapesAndTouchTargets() {
        openMap()
        listOf("adjacent_1_Review", "adjacent_2_Mini", "adjacent_3_PhaseTwo", "adjacent_3_Special")
            .forEach { compose.onNodeWithTag(it).assertIsDisplayed() }
        val node = compose.onNodeWithTag("level_2").fetchSemanticsNode().boundsInRoot
        assertTrue(node.width >= with(compose.density) { androidx.compose.ui.unit.Dp(48f).toPx() })
        saveRenderedScreenshot(compose.activity, "map-v2-compact")
        compose.onNodeWithTag("level_2").performClick()
        compose.onNodeWithTag("level_action").assertIsDisplayed()
    }
}
