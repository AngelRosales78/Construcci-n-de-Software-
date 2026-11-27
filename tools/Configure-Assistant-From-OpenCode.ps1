# Reuse the selected Open WebUI connection. Only encrypted credentials leave OpenCode.
$ErrorActionPreference='Stop'
$assistantProject=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$assistantState=Join-Path $assistantProject 'presentation_runtime_sprint3'
$assistantSource=Join-Path $env:USERPROFILE '.config/opencode/opencode.jsonc'
$assistantEditor=Get-Content -LiteralPath $assistantSource -Raw | ConvertFrom-Json
$assistantProvider=$assistantEditor.provider.'open-webui'
if (!$assistantProvider) { $assistantProvider=$assistantEditor.providers.'open-webui' }
$assistantOptions=$assistantProvider.options
if (!$assistantOptions) { $assistantOptions=$assistantProvider.settings }
$assistantSelected=[string]$assistantEditor.model
if (!$assistantSelected.StartsWith('open-webui/')) { throw 'Selecciona primero el modelo de Open WebUI en OpenCode.' }
$assistantModel=$assistantSelected.Substring('open-webui/'.Length)
$assistantBase=[string]$assistantOptions.baseURL
$assistantUri=[uri]$assistantBase
if ($assistantUri.Scheme -notin @('http','https') -or $assistantUri.UserInfo -or $assistantUri.Query -or $assistantUri.Fragment) { throw 'Dirección del proveedor no válida.' }
$assistantKey=[string]$assistantOptions.apiKey
if ($assistantKey -match '^\{env:([^}]+)\}$') { $assistantKey=[Environment]::GetEnvironmentVariable($Matches[1]) }
if ($assistantKey -match '^\{file:([^}]+)\}$') { $assistantKey=(Get-Content -LiteralPath $Matches[1] -Raw).Trim() }
if (!$assistantKey) { throw 'No se encontró la clave del proveedor seleccionado.' }
New-Item -ItemType Directory -Force -Path $assistantState | Out-Null
$assistantSecure=ConvertTo-SecureString -String $assistantKey -AsPlainText -Force
[pscustomobject]@{BaseURL=$assistantBase.TrimEnd('/');Model=$assistantModel;ApiKey=$assistantSecure} |
    Export-Clixml -LiteralPath (Join-Path $assistantState 'assistant-provider.xml')
$assistantKey=$null;$assistantSecure=$null;$assistantOptions=$null;$assistantProvider=$null;$assistantEditor=$null
Write-Host 'Conexión del asistente preparada. La clave queda cifrada fuera de Git.'
