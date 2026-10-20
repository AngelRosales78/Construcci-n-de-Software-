# Sprint 2 propuesto y distribución equilibrada de las 75 historias

Equipo: Diego Magallanes (web) y Angel Rosales (móvil). Revisión del 19 de octubre de 2026. Esta propuesta sustituye el reparto anterior de seis historias. Se inspira en la trazabilidad del ejemplo AutoSmart, con organización y contenido propios.

## Objetivo de reparto

| Sprint | Historias candidatas | Cantidad |
|---|---|---|
| 1 histórico | Épicas 1 2 3 y HU04_01 a HU04_04 | 19 |
| 2 propuesto | HU04_05; épicas 5 10 y 11 completas; HU13_01 HU13_03 HU13_04 | 19 |
| 3 candidato | Épicas 6 7 8; HU13_02 HU13_05 HU14_01 HU14_02 | 19 |
| 4 candidato | Épicas 9 12 15; HU14_03 HU14_04 HU14_05 | 18 |

No hay duplicados ni historias eliminadas. Las 19 del sprint 1 describen su selección histórica, no certifican de nuevo su aceptación. Sus correcciones y SMTP externo pendiente no se suman como historias nuevas.

## Capacidad y decisión pendiente

La capacidad declarada es 60 horas persona (2 × 3 h × 10 días); con 12 h de reserva quedan 48 h para tareas. El objetivo de 19 historias estima **116–190 horas persona antes de reserva**, o 128–202 h incluyendo ese margen. Excede las 48 h de trabajo disponibles por 68–142 h. Por eso es una cartera candidata y **no un compromiso viable de 19 historias en diez días con la capacidad actual**.

Los rangos son estimaciones preliminares por tarea, no Planning Poker, tiempo trabajado ni velocidad medida. No se mantienen las 48 h del plan de seis historias al agregar trece historias nuevas. El equipo debe estimar, confirmar capacidad y alcance antes de comprometer esta selección; si las restricciones siguen fijas, hace falta acordar su alcance con el profesor.

## Reparto de responsabilidades

Diego conserva la web y Angel el móvil. Para cubrir el servidor, se propone Diego para análisis y administración, y Angel para planes y favoritos; este reparto adicional todavía debe acordarse. Las filas conjuntas expresan horas persona totales, no esas horas para cada integrante. Para comprobar carga se dividen por mitades como hipótesis.

| Integrante | Función confirmada | Carga propuesta estimada | Capacidad bruta |
|---|---|---|---|
| Diego Magallanes | Web | 68,25–109,5 h | 30 h |
| Angel Rosales | Móvil | 47,75–80,5 h | 30 h |

El catálogo financiero Android todavía es una maqueta. S2-TEC03 hace visible la conexión mínima necesaria para favoritos móviles; no se declara que búsqueda, gráfico o análisis completos estén implementados en Android. La recuperación SMTP local probada no acredita el correo externo requerido por los comprobantes.

## Historias y tareas

### HU11_02 Planes Free y Pro

Como usuario, quiero comparar las características de los planes Free y Pro en una tabla clara.

Orden de desarrollo propuesto: 1. Horas estimadas: 2–4. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU11_02-T01 | Definir beneficios y configuración del precio simulado | Angel Rosales | API | 0,5–1 | — | — | Sin iniciar |
| HU11_02-T02 | Mostrar comparación de planes y acceso a la simulación | Diego Magallanes | Web | 1–2 | — | — | Sin iniciar |
| HU11_02-T03 | Contrastar tabla con restricciones de API | Diego y Angel | Pruebas | 0,5–1 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: La tabla distingue funciones Free y Pro y coincide con las restricciones de la API.
- CA2: El precio de demostración se obtiene de configuración y se identifica como simulado.
- CA3: Elegir Pro abre la simulación de pago, sin cobro real.

Dependencia: Definición compartida de beneficios.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU11_01 Alta Pro simulada

Como usuario, quiero realizar la simulación de pago para actualizar mi plan a Pro.

