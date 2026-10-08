package com.koto.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.ui.screens.map.SettingsSheet
import com.koto.app.ui.screens.settings.AdvancedSettingsScreen
import com.koto.app.ui.screens.settings.DisplayMode
import com.koto.app.ui.screens.settings.DisplayPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsSheetTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var preferences: DisplayPreferences
    private lateinit var audio: JapaneseTtsController

    @Before
    fun setUp() {
        val context = compose.activity
        preferences = DisplayPreferences.get(context)
        audio = JapaneseTtsController.get(context)
        // Reset defaults
        preferences.setDisplayMode(DisplayMode.KANA)
        preferences.setRomajiEnabled(true)
        audio.setSpeechEnabled(true)
    }

    @Test
    fun headerDisplaysCenteredTitleAndCloseButton() {
        var dismissed = false
        compose.setContent {
            SettingsSheet(
                onDismiss = { dismissed = true },
                audio = audio,
                preferences = preferences,
            )
        }

        compose.onNodeWithTag("settings_title").assertIsDisplayed().assertTextEquals("Settings")
        compose.onNodeWithTag("settings_close").assertIsDisplayed().performClick()
        assertTrue("Dismiss callback should be called", dismissed)
    }

    @Test
    fun japaneseDisplayPreferenceCardsSwitchBetweenKanaAndKanji() {
        compose.setContent {
            SettingsSheet(
                onDismiss = {},
                audio = audio,
                preferences = preferences,
            )
        }

        // Section title
        compose.onNodeWithText("How do you prefer to display Japanese text?").assertIsDisplayed()

        // Initial state: Kana is selected
        compose.onNodeWithTag("settings_display_kana").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("settings_display_kanji").assertIsDisplayed().assertIsNotSelected()
        assertEquals(DisplayMode.KANA, preferences.displayMode)

        // Select Kanji
        compose.onNodeWithTag("settings_display_kanji").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("settings_display_kanji").assertIsSelected()
        compose.onNodeWithTag("settings_display_kana").assertIsNotSelected()
        assertEquals(DisplayMode.KANJI_FURIGANA, preferences.displayMode)

        // Select Kana again
        compose.onNodeWithTag("settings_display_kana").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("settings_display_kana").assertIsSelected()
        compose.onNodeWithTag("settings_display_kanji").assertIsNotSelected()
        assertEquals(DisplayMode.KANA, preferences.displayMode)
    }

    @Test
    fun romajiToggleTogglesStateAndPersists() {
        compose.setContent {
            SettingsSheet(
                onDismiss = {},
                audio = audio,
                preferences = preferences,
            )
        }

        // Initially Romaji is enabled
        compose.onNodeWithTag("settings_romaji").assertIsDisplayed()
        assertTrue(preferences.romajiEnabled)

        // Toggle to Off
        compose.onNodeWithTag("settings_romaji").performClick()
        compose.waitForIdle()

        assertFalse(preferences.romajiEnabled)

        // Toggle back to On
        compose.onNodeWithTag("settings_romaji").performClick()
        compose.waitForIdle()

        assertTrue(preferences.romajiEnabled)
    }

    @Test
    fun advancedButtonTriggersDismissAndOpenAdvanced() {
        var dismissed = false
        var advancedOpened = false

        compose.setContent {
            SettingsSheet(
                onDismiss = { dismissed = true },
                onOpenAdvanced = { advancedOpened = true },
                audio = audio,
                preferences = preferences,
            )
        }

        compose.onNodeWithTag("settings_advanced").assertIsDisplayed().performClick()
        assertTrue("Dismiss should be called", dismissed)
        assertTrue("Open advanced should be called", advancedOpened)
    }

    @Test
    fun advancedSettingsScreenScaffoldDisplaysTopBarAndCleanSurface() {
        var backCalled = false

        compose.setContent {
            AdvancedSettingsScreen(onBack = { backCalled = true })
        }

        compose.onNodeWithTag("advanced_settings_screen").assertIsDisplayed()
        compose.onNodeWithTag("advanced_settings_title").assertIsDisplayed().assertTextEquals("Advanced Settings")
        compose.onNodeWithTag("advanced_settings_back").assertIsDisplayed().performClick()
        assertTrue("Back callback should be invoked", backCalled)
    }
}
