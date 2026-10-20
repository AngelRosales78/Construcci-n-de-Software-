# Avance de Diego del día 2

20 de octubre de 2026. Se construyó la parte de servidor y web de Liquidez, Rentabilidad y Solvencia. Ahora la ficha permite elegir el ejercicio fiscal y consultar siete indicadores con fórmulas, componentes y fuentes.

## Qué puedes mostrar al profesor

Abrir la ficha de Microsoft, buscar Pilares financieros anuales y elegir 2024. El periodo será del 1 de julio de 2023 al 30 de junio de 2024. No es un balance actual de 2026.

| Historia | Indicadores | MSFT FY2024 |
|---|---|---|
| HU05_02 Liquidez | Liquidez corriente y prueba ácida | 1,27 y 1,06 veces |
| HU05_03 Rentabilidad | ROE, ROA y margen neto | 37,13 %, 19,07 % y 35,96 % |
| HU05_04 Solvencia | Pasivos sobre patrimonio y cobertura de intereses | 0,91 y 37,72 veces |

Desplegar Ver fórmula, componentes y fuente para mostrar los datos que originan una cifra. Cambiar a 2020 permite demostrar que ROE sin cierre anterior importado y cobertura de intereses sin beneficio antes de impuestos aparecen como No disponible. La ficha de Apple no tiene balances anuales importados y no recibe datos de Microsoft.

Los colores siguen reglas académicas versionadas y explicadas. Rentabilidad no muestra una referencia sectorial porque no existe una muestra suficiente. El puntaje general y los dos pilares restantes siguen pendientes.

## Qué tareas avanzaron

| Tarea | Parte realizada | Estado y límite |
|---|---|---|
| HU05_02 T01 | Modelo anual, migración, importación validada, cálculos y endpoint. | Implementada y probada en servidor. |
| HU05_02 T02 | Vista web, ejercicio fiscal, carga, error y componentes. | Implementada y comprobada en navegador. |
| HU05_02 T04 | Pruebas de fórmulas, datos faltantes, importación y estados web. | Parte servidor y web comprobada; falta comparación Android. |
| HU05_03 T05 | ROE, ROA y margen neto con promedios comparables. | Implementada y probada en servidor. |
| HU05_03 T06 | Tarjetas web y explicación de referencia sectorial ausente. | Implementada y comprobada en navegador. |
| HU05_03 T08 | Ganancias cero, pérdidas, patrimonio y comparabilidad temporal. | Parte servidor y web comprobada; falta Android. |
| HU05_04 T09 | Ratios de solvencia y umbrales versionados. | Implementada y probada en servidor. |
| HU05_04 T10 | Vista de solvencia separada de Altman Z. | Implementada y comprobada en navegador. |
| HU05_04 T12 | Divisores, valores faltantes, pérdidas y límites. | Parte servidor y web comprobada; falta Android. |

Las tareas T03, T07 y T11 de Android corresponden a Angel. La revisión de GitHub al comenzar mostró su rama Rosales en `fe85825` del 13 de octubre, sin avances posteriores publicados; esto no permite afirmar qué tiene en su computadora. No se cambiaron sus archivos Android ni su rama.

## Cómo lo pruebas en tu equipo

El iniciador de presentación existente `tools/Start-Presentation.ps1` ahora incluye la importación de los componentes anuales en su base de demostración aislada. Su migración agrega la nueva tabla; no sustituye los balances ni las rutas antiguas. Las credenciales de esa demostración se mantienen fuera de Git.

Si ya tienes el servidor configurado y la empresa MSFT en tu base de desarrollo, después de actualizar el código y con respaldo según tu entorno:

```text
python manage.py migrate
python manage.py import_annual_financials ../docs/datos/sprint2_disponibilidad_MSFT.json
```

La importación es repetible: conserva cinco ejercicios, distingue los datos ausentes y valida todo el archivo antes de escribir. Hoy se ejecutó sobre bases de prueba aisladas; no se migró una base personal o compartida del equipo.

## Qué puede conectar Angel

`GET /api/v1/companies/MSFT/pillars/?fiscal_year=2024`, usando el token real de acceso. La ruta devuelve las mismas siete métricas mostradas por la web. Ejemplo completo sin credenciales: [respuesta de MSFT FY2024](evidencias/sprint2_MSFT_pillars_2024.json). Los nombres y reglas están en el [contrato actualizado](Sprint_2_contrato_pilares.md).

## Verificación

- Servidor: 226 pruebas aprobadas y una prueba de servicio omitida en la primera ejecución completa. Esa prueba se ejecutó después con Redis explícito y pasó: 227 comprobaciones distintas aprobadas entre ambas ejecuciones.
- De ellas, 45 verifican los nuevos pilares y la importación anual. Se comprobó con SQLite temporal y con PostgreSQL de validación.
- Web: 54 pruebas aprobadas, incluidas seis nuevas de los pilares. Compilación aprobada; conserva el aviso previo sobre tamaño del paquete.
- Navegador Edge: seis recorridos aprobados, sin errores JavaScript. Cifras contra un cálculo independiente, procedencia, cambio de año, ausencia de balances, reintento y vista a 1440 y 390 píxeles.
- Sin migraciones pendientes de generar.

El script `frontend/scripts/verify_pillars_day2.cjs` repite la comprobación contra servicios locales aislados. Las capturas y reportes completos se guardaron en output, fuera del repositorio. La [evidencia resumida](evidencias/sprint2_dia2_Diego.json) no contiene usuarios ni tokens de prueba.

## Cómo explicarlo con tus palabras

“Hoy agregué tres formas de analizar Microsoft. Los cálculos se realizan una sola vez en el servidor y la web los muestra por año, con sus fuentes. Probé cifras conocidas y los casos donde falta información. Angel ya puede conectar Android a la misma respuesta. Las historias completas todavía requieren esa integración móvil.”
