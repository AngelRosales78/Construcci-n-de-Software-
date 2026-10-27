$ErrorActionPreference = 'Stop'
$project = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$localAiRoot = Join-Path $project 'local_ai'
$localAiVersion = (Get-Content -LiteralPath (Join-Path $localAiRoot 'provenance.json') -Raw | ConvertFrom-Json).engine
if ($localAiVersion -notmatch '^b[0-9]+$') { throw 'Versión local no reconocida' }
$localAiExe = Join-Path $localAiRoot "llama/$localAiVersion/llama-server.exe"
$localAiModel = Join-Path $localAiRoot 'Qwen3-1.7B-Q8_0.gguf'
foreach ($localAiFile in @($localAiExe,$localAiModel)) { if (!(Test-Path -LiteralPath $localAiFile)) { throw 'Ejecutar tools/prepare_local_ai.py antes de iniciar IA.' } }
$localAiProvenance = Get-Content -LiteralPath (Join-Path $localAiRoot 'provenance.json') -Raw | ConvertFrom-Json
if ((Get-FileHash -LiteralPath $localAiModel -Algorithm SHA256).Hash.ToLowerInvariant() -ne $localAiProvenance.model_sha256) { throw 'El modelo no coincide con el SHA-256 verificado.' }
$localAiSocket = [Net.Sockets.TcpClient]::new()
try { $localAiSocket.Connect('127.0.0.1',58110); throw 'El puerto local 58110 está ocupado; verificar el servicio existente.' }
catch [Net.Sockets.SocketException] { }
finally { $localAiSocket.Dispose() }
$localAiArgs = @('-m',('"' + $localAiModel + '"'),'--host','127.0.0.1','--port','58110','--ctx-size','8192','--threads','6','--parallel','1','--n-gpu-layers','0','--alias','sp500-local-qwen3')
$localAiProcess = Start-Process -FilePath $localAiExe -ArgumentList $localAiArgs -WindowStyle Hidden -RedirectStandardOutput (Join-Path $localAiRoot 'server.out') -RedirectStandardError (Join-Path $localAiRoot 'server.err') -PassThru
@{pid=$localAiProcess.Id;model=$localAiModel;port=58110} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localAiRoot 'process.json') -Encoding UTF8
Write-Host 'IA local iniciada; comprobar http://127.0.0.1:58110/health antes de generar texto.'
