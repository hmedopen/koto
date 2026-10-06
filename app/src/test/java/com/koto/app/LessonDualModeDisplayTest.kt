package com.koto.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.data.FoundationLessons
import com.koto.app.feature.lesson.data.LessonDataLoader
import com.koto.app.feature.lesson.model.*
import com.koto.app.feature.lesson.ui.JapaneseTextBlock
import com.koto.app.feature.lesson.ui.QuestionRenderer
import com.koto.app.ui.screens.settings.DisplayMode
import com.koto.app.ui.screens.settings.LocalJapaneseDisplayMode
import com.koto.app.ui.screens.settings.LocalRomajiVisibility
import com.koto.app.ui.theme.KotoTheme
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
class LessonDualModeDisplayTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lessonModelsAndAssetsContainKanjiMetadataAndPreserveValidationAndTts() {
        // 1. Verify levels 1 through 12 load and validate with zero errors
        for (levelId in 1..12) {
            val lesson = FoundationLessons.lesson(levelId)
            assertNotNull("Lesson $levelId must load", lesson)
            lesson!!.validate() // Asserts noHan(kana), noHan(romaji), noHan(tts)

            // 2. Verify all Japanese questions have valid TTS and non-empty Kana
            lesson.questions.forEach { q ->
                when (q) {
                    is Question.MeaningChoice -> {
                        (q.prompt as? LessonText.Japanese)?.let {
                            assertTrue("TTS must be supported for prompt", JapaneseTtsController.canSpeak(it.value))
                        }
                        q.options.forEach { opt ->
                            (opt.text as? LessonText.Japanese)?.let {
                                assertTrue("TTS must be supported for option", JapaneseTtsController.canSpeak(it.value))
                            }
                        }
                    }
                    is Question.SentenceBuilder -> {
                        assertTrue("TTS must be supported for sentence", JapaneseTtsController.canSpeak(q.sentence))
                        q.tiles.forEach { tile ->
                            val jp = (tile.text as LessonText.Japanese).value
                            assertTrue("TTS must be supported for tile", JapaneseTtsController.canSpeak(jp))
                        }
                    }
                    is Question.Cloze -> {
                        assertTrue("TTS must be supported for cloze", JapaneseTtsController.canSpeak(q.sentence))
                        q.options.forEach { opt ->
                            val jp = (opt.text as LessonText.Japanese).value
                            assertTrue("TTS must be supported for cloze option", JapaneseTtsController.canSpeak(jp))
                        }
                    }
                    is Question.ConversationResponse -> {
                        assertTrue("TTS must be supported for incoming", JapaneseTtsController.canSpeak(q.incoming))
                    }
                    is Question.PairMatch -> {
                        q.pairs.forEach { pair ->
                            assertTrue("TTS must be supported for pair", JapaneseTtsController.canSpeak(pair.japanese))
                        }
                    }
                    is Question.Listening -> {
                        assertTrue("TTS must be supported for listening", JapaneseTtsController.canSpeak(q.target))
                    }
                }
            }
        }

        // 3. Verify specific vocabulary items resolve to native Kanji
        val water = LessonDataLoader.parseJapaneseChoice("みず")
        assertEquals("みず", water.kana)
        assertEquals("mizu", water.romaji)
        assertEquals("水", water.displayKanji)

        val student = LessonDataLoader.parseJapaneseChoice("がくせい")
        assertEquals("がくせい", student.kana)
        assertEquals("gakusei", student.romaji)
        assertEquals("学生", student.displayKanji)

        val teacher = LessonDataLoader.parseJapaneseChoice("せんせい")
        assertEquals("先生", teacher.displayKanji)

