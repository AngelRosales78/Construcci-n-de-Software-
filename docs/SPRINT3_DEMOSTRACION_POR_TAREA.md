# Sprint 3: explicación y demostración por tarea

Referencia: `Sprint_3_plan_detallado_Android_y_Web (1).pdf`. Se comprobó que es idéntico al PDF anterior mediante SHA-256. Contiene 15 HU y 60 tareas: 30 de Diego (.3/.4) y 30 Android de Angel (.1/.2).

## Preparación y forma de demostrar

1. Abre http://127.0.0.1:55193/search?with_data=true. El filtro corregido muestra MSFT, GOOGL y META.
2. Usa `sprint3_pro` para comparación y exportación dual; `sprint3_free` para el bloqueo. Ambas contraseñas locales de demostración son las entregadas al usuario; no se incluyen en este archivo ni se proyectan credenciales.
3. Usa MSFT FY2024 para la ficha con resumen y GOOGL/META FY2024 para comparación de periodos coincidentes. Hay cinco filas pero solo tres notas comparables para ese par.
4. Para cada tarea web: di su propósito, realiza la acción y señala el resultado. Para cada tarea del servidor: relaciona esa misma acción con la función indicada y una prueba concreta. No hace falta repetir el mismo recorrido dos veces.
5. Una pantalla funcionando no acredita por sí sola todos los casos de error. Las pruebas registradas complementan la demostración en vivo; identifica siempre cuál evidencia estás mostrando.
6. Android se demuestra en Android. La web no sustituye las 30 tareas de Rosales. La sincronización requiere la misma cuenta y la misma API, no dos demos distintas.

## Las 30 tareas de Diego, historia por historia

### 1. HU08_01 — Selección de dos empresas

**T01.3 — Selector dual en Web**
- Qué decir: «Puedo elegir dos empresas distintas y conservar la selección.»
- Demostración y resultado: En Comparar, selecciona GOOGL y META, FY2024. Intenta repetir GOOGL, intercambia A/B y recarga. Debe rechazar la repetida y conservar los símbolos.
- Código principal: [CompanyPicker.jsx](../frontend/src/components/compare/CompanyPicker.jsx).

**T01.4 — Servicio compartido de comparación**
- Qué decir: «El servidor valida empresas, ejercicio y acceso antes de consultar los datos.»
- Evidencia: Abre ComparisonView.get en backend/api/comparison_views.py. Señala validación de a/b, rechazo de símbolos iguales y llamadas a CompanyPillarsView. Relaciona sus parámetros con la selección que acabas de mostrar.
- Código principal: [comparison_views.py](../backend/api/comparison_views.py).

### 2. HU08_02 — Comparación de cinco pilares

**T02.3 — Tabla comparativa en Web**
- Qué decir: «La tabla diferencia notas disponibles, empates y datos no comparables.»
- Demostración y resultado: Muestra las cinco filas de GOOGL/META FY2024: tres comparables y dos sin nota. Intercambia A/B: la diferencia cambia de signo. Con MSFT/GOOGL FY2024 explica las fechas de cierre diferentes.
- Código principal: [ComparisonPage.jsx](../frontend/src/components/compare/ComparisonPage.jsx).

**T02.4 — Comparabilidad y pruebas de la API**
- Qué decir: «La API compara periodos completos y versiones; calcula A menos B con la precisión visible.»
- Evidencia: Abre comparison_rows en backend/api/comparison_views.py. Señala periods_match, methods_match, ROUND_HALF_UP y delta. Muestra test_same_fiscal_year_with_different_periods_has_no_winner en backend/api/test_sprint3_comparison.py.
- Código principal: [comparison_views.py](../backend/api/comparison_views.py).

### 3. HU08_03 — Rendimiento histórico superpuesto

**T03.3 — Gráfico histórico en Web**
- Qué decir: «Puedo ver la evolución relativa de dos acciones en la misma escala.»
- Demostración y resultado: Pulsa Consultar rendimiento. Cambia 1Y por 6M o 5Y y mueve el deslizador de fecha. Deben aparecer dos series y la lectura de ambas para la misma fecha.
- Código principal: [ComparisonHistory.jsx](../frontend/src/components/compare/ComparisonHistory.jsx).

**T03.4 — Normalización histórica compartida**
- Qué decir: «El servidor cruza fechas y normaliza cada serie desde su primer cierre común.»
- Evidencia: Abre aligned_history en backend/api/comparison_services.py. Señala la intersección de fechas y la fórmula (precio/base - 1) × 100. La primera lectura del gráfico debe ser 0 % para ambas.
- Código principal: [comparison_services.py](../backend/api/comparison_services.py).

