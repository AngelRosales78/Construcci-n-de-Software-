# Datos del primer avance

`GOOGL-annual.json` y `META-annual.json` se recuperaron sin modificar de
`docs/datos/empresas/` en el commit `93e5ea3` de la rama Rosales. Son inventarios
anuales con URL SEC, etiquetas, fechas, accesiones y hash del snapshot original.
En el primer avance no se volvieron a descargar ni a verificar contra la fuente
en vivo. La ampliación del 19 de noviembre descrita abajo modifica únicamente
cuatro componentes, conservando la atribución del resto de los inventarios.

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

## Ampliación verificada el 19 de noviembre de 2026

Los resúmenes `GOOGL-metrics.json` y `META-metrics.json` se copiaron sin modificar
del mismo commit de Rosales, desde `docs/datos/empresas/`. Se reutilizan sus valores
y sus fuentes por campo. No son cotizaciones en tiempo real. La fecha del balance
no se presenta como fecha de la capitalización, el PER, la beta o el dividendo.
Cada tarjeta y el PDF conservan la fecha y la URL particular del indicador.

Se contrastó la nota de intereses de los informes 10-K publicados en SEC y se
transcribió el gasto neto reconocido, expresado en USD positivos, para solvencia.

| Empresa | Ejercicio | Gasto por intereses | Informe oficial |
|---|---|---|---|
| GOOGL | 2024 | 268.000.000 USD | https://www.sec.gov/Archives/edgar/data/1652044/000165204425000014/goog-20241231.htm |
| GOOGL | 2025 | 736.000.000 USD | https://www.sec.gov/Archives/edgar/data/1652044/000165204426000018/goog-20251231.htm |
| META | 2024 | 715.000.000 USD | https://www.sec.gov/Archives/edgar/data/1326801/000132680125000017/meta-20241231.htm |
| META | 2025 | 1.165.000.000 USD | https://www.sec.gov/Archives/edgar/data/1326801/000162828026003942/meta-20251231.htm |

La evidencia local es `intereses_informes.json`, una selección transcrita de hechos
verificados, no una descarga íntegra del informe. Su SHA-256 identifica ese archivo
de evidencia. Cada componente guarda URL, accesión, fecha de presentación y
localizador; el hash original del inventario sigue correspondiendo a los otros
componentes. La respuesta de pilares declara `snapshot_kind` para distinguirlos.

No se rellenó inventario con cero ni se inventó una nota global. GOOGL y META
permiten calcular solvencia FY2024/FY2025, pero eficiencia y nota global siguen
sin estar disponibles por componentes ausentes. Las comparaciones aún requieren
períodos completos y versiones compatibles, como antes.
