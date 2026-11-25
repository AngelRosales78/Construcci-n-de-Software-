# Identidad visual y lectura de empresas

## Prompt de trabajo

Actúa como diseñador de interfaces y desarrollador de la web financiera. Evalúa cada pantalla desde la perspectiva de una persona que todavía no conoce los indicadores. Dale una identidad coherente y facilita encontrar una empresa, entender qué hace y consultar sus cifras.

1. Sustituye los títulos de estilo literario por una familia sobria y legible. Usa pesos y tamaños consistentes; reserva la tipografía monoespaciada para símbolos y referencias breves.
2. Refuerza la identidad de cada empresa con sus logos existentes y un reemplazo legible cuando falten. Conserva su descripción, sector y fecha del precio.
3. Da protagonismo al histórico real de precios. Mantén controles sencillos y permite consultar una sesión con teclado, sin inventar precios ni ganancias.
4. Presenta el puntaje, su zona y su significado juntos. Conserva las fechas, las fuentes y los motivos concretos cuando no se pueda calcular.
5. Facilita comparar las notas con barras y colores consistentes. Dibuja barras únicamente para valores disponibles, sobre 100, y periodos compatibles. Conserva la precisión del servidor.
6. Ofrece entradas por sector, iconos coherentes y estados de interacción claros. Respeta el movimiento reducido y comprueba el teléfono además del escritorio.
7. Verifica los recorridos, el diseño y las pruebas existentes. Entrega un cambio coherente en la rama actual.

## Cambios aplicados

| Pantalla | Comportamiento visible |
| --- | --- |
| Toda la web | IBM Plex Sans para lectura y títulos; IBM Plex Mono para símbolos y referencias. Paleta crema, verde y terracota conservada. |
| Inicio | Seis entradas visuales por sector, con ejemplos de empresas y logos existentes. El catálogo completo sigue accesible. |
| Explorador | Accesos rápidos por sector sincronizados con el selector. Conservan consulta, orden y disponibilidad; reinician la página. |
| Ficha | Logo, empresa, sector y cierre con fecha. El gráfico aparece junto a la lectura del balance y antes de las métricas en escritorio. En teléfono se apilan. |
| Histórico | Precio y fecha de la sesión consultada, control con teclado y línea vertical correspondiente. La explicación técnica queda debajo del gráfico. |
| Comparador | Logos y colores estables para A y B. Barras acompañadas de notas escritas; las incompatibilidades mantienen su explicación. |

Las direcciones largas de precios se presentan como enlaces con el nombre del proveedor; el destino original se conserva completo. Las cifras principales y su interpretación permanecen visibles. No se añadieron nuevos desplegables.

## Recursos

Las fuentes están alojadas localmente en `frontend/public/fonts/`. `sources.json` registra sus archivos originales y SHA-256; `ibm-plex-OFL.txt` conserva la licencia SIL OFL. No se requiere consultar Google Fonts durante el uso de la aplicación. Los logos se reutilizan del catálogo verificado y muestran el símbolo si falla la imagen.

## Verificación

- Suite completa: 149 pruebas aprobadas en 34 archivos; después de los últimos ajustes, también se aprobaron las 16 pruebas de gráficos y comparación afectadas.
- Compilación de producción con Vite.
- `tools/verify_company_reading.cjs`: guía, foco, información financiera y ausencia de desbordamiento a 360, 390, 768 y 1440 px.
- `tools/verify_visual_identity.cjs`: carga de fuentes locales, seis sectores con resultados reales, consulta de sesiones con flechas, cinco años de precios, barras contrastadas con la API y comparación incompatible sin barras. Incluye movimiento reducido y errores de JavaScript.
- Capturas y resultados del navegador se guardan fuera de Git, en `../output/visual-identity/`.

Los cálculos y las reglas de disponibilidad permanecen en el servidor. Altman-Z conserva su escala original; las barras sobre 100 corresponden a los cinco pilares. Esta revisión comprueba el funcionamiento y la presentación; la comprensión en cinco segundos requiere observar también a usuarios nuevos.
