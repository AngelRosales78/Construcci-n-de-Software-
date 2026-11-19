# Sprint 4: plan, fechas y reparto

Ventana acordada: miércoles 18 a martes 24 de noviembre de 2026. La revisión comenzó
el 18; las implementaciones y los commits llevan la fecha real de su ejecución.
El 19 se confirmó el reparto. El martes se reserva para pruebas, integración y entrega.

## Reparto

- Diego Magallanes: web React, ViewModel, repositorios, servidor Django, datos,
  permisos y pruebas compartidas. También corrige pendientes de Android anteriores.
- Angel Rosales: Android del Sprint 4, consumiendo los contratos compartidos.
- La rama de Diego es `magallanes-sprint4`, creada desde `81d3503`. La revisión de
  `origin/Rosales` al comenzar encontró `93e5ea3`; su trabajo se reutiliza con atribución.
- Se conserva la rama anterior como referencia. Los cambios de Rosales se revisan
  antes de integrarlos, con pruebas de contratos y resolución explícita de migraciones.

## Alcance base y criterios

La hoja de ruta asigna las épicas 12 a 15 al Sprint 4. El backlog general contiene
20 historias y 100 tareas propuestas, cinco por historia. Es el total base del
documento, no una cantidad de tareas exclusiva de Diego. Los identificadores
originales se mantienen; una variante Android consume el mismo contrato y no se
cuenta como otra historia terminada. Las tareas de interfaz, servidor, datos, UX
y pruebas se cierran juntas cuando el flujo real cumple sus criterios.

| Historia | Tareas originales | Resultado verificable |
|---|---|---|
| HU12_01 | T12.1.1 a T12.1.5 | Cambio válido de categoría en favoritos genera una alerta privada, fechada, con enlace y sin duplicados. |
| HU12_02 | T12.2.1 a T12.2.5 | Django entrega Push por FCM; Android abre la empresa y respeta permisos, preferencias y tokens vencidos. |
| HU12_03 | T12.3.1 a T12.3.5 | Campana web con avisos cronológicos, paginación y contador de no leídos. |
| HU12_04 | T12.4.1 a T12.4.5 | Preferencias persistidas por tipo y canal, aplicadas en el envío. |
| HU12_05 | T12.5.1 a T12.5.5 | Lectura individual y masiva privada, contador coherente y reintentos seguros. |
| HU13_01 | T13.1.1 a T13.1.5 | Administración lista usuarios con paginación, búsqueda, rol, plan y estado. |
| HU13_02 | T13.2.1 a T13.2.5 | Cambio autorizado y auditado de rol/plan, protegiendo al último administrador activo. |
| HU13_03 | T13.3.1 a T13.3.5 | Bloqueo rechaza login y sesiones existentes; desbloqueo requiere un nuevo acceso. |
| HU13_04 | T13.4.1 a T13.4.5 | Perfil editable y avatar validado, menor de 2 MB, con vista previa y reflejo en navegación. |
| HU13_05 | T13.5.1 a T13.5.5 | Eliminación propia con reautenticación, borrado de datos asociados y registros retenidos anonimizados. |
| HU14_01 | T14.1.1 a T14.1.5 | Administración activa/desactiva empresas; las inactivas se ocultan en búsquedas compartidas. |
| HU14_02 | T14.2.1 a T14.2.5 | Catálogo y piloto diferenciados, con cobertura, fecha y fuente por empresa. |
| HU14_03 | T14.3.1 a T14.3.5 | Actualización manual idempotente, con resultado visible, conservación ante errores y límites de llamadas. |
| HU14_04 | T14.4.1 a T14.4.5 | Edición validada de descripción, sector y sitio web con persistencia y reflejo público. |
| HU14_05 | T14.5.1 a T14.5.5 | Alta validada de ticker sin duplicados ni clasificación ficticia cuando faltan datos. |
| HU15_01 | T15.1.1 a T15.1.5 | Consumo real de llamadas/tokens de IA y estimación con tarifa versionada cuando exista. |
| HU15_02 | T15.2.1 a T15.2.5 | Usuarios por plan efectivo, cantidades, porcentajes, fecha y estado vacío. |
| HU15_03 | T15.3.1 a T15.3.5 | Presupuesto de API configurable, avisos al 85 %, bloqueo al 100 % y reinicio de ventana. |
| HU15_04 | T15.4.1 a T15.4.5 | Importe mensual simulado con vigencias y tarifas históricas de la épica 11. |
| HU15_05 | T15.5.1 a T15.5.5 | Registro privado de errores, filtros, paginación y exclusión de claves y datos personales innecesarios. |

## Correcciones anteriores incorporadas a la ventana

