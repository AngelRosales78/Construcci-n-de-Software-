$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$project = Split-Path $repo -Parent
$state = Join-Path $project 'presentation_runtime_sprint3'
function Platform-PortOpen([int]$port) {
    $socket = [Net.Sockets.TcpClient]::new()
    try { $socket.Connect('127.0.0.1', $port); return $true }
    catch [Net.Sockets.SocketException] { return $false }
    finally { $socket.Dispose() }
}
if (!(Platform-PortOpen 58020) -or !(Platform-PortOpen 55193)) {
    & (Join-Path $PSScriptRoot 'Start-Sprint3-Demo.ps1')
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
$ai = [Net.Sockets.TcpClient]::new()
$aiRunning = $false
try { $ai.Connect('127.0.0.1', 58110); $aiRunning = $true }
catch [Net.Sockets.SocketException] { }
finally { $ai.Dispose() }
if (!$aiRunning -and (Test-Path -LiteralPath (Join-Path $project 'local_ai/provenance.json'))) {
    & (Join-Path $PSScriptRoot 'Start-LocalAI.ps1')
}
Write-Host 'Plataforma disponible en http://127.0.0.1:55193/login'
