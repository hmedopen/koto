[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$inputPath = "c:\Users\HMED OPEN\Documents\koto\cards tab database.md"
$decksJsonPath = "c:\Users\HMED OPEN\Documents\koto\koto the project\app\src\main\assets\flashcard_decks.json"
$contextsJsonPath = "c:\Users\HMED OPEN\Documents\koto\koto the project\app\src\main\assets\card_contexts.json"

function Escape-Json([string]$str) {
    if ([string]::IsNullOrEmpty($str)) { return "" }
    $sb = [System.Text.StringBuilder]::new()
    for ($i = 0; $i -lt $str.Length; $i++) {
        $c = $str[$i]
        switch ($c) {
            '"'  { [void]$sb.Append('\"') }
            '\'  { [void]$sb.Append('\\') }
            "`n" { [void]$sb.Append('\n') }
            "`r" { [void]$sb.Append('\r') }
            "`t" { [void]$sb.Append('\t') }
            default { [void]$sb.Append($c) }
        }
    }
    return $sb.ToString()
}

function Get-DeckIcon([int]$number) {
    switch ($number) {
        1  { return "chatbubble" }
        2  { return "chatbubble" }
        3  { return "hashtag" }
        4  { return "hashtag" }
        5  { return "hashtag" }
        6  { return "hashtag" }
        7  { return "hashtag" }
        8  { return "chatbubble" }
        9  { return "chatbubble" }
        10 { return "chatbubble" }
        11 { return "footsteps" }
        12 { return "home" }
        13 { return "footsteps" }
        14 { return "footsteps" }
        15 { return "footsteps" }
        16 { return "utensils" }
        17 { return "utensils" }
        18 { return "utensils" }
        19 { return "home" }
        20 { return "home" }
        21 { return "footsteps" }
        22 { return "footsteps" }
        23 { return "footsteps" }
        24 { return "footsteps" }
        25 { return "footsteps" }
        26 { return "footsteps" }
        27 { return "train" }
        28 { return "train" }
        29 { return "train" }
        30 { return "chatbubble" }
        31 { return "footsteps" }
        32 { return "footsteps" }
        33 { return "footsteps" }
        34 { return "footsteps" }
        35 { return "chatbubble" }
        36 { return "chatbubble" }
        37 { return "footsteps" }
        38 { return "footsteps" }
        39 { return "home" }
        40 { return "footsteps" }
        41 { return "footsteps" }
        42 { return "train" }
        43 { return "footsteps" }
        44 { return "chatbubble" }
        45 { return "chatbubble" }
        46 { return "chatbubble" }
        47 { return "chatbubble" }
        48 { return "chatbubble" }
        49 { return "footsteps" }
        50 { return "chatbubble" }
        default { return "footsteps" }
    }
}

function Get-DeckTierInfo([int]$number) {
    if ($number -le 16) {
        return @{
            Tier = 1
            Category = "Survival, Anchor Vocabulary & Core Mechanics"
        }
    }
    elseif ($number -le 34) {
        return @{
            Tier = 2
            Category = "Real-World Living, Navigation & Daily Tasks"
        }
    }
    else {
        return @{
            Tier = 3
            Category = "Complex Expression, Society, Nuance & Culture"
        }
    }
}

$lines = [System.IO.File]::ReadAllLines($inputPath, [System.Text.Encoding]::UTF8)

$currentDeck = $null
$decks = [System.Collections.Generic.List[PSCustomObject]]::new()
$currentCard = $null
$currentExample = $null
$inContext = $false

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $trimmed = $line.Trim()

    if ($trimmed -match '^#\s+Deck\s+(\d+):\s*(.*)') {
        $deckNum = [int]$matches[1]
        $deckTitle = $matches[2].Trim()
        $tierInfo = Get-DeckTierInfo $deckNum
        $currentDeck = [PSCustomObject]@{
            number = $deckNum
            id = ("deck_{0:D2}" -f $deckNum)
            title = $deckTitle
            category = $tierInfo.Category
            tier = $tierInfo.Tier
            icon = (Get-DeckIcon $deckNum)
            cards = [System.Collections.Generic.List[PSCustomObject]]::new()
        }
        $decks.Add($currentDeck)
        $currentCard = $null
        $inContext = $false
        continue
    }

    if ($trimmed -match '^###\s+Card\s+(\d+)') {
        $cardNum = $currentDeck.cards.Count + 1
        $cardId = ("card_{0:D2}_{1:D3}" -f $currentDeck.number, $cardNum)
        $currentCard = [PSCustomObject]@{
            cardIndex = $cardNum
            id = $cardId
            kana = ""
            romaji = ""
            english = ""
            usageNote = ""
            examples = [System.Collections.Generic.List[PSCustomObject]]::new()
        }
        $currentDeck.cards.Add($currentCard)
        $inContext = $false
        continue
    }

    if ($trimmed -eq '{?}') {
        $inContext = $true
        continue
    }

    if ($currentCard -ne $null) {
        if (-not $inContext) {
            if ($trimmed -match '^\*\s+\*\*Front\s*\(Kana\):\*\*\s*(.*)') {
                $currentCard.kana = $matches[1].Trim()
            }
            elseif ($trimmed -match '^\*\s+\*\*Front\s*\(Romaji\):\*\*\s*(.*)') {
                $currentCard.romaji = $matches[1].Trim()
            }
            elseif ($trimmed -match '^\*\s+\*\*Back\s*\(English\):\*\*\s*(.*)') {
                $currentCard.english = $matches[1].Trim()
            }
        }
        else {
            if ($trimmed -match '^\*\s+\*\*Context\s*/\s*Usage Note:\*\*\s*(.*)') {
                $currentCard.usageNote = $matches[1].Trim()
            }
            elseif ($trimmed -match '^\*\s+\*\*Example\s+\d+:\*\*') {
                $currentExample = [PSCustomObject]@{
                    kana = ""
                    romaji = ""
                    english = ""
                }
                $currentCard.examples.Add($currentExample)
            }
            elseif ($trimmed -match '^\*\s+\*\*Kana:\*\*\s*(.*)') {
                if ($currentExample -ne $null) {
                    $currentExample.kana = $matches[1].Trim()
                }
            }
            elseif ($trimmed -match '^\*\s+\*\*Romaji:\*\*\s*(.*)') {
                if ($currentExample -ne $null) {
                    $currentExample.romaji = $matches[1].Trim()
                }
            }
            elseif ($trimmed -match '^\*\s+\*\*English:\*\*\s*(.*)') {
                if ($currentExample -ne $null) {
                    $currentExample.english = $matches[1].Trim()
                }
            }
        }
    }
}

