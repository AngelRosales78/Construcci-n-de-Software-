$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$project=Split-Path $repo -Parent
$state=Join-Path $project 'presentation_runtime'
$access=Get-Content -LiteralPath (Join-Path $state 'benchmark_access.json') -Raw | ConvertFrom-Json
$headers=@{Authorization='Bearer '+$access.access}
Write-Host 'HU03: respuesta paginada, sector y orden'
$data=Invoke-RestMethod -Uri 'http://127.0.0.1:58000/api/v1/search/?sector=technology&ordering=-market_cap' -Headers $headers
Write-Host "Empresas encontradas: $($data.count)"
$data.results | Select-Object -First 5 ticker,sector,market_cap | Format-Table
Write-Host 'HU03_02: sugerencias Redis'
(Invoke-RestMethod -Uri 'http://127.0.0.1:58000/api/v1/search/autocomplete/?q=T0' -Headers $headers).suggestions | Select-Object -First 5 | Format-Table
Write-Host 'HU03_05: historial de la cuenta de demostracion'
(Invoke-RestMethod -Uri 'http://127.0.0.1:58000/api/v1/search/history/' -Headers $headers).results | Format-Table id,query,created_at
Write-Host 'HU04: ficha, clasificacion y Altman-Z'
$summary=Invoke-RestMethod -Uri 'http://127.0.0.1:58000/api/v1/companies/T000/summary/' -Headers $headers
$summary | Select-Object ticker,per,roe,market_cap,beta,dividend_yield,source,is_demo | Format-List
$summary.profile | Format-List
$summary.altman_z | ConvertTo-Json -Depth 4
Write-Host 'HU04_04: numero de precios persistidos para 5Y'
$prices=Invoke-RestMethod -Uri 'http://127.0.0.1:58000/api/v1/companies/T000/history/?range=5Y' -Headers $headers
$prices | Select-Object ticker,count,range,price_type,is_demo | Format-List
$env:DB_HOST='127.0.0.1';$env:DB_PORT='55432';$env:DB_USER='postgres';$env:DB_PASSWORD='';$env:DB_NAME='sprint_validation'
$env:PYTHONPATH=$state;$env:DJANGO_SETTINGS_MODULE='presentation_settings'
Push-Location (Join-Path $repo 'backend')
try { & (Join-Path $project 'validation_env\Scripts\python.exe') -B manage.py show_task_evidence } finally {Pop-Location}
