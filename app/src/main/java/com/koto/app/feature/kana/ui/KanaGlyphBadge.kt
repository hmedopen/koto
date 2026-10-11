package com.koto.app.feature.kana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.ui.screens.settings.LocalJapaneseFont

internal object KanaColors {
    val Coral = Color(0xFFFA5252)
    val CoralDepth = Color(0xFFE03131)
    val CoralTint5 = Color(0xFFFFF5F5)
    val CoralEdge = Color(0xFFFED7D7)
    val DarkActivePill = Color(0xFF262626)
    val TileBackground = Color(0xFFF5F6F8)
    val TileBorder = Color(0xFFEAECEF)
}

/**
 * Interlocking hiragana 'あ' and katakana 'ア' glyph badge.
 * Provides a crisp, culturally authentic visual emblem for the Kana module.
 */
@Composable
fun KanaGlyphBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 44.dp,
    containerColor: Color = KanaColors.Coral,
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        // Interlocking glyph layout: 'あ' foreground left, 'ア' slight offset right
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Katakana 'ア' (offset top-right with subtle translucency)
            Text(
                text = "ア",
                color = contentColor.copy(alpha = 0.82f),
                fontSize = (badgeSize.value * 0.44f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalJapaneseFont.current,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (badgeSize.value * 0.16f).dp, y = -(badgeSize.value * 0.10f).dp),
            )
            // Hiragana 'あ' (prominent center-left)
            Text(
                text = "あ",
                color = contentColor,
                fontSize = (badgeSize.value * 0.48f).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = LocalJapaneseFont.current,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = -(badgeSize.value * 0.14f).dp, y = (badgeSize.value * 0.10f).dp),
            )
        }
    }
}
