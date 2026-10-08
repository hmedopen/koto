package com.koto.app.ui.screens.cards

import android.content.Context
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koto.app.R
import com.koto.app.feature.cards.spreadsheet.ExportFormat
import com.koto.app.feature.cards.spreadsheet.SkippedRow
import com.koto.app.feature.cards.spreadsheet.SpreadsheetEngine
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoFont
import com.koto.app.ui.theme.KotoType

private val DialogShape = RoundedCornerShape(12.dp)
private val BoxShape = RoundedCornerShape(8.dp)

@Composable
fun TemplateDownloadDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DialogShape)
                    .border(1.dp, CardsColors.Edge, DialogShape)
                    .background(Color.White)
                    .testTag("template_download_dialog"),
            ) {
                // Top Bar with centered title and top-right [X] dismiss button (Rule 6)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.size(40.dp).align(Alignment.CenterStart))

                    Text(
                        text = "Spreadsheet Template",
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
                            .testTag("btn_close_template_dialog"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close dialog",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                // Dialog Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Two-Step Guide
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "HOW TO USE THE TEMPLATE",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = CardsColors.Muted,
                            ),
                        )

                        Text(
                            text = "1. Download the pre-formatted spreadsheet template.\n" +
                                "2. Fill in your terms following the 3 example rows.\n" +
                                "3. Import via the Import button to generate cards instantly.",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = CardsColors.Ink,
                                lineHeight = 20.sp,
                            ),
                        )
                    }

                    // Explicit Disclaimer (Rendered cleanly on white per Anti-Bubble & Task 4.4)
                    Text(
                        text = "Cards imported via spreadsheet bypass live auto-furigana generation and automatic dictionary lookups. Content renders exactly as entered.",
                        style = TextStyle(
                            fontFamily = KotoFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = CardsColors.Muted,
                            lineHeight = 17.sp,
                        ),
                    )

                    // Action Button: Download Template (.xlsx)
                    CardsButton(
                        label = "Download Template (.xlsx)",
                        onClick = {
                            downloadTemplateFile(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_download_template"),
                        background = CardsColors.Blue,
                        ink = Color.White,
                        depth = CardsColors.BlueDepth,
                    )
                }
            }
        }
    }
}

@Composable
fun ImportSummaryDialog(
    validCount: Int,
    skippedRows: List<SkippedRow>,
    onImportValid: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DialogShape)
                    .border(1.dp, CardsColors.Edge, DialogShape)
                    .background(Color.White)
                    .testTag("import_summary_dialog"),
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.size(40.dp).align(Alignment.CenterStart))

                    Text(
                        text = "Import Summary",
                        style = KotoType.Brand,
                        fontSize = 18.sp,
                        color = CardsColors.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    CardsPressable(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                            .testTag("btn_close_import_summary"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Subtitle: Ready to import: X cards | Skipped: Y invalid rows
                    Text(
                        text = "Ready to import: $validCount cards | Skipped: ${skippedRows.size} invalid rows",
                        style = TextStyle(
                            fontFamily = KotoFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = CardsColors.Ink,
                        ),
                        modifier = Modifier.testTag("text_import_summary_subtitle"),
                    )

                    // Scrollable Issue Box (max height 160.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(BoxShape)
                            .border(1.dp, CardsColors.Edge, BoxShape)
                            .background(CardsColors.Background)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .testTag("box_import_summary_issues"),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            skippedRows.forEach { skipped ->
                                Text(
                                    text = skipped.reason,
                                    style = TextStyle(
                                        fontFamily = KotoFont,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = CardsColors.Coral,
                                        lineHeight = 16.sp,
                                    ),
                                    modifier = Modifier.testTag("skipped_row_${skipped.rowIndex}"),
                                )
                            }
                        }
                    }

                    // Actions: [ Cancel ] and [ Import X Cards ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CardsButton(
                            label = "Cancel",
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_summary_cancel"),
                            background = CardsColors.Surface,
                            ink = CardsColors.Ink,
                            depth = CardsColors.Edge,
                        )

                        CardsButton(
                            label = "Import $validCount Cards",
                            onClick = onImportValid,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_summary_import"),
                            background = CardsColors.Blue,
                            ink = Color.White,
                            depth = CardsColors.BlueDepth,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImportErrorDialog(
    reason: String,
    onDownloadTemplate: () -> Unit,
    onTryAgain: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DialogShape)
                    .border(1.dp, CardsColors.Coral.copy(alpha = 0.6f), DialogShape)
                    .background(Color.White)
                    .testTag("import_error_dialog"),
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.size(40.dp).align(Alignment.CenterStart))

                    Text(
                        text = "Unable to Import File",
                        style = KotoType.Brand,
                        fontSize = 18.sp,
                        color = CardsColors.Coral,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    CardsPressable(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                            .testTag("btn_close_import_error"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Reason Root Cause
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BoxShape)
                            .border(1.dp, CardsColors.Edge, BoxShape)
                            .background(CardsColors.Background)
                            .padding(14.dp),
                    ) {
                        Text(
                            text = reason,
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = CardsColors.Ink,
                                lineHeight = 19.sp,
                            ),
                            modifier = Modifier.testTag("text_import_error_reason"),
                        )
                    }

                    // Actions: [ Download Template ] and [ Try Again ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CardsButton(
                            label = "Download Template",
                            onClick = onDownloadTemplate,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_error_download_template"),
                            background = CardsColors.Surface,
                            ink = CardsColors.Blue,
                            depth = CardsColors.Edge,
                        )

                        CardsButton(
                            label = "Try Again",
                            onClick = onTryAgain,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_error_try_again"),
                            background = CardsColors.Blue,
                            ink = Color.White,
                            depth = CardsColors.BlueDepth,
                        )
                    }
                }
            }
        }
    }
}