### 4. HU08_04 — Conclusión comparativa con IA local

**T04.3 — Conclusión comparativa en Web**
- Qué decir: «El análisis identifica su origen, fecha, fuentes y fortalezas relativas.»
- Demostración y resultado: Abre Conclusión comparativa y reporte PDF y pulsa Generar conclusión comparativa. Lee Origen y abre Fuentes del análisis. Explica por qué no se declara un ganador global si faltan pilares.
- Código principal: [ReportPanel.jsx](../frontend/src/components/reports/ReportPanel.jsx).

**T04.4 — Generación comparativa y caché**
- Qué decir: «Qwen local elige un resumen entre conclusiones verificadas; se valida y se guarda en caché.»
- Evidencia: Abre generate_comparison y conclusion en backend/api/comparison_services.py. Señala el esquema restringido, la validación del texto, generation_mode y cache_hit. El fallo del motor se etiqueta rules; no lo presentes como IA.
- Código principal: [comparison_services.py](../backend/api/comparison_services.py).

### 5. HU08_05 — Acceso según plan

**T05.3 — Bloqueo y navegación Pro en Web**
- Qué decir: «Free muestra una vista previa; Pro permite consultar la comparación.»
- Demostración y resultado: Con sprint3_free entra a Comparar y muestra Vista previa. Cambia a sprint3_pro y muestra el resultado del par. No presentes el botón de planes como una compra real.
- Código principal: [useComparisonViewModel.js](../frontend/src/viewmodels/useComparisonViewModel.js).

**T05.4 — Autorización del comparador**
- Qué decir: «Los permisos se comprueban también en la API y en cada descarga comparativa.»
- Evidencia: Abre ComparisonView.get y authorize en backend/api/report_views.py. Señala la comprobación de compare. Muestra la prueba test_access_is_enforced_at_api_and_expiration_is_immediate y la revocación en test_comparison_pdf_is_vector_landscape_two_pages_and_rechecks_pro.
- Código principal: [comparison_views.py](../backend/api/comparison_views.py).

### 6. HU09_01 — Ficha financiera en PDF

**T06.3 — Exportación de ficha en Web**
- Qué decir: «La ficha elegida se puede exportar con su empresa, periodo y fuentes.»
- Demostración y resultado: Abre Microsoft, Exportar reporte PDF de esta empresa, Solicitar PDF y Descargar PDF verificado. Abre el archivo descargado y señala MSFT, FY2024, indicadores y fuentes.
- Código principal: [ReportPanel.jsx](../frontend/src/components/reports/ReportPanel.jsx).

**T06.4 — Generación de reporte compartido**
- Qué decir: «El servidor congela los datos elegidos y construye el documento autenticado.»
- Evidencia: Abre create_snapshot en backend/api/report_views.py y render_report en backend/api/reporting.py. Contrasta identidad, periodo y un valor del PDF con la ficha. Señala que ReportFileView limita el archivo a su propietario.
- Código principal: [report_views.py](../backend/api/report_views.py).

### 7. HU09_02 — Gráficos vectoriales en PDF

**T07.3 — Opciones y revisión gráfica en Web**
- Qué decir: «Puedo incluir los gráficos del rango seleccionado en el informe.»
- Demostración y resultado: Elige un rango, marca Incluir gráficos vectoriales y genera otro PDF. Ábrelo al 200 %: revisa líneas, etiquetas y fechas. Si falta una nota, el radar debe indicarlo; no inventar un polígono completo.
- Código principal: [ReportPanel.jsx](../frontend/src/components/reports/ReportPanel.jsx).

**T07.4 — Gráficos vectoriales en la plantilla**
- Qué decir: «La plantilla dibuja los gráficos con trazos vectoriales desde el snapshot.»
- Evidencia: Abre VectorChart.draw en backend/api/reporting.py. Señala drawPath/line y el tratamiento de notas ausentes. Muestra la comprobación not page.images en backend/api/test_sprint3_completion.py; el PDF no usa capturas PNG para esos gráficos.
- Código principal: [reporting.py](../backend/api/reporting.py).

### 8. HU09_03 — Análisis vigente dentro del PDF

