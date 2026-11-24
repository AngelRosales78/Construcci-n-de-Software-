# Sprint 4: estado y evidencia del 23 de noviembre

## Qué quedó preparado

Se completaron el monitoreo del servidor y la web, los canales de avisos, las pantallas Android pendientes y una interfaz web consistente para escritorio y teléfono. Android consume los mismos servicios y reglas. Se añadió el historial de suscripciones móvil de E11, conservando la activación sin cargo monetario.

**El sprint no debe declararse aceptado al 100 %:** falta configurar y comprobar la recepción real de Firebase/FCM. La actualización automática nueva sigue limitada a precios; la fuente oficial de balances volvió a responder HTTP 403 el 23 de noviembre. Las preferencias de Push dependen de esa misma configuración pendiente.

**Actualización de datos del 24 de noviembre:** las 504 fichas cuentan ahora con referencias financieras anuales de una captura pública alternativa, conservando los registros SEC y distinguiendo fechas normalizadas. Esto amplía la cobertura almacenada; la actualización manual de administración continúa limitada a precios. Fuentes, límites y comprobaciones nuevas: [DATOS_FINANCIEROS_CATALOGO.md](DATOS_FINANCIEROS_CATALOGO.md). Esta ejecución no modificó Android.

La rama de trabajo sigue siendo `magallanes-sprint4`; no se crearon ramas adicionales. Se volvió a consultar `origin/Rosales`: permanece en `93e5ea3`, correspondiente al cierre Android del Sprint 3. Se conserva ese trabajo anterior; las incorporaciones móviles de esta ejecución no se atribuyen a Angel.

## Correspondencia con la tabla original

Cada rango incluye `.1` y `.2` de Android, y `.3` y `.4` de Web/API. Se mantienen las veinte historias y los ochenta códigos; las implementaciones compartidas no cuentan como historias adicionales. «Implementada» describe el código y las comprobaciones indicadas aquí, no una aprobación ya otorgada por el profesor.

| Épica | Historia | Tareas | Resultado y estado |
|---|---|---|---|
| E12 | HU12_01 | S4-T01.1–.4 | Alertas privadas ante cambios válidos en favoritas, con fecha y enlace. Generación, audiencia e idempotencia verificadas con pruebas del servidor. No se fabricaron cambios financieros para llenar la pantalla. |
| E12 | HU12_02 | S4-T02.1–.4 | **Parcial:** SDK Android, registro por propietario/sesión, cola de envío y baja de tokens implementados; recepción real pendiente de Firebase. Web informa el estado real del canal y teléfonos vinculados. |
| E12 | HU12_03 | S4-T03.1–.4 | Campana y centro de avisos, contador y paginación; centro Android conectado al mismo contrato. |
| E12 | HU12_04 | S4-T04.1–.4 | Preferencias persistentes verificadas entre Android y API; correo propio con transporte real comprobado. **Aceptación de Push pendiente**, sin sustituirla por correo o campana. |
| E12 | HU12_05 | S4-T05.1–.4 | Lectura individual y masiva privada con límite de lista y contador coherente. |
| E13 | HU13_01 | S4-T06.1–.4 | Usuarios con búsqueda, paginación, rol, plan y estado. Búsqueda administrativa comprobada también en Android real. |
| E13 | HU13_02 | S4-T07.1–.4 | Cambios de rol/plan confirmados, auditados y autorizados; protección del último administrador. Reglas y cambios verificados en servidor; controles disponibles en ambos clientes. |
| E13 | HU13_03 | S4-T08.1–.4 | Bloqueo con motivo y revocación; desbloqueo requiere nuevo acceso. Reglas verificadas en servidor. |
| E13 | HU13_04 | S4-T09.1–.4 | Edición de perfil y avatar, validación de imagen, vista previa y almacenamiento privado. Contrato y validaciones probados; pantalla Android compilada e integrada. |
| E13 | HU13_05 | S4-T10.1–.4 | Eliminación propia con contraseña; datos personales borrados y comprobantes/consumo retenidos sin usuario identificado. No se borró la cuenta del presentador. |
| E14 | HU14_01 | S4-T11.1–.4 | Activación/desactivación con confirmación y ocultación en búsqueda compartida. |
| E14 | HU14_02 | S4-T12.1–.4 | Catálogo general y quince empresas de selección inicial diferenciados; cobertura, fuentes, fechas e indicadores ausentes visibles. Consulta MSFT verificada en Android. La selección continúa sujeta a validación académica. |
| E14 | HU14_03 | S4-T13.1–.4 | **Parcial en alcance de datos:** actualización manual de precios, estado, fechas, conservación ante errores e idempotencia. No descarga balances nuevos; SEC sigue rechazando el acceso. |
| E14 | HU14_04 | S4-T14.1–.4 | Descripción, sector y sitio web editables con validación y persistencia. |
| E14 | HU14_05 | S4-T15.1–.4 | Verificación y alta única de ticker con precios y fuente; no inventa balances, pertenencia al índice ni calificaciones. |
| E15 | HU15_01 | S4-T16.1–.4 | Llamadas y tokens observados; costes históricos solo con tarifa versionada. Generación real comprobada y caché sin llamada adicional. |
| E15 | HU15_02 | S4-T17.1–.4 | Cantidades y porcentajes por plan efectivo, considerando vencimiento y bloqueo, sin confundir rol con plan. Totales reales comprobados en Android. |
| E15 | HU15_03 | S4-T18.1–.4 | Presupuestos diarios editables; aviso al 85 %, bloqueo al 100 %, versión y reinicio por zona. Límites y concurrencia probados; consulta móvil real comprobada. |
| E15 | HU15_04 | S4-T19.1–.4 | Valor mensual proporcional usando vigencias, moneda e importes históricos, conservado tras eliminación del usuario. Es estimación de activaciones, no facturación cobrada. |
| E15 | HU15_05 | S4-T20.1–.4 | Registros privados con filtros, paginación, detalle depurado y límites de conservación. No almacena prompts, contraseñas ni tokens en esos registros. |