Orden de desarrollo propuesto: 2. Horas estimadas: 6–10. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU11_01-T01 | Persistir simulación idempotente y activar Pro solo al éxito | Angel Rosales | API | 3–5 | — | — | Sin iniciar |
| HU11_01-T02 | Crear flujo de pago ficticio sin cobro real | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU11_01-T03 | Verificar éxito fallo y repetición de operación | Diego y Angel | Pruebas | 1–2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Utiliza únicamente datos ficticios y presenta la operación como simulada sin cobro.
- CA2: El éxito actualiza el plan a Pro, no el rol a Administrador.
- CA3: Repetir la misma operación no duplica la transacción y un fallo no activa el plan.

Dependencia: Autenticación y matriz Free y Pro.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU11_04 Estado de suscripción

Como usuario, quiero ver el estado de mi suscripción actual y su fecha de vencimiento.

Orden de desarrollo propuesto: 3. Horas estimadas: 3–5. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU11_04-T01 | Exponer plan vigencia y transacciones propias | Angel Rosales | API | 1–2 | — | — | Sin iniciar |
| HU11_04-T02 | Mostrar estado y renovación simulada en perfil | Diego Magallanes | Web | 1–2 | — | — | Sin iniciar |
| HU11_04-T03 | Comprobar fechas aislamiento entre usuarios e historial | Diego y Angel | Pruebas | 1 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Muestra del plan activo (Free o Pro).
- CA2: Fecha exacta de la próxima renovación simulada.
- CA3: Historial de transacciones pasadas.
- CA4: Ubicado dentro del perfil de usuario.

Dependencia: HU11_01.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU11_05 Cancelación de suscripción

Como usuario Pro, quiero poder cancelar mi suscripción en cualquier momento para volver a Free.

Orden de desarrollo propuesto: 4. Horas estimadas: 3–5. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU11_05-T01 | Cancelar renovación y aplicar vencimiento sin alterar rol | Angel Rosales | API | 1–2 | — | — | Sin iniciar |
| HU11_05-T02 | Mostrar cancelación y fecha final de acceso Pro | Diego Magallanes | Web | 1–2 | — | — | Sin iniciar |
| HU11_05-T03 | Probar cancelación repetida y paso a Free al vencer | Diego y Angel | Pruebas | 1 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Cancelar desactiva la renovación y conserva Pro hasta el fin de vigencia.
- CA2: Al vencer la vigencia el plan pasa a Free sin cambiar el rol del usuario.
- CA3: Repetir la cancelación no crea cobros ni altera la fecha de término.

Dependencia: HU11_04.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU11_03 Comprobante simulado

Como usuario, quiero recibir un comprobante digital tras procesar la suscripción simulada.

Orden de desarrollo propuesto: 5. Horas estimadas: 5–8. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU11_03-T01 | Generar recibo con ID y copia mediante servicio de correo | Angel Rosales | API correo | 2–3 | — | — | Sin iniciar |
| HU11_03-T02 | Mostrar descargar o imprimir comprobante simulado | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU11_03-T03 | Comprobar importe plan titular y envío de copia | Diego y Angel | Pruebas | 1–2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Recibo digital en pantalla con ID de transacción.
- CA2: Resumen del plan contratado y monto simulado.
- CA3: Opción para descargar o imprimir comprobante.
- CA4: Envío automático de copia por correo.

Dependencia: HU11_01.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU10_01 Agregar favoritos

Como usuario, quiero agregar empresas a mi lista de favoritos para seguimiento directo.

Orden de desarrollo propuesto: 6. Horas estimadas: 6–9. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU10_01-T01 | Guardar favorito único y exigir límite por plan en servidor | Angel Rosales | API | 2–3 | — | — | Sin iniciar |
| HU10_01-T02 | Agregar estrella estado y confirmación en la ficha | Diego Magallanes | Web | 1–2 | — | — | Sin iniciar |
| HU10_01-T03 | Permitir agregar desde el catálogo móvil conectado | Angel Rosales | Móvil | 1–2 | — | — | Sin iniciar |
| HU10_01-T04 | Probar duplicados límite Free y acceso Pro | Diego y Angel | Pruebas | 2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Ícono de estrella interactivo en cada perfil.
- CA2: Actualización instantánea del estado visual.
- CA3: Confirmación con mensaje corto (toast).
- CA4: Límite de 10 en Free / Ilimitado en Pro.

