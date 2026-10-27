# Empresas, escenarios e IA local

## Datos cargados

El catálogo completo de **503 empresas** tiene fichas, balances anuales publicados y cierres ajustados cargados. El histórico llega hasta cinco años cuando la empresa tiene esa antigüedad; las cotizaciones de HONA y VYLR comienzan con su propia salida a bolsa. No se copia el histórico bursátil de la matriz para alargar artificialmente sus series.

La ficha incorpora capitalización y P/E reportados por la serie de valoración de Yahoo, ROE calculado con los balances comparables del mismo emisor, beta estimada frente al S&P 500 y rendimiento de dividendos registrado. Cada indicador conserva su propia fecha/fuente: no se presentan los datos anuales como si fueran valores en vivo.

La cobertura y los motivos de ausencia están en `docs/datos/empresas/coverage.json`. ROE no se interpreta con patrimonio no positivo; beta requiere al menos ciento ochenta y un cierres coincidentes; la rentabilidad por dividendos no se calcula si un split reciente impide comparar los pagos sin un ajuste adicional. Un dato no aplicable no se sustituye por cero.

El filtro inicial «Solo empresas con datos para analizar» incluye balances publicados y un mínimo de diez cierres reales. Para series de menos de sesenta cierres el servicio de escenarios declara explícitamente la historia limitada y Android muestra el aviso.

### Reorganizaciones y estados de negocios escindidos

- XOM conserva los estados históricos de Exxon Mobil Corporation, operadora del nuevo holding. Se identifica la entidad y la fecha original.
- SKYD conserva los años completos de Paramount Global anteriores a la combinación. Se indica que no representan el grupo sucesor ni se comparan automáticamente con los períodos posteriores.
- HONA usa exclusivamente las tablas auditadas del negocio Honeywell Aerospace del formulario de registro, excluyendo las columnas pro forma.
- VYLR usa exclusivamente las tablas auditadas del Seed Business, excluyendo los balances de Corteva completo y los ajustes pro forma.

Estas notas se conservan en cada balance y se muestran en la ficha, los pilares y el informe. Las fuentes archivadas SEC están enlazadas en los inventarios.

Los inventarios y CSV de `docs/datos/empresas/` se importan al preparar el servidor. La carga es idempotente. Los snapshots completos descargados quedan en `backend/data-cache/`, excluidos de Git. Cada inventario conserva fuente SEC, filing y SHA-256; los cierres tienen su archivo de procedencia. La selección general conserva un mismo 10-K original por ejercicio y deja nulos los datos ausentes o ambiguos; los cuatro casos corporativos documentados utilizan sus fuentes específicas.

