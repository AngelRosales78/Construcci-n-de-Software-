$ErrorActionPreference = 'Stop'
$project = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$localAiRoot = Join-Path $project 'local_ai'
$localAiExe = Join-Path $localAiRoot 'llama/llama-server.exe'
$localAiModel = Join-Path $localAiRoot 'qwen2.5-1.5b-instruct-q4_k_m.gguf'
foreach ($localAiFile in @($localAiExe,$localAiModel)) { if (!(Test-Path -LiteralPath $localAiFile)) { throw 'Ejecutar tools/prepare_local_ai.py antes de iniciar IA.' } }
$localAiSocket = [Net.Sockets.TcpClient]::new()
try { $localAiSocket.Connect('127.0.0.1',58110); throw 'El puerto local 58110 está ocupado; verificar el servicio existente.' }
catch [Net.Sockets.SocketException] { }
finally { $localAiSocket.Dispose() }
$localAiArgs = @('-m',('"' + $localAiModel + '"'),'--host','127.0.0.1','--port','58110','--ctx-size','8192','--threads','6','--parallel','1','--n-gpu-layers','0','--alias','sp500-local')
$localAiProcess = Start-Process -FilePath $localAiExe -ArgumentList $localAiArgs -WindowStyle Hidden -RedirectStandardOutput (Join-Path $localAiRoot 'server.out') -RedirectStandardError (Join-Path $localAiRoot 'server.err') -PassThru
@{pid=$localAiProcess.Id;model=$localAiModel;port=58110} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localAiRoot 'process.json') -Encoding UTF8
Write-Host 'IA local iniciada; comprobar http://127.0.0.1:58110/health antes de generar texto.'
