package com.koto.app.feature.translator.data

import android.content.Context
import com.koto.app.feature.translator.data.db.KotoDatabase
import com.koto.app.feature.translator.data.db.TranslationCardDao
import com.koto.app.feature.translator.data.db.TranslationCardEntity
import com.koto.app.feature.translator.model.SavedTranslationCard
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.ui.screens.cards.CardContext
import com.koto.app.ui.screens.cards.CardContextLoader
import com.koto.app.ui.screens.cards.CardExample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TranslatorCardStore(context: Context) {
    private val db = KotoDatabase.getInstance(context.applicationContext)
    private val dao: TranslationCardDao = db.translationCardDao()

    val starredCardsFlow: Flow<List<SavedTranslationCard>> = dao.getAllCardsFlow().map { entities ->
        entities.map { it.toSavedCard() }
    }

    fun loadStarredCards(): List<SavedTranslationCard> {
        val entities = runCatching { dao.getAllCards() }.getOrElse { emptyList() }
        // Ensure contexts are registered
        for (entity in entities) {
            registerEntityContext(entity)
        }
        return entities.map { it.toSavedCard() }
    }

    fun isStarred(sourceText: String, targetText: String): Boolean {
        if (sourceText.isBlank() || targetText.isBlank()) return false
        val s = sourceText.trim()
        val t = targetText.trim()
        return runCatching {
            if (dao.isStarred(s, t) || dao.isStarred(t, s)) return@runCatching true
            val pureS = KanaConverter.toPureKana(s)
            val pureT = KanaConverter.toPureKana(t)
            val allCards = dao.getAllCards()
            allCards.any { card ->
                val cardPureJp = KanaConverter.toPureKana(card.japanese)
                (card.english.equals(s, ignoreCase = true) && (card.japanese.equals(t, ignoreCase = true) || cardPureJp.equals(pureT, ignoreCase = true) || card.romaji.equals(t, ignoreCase = true))) ||
                (card.english.equals(t, ignoreCase = true) && (card.japanese.equals(s, ignoreCase = true) || cardPureJp.equals(pureS, ignoreCase = true) || card.romaji.equals(s, ignoreCase = true)))
            }
        }.getOrDefault(false)
    }

    fun isStarred(sourceText: String, targetText: String, cards: List<SavedTranslationCard>): Boolean {
        if (sourceText.isBlank() || targetText.isBlank()) return false
        val s = sourceText.trim()
        val t = targetText.trim()
        val pureS = KanaConverter.toPureKana(s)
        val pureT = KanaConverter.toPureKana(t)
        return cards.any { card ->
            val cardPureJp = KanaConverter.toPureKana(card.targetText)
            (card.sourceText.equals(s, ignoreCase = true) && (card.targetText.equals(t, ignoreCase = true) || cardPureJp.equals(pureT, ignoreCase = true) || card.targetRomaji.equals(t, ignoreCase = true))) ||
            (card.sourceText.equals(t, ignoreCase = true) && (card.targetText.equals(s, ignoreCase = true) || cardPureJp.equals(pureS, ignoreCase = true) || card.targetRomaji.equals(s, ignoreCase = true))) ||
            (card.targetText.equals(s, ignoreCase = true) && card.sourceText.equals(t, ignoreCase = true)) ||
            (card.targetText.equals(t, ignoreCase = true) && card.sourceText.equals(s, ignoreCase = true))
        }
    }

    fun toggleStar(
        sourceText: String,
        targetText: String,
        targetRomaji: String,
        sourceLang: TranslationLanguage,
        targetLang: TranslationLanguage,
    ): Boolean {
        if (sourceText.isBlank() || targetText.isBlank()) return false

        val isJpTarget = targetLang == TranslationLanguage.Japanese
        val rawJp = if (isJpTarget) targetText.trim() else sourceText.trim()
        val enText = if (isJpTarget) sourceText.trim() else targetText.trim()

        val pureKana = KanaConverter.toPureKana(rawJp)
        val romaji = if (targetRomaji.isNotBlank()) {
            targetRomaji.trim()
        } else {
            KanaConverter.toRomaji(pureKana)
        }

        val currentlyStarred = isStarred(sourceText, targetText) || isStarred(pureKana, enText) || isStarred(rawJp, enText)

        if (currentlyStarred) {
            runCatching {
                dao.deleteByPair(pureKana, enText)
                dao.deleteByPair(rawJp, enText)
                dao.deleteByPair(sourceText.trim(), targetText.trim())
            }
            return false
        } else {
            val id = "trans_${System.currentTimeMillis()}_${(rawJp + enText).hashCode()}"
            val isSentence = isSentenceText(sourceText) || isSentenceText(targetText)
            val entity = TranslationCardEntity(
                id = id,
                japanese = rawJp,
                romaji = romaji,
                english = enText,
                isSentence = isSentence,
                contextNote = if (isSentence) "Full sentence translation." else "Quick translation card.",
                exampleKana = if (isSentence) rawJp else "",
                exampleRomaji = if (isSentence) romaji else "",
                exampleEnglish = if (isSentence) enText else "",
                timestamp = System.currentTimeMillis(),
            )
            runCatching { dao.insertCard(entity) }
            registerEntityContext(entity)
            return true
        }
    }

    fun removeCard(cardId: String) {
        runCatching { dao.deleteById(cardId) }
        CardContextLoader.removeContext(cardId)
    }

    fun clearAll() {
        runCatching { dao.clearAll() }
    }

    private fun registerEntityContext(entity: TranslationCardEntity) {
        val examples = if (entity.isSentence && entity.exampleKana.isNotBlank()) {
            listOf(
                CardExample(
                    kana = entity.exampleKana,
                    romaji = entity.exampleRomaji,
                    english = entity.exampleEnglish,
                )
            )
        } else {
            emptyList()
        }

        CardContextLoader.registerContext(
            cardId = entity.id,
            context = CardContext(
                cardId = entity.id,
                kana = entity.japanese,
                romaji = entity.romaji,
                english = entity.english,
                usageNote = entity.contextNote.ifEmpty { "Saved from Quick Translations." },
                examples = examples,
            ),
        )
    }

    private fun isSentenceText(text: String): Boolean {
        val clean = text.trim()
        return clean.any { it in ".!?。！？" } || clean.split(Regex("\\s+")).size >= 3
    }

    private fun TranslationCardEntity.toSavedCard(): SavedTranslationCard {
        return SavedTranslationCard(
            id = id,
            sourceText = english,
            targetText = japanese,
            targetRomaji = romaji,
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
            timestamp = timestamp,
        )
    }
}
