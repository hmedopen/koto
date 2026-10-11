package com.koto.app.feature.kana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.feature.kana.data.KanaMasteryStore
import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaGroup
import com.koto.app.feature.kana.model.KanaRow
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.theme.KotoType

@Composable
fun KanaChartTab(
    script: KanaScript,
    onScriptChanged: (KanaScript) -> Unit,
    onSelectCharacter: (KanaCharacter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val masteryStore = remember(context) { KanaMasteryStore.get(context) }
    // Observe scoreVersion to trigger recomposition when scores update
    @Suppress("UNUSED_VARIABLE")
    val currentScoreVersion = masteryStore.scoreVersion

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("kana_chart_tab"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Script Toggle (Hiragana / Katakana Normalized Blue Theme Pill Toggle)
        item {
            KanaScriptPillToggle(
                selectedScript = script,
                onScriptSelected = onScriptChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }

        // 2. Groups (Basic, Dakuon, Handakuon, Yoon) with 1:1 square matrix cells
        KanaRepository.groups.forEach { group ->
            // Category Header: Enlarge font size and visual hierarchy
            item(key = "header_${group.id}") {
                Text(
                    text = group.title,
                    style = KotoType.Brand,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardsColors.Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                        .testTag("kana_category_header_${group.id}"),
                )
            }

            // Column Alignment Header
            item(key = "columns_${group.id}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.width(32.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        KanaRepository.columnVowels.forEach { vowel ->
                            Text(
                                text = vowel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CardsColors.Muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kana_column_header_${group.id}_$vowel"),
                            )
                        }
                    }
                }
            }

            // Flat Tile Matrix: Rows for this group
            items(
                items = group.rows,
                key = { "${group.id}_${it.id}" },
            ) { row ->
                KanaMatrixRow(
                    row = row,
                    script = script,
                    masteryStore = masteryStore,
                    onSelectCharacter = onSelectCharacter,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Two-way segmented toggle between Hiragana and Katakana.
 * Blue active pill (CardsColors.Blue) with white text; transparent inactive segment with ink text.
 */
@Composable
private fun KanaScriptPillToggle(
    selectedScript: KanaScript,
    onScriptSelected: (KanaScript) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(CardsColors.Surface)
            .border(1.dp, CardsColors.Edge, shape)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Hiragana Pill
            val isHiragana = selectedScript == KanaScript.Hiragana
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHiragana) CardsColors.Blue else Color.Transparent)
                    .clickable { onScriptSelected(KanaScript.Hiragana) }
                    .padding(vertical = 8.dp)
                    .testTag("toggle_script_hiragana"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Hiragana (ひらがな)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalJapaneseFont.current,
                    color = if (isHiragana) Color.White else CardsColors.Ink,
                )
            }

            // Katakana Pill
            val isKatakana = selectedScript == KanaScript.Katakana
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isKatakana) CardsColors.Blue else Color.Transparent)
                    .clickable { onScriptSelected(KanaScript.Katakana) }
                    .padding(vertical = 8.dp)
                    .testTag("toggle_script_katakana"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Katakana (カタカナ)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalJapaneseFont.current,
                    color = if (isKatakana) Color.White else CardsColors.Ink,
                )
            }
        }
    }
}

/**
 * Row in the Kana Matrix with consonant marker label down left margin
 * and 5 tiles / spacers.
 */
@Composable
private fun KanaMatrixRow(
    row: KanaRow,
    script: KanaScript,
    masteryStore: KanaMasteryStore,
    onSelectCharacter: (KanaCharacter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.testTag("kana_row_${row.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Vertical consonant marker down left margin
        Text(
            text = row.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CardsColors.Muted,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .width(32.dp)
                .testTag("kana_row_label_${row.id}"),
        )

        // 5 column positions
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.columns.forEachIndexed { _, char ->
                if (char != null) {
                    val score = masteryStore.getCharacterMastery(char.id, script == KanaScript.Hiragana)
                    KanaTile(
                        character = char,
                        script = script,
                        masteryScore = score,
                        onClick = { onSelectCharacter(char) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // Gap handling: Invisible spacer with symmetrical 1:1 square geometry
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
            }
        }
    }
}

/**
 * Flat Tile Matrix Tile:
 * - Symmetrical 1:1 square cell geometry
 * - Flat light-gray background fill (#F5F6F8)
 * - Rounded corners (12dp radius)
 * - Dynamic mini progress bar under Romaji (0–100% with Red -> Yellow -> Green -> Blue thresholds)
 */
@Composable
private fun KanaTile(
    character: KanaCharacter,
    script: KanaScript,
    masteryScore: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val charGlyph = character.characterFor(script)
    val progress = (masteryScore / 100f).coerceIn(0f, 1f)
    val barColor = when {
        masteryScore <= 30 -> Color(0xFFE53935) // Red (Learning)
        masteryScore <= 60 -> Color(0xFFFBC02D) // Yellow (Familiar)
        masteryScore <= 89 -> Color(0xFF43A047) // Green (Proficient)
        else -> Color(0xFF1976D2)               // Blue (Mastered)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(KanaColors.TileBackground)
            .border(1.dp, KanaColors.TileBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("kana_tile_${character.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Japanese character on top
            Text(
                text = charGlyph,
                fontSize = if (charGlyph.length > 1) 16.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalJapaneseFont.current,
                color = CardsColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("kana_tile_glyph_${character.id}"),
            )

            // Latin Romaji underneath
            Text(
                text = character.romaji.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = CardsColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("kana_tile_romaji_${character.id}"),
            )

            Spacer(Modifier.height(3.dp))

            // Dynamic mini progress bar (fills 0–100% and changes color based on performance)
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFFE0E0E0))
                    .testTag("kana_tile_progress_${character.id}"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceAtLeast(0.06f))
                        .fillMaxHeight()
                        .background(barColor),
                )
            }
        }
    }
}
