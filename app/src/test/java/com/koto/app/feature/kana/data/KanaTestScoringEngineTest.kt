package com.koto.app.feature.kana.data

import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.kana.ui.QuizKanaTarget
import org.junit.Assert.*
import org.junit.Test

class KanaTestScoringEngineTest {

    private fun dummyTarget(id: String, isHiragana: Boolean = true): QuizKanaTarget {
        val script = if (isHiragana) KanaScript.Hiragana else KanaScript.Katakana
        val character = KanaRepository.allCharacters.firstOrNull { it.id == id }
            ?: KanaCharacter(
                id = id,
                hiragana = "あ",
                katakana = "ア",
                romaji = id.removePrefix("kana_"),
                rowLabel = "A",
                colIndex = 0,
                strokeStepsHira = emptyList(),
                strokeStepsKata = emptyList(),
            )
        return QuizKanaTarget(character = character, script = script)
    }

    @Test
    fun scenario1_duplicateSamplingInOneSessionCapsGainAt25() {
        // Kana "あ" starts at 0
        var currentScore = 0
        val engine = KanaTestScoringEngine(initialScoreProvider = { _, _ -> currentScore })
        val targetA = dummyTarget("kana_a")

        // Q3: "あ" correct
        engine.recordAttempt(targetA, isCorrect = true, isRetryPhase = false)
        // Q18: "あ" correct again
        engine.recordAttempt(targetA, isCorrect = true, isRetryPhase = false)

        assertFalse("No items should be in retry queue", engine.hasPendingRetries())

        val outcomes = engine.computeFinalOutcomes()
        assertEquals(1, outcomes.size)
        val outcomeA = outcomes.first()
        assertEquals("kana_a", outcomeA.kanaId)
        assertEquals(0, outcomeA.initialScore)
        // Result must be 25 (Quarter Blue), NOT 50
        assertEquals(25, outcomeA.newScore)
    }

    @Test
    fun scenario2_mistakePlusRetryQueueClearanceDemotesAndAwardsZeroBonus() {
        // Kana "か" starts at 50
        val initialScore = 50
        val engine = KanaTestScoringEngine(initialScoreProvider = { _, _ -> initialScore })
        val targetKa = dummyTarget("kana_ka")

        // Q7: user misses "か"
        engine.recordAttempt(targetKa, isCorrect = false, isRetryPhase = false)
        assertTrue("Missed item must be in retry queue", engine.hasPendingRetries())
        assertEquals(1, engine.pendingRetryCount)
        assertEquals(targetKa, engine.nextRetryTarget())

        // Q21 (Retry): user selects correct option
        engine.recordAttempt(targetKa, isCorrect = true, isRetryPhase = true)
        assertFalse("Retry queue should now be empty", engine.hasPendingRetries())

        val outcomes = engine.computeFinalOutcomes()
        assertEquals(1, outcomes.size)
        val outcomeKa = outcomes.first()
        assertEquals(50, outcomeKa.initialScore)
        // Result: Score drops to 25 (Half Blue -> Quarter Blue). Retry prevented further penalty but awarded 0 points.
        assertEquals(25, outcomeKa.newScore)
    }

    @Test
    fun scenario3_masteredDegradationDropsTo75AndRequiresSubsequentCleanSession() {
        // Kana "さ" starts at 100 (Solid Green)
        var saScore = 100
        val session1 = KanaTestScoringEngine(initialScoreProvider = { _, _ -> saScore })
        val targetSa = dummyTarget("kana_sa")

        // User misses "さ" on first presentation
        session1.recordAttempt(targetSa, isCorrect = false, isRetryPhase = false)
        // Clears it on retry
        session1.recordAttempt(targetSa, isCorrect = true, isRetryPhase = true)

        val outcomes1 = session1.computeFinalOutcomes()
        assertEquals(75, outcomes1.first().newScore)

        // Session 1 updates persistent score to 75
        saScore = outcomes1.first().newScore
        assertEquals(75, saScore)

        // Session 2: Clean first attempt
        val session2 = KanaTestScoringEngine(initialScoreProvider = { _, _ -> saScore })
        session2.recordAttempt(targetSa, isCorrect = true, isRetryPhase = false)

        val outcomes2 = session2.computeFinalOutcomes()
        assertEquals(100, outcomes2.first().newScore)
    }

    @Test
    fun fourMilestoneLadderFullProgression() {
        // 0 -> 25 -> 50 -> 75 -> 100 across 4 distinct test sessions
        var score = 0
        val target = dummyTarget("kana_ta")

        for (sessionIndex in 1..4) {
            val engine = KanaTestScoringEngine(initialScoreProvider = { _, _ -> score })
            engine.recordAttempt(target, isCorrect = true, isRetryPhase = false)
            val outcomes = engine.computeFinalOutcomes()
            score = outcomes.first().newScore
            assertEquals(sessionIndex * 25, score)
        }
        assertEquals(100, score)

        // Fifth clean session stays clamped at 100
        val engine5 = KanaTestScoringEngine(initialScoreProvider = { _, _ -> score })
        engine5.recordAttempt(target, isCorrect = true, isRetryPhase = false)
        assertEquals(100, engine5.computeFinalOutcomes().first().newScore)
    }

    @Test
    fun asymmetricDemotionLadder() {
        val testFloors = listOf(
            75 to 50,
            50 to 25,
            25 to 0,
            0 to 0,
        )

        for ((startScore, expectedEnd) in testFloors) {
            val engine = KanaTestScoringEngine(initialScoreProvider = { _, _ -> startScore })
            val target = dummyTarget("kana_na")
            engine.recordAttempt(target, isCorrect = false, isRetryPhase = false)
            engine.recordAttempt(target, isCorrect = true, isRetryPhase = true)
            val finalScore = engine.computeFinalOutcomes().first().newScore
            assertEquals("Start $startScore must drop to $expectedEnd", expectedEnd, finalScore)
        }
    }

    @Test
    fun nearestMilestoneRoundingSanitization() {
        assertEquals(0, KanaMasteryStore.snapToMilestone(0))
        assertEquals(25, KanaMasteryStore.snapToMilestone(20))
        assertEquals(50, KanaMasteryStore.snapToMilestone(40))
        assertEquals(50, KanaMasteryStore.snapToMilestone(60))
        assertEquals(75, KanaMasteryStore.snapToMilestone(80))
        assertEquals(100, KanaMasteryStore.snapToMilestone(95))
        assertEquals(100, KanaMasteryStore.snapToMilestone(100))
    }

    @Test
    fun retryQueueRepeatedFailuresStayInQueueUntilCorrect() {
        val engine = KanaTestScoringEngine(initialScoreProvider = { _, _ -> 50 })
        val target = dummyTarget("kana_ha")

        // First attempt missed
        engine.recordAttempt(target, isCorrect = false, isRetryPhase = false)
        assertTrue(engine.hasPendingRetries())

        // Retry 1: missed again
        engine.recordAttempt(target, isCorrect = false, isRetryPhase = true)
        assertTrue("Must still have pending retries", engine.hasPendingRetries())

        // Retry 2: correct
        engine.recordAttempt(target, isCorrect = true, isRetryPhase = true)
        assertFalse("Must now be cleared", engine.hasPendingRetries())

        // Score was penalized only once (-25)
        val finalScore = engine.computeFinalOutcomes().first().newScore
        assertEquals(25, finalScore)
    }
}
