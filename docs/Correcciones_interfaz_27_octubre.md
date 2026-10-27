# Correcciones de interfaz — 27 de octubre de 2026

Rama: `magallanes-sprint2-simulaciones`. Se aplicaron las prioridades de la revisión de uso del 27 de octubre, manteniendo la arquitectura MVVM y los contratos financieros existentes.

## Cambios comprobados

- La consulta, sector, orden, página y filtro de cobertura se conservan en la URL. Abrir una empresa y volver recupera el estado anterior; recargar el explorador también lo conserva.
- La búsqueda distingue una respuesta vacía de un fallo de conexión. Reintentar y Buscar vuelven a solicitar la misma consulta después de un error.
- El filtro de indicadores cargados se aplica en Django antes de paginar. Incluye un valor cero válido y comprueba las métricas que entrega el catálogo: capitalización, PER y ROE. Sus resultados tienen una clave de caché independiente del catálogo completo.
- Las tarjetas del catálogo son compactas y no repiten varios avisos de ausencia de datos. El acceso al ejemplo de Microsoft conserva la búsqueda de origen.
- El encabezado distingue el balance del resumen. En Salud financiera y Explicación de IA se muestra el ejercicio elegido dentro de esas secciones, sin presentar el balance fijo de 2024 como si correspondiera al año seleccionado.
- El perfil de Altman-Z utiliza etiquetas académicas y muestra su alcance. El puntaje de cinco pilares se distingue de una probabilidad de ganancia y de Altman-Z.
- Los cinco pilares se abren de uno en uno. Los enlaces del radar y de las notas abren el detalle correspondiente. Fórmulas, componentes y series siguen disponibles.
- El encabezado móvil es más compacto y la navegación de la ficha permanece accesible al desplazarse. Las empresas sin datos del resumen presentan un aviso único.
- Las simulaciones indican que el horizonte empieza en el último cierre importado. Hay ayuda breve para percentiles y supuestos, y colores de referencia junto a la leyenda.
- La IA se presenta como guía de interpretación y muestra una cifra referenciada junto a cada observación. Conserva el alcance de vocabulario financiero revisado.
- Las validaciones locales y los mensajes de respaldo de los formularios están en español. Los errores se asocian a los campos mediante `aria-describedby` y `aria-invalid`; se añadieron anuncios de error/estado.
- Las contraseñas pueden mostrarse y ocultarse sin perder el contenido ni enviar el formulario. El registro distingue los campos opcionales, explica su tipo de cuenta y muestra los requisitos fuera del placeholder. El restablecimiento permite solicitar otro enlace.

## Verificación de esta revisión

- 74 pruebas frontend aprobadas en 18 archivos; compilación de producción correcta.
- 23 pruebas del servidor aprobadas para búsqueda, filtros, paginación y caché, con PostgreSQL aislado. Se repitieron las pruebas pertinentes a este cambio; no se presenta esta cifra como una nueva ejecución de toda la suite backend.
- Recorrido real en Google Chrome: acceso, filtros, retorno desde la ficha, recarga, fallo de red simulado, ambos mecanismos de reintento, cambio de ejercicio, acordeones desde el radar, simulación, IA validada y estados sin datos.
- Comprobación a 1440 y 390 píxeles, sin desbordamiento horizontal en las vistas inspeccionadas ni excepciones JavaScript registradas. Se revisaron las capturas.

| Vista móvil de 390 píxeles | Alto anterior | Alto comprobado | Reducción aproximada |
|---|---:|---:|---:|
| Explorador, página del catálogo completo | 9208 px | 4583 px | 50 % |
| Salud financiera, detalle inicial de Liquidez | 5398 px | 3070 px | 43 % |
| Resumen de Microsoft | 3332 px | 2821 px | 15 % |

Los valores describen este conjunto de datos, tamaño de pantalla y estado de los desplegables; no son una promesa para todas las empresas o dispositivos. La compilación aún recomienda dividir el paquete JavaScript para optimizar la carga inicial.

La comprobación de navegador se repite desde `frontend` con `node scripts/verify_ui_review.cjs`, después de iniciar la demostración y preparar su cuenta local. Las capturas permanecen fuera del repositorio, en `../output/ui_correcciones_27_octubre`. El resumen de evidencia sin credenciales figura en `docs/evidencias/ui_correcciones_27_octubre.json`.

## Alcance pendiente

La cobertura financiera completa de la demostración sigue siendo Microsoft. No se certificó Android ni una sesión de administrador con permisos durante esta revisión; esas comprobaciones conjuntas y la ampliación de datos conservan su estado pendiente.
