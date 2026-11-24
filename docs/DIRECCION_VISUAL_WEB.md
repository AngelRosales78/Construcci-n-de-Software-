# Dirección visual: un cuaderno financiero

Rediseñar S&P 500 Finance como una publicación financiera contemporánea, con identidad propia y lectura clara para el jurado. Trabajar únicamente en la web.

La dirección combina papel cálido, tinta verde oscura y terracota. Newsreader da carácter a los titulares y Manrope mantiene legibles los controles. Las cifras usan alineación tabular. Las fuentes se sirven localmente, con sus licencias: [Newsreader](https://github.com/google/fonts/tree/main/ofl/newsreader) y [Manrope](https://github.com/google/fonts/tree/main/ofl/manrope).

La portada debe tener una composición editorial, el catálogo un ritmo ordenado y las fichas dar protagonismo a los datos. Los paneles de lectura son claros; los instrumentos gráficos conservan un fondo oscuro para distinguir el análisis. Los estados favorables, intermedios y de atención mantienen significado propio.

Reducir las tarjetas repetidas, los bordes excesivamente redondos, los degradados decorativos y los gráficos inventados. Usar jerarquía tipográfica, divisores y espacio con intención. Conservar todas las funciones, permisos, datos, fuentes y fechas. El diseño no acredita que una cotización sea en tiempo real.

Aplicar la identidad a acceso, inicio, navegación, catálogo, fichas, comparador, cuenta y administración. Comprobar lectura, contraste, teclado, estados de error y recorridos reales. No modificar Android ni crear commits por cada retoque visual.

## Resultado aplicado

Portada editorial con rutas reales al catálogo, al balance de Microsoft y al comparador. Nuevo monograma propio. Acceso y recuperación comparten una composición de dos columnas; el catálogo utiliza filas y divisores en lugar de tarjetas repetidas. Las fichas combinan lectura sobre papel con instrumentos oscuros. Cuenta, administración y avisos mantienen la misma identidad.

Las ventanas pequeñas reorganizan la web. El menú guarda el foco, permite recorrer sus enlaces con Tab, cierra con Escape y devuelve el foco al botón. Los cambios de ruta o de anchura cierran el menú y restauran el desplazamiento.

## Verificación

- 130 pruebas de la web aprobadas en 28 archivos; compilación de producción correcta.
- Recorridos reales con cuentas Free, Pro y administración: acceso, búsqueda, comparación y sus restricciones, favoritos, suscripción, usuarios, catálogo, cinco secciones de monitoreo, avisos y perfil. Sin errores de página.
- Conservación de sección, ejercicio, rango y filtros al navegar por las fichas.
- `tools/verify_editorial_design.cjs`: fuentes locales, validaciones de formulario, menú con teclado, diálogo de cuenta abierto y cancelado, vistas a 1440, 1024 y 390 píxeles. Quince muestras de texto cumplen contraste mínimo de 4,5:1. Es una comprobación dirigida, no una certificación completa de accesibilidad.
- Capturas y resultados locales en `../output/web_editorial/`; las verificaciones anteriores también están en `../output/sprint4_jury/` y `../output/web_ficha/`.

El rediseño no cambia datos financieros, permisos, Android ni los servicios de correo y notificaciones. Las limitaciones de esos servicios siguen siendo independientes del aspecto visual.
