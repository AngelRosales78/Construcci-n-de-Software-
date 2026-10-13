# Datos importados para la presentación

Snapshot preparado el lunes 12 de octubre de 2026. No es un servicio de cotizaciones en tiempo real.

- `catalogo_sp500.csv`: 503 símbolos del conjunto público mantenido por https://github.com/datasets/s-and-p-500-companies, descargado de su archivo `data/constituents.csv`. Es un catálogo de referencia; no una certificación oficial de membresía emitida por S&P.
- `logos_procedencia.json`: URLs del proveedor https://financialmodelingprep.com/image-stock/ y SHA-256 de 500 imágenes PNG locales. GOOG/GOOGL y NWS/NWSA comparten marca. SKYD, VMRK y VYLR no devolvieron PNG verificables; la pantalla muestra un ticker alternativo y no inventa un logo. Las marcas pertenecen a sus titulares.
- `MSFT_balance_2024.csv`: balance anual al 30/06/2024 y métricas calculadas con datos de SEC y Yahoo Finance. Se eligió ese ejercicio porque tiene el gasto financiero anual necesario para calcular EBIT. No se presenta como balance de 2026.
- `MSFT_cierres_5y.csv`: 1254 precios de cierre ajustado publicados por Yahoo Finance, en USD, junto con fecha y volumen. El cierre no ajustado empleado para capitalización/PER se distingue del cierre ajustado utilizado en el gráfico.
- `MSFT_procedencia.json`: hechos SEC originales seleccionados, URLs, hashes de snapshots y fórmulas de las métricas derivadas. Beta usa SPY como referencia y dos años de retornos diarios; ROE usa patrimonio medio anual. Los resultados pueden diferir de otros proveedores con distinta metodología.

Los JSON completos de las fuentes se conservan fuera de Git en el directorio local `data_sources`. Para reproducir la transformación sin red: `python tools/prepare_real_data.py <snapshots> docs/datos` desde la raíz del repositorio. Para cargar, usar `import_company_catalog`, `import_market_data` y `preload_search`; `tools/Start-Presentation.ps1` los ejecuta exclusivamente en la base aislada `sprint_delivery_validation`.

La cobertura financiera real es **una empresa: MSFT**. Las otras 502 no tienen balances ni precios importados; muestran datos ausentes. La importación es repetible y conserva balances existentes. Las empresas y precios sintéticos del benchmark previo permanecen en `sprint_validation`, separados de esta presentación.

Los PNG se decodificaron también en navegador para identificar las marcas con más del 90 % de sus píxeles visibles claros. `frontend/src/data/lightLogos.json` registra 56 símbolos que necesitan fondo oscuro; se aplica por CSS, conservando las imágenes originales.
