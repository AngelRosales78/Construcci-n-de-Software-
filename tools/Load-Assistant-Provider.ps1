# Dot-source when starting the API; absence preserves the existing local provider.
$assistantConfigFile=Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'presentation_runtime_sprint3/assistant-provider.xml'
if (Test-Path -LiteralPath $assistantConfigFile) {
    $assistantLocal=Import-Clixml -LiteralPath $assistantConfigFile
    $env:ASSISTANT_API_BASE_URL=$assistantLocal.BaseURL
    $env:ASSISTANT_MODEL=$assistantLocal.Model
    $env:ASSISTANT_API_KEY=[Net.NetworkCredential]::new('', $assistantLocal.ApiKey).Password
    $assistantLocal=$null
}