**T08.3 — Análisis exportable en Web**
- Qué decir: «Puedo elegir incluir el análisis preparado y mantener su origen.»
- Demostración y resultado: En el panel PDF prepara primero el análisis, comprueba fecha/origen y marca Incluir el análisis preparado. Descarga y busca el mismo resumen en el archivo.
- Código principal: [ReportPanel.jsx](../frontend/src/components/reports/ReportPanel.jsx).

**T08.4 — Texto y trazabilidad de IA en PDF**
- Qué decir: «La exportación usa el texto almacenado de esa empresa, periodo y versión.»
- Evidencia: Abre individual_analysis y create_snapshot en backend/api/report_views.py. Señala analysis_version y analysis_origin, y el rechazo de una versión que ya no coincide. No se vuelve a llamar al modelo al dibujar el PDF.
- Código principal: [report_views.py](../backend/api/report_views.py).

### 9. HU09_04 — PDF comparativo para Pro

**T09.3 — Exportación comparativa en Web**
- Qué decir: «La comparación de las dos empresas se exporta en un documento legible.»
- Demostración y resultado: Con Pro, GOOGL/META FY2024, solicita el PDF comparativo. Ábrelo: deben aparecer ambas empresas, cinco filas, fuentes y hasta dos páginas horizontales.
- Código principal: [ReportPanel.jsx](../frontend/src/components/reports/ReportPanel.jsx).

**T09.4 — Plantilla comparativa y autorización**
- Qué decir: «La plantilla dual y los permisos se conservan incluso al descargar un reporte anterior.»
- Evidencia: Abre authorize, ReportFileView y render_report. Señala landscape(A4), límite de dos páginas y verificación Pro al entregar el archivo. Muestra test_comparison_pdf_is_vector_landscape_two_pages_and_rechecks_pro.
- Código principal: [reporting.py](../backend/api/reporting.py).

### 10. HU09_05 — Exportación asíncrona y recuperación

**T10.3 — Seguimiento de exportación en Web**
- Qué decir: «Puedo seguir el reporte sin bloquear la página ni duplicarlo al reintentar.»
- Demostración y resultado: Solicita un PDF y muestra En cola/Generando/Listo según alcance a verse. Sal de la vista y vuelve al mismo par/ejercicio: recupera el trabajo. Para estados demasiado rápidos, enseña la prueba registrada; no inventes una espera.
- Código principal: [useReportViewModel.js](../frontend/src/viewmodels/useReportViewModel.js).

**T10.4 — Cola y optimización del servicio PDF**
- Qué decir: «Un proceso separado consume trabajos persistentes e idempotentes de PostgreSQL.»
- Evidencia: Abre ReportsView.post y process_report, y backend/api/management/commands/report_worker.py. Señala request_id por usuario, GET de estado y cancelación. En docs/evidencias/sprint3_cierre/resultados.json distingue acceptance_ms, ready_ms y full_download_ms.
- Código principal: [report_worker.py](../backend/api/management/commands/report_worker.py).

### 11. HU10_01 — Añadir favoritos

**T11.3 — Marcado y listado en Web**
- Qué decir: «Guardo una empresa desde su tarjeta o ficha y aparece en mi lista.»
- Demostración y resultado: En Explorar, pulsa Guardar favorito en GOOGL o META. Abre Favoritos y comprueba símbolo y nombre. Si ya está guardada, usa otra; no dupliques registros.
- Código principal: [FavoriteButton.jsx](../frontend/src/components/favorites/FavoriteButton.jsx).

**T11.4 — Alta de favoritos por usuario**
- Qué decir: «El alta pertenece al usuario autenticado y evita empresas repetidas.»
- Evidencia: Abre FavoritesView.post en backend/api/favorite_views.py y el modelo Favorite en backend/api/models.py. Señala user=request.user y la unicidad usuario/empresa. El guardado repetido devuelve el favorito existente.
- Código principal: [favorite_views.py](../backend/api/favorite_views.py).

### 12. HU10_02 — Quitar favoritos

**T12.3 — Eliminación de favoritos en Web**
- Qué decir: «La eliminación requiere confirmación y permite cancelar.»
- Demostración y resultado: En Favoritos pulsa Quitar. Cancela y comprueba que sigue. Repite y confirma: desaparece de la lista y se desmarca en su ficha.
- Código principal: [FavoriteRemovalDialog.jsx](../frontend/src/components/favorites/FavoriteRemovalDialog.jsx).

