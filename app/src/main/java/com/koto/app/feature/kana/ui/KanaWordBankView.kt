package com.koto.app.feature.kana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.kana.model.PracticeWord
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.theme.KotoType

private enum class WordBankTab {
    Available,
    AllWords,
}

/**
 * Dedicated Word Bank inspection screen:
 * - Anti-Bubble design: Pure white background, hairline 1dp borders, square geometry.
 * - Symmetrical header with top-left [←] and top-right tactile 3D circular [X] dismiss.
 * - Interactive segmented switch between "Available Words" and "All Words (Pool)".
 * - Search bar supporting Kana, Romaji, and English search.
 * - Crisp word rows with Romaji strictly placed under Kana in a vertical Column.
 * - Tactile 3D speaker button playing native Japanese TTS pronunciation.
 */
@Composable
fun KanaWordBankView(
    availableWords: List<PracticeWord>,
    allWords: List<PracticeWord>,
    audio: JapaneseTtsController?,
    onDismiss: () -> Unit,
    onOpenFilter: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(WordBankTab.Available) }
    var scriptFilter by remember { mutableStateOf<KanaScript?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val currentList = when (selectedTab) {
        WordBankTab.Available -> availableWords
        WordBankTab.AllWords -> allWords
    }

    val hiraCount = remember(currentList) { currentList.count { it.script == KanaScript.Hiragana } }
    val kataCount = remember(currentList) { currentList.count { it.script == KanaScript.Katakana } }

    val filteredList = remember(currentList, searchQuery, scriptFilter) {
        var list = currentList
        if (scriptFilter != null) {
            list = list.filter { it.script == scriptFilter }
        }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            list
        } else {
            list.filter {
                it.kana.contains(q, ignoreCase = true) ||
                    it.romaji.lowercase().contains(q) ||
                    it.english.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // Prevent bleed-through to background screens
            )
            .testTag("kana_word_bank_view"),
    ) {
        // 1. Universal Top Header: [Back Arrow] | Word Bank | [Settings Gear]
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
                    .testTag("btn_word_bank_back"),
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

            // Center: Screen Title
            Text(
                text = "Word Bank",
                style = KotoType.Brand,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CardsColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("word_bank_title"),
            )

            // Right: Settings gear
            CardsPressable(
                onClick = onSettings,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterEnd)
                    .testTag("btn_word_bank_settings"),
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

        // 2. Segmented Mode Switcher: [ Available (X) | All Words (200) ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardsColors.Ice)
                .border(1.dp, CardsColors.Edge, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Available Tab
            val isAvailableActive = selectedTab == WordBankTab.Available
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAvailableActive) CardsColors.Surface else Color.Transparent)
                    .border(
                        width = if (isAvailableActive) 1.dp else 0.dp,
                        color = if (isAvailableActive) CardsColors.Edge else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { selectedTab = WordBankTab.Available }
                    .testTag("tab_words_available"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Available (${availableWords.size})",
                    fontSize = 13.sp,
                    fontWeight = if (isAvailableActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAvailableActive) CardsColors.Blue else CardsColors.Muted,
                )
            }

            // All Words Tab
            val isAllActive = selectedTab == WordBankTab.AllWords
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAllActive) CardsColors.Surface else Color.Transparent)
                    .border(
                        width = if (isAllActive) 1.dp else 0.dp,
                        color = if (isAllActive) CardsColors.Edge else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { selectedTab = WordBankTab.AllWords }
                    .testTag("tab_words_all"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "All Words (${allWords.size})",
                    fontSize = 13.sp,
                    fontWeight = if (isAllActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAllActive) CardsColors.Blue else CardsColors.Muted,
                )
            }
        }

        // 3. Search Field (Filter Kana, Romaji, English)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, CardsColors.Edge, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = "Search Kana, Romaji, or English...",
                    fontSize = 14.sp,
                    color = CardsColors.Muted,
                )
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = CardsColors.Ink,
                ),
                cursorBrush = SolidColor(CardsColors.Blue),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_word_bank_search"),
            )
        }

        // 4. Script Filter Segment: All | ひらがな | カタカナ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val scripts: List<Pair<KanaScript?, String>> = listOf(
                null to "All (${currentList.size})",
                KanaScript.Hiragana to "ひらがな ($hiraCount)",
                KanaScript.Katakana to "カタカナ ($kataCount)",
            )
            scripts.forEach { (script, label) ->
                val isSelected = scriptFilter == script
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CardsColors.Blue else CardsColors.Surface)
                        .border(1.dp, if (isSelected) CardsColors.Blue else CardsColors.Edge, RoundedCornerShape(8.dp))
                        .clickable { scriptFilter = script },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else CardsColors.Ink,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 5. Content List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "No words match \"$searchQuery\""
                        } else if (selectedTab == WordBankTab.Available) {
                            "No words can be spelled with your current Kana selection yet."
                        } else {
                            "No words found."
                        },
                        fontSize = 14.sp,
                        color = CardsColors.Muted,
                        textAlign = TextAlign.Center,
                    )

                    if (selectedTab == WordBankTab.Available && searchQuery.isEmpty()) {
                        CardsPressable(
                            onClick = onOpenFilter,
                            face = CardsColors.Blue,
                            depth = CardsColors.BlueDepth,
                            padding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_empty_edit_filter"),
                        ) {
                            Text(
                                text = "Edit Kana Selection",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(filteredList, key = { it.id }) { word ->
                    WordItemCard(
                        word = word,
                        onSpeak = {
                            audio?.speak(JapaneseText(kana = word.kana, romaji = word.romaji))
                        },
                    )
                }
            }
        }
    }
}

/**
 * Word Item Row complying with Anti-Bubble and Romaji Placement rules:
 * - Pure white background, hairline 1dp border, crisp 10dp squircle geometry.
 * - Japanese Kana on top with Romaji strictly placed DIRECTLY UNDER in a dedicated vertical Column.
 * - English definition and 3D tactile speaker button on the right.
 */
@Composable
private fun WordItemCard(
    word: PracticeWord,
    onSpeak: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardsColors.Edge, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("word_row_${word.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Left Column: Kana & Romaji directly underneath (Rule #4)
        Column(
            modifier = Modifier.weight(1.1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = word.kana,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalJapaneseFont.current,
                color = CardsColors.Ink,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = word.romaji,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CardsColors.Muted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Center / Right: English definition (budgeted weight, no wrap distortion)
        Text(
            text = word.english,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = CardsColors.Ink,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Right: 3D tactile audio speaker button
        CardsPressable(
            onClick = onSpeak,
            modifier = Modifier
                .size(38.dp)
                .testTag("btn_speak_${word.id}"),
            face = CardsColors.Surface,
            depth = CardsColors.Edge,
            padding = PaddingValues(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_speaker),
                contentDescription = "Speak pronunciation",
                tint = CardsColors.Blue,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
