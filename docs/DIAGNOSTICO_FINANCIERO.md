# Diagnóstico de datos ausentes — 24 de noviembre de 2026

Las 504 fichas se revisaron contra las copias financieras y de precios conservadas. El alcance es la disponibilidad de los cinco indicadores del Resumen y Altman-Z; no constituye una auditoría contable de los emisores.

[Listado por empresa](datos/diagnostico_financiero.csv): una fila por ficha, con el estado de cada indicador, los componentes Altman pendientes y el siguiente paso.

## Qué significa cada ausencia

| Indicador | Disponible | Motivo de ausencia |
|---|---:|---|
| Capitalización | 504 | Ninguna ausencia en esta copia |
| PER | 502 | Dos valores no publicados como cifras válidas en la captura |
| ROE | 467 | 33 patrimonios no positivos; tres beneficios netos ausentes; un patrimonio ausente |
| Beta | 500 | Cuatro históricos con menos de 253 sesiones comunes |
| Dividendo | 390 | 95 sin pagos registrados en la ventana; 15 con desdoblamiento sin base por acción conciliada; cuatro con menos de un año de histórico |
| Altman-Z | 180 | 182 referencias por conciliar; 97 modelos sectoriales por revisar; 40 emisores con varios valores cotizados; cinco identidades o referencias pendientes |

Estos casos tienen tratamientos distintos. Un patrimonio no positivo no se corrige inventando un ROE. La falta de eventos de dividendos no prueba por sí sola que la empresa nunca pague o que el rendimiento sea cero. Los históricos cortos no se interpolan para alcanzar un mínimo.

Las fichas muestran el motivo concreto junto al dato ausente. En Salud financiera, Información y fuentes conserva la explicación y el siguiente paso por indicador. El modelo Altman también identifica sus componentes faltantes. El PDF individual conserva estas causas.

## Datos completados en este bloque

Se añadieron referencias Altman para CAT y KO, conservando AAPL, AMZN, WMT y el cálculo original de MSFT.

- **CAT: 4,25**, ejercicio 2025. [10-K original](https://www.sec.gov/Archives/edgar/data/18230/000001823026000008/cat-20251231.htm): acciones al cierre, capital de trabajo, activos, beneficios retenidos, beneficio operativo, pasivos y ventas/ingresos consolidados. El grupo incluye Productos Financieros; el método explica esa composición y no certifica validez predictiva del modelo para el grupo.
- **KO: 4,76**, ejercicio 2025. [10-K original](https://investors.coca-colacompany.com/filings-reports/annual-filings-10-k/content/0001628280-26-010047/ko-20251231.htm): las acciones del cierre se estiman con las cifras redondeadas del balance, 7.040 millones emitidas menos 2.738 millones en tesorería. No se usa la cifra de portada de una fecha posterior. EBIT usa beneficio antes de impuestos más intereses; pasivos excluyen todo el patrimonio, incluidas participaciones no controladoras.

Ambas referencias emplean el cierre sin ajustar de la última sesión hasta el cierre contable, verificado contra la copia de precios. Los métodos y sus fuentes se muestran en el detalle. Son puntajes académicos, no recomendaciones ni probabilidades de quiebra.

## Cómo continuar con los pendientes

1. Revisar en otra fuente los beneficios de AEE, ISRG y SKYD, y el patrimonio de WTW. Deben corresponder a los mismos ejercicios; este bloque los deja pendientes.
2. Consultar la información de resultados de BAX y TTWO antes de decidir si existe un PER interpretable. La captura actual no proporciona ese dato.
3. Para Altman, conciliar cierres reales, acciones por clase y precios del mismo cierre; después revisar los restantes componentes y la aplicabilidad del modelo al sector.
4. Para dividendos, verificar eventos y ajustes por acción. Para beta, adquirir sesiones comunes adicionales cuando existan. No completar series con valores sintéticos.

La ampliación SEC eleva la cobertura a 180 puntuaciones; 324 fichas siguen sin un cálculo publicable. Procedencia y reproducción: [ALTMAN_CATALOGO_SEC.md](ALTMAN_CATALOGO_SEC.md).

### Revisión posterior de cobertura y presentación

La auditoría anterior a la ampliación SEC confirmó 6 resultados disponibles, 439 valoraciones del cierre pendientes y 59 casos con más componentes ausentes. Ese fue el punto de partida de las 174 referencias adicionales descritas en la ampliación enlazada arriba.

Para ampliar la cobertura:

1. Revisar primero el grupo de 439: obtener el cierre fiscal real del informe, el número de acciones en circulación de esa misma fecha y el cierre bursátil sin ajustar compatible con las acciones. Conciliar también los importes del balance; la fecha normalizada del proveedor no acredita el cierre real.
2. Revisar después los 59 con más carencias: además de la valoración, completar los componentes identificados por empresa en el listado.
3. Registrar informe, ubicación de cada cifra, moneda, fechas, método y precios en referencias verificadas. Validar con el importador existente antes de publicar y volver a ejecutar la auditoría de cobertura.
4. Revisar la aplicabilidad antes de presentar una puntuación como comparable. La fórmula original se desarrolló principalmente para manufactureras; otras variantes tienen coeficientes y límites diferentes. Cualquier modelo adicional debe identificarse y documentarse por separado.

La entrevista de [Edward Altman en CFA Institute](https://rpc.cfainstitute.org/blogs/enterprising-investor/2016/the-altman-z-score-in-edward-altmans-own-words) explica el alcance del modelo original y sus variantes. Una nota de cobertura de datos o un promedio parcial no sustituye el puntaje financiero.

El detalle visual ahora ocupa el ancho de la ficha, elimina el contenedor claro dentro de otro oscuro cuando el cálculo está pendiente y reúne el siguiente paso técnico con la metodología. El motivo y el estado siguen visibles.

## Reproducción y conservación

La migración 0026 añade diagnósticos independientes a FinancialMetrics. Una huella liga cada diagnóstico al contenido del resumen; si cambian sus datos o su fuente, la API deja de mostrar el motivo anterior como vigente. Los ceros conocidos se mantienen como valores disponibles. La importación financiera conserva las referencias y diagnósticos, y el arranque los verifica después de cargar los datos.

Desde backend, con la configuración local habitual:

```powershell
python manage.py audit_financial_availability ../docs/datos/catalog_financials.json.gz --prices ../docs/datos/catalog_prices.json.gz --csv ../docs/datos/diagnostico_financiero.csv
```

El comando verifica identidades y huellas antes de escribir. No modifica cifras financieras ni accede a la red; clasifica las copias ya capturadas y permite --dry-run. El JSON local con conteos queda en ../output/financial_availability.json.

Verificación: 545 pruebas de servidor aprobadas, una optativa omitida; 138 pruebas completas de web y dos nuevas pruebas de presentación aprobadas; las seis pruebas del motor de causas se repitieron tras un ajuste de redacción. Compilación de producción y comprobaciones Django correctas. Diez fichas verificadas en escritorio y ventana estrecha, PDF de Apple descargado y comparaciones previas conservadas. Android y pagos sin modificaciones.
