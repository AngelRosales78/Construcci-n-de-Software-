# Día 1 de Diego

## Qué avanzamos hoy

Revisamos la búsqueda y la ficha web, inventariamos los datos existentes y preparamos las fórmulas y el contrato que compartirán web y Android. Es preparación de T65; los pilares todavía no están programados.

La diferencia principal es esta: **el archivo original de la SEC tiene más información que la base actual**. Podemos aprovecharlo, pero hay que preparar su importación y guardar varios ejercicios antes de mostrar los nuevos indicadores.

## Lo que ya funciona y lo que falta

| Parte | Resultado de la revisión | Qué sigue |
|---|---|---|
| Búsqueda web | Usa el servidor, admite filtros y abre una ficha por ticker. Pruebas existentes aprobadas. | Conservarla al agregar los pilares. |
| Ficha web | Presenta fecha, fuente, indicadores, Altman Z e histórico; explica datos ausentes. | Agregar los nuevos indicadores desde el mismo servidor. |
| Catálogo preparado | 503 símbolos en el CSV. | No confundir catálogo con cobertura financiera. |
| Balance preparado | Una fila de MSFT al 30 de junio de 2024. | Ampliar el almacenamiento anual. |
| Precios preparados | 1254 cierres de MSFT, del 13 de octubre de 2021 al 12 de octubre de 2026. | Son una base para revisar simulaciones después; no son un precio en vivo. |
| Modelo de balances | `FinancialMetrics` guarda una sola ficha por empresa. No conserva componentes completos de liquidez ni series anuales. | Diseñar registros anuales, procedencia y componentes sin romper la ficha existente. |
| Archivo SEC local | FY2022, FY2023 y FY2024 tienen los 14 componentes seleccionados. FY2020 y FY2021 tienen 13 de 14: falta beneficio antes de impuestos en la etiqueta seleccionada. | Importar los componentes necesarios; no sustituir el dato ausente por cero. |
| Crecimiento | El archivo contiene ingresos de FY2021 a FY2024. | Hay cuatro cierres para crecimiento compuesto a tres años, todavía fuera de la base actual. |

La cobertura financiera preparada sigue limitada a MSFT. Este inventario revisó archivos y código, no certifica el contenido de una base activa ni datos actuales de las 503 empresas.

## Qué significa para mañana

1. Conservar la búsqueda y ficha existentes.
2. Preparar el guardado e importación de componentes anuales con fuente y periodo. Es un requisito de las tareas de servidor, no una historia extra.
3. Empezar HU05_02, HU05_03 y HU05_04 usando las fórmulas propuestas en [Metodología de los pilares](Sprint_2_metodologia_pilares.md).
4. Conectar cada pantalla a los resultados del servidor. Angel puede usar el [contrato propuesto](Sprint_2_contrato_pilares.md) para preparar Android.

## Qué comprobamos

El 19 de octubre de 2026 pasaron **48 pruebas existentes de la web** y **87 pruebas existentes del servidor** sobre búsqueda, datos financieros y Altman Z. Cuatro comprobaciones específicas de PostgreSQL se omitieron porque esta revisión usó SQLite temporal. No se midió rendimiento ni se realizó hoy un recorrido manual de navegador.

La primera ejecución encontró restricciones de acceso a dependencias; las pruebas se repitieron con acceso autorizado y finalizaron correctamente. No se modificó el código de la aplicación para hacerlas pasar.

El inventario se reproduce sin red desde la raíz del repositorio:

```text
python tools/audit_sprint2_data.py ../data_sources/MSFT_sec.json
```

La salida [Disponibilidad de datos de MSFT](datos/sprint2_disponibilidad_MSFT.json) conserva las etiquetas SEC, importes, ejercicio, unidad, filing y hash del archivo. La selección usa un mismo informe anual original por ejercicio para evitar mezclar versiones reexpresadas de distintas publicaciones. Los archivos originales no se modificaron.

## Qué queda pendiente

| Tarea | Estado real |
|---|---|
| T65 parte de Diego | Inventario, revisión web y propuesta de fórmulas y contrato preparados. |
| T65 parte de Angel | Conexión Android pendiente de su implementación y prueba. |
| T65 acuerdo del equipo | Revisar juntos fórmulas, reglas de puntuación y contrato. No se registra aprobación de Angel. |
| T66 | Planning Poker y acuerdo de estimaciones pendientes; no se inventan votos. |
| T67 | Configurar y comprobar el proveedor de IA corresponde a Angel; pendiente. |

## Cómo lo puedes explicar

“Hoy comprobé que la búsqueda y la ficha web siguen funcionando en sus pruebas. Revisé qué datos tenemos y encontré que el archivo original de Microsoft contiene los componentes para ampliar el análisis. Dejé las fórmulas y la estructura de respuesta preparadas para que web y Android usen los mismos resultados. Ahora falta importar esos componentes y programar los pilares.”

La procedencia de los datos sigue el mecanismo documentado por la [SEC para sus API de EDGAR](https://www.sec.gov/search-filings/edgar-application-programming-interfaces). Hoy se inspeccionó el snapshot local existente, sin descargar un balance nuevo.
