param([string]$Sender)
$ErrorActionPreference = 'Stop'
$project = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$state = Join-Path $project 'presentation_runtime_sprint3'
if (!$Sender) { $Sender = Read-Host 'Direccion de Gmail remitente' }
if ($Sender -notmatch '^[^\s@]+@(gmail\.com|googlemail\.com)$') { throw 'Indica una direccion personal de Gmail valida.' }
Write-Host 'Configurar Gmail para comprobantes de la demo'
Write-Host 'Crea una contrasena de aplicacion en https://myaccount.google.com/apppasswords'
Write-Host 'Google requiere verificacion en dos pasos para crearla.'
Write-Host 'Al continuar se enviara un mensaje de prueba a esta misma direccion.'
$password = Read-Host 'Introduce la contrasena de aplicacion de 16 letras, sin espacios' -AsSecureString
if ($password.Length -ne 16) { throw 'Se esperan las 16 letras de la contrasena de aplicacion.' }
$credential = [PSCredential]::new($Sender, $password)
$smtp = [Net.Mail.SmtpClient]::new('smtp.gmail.com', 587)
$smtp.EnableSsl = $true
$smtp.Timeout = 15000
$smtp.Credentials = [Net.NetworkCredential]::new($Sender, $password)
$message = [Net.Mail.MailMessage]::new($Sender, $Sender)
$message.Subject = 'Prueba de correo de S&P 500 Finance'
$message.Body = 'La conexion de correo para los comprobantes simulados fue verificada. Esta prueba no activa ni cobra una suscripcion.'
try {
    $smtp.Send($message)
    New-Item -ItemType Directory -Force -Path $state | Out-Null
    $temporary = Join-Path $state 'gmail.credential.pending.xml'
    $credential | Export-Clixml -LiteralPath $temporary
    Move-Item -LiteralPath $temporary -Destination (Join-Path $state 'gmail.credential.xml') -Force
    Write-Host 'Google acepto el mensaje. Comprueba la bandeja de entrada y spam.'
    Write-Host 'La credencial queda cifrada para tu usuario de Windows y fuera del repositorio.'
    Write-Host 'Reinicia la demo para aplicar Gmail.'
} catch {
    throw 'No se confirmo la prueba SMTP. Revisa conexion, direccion, verificacion en dos pasos y contrasena de aplicacion.'
} finally {
    $message.Dispose()
    $smtp.Dispose()
    $password.Dispose()
}
