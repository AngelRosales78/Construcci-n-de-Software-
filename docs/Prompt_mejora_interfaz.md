# Prompt de mejora de la interfaz

Actúa como diseñador de interfaces y desarrollador React. Mejora la web S&P 500 Finance para estudiantes e inversores principiantes que necesitan encontrar empresas e interpretar información histórica sin conocimientos financieros avanzados.

Conserva el tema Terminal Apex, las funciones existentes y MVVM. Trabaja en Magallanes. No modifiques secretos, bases de datos, dependencias locales ni las ramas del compañero. No añadas cotizaciones, indicadores o funciones de compra simuladas.

Prioriza estas mejoras:

1. Identifica en las tarjetas si existen indicadores disponibles usando los valores recibidos de la API, conservando los ceros válidos. Cuando falten todos, explica que la empresa está en el catálogo y su información financiera está pendiente de carga. No infieras cobertura de precios o balance completo a partir de una métrica.
2. Ofrece un acceso directo a Microsoft como ficha de referencia de esta demostración, sin ocultar el resto del catálogo ni filtrar únicamente la página actual.
3. Separa la fecha del balance de la fecha del cierre importado. Explica que cambiar el rango del gráfico no modifica las tarjetas financieras.
4. Sustituye abreviaturas poco claras por rangos legibles; explica cierre ajustado, datos diarios y sesiones disponibles. Mantén la consulta por rangos y el zoom.
5. Muestra Altman-Z con tres zonas, un marcador limitado a la escala y el puntaje real sin truncar. Explica sus componentes y su límite académico. No presentes el puntaje como porcentaje ni garantía.
6. Permite consultar fuentes y fórmulas con controles desplegables accesibles. Usa textos breves, español y números legibles; mejora los estados vacíos, el foco del teclado y la adaptación móvil.

No amplíes la cobertura de datos como parte de este cambio visual. Verifica regresiones frontend, compilación, navegación real, pantallas de escritorio/móvil y una ficha sin datos. Conserva evidencias y documenta los cambios concretos. Guarda y sube los cambios exclusivamente a Magallanes conforme a la autorización de la sesión.
