# Información financiera del catálogo — 24 de noviembre de 2026

## Qué cambió y cómo mostrarlo

Las 504 fichas ahora tienen datos financieros anuales y un resumen de indicadores. Antes, los balances y resúmenes estaban disponibles únicamente para MSFT, GOOGL y META. Se conservan sus datos originales.

Recorrido breve para la exposición:

1. Abre **Apple → Resumen**. Muestra los indicadores y las fuentes de cada uno. Explica que cada dato conserva su propia fecha.
2. Entra en **Salud financiera → Ver estados anuales importados**. Muestra activos, pasivos, ingresos y beneficio en USD completos; cambia el año para consultar otra referencia.
3. Señala la nota de fecha: Yahoo entrega referencias anuales normalizadas. No conocemos a partir de esa respuesta el inicio contable exacto ni la fecha de publicación del informe.
4. Descarga el PDF o abre el análisis. Ambos conservan esa aclaración y los valores ausentes.

Frase para explicarlo: «Ampliamos la información financiera a todo el catálogo. Cada cifra indica su fuente y su fecha; si falta información, la página lo muestra y evita sacar una conclusión que esos datos no permiten».

## Cobertura comprobada

Las cantidades corresponden a fichas por símbolo, no a emisores únicos ni a membresía actual certificada por S&P.

| Información | Fichas con datos |
|---|---:|
| Referencias financieras anuales | 504 de 504 |
| Resumen con al menos un indicador | 504 de 504 |
| Capitalización | 504 |
| PER | 502 |
| ROE | 467 |
| Beta | 500 |
| Rendimiento por dividendo | 390 |

La base contiene **2.015 registros anuales**: 1.998 referencias nuevas del proveedor y 17 registros SEC conservados. Se analizaron 2.008 referencias del proveedor; diez coincidían en empresa/año con registros SEC y no los sustituyeron. Los tres resúmenes anteriores también se conservaron.

La tabla no significa que cada empresa tenga todos los componentes o una puntuación completa. Los datos insuficientes permanecen ausentes, nunca se convierten en cero.

## Fuente y fechas

