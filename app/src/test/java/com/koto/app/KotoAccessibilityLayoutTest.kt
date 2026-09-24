package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.koto.app.ui.KotoApp
import com.koto.app.ui.components.KotoBottomBar
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.theme.KotoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KotoAccessibilityLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mapPopupSupportsDoubleFontScale() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        compose.setContent { KotoTheme { KotoApp() } }
        compose.onNodeWithTag("tab_map").performClick()
        compose.onNodeWithTag("level_2").assertIsDisplayed().performClick()
        compose.runOnIdle { saveOverlayScreenshot("map-v2-popup-large-text") }
        compose.onNodeWithText("First Words · 7 questions")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                val results = mutableListOf<TextLayoutResult>()
                assertTrue(getLayout(results))
                assertTrue("The expectation must wrap without clipping", results.none { it.hasVisualOverflow })
            }
        compose.onNodeWithTag("level_action").performScrollTo().assertIsDisplayed()

    }

    @Test
    fun doubleFontScaleFitsLabelsAndTheBarGrowsWithText() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                KotoTheme { KotoApp() }
            }
        }
        listOf("map", "learn", "cards").forEach {
            compose.onNodeWithTag("tab_$it").assertIsDisplayed().performClick().assertIsSelected()
            compose.onNode(
                hasText(it.replaceFirstChar(Char::titlecase)) and hasAnyAncestor(hasTestTag("tab_$it")),
                useUnmergedTree = true,
            ).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                val results = mutableListOf<TextLayoutResult>()
                assertTrue(getLayout(results))
                assertTrue(results.none { result -> result.hasVisualOverflow })
            }
        }
        val bar = compose.onNodeWithTag("bottom_bar").fetchSemanticsNode().boundsInRoot
        val cards = compose.onNodeWithTag("tab_cards").fetchSemanticsNode().boundsInRoot
        assertTrue(cards.bottom <= bar.bottom)
        saveRenderedScreenshot(compose.activity, "cards-large-text")
    }

    @Test
    fun tabSlotsRemainEqualInRtl() {
        compose.setContent {
            val selected = remember { mutableStateOf(KotoDestination.Learn) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KotoTheme { KotoBottomBar(selected.value, { selected.value = it }) }
            }
        }
        val bounds = listOf("map", "learn", "cards").map { tab ->
            compose.onNodeWithTag("tab_$tab").performClick().assertIsSelected()
            compose.onNodeWithTag("tab_$tab").fetchSemanticsNode().boundsInRoot
        }
        assertEquals(bounds[0].width, bounds[1].width, 1f)
        assertEquals(bounds[1].width, bounds[2].width, 1f)
    }
}
