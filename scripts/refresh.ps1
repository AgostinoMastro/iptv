<#
    refresh.ps1
    Rebuilds the working-only IPTV playlist and pushes it to GitHub.

    What it does:
      1. Downloads the configured source playlist(s), applying an optional per-source
         name filter (so we can keep only select channels from a country).
      2. Merges them, then runs iptv-checker to keep only reachable streams.
      3. Writes the result to the tracked output file (stable filename -> stable raw URL).
      4. Commits and pushes only when the playlist actually changed.

    Run manually:   powershell -ExecutionPolicy Bypass -File scripts\refresh.ps1
    Runs weekly via Windows Task Scheduler (see README).
#>

[CmdletBinding()]
param(
    [int]$TimeoutMs = 12000,
    [int]$Parallel  = 30,
    [int]$Retry     = 2,
    [switch]$NoPush
)

$ErrorActionPreference = 'Stop'

# Repo root = parent of this script's folder
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

# --- Configuration -------------------------------------------------------
# Each source: Url = playlist to fetch; Include = regex matched against the channel
# display name. Include = $null keeps ALL channels from that source.
# Everything is merged into the single OutputFile, so the TV URL never changes.
$Sources = @(
    [pscustomobject]@{ Url = 'https://iptv-org.github.io/iptv/countries/it.m3u'; Include = $null; GroupTitle = $null },
    [pscustomobject]@{ Url = 'https://iptv-org.github.io/iptv/countries/ca.m3u'; Include = 'CBC|CP24|TSN|Rai World'; GroupTitle = 'Canada' }
)
$OutputFile = Join-Path $RepoRoot 'playlist.m3u'
# ------------------------------------------------------------------------

$work = Join-Path $env:TEMP ("iptv-refresh-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null

function Set-GroupTitle {
    param([string]$ExtInfLine, [string]$GroupTitle)
    if ($ExtInfLine -match 'group-title="[^"]*"') {
        return ($ExtInfLine -replace 'group-title="[^"]*"', "group-title=`"$GroupTitle`"")
    }
    return ($ExtInfLine -replace '(#EXTINF:[^\r\n]*)(,\s*)', "`$1 group-title=`"$GroupTitle`"`$2")
}

# Adds an entry (its #EXTINF line + following option/URL lines) to $lines when it
# passes the include filter.
function Add-Entry {
    param($Buffer, $Include, $GroupTitle, $Lines)
    if ($null -eq $Buffer -or $Buffer.Count -eq 0) { return }
    $name = ($Buffer[0] -replace '^.*?,', '')
    if ($null -eq $Include -or $name -match $Include) {
        for ($i = 0; $i -lt $Buffer.Count; $i++) {
            $line = $Buffer[$i]
            if ($i -eq 0 -and $GroupTitle) {
                $line = Set-GroupTitle -ExtInfLine $line -GroupTitle $GroupTitle
            }
            $Lines.Add($line)
        }
    }
}

try {
    Write-Host "[1/4] Downloading + filtering $($Sources.Count) source playlist(s)..."
    $merged = Join-Path $work 'merged-source.m3u'
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('#EXTM3U')

    foreach ($src in $Sources) {
        # Download to a file (Invoke-WebRequest .Content can return raw bytes), then read as text.
        $tmp = Join-Path $work ("src-" + [guid]::NewGuid().ToString('N') + ".m3u")
        Invoke-WebRequest -Uri $src.Url -OutFile $tmp -UseBasicParsing

        # Walk entries: an entry is a #EXTINF line plus every line until the next #EXTINF.
        $buffer = $null
        foreach ($line in (Get-Content -LiteralPath $tmp -Encoding UTF8)) {
            if ($line -match '^\s*#EXTM3U') { continue }
            if ($line -match '^\s*#EXTINF') {
                Add-Entry -Buffer $buffer -Include $src.Include -GroupTitle $src.GroupTitle -Lines $lines
                $buffer = New-Object System.Collections.Generic.List[string]
                $buffer.Add($line)
            }
            elseif ($null -ne $buffer) {
                $buffer.Add($line)
            }
        }
        Add-Entry -Buffer $buffer -Include $src.Include -GroupTitle $src.GroupTitle -Lines $lines
    }

    # Write UTF-8 WITHOUT a BOM (a BOM makes iptv-checker fail with "Unable to parse a playlist")
    [System.IO.File]::WriteAllLines($merged, $lines, (New-Object System.Text.UTF8Encoding($false)))
    $preCount = (Select-String -Path $merged -Pattern '#EXTINF' -AllMatches).Count
    Write-Host "      $preCount channels selected before health check."

    Write-Host "[2/4] Checking streams (timeout ${TimeoutMs}ms, parallel ${Parallel}, retry ${Retry})..."
    $checkOut = Join-Path $work 'checked'
    & npx --yes iptv-checker@latest $merged -o $checkOut -t $TimeoutMs -p $Parallel -r $Retry -k
    $online = Join-Path $checkOut 'online.m3u'
    if (-not (Test-Path $online)) {
        throw "iptv-checker did not produce online.m3u - aborting so the good playlist is not overwritten."
    }

    $count = (Select-String -Path $online -Pattern '#EXTINF' -AllMatches).Count
    if ($count -lt 1) {
        throw "online.m3u has 0 channels - refusing to publish an empty playlist."
    }
    Write-Host "[3/4] $count working channels found. Writing $OutputFile"
    Copy-Item $online $OutputFile -Force

    if ($NoPush) {
        Write-Host "[4/4] -NoPush set; skipping git commit/push."
        return
    }

    Write-Host "[4/4] Committing and pushing..."
    & git add -- (Split-Path -Leaf $OutputFile)
    $pending = & git status --porcelain -- (Split-Path -Leaf $OutputFile)
    if ([string]::IsNullOrWhiteSpace($pending)) {
        Write-Host "No changes to publish - playlist is already up to date."
        return
    }
    $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm'
    & git commit -m "Refresh playlist ($count channels) - $stamp"
    & git push
    Write-Host "Published $count channels."
}
finally {
    Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
