# Asistente financiero de la web

## Cómo demostrar las seis funciones

1. Abre Microsoft, entra en **Explicación de IA** y selecciona **2024**. «Lectura rápida» muestra qué destaca, qué merece atención y qué falta conocer. Cada tarjeta conserva cifra, significado, límite y fuente.
2. En **Preguntar**, pulsa «¿Cómo están sus deudas?». La respuesta utiliza los indicadores de solvencia de Microsoft y ese ejercicio.
3. Escribe «¿Podrías explicarlo más?». Mantiene el tema anterior y añade un ejemplo identificado como ficticio. Puedes reiniciar la conversación sin cambiar la empresa.
4. En **Cambios entre años**, selecciona 2023 y compara con 2024. Los cambios se calculan en el servidor; los porcentajes se expresan en puntos porcentuales. Con referencias anuales normalizadas, como AAPL 2025 frente a 2024, se explica por qué no se declara una comparación contable válida.
5. En **Aprender**, abre una lección y responde su pregunta. El servidor comprueba la opción elegida y muestra una explicación. Los ejemplos no son datos de la empresa abierta.
6. En el comparador Pro, selecciona GOOGL y META, ejercicio 2024. Abre **Entiende la comparación** y pide la lectura o escribe una pregunta. Las fechas y versiones del comparador original determinan qué notas se pueden explicar como comparables.

Las preguntas sugeridas y las preguntas libres comparten las mismas comprobaciones. Para mostrar límites, pregunta por causas, predicciones o por una empresa diferente de la abierta: no se inventan noticias ni se usan cifras de una empresa para describir otra.

## Prompt de implementación aplicado

Implementar un asistente comprensible para principiantes, con resumen específico, preguntas sugeridas, explicación de comparaciones, cambios entre ejercicios, tutor y preguntas libres con seguimiento. Usar solamente las cifras y fuentes disponibles, conservar fórmulas y permisos, separar ejemplos ilustrativos de observaciones, mantener la identidad visual y verificar escritorio, celular, teclado, fallos y datos incompatibles. Trabajar en la web y su servidor, preservando los contratos de Android.

## Qué hace la IA y qué comprueba el servidor

El motor local existente, Qwen3 mediante llama.cpp, identifica los temas de la pregunta y recupera el tema de los turnos anteriores. Solo puede devolver identificadores de temas de un catálogo cerrado y una indicación de contexto. No puede insertar texto libre, cifras, enlaces ni campos adicionales en la respuesta financiera.

El servidor construye las tarjetas desde los cálculos de pilares y el Altman-Z existentes. Sus explicaciones relacionan las cifras reales con interpretaciones acotadas y límites explícitos. Las fechas de Altman-Z se conservan separadas del ejercicio seleccionado. La comprobación de nombres y símbolos conocidos impide responder sobre otra empresa con las cifras del contexto abierto, independientemente de la clasificación del modelo.

La lectura inicial, los cambios y las lecciones funcionan sin llamar a la IA y se identifican como tales. Las consultas a la IA fallidas no se sustituyen por una respuesta que finja haber sido generada por el modelo. La conversación conserva su último resultado y el texto de la pregunta para reintentar.

## Contratos y módulos

| Ruta | Uso |
| --- | --- |
| `GET /api/v1/companies/{ticker}/assistant/?fiscal_year=2024` | Lectura, referencias, años, preguntas sugeridas y lecciones disponibles. |
| `POST /api/v1/companies/{ticker}/assistant/` | Acciones `summary`, `question`, `changes`, `learn`. |
| `GET/POST /api/v1/compare/assistant/` | Contexto `a`, `b`, `fiscal_year`; explicación y preguntas con acceso Pro. |

Contrato: `evidence-assistant-v1`. Los endpoints anteriores de análisis se mantienen para Android y los reportes. No hay migraciones ni cambios de fórmulas.

- `assistant_facts.py`: cifras, interpretaciones, fuentes y diferencias calculadas con Decimal.
- `assistant_context.py`: comprobación independiente de empresas mencionadas.
- `assistant_provider.py`: consulta local y validación estricta de temas; caché por usuario, pregunta, contexto y referencias.
- `assistant_lessons.py`: ejemplos y ejercicios revisados, separados de los datos financieros.
- `assistant_views.py`: autenticación, acceso Pro, validación de entradas y límites de consultas.
- En la web se conserva la separación entre componente, ViewModel y repositorio.

El historial permanece en memoria del navegador y se reinicia al cambiar empresa o ejercicio. El servidor recibe como máximo los cuatro turnos anteriores para interpretar el seguimiento; no almacena el texto de las conversaciones. No se incorporan proveedores externos ni credenciales nuevas. Los límites de consulta y el registro interno de uso se mantienen activos.

## Verificación

La suite general del servidor aprobó 801 pruebas y omitió una comprobación opcional de servicios externos. Después de los últimos ajustes se aprobaron las 35 pruebas específicas del asistente. La suite completa de la web aprobó 156 pruebas; se verificaron de nuevo los componentes afectados y la compilación final.

`tools/verify_financial_assistant.cjs` prueba el modelo local real con deudas, liquidez, seguimiento, Altman-Z, datos faltantes, causas y límites de contexto. También recorre el tutor, las diferencias entre ejercicios y la comparación; comprueba pantallas de 360, 390, 768 y 1440 px. Capturas y resultados se guardan fuera de Git en `../output/financial-assistant/`.

## Límites que permanecen

- No es un chat de propósito general ni consulta noticias o precios en vivo.
- Una interpretación académica no acredita solvencia ni permite recomendar una inversión.
- Los cambios numéricos no demuestran sus causas; los periodos incompatibles no producen diferencias.
- Un ejemplo educativo no sustituye datos faltantes. La disponibilidad financiera del catálogo permanece intacta.
- Un modelo pequeño puede interpretar mal una pregunta ambigua. El contexto, los identificadores permitidos y las cifras compiladas por el servidor acotan lo que puede responder; se puede reformular la pregunta.
