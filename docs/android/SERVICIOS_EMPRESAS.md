# Empresas, escenarios e IA local

## Datos cargados

El catálogo conserva 503 símbolos. Se incluyen balances anuales reales de 2020 a 2025 cuando el filing original está disponible y cinco años de cierres ajustados para AAPL, NVDA, JPM, JNJ, GOOGL, AMZN, META y TSLA. MSFT conserva sus cinco ejercicios y el histórico entregado con Script 1. No todas las empresas del catálogo tienen cobertura financiera.

El inicio activa por defecto «Solo empresas con datos para analizar». El filtro `ready=true` exige al menos un balance anual real y sesenta precios reales. Desactivarlo muestra también empresas sin cobertura. La disponibilidad de cada indicador depende de los componentes y de ejercicios comparables; por ejemplo, los balances bancarios no siempre desglosan activos corrientes.

Los inventarios y CSV de `docs/datos/empresas/` se importan al preparar el servidor. La carga es idempotente. Los snapshots completos descargados quedan en `backend/data-cache/`, excluidos de Git. Cada inventario conserva fuente SEC, filing y SHA-256; los cierres tienen su archivo de procedencia. La selección anual conserva un mismo 10-K original por ejercicio y deja nulos los datos ausentes o ambiguos.

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

`tools/verify_company_services.py` consulta pilares y escenarios por HTTP para las nueve empresas y solicita una explicación real de AAPL a la IA local. Usa una cuenta efímera, la elimina y guarda solo metadatos sin credenciales en `docs/android/evidence/company_services.json`.

La pantalla financiera conserva fuentes, fórmulas, componentes, referencia sectorial, ejercicios, serie de eficiencia, radar y puntaje. La consulta queda vinculada a la empresa elegida en la ficha. La metodología académica sigue pendiente de revisión del equipo; no se declara una aprobación que no ocurrió.
