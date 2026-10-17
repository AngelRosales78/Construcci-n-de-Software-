# Sprint 2 propuesto con seguimiento por historia y tarea

Versión revisada del 16 de octubre de 2026. Sustituye la propuesta anterior de tres historias. Se adapta la organización del informe AutoSmart facilitado por el usuario, conservando los identificadores y criterios del proyecto S&P 500. No se reproducen sus cifras, responsables, commits, PR ni conclusiones de cumplimiento.

## 1 Resumen del alcance y la capacidad

Meta propuesta: completar en la ficha web el análisis de los cinco pilares y el puntaje general de una empresa piloto, mostrando periodos, fuente, metodología y datos ausentes. El sprint 1 conserva su calificación y sus evidencias.

| Indicador | Planificación actual |
|---|---|
| Historias propuestas | 6 nuevas: HU05_01 a HU05_05 y HU04_05 |
| Capacidad declarada | 2 personas × 3 h diarias × 10 días = 60 horas persona |
| Tareas de historias | 36 h |
| Tareas compartidas | 12 h |
| Trabajo previsto total | 48 h |
| Reserva | 12 h para errores ajustes y dependencias |
| Horas realmente trabajadas en estas historias | Sin registrar |
| Historias nuevas aceptadas como terminadas | Ninguna registrada |
| Puntos de historia | Por estimar con Planning Poker real |
| Integración del código del sprint 2 | No iniciada en esta planificación |

**Estimación preliminar optimista.** Se amplía el alcance aprovechando un único conjunto de datos, contrato de API y componentes reutilizables. La propuesta anterior estimaba por separado más trabajo de preparación y presentación para tres detalles; esta revisión concentra la base común y reduce esas estimaciones. Esa reducción no es velocidad medida ni consenso del equipo. Las 48 h solo son una hipótesis que debe contrastarse con Planning Poker y la disponibilidad real de datos el día 1. No se rebajan los criterios para hacer entrar seis historias.

Piloto: MSFT, con cuatro cierres anuales comparables para poder calcular CAGR a tres años y promedios. La revisión anterior acreditó un balance importado, no estos cuatro cierres. Obtener y comprobar los componentes es una dependencia pendiente, no un trabajo terminado. Para los otros tickers se muestra No disponible cuando falten datos. No se promete cobertura financiera de las 503 empresas.

## 2 Responsables estado y evidencia

A y B son roles por asignar a los dos integrantes: A se centra en datos y servidor; B en interfaz y evidencia; ambos prueban e integran. La distribución propuesta es 24 h de trabajo y 6 h de reserva por persona. No se atribuye trabajo previo a nadie ni se inventan fechas.

Estados: **Pendiente → En progreso → Implementada en rama → Integrada → Verificada**. Una historia solo se marca Terminada cuando todos sus criterios pasan y su código está integrado en la rama acordada. Una implementación parcial, un PR pendiente o un ajuste obligatorio pendiente no equivalen a Terminada. El estado de GitHub se registrará al publicar; los commits locales no se presentan como PR integrados en main.

## 3 Historias y tareas propuestas

Todas las filas empiezan Pendientes. La columna h corresponde a estimación, no a tiempo registrado. Los commits y fechas se rellenan cuando exista el trabajo. No es obligatorio producir un commit por cada fila: cada commit debe representar una mejora concreta y permitir rastrear la tarea.

### HU05_02 Liquidez

Como usuario, quiero analizar el Pilar de Liquidez con ratios de corto plazo.

Prioridad de desarrollo: 1. Total propuesto: 6 h. SP: por votar. Dependencia: Datos y metodología de liquidez.

| Tarea | Trabajo verificable | Responsable | Capa | h | Commit | Fecha | Estado de integración |
|---|---|---|---|---|---|---|---|
| HU05_02-T01 | Calcular liquidez corriente y prueba ácida con componentes y casos no disponibles | A | Servidor | 2 | — | — | Pendiente |
| HU05_02-T02 | Mostrar ratios componentes periodo fuente y umbrales explicados | B | Web | 2 | — | — | Pendiente |
| HU05_02-T03 | Verificar fórmula conocida denominador cero y componente ausente | A 1 h y B 1 h | Pruebas | 2 | — | — | Pendiente |

