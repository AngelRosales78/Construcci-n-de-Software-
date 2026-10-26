# Script 2: Android

Base: Script 1, historial Rosales, sin modificar la carpeta original. Servidor/web incorporados desde origin/diego/sprint2-dia3-radar-puntaje. La base de datos SQLite copiada de Script 1 se conserva localmente y necesita migraciones antes de usar los modelos anuales.

## Sesión y Android Studio

Se integró el flujo autenticado de Diego: registro e inicio de sesión reales, validación de sesión al abrir, biometría solo habilitada después de autenticarse y sin acceso de demostración ante errores. Se añadió renovación sincronizada de JWT para todas las consultas, sin recursión y conservando la sesión ante fallos temporales. Los tokens permanecen cifrados.

Abrir android/ con Java 17 en Android Studio. API predeterminada: http://10.0.2.2:8000/api/v1/. Se puede definir apiBaseUrl en las propiedades de Gradle (por ejemplo, la IP del equipo para un dispositivo físico), conservando /api/v1/. La carpeta .idea y la selección local de JDK no se publican. No se entrega un APK.

Las pruebas JVM no necesitan un dispositivo. Una ejecución visual solo se registrará cuando exista evidencia real.

## Pilares y ficha conectada

La entrada principal consulta catálogo, ficha e histórico reales del servidor. Permite paginar y abrir un símbolo explícito. HU05_02 y HU05_03 usan ahora el contrato implementado annual-ratios-v2: componentes accession/filed/currency, umbrales y referencia sectorial a nivel de pilar. No se recalculan ratios ni redondeos en Android. El ejercicio solicitado no cambia silenciosamente.

Se conservó en test/resources una respuesta publicada de MSFT FY2024 para detectar diferencias de contrato. Esta respuesta se usa únicamente en pruebas, nunca para rellenar la aplicación. Compilación Kotlin y pruebas JVM correctas; 92 pruebas existentes del servidor aprobadas sobre SQLite aislado. El entorno tiene Django 6.1.1 instalado; las dependencias declaradas del proyecto siguen fijadas por Diego, por lo que esa ejecución no certifica su entorno PostgreSQL de producción.
