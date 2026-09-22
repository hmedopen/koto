package com.koto.app

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
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
class KotoShellTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshLaunchHasExactlyThreeOrderedTabsAndLearnSelected() {
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertCountEquals(3)
        compose.onNodeWithTag("tab_learn").assertIsSelected()
        compose.onNodeWithTag("tab_map").assertIsNotSelected()
        compose.onNodeWithTag("tab_cards").assertIsNotSelected()
        compose.onNodeWithTag("screen_learn").assertIsDisplayed()

        val map = bounds("tab_map")
        val learn = bounds("tab_learn")
        val cards = bounds("tab_cards")
        assertTrue(map.center.x < learn.center.x && learn.center.x < cards.center.x)
        assertEquals(bounds("bottom_bar").center.x, learn.center.x, 1f)
        screenshot("learn-phone")
    }

    @Test
    fun iconStateChangesImmediatelyWithTheDestination() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("tab_map").performClick()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onNodeWithTag("screen_map").assertIsDisplayed()
        compose.onNodeWithTag("tab_map").assertIsSelected()
    }

    @Test
    fun rapidSwitchingInterruptsMotionAndSettlesAtTheFinalTarget() {
        compose.mainClock.autoAdvance = false
        repeat(3) {
            listOf("map", "learn", "cards", "learn", "map").forEach { tab ->
                compose.onNodeWithTag("tab_$tab").performClick()
                compose.mainClock.advanceTimeBy(32)
            }
        }
        compose.onNodeWithTag("tab_cards").performClick()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onNodeWithTag("tab_cards").assertIsSelected()
        compose.onNodeWithTag("screen_cards").assertIsDisplayed()
        compose.onNodeWithTag("tab_map").assertIsNotSelected()
        screenshot("cards-phone")
    }

    @Test
    fun cancelledPressDoesNotSelectAnotherDestination() {
        compose.onNodeWithTag("tab_map").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("tab_learn").assertIsSelected()
        compose.onNodeWithTag("tab_map").performTouchInput { cancel() }
        compose.waitForIdle()
        compose.onNodeWithTag("tab_learn").assertIsSelected()
        compose.onNodeWithTag("screen_learn").assertIsDisplayed()
    }

    @Test
    fun recreationPreservesTheCurrentTaskButANewTaskStartsOnLearn() {
        compose.onNodeWithTag("tab_cards").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("tab_cards").assertIsSelected()

        compose.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("tab_learn").assertIsSelected()
            compose.onNodeWithTag("screen_learn").assertIsDisplayed()
        }
    }

    @Test
    fun backReturnsToLearnWithoutReplayingTabHistory() {
        listOf("cards", "map", "cards").forEach {
            compose.onNodeWithTag("tab_$it").performClick()
        }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("tab_learn").assertIsSelected()
        compose.onNodeWithTag("screen_learn").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun compactWidthKeepsComfortableTargetsAndFullLabels() {
        listOf("map", "learn", "cards").forEach { tab ->
            val node = compose.onNodeWithTag("tab_$tab").assertIsDisplayed().fetchSemanticsNode()
            val minPixels = with(compose.density) { androidx.compose.ui.unit.Dp(48f).toPx() }
            assertTrue(node.boundsInRoot.width >= minPixels)
            assertTrue(node.boundsInRoot.height >= minPixels)
            compose.onNodeWithTag("tab_$tab").performClick().assertIsSelected()
        }
        screenshot("cards-compact")
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
    fun landscapeKeepsTheBarVisibleAndCentered() {
        compose.onNodeWithTag("screen_learn").assertIsDisplayed()
        compose.onNodeWithTag("tab_map").performClick()
        compose.onNodeWithTag("screen_map").assertIsDisplayed()
        screenshot("map-landscape")
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot

    private fun screenshot(name: String) {
        compose.waitForIdle()
        saveRenderedScreenshot(compose.activity, name)
    }
}
