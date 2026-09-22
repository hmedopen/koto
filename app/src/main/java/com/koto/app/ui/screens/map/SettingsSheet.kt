package com.koto.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.koto.app.feature.lesson.audio.*
import com.koto.app.ui.components.*
import com.koto.app.ui.theme.KotoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(onDismiss: () -> Unit,
    audio: JapaneseTtsController = JapaneseTtsController.get(LocalContext.current)) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KotoColors.Background, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(24.dp).testTag("settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("Japanese audio", style = MaterialTheme.typography.titleMedium)
            Text("Speak Japanese when you select a word or response. English stays silent.",
                color = KotoColors.QuietInk, style = MaterialTheme.typography.bodyMedium)
            TactileButton({ audio.setSpeechEnabled(!audio.enabled) }, Modifier.fillMaxWidth().testTag("settings_sound"),
                tone = if (audio.enabled) TactileTone.Selected else TactileTone.Default,
                selected = audio.enabled, stateLabel = if (audio.enabled) "On" else "Off") {
                Text(if (audio.enabled) "Japanese audio: On" else "Japanese audio: Off", modifier = Modifier.fillMaxWidth())
            }
            Text(when (audio.status) {
                SpeechStatus.Loading -> "Preparing Japanese voice…"
                SpeechStatus.Ready -> "Japanese voice ready on this device."
                SpeechStatus.Unavailable -> "Japanese voice unavailable. Lessons still work without audio."
            }, style = MaterialTheme.typography.bodySmall, color = KotoColors.QuietInk)
            TactileButton(onDismiss, Modifier.fillMaxWidth().testTag("settings_done"), tone = TactileTone.Primary) {
                Text("Done", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
