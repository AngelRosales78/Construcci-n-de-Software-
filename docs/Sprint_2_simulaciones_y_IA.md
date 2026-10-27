# Simulaciones e IA local: continuación del Sprint 2

Base: día 3, commit 33d75e8 del 21 de octubre. Rama de continuación: magallanes-sprint2-simulaciones. El usuario autorizó completar servidor y web, implementar también el servicio de IA y preparar un proveedor local sin cuentas externas. No se modifica Android ni se acredita aceptación del equipo por esa autorización.

## Prompt aplicado

Continúa las tareas pendientes desde el avance auditado del día 3. Conserva MVVM, datos y contratos existentes. Implementa un único motor de simulación con histórico persistido, parámetros, semilla y versión; usa los percentiles 10, 50 y 90 de las mismas trayectorias. Conecta las pantallas y valida los límites. Implementa el análisis con un proveedor local real y valida estructura, referencias y cifras antes de publicarlo. Si falla, muestra análisis no disponible, sin respuestas ficticias de respaldo. Registra cada tarea realizada en un commit propio con fecha real y sin reescribir el historial. Comprueba regresiones y recorridos reales en Chrome, incluida recarga. Mantén explícitos Android, revisión académica y acuerdos del equipo pendientes.

## Modelo de simulación

Se consulta como máximo el histórico más reciente de 1261 cierres ajustados persistidos hasta la fecha actual. Se necesitan al menos 253 observaciones positivas, ordenadas y en una moneda. El horizonte comienza en el último cierre importado, no en una cotización en vivo.

Se calculan retornos logarítmicos diarios. Volatilidad anual: desviación muestral de retornos por raíz de 252. Tasa anual continua inicial: media de retornos por 252 más la mitad de la varianza anual. Se generan 2000 trayectorias GBM con Random(seed). Cada mes contiene 21 pasos de duración 1/252 de año. Los trece puntos incluyen origen y doce meses calendario; el extremo de mes se ajusta cuando el día no existe.

La evolución por paso es P siguiente = P anterior × exp((tasa - volatilidad²/2)/252 + volatilidad/raíz(252) × normal estándar). No modela dividendos futuros, comisiones, impuestos, cambios estructurales ni variación futura de la volatilidad. Son supuestos académicos, no garantías.

Los tres escenarios son cuantiles interpolados por fecha: pesimista P10, base P50 y optimista P90. No son máximos/mínimos ni necesariamente una trayectoria individual. Los controles aceptan tasa continua de -100 a 150 % y volatilidad de 0 a 200 %. Son límites de la aplicación; no rendimientos garantizados. Restablecer vuelve a los supuestos históricos y la misma semilla. Ocultar curvas solo modifica la presentación.

SimulationRun conserva usuario, empresa, histórico exacto y respuesta con semilla, parámetros, versión y SHA-256. Recuperar una ejecución exige el mismo usuario. La respuesta incluye fechas, moneda, fuentes y tabla de precios finales. Los resultados solo son reproducibles con la misma versión del motor y datos/configuración; los cambios de versión se registran.

## Contratos nuevos para ambos clientes

- POST /api/v1/companies/{ticker}/simulations/: JSON con seed opcional, annual_return y annual_volatility opcionales en fracciones, no porcentajes enteros. Devuelve status, simulation_id, parameters, defaults, scenarios, percentiles, final_prices, as_of, currency y source_sha256.
- GET /api/v1/simulations/{id}/: recupera la respuesta persistida del propietario sin recalcular.
- GET /api/v1/companies/{ticker}/analysis/?fiscal_year=2024&level=simple: análisis del ejercicio; level admite simple y technical. Con datos insuficientes devuelve unavailable. Con fallo de proveedor/validación devuelve 503 y no publica texto inválido.

Hay límites de frecuencia por usuario para las solicitudes costosas. Los datos públicos de empresas no incluyen contraseñas, tokens o mensajes personales en el contexto de IA.

## IA local y validación

Se descargan el binario CPU de llama.cpp desde sus releases oficiales y Qwen3 1.7B Q8_0 desde la organización oficial Qwen en Hugging Face. tools/prepare_local_ai.py exige los SHA-256 publicados y guarda el paquete, pesos y procedencia en ../local_ai, fuera del repositorio. tools/Start-LocalAI.ps1 inicia el proceso oculto limitado a 127.0.0.1:58110. No necesita cuenta, clave ni pago externo. El consumo local de memoria/CPU y la latencia sí existen.