**T12.4 — Baja autorizada y pruebas del servicio**
- Qué decir: «La API elimina únicamente el favorito del propietario.»
- Evidencia: Abre FavoriteDetailView.delete en backend/api/favorite_views.py. Señala el filtro por usuario e identificador del favorito. El identificador del favorito no se debe confundir con el de la empresa.
- Código principal: [favorite_views.py](../backend/api/favorite_views.py).

### 13. HU10_03 — Sincronización web y Android

**T13.3 — Actualización de favoritos en Web**
- Qué decir: «Las dos pantallas comparten la lista del servidor sin reiniciar sesión.»
- Demostración y resultado: Angel abre Favoritos en Android contra la misma API y con la misma cuenta. Añade desde Android y observa la web; elimina desde web y observa Android. Muestra la hora de sincronización.
- Código principal: [useFavoritesViewModel.js](../frontend/src/viewmodels/useFavoritesViewModel.js).

**T13.4 — Contrato y prueba de sincronización**
- Qué decir: «El servidor es la referencia común y devuelve versión/fecha para comprobar actualizaciones.»
- Evidencia: Abre FavoritesView.get y useFavoritesViewModel.js. Señala version/fetched_at y la consulta periódica de dos segundos con la vista activa. En resultados.json muestra la medida final registrada: 4125 ms y 640 ms; no son una garantía universal.
- Código principal: [favorite_views.py](../backend/api/favorite_views.py).

### 14. HU10_04 — Diez empresas recientes

**T14.3 — Vistos recientemente en Web**
- Qué decir: «Se recuerdan las empresas consultadas, no solo lo escrito en el buscador.»
- Demostración y resultado: Abre las fichas de MSFT, GOOGL y META. Ve a Inicio: deben aparecer en Vistos recientemente. Reabre una de ellas y comprueba que sube al inicio sin duplicarse.
- Código principal: [RecentCompanies.jsx](../frontend/src/components/RecentCompanies.jsx).

**T14.4 — Historial personal y límite de registros**
- Qué decir: «El servidor deduplica eventos de visita y conserva diez empresas únicas por usuario.»
- Evidencia: Abre CompanyVisitView.post en backend/api/personal_views.py. Señala event_id, update_or_create y el recorte a diez. Muestra test_recent_ten_unique_revisit_and_event_replay_do_not_reorder: con tres visitas visibles no se demuestra por sí solo el límite de diez.
- Código principal: [personal_views.py](../backend/api/personal_views.py).

### 15. HU10_05 — Etiquetas y listas privadas

**T15.3 — Gestión y filtros de etiquetas en Web**
- Qué decir: «Una empresa puede pertenecer a varias listas sin duplicar el favorito.»
- Demostración y resultado: En Favoritos abre Organizar favoritos con etiquetas. Crea dos, asigna ambas a la misma empresa y filtra por cada una. Edita un nombre y borra una etiqueta: el favorito debe permanecer.
- Código principal: [TagsPanel.jsx](../frontend/src/components/favorites/TagsPanel.jsx).

**T15.4 — Etiquetas privadas y relación múltiple**
- Qué decir: «Las etiquetas y asignaciones son privadas y detectan cambios concurrentes.»
- Evidencia: Abre TagsView, TagDetailView y FavoriteTagsView en backend/api/personal_views.py. Señala normalización del nombre, propiedad, relación múltiple y version. Muestra las pruebas de nombre duplicado, etiqueta ajena y conflicto 409 sin sobrescritura.
- Código principal: [personal_views.py](../backend/api/personal_views.py).

## Las 30 tareas Android de Angel

Esta sección es el recorrido de Rosales. El código móvil original se incorporó desde `origin/Rosales@93e5ea3` y se adaptó en el commit de integración de Diego. El emulador ya tiene pruebas registradas; el teléfono físico y el ensayo conjunto permanecen como comprobaciones del equipo.

