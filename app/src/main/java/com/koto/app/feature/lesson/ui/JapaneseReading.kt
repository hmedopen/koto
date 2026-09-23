package com.koto.app.feature.lesson.ui

import com.koto.app.feature.lesson.model.JapaneseText

/** One visually inseparable kana reading unit (for example, きょ / kyo). */
internal data class KanaReadingUnit(
    val kana: String,
    val romaji: String,
    val breakBefore: Boolean = false,
)

/**
 * Aligns authored romaji to kana without changing the lesson data. The supplied
 * reading wins for particles (は/wa, へ/e, を/o) and accepted Hepburn variants.
 */
internal fun kanaReadingUnits(text: JapaneseText): List<KanaReadingUnit> {
    val kanaUnits = segmentKana(text.kana)
    val romaji = text.romaji.lowercase()
    val result = mutableListOf<KanaReadingUnit>()
    var romajiIndex = 0
    var breakBeforeNext = false

    kanaUnits.forEachIndexed { index, kana ->
        if (kana.all(Char::isWhitespace)) {
            while (romajiIndex < romaji.length && romaji[romajiIndex].isWhitespace()) romajiIndex++
            breakBeforeNext = true
            return@forEachIndexed
        }
        if (kana.first() in japanesePunctuation) {
            while (romajiIndex < romaji.length && isReadingSeparator(romaji[romajiIndex])) romajiIndex++
            result += KanaReadingUnit(kana, "", breakBeforeNext)
            breakBeforeNext = true
            return@forEachIndexed
        }

        var skippedWhitespace = false
        while (romajiIndex < romaji.length && isReadingSeparator(romaji[romajiIndex])) {
            skippedWhitespace = skippedWhitespace || romaji[romajiIndex].isWhitespace()
            romajiIndex++
        }
        val remaining = romaji.substring(romajiIndex)
        val candidates = readingsFor(kana)
        val matched = candidates.sortedByDescending(String::length).firstOrNull(remaining::startsWith)
        val reading = when {
            matched != null -> matched.also { romajiIndex += it.length }
            kana == "っ" || kana == "ッ" -> remaining.firstOrNull()
                ?.takeIf { it.isLetter() && remaining.length > 1 && remaining[1] == it }
                ?.toString().orEmpty().also { romajiIndex += it.length }
            else -> {
                val nextCandidates = kanaUnits.drop(index + 1)
                    .firstOrNull { it.none(Char::isWhitespace) && it.first() !in japanesePunctuation }
                    ?.let(::readingsFor).orEmpty()
                val nextStart = nextCandidates.mapNotNull { candidate ->
                    romaji.indexOf(candidate, startIndex = romajiIndex + 1).takeIf { it >= 0 }
                }.minOrNull()
                val separator = (romajiIndex until romaji.length)
                    .firstOrNull { isReadingSeparator(romaji[it]) }
                val end = listOfNotNull(nextStart, separator).minOrNull() ?: romaji.length
                romaji.substring(romajiIndex, end).also { romajiIndex = end }
            }
        }
        result += KanaReadingUnit(kana, reading, breakBeforeNext || skippedWhitespace)
        breakBeforeNext = false
    }
    return result
}

private fun segmentKana(value: String): List<String> = buildList {
    var index = 0
    while (index < value.length) {
        val current = value[index].toString()
        val pair = if (index + 1 < value.length) current + value[index + 1] else current
        if (pair.length == 2 && readingsFor(pair).isNotEmpty()) {
            add(pair)
            index += 2
        } else {
            add(current)
            index++
        }
    }
}

private fun readingsFor(kana: String): List<String> {
    val normalized = kana.map { character ->
        if (character in 'ァ'..'ヶ') (character.code - 0x60).toChar() else character
    }.joinToString("")
    return kanaReadings[normalized].orEmpty()
}

private fun isReadingSeparator(value: Char) = value.isWhitespace() || value in ".,!?;:()[]"
private val japanesePunctuation = setOf('。', '、', '？', '！', '・', '「', '」', '（', '）')

