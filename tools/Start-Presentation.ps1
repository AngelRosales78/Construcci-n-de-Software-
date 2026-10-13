$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$project = Split-Path $repo -Parent
$state = Join-Path $project 'presentation_runtime'
$python = Join-Path $project 'validation_env\Scripts\python.exe'
$pg = Join-Path $project 'validation_postgres'
$redis = Join-Path $project 'validation_redis'
$node = (Get-Command node.exe -ErrorAction Stop).Source
foreach($required in @($python,"$pg\pgsql\bin\pg_ctl.exe","$redis\redis-server.exe","$repo\frontend\node_modules\vite\bin\vite.js")) {
  if(!(Test-Path -LiteralPath $required)){throw "No esta preparado: $required"}
}
New-Item -ItemType Directory -Force -Path $state | Out-Null
function Port-Open([int]$port) {
  $socket = New-Object Net.Sockets.TcpClient
  try {$socket.Connect('127.0.0.1',$port);return $true} catch {return $false} finally {$socket.Dispose()}
}
foreach($port in @(55432,56379,58000,55173)) {
  if(Port-Open $port){throw "El puerto $port esta ocupado. Detener la demo previa antes de iniciar; no se alteran servicios ajenos."}
}
& "$pg\pgsql\bin\pg_ctl.exe" -D "$pg\data" -l "$pg\presentation.log" -o '-h 127.0.0.1 -p 55432' -w start
if($LASTEXITCODE -ne 0){throw 'No se pudo iniciar PostgreSQL temporal'}
$redisProcess = Start-Process -FilePath "$redis\redis-server.exe" -ArgumentList 'validation.conf' -WorkingDirectory $redis -WindowStyle Hidden -PassThru
$env:DB_HOST='127.0.0.1';$env:DB_PORT='55432';$env:DB_USER='postgres';$env:DB_PASSWORD='';$env:DB_NAME='sprint_delivery_validation'
$env:REDIS_URL='redis://127.0.0.1:56379/0'
$env:PYTHONPATH=$state
$env:DJANGO_SETTINGS_MODULE='presentation_settings'
$settings = "from config.settings import *`nDEBUG = False`nCORS_ALLOWED_ORIGINS = ['http://127.0.0.1:55173']`n"
[IO.File]::WriteAllText((Join-Path $state 'presentation_settings.py'),$settings,(New-Object Text.UTF8Encoding($false)))
$access = Join-Path $state 'demo_credentials.json'
$exists = & "$pg\pgsql\bin\psql.exe" -h 127.0.0.1 -p 55432 -U postgres -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='sprint_delivery_validation'"
if($exists -ne '1'){ & "$pg\pgsql\bin\createdb.exe" -h 127.0.0.1 -p 55432 -U postgres sprint_delivery_validation; if($LASTEXITCODE -ne 0){throw 'No se creo la base aislada'} }
Push-Location (Join-Path $repo 'backend')
try {
  & $python -B manage.py migrate --noinput
  if($LASTEXITCODE -ne 0){throw 'La migracion temporal fallo'}
  & $python -B manage.py import_company_catalog ..\docs\datos\catalogo_sp500.csv
  if($LASTEXITCODE -ne 0){throw 'Fallo el catalogo'}
  & $python -B manage.py import_market_data --metrics ..\docs\datos\MSFT_balance_2024.csv --source 'SEC, balance anual FY2024; Yahoo Finance, metricas derivadas'
  if($LASTEXITCODE -ne 0){throw 'Fallo el balance'}
  & $python -B manage.py import_market_data --prices ..\docs\datos\MSFT_cierres_5y.csv --source 'Yahoo Finance; cierres ajustados MSFT, snapshot 12-10-2026'
  if($LASTEXITCODE -ne 0){throw 'Fallaron los precios'}
  & $python -B manage.py preload_search
  if($LASTEXITCODE -ne 0){throw 'Fallo la precarga Redis'}
  & $python -B manage.py prepare_presentation --credentials-file $access
  if($LASTEXITCODE -ne 0){throw 'Fallo la cuenta desechable'}
} finally {Pop-Location}
$api = Start-Process -FilePath $python -ArgumentList '-B','-m','waitress','--listen=127.0.0.1:58000','--threads=8','config.wsgi:application' -WorkingDirectory (Join-Path $repo 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $state 'api.out') -RedirectStandardError (Join-Path $state 'api.err') -PassThru
$env:VITE_API_URL='http://127.0.0.1:58000/api/v1'
$web = Start-Process -FilePath $node -ArgumentList 'node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','55173','--strictPort' -WorkingDirectory (Join-Path $repo 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $state 'web.out') -RedirectStandardError (Join-Path $state 'web.err') -PassThru
$manifest = @{ api=$api.Id;web=$web.Id;redis=$redisProcess.Id;repo=$repo }
$manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $state 'processes.json') -Encoding UTF8
foreach($port in @(58000,55173)) {
  for($attempt=0;$attempt -lt 40;$attempt++){if(Port-Open $port){break};Start-Sleep -Milliseconds 250}
  if(!(Port-Open $port)){throw "No inicio el puerto $port. Revisar los registros en $state"}
}
$env:PRESENTATION_ACCESS_FILE=$access
$env:PRESENTATION_PROFILE=Join-Path $state 'browser-profile'
$browserArgs = '"' + (Join-Path $PSScriptRoot 'open_presentation.cjs') + '"'
Start-Process -FilePath $node -ArgumentList $browserArgs -WorkingDirectory $repo -WindowStyle Hidden -RedirectStandardOutput (Join-Path $state 'browser.out') -RedirectStandardError (Join-Path $state 'browser.err') | Out-Null
Write-Host 'Demo: http://127.0.0.1:55173/search'
Write-Host 'API: http://127.0.0.1:58000/api/docs/'
Write-Host 'Acceso real por formulario; catalogo de 503 simbolos y ficha MSFT con balance 2024 y precios importados.'
