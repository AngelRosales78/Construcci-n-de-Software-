# Administración de usuarios y perfil

Avance del 19 de noviembre de 2026. Rama `magallanes-sprint4`.
Este documento acredita web y servidor. La implementación Android del Sprint 4
corresponde a Rosales y no se declara terminada por estas pruebas.

## Historias y demostración

| Historia y tareas del backlog | Web y servidor implementados | Cómo comprobarlo |
|---|---|---|
| HU13_01 · T13.1.1–5 | Lista de usuarios con ID, nombre, correo, rol, plan y estado; páginas de 20, búsqueda y filtros. | Iniciar como administrador, abrir Administración y Administrar usuarios. Filtrar por nombre, plan o estado y cambiar de página. Una cuenta normal recibe 403 en la API. |
| HU13_02 · T13.2.1–5 | Cambio confirmado de rol o plan, validación de permisos, auditoría y protección del último administrador. | Elegir un rol o plan, revisar el diálogo y confirmar. Pro concede funciones de suscripción, sin convertir a nadie en administrador. El último administrador no puede degradarse. |
| HU13_03 · T13.3.1–5 | Bloqueo con motivo, desbloqueo y revocación de accesos y renovaciones anteriores. | Bloquear una cuenta de ensayo. Su acceso abierto y su nuevo login fallan. Desbloquearla permite un login nuevo; los tokens anteriores siguen revocados. |
| HU13_04 · T13.4.1–5 | Nombre, apellido y avatar privados; validación, vista previa, recorte circular y actualización en navegación. | Mi perfil, editar nombre, escoger PNG/JPEG/WebP menor de 2 MB y guardar. Recargar y comprobar persistencia. Una imagen inválida no modifica los nombres. |
| HU13_05 · T13.5.1–5 | Eliminación propia con confirmación y contraseña actual; borrado asociado y auditoría sin referencias personales. | Usar únicamente una cuenta desechable. Cancelar conserva los datos; contraseña incorrecta rechaza. Confirmar con contraseña correcta elimina la cuenta y sus datos. La prueba automatizada comprueba que las empresas del catálogo sobreviven. |

## Código principal

- `backend/api/user_management.py` contiene permisos, lista, cambios, bloqueo,
  avatar y eliminación. `models.py` y la migración `0013_user_administration`
  almacenan la versión de sesión, motivo, foto y registro administrativo.
- `backend/api/authentication.py`, `serializers.py` y el refresco de `views.py`
  rechazan sesiones revocadas. Tokens antiguos sin `sv` se interpretan como versión
  cero para conservar compatibilidad hasta que se revoque esa cuenta.
- `frontend/src/components/account/` contiene las vistas y el diálogo accesible.
  `useProfileViewModel` y `useAdminUsersViewModel` coordinan estado y validaciones;
  los repositorios encapsulan las peticiones. Se conserva MVVM.
- La imagen se normaliza a PNG de 256 × 256, se quitan metadatos y se guarda en
  PostgreSQL con URL privada. Así el borrado de la cuenta también elimina su foto
  dentro de la misma transacción. No se publican archivos personales en una carpeta.
- La auditoría registra actor, cuenta, acción y cambios de rol, plan y estado.
  Tras una eliminación se conservan acciones sin referencias al usuario eliminado.

## Contrato compartido para Android de Rosales

Base `/api/v1`; cabecera `Authorization: Bearer <access>`.

| Método y ruta | Entrada / salida |
|---|---|
| GET `/admin/users/?q=&plan=all&state=all&page=1` | `{count,next,previous,results}`; plan `all/free/pro`, estado `all/active/blocked`. Solo superusuario activo y staff. |
| PATCH `/admin/users/{id}/` | Uno o más de `role: admin/user`, `plan: free/pro`, `is_active: boolean`. Bloquear exige `reason` no vacío, hasta 240 caracteres. Devuelve el usuario actualizado. |
| GET/PATCH `/auth/profile/` | Nombres y campos existentes; nuevos `avatar_url` y `avatar_version` de solo lectura. Foto por multipart en `avatar`. `avatar: null` elimina la imagen desde clientes que admitan esa acción. |
| GET `/auth/profile/avatar/` | PNG de la cuenta autenticada, privado y sin caché. No acepta un ID ajeno para seleccionar una foto. |
| DELETE `/auth/account/` | JSON `{password: contraseña_actual}`, sin ID de usuario. Éxito 204. |

Errores esperados 400 por entrada inválida, 401 por sesión inválida, 403 por falta
de permiso o contraseña incorrecta, 404 por recurso ausente y 409 por intento de
eliminar o degradar al último administrador. Android debe regresar al acceso ante
401 y no conservar formularios privados al cambiar de cuenta.

## Verificación ejecutada

- Servidor completo sobre PostgreSQL de pruebas, 423 pruebas aprobadas y una
  optativa de Redis omitida por no activar su variable de entorno.
- Web completa, 109 pruebas aprobadas en 24 archivos. Compilación de producción
  correcta. Vite mantiene un aviso por el tamaño del paquete principal.
- Las 11 pruebas específicas de usuarios cubren permisos, paginación, independencia
  rol/plan, bloqueo, foto, eliminación y dos administradores concurrentes. Las seis
  pruebas de interfaz comprueban confirmaciones, multipart, tamaño y aislamiento
  de una respuesta tardía entre cuentas.
- Migraciones 0012 y 0013 aplicadas en `sprint3_demo_validation`. Verificación HTTP
  del listado administrativo y de las fichas MSFT, GOOGL y META. Evidencia de la
  ejecución y credenciales de demostración fuera de Git.

No se han implementado aquí las otras tres épicas del Sprint 4 ni se acredita
recepción Push o ejecución Android. Se mantienen en el cronograma.
