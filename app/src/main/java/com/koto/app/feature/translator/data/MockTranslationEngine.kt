package com.koto.app.feature.translator.data

import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.feature.translator.model.TranslationResult

object MockTranslationEngine {

    private data class Entry(
        val english: String,
        val japanese: String,
        val romaji: String,
        val kana: String = "",
    )

    private val DICTIONARY = listOf(
        Entry("hello", "こんにちは", "konnichiwa", "こんにちは"),
        Entry("hi", "こんにちは", "konnichiwa", "こんにちは"),
        Entry("hello friend", "こんにちは、 友よ", "konnichiwa, tomoyo", "こんにちは、 ともよ"),
        Entry("hello, friend", "こんにちは、 友よ", "konnichiwa, tomoyo", "こんにちは、 ともよ"),
        Entry("friend", "友達", "tomodachi", "ともだち"),
        Entry("my friend", "私の友達", "watashi no tomodachi", "わたしのともだち"),
        Entry("good morning", "おはようございます", "ohayou gozaimasu", "おはようございます"),
        Entry("good evening", "こんばんは", "konbanwa", "こんばんは"),
        Entry("good night", "おやすみなさい", "oyasuminasai", "おやすみなさい"),
        Entry("thank you", "ありがとうございます", "arigatou gozaimasu", "ありがとうございます"),
        Entry("thanks", "ありがとう", "arigatou", "ありがとう"),
        Entry("thank you very much", "どうもありがとうございます", "doumo arigatou gozaimasu", "どうもありがとうございます"),
        Entry("you are welcome", "どういたしまして", "douitashimashite", "どういたしまして"),
        Entry("welcome", "ようこそ", "youkoso", "ようこそ"),
        Entry("goodbye", "さようなら", "sayounara", "さようなら"),
        Entry("see you later", "またね", "mata ne", "またね"),
        Entry("yes", "はい", "hai", "はい"),
        Entry("no", "いいえ", "iie", "いいえ"),
        Entry("please", "お願いします", "onegaishimasu", "おねがいします"),
        Entry("excuse me", "すみません", "sumimasen", "すみません"),
        Entry("sorry", "ごめんなさい", "gomennasai", "ごめんなさい"),
        Entry("how are you", "お元気ですか？", "ogenki desu ka?", "おげんきですか？"),
        Entry("how are you?", "お元気ですか？", "ogenki desu ka?", "おげんきですか？"),
        Entry("i am fine", "元気です", "genki desu", "げんきです"),
        Entry("what is your name", "お名前は何ですか？", "onamae wa nan desu ka?", "おなまえはなんですか？"),
        Entry("what is your name?", "お名前は何ですか？", "onamae wa nan desu ka?", "おなまえはなんですか？"),
        Entry("nice to meet you", "初めまして", "hajimemashite", "はじめまして"),
        Entry("water", "水", "mizu", "みず"),
        Entry("tea", "お茶", "ocha", "おちゃ"),
        Entry("coffee", "コーヒー", "koohii", "コーヒー"),
        Entry("bread", "パン", "pan", "パン"),
        Entry("book", "本", "hon", "ほん"),
        Entry("cat", "猫", "neko", "ねこ"),
        Entry("dog", "犬", "inu", "いぬ"),
        Entry("teacher", "先生", "sensei", "せんせい"),
        Entry("student", "学生", "gakusei", "がくせい"),
        Entry("i", "私", "watashi", "わたし"),
        Entry("i am a student", "私は学生です", "watashi wa gakusei desu", "わたしはがくせいです"),
        Entry("i am a teacher", "私は先生です", "watashi wa sensei desu", "わたしはせんせいです"),
        Entry("i drink water", "私は水を飲みます", "watashi wa mizu o nomimasu", "わたしはみずをのみます"),
        Entry("i eat bread", "私はパンを食べます", "watashi wa pan o tabemasu", "わたしはパンをたべます"),
        Entry("where is the station", "駅はどこですか？", "eki wa doko desu ka?", "えきはどこですか？"),
        Entry("where is the station?", "駅はどこですか？", "eki wa doko desu ka?", "えきはどこですか？"),
        Entry("station", "駅", "eki", "えき"),
        Entry("train", "電車", "densha", "でんしゃ"),
        Entry("delicious", "美味しい", "oishii", "おいしい"),
        Entry("beautiful", "美しい", "utsukushii", "うつくしい"),
        Entry("help", "助けて", "tasukete", "たすけて"),
        Entry("love", "愛", "ai", "あい"),
        Entry("i love you", "愛しています", "aishiteimasu", "あいしています"),
        Entry("see you tomorrow", "また明日", "mata ashita", "またあした"),
    )

