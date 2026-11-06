# Servicios activos de Script 3 - 06/11/2026

El servidor de Script 3 implementa las rutas que antes figuraban pendientes. El teléfono conserva la aplicación ya instalada y usa el mismo puerto 8000 mediante USB. No se modificó la Web ni el código de Script 2.

## Arranque local

Ejecutar `tools/Start-AndroidUsb.ps1` con Docker Desktop iniciado y el teléfono autorizado. El comando comprueba Redis y el servidor mediante `GET /api/v1/health/`, inicia el modelo Ollama previamente descargado cuando está disponible y restaura el túnel USB. El servidor activo usa `tools/run_android_backend.py` y `Script 3/android-local.sqlite3`. El comando no confunde la antigua API de Script 2 con la nueva: exige `android-services-v1` en health.

La primera preparación usa SQLite backup para conservar las cuentas y empresas de Script 2 sin copiar su archivo de secretos. El arranque reutiliza en memoria la clave de firma local y la configuración de IA de esa instalación, conservando las sesiones. Una base de Script 3 ya preparada no se sobrescribe al arrancar. La copia anterior a la activación se conservó localmente. Bases, claves, bibliotecas aisladas, logs y modelos permanecen excluidos de Git.

En este equipo se reutiliza Python de `Script 2/.venv` y las bibliotecas PDF locales de `Script 3/.server-packages`; `backend/requirements.txt` incluye las versiones requeridas para una instalación independiente. Si se prepara un entorno nuevo, instalar esas dependencias antes de iniciar el servidor.

## Funciones disponibles

| Función | Implementación real |
|---|---|
| Favoritos | Lista por cuenta, alta idempotente y baja por identificador; otras cuentas no pueden acceder. |
| Etiquetas | CRUD, normalización de nombres, colores, asignación múltiple y conflictos de versión HTTP 409. Borrar una etiqueta conserva los favoritos. |
| Recientes | Últimas diez empresas únicas, visitas con event_id y deduplicación. |
| Plan local | Pro académico para desarrollo en config.android_local, identificado como local-development. No implica compra ni suscripción pagada. En producción, sin suscripción explícita, el usuario es Free. Se comprueban vencimiento y permisos también en el servidor. |
| Comparador | Cinco pilares, componentes y notas del servidor. Solo compara notas con periodo y metodología equivalentes; si difieren, conserva los datos y anuncia que no son comparables. |
| Conclusión | Texto por reglas verificables, con fuentes y versión. No se etiqueta como IA. |
| IA individual | Ollama local qwen2.5:1.5b, explicaciones conceptuales y cifras adjuntas del balance real. Si el modelo falla se identifica expresamente el informe por reglas. |
| Escenarios | Simulación existente con histórico real, semilla reproducible y percentiles de la misma ejecución. |
| PDF | Archivo real con hash y tamaño, gráficos vectoriales, fuentes y snapshot del análisis solicitado. Comparación horizontal de hasta dos páginas. |
| Trabajos PDF | Solicitud persistida, idempotencia por cuenta/request_id, estado, cancelación y descarga autenticada. La primera consulta de estado reclama y genera el trabajo; otra consulta observa running. Un proceso interrumpido puede recuperar el trabajo tras vencer la reserva de dos minutos. |

Los análisis consultados se conservan como snapshots propios para que el PDF use exactamente el texto y origen solicitados. Un reporte individual con explicaciones de IA y resumen por reglas se identifica como mixed, sin atribuir todo el contenido al modelo.

## Verificación

27 pruebas funcionales nuevas y de regresión aprobadas. Cubren cuentas separadas, autenticación, duplicados, versiones obsoletas, acceso Pro/Free/vencido, comparación de periodos, snapshots, idempotencia, cancelación, integridad y permisos del PDF.

Se verificaron con datos reales del catálogo de 503 empresas: favoritos, etiquetas, visitas, comparador, análisis y PDF de MSFT y AAPL. Las escrituras de esta comprobación se revirtieron. Los reportes de prueba tienen dos páginas, menos de 10 KB, gráficos vectoriales y fuentes; sus cuatro páginas se renderizaron y revisaron visualmente. Estas mediciones locales no son una certificación de rendimiento para todos los casos.

Se confirmó desde el teléfono la respuesta HTTP 200 del health nuevo a través de USB. La prueba HTTP autenticada usa exclusivamente una cuenta temporal y la elimina al terminar; no modifica los favoritos del usuario. Los resultados se conservan en evidence/sprint3_services.json y evidence/sprint3_http_services.json.

Para repetir los casos aislados: `python backend/manage.py test api.test_sprint3_services --settings=config.test_settings --noinput`. Para verificar datos reales sin conservar escrituras: `python tools/verify_sprint3_services.py` (genera PDF de comprobación en la carpeta de evidencia; no son documentos personales).

El funcionamiento local requiere mantener computadora, Docker/Redis, servidor y cable activos. Después de desconectar o reiniciar, repetir el comando USB. Los indicadores sin datos suficientes siguen identificados como no disponibles; activar rutas no inventa información faltante. Queda la aceptación visual del usuario en cada pantalla del teléfono, incluida la confirmación de su huella.
