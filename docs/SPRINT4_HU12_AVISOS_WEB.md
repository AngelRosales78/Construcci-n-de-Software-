# Avisos web y preferencias

Avance real del 19 de noviembre de 2026. Rama `magallanes-sprint4`.
El usuario confirmó que Rosales todavía no tiene Firebase configurado. Push en
Android continúa pendiente y no se acredita recepción en un teléfono.

## Seguimiento del plan

| Historia | Tareas de ejecución | Estado de Web/API |
|---|---|---|
| HU12_01 | S4-T01.3 y S4-T01.4 | Verificadas: registrar cambios válidos del perfil y distribuir un aviso privado por favorito, sin duplicados. |
| HU12_03 | S4-T03.3 y S4-T03.4 | Verificadas: campana, contador, centro de avisos, enlace a empresa y paginación estable. |
| HU12_05 | S4-T05.3 y S4-T05.4 | Verificadas: lectura individual y masiva, contador coherente y reintentos idempotentes. |
| HU12_04 | S4-T04.3 y S4-T04.4 | Parciales: preferencias web con guardado automático y control de versión; faltan canales de correo de alertas y Push. |
| HU12_02 | S4-T02.3 y S4-T02.4 | Pendientes: servicio de entrega Push, proyecto Firebase y prueba integrada con Android. |

Se suman seis tareas verificadas a las veinte anteriores: **26/40 Web/API**.
Las dos tareas de preferencias no se cuentan cerradas mientras falten los otros
canales. Esto no cierra las historias completas de ambos clientes; Android del
Sprint 4 corresponde a Rosales. Referencias originales T12.1.1 a T12.5.5.

## Qué cambio produce una alerta

Se observa el **perfil Altman-Z de la ficha**, cuyas categorías existentes son
Muy sólido, Moderado y Muy débil. No se inventó otra escala ni se confunde ese
perfil con el promedio de cinco pilares o con una variación del precio.

Los importadores de resúmenes y balances CSV comparan el último perfil válido
con el nuevo, después de validar y guardar los datos. La primera observación
establece una línea base sin generar avisos retroactivos. Un resumen de
demostración, un resultado incompleto o una fecha anterior no producen alertas.
Reimportar el mismo perfil no genera otro evento. Un cambio de categoría válido
incrementa una revisión y registra antes/después, fuente y fecha del balance.

El evento se guarda en la misma transacción que el balance. Un procesador separado
lo distribuye después, para que una falla al crear avisos no revierta datos
financieros ya confirmados. Solo reciben el aviso las cuentas activas que siguen
manteniendo el favorito, añadido antes del evento, y tienen habilitado ese tipo
de aviso y el canal web en el momento de distribuirlo. Quitar y volver a añadir
un favorito no recupera eventos anteriores.

La restricción usuario/evento impide duplicados. Un error de integridad deja el
evento pendiente para reintentar. El registro de lectura anterior no se reinicia.
Una actualización exclusivamente de precios no genera cambios ficticios del perfil.

## Cómo demostrarlo en la web

1. Iniciar con la cuenta de ensayo y pulsar la campana. Muestra el contador,
   los últimos tres avisos y accesos al centro y a preferencias.
2. Abrir Ver todos los avisos. Cada elemento incluye empresa, fecha, estado de
   lectura, perfil anterior/nuevo y enlace a su ficha. Ver balance y procedencia
   muestra las referencias utilizadas.
3. Marcar un aviso o Marcar todos como leídos. La campana y la página consultan
   el resultado del servidor y reflejan el nuevo contador. Repetir no duplica cambios.
   Marcar todos limita la operación a la lista cargada, conservando avisos posteriores.
4. Abrir Preferencias. Desactivar En la web guarda automáticamente con feedback.
   Recargar permite comprobar persistencia. Se puede volver a activar.
5. Push en Android y Correo de alertas están deshabilitados y explican el pendiente.
   La API tampoco acepta activarlos mientras no exista entrega real. El correo de
   comprobantes de suscripción ya configurado conserva su funcionamiento independiente.

La cuenta `sprint4_admin` dispone de un **aviso etiquetado de prueba**, creado solo
para comprobar la interfaz. Se marcó como leído durante la verificación visual.
No afirma que Microsoft haya cambiado realmente de perfil. No se modificaron
balances ni se enviaron correos o Push. Para demostrar el evento automático sin
inventar información real se ejecuta `test_notifications.py`, que cambia balances
ficticios exclusivamente dentro de la base de pruebas y comprueba todo el flujo.

