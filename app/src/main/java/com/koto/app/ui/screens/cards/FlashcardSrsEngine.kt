package com.koto.app.ui.screens.cards

import android.content.Context
import com.koto.app.feature.cards.data.db.CardSrsEntity
import com.koto.app.feature.cards.data.db.FsrsReviewLogEntity
import com.koto.app.feature.cards.srs.FsrsCard
import com.koto.app.feature.cards.srs.FsrsConfig
import com.koto.app.feature.cards.srs.FsrsEngine
import com.koto.app.feature.cards.srs.FsrsRating
import com.koto.app.feature.cards.srs.FsrsReviewLog
import com.koto.app.feature.cards.srs.FsrsState
import com.koto.app.feature.translator.data.db.KotoDatabase
import org.json.JSONObject

typealias CardRating = FsrsRating
typealias ReviewAction = FsrsRating
typealias CardSrsRecord = FsrsCard

val FsrsCard.isMastered: Boolean get() = FsrsEngine.isMastered(this)
val FsrsCard.dueDateEpochDay: Long get() = due / 86_400_000L
val FsrsCard.lastReviewedTimestamp: Long? get() = last_review

fun FsrsCard?.isDue(now: Long = System.currentTimeMillis()): Boolean {
    if (this == null || this.state == FsrsState.New) return true
    return this.due <= now
}

/**
 * Persistent store for flashcard FSRS records backed by Room Database,
 * with fallback to SharedPreferences and in-memory caching.
 */
class FlashcardSrsStore(context: Context? = null) {
    private val appContext = context?.applicationContext
    private val preferences = appContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val cardSrsDao by lazy {
        appContext?.let { runCatching { KotoDatabase.getInstance(it).cardSrsDao() }.getOrNull() }
    }
    private val reviewLogDao by lazy {
        appContext?.let { runCatching { KotoDatabase.getInstance(it).fsrsReviewLogDao() }.getOrNull() }
    }
    private val memoryStore = mutableMapOf<String, FsrsCard>()

    fun loadAll(): Map<String, FsrsCard> {
        // 1. Try Room Database first
        val dao = cardSrsDao
        if (dao != null) {
            val entities = runCatching { dao.getAll() }.getOrNull()
            if (!entities.isNullOrEmpty()) {
                val result = entities.associate { entity ->
                    entity.cardId to entityToFsrsCard(entity)
                }
                memoryStore.putAll(result)
                return result
            }
        }

        // 2. Try SharedPreferences
        val json = preferences?.getString(KEY_SRS_DATA, null)
        if (json != null) {
            val loaded = runCatching {
                val root = JSONObject(json)
                val result = mutableMapOf<String, FsrsCard>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = root.getJSONObject(key)
                    val cardId = obj.getString("cardId")
                    val deckId = obj.getString("deckId")
                    val due = obj.optLong("due", 0L)
                    val stability = obj.optDouble("stability", 0.0)
                    val difficulty = obj.optDouble("difficulty", 0.0)
                    val elapsedDays = obj.optLong("elapsed_days", 0L)
                    val scheduledDays = obj.optLong("scheduled_days", 0L)
                    val reps = obj.optInt("reps", 0)
                    val lapses = obj.optInt("lapses", 0)
                    val stateStr = obj.optString("state", "New")
                    val state = runCatching { FsrsState.valueOf(stateStr) }.getOrDefault(FsrsState.New)
                    val lastReview = if (obj.has("last_review")) obj.getLong("last_review") else null
                    val lastRatingStr = obj.optString("last_rating", "")
                    val lastRating = if (lastRatingStr.isNotEmpty()) runCatching { FsrsRating.valueOf(lastRatingStr) }.getOrNull() else null
                    val step = obj.optInt("step", 0)

                    result[cardId] = FsrsCard(
                        cardId = cardId,
                        deckId = deckId,
                        due = due,
                        stability = stability,
                        difficulty = difficulty,
                        elapsed_days = elapsedDays,
                        scheduled_days = scheduledDays,
                        reps = reps,
                        lapses = lapses,
                        state = state,
                        last_review = lastReview,
                        last_rating = lastRating,
                        step = step,
                    )
                }
                result
            }.getOrDefault(emptyMap())

            if (loaded.isNotEmpty()) {
                memoryStore.putAll(loaded)
                saveAll(loaded)
                return loaded
            }
        }

