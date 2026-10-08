package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.koto.app.ui.screens.settings.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdvancedSettingsScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun topBarDisplaysTitleAndBackNavigation() {
        var backed = false
        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(),
                snackbarHostState = SnackbarHostState(),
                onBack = { backed = true },
                onFuriganaChange = {},
                onRomajiChange = {},
                onJapaneseFontChange = {},
                onEnglishFontChange = {},
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = {},
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = {},
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = {},
                onOpenUrl = {},
            )
        }

        compose.onNodeWithTag("advanced_settings_title").assertIsDisplayed().assertTextEquals("Advanced Settings")
        compose.onNodeWithTag("advanced_settings_back").assertIsDisplayed().performClick()
        assertTrue("Back callback should be invoked", backed)
    }

    @Test
    fun allCategoryHeadersAreDisplayed() {
        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(),
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onFuriganaChange = {},
                onRomajiChange = {},
                onJapaneseFontChange = {},
                onEnglishFontChange = {},
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = {},
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = {},
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = {},
                onOpenUrl = {},
            )
        }

        compose.onNodeWithText("DISPLAY & TYPOGRAPHY").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("AUDIO & SPEECH").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("STUDY & CARD INTERACTION").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("CUSTOM DECK CREATION").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("STORAGE & DATA").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ABOUT & LEGAL").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun displayTogglesAndFontBadgesWork() {
        var furiganaState by mutableStateOf(true)
        var romajiState by mutableStateOf(false)
        var jpFontId by mutableStateOf("noto_sans_jp")
        var enFontId by mutableStateOf("inter_roboto")

        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(
                    furiganaDisplay = furiganaState,
                    romajiDisplay = romajiState,
                    japaneseFontId = jpFontId,
                    englishFontId = enFontId,
                ),
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onFuriganaChange = { furiganaState = it },
                onRomajiChange = { romajiState = it },
                onJapaneseFontChange = { jpFontId = it },
                onEnglishFontChange = { enFontId = it },
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = {},
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = {},
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = {},
                onOpenUrl = {},
            )
        }

        // Toggle furigana to OFF
        compose.onNodeWithTag("toggle_furigana_display").performScrollTo().assertIsDisplayed().performClick()
        assertFalse(furiganaState)

        // Open Japanese font picker dialog
        compose.onNodeWithTag("badge_japanese_font").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("font_picker_dialog").assertIsDisplayed()

        // Select M PLUS 1p
        compose.onNodeWithTag("font_item_m_plus_1p").assertIsDisplayed().performClick()
        assertEquals("m_plus_1p", jpFontId)
        compose.onNodeWithTag("font_picker_dialog").assertDoesNotExist()

        // Open English font picker dialog
        compose.onNodeWithTag("badge_english_font").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("font_picker_dialog").assertIsDisplayed()

        // Close via [X]
        compose.onNodeWithTag("font_picker_close").assertIsDisplayed().performClick()
        compose.onNodeWithTag("font_picker_dialog").assertDoesNotExist()
    }

    @Test
    fun dynamicVoicePackWarningCardVisibility() {
        var warningClicked = false

        // 1. When TTS is ON and voice is missing -> warning card shown
        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(
                    textToSpeech = true,
                    isJapaneseVoiceAvailable = false,
                ),
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onFuriganaChange = {},
                onRomajiChange = {},
                onJapaneseFontChange = {},
                onEnglishFontChange = {},
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = { warningClicked = true },
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = {},
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = {},
                onOpenUrl = {},
            )
        }

        compose.onNodeWithTag("card_voice_pack_warning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("button_install_voice_pack").performScrollTo().assertIsDisplayed().performClick()
        assertTrue("Install voice pack callback should be invoked", warningClicked)
    }

    @Test
    fun voicePackWarningHiddenWhenVoiceAvailableOrTtsOff() {
        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(
                    textToSpeech = true,
                    isJapaneseVoiceAvailable = true,
                ),
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onFuriganaChange = {},
                onRomajiChange = {},
                onJapaneseFontChange = {},
                onEnglishFontChange = {},
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = {},
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = {},
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = {},
                onOpenUrl = {},
            )
        }

        compose.onNodeWithTag("card_voice_pack_warning").assertDoesNotExist()
    }

    @Test
    fun retryFailedCardsSelectorSwitchesBetweenSoonAndEnd() {
        var currentPolicy by mutableStateOf(RetryFailedCardsPolicy.SOON)

        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(retryFailedCards = currentPolicy),
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onFuriganaChange = {},
                onRomajiChange = {},
                onJapaneseFontChange = {},
                onEnglishFontChange = {},
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = {},
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = { currentPolicy = it },
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = {},
                onOpenUrl = {},
            )
        }

        compose.onNodeWithTag("toggle_retry_failed").performScrollTo().assertIsDisplayed()
        compose.onNode(hasAnyAncestor(hasTestTag("toggle_retry_failed")) and hasText("LATER")).performClick()
        assertEquals(RetryFailedCardsPolicy.END, currentPolicy)

        compose.onNode(hasAnyAncestor(hasTestTag("toggle_retry_failed")) and hasText("SOON")).performClick()
        assertEquals(RetryFailedCardsPolicy.SOON, currentPolicy)
    }

    @Test
    fun clearCacheDisplaysFormattedSizeAndInvokesCallback() {
        var clearCacheCalled = false

        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(cacheSizeBytes = 44879052L), // ~42.8 MB
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onFuriganaChange = {},
                onRomajiChange = {},
                onJapaneseFontChange = {},
                onEnglishFontChange = {},
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = {},
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = {},
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = { clearCacheCalled = true },
                onOpenUrl = {},
            )
        }

        compose.onNodeWithTag("row_clear_cache").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("text_clear_cache_size", useUnmergedTree = true).assertIsDisplayed().assertTextEquals("42.8 MB")
        compose.onNodeWithTag("row_clear_cache").performClick()
        assertTrue("onClearCache should be called", clearCacheCalled)
    }

    @Test
    fun aboutAndLegalRowsDisplayVersionAndUrls() {
        var openedUrl: String? = null

        compose.setContent {
            AdvancedSettingsContent(
                state = AdvancedSettingsState(),
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onFuriganaChange = {},
                onRomajiChange = {},
                onJapaneseFontChange = {},
                onEnglishFontChange = {},
                onTtsChange = {},
                onSpeechSpeedChange = {},
                onInstallVoicePack = {},
                onRevealFuriganaChange = {},
                onKanjiLookupChange = {},
                onCardFlipChange = {},
                onRetryPolicyChange = {},
                onAutoFillChange = {},
                onAutoGenerateFuriganaChange = {},
                onClearCache = {},
                onOpenUrl = { openedUrl = it },
            )
        }

        compose.onNodeWithText("v1.0.0").performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("row_privacy_policy").performScrollTo().performClick()
        assertEquals("https://koto.app/privacy", openedUrl)

        compose.onNodeWithTag("row_terms_of_service").performScrollTo().performClick()
        assertEquals("https://koto.app/terms", openedUrl)
    }
}
