# Forma de trabajo del Sprint 3

## Instrucción de trabajo

Trabaja sobre el plan Sprint 3 fechado el 30 de octubre. Diego Magallanes se encarga
de web y servidor; Angel Rosales de Android. Antes de modificar una función,
revisa el código existente y los contratos de la rama de Angel. Conserva las
funciones del Sprint 2 y no mezcles ramas con cambios incompatibles sin resolverlos.

Completa cada avance de extremo a extremo: datos reales disponibles, servidor,
pantalla y pruebas. Respeta la separación Model / ViewModel / View. No inventes
datos, fechas de trabajo, resultados de pruebas ni historias terminadas. Una
pantalla con servicios simulados no equivale a integración completada.

Actualización autorizada el 9 de noviembre: agrupar las tareas relacionadas en
cinco commits de cierre, con sus HU y códigos de tarea en el cuerpo. Esta decisión
sustituye la regla anterior de un commit por tarea. Conservar las fechas reales y
el historial publicado. No hacer cambios en la rama de Angel ni en la principal.

Explica cada función con palabras sencillas: qué permite hacer, dónde probarla,
qué debería ocurrir y qué falta. Entrega evidencia verificable, sin credenciales.

## Primer avance: martes 3 de noviembre

Hoy, lunes 2, se empieza por HU08_01 (seleccionar dos empresas) y HU10_01
(guardar favoritos), tal como prioriza el PDF. Se añade la validación del plan
como requisito del comparador, sin pagos ni ascenso de plan desde el navegador.

Orden: contratos y servidor → web → pruebas → commits y push → guía de exposición.
El objetivo es demostrar estas funciones; no declarar cerrado todo el Sprint 3.

## Revisión inicial

- Base de Diego: `f906282`, Sprint 2.
- Rama revisada de Angel: `origin/Rosales`, `93e5ea3`, 2 de noviembre.
- Angel tiene implementaciones de cliente para las 30 tareas Android. Su informe
  registra 132 pruebas JVM aprobadas y 3 omitidas; esas cifras son de su informe,
  no de una ejecución nueva de Diego. Falta comprobar integración con servidor
  y ejecución en emulador/dispositivo.
- Faltan las nuevas rutas del Sprint 3 en el servidor revisado.
- Las migraciones y funciones de servidor de ambas ramas divergen desde 0005.
  Se conserva la base de Diego y se respetan los contratos del cliente de Angel.

## Próximas entregas

El PDF tiene fecha de referencia, pero no fija la entrega. Diego confirmó avance
el 3 y entrega final el martes 10 de noviembre. Las prioridades del PDF son:
HU08_01, HU08_02, HU08_03, HU08_05, HU09_01, HU10_01, HU10_02 y HU10_03.
Primero se verifican estas ocho; las otras siete continúan pendientes hasta
implementar y probar cada una. El alcance completo sigue siendo 15 HU / 60 tareas.
