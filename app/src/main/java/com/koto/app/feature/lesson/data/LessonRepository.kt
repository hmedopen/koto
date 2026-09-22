package com.koto.app.feature.lesson.data

import com.koto.app.BuildConfig
import com.koto.app.feature.lesson.model.LessonDefinition

// Development builds only. One switch restores sequential progression for testing.
const val UNLOCK_ALL_PROTOTYPE_LEVELS = true

interface LessonRepository {
    val lessons: List<LessonDefinition>
    fun lesson(id: Int): LessonDefinition? = lessons.firstOrNull { it.id == id }
}

fun isLessonUnlocked(id: Int, completed: Set<Int>, repository: LessonRepository = PrototypeLessons,
    unlockAll: Boolean = BuildConfig.DEBUG && UNLOCK_ALL_PROTOTYPE_LEVELS): Boolean {
    val index = repository.lessons.indexOfFirst { it.id == id }
    if (index < 0) return false
    return unlockAll || index == 0 || id in completed || repository.lessons[index - 1].id in completed
}
