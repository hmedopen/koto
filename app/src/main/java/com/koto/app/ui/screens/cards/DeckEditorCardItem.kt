package com.koto.app.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.R
import com.koto.app.feature.cards.data.CustomCardItem
import com.koto.app.ui.components.TactileButton
import com.koto.app.ui.components.TactileTone
import com.koto.app.ui.theme.KotoFont
import java.util.Locale

private val ChipShape = RoundedCornerShape(6.dp)

@Composable
fun DeckEditorCardItem(
    index: Int,
    card: CustomCardItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("deck_card_row_${card.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Index sequence number (01, 02...) on the left
        Text(
            text = String.format(Locale.US, "%02d", index),
            style = TextStyle(
                fontFamily = KotoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = CardsColors.Muted,
            ),
            modifier = Modifier
                .width(28.dp)
                .testTag("deck_card_index_${card.id}"),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Dynamic Center Content: Vertically centered within row
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
        ) {
            // Furigana: Rendered directly above Kanji text in a small font scale (0.55x). Collapses if empty.
            if (!card.furigana.isNullOrBlank()) {
                Text(
                    text = card.furigana,
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp, // ~0.55x of 20sp Japanese font
                        color = CardsColors.Blue,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("deck_card_furigana_${card.id}"),
                )
            }

            // Japanese Row: Primary bold Japanese font + Romaji chip on right edge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = card.japanese,
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = CardsColors.Ink,
                    ),
                    modifier = Modifier.testTag("deck_card_japanese_${card.id}"),
                )

                // Romaji: Subtle chip container on right edge of Japanese row. Collapses if empty.
                if (!card.romaji.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(ChipShape)
                            .background(CardsColors.Surface)
                            .border(1.dp, CardsColors.Edge, ChipShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("deck_card_romaji_chip_${card.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = card.romaji,
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = CardsColors.Muted,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("deck_card_romaji_${card.id}"),
                        )
                    }
                }
            }

            // English: High-contrast text on the next row
            Text(
                text = card.english,
                style = TextStyle(
                    fontFamily = KotoFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = CardsColors.Ink,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("deck_card_english_${card.id}"),
            )

            // Notes: Secondary muted text below English with bullet prefix (•). Collapses if empty.
            if (!card.notes.isNullOrBlank()) {
                Text(
                    text = "• ${card.notes}",
                    style = TextStyle(
                        fontFamily = KotoFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = CardsColors.Muted,
                        lineHeight = 16.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("deck_card_notes_${card.id}"),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Row Actions: Edit and Delete
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TactileButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp, 38.dp)
                    .testTag("deck_card_edit_${card.id}"),
                tone = TactileTone.Quiet,
                description = "Edit Card",
                padding = PaddingValues(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    tint = CardsColors.Blue,
                    modifier = Modifier.size(16.dp),
                )
            }

            TactileButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp, 38.dp)
                    .testTag("deck_card_delete_${card.id}"),
                tone = TactileTone.Quiet,
                description = "Delete Card",
                padding = PaddingValues(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = null,
                    tint = CardsColors.Coral,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
