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

## Navegación financiera

Los tres accesos del resumen abren el pilar correspondiente, conservando ejercicio fiscal, rango de precios y dirección de retorno. El enlace «Ver cálculo y fuentes» abre Salud financiera y lleva al bloque Altman-Z. Cambiar de sección no vuelve a consultar la ficha ni registra otra visita.

## Verificación

Desde `frontend`: `node node_modules/vitest/vitest.mjs run` y `node node_modules/vite/bin/vite.js build`.

Desde la raíz del repositorio, con los servicios locales disponibles: `node tools/verify_findscore_design.cjs`.

El recorrido visual revisa acceso, registro, recuperación, inicio, explorador, favoritos, alertas, preferencias, perfil, suscripción, comparación, las cuatro secciones de empresa y administración. Comprueba ausencia de desbordamiento, navegación a pilares, simulación con el histórico existente, apertura/cierre del asistente, teclado, movimiento reducido, clasificación pendiente y recuperación de un error de red.

Las capturas reales y el resultado del recorrido se guardan en `../output/findscore-design/`, fuera de Git. La comprobación usa cuentas locales privadas sin imprimir ni versionar contraseñas.

## Prompt de mantenimiento

> Conserva la identidad FINDSCORE y el diseño aprobado: crema, verde bosque, terracota y títulos Bitter. En cada pantalla define un foco principal, reduce contenedores y acciones repetidas, y mantén disponibles los datos y las funciones existentes. Usa animaciones cortas para orientar, respetando movimiento reducido. Verifica escritorio, móvil, teclado, estados de carga/error/vacío y fechas/fuentes. No inventes métricas, no cambies las reglas financieras y no modifiques Android.
