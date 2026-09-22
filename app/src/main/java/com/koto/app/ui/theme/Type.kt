package com.koto.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.koto.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val KotoFont = FontFamily(
    Font(R.font.mplus_rounded_1c_regular, FontWeight.Normal),
    Font(R.font.mplus_rounded_1c_medium, FontWeight.Medium),
    Font(R.font.mplus_rounded_1c_bold, FontWeight.Bold),
)

object KotoType {
    val Brand = TextStyle(
        fontFamily = KotoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.5).sp,
    )
    val ScreenTitle = TextStyle(
        fontFamily = KotoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp,
    )
    val Body = TextStyle(
        fontFamily = KotoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    )
    // Fixed metrics and weight: selection never changes label geometry.
    val Navigation = TextStyle(
        fontFamily = KotoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    )
}

private val DefaultTypography = Typography()
val KotoTypography = Typography(
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = KotoFont),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = KotoFont),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = KotoFont),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = KotoFont),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = KotoFont),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = KotoFont),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = KotoFont),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = KotoFont),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = KotoFont),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = KotoFont),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = KotoFont),
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = KotoFont),
    titleLarge = KotoType.ScreenTitle,
    bodyMedium = KotoType.Body,
    labelMedium = KotoType.Navigation,
)
