$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$project = Split-Path $repo -Parent
$state = Join-Path $project 'presentation_runtime_sprint3'
$python = Join-Path $project 'validation_env/Scripts/python.exe'
$pg = Join-Path $project 'validation_postgres'
$redis = Join-Path $project 'validation_redis'
$node = 'C:/Users/Magal/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
function Port-Open([int]$port) {
    $socket = [Net.Sockets.TcpClient]::new()
    try { $socket.Connect('127.0.0.1', $port); return $true } catch { return $false } finally { $socket.Dispose() }
}
foreach ($port in @(58020,55193)) { if (Port-Open $port) { throw "Puerto $port ocupado; no se altera el servicio existente." } }
New-Item -ItemType Directory -Force -Path $state | Out-Null
if (!(Port-Open 55432)) {
    & "$pg/pgsql/bin/pg_ctl.exe" -D "$pg/data" -l "$pg/presentation.log" -o '-h 127.0.0.1 -p 55432' -w start
    if ($LASTEXITCODE -ne 0) { throw 'No inició PostgreSQL aislado' }
}
if (!(Port-Open 56379)) {
    Start-Process -FilePath "$redis/redis-server.exe" -ArgumentList 'validation.conf' -WorkingDirectory $redis -WindowStyle Hidden | Out-Null
}
$env:DB_HOST='127.0.0.1'; $env:DB_PORT='55432'; $env:DB_USER='postgres'; $env:DB_PASSWORD=''; $env:DB_NAME='sprint3_demo_validation'; $env:USE_POSTGRES='True'
$env:REDIS_URL='redis://127.0.0.1:56379/0'
$env:FRONTEND_URL='http://127.0.0.1:55193'
$env:PYTHONPATH=$state; $env:DJANGO_SETTINGS_MODULE='sprint3_settings'
if (!(Test-Path (Join-Path $state 'sprint3_settings.py'))) {
    $settings = "from config.settings import *`nDEBUG = False`nCORS_ALLOWED_ORIGINS = ['http://127.0.0.1:55193']`nSECRET_KEY = '" + [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N') + "'`n"
    [IO.File]::WriteAllText((Join-Path $state 'sprint3_settings.py'), $settings, [Text.UTF8Encoding]::new($false))
}
$exists = & "$pg/pgsql/bin/psql.exe" -h 127.0.0.1 -p 55432 -U postgres -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='sprint3_demo_validation'"
if ($exists -ne '1') {
    & "$pg/pgsql/bin/createdb.exe" -h 127.0.0.1 -p 55432 -U postgres sprint3_demo_validation
    if ($LASTEXITCODE -ne 0) { throw 'No se creó la base aislada' }
}
Push-Location (Join-Path $repo 'backend')
try {
    & $python -B manage.py migrate --noinput
    if ($LASTEXITCODE -ne 0) { throw 'Falló la migración aislada' }
    & $python -B manage.py import_company_catalog ../docs/datos/catalogo_sp500.csv
    if ($LASTEXITCODE -ne 0) { throw 'Falló el catálogo' }
    & $python -B manage.py import_market_data --metrics ../docs/datos/MSFT_balance_2024.csv --source 'SEC FY2024 y métricas históricas derivadas'
    if ($LASTEXITCODE -ne 0) { throw 'Falló el balance' }
    & $python -B manage.py import_market_data --prices ../docs/datos/MSFT_cierres_5y.csv --source 'Yahoo Finance; cierres ajustados MSFT; snapshot 12-10-2026'
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron los precios' }
    foreach ($inventory in @('../docs/datos/sprint2_disponibilidad_MSFT.json','../docs/datos/sprint3/GOOGL-annual.json','../docs/datos/sprint3/META-annual.json')) {
        & $python -B manage.py import_annual_financials $inventory
        if ($LASTEXITCODE -ne 0) { throw "Falló el inventario $inventory" }
    }
    foreach ($ticker in @('MSFT','GOOGL','META')) {
        $provenance = Get-Content -LiteralPath "../docs/datos/sprint3/$ticker-prices-source.json" -Raw | ConvertFrom-Json
        & $python -B manage.py import_market_data --prices "../docs/datos/sprint3/$ticker-prices.csv" --source $provenance.source
        if ($LASTEXITCODE -ne 0) { throw "Falló el histórico $ticker" }
    }
    & $python -B manage.py preload_search
    if ($LASTEXITCODE -ne 0) { throw 'Falló la precarga de búsqueda' }
    & $python -B manage.py prepare_sprint3_demo --credentials-file (Join-Path $state 'demo_credentials.json')
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las cuentas aisladas' }
} finally { Pop-Location }
$api = Start-Process -FilePath $python -ArgumentList '-B','-m','waitress','--listen=127.0.0.1:58020','--threads=4','config.wsgi:application' -WorkingDirectory (Join-Path $repo 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $state 'api.out') -RedirectStandardError (Join-Path $state 'api.err') -PassThru
$reports = Start-Process -FilePath $python -ArgumentList '-B','manage.py','report_worker' -WorkingDirectory (Join-Path $repo 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $state 'reports.out') -RedirectStandardError (Join-Path $state 'reports.err') -PassThru
$env:VITE_API_URL='http://127.0.0.1:58020/api/v1'
$web = Start-Process -FilePath $node -ArgumentList 'node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','55193','--strictPort' -WorkingDirectory (Join-Path $repo 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $state 'web.out') -RedirectStandardError (Join-Path $state 'web.err') -PassThru
@{api=$api.Id;web=$web.Id;reports=$reports.Id;repo=$repo} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $state 'processes.json') -Encoding UTF8
Write-Host 'Demo Sprint 3: http://127.0.0.1:55193/login'
Write-Host "Cuentas Free y Pro: $state/demo_credentials.json (fuera de Git)."
