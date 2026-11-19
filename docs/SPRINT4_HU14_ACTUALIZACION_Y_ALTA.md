# Actualización de precios y alta verificada

Avance del 19 de noviembre de 2026. Rama `magallanes-sprint4`, Diego Magallanes.
Este registro acredita Web y API. Android del Sprint 4 corresponde a Rosales y
necesita su propia implementación y evidencia.

| Historia | Tareas del plan detallado | Alcance verificado |
|---|---|---|
| HU14_03 | S4-T13.3 y S4-T13.4 | Consulta manual de precios ajustados, persistencia, resultado fechado, errores, reintentos idempotentes y límite interno de llamadas. |
| HU14_05 | S4-T15.3 y S4-T15.4 | Verificar ticker y nombre, revisar precios antes del alta y confirmar sin duplicados ni calificaciones ficticias. |

Las referencias originales son T14.3.1 a T14.3.5 y T14.5.1 a T14.5.5. Se suman
cuatro tareas Web/API al seguimiento: 20/40 verificadas en el desglose de ochenta
entregables, cuarenta por integrante. Los cuatro bloques anteriores de Android
siguen fuera de ese denominador.

## Qué se actualiza y qué no

La actualización descarga hasta cinco años de cierres ajustados desde Yahoo
Finance y guarda los precios en USD de sesiones anteriores al día de consulta en
Nueva York. No presenta una vela intradía como un cierre definitivo. Se utilizan
los valores `adjclose` proporcionados por la fuente, sin sustituirlos por `close`
cuando faltan. El ajuste incorpora dividendos y desdoblamientos según la
[explicación de Yahoo](https://help.yahoo.com/kb/SLN28256.html).

Se comprobaron SEC y Yahoo en esta ejecución. SEC devolvió HTTP 403 y Yahoo
respondió correctamente. El adaptador nuevo actualiza **precios**, no estados
anuales ni los cinco indicadores del resumen. PER, ROE, capitalización, beta y
dividendo conservan sus fuentes y fechas anteriores. La descarga automática de
balances anuales sigue pendiente; no se declara resuelta por tener precios nuevos.

Una empresa añadida puede tener precios y no tener balances. En ese caso la ficha
mantiene los ratios ausentes y Altman-Z sin calcular. Tampoco se da por acreditada
su pertenencia al S&P 500. El sector se elige administrativamente y queda registrado
como tal, sin atribuir esa clasificación al proveedor de precios.

## Cómo demostrar HU14_03

1. Iniciar como administrador, abrir Administración y Administrar empresas y datos.
2. Buscar MSFT o AAPL y pulsar Actualizar precios. Revisar el diálogo que explica
   que los balances conservarán sus fechas. Confirmar para realizar la consulta.
3. Esperar al resultado. Mostrar la fecha de los precios y la última consulta, que
   permanecen visibles al recargar la lista. La cuota indica llamadas de la app.
4. Abrir la ficha y el gráfico, elegir 1 mes o 5 años y mostrar la fuente y las
   sesiones recibidas. Cambiar el rango no solicita nuevos precios al proveedor:
   consulta las series guardadas en PostgreSQL.
5. Ante una respuesta perdida, Recuperar la misma operación reutiliza su UUID.
   Si ya terminó se devuelve su resultado sin repetir la consulta ni los precios.
   Una segunda actualización explícita usa otra operación y hace upsert por fecha.
6. Las pruebas del servidor simulan fallo del proveedor, moneda incompatible,
   serie inválida y revocación del administrador. En todos esos casos no reemplaza
   parcialmente el historial. No se provoca una caída real durante la exposición.

## Cómo demostrar HU14_05

1. Abrir Añadir una empresa. Escribir un ticker que aún no esté registrado y elegir
   su sector. Pulsar Verificar ticker. El nombre no se escribe ni se inventa.
2. Revisar el nombre devuelto, moneda, cantidad de precios, fecha y fuente.
   Hasta este momento no se crea la empresa. La verificación dura diez minutos.
3. Pulsar Confirmar alta y revisar el diálogo. Añadir al catálogo crea la empresa
   y sus precios en una misma transacción. Confirmar otra vez no crea duplicados.
4. Abrir la ficha y mostrar Alta manual e índice sin verificar. Los indicadores
   faltantes permanecen ausentes. La búsqueda pública distingue estas ampliaciones
   del catálogo original y no afirma que sean componentes verificados del índice.
5. Probar ticker inválido o existente para mostrar el rechazo. Una respuesta de
   otra cuenta o una verificación vencida no permiten dar de alta la empresa.

## Código principal

- `backend/api/market_provider.py`: una consulta HTTPS al host fijo de Yahoo,
  respuesta máxima de 2 MB, timeout, normalización del ticker y validación de
  identidad, moneda, instrumento, fechas, precios y volúmenes. Sin redirecciones
  a destinos arbitrarios ni ejecución de contenido recibido.
- `backend/api/market_updates.py`: operación persistida, límite de llamadas,
  reutilización de request_id, protección de consultas simultáneas, preparación
  y confirmación del alta. Las llamadas HTTP se realizan fuera de las transacciones
  que escriben precios. Los permisos se revisan otra vez antes de escribir.
- `backend/api/market_views.py`: validación de entrada y rutas compartidas.
- `models.py` y migración `0015_market_data_operations`: operaciones, respuesta
  original con SHA-256, contador diario, procedencia de identidad y marca de alta
  manual. La respuesta original no se expone en la API ni contiene credenciales.
- `useMarketDataViewModel`, `adminRepository` y `MarketDataControls`: estado,
  confirmaciones, recuperación de una operación y presentación de resultados.
  Las vistas siguen sin acceder directamente a HTTP ni al almacenamiento.
- `import_market_data --preserve-refreshed` y `Start-Sprint3-Demo.ps1`: los CSV
  antiguos de la demostración no sobrescriben precios actualizados desde el panel
  al reiniciar. Las importaciones explícitas sin esa opción conservan su función.

## Contrato compartido para Android

Base `/api/v1`, access JWT y permiso de superusuario activo y staff.

| Método y ruta | Entrada y resultado |
|---|---|
| POST `/admin/companies/{id}/refresh/` | JSON `{request_id: UUID}`. Consulta y valida precios. |
| POST `/admin/companies/verify/` | JSON `{request_id: UUID, ticker, sector}`. Devuelve verificación `ready`, sin crear empresa. |
| POST `/admin/companies/` | JSON `{verification_id: UUID}`. Confirma una verificación propia y vigente; 201 al crear y 200 al recuperar el alta ya realizada. |
| GET `/admin/market-operations/{id}/` | Estado de una operación propia; nunca devuelve la respuesta original binaria. |

Las respuestas incluyen `operation` y `provider`. Estados `running`, `ready`,
`succeeded` y `failed`. `operation` incluye ID, empresa, ticker, fechas, mensaje,
código de error y resultado con cantidad de precios, fuente, fecha y SHA-256.
La lista administrativa añade `last_update` y `market_provider`.

Un UUID no puede reutilizarse para otro ticker, sector o tipo de operación.
Operación ya iniciada: 202. Identificador incompatible, duplicado, verificación
vencida u otra consulta simultánea del ticker: 409. Campos inválidos: 400.
Acceso normal: 403; verificación ajena: 404. Cuota o frecuencia: 429.
Respuesta incompatible: 502; fuente no disponible: 503.

Después de un timeout sin resultado confirmado, Android debe recuperar el mismo
request_id, en lugar de generar otro inmediatamente. Una falla ya confirmada por
el servidor permite iniciar una nueva consulta explícita. Las operaciones
interrumpidas se liberan después de cuatro minutos; una operación vencida ya no
puede escribir resultados sobre una nueva.

## Límite interno y relación con la épica 15

El presupuesto predeterminado es de cien consultas por día UTC y se conserva en
PostgreSQL; no representa una cuota oficial de Yahoo. Cuenta intentos HTTP reales,
incluidos fallos del proveedor. Repetir una operación ya procesada no consume otra
llamada. Se reserva el cupo bajo bloqueo para impedir excederlo con concurrencia.
Existe separación mínima de un segundo entre consultas de la aplicación.

`MARKET_DATA_DAILY_LIMIT` define el presupuesto inicial de cada nueva ventana
diaria y `MARKET_DATA_MIN_INTERVAL` la separación. No se modificó ningún `.env`.
El contador prepara infraestructura útil para HU15_03, pero no cierra esa historia:
faltan el aviso al 85 %, su configuración administrativa y el monitoreo general.

## Evidencia ejecutada

- Servidor completo: 464 pruebas aprobadas y una optativa de Redis omitida.
  Veintinueve casos específicos comprueban parser, errores HTTP, idempotencia,
  permisos, vigencia, contador, concurrencia y conservación al reiniciar.
- Web completa: 116 pruebas aprobadas en 25 archivos. Siete pruebas del catálogo
  cubren filtros, edición, confirmaciones, verificación y recuperación del UUID.
- Compilación de producción correcta; permanece el aviso previo de tamaño del
  paquete. Verificación visual del gráfico y del resultado de actualización.
- Consulta real de MSFT: precios hasta el 18/11/2026 y métricas del resumen
  conservadas. Repetir el request_id recuperó el mismo registro.
- HIMS se añadió como ensayo mediante verificación y confirmación, con 1254
  sesiones del 19/11/2021 al 18/11/2026. Sector Salud asignado administrativamente,
  sin balances inventados ni pertenencia al índice afirmada. La confirmación
  repetida devolvió la misma empresa.
- AAPL se actualizó desde la interfaz y su gráfico mostró precios hasta el
  18/11/2026. Tres consultas del proveedor contabilizadas por la app en esa prueba.
- La base de demostración contiene 503 empresas originales y HIMS como alta manual.
  Los quince tickers piloto originales se conservan. Evidencia técnica y captura
  fuera del repositorio, sin tokens ni contraseñas en Git.

Quedan pendientes las épicas 12 y 15, el Android anterior de la épica 11 y la
integración con Rosales. Los balances nuevos requieren una fuente accesible y
validación anual específica; los precios no sustituyen esos datos.
