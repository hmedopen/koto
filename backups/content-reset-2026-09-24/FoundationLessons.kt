package com.koto.app.feature.lesson.data

import com.koto.app.feature.lesson.model.*

/**
 * Stage 1–2 only. P = Production Database, F = FULL Question Database.
 * References preserve source level/question numbers; review never invents a question.
 * Only the requested level's factories execute. No question bank or level cache is retained.
 */
object FoundationLessons : LessonRepository {
    internal val sources = listOf(
        // Seven-question introductory level: five written non-listening items,
        // followed by two explicit reviews under the user's reuse approval.
        listOf("P01Q1", "P01Q2", "P01Q4", "P01Q5", "F01Q2", "P01Q1", "P01Q2"),
        listOf("P02Q1", "P02Q2", "P02Q3", "P02Q4", "P02Q5", "F02Q2", "F02Q3"),
        listOf("P03Q1", "P03Q2", "P03Q3", "F03Q1", "P02Q2", "P02Q3", "P01Q1", "P02Q5", "F02Q3", "P02Q4"),
        listOf("P04Q1", "P04Q2", "F04Q1", "P02Q2", "F02Q3", "P03Q1", "P03Q2", "F03Q1", "P01Q2", "P03Q3"),
        listOf("P01Q1", "P02Q1", "P03Q1", "P04Q1", "P02Q2", "P01Q2", "P03Q2", "P02Q5", "F02Q3", "P04Q2"),
        listOf("P06Q1", "P06Q2", "P06Q3", "P04Q1", "P02Q2", "P02Q1", "P03Q2", "P01Q2", "F02Q3", "P04Q2"),
        listOf("P07Q1", "P07Q2", "P06Q1", "P06Q2", "P06Q3", "P02Q5", "P02Q2", "P04Q1", "P03Q2", "P01Q1"),
        listOf("P08Q1", "F08Q2", "P06Q1", "P06Q3", "P06Q2", "P07Q1", "P04Q1", "F02Q3", "P03Q2", "P02Q5"),
        listOf("F09Q1", "P08Q1", "F08Q2", "P06Q1", "P06Q2", "P07Q1", "P04Q1", "P02Q2", "P03Q2", "P01Q2"),
        listOf("F10Q1", "F09Q1", "P08Q1", "P06Q2", "P03Q2", "P02Q1", "P04Q1", "F02Q3", "P02Q5", "P03Q1"),
        // Listening is not an existing quiz type. Known material remains review with
        // the existing optional speaker buttons; this is not an audio-only assessment.
        listOf("P01Q1", "F01Q2", "P02Q1", "F02Q2", "P03Q3", "F03Q1", "P06Q1", "F09Q1", "F10Q1", "P03Q2"),
        listOf("P06Q1", "P07Q1", "P08Q1", "F09Q1", "F10Q1", "P04Q1", "F04Q1", "P02Q2", "F02Q3", "P03Q2", "P02Q5", "P01Q2"),
    )

    override val levels = listOf(
        "Japanese First Steps", "First Words", "Introducing Yourself", "The Desu Pattern",
        "First Integration", "Actions Begin", "Food And Drinks", "Similar Verb Training",
        "Daily Actions", "Simple Questions", "Listening Practice", "Stage Integration",
    ).mapIndexed { index, title -> LevelSummary(index + 1, title, if (index < 5) 1 else 2, sources[index].size) }

    override fun lesson(id: Int): LessonDefinition? {
        val summary = levels.firstOrNull { it.id == id } ?: return null
        val questions = sources[id - 1].mapIndexed { index, source ->
            authored(source, "L${id.toString().padStart(2, '0')}_Q${(index + 1).toString().padStart(2, '0')}")
        }
        return LessonDefinition(id, summary.title, summary.section, questions).also { it.validate() }
    }

