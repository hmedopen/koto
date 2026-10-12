package com.koto.app.feature.kana.data

import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.kana.ui.QuizKanaTarget

/**
 * Tracks the in-memory session state for a single Kana during a Test quiz.
 */
data class KanaSessionRecord(
    val kanaId: String,
    val isHiragana: Boolean,
    val initialScore: Int,
    var firstAttemptResolved: Boolean = false,
    var firstAttemptPassed: Boolean = false,
)

/**
 * Result outcome of a completed Test session for a Kana, ready for persistent commit.
 */
data class KanaMasteryOutcome(
    val kanaId: String,
    val isHiragana: Boolean,
    val initialScore: Int,
    val newScore: Int,
)

/**
 * Hybrid Mastery Engine for single-character Test quizzes.
 *
 * Enforces:
 * - 4-Milestone Ladder (0, 25, 50, 75, 100).
 * - Rule A: Session Advancement Cap (+25 max per session).
 * - Rule B: First-Attempt Gate (only first encounter determines score change).
 * - Rule C: End-of-Quiz Retry Queue (missed items replay; retries award 0 points).
 * - Rule D: Asymmetric Demotion (-25 floor on mistake; Mastered 100 drops to 75).
 * - Anti-Yo-Yo Lock (requires separate subsequent sessions to re-master).
 */
class KanaTestScoringEngine(
    private val initialScoreProvider: (kanaId: String, isHiragana: Boolean) -> Int,
) {
    private val records = mutableMapOf<String, KanaSessionRecord>()
    private val _retryQueue = mutableListOf<QuizKanaTarget>()

    val retryQueue: List<QuizKanaTarget>
        get() = _retryQueue.toList()

    val pendingRetryCount: Int
        get() = _retryQueue.size

    fun getRecord(kanaId: String, isHiragana: Boolean): KanaSessionRecord {
        val key = makeKey(kanaId, isHiragana)
        return records.getOrPut(key) {
            val rawScore = initialScoreProvider(kanaId, isHiragana)
            val snappedScore = KanaMasteryStore.snapToMilestone(rawScore)
            KanaSessionRecord(
                kanaId = kanaId,
                isHiragana = isHiragana,
                initialScore = snappedScore,
            )
        }
    }

    /**
     * Records an answer attempt for a target.
     *
     * @param target The target being tested.
     * @param isCorrect Whether the answer was correct (false on wrong choice or 8s timeout).
     * @param isRetryPhase Whether this attempt is occurring in the post-quiz Retry Queue.
     */
    fun recordAttempt(target: QuizKanaTarget, isCorrect: Boolean, isRetryPhase: Boolean) {
        val isHira = target.script == KanaScript.Hiragana
        val record = getRecord(target.id, isHira)

        if (!isRetryPhase) {
            // Rule B: First-Attempt Gate
            if (!record.firstAttemptResolved) {
                record.firstAttemptResolved = true
                record.firstAttemptPassed = isCorrect
            }

            // Rule C: Queue missed items for retry
            if (!isCorrect) {
                if (_retryQueue.none { it.id == target.id && it.script == target.script }) {
                    _retryQueue.add(target)
                }
            }
        } else {
            // Rule C: Retry Phase - Retries award 0 points.
            if (isCorrect) {
                // Resolved on retry -> remove from queue
                val index = _retryQueue.indexOfFirst { it.id == target.id && it.script == target.script }
                if (index != -1) {
                    _retryQueue.removeAt(index)
                }
            } else {
                // Failed retry -> push to back of queue to replay until mastered
                val index = _retryQueue.indexOfFirst { it.id == target.id && it.script == target.script }
                if (index != -1) {
                    val item = _retryQueue.removeAt(index)
                    _retryQueue.add(item)
                }
            }
        }
    }

    fun hasPendingRetries(): Boolean = _retryQueue.isNotEmpty()

    fun nextRetryTarget(): QuizKanaTarget? = _retryQueue.firstOrNull()

    /**
     * Computes the final milestone score adjustments across all encountered Kana.
     * - If firstAttemptPassed: min(100, initialScore + 25)
     * - If !firstAttemptPassed: max(0, initialScore - 25)
     */
    fun computeFinalOutcomes(): List<KanaMasteryOutcome> {
        return records.values.map { record ->
            val computed = if (record.firstAttemptPassed) {
                (record.initialScore + 25).coerceAtMost(100)
            } else {
                (record.initialScore - 25).coerceAtLeast(0)
            }
            KanaMasteryOutcome(
                kanaId = record.kanaId,
                isHiragana = record.isHiragana,
                initialScore = record.initialScore,
                newScore = computed,
            )
        }
    }

    companion object {
        fun makeKey(kanaId: String, isHiragana: Boolean): String {
            return if (isHiragana) "hira_$kanaId" else "kata_$kanaId"
        }
    }
}