Criterios de aceptación conservados:

- CA1: Muestra liquidez corriente y prueba ácida con componentes y periodo.
- CA2: Denominador cero o datos insuficientes muestran No disponible.
- CA3: Los colores usan umbrales configurados y explicados; no un óptimo universal.

Prueba y evidencia por criterio: **Pendiente de ejecución**. Registrar dato de entrada, resultado esperado, resultado obtenido, archivo o prueba y commit. No se confunde la presencia de una clase con la ejecución del criterio.

### HU05_03 Rentabilidad

Como usuario, quiero evaluar el Pilar de Rentabilidad examinando márgenes y retorno.

Prioridad de desarrollo: 2. Total propuesto: 6 h. SP: por votar. Dependencia: Datos de rentabilidad; muestra comparable para referencia sectorial.

| Tarea | Trabajo verificable | Responsable | Capa | h | Commit | Fecha | Estado de integración |
|---|---|---|---|---|---|---|---|
| HU05_03-T01 | Calcular ROE ROA y margen neto con la política de promedios acordada | A | Servidor | 2 | — | — | Pendiente |
| HU05_03-T02 | Mostrar ratios y explicar ausencia de muestra sectorial comparable | B | Web | 2 | — | — | Pendiente |
| HU05_03-T03 | Verificar redondeo patrimonio negativo y denominadores cero | A 1 h y B 1 h | Pruebas | 2 | — | — | Pendiente |

Criterios de aceptación conservados:

- CA1: Muestra ROE, ROA y margen neto con fórmula y periodo.
- CA2: La referencia sectorial declara muestra y fecha y se omite si es insuficiente.
- CA3: Explica denominadores cero y patrimonio negativo.

Prueba y evidencia por criterio: **Pendiente de ejecución**. Registrar dato de entrada, resultado esperado, resultado obtenido, archivo o prueba y commit. No se confunde la presencia de una clase con la ejecución del criterio.

### HU05_04 Solvencia

Como usuario, quiero revisar el Pilar de Solvencia para entender la estructura de deuda.

Prioridad de desarrollo: 3. Total propuesto: 6 h. SP: por votar. Dependencia: Datos de deuda y gastos financieros.

| Tarea | Trabajo verificable | Responsable | Capa | h | Commit | Fecha | Estado de integración |
|---|---|---|---|---|---|---|---|
| HU05_04-T01 | Calcular deuda sobre patrimonio y cobertura de intereses | A | Servidor | 2 | — | — | Pendiente |
| HU05_04-T02 | Mostrar componentes fecha y diferencia respecto de Altman Z | B | Web | 2 | — | — | Pendiente |
| HU05_04-T03 | Verificar gastos cero patrimonio negativo y datos ausentes | A 1 h y B 1 h | Pruebas | 2 | — | — | Pendiente |

Criterios de aceptación conservados:

- CA1: Muestra Deuda/Patrimonio y Cobertura de Intereses con componentes y fecha.
- CA2: Documenta umbrales y casos no aplicables.
- CA3: Distingue este detalle del gauge Altman-Z.

Prueba y evidencia por criterio: **Pendiente de ejecución**. Registrar dato de entrada, resultado esperado, resultado obtenido, archivo o prueba y commit. No se confunde la presencia de una clase con la ejecución del criterio.

### HU05_05 Eficiencia

Como usuario, quiero inspeccionar el Pilar de Eficiencia Operativa respecto a la rotación de activos.

Prioridad de desarrollo: 4. Total propuesto: 7 h. SP: por votar. Dependencia: Histórico de activos, ingresos e inventario aplicable.

