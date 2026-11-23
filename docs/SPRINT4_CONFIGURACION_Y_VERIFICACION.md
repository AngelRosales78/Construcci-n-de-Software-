# Configuración y comprobación de la entrega

## Arranque local

Desde Windows, abre `Iniciar Plataforma.cmd`. También se puede ejecutar `tools/Start-Platform.ps1`. Se conservan los nombres históricos de algunas carpetas y scripts para no perder datos ni accesos anteriores. La pantalla del producto usa el nombre de la plataforma.

El arranque aplica las migraciones, conserva los datos ya actualizados, inicia la web, API y trabajadores, y prepara el motor local si está instalado. El servicio web se abre en `http://127.0.0.1:55193/login`; la API escucha en el puerto 58020. La base de esta computadora sigue aislada en `sprint3_demo_validation`. Este arranque local no publica la plataforma en internet.

Los archivos privados `demo_credentials.json`, `sprint4_admin_credentials.json` y `gmail.credential.xml` permanecen en la carpeta `presentation_runtime_sprint3`, fuera del repositorio. El correo del arranque está limitado a la cuenta propia autorizada. La aceptación SMTP acredita transporte; la recepción en la bandeja depende del proveedor y no se afirma sin observarla.

## Activar Push cuando se disponga de Firebase

1. Obtener del proyecto Firebase la configuración Android para `com.example.sp500auth` y guardarla como `android/app/google-services.json`.
2. Obtener la credencial de servicio del mismo proyecto y guardarla fuera del repositorio como `presentation_runtime_sprint3/firebase-credentials.json`. No pegar su contenido en código, documentos ni commits.
3. Reiniciar los servicios con los scripts del proyecto. El arranque detecta la credencial local; en otro entorno se configura `FCM_CREDENTIALS_FILE` con su ruta privada.
4. Volver a compilar e instalar Android, iniciar sesión, permitir notificaciones y habilitar el canal en Preferencias. La compilación toma la configuración Android del archivo local.
5. Verificar recepción real en primer plano, segundo plano y cierre normal, apertura de la empresa, cambio de cuenta, cierre de sesión, permiso denegado y retirada de un token inválido. Conservar la evidencia antes de dar por aceptada HU12_02.

La instalación de `firebase-admin` y la compilación de Firebase Messaging no acreditan recepción real. Sin ambos archivos, el canal permanece no disponible y el resto de la plataforma funciona.

En esta ejecución tampoco se pudo abrir la consola para revisar proyectos de la cuenta: la herramienta de navegación falló al iniciar, incluso después de reiniciarla. No se afirma que la cuenta carezca de proyectos; lo comprobado es que los archivos de configuración no están disponibles en esta instalación.

## Pruebas reproducibles

Servidor: instalar `backend/requirements.txt`, usar PostgreSQL y Redis de validación y ejecutar `python -m pytest` desde `backend`. No ejecutar pruebas contra datos de usuarios reales. Las pruebas de concurrencia del proyecto requieren PostgreSQL; SQLite no sustituye esa comprobación.

Web: desde `frontend`, ejecutar `npm test` y `npm run build`. El recorrido visual real se ejecuta con `node tools/verify_sprint4_jury.cjs` desde la raíz del repositorio, con servicios y cuentas locales preparados y Edge/Playwright disponibles. Sus capturas y resultados se guardan fuera del repositorio en `output/sprint4_jury`.

Android: Java 17 y SDK instalado, `gradlew :app:assembleDebug :app:testDebugUnitTest -PapiBaseUrl=http://10.0.2.2:58020/api/v1/`. Esa dirección corresponde al emulador de esta computadora. La versión de distribución requiere una API HTTPS; solo la compilación debug admite HTTP local.

Las pruebas `Sprint4LiveUiTest` usan un archivo local excluido de Git en `android/app/src/androidTest/assets/sprint4-live.json`: objeto `admin` con los campos `username` y `password` de la cuenta local autorizada. No incorporarlo al repositorio. Con servicios activos y un emulador estable, compilar `assembleDebugAndroidTest` y ejecutar la clase con AndroidJUnitRunner. Si falta ese archivo, las pruebas en vivo se omiten explícitamente; no se cuentan como aprobadas.

En esta computadora hubo bloqueos del sistema del emulador durante el arranque. Se utilizó una imagen de datos de verificación separada, sin borrar el dispositivo original, y precompilación de los paquetes para comprobar el arranque normal. Referencia de los directorios y opciones de imagen: [documentación oficial de Android Emulator](https://developer.android.com/studio/run/emulator-commandline).

## Antes de una publicación real

Configurar un `SECRET_KEY` persistente y privado, los dominios permitidos, HTTPS, el alojamiento y las credenciales de los servicios. El arranque local ya conserva su clave privada entre reinicios. La integración actual de Pro registra activaciones sin cargo; una pasarela bancaria real no forma parte de esta entrega y no debe anunciarse como operativa.
