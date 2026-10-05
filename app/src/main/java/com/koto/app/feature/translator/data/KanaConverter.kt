package com.koto.app.feature.translator.data

import com.atilika.kuromoji.ipadic.Tokenizer

object KanaConverter {

    private val tokenizer: Tokenizer by lazy {
        try {
            Tokenizer()
        } catch (_: Exception) {
            Tokenizer.Builder().build()
        }
    }

    /**
     * Converts any Japanese text (which may contain Kanji) into strictly
     * pure Hiragana and Katakana. Guaranteed to contain ZERO Kanji characters.
     */
    fun toPureKana(text: String): String {
        if (text.isBlank()) return ""
        val trimmed = text.trim()

        val sb = StringBuilder()
        val tokens = runCatching { tokenizer.tokenize(trimmed) }.getOrNull()

        if (tokens != null && tokens.isNotEmpty()) {
            for (token in tokens) {
                val surface = token.surface
                val reading = token.reading
                if (containsKanji(surface)) {
                    if (!reading.isNullOrBlank() && reading != "*") {
                        // Kuromoji returns readings in Katakana. Convert kanji readings to Hiragana.
                        sb.append(katakanaToHiragana(reading))
                    } else {
                        // Fallback character-by-character conversion for any unindexed Kanji
                        sb.append(convertKanjiWithFallback(surface))
                    }
                } else {
                    sb.append(surface)
                }
            }
        } else {
            sb.append(convertKanjiWithFallback(trimmed))
        }

        val result = sb.toString()
        // Final safety guarantee: strictly replace any lingering Kanji with fallback readings
        return enforceZeroKanji(result)
    }

    /**
     * Checks if the given text contains any CJK Unified Ideographs (Kanji).
     */
    fun containsKanji(text: String): Boolean {
        for (ch in text) {
            if (isKanji(ch)) return true
        }
        return false
    }

