package com.koto.app.feature.lesson.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.lesson.model.*
import com.koto.app.ui.components.*
import com.koto.app.ui.theme.KotoColors

@Composable
fun JapaneseTextBlock(text: JapaneseText, modifier: Modifier = Modifier, size: TextUnit = 22.sp) {
    val ink = LocalContentColor.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text.romaji, fontSize = size * .7f, lineHeight = size * .9f,
            color = if (ink == androidx.compose.ui.graphics.Color.White) androidx.compose.ui.graphics.Color(0xFFE3E8EE) else KotoColors.QuietInk,
            textAlign = TextAlign.Center, fontWeight = FontWeight.Normal)
        Text(text.kana, fontSize = size, lineHeight = size * 1.35f,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ContentText(text: LessonText, size: TextUnit = 22.sp) {
    when (text) {
        is LessonText.Japanese -> JapaneseTextBlock(text.value, size = size)
        is LessonText.English -> Text(text.value, fontSize = size, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
    }
}

@Composable
internal fun SpeakerButton(text: JapaneseText, speechReady: Boolean, speak: (JapaneseText) -> Unit) {
    TactileButton({ speak(text) }, Modifier.size(48.dp, 52.dp), enabled = speechReady,
        tone = TactileTone.Quiet, description = "Play Japanese: ${text.romaji}", padding = PaddingValues(10.dp)) {
        Icon(painterResource(R.drawable.ic_speaker), null, modifier = Modifier.size(22.dp), tint = KotoColors.LessonBlue)
    }
}
