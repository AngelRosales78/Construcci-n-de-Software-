# Asistente financiero de la web

## Cómo demostrar las seis funciones

1. Abre Microsoft, entra en **Explicación de IA** y selecciona **2024**. Se abre el chat. Escribe «Hola»: responde con un saludo y no muestra indicadores. En **Otras herramientas → Lectura rápida** siguen disponibles las tarjetas de qué destaca, qué merece atención y qué falta conocer.
2. En **Preguntar**, pulsa «¿Cómo están sus deudas?». La respuesta utiliza los indicadores de solvencia de Microsoft y ese ejercicio.
3. Escribe «¿Podrías explicarlo más?». Mantiene el tema anterior y añade un ejemplo identificado como ficticio. Puedes reiniciar la conversación sin cambiar la empresa.
4. En **Otras herramientas → Cambios entre años**, selecciona 2023 y compara con 2024. Los cambios se calculan en el servidor; los porcentajes se expresan en puntos porcentuales. Con referencias anuales normalizadas, como AAPL 2025 frente a 2024, se explica por qué no se declara una comparación contable válida.
5. En **Otras herramientas → Aprender**, abre una lección y responde su pregunta. El servidor comprueba la opción elegida y muestra una explicación. Los ejemplos no son datos de la empresa abierta.
6. En el comparador Pro, selecciona GOOGL y META, ejercicio 2024. Abre **Entiende la comparación** y pide la lectura o escribe una pregunta. Las fechas y versiones del comparador original determinan qué notas se pueden explicar como comparables.

Las preguntas sugeridas y las preguntas libres comparten las mismas comprobaciones. Para mostrar límites, pregunta por causas, predicciones o por una empresa diferente de la abierta: no se inventan noticias ni se usan cifras de una empresa para describir otra.

## Prompt de implementación aplicado

Implementar un asistente comprensible para principiantes, con resumen específico, preguntas sugeridas, explicación de comparaciones, cambios entre ejercicios, tutor y preguntas libres con seguimiento. Usar solamente las cifras y fuentes disponibles, conservar fórmulas y permisos, separar ejemplos ilustrativos de observaciones, mantener la identidad visual y verificar escritorio, celular, teclado, fallos y datos incompatibles. Trabajar en la web y su servidor, preservando los contratos de Android.

## Qué hace la IA y qué comprueba el servidor

El chat web usa la conexión de Open WebUI que Diego ya tenía configurada en OpenCode: **Qwen/Qwen3.6-35B-A3B-FP8**, consultado desde la API de FINDSCORE. No se descarga ese modelo a la laptop. Los endpoints anteriores de análisis conservan Qwen3 1.7B y llama.cpp para no alterar los contratos de Android y reportes.

Primero se identifican los temas dentro del catálogo permitido. Después, el modelo recibe solamente las evidencias de esos temas y redacta uno o dos párrafos breves. Se comprueban estructura, identificadores de evidencia y cifras; se rechazan números nuevos, imágenes, HTML y determinados patrones de recomendaciones. Estas comprobaciones no equivalen a verificar automáticamente todos los matices semánticos del texto generado. Las fuentes y fechas que muestra la web las añade el servidor, no el modelo.

Los saludos, agradecimientos y despedidas habituales tienen respuesta inmediata sin llamar al modelo. Los seguimientos comunes, como «¿y eso qué significa?», recuperan el tema financiero anterior aunque haya un agradecimiento intermedio. Un mensaje ambiguo pide aclaración en lugar de mostrar una tarjeta arbitraria.

Las consultas por otro ejercicio, promedios sectoriales ausentes, Altman-Z no aplicable y comparaciones sin pilares compatibles reciben una explicación directa basada en el contexto, antes de consultar al modelo. Una premisa que atribuye un Altman-Z incorrecto se corrige con la cifra de la ficha. Preguntar si un dato ausente equivale a cero produce una aclaración explícita; no una lista de indicadores que no responde la duda.

El servidor construye las tarjetas desde los cálculos de pilares y el Altman-Z existentes. Sus explicaciones relacionan las cifras reales con interpretaciones acotadas y límites explícitos. Las fechas de Altman-Z se conservan separadas del ejercicio seleccionado. La comprobación de nombres y símbolos conocidos impide responder sobre otra empresa con las cifras del contexto abierto, independientemente de la clasificación del modelo.

La lectura inicial, los cambios y las lecciones funcionan sin llamar a la IA y se identifican como tales. Las consultas a la IA fallidas no se sustituyen por una respuesta que finja haber sido generada por el modelo. La conversación conserva su último resultado y el texto de la pregunta para reintentar.

En el chat, las respuestas se presentan como mensajes. Las cifras detalladas quedan en «Ver cifras y fechas usadas»; las referencias de fecha y los enlaces a fuentes permanecen visibles junto al mensaje. Altman-Z conserva su cierre propio aunque sea distinto del ejercicio seleccionado. Enter envía y Mayús+Enter añade otra línea. «Detener espera» cancela la espera del navegador, conserva el borrador y descarta una respuesta tardía; no promete interrumpir la inferencia que ya se esté ejecutando en el servidor.

El panel lateral mantiene el cuadro de escritura visible y desplaza solamente la conversación. La bienvenida ofrece tres preguntas sugeridas; las herramientas secundarias quedan en una barra compacta. Los mensajes usan ancho de lectura limitado y agrupan las fechas compartidas para evitar repetirlas por cada indicador. Se conservan los colores, las fuentes tipográficas y la preferencia de movimiento reducido de FINDSCORE.

## Conexión y credenciales

