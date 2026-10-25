# Integración del sprint 1

Actualización posterior: [Cierre funcional del sprint 1](Cierre_funcional_sprint1.md) documenta la corrección del enlace y formulario de recuperación, SMTP local, cierre entre pestañas y renovación tardía. Sus resultados actuales son 182 pruebas backend y 48 web aprobadas; los números de abajo describen la validación inicial.

Esta rama reúne la web financiera de Magallanes (`99fd9b1`) y el Android reciente de Rosales (`fe85825`). Las ramas originales se conservaron. Al provenir de historiales independientes, la unión conserva ambos padres y selecciona explícitamente las fuentes; no incorpora bases de datos, cachés o archivos generados de compilación.

## Correcciones

- El servidor aplica complejidad de contraseña y utiliza BCrypt-SHA256 para nuevas claves. Los hashes PBKDF2 anteriores siguen siendo válidos y se actualizan al autenticarse.
- El login acepta usuario o correo y diferencia el rol de administración del tipo de inversor. La web dirige al administrador a un resumen protegido por permisos del servidor.
- La sesión tiene un identificador que se conserva al rotar tokens. Logout comprueba propiedad, revoca refresh y bloquea los access tokens de esa sesión, incluso los emitidos antes de la rotación.
- Android envía Bearer en recursos privados, valida la sesión al reabrir, renueva tokens vencidos y conserva el refresh rotado. No concede acceso cuando no puede verificarla.
- Registro móvil conserva los tokens devueltos antes de abrir el panel. El login visual llama al servidor; una contraseña inválida no permite navegar al panel.
- Huella se activa desde Perfil después de un acceso válido. Cancelar o fallar no permite entrar. Tras reconocer la huella se vuelve a validar la sesión real con el servidor. Salir borra tokens y desactiva la huella para esa cuenta local.
- No se guardan contraseñas para autologin ni se registran cuerpos de autenticación en los logs de red. Se eliminó el helper de credenciales que no se utilizaba.
- Se conserva la opción SQLite para desarrollo, con migraciones compatibles. PostgreSQL sigue siendo la opción principal para búsqueda e índices y para las mediciones de rendimiento.
- Se añadió el wrapper de Gradle 8.4 y se eliminó la ruta absoluta a un JBR particular. El proyecto usa Java 17.

## Puesta en marcha

Backend, desde `backend`, con Python 3.12 y un entorno virtual:

```powershell
python -m pip install -r requirements.txt
Copy-Item .env.example .env
```

Editar `.env` con los datos locales de PostgreSQL y Redis antes de continuar. Para desarrollo portátil se puede elegir `USE_POSTGRES=False`; se conserva una base SQLite local ignorada por Git. Las pruebas de rendimiento de búsqueda corresponden a PostgreSQL, no a SQLite.

```powershell
python manage.py migrate
python manage.py runserver 127.0.0.1:8000
```

Frontend, desde `frontend`:

```powershell
npm ci
npm run dev
```

Android: abrir la carpeta `android` en Android Studio y seleccionar Java 17 como Gradle JDK. La URL predeterminada `http://10.0.2.2:8000/api/v1/` corresponde al backend de este equipo desde el emulador.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Para dispositivo físico, configurar una URL accesible desde el teléfono en `NetworkModule.kt` y usar el backend de desarrollo autorizado por el equipo. Las credenciales no se comparten en el código ni en capturas. La activación de la huella requiere un sensor compatible y una huella registrada en el sistema.

## Datos y alcance

La web conserva la búsqueda, filtros, historial y ficha financiera con sus evidencias anteriores. La base de demostración revisada tiene 503 empresas, un balance de Microsoft y 1254 precios; esta integración no amplía automáticamente esa cobertura ni modifica esa base.

Las pantallas financieras que Rosales añadió en Android se conservan como maqueta y se identifican con datos de ejemplo. Su conexión financiera no se declara implementada ni se contabiliza de nuevo como autenticación. Las funciones comprometidas del sprint 1 para Android son registro, login, huella, persistencia y logout.

La recuperación de contraseña dispone de endpoints y pantalla móvil para solicitar el enlace; el enlace conduce a la pantalla web existente de restablecimiento. Por defecto se usa correo de consola para desarrollo. Para entregar correos reales se debe configurar un proveedor SMTP en `.env`; las pruebas utilizan correo en memoria y no certifican un proveedor externo.

## Verificación

- Backend: `python -m pytest api`, con una configuración de pruebas aislada y PostgreSQL y Redis de validación. El entorno de revisión y sus resultados se conservan fuera del repositorio para no incluir configuraciones del equipo ni bases de datos.
- Web: `npm test` y `npm run build`.
- Android: pruebas unitarias de login, registro, renovación, sesión revocada, reapertura sin red y logout sin conexión. Las pruebas conectadas usan un emulador de solo lectura y un backend de prueba separado.
- La prueba antigua que esperaba treinta precios sin insertar ninguno ahora verifica una serie vacía. Las pruebas específicas de series importadas siguen verificando datos reales y casos conocidos.

Este trabajo no modifica la calificación del sprint 1 ni constituye una nueva planificación del sprint 2. Permite partir de una versión conjunta comprobable y separar los pendientes de configuración y cobertura del trabajo nuevo.

### Resultados de esta integración

- Backend: 179 pruebas aprobadas con PostgreSQL y Redis aislados.
- Web: 36 pruebas aprobadas y compilación de producción correcta.
- Android: 9 pruebas unitarias aprobadas y APK compilado con Java 17.
- Emulador: 2 pruebas conectadas aprobadas contra el backend aislado: registro, reapertura y logout; contraseña incorrecta rechazada. El acceso por huella sin activación previa no abre el panel. La lectura de una huella física y el envío por SMTP externo quedan pendientes de comprobar con el equipo/proveedor correspondiente.

El historial separa la unión de ramas, la protección de contraseñas, las sesiones del servidor, la configuración, el acceso de administradores en web, las correcciones móviles, la compilación y las evidencias de prueba. Los commits se registraron durante esta revisión; no representan fechas anteriores ni atribuyen este trabajo a los integrantes.
