package com.koto.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.settings.DisplayMode
import com.koto.app.ui.screens.settings.LocalJapaneseDisplayMode
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.screens.settings.LocalEnglishFont
import com.koto.app.ui.screens.settings.LocalRomajiVisibility
import com.koto.app.ui.screens.settings.RubyToken

/**
 * Universal Ruby / Furigana text rendering composable for Japanese text.
 * Strictly adheres to Koto Design Philosophy:
 * - Anti-Bubble: Pure typography on white with disciplined spacing, no bulbous containers.
 * - Respects DisplayMode (Kana Only vs Kanji with Furigana vs Kanji Only).
 * - Accessible TalkBack semantics via phonetic contentDescription.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RubyText(
    tokens: List<RubyToken>,
    modifier: Modifier = Modifier,
    mode: DisplayMode = LocalJapaneseDisplayMode.current,
    baseFontSize: TextUnit = 22.sp,
    baseColor: Color = CardsColors.Ink,
    furiganaColor: Color = CardsColors.Blue,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: androidx.compose.ui.text.font.FontFamily = LocalJapaneseFont.current,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onFuriganaClick: (() -> Unit)? = null,
) {
    if (tokens.isEmpty()) return

    val fullPhoneticReading = tokens.joinToString("") { it.reading ?: it.surface }

    FlowRow(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = fullPhoneticReading
        },
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.Bottom,
    ) {
        tokens.forEach { token ->
            when (mode) {
                DisplayMode.KANA -> {
                    // Plain Kana phonetic reading
                    Text(
                        text = token.reading ?: token.surface,
                        fontSize = baseFontSize,
                        color = baseColor,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily,
                        lineHeight = (baseFontSize.value * 1.3f).sp,
                    )
                }

                DisplayMode.KANJI_ONLY -> {
                    // Raw Kanji without ruby annotations
                    Text(
                        text = token.surface,
                        fontSize = baseFontSize,
                        color = baseColor,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily,
                        lineHeight = (baseFontSize.value * 1.3f).sp,
                    )
                }

                DisplayMode.KANJI_FURIGANA -> {
                    if (token.hasRuby) {
                        // Vertical column with Furigana cleanly centered above the Kanji compound
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            modifier = Modifier.padding(horizontal = 0.5.dp),
                        ) {
                            Text(
                                text = token.reading!!,
                                fontSize = (baseFontSize.value * 0.48f).coerceAtLeast(10f).sp,
                                color = furiganaColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily,
                                maxLines = 1,
                                lineHeight = (baseFontSize.value * 0.55f).sp,
                                modifier = if (onFuriganaClick != null) {
                                    Modifier.testTag("furigana_text").clickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClick = onFuriganaClick,
                                    )
                                } else Modifier,
                            )
                            Text(
                                text = token.surface,
                                fontSize = baseFontSize,
                                color = baseColor,
                                fontWeight = fontWeight,
                                fontFamily = fontFamily,
                                lineHeight = (baseFontSize.value * 1.2f).sp,
                            )
                        }
                    } else {
                        // Plain Kana or punctuation token: top spacer locks identical line-height across rows, eliminating jitter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            Spacer(
                                modifier = Modifier.height((baseFontSize.value * 0.55f).dp),
                            )
                            Text(
                                text = token.surface,
                                fontSize = baseFontSize,
                                color = baseColor,
                                fontWeight = fontWeight,
                                fontFamily = fontFamily,
                                lineHeight = (baseFontSize.value * 1.2f).sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convenience overload for rendering Japanese text directly from a String.
 * Uses Kuromoji via KanaConverter to extract RubyTokens automatically.
 */
