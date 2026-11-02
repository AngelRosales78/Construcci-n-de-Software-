# Estado de Android al 2 de noviembre de 2026

Se implementaron las 30 tareas Android del plan de 15 historias. Diez tareas ya estaban en la base y las veinte restantes tienen un commit individual. Esta entrega corresponde al cliente Android, sus contratos y pruebas; no constituye la aceptación integral de las historias en Web, servidor y dispositivo.

## Matriz de implementación

| HU | Tareas Android | Implementación |
|---|---|---|
| HU08_01 | T01.1 y T01.2 | Selección dual, búsquedas independientes, intercambio, estado restaurable y consulta del par. |
| HU08_02 | T02.1 y T02.2 | Tabla de cinco pilares, fuentes, notas, datos ausentes y decisiones comparativas del servidor. |
| HU08_03 | T03.1 y T03.2 | Histórico superpuesto, fechas comunes, rangos y consulta por fecha. |
| HU08_04 | T04.1 y T04.2 | Conclusión del par y ejercicio, origen IA/reglas, fuentes y cancelación de respuestas anteriores. |
| HU08_05 | T05.1 y T05.2 | Vista previa Free, consulta de plan y verificación Pro. No se inventan permisos ni se simulan pagos. |
| HU09_01 | T06.1 y T06.2 | Generación solicitada al servidor, descarga autenticada, comprobación de PDF, guardado, apertura y compartir. |
| HU09_02 | T07.1 y T07.2 | Opciones de gráficos y rango, comprobación del formato vectorial anunciado y coincidencia con la solicitud. |
| HU09_03 | T08.1 y T08.2 | Comprobación de análisis disponible, nivel y origen; vinculación de la versión del texto al PDF. |
| HU09_04 | T09.1 y T09.2 | Exportación del par activo, permiso Pro, hasta dos páginas y orientación horizontal comprobadas al abrir el PDF. |
| HU09_05 | T10.1 y T10.2 | Trabajos asíncronos, seguimiento, recuperación por identificador, idempotencia, cancelación y medidas separadas. |
| HU10_01 | T11.1 y T11.2 | Alta confirmada, lista personal, duplicados y aislamiento de cuentas. |
| HU10_02 | T12.1 y T12.2 | Confirmación y Cancelar, baja por identificador y comprobación de bajas repetidas. |
| HU10_03 | T13.1 y T13.2 | Gesto de actualización, fecha de sincronización, consulta cada dos segundos mientras la lista está visible y copia anterior identificada. |
| HU10_04 | T14.1 y T14.2 | Últimas diez empresas, visitas registradas tras una ficha cargada y deduplicación del evento. |
| HU10_05 | T15.1 y T15.2 | CRUD de etiquetas, colores, asignación múltiple, filtros y control de conflictos con versión. |

## Servicios necesarios para la aceptación conjunta

La copia de backend revisada conserva las rutas del Sprint 2. Continúan pendientes compare, conclusión comparativa, planes, PDF, favoritos, visitas y etiquetas. Los errores se muestran en Android; las pantallas no sustituyen respuestas ausentes por ejemplos de producción.

El comparador requiere un permiso Pro verificado antes de cargar su contenido. Si falta user/subscription, se presenta un error de verificación y no se concede acceso. La generación de pagos o suscripciones pertenece a la épica 11 y no se incluye en este sprint.

Las peticiones personales llevan Bearer y una etiqueta interna de cuenta comprobada antes del transporte y los reintentos. Los contratos incluyen owner_id para rechazar respuestas de otro usuario. Esto complementa la autorización que debe aplicar el servidor; no la reemplaza.

### Comparador y planes

