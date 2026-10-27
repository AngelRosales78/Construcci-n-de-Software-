# Avance comprobado del Sprint 2 — 26 de octubre de 2026

Rama: `magallanes-sprint2-simulaciones`, continuación de `33d75e8` del 21 de octubre. Se conserva la base integrada de autenticación. No se modificó Android ni se reescribió el historial.

## Resultado por responsabilidad

- Las 27 tareas exclusivas de Diego tienen implementación servidor/web. Doce venían del día 3 y quince se completaron en esta continuación.
- Con autorización del usuario se implementaron también T45, T49, T53, T57 y T61 (servicio IA) y T67 (proveedor local), originalmente asignadas a Angel.
- Dieciséis tareas Android siguen sin verificación en esta rama. Las dieciséis tareas de pruebas compartidas tienen evidencia servidor/web; falta compararlas con Android.
- T65, T68 y T69 siguen parciales por revisión conjunta, recorrido móvil y seguimiento real. T66 requiere votos y acuerdos reales.

Conteo: 33 implementadas en su ámbito, 19 parciales y 17 pendientes. No equivale a 69 tareas aceptadas ni a historias completas en ambas plataformas.

## Verificación

- Servidor: suite completa de 326 pruebas aprobadas con PostgreSQL y Redis aislados.
- Web: 67 pruebas aprobadas y compilación correcta. Tras reutilizar Highstock, se comprobaron nuevamente los siete casos de gráficos y la descarga JavaScript principal bajó de 1427,45 a 1127,13 kB (aproximadamente 21 %). La compilación aún recomienda dividir el paquete para mejorar la carga inicial.
- Chrome: inicio de sesión, MSFT FY2024, puntaje 85,25, 2000 trayectorias con 1254 cierres, orden P10/P50/P90, parámetros, restablecimiento, leyenda sin recalcular, dos niveles de IA real, caché tras recarga, ausencia de datos en AAPL y tamaños 1440/390 sin desbordamiento.
- Se revisaron las capturas y el texto generado. La IA valida estructura, referencias y cifras; no certifica toda interpretación financiera.
- No hay migraciones de modelos pendientes. Los pesos Qwen3 y los binarios permanecen fuera de Git, con SHA-256 verificado.

## Matriz de tareas

La evidencia enumera hasta dos commits por tarea; los restantes se consultan con `git log --all --grep="Txx"`. Un commit acredita trabajo, no aprobación conjunta. La definición original de cada tarea está en `Sprint_2_tareas_detalladas.md`.

