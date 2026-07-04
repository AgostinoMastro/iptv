# iptv

A self-refreshing, working-only IPTV playlist for use in any M3U player (IPTV Smarters, VLC, IPTVnator, etc.).

A local script downloads public IPTV source playlists, verifies every stream, keeps only the reachable ones, and pushes the result here. Your TV points at one permanent raw URL; only the contents change over time.

## Playlist URL (paste this into your player)

```
https://raw.githubusercontent.com/AgostinoMastro/iptv/main/italy-working.m3u
```

This URL never changes. In IPTV Smarters, choose "Load Your Playlist or File/URL" (not Xtream Codes) and paste it.

## Refresh manually

```powershell
powershell -ExecutionPolicy Bypass -File scripts\refresh.ps1
```

Requires Node.js (uses `npx iptv-checker`) and a git remote you can push to.

## Automate weekly (Windows Task Scheduler)

Create a task that runs the refresh every Sunday at 4am:

```powershell
$action  = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument "-ExecutionPolicy Bypass -File `"$PWD\scripts\refresh.ps1`""
$trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At 4am
Register-ScheduledTask -TaskName "IPTV Playlist Refresh" -Action $action -Trigger $trigger
```

## Add more countries later

Edit the `$Sources` array in [scripts/refresh.ps1](scripts/refresh.ps1), e.g. add:

```powershell
'https://iptv-org.github.io/iptv/countries/us.m3u'
'https://iptv-org.github.io/iptv/countries/ca.m3u'
```

All sources are merged into the single output file, so the playlist URL above stays the same.

## Notes

- Some channels are tagged `[Geo-blocked]` and only play from inside their home country (use a VPN).
- The list is a point-in-time snapshot; the weekly refresh keeps it healthy.
- Source data: [iptv-org/iptv](https://github.com/iptv-org/iptv). Checker: [freearhey/iptv-checker](https://github.com/freearhey/iptv-checker).
