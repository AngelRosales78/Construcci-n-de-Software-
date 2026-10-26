# Alcance y evidencia Android — 25/10/2026

| Historia / tarea móvil | Implementación | Evidencia y límite |
|---|---|---|
| T65 conexión de ficha | Catálogo, ficha e histórico del servidor; selección por ticker | Retrofit contra Django real; no recorrido visual en emulador |
| HU05_02 T03/T04 | Liquidez corriente y prueba ácida, componentes, periodo, umbrales y fuentes | Pruebas JVM y respuesta real MSFT FY2024 |
| HU05_03 T07/T08 | ROE, ROA, margen neto y referencia sectorial a nivel de pilar | Casos negativos/ausentes, muestra insuficiente y contrato real |
| HU05_04 T11/T12 | Pasivos/patrimonio y cobertura de intereses, sin confundir Altman Z | SolvencyTest y contrato real |
| HU05_05 T15/T16 | Rotación y días de inventario, hasta tres ejercicios existentes | EfficiencyTest, No aplica distinto de cero |
| HU05_01 T19/T20 | Radar de cinco ejes y detalle de crecimiento | RadarGrowthTest; pruebas de interfaz compiladas, no ejecutadas |
| HU04_05 T23/T24 | Puntaje y cinco contribuciones, periodo y versión comunes | OverallScoreTest y consulta real: MSFT 2024 = 85,25/100 |
| HU06_01 T27/T28 | Escenario base, supuestos, semilla e identificador | Cliente y pruebas de contrato; servidor pendiente |
| HU06_02 T31/T32 | P90 de la misma ejecución | Validación de percentiles compartidos; servidor pendiente |
| HU06_03 T35/T36 | P10 de la misma ejecución | Rechazo de orden/fechas incoherentes; servidor pendiente |
| HU06_04 T39/T40 | Controles con límites del servidor, validación y restablecimiento | ScenariosViewModelTest; servidor pendiente |
| HU06_05 T43/T44 | Tres curvas con escala común, visibilidad local y tabla final | Ocultar no recalcula; servidor pendiente |
| HU07_04 T47/T48 | Estado de validación, fecha y fuentes | Cliente rechaza respuestas no validadas; la verificación de cifras es responsabilidad del servidor pendiente |
| HU07_01 T51/T52 | Resumen de hasta tres párrafos, carga, error y reintento | Análisis sin caché persistente y sin reutilización entre empresa/año; servicio pendiente |
| HU07_02 T55/T56 | Cinco secciones con referencias a métricas del pilar | AnalysisValidationTest; servicio pendiente |
| HU07_03 T59/T60 | Hasta cinco fortalezas y riesgos, sin completar listas | Rechazo de referencias desconocidas y estructuras inválidas; servicio pendiente |
| HU07_05 T63/T64 | Selector sencillo/técnico, preserva último análisis válido al fallar | AnalysisViewModelTest; servicio pendiente |
| T68 integración compartida | Pilares y ficha probados por HTTP real | No certifica comparación visual web/Android ni escenarios/IA reales |
| T69 evidencia | Reportes JSON, pruebas y commits | Sin capturas ni horas/burndown inventados |

No se registra Planning Poker ni aprobación de metodología por el equipo. La implementación del servidor financiero se identifica por su rama y versión, conservando su estado pendiente de revisión colectiva.
