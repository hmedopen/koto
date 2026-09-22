package com.koto.app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoType
import kotlinx.coroutines.launch

@Composable
fun MapScreen() {
    var selectedNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val prototypeMessage = stringResource(R.string.map_lesson_placeholder)
    val openLevel: (Int) -> Unit = remember { { selectedNumber = it } }

    Box(Modifier.fillMaxSize().background(KotoColors.Background).testTag("screen_map")) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("map_list"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(MapFixtures.rows, key = { it.key }, contentType = {
                    when (it) { is MapRow.Stage -> "stage"; is MapRow.Level -> "level" }
                }) { row ->
                    when (row) {
                        is MapRow.Stage -> StageLabel(row.title)
                        is MapRow.Level -> LevelRow(row.level, openLevel,
                            isLast = row.level.number == 12)
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    MapFixtures.level(selectedNumber)?.let { level ->
        LevelPopup(level, onDismiss = { selectedNumber = null }, onPlay = {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(prototypeMessage)
            }
        })
    }
}