| HU | Tarea Android | Qué debe demostrar Rosales | Código |
|---|---|---|---|
| HU08_01 | T01.1 — Selector dual en Android | Seleccionar dos empresas, rechazar la repetida e intercambiarlas en el teléfono/emulador. | [CompanySelection.kt](../android/app/src/main/java/com/example/sp500auth/ui/compare/CompanySelection.kt) |
| HU08_01 | T01.2 — Consulta y pruebas del comparador móvil | Mostrar la consulta real del par, cambio de ejercicio y error recuperable con selección conservada. | [ComparisonRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ComparisonRepository.kt) |
| HU08_02 | T02.1 — Tabla de pilares en Android | Leer las cinco filas, las dos notas y el estado de comparación. | [ComparisonTable.kt](../android/app/src/main/java/com/example/sp500auth/ui/compare/ComparisonTable.kt) |
| HU08_02 | T02.2 — Integración y casos límite de pilares | Probar periodos distintos y notas ausentes; explicar que no se sustituyen por cero. | [ComparisonRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ComparisonRepository.kt) |
| HU08_03 | T03.1 — Gráfico de dos empresas en Android | Mostrar dos series, cambiar rango y consultar fechas con los controles accesibles. | [HistoryComparisonChart.kt](../android/app/src/main/java/com/example/sp500auth/ui/compare/HistoryComparisonChart.kt) |
| HU08_03 | T03.2 — Series históricas y pruebas móviles | Señalar el uso de compare/history y las fechas/porcentajes comunes del servidor. | [HistoryComparisonRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/HistoryComparisonRepository.kt) |
| HU08_04 | T04.1 — Lectura comparativa de IA en Android | Abrir la conclusión y señalar origen, fuentes y fortalezas. | [ComparisonConclusionCard.kt](../android/app/src/main/java/com/example/sp500auth/ui/compare/ComparisonConclusionCard.kt) |
| HU08_04 | T04.2 — Integración y validación móvil de IA | Mostrar que el repositorio valida par, ejercicio, cuenta y origen; el texto por reglas no se etiqueta IA. | [ConclusionRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ConclusionRepository.kt) |
| HU08_05 | T05.1 — Vista previa y acceso Pro en Android | Con Free mostrar vista previa y con Pro el comparador. | [AccessScreens.kt](../android/app/src/main/java/com/example/sp500auth/ui/sprint3/AccessScreens.kt) |
| HU08_05 | T05.2 — Permisos y pruebas de acceso móvil | Señalar permisos/expiración y el error real de API; una vista previa no activa Pro. | [AccessRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/AccessRepository.kt) |
| HU09_01 | T06.1 — Descarga y apertura de PDF en Android | Generar y descargar la ficha; abrir el selector para guardar y después abrir el PDF. | [ReportScreen.kt](../android/app/src/main/java/com/example/sp500auth/ui/reports/ReportScreen.kt) |
| HU09_01 | T06.2 — Servicio de PDF y pruebas móviles | Mostrar comprobación de binario, cancelación y pruebas de red/almacenamiento; el error ENOSPC se simuló, no se llenó el disco. | [ReportRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ReportRepository.kt) |
| HU09_02 | T07.1 — Opciones de gráficos en Android | Elegir incluir gráficos y el rango solicitado. | [ChartOptions.kt](../android/app/src/main/java/com/example/sp500auth/ui/reports/ChartOptions.kt) |
| HU09_02 | T07.2 — Verificación de gráficos desde Android | Abrir el archivo con PdfRenderer y ampliar gráficos; comprobar rango, formato vectorial y notas ausentes. | [ReportRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ReportRepository.kt) |
| HU09_03 | T08.1 — Selección de análisis para PDF en Android | Preparar y seleccionar el análisis indicando nivel y origen. | [ReportAnalysisSelector.kt](../android/app/src/main/java/com/example/sp500auth/ui/reports/ReportAnalysisSelector.kt) |
| HU09_03 | T08.2 — Consistencia del análisis exportado móvil | Comparar empresa, periodo, versión y origen con el PDF descargado. | [ReportAnalysisRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ReportAnalysisRepository.kt) |
| HU09_04 | T09.1 — Exportación comparativa en Android | Solicitar la comparación con Pro y conservar el orden de empresas. | [ReportScreen.kt](../android/app/src/main/java/com/example/sp500auth/ui/reports/ReportScreen.kt) |
| HU09_04 | T09.2 — Validación móvil del reporte comparativo | Abrir el PDF horizontal de hasta dos páginas y mostrar bloqueo de Free. | [ReportRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ReportRepository.kt) |
| HU09_05 | T10.1 — Estado de exportación en Android | Mostrar estado de trabajo, salir de la pantalla y volver al seguimiento. | [ReportsViewModel.kt](../android/app/src/main/java/com/example/sp500auth/ui/reports/ReportsViewModel.kt) |
| HU09_05 | T10.2 — Rendimiento y recuperación móvil | Explicar solicitud, disponibilidad y descarga por separado; demostrar reintento/cancelación y pruebas de reconexión. | [ReportRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/ReportRepository.kt) |
| HU10_01 | T11.1 — Marcado de favoritos en Android | Marcar una empresa y verla en Favoritos. | [FavoritesScreen.kt](../android/app/src/main/java/com/example/sp500auth/ui/favorites/FavoritesScreen.kt) |
| HU10_01 | T11.2 — Persistencia y pruebas móviles de alta | Mostrar persistencia y ausencia de duplicados en el servidor. | [FavoritesRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/FavoritesRepository.kt) |
| HU10_02 | T12.1 — Eliminación de favoritos en Android | Pedir quitar una empresa, cancelar y después confirmar. | [FavoritesViewModel.kt](../android/app/src/main/java/com/example/sp500auth/ui/favorites/FavoritesViewModel.kt) |
| HU10_02 | T12.2 — Baja y pruebas móviles de favoritos | Comprobar actualización de ficha/lista y el rechazo de operaciones sobre favoritos ajenos. | [FavoritesRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/FavoritesRepository.kt) |
| HU10_03 | T13.1 — Actualización visible en Android | Volver a primer plano o actualizar la lista y mostrar la hora de sincronización. | [FavoritesScreen.kt](../android/app/src/main/java/com/example/sp500auth/ui/favorites/FavoritesScreen.kt) |
| HU10_03 | T13.2 — Sincronización y pruebas entre clientes | Con Diego, verificar altas/bajas entre web y Android y mostrar los tiempos medidos. | [FavoritesRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/FavoritesRepository.kt) |
| HU10_04 | T14.1 — Vistos recientemente en Android | Abrir varias empresas y leer los vistos recientemente. | [RecentViewModel.kt](../android/app/src/main/java/com/example/sp500auth/ui/sprint3/RecentViewModel.kt) |
| HU10_04 | T14.2 — Registro y pruebas del historial móvil | Revisitar una, comprobar orden sin duplicados y mostrar la prueba del límite de diez. | [RecentRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/RecentRepository.kt) |
| HU10_05 | T15.1 — Gestión de etiquetas en Android | Crear, renombrar y borrar etiquetas; asignar varias a una empresa y filtrar. | [TagsScreen.kt](../android/app/src/main/java/com/example/sp500auth/ui/favorites/TagsScreen.kt) |
| HU10_05 | T15.2 — Etiquetas y pruebas de integración móvil | Comprobar respuesta del servidor, conservación del favorito, cambio de cuenta y casos de error. | [TagsRepository.kt](../android/app/src/main/java/com/example/sp500auth/data/repository/TagsRepository.kt) |

