# Plan del sprint 2

Diego Magallanes y Angel Rosales | Proyecto S&P 500 | Duración propuesta de 8 días


## Qué queremos lograr

Al consultar una empresa, el usuario podrá entender sus cinco pilares financieros, ver un puntaje general, comparar tres escenarios futuros y leer una explicación de IA basada en esos datos.

Este es el plan de trabajo. Las historias se marcarán como terminadas cuando funcionen y tengan pruebas. El sprint 1 conserva su calificación y sus identificadores.

Total del proyecto: 15 épicas y 75 historias. Después del sprint 2 quedarían 40 historias de las épicas 8 a 15. El reparto indica en qué entrega van; no demuestra que estén aprobadas o completas.


## Quién hará cada parte

Diego desarrollará la web y, como reparto propuesto, los cálculos financieros y las simulaciones del servidor. Angel desarrollará Android y, como reparto propuesto, el servicio de IA y su validación. Ambos integrarán y probarán las tres partes.

El servidor hace los cálculos una sola vez. La web y Android consultan esos mismos resultados para mostrar cifras coherentes.


## Por dónde empezaremos

Ya existe una base web de consulta financiera y autenticación integrada. En Android hay pantallas financieras de ejemplo que requieren conexión real. Primero revisaremos los datos disponibles y esa conexión; luego construiremos pilares, puntaje, escenarios y explicaciones, en ese orden.


## Entender los cinco pilares

Épica 5. Un pilar es una forma de mirar las finanzas de una empresa. Los valores deben indicar de dónde salen y a qué periodo corresponden.

Ayuda a entender si la empresa puede cubrir sus deudas de corto plazo. Mostraremos liquidez corriente y prueba ácida con sus componentes.

Cómo lo comprobaremos: Comparar un cálculo manual con ambas pantallas; si falta un dato o el divisor es cero, explicar que no está disponible. Los colores tendrán umbrales explicados.

Muestra cómo la empresa genera ganancias mediante ROE, ROA y margen neto. Cada indicador incluirá fórmula y periodo.

Cómo lo comprobaremos: Comprobar cifras y redondeo, divisor cero y patrimonio negativo. La comparación sectorial solo aparecerá con una muestra suficiente, identificada y comparable.

Muestra deuda respecto del patrimonio y cobertura de intereses. Ayuda a entender cuánto depende la empresa de la deuda.

Cómo lo comprobaremos: Revisar componentes, fechas, umbrales y casos no aplicables. Diferenciar estos ratios del Altman Z que ya existe; no son el mismo indicador.


## Ejemplo para explicarlo

Si una empresa tiene activos corrientes de 200 y pasivos corrientes de 100, su liquidez corriente es 2. El sistema debe mostrar el mismo 2 en la web y en Android. Este ejemplo sirve para probar la fórmula; no se presentará como dato real de una empresa.


## Completar el análisis y el puntaje

Muestra cómo se aprovechan los activos y, cuando corresponda, los inventarios. Incluye rotación de activos y días de inventario, con hasta tres años disponibles.

Cómo lo comprobaremos: Comparar cálculos y periodos; no inventar años faltantes. Si el inventario no aplica o faltan componentes, mostrar el motivo.

Reúne Liquidez, Rentabilidad, Solvencia, Eficiencia y Crecimiento en un gráfico radar. Cada eje abre su detalle. Esta historia también incluye crecimiento anual de ingresos y crecimiento compuesto a tres años cuando haya datos.

Cómo lo comprobaremos: Mostrar los cinco ejes con notas y reglas versionadas. Para crecimiento compuesto a tres años se necesitan cuatro cierres anuales. Sin datos suficientes, indicar qué falta; toda comparación declara periodo y cobertura.

Combina las cinco notas en un resultado general. Antes de programarlo se acordarán y documentarán las ponderaciones y la forma de convertir los indicadores en notas.

Cómo lo comprobaremos: Reproducir un cálculo conocido, comprobar límites y el mismo redondeo en servidor y pantallas. Sin los cinco pilares, mostrar No disponible. No renombrar Altman Z como puntaje general.


## Qué mostrará cada integrante

Diego presentará los indicadores, el radar y el puntaje en la web. Angel mostrará los mismos resultados en Android, con carga, errores y datos ausentes. Las pantallas usarán los datos del servidor, sin repetir las fórmulas por separado.


## Comparar escenarios futuros

Épica 6. Son simulaciones a doce meses, no ganancias garantizadas. Usaremos un solo conjunto de trayectorias generadas por Monte Carlo, es decir, muchos caminos posibles calculados con los mismos supuestos.