## Archivos y contratos

- `notification_services.py`: línea base, detección y distribución de eventos.
- `notification_views.py`: lista privada, lectura y preferencias con versión.
- `models.py` y migración `0016_notifications`: preferencias, estado, evento y aviso.
- `import_company_metrics` e `import_market_data`: integración con la importación
  validada. Editar tablas mediante SQL crudo no equivale a usar estos flujos.
- `notification_worker`: cola persistida en PostgreSQL, sin Firebase ni broker nuevo.
- `notificationRepository`, `useNotificationsViewModel` y
  `useNotificationPreferencesViewModel`: Model y coordinación de estado.
- `components/notifications/`: campana y vistas; no consultan HTTP ni almacenamiento
  directamente. Se mantiene la separación MVVM y el aislamiento entre cuentas.

| Método y ruta bajo `/api/v1` | Entrada y comportamiento |
|---|---|
| GET `/notifications/?page=1&through_id=N` | Lista de diez, `count,next,previous,results,owner_id,unread_count,read_through`. El límite estabiliza páginas ante nuevos avisos. |
| POST `/notifications/mark-read/` | `{ids:[1,2]}` para individuales o `{all:true,through_id:N}` para masiva. Devuelve cantidad cambiada y contador. |
| GET `/user/notification-preferences/` | Tipo de aviso, canales elegidos, canales disponibles, explicación de pendientes y versión. |
| PUT `/user/notification-preferences/` | `{version,rating_changes,channels:{web,email,push}}`. Devuelve nueva versión. Conflicto de edición simultánea: 409. |

Todas las rutas requieren JWT. El propietario se obtiene del token; no se acepta
un ID de usuario para consultar o modificar otra cuenta. Aviso inexistente o
ajeno: 404. Entrada inválida o canal no disponible: 400. Las respuestas son privadas
y no se almacenan en caché pública. Android consumirá este contrato cuando Rosales
implemente su parte, manteniendo los mismos estados y reglas del servidor.

La campana consulta cada treinta segundos con la página visible y al volver a
enfocar la ventana. Al abrirla fuerza una actualización. No se declara entrega
instantánea ni se presenta esta consulta periódica como una notificación Push.

## Arranque y evidencia

`tools/Start-Sprint3-Demo.ps1` aplica migraciones e inicia también el procesador
de avisos, sin pasarle las credenciales SMTP. Su identificador queda registrado
fuera de Git junto a los otros servicios. `tools/Stop-Sprint3-Demo.ps1` comprueba
los procesos registrados antes de detenerlos y permite reiniciar sin dejar un
procesador anterior ejecutándose.

El procesador arranca antes de exportar la configuración SMTP temporal. Al limpiar
esa configuración se eliminan las variables que no existían, en lugar de dejar
cadenas vacías que impedían cargar el puerto de correo en un proceso posterior.

`prepare_notification_demo --username sprint4_admin` solo funciona en la base
aislada `sprint3_demo_validation` y con las cuentas de ensayo admitidas. Es
idempotente y distingue los eventos de ejemplo de los eventos reales. No se usa
para fabricar cambios financieros ni enviar avisos a otras personas.

- Servidor completo: 475 pruebas aprobadas y una optativa de Redis omitida.
- Web completa: 122 pruebas aprobadas en 26 archivos. Compilación correcta.
- Once pruebas nuevas de servidor cubren detección, duplicados, preferencias,
  cuenta/favorito inactivo, privacidad, fechas de favoritos, páginas estables,
  lectura, rollback, reintento y dos procesadores concurrentes.
- Seis pruebas de Web cubren lectura, contador, campana, autosave, fallo de red
  y cambio de cuenta. Las pruebas de búsqueda distinguen las peticiones de avisos
  de las búsquedas de empresas.
- Prueba visual de campana, centro, persistencia de preferencias y lectura masiva;
  contador de la cuenta de ensayo actualizado de uno a cero.

Pendientes: correo de alertas, Firebase/FCM, integración y recepción en Android,
monitoreo de la épica 15, balances nuevos y Android anterior de la épica 11.
