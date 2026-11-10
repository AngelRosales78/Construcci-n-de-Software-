# Demostración del Sprint 3 — guía breve

Diego presenta la web y el servidor. Angel presenta Android y participa en la prueba cruzada. El alcance es de 15 HU / 60 tareas; cada integrante tiene 30. La matriz permite abrir el archivo de cada tarea sin afirmar que cada tarea necesita un commit diferente.

## Preparación

- Abrir `http://127.0.0.1:55193/login`. Usar las cuentas locales `sprint3_free` y `sprint3_pro`; consultar sus contraseñas en `presentation_runtime_sprint3/demo_credentials.json`, fuera del repositorio y sin proyectar el archivo.
- Mantener activos API 58020, PostgreSQL 55432, Redis 56379, procesador de reportes y motor Qwen local 58110. El arranque está en `tools/Start-Sprint3-Demo.ps1` y `tools/Start-LocalAI.ps1`. Si los servicios ya están activos, abrir la dirección; los scripts no reemplazan procesos que ocupen sus puertos.
- Elegir Microsoft FY2024 para una ficha con resumen cargado. Elegir Alphabet/Meta FY2024 para la comparación con fechas coincidentes. Tener abierta la matriz y la carpeta de descargas.
- Angel configura la aplicación con `apiBaseUrl=http://10.0.2.2:58020/api/v1/` en el emulador y usa la misma cuenta en la prueba de favoritos.

## Recorrido de seis minutos

| Tiempo | Quién | Acción y explicación | Historias |
|---|---|---|---|
| 0:00–0:35 | Diego | Explicar: «La aplicación permite consultar y comparar información financiera histórica del S&P 500. Separa lo que se conoce de lo que falta; no recomienda comprar acciones». Con Free, abrir Comparar y mostrar la vista previa. Pro se asigna por administración; no hay cobros en este sprint. | HU08_05 |
| 0:35–1:25 | Diego | Con Pro, abrir Microsoft, guardar un favorito, abrir otra empresa y volver a Inicio. Mostrar Vistos recientemente: máximo diez empresas distintas; una revisita mueve la empresa al inicio. | HU10_01, HU10_04 |
| 1:25–2:15 | Diego | En Favoritos, crear dos etiquetas, asignar la misma empresa a ambas y filtrar. Renombrar una etiqueta. Borrarla y comprobar que la empresa sigue guardada. Pedir quitar el favorito, cancelar y volver a confirmar. | HU10_02, HU10_05 |
| 2:15–2:40 | Ambos | Mantener Favoritos visible en ambos clientes, añadir desde Android y observar la web; quitar desde web y observar Android. Explicar que ambos guardan en la misma API y consultan cambios cada dos segundos cuando la lista está activa. | HU10_03 |
| 2:40–3:45 | Diego | Seleccionar GOOGL y META, FY2024. Intercambiar A/B y explicar el signo de A menos B. Hay cinco filas: tres notas comparables y dos ausentes. Con MSFT/GOOGL FY2024 mostrar que el mismo número de año no implica las mismas fechas de cierre. | HU08_01, HU08_02 |
| 3:45–4:20 | Diego | Consultar rendimiento 1Y; cambiar a 6M o 5Y. Ambas series comienzan en 0 % en la primera fecha común; no se rellenan huecos. El deslizador permite leer una fecha sin depender del ratón. | HU08_03 |
| 4:20–4:50 | Diego | Preparar la conclusión comparativa. Mostrar origen, fecha, fortalezas y fuentes. «Qwen local elige un resumen entre conclusiones verificadas; las cifras y fortalezas salen de reglas del servidor. Si el motor falla, se identifica el texto por reglas». | HU08_04 |
| 4:50–5:35 | Diego | Marcar gráficos y análisis preparado, solicitar PDF y observar cola/generación/listo. Descargar y abrir: comparación horizontal de hasta dos páginas, fuentes, fecha y trazos vectoriales. Explicar que se exporta la versión ya preparada; no se genera otro análisis a escondidas. La ficha individual tiene su propio PDF desde Microsoft. | HU09_01, HU09_02, HU09_03, HU09_04, HU09_05 |
| 5:35–6:00 | Diego | Abrir la matriz y un archivo representativo. Mostrar los commits de cierre: «Se agruparon cambios relacionados en cinco commits; los códigos de las tareas están en sus descripciones y las pruebas acreditan el comportamiento». | Trazabilidad |

Angel amplía después la demostración móvil: selección, gráficos, lector PDF, guardado/cancelación y etiquetas. Diego no necesita explicar la implementación de cada pantalla Android para defender su trabajo web/servidor.

## Respuestas que conviene anticipar

