package com.koto.app.feature.lesson.model

/**
 * Authored choice from the raw JSON dataset.
 */
data class Choice(
    val id: String,
    val text: String,
    val correct: Boolean
)

/**
 * Audio playback metadata for listening exercises.
 */
data class AudioConfig(
    val enabled: Boolean = false,
    val file: String = ""
)

/**
 * Question record parsed directly from the local JSON dataset.
 */
data class QuestionData(
    val questionId: String,
    val stageId: String,
    val levelId: String,
    val quizType: String,
    val skillTags: List<String> = emptyList(),
    val difficulty: Int = 1,
    val newConcept: Boolean = false,
    val japanese: String,
    val romaji: String,
    val english: String,
    val choices: List<Choice> = emptyList(),
    val explanation: String = "",
    val audio: AudioConfig = AudioConfig(),
    val reviewTags: List<String> = emptyList()
)

/**
 * Level model aggregating questions for a specific curriculum level.
 */
data class Level(
    val id: String,
    val number: Int,
    val title: String,
    val stageId: String,
    val questions: List<Question> = emptyList(),
    val rawQuestions: List<QuestionData> = emptyList()
)

/**
 * Stage model aggregating multiple levels (e.g. Stage 1: First Contact).
 */
data class Stage(
    val id: String,
    val number: Int,
    val title: String,
    val levels: List<Level> = emptyList()
)
