package com.koto.app.ui.navigation

import androidx.annotation.StringRes
import com.koto.app.R

enum class KotoDestination(@param:StringRes val label: Int) {
    Map(R.string.destination_map),
    Learn(R.string.destination_learn),
    Cards(R.string.destination_cards),
}