    fun translate(
        text: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): TranslationResult {
        val clean = text.trim()
        if (clean.isBlank()) {
            return TranslationResult(
                sourceText = "",
                translatedText = "",
                romaji = "",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
            )
        }

        if (sourceLanguage == targetLanguage) {
            return TranslationResult(
                sourceText = clean,
                translatedText = clean,
                romaji = if (sourceLanguage == TranslationLanguage.Japanese) KanaConverter.toSpacedRomaji(clean) else "",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
            )
        }

        return if (sourceLanguage == TranslationLanguage.English) {
            translateEnglishToJapanese(clean, sourceLanguage, targetLanguage)
        } else {
            translateJapaneseToEnglish(clean, sourceLanguage, targetLanguage)
        }
    }

    private fun translateEnglishToJapanese(
        english: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): TranslationResult {
        val normalized = english.lowercase().trim().removeSuffix(".").removeSuffix("!")
        val match = DICTIONARY.firstOrNull {
            it.english.equals(normalized, ignoreCase = true) ||
                it.english.equals(english.trim(), ignoreCase = true)
        }

        if (match != null) {
            val kana = if (match.kana.isNotBlank()) match.kana else KanaConverter.toPureKana(match.japanese)
            return TranslationResult(
                sourceText = english,
                translatedText = match.japanese,
                romaji = match.romaji,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                kanaText = kana,
                isOffline = true,
            )
        }

        // Substring word-by-word replacement if multiple known tokens exist
        val words = normalized.split(Regex("\\s+"))
        val matchedTokens = words.mapNotNull { word ->
            DICTIONARY.firstOrNull { it.english.equals(word, ignoreCase = true) }
        }

        if (matchedTokens.size == words.size && matchedTokens.isNotEmpty()) {
            val jp = matchedTokens.joinToString(" ") { it.japanese }
            val ro = matchedTokens.joinToString(" ") { it.romaji }
            val kn = matchedTokens.joinToString(" ") { if (it.kana.isNotBlank()) it.kana else it.japanese }
            return TranslationResult(
                sourceText = english,
                translatedText = jp,
                romaji = ro,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                kanaText = kn,
                isOffline = true,
            )
        }

        // Realistic offline fallback
        val fallbackJp = "「$english」の翻訳"
        val fallbackRo = "[$english] no hon'yaku"
        return TranslationResult(english, fallbackJp, fallbackRo, sourceLanguage, targetLanguage)
    }

    private fun translateJapaneseToEnglish(
        japanese: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): TranslationResult {
        val cleanJp = japanese.trim()
        val match = DICTIONARY.firstOrNull {
            it.japanese == cleanJp || cleanJp.contains(it.japanese) ||
                (it.kana.isNotBlank() && (it.kana == cleanJp || cleanJp.contains(it.kana))) ||
                it.romaji.equals(cleanJp, ignoreCase = true)
        }

        if (match != null) {
            return TranslationResult(
                sourceText = japanese,
                translatedText = match.english.replaceFirstChar { it.uppercase() },
                romaji = match.romaji,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
            )
        }

        val romaji = KanaConverter.toSpacedRomaji(cleanJp).ifBlank { toRomaji(cleanJp) }
        val fallbackEn = "Translation for $cleanJp"
        return TranslationResult(japanese, fallbackEn, romaji, sourceLanguage, targetLanguage)
    }

    private fun toRomaji(kana: String): String {
        val romajiMap = mapOf(
            "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
            "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
            "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
            "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
            "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
            "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
            "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
            "や" to "ya", "ゆ" to "yu", "よ" to "yo",
            "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
            "わ" to "wa", "を" to "o", "ん" to "n",
            "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
            "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
            "だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
            "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
            "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
            "、" to ", ", "。" to ". ", "！" to "!", "？" to "?"
        )
        val sb = StringBuilder()
        for (ch in kana) {
            sb.append(romajiMap[ch.toString()] ?: ch.toString())
        }
        return sb.toString().trim()
    }
}