Dependencia: Sesión autenticada y catálogo; control de plan para aplicar límites.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU10_02 Quitar favoritos

Como usuario, quiero quitar empresas de mis favoritos cuando ya no las requiera.

Orden de desarrollo propuesto: 7. Horas estimadas: 4–6. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU10_02-T01 | Eliminar solo la relación del usuario autenticado | Angel Rosales | API | 1–2 | — | — | Sin iniciar |
| HU10_02-T02 | Quitar desde ficha y lista web | Diego Magallanes | Web | 1 | — | — | Sin iniciar |
| HU10_02-T03 | Actualizar lista y acción de quitar en Android | Angel Rosales | Móvil | 1 | — | — | Sin iniciar |
| HU10_02-T04 | Probar reintento y rechazo de borrado ajeno | Diego y Angel | Pruebas | 1–2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Quitar un favorito elimina solo la relación de la cuenta autenticada.
- CA2: La ficha y la lista reflejan el cambio tras confirmación del servidor.
- CA3: Deshacer es una mejora opcional fuera del criterio mínimo y no una tarea obligatoria.

Dependencia: HU10_01.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU10_03 Sincronización de favoritos

Como usuario, quiero que mis empresas favoritas se sincronicen entre la Web y la App Móvil.

Orden de desarrollo propuesto: 8. Horas estimadas: 6–10. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU10_03-T01 | Exponer el estado centralizado de la misma cuenta | Angel Rosales | API | 1–2 | — | — | Sin iniciar |
| HU10_03-T02 | Sincronizar lista y fecha de actualización web | Diego Magallanes | Web | 1–2 | — | — | Sin iniciar |
| HU10_03-T03 | Sincronizar Android con reintento y caché desactualizada | Angel Rosales | Móvil | 2–4 | — | — | Sin iniciar |
| HU10_03-T04 | Verificar paridad y aislamiento con dos cuentas | Diego y Angel | Pruebas | 2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Web y Android consultan favoritos de la misma cuenta en el servidor.
- CA2: Los cambios aparecen al sincronizar, con fecha de última sincronización.
- CA3: Sin conexión se declara caché desactualizada y se define cómo reintentar; no se promete inmediatez sin conexión.

Dependencia: HU10_01 y sesión web y Android.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU10_04 Fichas vistas recientemente

Como usuario, quiero revisar un historial de empresas consultadas recientemente.

Orden de desarrollo propuesto: 9. Horas estimadas: 4–6. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU10_04-T01 | Persistir últimas diez fichas sin duplicados por usuario | Diego Magallanes | API | 1–2 | — | — | Sin iniciar |
| HU10_04-T02 | Registrar aperturas y mostrar fichas recientes | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU10_04-T03 | Verificar orden límite y diferencia con búsquedas | Diego y Angel | Pruebas | 1 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Registra las últimas diez fichas abiertas por la cuenta, con fecha y orden reciente.
- CA2: Reabrir una ficha la mueve al inicio sin duplicarla.
- CA3: No sustituye ni duplica el historial de términos de HU03_05.

Dependencia: HU04_02 y sesión autenticada.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU10_05 Etiquetas de favoritos

Como usuario, quiero etiquetar y categorizar mis empresas favoritas en listas personalizadas.

Orden de desarrollo propuesto: 10. Horas estimadas: 6–10. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU10_05-T01 | Crear etiquetas y relaciones con autorización Pro | Angel Rosales | API | 2–3 | — | — | Sin iniciar |
| HU10_05-T02 | Gestionar asignación múltiple y filtro por etiqueta | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU10_05-T03 | Probar propiedad relaciones y rechazo del plan Free | Diego y Angel | Pruebas | 2–4 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Crear etiquetas personalizadas (ej. "Monitorear").
- CA2: Asignar una o varias etiquetas por empresa.
- CA3: Filtrado por etiqueta en la vista de favoritos.
- CA4: Función exclusiva para usuarios Pro.

