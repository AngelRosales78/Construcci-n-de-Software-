# Trabajo del lunes 12 de octubre de 2026

Se aplicó el prompt guardado en `Prompt_trabajo_12_octubre_2026.md`.

## Resultado

Autocompletado Redis precargado y navegación con teclado; historial de diez entradas por usuario con borrado y copia local; índices GIN y B-Tree PostgreSQL; clasificación financiera, ficha de cinco indicadores con valores cero y nulos correctos, fuente y fecha; tablas de balances y precios; gráfico Highcharts con 1M, 6M, 1Y, 5Y, crosshair, zoom y navegador.

Se eliminaron los precios aleatorios del endpoint. La importación CSV es explícita y atómica; valida fecha, fuente, moneda, duplicados y valores no finitos. Se aceptan identificadores numéricos o tickers en los endpoints financieros. Las rutas `/api/v1/search/` requeridas por el documento coexisten con las rutas `/companies/` ya usadas por la interfaz.

## Pruebas y rendimiento

129 pruebas de backend y 25 de interfaz aprobadas. Migración aplicada en PostgreSQL portátil 16.15 de validación; sin desajuste entre modelos y migración. Compilación del frontend aprobada.

La primera carga de búsqueda no cumplió 200 ms. Se redujo el payload, se habilitaron conexiones persistentes y se añadió caché Redis de 30 segundos, validada y separada por usuario y URL. Las cuatro mediciones se conservaron en `evidencias/`. Confirmación final: 1708 peticiones, promedio 32 ms, P95 63 ms y una respuesta de 652 ms. Sigue parcial: no se reinterpretó un P95 favorable como cumplimiento para todas las peticiones.

Extracción Redis: máximo 1,658 ms en 100 muestras. Gráfico Highcharts real: 1200 puntos en 405,13 ms, medidos desde el clic en 5Y hasta una serie visible en Edge. Datos sintéticos etiquetados como fixtures; no son precios de mercado.

## Operación e importación

Desde backend, con PostgreSQL y Redis del proyecto configurados:

```powershell
python manage.py migrate
python manage.py preload_search
python manage.py import_market_data --metrics balances.csv --prices precios.csv --source "Nombre y referencia de la fuente"
waitress-serve --listen=127.0.0.1:8000 --threads=8 config.wsgi:application
```

No se ejecutaron estos comandos contra la base del usuario. `--demo` identifica explícitamente datos de demostración. Columnas de balances: `ticker,as_of,market_cap,per,roe,beta,dividend_yield,working_capital,total_assets,retained_earnings,ebit,equity_value,total_liabilities,sales`; opcional `logo_url` HTTPS. Los indicadores ausentes permanecen nulos. ROE y dividend_yield se expresan en porcentaje. Columnas de precios: `ticker,date,adjusted_close,volume,currency`; fechas ISO YYYY-MM-DD, volume opcional y currency USD por defecto. No debe confundirse cierre ordinario con cierre ajustado.

Para pruebas funcionales rápidas sin PostgreSQL: `pytest api/test_company_search.py api/test_altman_z.py api/test_search_history.py api/test_financial_data.py api/test_search_cache.py --ds=config.test_settings --nomigrations`. Las pruebas específicas de PostgreSQL se omiten en ese modo; no acredita índices o FTS.

Pruebas reales: configurar una base PostgreSQL independiente y usar `--ds=config.settings` sin `--nomigrations`. Instalar dependencias de `requirements-test.txt` además de las de backend. `SPRINT_LIVE_REDIS_URL` debe señalar una base Redis exclusiva de validación.

Carga aislada: `locust -f locustfile.py SearchPerformanceUser --headless --users 10 --spawn-rate 2 --run-time 30s --host http://127.0.0.1:8000 --csv resultado`. Requiere token/credenciales de un usuario de prueba; `LOCUST_SEARCH_QUERIES` define consultas presentes en el catálogo. Nunca guardar tokens en un commit.

Gráfico: `node scripts/benchmark_chart.cjs` desde frontend con `BENCHMARK_ACCESS_FILE`, `BENCHMARK_BROWSER_EXECUTABLE`, `BENCHMARK_FRONTEND_URL`, `BENCHMARK_OUTPUT` y `BENCHMARK_SCREENSHOT`. El archivo de acceso temporal contiene access y user y debe mantenerse fuera del repositorio. El fixture del benchmark es T000 con más de mil precios; es exclusivo del entorno de validación.

## Martes 13: pendientes concretos

1. Integrar autenticación web/Android de Rosales, revisar diferencias de rutas y claves de sesión y ejecutar login → búsqueda → ficha → historial → logout. No se ha certificado ese recorrido completo.
2. Resolver o acordar con el profesor el incumplimiento aislado del límite estricto de búsqueda; repetir la carga en el entorno final. No dar por cerradas las dos tareas de latencia todavía.
3. Importar los balances, precios ajustados y logos de la entrega con fuente verificable. Los modelos y el importador están probados, pero no se importó un catálogo financiero real a la base del usuario.
4. Revisar la matriz de 45 tareas, pruebas y commits y preparar presentación. Los cambios se publican exclusivamente en Magallanes con fecha real y autor DiegoMagallanes11.

La compilación aún informa un bundle grande; la integración final debe contemplar esa carga. El martes no debe dedicarse a reemplazar las funciones ya implementadas.

## Referencias técnicas utilizadas

[Redis ZRANGEBYLEX](https://redis.io/docs/latest/commands/zrangebylex/), [Highcharts y React](https://www.highcharts.com/blog/integration/highcharts-wrapper-for-react-101/), [Waitress](https://docs.pylonsproject.org/projects/waitress/en/stable/runner.html), [PostgreSQL Windows](https://www.postgresql.org/download/windows/).

Confirmación de la interfaz final: 1200 puntos en 294,33 ms; evidencia en `evidencias/grafico_1200_puntos_final.json`. Se conserva también la primera medición de 405,13 ms.

Para repetir el benchmark, crear exclusivamente una base `sprint_validation` y ejecutar `python manage.py prepare_benchmark --confirm-test-database --access-file RUTA_FUERA_DEL_REPOSITORIO/benchmark_access.json`. El comando rechaza otros nombres de base y requiere confirmación explícita en su argumento. Los tres tests de esa protección también pasaron. El archivo de acceso debe mantenerse privado y fuera de Git.
