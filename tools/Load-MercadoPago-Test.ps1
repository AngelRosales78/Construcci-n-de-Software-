# Dot-source before starting the local API. Secrets remain outside the repository.
$mpConfigFile=Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'presentation_runtime_sprint3/mercadopago-test.xml'
if (Test-Path -LiteralPath $mpConfigFile) {
    $mpLocal=Import-Clixml -LiteralPath $mpConfigFile
    $env:PAYMENT_PROVIDER='mercadopago_test'
    $env:MERCADOPAGO_TEST_SELLER_ID=$mpLocal.SellerId
    $env:MERCADOPAGO_TEST_BUYER_EMAIL=$mpLocal.BuyerEmail
    $env:MERCADOPAGO_RETURN_URL=$mpLocal.PublicBase+'/api/v1/payments/mercadopago/return/'
    $env:MERCADOPAGO_WEBHOOK_URL=$mpLocal.PublicBase+'/api/v1/payments/mercadopago/webhook/'
    $env:MERCADOPAGO_ACCESS_TOKEN=[Net.NetworkCredential]::new('', $mpLocal.AccessToken).Password
    $env:MERCADOPAGO_WEBHOOK_SECRET=[Net.NetworkCredential]::new('', $mpLocal.WebhookSecret).Password
    $mpLocal=$null
}