| Tarea | Trabajo verificable | Responsable | Capa | h | Commit | Fecha | Estado de integración |
|---|---|---|---|---|---|---|---|
| HU05_05-T01 | Calcular rotación de activos y días de inventario con periodos comparables | A | Servidor | 3 | — | — | Pendiente |
| HU05_05-T02 | Mostrar hasta tres años disponibles y explicar inventario no aplicable | B | Web | 2 | — | — | Pendiente |
| HU05_05-T03 | Verificar series incompletas componentes promediados y divisiones por cero | A 1 h y B 1 h | Pruebas | 2 | — | — | Pendiente |

Criterios de aceptación conservados:

- CA1: Muestra rotación de activos y días de inventario con definición y periodo.
- CA2: Presenta hasta tres años disponibles sin inventar los años faltantes.
- CA3: Si el inventario no aplica o falta el denominador, informa No disponible y explica el motivo.

Prueba y evidencia por criterio: **Pendiente de ejecución**. Registrar dato de entrada, resultado esperado, resultado obtenido, archivo o prueba y commit. No se confunde la presencia de una clase con la ejecución del criterio.

### HU05_01 Cinco pilares y crecimiento

Como usuario, quiero explorar el desglose de los 5 pilares financieros de una empresa.

Prioridad de desarrollo: 5. Total propuesto: 7 h. SP: por votar. Dependencia: Datos financieros suficientes; HU05_02 a HU05_05 para completar el radar.

| Tarea | Trabajo verificable | Responsable | Capa | h | Commit | Fecha | Estado de integración |
|---|---|---|---|---|---|---|---|
| HU05_01-T01 | Calcular crecimiento anual y CAGR a tres años con cobertura explícita | A | Servidor | 2 | — | — | Pendiente |
| HU05_01-T02 | Mostrar radar de cinco ejes y acceso al detalle de cada pilar | B | Web | 3 | — | — | Pendiente |
| HU05_01-T03 | Verificar notas versionadas cinco ejes y años insuficientes | A 1 h y B 1 h | Pruebas | 2 | — | — | Pendiente |

Criterios de aceptación conservados:

- CA1: El radar muestra Liquidez, Rentabilidad, Solvencia, Eficiencia y Crecimiento, con notas versionadas o No disponible.
- CA2: Esta HU incluye Crecimiento: variación anual de ingresos y tasa compuesta a tres años si hay datos.
- CA3: Cada eje abre su detalle; las comparaciones declaran cobertura y periodo.

Prueba y evidencia por criterio: **Pendiente de ejecución**. Registrar dato de entrada, resultado esperado, resultado obtenido, archivo o prueba y commit. No se confunde la presencia de una clase con la ejecución del criterio.

### HU04_05 Puntaje general

Como usuario, quiero visualizar un puntaje numérico unificado sobre la salud general (0 a 100).

Prioridad de desarrollo: 6. Total propuesto: 4 h. SP: por votar. Dependencia: HU05_01 a HU05_05.

| Tarea | Trabajo verificable | Responsable | Capa | h | Commit | Fecha | Estado de integración |
|---|---|---|---|---|---|---|---|
| HU04_05-T01 | Agregar las cinco notas según ponderaciones versionadas acordadas | A | Servidor | 1 | — | — | Pendiente |
| HU04_05-T02 | Mostrar 0 a 100 con redondeo consistente o No disponible | B | Web | 1 | — | — | Pendiente |
| HU04_05-T03 | Verificar cálculo conocido límites y ausencia de un pilar | A 1 h y B 1 h | Pruebas | 2 | — | — | Pendiente |

Criterios de aceptación conservados:

- CA1: Agrega las cinco notas de pilares mediante una fórmula versionada y documentada.
- CA2: El resultado está entre 0 y 100 y el redondeo es igual en API e interfaz.
- CA3: Sin cobertura de los cinco pilares muestra No disponible y explica qué falta; no sustituye Altman-Z.

