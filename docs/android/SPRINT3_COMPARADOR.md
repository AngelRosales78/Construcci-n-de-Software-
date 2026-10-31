# Sprint 3 Android

Base: Script 2, conservando su historial en Rosales. Script 1 y Script 2 no se modifican.
Alcance: solo las tareas Android de HU08_01 y HU08_02. No se implementan Web, pagos, IA comparativa ni gráficos de rendimiento.

## T01.1 Selector dual en Android

Se incorpora Comparar desde Explorar y desde la ficha de una empresa. Hay dos buscadores independientes, hasta cinco sugerencias reales, espera de 300 ms al escribir, validación de empresas distintas, quitar e intercambiar. La selección y los textos se guardan con SavedStateHandle para restauración del estado. Al entrar desde una ficha, esa empresa se propone como A una sola vez.

Comprobación manual: buscar AAPL en A y MSFT en B; intentar seleccionar AAPL dos veces; intercambiar y comprobar que los nombres acompañan a los símbolos; quitar y girar el emulador. Abrir Comparar desde una ficha y comprobar la empresa inicial.

Las pruebas JVM cubren identificadores, duplicados, quitar, intercambio, búsquedas independientes y restauración del ViewModel. Las pruebas de interfaz requieren ejecutar el proyecto en Android Studio y no se confunden con pruebas JVM.

## T01.2 Consulta y pruebas del comparador móvil

Se añade una llamada Retrofit autenticada con A, B y fiscal_year, validación de identidad y ejercicio, progreso, error y reintento. La selección permanece tras un fallo. Cambiar el par o el ejercicio cancela la consulta e invalida el resultado anterior; una respuesta tardía no sustituye otra selección. El ejercicio se restaura con SavedStateHandle. La respuesta del comparador no se guarda como caché sin revalidarla al recrear el proceso.

Pruebas de contrato con respuestas HTTP controladas cubren autenticación, parámetros, orden A/B, datos de otra empresa, ejercicio incorrecto, JSON inválido y recuperación de red. Pruebas del ViewModel cubren reintento, selección incompleta y respuesta tardía. El contrato propuesto para coordinar con Diego está en CONTRATO_COMPARADOR.md.

## T02.1 Tabla de pilares en Android

Tabla de cinco ejes con columnas A y B y desplazamiento horizontal, notas sobre 100, periodo y moneda por empresa, metodología, versión de normalización, avisos y fuentes enlazadas. El resultado usa texto y símbolos además de color: Mayor nota, Empate o No comparable. El quinto eje es Crecimiento según el servidor actual. No se cambia a Valuación en el cliente.

Los valores ausentes se muestran como No disponible y no como cero. Android solo presenta la decisión y diferencia enviadas por el servicio. Una nota mayor no se anuncia como recomendación de inversión.

## T02.2 Integración y casos límite de pilares

Si compare responde 404 o 501, se consultan ambos endpoints reales de pilares y se identifica claramente el modo consulta independiente. La tabla conserva notas y fuentes reales, pero no calcula ganadores ni diferencias. No se activa esta alternativa ante 401, 403, 500 o 503 y tampoco se acepta una respuesta de otra empresa o ejercicio.

Con un servicio comparativo disponible, se muestran sus decisiones solo si hay notas válidas, periodos anuales idénticos y válidos, misma metodología y versión de normalización y una decisión completa. El contrato de esta versión exige equivalencia exacta de fechas para presentar un resultado; si Diego adopta otra política de comparabilidad, debe versionarse y acordarse con el cliente. Una nota ausente, fila duplicada, periodo incompatible, versión distinta o decisión incoherente se presenta como No comparable. Nunca se deriva un ganador a partir de ratios brutos.

Se añaden pruebas JVM para estos casos, recuperación de errores y consultas independientes, y una prueba Compose para las etiquetas de la tabla. La prueba Compose debe ejecutarse con un emulador conectado.

### Recorrido de aceptación en Android Studio

1. Iniciar el servidor y entrar con una cuenta real de prueba. Abrir Comparar desde Explorar.
2. Buscar AAPL en Empresa A y MSFT en Empresa B. Comprobar máximo cinco sugerencias e impedir AAPL en ambas.
3. Intercambiar: A pasa a MSFT y B a AAPL, conservando los nombres. Quitar una empresa deshabilita Comparar.
4. Abrir la ficha de AAPL y pulsar Comparar AAPL con otra empresa: A se completa inicialmente. Elegir MSFT en B y un ejercicio con datos, por ejemplo 2024.
5. Consultar: verificar cinco filas y notas del servidor. Con el servidor heredado debe aparecer el aviso de servicio comparativo pendiente y No comparable en cada resultado; esto no significa que se hayan inventado o perdido las notas individuales.
6. Deslizar la tabla para ver A y B; revisar periodos y fuentes. Girar el emulador conserva selección y ejercicio. Si el proceso se recrea, volver a consultar para obtener resultados vigentes.
7. Detener temporalmente el servidor, consultar y comprobar error con reintento sin perder A/B ni ejercicio; reiniciarlo y reintentar.
8. Con el servicio de Diego implementado, comprobar empate, nota ausente, periodos incompatibles y cambio rápido de empresas. Contrastar las cifras y decisiones con la Web para la misma cuenta y periodo antes de declarar aceptación integral de la HU.

### Pruebas locales

Desde android, con JDK 17: `gradlew.bat testDebugUnitTest compileDebugKotlin compileDebugAndroidTestKotlin`.
Con emulador conectado: `gradlew.bat connectedDebugAndroidTest`.

## Dependencia pendiente

La copia del servidor de Script 2 tiene búsqueda, ficha y pilares por empresa, pero no tiene una ruta de comparación. La integración Android queda preparada sin dar por realizado el servidor de Diego ni la aceptación conjunta de Web y Android.

## Resultado de la verificación del 30 de octubre de 2026

`compileDebugKotlin`, `testDebugUnitTest` y `compileDebugAndroidTestKotlin` finalizaron correctamente con Gradle 8.4 y Java 17. Se descubrieron 91 pruebas JVM: 88 aprobadas, 3 integraciones opcionales omitidas, cero fallos y cero errores. Las 23 pruebas nuevas del comparador aprobaron. Se precargó el agente Byte Buddy existente para ejecutar las pruebas heredadas que usan Mockito en este entorno; no se cambian dependencias de producción.

La prueba Compose del comparador está compilada, pero no ejecutada. `adb devices` no encontró emulador ni teléfono conectado. Por ello, la revisión visual, el giro real del dispositivo y la comparación conjunta con Web siguen pendientes; las pruebas de restauración aquí ejecutadas son JVM sobre SavedStateHandle, no una captura o ejecución instrumental en un dispositivo. Ver evidencia resumida en `evidence/sprint3_comparador.json`.

## Ejecutar

Abrir `Script 3/android` en Android Studio, seleccionar JDK 17 y ejecutar app. Se conserva la dirección de API del emulador `http://10.0.2.2:8000/api/v1/`. Puede usarse el servidor ya preparado de Script 2; para restaurar datos en la copia de Script 3 seguir `tools/Start-AndroidServer.ps1 -Prepare` con un entorno Python configurado, según README_Script_2.md. No se copian bases personales, claves, modelos de Ollama ni entornos virtuales.
