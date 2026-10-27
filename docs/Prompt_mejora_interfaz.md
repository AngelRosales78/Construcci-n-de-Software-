# Prompt para mejorar la interfaz financiera

Actúa como diseñador de producto y desarrollador frontend. Mejora esta aplicación de análisis de empresas para estudiantes y personas que empiezan a invertir. El usuario debe distinguir rápidamente el precio histórico, los indicadores del balance y los escenarios simulados.

Primero revisa el código y la interfaz real. Reduce el texto visible sin eliminar información necesaria para comprobar los cálculos. Organiza la ficha en Resumen, Salud financiera, Simulaciones y Explicación de IA. Mantén una sola sección principal visible y conserva los resultados al cambiar entre secciones.

Muestra primero el valor, su unidad y su fecha. Usa títulos cortos, espacio entre bloques y acciones claras. Deja fórmulas, fuentes, componentes, supuestos y definiciones en desplegables accesibles. Los ajustes de simulación son opcionales; generar escenarios debe funcionar con los valores históricos predeterminados.

Conserva MVVM, la autenticación, el ejercicio seleccionado, la caché y todos los estados de carga, error y datos ausentes. No conviertas ausencia de datos en cero, no presentes simulaciones como cotizaciones reales y no atribuyas a la IA cálculos que hace Django. Explica que el modelo local selecciona entre explicaciones financieras revisadas.

Comprueba los recorridos en Chrome a 1440 y 390 píxeles, con teclado, recarga y empresas sin datos. Verifica que los desplegables permiten demostrar las tareas originales y que cambiar de sección no vuelve a generar escenarios ni explicaciones. Ejecuta las pruebas pertinentes y entrega cambios, evidencias y mejoras que aún conviene abordar.
