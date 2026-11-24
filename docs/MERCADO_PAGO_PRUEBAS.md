# Pago de prueba con Mercado Pago

## Estado actual

La conexión está implementada en el código y verificada con pruebas controladas. **Todavía no está vinculada a una cuenta de Mercado Pago ni se ha ejecutado un pago en su plataforma.** No hay credenciales externas cargadas.

La página Mi suscripción muestra ese estado y no permite activar Pro pulsando una aprobación ficticia. Los planes, las cuentas y los comprobantes anteriores se conservan.

## Para Diego: el siguiente paso

1. Crear una cuenta propia en [Mercado Pago Perú](https://www.mercadopago.com.pe/). Esa cuenta es distinta de la cuenta de nuestra web.
2. Entrar en [Tus integraciones](https://www.mercadopago.com.pe/developers/panel/app) y crear una aplicación para Checkout Pro en web.
3. Preparar las cuentas de vendedor y comprador de prueba, siguiendo la [guía oficial](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/test-accounts). No usar tarjetas reales.
4. Conectar una dirección HTTPS pública para las notificaciones. El servidor actual solo está en 127.0.0.1; Mercado Pago no puede acceder a esa dirección desde internet.
5. Cargar localmente la configuración y hacer una compra de prueba aprobada y otra rechazada. Las claves no deben enviarse por el chat ni subirse a GitHub.

Crear la cuenta y resolver sus verificaciones corresponde al titular. No se han creado cuentas, publicado el servidor ni abierto un túnel externo automáticamente.

## Qué verá el usuario cuando esté conectado

Elegir Pro → Preparar pago de prueba → Continuar en Mercado Pago → completar el formulario alojado por Mercado Pago → volver a Mi suscripción → verificar estado.

Un pago pendiente mantiene Free. El servidor verifica la cuenta vendedora de prueba, la moneda, el importe, la referencia de la operación, el estado de las transacciones y la notificación firmada. Solo una aprobación acreditada y notificada concede Pro.

El retorno del navegador nunca acredita por sí solo un pago. No existe renovación con cargo automático: este flujo concede el período acordado y requiere otro pago al vencer.

## Configuración local

Se mantiene la tarifa ya existente: PEN 9,90 por 30 días, tomada del servidor. Cada intento conserva su importe y duración originales aunque la tarifa cambie después.

El script tools/Configure-MercadoPago-Test.ps1 solicita los datos cuando las cuentas y la dirección HTTPS ya estén preparadas:

- ID del vendedor de prueba de Perú.
- Correo del comprador de prueba, terminado en @testuser.com.
- Origen HTTPS público que sirva las rutas de retorno y notificación.
- Access Token de la aplicación del vendedor de prueba.
- Clave secreta de Webhooks.

Las claves se introducen con entrada oculta. Antes de guardar, el comando verifica por API que el vendedor tiene la etiqueta test_user y el sitio MPE; no crea un pago. En Orders API, las credenciales del usuario de prueba pueden tener prefijo APP_USR: no se decide el entorno únicamente por el prefijo del token.

El archivo cifrado se guarda fuera del repositorio, en ../presentation_runtime_sprint3/mercadopago-test.xml. Windows lo vincula al usuario y equipo que lo creó. Load-MercadoPago-Test.ps1 carga las variables al iniciar el servidor, sin imprimir las claves.

Después de guardar, ejecutar tools/Restart-Platform-Api.ps1 para reiniciar únicamente la API local validada. La web y sus cuentas se conservan. Si el servidor todavía no está iniciado, usar Iniciar Plataforma.cmd.

Como alternativa de despliegue, las mismas variables pueden suministrarse en el entorno del servidor. No se habilitan cobros reales con esta implementación:

```text
PAYMENT_PROVIDER=mercadopago_test
MERCADOPAGO_ACCESS_TOKEN=
MERCADOPAGO_WEBHOOK_SECRET=
MERCADOPAGO_TEST_SELLER_ID=
MERCADOPAGO_TEST_BUYER_EMAIL=
MERCADOPAGO_RETURN_URL=https://TU_ORIGEN/api/v1/payments/mercadopago/return/
MERCADOPAGO_WEBHOOK_URL=https://TU_ORIGEN/api/v1/payments/mercadopago/webhook/
```

El endpoint HTTPS de Webhooks debe configurarse en la aplicación de Mercado Pago para eventos de Orders. El código admite el tipo order/orders_v2 y exige live_mode=false. La firma usa el identificador exactamente como fue recibido, el identificador de solicitud y el timestamp.

La ruta pública de retorno solo redirige a la dirección fija del frontend local; ignora cualquier aprobación o URL de destino enviada en los parámetros. No publica el frontend completo ni consulta un pedido de otra cuenta.

## Contrato y conservación

- POST /api/v1/user/subscription/checkout/ y POST /api/v1/user/subscription/payment-orders/: únicamente request_id. El navegador no elige precio, moneda, duración, vendedor ni resultado.
- GET /api/v1/user/subscription/payment-orders/{id}/: estado guardado, privado por propietario.
- POST sobre esa misma ruta: vuelve a consultar al proveedor. Solo concede acceso después de recibir una notificación firmada.
- POST /api/v1/payments/mercadopago/webhook/: verifica la firma y consulta la orden real antes de conciliarla.
- GET /api/v1/payments/mercadopago/return/: retorno de navegación, sin activación de acceso.
- Historial de suscripciones: conserva recibos anteriores e incorpora las últimas veinte operaciones del propietario.

La migración 0027 añade PaymentOrder y el origen del acceso de AccountPlan. El proveedor recibe un UUID estable como clave de idempotencia y referencia interna. Un reintento de red conserva el mismo cuerpo. Solo puede quedar una operación abierta por usuario; confirmaciones simultáneas no duplican el comprobante ni amplían el período dos veces.

La aplicación no recopila números de tarjeta ni guarda respuestas completas del proveedor. No imprime credenciales ni cuerpos de errores externos. Un reembolso o contracargo observado suspende únicamente el acceso originado por esa operación; no elimina un acceso administrativo distinto. No se implementa una pantalla para solicitar reembolsos al proveedor.

La configuración legacy_demo queda para conservar pruebas y escenarios históricos; la configuración normal es mercadopago_test y no acepta outcome=approved desde clientes antiguos. Android no se modificó: Angel deberá adaptar su pantalla de pago si quiere iniciar estos checkouts desde móvil. No se cambió la lectura del plan existente.

## Verificación ejecutada

- 570 pruebas de servidor aprobadas y una optativa omitida.
- 143 pruebas de web aprobadas y compilación de producción correcta.
- Casos nuevos: configuración ausente, vendedor real rechazado, importe o moneda incorrectos, firma falsa, retorno manipulado, rechazo, pendiente, aprobación, reintento tras pérdida de respuesta, privacidad, reembolso y confirmaciones concurrentes.
- Navegador con API local real: configuración pendiente y ausencia de activación directa, en escritorio y teléfono.
- Navegador con respuestas controladas: formulario externo preparado, pendiente conserva Free y aprobación confirmada muestra Pro. No se abrió un pago externo.
- Los tres scripts PowerShell se analizaron sintácticamente; Restart-Platform-Api.ps1 se ejecutó contra el servidor local. El configurador no se ejecutó interactivamente porque faltan las cuentas y credenciales.
- Esquema Django y comprobaciones del servidor sin problemas.

La compra de prueba real, la entrega de Webhooks desde Mercado Pago y la verificación con las credenciales del titular **siguen pendientes**. Las evidencias locales están fuera de Git en ../output/mercadopago/.

## Fuentes técnicas

Integración basada en [crear una order Checkout Pro](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/create-order), [URLs de retorno](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/web-integration/configure-back-urls), [estados de la order](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/payment-management/status/order-status), [notificaciones](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/notifications) y [validación de firma del SDK oficial](https://mercadopago.github.io/sdk-php/docs/webhooks.html).
