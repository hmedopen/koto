package com.koto.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.koto.app.R

/** Shared lesson and card study controls. */
@Composable
fun SessionControlBar(progress: Float, onClose: () -> Unit, onSettings: () -> Unit,
    closeTag: String, progressTag: String, settingsTag: String,
    closeDescription: String, settingsDescription: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TactileButton(onClose, Modifier.size(48.dp, 52.dp).testTag(closeTag),
            tone = TactileTone.Quiet, description = closeDescription, padding = PaddingValues(12.dp)) {
            Icon(painterResource(R.drawable.ic_close), null, Modifier.size(24.dp))
        }
        // The controls have a 48 dp face and 4 dp bottom edge. Align the
        // rail's center to the face/icon centers, not the bottom shadow.
        SessionProgress(progress, Modifier.weight(1f).height(12.dp)
            .offset(y = (-2).dp).testTag(progressTag))
        TactileButton(onSettings, Modifier.size(48.dp, 52.dp).testTag(settingsTag),
            tone = TactileTone.Quiet, description = settingsDescription, padding = PaddingValues(12.dp)) {
            Icon(painterResource(R.drawable.ic_settings), null, Modifier.size(24.dp))
        }
    }
}
