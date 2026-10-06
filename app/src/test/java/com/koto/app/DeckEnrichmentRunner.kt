package com.koto.app

import com.koto.app.feature.translator.data.KanaConverter
import com.koto.app.ui.screens.cards.CardExample
import com.koto.app.ui.screens.cards.loadFlashcardDecksFallback
import org.junit.Ignore
import org.junit.Test
import java.io.File

class DeckEnrichmentRunner {

    data class JmdictSense(
        val glosses: List<String>,
        val isUk: Boolean,
    )

    data class JmdictKanji(
        val text: String,
        val common: Boolean,
        val isRare: Boolean,
    )

    data class JmdictEntry(
        val id: String,
        val kanji: List<JmdictKanji>,
        val kana: List<String>,
        val senses: List<JmdictSense>,
    )

    data class CardEnrichment(
        val kanji: String?,
        val furigana: String?,
    )

    private fun extractStrings(json: String, key: String): List<String> {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.findAll(json).map { it.groupValues[1] }.toList()
    }

    private fun extractJsonString(source: String, key: String): String {
        val search = "\"$key\":\""
        val startIdx = source.indexOf(search)
        if (startIdx == -1) return ""
        val valStart = startIdx + search.length
        val sb = StringBuilder()
        var i = valStart
        while (i < source.length) {
            val c = source[i]
            if (c == '\\' && i + 1 < source.length) {
                when (val next = source[i + 1]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> { sb.append('\\'); sb.append(next) }
                }
                i += 2
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun parseJmdictLine(line: String): JmdictEntry? {
        if (!line.startsWith("{\"id\":")) return null
        val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: return null

        val kanjiBlock = line.substringAfter("\"kanji\":[", "").substringBefore("],\"kana\":", "")
        val kanjiList = mutableListOf<JmdictKanji>()
        if (kanjiBlock.isNotBlank()) {
            val kanjiObjs = kanjiBlock.split("},{")
            for (ko in kanjiObjs) {
                val text = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(ko)?.groupValues?.get(1) ?: continue
                val common = ko.contains("\"common\":true")
                val isRare = ko.contains("\"rK\"") || ko.contains("\"iK\"")
                kanjiList.add(JmdictKanji(text, common, isRare))
            }
        }

        val kanaBlock = line.substringAfter("\"kana\":[", "").substringBefore("],\"sense\":", "")
        val kanaList = extractStrings(kanaBlock, "text")

        val senseBlock = line.substringAfter("\"sense\":[", "").substringBeforeLast("]")
        val senseList = mutableListOf<JmdictSense>()
        if (senseBlock.isNotBlank()) {
            val senseObjs = senseBlock.split("},{")
            for (so in senseObjs) {
                val isUk = so.contains("\"uk\"")
                val glosses = extractStrings(so, "text")
                senseList.add(JmdictSense(glosses, isUk))
            }
        }

        return JmdictEntry(id, kanjiList, kanaList, senseList)
    }

    private fun generateFurigana(text: String): String {
        if (!KanaConverter.containsKanji(text)) return text
        if (text.contains("/")) {
            return text.split("/").joinToString(" / ") { generateFurigana(it.trim()) }
        }
        val tokens = KanaConverter.extractRubyTokens(text)
        return tokens.joinToString("") { token ->
            if (token.reading != null && token.reading != token.surface) {
                "${token.surface}{${token.reading}}"
            } else {
                token.surface
            }
        }
    }

    @Ignore("Offline dataset enrichment tool")
    @Test
    fun executeAssetEnrichment() {
        val jmdictFile = File("C:/Users/HMED OPEN/.gemini/antigravity/brain/616d938c-0be7-4848-8a3a-5e46529cbc10/scratch/jmdict-eng-common-3.6.2.json")
        println("Loading JMdict from: ${jmdictFile.absolutePath}")
        require(jmdictFile.exists()) { "JMdict file not found" }

        val kanaMap = mutableMapOf<String, MutableList<JmdictEntry>>()
        jmdictFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim().removeSuffix(",")
                val entry = parseJmdictLine(trimmed) ?: continue
                for (k in entry.kana) {
                    kanaMap.getOrPut(k) { mutableListOf() }.add(entry)
                }
            }
        }

        val rootDir = File(".").canonicalFile
        val candidates = listOf(
            File(rootDir, "src/main/assets/flashcard_decks.json"),
            File(rootDir, "app/src/main/assets/flashcard_decks.json"),
            File("../app/src/main/assets/flashcard_decks.json"),
            File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/flashcard_decks.json"),
        )
        val decksFile = candidates.firstOrNull { it.exists() } ?: error("Decks asset not found")
        val decks = loadFlashcardDecksFallback(decksFile.readText(Charsets.UTF_8))

        val stopWords = setOf("to", "a", "an", "the", "in", "on", "at", "of", "for", "with", "by", "from", "or", "and", "esp", "e.g")

        fun extractWords(text: String): Set<String> {
            return text.lowercase()
                .replace(Regex("[^a-z0-9 ]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 1 && it !in stopWords }
                .toSet()
        }

        fun scoreCandidate(cand: JmdictEntry, cardWords: Set<String>, english: String): Int {
            var score = 0
            val candWords = cand.senses.flatMap { it.glosses }.flatMap { extractWords(it) }.toSet()
            val overlap = cardWords.intersect(candWords).size
            score += overlap * 10

            val candGlossesLower = cand.senses.flatMap { it.glosses }.map { it.lowercase() }
            for (g in candGlossesLower) {
                if (g in english.lowercase() || english.lowercase() in g) {
                    score += 15
                }
            }

            if (cand.kanji.any { it.common }) score += 2
            return score
        }

        val curatedOverrides = mapOf(
            "ごめんなさい" to "ごめんなさい",
            "じゃあね" to "じゃあね",
            "おつかれさまです" to "お疲れ様です",
            "わたくし" to "私",
            "あなたたち" to "あなた達",
            "ぜろ / れい" to "ゼロ / 零",
            "よん / し" to "四",
            "なな / しち" to "七",
            "きゅう / く" to "九",
            "にじゅうご" to "二十五",
            "ぷらす" to "プラス",
            "まいなす" to "マイナス",
            "ほん / ぼん / ぽん" to "本",
            "はい / ばい / ぱい" to "杯",
            "ひき / びき / ぴき" to "匹",
            "ふん / ぷん" to "分",
            "くらい / ぐらい" to "くらい / ぐらい",
            "なに / なん" to "何",
            "どのくらい / どれくらい" to "どのくらい / どれくらい",
            "どうやって" to "どうやって",
            "こちら / こっち" to "こちら / こっち",
            "そちら / そっち" to "そちら / そっち",
            "あちら / あっち" to "あちら / あっち",
            "どちら / どっち" to "どちら / どっち",
            "じゅうでんき" to "充電器",
            "あんないじょ" to "案内所",
            "ひなんじょ" to "避難所",
            "ざんぎょうだい" to "残業代",
            "にゅうじょうりょう" to "入場料",
            "れんしゅうもんだい" to "練習問題",
            "おかしづくり" to "お菓子作り",
            "ゆきだるま" to "雪だるま",
            "せつぶん" to "節分",
            "おみくじ" to "おみくじ",
            "たすけて" to "助けて",
            "こまった" to "困った",
            "よかった" to "よかった",
            "そうだね" to "そうだね",
            "まじで" to "マジで",
            "るんるん" to "るんるん",
            "バスケ" to "バスケ",
            "ワイファイ" to "Wi-Fi",
            "アップロード" to "アップロード",
            "ペースト" to "ペースト",
            "リュック" to "リュック",
            "らいおん" to "ライオン",
            "やぎ" to "ヤギ",
            "ほしがた" to "星形",
            "あたまがいい" to "頭がいい",
            "きぶんがいい" to "気分がいい",
            "ゆうきがある" to "勇気がある",
            "れいぎただしい" to "礼儀正しい",
            "そのため" to "そのため",
            "でんしレンジ" to "電子レンジ",
            "ひじょうベル" to "非常ベル",
            "しゅげい" to "手芸",
            "あたため" to "温め",
            "ちゃいろ / ちゃいろい" to "茶色 / 茶色い",
            "おゆ" to "お湯",
            "おきゃくさん" to "お客さん",
            "ふくろ / レジぶくろ" to "袋 / レジ袋",
            "すくなめ" to "少なめ",
            "クローゼット" to "クローゼット",
            "ポット / ケトル" to "ポット / ケトル",
            "まつげ" to "まつ毛",
            "ほっぺ" to "ほっぺ",
            "ふくらはぎ" to "ふくらはぎ",
            "ほけんしょう" to "保険証",
            "いたみどめ" to "痛み止め",
            "ばんそうこう" to "絆創膏",
            "しっぷ" to "湿布",
            "ばんせん" to "番線",
            "おりば" to "降り場",
            "かくえきていしゃ" to "各駅停車",
            "すごく" to "すごく",
            "がっかりする" to "がっかりする",
            "ほっとする" to "ほっとする",
            "わくわくする" to "わくわくする",
            "ドキドキする" to "ドキドキする",
            "モヤモヤする" to "モヤモヤする",
            "せっかちな" to "せっかちな",
            "じょうねつてきな" to "情熱的な",
            "けちな" to "けちな",
            "おおらかな" to "大らかな",
        )

        fun findBestMatch(term: String, cardWords: Set<String>, english: String): Pair<String?, Boolean>? {
            val trimmedTerm = term.trim()
            if (trimmedTerm.isBlank()) return null

            curatedOverrides[trimmedTerm]?.let {
                val hasKanji = KanaConverter.containsKanji(it)
                return Pair(it, !hasKanji)
            }

            val candidates = kanaMap[trimmedTerm]
            if (!candidates.isNullOrEmpty()) {
                var bestCandidate: JmdictEntry? = null
                var bestScore = -1
                for (cand in candidates) {
                    val score = scoreCandidate(cand, cardWords, english)
                    if (score > bestScore) {
                        bestScore = score
                        bestCandidate = cand
                    }
                }
                if (bestCandidate != null && bestCandidate.kanji.isNotEmpty()) {
                    val isUk = bestCandidate.senses.firstOrNull()?.isUk == true
                    val allRare = bestCandidate.kanji.all { it.isRare }
                    if (isUk || allRare) {
                        return Pair(trimmedTerm, true)
                    }
                    val chosen = bestCandidate.kanji.firstOrNull { it.common }?.text
                        ?: bestCandidate.kanji.first().text
                    return Pair(chosen, false)
                }
                if (bestCandidate != null && bestCandidate.kanji.isEmpty()) {
                    return Pair(trimmedTerm, true)
                }
            }

            if (trimmedTerm.endsWith("な") && trimmedTerm.length > 2) {
                val root = trimmedTerm.removeSuffix("な")
                val rootCandidates = kanaMap[root]
                if (!rootCandidates.isNullOrEmpty()) {
                    var bestCandidate: JmdictEntry? = null
                    var bestScore = -1
                    for (cand in rootCandidates) {
                        val score = scoreCandidate(cand, cardWords, english)
                        if (score > bestScore) {
                            bestScore = score
                            bestCandidate = cand
                        }
                    }
                    if (bestCandidate != null && bestCandidate.kanji.isNotEmpty()) {
                        val chosen = bestCandidate.kanji.firstOrNull { it.common }?.text
                            ?: bestCandidate.kanji.first().text
                        return Pair(chosen + "な", false)
                    }
                }
            }

            if (trimmedTerm.endsWith("に") && trimmedTerm.length > 2) {
                val root = trimmedTerm.removeSuffix("に")
                val rootCandidates = kanaMap[root]
                if (!rootCandidates.isNullOrEmpty()) {
                    var bestCandidate: JmdictEntry? = null
                    var bestScore = -1
                    for (cand in rootCandidates) {
                        val score = scoreCandidate(cand, cardWords, english)
                        if (score > bestScore) {
                            bestScore = score
                            bestCandidate = cand
                        }
                    }
                    if (bestCandidate != null && bestCandidate.kanji.isNotEmpty()) {
                        val chosen = bestCandidate.kanji.firstOrNull { it.common }?.text
                            ?: bestCandidate.kanji.first().text
                        return Pair(chosen + "に", false)
                    }
                }
            }

            if (trimmedTerm.endsWith("する") && trimmedTerm.length > 2) {
                val noun = trimmedTerm.removeSuffix("する")
                val nounCandidates = kanaMap[noun]
                if (!nounCandidates.isNullOrEmpty()) {
                    var bestCandidate: JmdictEntry? = null
                    var bestScore = -1
                    for (cand in nounCandidates) {
                        val score = scoreCandidate(cand, cardWords, english)
                        if (score > bestScore) {
                            bestScore = score
                            bestCandidate = cand
                        }
                    }
                    if (bestCandidate != null && bestCandidate.kanji.isNotEmpty()) {
                        val chosen = bestCandidate.kanji.firstOrNull { it.common }?.text
                            ?: bestCandidate.kanji.first().text
                        return Pair(chosen + "する", false)
                    }
                }
            }

            if (trimmedTerm.startsWith("お") && trimmedTerm.length > 2) {
                val root = trimmedTerm.removePrefix("お")
                val rootCandidates = kanaMap[root]
                if (!rootCandidates.isNullOrEmpty()) {
                    var bestCandidate: JmdictEntry? = null
                    var bestScore = -1
                    for (cand in rootCandidates) {
                        val score = scoreCandidate(cand, cardWords, english)
                        if (score > bestScore) {
                            bestScore = score
                            bestCandidate = cand
                        }
                    }
                    if (bestCandidate != null && bestCandidate.kanji.isNotEmpty()) {
                        val chosen = bestCandidate.kanji.firstOrNull { it.common }?.text
                            ?: bestCandidate.kanji.first().text
                        return Pair("お" + chosen, false)
                    }
                }
            }

            if (trimmedTerm.startsWith("ご") && trimmedTerm.length > 2) {
                val root = trimmedTerm.removePrefix("ご")
                val rootCandidates = kanaMap[root]
                if (!rootCandidates.isNullOrEmpty()) {
                    var bestCandidate: JmdictEntry? = null
                    var bestScore = -1
                    for (cand in rootCandidates) {
                        val score = scoreCandidate(cand, cardWords, english)
                        if (score > bestScore) {
                            bestScore = score
                            bestCandidate = cand
                        }
                    }
                    if (bestCandidate != null && bestCandidate.kanji.isNotEmpty()) {
                        val chosen = bestCandidate.kanji.firstOrNull { it.common }?.text
                            ?: bestCandidate.kanji.first().text
                        return Pair("ご" + chosen, false)
                    }
                }
            }

            return null
        }

        val cardMap = mutableMapOf<String, CardEnrichment>()
        val cardKanaMap = mutableMapOf<String, CardEnrichment>()

        // Process cards
        for (deck in decks) {
            for (card in deck.cards) {
                val japanese = card.japanese
                val english = card.english
                val cardWords = extractWords(english)

                val kanjiResult: String?
                if (japanese.contains("/")) {
                    val parts = japanese.split("/").map { it.trim() }
                    val partResults = parts.map { part -> findBestMatch(part, cardWords, english) }
                    val combined = partResults.map { it?.first ?: "" }.joinToString(" / ")
                    kanjiResult = if (KanaConverter.containsKanji(combined)) combined else null
                } else {
                    val match = findBestMatch(japanese, cardWords, english)
                    val matchedText = match?.first
                    kanjiResult = if (matchedText != null && KanaConverter.containsKanji(matchedText)) matchedText else null
                }

                val furiganaResult = kanjiResult?.let { generateFurigana(it) }
                val enrichment = CardEnrichment(kanjiResult, furiganaResult)
                cardMap[card.id] = enrichment
                cardKanaMap[card.japanese] = enrichment
            }
        }

        println("Enriched cards count: ${cardMap.size}. Total with kanji: ${cardMap.values.count { it.kanji != null }}")

        // 1. Rewrite flashcard_decks.json
        val sb = java.lang.StringBuilder()
        sb.append("[\n")
        decks.forEachIndexed { dIdx, deck ->
            sb.append("  {\n")
            sb.append("    \"id\": \"${deck.id}\",\n")
            sb.append("    \"number\": ${deck.number},\n")
            sb.append("    \"title\": \"${escapeJson(deck.title)}\",\n")
            sb.append("    \"category\": \"${escapeJson(deck.category)}\",\n")
            sb.append("    \"tier\": ${deck.tier},\n")
            sb.append("    \"icon\": \"${escapeJson(deck.icon)}\",\n")
            sb.append("    \"cards\": [\n")

            deck.cards.forEachIndexed { cIdx, card ->
                val enrichment = cardMap[card.id]
                sb.append("      {\n")
                sb.append("        \"id\": \"${card.id}\",\n")
                sb.append("        \"japanese\": \"${escapeJson(card.japanese)}\",\n")
                if (enrichment?.kanji != null) {
                    sb.append("        \"kanji\": \"${escapeJson(enrichment.kanji)}\",\n")
                    sb.append("        \"furigana\": \"${escapeJson(enrichment.furigana ?: enrichment.kanji)}\",\n")
                }
                sb.append("        \"romaji\": \"${escapeJson(card.romaji)}\",\n")
                sb.append("        \"english\": \"${escapeJson(card.english)}\",\n")
                sb.append("        \"dueTimestamp\": ${card.dueTimestamp},\n")
                sb.append("        \"intervalDays\": ${card.intervalDays},\n")
                sb.append("        \"state\": \"${card.state}\"\n")
                sb.append("      }${if (cIdx < deck.cards.size - 1) "," else ""}\n")
            }

            sb.append("    ]\n")
            sb.append("  }${if (dIdx < decks.size - 1) "," else ""}\n")
        }
        sb.append("]\n")

        decksFile.writeText(sb.toString(), Charsets.UTF_8)
        println("Wrote enriched flashcard_decks.json to ${decksFile.absolutePath}")

        // 2. Rewrite card_contexts.json
        val contextCandidates = listOf(
            File(rootDir, "src/main/assets/card_contexts.json"),
            File(rootDir, "app/src/main/assets/card_contexts.json"),
            File("../app/src/main/assets/card_contexts.json"),
            File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/card_contexts.json"),
        )
        val contextFile = contextCandidates.firstOrNull { it.exists() } ?: error("card_contexts.json not found")
        val contextLines = contextFile.readLines(Charsets.UTF_8)

        val ctxSb = java.lang.StringBuilder()
        for (line in contextLines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("\"") || !trimmed.contains("\": {")) {
                ctxSb.append(line).append("\n")
                continue
            }

            val cardId = extractJsonString(trimmed, "cardId")
            val kana = extractJsonString(trimmed, "kana")
            val enrichment = cardMap[cardId] ?: cardKanaMap[kana]

            var updatedLine = line
            if (enrichment?.kanji != null) {
                val kanjiJson = "\"kanji\":\"${escapeJson(enrichment.kanji)}\","
                // Insert after "kana":"..."
                val kanaSearch = "\"kana\":\""
                val kanaStart = updatedLine.indexOf(kanaSearch)
                if (kanaStart != -1) {
                    val kanaValEnd = updatedLine.indexOf("\"", kanaStart + kanaSearch.length)
                    if (kanaValEnd != -1 && updatedLine.indexOf(",\"kanji\":", kanaValEnd) == -1 && updatedLine.indexOf("\"kanji\":", kanaValEnd) != kanaValEnd + 2) {
                        // Insert kanji right after comma
                        val commaPos = updatedLine.indexOf(",", kanaValEnd)
                        if (commaPos != -1) {
                            updatedLine = updatedLine.substring(0, commaPos + 1) + kanjiJson + updatedLine.substring(commaPos + 1)
                        }
                    }
                }

                // In sentence examples: if example kana contains target word, insert kanji for the example
                val targetKana = kana.split("/").first().trim()
                val targetKanji = enrichment.kanji.split("/").first().trim()
                if (targetKana.isNotBlank() && targetKanji.isNotBlank()) {
                    val exMarker = "\"examples\":["
                    val exIdx = updatedLine.indexOf(exMarker)
                    if (exIdx != -1) {
                        val prefix = updatedLine.substring(0, exIdx + exMarker.length)
                        val exBody = updatedLine.substring(exIdx + exMarker.length)
                        // Process each example
                        val updatedExBody = exBody.replace(Regex("\\{\"kana\":\"([^\"]+)\"")) { match ->
                            val exKana = match.groupValues[1]
                            val exKanji = exKana.replace(targetKana, targetKanji)
                            if (exKanji != exKana && KanaConverter.containsKanji(exKanji)) {
                                "{\"kana\":\"$exKana\",\"kanji\":\"${escapeJson(exKanji)}\""
                            } else {
                                match.value
                            }
                        }
                        updatedLine = prefix + updatedExBody
                    }
                }
            }

            ctxSb.append(updatedLine).append("\n")
        }

        contextFile.writeText(ctxSb.toString(), Charsets.UTF_8)
        println("Wrote enriched card_contexts.json to ${contextFile.absolutePath}")
    }

    @Ignore("Offline dataset enrichment tool")
    @Test
    fun executeContextAssetEnrichment() {
        val rootDir = java.io.File(".").canonicalFile
        val deckCandidates = listOf(
            java.io.File(rootDir, "src/main/assets/flashcard_decks.json"),
            java.io.File(rootDir, "app/src/main/assets/flashcard_decks.json"),
            java.io.File("../app/src/main/assets/flashcard_decks.json"),
            java.io.File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/flashcard_decks.json"),
        )
        val decksFile = deckCandidates.firstOrNull { it.exists() } ?: error("Decks asset not found")
        val decks = loadFlashcardDecksFallback(decksFile.readText(Charsets.UTF_8))
        val cardMap = decks.flatMap { it.cards }.associateBy { it.id }
        val cardKanaMap = decks.flatMap { it.cards }.associateBy { it.japanese }

        val ambiguityBlacklist = setOf(
            "せき", "はい", "いい", "いる", "ある", "する", "いう", "みる", "きく", "いく", "くる", "でる", "たつ", "ねる", "のむ",
            "かる", "かう", "きる", "てる", "しる", "ちち", "はは", "あし", "て", "め", "き", "は", "か", "ひ", "え", "と",
            "から", "まで", "より", "どこ", "だれ", "なに", "なん", "どう", "いくら", "いくつ", "これ", "それ", "あれ", "どれ",
            "ここ", "そこ", "あそこ", "こちら", "そちら", "あちら", "どちら", "こっち", "そっち", "あっち", "どっち",
            "ました", "ません", "でした", "です", "ます", "たい", "ない", "かった", "た", "て", "だ", "んだ", "んで",
            "みます", "みません", "かります", "かりました", "しました", "します", "いた", "いて", "いった", "いって",
            "わたくし", "ぼく", "あなた", "みなさん", "ひと", "もの", "こと", "とき", "まえ", "あと"
        )

        val vocab = mutableMapOf<String, String>()

        fun addWord(k: String, v: String) {
            val tk = k.trim()
            val tv = v.trim()
            if (tk.length >= 2 && tv.isNotBlank() && tk !in ambiguityBlacklist && KanaConverter.containsKanji(tv)) {
                vocab[tk] = tv
            }
        }

        // Add curated conversational phrases and mappings
        val curated = mapOf(
            "よろしくおねがいします" to "よろしくお願いします",
            "おつかれさまでした" to "お疲れ様でした",
            "おつかれさまです" to "お疲れ様です",
            "ごちそうさまでした" to "ご馳走様でした",
            "もうしわけありません" to "申し訳ありません",
            "もうしわけございません" to "申し訳ございません",
            "おやすみなさい" to "お休みなさい",
            "おやすみ" to "お休み",
            "おねがいします" to "お願いします",
            "しつれいしました" to "失礼しました",
            "しつれいします" to "失礼します",
            "はじめましょう" to "始めましょう",
            "おわりましょう" to "終わりましょう",
            "つくりましょう" to "作りましょう",
            "おきましょう" to "起きましょう",
            "いきませんか" to "行きませんか",
            "いきましょう" to "行きましょう",
            "たべましょう" to "食べましょう",
            "のみましょう" to "飲みましょう",
            "みましょう" to "見ましょう",
            "ききましょう" to "聞きましょう",
            "はなしましょう" to "話しましょう",
            "てつだってくれて" to "手伝ってくれて",
            "おしえてくれて" to "教えてくれて",
            "いってきます" to "行ってきます",
            "おげんきですか" to "お元気ですか",
            "おげんきで" to "お元気で",
            "いただきます" to "頂きます",
            "はじめまして" to "初めまして",
            "さようなら" to "さようなら",
            "とうきょう" to "東京",
            "にほんご" to "日本語",
            "にほん" to "日本",
            "えいご" to "英語",
            "べんきょう" to "勉強",
            "しゅくだい" to "宿題",
            "かいぎ" to "会議",
            "かいしゃ" to "会社",
            "がっこう" to "学校",
            "おてんき" to "お天気",
            "てんき" to "天気",
            "あさごはん" to "朝ご飯",
            "ひるごはん" to "昼ご飯",
            "よるごはん" to "夜ご飯",
            "ばんごはん" to "晩ご飯",
            "ごはん" to "ご飯",
            "おべんとう" to "お弁当",
            "おにぎり" to "お握り",
            "たなかさん" to "田中さん",
            "やまださん" to "山田さん",
            "すずきさん" to "鈴木さん",
            "さとうさん" to "佐藤さん",
            "けんくん" to "健くん",
            "たなか" to "田中",
            "やまだ" to "山田",
            "すずき" to "鈴木",
            "さとう" to "佐藤",
            "おとうさん" to "お父さん",
            "おかあさん" to "お母さん",
            "おにいさん" to "お兄さん",
            "おねえさん" to "お姉さん",
            "おとうと" to "弟",
            "いもうと" to "妹",
            "かぞく" to "家族",
            "せんせい" to "先生",
            "がくせい" to "学生",
            "ともだち" to "友達",
            "いっしょに" to "一緒に",
            "ほんとうに" to "本当に",
            "きょう" to "今日",
            "あした" to "明日",
            "きのう" to "昨日",
            "あさ" to "朝",
            "ひる" to "昼",
            "よる" to "夜",
            "ばん" to "晩",
            "けさ" to "今朝",
            "こんばん" to "今晩",
            "まいあさ" to "毎朝",
            "まいにち" to "毎日",
            "まいばん" to "毎晩",
            "まいしゅう" to "毎週",
            "まいげつ" to "毎月",
            "まいとし" to "毎年",
            "こんしゅう" to "今週",
            "らいしゅう" to "来週",
            "せんしゅう" to "先週",
            "でんわ" to "電話",
            "えいが" to "映画",
            "りょうり" to "料理",
            "しゃしん" to "写真",
            "おんがく" to "音楽",
            "じかん" to "時間",
            "びょういん" to "病院",
            "くすり" to "薬",
            "とけい" to "時計",
            "さいふ" to "財布",
            "へや" to "部屋",
            "えき" to "駅",
            "くるま" to "車",
            "でんしゃ" to "電車",
            "じてんしゃ" to "自転車",
            "ひこうき" to "飛行機",
            "となり" to "隣",
            "ゆめ" to "夢",
            "かさ" to "傘",
            "みち" to "道",
            "おちゃ" to "お茶",
            "おゆ" to "お湯",
            "みず" to "水",
            "さかな" to "魚",
            "にく" to "肉",
            "やさい" to "野菜",
            "くだもの" to "果物",
            "ほん" to "本",
            "いぬ" to "犬",
            "ねこ" to "猫",
            "あめ" to "雨",
            "ゆき" to "雪",
            "かぜ" to "風",
            "そら" to "空",
            "うみ" to "海",
            "やま" to "山",
            "かわ" to "川",
            "はな" to "花",
            "しちじ" to "七時",
            "はちじ" to "八時",
            "ろくじ" to "六時",
            "ごじ" to "五時",
            "よじ" to "四時",
            "さんじ" to "三時",
            "にじ" to "二時",
            "いちじ" to "一時",
            "くじ" to "九時",
            "じゅうじ" to "十時",
            "すこし" to "少し",
            "さむく" to "寒く",
            "あつく" to "暑く"
        )
        for ((k, v) in curated) addWord(k, v)

        // Add 3+ kana nouns and vocabulary from decks
        for (d in decks) {
            for (c in d.cards) {
                if (!c.kanji.isNullOrBlank() && c.japanese.length >= 3 && !c.japanese.contains("/")) {
                    addWord(c.japanese, c.kanji)
                }
            }
        }

        val sortedVocab = vocab.entries.sortedByDescending { it.key.length }
        println("Prepared ${sortedVocab.size} vocabulary replacement patterns.")

        val curatedCardKanji = mapOf(
            "card_01_001" to "お早う",
            "card_01_002" to "お早うございます",
            "card_01_003" to "今日は",
            "card_01_004" to "今晩は",
            "card_01_005" to "お休み",
            "card_01_006" to "お休みなさい",
            "card_01_007" to "有難う",
            "card_01_008" to "有難うございます",
            "card_01_009" to "どう致しまして",
            "card_01_010" to "済みません",
            "card_01_011" to "御免なさい",
            "card_01_014" to "よろしくお願いします",
            "card_01_015" to "初めまして",
            "card_01_016" to "左様なら",
            "card_01_018" to "又ね",
            "card_01_019" to "失礼します",
            "card_01_020" to "お疲れ様です",
            "card_01_021" to "お疲れ様でした",
            "card_01_022" to "頂きます",
            "card_01_023" to "ご馳走様でした",
            "card_01_024" to "行ってきます",
            "card_01_025" to "只今",
            "card_01_026" to "お帰りなさい",
            "card_01_027" to "お大事に",
            "card_01_028" to "おめでとうございます",
            "card_01_029" to "申し訳ありません",
            "card_01_030" to "お元気ですか",
            "card_02_005" to "貴方",
            "card_03_014" to "二十",
            "card_03_016" to "三十",
            "card_03_017" to "四十",
            "card_03_018" to "五十",
            "card_03_019" to "六十",
            "card_03_020" to "七十",
            "card_03_021" to "八十",
            "card_03_022" to "九十",
            "card_04_011" to "幾つ",
            "card_05_020" to "頃",
            "card_12_005" to "バッグ / 鞄",
            "card_12_008" to "眼鏡",
            "card_16_020" to "林檎",
            "card_16_022" to "蜜柑",
            "card_16_023" to "苺",
            "card_17_007" to "お握り"
        )

        data class Span(val start: Int, val end: Int, val replacement: String)

        fun enrichSentence(
            sentenceKana: String,
            targetKana: String?,
            targetKanji: String?
        ): String {
            val accepted = mutableListOf<Span>()
            fun overlaps(start: Int, end: Int): Boolean =
                accepted.any { !(end <= it.start || start >= it.end) }

            // 1. Target word priority if target has kanji
            if (!targetKana.isNullOrBlank() && !targetKanji.isNullOrBlank() && KanaConverter.containsKanji(targetKanji)) {
                val tKanaClean = targetKana.split("/").first().trim()
                val tKanjiClean = targetKanji.split("/").first().trim()
                val targetPairs = mutableListOf(Pair(tKanaClean, tKanjiClean))

                // If target word is an ichidan verb, add its own polite/te forms for its own examples
                if (tKanaClean.endsWith("る") && tKanjiClean.endsWith("る") && tKanaClean.length >= 2) {
                    val stemK = tKanaClean.removeSuffix("る")
                    val stemV = tKanjiClean.removeSuffix("る")
                    targetPairs.add(Pair("${stemK}ます", "${stemV}ます"))
                    targetPairs.add(Pair("${stemK}ました", "${stemV}ました"))
                    targetPairs.add(Pair("${stemK}ません", "${stemV}ません"))
                    targetPairs.add(Pair("${stemK}て", "${stemV}て"))
                    targetPairs.add(Pair("${stemK}た", "${stemV}た"))
                    targetPairs.add(Pair("${stemK}ない", "${stemV}ない"))
                    targetPairs.add(Pair("${stemK}ましょう", "${stemV}ましょう"))
                }

                for ((tk, tv) in targetPairs) {
                    if (tk.length >= 2 && tv.isNotBlank() && sentenceKana.contains(tk)) {
                        var searchIdx = 0
                        while (searchIdx < sentenceKana.length) {
                            val found = sentenceKana.indexOf(tk, searchIdx)
                            if (found == -1) break
                            val end = found + tk.length
                            if (!overlaps(found, end)) {
                                accepted.add(Span(found, end, tv))
                            }
                            searchIdx = end
                        }
                    }
                }
            }

            // 2. Greedy longest match from sorted vocabulary
            for ((k, v) in sortedVocab) {
                if (!sentenceKana.contains(k)) continue
                var searchIdx = 0
                while (searchIdx < sentenceKana.length) {
                    val found = sentenceKana.indexOf(k, searchIdx)
                    if (found == -1) break
                    val end = found + k.length
                    if (!overlaps(found, end)) {
                        accepted.add(Span(found, end, v))
                    }
                    searchIdx = found + 1
                }
            }

            accepted.sortBy { it.start }

            val sb = StringBuilder()
            var curr = 0
            for (span in accepted) {
                if (span.start > curr) {
                    sb.append(sentenceKana.substring(curr, span.start))
                }
                sb.append(span.replacement)
                curr = span.end
            }
            if (curr < sentenceKana.length) {
                sb.append(sentenceKana.substring(curr))
            }
            return sb.toString()
        }

        // Process card_contexts.json
        val contextCandidates = listOf(
            java.io.File(rootDir, "src/main/assets/card_contexts.json"),
            java.io.File(rootDir, "app/src/main/assets/card_contexts.json"),
            java.io.File("../app/src/main/assets/card_contexts.json"),
            java.io.File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets/card_contexts.json"),
        )
        val contextFile = contextCandidates.firstOrNull { it.exists() } ?: error("card_contexts.json not found")
        val lines = contextFile.readLines(Charsets.UTF_8)

        var totalExamplesEnriched = 0
        var totalCardsEnriched = 0

        val newLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("\"") || !trimmed.contains("\": {")) {
                newLines.add(line)
                continue
            }

            val keyEnd = trimmed.indexOf("\": {")
            if (keyEnd == -1) {
                newLines.add(line)
                continue
            }
            val key = trimmed.substring(1, keyEnd)

            val cardId = extractJsonString(trimmed, "cardId")
            val kana = extractJsonString(trimmed, "kana")
            val romaji = extractJsonString(trimmed, "romaji")
            val english = extractJsonString(trimmed, "english")
            val usageNote = extractJsonString(trimmed, "usageNote")
            val existingKanji = extractJsonString(trimmed, "kanji").takeIf { it.isNotBlank() }

            val cardKanji = curatedCardKanji[cardId]
                ?: curatedCardKanji[kana]
                ?: cardMap[cardId]?.kanji
                ?: cardKanaMap[kana]?.kanji
                ?: existingKanji

            if (cardKanji != null) totalCardsEnriched++

            // Parse examples
            val exMarker = "\"examples\":["
            val exStart = trimmed.indexOf(exMarker)
            val examples = mutableListOf<CardExample>()
            if (exStart != -1) {
                val exContent = trimmed.substring(exStart + exMarker.length).substringBeforeLast("]")
                var searchFrom = 0
                while (searchFrom < exContent.length) {
                    val objStart = exContent.indexOf("{", searchFrom)
                    if (objStart == -1) break
                    val objEnd = exContent.indexOf("}", objStart)
                    if (objEnd == -1) break
                    val exStr = exContent.substring(objStart, objEnd + 1)

                    val exKana = extractJsonString(exStr, "kana")
                    val exRomaji = extractJsonString(exStr, "romaji")
                    val exEnglish = extractJsonString(exStr, "english")
                    val existingExKanji = extractJsonString(exStr, "kanji").takeIf { it.isNotBlank() }

                    val enrichedKanji = enrichSentence(exKana, kana, cardKanji)
                    val finalExKanji = when {
                        KanaConverter.containsKanji(enrichedKanji) -> enrichedKanji
                        existingExKanji != null && KanaConverter.containsKanji(existingExKanji) -> existingExKanji
                        else -> null
                    }
                    if (finalExKanji != null) totalExamplesEnriched++

                    examples.add(CardExample(exKana, exRomaji, exEnglish, finalExKanji))
                    searchFrom = objEnd + 1
                }
            }

            val sb = StringBuilder()
            sb.append("\"").append(key).append("\": {")
            sb.append("\"cardId\":\"").append(cardId).append("\",")
            sb.append("\"kana\":\"").append(escapeJson(kana)).append("\",")
            if (cardKanji != null) {
                sb.append("\"kanji\":\"").append(escapeJson(cardKanji)).append("\",")
            }
            sb.append("\"romaji\":\"").append(escapeJson(romaji)).append("\",")
            sb.append("\"english\":\"").append(escapeJson(english)).append("\",")
            sb.append("\"usageNote\":\"").append(escapeJson(usageNote)).append("\",")
            sb.append("\"examples\":[")
            for (i in examples.indices) {
                val ex = examples[i]
                sb.append("{")
                sb.append("\"kana\":\"").append(escapeJson(ex.kana)).append("\",")
                if (ex.kanji != null) {
                    sb.append("\"kanji\":\"").append(escapeJson(ex.kanji)).append("\",")
                }
                sb.append("\"romaji\":\"").append(escapeJson(ex.romaji)).append("\",")
                sb.append("\"english\":\"").append(escapeJson(ex.english)).append("\"")
                sb.append("}")
                if (i < examples.size - 1) sb.append(",")
            }
            sb.append("]},")

            newLines.add(sb.toString())
        }

        // Fix trailing comma on last line if needed
        if (newLines.isNotEmpty() && newLines.last().endsWith("},")) {
            newLines[newLines.size - 1] = newLines.last().removeSuffix(",")
        }

        val enrichedContent = newLines.joinToString("\n")
        contextFile.writeText(enrichedContent, Charsets.UTF_8)

        // Also write to app/src/main/assets if different
        val appAssetFile = java.io.File(rootDir, "app/src/main/assets/card_contexts.json")
        if (appAssetFile.exists() && appAssetFile.canonicalPath != contextFile.canonicalPath) {
            appAssetFile.writeText(enrichedContent, Charsets.UTF_8)
        }

        println("Successfully wrote enriched card_contexts.json!")
        println("Total card entries with kanji: $totalCardsEnriched")
        println("Total sentence examples enriched with kanji: $totalExamplesEnriched")
    }
}





