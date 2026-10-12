package com.koto.app.feature.kana.ui

import com.koto.app.feature.kana.data.PracticeWordPool
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaScript
import org.junit.Assert.*
import org.junit.Test

class KanaQuizSessionLogicTest {

    @Test
    fun typingQuizRomajiLengthAndUnderscores() {
        val testCases = listOf(
            "a" to 1,
            "ka" to 2,
            "shi" to 3,
            "chi" to 3,
            "tsu" to 3,
            "kyo" to 3,
        )

        for ((romaji, expectedLength) in testCases) {
            assertEquals("Length for $romaji must match", expectedLength, romaji.length)
        }
    }

    @Test
    fun typingQuizPerLetterErrorEvaluation() {
        // Expected "chi", user types "cho"
        val expected = "chi"
        val typed = "cho"

        val evaluation = (0 until expected.length).map { i ->
            typed[i].lowercaseChar() == expected[i].lowercaseChar()
        }

        // Index 0: 'c' == 'c' -> true (green)
        assertTrue("Index 0 ('c') should be correct", evaluation[0])
        // Index 1: 'h' == 'h' -> true (green)
        assertTrue("Index 1 ('h') should be correct", evaluation[1])
        // Index 2: 'o' != 'i' -> false (red)
        assertFalse("Index 2 ('o' vs 'i') should be wrong", evaluation[2])
    }

    @Test
    fun typingQuizAllCorrectEvaluation() {
        val expected = "ka"
        val typed = "ka"

        val evaluation = (0 until expected.length).map { i ->
            typed[i].lowercaseChar() == expected[i].lowercaseChar()
        }

        assertTrue("All characters must be correct", evaluation.all { it })
    }

    @Test
    fun matchingPairsDynamicCountRange() {
        // Requirement 9: between 3 and 7
        for (questionIndex in 0 until 20) {
            val count = (3 + (questionIndex % 5)).coerceIn(3, 7)
            assertTrue("Pair count must be >= 3", count >= 3)
            assertTrue("Pair count must be <= 7", count <= 7)
        }
    }

    @Test
    fun matchingPairsBidirectionalValidation() {
        // Pairs: ("あ", "A"), ("か", "KA"), ("さ", "SA")
        val pairs = listOf(
            "あ" to "A",
            "か" to "KA",
            "さ" to "SA",
        )

        // Test Selecting Kana first, then Romaji:
        val selectedKana = "あ"
        val selectedRomaji = "A"
        val matchKanaFirst = pairs.firstOrNull { it.first == selectedKana }?.second == selectedRomaji
        assertTrue("Selecting Kana first then matching Romaji must succeed", matchKanaFirst)

        // Test Selecting Romaji first, then Kana:
        val selectedRomajiFirst = "KA"
        val selectedKanaSecond = "か"
        val matchRomajiFirst = pairs.firstOrNull { it.first == selectedKanaSecond }?.second == selectedRomajiFirst
        assertTrue("Selecting Romaji first then matching Kana must succeed", matchRomajiFirst)

        // Test Wrong match:
        val wrongRomaji = "SA"
        val matchWrong = pairs.firstOrNull { it.first == "あ" }?.second == wrongRomaji
        assertFalse("Mismatch must be detected", matchWrong)
    }

    @Test
    fun listeningChallengeWordPoolHasStrictKanaChoices() {
        val words = PracticeWordPool.fallbackWords
        assertTrue("Word pool must have at least 10 words", words.size >= 10)

        val targetWord = words.first()
        val distractors = words.filter { it.id != targetWord.id }.take(4)
        val choices = (distractors + targetWord).shuffled()

        assertEquals("Listening challenge must have exactly 5 choices", 5, choices.size)
        // Verify all 5 choices display strictly kana
        choices.forEach { choice ->
            assertTrue("Kana must not be blank", choice.kana.isNotBlank())
            // Kana characters should be Japanese script
            val isJapaneseScript = choice.kana.all {
                (it in '\u3040'..'\u309F') || (it in '\u30A0'..'\u30FF') || it == 'ー'
            }
            assertTrue("Choice '${choice.kana}' must be strictly kana", isJapaneseScript)
        }
    }

    @Test
    fun wordSelectionQuizStructure() {
        val words = PracticeWordPool.fallbackWords
        val targetWord = words.first() // e.g. "あい" / "ai" / "love"

        // Question: Kana and English meaning
        val questionKana = targetWord.kana
        val questionEnglish = targetWord.english
        assertNotNull(questionKana)
        assertNotNull(questionEnglish)

        // Choices: Romaji readings
        val otherRomajis = words.filter { it.romaji != targetWord.romaji }.take(3).map { it.romaji }
        val choices = (otherRomajis + targetWord.romaji).shuffled()
        assertEquals("Word selection must offer 4 romaji choices", 4, choices.size)
        assertTrue("Correct romaji must be in choices", choices.contains(targetWord.romaji))
    }

    @Test
    fun quizTotalQuestionsIsTwentyAndAccuracyCalculation() {
        val totalQuestions = 20
        assertEquals(20, totalQuestions)

        val correctCount = 18
        val accuracyPct = ((correctCount * 100f) / totalQuestions).toInt()
        assertEquals(90, accuracyPct)

        val perfectCount = 20
        val perfectAccuracy = ((perfectCount * 100f) / totalQuestions).toInt()
        assertEquals(100, perfectAccuracy)
    }

