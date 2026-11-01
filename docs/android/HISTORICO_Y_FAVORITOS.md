# Sprint 3 histórico comparativo y favoritos

Alcance Android de HU08_03, HU10_01 y HU10_02. No se modifica Web ni backend ni se implementa la sincronización periódica de HU10_03.

## HU08_03 rendimiento histórico

T03.1: gráfico de dos líneas con colores y trazos distintos, escala porcentual y consulta de fecha mediante toque, arrastre o botones Anterior y Siguiente. Ambas empresas comienzan en 0 % sobre la misma primera fecha disponible para las dos. Se conservan precios originales para la consulta de cierres y se muestran fuentes, fechas y monedas.

T03.2: consulta autenticada de los históricos existentes, en paralelo, con rangos 1M, 6M, 1Y y 5Y. Android alinea exclusivamente fechas observadas en ambas series y calcula la normalización visual `(precio / precio inicial - 1) * 100`. No se presenta como cálculo del servicio comparativo. No rellena huecos ni estima datos faltantes. Se rechazan empresas o rangos incorrectos, fechas duplicadas, precios no positivos, valores no finitos y menos de dos fechas comunes. Las respuestas viejas se cancelan al cambiar empresas o rango. Se conserva el rango al recrear el estado.

La API de históricos ya existe en Script 2. La normalización no requiere el endpoint compare pendiente. Se avisa cuando hay pocos cierres, faltan fuentes o no se identifica la verificación de los datos. No convierte monedas ni calcula reinversión de dividendos. Las pruebas incluyen una serie de 1500 cierres; el rendimiento visual real requiere un emulador.

## HU10_01 añadir favoritos

T11.1: Mis favoritos, acceso desde Explorar y controles para marcar desde ficha y resultados. La lista y las fichas comparten estado de la cuenta activa. Al volver a una pantalla se consulta de nuevo; esto no equivale a la sincronización continua entre Web y Android de HU10_03.

T11.2: GET y POST autenticados con contrato propuesto favorites-v1. No se marca nada antes de confirmarlo el servidor. Se bloquean pulsaciones duplicadas y se comprueba una respuesta de conflicto contra la lista persistida. No se guardan favoritos personales en preferencias o una lista local que suplante al servidor. La memoria se limpia al cerrar sesión y las respuestas de otra cuenta no se aplican. Las peticiones de favoritos llevan una etiqueta interna de cuenta que el cliente comprueba también antes de enviar/reintentar una operación con una sesión distinta. La autorización real pertenece al servidor.

## HU10_02 quitar favoritos

T12.1: quitar desde lista, ficha y resultados con confirmación y Cancelar. Cancelar no hace ninguna petición. Mientras se confirma se impiden nuevas operaciones; una respuesta fallida conserva el favorito y permite reintentar. El contador y el estado vacío se actualizan tras una baja confirmada.

T12.2: DELETE usa el identificador del favorito, no el ticker ni la posición de la fila. Se acepta 204 como confirmación; un 404 solo se trata como baja ya realizada si una lectura autenticada nueva confirma que no existe ni ese identificador ni ese ticker. Si también falta el servicio de listado, no se anuncia una baja ficticia. Los errores de permisos retiran la lista privada.

## Contrato de favoritos pendiente de Diego

El backend copiado todavía NO contiene estas rutas. El cliente está preparado y mostrará un error de servicio no disponible hasta que se implemente el contrato. No se puede certificar persistencia real, autorización entre usuarios ni aceptación conjunta con Web usando solo las respuestas controladas de las pruebas Android.

Todas las rutas usan la misma sesión Bearer existente y se filtran por usuario autenticado en el servidor:

* GET `/api/v1/favorites/`: 200 con `schema_version: favorites-v1`, `owner_id` y `results` (lista completa). Una lista vacía válida es `results: []`; no se confunde con un fallo de red.
* POST `/api/v1/favorites/`: cuerpo `{"ticker":"AAPL"}`. Respuesta confirmada 201, o 200 idempotente, con `schema_version`, `owner_id` y `favorite`. Si hay duplicado, 409 permite refrescar y comprobarlo.
* DELETE `/api/v1/favorites/{id}/`: 204 tras eliminar un favorito propio. 401/403 para denegación; 404 para inexistencia. El servidor debe impedir operar sobre favoritos ajenos incluso con petición directa.

Un favorito contiene `id` numérico positivo, `ticker` y `name`. `owner_id` coincide con user_id de la sesión. La lista no contiene identificadores ni tickers duplicados. El ejemplo es un contrato propuesto, no una ruta ya implementada. Si se decide paginar la lista se debe acordar y adaptar el contrato antes de integrarla; esta versión requiere la colección completa.

## Verificar desde Android Studio

1. Abrir Script 3/android con JDK 17 e iniciar el servidor. No se entrega un APK.
2. En Comparar elegir AAPL y MSFT. Cambiar los cuatro rangos y verificar que se muestran las mismas fechas para las dos series. Consultar una fecha con toque y con Anterior/Siguiente. Cambiar rápidamente B y comprobar que no quedan resultados de la selección anterior.
3. Comparar una serie corta, una serie sin fechas comunes y un fallo de red; no deben aparecer valores inventados. Girar el dispositivo conserva la selección y el rango; si el proceso se recrea se consultan nuevamente los datos.
4. Cuando Diego tenga el servicio, añadir AAPL desde ficha, comprobar que aparece una sola vez en Mis favoritos y en los resultados; volver a abrir la app y comprobar la persistencia del servidor.
5. Pedir quitar AAPL, cancelar y verificar que sigue. Confirmar, revisar que desaparece en ficha y lista y que quitar el último muestra el estado vacío.
6. Probar una baja ya realizada, red interrumpida y sesión sin permiso: no se muestra éxito falso. Cambiar de cuenta no conserva favoritos, mensajes o confirmaciones de la anterior.

Pruebas JVM: `gradlew.bat testDebugUnitTest`. Compilación de interfaz: `gradlew.bat compileDebugAndroidTestKotlin`. Con emulador: `gradlew.bat connectedDebugAndroidTest`. Las pruebas con respuestas HTTP controladas y los dobles del servicio viven solo en test y androidTest.

## Verificación registrada el 1 de noviembre de 2026

Gradle 8.4 y JDK 17: compileDebugKotlin, testDebugUnitTest y compileDebugAndroidTestKotlin terminaron correctamente. Se descubrieron 114 pruebas JVM: 111 aprobadas, 3 integraciones opcionales omitidas y cero fallos o errores. Las 23 pruebas nuevas aprobaron: 8 de alineación y carga histórica, 8 del contrato HTTP de favoritos, 3 del estado de alta y 4 de confirmación de baja.

Se compiló la prueba Compose para consultar fechas con botones y la de Cancelar una baja. No se ejecutaron en dispositivo ni se realizó inspección visual: adb no encontró dispositivos conectados. La comprobación de 1500 puntos es de transformación y búsqueda de fechas en JVM; no acredita una medición de fluidez gráfica en emulador. La persistencia real de favoritos, los permisos en servidor y el flujo conjunto con Web siguen pendientes del servicio de Diego. El agente Byte Buddy existente se precargó para las pruebas heredadas de Mockito; no se agregan dependencias de producción.

Evidencia resumida en `evidence/sprint3_historico_favoritos.json`. Los cambios de este avance están limitados a Android y su documentación.
