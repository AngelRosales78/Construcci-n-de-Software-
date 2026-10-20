# HU05_03 — Ángel: T07 Android y parte Android de T08

Pantalla de rentabilidad desde Detalle de activo > Consultar pilar de rentabilidad. Permite seleccionar empresa/ejercicio; consulta el mismo servicio propuesto que liquidez, presentando roe, roa y net_margin, valores de display_value sin recalcular, porcentajes negativos válidos, fórmulas, componentes, periodo y procedencia. Carga, errores y datos ausentes comparten el manejo probado de HU05_02.

## Referencia sectorial

La propuesta de Diego todavía no fija la representación JSON de referencias sectoriales. El adaptador opcional sector_reference de cada métrica es una propuesta Android, pendiente de confirmación. Campos: status, value (decimal sin redondear), display_value, unit=percent, reason, sample_size, universe, as_of, period={start,end}, source={label,url,...}. Un resultado solo se presenta si status=available y están completos el valor, unidad, tamaño positivo de muestra, universo, fecha, periodo y fuente. La decisión de suficiencia/comparabilidad corresponde al servidor: Android no fija un umbral sectorial ni calcula una media. Si no hay referencia o la muestra es insuficiente, se muestra No disponible con su motivo.

## Pruebas

Desde android con Java 17 y Gradle 8.4: gradle testDebugUnitTest --tests "com.example.sp500auth.pillars.*". Para abrir: Android Studio > Open > carpeta android; seleccionar JDK 17 para Gradle, sincronizar y ejecutar la configuración app en un emulador. La dirección 10.0.2.2 apunta al equipo anfitrión del emulador. Mantener el servidor en el puerto 8000 e iniciar sesión para la consulta; si se usa un dispositivo físico debe configurarse la dirección accesible del servidor en NetworkModule.

ProfitabilityTest comprueba ROE 20,00 %, ROA 10,00 %, margen 12,50 %, redondeo enviado por servidor (12.345 -> 12,35), pérdida válida, cero válido, patrimonio negativo, muestra insuficiente y conservación de procedencia. No replica las fórmulas de Django: las pruebas de fórmulas son parte de T08 del servidor.

## Pendiente para el cierre integral

La rama de Diego revisada contiene la propuesta, pero no la ruta companies/{ticker}/pillars/. Se necesita confirmar los identificadores roe/roa/net_margin, el formato de referencias sectoriales y habilitar el endpoint. Después: ejecutar app y web con el mismo servidor, comparar empresa/ejercicio/valores y obtener evidencia visual. No se afirma aprobación del contrato por Diego, conexión real ni cierre de las pruebas compartidas del servidor. No se entrega un APK; el proyecto se prepara para Android Studio.

## Configuración de Android Studio

Se eliminó org.gradle.java.home que forzaba el JBR de Android Studio: ese JBR instalado es Java 25 y Gradle 8.4 requiere otra versión compatible. El proyecto local de Android Studio ya selecciona temurin-17.0.20.1 mediante GRADLE_LOCAL_JAVA_HOME. Se conserva esa selección del IDE y no se versiona una ruta personal nueva. En otro equipo seleccionar Java 17 en Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK.

## Resultado del 20/10/2026

Compilación Kotlin correcta y 23 pruebas JVM aprobadas: 5 de liquidez, 7 de rentabilidad, 7 de repositorio y 4 de estado de pantalla. Sin dispositivo/emulador conectado, no se realizó una ejecución visual en Android Studio. La publicación GitHub no pudo autenticarse: el gestor de credenciales de Windows devolvió Acceso denegado al consultar la credencial de github.com. Los commits por historia se conservan en Rosales local; se pueden publicar con Git > Push desde Android Studio.
