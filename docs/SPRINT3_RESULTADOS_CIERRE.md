# Resultados de cierre del Sprint 3

Revisión final para la entrega del 10/11/2026. Se registraron cuatro commits de implementación y un quinto de documentación; cada commit conserva su fecha real. No se modificaron fechas anteriores ni las ramas Rosales/main.

## Commits de implementación

- `c20f220 2026-11-09T23:16:47-05:00 Se integró Android de Rosales con los servicios del Sprint 3`
- `84b95cf 2026-11-09T23:12:54-05:00 Se incorporaron el rendimiento comparativo y los reportes PDF en la web`
- `645ad2d 2026-11-09T23:12:54-05:00 Se completaron los favoritos, las etiquetas y las empresas recientes en la web`
- `b85295e 2026-11-09T23:10:48-05:00 Se completaron los servicios compartidos del Sprint 3`

El quinto commit contiene este documento, la matriz y las evidencias. Su hash se obtiene con `git log -5 --oneline`; no se añade otro commit solo para escribir su propio hash.

## Validación

| Comprobación | Resultado |
|---|---|
| Servidor PostgreSQL/Redis | 389 aprobadas, 1 omitida; cero fallos. La omitida es una comprobación opcional y no se cuenta como aprobada. |
| Regresión final de reportes | 14 aprobadas; se solapan con la suite y no se suman como tareas nuevas. |
| Web | 94 aprobadas; 20 de sesión/favoritos/etiquetas se repitieron tras los últimos ajustes. Build aprobado. |
| Android JVM/HTTP real | 151 aprobadas, cero fallos y cero omisiones. |
| Android en emulador | 9 recorridos aprobados + 1 ensayo interactivo de sincronización aprobado. |
| PDF nativo | PdfRenderer abrió y dibujó el reporte real; cancelación de guardado y error ENOSPC simulado conservaron un estado seguro. |
| PDF de empresa con IA | Microsoft FY2024: misma versión/texto validado, sin volver a generar IA durante la exportación. |

## Rendimiento observado

Se ejecutaron diez ensayos de PDF: ficha individual y comparación, con cuatro rangos distintos por tipo más una repetición de 1Y. Hubo ocho generaciones sin caché de reportes y dos con caché. Se midieron aceptación HTTP, disponibilidad y descarga completa por separado. El motor de IA se preparó antes; su generación no está escondida en esos tiempos.

- Mayor tiempo de descarga completa del cliente HTTP local: **426.92 ms**.
- Mayor tamaño de esos diez archivos: **30,961 bytes**.
- Los diez cumplieron el objetivo local de tres segundos y dos MB. Esto no garantiza el mismo tiempo en otra laptop, red o teléfono.
- Sincronización final, un ensayo con ambas pantallas abiertas: Android → web **4125 ms**; web → Android **640 ms**. Se incluyó la actualización de las listas visibles y se usaron los relojes del host/emulador. Ambos valores quedaron dentro de cinco segundos.

Los datos detallados, fechas UTC, tamaños y tiempos por ensayo están en [resultados.json](evidencias/sprint3_cierre/resultados.json). Las capturas y PDFs de revisión se conservan en `output/sprint3_cierre`, en el directorio padre del repositorio. No contienen credenciales.

## Cobertura que se puede defender

La [matriz de 60 tareas](SPRINT3_MATRIZ_CIERRE.md) distingue responsables y archivos. Se implementaron y conectaron las funciones de las 15 HU: selección/comparación, histórico, conclusión local, acceso Pro, PDF y seguimiento, favoritos, sincronización, recientes y etiquetas. Se reutilizó el código móvil publicado por Angel, con atribución en el commit de integración.

Los datos no se inventaron para completar el gráfico: en GOOGL/META FY2024 faltan dos notas. El catálogo no equivale a cobertura financiera de 503 empresas. Los snapshots de precios llegan al 27/10/2026 y no se presentan como cotización en vivo.

Quedan como aceptación del equipo: revisión formal de la metodología académica, ensayo conjunto del guion y comprobación en el teléfono físico elegido. No son servicios sin implementar. Las pruebas con respuestas controladas, las de HTTP real y las de pantalla se identifican por separado.

## Repetir las comprobaciones

- Servidor: ejecutar pytest en el entorno de validación con PostgreSQL/Redis aislados y las dependencias de `backend/requirements.txt`.
- Web: `npm test -- --run` y `npm run build` en `frontend`.
- Sistema real: `python tools/verify_sprint3_live.py` con la demo Sprint 3 iniciada; `--android` incluye las pruebas JVM por HTTP real. El ejecutor crea cuentas temporales solo en la base `sprint3_demo_validation` y guarda sesiones fuera de Git.
- Dispositivo: `Sprint3LiveUiTest` requiere la sesión efímera de prueba en el almacenamiento privado de la app. La prueba de sincronización es interactiva y necesita la web abierta con esa misma cuenta. No introducir contraseñas en el código ni en los argumentos públicos del historial.

Se excluyeron .env, db.sqlite3, dependencias, cachés, compilaciones y configuración local Android. La única migración añadida fue `0010_sprint3_personal_reports.py`, creada expresamente para estas funciones.
