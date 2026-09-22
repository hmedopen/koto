package com.koto.app.feature.lesson.model

data class JapaneseText(val kana: String, val romaji: String, val tts: String = kana)
sealed interface LessonText {
    data class English(val value: String) : LessonText
    data class Japanese(val value: JapaneseText) : LessonText
}
data class Answer(val id: String, val text: LessonText)
data class MatchPair(val id: String, val japanese: JapaneseText, val english: String)
data class LessonDefinition(val id: Int, val title: String, val section: String, val questions: List<Question>)

sealed interface Question {
    val id: String
    data class MeaningChoice(override val id: String, val prompt: LessonText,
        val options: List<Answer>, val correctId: String) : Question
    data class SentenceBuilder(override val id: String, val prompt: String,
        val tiles: List<Answer>, val correctOrder: List<String>, val sentence: JapaneseText) : Question
    data class Cloze(override val id: String, val sentence: JapaneseText,
        val options: List<Answer>, val correctId: String) : Question {
        fun filled(answerId: String?): JapaneseText {
            val word = (options.firstOrNull { it.id == answerId }?.text as? LessonText.Japanese)?.value
                ?: return sentence
            return JapaneseText(sentence.kana.replace("___", word.kana),
                sentence.romaji.replace("___", word.romaji))
        }
    }
    data class ConversationResponse(override val id: String, val incoming: JapaneseText,
        val responses: List<Answer>, val correctId: String) : Question
    data class PairMatch(override val id: String, val pairs: List<MatchPair>) : Question
}

/** Fail early on authored content, independently of the renderer and speech engine. */
fun LessonDefinition.validate() {
    fun noHan(value: String) = require(value.codePoints().noneMatch {
        Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN
    }) { "Lesson $id contains kanji: $value" }
    fun japanese(value: JapaneseText) {
        require(value.kana.isNotBlank() && value.romaji.isNotBlank() && value.tts.isNotBlank())
        noHan(value.kana); noHan(value.romaji); noHan(value.tts)
        require(value.tts.any { it in '\u3040'..'\u30ff' })
        require(value.tts.none { it in 'a'..'z' || it in 'A'..'Z' })
    }
    fun text(value: LessonText) = when (value) {
        is LessonText.English -> noHan(value.value)
        is LessonText.Japanese -> japanese(value.value)
    }
    fun answers(options: List<Answer>, correct: String? = null) {
        require(options.isNotEmpty() && options.map { it.id }.distinct().size == options.size)
        require(options.all { it.id.isNotBlank() })
        if (correct != null) require(options.count { it.id == correct } == 1)
        options.forEach { text(it.text) }
    }
    noHan(title); noHan(section)
    require(questions.isNotEmpty() && questions.map { it.id }.distinct().size == questions.size)
    questions.forEach { q ->
        when (q) {
            is Question.MeaningChoice -> { text(q.prompt); answers(q.options, q.correctId) }
            is Question.ConversationResponse -> { japanese(q.incoming); answers(q.responses, q.correctId) }
            is Question.Cloze -> {
                japanese(q.sentence); answers(q.options, q.correctId)
                require(q.sentence.kana.split("___").size == 2 && q.sentence.romaji.split("___").size == 2)
                require(q.options.all { it.text is LessonText.Japanese })
            }
            is Question.SentenceBuilder -> {
                noHan(q.prompt); japanese(q.sentence); answers(q.tiles)
                require(q.tiles.all { it.text is LessonText.Japanese })
                require(q.correctOrder.isNotEmpty() && q.correctOrder.distinct() == q.correctOrder)
                require(q.correctOrder.all { id -> q.tiles.any { it.id == id } })
            }
            is Question.PairMatch -> {
                require(q.pairs.isNotEmpty() && q.pairs.map { it.id }.distinct().size == q.pairs.size)
                q.pairs.forEach { require(it.id.isNotBlank()); japanese(it.japanese); noHan(it.english) }
            }
        }
    }
}
