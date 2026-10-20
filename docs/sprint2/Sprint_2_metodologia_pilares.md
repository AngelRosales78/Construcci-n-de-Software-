# Fórmulas propuestas para los cinco pilares

Preparación de T65. Versión `pilares-propuesta-v1`. Son decisiones de implementación propuestas para revisar con Angel antes de cerrar el contrato; no se afirma aprobación ni implementación.

## Reglas comunes

- Usar balances anuales completos de una empresa, misma moneda y unidad. No mezclar valores trimestrales con resultados anuales.
- Promedio de un saldo = (saldo al inicio + saldo al final del ejercicio) / 2. El saldo inicial viene del cierre inmediatamente anterior.
- Si falta un componente, devolver valor nulo y explicar el faltante. Cero y dato ausente son distintos.
- No dividir entre cero. No sustituir un denominador ausente por uno ni inventar datos.
- Conservar valor sin redondear para calcular; presentar ratios y porcentajes con dos decimales, redondeo HALF_UP. El servidor entregará el texto de presentación para evitar redondeos distintos entre clientes.
- Mostrar fuente, periodo, fórmula y componentes. Los cálculos no equivalen a una recomendación de compra.

## Liquidez HU05_02

| Indicador | Fórmula | Qué explica |
|---|---|---|
| Liquidez corriente | Activos corrientes / pasivos corrientes | Recursos de corto plazo frente a obligaciones de corto plazo. |
| Prueba ácida | (Efectivo + inversiones a corto plazo + cuentas por cobrar netas) / pasivos corrientes | Cobertura con activos de mayor liquidez, excluyendo inventarios y anticipos. |

Pasivos corrientes deben ser mayores que cero. La prueba ácida requiere sus tres componentes explícitos; la falta de inversiones no se interpreta como cero. Se adopta esta definición para evitar incluir otros activos corrientes que no sean líquidos. Base: [OpenStax sobre ratios de liquidez](https://openstax.org/books/principles-finance-2e/pages/6-3-liquidity-ratios).

## Rentabilidad HU05_03

| Indicador | Fórmula | Unidad |
|---|---|---|
| ROE | Beneficio neto / patrimonio contable promedio × 100 | Porcentaje |
| ROA | Beneficio neto / activos totales promedio × 100 | Porcentaje |
| Margen neto | Beneficio neto / ingresos anuales × 100 | Porcentaje |

Para ROE, si el patrimonio inicial o final es no positivo, mostrar No disponible con la explicación de patrimonio no positivo. Activos promedio e ingresos deben ser positivos. Las pérdidas sí pueden producir porcentajes negativos. No recalcular ROE usando capitalización bursátil. No mostrar referencia sectorial hasta definir universo, cobertura mínima y fecha. Base de las fórmulas: [OpenStax sobre análisis financiero](https://openstax.org/books/principles-financial-accounting/pages/a-financial-statement-analysis).

## Solvencia HU05_04

| Indicador | Fórmula propuesta | Regla |
|---|---|---|
| Pasivos sobre patrimonio | Pasivos totales / patrimonio contable al cierre | Patrimonio positivo. Se etiquetará explícitamente así. |
| Cobertura de intereses | EBIT / gasto por intereses anual | Intereses mayores que cero; EBIT negativo es un resultado válido. |

Para esta versión, la palabra deuda del backlog se interpreta como pasivos totales, siguiendo la variante de ratio de [OpenStax sobre solvencia](https://openstax.org/books/principles-finance-2e/pages/6-4-solvency-ratios). El nombre visible evita confundirlo con deuda financiera con intereses. Si el equipo quiere esta última, deberá cambiar expresamente la definición y los campos antes de implementar.

EBIT se deriva de beneficio antes de impuestos + gasto por intereses del mismo informe y ejercicio. Se conserva esta definición del preparador actual, identificándola en la metodología; no se intercambia silenciosamente con otro subtotal. `equity_value` actual representa valor de mercado en el CSV y no debe reutilizarse como patrimonio contable.

## Eficiencia HU05_05

| Indicador | Fórmula | Regla |
|---|---|---|
| Rotación de activos | Ingresos anuales / activos totales promedio | Activos promedio positivos. |
| Días de inventario | Inventario promedio / costo de ventas anual × 365 | Costo positivo e inventario aplicable al negocio. |

Se adopta una convención anual de 365 días, también en años bisiestos, y se documenta en la respuesta. Si el inventario no aplica, el estado será No aplica; si no se sabe, será No disponible. Un inventario cero expresamente reportado puede producir cero días. Mostrar hasta tres años calculables, sin fabricar años faltantes. Bases: [rotación de activos](https://openstax.org/books/principles-financial-accounting/pages/a-financial-statement-analysis) y [eficiencia del inventario](https://openstax.org/books/principles-financial-accounting/pages/10-5-examine-the-efficiency-of-inventory-management-using-financial-ratios).

## Crecimiento y radar HU05_01

| Indicador | Fórmula | Datos necesarios |
|---|---|---|
| Variación anual de ingresos | (Ingresos del año / ingresos del año anterior − 1) × 100 | Dos ejercicios consecutivos; ingresos anteriores positivos. |
| Crecimiento compuesto a tres años | ((Ingresos finales / ingresos de tres años antes) elevado a 1/3 − 1) × 100 | Cuatro cierres anuales consecutivos; ingreso inicial positivo y final no negativo. |

Las fórmulas de crecimiento son la convención propuesta del proyecto. No confundir crecimiento de ingresos con rentabilidad de una acción. El inventario local confirma ingresos FY2021, FY2022, FY2023 y FY2024; falta llevarlos a almacenamiento anual.

## Notas del radar y puntaje general HU04_05

Las fórmulas anteriores producen ratios; no producen automáticamente una nota de 0 a 100. Antes de implementar el puntaje se debe registrar una tabla versionada de conversión para cada indicador y cómo se combinan en cada pilar.

Propuesta inicial para discutir: cinco pilares con igual peso del 20 %; puntaje = suma de nota de cada pilar × 0,20. Esto queda condicionado a acordar las reglas de normalización y los umbrales, que todavía están pendientes. No se adoptarán umbrales universales sin explicar su alcance.

Sin las cinco notas válidas, mostrar No disponible con el pilar o regla que falta. Nunca reemplazar este puntaje con Altman Z ni pintar un valor faltante como cero en el radar.

## Ejemplo didáctico para comprobar mañana

Activos corrientes 200 y pasivos corrientes 100 producen liquidez corriente 2,00 veces. Con efectivo 50, inversiones 20 y cuentas por cobrar 40, la prueba ácida es 1,10 veces. Son cifras ficticias de prueba, no datos de Microsoft. Con pasivos corrientes cero, ambos indicadores deben informar No disponible.
