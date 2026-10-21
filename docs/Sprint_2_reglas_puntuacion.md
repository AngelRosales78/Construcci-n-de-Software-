# Cómo calculamos las notas y el puntaje

Versión `academic-pillar-scores-v1`, implementada el 21 de octubre de 2026. Es una **convención académica del proyecto**, pendiente de revisión del equipo. No es un ranking sectorial, una probabilidad de ganancia ni un modelo validado por el profesor. Se publica la regla para que Diego pueda reproducirla y explicarla.

## Primero calculamos indicadores reales

Los indicadores usan los componentes anuales con fuente y periodo. Las fórmulas siguen la [metodología de los pilares](Sprint_2_metodologia_pilares.md), versión implementada `annual-ratios-v2`. Las notas son una transformación posterior, separada de los datos originales y de Altman Z.

## Después convertimos cada indicador en una nota

En la tabla, cada pareja significa indicador → nota. Entre dos puntos se usa una línea recta. Fuera de los extremos se conserva la nota del extremo, siempre entre 0 y 100. Los porcentajes se expresan en puntos porcentuales: ROE 10 significa 10 %, no 0,10.

| Indicador | Unidad del indicador | Puntos de conversión |
|---|---|---|
| Liquidez corriente | Veces | 0 → 0; 1 → 50; 2 → 100 |
| Prueba ácida | Veces | 0 → 0; 0,5 → 50; 1 → 100 |
| ROE | Porcentaje | 0 → 0; 10 → 50; 20 → 100 |
| ROA | Porcentaje | 0 → 0; 5 → 50; 10 → 100 |
| Margen neto | Porcentaje | 0 → 0; 10 → 50; 20 → 100 |
| Pasivos sobre patrimonio | Veces | 0 → 100; 1 → 75; 2 → 50; 4 → 0 |
| Cobertura de intereses | Veces | 0 → 0; 1 → 50; 3 → 100 |
| Rotación de activos | Veces | 0 → 0; 0,5 → 50; 1 → 100 |
| Días de inventario | Días | 0 → 100; 90 → 50; 180 → 0 |
| Variación anual de ingresos | Porcentaje | 0 → 0; 5 → 50; 20 → 100 |
| Crecimiento compuesto a tres años | Porcentaje | 0 → 0; 5 → 50; 20 → 100 |

Ejemplo: liquidez corriente 1,5 está a mitad de camino entre 1 y 2, por eso recibe 75 puntos. En pasivos sobre patrimonio y días de inventario, la regla de esta versión asigna más puntos a valores menores. Son simplificaciones académicas: no establecen un óptimo universal. Empresas de distintos sectores pueden necesitar otros puntos y no deben presentarse como comparables automáticamente.

Los puntos se configuran en `backend/api/pillar_scoring.py` y también se incluyen en `scoring_policy.anchors` de la API. Cambiar las reglas requiere una nueva versión y sus pruebas; no se mezclan notas de versiones distintas.

## Luego calculamos la nota de cada pilar

- Liquidez: promedio de las notas de liquidez corriente y prueba ácida.
- Rentabilidad: promedio de las notas de ROE, ROA y margen neto.
- Solvencia: promedio de las notas de pasivos sobre patrimonio y cobertura de intereses.
- Eficiencia: promedio de las notas de rotación de activos y días de inventario.
- Crecimiento: promedio de las notas de variación anual y tasa compuesta a tres años.

Si falta un indicador requerido, la nota del pilar es No disponible. No se rellena con cero y tampoco se ignora para aumentar artificialmente la cobertura.

Única exclusión de esta versión: inventario expresamente clasificado como no aplicable, con motivo documentado en el balance anual. En ese caso Eficiencia usa solo la rotación de activos y publica la exclusión. El campo `inventory_applicable: false` requiere `inventory_applicability_note`. Un inventario cero reportado sigue siendo un dato válido; no significa que no aplique. Si se desconoce la aplicabilidad y faltan componentes, la nota de Eficiencia no se calcula.

## Finalmente calculamos el puntaje general

Cada uno de los cinco pilares pesa 20 %:

```text
Puntaje = 0,20 × (Liquidez + Rentabilidad + Solvencia + Eficiencia + Crecimiento)
```

Se necesitan las cinco notas, válidas y de la misma versión. Si falta una, el resultado es No disponible y se identifica el pilar pendiente. No se redistribuye su peso entre los demás.

Se calcula con decimales sin redondeo previo de las notas. Solo al presentar se redondea a dos decimales con HALF_UP. El servidor entrega `display_value` tanto para cada nota como para el puntaje; la web y Android deben mostrar ese texto, sin reconstruir el promedio a partir de las notas visibles.

## Ejemplo de Microsoft FY2024

| Pilar | Nota visible | Peso |
|---|---|---|
| Liquidez | 81,87 | 20 % |
| Rentabilidad | 100,00 | 20 % |
| Solvencia | 88,65 | 20 % |
| Eficiencia | 73,96 | 20 % |
| Crecimiento | 81,78 | 20 % |

El resultado es **85,25/100** bajo esta convención. Sus componentes corresponden al ejercicio terminado el 30 de junio de 2024. No se presenta como evaluación actual de 2026 ni como recomendación de inversión. El ejemplo completo conserva los valores de cálculo sin redondear en la [respuesta de la API del día 3](evidencias/sprint2_MSFT_pillars_2024_dia3.json).
