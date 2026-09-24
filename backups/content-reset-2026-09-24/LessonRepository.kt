package com.koto.app.feature.lesson.data

import com.koto.app.BuildConfig
import com.koto.app.feature.lesson.model.LessonDefinition

// Development builds only. One switch restores sequential progression for testing.
const val UNLOCK_ALL_LEVELS = true

/** Map metadata never holds or constructs quiz objects. */
data class LevelSummary(val id: Int, val title: String, val stage: Int, val questionCount: Int) {
    val section get() = if (stage == 1) "FIRST CONTACT" else "BUILDING SENTENCES"
}

interface LessonRepository {
    val levels: List<LevelSummary>
    fun lesson(id: Int): LessonDefinition?
}

fun isLessonUnlocked(id: Int, completed: Set<Int>, repository: LessonRepository = FoundationLessons,
    unlockAll: Boolean = BuildConfig.DEBUG && UNLOCK_ALL_LEVELS): Boolean {
    val index = repository.levels.indexOfFirst { it.id == id }
    if (index < 0) return false
    return unlockAll || index == 0 || id in completed || repository.levels[index - 1].id in completed
}
