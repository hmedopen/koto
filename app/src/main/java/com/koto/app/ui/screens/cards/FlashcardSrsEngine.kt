package com.koto.app.ui.screens.cards

import android.content.Context
import org.json.JSONObject

/**
 * Record representing the Spaced Repetition System (SRS) state of an individual card.
 */
data class CardSrsRecord(
    val cardId: String,
    val deckId: String,
    val dueTimestamp: Long,
    val lastRating: CardRating?,
    val consecutiveEasyCount: Int = 0,
    val isMastered: Boolean = false,
    val lastReviewedTimestamp: Long? = null,
)

/**
 * Core SRS interval scheduling engine.
 *
 * Interval rules:
 * - Again: Due today (re-queued in current session; stays in today's active pool).
 * - Hard / Bad: Next review = Today + 1 Day (categorized as Weak on tomorrow's refresh).
 * - Good: Next review = Today + 3 Days.
 * - Easy: Next review = Today + 7 Days (if rated Easy consecutively, flagged as Mastered).
 */
object FlashcardSrsScheduler {
    const val ONE_DAY_MS = 24L * 60 * 60 * 1000L

    fun scheduleNext(
        current: CardSrsRecord?,
        cardId: String,
        deckId: String,
        rating: CardRating,
        now: Long = System.currentTimeMillis(),
    ): CardSrsRecord {
        return when (rating) {
            CardRating.Again -> CardSrsRecord(
                cardId = cardId,
                deckId = deckId,
                dueTimestamp = now, // Due today
                lastRating = rating,
                consecutiveEasyCount = 0,
                isMastered = false,
                lastReviewedTimestamp = now,
            )
            CardRating.Hard -> CardSrsRecord(
                cardId = cardId,
                deckId = deckId,
                dueTimestamp = now + 1 * ONE_DAY_MS, // Today + 1 Day
                lastRating = rating,
                consecutiveEasyCount = 0,
                isMastered = false,
                lastReviewedTimestamp = now,
            )
            CardRating.Good -> CardSrsRecord(
                cardId = cardId,
                deckId = deckId,
                dueTimestamp = now + 3 * ONE_DAY_MS, // Today + 3 Days
                lastRating = rating,
                consecutiveEasyCount = 0,
                isMastered = false,
                lastReviewedTimestamp = now,
            )
            CardRating.Easy -> {
                val prevConsecutive = current?.consecutiveEasyCount ?: 0
                val newConsecutive = prevConsecutive + 1
                val isMastered = newConsecutive >= 2 || (current?.isMastered == true)
                CardSrsRecord(
                    cardId = cardId,
                    deckId = deckId,
                    dueTimestamp = now + 7 * ONE_DAY_MS, // Today + 7 Days
                    lastRating = rating,
                    consecutiveEasyCount = newConsecutive,
                    isMastered = isMastered,
                    lastReviewedTimestamp = now,
                )
            }
        }
    }
}

/**
 * Local persistent store for flashcard SRS records using SharedPreferences.
 */
class FlashcardSrsStore(context: Context? = null) {
    private val appContext = context?.applicationContext
    private val preferences = appContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        // Wipe legacy v1 session / review data if present to ensure clean database replacement
        if (appCtxHasLegacyV1()) {
            appContext?.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
        }
    }

    private fun appCtxHasLegacyV1(): Boolean {
        val legacy = appContext?.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE) ?: return false
        return legacy.all.isNotEmpty()
    }

    fun clearAll() {
        preferences?.edit()?.clear()?.apply()
    }

    fun clear() = clearAll()

    fun loadAll(): Map<String, CardSrsRecord> {
        val json = preferences?.getString(KEY_SRS_DATA, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(json)
            val result = mutableMapOf<String, CardSrsRecord>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                val cardId = obj.getString("cardId")
                val deckId = obj.getString("deckId")
                val due = obj.getLong("dueTimestamp")
                val ratingName = obj.optString("lastRating", "")
                val rating = if (ratingName.isNotEmpty()) runCatching { CardRating.valueOf(ratingName) }.getOrNull() else null
                val consecutiveEasy = obj.optInt("consecutiveEasyCount", 0)
                val isMastered = obj.optBoolean("isMastered", false)
                val lastReviewed = if (obj.has("lastReviewedTimestamp")) obj.getLong("lastReviewedTimestamp") else null
                result[cardId] = CardSrsRecord(cardId, deckId, due, rating, consecutiveEasy, isMastered, lastReviewed)
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun saveAll(records: Map<String, CardSrsRecord>) {
        if (preferences == null) return
        val root = JSONObject()
        for ((id, record) in records) {
            val obj = JSONObject()
            obj.put("cardId", record.cardId)
            obj.put("deckId", record.deckId)
            obj.put("dueTimestamp", record.dueTimestamp)
            record.lastRating?.let { obj.put("lastRating", it.name) }
            obj.put("consecutiveEasyCount", record.consecutiveEasyCount)
            obj.put("isMastered", record.isMastered)
            record.lastReviewedTimestamp?.let { obj.put("lastReviewedTimestamp", it) }
            root.put(id, obj)
        }
        preferences.edit().putString(KEY_SRS_DATA, root.toString()).apply()
    }

    companion object {
        private const val LEGACY_PREFERENCES_NAME = "koto_flashcard_srs_v1"
        private const val PREFERENCES_NAME = "koto_flashcard_srs_v2"
        private const val KEY_SRS_DATA = "flashcard_srs_records"
        var defaultInstance: FlashcardSrsStore? = null
            get() = field ?: FlashcardSrsStore().also { field = it }
    }
}