Prueba y evidencia por criterio: **Pendiente de ejecución**. Registrar dato de entrada, resultado esperado, resultado obtenido, archivo o prueba y commit. No se confunde la presencia de una clase con la ejecución del criterio.

## 4 Tareas técnicas compartidas

| ID | Tarea | Responsable | h | Estado |
|---|---|---|---|---|
| S2-TEC01 | Acordar metodología de ratios notas ponderaciones y Planning Poker | A 0.5 h y B 0.5 h | 1 | Pendiente |
| S2-TEC02 | Verificar e importar componentes reales de MSFT con cuatro cierres comparables | A 2 h y B 2 h | 4 | Pendiente |
| S2-TEC03 | Preparar persistencia por periodo contrato común y tarjetas reutilizables | A 1 h y B 1 h | 2 | Pendiente |
| S2-TEC04 | Integrar y ejecutar regresión completa incluida ausencia de datos | A 1.5 h y B 1.5 h | 3 | Pendiente |
| S2-TEC05 | Preparar demostración evidencias cierre y actualización del burndown | A 1 h y B 1 h | 2 | Pendiente |

La base de datos debe distinguir periodos, componentes, moneda y fuente sin sobrescribir la ficha existente. La importación debe ser repetible y rechazar datos inconsistentes. Notas 0 a 100, ponderaciones, umbrales, redondeo y casos no aplicables deben acordarse y versionarse antes de programar el score. Altman-Z no se renombra como score general.

## 5 Secuencia y burndown

| Días | Resultado objetivo | Dependencia |
|---|---|---|
| 1–2 | Refinamiento datos y base común; iniciar liquidez | Cuatro cierres y metodología disponibles |
| 3–4 | Liquidez rentabilidad y solvencia integradas por entregas | Contrato común y componentes reales |
| 5–6 | Eficiencia y crecimiento con periodos comparables | Histórico suficiente |
| 7 | Radar completo y puntaje general | Cinco notas válidas y versionadas |
| 8–9 | Regresión demostración y evidencias por criterio | Código integrado |
| 10 | Reserva y entrega | Resolver incidencias antes de cerrar |

La secuencia es una hipótesis de trabajo paralelo, no un avance real. El burndown inicial parte de 48 h pendientes. Cada día se registra una nueva estimación de horas restantes y los bloqueos; los valores reales comienzan sin completar. Las horas de la reserva no se convierten en historias terminadas automáticamente.

## 6 Condiciones para mantener el alcance

- Durante el primer día se verifica fuente, cuatro periodos y componentes. Si no están disponibles, se revisa la estimación antes de comprometer el sprint.
- Si el equipo estima más de 48 h, debe quitar o renegociar historias completas. El score depende de los cinco pilares: no se cierra con tres ni rellenando notas ausentes.
- Si una dependencia bloquea una historia, se registra inmediatamente y se replanifica el reparto de las 75; no se mueve al sprint 4 sin revisar su capacidad.
- El alcance aquí descrito es web. La conexión financiera Android se registra como pendiente de alcance, no se da por realizada por sus maquetas.
- Al final se conserva evidencia por criterio, pruebas aprobadas y una demostración reproducible. La integración por sí sola no sustituye los criterios de aceptación.

## 7 Relación con las 75 historias

Completar este módulo deja seis historias nuevas cerradas si todos los criterios se cumplen. No basta para asegurar las 75 dentro de cuatro sprints. La hoja de ruta adjunta hace visible el resto, sus dependencias y la concentración de carga que hay que revisar ahora.

## 8 Conclusiones

1. Se propone terminar un módulo coherente: cinco pilares y score, en lugar de dejar seis funcionalidades parcialmente iniciadas.
2. La estructura del ejemplo se usa para relacionar tareas, responsables y evidencia; sus cifras no son una referencia de nuestra productividad.
3. Los puntos, el compromiso y el burndown real requieren datos del equipo. La restricción de cuatro sprints exige revisar la capacidad futura y el alcance restante con el profesor.
