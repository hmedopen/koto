package com.koto.app.ui.screens.cards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone

@Composable
internal fun CreateDeckPlaceholderScreen(
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .testTag("create_deck_placeholder_screen"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Crisp square icon box
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(CardsColors.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, CardsColors.Edge, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = null,
                        tint = CardsColors.Blue,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Text(
                    text = "Custom Decks Coming Soon",
                    color = CardsColors.Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "You will be able to create custom decks word-by-word, import vocabulary from files, and export personal study collections.",
                    color = CardsColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
