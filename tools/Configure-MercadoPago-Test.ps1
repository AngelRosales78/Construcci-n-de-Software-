$ErrorActionPreference='Stop'
$mpRepo=Split-Path $PSScriptRoot -Parent
$mpProject=Split-Path $mpRepo -Parent
$mpRuntime=Join-Path $mpProject 'presentation_runtime_sprint3'
$mpPython=Join-Path $mpProject 'validation_env/Scripts/python.exe'
Write-Host 'Conectar Mercado Pago SOLO en modo de prueba. No se crean pagos durante esta configuración.'
$mpSellerId=Read-Host 'ID del vendedor de prueba'
$mpBuyerEmail=Read-Host 'Correo del comprador de prueba (termina en @testuser.com)'
$mpPublicBase=(Read-Host 'Dirección HTTPS pública de las notificaciones (sin ruta)').TrimEnd('/')
$mpAccess=Read-Host 'Access Token de la aplicación del vendedor de prueba' -AsSecureString
$mpSecret=Read-Host 'Clave secreta de Webhooks' -AsSecureString
if ($mpSellerId -notmatch '^[0-9]{1,40}$' -or $mpBuyerEmail -notmatch '^[^@\s]+@testuser\.com$') {throw 'Revisa las cuentas de prueba.'}
$mpAddress=[Uri]$mpPublicBase
if ($mpAddress.Scheme -ne 'https' -or $mpAddress.AbsolutePath -ne '/' -or $mpAddress.Query -or $mpAddress.Fragment) {throw 'Se requiere el origen HTTPS, sin rutas ni parámetros.'}
$mpPrevious=@{}
$mpValues=@{
    PAYMENT_PROVIDER='mercadopago_test'; MERCADOPAGO_TEST_SELLER_ID=$mpSellerId; MERCADOPAGO_TEST_BUYER_EMAIL=$mpBuyerEmail;
    MERCADOPAGO_RETURN_URL=$mpPublicBase+'/api/v1/payments/mercadopago/return/';
    MERCADOPAGO_WEBHOOK_URL=$mpPublicBase+'/api/v1/payments/mercadopago/webhook/';
    MERCADOPAGO_ACCESS_TOKEN=[Net.NetworkCredential]::new('', $mpAccess).Password;
    MERCADOPAGO_WEBHOOK_SECRET=[Net.NetworkCredential]::new('', $mpSecret).Password
}
try {
    foreach($mpName in $mpValues.Keys) {
        $mpPrevious[$mpName]=[Environment]::GetEnvironmentVariable($mpName,'Process')
        [Environment]::SetEnvironmentVariable($mpName,$mpValues[$mpName],'Process')
    }
    Push-Location (Join-Path $mpRepo 'backend')
    try {
        & $mpPython -B manage.py check_mercadopago_test --verify-seller
        if ($LASTEXITCODE -ne 0) {throw 'No se guardó la configuración: la verificación del vendedor falló.'}
    } finally {Pop-Location}
    New-Item -ItemType Directory -Force -Path $mpRuntime | Out-Null
    [PSCustomObject]@{SellerId=$mpSellerId;BuyerEmail=$mpBuyerEmail;PublicBase=$mpPublicBase;AccessToken=$mpAccess;WebhookSecret=$mpSecret} |
        Export-Clixml -LiteralPath (Join-Path $mpRuntime 'mercadopago-test.xml')
    Write-Host 'Configuración cifrada guardada para tu usuario de Windows. Reinicia el servidor de la plataforma para cargarla.'
    Write-Host ('Configura la URL de notificaciones en Mercado Pago: '+$mpPublicBase+'/api/v1/payments/mercadopago/webhook/')
} finally {
    foreach($mpName in $mpPrevious.Keys) {[Environment]::SetEnvironmentVariable($mpName,$mpPrevious[$mpName],'Process')}
    $mpValues=$null; $mpAccess=$null; $mpSecret=$null
}
