# FINDSCORE · Interfaz web

## Objetivo y alcance

Aplicar a la web la composición aprobada: un resultado principal, accesos claros a las cifras y ayuda de IA contextual. El nombre visible es **FINDSCORE**. S&P 500 se conserva únicamente al describir el catálogo o la pertenencia al índice.

Este trabajo no modifica Android/Kotlin, las reglas financieras, los datos, los planes ni el método de pago. Los PDF nuevos usan FINDSCORE en título, pie y metadatos; los archivos ya emitidos conservan su contenido.

## Criterios de diseño

- Crema `#f4f1e8`, verde bosque `#203b2f`, salvia `#e9ede1` y terracota `#99472f`.
- Bitter local para títulos principales; Source Sans 3 para lectura y controles. Se conservan los archivos de licencia de las fuentes.
- Una misma navegación, tratamiento de formularios, tablas, mensajes y tarjetas en las rutas públicas, personales y administrativas.
- En la ficha: empresa → clasificación Altman-Z → liquidez/deudas/rentabilidad → ayuda de IA. El negocio, las fuentes, los precios, los indicadores y los reportes siguen disponibles.
- El puntaje conserva su valor real, su fecha y sus límites. La clasificación pendiente no recibe puntuación, zona favorable ni un cero artificial.
- La IA abre el asistente existente. Se elimina el botón flotante del espacio web; sigue accesible desde navegación, cabecera y los resultados pertinentes.

## Movimiento y accesibilidad

Transiciones de interacción de 140 ms; entrada de página de 180 ms, cambio de panel de 160 ms y apertura del asistente de 200 ms. No hay decoraciones animadas en bucle ni retrasos deliberados. `prefers-reduced-motion: reduce` elimina animaciones y transiciones.

Se conserva el uso por teclado, el enlace para saltar al contenido, el cierre con Escape y el retorno del foco en menú y asistente. Los resultados usan etiquetas y símbolos además del color. Las tablas conservan desplazamiento interno en pantallas estrechas.

### Respuesta a las acciones

- Tarjetas: entrada de 200 ms con escalonado máximo de 100 ms; elevación de 3 px únicamente con ratón, sin desplazar el resto de la cuadrícula.
- Pestañas: el subrayado se despliega en 180 ms, conservando la navegación y el contenido existentes.
- Favoritos: la estrella responde durante 320 ms únicamente tras confirmar un guardado solicitado en esta pantalla. Cargar un favorito anterior o recibir un error no activa la confirmación. Los mensajes conservan texto legible y no desaparecen automáticamente.
- Carga: estructuras neutras, sin cifras ficticias y ocultas a los lectores de pantalla; el estado de carga sí se anuncia. El brillo hace dos pasadas y después queda estático.
- Los botones principales responden a la pulsación con un desplazamiento de 1 px. No se animan los valores financieros ni se agregan efectos a los gráficos.

Todos estos efectos respetan movimiento reducido y no añaden solicitudes ni retrasos a las operaciones.

### Movimiento en el recorrido completo

- Buscador y selectores de empresas: aparecen las sugerencias sin retrasar la selección con flechas/Enter. El indicador de consulta existe únicamente durante una búsqueda real. La etiqueta del sector se anima cuando cambia.
- Gráficos: entrada de opacidad de 220 ms al cargar una serie. Consultar otra sesión o esconder una curva no vuelve a animar la serie ni altera sus valores. Highcharts conserva desactivada la animación de datos.
- Asistente: indicador de espera con texto explícito y puntos decorativos ocultos al lector de pantalla. Las respuestas se muestran completas, con una entrada de 180 ms; no se simula escritura. Cambios, lecciones y evaluación comparten la misma entrada.
- Guía: cada paso tiene entrada de 180 ms y el marcador de paso cambia suavemente. Cerrar mantiene el retorno del foco.
- Formularios: el estado ocupado se comunica con texto y `aria-busy`; la línea de actividad solo existe mientras se procesa la operación. Los errores mantienen su texto y borde de identificación. Perfil y preferencias usan confirmaciones del servidor.
- Campana: un único movimiento de 320 ms al observar un aviso nuevo real y sin leer. La carga inicial, los ejemplos, marcar leído y cambiar de cuenta no producen el movimiento; no se modifica el contador para animarlo.
- Diálogos: apertura y cierre mediante transiciones nativas de 160–180 ms en navegadores compatibles. Se conserva la apertura existente como alternativa; no hay temporizadores de cierre que retrasen la acción o el foco.
- Las esperas reales pueden mantener un indicador indeterminado. Los efectos decorativos son breves o tienen repeticiones limitadas. Con movimiento reducido desaparecen todas las animaciones, incluidos pseudo-elementos y fondos.