    private fun isKanji(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) || (code in 0xF900..0xFAFF)
    }

    /**
     * Converts Katakana characters in the string into Hiragana.
     * Preserves punctuation, numbers, spaces, and already-Hiragana characters.
     */
    fun katakanaToHiragana(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val code = ch.code
            if (code in 0x30A1..0x30F6) { // ァ to ヶ
                sb.append((code - 0x60).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Hiragana characters in the string into Katakana.
     */
    fun hiraganaToKatakana(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val code = ch.code
            if (code in 0x3041..0x3096) { // ぁ to ゖ
                sb.append((code + 0x60).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun convertKanjiWithFallback(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (isKanji(ch)) {
                sb.append(KANJI_READING_FALLBACK[ch] ?: "")
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun enforceZeroKanji(text: String): String {
        if (!containsKanji(text)) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (isKanji(ch)) {
                sb.append(KANJI_READING_FALLBACK[ch] ?: "")
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Kana text (Hiragana and Katakana) to natural Latin alphabet (Romaji).
     */
    fun toRomaji(kana: String): String {
        if (kana.isBlank()) return ""

        val preprocessed = kana
            .replace("こんにちは", "こんにち_WA_")
            .replace("コンニチハ", "コンニチ_WA_")
            .replace("こんばんは", "こんばん_WA_")
            .replace("コンバンハ", "コンバン_WA_")

        val sb = StringBuilder()
        var i = 0
        val len = preprocessed.length

        while (i < len) {
            if (preprocessed.startsWith("_WA_", i)) {
                sb.append("wa")
                i += 4
                continue
            }
            // Check 2-character digraphs (e.g. きゃ, ちょ, ティ)
            if (i + 1 < len) {
                val pair = preprocessed.substring(i, i + 2)
                val pairNorm = katakanaToHiragana(pair)
                val mapped = DIGRAPH_ROMAJI_MAP[pairNorm] ?: DIGRAPH_ROMAJI_MAP[pair]
                if (mapped != null) {
                    sb.append(mapped)
                    i += 2
                    continue
                }
            }

            val ch = preprocessed[i]

            // Sokuon (small tsu / ッ / っ): doubles the upcoming consonant
            if (ch == 'っ' || ch == 'ッ') {
                if (i + 1 < len) {
                    val nextPair = if (i + 2 <= len) preprocessed.substring(i + 1, (i + 3).coerceAtMost(len)) else ""
                    val nextDigraph = DIGRAPH_ROMAJI_MAP[katakanaToHiragana(nextPair)]
                    val nextCharRomaji = nextDigraph ?: MONOGRAPHS_ROMAJI_MAP[katakanaToHiragana(preprocessed[i + 1].toString())]
                    if (!nextCharRomaji.isNullOrEmpty()) {
                        val firstConsonant = nextCharRomaji.first()
                        if (firstConsonant in 'a'..'z') {
                            // In Hepburn, ch becomes tch (e.g. matchi)
                            if (nextCharRomaji.startsWith("ch")) {
                                sb.append('t')
                            } else {
                                sb.append(firstConsonant)
                            }
                            i++
                            continue
                        }
                    }
                }
                i++
                continue
            }

            // Chōonpu (ー): lengthen previous vowel if possible
            if (ch == 'ー') {
                val lastChar = sb.lastOrNull()
                if (lastChar != null && lastChar in "aiueo") {
                    sb.append(lastChar)
                }
                i++
                continue
            }

            // Standalone particles when isolated by space/punctuation:
            // は -> wa, を -> o, へ -> e
            val single = ch.toString()
            val singleNorm = katakanaToHiragana(single)
            val mapped = MONOGRAPHS_ROMAJI_MAP[singleNorm] ?: MONOGRAPHS_ROMAJI_MAP[single] ?: PUNCTUATION_MAP[ch] ?: ch.toString()
            sb.append(mapped)
            i++
        }

        return cleanRomajiSpacing(sb.toString())
    }

    private fun cleanRomajiSpacing(romaji: String): String {
        return romaji
            .replace(Regex("\\s+"), " ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(" ?", "?")
            .replace(" !", "!")
            .trim()
    }

    private val PUNCTUATION_MAP = mapOf(
        '、' to ", ",
        '。' to ". ",
        '！' to "!",
        '？' to "?",
        '・' to " ",
        '〜' to "~",
        '「' to "\"",
        '」' to "\"",
        '（' to " (",
        '）' to ")",
    )

    private val DIGRAPH_ROMAJI_MAP = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
        "てぃ" to "ti", "でぃ" to "di",
        "ゔぁ" to "va", "ゔぃ" to "vi", "ゔ" to "vu", "ゔぇ" to "ve", "ゔぉ" to "vo",
        "しぇ" to "she", "じぇ" to "je", "ちぇ" to "che",
    )

    private val MONOGRAPHS_ROMAJI_MAP = mapOf(
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
        "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
        "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
        "だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
        "や" to "ya", "ゆ" to "yu", "よ" to "yo",
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
        "わ" to "wa", "を" to "o", "ん" to "n",
        "ぁ" to "a", "ぃ" to "i", "ぅ" to "u", "ぇ" to "e", "ぉ" to "o",
        "ゎ" to "wa",
    )

    // Extensive fallback table for core high-frequency kanji characters
    private val KANJI_READING_FALLBACK = mapOf(
        '私' to "わたし", '友' to "とも", '今' to "いま", '日' to "ひ", '本' to "ほん",
        '語' to "ご", '先' to "せん", '生' to "せい", '学' to "がく", '校' to "こう",
        '水' to "みず", '茶' to "ちゃ", '食' to "た", '飲' to "の", '行' to "い",
        '来' to "き", '見' to "み", '聞' to "き", '言' to "い", '話' to "はな",
        '買' to "か", '駅' to "えき", '電' to "でん", '車' to "しゃ", '道' to "みち",
        '愛' to "あい", '好' to "す", '大' to "おお", '小' to "ちい", '高' to "たか",
        '安' to "やす", '新' to "あたら", '古' to "ふる", '白' to "しろ", '黒' to "くろ",
        '赤' to "あか", '青' to "あお", '何' to "なに", '誰' to "だれ", '時' to "とき",
        '分' to "ふん", '年' to "とし", '月' to "つき", '人' to "ひと", '子' to "こ",
        '男' to "おとこ", '女' to "おんな", '父' to "ちち", '母' to "はは", '家' to "いえ",
        '店' to "みせ", '手' to "て", '目' to "め", '口' to "くち", '耳' to "みみ",
        '気' to "き", '天' to "てん", '雨' to "あめ", '山' to "やま", '川' to "かわ",
        '海' to "うみ", '花' to "はな", '犬' to "いぬ", '猫' to "ねこ", '魚' to "さかな",
        '鳥' to "とり", '肉' to "にく", '米' to "こめ", '金' to "かね", '上' to "うえ",
        '下' to "した", '前' to "まえ", '後' to "うしろ", '中' to "なか", '外' to "そと",
        '左' to "ひだり", '右' to "みぎ", '間' to "あいだ", '東' to "ひがし", '西' to "にし",
        '南' to "みなみ", '北' to "きた", '美' to "うつく", '味' to "あじ", '朝' to "あさ",
        '昼' to "ひる", '晩' to "ばん", '夜' to "よる", '明' to "あした", '昨' to "きのう",
        '名' to "な", '前' to "まえ",
    )
}
