package com.koto.app.ui.screens.settings

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.koto.app.R
import com.koto.app.ui.theme.KotoFont

data class AppFont(
    val id: String,
    val name: String,
    val sampleGlyph: String,
    val fontFamily: FontFamily,
    val isJapanese: Boolean = false,
)

object FontCatalog {
    private val fontProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

    private fun googleFontFamily(fontName: String, fallback: FontFamily = FontFamily.SansSerif): FontFamily {
        return try {
            FontFamily(
                Font(googleFont = GoogleFont(fontName), fontProvider = fontProvider),
            )
        } catch (_: Throwable) {
            fallback
        }
    }

    val englishFonts: List<AppFont> = listOf(
        AppFont("inter_roboto", "Inter / Roboto", "a", googleFontFamily("Inter", FontFamily.SansSerif)),
        AppFont("plus_jakarta_sans", "Plus Jakarta Sans", "a", googleFontFamily("Plus Jakarta Sans", FontFamily.SansSerif)),
        AppFont("outfit", "Outfit", "a", googleFontFamily("Outfit", FontFamily.SansSerif)),
        AppFont("nunito", "Nunito", "a", googleFontFamily("Nunito", FontFamily.SansSerif)),
        AppFont("manrope", "Manrope", "a", googleFontFamily("Manrope", FontFamily.SansSerif)),
        AppFont("dm_sans", "DM Sans", "a", googleFontFamily("DM Sans", FontFamily.SansSerif)),
        AppFont("lexend_deca", "Lexend Deca", "a", googleFontFamily("Lexend Deca", FontFamily.SansSerif)),
        AppFont("rubik", "Rubik", "a", googleFontFamily("Rubik", FontFamily.SansSerif)),
        AppFont("urbanist", "Urbanist", "a", googleFontFamily("Urbanist", FontFamily.SansSerif)),
    )

    val japaneseFonts: List<AppFont> = listOf(
        AppFont("noto_sans_jp", "Noto Sans JP", "あ", googleFontFamily("Noto Sans JP", KotoFont), isJapanese = true),
        AppFont("m_plus_1p", "M PLUS 1p", "あ", KotoFont, isJapanese = true),
        AppFont("zen_kaku_gothic", "Zen Kaku Gothic New", "あ", googleFontFamily("Zen Kaku Gothic New", KotoFont), isJapanese = true),
        AppFont("zen_maru_gothic", "Zen Maru Gothic", "あ", googleFontFamily("Zen Maru Gothic", KotoFont), isJapanese = true),
        AppFont("kosugi_maru", "Kosugi Maru", "あ", googleFontFamily("Kosugi Maru", KotoFont), isJapanese = true),
        AppFont("noto_serif_jp", "Noto Serif JP", "あ", googleFontFamily("Noto Serif JP", FontFamily.Serif), isJapanese = true),
        AppFont("shippori_mincho", "Shippori Mincho", "あ", googleFontFamily("Shippori Mincho", FontFamily.Serif), isJapanese = true),
        AppFont("klee_one", "Klee One", "あ", googleFontFamily("Klee One", KotoFont), isJapanese = true),
        AppFont("kaisei_decol", "Kaisei Decol", "あ", googleFontFamily("Kaisei Decol", FontFamily.Serif), isJapanese = true),
    )

    fun findEnglishFont(id: String): AppFont =
        englishFonts.firstOrNull { it.id == id } ?: englishFonts.first()

    fun findJapaneseFont(id: String): AppFont =
        japaneseFonts.firstOrNull { it.id == id } ?: japaneseFonts.first()
}
