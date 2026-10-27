param([string]$OllamaPath = "")
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $OllamaPath) {
    $portable = Join-Path $projectRoot '.local-ai/bin/ollama.exe'
    if (Test-Path -LiteralPath $portable) { $OllamaPath = $portable }
    else { $OllamaPath = (Get-Command ollama -ErrorAction Stop).Source }
}
$env:OLLAMA_HOST = '127.0.0.1:11434'
$env:OLLAMA_MODELS = Join-Path $projectRoot '.local-ai/models'
$env:OLLAMA_NO_CLOUD = '1'
Write-Host 'IA local en 127.0.0.1:11434. Mantén esta ventana abierta.'
& $OllamaPath serve
