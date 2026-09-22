package com.koto.app.ui.screens.map

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.koto.app.R

enum class LevelState(@param:StringRes val label: Int) { Locked(R.string.map_locked), Available(R.string.map_available), Current(R.string.map_current), Completed(R.string.map_completed) }
enum class AdjacentType(@param:StringRes val label: Int) { Review(R.string.map_review), Mini(R.string.map_mini), PhaseTwo(R.string.map_phase_two), Special(R.string.map_special) }
@Immutable data class KanaPreview(val romaji: String, val kana: String)
@Immutable data class MapLevel(val number: Int, val state: LevelState, @param:StringRes val expectation: Int, val preview: KanaPreview? = null, val left: AdjacentType? = null, val right: AdjacentType? = null) { val displayNumber get() = number.toString().padStart(2, '0') }
@Immutable sealed interface MapRow { val key: String
    data class Stage(@param:StringRes val title: Int): MapRow { override val key = "stage_$title" }
    data class Level(val level: MapLevel): MapRow { override val key = "level_${level.number}" }
}
internal object MapFixtures {
    val rows = listOf<MapRow>(
        MapRow.Stage(R.string.map_stage_kana),
        MapRow.Level(MapLevel(1, LevelState.Completed, R.string.map_expect_sounds, KanaPreview("a  i  u  e  o", "あいうえお"), left = AdjacentType.Review)),
        MapRow.Level(MapLevel(2, LevelState.Current, R.string.map_expect_kana, KanaPreview("ka  ki  ku  ke  ko", "かきくけこ"), right = AdjacentType.Mini)),
        MapRow.Level(MapLevel(3, LevelState.Available, R.string.map_expect_practice, left = AdjacentType.PhaseTwo, right = AdjacentType.Special)),
        MapRow.Level(MapLevel(4, LevelState.Locked, R.string.map_expect_practice)),
        MapRow.Stage(R.string.map_stage_words),
        MapRow.Level(MapLevel(5, LevelState.Locked, R.string.map_expect_words)), MapRow.Level(MapLevel(6, LevelState.Locked, R.string.map_expect_words)),
        MapRow.Level(MapLevel(7, LevelState.Locked, R.string.map_expect_words, right = AdjacentType.Review)), MapRow.Level(MapLevel(8, LevelState.Locked, R.string.map_expect_words)),
        MapRow.Stage(R.string.map_stage_sentences),
        MapRow.Level(MapLevel(9, LevelState.Locked, R.string.map_expect_sentences)), MapRow.Level(MapLevel(10, LevelState.Locked, R.string.map_expect_sentences)),
        MapRow.Level(MapLevel(11, LevelState.Locked, R.string.map_expect_sentences)), MapRow.Level(MapLevel(12, LevelState.Locked, R.string.map_expect_sentences)))
    fun level(number: Int?) = rows.filterIsInstance<MapRow.Level>().firstOrNull { it.level.number == number }?.level
}
