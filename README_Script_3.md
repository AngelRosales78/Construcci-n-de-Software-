# Script 3 Android

Proyecto completo basado en Script 2. Las modificaciones de este sprint están en Android y su documentación; no se implementan tareas Web ni se cambian los servicios del servidor.

## Abrir y ejecutar

1. Abrir `C:\Users\ADMIN\Ciclo4\ConstSoftware\Script 3\android` en Android Studio.
2. Seleccionar JDK 17 como Gradle JDK, sincronizar y ejecutar la configuración app con un emulador.
3. Mantener iniciado el servidor de desarrollo. El emulador usa `http://10.0.2.2:8000/api/v1/`. Puede utilizarse el servidor y la base ya preparados de Script 2. No deben iniciarse dos servidores en el mismo puerto.
4. Iniciar sesión, abrir Comparar desde la barra de Explorar o desde la ficha de una empresa y elegir A y B.
5. Consultar un ejercicio con balances disponibles. La selección y el ejercicio se conservan al recrear el estado; los resultados se vuelven a consultar para evitar datos antiguos.

No se entrega un APK. El proyecto conserva Gradle Wrapper y las dependencias Android de la base. Los entornos virtuales, bases personales, credenciales, cachés y modelos locales no se copian ni se incluyen en commits. Si se quiere preparar un servidor independiente en Script 3, seguir la restauración de datos descrita en README_Script_2.md y los scripts de tools.

## Historias y tareas Android

| Historia | Tarea | Entrega |
|---|---|---|
| HU08_01 | T01.1 | Dos buscadores, sugerencias, empresas distintas, quitar, intercambio y restauración de selección. |
| HU08_01 | T01.2 | Contrato Retrofit, consulta por A/B y ejercicio, carga, error, reintento y rechazo de respuestas antiguas. |
| HU08_02 | T02.1 | Tabla desplazable de cinco pilares con notas, periodos, versiones, fuentes y resultado textual. |
| HU08_02 | T02.2 | Validaciones de comparabilidad, datos ausentes, permisos y compatibilidad con los pilares reales del servidor existente; pruebas automatizadas. |

La implementación Android no acredita por sí sola la aceptación integral de las historias en Web y servidor. El servicio compare aún no existe en la copia del backend: Android consulta los pilares reales de cada empresa y deja visible el aviso de consulta independiente. Las notas individuales se muestran; ganadores y diferencias requieren el servicio compartido. No se inventan ni se calculan localmente.

El quinto pilar actual es Crecimiento, de acuerdo con el servidor de Script 2. Los nombres y las metodologías se mantienen coherentes con esa base.

## Verificación

Ejecutar desde android con JDK 17:

```powershell
.\gradlew.bat testDebugUnitTest compileDebugKotlin compileDebugAndroidTestKotlin
```

Con emulador conectado, ejecutar también las pruebas de interfaz y el recorrido manual de `docs/android/SPRINT3_COMPARADOR.md`. El contrato para coordinar el servicio con Diego está en `docs/android/CONTRATO_COMPARADOR.md`. Las respuestas artificiales de pruebas están aisladas en test y androidTest; no se usan como datos de producción.

Verificación del 30/10/2026: compilación de aplicación y de pruebas de interfaz aprobada; 91 pruebas JVM descubiertas, 88 aprobadas y 3 integraciones opcionales omitidas. Las 23 nuevas pruebas del comparador aprobaron. La interfaz no se ejecutó en dispositivo porque no había ninguno conectado. El resultado resumido se registra en `docs/android/evidence/sprint3_comparador.json`.
