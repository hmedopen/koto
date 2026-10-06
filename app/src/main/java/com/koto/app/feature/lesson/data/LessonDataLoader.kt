package com.koto.app.feature.lesson.data

import android.content.Context
import com.koto.app.feature.lesson.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream

/**
 * Data loader and parser for local lesson datasets.
 *
 * Parses JSON array of authored questions into Stage, Level, Question, and Choice data models,
 * preserving Kana + Romaji formatting and mapping all 6 mini-game quiz types.
 */
object LessonDataLoader {
    const val DEFAULT_FILE_PATH = "C:\\Users\\HMED OPEN\\Documents\\koto documents\\gemini-code-1790274690167"
    const val ASSET_FILE_NAME = "levels_1_5.json"

    private val STAGE_TITLES = mapOf(
        "STAGE_01" to "First Contact"
    )

    private val LEVEL_TITLES = mapOf(
        "LEVEL_01" to "First Japanese Greetings",
        "LEVEL_02" to "First Words",
        "LEVEL_03" to "Introducing Yourself",
        "LEVEL_04" to "The Desu Pattern",
        "LEVEL_05" to "First Integration"
    )

    val VOCABULARY_KANJI = mapOf(
        "みず" to "水",
        "ねこ" to "猫",
        "いぬ" to "犬",
        "ほん" to "本",
        "わたし" to "私",
        "がくせい" to "学生",
        "せんせい" to "先生",
        "ともだち" to "友達",
        "パン" to "パン",
        "ぱん" to "パン",
        "これ" to "これ",
        "それ" to "それ",
        "あれ" to "あれ",
        "おはよう" to "おはよう",
        "こんにちは" to "こんにちは",
        "こんばんは" to "こんばんは",
        "ありがとう" to "ありがとう",
        "たべます" to "食べます",
        "のみます" to "飲みます",
        "みます" to "見ます",
        "ききます" to "聞きます",
        "ねます" to "寝ます",
        "いきます" to "行きます",
        "おんがく" to "音楽",
        "おちゃ" to "お茶",
        "なに" to "何",
        "すし" to "寿司",
    )

    fun kanjiForKana(kana: String): String? {
        val clean = kana.trim()
        VOCABULARY_KANJI[clean]?.let { return it }
        if (" " in clean) {
            val parts = clean.split(" ")
            val converted = parts.map { VOCABULARY_KANJI[it] ?: it }
            if (converted != parts) {
                return converted.joinToString(" ")
            }
        }
        return null
    }

    private val VOCABULARY_ROMAJI = mapOf(
        "おはよう" to "ohayou",
        "こんにちは" to "konnichiwa",
        "こんばんは" to "konbanwa",
        "ありがとう" to "arigatou",
        "みず" to "mizu",
        "パン" to "pan",
        "ぱん" to "pan",
        "ねこ" to "neko",
        "いぬ" to "inu",
        "ほん" to "hon",
        "これ" to "kore",
        "それ" to "sore",
        "あれ" to "are",
        "わたし" to "watashi",
        "がくせい" to "gakusei",
        "せんせい" to "sensei",
        "ともだち" to "tomodachi",
        "ケン" to "Ken",
        "ユーキ" to "Yuuki",
        "ユキ" to "Yuki",
        "は" to "wa",
        "を" to "o",
        "で" to "de",
        "に" to "ni",
        "の" to "no",
        "が" to "ga",
        "です" to "desu",
        "ます" to "masu",
        "か" to "ka"
    )

    private val KANA_ROMAJI = mapOf(
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
        "わ" to "wa", "を" to "o", "ん" to "n"
    )

    /**
     * Loads raw questions from file path or Android assets.
     */
    fun loadRawQuestions(
        filePath: String = DEFAULT_FILE_PATH,
        context: Context? = null
    ): List<QuestionData> {
        val jsonString = readJsonString(filePath, context)
            ?: error("Could not load dataset from file '$filePath' or assets '$ASSET_FILE_NAME'")
        return parseJson(jsonString)
    }

