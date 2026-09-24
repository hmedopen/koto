package com.koto.app.feature.lesson.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.model.*
import com.koto.app.ui.components.*
import com.koto.app.ui.theme.KotoColors

@Composable
fun QuestionRenderer(session: LessonSession, minHeight: Dp, speechReady: Boolean, isSpeaking: Boolean,
    speak: (JapaneseText) -> Unit,
    sentenceScrollState: ScrollState? = null) {
    val q = session.question ?: return
    Column(Modifier.fillMaxWidth().heightIn(min = minHeight).testTag("question_${q.id}"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val title = when (q) {
            is Question.MeaningChoice -> "Choose the correct meaning"
            is Question.SentenceBuilder -> "Build the sentence"
            is Question.Cloze -> "Fill in the blank"
            is Question.ConversationResponse -> "Pick the best response"
            is Question.PairMatch -> "Match the pairs"
            is Question.Listening -> "Listen and choose"
        }
        Text(title, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().semantics { heading() }, color = KotoColors.Navy)
        Spacer(Modifier.height(16.dp))
        when (q) {
            is Question.MeaningChoice -> MeaningChoiceQuestion(q, session, speechReady, isSpeaking, speak)
            is Question.SentenceBuilder -> SentenceBuilderQuestion(q, session, speak, sentenceScrollState)
            is Question.Cloze -> ClozeQuestion(q, session, speechReady, isSpeaking, speak)
            is Question.ConversationResponse -> ConversationQuestion(q, session, speechReady, isSpeaking, speak)
            is Question.PairMatch -> PairMatchQuestion(q, session, speak)
            is Question.Listening -> ListeningQuestion(q, session, speechReady, isSpeaking, speak)
        }
        if (q !is Question.PairMatch) {
            // Reserve breathing room from the outset, independent of feedback visibility.
            Spacer(Modifier.height((minHeight * .065f).coerceIn(24.dp, 40.dp)))
        }
    }
}

@Composable
private fun Prompt(text: LessonText, speechReady: Boolean, isSpeaking: Boolean,
    speak: (JapaneseText) -> Unit, dialogue: Boolean = false, sentence: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), color = KotoColors.Background,
        shape = if (dialogue) RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp) else RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, KotoColors.Hairline)) {
        Column(Modifier.padding(if (sentence) 12.dp else 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ContentText(text, if (dialogue || sentence) 24.sp else 28.sp, alignReading = false)
            if (text is LessonText.Japanese) SpeakerButton(text.value, speechReady, isSpeaking, speak)
        }
    }
}

@Composable
private fun ColumnScope.AnswerGap() { Spacer(Modifier.weight(1f).heightIn(min = 20.dp, max = 120.dp)) }

