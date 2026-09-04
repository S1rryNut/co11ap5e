$ErrorActionPreference = "Stop"

$rewrittenDir = "C:\Users\1\Desktop\design\rewritten"
$sqlPath = "C:\Users\1\Desktop\design\articles\update_articles.sql"
$jsonPath = "C:\Users\1\Desktop\design\articles\excerpts.json"

$utf8 = New-Object System.Text.UTF8Encoding($false)
$jsonText = [System.IO.File]::ReadAllText($jsonPath, $utf8)
$excerpts = $jsonText | ConvertFrom-Json

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("BEGIN;")

Get-ChildItem "$rewrittenDir\*.md" | ForEach-Object {
  $slug = $_.BaseName
  $content = [System.IO.File]::ReadAllText($_.FullName, $utf8)
  $content = $content -replace "^\uFEFF", ""
  $plain = $content -replace '#{1,6}\s', '' -replace '`', '' -replace '\s+', ''
  $rm = [Math]::Max(2, [Math]::Ceiling($plain.Length / 300))
  $excerpt = $excerpts.$slug
  if (-not $excerpt) { throw "no excerpt for $slug" }
  $excerptEsc = $excerpt.Replace("'", "''")
  $line = "UPDATE articles SET content = `$`$" + $content + "`$`$, excerpt = '" + $excerptEsc + "', reading_minutes = " + $rm + ", updated_at = now() WHERE slug = '" + $slug + "';"
  [void]$sb.AppendLine($line)
}

[void]$sb.AppendLine("COMMIT;")
[System.IO.File]::WriteAllText($sqlPath, $sb.ToString(), $utf8)
Write-Output "SQL written OK, updates: $((Get-ChildItem "$rewrittenDir\*.md").Count)"