Muestra la mediana de las trayectorias. Es el punto central de los resultados simulados.

Cómo lo comprobaremos: Guardar histórico, parámetros, fecha, semilla y versión. Repetir con los mismos datos y semilla debe dar el mismo resultado a doce meses.

Muestra el percentil 90 del mismo conjunto: una referencia favorable, no la ganancia máxima.

Cómo lo comprobaremos: Usar las mismas fechas, moneda y supuestos que el escenario base; comprobar la correspondencia con su ejecución.

Muestra el percentil 10 del mismo conjunto: una referencia desfavorable, no la pérdida máxima.

Cómo lo comprobaremos: Comprobar que el percentil 10 no supera la mediana y que las fechas coinciden. No presentarlo como soporte técnico de precio.

Permite modificar retorno anual supuesto y volatilidad mediante controles con límites visibles. Volatilidad significa cuánto varía el precio.

Cómo lo comprobaremos: Validar límites, recalcular los tres escenarios con el mismo motor y mostrar espera. Restablecer recupera los valores iniciales; no mezclar crecimiento de ventas con retorno bursátil.

Muestra las tres curvas en un solo gráfico y una tabla con sus precios finales.

Cómo lo comprobaremos: Mostrar percentiles 10, 50 y 90 de la misma ejecución. Ocultar una curva no recalcula los resultados. Usar igual horizonte y unidad y diferenciar histórico de proyección.


## Explicar los resultados con IA

Épica 7. La IA explicará las métricas que el sistema ya calculó. La conexión con un proveedor, sus credenciales y su disponibilidad se revisarán al inicio; una respuesta de ejemplo no contará como integración terminada.

Es la base de las otras historias de IA. Antes de publicar una respuesta, el servidor comprueba su estructura y contrasta sus cifras con las métricas autorizadas.

Cómo lo comprobaremos: Introducir una cifra falsa y comprobar su rechazo. Conservar origen, entrada, versión y resultado de validación sin secretos. Mostrar fuentes, fecha y análisis no disponible si falla.

Presenta hasta tres párrafos con la situación de la empresa, su periodo y sus fuentes.

Cómo lo comprobaremos: Probar carga, error y reintento. La caché se separa por empresa, fecha de datos, versión y nivel de lenguaje; no reutilizar una explicación de datos antiguos.

Presenta cinco explicaciones fáciles de leer, relacionadas con las métricas y su periodo.

Cómo lo comprobaremos: Comprobar las cinco secciones y la presentación segura del texto. Si un pilar no tiene datos, indicarlo sin inventar cifras.

Muestra hasta cinco puntos a favor y cinco riesgos sustentados por los datos.

Cómo lo comprobaremos: Aceptar listas cortas si hay poca evidencia. Rechazar cifras falsas o respuestas inválidas; no completar puntos solo para llenar la pantalla.

Permite elegir cómo leer la explicación y conserva la selección durante la consulta.

Cómo lo comprobaremos: Ambos niveles mantienen cifras, periodos y fuentes coherentes. Separar caché por nivel y mostrar carga; si falla el cambio, conservar el último análisis válido.


## Organización de los ocho días

El día 1 es el inicio que acuerde el equipo. Esta secuencia organiza una meta exigente; las estimaciones y dependencias deben revisarse antes de comprometer las 16 historias.

Al cerrar cada día, ambos revisarán lo integrado. Si los datos, el proveedor de IA o la conexión Android impiden avanzar, se registrará el bloqueo y se revisará el compromiso; ninguna historia se dará por terminada para mantener el calendario.


## Seguimiento y presentación


## Tareas y estimación inicial

Estas horas persona son rangos orientativos para preparar la conversación, no horas trabajadas ni resultados de Planning Poker. Una hora de trabajo de cada integrante suma dos horas persona.

En la planificación, ambos estimarán cada historia por separado, compararán sus votos de Planning Poker y guardarán evidencia real del acuerdo. Luego dividirán las tareas en horas por persona. El calendario de ocho días no sustituye esa validación.


## Cuándo una historia está terminada

Debe cumplir sus criterios, funcionar en web y Android cuando corresponda y usar el servidor real. Ambos revisarán el resultado y guardarán la prueba, el commit y la fecha. Una pantalla hecha sin conexión todavía está en proceso.

Estados de seguimiento: Pendiente, En proceso, En prueba, Terminada o Bloqueada. Las 16 historias de este plan comienzan como Pendiente de verificación; este documento no acredita su implementación.