* GET `user/subscription/`: subscription-v1, owner_id, plan free/pro, active booleano, features y valid_until UTC. Para comparar, Pro activo debe incluir compare. Los permisos vencidos vuelven a comprobarse.
* GET `compare/?a=&b=&fiscal_year=`: contrato de CONTRATO_COMPARADOR.md. Cinco ejes vigentes: liquidez, rentabilidad, solvencia, eficiencia y crecimiento.
* GET `compare/conclusion/?a=&b=&fiscal_year=&level=`: comparison-conclusion-v1, owner_id, a, b, fiscal_year, level simple/technical, generation_mode ai/rules, generated_at, data_version, paragraphs, strengths_a, strengths_b y sources con URL. Debe fundamentarse en datos verificables. La validación estructural de Android no certifica por sí sola la exactitud semántica del texto.

### Reportes PDF

* POST `reports/jobs/`: ReportRequest con request_id único, kind company/comparison, a, b opcional, fiscal_year, range, include_charts, include_analysis, analysis_level, analysis_version y analysis_origin. El servidor debe deduplicar por usuario y request_id; repetir una solicitud incierta conserva ese identificador.
* GET `reports/jobs/{id}/`: consulta de un trabajo propio con ReportJob. POST a `reports/jobs/{id}/cancel/` debe confirmar un estado terminal antes de anunciar cancelación.
* ReportJob contiene schema_version report-job-v1, owner_id, id, status queued/running/ready/error/cancelled, request completo y progress opcional. Un trabajo ready incluye data_version, content_sha256 SHA-256 hexadecimal y size_bytes. Nunca se acepta que cambie la selección u opciones de request.
* Si se pidieron gráficos, chart_format debe ser vector y chart_range debe coincidir. Si se pidió análisis, analysis_version y analysis_origin deben coincidir. Para comparación, page_count está entre uno y dos y orientation es landscape.
* GET `reports/jobs/{id}/file/` devuelve application/pdf y el archivo completo del trabajo. Android verifica tamaño, firma PDF, terminación, hash y apertura con PdfRenderer. En comparación comprueba también cantidad y orientación de páginas. Un archivo HTML, incompleto o de otra solicitud no se habilita para guardar.
* El servidor comprueba Pro para comparar y exportar comparación, incluso ante peticiones directas. Android vuelve a comprobar el plan antes de solicitar o descargar una comparación.

Las medidas de aceptación, disponibilidad observada y descarga se muestran por separado. Al recrear el proceso, la medida de disponibilidad parte de la nueva sesión de seguimiento; no reconstruye el tiempo total previo del servidor. El objetivo de tres segundos y dos MB necesita mediciones reales con el servicio. Los veinte MB son un límite defensivo del cliente, no el objetivo de tamaño. No se ha certificado rendimiento de generación ni calidad vectorial mediante inspección de un reporte real del servicio pendiente.

Para la IA individual se usa el análisis existente del Sprint 2. Si ese servicio informa IA para las explicaciones pero conserva el resumen por reglas, el origen del reporte se identifica como mixed. No se relabela todo el resumen como generado por IA. La versión incluye metodología, fecha de datos y fecha de generación para vincular el texto correcto.

### Favoritos y etiquetas

Favoritos mantiene el contrato de HISTORICO_Y_FAVORITOS.md. GET es una colección completa y personal; las altas y bajas solo se reflejan tras confirmación. Una copia en memoria retenida tras un error se marca como anterior, muestra su fecha y deshabilita escrituras hasta sincronizar. Un error de permisos retira los datos privados. Con la lista activa se consulta cada dos segundos; al salir o ir a segundo plano se detiene la consulta periódica. El plazo de propagación Web/Android depende del servicio real y todavía no está medido.

* GET `tags/`: tags-v1, owner_id, version, tags y assignments. Cada etiqueta: id positivo, name de uno a cuarenta caracteres y color hexadecimal. No hay nombres equivalentes repetidos tras normalizar espacios, mayúsculas y Unicode.
* POST `tags/` crea; PATCH `tags/{id}/` edita con name, color y version esperada. DELETE `tags/{id}/?version=` elimina una etiqueta sin eliminar favoritos. Todas devuelven la instantánea completa y confirmada.
* PUT `favorites/{id}/tags/`: tag_ids y version esperada; devuelve la asignación confirmada para ese favorito. Se admite una lista vacía explícita. El servidor debe impedir referencias a etiquetas o favoritos ajenos y rechazar conflictos de versión con 409, sin sobrescribir cambios concurrentes.
* Una instantánea contiene tags únicos y assignments con favorite_id y tag_ids únicos que pertenecen al catálogo de etiquetas de esa cuenta. La respuesta conserva las etiquetas restantes y la colección de favoritos.

