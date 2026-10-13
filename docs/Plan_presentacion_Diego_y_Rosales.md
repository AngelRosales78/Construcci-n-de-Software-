# Presentación de Diego y Rosales

Duración propuesta: 12 minutos. Entrega: martes 13 de octubre de 2026. Ajustar al tiempo que indique el profesor.

## Reparto real

Diego: épicas 3 y 4, API Django/PostgreSQL/Redis y pantallas React de búsqueda y análisis. No es solamente backend: la interfaz permite mostrar visualmente lo que resuelve la API.
Rosales: épicas 1 y 2, autenticación web y aplicación Android con Kotlin/Compose/Retrofit. Ambos participan en el backend común; Rosales explica la autenticación que implementó.

## Guion y demostración

| Tiempo | Responsable | Acción en pantalla | Qué explicar |
|---|---|---|---|
| 0:00–1:00 | Ambos | Objetivo y reparto por épicas | Una plataforma con autenticación, búsqueda y consulta de información financiera. |
| 1:00–3:30 | Rosales | Abrir Android, registrar/iniciar sesión y volver a entrar | Validación de formularios, consumo de API y almacenamiento seguro de sesión; mostrar únicamente funciones probadas. |
| 3:30–4:30 | Rosales | Cerrar sesión; acceso biométrico solo si está probado | Qué ocurre con las credenciales y cómo vuelve al login. No prometer biometría si no hay evidencia de challenge válido. |
| 4:30–6:00 | Diego | Abrir búsqueda, escribir MS, elegir MSFT con flechas y Enter, cambiar sector y ordenar | Espera de 250 ms en búsqueda, autocompletado Redis, filtros y orden numérico. |
| 6:00–7:00 | Diego | Confirmar dos búsquedas, seleccionar historial, borrar una y limpiar | Historial privado por usuario, máximo diez y copia local por cuenta. |
| 7:00–9:00 | Diego | Abrir MSFT y mostrar PER, ROE, capitalización, beta y dividendo | Diferencia entre cero y dato ausente; fuente/fecha visibles. Explicar el perfil y la leyenda de Altman-Z. |
| 9:00–10:00 | Diego | Cambiar el gráfico a 5Y y usar el navegador inferior/zoom | Los precios salen de la base, no se generan aleatoriamente en cada consulta. La demo usa 1254 cierres ajustados importados con fuente y fecha. |
| 10:00–11:00 | Diego | Mostrar views.py, models.py y pruebas/evidencias en VS Code | Una solicitud pasa por API, consulta/caché y respuesta a React. Mostrar 138 pruebas backend y 28 frontend aprobadas, sin ejecutar toda la carga durante la exposición. |
| 11:00–12:00 | Ambos | Historial de commits y estado final | Identificar trabajo y fechas reales. Mostrar la carga aprobada y los 500 logos verificados; aclarar cobertura de datos y estado de integración Android. |

## Qué decir durante tu parte

“Mi parte corresponde a búsqueda y análisis financiero. Estas pantallas consumen nuestra API Django, que consulta PostgreSQL y utiliza Redis para autocompletar y acelerar consultas. El historial está separado por usuario. La ficha conserva valores cero y distingue datos ausentes. El indicador usa la fórmula Altman-Z y el gráfico recibe precios persistidos.”

“La demo inicia sesión por el formulario y consulta datos importados con fuente y fecha. Microsoft muestra su balance anual de 2024 y precios ajustados; las empresas sin balance importado aparecen sin datos. Rosales demuestra las historias de autenticación y Android que tiene asignadas.”

Si preguntan por rendimiento: “En la medición local de diez usuarios concurrentes hubo 1708 solicitudes, promedio de 32 ms y P95 de 63 ms, pero una superó 200 ms. El límite estricto sigue pendiente.” No decir que todas tardaron menos de 200 ms.

## IDE y ejecución de la demostración

Abrir `Presentacion_Diego.code-workspace` en VS Code. La carpeta abierta es el worktree Magallanes; no el checkout Rosales de Downloads.

Ejecutar la tarea **Iniciar demostracion de Diego** desde Terminal → Ejecutar tarea. Abre una ventana Edge separada con inicio de sesión real de la cuenta desechable y la búsqueda en `http://127.0.0.1:55173/search`. La API temporal está en `http://127.0.0.1:58000`; documentación en `/api/docs/`. La sesión renueva sus tokens a través de la API. No compartir el archivo local de credenciales desechables.

La tarea usa exclusivamente el PostgreSQL portátil de validación en 55432 y Redis de validación en 56379. No conecta a la base de entrega ni edita .env. Requiere el entorno de validación ya preparado en esta PC; no es un instalador para cualquier máquina. Los procesos de servidor quedan ocultos y el navegador es visible para ensayar.

Abrir estos archivos durante la explicación:

- `backend/api/views.py`: búsqueda, historial, clasificación, ficha y precios.
- `backend/api/models.py`: empresas, estados financieros e historial de precios.
- `backend/api/search_cache.py`: ZSET y lista de historial Redis.
- `backend/api/test_financial_data.py`: datos cero/nulo, JOIN y rangos.
- `frontend/src/components/search/CompanySearch.jsx`: consumo de API.
- `frontend/src/components/company/CompanyDashboard.jsx`: ficha y perfil.
- `frontend/src/components/company/PriceChart.jsx`: rangos, crosshair y zoom.
- `docs/Matriz_epicas_3_y_4_12_octubre_2026.md`: trazabilidad.

Usar la tarea **Detener demostracion** al terminar. Rosales debe abrir su carpeta `android` en Android Studio y usar su rama aprobada, conservando tu trabajo en Magallanes. El emulador normalmente accede a la PC por 10.0.2.2: verificar la URL y el puerto efectivos antes del ensayo conjunto.

## Preparación indispensable antes de la exposición final

Confirmar la rama integrada sin historias duplicadas; probar registro/login → búsqueda → ficha → historial → logout; validar Android en dispositivo o emulador; importar datos y logos verificables; revisar el informe de cierre y ensayar las 45 tareas. Si algo no funciona, explicar su estado y mostrar la evidencia disponible, no sustituirlo por una simulación presentada como integración real.

## Lista obligatoria y prueba en vivo

Abrir primero Lista_45_tareas_demostracion_en_vivo.md. Cada fila tiene requisito, estado, archivo/linea y accion. Por HU, acreditar las cinco tareas (Frontend, Backend, Database, UI/UX, Testing) y marcar las mostradas. Ejecutar Mostrar evidencia en vivo de las tareas para la API y PostgreSQL. Para Testing mostrar el test y ejecutar una muestra corta cuando haya tiempo. Si hay que ejecutar las 45 demostraciones individualmente, ampliar Diego a 18-25 minutos; el esquema de 12 minutos exige agrupar y no leer todas las filas. Rosales debe traer su propia lista de 50 tareas de las epicas 1 y 2.

## Estado al cierre del lunes 12

45/45 tareas de Diego verificadas localmente. La demo ya usa login real, catálogo de 503 símbolos, 500 logos locales con alternativa para tres faltantes y ficha MSFT del balance anual 2024. La carga de búsqueda pasó con 2011 respuestas y máximo 142,446 ms. El recorrido web completo pasó; Android y el ensayo conjunto con Rosales siguen pendientes. No anunciar cobertura financiera de los 503 símbolos: únicamente MSFT tiene balance y precios importados. Consultar `Cierre_epicas_3_y_4_12_octubre_2026.md` antes de exponer.
