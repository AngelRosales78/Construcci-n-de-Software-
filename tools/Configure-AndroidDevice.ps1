param(
    [ValidateSet('Usb', 'Phone', 'Emulator')][string]$Device = 'Usb',
    [string]$ServerAddress,
    [ValidateRange(1, 65535)][int]$Port = 8000
)
$ErrorActionPreference = 'Stop'
if ($Device -eq 'Usb') { $ServerAddress = '127.0.0.1' }
elseif ($Device -eq 'Emulator') { $ServerAddress = '10.0.2.2' }
else {
    $parsedAddress = $null
    if (![System.Net.IPAddress]::TryParse($ServerAddress, [ref]$parsedAddress) -or
        $parsedAddress.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork -or
        [System.Net.IPAddress]::IsLoopback($parsedAddress) -or $ServerAddress -eq '10.0.2.2') {
        throw 'Para Phone indica la IPv4 Wi-Fi de la computadora en -ServerAddress.'
    }
}
$projectRoot = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $projectRoot 'android/local.properties'
$lines = if (Test-Path -LiteralPath $propertiesPath) { [System.IO.File]::ReadAllLines($propertiesPath) } else { @() }
$keptLines = @($lines | Where-Object { $_ -notmatch '^\s*apiBaseUrl\s*[:=]' })
$url = "http://${ServerAddress}:${Port}/api/v1/"
[System.IO.File]::WriteAllLines($propertiesPath, @($keptLines) + "apiBaseUrl=$url", [System.Text.UTF8Encoding]::new($false))
Write-Host "Destino: $Device. API: $url"
Write-Host 'Sincroniza Gradle y vuelve a ejecutar app desde Android Studio sobre el dispositivo.'
if ($Device -eq 'Phone') {
    Write-Host 'Computadora y telefono deben estar en la misma red. Mantén el servidor iniciado.'
    Write-Host "Al iniciar el servidor, incluye $ServerAddress en ANDROID_ALLOWED_HOSTS."
}
if ($Device -eq 'Usb') { Write-Host 'Ejecuta tools/Start-AndroidUsb.ps1 con el telefono conectado y la depuracion USB autorizada.' }