- Completar el resumen de GOOGL y META reutilizando las instantáneas de Rosales y
  conservar la fecha y fuente de cada indicador. Mantener la cobertura de MSFT.
- Complementar gasto por intereses de FY2024/FY2025 desde los informes 10-K,
  identificando la evidencia transcrita y su procedencia por componente.
- Conservar inventario y notas globales como pendientes cuando falten componentes.
- Revisar Android anterior contra el código y los contratos publicados. El faltante
  identificado es la épica 11: planes, checkout simulado, comprobante, vigencia,
  historial y cancelación. Verificar además expiración, fuentes y mensajes de datos.
- Mantener el envío institucional de comprobantes ya verificado. El propietario
  confirmó recepción; las credenciales siguen fuera del repositorio.

## Cronograma de ejecución y commits

Las horas son orientativas. Son objetivos de trabajo; cada commit exige código y
verificación reales. Si cambia el avance se actualiza el plan, sin fechar commits
en días pasados o futuros ni crear commits vacíos.

| Día | Hora aproximada | Rama de Diego | Bloque y código a escribir | Mensaje previsto |
|---|---|---|---|---|
| Mié 18 | Revisión inicial | Rama anterior | Revisar alcance y cobertura. Sin commit artificial por la planificación. | Sin commit previsto |
| Jue 19 | Primer bloque | magallanes-sprint4 | Procedencia por indicador/componente, importación validada y presentación de fechas. | Se completaron los indicadores disponibles y su procedencia por empresa |
| Jue 19 | Segundo bloque | magallanes-sprint4 | HU13_01–05: administración de usuarios, permisos, auditoría, perfil y eliminación. | Se incorporó la administración de usuarios y la gestión del perfil |
| Vie 20 | Desarrollo y verificación | magallanes-sprint4 | HU14_01–05: catálogo administrable, cobertura y actualización manual. | Se incorporó la gestión del catálogo y la actualización de datos |
| Sáb 21 | Desarrollo y verificación | magallanes-sprint4 | HU12_01–05: eventos, avisos web, preferencias y servicio Push compartido. | Se implementaron las alertas de favoritos y las preferencias de notificación |
| Dom 22 | Desarrollo y verificación | magallanes-sprint4 | HU15_01–05: métricas de uso, planes, cuotas y logs administrativos. | Se incorporó el monitoreo de uso y los registros del sistema |
| Lun 23 | Integración | magallanes-sprint4 | Android de épica 11 pendiente, revisión de contratos de Rosales y pruebas integradas. | Se completaron las suscripciones móviles pendientes y se verificó la integración |
| Mar 24 | Mañana y cierre | magallanes-sprint4 | Regresión, dispositivo/emulador, comprobantes, seguridad y evidencias para presentar. | Se verificó la entrega del Sprint 4 y se documentaron sus evidencias |

Los mensajes son previsiones, no commits ya realizados. Un defecto
encontrado durante las pruebas se corrige en un commit específico cuando corresponda.
Rosales conserva autoría de su Android del Sprint 4 y sus propios commits reales.

## Avance real del 19 de noviembre

- Datos complementados y verificados, commit `a27317f`. Las fuentes y límites
  se detallan en `datos/sprint3/PROCEDENCIA.md`.
- HU13_01–05 implementadas y verificadas en web y servidor. Evidencias, archivos,
  demostración y contrato Android en `SPRINT4_HU13_EVIDENCIAS.md`.
- Pruebas completas de este bloque, 423 del servidor y 109 de la web aprobadas.
  Una prueba optativa de Redis quedó omitida; la compilación web fue correcta.
- HU12, HU14 y HU15, Android anterior de épica 11 y la integración final siguen
  pendientes. El Android del Sprint 4 de Rosales requiere su propia evidencia.
- Este cronograma organiza el trabajo, no programa ejecuciones automáticas en
  días futuros ni acredita tareas aún sin implementar.

## Dependencias que deben resolverse antes del cierre

- Firebase/FCM requiere proyecto, configuración Android y credencial de servidor
  fuera de Git. La recepción Push se acepta en dispositivo/emulador compatible,
  incluyendo primer plano, segundo plano y aplicación cerrada normalmente.
- La muestra de quince empresas se configura como piloto sin reducir el catálogo
  completo. Debe quedar registrada su selección; mostrar símbolos no certifica
  cobertura financiera completa.
- Se distinguen las cuotas oficiales del proveedor de los presupuestos internos
  configurados por la aplicación. La estimación de planes Pro conserva su carácter simulado.
- El cierre requiere evidencia por criterio. La existencia de una pantalla o una
  prueba simulada de FCM no certifica recepción Push real.
