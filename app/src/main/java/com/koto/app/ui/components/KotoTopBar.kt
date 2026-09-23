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
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoType

@Composable
fun KotoTopBar(title: String, onSettings: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .drawBehind {
                drawLine(KotoColors.Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f)
            }
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            color = KotoColors.Navy,
            style = KotoType.Brand,
            modifier = Modifier.align(Alignment.Center),
        )
        TactileButton(
            onClick = onSettings,
            modifier = Modifier.align(Alignment.CenterEnd).size(48.dp, 52.dp).testTag("map_settings"),
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
