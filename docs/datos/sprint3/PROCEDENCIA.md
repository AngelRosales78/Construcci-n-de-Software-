# Datos del primer avance

`GOOGL-annual.json` y `META-annual.json` se recuperaron sin modificar de
`docs/datos/empresas/` en el commit `93e5ea3` de la rama Rosales. Son inventarios
anuales con URL SEC, etiquetas, fechas, accesiones y hash del snapshot original.
No se volvieron a descargar ni a verificar contra la fuente en vivo en este avance.

Se importan con el comando existente `import_annual_financials`, sin reemplazar
los datos ni las funciones del Sprint 2. Los períodos 2024 de ambas empresas
van del 1 de enero al 31 de diciembre. Los datos ausentes permanecen ausentes.
Una mayor nota académica no equivale a una recomendación de compra.

Microsoft conserva el inventario del Sprint 2 y sirve para mostrar que dos cierres
etiquetados FY2024 con fechas distintas no permiten asignar un ganador.

## Históricos incorporados al cierre

Los seis archivos `MSFT-prices`, `GOOGL-prices` y `META-prices` (CSV y JSON de fuente) se copiaron del mismo commit `93e5ea3` de Rosales. Cada CSV contiene 1.255 cierres ajustados, hasta el 27 de octubre de 2026. Los JSON conservan la URL Yahoo Finance, fecha de descarga original y hash de la respuesta original. No se descargaron de nuevo durante este cierre; por tanto no se afirma que estén actualizados al día de presentación. El catálogo de 503 símbolos no significa que las 503 empresas tengan estados financieros importados.

El `raw_sha256` del JSON identifica la respuesta original, no el CSV transformado. Los hashes de los CSV efectivamente incluidos son:

- `MSFT-prices.csv`: `05712534bd1a6206edcb3456d41587481e434b4ee42369506c0bcd3fb2df08a4`.
- `GOOGL-prices.csv`: `5b941faa3cbea97804a5da6d3b83b1c78fb67fa139f05fe36d969808043af05a`.
- `META-prices.csv`: `3047115bc4080d95940367ecf050893bfa3bccb1c9cabbf0af400d0d3eac63c9`.

La normalización usa solamente fechas observadas en ambas series: `(precio / primer precio común - 1) × 100`. Se rechazan precios no positivos y monedas mezcladas; no se rellenan huecos ni se convierten divisas. El rango se calcula desde la fecha actual y la pantalla declara la fecha del último dato.
