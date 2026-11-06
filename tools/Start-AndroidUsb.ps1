param(
    [string]$Serial,
    [string]$ServerProject,
    [ValidateRange(1, 65535)][int]$Port = 8000,
    [string]$Adb
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (!$ServerProject) { $ServerProject = $projectRoot }
if (!$Adb) {
    $localProperties = Join-Path $projectRoot 'android/local.properties'
    $sdkEntry = if (Test-Path -LiteralPath $localProperties) {
        Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir\s*=' } | Select-Object -First 1
    }
    $sdk = if ($sdkEntry) { ($sdkEntry -replace '^sdk\.dir\s*=\s*', '').Replace('\:', ':').Replace('\\', '\') }
        elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME }
        else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    $Adb = Join-Path $sdk 'platform-tools/adb.exe'
}
if (!(Test-Path -LiteralPath $Adb)) { throw 'No se encontró adb. Indica la ruta con -Adb o instala Platform Tools desde Android Studio.' }
$rawDevices = & $Adb devices
if ($LASTEXITCODE -ne 0) { throw 'No se pudo comunicar con Android. Revisa Platform Tools y vuelve a intentarlo.' }
$devices = @($rawDevices | ForEach-Object {
    if ($_ -match '^(\S+)\s+(device|unauthorized|offline)\s*$' -and $Matches[1] -notmatch '^emulator-') {
        [pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2] }
    }
})
if ($Serial) { $devices = @($devices | Where-Object Serial -eq $Serial) }
if ($devices.Count -eq 0) {
    throw 'Conecta el teléfono con un cable de datos, activa Depuración USB en sus Opciones de desarrollador y acepta la autorización en el teléfono. Después repite este comando.'
}
if ($devices.Count -ne 1) { throw 'Hay varios dispositivos. Selecciona uno con -Serial (identificador mostrado por adb devices).' }
$device = $devices[0]
if ($device.State -eq 'unauthorized') { throw 'Desbloquea el teléfono y acepta la autorización de depuración USB; después repite el comando.' }
if ($device.State -ne 'device') { throw 'El teléfono está offline. Desconecta y conecta el cable y espera a que Android termine de iniciar.' }

# Reuse the already downloaded local model without copying several GB or any secrets.
$aiRoot = Join-Path $ServerProject '.local-ai'
if (!(Test-Path -LiteralPath (Join-Path $aiRoot 'bin/ollama.exe'))) {
    $aiRoot = Join-Path (Split-Path -Parent $projectRoot) 'Script 2/.local-ai'
}
$ollama = Join-Path $aiRoot 'bin/ollama.exe'
try { $null = Invoke-RestMethod -Uri 'http://127.0.0.1:11434/api/tags' -TimeoutSec 2; $aiRunning = $true }
catch { $aiRunning = $false }
if (!$aiRunning -and (Test-Path -LiteralPath $ollama)) {
    $oldModelPath = $env:OLLAMA_MODELS; $oldHost = $env:OLLAMA_HOST; $oldCloud = $env:OLLAMA_NO_CLOUD
    try {
        $env:OLLAMA_MODELS = Join-Path $aiRoot 'models'; $env:OLLAMA_HOST = '127.0.0.1:11434'; $env:OLLAMA_NO_CLOUD = '1'
        Start-Process -FilePath $ollama -ArgumentList 'serve' -WindowStyle Hidden -RedirectStandardOutput (Join-Path $projectRoot 'ollama.stdout.log') -RedirectStandardError (Join-Path $projectRoot 'ollama.stderr.log') | Out-Null
    } finally { $env:OLLAMA_MODELS = $oldModelPath; $env:OLLAMA_HOST = $oldHost; $env:OLLAMA_NO_CLOUD = $oldCloud }
}

function Test-Redis {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $pending = $client.ConnectAsync('127.0.0.1', 6379)
        if (!$pending.Wait(1500)) { return $false }
        $stream = $client.GetStream()
        $stream.ReadTimeout = 1500
        $stream.WriteTimeout = 1500
        $ping = [System.Text.Encoding]::ASCII.GetBytes("*1`r`n" + '$4' + "`r`nPING`r`n")
        $stream.Write($ping, 0, $ping.Length)
        $buffer = New-Object byte[] 64
        $count = $stream.Read($buffer, 0, $buffer.Length)
        return [System.Text.Encoding]::ASCII.GetString($buffer, 0, $count).StartsWith('+PONG')
    } catch { return $false }
    finally { $client.Dispose() }
}
if (!(Test-Redis)) {
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'El servidor requiere Redis para renovar y revocar sesiones. Prepara Docker/Redis según README_Script_2.md.'
    }
    & docker compose -f (Join-Path $ServerProject 'docker-compose.yml') up -d redis
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar Redis. Abre Docker Desktop, espera a que su motor esté iniciado y repite este comando.' }
    $redisReady = $false
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        if (Test-Redis) { $redisReady = $true; break }
        Start-Sleep -Seconds 1
    }
    if (!$redisReady) { throw 'Redis no respondió. Revisa el contenedor sp500_redis en Docker Desktop.' }
}