Las cifras se calculan en Django antes de pedir texto. El contexto enumera métricas disponibles, unidades, periodo, versión y tramos académicos. El formato de generación limita la estructura y longitud; otro validador contrasta referencias y cifras con las métricas autorizadas, exige las cinco secciones y rechaza listas duplicadas, contenido sin respaldo, campos extra, texto de relleno y palabras o frases repetidas en bucle. Las referencias y fuentes mostradas provienen del servidor. La validación de estructura y cifras no certifica todas las interpretaciones semánticas del modelo.

El servidor envía al modelo los nombres de las métricas y sus tramos derivados, sin pedirle recalcular valores. Se solicita redactar explicaciones breves; los valores exactos aparecen en las referencias que añade el servidor. El validador independiente rechaza cifras sin respaldo, repeticiones y oraciones truncadas en cualquier respuesta recibida. Los límites de simulaciones (12/minuto) y análisis (4/minuto) son independientes por usuario.

La caché persistida se separa por empresa, métricas y periodo exactos, procedencia, versión de contrato, hash del modelo y nivel. Se reutilizan únicamente respuestas validadas recientes. Los fallos conservan el último resultado válido en la web, claramente identificado por nivel y periodo. Cambiar empresa o ejercicio elimina el resultado anterior. El servidor asigna las métricas de referencia a cada pregunta y valida otra vez el texto resultante; el modelo no decide qué referencias inventar.

## Iniciar y presentar

1. Preparar el proveedor con tools/prepare_local_ai.py; iniciar tools/Start-LocalAI.ps1 si el puerto 58110 no está ocupado por ese servicio.
2. Ejecutar tools/Start-Sprint2-Demo.ps1. Usa exclusivamente sprint2_completion_validation; importa el catálogo y las fuentes existentes. La web usa 55183 y la API 58010. No reutiliza el checkout antiguo Magallanes ni reemplaza una base personal.
3. Abrir la web en Chrome, iniciar sesión con la cuenta desechable local y abrir MSFT. No compartir el archivo de credenciales.
4. Mostrar los cinco pilares y su puntaje FY2024; abrir sus fórmulas y fuentes. El balance es histórico.
5. Generar escenarios. Mostrar P10/P50/P90, tabla, semilla y fecha de origen. Ocultar una curva no genera otra petición.
6. Poner tasa y volatilidad a cero: el precio simulado permanece constante. Restablecer y explicar que la misma semilla/histórico recupera las curvas.
7. Generar IA; mostrar resumen, cinco explicaciones, fortalezas/riesgos y sus métricas referenciadas. Abrir fuentes y validación. Cambiar a Técnico conserva periodo y cifras; la primera generación puede tardar.
8. Recargar y volver a generar la explicación: una respuesta validada vigente se recupera de caché. Abrir AAPL para demostrar la ausencia de histórico y componentes sin heredar información MSFT.
9. Mostrar los commits HU/T, archivos y pruebas. El historial acredita tareas reales; no reemplaza una validación conjunta con Android ni Planning Poker real.

Para abrir Chrome con la cuenta local de demostración, desde frontend ejecutar `node scripts/open_sprint2_demo.cjs`. Ese perfil está fuera de Git y no sustituye el perfil personal.

## Dónde mostrar el código

| Función que se demuestra | Archivo |
|---|---|
| Fórmulas, componentes y procedencia de los cinco pilares | backend/api/pillars.py y backend/api/pillar_views.py |
| Monte Carlo, calibración y percentiles | backend/api/simulations.py |
| Validación, autenticación y persistencia de simulaciones | backend/api/simulation_views.py y backend/api/models.py |
| Conexión con el modelo local real | backend/api/analysis_provider.py |
| Contraste independiente del texto con las métricas | backend/api/analysis_validation.py |
| Caché de análisis y respuestas disponibles/no disponibles | backend/api/analysis_views.py |
| Acceso web a los nuevos servicios (Model) | frontend/src/models/simulationRepository.js y analysisRepository.js |
| Estado, carga, errores y acciones (ViewModel) | frontend/src/viewmodels/useSimulationViewModel.js y useAnalysisViewModel.js |
| Gráficos, controles y explicaciones (View) | frontend/src/components/company/SimulationScenarios.jsx y FinancialAnalysis.jsx |

Los repositorios solicitan datos; los ViewModels controlan el estado y las acciones; las vistas presentan esos estados. Django calcula y almacena los resultados. El modelo de lenguaje explica las métricas recibidas; no calcula los pilares ni los escenarios.

## Referencias del proveedor

- https://github.com/ggml-org/llama.cpp/releases
- https://github.com/ggml-org/llama.cpp/tree/master/tools/server
- https://huggingface.co/Qwen/Qwen3-1.7B-GGUF

Los hashes y la versión realmente descargados quedan en ../local_ai/provenance.json. Los pesos y los ejecutables no se suben a Git.
