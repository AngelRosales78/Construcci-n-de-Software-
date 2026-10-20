# Contrato propuesto para web y Android

Actualización del 20 de octubre de 2026. Un contrato es el acuerdo sobre qué solicita una pantalla y cómo responde el servidor. La ruta de pilares ya está implementada para Liquidez, Rentabilidad y Solvencia; Angel aún no ha confirmado el acuerdo ni su integración Android. La implementación sigue la propuesta de T65, con las precisiones que se indican a continuación.

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

El servidor devuelve los cinco identificadores de pilares. Liquidez, Rentabilidad y Solvencia incluyen siete indicadores calculables. Eficiencia y Crecimiento siguen con `metrics: []` y `reason: not_implemented`; sus historias son del siguiente avance. Las notas y el puntaje general están no disponibles porque la normalización aún no está configurada. Un año es un ejercicio fiscal, no necesariamente de enero a diciembre. Un año solicitado sin datos no se cambia por otro.

Sin `fiscal_year` se usa el último ejercicio importado; `available_years` enumera los disponibles. Si no existe ninguno, el año y fechas son nulos. No es una cotización en tiempo real. El modelo de componentes se identifica como `annual-ratios-v1` y su estado indica que falta revisión del equipo. Ejemplo completo real: [MSFT FY2024](evidencias/sprint2_MSFT_pillars_2024.json).

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

Otros motivos implementados: `zero_denominator`, `invalid_denominator`, `nonpositive_equity`, `insufficient_history`, `invalid_components`, `unconfigured_scoring`. `explanation` ofrece el mensaje legible; `formula_display` y el `label` de cada componente permiten explicarlo en español. `value` conserva el resultado decimal con precisión de cálculo y `display_value` aplica dos decimales HALF_UP con coma. Los colores usan `thresholds`, con versión, límites, dirección y explicación. No son notas de 0 a 100.

## Errores y conservación de compatibilidad

- 401: sesión ausente o expirada, con el mecanismo de renovación ya existente.
- 404: ticker no encontrado. Empresa existente con año sin datos: respuesta 200 con periodo solicitado e indicadores no disponibles; no inventar cobertura.
- 400: parámetro inválido, como un año no numérico.
- 503: dependencia externa necesaria temporalmente no disponible; no usarlo para un dato simplemente ausente.
- No renombrar ni eliminar campos de la ficha actual. No reutilizar `altman_z.score` para el nuevo puntaje.

## Pendientes del acuerdo con Angel

Revisar nombres de campos, estados y formato decimal implementados; conectar Android y comparar sus siete indicadores con la respuesta real de MSFT. Esta implementación fue autorizada como avance por criterio de Diego; no se registra una aceptación de Angel. Las notas y ponderaciones siguen pendientes.
