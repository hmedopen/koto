package com.koto.app.feature.kana.data

import android.content.Context
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.kana.model.PracticeWord

/**
 * Curated repository of Japanese vocabulary words specifically
 * designed for Kana reading, transliteration, and building drills.
 *
 * Supports streaming 5,000+ words from assets/kana_practice_words.csv with
 * zero-lag in-memory caching and 200 hardcoded fallback words for headless tests.
 */
object PracticeWordPool {

    @Volatile
    private var cachedWords: List<PracticeWord>? = null

    /**
     * Returns all practice words (5,000 words if Context is provided).
     * Caches in-memory after initial < 10ms stream.
     */
    fun getWords(context: Context? = null): List<PracticeWord> {
        cachedWords?.let { return it }
        if (context != null) {
            try {
                val loaded = loadFromAssets(context)
                if (loaded.isNotEmpty()) {
                    cachedWords = loaded
                    return loaded
                }
            } catch (_: Exception) {}
        }
        return fallbackWords
    }

    /**
     * Immediate accessor returning cached words or in-memory fallback.
     */
    val words: List<PracticeWord>
        get() = cachedWords ?: fallbackWords

    private fun loadFromAssets(context: Context): List<PracticeWord> {
        val list = mutableListOf<PracticeWord>()
        context.assets.open("kana_practice_words.csv").bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.startsWith("id,kana,") || line.isBlank()) continue
                val parts = parseCsvLine(line)
                if (parts.size >= 6) {
                    val script = if (parts[4].equals("Katakana", ignoreCase = true)) KanaScript.Katakana else KanaScript.Hiragana
                    list.add(
                        PracticeWord(
                            id = parts[0],
                            kana = parts[1],
                            romaji = parts[2],
                            english = parts[3],
                            script = script,
                            category = parts[5],
                        )
                    )
                }
            }
        }
        return list
    }

    private fun parseCsvLine(line: String): List<String> {
        val list = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        list.add(sb.toString())
        return list
    }

    val fallbackWords: List<PracticeWord> = listOf(
        // ==========================================
        // 1. BASIC HIRAGANA (GOJŪON 2-SYLLABLE WORDS)
        // High hit-rate for learners with limited Kana selected
        // ==========================================
        PracticeWord("h1", "あい", "ai", "love", KanaScript.Hiragana, "Basic"),
        PracticeWord("h2", "あお", "ao", "blue", KanaScript.Hiragana, "Basic"),
        PracticeWord("h3", "あか", "aka", "red", KanaScript.Hiragana, "Basic"),
        PracticeWord("h4", "あき", "aki", "autumn", KanaScript.Hiragana, "Basic"),
        PracticeWord("h5", "あさ", "asa", "morning", KanaScript.Hiragana, "Basic"),
        PracticeWord("h6", "あし", "ashi", "foot", KanaScript.Hiragana, "Basic"),
        PracticeWord("h7", "あめ", "ame", "rain", KanaScript.Hiragana, "Basic"),
        PracticeWord("h8", "いえ", "ie", "house", KanaScript.Hiragana, "Basic"),
        PracticeWord("h9", "いけ", "ike", "pond", KanaScript.Hiragana, "Basic"),
        PracticeWord("h10", "いし", "ishi", "stone", KanaScript.Hiragana, "Basic"),
        PracticeWord("h11", "いぬ", "inu", "dog", KanaScript.Hiragana, "Basic"),
        PracticeWord("h12", "うた", "uta", "song", KanaScript.Hiragana, "Basic"),
        PracticeWord("h13", "うみ", "umi", "sea", KanaScript.Hiragana, "Basic"),
        PracticeWord("h14", "えき", "eki", "station", KanaScript.Hiragana, "Basic"),
        PracticeWord("h15", "かお", "kao", "face", KanaScript.Hiragana, "Basic"),
        PracticeWord("h16", "かさ", "kasa", "umbrella", KanaScript.Hiragana, "Basic"),
        PracticeWord("h17", "かわ", "kawa", "river", KanaScript.Hiragana, "Basic"),
        PracticeWord("h18", "きく", "kiku", "to listen", KanaScript.Hiragana, "Basic"),
        PracticeWord("h19", "くち", "kuchi", "mouth", KanaScript.Hiragana, "Basic"),
        PracticeWord("h20", "くも", "kumo", "cloud", KanaScript.Hiragana, "Basic"),
        PracticeWord("h21", "くり", "kuri", "chestnut", KanaScript.Hiragana, "Basic"),
        PracticeWord("h22", "くる", "kuru", "to come", KanaScript.Hiragana, "Basic"),
        PracticeWord("h23", "こえ", "koe", "voice", KanaScript.Hiragana, "Basic"),
        PracticeWord("h24", "ここ", "koko", "here", KanaScript.Hiragana, "Basic"),
        PracticeWord("h25", "さけ", "sake", "salmon", KanaScript.Hiragana, "Basic"),
        PracticeWord("h26", "さる", "saru", "monkey", KanaScript.Hiragana, "Basic"),
        PracticeWord("h27", "しか", "shika", "deer", KanaScript.Hiragana, "Basic"),
        PracticeWord("h28", "しま", "shima", "island", KanaScript.Hiragana, "Basic"),
        PracticeWord("h29", "しろ", "shiro", "white", KanaScript.Hiragana, "Basic"),
        PracticeWord("h30", "そら", "sora", "sky", KanaScript.Hiragana, "Basic"),
        PracticeWord("h31", "たこ", "tako", "octopus", KanaScript.Hiragana, "Basic"),
        PracticeWord("h32", "つき", "tsuki", "moon", KanaScript.Hiragana, "Basic"),
        PracticeWord("h33", "つち", "tsuchi", "soil", KanaScript.Hiragana, "Basic"),
        PracticeWord("h34", "てら", "tera", "temple", KanaScript.Hiragana, "Basic"),
        PracticeWord("h35", "とり", "tori", "bird", KanaScript.Hiragana, "Basic"),
        PracticeWord("h36", "なつ", "natsu", "summer", KanaScript.Hiragana, "Basic"),
        PracticeWord("h37", "にじ", "niji", "rainbow", KanaScript.Hiragana, "Basic"),
        PracticeWord("h38", "ねこ", "neko", "cat", KanaScript.Hiragana, "Basic"),
        PracticeWord("h39", "はな", "hana", "flower", KanaScript.Hiragana, "Basic"),
        PracticeWord("h40", "はる", "haru", "spring", KanaScript.Hiragana, "Basic"),
        PracticeWord("h41", "ひと", "hito", "person", KanaScript.Hiragana, "Basic"),
        PracticeWord("h42", "ふね", "fune", "ship", KanaScript.Hiragana, "Basic"),
        PracticeWord("h43", "ふゆ", "fuyu", "winter", KanaScript.Hiragana, "Basic"),
        PracticeWord("h44", "ほし", "hoshi", "star", KanaScript.Hiragana, "Basic"),
        PracticeWord("h45", "ほん", "hon", "book", KanaScript.Hiragana, "Basic"),
        PracticeWord("h46", "まち", "machi", "town", KanaScript.Hiragana, "Basic"),
        PracticeWord("h47", "まつ", "matsu", "pine tree", KanaScript.Hiragana, "Basic"),
        PracticeWord("h48", "みず", "mizu", "water", KanaScript.Hiragana, "Basic"),
        PracticeWord("h49", "みち", "michi", "road", KanaScript.Hiragana, "Basic"),
        PracticeWord("h50", "めし", "meshi", "meal", KanaScript.Hiragana, "Basic"),
        PracticeWord("h51", "もり", "mori", "forest", KanaScript.Hiragana, "Basic"),
        PracticeWord("h52", "やま", "yama", "mountain", KanaScript.Hiragana, "Basic"),
        PracticeWord("h53", "ゆき", "yuki", "snow", KanaScript.Hiragana, "Basic"),
        PracticeWord("h54", "よる", "yoru", "night", KanaScript.Hiragana, "Basic"),
        PracticeWord("h55", "わに", "wani", "crocodile", KanaScript.Hiragana, "Basic"),

        // ==========================================
        // 2. BASIC HIRAGANA (3-4 SYLLABLES)
        // ==========================================
        PracticeWord("h56", "あたま", "atama", "head", KanaScript.Hiragana, "Basic"),
        PracticeWord("h57", "あなた", "anata", "you", KanaScript.Hiragana, "Basic"),
        PracticeWord("h58", "いのち", "inochi", "life", KanaScript.Hiragana, "Basic"),
        PracticeWord("h59", "おとこ", "otoko", "man", KanaScript.Hiragana, "Basic"),
        PracticeWord("h60", "おんな", "onna", "woman", KanaScript.Hiragana, "Basic"),
        PracticeWord("h61", "かがみ", "kagami", "mirror", KanaScript.Hiragana, "Basic"),
        PracticeWord("h62", "からだ", "karada", "body", KanaScript.Hiragana, "Basic"),
        PracticeWord("h63", "きつね", "kitsune", "fox", KanaScript.Hiragana, "Basic"),
        PracticeWord("h64", "くすり", "kusuri", "medicine", KanaScript.Hiragana, "Basic"),
        PracticeWord("h65", "くだもの", "kudamono", "fruit", KanaScript.Hiragana, "Basic"),
        PracticeWord("h66", "くるま", "kuruma", "car", KanaScript.Hiragana, "Basic"),
        PracticeWord("h67", "こころ", "kokoro", "heart", KanaScript.Hiragana, "Basic"),
        PracticeWord("h68", "さくら", "sakura", "cherry blossom", KanaScript.Hiragana, "Basic"),
        PracticeWord("h69", "すずめ", "suzume", "sparrow", KanaScript.Hiragana, "Basic"),
        PracticeWord("h70", "せかい", "sekai", "world", KanaScript.Hiragana, "Basic"),
        PracticeWord("h71", "たまご", "tamago", "egg", KanaScript.Hiragana, "Basic"),
        PracticeWord("h72", "ちから", "chikara", "power", KanaScript.Hiragana, "Basic"),
        PracticeWord("h73", "てがみ", "tegami", "letter", KanaScript.Hiragana, "Basic"),
        PracticeWord("h74", "ともだち", "tomodachi", "friend", KanaScript.Hiragana, "Basic"),
        PracticeWord("h75", "なまえ", "namae", "name", KanaScript.Hiragana, "Basic"),
        PracticeWord("h76", "ひかり", "hikari", "light", KanaScript.Hiragana, "Basic"),
        PracticeWord("h77", "みらい", "mirai", "future", KanaScript.Hiragana, "Basic"),
        PracticeWord("h78", "むかし", "mukashi", "long ago", KanaScript.Hiragana, "Basic"),
        PracticeWord("h79", "めがね", "megane", "glasses", KanaScript.Hiragana, "Basic"),
        PracticeWord("h80", "やさい", "yasai", "vegetable", KanaScript.Hiragana, "Basic"),
        PracticeWord("h81", "わたし", "watashi", "I / me", KanaScript.Hiragana, "Basic"),

        // ==========================================
        // 3. DAKUTEN & HANDAKUTEN (が, ざ, だ, ば, ぱ)
        // ==========================================
        PracticeWord("d82", "かぎ", "kagi", "key", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d83", "すし", "sushi", "sushi", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d84", "みかん", "mikan", "mandarin", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d85", "りんご", "ringo", "apple", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d86", "かばん", "kaban", "bag", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d87", "まど", "mado", "window", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d88", "えんぴつ", "enpitsu", "pencil", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d89", "しんぶん", "shinbun", "newspaper", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d90", "でんしゃ", "densha", "train", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d91", "でんわ", "denwa", "telephone", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d92", "ぎんこう", "ginkō", "bank", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d93", "びょういん", "byōin", "hospital", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d94", "ごはん", "gohan", "rice / meal", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d95", "ぼうし", "bōshi", "hat", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d96", "ぶた", "buta", "pig", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d97", "ぞう", "zō", "elephant", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d98", "かぜ", "kaze", "wind / cold", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d99", "みずぎ", "mizugi", "swimsuit", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d100", "だんご", "dango", "dumpling", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d101", "げんき", "genki", "healthy / energetic", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d102", "さんぽ", "sanpo", "walk / stroll", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d103", "きんぎょ", "kingyo", "goldfish", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d104", "はなび", "hanabi", "fireworks", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d105", "てんき", "tenki", "weather", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d106", "ちず", "chizu", "map", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d107", "かぞく", "kazoku", "family", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d108", "じかん", "jikan", "time", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d109", "みどり", "midori", "green", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d110", "ひざ", "hiza", "knee", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d111", "ひげ", "hige", "beard", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d112", "すがお", "sugao", "bare face", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d113", "つばさ", "tsubasa", "wings", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d114", "ふで", "fude", "brush", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d115", "ほね", "hone", "bone", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d116", "きっぷ", "kippu", "ticket", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d117", "しっぽ", "shippo", "tail", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d118", "すっぱ", "suppa", "sour", KanaScript.Hiragana, "Dakuten"),
        PracticeWord("d119", "らっぱ", "rappa", "trumpet", KanaScript.Hiragana, "Dakuten"),

        // ==========================================
        // 4. CONTRACTED YŌON DIGRAPHS (きょ, しゃ, ちょ, etc.)
        // ==========================================
        PracticeWord("y120", "おちゃ", "ocha", "tea", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y121", "きょう", "kyō", "today", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y122", "きょねん", "kyonen", "last year", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y123", "しゃしん", "shashin", "photo", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y124", "しゅくだい", "shukudai", "homework", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y125", "しょうゆ", "shōyu", "soy sauce", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y126", "りょこう", "ryokō", "travel", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y127", "りょうり", "ryōri", "cooking", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y128", "きゅうり", "kyūri", "cucumber", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y129", "ぎゅうにゅう", "gyūnyū", "milk", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y130", "びょうき", "byōki", "illness", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y131", "きょうしつ", "kyōshitsu", "classroom", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y132", "ちゃいろ", "chairo", "brown", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y133", "じしょ", "jisho", "dictionary", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y134", "ひゃく", "hyaku", "hundred", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y135", "きょく", "kyoku", "melody / tune", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y136", "しゅみ", "shumi", "hobby", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y137", "りょうしん", "ryōshin", "parents", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y138", "じゅぎょう", "jugyō", "lesson / class", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y139", "りようする", "riyōsuru", "to use", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y140", "てきようする", "tekiyōsuru", "to apply", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y141", "ちゅうしゃ", "chūsha", "injection", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y142", "きゅうこう", "kyūkō", "express train", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y143", "にんぎょう", "ningyō", "doll", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y144", "じどうしゃ", "jidōsha", "automobile", KanaScript.Hiragana, "Yoon"),
        PracticeWord("y145", "きょか", "kyoka", "permission", KanaScript.Hiragana, "Yoon"),

        // ==========================================
        // 5. KATAKANA BASIC & ESSENTIAL LOANWORDS
        // Covers common signs, food, technology, and daily items
        // ==========================================
        PracticeWord("k146", "パン", "pan", "bread", KanaScript.Katakana, "Katakana"),
        PracticeWord("k147", "バス", "basu", "bus", KanaScript.Katakana, "Katakana"),
        PracticeWord("k148", "ドア", "doa", "door", KanaScript.Katakana, "Katakana"),
        PracticeWord("k149", "ペン", "pen", "pen", KanaScript.Katakana, "Katakana"),
        PracticeWord("k150", "ノート", "nōto", "notebook", KanaScript.Katakana, "Katakana"),
        PracticeWord("k151", "カメラ", "kamera", "camera", KanaScript.Katakana, "Katakana"),
        PracticeWord("k152", "テレビ", "terebi", "television", KanaScript.Katakana, "Katakana"),
        PracticeWord("k153", "ラジオ", "rajio", "radio", KanaScript.Katakana, "Katakana"),
        PracticeWord("k154", "ピアノ", "piano", "piano", KanaScript.Katakana, "Katakana"),
        PracticeWord("k155", "ギター", "gitā", "guitar", KanaScript.Katakana, "Katakana"),
        PracticeWord("k156", "ナイフ", "naifu", "knife", KanaScript.Katakana, "Katakana"),
        PracticeWord("k157", "フォーク", "fōku", "fork", KanaScript.Katakana, "Katakana"),
        PracticeWord("k158", "スプーン", "supūn", "spoon", KanaScript.Katakana, "Katakana"),
        PracticeWord("k159", "コップ", "koppu", "cup / glass", KanaScript.Katakana, "Katakana"),
        PracticeWord("k160", "グラス", "gurasu", "glass", KanaScript.Katakana, "Katakana"),
        PracticeWord("k161", "ミルク", "miruku", "milk", KanaScript.Katakana, "Katakana"),
        PracticeWord("k162", "チーズ", "chīzu", "cheese", KanaScript.Katakana, "Katakana"),
        PracticeWord("k163", "トマト", "tomato", "tomato", KanaScript.Katakana, "Katakana"),
        PracticeWord("k164", "バナナ", "banana", "banana", KanaScript.Katakana, "Katakana"),
        PracticeWord("k165", "リンゴ", "ringo", "apple", KanaScript.Katakana, "Katakana"),
        PracticeWord("k166", "ケーキ", "kēki", "cake", KanaScript.Katakana, "Katakana"),
        PracticeWord("k167", "アイス", "aisu", "ice cream", KanaScript.Katakana, "Katakana"),
        PracticeWord("k168", "コーヒー", "kōhī", "coffee", KanaScript.Katakana, "Katakana"),
        PracticeWord("k169", "ホテル", "hoteru", "hotel", KanaScript.Katakana, "Katakana"),
        PracticeWord("k170", "ビル", "biru", "building", KanaScript.Katakana, "Katakana"),
        PracticeWord("k171", "タクシー", "takushī", "taxi", KanaScript.Katakana, "Katakana"),
        PracticeWord("k172", "トラック", "torakku", "truck", KanaScript.Katakana, "Katakana"),
        PracticeWord("k173", "バイク", "baiku", "motorcycle", KanaScript.Katakana, "Katakana"),
        PracticeWord("k174", "シャツ", "shatsu", "shirt", KanaScript.Katakana, "Katakana"),
        PracticeWord("k175", "スカート", "sukāto", "skirt", KanaScript.Katakana, "Katakana"),
        PracticeWord("k176", "ズボン", "zubon", "trousers", KanaScript.Katakana, "Katakana"),
        PracticeWord("k177", "ネクタイ", "nekutai", "necktie", KanaScript.Katakana, "Katakana"),
        PracticeWord("k178", "ブーツ", "būtsu", "boots", KanaScript.Katakana, "Katakana"),
        PracticeWord("k179", "プール", "pūru", "pool", KanaScript.Katakana, "Katakana"),
        PracticeWord("k180", "スキー", "sukī", "skiing", KanaScript.Katakana, "Katakana"),
        PracticeWord("k181", "テニス", "tenisu", "tennis", KanaScript.Katakana, "Katakana"),
        PracticeWord("k182", "サッカー", "sakkā", "soccer", KanaScript.Katakana, "Katakana"),
        PracticeWord("k183", "ベッド", "beddo", "bed", KanaScript.Katakana, "Katakana"),
        PracticeWord("k184", "トイレ", "toire", "restroom", KanaScript.Katakana, "Katakana"),
        PracticeWord("k185", "シャワー", "shawā", "shower", KanaScript.Katakana, "Katakana"),
        PracticeWord("k186", "ポスト", "posuto", "mailbox", KanaScript.Katakana, "Katakana"),
        PracticeWord("k187", "テスト", "tesuto", "test / exam", KanaScript.Katakana, "Katakana"),
        PracticeWord("k188", "ゲーム", "gēmu", "game", KanaScript.Katakana, "Katakana"),
        PracticeWord("k189", "スマホ", "sumaho", "smartphone", KanaScript.Katakana, "Katakana"),
        PracticeWord("k190", "パソコン", "pasokon", "PC / computer", KanaScript.Katakana, "Katakana"),
        PracticeWord("k191", "ネット", "netto", "internet", KanaScript.Katakana, "Katakana"),
        PracticeWord("k192", "アニメ", "anime", "anime", KanaScript.Katakana, "Katakana"),
        PracticeWord("k193", "マンガ", "manga", "manga", KanaScript.Katakana, "Katakana"),
        PracticeWord("k194", "コンビニ", "konbini", "convenience store", KanaScript.Katakana, "Katakana"),
        PracticeWord("k195", "スーパー", "sūpā", "supermarket", KanaScript.Katakana, "Katakana"),
        PracticeWord("k196", "レストラン", "resutoran", "restaurant", KanaScript.Katakana, "Katakana"),
        PracticeWord("k197", "メニュー", "menyū", "menu", KanaScript.Katakana, "Katakana"),
        PracticeWord("k198", "サラダ", "sarada", "salad", KanaScript.Katakana, "Katakana"),
        PracticeWord("k199", "スープ", "sūpu", "soup", KanaScript.Katakana, "Katakana"),
        PracticeWord("k200", "ハイキング", "haikingu", "hiking", KanaScript.Katakana, "Katakana")
    )
}