    private fun authored(source: String, id: String): Question = when (source) {
        "P01Q1" -> forward(id, "こんにちは~konnichiwa", 1, "good morning", "hello", "thank you", "good night")
        "P01Q2" -> match(id, "おはよう~ohayou" to "good morning", "こんにちは~konnichiwa" to "hello", "ありがとう~arigatou" to "thank you")
        "P01Q4" -> reverse(id, "Which means thank you?", "ありがとう~arigatou", "こんにちは~konnichiwa", "おはよう~ohayou", "ねこ~neko")
        "P01Q5" -> reverse(id, "Choose the morning greeting.", "おはよう~ohayou", "こんにちは~konnichiwa", "ありがとう~arigatou", "みず~mizu")
        "F01Q2" -> forward(id, "ありがとう~arigatou", 0, "thank you", "hello", "morning", "book")
        "P02Q1" -> forward(id, "みず~mizu", 0, "water", "bread", "book", "cat")
        "P02Q2" -> build(id, "This is water.", "これ~kore", "は~wa", "みず~mizu", "です~desu")
        "P02Q3" -> reverse(id, "Which word means cat?", "ねこ~neko", "いぬ~inu", "ほん~hon", "ぱん~pan")
        "P02Q4" -> reverse(id, "Choose the correct sentence.", "これ は ねこ です~kore wa neko desu", "ねこ は これ たべます~neko wa kore tabemasu", "これ ねこ を です~kore neko o desu", "わたし ねこ のみます~watashi neko nomimasu")
        "P02Q5" -> match(id, "いぬ~inu" to "dog", "ほん~hon" to "book", "ぱん~pan" to "bread")
        "F02Q2" -> forward(id, "ねこ~neko", 0, "cat", "dog", "book", "water")
        "F02Q3" -> build(id, "This is a book.", "これ~kore", "は~wa", "ほん~hon", "です~desu")
        "P03Q1" -> reverse(id, "I am a student.", "わたし は がくせい です~watashi wa gakusei desu", "がくせい は わたし たべます~gakusei wa watashi tabemasu", "わたし を がくせい です~watashi o gakusei desu", "がくせい みず です~gakusei mizu desu")
        "P03Q2" -> conversation(id, "わたし は ケン です。~watashi wa Ken desu.", "わたし は ユキ です~watashi wa Yuki desu", "みず です~mizu desu", "ねこ です~neko desu", "ぱん たべます~pan tabemasu")
        "P03Q3" -> forward(id, "ともだち~tomodachi", 0, "friend", "teacher", "student", "book")
        "F03Q1" -> forward(id, "わたし~watashi", 0, "I", "friend", "teacher", "student")
        "P04Q1" -> cloze(id, "わたし は がくせい ___~watashi wa gakusei ___", "です~desu", "を~o", "は~wa", "に~ni")
        "P04Q2" -> reverse(id, "Choose: This is a book.", "これ は ほん です~kore wa hon desu", "これ ほん を たべます~kore hon o tabemasu", "ほん は ねこ です~hon wa neko desu", "わたし です みず~watashi desu mizu")
        "F04Q1" -> cloze(id, "わたし は せんせい ___~watashi wa sensei ___", "です~desu", "を~o", "に~ni", "の~no")
        "P06Q1" -> forward(id, "たべます~tabemasu", 0, "eat", "drink", "see", "sleep")
        "P06Q2" -> reverse(id, "I drink water.", "わたし は みず を のみます~watashi wa mizu o nomimasu", "わたし は みず を たべます~watashi wa mizu o tabemasu", "わたし は みず は みます~watashi wa mizu wa mimasu", "みず わたし です~mizu watashi desu")
        "P06Q3" -> reverse(id, "Which means watch?", "みます~mimasu", "のみます~nomimasu", "たべます~tabemasu", "ありがとう~arigatou")
        "P07Q1" -> reverse(id, "I eat sushi.", "わたし は すし を たべます~watashi wa sushi o tabemasu", "わたし は すし を のみます~watashi wa sushi o nomimasu", "わたし は すし です~watashi wa sushi desu", "すし は わたし~sushi wa watashi")
        "P07Q2" -> reverse(id, "Which is a drink?", "おちゃ~ocha", "ねこ~neko", "ほん~hon", "いぬ~inu")
        "P08Q1" -> reverse(id, "I listen to music.", "おんがく を ききます~ongaku o kikimasu", "おんがく を のみます~ongaku o nomimasu", "おんがく を たべます~ongaku o tabemasu", "おんがく を みます~ongaku o mimasu")
        "F08Q2" -> reverse(id, "I watch.", "みます~mimasu", "たべます~tabemasu", "のみます~nomimasu", "いきます~ikimasu")
        "F09Q1" -> forward(id, "ねます~nemasu", 0, "sleep", "eat", "drink", "study")
        "F10Q1" -> forward(id, "なに を します か？~nani o shimasu ka?", 0, "What do you do?", "Where is it?", "Who is it?", "How much?")
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
