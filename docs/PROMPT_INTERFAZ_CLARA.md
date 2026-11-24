# Prompt de trabajo: una ficha financiera clara

Actúa como diseñador senior de UI/UX y arquitecto de información. Trabaja sobre la web existente, inspecciona su código y las pantallas reales antes de editar.

## Contexto y objetivo

La aplicación permite explorar empresas, consultar precios y balances, interpretar indicadores, simular escenarios y exportar informes. Sus usuarios incluyen estudiantes y personas con poca experiencia financiera. Diego debe poder entender los cambios y explicarlos ante un jurado.

Primera pantalla: la ficha de empresa, en Resumen y Salud financiera. La acción principal es identificar los indicadores disponibles y elegir qué explorar. El objetivo de diseño es que en unos cinco segundos se reconozcan empresa, precio histórico, disponibilidad y siguiente acción; no afirmes haber validado ese tiempo sin observar usuarios.

## Criterios de diseño

1. Prioriza nombre, precio con fecha, navegación, valores y estados. Usa títulos cortos y una guía de una línea por sección.
2. Conserva visibles las diferencias entre datos históricos y actuales, referencias normalizadas y cierres exactos, ausencia de datos y ceros reales. No conviertas falta de información en un estado favorable.
3. Mueve definiciones, fórmulas, métodos, componentes y fuentes detalladas a desplegables con nombres claros. Conserva fecha y significado de cada indicador. Las ayudas deben funcionar con teclado y toque, sin depender de pasar el ratón.
4. Agrupa cifras en tarjetas legibles, con espacios consistentes y estados expresados con texto además de color. Evita repetir la misma explicación en varias zonas.
5. Mantén la identidad editorial existente: crema, verde oscuro y acentos terracota. Mejora jerarquía, contraste y espaciado sin añadir adornos ni bloques de texto de relleno.
6. Ofrece información corporativa, fuentes y exportación desde una zona secundaria accesible. Conserva advertencias de empresa inactiva o pertenencia al índice no verificada.
7. En Salud financiera, muestra el año, el estado y los pilares; deja reglas y componentes técnicos disponibles bajo demanda. Altman-Z y el puntaje de los cinco pilares deben distinguirse.
8. Revisa escritorio, tableta y pantalla estrecha: sin desbordamientos, controles accesibles y avisos críticos visibles en todos los tamaños.

## Alcance y entrega

Modifica la web, conserva cálculos, datos, permisos, PDF y contratos del servidor. Respeta el trabajo Android. Usa la rama existente y un commit por este bloque terminado.

Comprueba las interacciones reales, cambia de sección y rango, abre ayudas con teclado y genera un PDF. Ejecuta pruebas pertinentes y compilación; no alteres pruebas para ocultar fallos.

Entrega una explicación breve: qué se ve primero, qué se resumió y dónde quedó el detalle. Señala el alcance real y las limitaciones. Incluye una vista previa y el estado del commit.