### Empresas recientes

GET `companies/recent/` devuelve recent-companies-v1, owner_id y results con hasta diez empresas únicas, ordenadas de más reciente a más antigua. Cada fila incluye ticker, name y visited_at UTC. POST `companies/{ticker}/visits/` recibe event_id; el servidor deduplica ese evento y devuelve el historial actualizado con esa empresa al inicio. Android emite el evento después de una ficha cargada correctamente. No usa el historial de términos de búsqueda como historial de visitas.

## Comprobaciones pendientes de dispositivo y servidor

1. Free, Pro, permiso vencido y denegación directa en servidor. Verificar que cerrar sesión retira datos privados y que el cambio de cuenta no aplica respuestas anteriores.
2. Ejecutar las pruebas instrumentales y revisar las pantallas en emulador: giro, tamaños de pantalla, gesto de refresco, diálogo de etiquetas y lectura del gráfico.
3. Generar PDF real con y sin gráficos/análisis; abrirlo, ampliar al 200 %, revisar fuentes y acentos, comprobar las dos páginas comparativas y medir tiempos/tamaño.
4. Cancelar el selector de archivos, probar red interrumpida, proveedor de documentos no disponible, falta de espacio y aplicación lectora ausente. No debe habilitarse Abrir o Compartir como si el guardado hubiera terminado.
5. Abandonar el reporte, volver a la ficha y retomar seguimiento; recrear el proceso y consultar el mismo trabajo sin otro POST. Repetir un envío con respuesta perdida usando el mismo request_id.
6. Con Web y Android, dar de alta y quitar favoritos, comprobar propagación y conflictos; asignar varias etiquetas, renombrar y borrar una sin perder empresas.
7. Consultar doce empresas, comprobar las diez últimas, revisitar una y verificar que sube sin duplicarse. Un fallo de carga no debe generar una visita.

Las pruebas JVM con respuestas controladas verifican el cliente y sus contratos; no acreditan la implementación o rendimiento de un servidor que aún no existe. No se entregó un APK ni se cambió Script 1 o Script 2.

## Evidencia del cierre del cliente

Gradle 8.4 y Java 17: 135 pruebas JVM descubiertas, 132 aprobadas, 3 integraciones opcionales omitidas, cero fallos y cero errores. Las 21 pruebas nuevas del alcance restante aprobaron. Compilaron la aplicación y las pruebas instrumentales, incluidas dos nuevas de vista previa Pro, opciones del reporte y rotulado de origen por reglas. No se ejecutaron en dispositivo: adb no encontró emulador ni teléfono conectado. Después de las pruebas JVM se recompilaron aplicación y pruebas instrumentales al ajustar las insignias visuales de etiquetas; no cambió la lógica verificada por JVM.

Las pruebas nuevas cubren identidad y origen de conclusiones, acceso Pro y expiración, integridad binaria del PDF, opciones y metadatos de reportes, recuperación de trabajo y request_id, rechazo confirmado frente a respuesta incierta, aislamiento de cuentas, sincronización con copia anterior, orden y límite de recientes, respuestas tardías y conflictos de etiquetas. No se declara medido el objetivo de tres segundos ni la propagación entre clientes.

Hay veinte commits nuevos de tareas, uno por cada tarea Android pendiente, además del commit de verificación y documentación. El autor conserva la identidad AngelRosales78. El resumen reproducible se registra en `evidence/sprint3_cierre_android.json`; las pruebas se ejecutan desde android con JDK 17 mediante `gradlew.bat testDebugUnitTest compileDebugKotlin compileDebugAndroidTestKotlin`. En este entorno se precargó el agente Byte Buddy existente para Mockito. No se agregaron dependencias de producción.
