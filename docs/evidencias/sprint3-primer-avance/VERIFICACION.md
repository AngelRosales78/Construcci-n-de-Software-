# Verificación del primer avance

## Resultados obtenidos

| Comprobación | Resultado |
|---|---|
| Suite completa de servidor en PostgreSQL aislado | 369 aprobadas; 1 opcional omitida |
| Suite completa web | 86 aprobadas en 20 archivos |
| Build web | Correcto; advertencia de tamaño del paquete de gráficos existente |
| Comprobación de configuración Django | Sin problemas |
| Comprobación de migraciones | Sin cambios pendientes |
| Recorrido real Edge: API + web | 13 comprobaciones aprobadas |
| JavaScript de página en ese recorrido | Sin errores |

La única omitida es una prueba opcional de rendimiento Redis que requiere una
variable de activación específica. El resto se ejecutó con PostgreSQL y Redis
aislados, incluido el receptor SMTP local de las pruebas de recuperación de clave.
La prueba general inicial con SQLite no es el resultado de aceptación: los casos
específicos de PostgreSQL y los servicios auxiliares se verificaron con el entorno
correcto en la ejecución final.

Los logs completos y JUnit quedan fuera del repositorio en `output/` de la carpeta
superior: `sprint3_backend_tests.xml`, `sprint3_backend_tests.log`,
`sprint3_web_tests.log` y `sprint3_web_build.log`.

## Recorrido reproducible

Con la demo aislada iniciada por `tools/Start-Sprint3-Demo.ps1`, ejecutar
`node tools/verify_sprint3_advance.cjs` desde el repositorio. Necesita las
dependencias web y Microsoft Edge instalados. Lee las cuentas desde un archivo
local fuera del repositorio y no muestra sus contraseñas.

`resultado.json` enumera cada comprobación y su fecha real. Las capturas muestran
la vista previa Free, favoritos, comparación en escritorio y móvil, y períodos
distintos sin ganadores. En móvil la tabla conserva su tamaño legible y permite
desplazamiento horizontal dentro de su contenedor; no desborda la página.

Se revisaron visualmente las capturas de escritorio, móvil y favoritos.

## Límites del resultado

Esta es evidencia de web y API, no de ejecución del Android de Angel. Sus 132
pruebas JVM aprobadas y 3 omitidas provienen de su informe en `93e5ea3`, no de
esta ejecución. Quedan la prueba conjunta y las demás historias del Sprint 3.

El comparador reutiliza las fuentes y notas existentes. No inventa valores para
completar solvencia y eficiencia cuando sus componentes faltan. Los inventarios
GOOGL y META conservan la procedencia descrita en `docs/datos/sprint3/PROCEDENCIA.md`.
