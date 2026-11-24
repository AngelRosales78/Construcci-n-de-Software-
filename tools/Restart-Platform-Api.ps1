$ErrorActionPreference='Stop'
$apiRepo=(Resolve-Path (Split-Path $PSScriptRoot -Parent)).Path
$apiProject=Split-Path $apiRepo -Parent
$apiState=Join-Path $apiProject 'presentation_runtime_sprint3'
$apiPython=Join-Path $apiProject 'validation_env/Scripts/python.exe'
$apiStateFile=Join-Path $apiState 'processes.json'
$apiProcesses=Get-Content -LiteralPath $apiStateFile -Raw | ConvertFrom-Json
if ((Resolve-Path $apiProcesses.repo).Path -ne $apiRepo) {throw 'El proceso pertenece a otro repositorio.'}
$apiProcess=Get-CimInstance Win32_Process -Filter ("ProcessId = "+$apiProcesses.api)
if (!$apiProcess -or $apiProcess.ExecutablePath -ne $apiPython -or !$apiProcess.CommandLine.Contains('--listen=127.0.0.1:58020')) {
    throw 'No se encontró el servidor esperado. Abre primero Iniciar Plataforma.cmd.'
}
$apiEnvironment=@{
    DB_HOST='127.0.0.1';DB_PORT='55432';DB_USER='postgres';DB_PASSWORD='';DB_NAME='sprint3_demo_validation';USE_POSTGRES='True';
    REDIS_URL='redis://127.0.0.1:56379/0';PYTHONPATH=$apiState;DJANGO_SETTINGS_MODULE='sprint3_settings';FRONTEND_URL='http://127.0.0.1:55193'
}
$apiPrevious=@{}
$apiNames=@($apiEnvironment.Keys)+@('PAYMENT_PROVIDER','MERCADOPAGO_ACCESS_TOKEN','MERCADOPAGO_WEBHOOK_SECRET','MERCADOPAGO_TEST_SELLER_ID','MERCADOPAGO_TEST_BUYER_EMAIL','MERCADOPAGO_RETURN_URL','MERCADOPAGO_WEBHOOK_URL',
    'EMAIL_BACKEND','EMAIL_HOST','EMAIL_PORT','EMAIL_USE_TLS','EMAIL_HOST_USER','EMAIL_HOST_PASSWORD','DEFAULT_FROM_EMAIL','NOTIFICATION_EMAIL_ENABLED','NOTIFICATION_ALLOWED_RECIPIENTS')
foreach($apiName in $apiNames) {$apiPrevious[$apiName]=[Environment]::GetEnvironmentVariable($apiName,'Process')}
try {
    foreach($apiName in $apiEnvironment.Keys) {[Environment]::SetEnvironmentVariable($apiName,$apiEnvironment[$apiName],'Process')}
    . (Join-Path $PSScriptRoot 'Load-MercadoPago-Test.ps1')
    $apiMail=Join-Path $apiState 'gmail.credential.xml'
    if (Test-Path -LiteralPath $apiMail) {
        $apiGmail=Import-Clixml -LiteralPath $apiMail
        $env:EMAIL_BACKEND='django.core.mail.backends.smtp.EmailBackend';$env:EMAIL_HOST='smtp.gmail.com';$env:EMAIL_PORT='587';$env:EMAIL_USE_TLS='True'
        $env:EMAIL_HOST_USER=$apiGmail.UserName;$env:EMAIL_HOST_PASSWORD=$apiGmail.GetNetworkCredential().Password;$env:DEFAULT_FROM_EMAIL=$apiGmail.UserName
        $env:NOTIFICATION_EMAIL_ENABLED='True';$env:NOTIFICATION_ALLOWED_RECIPIENTS=$apiGmail.UserName
    }
    Stop-Process -Id $apiProcesses.api
    $apiNew=Start-Process -FilePath $apiPython -ArgumentList '-B','-m','waitress','--listen=127.0.0.1:58020','--threads=4','config.wsgi:application' -WorkingDirectory (Join-Path $apiRepo 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $apiState 'payment-api.out') -RedirectStandardError (Join-Path $apiState 'payment-api.err') -PassThru
    $apiProcesses.api=$apiNew.Id
    $apiProcesses | ConvertTo-Json | Set-Content -LiteralPath $apiStateFile -Encoding UTF8
    Write-Host 'Servidor actualizado. La web y las cuentas se conservan.'
} finally {
    foreach($apiName in $apiPrevious.Keys) {[Environment]::SetEnvironmentVariable($apiName,$apiPrevious[$apiName],'Process')}
    $apiGmail=$null
}