private val kanaReadings = mapOf(
    "あ" to listOf("a"), "い" to listOf("i"), "う" to listOf("u"), "え" to listOf("e"), "お" to listOf("o"),
    "か" to listOf("ka"), "き" to listOf("ki"), "く" to listOf("ku"), "け" to listOf("ke"), "こ" to listOf("ko"),
    "が" to listOf("ga"), "ぎ" to listOf("gi"), "ぐ" to listOf("gu"), "げ" to listOf("ge"), "ご" to listOf("go"),
    "さ" to listOf("sa"), "し" to listOf("shi", "si"), "す" to listOf("su"), "せ" to listOf("se"), "そ" to listOf("so"),
    "ざ" to listOf("za"), "じ" to listOf("ji", "zi"), "ず" to listOf("zu"), "ぜ" to listOf("ze"), "ぞ" to listOf("zo"),
    "た" to listOf("ta"), "ち" to listOf("chi", "ti"), "つ" to listOf("tsu", "tu"), "て" to listOf("te"), "と" to listOf("to"),
    "だ" to listOf("da"), "ぢ" to listOf("ji", "di"), "づ" to listOf("zu", "du"), "で" to listOf("de"), "ど" to listOf("do"),
    "な" to listOf("na"), "に" to listOf("ni"), "ぬ" to listOf("nu"), "ね" to listOf("ne"), "の" to listOf("no"),
    "は" to listOf("ha", "wa"), "ひ" to listOf("hi"), "ふ" to listOf("fu", "hu"), "へ" to listOf("he", "e"), "ほ" to listOf("ho"),
    "ば" to listOf("ba"), "び" to listOf("bi"), "ぶ" to listOf("bu"), "べ" to listOf("be"), "ぼ" to listOf("bo"),
    "ぱ" to listOf("pa"), "ぴ" to listOf("pi"), "ぷ" to listOf("pu"), "ぺ" to listOf("pe"), "ぽ" to listOf("po"),
    "ま" to listOf("ma"), "み" to listOf("mi"), "む" to listOf("mu"), "め" to listOf("me"), "も" to listOf("mo"),
    "や" to listOf("ya"), "ゆ" to listOf("yu"), "よ" to listOf("yo"),
    "ら" to listOf("ra"), "り" to listOf("ri"), "る" to listOf("ru"), "れ" to listOf("re"), "ろ" to listOf("ro"),
    "わ" to listOf("wa"), "ゐ" to listOf("wi"), "ゑ" to listOf("we"), "を" to listOf("o", "wo"), "ん" to listOf("n"),
    "ゔ" to listOf("vu"), "ぁ" to listOf("a"), "ぃ" to listOf("i"), "ぅ" to listOf("u"), "ぇ" to listOf("e"), "ぉ" to listOf("o"),
    "きゃ" to listOf("kya"), "きゅ" to listOf("kyu"), "きょ" to listOf("kyo"),
    "ぎゃ" to listOf("gya"), "ぎゅ" to listOf("gyu"), "ぎょ" to listOf("gyo"),
    "しゃ" to listOf("sha", "sya"), "しゅ" to listOf("shu", "syu"), "しょ" to listOf("sho", "syo"),
    "じゃ" to listOf("ja", "jya", "zya"), "じゅ" to listOf("ju", "jyu", "zyu"), "じょ" to listOf("jo", "jyo", "zyo"),
    "ちゃ" to listOf("cha", "tya"), "ちゅ" to listOf("chu", "tyu"), "ちょ" to listOf("cho", "tyo"),
    "にゃ" to listOf("nya"), "にゅ" to listOf("nyu"), "にょ" to listOf("nyo"),
    "ひゃ" to listOf("hya"), "ひゅ" to listOf("hyu"), "ひょ" to listOf("hyo"),
    "びゃ" to listOf("bya"), "びゅ" to listOf("byu"), "びょ" to listOf("byo"),
    "ぴゃ" to listOf("pya"), "ぴゅ" to listOf("pyu"), "ぴょ" to listOf("pyo"),
    "みゃ" to listOf("mya"), "みゅ" to listOf("myu"), "みょ" to listOf("myo"),
    "りゃ" to listOf("rya"), "りゅ" to listOf("ryu"), "りょ" to listOf("ryo"),
    "ふぁ" to listOf("fa"), "ふぃ" to listOf("fi"), "ふぇ" to listOf("fe"), "ふぉ" to listOf("fo"),
    "てぃ" to listOf("ti"), "でぃ" to listOf("di"), "ゔぁ" to listOf("va"), "ゔぃ" to listOf("vi"),
    "ゔぇ" to listOf("ve"), "ゔぉ" to listOf("vo"),
)
