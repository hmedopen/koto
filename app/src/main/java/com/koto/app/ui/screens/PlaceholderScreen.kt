package com.koto.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.koto.app.R
import com.koto.app.ui.theme.KotoColors
import com.koto.app.ui.theme.KotoDimens
import com.koto.app.ui.theme.KotoType

@Composable
internal fun PlaceholderScreen(@StringRes title: Int, tag: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(tag)
            .verticalScroll(rememberScrollState())
            .padding(KotoDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(KotoDimens.ContentGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(title),
            style = KotoType.ScreenTitle,
            color = KotoColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.placeholder_message),
            style = KotoType.Body,
            color = KotoColors.QuietInk,
            textAlign = TextAlign.Center,
        )
    }
}
