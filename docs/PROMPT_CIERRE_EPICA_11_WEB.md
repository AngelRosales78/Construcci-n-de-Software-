# Trabajo inmediato de web y servidor

Completar HU11_01 a HU11_05 del backlog corregido, reutilizando Django REST,
PostgreSQL, AccountPlan, autenticación y MVVM de React. Revisar la implementación
antes de modificarla y conservar los contratos del comparador usados por Android.

Implementar tabla Free/Pro servida por la API, tarifa y vigencia configurables,
checkout exclusivamente simulado con casos aprobado/rechazado, idempotencia por
usuario y operación, comprobantes privados imprimibles y envío de copia por correo,
historial dentro del perfil y cancelación de renovación que conserve Pro hasta vencer.
Registrar importes y fechas históricos sin modificar el rol del usuario. Rechazar
solicitudes sin sesión, datos de tarjeta y acceso a comprobantes de otra cuenta.

Separar pantalla, ViewModel y repositorio. Mostrar carga, error, reintento y vigencia.
Probar rechazo, concurrencia serializada, reenvío tras cancelación, expiración,
privacidad de comprobantes y errores de correo. Ejecutar pruebas existentes del
comparador y reportes, compilar web y verificar el recorrido por sus rutas normales.

Valores iniciales de demostración: PEN 9.90 por 30 días. Son configurables y cada
operación conserva una copia de su tarifa. La renovación se confirma manualmente
mediante otra simulación después del vencimiento. El correo depende del transporte
configurado: indicar salida local cuando corresponda y permitir reintentar pendientes.

Respetar archivos locales excluidos, historial y ramas del compañero. Informar por
historia qué se terminó, cómo demostrarlo y qué sigue pendiente. La implementación
Android y las épicas 12 a 15 requieren planificación posterior.

## Avance verificado el 17 de noviembre de 2026

| Historia | Evidencia en web y servidor |
|---|---|
| HU11_01 | Elegir Pro, aprobar/rechazar, transacción idempotente y activación sin modificar rol. |
| HU11_02 | Tabla obtenida de GET /api/v1/plans/, tarifa configurable y apertura del checkout. |
| HU11_03 | Recibo privado persistente, historial, impresión/guardar PDF y correo automático tras confirmar. |
| HU11_04 | Mi suscripción dentro del perfil, vigencia y operaciones anteriores. |
| HU11_05 | Confirmación de cancelación, conservación de Pro hasta vencer y acceso efectivo Free al expirar. |

Se ejecutaron 44 pruebas PostgreSQL de suscripciones, acceso, comparador y reportes,
100 pruebas web y compilación de producción. Se comprobó en la demo el recorrido
Free → Elegir Pro → Simular rechazo → historial y comprobante, conservando Free.
Los casos aprobados, concurrencia, vencimiento y correo se verificaron con pruebas
automatizadas. La demo registra los correos en consola; falta configurar SMTP y
verificar la entrega en una bandeja real para aceptar operativamente ese criterio.

Las asignaciones administrativas antiguas sin vencimiento obtienen un plazo de
30 días al confirmar la cancelación, visible antes de confirmar. El plazo usa la
configuración de duración vigente y se conserva en reintentos. Las operaciones
históricas guardan su tarifa y periodo propios. El comando
`python manage.py deliver_subscription_receipts` reintenta los correos pendientes.

Para presentar, abrir `/profile/subscription` desde Mi suscripción. Una operación
aprobada activa Pro por 30 días; la renovación posterior requiere confirmación
manual. El Sprint 4 sigue pendiente de planificación e implementación.
