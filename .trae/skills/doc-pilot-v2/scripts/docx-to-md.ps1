# docx-to-md.ps1 - convert a DOCX (Word 2007+ zip package) to a plain-text Markdown file.
# Pure ASCII by design (project convention: ps1 with Chinese requires BOM).
# NOTE: this is the FALLBACK converter for machines without Python. Prefer
# docx-to-md.py (python-docx), which renders real Markdown tables; this .ps1
# flattens table cells to '| ' prefixed lines, losing row structure.
# Usage: powershell -ExecutionPolicy Bypass -File docx-to-md.ps1 -DocxPath <in.docx> -MdPath <out.md>
# Method: read word/document.xml via .NET ZipFile, walk w:p nodes in document order.
# Paragraph text is built from w:t runs (InnerText, Unicode-safe in memory) and written
# once with [IO.File]::WriteAllText + UTF8Encoding($true). No text pipelines involved.
# Limits: tables are flattened to '| ' prefixed lines; images/footnotes are dropped.

param(
    [Parameter(Mandatory = $true)][string]$DocxPath,
    [Parameter(Mandatory = $true)][string]$MdPath
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem

$fullDocx = (Resolve-Path -LiteralPath $DocxPath).Path
$fullMd = [System.IO.Path]::GetFullPath($MdPath)
$mdDir = Split-Path -Parent $fullMd
if ($mdDir -and -not (Test-Path $mdDir)) { New-Item -ItemType Directory -Path $mdDir -Force | Out-Null }

$zip = [System.IO.Compression.ZipFile]::OpenRead($fullDocx)
try {
    $entry = $zip.GetEntry('word/document.xml')
    if (-not $entry) { throw 'word/document.xml not found - not a valid docx?' }
    $reader = New-Object System.IO.StreamReader($entry.Open(), [System.Text.Encoding]::UTF8)
    try { $raw = $reader.ReadToEnd() } finally { $reader.Dispose() }
}
finally { $zip.Dispose() }

[xml]$xml = $raw
$nsm = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
$nsm.AddNamespace('w', 'http://schemas.openxmlformats.org/wordprocessingml/2006/main')

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine('# Converted from DOCX')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('> Machine-converted by doc-pilot scripts/docx-to-md.ps1. Source of truth is the original DOCX.')
[void]$sb.AppendLine('> Tables are flattened to "|" prefixed lines; images and footnotes are dropped.')
[void]$sb.AppendLine('')

$paras = $xml.SelectNodes('//w:body//w:p', $nsm)
foreach ($p in $paras) {
    $text = New-Object System.Text.StringBuilder
    foreach ($n in $p.SelectNodes('.//w:t | .//w:tab | .//w:br', $nsm)) {
        switch ($n.LocalName) {
            't'   { [void]$text.Append($n.InnerText) }
            'tab' { [void]$text.Append("`t") }
            'br'  { [void]$text.Append(' ') }
        }
    }
    $line = $text.ToString().Trim()
    if ($line -eq '') { continue }

    # Heading detection: outline level first, then "HeadingN" style ids.
    $lvl = 0
    $ol = $p.SelectSingleNode('w:pPr/w:outlineLvl/@w:val', $nsm)
    if ($ol) { $lvl = [int]$ol.Value + 1 }
    else {
        $psNode = $p.SelectSingleNode('w:pPr/w:pStyle/@w:val', $nsm)
        if ($psNode -and $psNode.Value -match '(?i)heading\s*([1-6])') { $lvl = [int]$Matches[1] }
    }
    if ($lvl -ge 1 -and $lvl -le 6) { $line = ('#' * $lvl) + ' ' + $line }
    elseif ($p.SelectSingleNode('w:pPr/w:numPr', $nsm)) { $line = '- ' + $line }

    # Table context: prefix rows so table text stays distinguishable.
    $inTbl = $false
    $nd = $p
    while ($nd) {
        if ($nd.LocalName -eq 'tbl') { $inTbl = $true; break }
        $nd = $nd.ParentNode
    }
    if ($inTbl) { $line = '| ' + $line }

    [void]$sb.AppendLine($line)
    [void]$sb.AppendLine('')
}

$utf8Bom = New-Object System.Text.UTF8Encoding($true)
[System.IO.File]::WriteAllText($fullMd, $sb.ToString(), $utf8Bom)

"WROTE: $fullMd ($($sb.Length) chars, $($paras.Count) source paragraphs)"