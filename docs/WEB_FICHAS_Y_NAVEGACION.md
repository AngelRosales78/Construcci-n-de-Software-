# Fichas web: lectura y navegación

Este bloque modifica la página web y su arranque local. No cambia el código Android.

- El resumen indica cuántos de sus cinco indicadores están disponibles y distingue la fecha del balance de la fecha del precio. Cada indicador explica su significado sin abrir un desplegable; el método y la fuente quedan disponibles por separado. Cuando falta una fecha específica, se indica expresamente.
- Cada sección explica qué se puede consultar. La sección seleccionada se conserva en el enlace, al recargar y al usar Atrás/Adelante. Ejemplo: `/company/MSFT?section=pillars&fiscal_year=2024`.
- Cambiar el ejercicio o el rango de precios conserva la búsqueda de origen. «Volver» recupera sus filtros, orden y página.
- Una ficha que no carga permite reintentar. Los valores vacíos o no numéricos no se presentan como cero; un cero real sí se conserva.
- El arranque de PostgreSQL se separó de la captura de salida de PowerShell para evitar un bloqueo al iniciar la web desde la base detenida.

Validación: 130 pruebas web aprobadas, compilación de producción correcta y recorrido real con una cuenta Free. Se comprobó la lectura de indicadores, sección y ejercicio tras recargar, rango de precios, retorno a filtros y navegación Atrás. No hubo errores de página. El arranque desde PostgreSQL detenido completó migraciones, carga y servicios.

El recorrido puede repetirse con `node tools/verify_company_navigation.cjs`, con la plataforma y las cuentas locales preparadas. Guarda capturas y resultados en `output/web_ficha`, fuera del repositorio. Los accesos permanecen en sus archivos privados.
