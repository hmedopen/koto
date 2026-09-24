package com.koto.app.feature.lesson.data

import com.koto.app.feature.lesson.model.*

/** Historical synthetic fixtures for mechanics regressions only; never bundled in the app. */
object PrototypeLessons {
    fun lesson(id: Int) = lessons.firstOrNull { it.id == id }
    private fun jp(value: String): JapaneseText = value.split("~").let { JapaneseText(it[0], it[1]) }
    private fun ja(value: String): LessonText = LessonText.Japanese(jp(value))
    private fun en(value: String): LessonText = LessonText.English(value)
    private fun options(id: String, values: List<LessonText>) = values.mapIndexed { n, text -> Answer("$id-a$n", text) }
    private fun <T> arrange(id: String, values: List<T>): List<T> {
        val shift = 1 + Math.floorMod(id.hashCode(), values.size - 1)
        return values.drop(shift) + values.take(shift)
    }
    private fun choice(id: String, prompt: LessonText, vararg values: LessonText): Question =
        Question.MeaningChoice(id, prompt, arrange(id, options(id, values.toList())), "$id-a0")
    private fun forward(id: String, prompt: String, vararg english: String) = choice(id, ja(prompt), *english.map(::en).toTypedArray())
    private fun reverse(id: String, prompt: String, vararg japanese: String) = choice(id, en(prompt), *japanese.map(::ja).toTypedArray())
    private fun conversation(id: String, prompt: String, vararg responses: String): Question =
        Question.ConversationResponse(id, jp(prompt), arrange(id, options(id, responses.map(::ja))), "$id-a0")
    private fun cloze(id: String, sentence: String, vararg words: String): Question =
        Question.Cloze(id, jp(sentence), arrange(id, options(id, words.map(::ja))), "$id-a0")
    private fun build(id: String, prompt: String, words: List<String>, vararg distractors: String): Question {
        val tiles = options(id, (words + distractors).map(::ja))
        val correct = tiles.take(words.size)
        return Question.SentenceBuilder(id, prompt, arrange(id, tiles), correct.map { it.id },
            JapaneseText(correct.joinToString(" ") { (it.text as LessonText.Japanese).value.kana },
                correct.joinToString(" ") { (it.text as LessonText.Japanese).value.romaji }))
    }
    private fun match(id: String, vararg pairs: Pair<String, String>): Question =
        Question.PairMatch(id, pairs.mapIndexed { n, (japanese, english) -> MatchPair("$id-p$n", jp(japanese), english) })
    private fun level(id: Int, title: String, vararg questions: Question) = LessonDefinition(id, title,
        when (id) { in 1..4 -> "FIRST CONTACT"; in 5..8 -> "BUILD & USE"; else -> "CONVERSATION TEST" }, questions.toList())