## Acceso y registro

El acceso usa un panel editorial del 42 % y una región de formulario del 58 %. La tarjeta tiene espacio interior real y un máximo de 520–560 px en acceso y 660–720 px en registro. Se eliminó la cabecera repetida de la región derecha. Las etiquetas son de 15 px, las entradas de 16 px y los controles de 50 px.

El registro mantiene todos sus campos y validaciones: «Sobre ti» agrupa nombre y apellido opcionales; «Acceso a tu cuenta» agrupa usuario, correo, perfil y contraseñas. Los campos se distribuyen en dos columnas donde hay espacio y en una columna en teléfonos. En pantallas de menos de 900 px, el panel editorial se reduce a la marca para dar prioridad al formulario. No se modifica el zoom del navegador ni se usa escala artificial.

`node tools/verify_auth_design.cjs` comprueba las tres pantallas de acceso, registro y recuperación entre 320 y 1920 px, validación de campos, mostrar/ocultar contraseña y movimiento reducido. No envía credenciales ni crea cuentas. Capturas: `../output/findscore-auth/`.

## Navegación financiera

Los tres accesos del resumen abren el pilar correspondiente, conservando ejercicio fiscal, rango de precios y dirección de retorno. El enlace «Ver cálculo y fuentes» abre Salud financiera y lleva al bloque Altman-Z. Cambiar de sección no vuelve a consultar la ficha ni registra otra visita.

## Verificación

### Revisión de proporciones y controles

El explorador muestra los filtros de sector y orden juntos. En pantallas de hasta 1100 px se omiten los accesos de sector duplicados; todos los sectores siguen disponibles en el selector. El aviso del catálogo conserva su contenido y el anuncio de orden sigue disponible para lectores de pantalla. Las tarjetas integran el botón de favoritos dentro de un único contenedor.

El comparador reduce el tamaño de los selectores sin esconder los campos de búsqueda. Perfil agrupa nombre y apellido donde hay espacio y mantiene el formulario en un ancho de lectura cómodo. Favoritos reúne el contador, sincronización y actualización. Alertas muestra primero la lista y sus acciones; la explicación completa queda después. Los controles nativos usan el esquema claro de FINDSCORE.

`node tools/verify_interface_polish.cjs` comprueba siete pantallas en 1440, 768, 390 y 360 px, ausencia de desbordamiento, esquema claro y conservación de consulta, disponibilidad y orden al filtrar. Guarda capturas en `../output/interface-polish/after/`. En la revisión a 390 px, la primera empresa pasó de 1053 a 620 px desde el inicio, y cada selector del comparador pasó de 277 a 223 px de altura.

Desde `frontend`: `node node_modules/vitest/vitest.mjs run` y `node node_modules/vite/bin/vite.js build`.

Desde la raíz del repositorio, con los servicios locales disponibles: `node tools/verify_findscore_design.cjs`.

El recorrido visual revisa acceso, registro, recuperación, inicio, explorador, favoritos, alertas, preferencias, perfil, suscripción, comparación, las cuatro secciones de empresa y administración. Comprueba ausencia de desbordamiento, navegación a pilares, simulación con el histórico existente, apertura/cierre del asistente, teclado, movimiento reducido, clasificación pendiente y recuperación de un error de red.

Las capturas reales y el resultado del recorrido se guardan en `../output/findscore-design/`, fuera de Git. La comprobación usa cuentas locales privadas sin imprimir ni versionar contraseñas.

## Prompt de mantenimiento

> Conserva la identidad FINDSCORE y el diseño aprobado: crema, verde bosque, terracota y títulos Bitter. En cada pantalla define un foco principal, reduce contenedores y acciones repetidas, y mantén disponibles los datos y las funciones existentes. Usa animaciones cortas para orientar, respetando movimiento reducido. Verifica escritorio, móvil, teclado, estados de carga/error/vacío y fechas/fuentes. No inventes métricas, no cambies las reglas financieras y no modifiques Android.
