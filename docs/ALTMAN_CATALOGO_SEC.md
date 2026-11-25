# Ampliación de Altman-Z con datos oficiales SEC

Actualización: 24 de noviembre de 2026 (hora de Lima).

La cobertura pasa de **6 a 180 empresas con puntuación calculada**, con 174 referencias nuevas. Se mantienen la fórmula original de cinco factores, sus límites y las seis puntuaciones anteriores. Esta ampliación no completa numéricamente todo el catálogo.

## Cobertura comprobada en la base

| Estado | Empresas | Tratamiento |
| --- | ---: | --- |
| Puntuación disponible | 180 | Referencia de cierre compatible y cinco factores calculables |
| Componentes SEC por conciliar | 182 | No se sustituyen importes ausentes ni se mezclan informes |
| Modelo sectorial por revisar | 97 | Actividades financieras o inmobiliarias identificadas mediante SIC |
| Clases de acciones por conciliar | 40 | No se multiplica un precio por acciones de valores distintos |
| Identidad o referencia pendiente | 5 | No hay una captura compatible suficiente |
| Total | 504 | Todas conservan su estado y motivo |

El listado actualizado está en [diagnostico_financiero.csv](datos/diagnostico_financiero.csv). La selección técnica por empresa, incluido CIK, SIC y fuente, está en [altman_sec_revision.csv](datos/altman_sec_revision.csv).

## Cómo enseñar las nuevas puntuaciones

| Empresa | Puntaje | Cierre contable utilizado |
| --- | ---: | --- |
| NVIDIA | 59,94 | 25/01/2026 |
| Tesla | 19,93 | 31/12/2025 |
| IBM | 3,53 | 31/12/2025 |
| Costco | 9,05 | 30/08/2026 |
| Starbucks | 3,04 | 27/09/2026 |
| Adobe | 8,37 | 28/11/2025 |

1. Abrir la ficha y mostrar la lectura del Resumen.
2. Entrar en Salud financiera y abrir «Ver datos y fuentes de Altman-Z».
3. Mostrar el cierre propio de Altman, el precio sin ajustar y las acciones de esa fecha.
4. En «Hechos SEC utilizados», revisar etiquetas, informe, cierre y fecha de publicación.
5. Consultar el informe oficial o descargar el PDF individual.

El puntaje no es un porcentaje ni una nota sobre diez. Las zonas siguen la fórmula académica original; no se presentan como recomendaciones de inversión o garantías.

## Qué se validó

La captura utiliza las [API públicas de EDGAR](https://www.sec.gov/search-filings/edgar-application-programming-interfaces): Company Facts para los hechos XBRL y Submissions para comprobar la identidad y actividad del emisor. Se hicieron 1.002 consultas para 501 CIK distintos, incluida HIMS, añadida manualmente; 994 devolvieron copias válidas. Los resultados restantes se registran como pendientes.

- Identidad: CIK y símbolo coincidentes; los emisores con varios valores cotizados pasan a revisión.
- Período: hechos de un mismo 10-K y acceso, cierre real, flujos anuales entre 350 y 380 días y fechas no futuras.
- Importes: unidades USD o acciones identificadas, sin sustituir datos faltantes por cero.
- Pasivos: total publicado o suma de pasivos corrientes y no corrientes publicados en el mismo cierre.
- Acciones: número en circulación del cierre; alternativamente emitidas menos tesorería del mismo cierre. No se usa el promedio diluido ni una cifra de portada de otra fecha.
- EBIT: resultado operativo publicado; cuando no está, resultado antes de impuestos más gasto por intereses compatible. Cada referencia conserva el método exacto.
- Precio: cierre sin ajustar, de la sesión del cierre contable o la anterior, con separación máxima de tres días. Los desdoblamientos posteriores requieren conciliación y se dejan pendientes.
- Procedencia: URL, SHA-256, etiquetas y contextos originales de cada hecho conservados.

Los componentes seleccionados de NVIDIA, Tesla e IBM se contrastaron además con las tablas de sus informes oficiales. El resto del lote tiene validación automática de coherencia y procedencia; no una revisión contable manual de cada emisor.

La clasificación SIC entre 6000 y 6799 se conserva como «Requiere un modelo sectorial». No se inventa una puntuación alternativa. [Altman explica el alcance y las variantes de su modelo](https://rpc.cfainstitute.org/blogs/enterprising-investor/2016/the-altman-z-score-in-edward-altmans-own-words).

## Reproducción

La copia comprimida [altman_sec_references.json.gz](datos/altman_sec_references.json.gz) contiene las cinco referencias manuales previas y 174 adicionales con sus hechos SEC. Microsoft conserva su cálculo anterior separado. La copia no incluye datos de acceso ni correo de contacto.

Desde backend, con la configuración local habitual:

```powershell
python manage.py import_altman_references ../docs/datos/altman_sec_references.json.gz --prices ../docs/datos/catalog_prices.json.gz --dry-run
python manage.py import_altman_references ../docs/datos/altman_sec_references.json.gz --prices ../docs/datos/catalog_prices.json.gz
python manage.py audit_financial_availability ../docs/datos/catalog_financials.json.gz --prices ../docs/datos/catalog_prices.json.gz --sec-review ../docs/datos/altman_sec_revision.json
```

El arranque de la plataforma incorpora esta copia después de las referencias previas. Se puede restaurar sin red. Repetir la importación no modifica las cifras. Los cinco indicadores del resumen de las 504 empresas permanecen intactos.

Para una captura nueva, usar collect_sec_altman.py con --contact-file apuntando a un archivo privado y después prepare_sec_altman_references.py con el inventario de referencia. El correo identifica las consultas siguiendo las [indicaciones SEC](https://www.sec.gov/about/webmaster-frequently-asked-questions); no se añade al repositorio ni a las capturas. El colector limita el conjunto de trabajadores a dos solicitudes por segundo y detiene solicitudes nuevas si recibe 403 o 429.

## Comprobaciones

- 770 pruebas del servidor aprobadas; una optativa omitida.
- 145 pruebas de interfaz aprobadas y compilación de producción correcta.
- Ocho fichas nuevas comprobadas en navegador, a 1440 y 390 px, con sus fuentes y sin desbordamiento; cubren las tres zonas del modelo.
- PDF de NVIDIA generado, verificado y descargado.
- JPM conserva la revisión sectorial; GOOGL, la conciliación de valores cotizados.
- Android, pagos y los cinco indicadores existentes sin cambios.