Write-Host "Parsed $($decks.Count) decks."

# Generate flashcard_decks.json
$decksSb = [System.Text.StringBuilder]::new()
[void]$decksSb.AppendLine("[")

for ($dIdx = 0; $dIdx -lt $decks.Count; $dIdx++) {
    $d = $decks[$dIdx]
    [void]$decksSb.AppendLine("  {")
    [void]$decksSb.AppendLine("    `"id`": `"$($d.id)`",")
    [void]$decksSb.AppendLine("    `"number`": $($d.number),")
    [void]$decksSb.AppendLine("    `"title`": `"$($d.title.Replace('"', '\"'))`",")
    [void]$decksSb.AppendLine("    `"category`": `"$($d.category.Replace('"', '\"'))`",")
    [void]$decksSb.AppendLine("    `"tier`": $($d.tier),")
    [void]$decksSb.AppendLine("    `"icon`": `"$($d.icon)`",")
    [void]$decksSb.AppendLine("    `"cards`": [")

    for ($cIdx = 0; $cIdx -lt $d.cards.Count; $cIdx++) {
        $c = $d.cards[$cIdx]
        $cComma = if ($cIdx -lt $d.cards.Count - 1) { "," } else { "" }
        [void]$decksSb.AppendLine("      {")
        [void]$decksSb.AppendLine("        `"id`": `"$($c.id)`",")
        [void]$decksSb.AppendLine("        `"japanese`": `"$((Escape-Json $c.kana))`",")
        [void]$decksSb.AppendLine("        `"romaji`": `"$((Escape-Json $c.romaji))`",")
        [void]$decksSb.AppendLine("        `"english`": `"$((Escape-Json $c.english))`",")
        [void]$decksSb.AppendLine("        `"dueTimestamp`": 0,")
        [void]$decksSb.AppendLine("        `"intervalDays`": 0,")
        [void]$decksSb.AppendLine("        `"state`": `"NEW`"")
        [void]$decksSb.AppendLine("      }$cComma")
    }

    $dComma = if ($dIdx -lt $decks.Count - 1) { "," } else { "" }
    [void]$decksSb.AppendLine("    ]")
    [void]$decksSb.AppendLine("  }$dComma")
}
[void]$decksSb.AppendLine("]")

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($decksJsonPath, $decksSb.ToString(), $utf8NoBom)
Write-Host "Wrote flashcard_decks.json ($([System.IO.FileInfo]::new($decksJsonPath).Length) bytes)"

# Generate card_contexts.json
$contextsSb = [System.Text.StringBuilder]::new()
[void]$contextsSb.AppendLine("{")

$entries = [System.Collections.Generic.List[PSCustomObject]]::new()
$seenComposite = [System.Collections.Generic.HashSet[string]]::new()
$seenKana = [System.Collections.Generic.HashSet[string]]::new()

foreach ($d in $decks) {
    foreach ($c in $d.cards) {
        $compKey = "$($c.kana.Trim())|$($c.romaji.Trim())"
        $kanaKey = $c.kana.Trim()

        # Build example JSON array string
        $exParts = @()
        foreach ($ex in $c.examples) {
            $exParts += ("{`"kana`":`"$((Escape-Json $ex.kana))`",`"romaji`":`"$((Escape-Json $ex.romaji))`",`"english`":`"$((Escape-Json $ex.english))`"}")
        }
        $exStr = "[" + ($exParts -join ",") + "]"
        $valJson = "{`"cardId`":`"$($c.id)`",`"kana`":`"$((Escape-Json $c.kana))`",`"romaji`":`"$((Escape-Json $c.romaji))`",`"english`":`"$((Escape-Json $c.english))`",`"usageNote`":`"$((Escape-Json $c.usageNote))`",`"examples`":$exStr}"

        # 1. Primary key: Card ID (unique for every single card, ensures 0 collisions)
        $entries.Add([PSCustomObject]@{ Key = $c.id; Json = $valJson })

        # 2. Composite key ($kana|$romaji)
        if ($seenComposite.Add($compKey)) {
            $entries.Add([PSCustomObject]@{ Key = $compKey; Json = $valJson })
        }

        # 3. Kana key ($kana)
        if ($seenKana.Add($kanaKey)) {
            $entries.Add([PSCustomObject]@{ Key = $kanaKey; Json = $valJson })
        }
    }
}

for ($eIdx = 0; $eIdx -lt $entries.Count; $eIdx++) {
    $entry = $entries[$eIdx]
    $eComma = if ($eIdx -lt $entries.Count - 1) { "," } else { "" }
    $escapedKey = Escape-Json $entry.Key
    [void]$contextsSb.AppendLine("`"$escapedKey`": $($entry.Json)$eComma")
}

[void]$contextsSb.AppendLine("}")

[System.IO.File]::WriteAllText($contextsJsonPath, $contextsSb.ToString(), $utf8NoBom)
Write-Host "Wrote card_contexts.json with $($entries.Count) entries ($([System.IO.FileInfo]::new($contextsJsonPath).Length) bytes)"
