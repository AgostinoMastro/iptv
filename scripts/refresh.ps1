<#
    refresh.ps1
    Rebuilds the working-only IPTV playlist and pushes it to GitHub.

    What it does:
      1. Downloads the configured source playlist(s).
      2. Runs iptv-checker to keep only reachable streams.
      3. Writes the result to the tracked output file (stable filename -> stable raw URL).
      4. Commits and pushes only when the playlist actually changed.

    Run manually:   powershell -ExecutionPolicy Bypass -File scripts\refresh.ps1
    Runs weekly via Windows Task Scheduler (see README).
#>

[CmdletBinding()]
param(
    [int]$TimeoutMs = 8000,
    [int]$Parallel  = 30,
    [switch]$NoPush
)

$ErrorActionPreference = 'Stop'

# Repo root = parent of this script's folder
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

# --- Configuration -------------------------------------------------------
# Add more sources here later (e.g. US, Canada). All listed sources are merged
# into the single output file below, so the TV URL never changes.
$Sources = @(
    'https://iptv-org.github.io/iptv/countries/it.m3u'
)
$OutputFile = Join-Path $RepoRoot 'italy-working.m3u'
# ------------------------------------------------------------------------

$work = Join-Path $env:TEMP ("iptv-refresh-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null

try {
    Write-Host "[1/4] Downloading $($Sources.Count) source playlist(s)..."
    $merged = Join-Path $work 'merged-source.m3u'
    "#EXTM3U" | Out-File -FilePath $merged -Encoding utf8
    foreach ($url in $Sources) {
        $tmp = Join-Path $work ("src-" + [guid]::NewGuid().ToString('N') + ".m3u")
        Invoke-WebRequest -Uri $url -OutFile $tmp -UseBasicParsing
        # Append everything except the leading #EXTM3U header line
        Get-Content $tmp | Where-Object { $_ -notmatch '^\s*#EXTM3U' } |
            Out-File -FilePath $merged -Encoding utf8 -Append
    }

    Write-Host "[2/4] Checking streams (timeout ${TimeoutMs}ms, parallel ${Parallel})..."
    $checkOut = Join-Path $work 'checked'
    & npx --yes iptv-checker@latest $merged -o $checkOut -t $TimeoutMs -p $Parallel -k
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
