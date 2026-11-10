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
