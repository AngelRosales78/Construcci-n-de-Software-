# Rediseño para una primera visita

## Problema y objetivo

Una persona nueva no entendía qué mirar, no distinguía las zonas de Altman-Z, desconocía para qué servían los avisos y no encontraba la IA. El cambio prioriza orientación y significado antes que cifras aisladas.

La dirección aplicada utiliza fondos claros, azul para navegación y ayuda, acentos cálidos y colores específicos para clasificaciones. Source Sans 3 sustituye la tipografía principal; el nombre y el símbolo de la zona acompañan al color.

## Cambios visibles

- **Inicio:** búsqueda directa, accesos a empresas conocidas, fotografía temática y recorrido opcional de tres pasos. La primera acción se mantiene visible a 360, 390, 768 y 1440 px.
- **Altman-Z:** primero aparecen nombre de zona, símbolo y significado. Los tres rangos permanecen visibles y la zona actual queda marcada. El puntaje es un dato de respaldo, sin convertirlo en una escala sobre cien ni en una recomendación de compra.
- **Datos faltantes:** mantienen estado gris y el motivo concreto. No reciben una clasificación ni un puntaje inventado.
- **IA contextual:** «Explícame este resultado» abre un panel junto a la ficha. La ayuda de PER, beta, dividendos, valor en bolsa y ROE publicado utiliza sus fechas y fuentes propias. El comparador ofrece «Explícame las diferencias».
- **IA desde otras pantallas:** botón visible en escritorio y acceso en la cabecera del celular. Cuando no hay una empresa abierta, primero se selecciona una. El panel contextual muestra la respuesta antes que las herramientas secundarias.
- **Alertas:** «Mis avisos» pasa a «Alertas de tus empresas». Explica el recorrido favoritos → preferencias → cambio de clasificación. Aclara que no son alertas de precio ni un seguimiento en tiempo real.
- **Movimiento:** entradas breves del resultado y del panel, sin simular cifras intermedias. Se respeta la preferencia de movimiento reducido.

## Integridad de la información

Las fórmulas, umbrales, puntajes y reglas de notificaciones permanecen intactos. Los campos publicados en la ficha se explican con su referencia independiente del balance anual. Se conserva un cero real y no se presenta como cero un valor pequeño disponible. La advertencia de fecha anual normalizada se muestra cuando corresponde a los datos utilizados; no se atribuye al Altman-Z de otro cierre.

Los contratos de Android y los pagos no se modifican. No se incorporan proveedores de IA externos.

## Recursos visuales

Las fotografías son temáticas: ilustran actividades económicas y no se presentan como instalaciones de las empresas del catálogo. Se almacenan localmente, con origen y SHA-256 en `frontend/public/editorial-images/sources.json`; su licencia se consulta en [Unsplash](https://unsplash.com/license).

La fuente original se conserva sin modificar, junto con su licencia OFL, en `frontend/public/fonts/`. Su origen oficial y SHA-256 constan en `sources.json`.

## Verificación realizada

- 160 pruebas de la web aprobadas y compilación de producción correcta.
- 52 pruebas del servidor aprobadas para el asistente y las validaciones de análisis afectadas.
- `tools/verify_company_reading.cjs`: lectura, foco, fuentes, valores parciales y clasificación visible antes del detalle.
- `tools/verify_first_visit.cjs`: búsqueda y guía, zonas reales favorable/intermedia/atención y estado sin datos; ayuda contextual con la IA local real, consulta de PER, elección de empresa, cierre con Escape, retorno del foco, alertas y movimiento reducido.
- Capturas a 360, 390, 768 y 1440 px sin desbordamientos. Resultados fuera de Git en `../output/first-visit/`.

Las comprobaciones automáticas verifican funcionamiento, presentación y visibilidad; no sustituyen una prueba de comprensión con una persona nueva.

## Cómo repetir la prueba de comprensión

Abre [el inicio local](http://127.0.0.1:55193/dashboard) y pide a alguien que entre sin explicaciones previas. Dale tres tareas:

1. Buscar Apple y decir qué significa el resultado del balance, sin limitarse a leer el número.
2. Encontrar dónde pedir una explicación de ese resultado.
3. Explicar qué cambios recibiría en «Alertas de tus empresas» y cómo empezar a seguir una.

Observa dónde se detiene, qué interpreta por su cuenta y qué necesita preguntar. Esas observaciones permiten ajustar el diseño con evidencia de uso real.