    val lessons = listOf(
        level(1, "Greetings",
            forward("1-1", "こんにちは~konnichiwa", "Hello", "Goodbye", "Thank you", "Yes"),
            reverse("1-2", "Thank you", "ありがとう~arigatou", "こんにちは~konnichiwa", "さようなら~sayounara", "はい~hai"),
            match("1-3", "こんにちは~konnichiwa" to "Hello", "ありがとう~arigatou" to "Thank you", "はい~hai" to "Yes"),
            conversation("1-4", "こんにちは~konnichiwa", "こんにちは~konnichiwa", "ありがとう~arigatou", "いいえ~iie"),
            forward("1-5", "いいえ~iie", "No", "Yes", "Hello", "Thank you"),
            conversation("1-6", "ありがとう~arigatou", "どういたしまして~douitashimashite", "こんにちは~konnichiwa", "はい~hai")),
        level(2, "Everyday nouns",
            forward("2-1", "ねこ~neko", "Cat", "Dog", "Book", "Water"),
            reverse("2-2", "Book", "ほん~hon", "いぬ~inu", "みず~mizu", "ねこ~neko"),
            match("2-3", "ねこ~neko" to "Cat", "いぬ~inu" to "Dog", "ほん~hon" to "Book"),
            forward("2-4", "みず~mizu", "Water", "Apple", "Bread", "Dog"),
            match("2-5", "みず~mizu" to "Water", "りんご~ringo" to "Apple", "パン~pan" to "Bread"),
            build("2-6", "This is a book.", listOf("これ~kore", "は~wa", "ほん~hon", "です~desu"))),
        level(3, "Food & drink",
            forward("3-1", "パン~pan", "Bread", "Tea", "Water", "Apple"),
            reverse("3-2", "Water", "みず~mizu", "おちゃ~ocha", "パン~pan", "りんご~ringo"),
            cloze("3-3", "わたしは ___ を のみます~watashi wa ___ o nomimasu", "みず~mizu", "パン~pan", "ほん~hon"),
            build("3-4", "I eat bread.", listOf("わたし~watashi", "は~wa", "パン~pan", "を~o", "たべます~tabemasu"), "みず~mizu", "ねこ~neko"),
            match("3-5", "たべます~tabemasu" to "Eat", "のみます~nomimasu" to "Drink", "おちゃ~ocha" to "Tea"),
            conversation("3-6", "なにを のみますか？~nani o nomimasu ka?", "みずを のみます~mizu o nomimasu", "パンを たべます~pan o tabemasu", "ほんを よみます~hon o yomimasu")),
        level(4, "Simple actions",
            forward("4-1", "よみます~yomimasu", "Read", "Listen", "Sleep", "Go"),
            reverse("4-2", "Listen", "ききます~kikimasu", "みます~mimasu", "ねます~nemasu", "いきます~ikimasu"),
            cloze("4-3", "ほんを ___~hon o ___", "よみます~yomimasu", "のみます~nomimasu", "ねます~nemasu"),
            build("4-4", "I listen to music.", listOf("わたし~watashi", "は~wa", "おんがく~ongaku", "を~o", "ききます~kikimasu"), "みず~mizu", "を~o", "たべます~tabemasu"),
            match("4-5", "いきます~ikimasu" to "Go", "みます~mimasu" to "Watch/see", "ねます~nemasu" to "Sleep"),
            conversation("4-6", "なにを みますか？~nani o mimasu ka?", "テレビを みます~terebi o mimasu", "ほんを よみます~hon o yomimasu", "みずを のみます~mizu o nomimasu")),
        level(5, "Identity",
            reverse("5-1", "Student", "がくせい~gakusei", "せんせい~sensei", "ともだち~tomodachi", "わたし~watashi"),
            forward("5-2", "せんせい~sensei", "Teacher", "Student", "Friend", "I/me"),
            build("5-3", "I am a student.", listOf("わたし~watashi", "は~wa", "がくせい~gakusei", "です~desu"), "せんせい~sensei"),
            cloze("5-4", "わたしは ___ です~watashi wa ___ desu", "せんせい~sensei", "みず~mizu", "パン~pan"),
            match("5-5", "がくせい~gakusei" to "Student", "せんせい~sensei" to "Teacher", "ともだち~tomodachi" to "Friend"),
            conversation("5-6", "がくせいですか？~gakusei desu ka?", "はい、がくせいです~hai, gakusei desu", "パンです~pan desu", "みずを のみます~mizu o nomimasu")),
        level(6, "Questions",
            forward("6-1", "なに~nani", "What", "Who", "Where", "Yes"),
            reverse("6-2", "Who", "だれ~dare", "どこ~doko", "なに~nani", "いいえ~iie"),
            cloze("6-3", "これは ___ ですか？~kore wa ___ desu ka?", "なん~nan", "だれ~dare", "どこ~doko"),
            conversation("6-4", "これは なんですか？~kore wa nan desu ka?", "ほんです~hon desu", "せんせいです~sensei desu", "がっこうに いきます~gakkou ni ikimasu"),
            conversation("6-5", "あのひとは だれですか？~ano hito wa dare desu ka?", "せんせいです~sensei desu", "みずです~mizu desu", "パンを たべます~pan o tabemasu"),
            build("6-6", "Where is the shop?", listOf("みせ~mise", "は~wa", "どこ~doko", "です~desu", "か~ka"))),
        level(7, "Likes",
            forward("7-1", "すき~suki", "Like", "Read", "Go", "Drink"),
            build("7-2", "I like cats.", listOf("わたし~watashi", "は~wa", "ねこ~neko", "が~ga", "すき~suki", "です~desu"), "いぬ~inu", "が~ga"),
            cloze("7-3", "わたしは おんがくが ___~watashi wa ongaku ga ___", "すきです~suki desu", "よみます~yomimasu", "のみます~nomimasu"),
            conversation("7-4", "ねこが すきですか？~neko ga suki desu ka?", "はい、すきです~hai, suki desu", "みずです~mizu desu", "ほんを よみます~hon o yomimasu"),
            match("7-5", "ねこ~neko" to "Cat", "おんがく~ongaku" to "Music", "えいが~eiga" to "Movie"),
            conversation("7-6", "なにが すきですか？~nani ga suki desu ka?", "おんがくが すきです~ongaku ga suki desu", "がっこうに いきます~gakkou ni ikimasu", "おちゃを のみます~ocha o nomimasu")),
        level(8, "Places & movement",
            forward("8-1", "えき~eki", "Station", "School", "Shop", "Home"),
            reverse("8-2", "School", "がっこう~gakkou", "みせ~mise", "えき~eki", "いえ~ie"),
            cloze("8-3", "がっこうに ___~gakkou ni ___", "いきます~ikimasu", "よみます~yomimasu", "のみます~nomimasu"),
            build("8-4", "I go to the shop.", listOf("わたし~watashi", "は~wa", "みせ~mise", "に~ni", "いきます~ikimasu"), "えき~eki", "に~ni"),
            match("8-5", "いえ~ie" to "Home", "がっこう~gakkou" to "School", "えき~eki" to "Station"),
            conversation("8-6", "どこに いきますか？~doko ni ikimasu ka?", "えきに いきます~eki ni ikimasu", "ほんを よみます~hon o yomimasu", "ねこが すきです~neko ga suki desu")),
        level(9, "Mini conversations",
            conversation("9-1", "こんにちは。おげんきですか？~konnichiwa. ogenki desu ka?", "はい、げんきです~hai, genki desu", "パンです~pan desu", "えきに いきます~eki ni ikimasu"),
            conversation("9-2", "なにを のみますか？~nani o nomimasu ka?", "おちゃを のみます~ocha o nomimasu", "ほんを よみます~hon o yomimasu", "ねこが すきです~neko ga suki desu"),
            conversation("9-3", "どこに いきますか？~doko ni ikimasu ka?", "がっこうに いきます~gakkou ni ikimasu", "おんがくを ききます~ongaku o kikimasu", "パンを たべます~pan o tabemasu"),
            cloze("9-4", "わたしは ねこが ___~watashi wa neko ga ___", "すきです~suki desu", "ですか~desu ka", "のみます~nomimasu"),
            build("9-5", "I read a book.", listOf("わたし~watashi", "は~wa", "ほん~hon", "を~o", "よみます~yomimasu"), "みず~mizu", "を~o", "いきます~ikimasu"),
            match("9-6", "おげんきですか？~ogenki desu ka?" to "How are you?", "どこですか？~doko desu ka?" to "Where is it?", "ありがとう~arigatou" to "Thank you")),
        level(10, "Mixed challenge",
            reverse("10-1", "Teacher", "せんせい~sensei", "がくせい~gakusei", "ともだち~tomodachi", "わたし~watashi"),
            cloze("10-2", "わたしは みずを ___~watashi wa mizu o ___", "のみます~nomimasu", "よみます~yomimasu", "いきます~ikimasu"),
            build("10-3", "I like music.", listOf("わたし~watashi", "は~wa", "おんがく~ongaku", "が~ga", "すき~suki", "です~desu"), "えいが~eiga", "が~ga", "のみます~nomimasu"),
            conversation("10-4", "ほんを よみますか？~hon o yomimasu ka?", "はい、よみます~hai, yomimasu", "いいえ、みずです~iie, mizu desu", "えきに いきます~eki ni ikimasu"),
            match("10-5", "みず~mizu" to "Water", "せんせい~sensei" to "Teacher", "よみます~yomimasu" to "Read", "えき~eki" to "Station"),
            conversation("10-6", "どこに いきますか？~doko ni ikimasu ka?", "みせに いきます~mise ni ikimasu", "ねこが すきです~neko ga suki desu", "おちゃを のみます~ocha o nomimasu"))
    ).also { lessons ->
        require(lessons.map { it.id }.distinct().size == lessons.size)
        lessons.forEach { it.validate() }
    }
}
