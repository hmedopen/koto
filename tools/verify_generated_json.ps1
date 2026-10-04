$decksJson = Get-Content "app/src/main/assets/flashcard_decks.json" -Raw -Encoding UTF8 | ConvertFrom-Json
Write-Host "Decks in flashcard_decks.json: $($decksJson.Count)"

$totalCards = 0
$uniqueCardIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($d in $decksJson) {
    $totalCards += $d.cards.Count
    foreach ($c in $d.cards) {
        if (-not $uniqueCardIds.Add($c.id)) {
            Write-Host "Duplicate card ID: $($c.id)"
        }
    }
}
Write-Host "Total cards in flashcard_decks.json: $totalCards"
Write-Host "Unique card IDs: $($uniqueCardIds.Count)"

$contextsJson = Get-Content "app/src/main/assets/card_contexts.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$contextKeys = $contextsJson.psobject.properties.Name
Write-Host "Context keys in card_contexts.json: $($contextKeys.Count)"

# Check that every single card has a matching context by exact card ID as well as composite key!
$missingCardIdContexts = 0
$missingContexts = 0
foreach ($d in $decksJson) {
    foreach ($c in $d.cards) {
        if (-not ($contextKeys -contains $c.id)) {
            Write-Host "Missing exact card ID context for $($c.id)"
            $missingCardIdContexts++
        }
        $compKey = "$($c.japanese.Trim())|$($c.romaji.Trim())"
        $kanaKey = $c.japanese.Trim()
        $found = ($contextKeys -contains $compKey) -or ($contextKeys -contains $kanaKey)
        if (-not $found) {
            Write-Host "Missing context for card $($c.id): $($c.japanese) / $($c.romaji)"
            $missingContexts++
        }
    }
}
Write-Host "Cards missing cardId context: $missingCardIdContexts"
Write-Host "Cards missing fallback context: $missingContexts"
