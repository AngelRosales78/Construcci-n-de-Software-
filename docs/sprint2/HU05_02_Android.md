# HU05_02 — Ángel: T03 Android y parte Android de T04

## Implementación

Pantalla de liquidez accesible desde Detalle de activo > Consultar pilar de liquidez. La consulta tiene su propia selección explícita de empresa (MSFT por defecto) y ejercicio fiscal (2024 por defecto); no afirma que corresponda al detalle AAPL de demostración existente.

Presenta liquidez corriente y prueba ácida, componentes, fórmula, periodo, versión de metodología y procedencia SEC cuando el servidor los entrega. Distingue carga, error recuperable, dato no disponible y no aplicable. No calcula ratios ni sustituye campos ausentes por cero. Presenta literalmente display_value para conservar el redondeo del servidor.

Usa el Retrofit existente (http://10.0.2.2:8000/api/v1/ para emulador), enviando Authorization: Bearer con el token guardado. Sin sesión no consulta. Una sesión expirada informa que se requiere volver a iniciar sesión; la app actual no tiene renovación automática integrada. Cancelar una consulta al cambiar de selección impide reemplazarla por una respuesta antigua.

## Contrato y dependencia pendiente

Basado en docs/Sprint_2_contrato_pilares.md de origin/diego/sprint2-T65-preparacion, observado el 20/10/2026. Las copias adjuntas conservan el carácter de propuesta, sin atribuir aprobación al equipo.

GET companies/{ticker}/pillars/?fiscal_year=2024

schema_version=pillars-v1; ticker y fiscal_year deben coincidir con lo solicitado. pillars es una lista con id=liquidity y metrics con id=current_ratio y quick_ratio. quick_ratio y los campos detallados de source (label, tag, unit, filing, published_at, url) son convenciones del adaptador Android que Diego debe confirmar; no están fijados completamente en su propuesta.

**La ruta aún no existe en las ramas revisadas. No se certifica conexión real, coherencia visual con la web ni cierre integral de la historia.** Un 404 muestra servicio/empresa no disponible y permite reintentar, sin datos de demostración en producción. La implementación Android y las pruebas de contrato se pueden revisar de forma independiente.

## Verificación repetible

En android, con Java 17 y Gradle 8.4: gradle testDebugUnitTest --tests "com.example.sp500auth.pillars.*" y gradle assembleDebug.

LiquidityTest: valores de prueba 2,00 y 1,10 veces, cero válido, divisor cero, componentes ausentes, No aplica y respuesta incoherente. PillarsRepositoryTest: ruta, año, token, procedencia, periodo, 400/401/403/404/503/500, JSON inválido, cuerpo vacío y falta de sesión.

Son respuestas controladas, no datos reales ni validación de las fórmulas de Django. La parte compartida T04 requiere además las pruebas de cálculo del servidor de Diego y una ejecución contra ese servicio. Recorrido pendiente: iniciar sesión; abrir detalle y liquidez; consultar MSFT/2024; comparar valores/periodo/componentes con web; repetir con un año ausente; desconectar el servidor y reintentar. No hay captura real del flujo conectado todavía.

## Dependencias previas de navegación

ForgotPasswordScreen.kt y ProfileScreen.kt ya estaban presentes localmente y referenciadas por MainActivity, pero no estaban versionadas en Rosales. Se incorporan sin modificar su contenido para conservar esas dependencias al descargar el repositorio. El cambio local previo de DashboardScreen.kt se conserva fuera de los commits de este trabajo.

## Resultado del 20/10/2026

Compilación debug correcta y 16 pruebas JVM aprobadas: 5 de presentación de liquidez, 7 de repositorio y 4 de estado de pantalla. Java 17 y Gradle 8.4. Se usó una caché de verificación independiente por restricciones de acceso del entorno; no se modificó la configuración de Android Studio del usuario. No había dispositivo o emulador conectado; no se afirma una ejecución visual.