- **¿De dónde vienen los datos?** Los estados anuales se importaron desde inventarios derivados de SEC Company Facts, con etiquetas XBRL, fechas, accesiones y hashes. Los precios son CSV derivados de Yahoo Finance. Los snapshots de Rosales se reutilizaron con su procedencia; no se descargaron otra vez durante este cierre. Los históricos terminan el 27/10/2026.
- **¿Todas las empresas tienen información?** No. Hay un catálogo de 503 símbolos; este recorrido usa datos financieros y precios disponibles de MSFT, GOOGL y META. El resumen bursátil y los balances anuales tienen coberturas distintas. Una celda ausente no se convierte en cero.
- **¿Qué es cierre ajustado?** Es el valor histórico ajustado por el proveedor para eventos corporativos. La app importa ese campo; no calcula por su cuenta una reinversión de dividendos.
- **¿Qué significan 1M, 6M, 1Y y 5Y?** Uno/seis meses y uno/cinco años de observaciones desde la fecha actual. Son ventanas de consulta, no plazos de predicción. El último precio disponible puede ser anterior a hoy.
- **¿Cómo se calcula el gráfico comparativo?** Se cruzan las fechas de las dos series; para cada empresa se calcula `(precio / primer precio común - 1) × 100`. La API devuelve ambas series, y web/Android las dibujan. No se mezclan monedas ni se interpolan días sin datos.
- **¿Qué es ejercicio fiscal?** Es el periodo contable anual declarado por la empresa. FY2024 de Microsoft termina el 30/06/2024; Alphabet y Meta cierran el 31/12/2024. Las fechas completas y versiones deben coincidir para comparar notas.
- **¿Por qué no hay ganador en algunos pilares?** Faltan componentes requeridos o las fechas no coinciden. Las notas de 0 a 100 siguen la metodología académica del proyecto; Altman-Z es otro indicador y no se confunde con esas notas.
- **¿Entrenaron una IA?** No. Se ejecuta Qwen3 ya entrenado mediante llama.cpp en esta laptop. Se entrega contexto verificado y se restringe/valida la respuesta. No es una predicción de precios. El origen por reglas se informa cuando no hay inferencia real.
- **¿Un usuario Free puede cambiar la URL y entrar?** El servidor vuelve a comprobar el plan en comparación, análisis comparativo y reporte, incluso al descargar un archivo generado antes de retirar Pro. Ocultar un botón no es la protección.
- **¿Cómo evitan favoritos repetidos o etiquetas ajenas?** Hay restricciones de unicidad y consultas limitadas al propietario. Las etiquetas usan versiones; un cambio concurrente produce 409 y exige refrescar.
- **¿Se usa MVVM?** En React y Android las vistas delegan en ViewModel y repositorios. Django expone la API, modelos y servicios; no se presenta Django como si fuera un ViewModel de pantalla.
- **¿Por qué la exportación es asíncrona?** La petición guarda un trabajo privado en PostgreSQL y devuelve su identificador. Otro proceso prepara el archivo; la pantalla consulta estado. Reintentar con el mismo identificador no crea otro trabajo.
- **¿Cómo saben que el PDF llegó bien?** Se comprueban tipo, firma PDF, tamaño, final del archivo y SHA-256. Android además lo abre con PdfRenderer. Los errores no se anuncian como archivos guardados.
- **¿Tres segundos está garantizado?** Se midió en esta laptop, por separado: aceptación, disponibilidad y descarga completa. Consultar los resultados; no es una garantía universal. Preparar IA por primera vez se mide aparte de generar el PDF.

## Dónde está cada cosa

| Parte | Archivo o directorio |
|---|---|
| Tabla de comparación y selección | `frontend/src/components/compare/ComparisonPage.jsx`, `frontend/src/viewmodels/useComparisonViewModel.js` |
| Histórico compartido y conclusión local | `backend/api/comparison_services.py` |
| Pantalla y seguimiento PDF | `frontend/src/components/reports/ReportPanel.jsx`, `frontend/src/viewmodels/useReportViewModel.js` |
| Permisos, snapshots y trabajos | `backend/api/report_views.py` |
| Plantilla y gráficos vectoriales | `backend/api/reporting.py` |
| Procesador persistente | `backend/api/management/commands/report_worker.py` |
| Favoritos, visitas y etiquetas | `backend/api/favorite_views.py`, `backend/api/personal_views.py`, `backend/api/models.py` |
| Datos y procedencia | `docs/datos/sprint3/` |
| Pruebas repetibles del sistema real | `tools/verify_sprint3_live.py` |
| Trabajo Android original y adaptaciones | `android/app/src/main/java/com/example/sp500auth/` y referencia Rosales `93e5ea3` |

La aceptación formal de la metodología, los dispositivos físicos elegidos por el equipo y el ensayo conjunto de la exposición se revisan con Angel. La evidencia local no sustituye esa conformidad.