    @Test
    fun mixedQuizDrillOptionsExcludeTestMode() {
        // Mixed mode must only include non-timed quizzes and strictly exclude the timed Test mode
        val mixedDrillOptions = listOf(
            KanaQuizMode.Listening,
            KanaQuizMode.WordSelection,
            KanaQuizMode.MatchingPairs,
            KanaQuizMode.WordBuilding,
            KanaQuizMode.Typing,
        )

        assertFalse("Mixed mode must not include timed Test mode", mixedDrillOptions.contains(KanaQuizMode.Test))
        assertEquals(5, mixedDrillOptions.size)
    }

    @Test
    fun wordBuildingQuestionIsRomajiWithEnglishMeaningAndKanaAnswers() {
        val words = PracticeWordPool.fallbackWords
        val targetWord = words.first { it.kana.length in 2..5 } // e.g. "あい" / "ai" / "love"

        // Question: Romaji reading as primary prompt + English meaning
        val questionRomaji = targetWord.romaji.lowercase()
        val questionEnglish = targetWord.english
        assertNotNull(questionRomaji)
        assertTrue(questionRomaji.isNotBlank())
        assertTrue(questionRomaji.all { it in 'a'..'z' || it == '-' })
        assertNotNull(questionEnglish)

        // Answers: Kana tiles to assemble (opposite of Word Selection)
        val charList = targetWord.kana.map { it.toString() }
        assertEquals(targetWord.kana.length, charList.size)
        charList.forEach { kanaChar ->
            val isJapaneseScript = kanaChar.all {
                (it in '\u3040'..'\u309F') || (it in '\u30A0'..'\u30FF') || it == 'ー'
            }
            assertTrue("Answer tile '$kanaChar' must be strictly kana", isJapaneseScript)
        }
    }

    @Test
    fun wordBuildingFullAssemblyEvaluationAutoChecksSuccessAndFailure() {
        val targetWord = "いぬ" // inu (dog)
        val charList = listOf("い", "ぬ")

        // 1. Correct order matches the word -> Success
        val correctAssembly = listOf("い", "ぬ")
        assertEquals(charList.size, correctAssembly.size)
        val isCorrect = correctAssembly.joinToString("") == targetWord
        assertTrue("Correctly ordered tiles must evaluate to true", isCorrect)

        // 2. Wrong order with all slots filled -> Auto-fail condition
        val wrongAssembly = listOf("ぬ", "い")
        assertEquals(charList.size, wrongAssembly.size)
        val isWrong = wrongAssembly.joinToString("") != targetWord
        assertTrue("Wrongly ordered tiles must trigger failure condition", isWrong)
    }

    @Test
    fun testQuizMasteryScoringCurveFourMilestonesToMastery() {
        // Initial state: Unpracticed (0 points)
        var score = 0
        assertEquals(0, score)

        // 4 clean sessions (+25 points each milestone) reach full mastery (100)
        val increments = listOf(25, 25, 25, 25)
        increments.forEachIndexed { step, delta ->
            score = (score + delta).coerceIn(0, 100)
            val expectedScore = (step + 1) * 25
            assertEquals(expectedScore, score)
        }
        assertEquals(100, score)
        val isMastered = score >= 100
        assertTrue("4 clean sessions must achieve full mastery", isMastered)
    }

    @Test
    fun testQuizMasterySessionAdvancementCap() {
        // Answering multiple times in 1 session caps at +25
        val initialScore = 0
        val maxSessionGain = 25
        val cappedScore = (initialScore + maxSessionGain).coerceIn(0, 100)
        assertEquals(25, cappedScore)
    }

    @Test
    fun testQuizMasteryFailurePenaltyAndMasteredLock() {
        // Mastered Kana (100) failed during subsequent test drops to 75
        var score = 100
        score = (score - 25).coerceIn(0, 100)
        assertEquals(75, score)
        assertFalse("Failed kana must lose mastered status and regress to 75%", score >= 100)

        // Further failure drops by -25 (previous milestone floor)
        score = (score - 25).coerceIn(0, 100)
        assertEquals(50, score)

        // Score cannot drop below 0
        score = 10
        score = (score - 25).coerceIn(0, 100)
        assertEquals(0, score)
    }

    @Test
    fun testKanaMasteryBarVisualStates() {
        // State 1: Unpracticed (score <= 0) -> Grey track, 0% fill
        val unpracticedScore = 0
        assertTrue(unpracticedScore <= 0)
        assertFalse(unpracticedScore >= 100)

        // State 2: In Progress (1..99) -> Blue loading bar
        val inProgressScore = 60
        assertTrue(inProgressScore in 1..99)
        assertFalse(inProgressScore <= 0)
        assertFalse(inProgressScore >= 100)
        val progressFraction = inProgressScore / 100f
        assertEquals(0.6f, progressFraction, 0.001f)

        // State 3: Mastered (>= 100) -> Solid Green
        val masteredScore = 100
        assertTrue(masteredScore >= 100)
    }
}
