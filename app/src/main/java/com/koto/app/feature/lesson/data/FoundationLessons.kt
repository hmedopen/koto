package com.koto.app.feature.lesson.data

import com.koto.app.feature.lesson.model.*

/**
 * Foundation curriculum repository.
 *
 * Stage 1 (Levels 1–5) is parsed directly from the local JSON dataset via [LessonDataLoader],
 * mapping all 6 mini-game quiz types (meaning_choice, sentence_builder, fill_blank,
 * conversation_response, match_pairs, listening) and integrating with [SrsTracker].
 *
 * Stage 2 (Levels 6–12) authored items are preserved for testing continuity.
 */
object FoundationLessons : LessonRepository {
    private val stage1Levels: List<Level> by lazy {
        runCatching {
            LessonDataLoader.loadStages().firstOrNull { it.id == "STAGE_01" }?.levels
        }.getOrNull().orEmpty()
    }

    internal val stage1Questions: List<Question> by lazy {
        stage1Levels.flatMap { it.questions }
    }

    internal val sources: List<List<String>> = listOf(
        listOf("L01_Q01", "L01_Q02", "L01_Q03", "L01_Q04"),
        listOf("L02_Q01", "L02_Q02", "L02_Q03", "L02_Q04"),
        listOf("L03_Q01", "L03_Q02", "L03_Q03", "L03_Q04"),
        listOf("L04_Q01", "L04_Q02", "L04_Q03", "L04_Q04"),
        listOf("L05_Q01", "L05_Q02", "L05_Q03", "L05_Q04", "L05_Q05"),
        listOf("P06Q1", "P06Q2", "P06Q3", "P04Q1", "P02Q2", "P02Q1", "P03Q2", "P01Q2", "F02Q3", "P04Q2"),
        listOf("P07Q1", "P07Q2", "P06Q1", "P06Q2", "P06Q3", "P02Q5", "P02Q2", "P04Q1", "P03Q2", "P01Q1"),
        listOf("P08Q1", "F08Q2", "P06Q1", "P06Q3", "P06Q2", "P07Q1", "P04Q1", "F02Q3", "P03Q2", "P02Q5"),
        listOf("F09Q1", "P08Q1", "F08Q2", "P06Q1", "P06Q2", "P07Q1", "P04Q1", "P02Q2", "P03Q2", "P01Q2"),
        listOf("F10Q1", "F09Q1", "P08Q1", "P06Q2", "P03Q2", "P02Q1", "P04Q1", "F02Q3", "P02Q5", "P03Q1"),
        listOf("P01Q1", "F01Q2", "P02Q1", "F02Q2", "P03Q3", "F03Q1", "P06Q1", "F09Q1", "F10Q1", "P03Q2"),
        listOf("P06Q1", "P07Q1", "P08Q1", "F09Q1", "F10Q1", "P04Q1", "F04Q1", "P02Q2", "F02Q3", "P03Q2", "P02Q5", "P01Q2")
    )

    override val levels = listOf(
        "Level 01", "Level 02", "Level 03", "Level 04", "Level 05",
        "Actions Begin", "Food And Drinks", "Similar Verb Training",
        "Daily Actions", "Simple Questions", "Listening Practice", "Stage Integration"
    ).mapIndexed { index, title ->
        val levelNum = index + 1
        val stageNum = if (index < 5) 1 else 2
        val count = if (index < 5) {
            stage1Levels.getOrNull(index)?.questions?.size ?: sources[index].size
        } else {
            sources[index].size
        }
        LevelSummary(levelNum, title, stageNum, count)
    }

    override fun lesson(id: Int): LessonDefinition? {
        val summary = levels.firstOrNull { it.id == id } ?: return null

        if (id in 1..5) {
            val level = stage1Levels.firstOrNull { it.number == id }
            val baseQuestions = level?.questions ?: return null

            val priorQuestions = stage1Levels.filter { it.number < id }.flatMap { it.questions }
            val rawDefinition = LessonDefinition(id, summary.title, summary.section, baseQuestions)

            val srs = SrsTracker.defaultInstance
            val definition = if (srs != null && priorQuestions.isNotEmpty()) {
                srs.injectDynamicReviews(rawDefinition, priorQuestions)
            } else {
                rawDefinition
            }

            return definition.also { it.validate() }
        }

        if (id in 6..12) {
            val questions = sources[id - 1].mapIndexed { index, source ->
                authored(source, "L${id.toString().padStart(2, '0')}_Q${(index + 1).toString().padStart(2, '0')}")
            }
            return LessonDefinition(id, summary.title, summary.section, questions).also { it.validate() }
        }

        return null
    }