`tools/Configure-Assistant-From-OpenCode.ps1` reutiliza el proveedor Open WebUI seleccionado en la configuración del usuario. Guarda la conexión y la clave cifrada con la cuenta de Windows en `../presentation_runtime_sprint3/assistant-provider.xml`, fuera de Git. No modifica OpenCode ni publica la dirección privada. `Load-Assistant-Provider.ps1` carga las variables del servidor al iniciar o reiniciar la API.

En otro entorno se pueden proporcionar `ASSISTANT_API_BASE_URL`, `ASSISTANT_MODEL` y `ASSISTANT_API_KEY` directamente al proceso de la API. Ninguna de estas claves se envía al navegador. El cliente HTTP no sigue redirecciones con la credencial. Si no se configura la nueva conexión, se conserva la ruta local anterior; si una conexión configurada falla, el chat informa el fallo sin simular una respuesta del modelo. La laptop debe poder acceder al servidor de Open WebUI de la red.

## Contratos y módulos

| Ruta | Uso |
| --- | --- |
| `GET /api/v1/companies/{ticker}/assistant/?fiscal_year=2024` | Lectura, referencias, años, preguntas sugeridas y lecciones disponibles. |
| `POST /api/v1/companies/{ticker}/assistant/` | Acciones `summary`, `question`, `changes`, `learn`. |
| `GET/POST /api/v1/compare/assistant/` | Contexto `a`, `b`, `fiscal_year`; explicación y preguntas con acceso Pro. |

Contrato: `evidence-assistant-v1`. Los endpoints anteriores de análisis se mantienen para Android y los reportes. No hay migraciones ni cambios de fórmulas.

- `assistant_facts.py`: cifras, interpretaciones, fuentes y diferencias calculadas con Decimal.
- `assistant_context.py`: comprobación independiente de empresas mencionadas.
- `assistant_guards.py`: ejercicios, datos ausentes, promedio sectorial, aplicabilidad y premisas que contradicen la evidencia.
- `assistant_provider.py`: selección de temas mediante el proveedor configurado; conserva la conexión local anterior cuando no hay otra configurada.
- `assistant_connection.py`: conexión del servidor con Open WebUI, límites de respuesta, tiempo de espera y registro del modelo y tokens observados.
- `assistant_chat.py`: saludos, seguimiento y redacción con evidencias; validación del resultado y caché por usuario, modelo, pregunta, contexto y referencias.
- `assistant_lessons.py`: ejemplos y ejercicios revisados, separados de los datos financieros.
- `assistant_views.py`: autenticación, acceso Pro, validación de entradas y límites de consultas.
- En la web se conserva la separación entre componente, ViewModel y repositorio.

El historial permanece en memoria del navegador y se reinicia al cambiar empresa o ejercicio. El servidor recibe como máximo las preguntas y temas de cuatro turnos anteriores; no recibe respuestas redactadas por el cliente como si fueran evidencias. No se crea un historial persistente en la base de datos; las respuestas pueden mantenerse cinco minutos en la caché del servidor. Los límites y el registro interno de uso se mantienen activos. El registro distingue el modelo real de cada llamada y no aplica al nuevo modelo una tarifa asignada al anterior.

## Verificación

La suite general del servidor aprobó 836 pruebas y omitió una comprobación opcional de servicios externos. Tras añadir la aclaración sobre datos ausentes y reforzar la comprobación de fechas, la comprobación final de los módulos afectados aprobó 67 pruebas. La suite completa de la web aprobó 165 pruebas, incluidas las del saludo sin tarjetas, detener y descartar una respuesta tardía, y el cierre propio de Altman-Z visible fuera del desplegable de cifras. La compilación final también pasó.

`tools/verify_assistant_chat.cjs` comprueba la conexión real con Qwen3.6, saludos, seguimiento, mensajes ambiguos, fuentes, detener, errores, reinicio y chat lateral. Guarda capturas en `../output/assistant-chat/`. `tools/verify_financial_assistant.cjs` recorre deudas, liquidez, Altman-Z, datos faltantes, causas, tutor, diferencias entre ejercicios y comparación, en 360–1440 px; sus evidencias quedan en `../output/financial-assistant/`. No se versionan credenciales ni conversaciones de prueba.

`tools/verify_assistant_matrix.cjs --after` ejecuta 31 preguntas reales con las cuentas locales autorizadas: conversación, errores de escritura, seguimiento, liquidez, Altman-Z, fechas, datos ausentes, otra empresa, consejos personales, instrucciones ajenas y comparaciones. El resultado final aprobó las 31 comprobaciones y se revisó el contenido de las respuestas. Guarda los resultados locales en `../output/assistant-matrix/results-after.json`. No necesita saltarse los límites de consultas. Los errores de tiempo de espera, credenciales rechazadas, JSON inválido, respuesta incompleta y tamaño excesivo se simulan en pruebas del servidor; no se presentan como pruebas de fallos reales del proveedor. El recorrido del navegador comprueba el cuadro de escritura visible y la ausencia de desbordamiento horizontal entre 320 y 1440 px, además de cierre con Escape y movimiento reducido.

## Límites que permanecen

- No es un chat de propósito general ni consulta noticias o precios en vivo.
- Una interpretación académica no acredita solvencia ni permite recomendar una inversión.
- Los cambios numéricos no demuestran sus causas; los periodos incompatibles no producen diferencias.
- Un ejemplo educativo no sustituye datos faltantes. La disponibilidad financiera del catálogo permanece intacta.
- El modelo puede interpretar mal una pregunta o redactar un matiz impreciso. Los identificadores permitidos, la comprobación de cifras y la consulta de fuentes reducen ese riesgo; se puede reformular la pregunta y contrastar la evidencia.
