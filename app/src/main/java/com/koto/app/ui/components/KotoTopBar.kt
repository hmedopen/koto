package com.koto.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row

import androidx.compose.ui.text.style.TextOverflow

@Composable
fun KotoTopBar(
    title: String,
    onSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    onRandomDeck: (() -> Unit)? = null,
    onStarredWords: (() -> Unit)? = null,
    onCreateDeck: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .shadow(
                elevation = 4.dp,
                shape = androidx.compose.ui.graphics.RectangleShape,
                clip = false,
                ambientColor = Color(0x201A3761),
                spotColor = Color(0x301A3761),
            )
            .background(Color.White)
            .heightIn(min = 64.dp)
            .drawBehind {
                drawLine(KotoColors.Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f)
            }
            .padding(horizontal = 16.dp),
    ) {
        if (onBack != null || onStarredWords != null || onCreateDeck != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    TactileButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp, 52.dp).testTag("cards_detail_back"),
                        tone = TactileTone.Quiet,
                        description = "Back to all decks",
                        padding = PaddingValues(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = null,
                            tint = KotoColors.Navy,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                if (onStarredWords != null) {
                    TactileButton(
                        onClick = onStarredWords,
                        modifier = Modifier.size(48.dp, 52.dp).testTag("cards_starred_words"),
                        tone = TactileTone.Quiet,
                        description = "Starred words",
                        padding = PaddingValues(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_review_star),
                            contentDescription = null,
                            tint = KotoColors.Navy,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                if (onCreateDeck != null) {
                    TactileButton(
                        onClick = onCreateDeck,
                        modifier = Modifier.size(48.dp, 52.dp).testTag("cards_create_deck"),
                        tone = TactileTone.Quiet,
                        description = "Create deck",
                        padding = PaddingValues(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = null,
                            tint = KotoColors.Navy,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        Text(
            text = title,
            color = KotoColors.Navy,
            style = KotoType.Brand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("top_bar_title"),
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onRandomDeck != null) {
                TactileButton(
                    onClick = onRandomDeck,
                    modifier = Modifier.size(48.dp, 52.dp).testTag("cards_random_deck"),
                    tone = TactileTone.Quiet,
                    description = "Surprise Me: Random deck",
                    padding = PaddingValues(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dice),
                        contentDescription = null,
                        tint = KotoColors.Navy,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            TactileButton(
                onClick = onSettings,
                modifier = Modifier.size(48.dp, 52.dp).testTag("map_settings"),
                tone = TactileTone.Quiet,
                description = stringResource(R.string.map_settings),
                padding = PaddingValues(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                    tint = KotoColors.Navy,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
