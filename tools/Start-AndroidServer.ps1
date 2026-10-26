param([int]$Port = 8000, [string]$Python = "python", [switch]$Prepare)
$ErrorActionPreference = "Stop"
if (Test-Path -LiteralPath $Python) { $Python = (Resolve-Path -LiteralPath $Python).Path }
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot "backend"
function Invoke-Manage {
    param([string[]]$ManageArguments)
    & $Python (Join-Path $backendRoot "manage.py") @ManageArguments "--settings=config.android_local"
    if ($LASTEXITCODE -ne 0) { throw "El comando del servidor no terminó correctamente." }
}
Push-Location $backendRoot
try {
    if ($Prepare) {
        Invoke-Manage -ManageArguments @("migrate", "--noinput")
        Invoke-Manage -ManageArguments @("import_company_catalog", (Join-Path $projectRoot "docs/datos/catalogo_sp500.csv"))
        Invoke-Manage -ManageArguments @("import_annual_financials", (Join-Path $projectRoot "docs/datos/sprint2_disponibilidad_MSFT.json"))
        Invoke-Manage -ManageArguments @("import_market_data", "--metrics", (Join-Path $projectRoot "docs/datos/MSFT_balance_2024.csv"), "--source", "https://www.sec.gov/edgar/browse/?CIK=789019")
        Invoke-Manage -ManageArguments @("import_market_data", "--prices", (Join-Path $projectRoot "docs/datos/MSFT_cierres_5y.csv"), "--source", "https://finance.yahoo.com/quote/MSFT/history/")
    }
    Write-Host "Servidor local de Android. Base separada android-local.sqlite3. Registra una cuenta desde la app."
    Invoke-Manage -ManageArguments @("runserver", "0.0.0.0:$Port", "--noreload")
} finally { Pop-Location }
