# Datos importados para la presentación

Snapshot preparado el lunes 12 de octubre de 2026. No es un servicio de cotizaciones en tiempo real.

- `catalogo_sp500.csv`: 503 símbolos del conjunto público mantenido por https://github.com/datasets/s-and-p-500-companies, descargado de su archivo `data/constituents.csv`. Es un catálogo de referencia; no una certificación oficial de membresía emitida por S&P.
- `logos_procedencia.json`: URLs del proveedor https://financialmodelingprep.com/image-stock/ y SHA-256 de 500 imágenes PNG locales. GOOG/GOOGL y NWS/NWSA comparten marca. SKYD, VMRK y VYLR no devolvieron PNG verificables; la pantalla muestra un ticker alternativo y no inventa un logo. Las marcas pertenecen a sus titulares.
- `MSFT_balance_2024.csv`: balance anual al 30/06/2024 y métricas calculadas con datos de SEC y Yahoo Finance. Se eligió ese ejercicio porque tiene el gasto financiero anual necesario para calcular EBIT. No se presenta como balance de 2026.
- `MSFT_cierres_5y.csv`: 1254 precios de cierre ajustado publicados por Yahoo Finance, en USD, junto con fecha y volumen. El cierre no ajustado empleado para capitalización/PER se distingue del cierre ajustado utilizado en el gráfico.
- `MSFT_procedencia.json`: hechos SEC originales seleccionados, URLs, hashes de snapshots y fórmulas de las métricas derivadas. Beta usa SPY como referencia y dos años de retornos diarios; ROE usa patrimonio medio anual. Los resultados pueden diferir de otros proveedores con distinta metodología.

Los JSON completos de las fuentes se conservan fuera de Git en el directorio local `data_sources`. Para reproducir la transformación sin red: `python tools/prepare_real_data.py <snapshots> docs/datos` desde la raíz del repositorio. Para cargar, usar `import_company_catalog`, `import_market_data` y `preload_search`; `tools/Start-Presentation.ps1` los ejecuta exclusivamente en la base aislada `sprint_delivery_validation`.

La cobertura descrita arriba corresponde a la primera instantánea de octubre. La ampliación de noviembre añade perfiles públicos y sitios web a las 503 empresas del catálogo de referencia y a HIMS, añadida mediante el flujo administrativo. `company_profiles.json` conserva actividad, sede y fundación de la referencia, reseñas breves de Wikidata o la clasificación de actividad, enlaces oficiales revisados y fuentes por empresa. Los campos no encontrados permanecen ausentes.

`catalog_prices.json.gz` conserva las respuestas públicas verificadas del lote de históricos, con ticker, fecha de consulta, fuente y SHA-256 de cada respuesta. El número de sesiones depende de la antigüedad de cada símbolo. El importador excluye la sesión actual y conserva datos posteriores que ya estén guardados. No convierte precios en PER, ROE ni balances. La cobertura comprobada al terminar la carga se documenta en [CATALOGO_COMPLETADO.md](../CATALOGO_COMPLETADO.md).

El arranque carga estas copias sin volver a descargar el catálogo completo. Las importaciones no sustituyen ediciones descriptivas posteriores del equipo. Las empresas y precios sintéticos del benchmark anterior permanecen en `sprint_validation`, separados de esta presentación.

Los PNG se decodificaron también en navegador para identificar las marcas con más del 90 % de sus píxeles visibles claros. `frontend/src/data/lightLogos.json` registra 56 símbolos que necesitan fondo oscuro; se aplica por CSS, conservando las imágenes originales.

## Ampliación financiera del 24 de noviembre

`catalog_financials.json.gz` conserva 504 respuestas de las series financieras públicas de Yahoo Finance, sus URLs, momentos de consulta y SHA-256, además del histórico de referencia del S&P 500. La copia está vinculada al hash de `catalog_prices.json.gz`; no contiene credenciales. Se restaura con `import_provider_financials` después de los precios, también durante el arranque habitual.

La cobertura final es de 504 fichas con referencias anuales y resumen, 2.015 registros anuales en total (1.998 Yahoo y 17 SEC conservados), capitalización en 504, PER en 502, ROE en 467, beta en 500 y dividendo en 390. Los campos ausentes no se convierten en cero y las fechas normalizadas no se presentan como períodos contables oficiales. La captura no añade una actualización financiera automática.

La guía de exposición, las fórmulas, las comprobaciones puntuales contra informes originales y las instrucciones de reproducción están en [DATOS_FINANCIEROS_CATALOGO.md](../DATOS_FINANCIEROS_CATALOGO.md).
