package com.koto.app.feature.kana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.kana.data.KanaMasteryStore
import com.koto.app.feature.kana.data.KanaRepository
import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaRow
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.theme.KotoType

/**
 * Dedicated Full-Screen Kana Filter Page:
 * - Anti-Bubble principle: Pure white background, hairline 1dp borders, crisp square geometry.
 * - Universal top app bar: [Back Arrow] | Title: "${activeScript.displayName} Filter" | [Settings Gear]
 * - Granular column vowel batch buttons (+ / - for A, I, U, E, O) and row batch buttons.
 * - Bottom action bar: Script switcher (Hiragana / Katakana) and 3D Done button.
 */
@Composable
fun KanaFilterView(
    masteryStore: KanaMasteryStore,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
) {
    var activeScript by remember { mutableStateOf(KanaScript.Hiragana) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("kana_filter_modal"),
    ) {
        // 1. Universal Top App Bar: [Back Arrow] | Title | [Settings Gear]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp),
        ) {
            // Left: Back button
            CardsPressable(
                onClick = onDismiss,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterStart)
                    .testTag("btn_close_kana_filter"),
                face = CardsColors.Surface,
                depth = CardsColors.Edge,
                padding = PaddingValues(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = CardsColors.Ink,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Center: Screen Title (e.g. "Hiragana Filter" or "Katakana Filter")
            Text(
                text = "${activeScript.displayName} Filter",
                style = KotoType.Brand,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CardsColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("kana_filter_title"),
            )

            // Right: Settings gear
            CardsPressable(
                onClick = onSettings,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterEnd)
                    .testTag("btn_filter_settings"),
                face = CardsColors.Surface,
                depth = CardsColors.Edge,
                padding = PaddingValues(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = CardsColors.Ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

            // 2. Scrollable Matrix with Granular Batch (+ / -) and Tile Toggles
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                // Column Batch Controls Header Row (+ / - for each of 5 vowel columns)
                item {
                    val isHiragana = activeScript == KanaScript.Hiragana
                    val allChars = KanaRepository.allCharacters
                    val allSelected = allChars.all { masteryStore.isCharacterSelected(it.id, isHiragana) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Top-left master toggle button
                        BatchSquareButton(
                            symbol = if (allSelected) "×" else "+",
                            onClick = {
                                if (allSelected) {
                                    masteryStore.clearAllCharacters(isHiragana)
                                } else {
                                    masteryStore.selectAllCharacters(isHiragana)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "btn_filter_toggle_all",
                        )

                        // 5 vowel columns: -A, -I, -U, -E, -O
                        for (colIndex in 0 until 5) {
                            val colChars = KanaRepository.rows.mapNotNull { it.columns.getOrNull(colIndex) }
                            val allColSelected = colChars.isNotEmpty() && colChars.all { masteryStore.isCharacterSelected(it.id, isHiragana) }
                            BatchSquareButton(
                                symbol = if (allColSelected) "−" else "+",
                                onClick = {
                                    masteryStore.setCharactersSelected(colChars.map { it.id }, isHiragana, !allColSelected)
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "btn_filter_col_$colIndex",
                            )
                        }
                    }
                }

                // Group-by-group separated matrix (Basic, Dakuon, Handakuon, Yoon)
                val isHiragana = activeScript == KanaScript.Hiragana
                KanaRepository.groups.forEach { group ->
                    item(key = "header_${group.id}") {
                        val groupChars = group.rows.flatMap { it.columns.filterNotNull() }
                        val allGroupSelected = groupChars.isNotEmpty() && groupChars.all { masteryStore.isCharacterSelected(it.id, isHiragana) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = group.title,
                                style = KotoType.Brand,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CardsColors.Ink,
                            )
                            Text(
                                text = if (allGroupSelected) "Deselect All" else "Select All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CardsColors.Blue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        masteryStore.setCharactersSelected(groupChars.map { it.id }, isHiragana, !allGroupSelected)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    items(
                        items = group.rows,
                        key = { "${group.id}_${it.id}" },
                    ) { row ->
                        val rowChars = row.columns.filterNotNull()
                        val allRowSelected = rowChars.isNotEmpty() && rowChars.all { masteryStore.isCharacterSelected(it.id, isHiragana) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // Left-side Row batch (+ / -) toggle
                            BatchSquareButton(
                                symbol = if (allRowSelected) "−" else "+",
                                onClick = {
                                    masteryStore.setCharactersSelected(rowChars.map { it.id }, isHiragana, !allRowSelected)
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "btn_filter_row_${row.id}",
                            )

                            // 5 column positions
                            row.columns.forEachIndexed { _, char ->
                                if (char != null) {
                                    val isSelected = masteryStore.isCharacterSelected(char.id, isHiragana)
                                    FilterTile(
                                        character = char,
                                        script = activeScript,
                                        isSelected = isSelected,
                                        onClick = { masteryStore.toggleCharacterSelected(char.id, isHiragana) },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
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
            }

            // 3. Bottom Action Bar: Script Pill & Done Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Script Switcher (Hiragana / Katakana)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardsColors.Surface)
                        .border(1.dp, CardsColors.Edge, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                ) {
                    val isHira = activeScript == KanaScript.Hiragana
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isHira) CardsColors.Blue else Color.Transparent)
                            .clickable { activeScript = KanaScript.Hiragana }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "ひらがな (${masteryStore.selectedHiraganaIds.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalJapaneseFont.current,
                            color = if (isHira) Color.White else CardsColors.Ink,
                        )
                    }

                    val isKata = activeScript == KanaScript.Katakana
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isKata) CardsColors.Blue else Color.Transparent)
                            .clickable { activeScript = KanaScript.Katakana }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "カタカナ (${masteryStore.selectedKatakanaIds.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalJapaneseFont.current,
                            color = if (isKata) Color.White else CardsColors.Ink,
                        )
                    }
                }

                // Done Button
                CardsPressable(
                    onClick = onDismiss,
                    modifier = Modifier
                        .width(100.dp)
                        .testTag("btn_filter_done"),
                    face = CardsColors.Ink,
                    depth = Color(0xFF1B1B1B),
                    padding = PaddingValues(vertical = 10.dp),
                ) {
                    Text(
                        text = "Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

/**
 * Backward compatibility wrapper.
 */
@Composable
fun KanaFilterModal(
    masteryStore: KanaMasteryStore,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
) {
    KanaFilterView(
        masteryStore = masteryStore,
        onDismiss = onDismiss,
        onSettings = onSettings,
        modifier = modifier,
    )
}

@Composable
private fun BatchSquareButton(
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F3F5))
            .border(1.dp, CardsColors.Edge, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CardsColors.Ink,
        )
    }
}

@Composable
private fun FilterTile(
    character: KanaCharacter,
    script: KanaScript,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glyph = character.characterFor(script)
    val bgColor = if (isSelected) CardsColors.Blue else Color(0xFFF1F3F5)
    val textColor = if (isSelected) Color.White else CardsColors.Ink
    val subTextColor = if (isSelected) Color.White.copy(alpha = 0.8f) else CardsColors.Muted

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, if (isSelected) CardsColors.BlueDepth else CardsColors.Edge, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
            .testTag("filter_tile_${character.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = glyph,
                fontSize = if (glyph.length > 1) 13.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalJapaneseFont.current,
                color = textColor,
                textAlign = TextAlign.Center,
            )
            Text(
                text = character.romaji.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = subTextColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