| Tarea | HU | Responsable original | Estado | Evidencia / pendiente |
|---|---|---|---|---|
| T01 | HU05_02 | Diego | Implementada | ccf41f0, f634740 |
| T02 | HU05_02 | Diego | Implementada | 424e5d5 |
| T03 | HU05_02 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T04 | HU05_02 | Diego y Angel | Parcial | c075f02; pruebas servidor/web aprobadas; falta comparación Android. |
| T05 | HU05_03 | Diego | Implementada | 2d1d5e0 |
| T06 | HU05_03 | Diego | Implementada | 229acac |
| T07 | HU05_03 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T08 | HU05_03 | Diego y Angel | Parcial | 7e62512; pruebas servidor/web aprobadas; falta comparación Android. |
| T09 | HU05_04 | Diego | Implementada | 0943a6d |
| T10 | HU05_04 | Diego | Implementada | e4fc445, 4d6cf06 |
| T11 | HU05_04 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T12 | HU05_04 | Diego y Angel | Parcial | ac100e4, 1de1564; pruebas servidor/web aprobadas; falta comparación Android. |
| T13 | HU05_05 | Diego | Implementada | 64b75d2 |
| T14 | HU05_05 | Diego | Implementada | 2b0b95c |
| T15 | HU05_05 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T16 | HU05_05 | Diego y Angel | Parcial | 5f0979c; pruebas servidor/web aprobadas; falta comparación Android. |
| T17 | HU05_01 | Diego | Implementada | 1cf6076, b0c3cca |
| T18 | HU05_01 | Diego | Implementada | 5d55f59 |
| T19 | HU05_01 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T20 | HU05_01 | Diego y Angel | Parcial | 72c8433; pruebas servidor/web aprobadas; falta comparación Android. |
| T21 | HU04_05 | Diego | Implementada | a769999 |
| T22 | HU04_05 | Diego | Implementada | f3bfb99 |
| T23 | HU04_05 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T24 | HU04_05 | Diego y Angel | Parcial | 385bfd7; pruebas servidor/web aprobadas; falta comparación Android. |
| T25 | HU06_01 | Diego | Implementada | d13ae56 |
| T26 | HU06_01 | Diego | Implementada | e3238e9 |
| T27 | HU06_01 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T28 | HU06_01 | Diego y Angel | Parcial | 9578caa; pruebas servidor/web aprobadas; falta comparación Android. |
| T29 | HU06_02 | Diego | Implementada | a56d691 |
| T30 | HU06_02 | Diego | Implementada | 327c6ce |
| T31 | HU06_02 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T32 | HU06_02 | Diego y Angel | Parcial | 1ec2175; pruebas servidor/web aprobadas; falta comparación Android. |
| T33 | HU06_03 | Diego | Implementada | 9d36099 |
| T34 | HU06_03 | Diego | Implementada | f00fa03 |
| T35 | HU06_03 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T36 | HU06_03 | Diego y Angel | Parcial | 2b28c6d; pruebas servidor/web aprobadas; falta comparación Android. |
| T37 | HU06_04 | Diego | Implementada | 0e7bf23, 1934bc9 |
| T38 | HU06_04 | Diego | Implementada | d1b9ef9, 510d341 |
| T39 | HU06_04 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T40 | HU06_04 | Diego y Angel | Parcial | 4e42a10; pruebas servidor/web aprobadas; falta comparación Android. |
| T41 | HU06_05 | Diego | Implementada | 82c8211 |
| T42 | HU06_05 | Diego | Implementada | f6cdd0d |
| T43 | HU06_05 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T44 | HU06_05 | Diego y Angel | Parcial | 0a44b55, fb01a42; pruebas servidor/web aprobadas; falta comparación Android. |
| T45 | HU07_04 | Angel | Implementada | 1266c5b, ae7e5d5; implementada por Diego con autorización del usuario. |
| T46 | HU07_04 | Diego | Implementada | 248d9f0 |
| T47 | HU07_04 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T48 | HU07_04 | Diego y Angel | Parcial | 814d1bc, 2a622bb; pruebas servidor/web aprobadas; falta comparación Android. |
| T49 | HU07_01 | Angel | Implementada | d32f058, 07f47bf; implementada por Diego con autorización del usuario. |
| T50 | HU07_01 | Diego | Implementada | 2a0bae4 |
| T51 | HU07_01 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T52 | HU07_01 | Diego y Angel | Parcial | 7d06571, b3845f0; pruebas servidor/web aprobadas; falta comparación Android. |
| T53 | HU07_02 | Angel | Implementada | 3fd91c9; implementada por Diego con autorización del usuario. |
| T54 | HU07_02 | Diego | Implementada | cdf8a1a |
| T55 | HU07_02 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T56 | HU07_02 | Diego y Angel | Parcial | 3590d3b; pruebas servidor/web aprobadas; falta comparación Android. |
| T57 | HU07_03 | Angel | Implementada | ed1c872, f8248e8; implementada por Diego con autorización del usuario. |
| T58 | HU07_03 | Diego | Implementada | 07386e6 |
| T59 | HU07_03 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T60 | HU07_03 | Diego y Angel | Parcial | 3c7e2b5; pruebas servidor/web aprobadas; falta comparación Android. |
| T61 | HU07_05 | Angel | Implementada | 33ef639; implementada por Diego con autorización del usuario. |
| T62 | HU07_05 | Diego | Implementada | 7cb975b, 4c08979 |
| T63 | HU07_05 | Angel | Pendiente | Implementación y demostración Android por Angel. |
| T64 | HU07_05 | Diego y Angel | Parcial | 5d2a7bb, 80e1884; pruebas servidor/web aprobadas; falta comparación Android. |
| T65 | Compartida | Diego y Angel | Parcial | 352cd7d, 7357958; metodología y contrato documentados; falta revisión conjunta y conexión Android. |
| T66 | Compartida | Diego y Angel | Pendiente | Planning Poker y acuerdos del equipo; no se inventaron votos. |
| T67 | Compartida | Angel | Implementada | 4f42fcc, 33435c3; implementada por Diego con autorización del usuario. |
| T68 | Compartida | Diego y Angel | Parcial | 57ba49e, 3d0e9d8; recorrido completo web comprobado; falta Android. |
| T69 | Compartida | Diego y Angel | Parcial | Sin commit de implementación verificado; guía/capturas preparadas; faltan horas reales, burndown y cierre conjunto. |

