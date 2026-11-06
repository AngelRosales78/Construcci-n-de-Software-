# Teléfono físico y huella

## Conexión por cable USB (configuración elegida)

No requiere que el teléfono y la computadora compartan Wi-Fi. Requiere un cable de datos y el teléfono autorizado para depuración USB.

1. Activa **Opciones de desarrollador → Depuración USB** en el teléfono. Conecta el cable, desbloquea el teléfono y acepta su autorización para esta computadora.
2. Abre Docker Desktop y espera a que su motor esté listo. Desde PowerShell en Script 3 ejecuta `./tools/Start-AndroidUsb.ps1`. Si hay varios teléfonos, indica `-Serial`.
3. El comando comprueba el dispositivo, inicia Redis si hace falta, comprueba su respuesta PONG, inicia el servidor preparado de Script 2 si hace falta, comprueba la ruta real de registro sin crear cuentas y aplica `adb reverse tcp:8000 tcp:8000`. Solo informa éxito si Android confirma el túnel.
4. Abre `Script 3/android`, sincroniza Gradle y selecciona el teléfono en Android Studio. Ejecuta **app** para actualizar la instalación con la dirección USB `http://127.0.0.1:8000/api/v1/`.
5. Regístrate; al terminar debes volver al login. Inicia sesión y activa la huella siguiendo el apartado inferior.

Cada vez que desconectes/reinicies el teléfono o reinicies la computadora, vuelve a ejecutar el comando con el cable conectado. Si tienes definida otra `apiBaseUrl` en las propiedades de Gradle, elimínala para usar la configuración local. El script usa el servidor y la base existentes de Script 2; no sustituye cuentas ni datos. Redis/Docker debe estar iniciado para las operaciones del servidor que lo requieran, como renovación y revocación. La huella y la sesión siguen necesitando una API disponible.

No hay un teléfono conectado durante la preparación: la instalación, el registro y el sensor deben verificarse en el dispositivo real para dar por cerrado el recorrido completo.

## Conexión del teléfono

`10.0.2.2` es la dirección de la computadora vista desde el emulador de Android Studio. No funciona desde un teléfono físico. El servidor local requiere la computadora encendida y ambos dispositivos en una red que permita comunicarse entre ellos; los datos móviles no permiten acceder directamente a este servidor.

1. Consulta la IPv4 Wi-Fi de la computadora con `ipconfig`.
2. Desde la raíz de Script 3 ejecuta `./tools/Configure-AndroidDevice.ps1 -Device Phone -ServerAddress <IPv4-de-la-computadora>`.
3. En otra consola, desde Script 2, inicia el servidor existente incluyendo esa IPv4 en los hosts permitidos:

```powershell
$env:ANDROID_ALLOWED_HOSTS = 'localhost,127.0.0.1,10.0.2.2,<IPv4-de-la-computadora>'
./tools/Start-AndroidServer.ps1 -Python ./.venv/Scripts/python.exe
```

4. En el navegador del teléfono abre `http://<IPv4-de-la-computadora>:8000/api/v1/auth/register/`. Una respuesta HTTP 405 o la página de la API acredita conexión; una espera agotada indica un problema de red o del servidor, antes del registro.
5. Abre `Script 3/android` en Android Studio, sincroniza y ejecuta `app` sobre el teléfono conectado. La instalación anterior conserva su dirección y debe actualizarse.

La configuración se guarda en `android/local.properties`, excluido de Git. No se publica la IP personal. `-PapiBaseUrl` o la propiedad Gradle del mismo nombre tienen prioridad: elimina cualquier valor antiguo si sustituye esta configuración. Si cambia la IP Wi-Fi, repite la configuración y reinstala desde Android Studio. Para volver al emulador ejecuta `./tools/Configure-AndroidDevice.ps1 -Device Emulator` y vuelve a ejecutar `app`.

Si el navegador tampoco conecta, comprueba el servidor, la misma red, las reglas de entrada de Windows y el aislamiento de clientes del Wi-Fi. El proyecto no modifica el firewall. Para comprobar por USB, Android Studio también permite usar `adb reverse tcp:8000 tcp:8000` y compilar con `-PapiBaseUrl=http://127.0.0.1:8000/api/v1/`; esa conexión depende del cable y de la autorización de depuración del teléfono.

## Activar y verificar la huella

1. Registra previamente una huella en los Ajustes de Android y configura el bloqueo del teléfono.
2. Crea tu cuenta y accede una vez con usuario y contraseña, con el servidor disponible.
3. Abre **Mi cuenta → Acceso por huella**, activa el interruptor y confirma la huella en el diálogo de Android.
4. Pulsa **Bloquear y usar huella**. Desde el login pulsa **Acceso con Huella Dactilar**. Solo una autenticación biométrica correcta y una sesión validada por el servidor permiten entrar.
5. Comprueba que Cancelar o una huella fallida mantienen el login. Si la sesión ha vencido o no se puede verificar, usa la contraseña o restablece la conexión.
6. **Cerrar sesión** revoca la sesión y elimina los tokens y la preferencia de huella de ese dispositivo. Para usar huella de nuevo, entra con contraseña y actívala otra vez. Bloquear conserva la sesión; no equivale a cerrar sesión.

La app no recibe la huella del sensor. Android realiza la comprobación del sistema y los tokens de sesión se guardan cifrados. El dispositivo debe disponer de biometría fuerte compatible; la pantalla distingue sensor ausente, sensor temporalmente no disponible y huella no registrada. La verificación con sensor real queda pendiente cuando no hay un teléfono conectado a Android Studio.