La captura procede de las series públicas de [Yahoo Finance](https://finance.yahoo.com/), endpoint `fundamentals-timeseries`, con series anuales `12M` y series publicadas de capitalización y PER `TTM`. Se conservan respuesta original, símbolo, URL, momento de consulta y SHA-256. Las nuevas referencias están denominadas en USD.

El acceso directo a SEC volvió a responder HTTP 403. Esto no convierte las respuestas Yahoo en documentos SEC. Los registros del proveedor tienen inicio contable y fecha de publicación ausentes, y no se les asigna un identificador de presentación SEC.

Ejemplo: Yahoo referencia Apple al 30/09/2026, mientras su [10-K de 2026](https://www.sec.gov/Archives/edgar/data/320193/000032019326000026/aapl-20260926.htm) cierra el 26/09/2026. La interfaz, el PDF y el análisis distinguen esa referencia del cierre contable exacto.

## Comprobaciones puntuales con informes originales

Se contrastó el activo total de estas cuatro referencias con informes del emisor. Los informes expresan los importes en millones; la aplicación muestra USD completos.

| Empresa | Referencia Yahoo | Cierre del informe | Activo total en USD | Informe original |
|---|---|---|---:|---|
| AAPL | 2026-09-30 | 2026-09-26 | 404.548.000.000 | [10-K Apple 2026](https://www.sec.gov/Archives/edgar/data/320193/000032019326000026/aapl-20260926.htm), balance consolidado |
| AMZN | 2025-12-31 | 2025-12-31 | 818.042.000.000 | [Informe Amazon 2025](https://s2.q4cdn.com/299287126/files/doc_financials/2026/ar/Amazon-2025-Annual-Report.pdf), balance consolidado |
| JPM | 2025-12-31 | 2025-12-31 | 4.424.900.000.000 | [Informe JPMorgan Chase 2025](https://www.jpmorganchase.com/content/dam/jpmc/jpmorgan-chase-and-co/investor-relations/documents/annualreport-2025.pdf), balance consolidado |
| WMT | 2026-01-31 | 2026-01-31 | 284.668.000.000 | [10-K Walmart 2026](https://stock.walmart.com/sec-filings/all-sec-filings/content/0000104169-26-000055/0000104169-26-000055.pdf), balance consolidado |

Estas comprobaciones no constituyen una auditoría de las 504 fichas ni certifican todos sus componentes. No cambian la procedencia Yahoo de la copia ni completan fechas que la respuesta original no proporcionaba.

## Cómo se calculan los indicadores nuevos

- **Capitalización y PER:** valores publicados en las series del proveedor, con fecha individual. No se reconstruyen usando cierres ajustados.
- **ROE:** beneficio neto dividido entre el patrimonio medio de dos referencias anuales consecutivas del mismo origen, multiplicado por 100. Requiere ambos patrimonios positivos. Puede diferir del ROE publicado por el emisor.
- **Beta:** covarianza de 252 retornos diarios ajustados comunes de la acción y el S&P 500, dividida entre la varianza del índice. Requiere 253 observaciones comunes y no interpola sesiones.
- **Rendimiento por dividendo:** dividendos en efectivo registrados durante 365 días divididos entre el cierre sin ajustar de esa referencia, multiplicado por 100. Se omite si faltan eventos, historia suficiente o una base por acción verificable ante un desdoblamiento reciente.
- **Ratios de salud financiera:** utilizan los componentes presentes. Los cálculos con historia anual exigen referencias consecutivas, misma moneda y la misma captura; no mezclan ejercicios SEC con referencias Yahoo.
- **Altman-Z:** permanece sin calcular en los resúmenes nuevos porque no se verificó la capitalización correspondiente al cierre contable exacto.

Las referencias normalizadas no permiten declarar que dos empresas tienen períodos contables idénticos. La comparación no asigna ganadores en esos casos. GOOGL/META de 2024 conserva la comparación con períodos oficiales completos. Una puntuación global requiere la cobertura definida por la metodología existente; no se reemplaza por la media de los pocos indicadores disponibles.

## Conservación y reproducción

`docs/datos/catalog_financials.json.gz` contiene 504 respuestas financieras originales y la referencia del índice. Versión de cálculo: `provider-financials-v1`. SHA-256 de la copia final:

```text
a0a63ba146a7f82487751bfe7e5f144773980b9bf640321fb1a323528e27daeb
```

El paquete está vinculado mediante SHA-256 a `catalog_prices.json.gz`. Las importaciones validan identidad, fuente, huellas, moneda y límites antes de escribir; conservan los registros SEC, los resúmenes originales y las referencias posteriores.

Desde el directorio `backend`, usando la configuración local habitual:

```powershell
python manage.py import_provider_financials ../docs/datos/catalog_financials.json.gz --prices ../docs/datos/catalog_prices.json.gz --dry-run
python manage.py import_provider_financials ../docs/datos/catalog_financials.json.gz --prices ../docs/datos/catalog_prices.json.gz
```

`Iniciar Plataforma.cmd` restaura esta copia después de los precios, sin repetir las consultas del catálogo. Repetir la importación final conservó las 504 fichas, sin nuevas filas ni cambios en sus resúmenes.

La captura inicial usa el comando administrativo `download_catalog_financials`, con pausas, cuota auditada y detención ante límites del proveedor. El límite interno del lote se amplió explícitamente a 1.200 consultas. La actualización manual de administración **sigue descargando solamente precios**; no se añadió una actualización financiera automática.

## Verificación del cambio

- Servidor completo: **525 pruebas aprobadas y 1 optativa omitida**.
- Web completa: **136 pruebas aprobadas** en 30 archivos; compilación de producción correcta.
- Esquema y comprobaciones Django sin cambios pendientes ni problemas.
- Recorrido real de ocho fichas: AAPL, AMZN, JPM, WMT, MCD, FDXF, VYLR y HIMS, en escritorio y ventana estrecha, sin errores de página ni desbordamiento horizontal.
- PDF real de Apple generado y descargado; aclaración de referencia conservada y sin fechas ficticias.
- Análisis con IA de Apple generado y validado con la nueva referencia.
- Comparaciones con fechas normalizadas y comparación oficial GOOGL/META comprobadas.
- Recarga sin modificaciones comprobada. No se modificó Android.

El recorrido reproducible es `tools/verify_provider_financials.cjs`. Las capturas, PDF, resultados y cobertura de la ejecución local quedan fuera de Git en `../output/provider_financials/`; los registros de pruebas están en `../output/`.

Esta ampliación no cierra la aceptación pendiente de recepción Firebase/FCM del Sprint 4, documentada en [SPRINT4_ESTADO_Y_EVIDENCIAS.md](SPRINT4_ESTADO_Y_EVIDENCIAS.md).