Dependencia: HU10_01 y control de plan.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU13_04 Edición del perfil

Como usuario, quiero editar la información básica de mi perfil (nombre y avatar).

Orden de desarrollo propuesto: 11. Horas estimadas: 8–12. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU13_04-T01 | Validar y guardar nombre apellido e imagen menor de 2 MB | Diego Magallanes | API | 2–3 | — | — | Sin iniciar |
| HU13_04-T02 | Editar previsualizar y actualizar cabecera web | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU13_04-T03 | Editar perfil y previsualizar avatar en Android | Angel Rosales | Móvil | 2–4 | — | — | Sin iniciar |
| HU13_04-T04 | Probar tamaño formato y actualización de ambas interfaces | Diego y Angel | Pruebas | 2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Edición de nombre, apellido y foto de perfil.
- CA2: Carga de imagen con vista previa.
- CA3: Validación de formato y tamaño de imagen (< 2MB).
- CA4: Actualización en la barra de navegación.

Dependencia: Contrato compartido de autenticación y permisos.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU13_01 Listado administrativo

Como Administrador, quiero visualizar la lista completa de usuarios registrados y sus planes.

Orden de desarrollo propuesto: 12. Horas estimadas: 4–6. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU13_01-T01 | Exponer usuarios paginados con rol plan estado y filtros | Diego Magallanes | API | 1–2 | — | — | Sin iniciar |
| HU13_01-T02 | Crear tabla búsqueda y filtros administrativos | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU13_01-T03 | Verificar paginación datos y acceso exclusivo de admin | Angel Rosales | Pruebas | 1 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Tabla paginada de usuarios en el Panel Admin.
- CA2: Mostrar: ID, Nombre, Correo, Rol, Plan, Estado.
- CA3: Buscador por nombre o correo electrónico.
- CA4: Filtros por plan (Free/Pro) y estado.

Dependencia: Contrato compartido de autenticación y permisos.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU13_03 Bloqueo de cuentas

Como Administrador, quiero inhabilitar o bloquear cuentas de usuario por mal uso.

Orden de desarrollo propuesto: 13. Horas estimadas: 3–5. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU13_03-T01 | Bloquear cuenta y revocar sesiones según política | Diego Magallanes | API | 1–2 | — | — | Sin iniciar |
| HU13_03-T02 | Mostrar acción administrativa de bloquear y desbloquear | Diego Magallanes | Web | 1 | — | — | Sin iniciar |
| HU13_03-T03 | Probar acceso activo bloqueado y desbloqueado | Angel Rosales | Pruebas | 1–2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Bloquear impide nuevos accesos e invalida las sesiones activas según la política documentada.
- CA2: Las API rechazan peticiones de la cuenta bloqueada.
- CA3: Desbloquear no restaura tokens revocados; exige un acceso válido.

Dependencia: Contrato compartido de autenticación y permisos.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU05_02 Liquidez

Como usuario, quiero analizar el Pilar de Liquidez con ratios de corto plazo.

Orden de desarrollo propuesto: 14. Horas estimadas: 6–10. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU05_02-T01 | Calcular liquidez corriente y prueba ácida con componentes | Diego Magallanes | API | 2–3 | — | — | Sin iniciar |
| HU05_02-T02 | Mostrar ratios fuente periodo y umbrales explicados | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU05_02-T03 | Probar fórmula conocida pasivo cero y datos ausentes | Diego y Angel | Pruebas | 2–4 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Muestra liquidez corriente y prueba ácida con componentes y periodo.
- CA2: Denominador cero o datos insuficientes muestran No disponible.
- CA3: Los colores usan umbrales configurados y explicados; no un óptimo universal.

Dependencia: Datos y metodología de liquidez.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU05_03 Rentabilidad

Como usuario, quiero evaluar el Pilar de Rentabilidad examinando márgenes y retorno.

