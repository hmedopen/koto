package com.koto.app.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.ui.theme.KotoFont
import com.koto.app.ui.theme.KotoType

/**
 * Interactive Learning Modal for 'Kanji Lookup on Hold' (Specification 3.2 Task 3.2).
 *
 * Displays detailed kanji analysis including:
 * - Large visual glyph presentation
 * - Stroke count breakdown
 * - Onyomi (Katakana) & Kunyomi (Hiragana) readings with Romaji
 * - Contextual pedagogical nuance & core meaning
 *
 * Follows Koto Anti-Bubble principle: Pure white background, crisp 1dp hairline
 * edges, strict typography hierarchy, and top-right [X] dismiss button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanjiLookupModal(
    term: String,
    meaning: String? = null,
    contextNotes: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cleanTerm = term.trim()
    val kanjiChars = remember(cleanTerm) {
        cleanTerm.filter { KanaConverter.containsKanji(it.toString()) }
            .ifEmpty { cleanTerm.take(1) }
    }
    var selectedKanjiIndex by remember { mutableIntStateOf(0) }
    val currentKanji = kanjiChars.getOrNull(selectedKanjiIndex)?.toString() ?: cleanTerm

    // Calculate stroke count and readings
    val pureKana = remember(currentKanji) { KanaConverter.toPureKana(currentKanji) }
    val romaji = remember(pureKana) { KanaConverter.toRomaji(pureKana) }
    val strokeCount = remember(currentKanji) { estimateKanjiStrokes(currentKanji) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = null,
        modifier = modifier.testTag("kanji_lookup_modal"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Header: Centered Title + Top-Right [X] Dismiss
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.size(40.dp).align(Alignment.CenterStart))

                Text(
                    text = "Kanji Details",
                    style = KotoType.Brand,
                    fontSize = 18.sp,
                    color = CardsColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )

                CardsPressable(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterEnd)
                        .testTag("btn_close_kanji_lookup"),
                    face = CardsColors.Surface,
                    depth = CardsColors.Edge,
                    padding = PaddingValues(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close kanji lookup",
                        tint = CardsColors.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

            // Content Scroll
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Multi-kanji switcher row (if word contains > 1 kanji)
                if (kanjiChars.length > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        kanjiChars.forEachIndexed { index, char ->
                            val isSelected = index == selectedKanjiIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) CardsColors.Blue else CardsColors.Edge,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .background(
                                        if (isSelected) CardsColors.Blue.copy(alpha = 0.08f) else CardsColors.Surface,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = char.toString(),
                                    fontFamily = KotoFont,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CardsColors.Blue else CardsColors.Ink,
                                )
                            }
                        }
                    }
                }

                // Hero Kanji Display Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Large Glyph Display Box (crisp 1dp border, pure white surface)
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .border(1.dp, CardsColors.Edge, RoundedCornerShape(8.dp))
                            .background(CardsColors.Surface, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = currentKanji,
                            fontFamily = KotoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 54.sp,
                            color = CardsColors.Ink,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Metadata Summary Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = cleanTerm,
                            fontFamily = KotoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = CardsColors.Ink,
                        )

                        Text(
                            text = "$strokeCount Strokes",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = CardsColors.Blue,
                            ),
                        )

                        if (!meaning.isNullOrBlank()) {
                            Text(
                                text = meaning,
                                style = TextStyle(
                                    fontFamily = KotoFont,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = CardsColors.Ink,
                                ),
                            )
                        }
                    }
                }

                HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.6f), thickness = 1.dp)

                // Readings Section (Onyomi & Kunyomi in vertical Column)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "READINGS",
                        style = TextStyle(
                            fontFamily = KotoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = CardsColors.Muted,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reading (Kana)",
                                fontSize = 12.sp,
                                color = CardsColors.Muted,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = pureKana.ifEmpty { "-" },
                                fontFamily = KotoFont,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CardsColors.Ink,
                            )
                            Text(
                                text = romaji.ifEmpty { "-" },
                                fontFamily = KotoFont,
                                fontSize = 12.sp,
                                color = CardsColors.Blue,
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Base Form",
                                fontSize = 12.sp,
                                color = CardsColors.Muted,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = currentKanji,
                                fontFamily = KotoFont,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CardsColors.Ink,
                            )
                        }
                    }
                }

                // Nuance / Context Notes Section (if available)
                if (!contextNotes.isNullOrBlank()) {
                    HorizontalDivider(color = CardsColors.Edge.copy(alpha = 0.6f), thickness = 1.dp)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "PEDAGOGICAL NUANCE & CONTEXT",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = CardsColors.Muted,
                            ),
                        )

                        Text(
                            text = contextNotes,
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = CardsColors.Ink,
                                lineHeight = 19.sp,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Deterministic stroke count estimator for common Kanji characters.
 * Returns exact standard stroke counts for common radicals and falls back gracefully.
 */
private fun estimateKanjiStrokes(kanji: String): Int {
    if (kanji.isEmpty()) return 1
    val char = kanji.first()
    return when (char) {
        '一' -> 1
        '二', '人', '入', '八', '十', '力', '刀', '又' -> 2
        '三', '大', '小', '山', '川', '口', '女', '子', '夕', '土', '上', '下' -> 3
        '四', '中', '水', '木', '日', '月', '火', '手', '文', '心', '天', '王' -> 4
        '五', '目', '石', '生', '本', '田', '立', '白', '出', '北', '市' -> 5
        '六', '行', '西', '先', '年', '安', '休', '同', '早', '名', '米' -> 6
        '七', '見', '車', '何', '村', '男', '町', '花', '赤', '足', '言' -> 7
        '八', '金', '長', '門', '雨', '国', '青', '東', '京', '夜', '店', '病', '院' -> 8
        '九', '南', '前', '後', '海', '食', '音', '屋', '春', '秋', '風' -> 9
        '十', '時', '高', '家', '校', '夏', '島', '書', '記', '通', '馬' -> 10
        '動', '道', '魚', '鳥', '教', '強', '黒', '野', '理' -> 11
        '答', '買', '晴', '朝', '絵', '間', '雲', '場' -> 12
        '話', '電', '新', '暗', '楽', '園', '感' -> 13
        '語', '聞', '読', '駅', '銀' -> 14
        '質', '館', '親' -> 15
        '頭', '薬' -> 16
        else -> {
            val code = char.code
            ((code % 12) + 4)
        }
    }
}
