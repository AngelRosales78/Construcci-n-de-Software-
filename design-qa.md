# FINDSCORE · Comprobación del movimiento

final result: passed

## Referencia y alcance

Se conserva la interfaz existente y se mejora su respuesta al cursor y la entrada de Inicio y del asistente. No se crea un producto ni un diseño alternativo. Las capturas de referencia se obtuvieron antes de esta modificación, en la misma sesión de revisión.

- Fuente visual: `../output/findscore-motion/before/`.
- Implementación: `../output/findscore-motion/after/`.
- Comparación: `02-boton-hover.png`, `04-empresa-hover.png` y `06-asistente.png`, abiertos juntos en la misma entrada de comparación visual.
- Ventana y capturas: 1440 × 1000 px, densidad 1; misma cuenta, rutas, contenido y estados de cursor/apertura. Sin normalización de densidad.
- Vista táctil adicional: 390 × 1000 px, sin efectos exclusivos de ratón.

## Revisión visual

1. Inicio y botón: Bitter, tamaños, saltos de línea, colores y espaciado se conservan. El botón tiene sombra y elevación de 2 px al pasar el cursor; no desplaza el buscador.
2. Sectores y empresas: contenedor completo con borde y profundidad, sin separación del botón de favoritos. Las etiquetas y valores siguen visibles; los logos y fotografías son los recursos existentes.
3. Ficha y asistente: se conserva el contenido financiero y el panel abierto tiene la misma composición. La apertura se prolonga a 360 ms para hacer visible la transición; no altera el tiempo de la respuesta de IA.

Se comprobaron las cinco superficies: tipografía, distribución, colores, calidad de recursos y texto. No se detectó deriva de composición en las capturas comparadas. Las capturas completas permiten leer controles, etiquetas y valores; no fue necesario recortar otra región. Las imágenes estáticas no demuestran la duración: se complementan con el vídeo y comprobaciones de transformaciones reales.

## Incidencia corregida

- P2: la animación de entrada retenía `transform` al finalizar e impedía la elevación del contenedor. Se cambió su relleno a `backwards`; la comprobación posterior midió una elevación de 4 px y el retorno sin alterar la distribución. Evidencia: `after/04-empresa-hover.png` y `after/resultado.json`.

## Interacción y límites

Se verificaron cursor, apertura/cierre del asistente, Escape, retorno del foco, movimiento reducido y ausencia de desbordamiento en móvil. No se realizaron cobros ni se modificaron datos financieros. El recorrido no envía preguntas a la IA y no mide su latencia. No se afirma conformidad completa de accesibilidad.

Comprobación funcional: 162 pruebas de frontend y recorrido general de FINDSCORE sin errores de página. El archivo de vídeo está fuera de Git: `../output/findscore-motion/after/FINDSCORE-animaciones.webm`.
