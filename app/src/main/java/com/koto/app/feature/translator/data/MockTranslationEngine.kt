package com.koto.app.feature.translator.data

import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.feature.translator.model.TranslationResult

object MockTranslationEngine {

    private data class Entry(
        val english: String,
        val japanese: String,
        val romaji: String,
    )

    private val DICTIONARY = listOf(
        Entry("hello", "こんにちは", "konnichiwa"),
        Entry("hi", "こんにちは", "konnichiwa"),
        Entry("hello friend", "こんにちは、 友よ", "konnichiwa, tomoyo"),
        Entry("hello, friend", "こんにちは、 友よ", "konnichiwa, tomoyo"),
        Entry("friend", "ともだち", "tomodachi"),
        Entry("my friend", "わたしのともだち", "watashi no tomodachi"),
        Entry("good morning", "おはようございます", "ohayou gozaimasu"),
        Entry("good evening", "こんばんは", "konbanwa"),
        Entry("good night", "おやすみなさい", "oyasuminasai"),
        Entry("thank you", "ありがとうございます", "arigatou gozaimasu"),
        Entry("thanks", "ありがとう", "arigatou"),
        Entry("thank you very much", "どうもありがとうございます", "doumo arigatou gozaimasu"),
        Entry("you are welcome", "どういたしまして", "douitashimashite"),
        Entry("welcome", "ようこそ", "youkoso"),
        Entry("goodbye", "さようなら", "sayounara"),
        Entry("see you later", "またね", "mata ne"),
        Entry("yes", "はい", "hai"),
        Entry("no", "いいえ", "iie"),
        Entry("please", "おねがいします", "onegaishimasu"),
        Entry("excuse me", "すみません", "sumimasen"),
        Entry("sorry", "ごめんなさい", "gomennasai"),
        Entry("how are you", "おげんきですか？", "ogenki desu ka?"),
        Entry("how are you?", "おげんきですか？", "ogenki desu ka?"),
        Entry("i am fine", "げんきです", "genki desu"),
        Entry("what is your name", "おなまえはなんですか？", "onamae wa nan desu ka?"),
        Entry("what is your name?", "おなまえはなんですか？", "onamae wa nan desu ka?"),
        Entry("nice to meet you", "はじめまして", "hajimemashite"),
        Entry("water", "みず", "mizu"),
        Entry("tea", "おちゃ", "ocha"),
        Entry("coffee", "コーヒー", "koohii"),
        Entry("bread", "パン", "pan"),
        Entry("book", "ほん", "hon"),
        Entry("cat", "ねこ", "neko"),
        Entry("dog", "いぬ", "inu"),
        Entry("teacher", "せんせい", "sensei"),
        Entry("student", "がくせい", "gakusei"),
        Entry("i", "わたし", "watashi"),
        Entry("i am a student", "わたしはがくせいです", "watashi wa gakusei desu"),
        Entry("i am a teacher", "わたしはせんせいです", "watashi wa sensei desu"),
        Entry("i drink water", "わたしはみずをのみます", "watashi wa mizu o nomimasu"),
        Entry("i eat bread", "わたしはパンをたべます", "watashi wa pan o tabemasu"),
        Entry("where is the station", "えきはどこですか？", "eki wa doko desu ka?"),
        Entry("where is the station?", "えきはどこですか？", "eki wa doko desu ka?"),
        Entry("station", "えき", "eki"),
        Entry("train", "でんしゃ", "densha"),
        Entry("delicious", "おいしい", "oishii"),
        Entry("beautiful", "うつくしい", "utsukushii"),
        Entry("help", "たすけて", "tasukete"),
        Entry("love", "あい", "ai"),
        Entry("i love you", "あいしています", "aishiteimasu"),
        Entry("see you tomorrow", "またあした", "mata ashita"),
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
                romaji = if (sourceLanguage == TranslationLanguage.Japanese) toRomaji(clean) else "",
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
            return TranslationResult(
                sourceText = english,
                translatedText = match.japanese,
                romaji = match.romaji,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
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
            return TranslationResult(english, jp, ro, sourceLanguage, targetLanguage)
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

        val romaji = toRomaji(cleanJp)
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
