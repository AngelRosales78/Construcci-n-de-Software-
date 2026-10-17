# Distribución candidata de las 75 historias en cuatro sprints

Esta es una hoja de ruta para detectar acumulación y dependencias. Solo se conoce la capacidad del sprint 2: 60 horas persona. No se han estimado por completo los sprints 3 y 4; asignar una historia a un sprint candidato no demuestra que quepa en él. Se mantienen los 75 identificadores del backlog.

| Sprint | Historias | Cantidad | Condición |
|---|---|---|---|
| 1 histórico | HU01_01 a HU03_05 y HU04_01 a HU04_04 | 19 | Conservar calificación; el cierre funcional posterior no altera la evaluación |
| 2 propuesto | HU05_01 a HU05_05 y HU04_05 | 6 | Hipótesis de 48 h más 12 h de reserva; falta estimación real |
| 3 candidato | Épicas 6 7 8 9 y 11 | 25 | Resolver planes antes del comparador Pro; IA antes de conclusiones y PDF |
| 4 candidato | Épicas 10 12 13 14 y 15 | 25 | Favoritos antes de alertas; administración antes de métricas administrativas |

**Alerta de carga:** esta distribución deja 50 historias después del sprint 2. No resuelve por sí sola la restricción de terminar 75 en cuatro sprints. Es una señal para revisar el alcance y la capacidad de los dos últimos, no una promesa de que 25 historias grandes caben en cada uno.

Como referencia de cantidad, repartir las 56 historias posteriores al plan original del sprint 1 de forma parecida daría 19, 19 y 18. La cantidad no sustituye el esfuerzo: escenarios financieros, IA, comparador y PDF tienen dependencias y no son equivalentes a un formulario pequeño. No se asignan estimaciones artificiales de una hora para hacerlas caber.

## Qué hacer para no acumular

1. Contrastar con el equipo los rangos preliminares de las 50 historias incluidos abajo; desglosarlas antes de comprometerlas.
2. Confirmar cuánto tiempo habrá para los sprints 3 y 4. No se asume que tengan la misma capacidad que el segundo.
3. Si esas estimaciones no caben, acordar explícitamente más capacidad, otra distribución o una reducción de alcance aceptada por el profesor. El número de historias no se reduce dividiendo las mismas funciones en tareas ni cambiando sus identificadores.
4. Solo adelantar una historia adicional al sprint 2 cuando sus dependencias y estimación estén resueltas y exista capacidad demostrada. La reserva protege la entrega del módulo; no representa diez historias adicionales prometidas.

## Registro por historia

| HU | Sprint candidato | Dependencia del backlog |
|---|---|---|
| HU01_01 | 1 | Contrato compartido de autenticación y permisos |
| HU01_02 | 1 | Contrato compartido de autenticación y permisos |
| HU01_03 | 1 | Contrato compartido de autenticación y permisos |
| HU01_04 | 1 | Contrato compartido de autenticación y permisos |
| HU01_05 | 1 | Contrato compartido de autenticación y permisos |
| HU02_01 | 1 | Contrato compartido de autenticación y permisos |
| HU02_02 | 1 | Contrato compartido de autenticación y permisos |
| HU02_03 | 1 | Contrato compartido de autenticación y permisos |
| HU02_04 | 1 | Contrato compartido de autenticación y permisos |
| HU02_05 | 1 | Contrato compartido de autenticación y permisos |
| HU03_01 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_02 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_03 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_04 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_05 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_01 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_02 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_03 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_04 | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_05 | 2 | HU05_01 a HU05_05 |
| HU05_01 | 2 | Datos financieros suficientes; HU05_02 a HU05_05 para completar el radar |
| HU05_02 | 2 | Datos y metodología de liquidez |
| HU05_03 | 2 | Datos de rentabilidad; muestra comparable para referencia sectorial |
| HU05_04 | 2 | Datos de deuda y gastos financieros |
| HU05_05 | 2 | Histórico de activos, ingresos e inventario aplicable |
| HU06_01 | 3 | HU04_04 y serie histórica suficiente |
| HU06_02 | 3 | HU06_01 |
| HU06_03 | 3 | HU06_01 |
| HU06_04 | 3 | HU06_01 |
| HU06_05 | 3 | HU06_01 a HU06_03 |
| HU07_01 | 3 | Datos financieros y HU07_04 |
| HU07_02 | 3 | HU05_01 a HU05_05 y HU07_04 |
| HU07_03 | 3 | Datos suficientes y HU07_04 |
| HU07_04 | 3 | Procedencia y métricas verificables |
| HU07_05 | 3 | HU07_01 o HU07_02 |
| HU08_01 | 3 | HU03_01 y dos empresas con cobertura |
| HU08_02 | 3 | HU08_01 y HU05_01 |
| HU08_03 | 3 | HU08_01 y HU04_04 |
| HU08_04 | 3 | HU08_02 y HU07_04 |
| HU08_05 | 3 | HU08_01 y HU11_01 |
| HU09_01 | 3 | HU04_02 y HU05_01 |
| HU09_02 | 3 | HU09_01 y gráficos disponibles |
| HU09_03 | 3 | HU09_01 y HU07_01 |
| HU09_04 | 3 | HU08_02, HU09_01 y control de plan |
| HU09_05 | 3 | HU09_01 |
| HU10_01 | 4 | Sesión autenticada y catálogo; control de plan para aplicar límites |
| HU10_02 | 4 | HU10_01 |
| HU10_03 | 4 | HU10_01 y sesión web y Android |
| HU10_04 | 4 | HU04_02 y sesión autenticada |
| HU10_05 | 4 | HU10_01 y control de plan |
| HU11_01 | 3 | Autenticación y matriz Free y Pro |
| HU11_02 | 3 | Definición compartida de beneficios |
| HU11_03 | 3 | HU11_01 |
| HU11_04 | 3 | HU11_01 |
| HU11_05 | 3 | HU11_04 |
| HU12_01 | 4 | HU10_01 y clasificación financiera actualizable |
| HU12_02 | 4 | HU12_01 y sesión Android |
| HU12_03 | 4 | HU12_01 |
| HU12_04 | 4 | HU12_01 y canales definidos |
| HU12_05 | 4 | HU12_03 |
| HU13_01 | 4 | Contrato compartido de autenticación y permisos |
| HU13_02 | 4 | Contrato compartido de autenticación y permisos |
| HU13_03 | 4 | Contrato compartido de autenticación y permisos |
| HU13_04 | 4 | Contrato compartido de autenticación y permisos |
| HU13_05 | 4 | Contrato compartido de autenticación y permisos |
| HU14_01 | 4 | Catálogo y permisos administrativos |
| HU14_02 | 4 | Catálogo y procedencia de métricas |
| HU14_03 | 4 | Fuente financiera, cuota y normalización |
| HU14_04 | 4 | HU14_01 |
| HU14_05 | 4 | HU14_03 |
| HU15_01 | 4 | Instrumentación de llamadas de HU07 |
| HU15_02 | 4 | Usuarios y planes |
| HU15_03 | 4 | Llamadas a la fuente financiera |
| HU15_04 | 4 | HU11_01 y HU11_04 |
| HU15_05 | 4 | Registro de errores y permisos administrativos |

