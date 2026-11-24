# Información del catálogo: ampliación del 23 de noviembre de 2026

La ampliación abarca las 503 empresas de la referencia y HIMS, añadida previamente desde administración. Se conservaron las cuentas, favoritos, balances existentes, permisos y el trabajo Android.

## Información incorporada

- 504 empresas con reseña breve o descripción de su actividad y sitio web HTTPS.
- Actividad en español para las 503 entradas de la referencia; sede y fundación cuando constan en esa misma fuente. HIMS conserva su descripción obtenida de la página corporativa, sin inventar sede ni fundación.
- Identificador CIK y enlace a informes SEC para las 504 empresas.
- Fuentes y fechas visibles en la sección «Acerca de» de cada ficha.
- Históricos públicos de precios para las 504 empresas, cargados mediante el flujo administrativo, con identificación del símbolo, moneda, cierre ajustado, fecha, volumen y SHA-256 de la respuesta. La longitud de las series varía según el símbolo; no se completan sesiones ausentes con cifras sintéticas.

Las reseñas proceden de 211 perfiles públicos en español, 292 clasificaciones de actividad traducidas y una página corporativa. Una clasificación de actividad no se presenta como una descripción detallada de productos. Los sitios que faltaban se revisaron en las páginas corporativas; las referencias quedan guardadas por empresa.

## Fuentes y alcance

El catálogo de referencia procede de [datasets/s-and-p-500-companies](https://github.com/datasets/s-and-p-500-companies/blob/main/data/constituents.csv), con instantánea del 12 de octubre. No acredita por sí solo membresía actual certificada por S&P.

Los perfiles se cruzaron primero por [CIK de Wikidata](https://www.wikidata.org/wiki/Property:P5531). Cuando faltaba esa relación se exigió coincidencia de bolsa, símbolo y nombre, o un CIK coincidente. Las identidades ambiguas no se usaron para atribuir reseñas o sitios. Los campos estructurados de Wikidata se publican bajo CC0; las URLs de cada entidad se conservan en la copia. La descripción de HIMS se revisó en su [página de relaciones con inversores](https://investors.hims.com/overview/default.aspx).

Los históricos proceden del endpoint público de gráficos de Yahoo Finance, con URL individual en cada registro. Se excluye la sesión actual incluso cuando el proveedor ya publica una vela. No se presentan como precios en tiempo real.

**Los balances e indicadores fundamentales siguen disponibles para MSFT, GOOGL y META.** La nueva carga de precios no crea PER, ROE, capitalización, dividendo, balances ni puntuaciones para las demás empresas. La consulta nueva a SEC volvió a responder HTTP 403. Las fichas indican la falta de estos datos y mantienen los históricos y la información descriptiva utilizables.

## Reproducción y conservación

- `tools/prepare_company_profiles.py` prepara `docs/datos/company_profiles.json`; la caché de consultas queda fuera del repositorio. Las traducciones y referencias corporativas revisadas se conservan en los dos JSON del directorio `tools`.
- `import_company_profiles` carga perfiles y preserva ediciones posteriores del equipo, incluidas descripciones y sitios borrados intencionalmente. No altera sectores editados, precios ni balances.
- `backfill_catalog_prices` usa el mismo proveedor, validaciones, cuotas y auditoría que la administración. El lote eleva explícitamente su límite interno de 100 a 600 consultas; conserva la cuota operativa de 1.000 llamadas y se detiene ante un límite del proveedor o cinco fallos consecutivos. El identificador de lote permite reanudar sin repetir operaciones terminadas.
- `docs/datos/catalog_prices.json.gz` conserva respuestas y huellas verificables. `import_catalog_prices` comprueba huella, símbolo, moneda y fuente; conserva históricos posteriores y no vuelve a reconstruir filas ya cubiertas durante cada arranque.
- `Iniciar Plataforma.cmd` carga las dos copias locales sin descargar de nuevo todo el catálogo.

## Comprobaciones

La ejecución completa del servidor aprobó 504 pruebas y omitió una optativa. Tras los últimos ajustes, las 13 pruebas de importación volvieron a aprobarse: integridad de copias, símbolo incorrecto, importación sin escrituras parciales, conservación de históricos posteriores y del corte de captura, ediciones propias y detención por límites.

La web aprobó 133 pruebas en 29 archivos y compiló correctamente. `tools/verify_catalog_enrichment.cjs` comprueba la cobertura completa mediante la API y recorre Apple, 3M, ExxonMobil, Zoetis y HIMS en escritorio y ventana estrecha. También comprueba una simulación de Apple y que los indicadores desconocidos no aparezcan como cifras válidas.

El recorrido real confirmó 504 reseñas, 504 sitios y 504 empresas con precios, sin símbolos repetidos ni errores de página. Los resúmenes y balances siguen siendo tres. Volver a cargar la copia comprimida conservó las 504 series existentes sin sustituirlas ni hacer nuevas llamadas al proveedor.

Los resultados y capturas locales quedan en `../output/catalog_enrichment/`; el lote conserva su informe en `../output/catalog_prices_backfill.json`. No se incluyen credenciales en estas evidencias ni en Git.