Orden de desarrollo propuesto: 15. Horas estimadas: 6–10. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU05_03-T01 | Calcular ROE ROA y margen neto con política de promedios | Diego Magallanes | API | 2–3 | — | — | Sin iniciar |
| HU05_03-T02 | Mostrar ratios y ausencia de referencia sectorial suficiente | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU05_03-T03 | Probar redondeo patrimonio negativo y denominadores cero | Diego y Angel | Pruebas | 2–4 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Muestra ROE, ROA y margen neto con fórmula y periodo.
- CA2: La referencia sectorial declara muestra y fecha y se omite si es insuficiente.
- CA3: Explica denominadores cero y patrimonio negativo.

Dependencia: Datos de rentabilidad; muestra comparable para referencia sectorial.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU05_04 Solvencia

Como usuario, quiero revisar el Pilar de Solvencia para entender la estructura de deuda.

Orden de desarrollo propuesto: 16. Horas estimadas: 6–10. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU05_04-T01 | Calcular deuda sobre patrimonio y cobertura de intereses | Diego Magallanes | API | 2–3 | — | — | Sin iniciar |
| HU05_04-T02 | Mostrar componentes y diferencia respecto de Altman Z | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU05_04-T03 | Probar gasto cero patrimonio negativo y datos ausentes | Diego y Angel | Pruebas | 2–4 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Muestra Deuda/Patrimonio y Cobertura de Intereses con componentes y fecha.
- CA2: Documenta umbrales y casos no aplicables.
- CA3: Distingue este detalle del gauge Altman-Z.

Dependencia: Datos de deuda y gastos financieros.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU05_05 Eficiencia

Como usuario, quiero inspeccionar el Pilar de Eficiencia Operativa respecto a la rotación de activos.

Orden de desarrollo propuesto: 17. Horas estimadas: 7–11. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU05_05-T01 | Calcular rotación de activos y días de inventario | Diego Magallanes | API | 3–4 | — | — | Sin iniciar |
| HU05_05-T02 | Mostrar hasta tres años comparables y casos no aplicables | Diego Magallanes | Web | 2–3 | — | — | Sin iniciar |
| HU05_05-T03 | Probar series incompletas promedios y divisiones por cero | Diego y Angel | Pruebas | 2–4 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Muestra rotación de activos y días de inventario con definición y periodo.
- CA2: Presenta hasta tres años disponibles sin inventar los años faltantes.
- CA3: Si el inventario no aplica o falta el denominador, informa No disponible y explica el motivo.

Dependencia: Histórico de activos, ingresos e inventario aplicable.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU05_01 Cinco pilares y crecimiento

Como usuario, quiero explorar el desglose de los 5 pilares financieros de una empresa.

Orden de desarrollo propuesto: 18. Horas estimadas: 7–12. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU05_01-T01 | Calcular crecimiento anual y CAGR a tres años | Diego Magallanes | API | 2–4 | — | — | Sin iniciar |
| HU05_01-T02 | Mostrar radar y abrir detalle de los cinco pilares | Diego Magallanes | Web | 3–4 | — | — | Sin iniciar |
| HU05_01-T03 | Probar cinco notas versionadas y cobertura insuficiente | Diego y Angel | Pruebas | 2–4 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: El radar muestra Liquidez, Rentabilidad, Solvencia, Eficiencia y Crecimiento, con notas versionadas o No disponible.
- CA2: Esta HU incluye Crecimiento: variación anual de ingresos y tasa compuesta a tres años si hay datos.
- CA3: Cada eje abre su detalle; las comparaciones declaran cobertura y periodo.

Dependencia: Datos financieros suficientes; HU05_02 a HU05_05 para completar el radar.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

### HU04_05 Puntaje general

Como usuario, quiero visualizar un puntaje numérico unificado sobre la salud general (0 a 100).

Orden de desarrollo propuesto: 19. Horas estimadas: 4–6. SP: pendiente de Planning Poker. Estado: Pendiente.

