package com.koto.app.feature.kana.data

import com.koto.app.feature.kana.model.KanaCharacter
import com.koto.app.feature.kana.model.KanaGroup
import com.koto.app.feature.kana.model.KanaRow
import com.koto.app.feature.kana.model.KanaScript
import com.koto.app.feature.kana.model.StrokeStep

object KanaRepository {

    val columnVowels = listOf("-A", "-I", "-U", "-E", "-O")

    val groups: List<KanaGroup> by lazy {
        listOf(
            KanaGroup("basic", "Basic", basicRows),
            KanaGroup("dakuon", "Dakuon", dakuonRows),
            KanaGroup("handakuon", "Handakuon", handakuonRows),
            KanaGroup("yoon", "Yoon", yoonRows),
        )
    }

    val rows: List<KanaRow> by lazy {
        basicRows + dakuonRows + handakuonRows + yoonRows
    }

    val allCharacters: List<KanaCharacter> by lazy {
        rows.flatMap { row -> row.columns.filterNotNull() }
    }

    val basicRows: List<KanaRow> by lazy {
        listOf(
            // 1. A-Row
            KanaRow(
                id = "row_a",
                label = "A-",
                displayName = "A-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_a",
                        hiragana = "あ",
                        katakana = "ア",
                        romaji = "A",
                        rowLabel = "A-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "→", "Horizontal stroke across top"),
                            StrokeStep(2, "↓", "Curved vertical stroke down through center"),
                            StrokeStep(3, "⤸", "Broad loop crossing right then curving around"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "㇇", "Horizontal stroke turning down-left hook"),
                            StrokeStep(2, "ノ", "Leftward curved slant stroke"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_i",
                        hiragana = "い",
                        katakana = "イ",
                        romaji = "I",
                        rowLabel = "A-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "亅", "Left curved vertical stroke with upward hook"),
                            StrokeStep(2, "丨", "Shorter gentle right stroke downward"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "ノ", "Left diagonal slash downward"),
                            StrokeStep(2, "丨", "Straight vertical stroke down from mid-slash"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_u",
                        hiragana = "う",
                        katakana = "ウ",
                        romaji = "U",
                        rowLabel = "A-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丶", "Short diagonal dot at top"),
                            StrokeStep(2, "⤹", "Sweeping curved loop down to the left"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丨", "Short top center vertical stroke"),
                            StrokeStep(2, "丨", "Short left vertical stroke"),
                            StrokeStep(3, "㇇", "Horizontal across turning down-left hook"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_e",
                        hiragana = "え",
                        katakana = "E",
                        romaji = "E",
                        rowLabel = "A-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丶", "Short diagonal dot at top"),
                            StrokeStep(2, "乙", "Continuous zigzag stroke with bottom right wave"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal stroke"),
                            StrokeStep(2, "丨", "Central vertical stroke down"),
                            StrokeStep(3, "一", "Longer bottom horizontal base stroke"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_o",
                        hiragana = "お",
                        katakana = "オ",
                        romaji = "O",
                        rowLabel = "A-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Short horizontal stroke"),
                            StrokeStep(2, "⤹", "Vertical crossing down into wide rounded loop"),
                            StrokeStep(3, "丶", "Accent dot on upper right"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "亅", "Vertical line with sharp left hook"),
                            StrokeStep(3, "ノ", "Leftward diagonal sweep"),
                        ),
                    ),
                ),
            ),

            // 2. K-Row
            KanaRow(
                id = "row_k",
                label = "K-",
                displayName = "K-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_ka",
                        hiragana = "か",
                        katakana = "カ",
                        romaji = "KA",
                        rowLabel = "K-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "㇇", "Left hook down with sharp inward turn"),
                            StrokeStep(2, "丿", "Crossing diagonal slash downward"),
                            StrokeStep(3, "丶", "Right accent dot"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "㇇", "Top bar turning down into sharp hook"),
                            StrokeStep(2, "丿", "Leftward sweeping diagonal slash"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ki",
                        hiragana = "き",
                        katakana = "キ",
                        romaji = "KI",
                        rowLabel = "K-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "一", "Second parallel horizontal bar"),
                            StrokeStep(3, "⤹", "Crossing diagonal down curving into open lower loop"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "一", "Second parallel horizontal bar"),
                            StrokeStep(3, "丿", "Crossing diagonal slash down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ku",
                        hiragana = "く",
                        katakana = "ク",
                        romaji = "KU",
                        rowLabel = "K-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "く", "Single angled stroke down-left then down-right"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Top-left diagonal sweep downward"),
                            StrokeStep(2, "㇇", "Horizontal bar turning into long down-left sweep"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ke",
                        hiragana = "け",
                        katakana = "ケ",
                        romaji = "KE",
                        rowLabel = "K-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Left vertical stroke with hook"),
                            StrokeStep(2, "一", "Horizontal crossbar on right"),
                            StrokeStep(3, "丿", "Right vertical stroke curving down-left"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Top-left diagonal sweep"),
                            StrokeStep(2, "一", "Horizontal stroke across"),
                            StrokeStep(3, "丿", "Long curving diagonal stroke down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ko",
                        hiragana = "こ",
                        katakana = "コ",
                        romaji = "KO",
                        rowLabel = "K-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Top horizontal stroke with slight tick"),
                            StrokeStep(2, "一", "Bottom curved horizontal base stroke"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal turning 90 degrees down"),
                            StrokeStep(2, "一", "Bottom horizontal line connecting corner"),
                        ),
                    ),
                ),
            ),

            // 3. S-Row
            KanaRow(
                id = "row_s",
                label = "S-",
                displayName = "S-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_sa",
                        hiragana = "さ",
                        katakana = "サ",
                        romaji = "SA",
                        rowLabel = "S-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Horizontal top bar"),
                            StrokeStep(2, "丿", "Crossing diagonal turning downward"),
                            StrokeStep(3, "◡", "Bottom floating curve"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Horizontal top bar"),
                            StrokeStep(2, "丨", "Left vertical stroke crossing down"),
                            StrokeStep(3, "丨", "Right vertical stroke curving down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_shi",
                        hiragana = "し",
                        katakana = "シ",
                        romaji = "SHI",
                        rowLabel = "S-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "し", "Single vertical stroke turning into smooth upward hook"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丶", "Top-left small dot"),
                            StrokeStep(2, "丶", "Middle-left small dot"),
                            StrokeStep(3, "ノ", "Bottom-left upward rising sweep"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_su",
                        hiragana = "す",
                        katakana = "ス",
                        romaji = "SU",
                        rowLabel = "S-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Horizontal top bar"),
                            StrokeStep(2, "⤹", "Vertical line forming center loop then descending"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "㇇", "Top bar turning into diagonal slant down-left"),
                            StrokeStep(2, "丶", "Downward-right diagonal stroke"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_se",
                        hiragana = "せ",
                        katakana = "セ",
                        romaji = "SE",
                        rowLabel = "S-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Horizontal bar"),
                            StrokeStep(2, "丨", "Right vertical stroke with hook"),
                            StrokeStep(3, "し", "Left vertical stroke curving right at bottom"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal bar turning down with hook"),
                            StrokeStep(2, "し", "Left vertical crossing and curving right"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_so",
                        hiragana = "そ",
                        katakana = "ソ",
                        romaji = "SO",
                        rowLabel = "S-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "そ", "Single continuous zigzag into smooth bottom curve"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丶", "Upper-left diagonal dot"),
                            StrokeStep(2, "丿", "Long downward sweeping diagonal stroke"),
                        ),
                    ),
                ),
            ),

