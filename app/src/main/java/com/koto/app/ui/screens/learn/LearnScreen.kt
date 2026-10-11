package com.koto.app.ui.screens.learn

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.ui.screens.cards.CardsColors
import com.koto.app.ui.screens.cards.CardsPressable
import com.koto.app.ui.screens.settings.LocalJapaneseFont
import com.koto.app.ui.theme.KotoType

@Composable
fun LearnScreen(
    onOpenTranslator: () -> Unit = {},
    onOpenKana: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("screen_learn"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Translate (Primary Action Banner - Full-Width Blue)
            LearnFeatureCard(
                title = "Translate",
                iconRes = R.drawable.ic_swap,
                faceColor = CardsColors.Blue,
                depthColor = CardsColors.BlueDepth,
                buttonTag = "learn_translate_button",
                titleTag = "learn_translate_title",
                onClick = onOpenTranslator,
            )

            // 2. Dual Side-by-Side 1:1 Square Modules: Kana & Kanji Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Kana: Solid Vibrant Red Square (1:1 aspect ratio)
                LearnSquareCard(
                    title = "Kana",
                    glyphText = "あア",
                    faceColor = Color(0xFFE53935),
                    depthColor = Color(0xFFB71C1C),
                    buttonTag = "learn_kana_button",
                    titleTag = "learn_kana_title",
                    onClick = onOpenKana,
                    modifier = Modifier.weight(1f),
                )

                // Kanji Canvas: Solid Deep Purple Square (1:1 aspect ratio)
                LearnSquareCard(
                    title = "Kanji Canvas",
                    iconRes = R.drawable.ic_edit,
                    faceColor = Color(0xFF5B4393),
                    depthColor = Color(0xFF3E2D64),
                    buttonTag = "learn_kanji_button",
                    titleTag = "learn_kanji_title",
                    onClick = { Toast.makeText(context, "Kanji Canvas Coming Soon", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f),
                )
            }

            // 3. Phrasebook & Idioms (Coral) - Full-Width Card
            LearnFeatureCard(
                title = "Phrasebook",
                iconRes = R.drawable.ic_trophy,
                faceColor = Color(0xFFC04B33),
                depthColor = Color(0xFF833323),
                buttonTag = "learn_phrasebook_button",
                titleTag = "learn_phrasebook_title",
                onClick = { Toast.makeText(context, "Phrasebook Coming Soon", Toast.LENGTH_SHORT).show() },
            )

            // 4. Audio & Listening Lab (Teal) - Full-Width Card
            LearnFeatureCard(
                title = "Audio Lab",
                iconRes = R.drawable.ic_speaker,
                faceColor = Color(0xFF18758C),
                depthColor = Color(0xFF0F4E5E),
                buttonTag = "learn_audio_button",
                titleTag = "learn_audio_title",
                onClick = { Toast.makeText(context, "Audio Lab Coming Soon", Toast.LENGTH_SHORT).show() },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LearnFeatureCard(
    title: String,
    iconRes: Int,
    faceColor: Color,
    depthColor: Color,
    buttonTag: String,
    titleTag: String,
    onClick: () -> Unit,
) {
    CardsPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(buttonTag),
        face = faceColor,
        depth = depthColor,
        padding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flat crisp white vector line icon directly on solid background
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )

            Spacer(Modifier.width(18.dp))

            // Title centered vertically with icon and arrow
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .testTag(titleTag),
            )

            // Arrow forward indicator (crisp line art)
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Open",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = 180f },
            )
        }
    }
}

@Composable
private fun LearnSquareCard(
    title: String,
    iconRes: Int? = null,
    glyphText: String? = null,
    faceColor: Color,
    depthColor: Color,
    buttonTag: String,
    titleTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardsPressable(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .testTag(buttonTag),
        face = faceColor,
        depth = depthColor,
        padding = PaddingValues(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Leading flat icon/glyph and arrow indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (glyphText != null) {
                    Text(
                        text = glyphText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalJapaneseFont.current,
                        color = Color.White,
                    )
                } else if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Open",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(15.dp)
                        .graphicsLayer { rotationZ = 180f },
                )
            }

            // Bottom: Title cleanly positioned with room to breathe
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(titleTag),
            )
        }
    }
}

