# Contratos Android pendientes de servidor

Estado revisado el 25/10/2026. Las ramas disponibles implementan catálogo, ficha, histórico y pilares. No implementan simulaciones ni explicaciones de IA. Las siguientes rutas son propuestas del cliente Android, **no endpoints publicados por Diego**. La app muestra el error del servicio y permite reintentar; no usa respuestas ficticias. Los ejemplos automáticos se limitan a pruebas JVM.

## Escenarios HU06_01 a HU06_05

- GET /api/v1/companies/{ticker}/simulations/defaults/ devuelve schema_version=simulation-defaults-v1, ticker, annual_return, annual_volatility, min_return, max_return, min_volatility, max_volatility, seed y paths.
- Los retornos y volatilidades son fracciones anuales (0.05 = 5 %); la interfaz presenta porcentajes. Los límites provienen del servidor, sin suponer valores iniciales de mercado.
- POST /api/v1/companies/{ticker}/simulations/ recibe annual_return, annual_volatility, seed, paths y horizon_months=12.
- Devuelve schema_version=simulation-v1, ticker, simulation_id, currency, model_version, generated_at, start_date, end_date, horizon_months, parameters (los efectivos), sources (lista de fuentes), history opcional y points. Cada point tiene date, p10, p50 y p90 de una sola ejecución.
- El cliente exige doce meses, fechas ordenadas, percentiles finitos no negativos P10 <= P50 <= P90, coincidencia de empresa/semilla/parámetros y procedencia. Ocultar curvas conserva el resultado, la escala y la tabla; restablecer recupera los supuestos originales del servidor y solicita una nueva ejecución común.
- Los supuestos no equivalen a crecimiento de ventas. Android no implementa Monte Carlo ni recalcula percentiles.

## IA HU07_01 a HU07_05

POST /api/v1/companies/{ticker}/analysis/ recibe fiscal_year y level (simple o technical).

Respuesta propuesta: schema_version=analysis-v1, ticker, fiscal_year, level, metrics_version, data_as_of, generated_at, period={start,end}, validation={status:passed,checked_at}, summary (uno a tres párrafos), pillars (exactamente cinco secciones), strengths y risks (hasta cinco cada una, sin completar listas artificialmente), sources={label,url}[]. Cada sección contiene pillar_id, text y metric_ids. Cada fortaleza/riesgo tiene text y metric_ids.

El cliente comprueba empresa, ejercicio, nivel, periodo, estructura, referencias conocidas y validación declarada. Presenta texto plano y solo abre fuentes HTTP/HTTPS sin credenciales embebidas. La validación numérica de las afirmaciones **corresponde al servidor HU07_04**; el cliente no demuestra que una cifra sea cierta solo porque la respuesta diga passed. Faltan ese servicio y el proveedor real de IA. No se guardan claves de proveedor en Android.

No hay caché persistente de análisis en la app. Solo se conserva el último análisis válido de la misma empresa/ejercicio si falla el cambio de lenguaje, con su nivel original visible. Se borra al cambiar empresa o año. La caché de servidor debe separar empresa, snapshot/versión de datos, nivel y política; su implementación no forma parte del cliente Android.

## Límites de cierre

Las pantallas y adaptadores están implementados; la integración de las épicas 6 y 7 sigue pendiente de esos servicios y del acuerdo de contrato. No se registran como historias integrales terminadas. T45, T49, T53, T57, T61 y T67 son trabajo del servidor/proveedor de IA y quedan fuera de la solicitud de Android. T66 y el burndown requieren acuerdos y horas reales del equipo: no se inventan.
