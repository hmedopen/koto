package com.koto.app.feature.lesson.data

import com.koto.app.feature.lesson.model.LessonDefinition
import com.koto.app.feature.lesson.model.Question
import com.koto.app.feature.lesson.model.validate

object FoundationLessons : LessonRepository {
    private val level01Questions: List<Question> = emptyList()

    override val levels = listOf(
        LevelSummary(
            id = 1,
            title = "Level 01",
            stage = 1,
            questionCount = level01Questions.size,
        ),
    )

    override fun lesson(id: Int): LessonDefinition? {
        val summary = levels.firstOrNull { it.id == id } ?: return null
        if (level01Questions.isEmpty()) return null

        return LessonDefinition(
            id = summary.id,
            title = summary.title,
            section = summary.section,
            questions = level01Questions,
        ).also { it.validate() }
    }
}
