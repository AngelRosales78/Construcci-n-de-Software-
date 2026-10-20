# Plan sencillo del sprint 2

Diego Magallanes y Angel Rosales. Propuesta del 19 de octubre de 2026. Sustituye para la selección inmediata la cartera anterior de 19 historias; el reparto de las 75 historias se conserva como referencia separada, sin afirmar que su capacidad esté resuelta.

## Entrega propuesta

Agregar y quitar empresas favoritas y consultar la misma lista desde web y Android usando la misma cuenta. Historias: HU10_01, HU10_02 y HU10_03.

## Capacidad

60 horas persona: 48 para tareas y 12 de reserva. Cada integrante tiene 24 horas previstas y 6 de reserva. Estimación preliminar; falta Planning Poker real.

## Tareas y responsables

| ID | Tarea | Responsable | Horas persona |
|---|---|---|---|
| T01 | Acordar los casos de prueba y estimar juntos el trabajo. | Diego y Angel | 2 |
| T02 | Conectar Android al catálogo real y abrir una ficha básica de empresa. | Angel Rosales | 6 |
| T03 | Guardar favoritos por cuenta, aplicar el límite Free/Pro y permitir quitarlos. | Diego Magallanes | 8 |
| T04 | Crear la lista y los botones de agregar y quitar en la web. | Diego Magallanes | 6 |
| T05 | Crear la lista y los botones de agregar y quitar en Android. | Angel Rosales | 6 |
| T06 | Actualizar los favoritos web y mostrar cuándo se sincronizaron. | Diego Magallanes | 2 |
| T07 | Actualizar favoritos Android, avisar si no hay conexión y permitir reintentar. | Angel Rosales | 4 |
| T08 | Probar límites, duplicados y protección de los favoritos de cada cuenta. | Diego y Angel | 4 |
| T09 | Probar juntos web y Android; corregir errores de integración. | Diego y Angel | 6 |
| T10 | Guardar capturas, resultados y preparar la demostración. | Diego y Angel | 4 |

El servicio de favoritos y la regla mínima Free/Pro quedan a cargo de Diego como propuesta. Angel conecta el catálogo móvil existente, la ficha básica y favoritos. La preparación de planes no equivale a completar la épica de pagos o suscripciones. Las horas conjuntas se dividen por mitades en el control de carga.

## Aceptación

HU10_01: estrella por empresa, confirmación, estado correcto, sin duplicados y límite de 10 Free; Pro permite más. HU10_02: solo se elimina la relación propia, la lista sigue al servidor y los errores no dejan estados falsos. HU10_03: misma cuenta y lista en ambos clientes, última sincronización visible y aviso/reintento sin conexión.

Probar cuenta separada, borrado ajeno, agregado duplicado, borrado repetido, límites Free/Pro y recorrido web → móvil → web. Las pantallas conectadas deben usar empresas reales del catálogo, no las maquetas.

## Cierre

Todas las tareas comienzan Pendientes. No hay commits ni resultados de ejecución de estas historias por el hecho de planificarlas. Guardar capturas, pruebas y commits reales. Validar capacidad de catálogo Android y preparación Free/Pro antes de comprometer la selección; si se excede, revisar alcance sin eliminar criterios silenciosamente. El sprint 1 conserva su evaluación.
