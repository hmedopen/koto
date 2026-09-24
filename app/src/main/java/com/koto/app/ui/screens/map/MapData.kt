package com.koto.app.ui.screens.map

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.koto.app.R
import com.koto.app.feature.lesson.data.FoundationLessons
import com.koto.app.feature.lesson.data.LessonRepository
import com.koto.app.feature.lesson.data.isLessonUnlocked

enum class LevelState(@param:StringRes val label: Int) { Locked(R.string.map_locked), Available(R.string.map_available), Current(R.string.map_current), Completed(R.string.map_completed) }
enum class AdjacentType(@param:StringRes val label: Int) { Review(R.string.map_review), Mini(R.string.map_mini), PhaseTwo(R.string.map_phase_two), Special(R.string.map_special) }
@Immutable data class KanaPreview(val romaji: String, val kana: String)
@Immutable data class MapLevel(val number: Int, val state: LevelState, @param:StringRes val expectation: Int, val preview: KanaPreview? = null, val left: AdjacentType? = null, val right: AdjacentType? = null, val lessonTitle: String? = null, val questionCount: Int = 0) { val displayNumber get() = number.toString().padStart(2, '0') }
@Immutable sealed interface MapRow { val key: String
    data class Stage(@param:StringRes val title: Int): MapRow { override val key = "stage_$title" }
    data class Level(val level: MapLevel): MapRow { override val key = "level_${level.number}" }
}
internal object MapFixtures {
    fun rows(completed: Set<Int>, repository: LessonRepository = FoundationLessons): List<MapRow> {
        val current = repository.levels.firstOrNull { it.id !in completed }?.id
        return buildList {
            var stage: Int? = null
            repository.levels.forEach { summary ->
                if (summary.stage != stage) {
                    stage = summary.stage
                    add(MapRow.Stage(if (stage == 1) R.string.map_stage_kana else R.string.map_stage_words))
                }
                val id = summary.id
                add(MapRow.Level(MapLevel(id, when {
                    id in completed -> LevelState.Completed
                    id == current -> LevelState.Current
                    isLessonUnlocked(id, completed, repository) -> LevelState.Available
                    else -> LevelState.Locked
                }, if (summary.stage == 1) R.string.map_expect_words else R.string.map_expect_sentences,
                    left = when (id) { 1 -> AdjacentType.Review; 3 -> AdjacentType.PhaseTwo; else -> null },
                    right = when (id) { 2 -> AdjacentType.Mini; 3 -> AdjacentType.Special; 7 -> AdjacentType.Review; else -> null },
                    lessonTitle = summary.title, questionCount = summary.questionCount)))
            }
        }
    }
    fun level(number: Int?, completed: Set<Int>): MapLevel? =
        if (number == null) null else rows(completed).filterIsInstance<MapRow.Level>().firstOrNull { it.level.number == number }?.level
}