## Comprobaciones ejecutadas

| Comprobación | Resultado |
|---|---|
| Servidor completo, PostgreSQL y Redis de validación | **494 aprobadas, 1 optativa omitida**. Tras ajustar solo el texto del comprobante, las **15 pruebas de suscripción** volvieron a pasar. |
| Web | **125 aprobadas** en 27 archivos; compilación de producción correcta. |
| Android unitario | **144 aprobadas, 10 omitidas**, de 154 casos descubiertos; compilación debug y del paquete de pruebas correcta. |
| Android contra API real | **3 aprobadas:** administración/totales/catálogo, preferencias con persistencia y restauración, suscripción con vigencia/historial del servidor. No son pruebas de recepción FCM ni una revisión de todos los gestos en un teléfono físico. |
| Navegador real | Free, Pro y administrador; escritorio y anchura de teléfono. Sin errores de página ni desbordamiento horizontal de la página. Tablas extensas conservan desplazamiento dentro de su contenedor. |
| IA local real | 1 llamada, **1.153 tokens de entrada y 383 de salida**. Segunda consulta desde caché; contador permanece en 1. Sin tarifa configurada: «No calculado». |
| Correo propio | El servidor SMTP real aceptó el mensaje de verificación destinado únicamente a la cuenta autorizada. No se afirma recepción en la bandeja sin observarla. |
| Integridad del servidor | `manage.py check` sin incidencias; ninguna migración pendiente de generar; dependencias Python sin incompatibilidades. |
| Fuente SEC | Nueva consulta oficial el 23/11: HTTP **403**; no se alteraron balances guardados. |
| Precios reales | MSFT actualizado con éxito: 1.254 sesiones, último cierre del **20/11/2026**, dos fechas nuevas y una llamada contabilizada al proveedor de mercado. |

Los primeros intentos del emulador tuvieron bloqueos del sistema y tiempos de arranque. Se usó una imagen de datos separada y precompilación de paquetes; el dispositivo original no se borró. Se corrigió el desplazamiento del acceso al aparecer el teclado y se volvió a compilar. Los tres recorridos citados corresponden a la ejecución final aprobada, no a intentos anteriores fallidos.

## Cambios guardados por bloques completos

| Commit | Alcance |
|---|---|
| `01de030` | Servidor: monitoreo HU15, entrega de avisos HU12 y conservación anónima HU13_05; migraciones y pruebas. |
| `25c0a13` | Web: monitoreo, estado de canales, navegación y diseño final; comprobantes comprensibles sin cargo. |
| `16a6172` | Android: cuenta, avisos, administración, catálogo, monitoreo y suscripciones; pruebas y exclusión de credenciales. |

Los archivos compartidos se guardaron completos para que cada bloque conserve sus dependencias. No se reescribió el historial anterior ni se crearon commits por retoques pequeños. Los códigos de esta tabla permiten encontrar las tareas aunque varias usen el mismo servicio compartido.

## Dónde mirar

- Presentación breve: [cómo presentar](SPRINT4_COMO_PRESENTAR.md).
- Arranque, pruebas y Firebase: [configuración y verificación](SPRINT4_CONFIGURACION_Y_VERIFICACION.md).
- Criterio seguido: [instrucción de trabajo](SPRINT4_PROMPT_DE_TRABAJO.md).
- Servidor: `backend/api/monitoring_*`, `delivery_services.py`, `notification_*` y `push_views.py`.
- Web: `AdminMonitoringPage.jsx`, `WorkspaceLayout.jsx`, `product.css` y sus ViewModels.
- Android: `ui/operations` muestra las pantallas; `OperationsViewModel` controla su estado; `OperationsRepository` llama a la API; `data/push` contiene la integración FCM.
- Evidencia pública depurada: carpeta [evidencias/sprint4](evidencias/sprint4/). Los registros extensos, las capturas completas y la compilación instalable permanecen en `output`, fuera del repositorio; los archivos de acceso no se publican.

Para cerrar lo pendiente hacen falta la configuración Firebase del mismo proyecto Android/servidor y una recepción real verificada. Los balances nuevos necesitan una fuente accesible y su validación anual. Tener permisos sobre esta computadora no resuelve esas dependencias externas.
