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
