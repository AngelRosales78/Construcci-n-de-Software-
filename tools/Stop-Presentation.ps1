$ErrorActionPreference = 'Stop'
$repo=Split-Path $PSScriptRoot -Parent
$project=Split-Path $repo -Parent
$state=Join-Path $project 'presentation_runtime'
$file=Join-Path $state 'processes.json'
if(Test-Path -LiteralPath $file) {
  $manifest=Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
  foreach($entry in @(@($manifest.api,'waitress.*58000'),@($manifest.web,'vite.*55173'))) {
    $ownedId=[int]$entry[0]
    $owned=Get-CimInstance Win32_Process -Filter "ProcessId = $ownedId"
    if($owned -and $owned.CommandLine -match $entry[1]){Stop-Process -Id $ownedId}
  }
  Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'msedge.exe' -and $_.CommandLine -and $_.CommandLine.Contains((Join-Path $state 'browser-profile')) } | ForEach-Object { Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue }
  $redis=Get-CimInstance Win32_Process -Filter "ProcessId = $([int]$manifest.redis)"
  $expected=Join-Path $project 'validation_redis\redis-server.exe'
  if($redis -and $redis.ExecutablePath -eq $expected){ & (Join-Path $project 'validation_redis\redis-cli.exe') -h 127.0.0.1 -p 56379 shutdown nosave }
  & (Join-Path $project 'validation_postgres\pgsql\bin\pg_ctl.exe') -D (Join-Path $project 'validation_postgres\data') -w stop
}
Write-Host 'Se detuvieron los servidores temporales de la presentacion.'
