# Prompt de diseño de la interfaz

Actúa como diseñador de producto y desarrollador frontend. Mejora la interfaz existente de la plataforma S&P 500 usando React y CSS del repositorio. Conserva la API, las fórmulas, los datos y las historias implementadas; trabaja en Magallanes.

**Objetivo:** facilitar la exploración de empresas y la lectura de sus métricas, perfil Altman-Z y precios históricos. El recorrido principal es iniciar sesión, buscar una empresa, filtrar o comparar resultados, abrir su ficha y entender qué muestran sus datos.

**Público asumido:** personas que comienzan a analizar empresas, inversores particulares y estudiantes con conocimientos financieros básicos. Necesitan lenguaje claro, explicaciones breves de indicadores, navegación sencilla y contexto sobre las fuentes. La interfaz debe ayudar a leer datos, sin presentar recomendaciones de compra o predicciones inexistentes.

**Dirección visual:** aplicación financiera sobria y cercana, con encabezado azul profundo, fondo claro, acentos verdes, tipografía de sistema legible y números bien alineados. Evita apariencia de maqueta, adornos excesivos, emojis, botones sin función y estadísticas inventadas. Usa logos locales verificados y una alternativa legible cuando falten.

**Búsqueda:** encabezado que explique para qué sirve la pantalla, barra de búsqueda protagonista, autocompletado accesible por teclado, historial lateral compacto, filtros claros y tarjetas de empresas con logo, ticker, nombre, sector y métricas. Muestra resultados y paginación sin saturar. Las tarjetas deben funcionar con ratón y teclado. Diseña estados de carga, vacío, error y dato ausente.

**Ficha:** empresa y sector visibles, regreso directo al explorador, cinco tarjetas para PER, ROE, capitalización, beta y dividendo; explicaciones al alcance del usuario. Separa la fecha del balance de la fecha de la cotización. Presenta el puntaje y clasificación Altman-Z con sus zonas, manteniendo los umbrales y colores semánticos. Da espacio al gráfico histórico y sus rangos 1M/6M/1Y/5Y, zoom y crosshair.

**Inicio:** muestra acciones reales disponibles y orienta hacia el explorador; elimina promesas visuales de IA, portafolios o alertas si no están implementadas. Reutiliza el acceso y cierre de sesión existentes.

**Criterios de aceptación:** diseño coherente en escritorio y móvil, foco visible, contraste legible, controles con etiquetas y navegación real. Nunca convertir un dato ausente en cero ni presentar el balance MSFT 2024 como actual. La cobertura es de 503 símbolos, 500 logos y una ficha financiera importada; deben conservarse estos límites. Verifica pruebas, compilación y recorrido real en navegador; inspecciona capturas de escritorio y móvil antes de entregar. Actualiza la lista de las tareas que se mostrarán al profesor y publica únicamente en Magallanes con autorización ya vigente.