@Composable
internal fun ColumnScope.MeaningChoiceQuestion(q: Question.MeaningChoice, session: LessonSession,
    speechReady: Boolean, isSpeaking: Boolean, speak: (JapaneseText) -> Unit) {
    Prompt(q.prompt, speechReady, isSpeaking, speak)
    AnswerGap()
    q.options.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { answer -> key(answer.id) {
                AnswerButton(answer, session, q.correctId, speak, Modifier.weight(1f).fillMaxHeight(), minHeight = 80.dp)
            } }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun ColumnScope.ConversationQuestion(q: Question.ConversationResponse, session: LessonSession,
    speechReady: Boolean, isSpeaking: Boolean, speak: (JapaneseText) -> Unit) {
    Prompt(LessonText.Japanese(q.incoming), speechReady, isSpeaking, speak, dialogue = true)
    AnswerGap()
    q.responses.forEach { answer -> key(answer.id) {
        AnswerButton(answer, session, q.correctId, speak, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
    } }
}

@Composable
internal fun ColumnScope.ClozeQuestion(q: Question.Cloze, session: LessonSession,
    speechReady: Boolean, isSpeaking: Boolean, speak: (JapaneseText) -> Unit) {
    ClozePrompt(q, session.state.selected, speechReady, isSpeaking, speak)
    AnswerGap()
    q.options.forEach { answer -> key(answer.id) {
        AnswerButton(answer, session, q.correctId, speak, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
    } }
}

@Composable
private fun ClozePrompt(q: Question.Cloze, selectedId: String?, speechReady: Boolean, isSpeaking: Boolean,
    speak: (JapaneseText) -> Unit) {
    val selected = (q.options.firstOrNull { it.id == selectedId }?.text as? LessonText.Japanese)?.value
    fun underlined(source: String, replacement: String?) = buildAnnotatedString {
        val pieces = source.split("___")
        append(pieces[0])
        pushStyle(androidx.compose.ui.text.SpanStyle(textDecoration = TextDecoration.Underline))
        append(replacement ?: "     ")
        pop()
        append(pieces[1])
    }
    Surface(Modifier.fillMaxWidth(), color = KotoColors.Background, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, KotoColors.Hairline)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(underlined(q.sentence.romaji, selected?.romaji), fontSize = 16.sp, color = KotoColors.QuietInk, textAlign = TextAlign.Center)
            Text(underlined(q.sentence.kana, selected?.kana), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            SpeakerButton(q.filled(selectedId), speechReady, isSpeaking, speak)
        }
    }
}

@Composable
private fun AnswerButton(answer: Answer, session: LessonSession, correctId: String,
    speak: (JapaneseText) -> Unit, modifier: Modifier, minHeight: Dp = 60.dp) {
    val selected = session.state.selected == answer.id
    val tone = when {
        session.checked && answer.id == correctId -> TactileTone.Correct
        session.checked && selected && session.correct == false -> TactileTone.Wrong
        selected -> TactileTone.Selected
        else -> TactileTone.Default
    }
    TactileButton({
        (answer.text as? LessonText.Japanese)?.let { speak(it.value) }
        session.select(answer.id)
    }, modifier.testTag("answer_${answer.id}"), enabled = !session.checked, tone = tone, selected = selected, deferPointerClick = false,
        stateLabel = when (tone) { TactileTone.Correct -> "Correct answer"; TactileTone.Wrong -> "Incorrect answer"; else -> null }) {
        Box(Modifier.fillMaxWidth().heightIn(min = minHeight - 24.dp), contentAlignment = Alignment.Center) {
            ContentText(answer.text, 20.sp)
        }
    }
}

@Composable
internal fun ColumnScope.PairMatchQuestion(q: Question.PairMatch, session: LessonSession, speak: (JapaneseText) -> Unit) {
    Text("Tap one Japanese card and its English match.", style = MaterialTheme.typography.bodyMedium,
        color = KotoColors.QuietInk, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(24.dp))
    val english = remember(q.id) { q.pairs.drop(1) + q.pairs.take(1) }
    q.pairs.indices.forEach { index ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(true to q.pairs[index], false to english[index]).forEach { (japanese, pair) -> key("$japanese-${pair.id}") {
                val matched = pair.id in session.state.matched
                val selected = pair.id == if (japanese) session.state.left else session.state.right
                val tone = when {
                    matched -> TactileTone.Correct
                    selected && session.state.mismatch -> TactileTone.Wrong
                    selected -> TactileTone.Selected
                    else -> TactileTone.Default
                }
                TactileButton({
                    if (japanese) speak(pair.japanese)
                    session.pair(pair.id, japanese)
                }, Modifier.weight(1f).fillMaxHeight().feedbackWiggle(selected && session.state.mismatch).testTag("pair_${if (japanese) "ja" else "en"}_${pair.id}"),
                    enabled = !matched && !session.checked && !session.state.mismatch, tone = tone, selected = selected,
                    stateLabel = if (matched) "Matched" else if (selected && session.state.mismatch) "Not a match" else null) {
                    Column(Modifier.fillMaxWidth().heightIn(min = 58.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        if (japanese) JapaneseTextBlock(pair.japanese, size = 18.sp)
                        else Text(pair.english, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            } }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun ColumnScope.ListeningQuestion(
    q: Question.Listening,
    session: LessonSession,
    speechReady: Boolean,
    isSpeaking: Boolean,
    speak: (JapaneseText) -> Unit
) {
    ListeningPrompt(q.target, speechReady, isSpeaking, speak)
    AnswerGap()
    q.options.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { answer -> key(answer.id) {
                AnswerButton(answer, session, q.correctId, speak, Modifier.weight(1f).fillMaxHeight(), minHeight = 80.dp)
            } }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ListeningPrompt(
    text: JapaneseText,
    speechReady: Boolean,
    isSpeaking: Boolean,
    speak: (JapaneseText) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        color = KotoColors.Background,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, KotoColors.Hairline)
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpeakerButton(text, speechReady, isSpeaking, speak)
            Text(
                "Tap to listen",
                fontSize = 16.sp,
                color = KotoColors.QuietInk,
                textAlign = TextAlign.Center
            )
        }
    }
}
