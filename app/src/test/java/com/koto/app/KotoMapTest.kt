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
        listOf(1 to "Completed", 2 to "Current level", 3 to "Available", 4 to "Locked").forEach { (number, state) ->
            compose.onNodeWithTag("map_list").performScrollToNode(hasTestTag("level_$number"))
            val node = compose.onNodeWithTag("level_$number").assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))
                .fetchSemanticsNode()
            assertEquals(center, node.boundsInRoot.center.x, 1f)
        }
        compose.onNodeWithTag("level_12").assertDoesNotExist()
        compose.onNodeWithTag("map_list").performScrollToNode(hasTestTag("level_12"))
        compose.onNodeWithTag("level_12").assertIsDisplayed()
        compose.onNodeWithTag("level_1").assertDoesNotExist()
        compose.onNodeWithTag("map_list").performScrollToIndex(0)
        saveRenderedScreenshot(compose.activity, "map-v2-phone")
    }

    @Test fun replayShowsKanaAndOnlyReturnsPlaceholderFeedback() {
        openMap()
        compose.onNodeWithTag("level_1").performClick()
        compose.onNodeWithTag("level_popup").assertIsDisplayed()
        compose.onNodeWithText("Level 01").assertIsDisplayed()
        compose.onNodeWithText("Replay").assertIsDisplayed()
        val romaji = compose.onNodeWithText("a  i  u  e  o").fetchSemanticsNode().boundsInRoot
        val kana = compose.onNodeWithTag("kana_preview").fetchSemanticsNode().boundsInRoot
        assertTrue(romaji.bottom <= kana.top)
        compose.onNodeWithTag("level_action").performClick()
        compose.onNodeWithTag("level_popup").assertDoesNotExist()
        compose.onNodeWithText("Preview only. Lessons will be added later.").assertIsDisplayed()
        compose.onNodeWithTag("level_1").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Completed"))
    }

    @Test fun lockedLevelCanBeInspectedButCannotPlay() {
        openMap()
        compose.onNodeWithTag("map_list").performScrollToNode(hasTestTag("level_4"))
        compose.onNodeWithTag("level_4").performClick()
        compose.onNodeWithText("Level 04").assertIsDisplayed()
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

    @Test fun settingsControlsAreInteractiveAndEphemeral() {
        openMap()
        compose.onNodeWithTag("map_settings").performClick()
        compose.onNodeWithTag("settings_sheet").assertIsDisplayed()
        compose.onNodeWithTag("settings_dropdown").performClick()
        compose.onNodeWithText("Option B").performClick()
        compose.onNodeWithText("Option B").assertIsDisplayed()
        compose.onNodeWithTag("settings_sound").assertIsOn().performClick().assertIsOff()
        compose.onNodeWithTag("settings_reminders").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("settings_slider").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(.8f) }
        compose.onNodeWithTag("settings_done").performClick()
        compose.onNodeWithTag("settings_sheet").assertDoesNotExist()
        compose.onNodeWithTag("map_settings").performClick()
        compose.onNodeWithTag("settings_sound").assertIsOn()
        compose.onNodeWithText("Option A").assertIsDisplayed()
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
