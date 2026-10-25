# Avance del domingo 11 de octubre de 2026

Rama: `Magallanes`. Autor: DiegoMagallanes11 <magallaneslda@gmail.com>.
Alcance asignado: épica 3 (25 tareas) y épica 4 (20 tareas), 45 en total.
Se avanzaron 12 tareas del bloque de hoy: 9 quedaron verificadas funcionalmente y 3 continuaron parciales.
Este balance corresponde al bloque de hoy; no convierte automáticamente las otras 33 tareas en faltantes.

| HU | Tarea del documento | Avance de hoy | Estado del bloque |
|---|---|---|---|
| HU03_01 | FE: búsqueda React con espera de 250 ms | Consulta al escribir, paginación y descarte de respuestas antiguas | Verificada |
| HU03_01 | BE: búsqueda PostgreSQL inferior a 200 ms | FTS con configuración simple, ticker exacto, validación y eliminación del límite de 50 | Parcial: faltaron PostgreSQL real y medición |
| HU03_01 | T: concurrencia con Locust inferior a 200 ms | Escenario con autenticación explícita, contrato paginado y umbral de latencia | Parcial: no se ejecutó la carga |
| HU03_03 | FE: selector de sectores | Se eliminaron peticiones repetidas y se permitió filtrar sin texto | Verificada |
| HU03_03 | BE: filtrado por sector | Se validó el sector y se filtró antes de paginar | Verificada |
| HU03_03 | T: exactitud del filtrado | Casos de sectores válidos, inválidos y segunda página | Verificada |
| HU03_04 | FE: selector de orden | Se conservó el sector y se reinició la página al ordenar | Verificada |
| HU03_04 | BE: orden por capitalización | Orden numérico ascendente/descendente con desempate por ticker | Verificada |
| HU03_04 | T: orden numérico | Valores 9, 20 y 100 y empates; paginación de 65 empresas | Verificada |
| HU03_05 | FE: historial local y sincronizado | Se corrigió la lectura paginada y la selección; se actualizó tras guardar | Parcial: faltaron almacenamiento local, borrado y límite de 10 |
| HU04_03 | BE: cálculo Altman-Z | Cálculo decimal, 1,81 inclusive en zona gris y datos inválidos sin puntaje | Verificada |
| HU04_03 | T: pruebas matemáticas | Fórmula de referencia, límites, pérdidas, divisores inválidos y respuesta nula | Verificada |

## Verificación

- Backend: 15 pruebas de búsqueda/filtros y 51 de Altman-Z aprobadas; 3 FTS omitidas por requerir PostgreSQL.
- Seguridad del sábado: 24 regresiones aprobadas antes de los cambios de cálculo.
- Interfaz: 17 pruebas aprobadas (8 de búsqueda/filtros/historial y 9 del indicador).
- Compilación del frontend: se corrigieron referencias inexistentes a servicios, esquemas y App.css. Se mantuvo la lógica de autenticación existente.
- No se ejecutó una prueba integral de login ni se certificó rendimiento con PostgreSQL/Redis.

Para repetir las pruebas funcionales desde `backend`:

```powershell
python -m pytest api/test_company_search.py api/test_altman_z.py api/test_password_reset_security.py --ds=config.test_settings --nomigrations -q -p no:cacheprovider
```

Desde `frontend`:

```powershell
npm ci
npm test
npm run build
```

Para ejecutar FTS se debe usar una instancia PostgreSQL de pruebas y `--ds=config.settings`; no usar la base de datos del proyecto para experimentar.
El escenario Locust requiere `LOCUST_ACCESS_TOKEN` o `LOCUST_USERNAME` y `LOCUST_PASSWORD`; la ruta de login puede configurarse con `LOCUST_LOGIN_PATH`.

## Trabajo siguiente antes del martes 13

Lunes 12: índices y verificación PostgreSQL, autocompletado Redis, historial de 10 elementos con borrado y sincronización local; clasificación financiera, métricas faltantes y precios persistidos con fuente documentada. Se deben priorizar las tareas obligatorias que aún no tengan implementación.

Martes 13: integración con las épicas 1 y 2 de Rosales, corrección de incompatibilidades de rutas y sesión, pruebas completas y preparación de entrega. La autenticación de esta rama conserva rutas frontend `/auth/` que no coinciden con las rutas actuales del backend y el login contiene una referencia a `models.Q` sin importar `models`; deben resolverse con la implementación del compañero al integrar. No se considera la aplicación lista para entrega por haber compilado.

Los commits y los pushes conservan sus horas reales del domingo. No se alteraron las fechas del sábado ni se modificaron los archivos locales del checkout original.