Fuentes: [API oficial SEC](https://www.sec.gov/search-filings/edgar-application-programming-interfaces) y [históricos Yahoo Finance](https://finance.yahoo.com/).

Para descargar o actualizar otras empresas existentes en el catálogo:

```powershell
.\.venv\Scripts\python.exe backend/manage.py sync_company_data --tickers AAPL,NVDA,JPM --settings=config.android_local
# Añadir --refresh para volver a consultar las fuentes.
```

La descarga es explícita. Una fuente caída produce un error, no cifras sustitutas. Para uso continuado, configurar `SEC_USER_AGENT` con la identificación y contacto del responsable. La base no actualiza precios automáticamente ni los presenta como cotizaciones en vivo.

## Escenarios reales

- GET `companies/{ticker}/simulations/defaults/`: supuestos derivados de hasta setecientos cincuenta y seis cierres reales; media/desviación de rendimientos logarítmicos con anualización de doscientas cincuenta y dos sesiones.
- POST `companies/{ticker}/simulations/`: GBM, pasos mensuales, doce meses, semilla reproducible y límites de carga. Retorno y volatilidad se reciben como fracciones anuales.
- P10/P50/P90 pertenecen a la misma ejecución de Monte Carlo. El inicio corresponde al último cierre disponible, no necesariamente a la fecha de hoy. Los percentiles son interpolados sobre las trayectorias simuladas; ocultarlos en Android no vuelve a calcularlos.
- Sin datos reales suficientes devuelve un error explicativo. No es una predicción ni una garantía de rendimiento.

## IA local

Ollama se instaló en modo CPU dentro de `.local-ai/`, con el modelo Qwen2.5 1.5B. Los ejecutables y modelos están excluidos de Git. El ejecutable tiene firma válida de Ollama Inc.; el GGUF se verificó contra el SHA-256 del registro oficial. La conexión a nube está desactivada y el servicio escucha solo en `127.0.0.1:11434`.

En una ventana de PowerShell:

```powershell
.\tools\Start-LocalAI.ps1
```

En otra:

```powershell
.\tools\Start-AndroidServer.ps1 -Python .\.venv\Scripts\python.exe
```

El archivo `.env` local contiene:

```dotenv
LOCAL_AI_MODEL=qwen2.5:1.5b
LOCAL_AI_URL=http://127.0.0.1:11434/api/chat
```

No se requiere clave de pago. Un checkout en otro equipo debe instalar Ollama y el modelo; no se publican los binarios. Puede usar una instalación normal de Ollama pasando `-OllamaPath` al script, con el modelo disponible en la carpeta configurada.

POST `companies/{ticker}/analysis/` recibe empresa, ejercicio y nivel sencillo/técnico. El servidor construye los indicadores desde el balance elegido. El modelo genera explicaciones conceptuales separadas; las cifras, fuentes, fortalezas/riesgos y fórmulas se adjuntan desde el cálculo del servidor. No se envían cuentas, contraseñas ni tokens a la IA.

Se valida la estructura, el número de secciones y la ausencia de cifras nuevas en la parte generada. Esto no certifica la interpretación semántica: se requiere revisión del texto. La caché local separa empresa, ejercicio, nivel, modelo, versión de política y snapshot; dura treinta minutos y se pierde al reiniciar el servidor. Si falta el modelo, falla la conexión o se rechaza la respuesta, se muestra claramente `Informe automático · Sin IA` con las métricas reales. Nunca se declara IA una plantilla.

Referencias de integración: [Ollama en Windows](https://docs.ollama.com/windows), [salida estructurada](https://docs.ollama.com/capabilities/structured-outputs), [modelo](https://ollama.com/library/qwen2.5:1.5b).

## Verificación

`tools/verify_full_catalog.py` verifica las fichas, los pilares y los supuestos de escenarios de las 503 empresas por HTTP y ejecuta escenarios de AAPL, HONA y VYLR. `tools/verify_company_services.py` conserva además una comprobación real de IA local para AAPL. Usa una cuenta efímera, la elimina y guarda solo metadatos sin credenciales en `docs/android/evidence/company_services.json`.

La pantalla financiera conserva fuentes, fórmulas, componentes, referencia sectorial, ejercicios, serie de eficiencia, radar y puntaje. La consulta queda vinculada a la empresa elegida en la ficha. La metodología académica sigue pendiente de revisión del equipo; no se declara una aprobación que no ocurrió.


## Carga completa y restauración

```powershell
.\.venv\Scripts\python.exe backend/manage.py sync_all_companies --settings=config.android_local
.\.venv\Scripts\python.exe backend/manage.py import_corporate_history --settings=config.android_local
.\.venv\Scripts\python.exe backend/manage.py rebuild_company_snapshots --settings=config.android_local
.\.venv\Scripts\python.exe backend/manage.py update_company_coverage --settings=config.android_local
```

Las descargas usan hasta tres trabajadores y limitan el acceso a SEC. Se guardan snapshots para no repetir consultas y se registra cada fallo sin producir sustitutos. `--resume` omite las empresas ya preparadas. Las fichas exportadas incluyen los valores y su procedencia, por lo que `Start-AndroidServer.ps1 -Prepare` restaura todo el paquete sin consultar fuentes externas. Los datos son el snapshot cargado, no una actualización automática en segundo plano.
