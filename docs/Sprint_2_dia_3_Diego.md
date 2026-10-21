# Avance de Diego del día 3

21 de octubre de 2026. Se completó la parte de servidor y web de Eficiencia, Crecimiento, radar de cinco pilares y puntaje general. La conexión Android y la revisión de las reglas por Angel siguen pendientes.

## Lo que puedes explicar al profesor

“Hoy completé los dos pilares que faltaban. Eficiencia muestra cómo se usan los activos y el inventario; Crecimiento compara los ingresos anuales. Después convertí los indicadores en cinco notas mediante reglas académicas publicadas. El radar muestra esas notas y el puntaje general es su promedio, con un peso del 20 % para cada una. Si faltan datos, el sistema explica qué falta y no inventa un puntaje.”

## Cómo demostrarlo

1. Abrir Microsoft y elegir el ejercicio fiscal 2024. El balance corresponde al periodo del 1 de julio de 2023 al 30 de junio de 2024, no a datos actuales de 2026.
2. Mostrar Eficiencia: rotación de activos **0,53 veces** y días de inventario **9,22 días**. La tabla presenta FY2024, FY2023 y FY2022 con los datos disponibles.
3. Mostrar Crecimiento: variación anual de ingresos **15,67 %** y tasa compuesta a tres años **13,40 %**. Abrir la fórmula y comprobar las cuatro fuentes de FY2021 a FY2024.
4. Mostrar el radar, sus cinco notas y el puntaje **85,25/100**. Pulsar un eje o el nombre de un pilar abre su detalle. Desplegar las reglas muestra cómo se obtiene cada nota.
5. Elegir FY2022. Falta FY2019 para completar los cuatro cierres de esa tasa compuesta. El puntaje queda No disponible, identifica Crecimiento como pendiente y el radar no dibuja un polígono falso.
6. Abrir Apple: como no tiene componentes anuales importados, no recibe cifras de Microsoft. Sus indicadores y puntaje se muestran como no disponibles.

## Cómo se llega al puntaje

| Pilar | Nota visible | Peso |
|---|---|---|
| Liquidez | 81,87 | 20 % |
| Rentabilidad | 100,00 | 20 % |
| Solvencia | 88,65 | 20 % |
| Eficiencia | 73,96 | 20 % |
| Crecimiento | 81,78 | 20 % |

El servidor usa los valores de cálculo sin redondear y muestra **85,25**. Los puntos de conversión son una convención académica del proyecto, no una evaluación sectorial ni una garantía de inversión. Se documentan en [Reglas de puntuación](Sprint_2_reglas_puntuacion.md); todavía no se atribuye aprobación del equipo ni del profesor.

## Tareas de hoy

| Historia | Servidor | Web | Pruebas y límite |
|---|---|---|---|
| HU05_05 Eficiencia | T13: cálculos, hasta tres ejercicios y clasificación documentada del inventario. | T14: indicadores, unidades, datos ausentes y tabla anual. | T16: casos manuales, inventario cero, no aplicable y periodos incompletos comprobados. Falta Android T15. |
| HU05_01 Cinco pilares | T17: crecimiento anual, compuesto y notas versionadas. | T18: cinco ejes, notas, enlaces a los detalles y explicación de reglas. | T20: cuatro cierres, límites de notas y radar sin datos inventados comprobados. Falta Android T19. |
| HU04_05 Puntaje general | T21: cinco notas compatibles, 20 % cada una y redondeo al presentar. | T22: puntaje, pesos, periodo y explicación de los pilares pendientes. | T24: límites, versiones, notas ausentes y redondeo comprobados. Falta Android T23. |

No se cierran las tres historias completas mientras falte su parte móvil. La verificación de GitHub al inicio encontró Rosales en `fe85825`, del 13 de octubre, sin commits posteriores publicados. No permite saber qué trabajo conserva Angel en su computadora. Sus archivos y ramas no se modificaron.

## Qué necesita Angel

Actualizar desde la versión compartida que contenga este avance, aplicar migraciones y mantener el token real de acceso. La consulta sigue siendo:

```text
GET /api/v1/companies/MSFT/pillars/?fiscal_year=2024
```

Devuelve once indicadores, cinco notas y el puntaje cuando existe cobertura. Para presentar cifras debe usar `display_value`; para dibujar puede convertir `value` a número solo si el estado es disponible. Un nulo no es cero. El [contrato actualizado](Sprint_2_contrato_pilares.md) incluye todos los campos y el [ejemplo real de MSFT](evidencias/sprint2_MSFT_pillars_2024_dia3.json) no contiene credenciales.

La nueva migración agrega los campos opcionales de aplicabilidad del inventario. El iniciador de presentación existente importa los mismos cinco ejercicios sin sustituir el balance de la ficha anterior. Hoy se validó en bases aisladas, sin migrar una base personal o compartida del equipo.

## Verificación realizada

- **Servidor: 274 pruebas aprobadas**, sin omisiones, con PostgreSQL y Redis aislados. Incluye las funciones anteriores y las 47 nuevas pruebas del día 3. Las nuevas funciones también se comprobaron con SQLite temporal.
- **Web: 61 pruebas aprobadas**, incluidas siete nuevas del día 3. Compilación aprobada; conserva el aviso previo sobre tamaño del paquete.
- **Edge: siete recorridos nuevos aprobados**, sin errores JavaScript: cifras y notas contra cálculo independiente, fuentes, radar y enlaces, año incompleto, vista a 1440 y 390 píxeles, empresa sin datos y recuperación de un fallo temporal con rechazo de acceso anónimo.
- Se repitieron además los seis recorridos de los indicadores del día 2: aprobados con la respuesta ampliada.
- Sin migraciones pendientes de generar. La migración nueva se aplicó correctamente en las bases aisladas de prueba.

La [evidencia resumida](evidencias/sprint2_dia3_Diego.json) se guarda sin tokens ni cuentas de prueba. Las capturas y reportes completos permanecen en output, fuera de Git. `frontend/scripts/verify_pillars_day3.cjs` permite repetir el recorrido contra servicios locales preparados con los datos existentes.

## Prompt de trabajo aplicado

Trabaja en el día 3 del sprint 2 del proyecto S&P 500. Diego realiza servidor y web; Angel realiza Android. Primero revisa GitHub y los cambios locales. Conserva el trabajo existente y no modifiques sources/.

Implementa HU05_05, Eficiencia con rotación de activos y días de inventario; HU05_01, crecimiento anual y compuesto a tres años con radar de los cinco pilares; y HU04_05, puntaje general de 0 a 100. Reutiliza los componentes anuales, las fórmulas y el contrato. Explica periodos, fuentes y datos faltantes. No inventes años ni conviertas valores ausentes en cero.

Define y documenta una metodología académica versionada para las notas. Usa igual peso para los cinco pilares y declara la revisión pendiente del equipo. Sin cinco notas válidas, no publiques un puntaje general. Trabaja solo en servidor y web, y prepara la API para Android sin afirmar que Angel la integró.

Verifica fórmulas, límites, años incompletos, radar y puntaje. Guarda commits separados por tarea: T13/T14/T16, T17/T18/T20 y T21/T22/T24. Publica el avance comprobado en la rama de Diego. Explica qué funciona, cómo demostrarlo, qué se publicó y qué queda pendiente.