function Test-RegistrationApi {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/v1/health/" -TimeoutSec 3
        if ($health.schema_version -ne 'android-services-v1') { return $false }
    } catch { return $false }
    try {
        $reply = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/api/v1/auth/register/" -Method Post -ContentType 'application/json' -Body '{}' -TimeoutSec 3 -UseBasicParsing
        return $false
    } catch {
        $reply = $_.Exception.Response
        # An empty form must be rejected with HTTP 400, without creating an account.
        if (!$reply -or [int]$reply.StatusCode -ne 400) { return $false }
        if ($_.ErrorDetails.Message -match 'username|password_confirm') { return $true }
        return $false
    }
}

if (!(Test-RegistrationApi)) {
    $python = Join-Path $ServerProject '.venv/Scripts/python.exe'
    if (!(Test-Path -LiteralPath $python)) { $python = Join-Path (Split-Path -Parent $projectRoot) 'Script 2/.venv/Scripts/python.exe' }
    $backend = Join-Path $ServerProject 'backend'
    $runner = Join-Path $ServerProject 'tools/run_android_backend.py'
    if (!(Test-Path -LiteralPath $python) -or !(Test-Path -LiteralPath $runner)) {
        throw "No está preparado el servidor de $ServerProject. Consulta README_Script_2.md o indica la carpeta preparada con -ServerProject."
    }
    $stdout = Join-Path $projectRoot 'android-usb-server.stdout.log'
    $stderr = Join-Path $projectRoot 'android-usb-server.stderr.log'
    $server = Start-Process -FilePath $python -ArgumentList @(('"' + $runner + '"'), '--port', "$Port") -WorkingDirectory $backend -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    Write-Host "Iniciando servidor local (proceso $($server.Id))..."
    $ready = $false
    for ($attempt = 0; $attempt -lt 15; $attempt++) {
        if (Test-RegistrationApi) { $ready = $true; break }
        if ($server.HasExited) { break }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw "La API de registro no está disponible. Revisa $stderr. No se cambió la conexión USB." }
}
& $Adb -s $device.Serial reverse "tcp:$Port" "tcp:$Port"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir la conexión USB hacia el servidor.' }
$forwarding = & $Adb -s $device.Serial reverse --list
if ($LASTEXITCODE -ne 0 -or !($forwarding -match "tcp:$Port\s+tcp:$Port")) {
    throw 'Android no confirmó el túnel USB. Desconecta y vuelve a conectar el teléfono.'
}
& (Join-Path $PSScriptRoot 'Configure-AndroidDevice.ps1') -Device Usb -Port $Port
Write-Host 'API de registro comprobada y túnel USB confirmado.'
Write-Host 'Ahora sincroniza Gradle y ejecuta app en ese teléfono desde Android Studio.'
Write-Host 'Tras desconectar o reiniciar el teléfono, repite este comando. La computadora y el servidor deben permanecer encendidos.'
