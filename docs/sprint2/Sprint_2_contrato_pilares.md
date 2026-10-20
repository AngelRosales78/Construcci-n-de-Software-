# Contrato propuesto para web y Android

Preparación de T65. Un contrato es el acuerdo sobre qué solicita una pantalla y cómo responde el servidor. Este documento prepara la conexión; los endpoints nuevos todavía no existen y Angel no ha confirmado el acuerdo.

## Conservar las rutas existentes

| Consulta | Ruta real actual |
|---|---|
| Buscar y filtrar | `GET /api/v1/companies/search/` |
| Autocompletar | `GET /api/v1/companies/autocomplete/?q=MS` |
| Ver ficha | `GET /api/v1/companies/MSFT/summary/` |
| Ver histórico | `GET /api/v1/companies/MSFT/history/?range=1Y` |

Requieren el token de acceso mediante `Authorization: Bearer …`. La web ya usa estas rutas desde `frontend/src/models/companyRepository.js`. Android puede conectarlas para su trabajo de hoy; no necesita esperar a que existan los nuevos pilares.

## Nueva consulta propuesta

`GET /api/v1/companies/MSFT/pillars/?fiscal_year=2024`

El servidor devolvería los cinco pilares, sus indicadores y el puntaje general. Un año es un ejercicio fiscal, no necesariamente de enero a diciembre. La cobertura por año debe ser explícita; no cambiar silenciosamente a otro periodo si se solicita uno sin datos.

| Campo | Contenido |
|---|---|
| `schema_version` | Versión de la estructura, inicialmente `pillars-v1`. |
| `methodology_version` | Versión de fórmulas y reglas aprobadas. La propuesta actual aún no está aprobada. |
| `ticker`, `fiscal_year`, `period` | Empresa y fechas exactas del ejercicio. |
| `pillars` | Cinco elementos con identificadores `liquidity`, `profitability`, `solvency`, `efficiency`, `growth`. |
| `metrics` de cada pilar | Identificador, valor, unidad, fórmula, componentes, periodo y fuentes. |
| `score` de cada pilar | Nota entre 0 y 100 o valor nulo con motivo. |
| `overall_score` | Puntaje general calculado con las cinco notas o valor nulo con motivo. |
| `source` de cada componente | URL, etiqueta SEC, unidad, filing y fecha de publicación del dato. |
| `history` cuando corresponda | Hasta tres ejercicios calculables de eficiencia, cada uno con sus fechas. |

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

Otros motivos previstos: `zero_denominator`, `nonpositive_equity`, `insufficient_history`, `unconfigured_scoring`. Los nombres definitivos se fijarán al implementar y se verificarán con pruebas del contrato.

## Errores y conservación de compatibilidad

- 401: sesión ausente o expirada, con el mecanismo de renovación ya existente.
- 404: ticker no encontrado. Empresa existente con año sin datos: respuesta 200 con periodo solicitado e indicadores no disponibles; no inventar cobertura.
- 400: parámetro inválido, como un año no numérico.
- 503: dependencia externa necesaria temporalmente no disponible; no usarlo para un dato simplemente ausente.
- No renombrar ni eliminar campos de la ficha actual. No reutilizar `altman_z.score` para el nuevo puntaje.

## Pendientes del acuerdo con Angel

Confirmar nombres de campos, estados, formato decimal y comportamiento del año sin datos. Implementar la ruta y su documentación después de ese acuerdo. Hoy queda preparada la propuesta; no se publica una ruta ficticia como funcional.
