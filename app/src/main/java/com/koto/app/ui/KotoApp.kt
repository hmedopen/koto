package com.koto.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.koto.app.ui.components.KotoBottomBar
import com.koto.app.ui.components.KotoTopBar
import com.koto.app.ui.navigation.KotoDestination
import com.koto.app.ui.navigation.KotoNavigation
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoDimens
import com.koto.app.ui.theme.KotoTheme
import com.koto.app.ui.screens.map.SettingsPlaceholderSheet

@Composable
fun KotoApp() {
    // Saved instance state preserves the current task through rotation/recreation.
    // A new task has no saved value and always starts on Learn. No disk persistence.
    var selected by rememberSaveable { mutableStateOf(KotoDestination.Learn) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    // Top-level section changes don't accumulate a history of tab taps.
    BackHandler(enabled = selected != KotoDestination.Learn) {
        selected = KotoDestination.Learn
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(KotoColors.Background),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
        ) {
            KotoTopBar(title = selected.title, onSettings = { settingsOpen = true })
            KotoNavigation(selected, Modifier.weight(1f).fillMaxWidth())
        }
        KotoBottomBar(selected = selected, onSelect = { selected = it })
    }
    if (settingsOpen) SettingsPlaceholderSheet(onDismiss = { settingsOpen = false })
}

private val KotoDestination.title: String
    get() = when (this) {
        KotoDestination.Map -> "Map"
        KotoDestination.Learn -> "Learn"
        KotoDestination.Cards -> "Cards"
    }

@Preview(name = "Koto — phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Koto — large text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Preview(name = "Koto — landscape", widthDp = 800, heightDp = 360)
@Composable
private fun KotoPreview() {
    KotoTheme { KotoApp() }
}

