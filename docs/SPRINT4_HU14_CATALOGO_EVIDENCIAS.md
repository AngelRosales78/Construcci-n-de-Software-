# Avance de administración del catálogo

Fecha real 19 de noviembre de 2026. Rama de Diego `magallanes-sprint4`.
Se implementó Web y servidor de tres historias. El Android del Sprint 4 de
Rosales se conserva pendiente de su propia implementación y evidencia.

## Tareas del plan detallado y estado

| Historia | Tareas de ejecución del PDF | Requisitos originales | Estado Web/API |
|---|---|---|---|
| HU14_01 | S4-T11.3 y S4-T11.4 | T14.1.1 a T14.1.5 | Verificado: activar/desactivar, persistir, auditar y ocultar en buscadores. |
| HU14_02 | S4-T12.3 y S4-T12.4 | T14.2.1 a T14.2.5 | Verificado: piloto de quince, cobertura, fechas, fuentes y catálogo completo diferenciado. Selección presentada como propuesta. |
| HU14_04 | S4-T14.3 y S4-T14.4 | T14.4.1 a T14.4.5 | Verificado: editar descripción, sector y sitio web, validar y reflejar en la ficha pública. |

Son seis tareas nuevas de las cuarenta Web/API del plan de ejecución. Sumadas
a las diez de HU13 ya verificadas, el seguimiento alcanza 16/40 tareas Web/API.
Esto no acredita 16 historias completas ni incluye el trabajo Android de Rosales.

El PDF mantiene la organización del Sprint 3, cuatro entregables por historia.
Las cien tareas genéricas del backlog original se relacionan con ochenta
entregables de ejecución, cuarenta por integrante. Los códigos S4-T pertenecen a
ese desglose y no sustituyen los identificadores originales del documento base.

## Cómo demostrar el avance

1. Iniciar como administrador y abrir Administración, Administrar empresas y datos.
   Se muestran las 503 empresas, en páginas de veinte y orden alfabético.
2. Seleccionar las quince empresas piloto. La cifra cambia a quince; los demás
   símbolos continúan en el catálogo completo. La selección es reproducible desde
   `PILOT_TICKERS` y se identifica como propuesta, no aprobación del profesor.
3. Filtrar empresas con datos y sin datos. Cada fila indica cuántos de los cinco
   indicadores del resumen tiene, años de estados anuales, fecha de precios y
   fuentes. Cinco indicadores del resumen no significan cinco pilares completos.
4. Desactivar una empresa de ensayo y confirmar. Buscarla en el explorador deja
   de devolverla; el autocompletado también la excluye. Abrir su ficha por enlace
   directo conserva los datos y muestra el estado inactivo. Sus favoritos sobreviven.
5. Activarla otra vez restaura su presencia en ambos buscadores. El estado y los
   cambios quedan auditados. La prueba visual usó AAPL y la dejó activa al terminar.
6. Editar descripción, sector o URL HTTPS, guardar y abrir la ficha. Se reflejan
   sin cambiar precios ni ratios. Las entradas inválidas se rechazan sin escrituras
   parciales. Cancelar el diálogo no realiza cambios.

## Código y decisiones

- `backend/api/catalog_views.py`: lista administrativa, filtros, cobertura, edición,
  autorización y auditoría. Usa el permiso de superusuario activo y staff de HU13.
- `backend/api/catalog_services.py`: quince tickers propuestos y versión del
  catálogo compartida entre procesos. La versión se incrementa en la misma
  transacción que la edición, evitando reutilizar búsquedas de una versión anterior.
- `models.py` y migración `0014_company_catalog`: estado, piloto, descripción,
  sitio web, precedencia de sector editado y versión del catálogo. No borran datos.
- `views.py`: búsqueda pública solo de activas y comprobación de estado en las
  sugerencias, incluso ante un índice Redis obsoleto. La ficha directa sigue siendo
  consultable e indica explícitamente que la empresa está inactiva.
- `import_company_catalog`: conserva estado y datos descriptivos, y respeta el
  sector corregido manualmente al reimportar el CSV. No reactiva empresas.
- `AdminCompaniesPage`, `useAdminCompaniesViewModel` y `adminRepository` conservan
  la separación MVVM. Los filtros se guardan en la URL, las operaciones requieren
  confirmación y las respuestas tardías no actualizan otra cuenta.
- `CompanyAbout` publica descripción, enlace oficial y aviso de empresa inactiva.
  El diálogo se ajustó para que los controles quepan en la pantalla pequeña.

## Contrato para Android de Rosales

Base `/api/v1`, con access JWT. Solo administradores activos autorizados.

| Ruta | Comportamiento |
|---|---|
| GET `/admin/companies/?q=&scope=all&state=all&coverage=all&page=1` | Respuesta paginada `count,next,previous,results` y metadatos `pilot`. |
| PATCH `/admin/companies/{id}/` | Uno o varios campos: `is_active`, `description`, `sector`, `website_url`. Devuelve la empresa con cobertura actualizada. |

`scope` admite `all/pilot`; `state`, `all/active/inactive`; `coverage`,
`all/loaded/missing`. La búsqueda administrativa acepta nombre o ticker.
`coverage` incluye número de indicadores de resumen, fecha del balance,
procedencia por indicador, ejercicios anuales, fecha de precio y fuentes.
No se afirma que existan indicadores completos por pertenecer al piloto.

Descripción máxima de 3000 caracteres; sitio web HTTPS de hasta 500 caracteres;
sector perteneciente al catálogo existente. Campos financieros, ticker e is_pilot
no se pueden modificar mediante este PATCH. Datos inválidos producen 400,
cuenta sin sesión 401, usuario no autorizado 403 y empresa inexistente 404.

La respuesta de `/companies/{ticker}/summary/` añade `is_active`, `description`
y `website_url` sin eliminar los campos anteriores. Android debe mostrar el
aviso de empresa inactiva en enlaces directos y consumir las búsquedas comunes.

## Verificación y límites

- 435 pruebas del servidor aprobadas; una prueba optativa de Redis omitida.
- 113 pruebas Web aprobadas en 25 archivos. Compilación de producción correcta,
  con el aviso previo por tamaño del paquete principal.
- Doce casos nuevos del servidor y cuatro de Web comprueban permisos, filtros,
  piloto, fechas, datos ausentes, cachés, edición, rechazo de campos y reimportación.
- Migración aplicada y prueba visual de quince empresas, desactivación, búsqueda
  sin resultados, ficha conservada, reactivación y formulario de edición.
- El caché de búsqueda ahora consulta una versión de PostgreSQL por petición,
  incluso al acertar caché; no se declara un nuevo resultado de rendimiento.

Quedan pendientes HU14_03, actualización manual de datos desde una fuente,
y HU14_05, alta de ticker validada. Este avance no descarga cotizaciones nuevas,
no inventa estados financieros y no cierra las épicas 12 o 15 ni Android.
