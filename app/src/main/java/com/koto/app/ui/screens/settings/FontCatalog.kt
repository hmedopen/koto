package com.koto.app.ui.screens.settings

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.koto.app.R

data class AppFont(
    val id: String,
    val name: String,
    val sampleGlyph: String,
    val fontFamily: FontFamily,
    val isJapanese: Boolean = false,
)

object FontCatalog {
    val englishFonts: List<AppFont> = listOf(
        AppFont("inter_roboto", "Inter / Roboto", "a", FontFamily(Font(R.font.font_inter))),
        AppFont("plus_jakarta_sans", "Plus Jakarta Sans", "a", FontFamily(Font(R.font.font_plus_jakarta_sans))),
        AppFont("outfit", "Outfit", "a", FontFamily(Font(R.font.font_outfit))),
        AppFont("nunito", "Nunito", "a", FontFamily(Font(R.font.font_nunito))),
        AppFont("manrope", "Manrope", "a", FontFamily(Font(R.font.font_manrope))),
        AppFont("dm_sans", "DM Sans", "a", FontFamily(Font(R.font.font_dm_sans))),
        AppFont("lexend_deca", "Lexend Deca", "a", FontFamily(Font(R.font.font_lexend_deca))),
        AppFont("rubik", "Rubik", "a", FontFamily(Font(R.font.font_rubik))),
        AppFont("urbanist", "Urbanist", "a", FontFamily(Font(R.font.font_urbanist))),
    )

    val japaneseFonts: List<AppFont> = listOf(
        AppFont("noto_sans_jp", "Noto Sans JP", "あ", FontFamily(Font(R.font.font_noto_sans_jp)), isJapanese = true),
        AppFont("m_plus_1p", "M PLUS 1p", "あ", FontFamily(Font(R.font.font_m_plus_1p)), isJapanese = true),
        AppFont("zen_kaku_gothic", "Zen Kaku Gothic New", "あ", FontFamily(Font(R.font.font_zen_kaku_gothic)), isJapanese = true),
        AppFont("zen_maru_gothic", "Zen Maru Gothic", "あ", FontFamily(Font(R.font.font_zen_maru_gothic)), isJapanese = true),
        AppFont("kosugi_maru", "Kosugi Maru", "あ", FontFamily(Font(R.font.font_kosugi_maru)), isJapanese = true),
        AppFont("noto_serif_jp", "Noto Serif JP", "あ", FontFamily(Font(R.font.font_noto_serif_jp)), isJapanese = true),
        AppFont("shippori_mincho", "Shippori Mincho", "あ", FontFamily(Font(R.font.font_shippori_mincho)), isJapanese = true),
        AppFont("klee_one", "Klee One", "あ", FontFamily(Font(R.font.font_klee_one)), isJapanese = true),
        AppFont("kaisei_decol", "Kaisei Decol", "あ", FontFamily(Font(R.font.font_kaisei_decol)), isJapanese = true),
    )

    fun findEnglishFont(id: String): AppFont =
        englishFonts.firstOrNull { it.id == id } ?: englishFonts.first()

    fun findJapaneseFont(id: String): AppFont =
        japaneseFonts.firstOrNull { it.id == id } ?: japaneseFonts.first()
}
