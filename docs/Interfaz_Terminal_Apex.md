# Interfaz basada en Terminal Apex

Referencia: `stitch_s_p_500_financial_app.zip` proporcionado por el usuario el martes 13 de octubre de 2026.

Se adaptaron las pantallas de acceso, exploración y ficha financiera a fondo oscuro, acentos verdes, tarjetas y cabecera del diseño. La ficha dispone el gráfico junto a las métricas en escritorio y apila sus secciones en móvil. El registro y login comparten una cabecera y navegación de acceso. Las reglas de sesión y consultas continúan en los ViewModels y repositorios existentes.

Los valores de ejemplo del HTML no se incorporaron como datos. El último cierre de la ficha procede de la respuesta API, con fecha y moneda. El explorador informa la cobertura financiera MSFT del balance 2024. Los bloques de cotización del índice, riesgo agregado, compra/venta, notificaciones y biometría de la maqueta no tienen implementación equivalente en la web; la adaptación utiliza las funciones y los datos disponibles.

`frontend/src/terminal.css` contiene el tema y los ajustes adaptables. `AuthHeader.jsx` contiene la cabecera común de acceso. `CompanyDashboard.jsx` distribuye las secciones y muestra el cierre importado; `PriceChart.jsx` conserva sus series y rangos con colores adaptados.

La evidencia visual se guarda en `docs/evidencias/terminal_apex_13_oct/`. `verify_layout.cjs` comprueba login, registro, inicio, búsqueda y ficha a 1440 y 390 píxeles; desplaza las tarjetas para cargar y verificar las imágenes diferidas. `verify_delivery.cjs` conserva el recorrido de acceso, filtros, historial, ficha, cuatro rangos, renovación y logout.

## Mejora de usabilidad aplicada el martes 13

Se aplicó `Prompt_mejora_interfaz.md`: etiquetas de indicadores disponibles o pendientes, tarjetas vacías explicadas, acceso directo a MSFT, sectores y rangos en español, fechas diferenciadas, ayudas desplegables de fuentes y fórmulas, y texto más legible. Altman-Z conserva el puntaje calculado y utiliza tres zonas de color con marcador limitado al intervalo visual 0–5.

Una etiqueta de indicadores disponibles significa que existe al menos una métrica válida de la respuesta; no certifica balance ni precios completos. Se mantienen los valores cero. La cobertura financiera de la demostración continúa limitada a Microsoft.

Pasaron 33 pruebas frontend, compilación, diez comprobaciones de pantallas en escritorio/móvil y el recorrido ampliado con ayudas desplegables y ficha AAPL sin datos. Evidencias: `docs/evidencias/usabilidad_13_oct/` e `integracion_usabilidad_13_oct.json`. En esta medición local el gráfico de 1254 puntos cargó en 544,23 ms. No se alteraron datos financieros ni contratos de la API.