    private fun authored(source: String, id: String): Question = when (source) {
        "P06Q1" -> forward(id, "たべます~tabemasu", 0, "eat", "drink", "see", "sleep")
        "P06Q2" -> reverse(id, "I drink water.", "わたし は みず を のみます~watashi wa mizu o nomimasu", "わたし は みず を たべます~watashi wa mizu o tabemasu", "わたし は みず は みます~watashi wa mizu wa mimasu", "みず わたし です~mizu watashi desu")
        "P06Q3" -> reverse(id, "Which means watch?", "みます~mimasu", "のみます~nomimasu", "たべます~tabemasu", "ありがとう~arigatou")
        "P07Q1" -> reverse(id, "I eat sushi.", "わたし は すし を たべます~watashi wa sushi o tabemasu", "わたし は すし を のみます~watashi wa sushi o nomimasu", "わたし は すし です~watashi wa sushi desu", "すし は わたし~sushi wa watashi")
        "P07Q2" -> reverse(id, "Which is a drink?", "おちゃ~ocha", "ねこ~neko", "ほん~hon", "いぬ~inu")
        "P08Q1" -> reverse(id, "I listen to music.", "おんがく を ききます~ongaku o kikimasu", "おんがく を のみます~ongaku o nomimasu", "おんがく を たべます~ongaku o tabemasu", "おんがく を みます~ongaku o mimasu")
        "F08Q2" -> reverse(id, "I watch.", "みます~mimasu", "たべます~tabemasu", "のみます~nomimasu", "いきます~ikimasu")
        "F09Q1" -> forward(id, "ねます~nemasu", 0, "sleep", "eat", "drink", "study")
        "F10Q1" -> forward(id, "なに を します か？~nani o shimasu ka?", 0, "What do you do?", "Where is it?", "Who is it?", "How much?")
        "P01Q1" -> forward(id, "こんにちは~konnichiwa", 1, "good morning", "hello", "thank you", "good night")
        "P01Q2" -> match(id, "おはよう~ohayou" to "good morning", "こんにちは~konnichiwa" to "hello", "ありがとう~arigatou" to "thank you")
        "P02Q1" -> forward(id, "みず~mizu", 0, "water", "bread", "book", "cat")
        "P02Q2" -> build(id, "This is water.", "これ~kore", "は~wa", "みず~mizu", "です~desu")
        "P02Q5" -> match(id, "いぬ~inu" to "dog", "ほん~hon" to "book", "ぱん~pan" to "bread")
        "F01Q2" -> forward(id, "ありがとう~arigatou", 0, "thank you", "hello", "morning", "book")
        "F02Q2" -> forward(id, "ねこ~neko", 0, "cat", "dog", "book", "water")
        "F02Q3" -> build(id, "This is a book.", "これ~kore", "は~wa", "ほん~hon", "です~desu")
        "P03Q2" -> conversation(id, "わたし は ケン です。~watashi wa Ken desu.", "わたし は ユキ です~watashi wa Yuki desu", "みず です~mizu desu", "ねこ です~neko desu", "ぱん たべます~pan tabemasu")
        "P03Q3" -> forward(id, "ともだち~tomodachi", 0, "friend", "teacher", "student", "book")
        "F03Q1" -> forward(id, "わたし~watashi", 0, "I", "friend", "teacher", "student")
        "P04Q1" -> cloze(id, "わたし は がくせい ___~watashi wa gakusei ___", "です~desu", "を~o", "は~wa", "に~ni")
        "P04Q2" -> reverse(id, "Choose: This is a book.", "これ は ほん です~kore wa hon desu", "これ ほん を たべます~kore hon o tabemasu", "ほん は ねこ です~hon wa neko desu", "わたし です みず~watashi desu mizu")
        "F04Q1" -> cloze(id, "わたし は せんせい ___~watashi wa sensei ___", "です~desu", "を~o", "に~ni", "の~no")
        else -> error("Unknown Stage 1–2 source: $source")
    }

    private fun jp(value: String) = value.split('~').let { JapaneseText(it[0], it[1]) }
    private fun ja(value: String) = LessonText.Japanese(jp(value))
    private fun options(id: String, values: List<LessonText>) = values.mapIndexed { index, text -> Answer("${id}_A$index", text) }
    private fun <T> arrange(id: String, values: List<T>): List<T> {
        val shift = 1 + Math.floorMod(id.hashCode(), values.size - 1)
        return values.drop(shift) + values.take(shift)
    }
    private fun forward(id: String, prompt: String, correct: Int, vararg values: String): Question =
        Question.MeaningChoice(id, ja(prompt), arrange(id, options(id, values.map { LessonText.English(it) })), "${id}_A$correct")
    private fun reverse(id: String, prompt: String, vararg values: String): Question =
        Question.MeaningChoice(id, LessonText.English(prompt), arrange(id, options(id, values.map(::ja))), "${id}_A0")
    private fun conversation(id: String, prompt: String, vararg values: String): Question =
        Question.ConversationResponse(id, jp(prompt), arrange(id, options(id, values.map(::ja))), "${id}_A0")
    private fun cloze(id: String, sentence: String, vararg values: String): Question =
        Question.Cloze(id, jp(sentence), arrange(id, options(id, values.map(::ja))), "${id}_A0")
    private fun build(id: String, prompt: String, vararg words: String): Question {
        val tiles = options(id, words.map(::ja))
        return Question.SentenceBuilder(id, prompt, arrange(id, tiles), tiles.map { it.id },
            JapaneseText(words.joinToString(" ") { jp(it).kana }, words.joinToString(" ") { jp(it).romaji }))
    }
    private fun match(id: String, vararg pairs: Pair<String, String>): Question = Question.PairMatch(id,
        pairs.mapIndexed { index, (japanese, english) -> MatchPair("${id}_P$index", jp(japanese), english) })
}
