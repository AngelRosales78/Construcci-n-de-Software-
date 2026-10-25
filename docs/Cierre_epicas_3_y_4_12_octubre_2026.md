# Cierre de las tareas de Diego

Lunes 12 de octubre de 2026, horario de Lima. Rama: Magallanes. Alcance: 45 tareas de las épicas 3 y 4, agrupadas en nueve HU. Las 50 tareas de Rosales no se incluyen en este cierre.

## Resultado y evidencia

**45/45 tareas implementadas y verificadas localmente.** Las tres tareas parciales de HU03_01 quedaron acreditadas: búsqueda optimizada, carga de latencia y presentación de logos. La tabla literal de cada tarea está en `Matriz_epicas_3_y_4_12_octubre_2026.md`; su demostración individual está en `Lista_45_tareas_demostracion_en_vivo.md`.

| Comprobación | Resultado | Evidencia reproducible |
|---|---|---|
| Backend con PostgreSQL y Redis reales de pruebas | 138 casos aprobados, incluida seguridad de restablecimiento, búsqueda, filtros, orden, historial, métricas, fórmula, precios e importación | Pytest: archivos `api/test_*.py` incluidos en la validación; ver comandos abajo |
| Interfaz | 28 casos aprobados y compilación Vite aprobada | `npm test`; `npm run build` |
| HU03_01 búsqueda | 10 usuarios durante 30 s; 2011 respuestas; cero fallos; máximo 142,446 ms; promedio 7,475 ms; P95 17 ms | `evidencias/locust_catalogo_real_12_oct.json`, CSV y `tools/run_search_performance.py` |
| Logos | 500 PNG locales con hashes; tres tickers alternativos para logos no disponibles | `datos/logos_procedencia.json`, `CompanyList.test.jsx` y recorrido en navegador |
| Integración web | Login por formulario, segunda página, filtro/orden, autocompletado por teclado, historial/borrado, ficha, cuatro rangos, renovación y logout; cero errores JS | `evidencias/integracion_real_12_oct.json`, `frontend/scripts/verify_delivery.cjs` |
| HU04_04 gráfico con datos reales | 1254 puntos; carga/renderización en 241,29 ms; presupuesto interno 2000 ms | El mismo informe de integración |

Se conservaron las mediciones de carga anteriores que fallaron. La carga final incluye las primeras peticiones; no elimina respuestas lentas ni sustituye el umbral por P95. La caché local es limitada a 500 entradas/5 s y se apoya en Redis/30 s; autentica y valida los parámetros antes de devolver cualquier resultado. Estas cifras acreditan el entorno y la carga medidos, no cualquier despliegue futuro.

## Datos y alcance del cierre

La presentación usa otra base, `sprint_delivery_validation`, con 503 símbolos, una ficha MSFT del balance anual 2024 y 1254 precios históricos importados. Los demás símbolos muestran datos ausentes; no hay cobertura financiera completa de todo el índice. Tres símbolos muestran ticker en vez de logo. Fuentes y metodología: `datos/README.md` y `datos/MSFT_procedencia.json`.

Se conservaron las pantallas y lógica de autenticación existentes. Los cambios de conexión agregan aliases `/auth/`, corrigen imports que impedían ejecutar registro/login, conectan el botón de búsqueda y hacen que la web use la ruta de renovación y almacene el refresh rotado. No se fusionaron ramas ni se reescribió Android.

## Comprobaciones para repetir

Desde el backend, con DB_HOST=127.0.0.1, DB_PORT=55432, DB_USER=postgres, DB_NAME=sprint_validation, REDIS_URL=redis://127.0.0.1:56379/0 y SPRINT_LIVE_REDIS_URL=redis://127.0.0.1:56379/1:

```text
python -m pytest api/test_company_search.py api/test_search_history.py api/test_search_cache.py api/test_financial_data.py api/test_altman_z.py api/test_password_reset_security.py api/test_live_services.py api/test_benchmark_guard.py api/test_delivery_integration.py --ds=config.settings -q
```

Desde frontend: `npm test` y `npm run build`. Desde el workspace de VS Code ejecutar **Iniciar demostracion de Diego** y **Mostrar evidencia en vivo de las tareas**. La cuenta es desechable, su contraseña se genera al iniciar y se guarda fuera de Git. El navegador inicia sesión por el formulario.

La carga se repite desde la raíz con `python tools/run_search_performance.py --credentials-file <archivo fuera de Git> --output <prefijo de informe nuevo>`. El recorrido web se repite desde frontend con PRESENTATION_CREDENTIALS_FILE y DELIVERY_REPORT definidos, ejecutando `node scripts/verify_delivery.cjs`. No sobrescribir evidencias anteriores al repetir cargas.

## Martes 13: ensayo y entrega

1. Diego muestra la lista de 45 tareas y acredita por HU la interfaz, API, base, código y pruebas. El workspace `Presentacion_Diego.code-workspace` ya contiene esas carpetas y tareas.
2. Rosales ejecuta su autenticación web y Android en emulador/dispositivo y aporta su lista de 50 tareas. Android todavía no queda certificado por esta validación web.
3. Ambos acuerdan la rama de integración y ensayan la API compartida. Merge/rebase y publicación en main/Rosales requieren aprobación; el trabajo de Diego permanece en Magallanes.
4. Ambos presentan los commits con fechas reales y revisan el material de entrega. Los timestamps UTC del informe caen el 13; en Lima corresponden al lunes 12 por la noche. No se cambiaron fechas de commits.

## Diseño solicitado al cierre

Se aplicó una interfaz pensada para inversores particulares y estudiantes: navegación común, inicio orientado a funciones reales, tarjetas accesibles por teclado, explicaciones de métricas y presentación clara de fuentes y fechas. Se revisaron seis capturas en 1440 px y 390 px, sin desbordamientos ni errores JS, y se verificó el arco visible del velocímetro (animación desactivada para evitar la carga en blanco). La capitalización usa las mismas unidades B/T en resultados y ficha. Los logos blancos reciben un fondo oscuro. Prompt: `Prompt_diseno_interfaz.md`. Evidencia: `evidencias/interfaz_validacion.json` y `evidencias/integracion_interfaz_final_12_oct.json`. Se conserva también el informe funcional anterior. Los saltos de línea de los CSV de Locust se normalizaron para Git; sus archivos originales se guardaron en `performance_raw` fuera del repositorio, sin alterar las mediciones.

El recorrido con el diseño final midió 240,36 ms para los 1254 puntos y comprobó el arco SVG visible del gauge; los cambios posteriores de contraste de logos se revisaron en las seis capturas finales.