    /**
     * Loads and structures questions into Stages and Levels.
     */
    fun loadStages(
        filePath: String = DEFAULT_FILE_PATH,
        context: Context? = null
    ): List<Stage> {
        val raw = loadRawQuestions(filePath, context)
        return buildStages(raw)
    }

    /**
     * Reads JSON content trying:
     * 1. Exact file path
     * 2. Path with .json appended
     * 3. Android asset if Context is provided
     * 4. ClassLoader resource
     */
    fun readJsonString(filePath: String, context: Context? = null): String? {
        val directFile = File(filePath)
        if (directFile.exists() && directFile.isFile) {
            return directFile.readText(Charsets.UTF_8)
        }

        val jsonFile = File("$filePath.json")
        if (jsonFile.exists() && jsonFile.isFile) {
            return jsonFile.readText(Charsets.UTF_8)
        }

        if (context != null) {
            runCatching {
                context.assets.open(ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrNull()?.let { return it }
        }

        val candidates = listOf(
            File("src/main/assets/$ASSET_FILE_NAME"),
            File("app/src/main/assets/$ASSET_FILE_NAME"),
            File("../app/src/main/assets/$ASSET_FILE_NAME"),
            File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/$ASSET_FILE_NAME"),
        )
        for (f in candidates) {
            if (f.exists() && f.isFile) {
                return runCatching { f.readText(Charsets.UTF_8) }.getOrNull()
            }
        }

        val stream: InputStream? = javaClass.classLoader?.getResourceAsStream(ASSET_FILE_NAME)
            ?: javaClass.classLoader?.getResourceAsStream("assets/$ASSET_FILE_NAME")
        if (stream != null) {
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }

        return null
    }

    /**
     * Parses a JSON string containing an array of question objects into [QuestionData].
     */
    fun parseJson(jsonString: String): List<QuestionData> {
        val array = JSONArray(jsonString.trim())
        val list = ArrayList<QuestionData>(array.length())

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val questionId = obj.getString("question_id")
            val stageId = obj.getString("stage_id")
            val levelId = obj.getString("level_id")
            val quizType = obj.getString("quiz_type")

            val skillTags = obj.optJSONArray("skill_tags")?.let { tagsArr ->
                List(tagsArr.length()) { tagsArr.getString(it) }
            }.orEmpty()

            val difficulty = obj.optInt("difficulty", 1)
            val newConcept = obj.optBoolean("new_concept", false)
            val japanese = obj.getString("japanese")
            val romaji = obj.getString("romaji")
            val english = obj.getString("english")

            val kanji = obj.optString("kanji", "").takeIf { it.isNotBlank() } ?: kanjiForKana(japanese)

            val choices = obj.optJSONArray("choices")?.let { choicesArr ->
                List(choicesArr.length()) { idx ->
                    val c = choicesArr.getJSONObject(idx)
                    val text = c.getString("text")
                    val choiceKanji = c.optString("kanji", "").takeIf { it.isNotBlank() } ?: kanjiForKana(text)
                    Choice(
                        id = c.getString("id"),
                        text = text,
                        correct = c.optBoolean("correct", false),
                        kanji = choiceKanji
                    )
                }
            }.orEmpty()

            val explanation = obj.optString("explanation", "")

            val audioObj = obj.optJSONObject("audio")
            val audio = AudioConfig(
                enabled = audioObj?.optBoolean("enabled", false) ?: false,
                file = audioObj?.optString("file", "") ?: ""
            )

            val reviewTags = obj.optJSONArray("review_tags")?.let { revArr ->
                List(revArr.length()) { revArr.getString(it) }
            }.orEmpty()

            list.add(
                QuestionData(
                    questionId = questionId,
                    stageId = stageId,
                    levelId = levelId,
                    quizType = quizType,
                    skillTags = skillTags,
                    difficulty = difficulty,
                    newConcept = newConcept,
                    japanese = japanese,
                    romaji = romaji,
                    english = english,
                    choices = choices,
                    explanation = explanation,
                    audio = audio,
                    reviewTags = reviewTags,
                    kanji = kanji
                )
            )
        }

        return list
    }

    /**
     * Groups raw question records into Stage and Level structures.
     */
    fun buildStages(rawQuestions: List<QuestionData>): List<Stage> {
        val groupedByStage = rawQuestions.groupBy { it.stageId }

        return groupedByStage.map { (stageId, stageQuestions) ->
            val stageNum = stageId.removePrefix("STAGE_").toIntOrNull() ?: 1
            val stageTitle = STAGE_TITLES[stageId] ?: "Stage $stageNum"

            val groupedByLevel = stageQuestions.groupBy { it.levelId }
            val levels = groupedByLevel.map { (levelId, levelRawQuestions) ->
                val levelNum = levelId.removePrefix("LEVEL_").toIntOrNull() ?: 1
                val levelTitle = LEVEL_TITLES[levelId] ?: "Level $levelNum"
                val domainQuestions = levelRawQuestions.map(::toDomainQuestion)

                Level(
                    id = levelId,
                    number = levelNum,
                    title = levelTitle,
                    stageId = stageId,
                    questions = domainQuestions,
                    rawQuestions = levelRawQuestions
                )
            }.sortedBy { it.number }

            Stage(
                id = stageId,
                number = stageNum,
                title = stageTitle,
                levels = levels
            )
        }.sortedBy { it.number }
    }

    /**
     * Maps a [QuestionData] record to its corresponding domain [Question] model
     * supporting all 6 mini-game quiz types.
     */
    fun toDomainQuestion(data: QuestionData): Question {
        return when (data.quizType.lowercase()) {
            "meaning_choice" -> mapMeaningChoice(data)
            "sentence_builder" -> mapSentenceBuilder(data)
            "fill_blank", "grammar_choice" -> mapCloze(data)
            "conversation_response" -> mapConversationResponse(data)
            "match_pairs" -> mapPairMatch(data)
            "listening" -> mapListening(data)
            else -> mapMeaningChoice(data)
        }
    }

    /**
     * Map meaning_choice quiz type:
     * Prompts with Japanese text, options with English translations.
     */
    private fun mapMeaningChoice(data: QuestionData): Question.MeaningChoice {
        val isChoiceEnglish = data.choices.all { choice ->
            choice.text.codePoints().noneMatch {
                val script = Character.UnicodeScript.of(it)
                script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA
            }
        }

        return if (isChoiceEnglish) {
            val promptKanji = data.kanji ?: kanjiForKana(data.japanese)
            val prompt = LessonText.Japanese(JapaneseText(data.japanese, data.romaji, kanji = promptKanji))
            val options = data.choices.map { Answer(it.id, LessonText.English(it.text)) }
            val correctId = data.choices.firstOrNull { it.correct }?.id ?: data.choices.first().id
            Question.MeaningChoice(data.questionId, prompt, options, correctId, data.reviewTags, data.explanation)
        } else {
            val prompt = LessonText.English(data.english)
            val options = data.choices.map { Answer(it.id, LessonText.Japanese(parseJapaneseChoice(it.text, it.kanji))) }
            val correctId = data.choices.firstOrNull { it.correct }?.id ?: data.choices.first().id
            Question.MeaningChoice(data.questionId, prompt, options, correctId, data.reviewTags, data.explanation)
        }
    }

    /**
     * Map sentence_builder quiz type:
     * English prompt, word tiles assembled into Japanese sentence.
     */
    private fun mapSentenceBuilder(data: QuestionData): Question.SentenceBuilder {
        val sentenceKanji = data.kanji ?: kanjiForKana(data.japanese)
        val sentence = JapaneseText(data.japanese, data.romaji, kanji = sentenceKanji)
        val kanaWords = data.japanese.split(" ").filter { it.isNotBlank() }
        val romajiWords = data.romaji.split(" ").filter { it.isNotBlank() }
        val kanjiWords = sentenceKanji?.split(" ")?.filter { it.isNotBlank() }

        val words: List<JapaneseText> = kanaWords.mapIndexed { idx, kana ->
            val rom = romajiWords.getOrNull(idx) ?: kanaToRomaji(kana)
            val kj = kanjiWords?.getOrNull(idx) ?: VOCABULARY_KANJI[kana]
            JapaneseText(kana, rom, kanji = kj)
        }

        val tiles = words.mapIndexed { idx, word ->
            Answer("${data.questionId}_T$idx", LessonText.Japanese(word))
        }
        val correctOrder = tiles.map { it.id }
        val arranged = arrangeTiles(data.questionId, tiles)

        return Question.SentenceBuilder(
            id = data.questionId,
            prompt = data.english,
            tiles = arranged,
            correctOrder = correctOrder,
            sentence = sentence,
            reviewTags = data.reviewTags,
            explanation = data.explanation
        )
    }

    /**
     * Map fill_blank (and grammar_choice):
     * Japanese sentence with '___', blank options with Romaji + Kana preserved.
     */
    private fun mapCloze(data: QuestionData): Question.Cloze {
        val sentenceKanji = data.kanji ?: kanjiForKana(data.japanese)
        val sentence = JapaneseText(data.japanese, data.romaji, kanji = sentenceKanji)
        val options = data.choices.map { choice ->
            val japaneseText = parseJapaneseChoice(choice.text, choice.kanji)
            Answer(choice.id, LessonText.Japanese(japaneseText))
        }
        val correctId = data.choices.firstOrNull { it.correct }?.id ?: data.choices.first().id

        return Question.Cloze(
            id = data.questionId,
            sentence = sentence,
            options = options,
            correctId = correctId,
            reviewTags = data.reviewTags,
            explanation = data.explanation
        )
    }

    /**
     * Map conversation_response:
     * Incoming dialogue bubble prompt, responses in Japanese.
     */
    private fun mapConversationResponse(data: QuestionData): Question.ConversationResponse {
        val cleanKana = data.japanese.replaceFirst(Regex("^A:\\s*"), "").trim()
        val cleanRomaji = data.romaji.replaceFirst(Regex("^A:\\s*"), "").trim()
        val cleanKanji = (data.kanji ?: kanjiForKana(cleanKana))?.replaceFirst(Regex("^A:\\s*"), "")?.trim()
        val incoming = JapaneseText(cleanKana, cleanRomaji, kanji = cleanKanji)

        val responses = data.choices.map { choice ->
            val japaneseText = parseJapaneseChoice(choice.text, choice.kanji)
            Answer(choice.id, LessonText.Japanese(japaneseText))
        }
        val correctId = data.choices.firstOrNull { it.correct }?.id ?: data.choices.first().id

        return Question.ConversationResponse(
            id = data.questionId,
            incoming = incoming,
            responses = responses,
            correctId = correctId,
            reviewTags = data.reviewTags,
            explanation = data.explanation
        )
    }

    /**
     * Map match_pairs:
     * Pairs of Japanese cards and English translations.
     */
    private fun mapPairMatch(data: QuestionData): Question.PairMatch {
        val pairs = data.choices.mapIndexed { idx, choice ->
            val split = choice.text.split(" - ")
            val kana = split[0].trim()
            val english = if (split.size > 1) split[1].trim() else kana
            val romaji = kanaToRomaji(kana)
            val kanji = choice.kanji?.split(" - ")?.getOrNull(0)?.trim() ?: VOCABULARY_KANJI[kana]

            MatchPair("${data.questionId}_P$idx", JapaneseText(kana, romaji, kanji = kanji), english)
        }

        return Question.PairMatch(
            id = data.questionId,
            pairs = pairs,
            reviewTags = data.reviewTags,
            explanation = data.explanation
        )
    }

    /**
     * Map listening:
     * Audio target to play via TTS/audio file, choice options.
     */
    private fun mapListening(data: QuestionData): Question.Listening {
        val targetKanji = data.kanji ?: kanjiForKana(data.japanese)
        val target = JapaneseText(data.japanese, data.romaji, kanji = targetKanji)
        val options = data.choices.map { choice ->
            val japaneseText = parseJapaneseChoice(choice.text, choice.kanji)
            Answer(choice.id, LessonText.Japanese(japaneseText))
        }
        val correctId = data.choices.firstOrNull { it.correct }?.id ?: data.choices.first().id

        return Question.Listening(
            id = data.questionId,
            target = target,
            options = options,
            correctId = correctId,
            audioFile = data.audio.file,
            reviewTags = data.reviewTags,
            explanation = data.explanation
        )
    }

    /**
     * Parses a Japanese choice text that may contain explicit romaji in parentheses,
     * e.g. "ありがとう (arigatou)", into a proper [JapaneseText].
     * If no parentheses are found, converts kana to romaji accurately.
     */
    fun parseJapaneseChoice(text: String, kanji: String? = null): JapaneseText {
        val parenthesized = Regex("""^(.+?)\s*\((.+?)\)$""").find(text.trim())
        if (parenthesized != null) {
            val kana = parenthesized.groupValues[1].trim()
            val romaji = parenthesized.groupValues[2].trim()
            val resolvedKanji = kanji ?: VOCABULARY_KANJI[kana]
            return JapaneseText(kana, romaji, kanji = resolvedKanji)
        }

        val kana = text.trim()
        val romaji = kanaToRomaji(kana)
        val resolvedKanji = kanji ?: kanjiForKana(kana) ?: VOCABULARY_KANJI[kana]
        return JapaneseText(kana, romaji, kanji = resolvedKanji)
    }

    /**
     * Converts a Japanese kana string into standard romaji using vocabulary lookup
     * and character-level Hepburn transliteration.
     */
    fun kanaToRomaji(kana: String): String {
        val clean = kana.trim()
        VOCABULARY_ROMAJI[clean]?.let { return it }

        // Multi-word sentence separated by spaces
        if (" " in clean) {
            return clean.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word -> kanaToRomaji(word) }
        }

        // Punctuation suffix handling (e.g. "こんにちは！")
        val punctMatch = Regex("""^(.+?)([！？。、!?,.]+)$""").find(clean)
        if (punctMatch != null) {
            val base = punctMatch.groupValues[1]
            val punct = punctMatch.groupValues[2]
                .replace("！", "!")
                .replace("？", "?")
                .replace("。", ".")
                .replace("、", ", ")
            return kanaToRomaji(base) + punct
        }

        // Character-by-character Hepburn transliteration
        val sb = StringBuilder()
        var i = 0
        while (i < clean.length) {
            val charStr = clean[i].toString()
            val nextChar = if (i + 1 < clean.length) clean[i + 1].toString() else null

            // Small tsu handling (sokuon)
            if (charStr == "っ" || charStr == "ッ") {
                if (nextChar != null) {
                    val nextRomaji = KANA_ROMAJI[nextChar] ?: ""
                    if (nextRomaji.isNotEmpty()) {
                        sb.append(nextRomaji[0])
                    }
                }
                i++
                continue
            }

            // Two-character combination (e.g. じょ, きゃ)
            val combo = if (nextChar != null) charStr + nextChar else null
            if (combo != null && VOCABULARY_ROMAJI.containsKey(combo)) {
                sb.append(VOCABULARY_ROMAJI[combo])
                i += 2
                continue
            }

            val r = KANA_ROMAJI[charStr] ?: VOCABULARY_ROMAJI[charStr] ?: charStr
            sb.append(r)
            i++
        }

        return sb.toString()
    }

    private fun arrangeTiles(id: String, tiles: List<Answer>): List<Answer> {
        if (tiles.size <= 1) return tiles
        val shift = 1 + Math.floorMod(id.hashCode(), tiles.size - 1)
        return tiles.drop(shift) + tiles.take(shift)
    }
}