## Límites de datos y de la demostración

El catálogo contiene 503 símbolos, pero el caso financiero completo preparado es Microsoft. Los 1254 cierres ajustados son un snapshot histórico hasta el 12 de octubre; no cotizaciones en vivo. Las simulaciones parten de ese último cierre y no son precios futuros reales. Las fórmulas y tramos son metodología académica versionada; faltan revisión conjunta y comparación sectorial suficiente.

La IA local funciona sin cuenta ni API de pago. Una nueva generación ocupa CPU y tarda; si falla el proveedor o la validación, se muestra no disponible. La aplicación no sustituye ese fallo con un análisis financiero ficticio.

Qwen3 selecciona entre explicaciones financieras revisadas compatibles con cada grupo de métricas y nivel. Las fórmulas y cifras las aporta Django. Este vocabulario controlado evita confundir indicadores, pero no equivale a una evaluación financiera experta de redacción libre.

## Presentar en conjunto

Diego demuestra servidor y web con `Sprint_2_simulaciones_y_IA.md`: fuentes, cálculos, MVVM, persistencia, escenarios y análisis. Angel debe demostrar Android contra el mismo contrato, empresa y periodo. La tabla distingue lo comprobado de lo que aún necesita esa demostración.


## Reconciliación con Google Docs y Rosales — 27 de octubre de 2026