Cada día se registrarán horas reales y trabajo restante. Con ese registro se construirá el burndown, un gráfico de lo que falta. Los commits describirán una mejora concreta, por ejemplo HU05_02 calcular liquidez corriente. El avance se mantendrá local hasta autorizar su publicación.


## Guía para explicarlo al profesor

En el sprint 2 trabajaremos 16 historias: la pendiente de la épica 4 y las épicas 5, 6 y 7 completas. Primero analizaremos las finanzas, después simularemos escenarios y finalmente explicaremos los resultados con IA. Diego se encargará de la web y Angel de Android; repartiremos también el trabajo del servidor.


## Demostración en cuatro pasos

1. Abrir la misma empresa en web y Android. Mostrar fuente y periodo, un cálculo comprobable, los cinco pilares y el puntaje.

2. Mostrar base, optimista y pesimista. Cambiar un supuesto, recalcular y restablecer los valores originales.

3. Leer el resumen de IA, abrir sus fuentes y cambiar de lenguaje sencillo a técnico manteniendo las cifras.

4. Mostrar qué ocurre cuando falta un dato o falla el servicio. Presentar resultados de pruebas y commits de las historias realmente terminadas.


## Preguntas que debemos poder responder

¿Qué es una épica? Un grupo de funciones relacionadas. ¿Qué es una historia? Una necesidad concreta del usuario. ¿Qué es una tarea? El trabajo necesario para construir esa historia.

¿Por qué el puntaje se hace después de los pilares? Porque se calcula a partir de ellos. ¿Por qué la validación de IA va antes del resumen? Porque primero debemos comprobar que sus cifras tienen respaldo.

¿Qué faltará después? Las 40 historias de las épicas 8 a 15, repartidas entre los sprints 3 y 4. El orden dentro de cada sprint respetará sus dependencias; por ejemplo, los permisos de planes deben existir antes de habilitar funciones Pro.


## Qué presentaremos al cierre

Una tabla con historia, resultado, responsable, fecha, prueba y commit; capturas de web y Android; resultados de pruebas; burndown basado en el registro diario; y una conclusión breve sobre lo logrado y lo pendiente. Los datos de cierre se completarán durante el trabajo, sin inventar resultados.


## Pendientes que se revisan por separado

La recuperación de contraseña del sprint 1 pasó las pruebas con correo local. La entrega a una bandeja externa sigue pendiente de configurar el proveedor. La huella conserva la validación previa y lo informado por el equipo; no se atribuye una nueva prueba en un sensor físico a este plan.

## Historias y criterios completos

### HU04_05
Como usuario, quiero visualizar un puntaje numérico unificado sobre la salud general (0 a 100).

- Agrega las cinco notas de pilares mediante una fórmula versionada y documentada.
- El resultado está entre 0 y 100 y el redondeo es igual en API e interfaz.
- Sin cobertura de los cinco pilares muestra No disponible y explica qué falta; no sustituye Altman-Z.

### HU05_01
Como usuario, quiero explorar el desglose de los 5 pilares financieros de una empresa.

- El radar muestra Liquidez, Rentabilidad, Solvencia, Eficiencia y Crecimiento, con notas versionadas o No disponible.
- Esta HU incluye Crecimiento: variación anual de ingresos y tasa compuesta a tres años si hay datos.
- Cada eje abre su detalle; las comparaciones declaran cobertura y periodo.

### HU05_02
Como usuario, quiero analizar el Pilar de Liquidez con ratios de corto plazo.

- Muestra liquidez corriente y prueba ácida con componentes y periodo.
- Denominador cero o datos insuficientes muestran No disponible.
- Los colores usan umbrales configurados y explicados; no un óptimo universal.

### HU05_03
Como usuario, quiero evaluar el Pilar de Rentabilidad examinando márgenes y retorno.

- Muestra ROE, ROA y margen neto con fórmula y periodo.
- La referencia sectorial declara muestra y fecha y se omite si es insuficiente.
- Explica denominadores cero y patrimonio negativo.

### HU05_04
Como usuario, quiero revisar el Pilar de Solvencia para entender la estructura de deuda.

- Muestra Deuda/Patrimonio y Cobertura de Intereses con componentes y fecha.
- Documenta umbrales y casos no aplicables.
- Distingue este detalle del gauge Altman-Z.

### HU05_05
Como usuario, quiero inspeccionar el Pilar de Eficiencia Operativa respecto a la rotación de activos.

