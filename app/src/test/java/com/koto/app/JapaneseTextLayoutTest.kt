package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.koto.app.ui.components.JapaneseWordDisplay
import com.koto.app.ui.components.RubyText
import com.koto.app.ui.screens.settings.DisplayMode
import com.koto.app.ui.screens.settings.RubyToken
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JapaneseTextLayoutTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rubyText_rendersKanaInKanaMode() {
        val tokens = listOf(
            RubyToken(surface = "私", reading = "わたし"),
            RubyToken(surface = "は", reading = null),
            RubyToken(surface = "水", reading = "みず"),
        )

        compose.setContent {
            RubyText(
                tokens = tokens,
                mode = DisplayMode.KANA,
            )
        }

        // In Kana mode, readings should be displayed instead of Kanji surfaces
        compose.onNodeWithText("わたし").assertIsDisplayed()
        compose.onNodeWithText("は").assertIsDisplayed()
        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("私").assertDoesNotExist()
        compose.onNodeWithText("水").assertDoesNotExist()
    }

    @Test
    fun rubyText_rendersFuriganaAndKanjiInFuriganaMode() {
        val tokens = listOf(
            RubyToken(surface = "水", reading = "みず"),
        )

        compose.setContent {
            RubyText(
                tokens = tokens,
                mode = DisplayMode.KANJI_FURIGANA,
            )
        }

        // In Furigana mode, both the reading and the kanji compound should be displayed
        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("水").assertIsDisplayed()
    }

    @Test
    fun rubyText_rendersOnlyKanjiInKanjiOnlyMode() {
        val tokens = listOf(
            RubyToken(surface = "水", reading = "みず"),
        )

        compose.setContent {
            RubyText(
                tokens = tokens,
                mode = DisplayMode.KANJI_ONLY,
            )
        }

        // In Kanji Only mode, only the kanji compound should be displayed
        compose.onNodeWithText("水").assertIsDisplayed()
        compose.onNodeWithText("みず").assertDoesNotExist()
    }

    @Test
    fun japaneseWordDisplay_respectsRomajiToggle() {
        val showRomaji = androidx.compose.runtime.mutableStateOf(true)
        compose.setContent {
            JapaneseWordDisplay(
                kanji = "水",
                kana = "みず",
                romaji = "mizu",
                mode = DisplayMode.KANA,
                showRomaji = showRomaji.value,
            )
        }

        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("mizu").assertIsDisplayed()

        // Toggle Romaji off
        showRomaji.value = false
        compose.waitForIdle()

        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("mizu").assertDoesNotExist()
    }
}
