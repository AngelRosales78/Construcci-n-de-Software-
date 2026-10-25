# Cierre funcional del sprint 1

Revisión del 16 de octubre de 2026 sobre la rama local `integracion/sprint1`. Complementa el informe de integración y actualiza sus pendientes. La calificación anterior no cambia. Los commits originales de Rosales y Magallanes se conservan; las correcciones nuevas se registran por separado y siguen sin publicarse.

## Resultado de las comprobaciones

| Área | Resultado | Evidencia y alcance |
|---|---|---|
| Backend | 182 pruebas aprobadas | PostgreSQL y Redis aislados. Incluye seguridad del enlace, complejidad de contraseña, rotación y revocación. |
| Web | 48 pruebas aprobadas | Tres pruebas del formulario de recuperación y nueve del ciclo de sesión, además de la regresión existente. |
| Compilación web | Aprobada | Persiste el aviso de tamaño del paquete, sin impedir la compilación. |
| Navegador | Seis recorridos aprobados sin errores JavaScript | Registro, dos pestañas, SMTP local y restablecimiento, contraseña anterior/nueva, enlace de un uso y renovación tardía durante logout. |
| Android | Conserva la validación anterior | Nueve pruebas unitarias, APK compilado y dos pruebas conectadas aprobadas en la integración anterior. No se modificó el código Android en este cierre. |
| Huella | El usuario informa que ya funcionaba | No se atribuye a esta revisión una nueva prueba en un teléfono ni un sensor físico. |

Los informes de esta revisión se guardan fuera del repositorio en `output/cierre_backend_final.xml` y `output/cierre_navegador.json`. No incluyen contraseñas ni tokens de prueba. El correo de prueba usa destinatarios ficticios y un receptor SMTP local; no se envía a personas.

## Recuperación de contraseña HU01_03

Se corrigieron dos diferencias entre interfaz y servidor: el correo utiliza `/reset-password/:uid/:token`, pero el formulario buscaba parámetros de consulta; además, el ViewModel enviaba un objeto a un servicio que recibe tres argumentos. Ahora el formulario extrae los parámetros de la ruta y envía `token`, `new_password` y `new_password_confirm` según el contrato real.

Se verificó el recorrido completo: solicitar desde el formulario, recibir el mensaje mediante el backend SMTP de Django, abrir su enlace, cambiar la contraseña, rechazar la anterior y autenticar con la nueva. Reutilizar el enlace se rechaza. Las pruebas del servidor verifican también la expiración de quince minutos y firmas inválidas.

No existe configuración de proveedor SMTP en el `.env` del checkout original revisado. Falta proporcionar host, usuario, credencial y remitente autorizados y comprobar la entrega a una bandeja real. El receptor local acredita protocolo, contenido y enlace; no certifica entregabilidad externa. La configuración predeterminada sigue siendo correo de consola para desarrollo.

## Cierre entre pestañas HU01_04 y HU01_05

Las rutas privadas observan la sesión compartida. Cerrar en una pestaña retira las pantallas privadas en la otra sin recargar. Volver atrás y abrir directamente una ficha sin sesión conducen al acceso. Cambiar de cuenta reinicia el estado privado del cliente.

Una renovación iniciada antes del logout ya no puede volver a guardar tokens después del cierre. Una respuesta de una cuenta anterior tampoco borra la sesión nueva. Las peticiones se reintentan como máximo una vez tras renovar; el login rechazado no intenta renovar otra sesión.

El servidor permite cerrar con un refresh anterior que otra pestaña ya rotó, después de verificar firma, expiración, tipo y propietario. Revoca también los tokens de la misma sesión emitidos por la rotación y rechaza su posterior renovación. Un access token no se acepta como refresh. La prueba de navegador retiene una respuesta real de renovación mientras otra pestaña cierra y comprueba tanto el cliente como el rechazo del servidor.

## Aclaración del registro Android HU02_03

El backlog corregido anterior indicaba volver al login y excluir autologin. Eso no describe la implementación integrada ni el recorrido que pasó en el emulador: el servidor devuelve tokens tras crear la cuenta y Android los conserva antes de abrir el panel, igual que la web.

Para documentar el comportamiento entregado se propone este criterio que sustituye aquella frase en futuras versiones del backlog: «Tras un registro válido, la aplicación guarda los tokens reales devueltos por el servidor y abre el panel; ante rechazo o datos inválidos, no concede una sesión nueva». Las validaciones y el rechazo de cuentas duplicadas continúan en el servidor. Esto aclara el comportamiento actual, sin afirmar que fue el criterio presentado o aceptado en la evaluación pasada.

## Cómo repetir las comprobaciones

Desde `backend`, con dependencias instaladas y PostgreSQL y Redis de prueba configurados, ejecutar `python -m pytest api`. La prueba `api/test_password_reset_smtp.py` abre su propio receptor SMTP en localhost y un puerto libre; no requiere un proveedor ni una cuenta externa.

Desde `frontend`, ejecutar `npm test` y `npm run build`. Para el recorrido completo, usar `scripts/verify_auth_closure.cjs` contra un backend aislado con SMTP local, indicando `CLOSURE_WEB_URL`, `CLOSURE_API_URL`, `CLOSURE_MAILBOX`, `CLOSURE_PYTHON` y opcionalmente `CLOSURE_REPORT`. El script crea cuentas ficticias, abre un navegador sin ventana y exige una API local. La URL web debe corresponder al enlace configurado en el backend. El receptor reutilizable de pruebas está en `backend/api/tests_smtp_support.py`; el puerto y la carpeta de mensajes se configuran fuera del repositorio. El emisor SMTP local no usa TLS ni credenciales y sirve solo para pruebas en localhost.

## Estado de cierre

Los recorridos funcionales comprobables localmente quedan corregidos y verificados. La huella se conserva según lo informado por el usuario y la validación Android previa. La entrega externa de correo queda pendiente de configuración; la aceptación académica y la conciliación del criterio de registro corresponden al equipo y al profesor. Las maquetas financieras Android y las funciones futuras de las otras épicas no se convierten en historias terminadas por este cierre.
