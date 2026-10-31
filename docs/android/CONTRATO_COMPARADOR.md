# Contrato Android propuesto para comparación

La ruta NO existe en el servidor heredado de Script 2. Este documento permite acordar su implementación con Diego sin cambiar Web ni servidor en este trabajo Android.

GET `/api/v1/compare/?a=AAPL&b=MSFT&fiscal_year=2024`

Autenticación Bearer de la sesión existente. `a` y `b` son tickers distintos del catálogo; el ejercicio es entero. Mantener A y B en el orden solicitado. No cambiar silenciosamente el ejercicio si falta un balance.

Respuesta propuesta:

```json
{
  "schema_version": "comparison-v1",
  "fiscal_year": 2024,
  "status": "available",
  "a": { "schema_version": "pillars-v1", "ticker": "AAPL", "fiscal_year": 2024 },
  "b": { "schema_version": "pillars-v1", "ticker": "MSFT", "fiscal_year": 2024 },
  "comparisons": []
}
```

Los objetos `a` y `b` deben incluir el objeto completo de pilares existente: periodo, moneda, metodología, versión de normalización, notas, métricas, fuentes y avisos. El ejemplo anterior es abreviado y no es una respuesta de producción para la tabla.

`comparisons` debe incluir una fila por cada eje vigente del servidor: liquidity, profitability, solvency, efficiency y growth. Este último es el eje actualmente implementado en Script 2; no se cambia a valuación solo en Android.

Cada fila incluye `id`, `status` (available o not_comparable), `result` (a, b, tie o not_comparable), `delta` decimal como cadena, `unit` (points) y `reason`. Delta significa nota A menos nota B. La API debe determinar comparabilidad y resultado con notas de la misma metodología y explicar las diferencias de periodos. No comparar ratios brutos asumiendo que mayor siempre es mejor. Android no reconstruye el resultado ni completa una fila omitida con cero.

400: parámetros inválidos o empresas repetidas; 401/403: sesión o permisos insuficientes; 404: empresa o servicio no disponible; 503: dependencia no disponible. No enviar contenido privado ante 403. La aceptación conjunta queda pendiente de que este contrato se confirme e implemente y se verifique contra la Web.