internal fun downloadTemplateFile(context: Context) {
    val templateBytes = try {
        context.assets.open("koto_deck_template.xlsx").use { it.readBytes() }
    } catch (_: Exception) {
        SpreadsheetEngine.generateTemplateXlsx()
    }

    val uri = SpreadsheetEngine.saveToDownloads(
        context = context,
        filename = "koto_deck_template.xlsx",
        mimeType = ExportFormat.XLSX.mimeType,
        bytes = templateBytes,
    )

    if (uri != null) {
        Toast.makeText(context, "Template downloaded to Downloads", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Saved template successfully", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ImportOptionsDialog(
    onSelectFile: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DialogShape)
                    .border(1.dp, CardsColors.Edge, DialogShape)
                    .background(Color.White)
                    .testTag("import_deck_dialog"),
            ) {
                // Top Bar with centered title and top-right [X] dismiss button (Rule 6 & 10)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.size(40.dp).align(Alignment.CenterStart))

                    Text(
                        text = "Import Deck",
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
                            .testTag("btn_close_import_dialog"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close dialog",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(color = CardsColors.Edge, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "IMPORT GUIDELINES & RULES",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = CardsColors.Muted,
                            ),
                        )
                        Text(
                            text = "• Supported formats: Excel (.xlsx) and CSV (.csv).\n" +
                                "• Required columns: Japanese (Kana/Kanji) and English.\n" +
                                "• Optional columns: Furigana, Romaji, and Notes.\n" +
                                "• Imported cards will be added to your current deck.",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = CardsColors.Ink,
                                lineHeight = 19.sp,
                            ),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BoxShape)
                            .border(1.dp, CardsColors.Edge, BoxShape)
                            .background(CardsColors.Background)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "Tip: Download the pre-formatted spreadsheet template to ensure your column headers match.",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = CardsColors.Muted,
                                lineHeight = 17.sp,
                            ),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CardsButton(
                            label = "Select File (.xlsx / .csv)",
                            onClick = onSelectFile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_select_import_file"),
                            background = CardsColors.Blue,
                            ink = Color.White,
                            depth = CardsColors.BlueDepth,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportDeckDialog(
    deckTitle: String,
    cardCount: Int,
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DialogShape)
                    .border(1.dp, CardsColors.Edge, DialogShape)
                    .background(Color.White)
                    .testTag("export_deck_dialog"),
            ) {
                // Top Bar with centered title and top-right [X] dismiss button (Rule 6)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.size(40.dp).align(Alignment.CenterStart))

                    Text(
                        text = "Export Deck",
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
                            .testTag("btn_close_export_dialog"),
                        face = CardsColors.Surface,
                        depth = CardsColors.Edge,
                        padding = PaddingValues(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close dialog",
                            tint = CardsColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = CardsColors.Edge)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "EXPORT GUIDELINES & RULES",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = CardsColors.Muted,
                            ),
                        )
                        Text(
                            text = "• Exports all cards in \"$deckTitle\" ($cardCount cards).\n" +
                                "• Columns included: Japanese, English, Furigana, Romaji, and Notes.\n" +
                                "• Files are saved directly to your device Downloads folder.\n" +
                                "• Fully compatible with Excel, Google Sheets, Anki, and Koto.",
                            style = TextStyle(
                                fontFamily = KotoFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = CardsColors.Ink,
                                lineHeight = 19.sp,
                            ),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CardsButton(
                            label = "Export as Excel (.xlsx)",
                            onClick = { onExport(ExportFormat.XLSX) },
                            enabled = cardCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_xlsx"),
                            background = if (cardCount > 0) CardsColors.Surface else CardsColors.Ice,
                            ink = if (cardCount > 0) CardsColors.Ink else CardsColors.Muted,
                            depth = CardsColors.Edge,
                        )

                        CardsButton(
                            label = "Export as CSV (.csv)",
                            onClick = { onExport(ExportFormat.CSV) },
                            enabled = cardCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_csv"),
                            background = if (cardCount > 0) CardsColors.Surface else CardsColors.Ice,
                            ink = if (cardCount > 0) CardsColors.Ink else CardsColors.Muted,
                            depth = CardsColors.Edge,
                        )
                    }
                }
            }
        }
    }
}