| Tarea | Trabajo | Responsable propuesto | Capa | h estimadas | Commit | Fecha | Integración |
|---|---|---|---|---|---|---|---|
| HU04_05-T01 | Agregar notas con ponderaciones y metodología versionadas | Diego Magallanes | API | 1–2 | — | — | Sin iniciar |
| HU04_05-T02 | Mostrar puntaje 0 a 100 o motivo de ausencia | Diego Magallanes | Web | 1–2 | — | — | Sin iniciar |
| HU04_05-T03 | Probar resultado conocido límites y falta de un pilar | Diego y Angel | Pruebas | 2 | — | — | Sin iniciar |

Criterios de aceptación:

- CA1: Agrega las cinco notas de pilares mediante una fórmula versionada y documentada.
- CA2: El resultado está entre 0 y 100 y el redondeo es igual en API e interfaz.
- CA3: Sin cobertura de los cinco pilares muestra No disponible y explica qué falta; no sustituye Altman-Z.

Dependencia: HU05_01 a HU05_05.
Evidencia de cada criterio: pendiente de ejecución. Registrar caso, resultado esperado, obtenido, prueba o archivo y commit.

## Tareas compartidas

| Trabajo | Responsable | Capa | h estimadas | Estado |
|---|---|---|---|---|
| S2-TEC01 Acordar metodología beneficios Free Pro y Planning Poker | Diego y Angel | Planificación | 2–4 | Pendiente |
| S2-TEC02 Obtener y validar cuatro cierres MSFT y persistencia por periodo | Diego y Angel | Datos | 4–8 | Pendiente |
| S2-TEC03 Conectar catálogo y navegación financiera mínima de Android | Angel Rosales | Móvil | 6–10 | Pendiente |
| S2-TEC04 Integrar contratos y ejecutar regresión web móvil y servidor | Diego y Angel | Integración | 6–10 | Pendiente |
| S2-TEC05 Registrar evidencias burndown y demostración | Diego y Angel | Cierre | 2–3 | Pendiente |

## Dependencias y secuencia

1. Primero se definen beneficios y plan compartido, con vigencia y cancelación simuladas; plan Pro y rol administrador permanecen separados.
2. Favoritos, límites y etiquetas dependen del plan; la sincronización requiere catálogo móvil real y la misma API para ambos clientes.
3. La lista de usuarios usa el plan compartido. Perfil incluye avatar y validación de formato y tamaño; no se da por completo con solo GET/PATCH de nombre.
4. Se importa la información suficiente para los pilares; radar y puntaje requieren cinco notas con metodología versionada. Altman-Z no reemplaza ese puntaje.
5. Sprint 3: escenarios e IA antes de conclusiones del comparador; los permisos Pro ya deberían existir. Sprint 4: PDF después de IA/comparador y notificaciones después de favoritos.

No se fija un calendario que prometa acabar todas las tareas en diez días mientras exista la brecha de capacidad. El día 1 debe cerrar estimación y alcance; después se registra trabajo pendiente cada día. El burndown real empieza sin datos de ejecución y no se rellena con commits de otros sprints.

## Estados evidencias y definición de terminado

Pendiente → En progreso → Implementada en rama → Integrada → Verificada. Terminada exige todos los criterios, código integrado y pruebas aprobadas. Un ajuste obligatorio pendiente o un PR sin integrar no equivale a Terminada. Se dejan vacíos commits, fechas y PR hasta tener evidencia real. No se atribuye a Diego o Angel el trabajo de sus compañeros del ejemplo.

El cierre exige una demostración reproducible con datos verificables, pruebas de permisos, errores y casos límite, más regresión de web, Android y servidor según las tareas afectadas. El alcance Android se valida por separado.

## Conclusiones

Se equilibra la cantidad de historias en 19, 19, 19 y 18 y se hacen visibles los prerrequisitos de planes, favoritos y móvil. Este reparto corrige la concentración de 25 historias en cada sprint final, pero no demuestra que el esfuerzo quepa. La carga de cada integrante y la brecha de horas deben resolverse antes de convertir la cartera en compromiso.
