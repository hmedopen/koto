package com.koto.app.feature.lesson.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.koto.app.feature.lesson.model.LessonDefinition
import com.koto.app.feature.lesson.model.Question
import org.json.JSONArray
import org.json.JSONObject

/**
 * Spaced Repetition System (SRS) Tracker.
 *
 * Tracks missed questions and review tags across sessions using persistent local storage
 * (SharedPreferences) so that missed items can be dynamically served in upcoming levels.
 */
class SrsTracker(context: Context? = null) {
    private val preferences = context?.applicationContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var missedQuestionIds by mutableStateOf(loadMissedQuestionIds())
        private set

    var tagMistakeCounts by mutableStateOf(loadTagMistakeCounts())
        private set

    var questionMistakeCounts by mutableStateOf(loadQuestionMistakeCounts())
        private set

    /**
     * Record the outcome of answering a question.
     *
     * @param questionId ID of the question (e.g. "L01_Q01")
     * @param reviewTags Review tags associated with the question (e.g. ["greetings", "level01"])
     * @param isCorrect Whether the answer was correct on the first attempt
     */
    fun recordResult(questionId: String, reviewTags: List<String>, isCorrect: Boolean) {
        if (!isCorrect) {
            missedQuestionIds = missedQuestionIds + questionId
            val currentQCount = questionMistakeCounts[questionId] ?: 0
            questionMistakeCounts = questionMistakeCounts + (questionId to (currentQCount + 1))

            val updatedTags = tagMistakeCounts.toMutableMap()
            reviewTags.forEach { tag ->
                updatedTags[tag] = (updatedTags[tag] ?: 0) + 1
            }
            tagMistakeCounts = updatedTags
        } else {
            // Successfully answered: clear from immediate missed list
            if (questionId in missedQuestionIds) {
                missedQuestionIds = missedQuestionIds - questionId
            }
        }
        persist()
    }

    /**
     * Returns true if the given question ID is currently marked as needing review.
     */
    fun isMissed(questionId: String): Boolean = questionId in missedQuestionIds

    /**
     * Returns true if any of the given tags have recorded mistakes.
     */
    fun hasMissedTags(tags: List<String>): Boolean = tags.any { (tagMistakeCounts[it] ?: 0) > 0 }

    /**
     * Finds questions from earlier levels that need review, either because their specific ID
     * was missed, or because their review tags match areas where the learner has struggled.
     *
     * @param upcomingLevelNumber The level about to be played (e.g. 2, 3, 4, 5)
     * @param candidatePool Pool of authored questions from earlier levels
     * @param maxItems Maximum number of review questions to return
     */
    fun getUpcomingReviewQuestions(
        upcomingLevelNumber: Int,
        candidatePool: List<Question>,
        maxItems: Int = 2
    ): List<Question> {
        if (candidatePool.isEmpty() || maxItems <= 0) return emptyList()

        // Prioritize: 1) explicitly missed question IDs, 2) questions matching missed review tags
        val explicitlyMissed = candidatePool.filter { it.id in missedQuestionIds }
        val tagMissed = candidatePool.filter { q ->
            q.id !in missedQuestionIds && q.reviewTags.any { (tagMistakeCounts[it] ?: 0) > 0 }
        }

        val candidates = (explicitlyMissed + tagMissed).distinctBy { it.id }
        return candidates.take(maxItems)
    }

    /**
     * Dynamically injects missed items from prior levels into [lesson] if review items are pending.
     */
    fun injectDynamicReviews(
        lesson: LessonDefinition,
        candidatePool: List<Question>,
        maxItems: Int = 2
    ): LessonDefinition {
        val reviewQuestions = getUpcomingReviewQuestions(lesson.id, candidatePool, maxItems)
        if (reviewQuestions.isEmpty()) return lesson

        // Filter out any questions already present in this lesson
        val existingIds = lesson.questions.map { it.id }.toSet()
        val newReviews = reviewQuestions.filterNot { it.id in existingIds }
        if (newReviews.isEmpty()) return lesson

        return lesson.copy(questions = lesson.questions + newReviews)
    }

    /**
     * Clears all recorded SRS mistakes and history (used in tests or user reset).
     */
    fun clear() {
        missedQuestionIds = emptySet()
        tagMistakeCounts = emptyMap()
        questionMistakeCounts = emptyMap()
        preferences?.edit()?.clear()?.apply()
    }

    private fun persist() {
        preferences?.edit()?.apply {
            putStringSet(KEY_MISSED_ITEMS, missedQuestionIds)
            putString(KEY_TAG_COUNTS, JSONObject(tagMistakeCounts).toString())
            putString(KEY_QUESTION_COUNTS, JSONObject(questionMistakeCounts).toString())
            apply()
        }
    }

    private fun loadMissedQuestionIds(): Set<String> {
        return preferences?.getStringSet(KEY_MISSED_ITEMS, emptySet()).orEmpty()
    }

    private fun loadTagMistakeCounts(): Map<String, Int> {
        val json = preferences?.getString(KEY_TAG_COUNTS, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, Int>()
            obj.keys().forEach { key ->
                result[key] = obj.optInt(key, 0)
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun loadQuestionMistakeCounts(): Map<String, Int> {
        val json = preferences?.getString(KEY_QUESTION_COUNTS, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, Int>()
            obj.keys().forEach { key ->
                result[key] = obj.optInt(key, 0)
            }
            result
        }.getOrDefault(emptyMap())
    }

    companion object {
        private const val PREFERENCES_NAME = "koto_srs_v1"
        private const val KEY_MISSED_ITEMS = "srs_missed_items"
        private const val KEY_TAG_COUNTS = "srs_tag_counts"
        private const val KEY_QUESTION_COUNTS = "srs_question_counts"

        /** Default shared instance for app-wide SRS tracking */
        var defaultInstance: SrsTracker? = null
            get() = field ?: SrsTracker().also { field = it }
    }
}
