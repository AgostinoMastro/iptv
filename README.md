# iptv

A self-refreshing, working-only IPTV playlist for use in any M3U player (IPTV Smarters, VLC, IPTVnator, etc.).

A local script downloads public IPTV source playlists, verifies every stream, keeps only the reachable ones, and pushes the result here. Your TV points at one permanent raw URL; only the contents change over time.

## Playlist URL (paste this into your player)

```
https://raw.githubusercontent.com/AgostinoMastro/iptv/main/playlist.m3u
```

This single combined URL never changes. In IPTV Smarters, choose "Load Your Playlist or File/URL" (not Xtream Codes) and paste it. Use the group/category filter in your player to jump between Italian and Canadian channels.

## What's in it

- Italy: all working free channels from iptv-org.
- Canada (curated): only selected channels - CBC (all regional feeds), CP24, TSN3, TSN The Ocho, Rai World Premium.

Premium cable channels (OMNI, TLC, TLN, Food Network, Sportsnet, most of TSN) are NOT in free public lists and are intentionally not included.

## Refresh manually

```powershell
powershell -ExecutionPolicy Bypass -File scripts\refresh.ps1
```

Requires Node.js (uses `npx iptv-checker`) and a git remote you can push to.

## Automate weekly (Windows Task Scheduler)

A task named "IPTV Playlist Refresh" runs every Sunday at 4am. To (re)create it:

```powershell
$action  = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument "-ExecutionPolicy Bypass -WindowStyle Hidden -File `"$PWD\scripts\refresh.ps1`"" `
    -WorkingDirectory "$PWD"
$trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At 4am
Register-ScheduledTask -TaskName "IPTV Playlist Refresh" -Action $action -Trigger $trigger -Force
```

## Change what's included

Edit the `$Sources` array in [scripts/refresh.ps1](scripts/refresh.ps1):

- `Include = $null` keeps ALL channels from that source (used for Italy).
- `Include = 'CBC|CP24|TSN|Rai World'` keeps only channels whose name matches the regex (used for Canada).

Add another line to include a new country; everything merges into the single `playlist.m3u`, so the URL above stays the same.

## Notes

- Some channels are tagged `[Geo-blocked]` and only play from inside their home country (use a VPN). The CBC regional feeds are geo-blocked to Canada.
- The list is a point-in-time snapshot; the weekly refresh (with retries) keeps it healthy.
- Source data: [iptv-org/iptv](https://github.com/iptv-org/iptv). Checker: [freearhey/iptv-checker](https://github.com/freearhey/iptv-checker).
