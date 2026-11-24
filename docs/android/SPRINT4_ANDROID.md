# Sprint 4 en Android: estado y evidencia del 23/11/2026

Responsable Android: Angel Rosales. Rama local: `Rosales`, sin commit al escribir este documento. Contrato consumido: API publicada por Diego en `origin/magallanes-sprint4` (`cfa8bee`). No se copió su cliente Android (`16a6172`).

## Correspondencia con el plan

| Historia | Tareas | Dónde está | Comprobado |
|---|---|---|---|
| HU12_01 | S4-T01.1, T01.2 | `ui/ops/NotificationsScreens.kt` | Categoría anterior y nueva, fecha, enlace, giro, vacío, otra cuenta |
| HU12_02 | S4-T02.1, T02.2 | `data/push` | **Parcial.** Registro, renovación, baja y apertura probados sin Firebase. Falta recepción real |
| HU12_03 | S4-T03.1, T03.2 | Campana en Explorar y centro de avisos | Contador, orden, páginas sin repetir, error y regreso |
| HU12_04 | S4-T04.1, T04.2 | Preferencias de avisos | Guardado automático, conflicto, canal no disponible, cuentas distintas |
| HU12_05 | S4-T05.1, T05.2 | Centro de avisos | 12 → 11 → 0 confirmado por el servidor, doble toque, aviso ajeno |
| HU13_01 | S4-T06.1, T06.2 | `UsersScreen.kt` | Seis campos, búsqueda, filtros, páginas de 20, usuario normal rechazado |
| HU13_02 | S4-T07.1, T07.2 | `UsersScreen.kt` | Confirmación, rol y plan independientes, último administrador |
| HU13_03 | S4-T08.1, T08.2 | `UsersScreen.kt`, `SessionEvents.kt` | Motivo obligatorio, sesión abierta revocada, login rechazado, nuevo acceso |
| HU13_04 | S4-T09.1, T09.2 | `AccountScreens.kt` | Multipart, foto privada y archivo inválido contra la API. El selector de fotos no se recorrió en el emulador |
| HU13_05 | S4-T10.1, T10.2 | `AccountScreens.kt` | Contraseña incorrecta y cancelación en emulador; borrado real por API con cuenta desechable |
| HU14_01 | S4-T11.1, T11.2 | `CatalogScreens.kt` | Confirmación, doble acción, ocultación en búsqueda, ficha con aviso de inactiva |
| HU14_02 | S4-T12.1, T12.2 | Filtro Piloto | Cobertura completa, parcial o ausente; no equivale a cinco pilares |
| HU14_03 | S4-T13.1, T13.2 | Botón Precios | Consulta real, mismo identificador en reintentos, timeout, cuota y proveedor caído (simulados) |
| HU14_04 | S4-T14.1, T14.2 | Diálogo Editar | URL, sector y longitud; cambio parcial; ficha con descripción y web |
| HU14_05 | S4-T15.1, T15.2 | Añadir empresa | SHOP verificado y creado una vez, duplicado, inexistente |
| HU15_01 | S4-T16.1, T16.2 | `MonitoringScreen.kt`, IA | Cero llamadas, periodos, «No calculado», rechazo. No había llamadas reales de IA que mostrar |
| HU15_02 | S4-T17.1, T17.2 | Planes | Cantidades, porcentajes, total, Sin datos |
| HU15_03 | S4-T18.1, T18.2 | Cuotas | 84, 85 y 100 % como estados del servidor (pruebas JVM); límite y conflicto por API |
| HU15_04 | S4-T19.1, T19.2 | Estimación | Simulado, cero planes, meses. Cancelación vigente y cambio de tarifa no se reprodujeron |
| HU15_05 | S4-T20.1, T20.2 | Errores | Nivel, componente, páginas, detalle con claves permitidas |

## Pruebas

- JVM con el servidor compartido activo: 199 descubiertas, 196 aprobadas, 3 opcionales omitidas. Sin servidor, las 13 de `Sprint4LiveTest` también se omiten y no cuentan como aprobadas.
- `Sprint4LiveTest`: 13 recorridos contra la API real con PostgreSQL desechable. Uso: `S4_BASE_URL` y `S4_ACCESS` (archivo local con cuentas de ensayo).
- Servidor local de Script 1 (SQLite, copia de la base con 503 empresas): las mismas 13 pruebas reales, 13 aprobadas, con consultas reales de precios.
- Backend: 458 aprobadas, 5 omitidas, 0 fallos con `--ds=config.test_android_settings`. Sin `fakeredis`, las pruebas de caché se ejecutaron contra la base 15 del Redis local mediante un sustituto fuera del repositorio.
- Emulador API 37: recorrido manual con cuentas de ensayo y 7 de 7 pruebas instrumentales. No hubo teléfono físico conectado.
- Extremo a extremo: un cambio de datos guardado creó el aviso sin intervención y apareció en la app; un informe con IA local quedó contado (5 llamadas, con tokens).
- Capturas en `evidence/sprint4`.

## Pendiente

1. **Push real (HU12_02).** Hace falta `android/app/google-services.json` del proyecto Firebase (excluido de Git), la credencial del servidor y la recepción en primer plano, segundo plano y app cerrada.
2. **Perfil y avisos con datos reales.** Solo 61 de 503 empresas tienen Altman-Z: a las demás les falta algún componente, sobre todo EBIT. Las otras no generan avisos de cambio.
3. **Contrato de la épica 11.** Las pantallas de planes de esta rama usan `payments/*`; el servidor de Diego publica `plans/` y `user/subscription/*`. El comparador sugerido usa `compare/candidates/`, que tampoco está en su rama.
4. **HU15_05.** El criterio pide filtrar por fecha, pero la API publicada solo admite nivel y componente.
5. **Integración.** `16a6172` contiene otra versión móvil de las mismas historias. El equipo debe decidir cuál se integra.

## Servidor local

El backend de `Script 1` incorpora las rutas del Sprint 4 con el contrato de `origin/magallanes-sprint4` (`cfa8bee`). Los módulos de usuarios, catálogo, precios, avisos, Push y monitoreo proceden de esa rama, de Diego Magallanes, y se adaptaron a los modelos locales: `UserSubscription` e `Invoice` en lugar de `AccountPlan` y `SubscriptionTransaction`, Ollama como motor de IA y SQLite con transacciones inmediatas. No se cambió la Web ni su rama.

- Migración `0012_sprint4_administration`. Se aplica sola al iniciar el servidor; conserva cuentas, favoritos y comprobantes.
- Avisos: `run_android_backend.py` los procesa mientras está abierto. La primera ejecución guarda la línea base y no crea avisos retroactivos.
- Pro asignado por un administrador: sin importe, comprobante, vencimiento ni renovación.
- Eliminar una cuenta conserva sus comprobantes simulados sin usuario ni tarjeta.
- `fill_balance_market_value` completó `equity_value` en 438 empresas, solo con la capitalización cuya fecha es exactamente la del balance.

### Primer administrador

Se autoriza desde el equipo, con el servidor detenido o abierto, y después hay que iniciar sesión otra vez en la app:

```powershell
& '..\Script 2\.venv\Scripts\python.exe' toolsun_android_backend.py --grant-admin USUARIO
```

`--revoke-admin USUARIO` lo retira; siempre debe quedar al menos un administrador activo.