        return memoryStore.toMap()
    }

    /**
     * Saves a card and review log IMMEDIATELY on every button press.
     */
    fun saveCard(card: FsrsCard, log: FsrsReviewLog? = null) {
        memoryStore[card.cardId] = card

        val dao = cardSrsDao
        if (dao != null) {
            runCatching {
                dao.insertOrUpdate(fsrsCardToEntity(card))
            }
        }
        val rDao = reviewLogDao
        if (rDao != null && log != null) {
            runCatching {
                rDao.insert(reviewLogToEntity(log))
            }
        }

        saveToPreferences(memoryStore)
    }

    fun saveAll(records: Map<String, FsrsCard>) {
        memoryStore.putAll(records)

        val dao = cardSrsDao
        if (dao != null && records.isNotEmpty()) {
            runCatching {
                val entities = records.values.map { fsrsCardToEntity(it) }
                dao.insertOrUpdateAll(entities)
            }
        }

        saveToPreferences(memoryStore)
    }

    private fun saveToPreferences(records: Map<String, FsrsCard>) {
        if (preferences == null) return
        val root = JSONObject()
        for ((id, card) in records) {
            val obj = JSONObject()
            obj.put("cardId", card.cardId)
            obj.put("deckId", card.deckId)
            obj.put("due", card.due)
            obj.put("stability", card.stability)
            obj.put("difficulty", card.difficulty)
            obj.put("elapsed_days", card.elapsed_days)
            obj.put("scheduled_days", card.scheduled_days)
            obj.put("reps", card.reps)
            obj.put("lapses", card.lapses)
            obj.put("state", card.state.name)
            card.last_review?.let { obj.put("last_review", it) }
            card.last_rating?.let { obj.put("last_rating", it.name) }
            obj.put("step", card.step)
            root.put(id, obj)
        }
        preferences.edit().putString(KEY_SRS_DATA, root.toString()).apply()
    }

    fun clearAll() {
        memoryStore.clear()
        preferences?.edit()?.clear()?.apply()
        runCatching { cardSrsDao?.clearAll() }
        runCatching { reviewLogDao?.clearAll() }
    }

    fun clear() = clearAll()

    companion object {
        private const val PREFERENCES_NAME = "koto_fsrs_v1"
        private const val KEY_SRS_DATA = "fsrs_card_records"
        var defaultInstance: FlashcardSrsStore? = null
            get() = field ?: FlashcardSrsStore().also { field = it }

        fun entityToFsrsCard(entity: CardSrsEntity): FsrsCard {
            val rating = entity.lastRating?.let { name -> runCatching { FsrsRating.valueOf(name) }.getOrNull() }
            val state = runCatching { FsrsState.valueOf(entity.state) }.getOrDefault(
                if (entity.stage >= 1) FsrsState.Review else FsrsState.New
            )
            val due = if (entity.due > 0L) entity.due else entity.dueDateEpochDay * 86_400_000L
            return FsrsCard(
                cardId = entity.cardId,
                deckId = entity.deckId,
                due = due,
                stability = entity.stability,
                difficulty = entity.difficulty,
                elapsed_days = entity.elapsedDays,
                scheduled_days = entity.scheduledDays,
                reps = entity.reps,
                lapses = entity.lapses,
                state = state,
                last_review = entity.lastReview ?: entity.lastReviewedTimestamp,
                last_rating = rating,
                step = entity.step,
            )
        }

        fun fsrsCardToEntity(card: FsrsCard): CardSrsEntity {
            return CardSrsEntity(
                cardId = card.cardId,
                deckId = card.deckId,
                due = card.due,
                stability = card.stability,
                difficulty = card.difficulty,
                elapsedDays = card.elapsed_days,
                scheduledDays = card.scheduled_days,
                reps = card.reps,
                lapses = card.lapses,
                state = card.state.name,
                lastReview = card.last_review,
                lastRating = card.last_rating?.name,
                step = card.step,
                stage = if (card.state == FsrsState.Review && card.stability >= FsrsConfig.MASTERED_STABILITY_DAYS) 8 else if (card.state != FsrsState.New) 1 else 0,
                dueDateEpochDay = card.due / 86_400_000L,
                lastReviewedTimestamp = card.last_review,
            )
        }

        fun reviewLogToEntity(log: FsrsReviewLog): FsrsReviewLogEntity {
            return FsrsReviewLogEntity(
                cardId = log.cardId,
                rating = log.rating.name,
                state = log.state.name,
                due = log.due,
                stability = log.stability,
                difficulty = log.difficulty,
                elapsedDays = log.elapsed_days,
                scheduledDays = log.scheduled_days,
                review = log.review,
            )
        }
    }
}