        val friend = LessonDataLoader.parseJapaneseChoice("ともだち")
        assertEquals("友達", friend.displayKanji)
    }

    @Test
    fun japaneseTextBlockRespectsDisplayModeAndRomaji() {
        val currentMode = mutableStateOf(DisplayMode.KANA)
        val showRomaji = mutableStateOf(true)

        val testWord = JapaneseText(
            kana = "みず",
            romaji = "mizu",
            tts = "みず",
            kanji = "水"
        )

        compose.setContent {
            CompositionLocalProvider(
                LocalJapaneseDisplayMode provides currentMode.value,
                LocalRomajiVisibility provides showRomaji.value,
            ) {
                KotoTheme {
                    Box(modifier = Modifier.testTag("block_root")) {
                        JapaneseTextBlock(text = testWord)
                    }
                }
            }
        }

        // 1. Initial State: KANA mode, showRomaji = true
        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("mizu").assertIsDisplayed()
        compose.onNodeWithText("水").assertDoesNotExist()

        // 2. Hide Romaji
        showRomaji.value = false
        compose.waitForIdle()
        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("mizu").assertDoesNotExist()

        // 3. Switch to KANJI_ONLY (Romaji still hidden)
        currentMode.value = DisplayMode.KANJI_ONLY
        compose.waitForIdle()
        compose.onNodeWithText("水").assertIsDisplayed()
        compose.onNodeWithText("みず").assertDoesNotExist()
        compose.onNodeWithText("mizu").assertDoesNotExist()

        // 4. Show Romaji in KANJI_ONLY mode
        showRomaji.value = true
        compose.waitForIdle()
        compose.onNodeWithText("水").assertIsDisplayed()
        compose.onNodeWithText("mizu").assertIsDisplayed()

        // 5. Switch to KANJI_FURIGANA mode
        currentMode.value = DisplayMode.KANJI_FURIGANA
        compose.waitForIdle()
        // In KANJI_FURIGANA, Furigana "みず" is displayed above Kanji "水", and Romaji "mizu" below
        compose.onNodeWithText("水").assertIsDisplayed()
        compose.onNodeWithText("みず").assertIsDisplayed()
        compose.onNodeWithText("mizu").assertIsDisplayed()
    }

    @Test
    fun questionRendererMeaningChoiceRespectsDualMode() {
        val currentMode = mutableStateOf(DisplayMode.KANA)
        val showRomaji = mutableStateOf(true)

        val question = Question.MeaningChoice(
            id = "test_mc_01",
            prompt = LessonText.Japanese(JapaneseText(kana = "ねこ", romaji = "neko", kanji = "猫")),
            options = listOf(
                Answer("A0", LessonText.English("Cat")),
                Answer("A1", LessonText.English("Dog")),
            ),
            correctId = "A0"
        )
        val session = LessonSession(
            LessonDefinition(99, "Test Level", "TEST", listOf(question))
        )

        compose.setContent {
            CompositionLocalProvider(
                LocalJapaneseDisplayMode provides currentMode.value,
                LocalRomajiVisibility provides showRomaji.value,
            ) {
                KotoTheme {
                    QuestionRenderer(
                        session = session,
                        minHeight = 400.dp,
                        speechReady = true,
                        isSpeaking = false,
                        speak = {}
                    )
                }
            }
        }

        // KANA mode: Shows "ねこ" prompt
        compose.onNodeWithText("ねこ").assertIsDisplayed()
        compose.onNodeWithText("neko").assertIsDisplayed()
        compose.onNodeWithText("猫").assertDoesNotExist()

        // Switch to KANJI_ONLY: Prompt updates to "猫"
        currentMode.value = DisplayMode.KANJI_ONLY
        compose.waitForIdle()
        compose.onNodeWithText("猫").assertIsDisplayed()
        compose.onNodeWithText("neko").assertIsDisplayed()
        compose.onNodeWithText("ねこ").assertDoesNotExist()

        // Switch to KANJI_FURIGANA: Shows both Furigana "ねこ" and Kanji "猫"
        currentMode.value = DisplayMode.KANJI_FURIGANA
        compose.waitForIdle()
        compose.onNodeWithText("猫").assertIsDisplayed()
        compose.onNodeWithText("ねこ").assertIsDisplayed()
        compose.onNodeWithText("neko").assertIsDisplayed()

        // Hide Romaji: "neko" disappears
        showRomaji.value = false
        compose.waitForIdle()
        compose.onNodeWithText("neko").assertDoesNotExist()
        compose.onNodeWithText("猫").assertIsDisplayed()
    }

    @Test
    fun clozeFilledPreservesKanji() {
        val question = Question.Cloze(
            id = "test_cloze_01",
            sentence = JapaneseText(kana = "これ は ___ です", romaji = "kore wa ___ desu", kanji = "これ は ___ です"),
            options = listOf(
                Answer("A0", LessonText.Japanese(JapaneseText(kana = "ほん", romaji = "hon", kanji = "本"))),
                Answer("A1", LessonText.Japanese(JapaneseText(kana = "みず", romaji = "mizu", kanji = "水"))),
            ),
            correctId = "A0"
        )

        val filled = question.filled("A0")
        assertEquals("これ は ほん です", filled.kana)
        assertEquals("kore wa hon desu", filled.romaji)
        assertEquals("これ は 本 です", filled.displayKanji)
        assertEquals("これ は ほん です", filled.tts)
    }
}
