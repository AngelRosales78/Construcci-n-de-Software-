# Altman-Z: referencias de cierre verificadas

Actualización: 24 de noviembre de 2026.

## Resultado para la exposición

La cobertura actual es de 180 puntuaciones después de la [ampliación SEC](ALTMAN_CATALOGO_SEC.md). Este documento conserva la procedencia de las primeras cinco referencias añadidas manualmente.

Se añadieron referencias independientes para calcular Altman-Z en cinco fichas que antes aparecían sin datos. Con Microsoft, son seis resultados disponibles. Los datos financieros anteriores se conservan. El diagnóstico del resto del catálogo está en [DIAGNOSTICO_FINANCIERO.md](DIAGNOSTICO_FINANCIERO.md).

| Empresa | Ejercicio contable | Última sesión al cierre | Puntaje |
|---|---|---|---:|
| Apple (AAPL) | 28/09/2025–26/09/2026 | 25/09/2026 | 12,70 |
| Amazon (AMZN) | 01/01/2025–31/12/2025 | 31/12/2025 | 5,37 |
| Walmart (WMT) | 01/02/2025–31/01/2026 | 30/01/2026 | 6,47 |
| Caterpillar (CAT) | 01/01/2025–31/12/2025 | 31/12/2025 | 4,25 |
| Coca-Cola (KO) | 01/01/2025–31/12/2025 | 31/12/2025 | 4,76 |

Los cinco resultados corresponden a la zona favorable de la fórmula académica existente. No constituyen probabilidades de quiebra ni acreditan que el modelo original sea válido para cada sector. Se conservó la fórmula de cinco factores y sus umbrales; no se sustituyó por otra variante para obtener puntajes.

Para mostrarlo:

1. Abre Apple y entra en **Salud financiera → Altman-Z Score**. El Resumen conserva la clasificación breve.
2. Muestra el puntaje y el perfil financiero.
3. Abre **Ver datos y fuentes de Altman-Z**: aparecen el ejercicio, la sesión del precio, las acciones y los siete importes utilizados.
4. Abre el informe oficial desde ese panel o descarga el PDF de la ficha.

Explicación breve: «Antes faltaba el valor bursátil del cierre. Ahora utilizamos las acciones publicadas en el balance y el precio histórico de esa fecha. Por eso podemos calcular el puntaje sin mezclarlo con la capitalización actual».

Un resultado superior a cinco conserva su valor completo. La interfaz actual muestra el puntaje y sus zonas sin una escala visual limitada a cinco.

## Fuentes y cálculo

Los componentes se revisaron en los siguientes informes:

- [Apple 10-K 2026](https://www.sec.gov/Archives/edgar/data/320193/000032019326000026/aapl-20260926.htm), operaciones y balance, páginas impresas 32 y 34. Acciones expresadas en miles. EBIT operativo publicado; se aclara que excluye otros ingresos netos.
- [Amazon informe 2025](https://s2.q4cdn.com/299287126/files/doc_financials/2026/ar/Amazon-2025-Annual-Report.pdf), operaciones y balance, páginas impresas 37 y 39. EBIT = resultado antes de impuestos + gasto por intereses. Pasivos = activos − patrimonio.
- [Walmart 10-K 2026](https://stock.walmart.com/sec-filings/all-sec-filings/content/0000104169-26-000055/0000104169-26-000055.pdf), operaciones, balance y movimientos patrimoniales, páginas impresas 52, 54 y 55. EBIT = resultado antes de impuestos + intereses de deuda y arrendamiento financiero. Ventas netas excluyen cuotas y otros ingresos; pasivos suman sus partidas, excluyendo participación no controladora rescatable.

Los importes se convierten de millones a USD completos. Se utilizan acciones **en circulación al cierre**, nunca el promedio diluido empleado para beneficio por acción. La capitalización histórica es una estimación calculada con la precisión de las acciones publicadas en el informe.

El precio procede del campo de cierre sin ajustar de las respuestas Yahoo originales ya conservadas en `catalog_prices.json.gz`. Se selecciona la última sesión hasta el cierre contable; en estas referencias hay como máximo un día de diferencia porque dos cierres caen en sábado. No se usa el último precio actual ni el cierre ajustado por dividendos.

El importador verifica símbolo, moneda, SHA-256, fecha y precio contra la copia original; rechaza un desdoblamiento posterior que requiera conciliar la base por acción. La fecha y el informe de esta referencia Altman son independientes de las referencias anuales normalizadas del resto de la ficha.

## Conservación, límites y reproducción

`docs/datos/altman_references.json` conserva los importes revisados, las URLs, las ubicaciones del informe y las huellas de precios. `tools/prepare_altman_references.py` permite reconstruirla usando las mismas cifras revisadas y la copia de precios; no descarga ni interpreta automáticamente nuevos informes.

Desde `backend`, con la configuración local habitual:

```powershell
python manage.py import_altman_references ../docs/datos/altman_references.json --prices ../docs/datos/catalog_prices.json.gz --dry-run
python manage.py import_altman_references ../docs/datos/altman_references.json --prices ../docs/datos/catalog_prices.json.gz
```

La migración 0025 añade un campo independiente al resumen. La carga valida el lote antes de escribir y repetirla no cambia los datos. Las restauraciones financieras generales conservan esta referencia; el arranque habitual la restaura después del resto de los datos.

**Estas ampliaciones añaden cinco fichas, no completan Altman-Z para todo el catálogo.** Las demás mantienen sus resultados anteriores o el estado sin datos cuando falten componentes compatibles. Los bancos no reciben un resultado artificial por tener activos y patrimonio disponibles. Los resúmenes de MSFT, GOOGL y META se conservan. Los métodos adicionales de CAT y KO, incluida la precisión redondeada de sus acciones, figuran en el diagnóstico enlazado al inicio.

## Comprobaciones

- Servidor completo: 537 pruebas aprobadas, una optativa omitida.
- Web: 136 pruebas anteriores aprobadas; las diez pruebas del medidor, incluida una nueva sobre fechas y fuentes, aprobadas. Total cubierto: 137 casos.
- Compilación de producción y comprobaciones Django correctas.
- Recorrido real de ocho fichas en escritorio y ventana estrecha, PDF de Apple descargado, comparaciones anteriores conservadas y sin errores de página.
- Importación repetida: tres referencias validadas, cero actualizaciones.
- Android y pagos sin modificaciones.