            // 4. T-Row
            KanaRow(
                id = "row_t",
                label = "T-",
                displayName = "T-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_ta",
                        hiragana = "た",
                        katakana = "タ",
                        romaji = "TA",
                        rowLabel = "T-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Horizontal top bar"),
                            StrokeStep(2, "丿", "Short downward diagonal slant"),
                            StrokeStep(3, "こ", "Upper right short dash"),
                            StrokeStep(4, "一", "Lower right short dash"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Top-left diagonal slash"),
                            StrokeStep(2, "㇇", "Horizontal turning into long down-left sweep"),
                            StrokeStep(3, "丶", "Central diagonal stroke"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_chi",
                        hiragana = "ち",
                        katakana = "チ",
                        romaji = "CHI",
                        rowLabel = "T-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Horizontal top bar"),
                            StrokeStep(2, "⤹", "Crossing vertical descending into round loop"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Top-left gentle diagonal stroke"),
                            StrokeStep(2, "一", "Middle horizontal bar"),
                            StrokeStep(3, "丿", "Crossing vertical stroke curving down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_tsu",
                        hiragana = "つ",
                        katakana = "ツ",
                        romaji = "TSU",
                        rowLabel = "T-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "つ", "Single smooth sweeping curve starting horizontal"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丶", "First diagonal dot on left"),
                            StrokeStep(2, "丶", "Second diagonal dot in middle"),
                            StrokeStep(3, "丿", "Long downward sweeping stroke from top-right"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_te",
                        hiragana = "て",
                        katakana = "テ",
                        romaji = "TE",
                        rowLabel = "T-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "て", "Single stroke across horizontal into wide open curve"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top short horizontal bar"),
                            StrokeStep(2, "一", "Middle longer horizontal bar"),
                            StrokeStep(3, "丿", "Center vertical curving down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_to",
                        hiragana = "と",
                        katakana = "ト",
                        romaji = "TO",
                        rowLabel = "T-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Short downward vertical stem"),
                            StrokeStep(2, "C", "Rightward curved arc connecting to stem"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丨", "Straight vertical stem downward"),
                            StrokeStep(2, "丶", "Right-angled diagonal branch"),
                        ),
                    ),
                ),
            ),

            // 5. N-Row
            KanaRow(
                id = "row_n",
                label = "N-",
                displayName = "N-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_na",
                        hiragana = "な",
                        katakana = "ナ",
                        romaji = "NA",
                        rowLabel = "N-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Short horizontal bar"),
                            StrokeStep(2, "丿", "Crossing diagonal slash"),
                            StrokeStep(3, "丶", "Right small dot"),
                            StrokeStep(4, "⤹", "Lower vertical loop stroke"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Horizontal top bar"),
                            StrokeStep(2, "丿", "Crossing vertical curving down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ni",
                        hiragana = "に",
                        katakana = "ニ",
                        romaji = "NI",
                        rowLabel = "N-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Left vertical stem with hook"),
                            StrokeStep(2, "一", "Top right horizontal bar"),
                            StrokeStep(3, "一", "Bottom right horizontal bar"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "一", "Bottom longer horizontal bar"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_nu",
                        hiragana = "ぬ",
                        katakana = "ヌ",
                        romaji = "NU",
                        rowLabel = "N-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丿", "Left diagonal slash downward"),
                            StrokeStep(2, "⤹", "Curving line looping through center with end knot"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "㇇", "Horizontal bar turning into diagonal slant"),
                            StrokeStep(2, "丶", "Crossing diagonal stroke downward-right"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ne",
                        hiragana = "ね",
                        katakana = "ネ",
                        romaji = "NE",
                        rowLabel = "N-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Straight vertical left stem"),
                            StrokeStep(2, "⤹", "Z-shape crossing over into looped knot tail"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丶", "Top center dot"),
                            StrokeStep(2, "㇇", "Left vertical turning into hook"),
                            StrokeStep(3, "丨", "Central vertical stem"),
                            StrokeStep(4, "丶", "Right diagonal stroke"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_no",
                        hiragana = "の",
                        katakana = "ノ",
                        romaji = "NO",
                        rowLabel = "N-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "の", "Single continuous curved spiral stroke"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Single downward sweeping diagonal slash"),
                        ),
                    ),
                ),
            ),

            // 6. H-Row
            KanaRow(
                id = "row_h",
                label = "H-",
                displayName = "H-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_ha",
                        hiragana = "は",
                        katakana = "ハ",
                        romaji = "HA",
                        rowLabel = "H-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Left vertical stem with hook"),
                            StrokeStep(2, "一", "Right horizontal crossbar"),
                            StrokeStep(3, "⤹", "Right vertical crossing into bottom loop"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Left diagonal downward sweep"),
                            StrokeStep(2, "丶", "Right diagonal downward stroke"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_hi",
                        hiragana = "ひ",
                        katakana = "ヒ",
                        romaji = "HI",
                        rowLabel = "H-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "ひ", "Single wide smile-like U curve starting with left bar"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal bar turning into hook"),
                            StrokeStep(2, "乚", "Crossing vertical line curving horizontal"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_fu",
                        hiragana = "ふ",
                        katakana = "フ",
                        romaji = "FU",
                        rowLabel = "H-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丶", "Top center dot"),
                            StrokeStep(2, "⤹", "Central downward sweeping arc"),
                            StrokeStep(3, "丿", "Left flank dot/slash"),
                            StrokeStep(4, "丶", "Right flank dot"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "㇇", "Horizontal bar turning into gentle curve down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_he",
                        hiragana = "へ",
                        katakana = "ヘ",
                        romaji = "HE",
                        rowLabel = "H-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "へ", "Single peaked roof stroke: diagonal up then long slope down"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "ヘ", "Single peaked stroke identical to Hiragana"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ho",
                        hiragana = "ほ",
                        katakana = "ホ",
                        romaji = "HO",
                        rowLabel = "H-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Left vertical stem with hook"),
                            StrokeStep(2, "一", "Top horizontal bar"),
                            StrokeStep(3, "一", "Parallel lower horizontal bar"),
                            StrokeStep(4, "⤹", "Right vertical crossing bar into bottom loop"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Horizontal top bar"),
                            StrokeStep(2, "亅", "Crossing vertical with upward hook"),
                            StrokeStep(3, "丿", "Left diagonal branch"),
                            StrokeStep(4, "丶", "Right diagonal branch"),
                        ),
                    ),
                ),
            ),

            // 7. M-Row
            KanaRow(
                id = "row_m",
                label = "M-",
                displayName = "M-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_ma",
                        hiragana = "ま",
                        katakana = "マ",
                        romaji = "MA",
                        rowLabel = "M-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "一", "Middle horizontal bar"),
                            StrokeStep(3, "⤹", "Vertical crossing bars into bottom closed loop"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "㇇", "Horizontal bar turning into diagonal down-left"),
                            StrokeStep(2, "丶", "Short diagonal dot on lower right"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_mi",
                        hiragana = "み",
                        katakana = "ミ",
                        romaji = "MI",
                        rowLabel = "M-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "⤹", "Diagonal crossing into loop and extending right"),
                            StrokeStep(2, "丿", "Crossing vertical slash downward"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Top diagonal parallel slash"),
                            StrokeStep(2, "丿", "Middle diagonal parallel slash"),
                            StrokeStep(3, "丿", "Bottom diagonal parallel slash"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_mu",
                        hiragana = "む",
                        katakana = "ム",
                        romaji = "MU",
                        rowLabel = "M-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Horizontal bar"),
                            StrokeStep(2, "⤹", "Vertical line forming loop and rising right"),
                            StrokeStep(3, "丶", "Accent dot on upper right"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "厶", "Diagonal down-left turning into bottom horizontal"),
                            StrokeStep(2, "丶", "Short diagonal dot"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_me",
                        hiragana = "め",
                        katakana = "メ",
                        romaji = "ME",
                        rowLabel = "M-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丿", "Left diagonal slash downward"),
                            StrokeStep(2, "⤹", "Right curving stroke looping across"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Top-right to bottom-left diagonal slash"),
                            StrokeStep(2, "丶", "Crossing diagonal slash downward-right"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_mo",
                        hiragana = "も",
                        katakana = "モ",
                        romaji = "MO",
                        rowLabel = "M-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "し", "Central curved vertical hook"),
                            StrokeStep(2, "一", "Top horizontal crossbar"),
                            StrokeStep(3, "一", "Lower horizontal crossbar"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "一", "Middle horizontal bar"),
                            StrokeStep(3, "乚", "Vertical stem curving into bottom horizontal"),
                        ),
                    ),
                ),
            ),

            // 8. Y-Row (gaps at col 1 [-I] and col 3 [-E])
            KanaRow(
                id = "row_y",
                label = "Y-",
                displayName = "Y-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_ya",
                        hiragana = "や",
                        katakana = "ヤ",
                        romaji = "YA",
                        rowLabel = "Y-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "⤹", "Left curved hook stroke"),
                            StrokeStep(2, "丶", "Small upper tick"),
                            StrokeStep(3, "丿", "Crossing long diagonal stroke"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "㇇", "Top bar turning down into diagonal hook"),
                            StrokeStep(2, "丿", "Crossing diagonal slash"),
                        ),
                    ),
                    null, // Gap at -I
                    KanaCharacter(
                        id = "kana_yu",
                        hiragana = "ゆ",
                        katakana = "ユ",
                        romaji = "YU",
                        rowLabel = "Y-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "⤹", "Vertical line curving into wide loop and horizontal"),
                            StrokeStep(2, "丨", "Vertical line crossing down through center"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "コ", "Top bar turning 90 degrees down into bottom bar"),
                            StrokeStep(2, "一", "Bottom connecting horizontal bar"),
                        ),
                    ),
                    null, // Gap at -E
                    KanaCharacter(
                        id = "kana_yo",
                        hiragana = "よ",
                        katakana = "ヨ",
                        romaji = "YO",
                        rowLabel = "Y-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Short horizontal bar"),
                            StrokeStep(2, "⤹", "Vertical stem looping left then curving right"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "コ", "Top bar turning 90 degrees down"),
                            StrokeStep(2, "一", "Middle horizontal bar"),
                            StrokeStep(3, "一", "Bottom horizontal bar"),
                        ),
                    ),
                ),
            ),

            // 9. R-Row
            KanaRow(
                id = "row_r",
                label = "R-",
                displayName = "R-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_ra",
                        hiragana = "ら",
                        katakana = "ラ",
                        romaji = "RA",
                        rowLabel = "R-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丶", "Top short dash"),
                            StrokeStep(2, "⤹", "Vertical stem into sweeping right curve"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top short horizontal bar"),
                            StrokeStep(2, "㇇", "Middle horizontal turning into down-left curve"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ri",
                        hiragana = "り",
                        katakana = "リ",
                        romaji = "RI",
                        rowLabel = "R-",
                        colIndex = 1,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Left shorter vertical line with hook"),
                            StrokeStep(2, "丿", "Right longer vertical curving down-left"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丨", "Left vertical stem"),
                            StrokeStep(2, "丿", "Right vertical curving smoothly down-left"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ru",
                        hiragana = "る",
                        katakana = "ル",
                        romaji = "RU",
                        rowLabel = "R-",
                        colIndex = 2,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "る", "Single continuous stroke forming 3-like shape with bottom closed circle"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丿", "Left downward diagonal stroke"),
                            StrokeStep(2, "乚", "Right vertical stroke curving into upward hook"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_re",
                        hiragana = "れ",
                        katakana = "レ",
                        romaji = "RE",
                        rowLabel = "R-",
                        colIndex = 3,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Straight vertical left stem"),
                            StrokeStep(2, "⤹", "Z-shape crossing over into upward curving wave"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "レ", "Single stroke down turning into sharp upward slant"),
                        ),
                    ),
                    KanaCharacter(
                        id = "kana_ro",
                        hiragana = "ろ",
                        katakana = "ロ",
                        romaji = "RO",
                        rowLabel = "R-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "ろ", "Single continuous stroke similar to 'る' without loop"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丨", "Left vertical stroke"),
                            StrokeStep(2, "㇇", "Top horizontal turning 90 degrees down"),
                            StrokeStep(3, "一", "Bottom closing horizontal line"),
                        ),
                    ),
                ),
            ),

            // 10. W-Row (gaps at col 1 [-I], col 2 [-U], col 3 [-E])
            KanaRow(
                id = "row_w",
                label = "W-",
                displayName = "W-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_wa",
                        hiragana = "わ",
                        katakana = "ワ",
                        romaji = "WA",
                        rowLabel = "W-",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "丨", "Left straight vertical stem"),
                            StrokeStep(2, "⤹", "Z-shape turning into open round arc"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丨", "Left short vertical stroke"),
                            StrokeStep(2, "㇇", "Horizontal across turning into sweeping down-left curve"),
                        ),
                    ),
                    null, // Gap at -I
                    null, // Gap at -U
                    null, // Gap at -E
                    KanaCharacter(
                        id = "kana_wo",
                        hiragana = "を",
                        katakana = "ヲ",
                        romaji = "WO",
                        rowLabel = "W-",
                        colIndex = 4,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "⤹", "Crossing vertical turning into hook"),
                            StrokeStep(3, "C", "Crossing open C-curve across bottom"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "一", "Top horizontal bar"),
                            StrokeStep(2, "一", "Middle horizontal bar"),
                            StrokeStep(3, "丿", "Crossing diagonal slash curving down-left"),
                        ),
                    ),
                ),
            ),

            // 11. N-Row (only col 0 [-A])
            KanaRow(
                id = "row_single_n",
                label = "N",
                displayName = "N-Row",
                columns = listOf(
                    KanaCharacter(
                        id = "kana_n",
                        hiragana = "ん",
                        katakana = "ン",
                        romaji = "N",
                        rowLabel = "N",
                        colIndex = 0,
                        strokeStepsHira = listOf(
                            StrokeStep(1, "ん", "Single downward stroke turning into sweeping upward wave"),
                        ),
                        strokeStepsKata = listOf(
                            StrokeStep(1, "丶", "Top-left short diagonal dot"),
                            StrokeStep(2, "ノ", "Rising sweeping diagonal slash from bottom-left to top-right"),
                        ),
                    ),
                    null,
                    null,
                    null,
                    null,
                ),
            ),
        )
    }

    val dakuonRows: List<KanaRow> by lazy {
        listOf(
            // G-Row
            KanaRow(
                id = "row_g",
                label = "G-",
                displayName = "G-Row",
                columns = listOf(
                    KanaCharacter("kana_ga", "が", "ガ", "GA", "G-", 0, listOf(StrokeStep(1, "→", "Horizontal stroke with curve"), StrokeStep(2, "↓", "Vertical curved stroke"), StrokeStep(3, "丶", "Right dot"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "一", "Horizontal stroke"), StrokeStep(2, "ノ", "Left curved diagonal"), StrokeStep(3, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_gi", "ぎ", "ギ", "GI", "G-", 1, listOf(StrokeStep(1, "→", "Upper horizontal stroke"), StrokeStep(2, "→", "Lower horizontal stroke"), StrokeStep(3, "⤹", "Slanted stroke into bottom loop"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "一", "Upper horizontal stroke"), StrokeStep(2, "一", "Lower horizontal stroke"), StrokeStep(3, "ノ", "Left curved slant stroke"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_gu", "ぐ", "グ", "GU", "G-", 2, listOf(StrokeStep(1, "く", "Single angular corner stroke"), StrokeStep(2, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "ノ", "Left diagonal slash"), StrokeStep(2, "一", "Horizontal turning into curved slant"), StrokeStep(3, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_ge", "げ", "ゲ", "GE", "G-", 3, listOf(StrokeStep(1, "丨", "Left vertical stroke with hook"), StrokeStep(2, "一", "Right horizontal stroke"), StrokeStep(3, "丨", "Right vertical stroke curving down"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "ノ", "Left top diagonal"), StrokeStep(2, "一", "Horizontal turning down"), StrokeStep(3, "丨", "Vertical slash"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_go", "ご", "ゴ", "GO", "G-", 4, listOf(StrokeStep(1, "一", "Top horizontal stroke"), StrokeStep(2, "⤹", "Bottom curved horizontal stroke"), StrokeStep(3, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "一", "Top horizontal stroke"), StrokeStep(2, "丨", "Left vertical down"), StrokeStep(3, "一", "Bottom horizontal base"), StrokeStep(4, "゛", "Two dakuten dots")))
                )
            ),
            // Z-Row
            KanaRow(
                id = "row_z",
                label = "Z-",
                displayName = "Z-Row",
                columns = listOf(
                    KanaCharacter("kana_za", "ざ", "ザ", "ZA", "Z-", 0, listOf(StrokeStep(1, "→", "Horizontal stroke"), StrokeStep(2, "↓", "Cross stroke down"), StrokeStep(3, "⤹", "Bottom loop stroke"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "一", "Top horizontal stroke"), StrokeStep(2, "丨", "Left vertical stroke"), StrokeStep(3, "ノ", "Right diagonal sweep"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_ji_z", "じ", "ジ", "JI", "Z-", 1, listOf(StrokeStep(1, "し", "Vertical stroke curving up right"), StrokeStep(2, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "丶", "Top-left diagonal dot"), StrokeStep(2, "丶", "Middle-left diagonal dot"), StrokeStep(3, "ノ", "Rising upward sweep"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_zu_z", "ず", "ズ", "ZU", "Z-", 2, listOf(StrokeStep(1, "一", "Horizontal stroke"), StrokeStep(2, "⤹", "Vertical drop into bottom loop"), StrokeStep(3, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "㇇", "Horizontal stroke turning down-left"), StrokeStep(2, "ノ", "Rightward diagonal slash"), StrokeStep(3, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_ze", "ぜ", "ゼ", "ZE", "Z-", 3, listOf(StrokeStep(1, "一", "Horizontal stroke"), StrokeStep(2, "丨", "Vertical stroke with hook"), StrokeStep(3, "丨", "Right vertical stroke"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "㇇", "Top horizontal turning down"), StrokeStep(2, "ノ", "Left downward curved stroke"), StrokeStep(3, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_zo", "ぞ", "ゾ", "ZO", "Z-", 4, listOf(StrokeStep(1, "乙", "Continuous zigzag stroke"), StrokeStep(2, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "丶", "Short top-left dot"), StrokeStep(2, "ノ", "Long downward sweep"), StrokeStep(3, "゛", "Two dakuten dots")))
                )
            ),
            // D-Row
            KanaRow(
                id = "row_d",
                label = "D-",
                displayName = "D-Row",
                columns = listOf(
                    KanaCharacter("kana_da", "だ", "ダ", "DA", "D-", 0, listOf(StrokeStep(1, "一", "Horizontal top stroke"), StrokeStep(2, "丨", "Vertical crossing slant"), StrokeStep(3, "こ", "Two curved right strokes"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "ノ", "Left diagonal slash"), StrokeStep(2, "㇇", "Horizontal turning down"), StrokeStep(3, "乀", "Rightward stroke"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_ji_d", "ぢ", "ヂ", "JI", "D-", 1, listOf(StrokeStep(1, "一", "Horizontal top stroke"), StrokeStep(2, "ち", "Vertical crossing into rounded bottom loop"), StrokeStep(3, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "一", "Top horizontal stroke"), StrokeStep(2, "丿", "Left slant down"), StrokeStep(3, "一", "Lower horizontal stroke"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_zu_d", "づ", "ヅ", "ZU", "D-", 2, listOf(StrokeStep(1, "つ", "Single curved horizontal and bottom arc"), StrokeStep(2, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "丶", "Top-left diagonal dot"), StrokeStep(2, "丶", "Top-right diagonal dot"), StrokeStep(3, "ノ", "Downward sweeping arc"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_de", "で", "デ", "DE", "D-", 3, listOf(StrokeStep(1, "て", "Horizontal stroke curving down right"), StrokeStep(2, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "一", "Top horizontal stroke"), StrokeStep(2, "一", "Middle horizontal stroke"), StrokeStep(3, "丨", "Vertical stroke curving right"), StrokeStep(4, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_do", "ど", "ド", "DO", "D-", 4, listOf(StrokeStep(1, "丨", "Vertical downward stroke"), StrokeStep(2, "⤹", "C-shaped curve to the right"), StrokeStep(3, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "丨", "Straight vertical stroke down"), StrokeStep(2, "丶", "Right diagonal dot stroke"), StrokeStep(3, "゛", "Two dakuten dots")))
                )
            ),
            // B-Row
            KanaRow(
                id = "row_b",
                label = "B-",
                displayName = "B-Row",
                columns = listOf(
                    KanaCharacter("kana_ba", "ば", "バ", "BA", "B-", 0, listOf(StrokeStep(1, "丨", "Left vertical stroke"), StrokeStep(2, "一", "Top horizontal cross"), StrokeStep(3, "⤹", "Vertical drop into loop"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "ノ", "Left downward slash"), StrokeStep(2, "乀", "Right downward slash"), StrokeStep(3, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_bi", "び", "ビ", "BI", "B-", 1, listOf(StrokeStep(1, "ひ", "Continuous looped curve"), StrokeStep(2, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "ノ", "Top-left curved stroke"), StrokeStep(2, "丨", "Right vertical stroke curving down"), StrokeStep(3, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_bu", "ぶ", "ブ", "BU", "B-", 2, listOf(StrokeStep(1, "丶", "Top center dot"), StrokeStep(2, "⤹", "Center loop down"), StrokeStep(3, "丶", "Left and right dots"), StrokeStep(4, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "㇇", "Horizontal stroke turning down-left"), StrokeStep(2, "ノ", "Downward sweeping stroke"), StrokeStep(3, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_be", "べ", "ベ", "BE", "B-", 3, listOf(StrokeStep(1, "へ", "Single chevron mountain wave"), StrokeStep(2, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "ヘ", "Single chevron stroke"), StrokeStep(2, "゛", "Two dakuten dots"))),
                    KanaCharacter("kana_bo", "ぼ", "ボ", "BO", "B-", 4, listOf(StrokeStep(1, "丨", "Left vertical stroke"), StrokeStep(2, "一", "Upper horizontal stroke"), StrokeStep(3, "一", "Lower horizontal stroke"), StrokeStep(4, "⤹", "Vertical down into loop"), StrokeStep(5, "゛", "Two dakuten dots")), listOf(StrokeStep(1, "木", "Central cross with side diagonal slashes"), StrokeStep(2, "゛", "Two dakuten dots")))
                )
            )
        )
    }

    val handakuonRows: List<KanaRow> by lazy {
        listOf(
            // P-Row
            KanaRow(
                id = "row_p",
                label = "P-",
                displayName = "P-Row",
                columns = listOf(
                    KanaCharacter("kana_pa", "ぱ", "パ", "PA", "P-", 0, listOf(StrokeStep(1, "丨", "Left vertical stroke"), StrokeStep(2, "一", "Top horizontal cross"), StrokeStep(3, "⤹", "Vertical drop into loop"), StrokeStep(4, "゜", "Handakuten circle")), listOf(StrokeStep(1, "ノ", "Left downward slash"), StrokeStep(2, "乀", "Right downward slash"), StrokeStep(3, "゜", "Handakuten circle"))),
                    KanaCharacter("kana_pi", "ぴ", "ピ", "PI", "P-", 1, listOf(StrokeStep(1, "ひ", "Continuous looped curve"), StrokeStep(2, "゜", "Handakuten circle")), listOf(StrokeStep(1, "ノ", "Top-left curved stroke"), StrokeStep(2, "丨", "Right vertical stroke curving down"), StrokeStep(3, "゜", "Handakuten circle"))),
                    KanaCharacter("kana_pu", "ぷ", "プ", "PU", "P-", 2, listOf(StrokeStep(1, "丶", "Top center dot"), StrokeStep(2, "⤹", "Center loop down"), StrokeStep(3, "丶", "Left and right dots"), StrokeStep(4, "゜", "Handakuten circle")), listOf(StrokeStep(1, "㇇", "Horizontal stroke turning down-left"), StrokeStep(2, "丨", "Straight vertical drop"), StrokeStep(3, "゜", "Handakuten circle"))),
                    KanaCharacter("kana_pe", "ぺ", "ペ", "PE", "P-", 3, listOf(StrokeStep(1, "へ", "Single chevron mountain wave"), StrokeStep(2, "゜", "Handakuten circle")), listOf(StrokeStep(1, "ヘ", "Single chevron stroke"), StrokeStep(2, "゜", "Handakuten circle"))),
                    KanaCharacter("kana_po", "ぽ", "ポ", "PO", "P-", 4, listOf(StrokeStep(1, "丨", "Left vertical stroke"), StrokeStep(2, "一", "Upper horizontal stroke"), StrokeStep(3, "一", "Lower horizontal stroke"), StrokeStep(4, "⤹", "Vertical down into loop"), StrokeStep(5, "゜", "Handakuten circle")), listOf(StrokeStep(1, "木", "Central cross with side diagonal slashes"), StrokeStep(2, "゜", "Handakuten circle")))
                )
            )
        )
    }

    val yoonRows: List<KanaRow> by lazy {
        listOf(
            KanaRow("row_ky", "KY-", "KY-Row", listOf(
                KanaCharacter("kana_kya", "きゃ", "キャ", "KYA", "KY-", 0, listOf(StrokeStep(1, "き", "KI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "キ", "KI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_kyu", "きゅ", "キュ", "KYU", "KY-", 2, listOf(StrokeStep(1, "き", "KI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "キ", "KI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_kyo", "きょ", "キョ", "KYO", "KY-", 4, listOf(StrokeStep(1, "き", "KI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "キ", "KI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_sh", "SH-", "SH-Row", listOf(
                KanaCharacter("kana_sha", "しゃ", "シャ", "SHA", "SH-", 0, listOf(StrokeStep(1, "し", "SHI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "シ", "SHI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_shu", "しゅ", "シュ", "SHU", "SH-", 2, listOf(StrokeStep(1, "し", "SHI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "シ", "SHI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_sho", "しょ", "ショ", "SHO", "SH-", 4, listOf(StrokeStep(1, "し", "SHI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "シ", "SHI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_ch", "CH-", "CH-Row", listOf(
                KanaCharacter("kana_cha", "ちゃ", "チャ", "CHA", "CH-", 0, listOf(StrokeStep(1, "ち", "CHI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "チ", "CHI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_chu", "ちゅ", "チュ", "CHU", "CH-", 2, listOf(StrokeStep(1, "ち", "CHI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "チ", "CHI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_cho", "ちょ", "チョ", "CHO", "CH-", 4, listOf(StrokeStep(1, "ち", "CHI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "チ", "CHI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_ny", "NY-", "NY-Row", listOf(
                KanaCharacter("kana_nya", "にゃ", "ニャ", "NYA", "NY-", 0, listOf(StrokeStep(1, "に", "NI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "ニ", "NI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_nyu", "にゅ", "ニュ", "NYU", "NY-", 2, listOf(StrokeStep(1, "に", "NI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "ニ", "NI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_nyo", "にょ", "ニョ", "NYO", "NY-", 4, listOf(StrokeStep(1, "に", "NI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "ニ", "NI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_hy", "HY-", "HY-Row", listOf(
                KanaCharacter("kana_hya", "ひゃ", "ヒャ", "HYA", "HY-", 0, listOf(StrokeStep(1, "ひ", "HI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "ヒ", "HI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_hyu", "ひゅ", "ヒュ", "HYU", "HY-", 2, listOf(StrokeStep(1, "ひ", "HI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "ヒ", "HI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_hyo", "ひょ", "ヒョ", "HYO", "HY-", 4, listOf(StrokeStep(1, "ひ", "HI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "ヒ", "HI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_my", "MY-", "MY-Row", listOf(
                KanaCharacter("kana_mya", "みゃ", "ミャ", "MYA", "MY-", 0, listOf(StrokeStep(1, "み", "MI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "ミ", "MI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_myu", "みゅ", "ミュ", "MYU", "MY-", 2, listOf(StrokeStep(1, "み", "MI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "ミ", "MI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_myo", "みょ", "ミョ", "MYO", "MY-", 4, listOf(StrokeStep(1, "み", "MI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "ミ", "MI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_ry", "RY-", "RY-Row", listOf(
                KanaCharacter("kana_rya", "りゃ", "リャ", "RYA", "RY-", 0, listOf(StrokeStep(1, "り", "RI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "リ", "RI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_ryu", "りゅ", "リュ", "RYU", "RY-", 2, listOf(StrokeStep(1, "り", "RI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "リ", "RI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_ryo", "りょ", "リョ", "RYO", "RY-", 4, listOf(StrokeStep(1, "り", "RI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "リ", "RI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_gy", "GY-", "GY-Row", listOf(
                KanaCharacter("kana_gya", "ぎゃ", "ギャ", "GYA", "GY-", 0, listOf(StrokeStep(1, "ぎ", "GI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "ギ", "GI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_gyu", "ぎゅ", "ギュ", "GYU", "GY-", 2, listOf(StrokeStep(1, "ぎ", "GI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "ギ", "GI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_gyo", "ぎょ", "ギョ", "GYO", "GY-", 4, listOf(StrokeStep(1, "ぎ", "GI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "ギ", "GI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_j", "J-", "J-Row", listOf(
                KanaCharacter("kana_ja", "じゃ", "ジャ", "JA", "J-", 0, listOf(StrokeStep(1, "じ", "JI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "ジ", "JI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_ju", "じゅ", "ジュ", "JU", "J-", 2, listOf(StrokeStep(1, "じ", "JI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "ジ", "JI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_jo", "じょ", "ジョ", "JO", "J-", 4, listOf(StrokeStep(1, "じ", "JI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "ジ", "JI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_by", "BY-", "BY-Row", listOf(
                KanaCharacter("kana_bya", "びゃ", "ビャ", "BYA", "BY-", 0, listOf(StrokeStep(1, "び", "BI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "ビ", "BI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_byu", "びゅ", "ビュ", "BYU", "BY-", 2, listOf(StrokeStep(1, "び", "BI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "ビ", "BI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_byo", "びょ", "ビョ", "BYO", "BY-", 4, listOf(StrokeStep(1, "び", "BI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "ビ", "BI base"), StrokeStep(2, "ョ", "Small YO")))
            )),
            KanaRow("row_py", "PY-", "PY-Row", listOf(
                KanaCharacter("kana_pya", "ぴゃ", "ピャ", "PYA", "PY-", 0, listOf(StrokeStep(1, "ぴ", "PI base"), StrokeStep(2, "ゃ", "Small YA")), listOf(StrokeStep(1, "ピ", "PI base"), StrokeStep(2, "ャ", "Small YA"))),
                null,
                KanaCharacter("kana_pyu", "ぴゅ", "ピュ", "PYU", "PY-", 2, listOf(StrokeStep(1, "ぴ", "PI base"), StrokeStep(2, "ゅ", "Small YU")), listOf(StrokeStep(1, "ピ", "PI base"), StrokeStep(2, "ュ", "Small YU"))),
                null,
                KanaCharacter("kana_pyo", "ぴょ", "ピョ", "PYO", "PY-", 4, listOf(StrokeStep(1, "ぴ", "PI base"), StrokeStep(2, "ょ", "Small YO")), listOf(StrokeStep(1, "ピ", "PI base"), StrokeStep(2, "ョ", "Small YO")))
            ))
        )
    }


    fun findPrevious(character: KanaCharacter): KanaCharacter {
        val index = allCharacters.indexOfFirst { it.id == character.id }
        return if (index > 0) allCharacters[index - 1] else allCharacters.last()
    }

    fun findNext(character: KanaCharacter): KanaCharacter {
        val index = allCharacters.indexOfFirst { it.id == character.id }
        return if (index >= 0 && index < allCharacters.size - 1) allCharacters[index + 1] else allCharacters.first()
    }
}