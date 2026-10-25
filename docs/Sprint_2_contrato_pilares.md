# Contrato implementado para web y Android

Actualización del 21 de octubre de 2026. La ruta de pilares entrega los cinco pilares, once indicadores, sus notas y el puntaje general cuando existe cobertura suficiente. Angel aún no ha confirmado el acuerdo ni su integración Android. La estructura sigue la propuesta de T65 y añade los campos descritos aquí.

## Conservar las rutas existentes

| Consulta | Ruta real actual |
|---|---|
| Buscar y filtrar | `GET /api/v1/companies/search/` |
| Autocompletar | `GET /api/v1/companies/autocomplete/?q=MS` |
| Ver ficha | `GET /api/v1/companies/MSFT/summary/` |
| Ver histórico | `GET /api/v1/companies/MSFT/history/?range=1Y` |

Requieren el token de acceso mediante `Authorization: Bearer …`. La web ya usa estas rutas desde `frontend/src/models/companyRepository.js`. Android puede conectarlas para su trabajo de hoy; no necesita esperar a que existan los nuevos pilares.

## Consulta implementada

`GET /api/v1/companies/MSFT/pillars/?fiscal_year=2024`

El servidor devuelve cinco pilares y once indicadores. Eficiencia incorpora rotación de activos y días de inventario; Crecimiento incorpora variación anual y tasa compuesta a tres años. Las notas usan `academic-pillar-scores-v1`, con las [reglas de conversión documentadas](Sprint_2_reglas_puntuacion.md). Sin cinco notas válidas de la misma versión, el puntaje es No disponible; no se publica un promedio parcial. Un año solicitado sin datos no se cambia por otro.

Sin `fiscal_year` se usa el último ejercicio importado; `available_years` enumera los disponibles. Si no existe ninguno, el año y fechas son nulos. No es una cotización en tiempo real. Las fórmulas se identifican como `annual-ratios-v2` y la metodología indica su revisión pendiente del equipo. Ejemplo completo vigente: [MSFT FY2024 día 3](evidencias/sprint2_MSFT_pillars_2024_dia3.json). Se conserva la respuesta del día 2 como evidencia histórica.

| Campo | Contenido |
|---|---|
| `schema_version` | Versión de la estructura, inicialmente `pillars-v1`. |
| `methodology_version` | Versión implementada de fórmulas. `methodology_status` distingue la implementación de la aprobación pendiente del equipo. |
| `ticker`, `fiscal_year`, `period` | Empresa y fechas exactas del ejercicio. |
| `pillars` | Cinco elementos con identificadores `liquidity`, `profitability`, `solvency`, `efficiency`, `growth`. |
| `metrics` de cada pilar | Identificador, valor, unidad, fórmula, componentes, periodo y fuentes. |
| `score` de cada pilar | Nota entre 0 y 100 o valor nulo con motivo. |
| `overall_score` | Puntaje general calculado con las cinco notas o valor nulo con motivo. |
| `source` de cada componente | URL, etiqueta SEC, unidad, filing y fecha de publicación del dato. |
| `history` cuando corresponda | Hasta tres ejercicios calculables de eficiencia, cada uno con sus fechas. |
| `scoring_policy` | Versión académica, anclajes de conversión, reglas de agregación y pesos del puntaje. |

Cada nota de pilar contiene `status`, `value`, `display_value`, `version`, `period`, `parts`, `missing_metrics` y `excluded_metrics`. Las partes muestran el valor original, sus anclajes y la nota derivada. El puntaje contiene `contributions`, con los cinco pesos del 20 %, y `missing_pillars` si falta cobertura. Los clientes usan `display_value` y no recalculan el resultado. Para dibujar el radar pueden convertir `value` a número, únicamente cuando la nota está disponible.

El historial de Eficiencia está ordenado de más reciente a más antiguo, con hasta tres ejercicios existentes no posteriores al solicitado. Un año sin cierre previo puede mostrar métricas no disponibles en su fila; no se inventa el año ni se toman cifras posteriores. La tasa compuesta exige los cuatro cierres consecutivos, su misma moneda e ingresos disponibles en los cuatro.

En la importación anual, `inventory_applicable` es opcional y admite nulo si se desconoce. Si vale `false`, se requiere `inventory_applicability_note` para documentar por qué no aplica. No se deduce ausencia de inventario a partir de un valor cero.

El estado del pilar puede ser `available`, `partial` o `unavailable`, según la cobertura de sus métricas. El estado de cada métrica sigue la tabla siguiente. La referencia sectorial de Rentabilidad declara `insufficient_sector_sample`; no se inventa una media con una sola empresa.

## Estados que las pantallas deben distinguir

| Estado | Qué significa | Qué verá el usuario |
|---|---|---|
| `available` | El indicador se pudo calcular. | Valor y unidad, incluso si es cero o negativo cuando sea válido. |
| `unavailable` | Falta información, falta una regla de puntuación o el divisor no sirve. | No disponible y un motivo. |
| `not_applicable` | La métrica no corresponde al negocio, con base documentada. | No aplica y explicación. |

Los errores de conexión y la espera de la pantalla se manejan aparte de esos estados financieros. La falta de un balance no es una falla del servidor.

## Ejemplo de una métrica sin datos

```json
{
  "id": "current_ratio",
  "label": "Liquidez corriente",
  "status": "unavailable",
  "value": null,
  "display_value": "No disponible",
  "unit": "times",
  "reason": "missing_components",
  "missing_fields": ["current_assets", "current_liabilities"],
  "formula": "current_assets / current_liabilities",
  "components": []
}
```

Este ejemplo describe una empresa sin componentes importados. No afirma que esos campos falten en el archivo SEC original de Microsoft. Cuando estén disponibles, `value` se entregará como texto decimal sin redondear, por ejemplo `"2"`, y `display_value` como `"2,00"`. La unidad se mostrará por separado. Los clientes no calcularán fórmulas ni convertirán `null` a cero.

Motivos implementados para indicadores: `zero_denominator`, `invalid_denominator`, `nonpositive_equity`, `insufficient_history`, `invalid_components`, `missing_components` e `inventory_not_applicable`. Para notas: `missing_metrics`; para puntaje: `incomplete_pillars`. `explanation` ofrece el mensaje legible; `formula_display` y el `label` de cada componente permiten explicarlo en español. `value` conserva el resultado decimal con precisión de cálculo y `display_value` aplica dos decimales HALF_UP con coma. Los colores de ratios usan `thresholds`; sus tramos son distintos de las notas numéricas del radar.

## Errores y conservación de compatibilidad

- 401: sesión ausente o expirada, con el mecanismo de renovación ya existente.
- 404: ticker no encontrado. Empresa existente con año sin datos: respuesta 200 con periodo solicitado e indicadores no disponibles; no inventar cobertura.
- 400: parámetro inválido, como un año no numérico.
- 503: dependencia externa necesaria temporalmente no disponible; no usarlo para un dato simplemente ausente.
- No renombrar ni eliminar campos de la ficha actual. No reutilizar `altman_z.score` para el nuevo puntaje.

## Pendientes del acuerdo con Angel

Revisar nombres de campos y convención de puntuación; conectar Android y comparar sus once indicadores, cinco notas y puntaje con la respuesta real de MSFT. La implementación se realizó con autorización de Diego para continuar por criterio; no se registra una aceptación de Angel. Las tareas móviles del día 3 son T15, T19 y T23.
