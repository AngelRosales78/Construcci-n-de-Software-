# Ejecutar el proyecto desde Script 1

La carpeta contiene Android, Django y la Web integrada hasta Sprint 3. La base activa es `android-local.sqlite3`. Se inicializó con una copia SQLite de Script 3 que conserva el catálogo, las cuentas y los favoritos. La base original de Script 1 quedó en `db.sqlite3` como respaldo. Las bases y los ajustes locales están excluidos de Git.

## Android

Abre `android` en Android Studio y selecciona JDK 17 para Gradle. Inicia Docker Desktop antes de arrancar el servidor.

**Teléfono por USB.** Conecta y autoriza el teléfono. Desde la raíz de Script 1, ejecuta en PowerShell:

```powershell
.\tools\Start-AndroidUsb.ps1
```

Ejecuta la configuración **app** en Android Studio. Después de desconectar el cable, repite el comando para restablecer el túnel. La API del teléfono está en `http://127.0.0.1:8000/api/v1/`.

**Emulador.** Inicia el servidor sin teléfono y selecciona la dirección del emulador antes de ejecutar la app:

```powershell
.\tools\Start-AndroidUsb.ps1 -ServerOnly
.\tools\Configure-AndroidDevice.ps1 -Device Emulator
```

En este equipo se reutilizan el entorno Python y el modelo Ollama preparados en Script 2. El servidor y la base activa se ejecutan en Script 1. Si el puerto 8000 responde desde otra copia del proyecto, detén esa instancia antes de iniciar Script 1. La ruta `/api/v1/health/` identifica la instancia activa en el campo `project`.

Para probar la comparación con los cinco pilares, usa **TXN y NUE**, ejercicio **2024**.

## Web

Con el servidor iniciado, abre otra consola en `frontend` y ejecuta:

```powershell
npm ci
npm run dev -- --host 127.0.0.1
```

Abre `http://127.0.0.1:5173`. La Web usa las pantallas existentes. Los recorridos de favoritos, comparador y exportación de Sprint 3 se prueban en Android.

Para inicializar otra copia sin base activa, ejecuta `tools/run_android_backend.py --prepare-only --data-source <ruta-a-la-base>` con el Python que tenga las dependencias de `backend/requirements.txt`. El arranque conserva una base ya inicializada.

## Cierre de Sprint 3 en Android (17/11/2026)

- **Épica 11 (pagos simulados).** Mi cuenta → Plan y suscripción muestra el plan actual, los días restantes y la tabla Free/Pro con «Más popular». «Actualizar a Pro» abre el checkout, que valida la tarjeta con Luhn y permite reintentar sin cobrar dos veces. Después del pago aparece el comprobante, que también se envía por correo y se consulta en «Ver comprobantes». La cancelación pide un motivo y conserva Pro hasta el final del periodo. Rutas: `payments/plans/`, `payments/simulate/`, `payments/invoices/`, `subscription/cancel/`. Tarjetas de prueba: `4242 4242 4242 4242` se aprueba y `4000 0000 0000 0002` se rechaza. El servidor no guarda el número de tarjeta, solo la marca y los últimos cuatro dígitos.
- **Cambio en el servidor local:** el Pro académico implícito queda desactivado por defecto, así que Pro se obtiene con el pago simulado. Para volver al comportamiento anterior, inicia el servidor con `ANDROID_LOCAL_PRO=true`.
- **HU01_04/HU02_05:** cerrar sesión desde Mi cuenta o desde «Salir» pide confirmación y ofrece «Cancelar».
- **HU04_01:** la ficha muestra el perfil financiero «Muy sólido / Moderado / Muy débil» con colores y una leyenda.
- **HU02_03:** el registro acepta un teléfono opcional. El servidor lo normaliza y no permite repetirlo entre cuentas (migración `0011`, que se aplica al iniciar el servidor).
- Se eliminaron `DashboardScreen` y `AssetDetailScreen`, que tenían precios escritos a mano y ya no se podían abrir desde la navegación.
- **Comparador con sugerencias:** al elegir una empresa, la app propone otras que cierran su ejercicio en las mismas fechas y ordena las propuestas según cuántos pilares tienen nota en ambas. También marca cada resultado de búsqueda con «x/5 pilares» o «No comparable». Ruta: `compare/candidates/?a=&fiscal_year=`, que requiere Pro. Si la empresa no tiene balance para ese año, muestra los ejercicios que sí tiene.
- **Importador SEC corregido:** ahora prueba etiquetas alternativas para efectivo, inversiones de corto plazo, cuentas por cobrar, ingresos y gasto por intereses, y guarda en cada dato la etiqueta que usó. Cuando la empresa no publica el pasivo total, lo calcula como pasivo+patrimonio menos patrimonio total. También se corrigió la etiqueta mal escrita `InterestExpenseNonOperating`, que debe ser `InterestExpenseNonoperating`. Para regenerar los datos sin internet: `python manage.py rebuild_annual_inventories --cache <carpeta con *-companyfacts.json>`. En FY2024, las empresas con los 5 pilares pasaron de 11 a 56. Se completaron 2.693 datos sin perder ni cambiar ningún valor existente. La copia de seguridad previa está en `android-local.before-annual-fix.sqlite3`.
- **Metodología `annual-ratios-v3`:** si el 10-K no publica ninguna partida de inversiones de corto plazo, la prueba ácida las toma como 0 y la app muestra el aviso «Supuesto». Si no publica ninguna partida de inventario, los días de inventario no aplican y se excluyen de la nota de Eficiencia. Cuando hay partidas ambiguas o desglosadas, el dato queda sin valor. En FY2024, las empresas con los 5 pilares pasaron de 56 a 186.

## Sprint 4 en el servidor local (23/11/2026)

El servidor de Script 1 ya ofrece las rutas que usa Android en las épicas 12 a 15. Detalle, pruebas, límites y cómo autorizar al primer administrador: [docs/android/SPRINT4_ANDROID.md](docs/android/SPRINT4_ANDROID.md). Copia previa de la base: `android-local.before-sprint4.sqlite3` (fuera de Git).
