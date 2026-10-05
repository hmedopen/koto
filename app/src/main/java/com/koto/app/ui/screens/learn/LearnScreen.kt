package com.koto.app.ui.screens.learn

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.koto.app.ui.theme.KotoType

@Composable
fun LearnScreen(
    onOpenTranslator: () -> Unit = {},
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


            // 1. Translate (Primary Action Card - Blue)
            LearnFeatureCard(
                title = "Translate",
                subtitle = "English ⇄ Japanese",
                iconRes = R.drawable.ic_swap,
                faceColor = CardsColors.Blue,
                depthColor = CardsColors.BlueDepth,
                buttonTag = "learn_translate_button",
                titleTag = "learn_translate_title",
                subtitleTag = "learn_translate_subtitle",
                onClick = onOpenTranslator,
            )

            // 2. Grammar Library (Emerald Green)
            LearnFeatureCard(
                title = "Grammar Library",
                subtitle = "Particles, conjugation & syntax",
                iconRes = R.drawable.ic_document,
                faceColor = Color(0xFF1E7E55),
                depthColor = Color(0xFF145338),
                buttonTag = "learn_grammar_button",
                titleTag = "learn_grammar_title",
                subtitleTag = "learn_grammar_subtitle",
                onClick = { Toast.makeText(context, "Grammar Library Coming Soon", Toast.LENGTH_SHORT).show() },
            )

            // 3. Kanji Stroke Canvas (Indigo / Purple)
            LearnFeatureCard(
                title = "Kanji Canvas",
                subtitle = "Radicals, stroke order & writing",
                iconRes = R.drawable.ic_edit,
                faceColor = Color(0xFF5B4393),
                depthColor = Color(0xFF3E2D64),
                buttonTag = "learn_kanji_button",
                titleTag = "learn_kanji_title",
                subtitleTag = "learn_kanji_subtitle",
                onClick = { Toast.makeText(context, "Kanji Canvas Coming Soon", Toast.LENGTH_SHORT).show() },
            )

            // 4. Phrasebook & Idioms (Coral)
            LearnFeatureCard(
                title = "Phrasebook",
                subtitle = "Daily expressions & idioms",
                iconRes = R.drawable.ic_trophy,
                faceColor = Color(0xFFC04B33),
                depthColor = Color(0xFF833323),
                buttonTag = "learn_phrasebook_button",
                titleTag = "learn_phrasebook_title",
                subtitleTag = "learn_phrasebook_subtitle",
                onClick = { Toast.makeText(context, "Phrasebook Coming Soon", Toast.LENGTH_SHORT).show() },
            )

            // 5. Audio & Listening Lab (Teal)
            LearnFeatureCard(
                title = "Audio Lab",
                subtitle = "Pitch accent & listening drills",
                iconRes = R.drawable.ic_speaker,
                faceColor = Color(0xFF18758C),
                depthColor = Color(0xFF0F4E5E),
                buttonTag = "learn_audio_button",
                titleTag = "learn_audio_title",
                subtitleTag = "learn_audio_subtitle",
                onClick = { Toast.makeText(context, "Audio Lab Coming Soon", Toast.LENGTH_SHORT).show() },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LearnFeatureCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    faceColor: Color,
    depthColor: Color,
    buttonTag: String,
    titleTag: String,
    subtitleTag: String,
    onClick: () -> Unit,
) {
    CardsPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(buttonTag),
        face = faceColor,
        depth = depthColor,
        padding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon in crisp container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            // Title & Subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag(titleTag),
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.testTag(subtitleTag),
                )
            }

            // Arrow forward indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Open",
                    tint = Color.White,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = 180f },
                )
            }
        }
    }
}