Este apartado actualiza la lectura del equipo; la matriz anterior conserva el corte del 26/10. Fuente vigente: [Sprint 2 con historias y tareas](https://docs.google.com/document/d/1d6Jg1TQCZ5yVo6e-8OM7S3WnKTmko-pOx3xh5TXKPI4/edit). Contiene 16 historias y 69 tareas, distintas del reparto de 45 tareas del Sprint 1. No se cambian responsables originales, horas estimadas ni criterios para acomodarlos a lo implementado.

Referencias comparadas: Diego `046fff2` y `origin/Rosales` `18e4893`, actualizada mediante fetch. No hay una rama remota llamada Angel. Las evidencias publicadas por Angel registran 56 pruebas JVM aprobadas y 110 pruebas de servidor aprobadas (113 casos, 3 omitidos), con dos pruebas de interfaz compiladas pero no ejecutadas y sin dispositivo conectado. Es evidencia de su corte del 25/10, no una nueva ejecución contra el servidor actual. La revisión web/servidor de Diego tiene 77 y 333 pruebas aprobadas respectivamente.

### Bloqueos detectados antes de la integración b4a0ee3

Los dos bloqueos de contrato y el tiempo de espera de IA descritos abajo ya quedaron corregidos en la rama integrada. La tabla conserva el diagnóstico que motivó el cambio; las tareas de revisión del equipo siguen abiertas.

| Prioridad | Archivos | Diferencia real | Corrección necesaria |
|---|---|---|---|
| P0 integración | Android `data/api/AnalysisApiService.kt`, `data/api/models/AnalysisModels.kt`, `ui/analysis/AnalysisValidation.kt`; servidor `backend/api/analysis_views.py` | Android envía POST y espera `analysis-v1`, `validation.status`, resumen/listas en raíz y fuentes objeto. El servidor expone GET, `controlled-analysis-v1`, `validated`, contenido bajo `analysis` y fuentes como URL. | Elegir y documentar un contrato; adaptar el repositorio Android o un adaptador explícito del servidor. Conservar validación de cifras y referencias; probar ambos niveles y errores con HTTP real. |
| P0 integración | Android `data/api/SimulationApiService.kt`, `data/api/models/SimulationModels.kt`, `ui/scenarios/SimulationValidation.kt`; servidor `backend/api/simulation_views.py` y `api/urls.py` | Android requiere GET `simulations/defaults/`, ruta inexistente; envía `paths` y `horizon_months`, rechazados por el serializador actual. Espera `points`, parámetros completos y fechas raíz; el servidor devuelve `scenarios` y metadatos con otro formato. | Publicar límites/defaults coherentes con el motor, acordar campos admitidos y adaptar las series y parámetros. No eliminar validadores para ocultar la incompatibilidad. |
| P1 integración | Configuración Retrofit/arranque de ambos | Android documenta emulador 10.0.2.2:8000; la demo web usa servidor local 58010. La integración de pilares publicada usa una copia anterior de Django. | Apuntar ambos al mismo servidor accesible y mismo snapshot; no confundir 127.0.0.1 del emulador con la PC. Probar JWT y caché con tiempos de IA real. |
| P1 aceptación | T65/T68 y pruebas compartidas | Hay pruebas de cada parte, no un recorrido conjunto certificado de todos los módulos. | Ejecutar MSFT FY2024 en web y dispositivo/emulador, comparar ratios, 85,25, percentiles, fuentes y niveles; comprobar ausencias y fallo de servicio. |
| P2 seguimiento | T66/T69 | No hay votos/horas reales suficientes para cerrar todo el seguimiento. | Registrar acuerdos y trabajo verificable desde ahora; no inventar burndown ni fechas. |

No se declara terminado el Sprint 2 completo. La ausencia de comparación sectorial es un caso admitido por el plan cuando falta muestra; no equivale a implementar una referencia sectorial real. La cobertura de todas las empresas y SMTP externo son límites de producto/base, no tareas nuevas del Sprint 2 inventadas para inflar el alcance.

### Matriz reconciliada de las 69 tareas

Estado después de integrar y comprobar ambos clientes: **66 tareas terminadas (100 %) en su alcance técnico; T65 y T69 parciales; T66 pendiente.** T01–T64 y T67–T68 tienen código y evidencia. Esto no equivale a aceptación académica del Sprint: faltan revisión conjunta de fórmulas/pesos, Planning Poker y seguimiento real. El Android original pertenece a Angel; la adaptación e integración están en `b4a0ee3` de Diego. Las comprobaciones móviles actuales se ejecutaron en emulador, no solo como pruebas JVM.

| Tarea | HU | Responsable original | Estado actualizado | Evidencia o pendiente |
|---|---|---|---|---|
| T01 | HU05_02 | Diego | Terminada (100 %) | ccf41f0, f634740 |
| T02 | HU05_02 | Diego | Terminada (100 %) | 424e5d5 |
| T03 | HU05_02 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T04 | HU05_02 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T05 | HU05_03 | Diego | Terminada (100 %) | 2d1d5e0 |
| T06 | HU05_03 | Diego | Terminada (100 %) | 229acac |
| T07 | HU05_03 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T08 | HU05_03 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T09 | HU05_04 | Diego | Terminada (100 %) | 0943a6d |
| T10 | HU05_04 | Diego | Terminada (100 %) | e4fc445, 4d6cf06 |
| T11 | HU05_04 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T12 | HU05_04 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T13 | HU05_05 | Diego | Terminada (100 %) | 64b75d2 |
| T14 | HU05_05 | Diego | Terminada (100 %) | 2b0b95c |
| T15 | HU05_05 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T16 | HU05_05 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T17 | HU05_01 | Diego | Terminada (100 %) | 1cf6076, b0c3cca |
| T18 | HU05_01 | Diego | Terminada (100 %) | 5d55f59 |
| T19 | HU05_01 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T20 | HU05_01 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T21 | HU04_05 | Diego | Terminada (100 %) | a769999 |
| T22 | HU04_05 | Diego | Terminada (100 %) | f3bfb99 |
| T23 | HU04_05 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T24 | HU04_05 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T25 | HU06_01 | Diego | Terminada (100 %) | d13ae56 |
| T26 | HU06_01 | Diego | Terminada (100 %) | e3238e9 |
| T27 | HU06_01 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T28 | HU06_01 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T29 | HU06_02 | Diego | Terminada (100 %) | a56d691 |
| T30 | HU06_02 | Diego | Terminada (100 %) | 327c6ce |
| T31 | HU06_02 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T32 | HU06_02 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T33 | HU06_03 | Diego | Terminada (100 %) | 9d36099 |
| T34 | HU06_03 | Diego | Terminada (100 %) | f00fa03 |
| T35 | HU06_03 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T36 | HU06_03 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T37 | HU06_04 | Diego | Terminada (100 %) | 0e7bf23, 1934bc9 |
| T38 | HU06_04 | Diego | Terminada (100 %) | d1b9ef9, 510d341 |
| T39 | HU06_04 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T40 | HU06_04 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T41 | HU06_05 | Diego | Terminada (100 %) | 82c8211 |
| T42 | HU06_05 | Diego | Terminada (100 %) | f6cdd0d |
| T43 | HU06_05 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T44 | HU06_05 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T45 | HU07_04 | Angel | Terminada (100 %) | 1266c5b, ae7e5d5; implementada por Diego con autorización del usuario. |
| T46 | HU07_04 | Diego | Terminada (100 %) | 248d9f0 |
| T47 | HU07_04 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T48 | HU07_04 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T49 | HU07_01 | Angel | Terminada (100 %) | d32f058, 07f47bf; implementada por Diego con autorización del usuario. |
| T50 | HU07_01 | Diego | Terminada (100 %) | 2a0bae4 |
| T51 | HU07_01 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T52 | HU07_01 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T53 | HU07_02 | Angel | Terminada (100 %) | 3fd91c9; implementada por Diego con autorización del usuario. |
| T54 | HU07_02 | Diego | Terminada (100 %) | cdf8a1a |
| T55 | HU07_02 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T56 | HU07_02 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T57 | HU07_03 | Angel | Terminada (100 %) | ed1c872, f8248e8; implementada por Diego con autorización del usuario. |
| T58 | HU07_03 | Diego | Terminada (100 %) | 07386e6 |
| T59 | HU07_03 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T60 | HU07_03 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T61 | HU07_05 | Angel | Terminada (100 %) | 33ef639; implementada por Diego con autorización del usuario. |
| T62 | HU07_05 | Diego | Terminada (100 %) | 7cb975b, 4c08979 |
| T63 | HU07_05 | Angel | Terminada (100 %) | Android original de Rosales 18e4893; integrado en b4a0ee3. 68 pruebas JVM y recorrido real en Pixel_7; docs/evidencias/android_dispositivo_real.json. |
| T64 | HU07_05 | Diego y Angel | Terminada (100 %) | Pruebas de casos y contratos: 347 servidor, 77 web y 68 JVM; recorrido web y emulador con la misma API. b4a0ee3; evidencias de integración. |
| T65 | Compartida | Diego y Angel | Parcial | Conexión y contratos ya comprobados. Falta revisión y acuerdo real de ambos sobre fórmulas, fuentes y ponderaciones. |
| T66 | Compartida | Diego y Angel | Pendiente | Requiere votos y acuerdos reales de ambos; no se reconstruyen estimaciones como si hubieran ocurrido. |
| T67 | Compartida | Angel | Terminada (100 %) | 4f42fcc, 33435c3; implementada por Diego con autorización del usuario. |
| T68 | Compartida | Diego y Angel | Terminada (100 %) | b4a0ee3; Chrome y Pixel_7 contra la misma API: pilares/85,25, escenarios/restauración y ambos niveles de IA. Tres pruebas de interfaz aprobadas. |
| T69 | Compartida | Diego y Angel | Parcial | Capturas, guía, matriz y reportes actualizados. Faltan registros reales de horas, seguimiento diario, burndown y cierre del equipo. |

### Por qué hay tantos commits y qué ramas usar

Conteo después del fetch, antes de nuevos commits de esta revisión:

- Rama de Diego `magallanes-sprint2-simulaciones`: 182 commits alcanzables, de los cuales 85 ya existían al crear el plan `5b2c624` y 97 son posteriores. De esos 97, 90 comienzan con T/HU; eso no acredita 90 tareas diferentes.
- `origin/Rosales`: 30; `origin/Magallanes`: 26. Todos los historiales conocidos contienen 211 commits únicos. Estos números no se suman porque comparten ancestros.
- Siete ramas remotas reales; `origin/...` en la PC es la referencia de seguimiento de una rama remota, no otro trabajo duplicado.
- Las tres ramas `diego/sprint2-*` son antecesoras de la rama actual. Se usaron como hitos por días, no contienen tres implementaciones independientes.
- Hubo fragmentación excesiva: tareas, pruebas, correcciones de metodología, integración, IA e interfaz generaron varios commits para una misma tarea. Un commit representa una instantánea, no una tarea adicional. No se verificó una cifra de 400 commits únicos.

Uso final recomendado: mantener como ramas activas `magallanes-sprint2-simulaciones` (servidor/web) y `Rosales` (Android), y `main` como destino de una integración revisada. `Magallanes`, las tres ramas por días y los respaldos son históricos. No se crean más ramas por fecha. Renombrar, borrar ramas o reescribir los commits publicados no se ejecutó en esta revisión.

Conservar lo publicado y usar esta matriz permite defender la trazabilidad sin alterar la historia del equipo. Si se decide compactar, primero se debe acordar una base y una lista concreta de grupos por HU, preservar referencias de respaldo y rehacer toda la matriz con los nuevos hashes. Un squash cambia identificadores y requiere actualizar las copias de ambos; no reduce las 69 tareas ni permite inventar fechas anteriores. Para nuevas correcciones: un cambio comprobable por problema/HU, con pruebas relacionadas incluidas, sin un commit separado solo para anunciar cada comprobación.

## Resultado de la integración comprobada — 27/10/2026

- Commit de código: `b4a0ee3`. Android se incorporó desde `origin/Rosales@18e4893`, conservando autoría original en la descripción del commit; no se hizo push a Rosales ni se reescribió su historial.
- API: GET de IA conserva la web; POST ofrece la representación `analysis-v1` de Android sobre la misma generación, referencias, validación y caché. El formato de transporte tiene versión propia; no hay un segundo proveedor ni cifras recalculadas en el móvil.
- Simulaciones: GET de defaults calibra sin ejecutar Monte Carlo ni persistir; POST acepta únicamente las 2000 trayectorias y doce meses del motor. `points` y `scenarios` representan los mismos percentiles, con parámetros efectivos, fechas y procedencia. La recuperación de ejecuciones sigue limitada a su propietario.
- Android: se conservó la suite anterior de autenticación. La prueba de interfaz del flujo antiguo «Explorar · ejemplo» fue sustituida por el recorrido de pantallas reales y los controles de radar. El cliente mantiene 30 segundos para datos y permite 160 segundos para IA local.
- Verificación final: **347 pruebas del servidor, 77 de la web, 68 JVM Android y 3 de interfaz en Pixel_7 aprobadas**; sin casos omitidos en las suites servidor/JVM. APK de depuración compilado para `http://10.0.2.2:58010/api/v1/`.
- Chrome y el emulador comprobaron MSFT FY2024, cinco pilares y puntaje 85,25, tres percentiles, leyenda sin recalcular, escenario constante con tasa/volatilidad cero, restauración y dos niveles de IA. JVM también comprobó datos ausentes y el mismo contexto/periodo en ambas representaciones.
- Evidencias: `docs/evidencias/android_integracion_real.json`, `android_dispositivo_real.json`; XML completo del servidor y capturas en la carpeta local `output/guia_sprint2`; recorrido Chrome en `output/sprint2_integracion_27_oct/resultado.json`. Las nueve capturas finales del emulador se inspeccionaron sin el aviso transitorio del sistema que afectó al primer arranque.

### Qué falta para cerrar las tres tareas restantes

| Tarea | Lo que ya está preparado | Lo que deben aportar Diego y Angel |
|---|---|---|
| T65 | Fuentes, metodología, pesos, contratos y conexión comprobados. | Revisar juntos las fórmulas y pesos y registrar el acuerdo real. La implementación conserva el estado de metodología pendiente de revisión; no se inventa una aprobación. |
| T66 | Las 16 historias, 69 tareas y estimaciones iniciales están definidas. | Hacer Planning Poker y anotar los votos, desacuerdos y estimación final. Una estimación actual no se atribuye retroactivamente al inicio del Sprint. |
| T69 | Guion, matriz, capturas y pruebas actuales. | Aportar registros verificables de trabajo restante/horas por fecha, construir el burndown con ellos y registrar el cierre. Si no existen registros previos, declarar la limitación y empezar el seguimiento desde ahora. |

### Cómo ejecutar la demostración Android integrada

1. Iniciar los servicios con `tools/Start-LocalAI.ps1` y `tools/Start-Sprint2-Demo.ps1` solo si están detenidos. La API de esta demo es 58010 y la web 55183.
2. Abrir `android` en Android Studio con JDK 17 y seleccionar el emulador. Compilar con `-PapiBaseUrl=http://10.0.2.2:58010/api/v1/`. El valor 10.0.2.2 apunta a la PC desde el emulador; no sirve directamente para un teléfono físico.
3. Entrar con una cuenta de la base aislada, buscar MSFT, abrir la ficha y Cinco pilares y puntaje. La web y Android deben indicar FY2024 y 85,25/100.
4. Demostrar Escenarios a doce meses y Explicación de IA; usar Sencillo/Técnico y fuentes. No es necesario detener el proveedor para la exposición.
5. Para repetir las pruebas JVM/HTTP usar `tools/verify_sprint2_clients.py --java-home <JDK17> --gradle-home <Gradle8.4>` con el SDK en ANDROID_HOME. El ejecutor solo crea cuentas de prueba en la API aislada 58010; no imprime contraseñas ni tokens.
