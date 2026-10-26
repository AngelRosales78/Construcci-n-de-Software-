# Script 2 — Android

Proyecto completo creado a partir de Script 1 en una carpeta independiente, conservando Rosales y el código local del dashboard. Script 1 no se modificó. Se incorporó el servidor/web de origin/diego/sprint2-dia3-radar-puntaje para usar su contrato financiero implementado.

## Ejecutar en Android Studio

1. Abrir la carpeta **android** de este proyecto, no android de Script 1.
2. En Settings > Build, Execution, Deployment > Build Tools > Gradle seleccionar **JDK 17** y sincronizar. Se incluye Gradle Wrapper 8.4.
3. Preparar e iniciar el servidor como se indica abajo. Crear una cuenta desde la pantalla de registro de Android.
4. Elegir un emulador y ejecutar la configuración **app**. No hay un APK de entrega.
5. Buscar **MSFT**, abrir la ficha y elegir **Cinco pilares y puntaje**. Consultar **2024** para reproducir los datos anuales importados. El catálogo tiene 503 símbolos; la cobertura financiera anual disponible está limitada a MSFT.

## Servidor de desarrollo separado

La base de Script 1 se copió localmente, pero no se usa para las pruebas ni para este arranque. El perfil config.android_local utiliza android-local.sqlite3. Los datos y las credenciales locales no se publican.

Desde la raíz de Script 2:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
docker compose up -d redis
.\tools\Start-AndroidServer.ps1 -Prepare -Python .\.venv\Scripts\python.exe
```

La primera preparación migra la base independiente e importa el catálogo, los ejercicios anuales y los cierres históricos incluidos. No descarga precios nuevos ni sustituye datos ausentes por cifras inventadas. El comando puede repetirse; las importaciones son idempotentes.

Redis se necesita para la renovación y revocación de sesiones del servidor existente. Si solo falta la caché de búsqueda, el perfil local evita esperas largas. El correo de recuperación se imprime en la consola local.

La app usa http://10.0.2.2:8000/api/v1/ para el emulador. Para un teléfono físico definir apiBaseUrl en las propiedades de Gradle con la IP accesible del servidor y el sufijo /api/v1/; añadir esa IP a ANDROID_ALLOWED_HOSTS antes de iniciar el servidor.

## Funciones y límites actuales

- Sesión real y restauración verificada, biometría después de autenticarse, tokens cifrados y renovación sincronizada.
- Catálogo paginado, búsqueda, ficha e histórico del servidor.
- Liquidez, rentabilidad, solvencia, eficiencia con serie anual y crecimiento; radar y puntaje con componentes, fuentes, versiones y estados ausentes.
- Pantallas de escenarios a doce meses, tres percentiles, controles y restablecimiento: cliente implementado, **servidor de simulaciones pendiente**.
- Pantallas de resumen de IA, cinco explicaciones, fortalezas/riesgos, fuentes y selector sencillo/técnico: cliente implementado, **servicio validado y proveedor de IA pendientes**.

Los contratos pendientes están en docs/android/CONTRATOS_PENDIENTES.md. No se presentan respuestas de ejemplo en la aplicación. Una pantalla preparada sin su servidor no se declara historia integral terminada.

## Verificación

- 56 pruebas JVM de Android aprobadas, incluidas tres contra Django real con una base aislada.
- 110 pruebas seleccionadas del servidor aprobadas con las versiones declaradas de Django/DRF/SimpleJWT; tres comprobaciones exclusivas de PostgreSQL omitidas al usar SQLite.
- Dos pruebas de interfaz compiladas. No se ejecutaron en dispositivo porque no había uno conectado.
- Evidencias sin credenciales en docs/android/evidence/ y matriz de alcance en docs/android/MATRIZ.md.

Desde android, las pruebas locales se ejecutan con gradlew.bat testDebugUnitTest. Los tres casos de servidor real solo se habilitan mediante tools/verify_android_server.py; no deben recibir credenciales de una base personal.