## Qué decir sobre la corrección del catálogo

«El filtro anterior solo miraba tres campos del resumen: capitalización, PER y ROE. Lo corregimos para que reconozca también los balances anuales ya importados. Ahora aparecen tres empresas y cada tarjeta indica su cobertura. No generamos ratios ficticios ni afirmamos que las 503 empresas tengan toda la información».

Microsoft dispone del resumen cargado. Alphabet y Meta tienen balances anuales y precios; sus cinco métricas del resumen no están cargadas. Para mostrar sus indicadores anuales entra a Salud financiera. La falta de componentes de dos pilares se mantiene explícita.

## Cómo defender los commits

«Agrupamos tareas relacionadas en commits con sus códigos y archivos. Los cuatro commits de implementación del cierre incluyen los 60 identificadores, con atribución del código Android de Angel. El quinto recoge la documentación y después se añadió esta corrección de cobertura. La cantidad de commits no equivale a la cantidad de tareas».

Abre [SPRINT3_MATRIZ_CIERRE.md](SPRINT3_MATRIZ_CIERRE.md) y [SPRINT3_RESULTADOS_CIERRE.md](SPRINT3_RESULTADOS_CIERRE.md). El informe inicial conserva los resultados de ese cierre; esta corrección añade 28 pruebas de servidor y 24 de búsqueda web aprobadas, con compilación correcta. No sumes ejecuciones repetidas como si fueran tareas nuevas.

## Límites que debes reconocer

- No afirmar cobertura completa del S&P 500: el catálogo contiene símbolos; los datos importados tienen cobertura limitada.
- No afirmar que el precio es en vivo: los snapshots llegan al 27/10/2026.
- No presentar reglas o notas académicas como predicciones ni garantías de inversión.
- No afirmar que todas las pruebas se hicieron en un teléfono físico: hubo pruebas JVM/HTTP y diez recorridos en emulador; la aceptación física y la metodología formal se revisan con Angel.
- No presentar la ocultación de un botón como prueba suficiente de seguridad: acompáñala con la validación del servidor y sus pruebas.