- Muestra rotación de activos y días de inventario con definición y periodo.
- Presenta hasta tres años disponibles sin inventar los años faltantes.
- Si el inventario no aplica o falta el denominador, informa No disponible y explica el motivo.

### HU06_01
Como usuario, quiero consultar la proyección del Escenario Base para estimar retornos esperados.

- Genera el escenario base a doce meses como mediana de un conjunto común de trayectorias Monte Carlo.
- La simulación registra histórico utilizado, parámetros, fecha, semilla y versión del modelo.
- Con los mismos datos y semilla obtiene el mismo resultado; se identifica como simulación, no predicción garantizada.

### HU06_02
Como usuario, quiero visualizar el Escenario Optimista bajo condiciones favorables.

- La curva optimista corresponde al percentil 90 de las mismas trayectorias de HU06_01.
- Comparte fechas, moneda y supuestos con el escenario base.
- Se explica como percentil de la simulación, no ganancia máxima.

### HU06_03
Como usuario, quiero analizar el Escenario Pesimista para entender los riesgos de caída.

- La curva pesimista corresponde al percentil 10 de las mismas trayectorias de HU06_01.
- Comparte fechas y parámetros con el resto de escenarios.
- No se presenta como pérdida máxima ni como nivel de soporte técnico.

### HU06_04
Como usuario, quiero manipular variables mediante deslizadores para recalculaciones personalizadas.

- Permite ajustar retorno anual supuesto y volatilidad anual dentro de límites visibles.
- Envía los parámetros al mismo motor que genera los tres escenarios y muestra el tiempo de espera.
- Restablecer recupera los parámetros originales; no se mezclan crecimiento de ventas y retorno bursátil sin un modelo explícito.

### HU06_05
Como usuario, quiero comparar gráficamente los tres escenarios en un mismo plano.

- Muestra percentiles 10, 50 y 90 de una misma ejecución en un gráfico.
- Permite ocultar curvas sin alterar el resultado y muestra una tabla final.
- Todas las curvas comparten horizonte y unidad; el histórico y la proyección se distinguen.

### HU07_01
Como usuario, quiero leer un resumen ejecutivo generado por IA sobre la situación de la empresa.

- Entrega hasta tres párrafos basados en las métricas disponibles, con periodo y fuentes identificables.
- Muestra carga y permite recuperarse de error o tiempo de espera agotado.
- La caché se identifica por empresa, fecha de datos, versión y nivel de lenguaje; no se promete una respuesta nueva en tres segundos.

### HU07_02
Como usuario, quiero recibir explicaciones en lenguaje claro redactadas por IA sobre los 5 pilares.

- Explica por separado los cinco pilares con métricas disponibles y periodo.
- Un pilar sin datos se identifica como insuficiente sin inventar explicación numérica.
- El formato de texto se presenta de forma segura y legible.

### HU07_03
Como usuario, quiero ver una lista de pros y contras elaborada por IA sobre la inversión.

- Presenta hasta cinco fortalezas y cinco riesgos respaldados por las métricas disponibles.
- Si no existen tres puntos sustentables, no se obliga al modelo a completarlos.
- Valida estructura y contenido; una respuesta inválida no se publica como análisis verificado.

### HU07_04
Como usuario, quiero consultar la procedencia y validación del análisis de IA para evaluar la confiabilidad de sus explicaciones.

- Toda cifra publicada debe coincidir con una métrica autorizada o un cálculo verificable.
- Las discrepancias y respuestas inválidas se bloquean o muestran como análisis no disponible.
- Se conserva la procedencia de los datos; no se promete ausencia absoluta de errores ni se califica de auditado un dato sin auditoría.

### HU07_05
Como usuario, quiero alternar el lenguaje de las explicaciones de IA entre un nivel simple y uno técnico.

- Permite elegir Sencillo o Técnico y conserva la selección al consultar.
- Ambos niveles mantienen cifras, periodo y fuentes coherentes.
- Cambiar nivel muestra carga y maneja errores sin borrar el último análisis válido.

## Calendario de integración

- Día 1: Plan y datos suficientes para comenzar.
- Día 2: HU05_02, HU05_03 y HU05_04 integradas.
- Día 3: HU05_05, HU05_01 y HU04_05 integradas.
- Día 4: HU06_01 a HU06_03 y base HU07_04.
- Día 5: HU06_04 y HU06_05 integradas.
- Día 6: HU07_01, HU07_02 y HU07_04 integradas.
- Día 7: HU07_03 y HU07_05 integradas.
- Día 8: Revisión conjunta y demostración.