@Composable
fun RubyText(
    text: String,
    modifier: Modifier = Modifier,
    mode: DisplayMode = LocalJapaneseDisplayMode.current,
    baseFontSize: TextUnit = 22.sp,
    baseColor: Color = CardsColors.Ink,
    furiganaColor: Color = CardsColors.Blue,
    fontWeight: FontWeight = FontWeight.Bold,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onFuriganaClick: (() -> Unit)? = null,
) {
    val tokens = KanaConverter.extractRubyTokens(text)
    RubyText(
        tokens = tokens,
        modifier = modifier,
        mode = mode,
        baseFontSize = baseFontSize,
        baseColor = baseColor,
        furiganaColor = furiganaColor,
        fontWeight = fontWeight,
        horizontalArrangement = horizontalArrangement,
        onFuriganaClick = onFuriganaClick,
    )
}

/**
 * Dedicated Vocabulary & Flashcard Japanese Display block.
 * Adheres strictly to:
 * - Design Rule 1 (Anti-Bubble): pure text hierarchy.
 * - Design Rule 2 (Romaji Placement Rule): Romaji is ALWAYS placed directly
 *   UNDER the Kana/Kanji in a dedicated vertical block (Column). Never side-by-side.
 */
@Composable
fun JapaneseWordDisplay(
    kanji: String,
    kana: String,
    romaji: String,
    modifier: Modifier = Modifier,
    wordModifier: Modifier = Modifier,
    romajiModifier: Modifier = Modifier,
    mode: DisplayMode = LocalJapaneseDisplayMode.current,
    showRomaji: Boolean = LocalRomajiVisibility.current,
    revealFuriganaOnTap: Boolean = false,
    furiganaRevealed: Boolean = true,
    fontSize: TextUnit = 24.sp,
    fontColor: Color = CardsColors.Ink,
    furiganaColor: Color = CardsColors.Blue,
    romajiColor: Color = CardsColors.Blue,
    fontFamily: androidx.compose.ui.text.font.FontFamily = LocalJapaneseFont.current,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    onFuriganaClick: (() -> Unit)? = null,
) {
    val effectiveKanji = kanji.ifBlank { kana }
    val effectiveKana = kana.ifBlank { KanaConverter.toPureKana(effectiveKanji) }
    val effectiveRomaji = romaji.ifBlank { KanaConverter.toRomaji(effectiveKana) }

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (mode) {
            DisplayMode.KANA -> {
                Text(
                    text = effectiveKana,
                    modifier = wordModifier,
                    fontSize = fontSize,
                    color = fontColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
                )
            }

            DisplayMode.KANJI_ONLY -> {
                Text(
                    text = effectiveKanji,
                    modifier = wordModifier,
                    fontSize = fontSize,
                    color = fontColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
                )
            }

            DisplayMode.KANJI_FURIGANA -> {
                val effectiveFuriganaColor = if (revealFuriganaOnTap && !furiganaRevealed) Color.Transparent else furiganaColor
                if (KanaConverter.containsKanji(effectiveKanji)) {
                    val tokens = KanaConverter.extractRubyTokens(effectiveKanji)
                    RubyText(
                        tokens = tokens,
                        modifier = wordModifier,
                        mode = DisplayMode.KANJI_FURIGANA,
                        baseFontSize = fontSize,
                        baseColor = fontColor,
                        furiganaColor = effectiveFuriganaColor,
                        fontFamily = fontFamily,
                        horizontalArrangement = if (horizontalAlignment == Alignment.CenterHorizontally) Arrangement.Center else Arrangement.Start,
                        onFuriganaClick = onFuriganaClick,
                    )
                } else {
                    Text(
                        text = effectiveKana,
                        modifier = wordModifier,
                        fontSize = fontSize,
                        color = fontColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
                    )
                }
            }
        }

        // Romaji placed strictly UNDER the Kana/Kanji in vertical Column (Rule 2)
        if (showRomaji && effectiveRomaji.isNotBlank()) {
            Text(
                text = effectiveRomaji,
                modifier = romajiModifier,
                fontSize = (fontSize.value * 0.52f).coerceAtLeast(12f).sp,
                color = romajiColor,
                fontWeight = FontWeight.Normal,
                fontFamily = LocalEnglishFont.current,
                textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}
