package com.koto.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPlaceholderSheet(onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Intentionally ephemeral controls: nothing changes app behavior or writes to disk.
    var dropdownOpen by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableIntStateOf(0) }
    var sound by remember { mutableStateOf(true) }
    var reminders by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(.5f) }
    val sliderLabel = stringResource(R.string.map_settings_slider)
    val options = listOf(R.string.map_option_default, R.string.map_option_alternative)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet,
        containerColor = KotoColors.Background, tonalElevation = 0.dp,
        scrimColor = KotoColors.NavyDepth.copy(alpha = .28f)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 24.dp).testTag("settings_sheet")) {
            Text(stringResource(R.string.map_settings), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.map_settings_hint), style = MaterialTheme.typography.bodyMedium,
                color = KotoColors.QuietInk)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.map_settings_dropdown), style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(onClick = { dropdownOpen = true }, modifier = Modifier.testTag("settings_dropdown")) {
                    Text(stringResource(options[selectedOption]))
                }
                DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                    options.forEachIndexed { index, label ->
                        DropdownMenuItem(text = { Text(stringResource(label)) },
                            onClick = { selectedOption = index; dropdownOpen = false })
                    }
                }
            }
            PlaceholderSwitch(stringResource(R.string.map_settings_sound), sound, { sound = it }, "settings_sound")
            PlaceholderSwitch(stringResource(R.string.map_settings_reminders), reminders, { reminders = it }, "settings_reminders")
            Spacer(Modifier.height(12.dp))
            Text(sliderLabel, style = MaterialTheme.typography.bodyMedium)
            Slider(value = volume, onValueChange = { volume = it }, modifier = Modifier
                .testTag("settings_slider").semantics { contentDescription = sliderLabel })
            TextButton(onClick = { scope.launch { sheet.hide(); onDismiss() } },
                modifier = Modifier.align(Alignment.End).testTag("settings_done")) {
                Text(stringResource(R.string.map_done))
            }
        }
    }
}

@Composable
private fun PlaceholderSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange,
            modifier = Modifier.testTag(tag).semantics { contentDescription = label })
    }
}