## Estimación preliminar del trabajo restante

Estos rangos en horas persona son una primera estimación de ingeniería según los criterios y dependencias del backlog. No son votos de Planning Poker, productividad histórica ni tiempo trabajado. Consideran implementar, conectar y probar cada historia; requieren revisión con tareas y responsables. No incluyen reserva adicional. Proveedor de IA, datos, cuotas, despliegue y cambios de alcance pueden aumentar el esfuerzo.

| HU | Sprint candidato | Horas mínimas propuestas | Horas máximas propuestas |
|---|---|---|---|
| HU06_01 | 3 | 8 | 12 |
| HU06_02 | 3 | 4 | 6 |
| HU06_03 | 3 | 4 | 6 |
| HU06_04 | 3 | 4 | 8 |
| HU06_05 | 3 | 4 | 6 |
| HU07_01 | 3 | 10 | 16 |
| HU07_02 | 3 | 4 | 6 |
| HU07_03 | 3 | 4 | 6 |
| HU07_04 | 3 | 6 | 10 |
| HU07_05 | 3 | 3 | 5 |
| HU08_01 | 3 | 4 | 6 |
| HU08_02 | 3 | 4 | 8 |
| HU08_03 | 3 | 4 | 8 |
| HU08_04 | 3 | 4 | 6 |
| HU08_05 | 3 | 3 | 5 |
| HU09_01 | 3 | 8 | 12 |
| HU09_02 | 3 | 4 | 8 |
| HU09_03 | 3 | 3 | 5 |
| HU09_04 | 3 | 4 | 8 |
| HU09_05 | 3 | 4 | 6 |
| HU10_01 | 4 | 4 | 6 |
| HU10_02 | 4 | 2 | 3 |
| HU10_03 | 4 | 6 | 10 |
| HU10_04 | 4 | 4 | 6 |
| HU10_05 | 4 | 6 | 10 |
| HU11_01 | 3 | 6 | 10 |
| HU11_02 | 3 | 2 | 4 |
| HU11_03 | 3 | 3 | 5 |
| HU11_04 | 3 | 3 | 5 |
| HU11_05 | 3 | 3 | 5 |
| HU12_01 | 4 | 8 | 12 |
| HU12_02 | 4 | 8 | 14 |
| HU12_03 | 4 | 4 | 6 |
| HU12_04 | 4 | 4 | 6 |
| HU12_05 | 4 | 2 | 4 |
| HU13_01 | 4 | 4 | 6 |
| HU13_02 | 4 | 4 | 6 |
| HU13_03 | 4 | 3 | 5 |
| HU13_04 | 4 | 4 | 6 |
| HU13_05 | 4 | 4 | 8 |
| HU14_01 | 4 | 3 | 5 |
| HU14_02 | 4 | 3 | 4 |
| HU14_03 | 4 | 6 | 10 |
| HU14_04 | 4 | 3 | 5 |
| HU14_05 | 4 | 4 | 6 |
| HU15_01 | 4 | 6 | 10 |
| HU15_02 | 4 | 3 | 5 |
| HU15_03 | 4 | 4 | 6 |
| HU15_04 | 4 | 3 | 5 |
| HU15_05 | 4 | 4 | 8 |

### Lectura de capacidad

- Sprint 3 candidato: **110–182 h**, antes de reserva.
- Sprint 4 candidato: **106–172 h**, antes de reserva.
- Si cada uno volviera a disponer de solo 60 h, esta hoja de ruta no cabría. No se confirma esa disponibilidad futura; se muestra el escenario para detectar el problema ahora.
- Mantener las 75 historias completas requeriría más capacidad o tiempo si estos rangos se confirman. Si ambas variables están fijas, hay que acordar con el profesor la profundidad de entrega antes de comprometerla; no se da por cumplido lo que falte.
- Las cantidades 19, 6, 25 y 25 indican ubicaciones candidatas, no historias aceptadas. Las correcciones posteriores y el SMTP externo pendiente del sprint 1 se registran aparte, sin volver a sumar historias nuevas.
