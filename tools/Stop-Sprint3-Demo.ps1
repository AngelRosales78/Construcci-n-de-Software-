$ErrorActionPreference = 'Stop'
$project = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$file = Join-Path $project 'presentation_runtime_sprint3/processes.json'
if (!(Test-Path -LiteralPath $file)) { Write-Host 'No hay servicios registrados.'; return }
$state = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
$rules = @{api='--listen=127.0.0.1:58020';web='--port 55193';reports='manage.py report_worker';notifications='manage.py notification_worker'}
$owned = @()
foreach ($name in $rules.Keys) {
    $serviceId = $state.$name
    if (!$serviceId) { continue }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$serviceId"
    if (!$process) { continue }
    $expected = if ($name -eq 'web') { 'C:/Users/Magal/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe' } else { Join-Path $project 'validation_env/Scripts/python.exe' }
    $declaredPath = $process.CommandLine.Replace('/', '\')
    $expectedPath = [IO.Path]::GetFullPath($expected).Replace('/', '\')
    if ([IO.Path]::GetFileName($process.ExecutablePath) -ne [IO.Path]::GetFileName($expectedPath) -or
        $declaredPath.IndexOf($expectedPath, [StringComparison]::OrdinalIgnoreCase) -lt 0 -or !$process.CommandLine.Contains($rules[$name])) {
        throw "El proceso registrado como $name cambió. No se detiene ningún servicio."
    }
    $owned += @{Name=$name;Id=$serviceId}
}
foreach ($service in $owned) {
    Stop-Process -Id $service.Id
    Wait-Process -Id $service.Id -Timeout 10 -ErrorAction SilentlyContinue
    Write-Host "Servicio detenido: $($service.Name)"
}
