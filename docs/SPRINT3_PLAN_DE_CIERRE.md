# Cierre del Sprint 3 — 9 de noviembre de 2026

Entrega confirmada: martes 10 de noviembre. Se trabajó en `magallanes-sprint3-primer-avance`, a partir de `5c838da`. Se reutilizó Android de `origin/Rosales@93e5ea3`, sin modificar esa rama ni sustituir el servidor de Diego.

## Alcance y decisiones

El PDF `Sprint_3_plan_detallado_Android_y_Web.pdf` contiene 15 historias de las épicas 8, 9 y 10: 60 tareas. Angel: 30 tareas Android (.1/.2). Diego: 30 tareas web/servidor (.3/.4). Las 45 tareas del Sprint 1 pertenecen a otro alcance y no se suman aquí. Pagos y contratación de planes (épica 11) quedan fuera.

Diego autorizó reducir los commits y conservar Qwen3 con llama.cpp. Se reutilizaron repositorios, el servicio de pilares, la sesión existente, los componentes Android y los datos publicados por Rosales. Las vistas web usan ViewModel; las cargas compartidas tienen cancelación e identidad de cuenta. Los PDF usan una cola persistente de PostgreSQL y un proceso dedicado, sin añadir otro sistema de colas.

## Cinco commits de cierre

| Grupo | Mensaje exacto | Contenido |
|---|---|---|
| C1 | Se completaron los servicios compartidos del Sprint 3 | Históricos, conclusión validada, PDF vectorial, cola, etiquetas, recientes y pruebas. Migración nueva 0010, snapshots de precios y arranque local. |
| C2 | Se completaron los favoritos, las etiquetas y las empresas recientes en la web | Eliminación confirmada, refresco visible, aislamiento de sesión, gestor de etiquetas y visitas. Carga compartida mediante ViewModel. |
| C3 | Se incorporaron el rendimiento comparativo y los reportes PDF en la web | Gráfico normalizado, acceso Pro renovable, IA preparada, trabajos reanudables y descarga íntegra. |
| C4 | Se integró Android de Rosales con los servicios del Sprint 3 | Código móvil original atribuido a Rosales; adaptación del histórico común, origen de IA y estado observable de cuenta/selección. Pruebas JVM y de dispositivo. |
| C5 | Se documentaron las tareas y las pruebas de cierre del Sprint 3 | Matriz de 60 tareas, guía de exposición, procedencia y resultados reproducibles sin credenciales. |

Los códigos HU/T figuran en los cuerpos de los commits y en la matriz. Un commit puede cerrar o verificar varias tareas relacionadas. La cantidad de commits no representa la cantidad de tareas. No se generaron commits vacíos ni se cambiaron fechas anteriores. No se reescribió el historial publicado.

## Antes de presentar

1. Arrancar PostgreSQL/Redis y la demo con `tools/Start-Sprint3-Demo.ps1`; iniciar IA con `tools/Start-LocalAI.ps1` si no está activa. Ambos scripts verifican sus servicios y no sustituyen procesos ocupando los puertos.
2. Web: `http://127.0.0.1:55193/login`. API: `http://127.0.0.1:58020/api/v1/`. Las credenciales se conservan fuera de Git en `presentation_runtime_sprint3/demo_credentials.json`, en el directorio padre del repositorio.
3. En Android usar `apiBaseUrl=http://10.0.2.2:58020/api/v1/`. No abrir la demo anterior de Sprint 2 para mostrar estas funciones.
4. Mostrar Microsoft para ficha y análisis individual; Alphabet/Meta FY2024 para comparación con fechas coincidentes. Microsoft FY2024 cierra en otra fecha y permite explicar por qué no se declara un ganador frente a Alphabet.
5. Mantener explícitos los datos ausentes. Alphabet/Meta tienen tres pilares comparables; faltan componentes de solvencia y eficiencia. Los precios son snapshots hasta el 27 de octubre, no una cotización en vivo.
6. Revisar la matriz: código implementado, pruebas automatizadas y aceptación manual son evidencias distintas. No presentar una prueba de repositorio como si fuera una prueba de pantalla.
