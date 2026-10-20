# Hoja de ruta revisada de las 75 historias

Actualización del 19 de octubre de 2026: la propuesta inmediata de trabajo se simplificó a HU10_01, HU10_02 y HU10_03 en [Plan sencillo del sprint 2](Sprint_2_planificacion.md). El reparto de 19 historias del sprint 2 que figura abajo se conserva como referencia de una propuesta anterior, pendiente de revisar; no es el alcance comprometido del plan sencillo. La meta de completar las 75 en cuatro entregas sigue necesitando revisión de alcance y capacidad con el equipo y el profesor.

Reparto candidato de 19, 19, 19 y 18 historias. Solo el sprint 2 tiene capacidad declarada: 60 h. Su selección completa excede esa capacidad según la estimación preliminar; no se presenta ningún sprint futuro como comprometido o realizado.

| HU | Historia | Sprint candidato | Dependencia |
|---|---|---|---|
| HU01_01 | Como usuario, quiero registrarme en la plataforma web ingresando mis datos para crear una cuenta personal. | 1 | Contrato compartido de autenticación y permisos |
| HU01_02 | Como usuario/admin, quiero iniciar sesión en la web para ingresar al sistema según mi rol. | 1 | Contrato compartido de autenticación y permisos |
| HU01_03 | Como usuario, quiero solicitar el restablecimiento de clave mediante mi correo para recuperar el acceso. | 1 | Contrato compartido de autenticación y permisos |
| HU01_04 | Como usuario, quiero cerrar mi sesión activamente en la web para proteger mis datos. | 1 | Contrato compartido de autenticación y permisos |
| HU01_05 | Como usuario, quiero mantener mi sesión activa mediante JWT sin reingresar credenciales constantemente. | 1 | Contrato compartido de autenticación y permisos |
| HU02_01 | Como usuario móvil, quiero iniciar sesión en la app Android con mis datos para acceder a mi cuenta. | 1 | Contrato compartido de autenticación y permisos |
| HU02_02 | Como usuario móvil, quiero ingresar a la app usando huella dactilar para un acceso rápido. | 1 | Contrato compartido de autenticación y permisos |
| HU02_03 | Como usuario móvil, quiero registrar una cuenta directamente desde la app. | 1 | Contrato compartido de autenticación y permisos |
| HU02_04 | Como usuario móvil, quiero que la app recuerde mi sesión abierta de forma segura. | 1 | Contrato compartido de autenticación y permisos |
| HU02_05 | Como usuario móvil, quiero cerrar sesión en la app desde el menú de perfil. | 1 | Contrato compartido de autenticación y permisos |
| HU03_01 | Como usuario, quiero buscar empresas del S&P 500 por nombre o Ticker. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_02 | Como usuario, quiero ver sugerencias con autocompletado mientras escribo en el buscador. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_03 | Como usuario, quiero filtrar la lista de empresas por su sector industrial. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_04 | Como usuario, quiero ordenar los resultados de búsqueda por capitalización bursátil. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU03_05 | Como usuario, quiero consultar mi historial de búsquedas recientes. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_01 | Como usuario, quiero ver la clasificación visual del Perfil Financiero (Muy Sólido a Muy Débil). | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_02 | Como usuario, quiero acceder a la ficha técnica resumida con indicadores clave del S&P 500. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_03 | Como usuario, quiero ver un indicador visual del riesgo de solvencia de la empresa. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_04 | Como usuario, quiero ver el gráfico de evolución del precio histórico del activo. | 1 | Conservar contratos y datos documentados del sprint 1 |
| HU04_05 | Como usuario, quiero visualizar un puntaje numérico unificado sobre la salud general (0 a 100). | 2 | HU05_01 a HU05_05 |
| HU05_01 | Como usuario, quiero explorar el desglose de los 5 pilares financieros de una empresa. | 2 | Datos financieros suficientes; HU05_02 a HU05_05 para completar el radar |
| HU05_02 | Como usuario, quiero analizar el Pilar de Liquidez con ratios de corto plazo. | 2 | Datos y metodología de liquidez |
| HU05_03 | Como usuario, quiero evaluar el Pilar de Rentabilidad examinando márgenes y retorno. | 2 | Datos de rentabilidad; muestra comparable para referencia sectorial |
| HU05_04 | Como usuario, quiero revisar el Pilar de Solvencia para entender la estructura de deuda. | 2 | Datos de deuda y gastos financieros |
| HU05_05 | Como usuario, quiero inspeccionar el Pilar de Eficiencia Operativa respecto a la rotación de activos. | 2 | Histórico de activos, ingresos e inventario aplicable |
| HU06_01 | Como usuario, quiero consultar la proyección del Escenario Base para estimar retornos esperados. | 3 | HU04_04 y serie histórica suficiente |
| HU06_02 | Como usuario, quiero visualizar el Escenario Optimista bajo condiciones favorables. | 3 | HU06_01 |
| HU06_03 | Como usuario, quiero analizar el Escenario Pesimista para entender los riesgos de caída. | 3 | HU06_01 |
| HU06_04 | Como usuario, quiero manipular variables mediante deslizadores para recalculaciones personalizadas. | 3 | HU06_01 |
| HU06_05 | Como usuario, quiero comparar gráficamente los tres escenarios en un mismo plano. | 3 | HU06_01 a HU06_03 |
| HU07_01 | Como usuario, quiero leer un resumen ejecutivo generado por IA sobre la situación de la empresa. | 3 | Datos financieros y HU07_04 |
| HU07_02 | Como usuario, quiero recibir explicaciones en lenguaje claro redactadas por IA sobre los 5 pilares. | 3 | HU05_01 a HU05_05 y HU07_04 |
| HU07_03 | Como usuario, quiero ver una lista de pros y contras elaborada por IA sobre la inversión. | 3 | Datos suficientes y HU07_04 |
| HU07_04 | Como usuario, quiero consultar la procedencia y validación del análisis de IA para evaluar la confiabilidad de sus explicaciones. | 3 | Procedencia y métricas verificables |
| HU07_05 | Como usuario, quiero alternar el lenguaje de las explicaciones de IA entre un nivel simple y uno técnico. | 3 | HU07_01 o HU07_02 |
| HU08_01 | Como usuario, quiero seleccionar dos empresas simultáneamente para realizar una comparación. | 3 | HU03_01 y dos empresas con cobertura |
| HU08_02 | Como usuario, quiero examinar una tabla comparativa lado a lado con los 5 pilares de ambas empresas. | 3 | HU08_01 y HU05_01 |
| HU08_03 | Como usuario, quiero ver un gráfico de rendimiento de precios superpuesto entre las dos marcas. | 3 | HU08_01 y HU04_04 |
| HU08_04 | Como usuario, quiero leer una conclusión comparativa generada por IA sobre cuál empresa es más sólida. | 3 | HU08_02 y HU07_04 |
| HU08_05 | Como usuario Free, quiero ver una vista previa bloqueada del comparador para ser incentivado a la versión Pro. | 3 | HU08_01 y HU11_01 |
| HU09_01 | Como usuario, quiero descargar un reporte en PDF con la ficha técnica completa de la empresa. | 4 | HU04_02 y HU05_01 |
| HU09_02 | Como usuario, quiero que el PDF exportado incluya gráficos vectoriales de alta calidad. | 4 | HU09_01 y gráficos disponibles |
| HU09_03 | Como usuario, quiero que los reportes PDF incluyan los resúmenes redactados por la IA. | 4 | HU09_01 y HU07_01 |
| HU09_04 | Como usuario Pro, quiero exportar un reporte PDF que incluya la comparación entre dos empresas. | 4 | HU08_02, HU09_01 y control de plan |
| HU09_05 | Como usuario, quiero una descarga de PDF optimizada que no tarde más de 3 segundos. | 4 | HU09_01 |
| HU10_01 | Como usuario, quiero agregar empresas a mi lista de favoritos para seguimiento directo. | 2 | Sesión autenticada y catálogo; control de plan para aplicar límites |
| HU10_02 | Como usuario, quiero quitar empresas de mis favoritos cuando ya no las requiera. | 2 | HU10_01 |
| HU10_03 | Como usuario, quiero que mis empresas favoritas se sincronicen entre la Web y la App Móvil. | 2 | HU10_01 y sesión web y Android |
| HU10_04 | Como usuario, quiero revisar un historial de empresas consultadas recientemente. | 2 | HU04_02 y sesión autenticada |
| HU10_05 | Como usuario, quiero etiquetar y categorizar mis empresas favoritas en listas personalizadas. | 2 | HU10_01 y control de plan |
| HU11_01 | Como usuario, quiero realizar la simulación de pago para actualizar mi plan a Pro. | 2 | Autenticación y matriz Free y Pro |
| HU11_02 | Como usuario, quiero comparar las características de los planes Free y Pro en una tabla clara. | 2 | Definición compartida de beneficios |
| HU11_03 | Como usuario, quiero recibir un comprobante digital tras procesar la suscripción simulada. | 2 | HU11_01 |
| HU11_04 | Como usuario, quiero ver el estado de mi suscripción actual y su fecha de vencimiento. | 2 | HU11_01 |
| HU11_05 | Como usuario Pro, quiero poder cancelar mi suscripción en cualquier momento para volver a Free. | 2 | HU11_04 |
| HU12_01 | Como usuario, quiero recibir alertas cuando una empresa de mis favoritos cambie su calificación. | 4 | HU10_01 y clasificación financiera actualizable |
| HU12_02 | Como usuario móvil, quiero recibir notificaciones Push sobre eventos financieros relevantes. | 4 | HU12_01 y sesión Android |
| HU12_03 | Como usuario web, quiero revisar mis notificaciones en un centro de avisos desplegable. | 4 | HU12_01 |
| HU12_04 | Como usuario, quiero configurar mis preferencias sobre qué notificaciones deseo recibir. | 4 | HU12_01 y canales definidos |
| HU12_05 | Como usuario, quiero poder marcar notificaciones como leídas para despejar el centro de avisos. | 4 | HU12_03 |
| HU13_01 | Como Administrador, quiero visualizar la lista completa de usuarios registrados y sus planes. | 2 | Contrato compartido de autenticación y permisos |
| HU13_02 | Como Administrador, quiero cambiar manualmente el rol o plan de cualquier usuario. | 3 | Contrato compartido de autenticación y permisos |
| HU13_03 | Como Administrador, quiero inhabilitar o bloquear cuentas de usuario por mal uso. | 2 | Contrato compartido de autenticación y permisos |
| HU13_04 | Como usuario, quiero editar la información básica de mi perfil (nombre y avatar). | 2 | Contrato compartido de autenticación y permisos |
| HU13_05 | Como usuario, quiero solicitar la eliminación definitiva de mi cuenta y mis datos. | 3 | Contrato compartido de autenticación y permisos |
| HU14_01 | Como Administrador, quiero habilitar o deshabilitar empresas dentro del catálogo activo. | 3 | Catálogo y permisos administrativos |
| HU14_02 | Como Administrador, quiero ver el listado y estado de las 15 empresas seleccionadas. | 3 | Catálogo y procedencia de métricas |
| HU14_03 | Como Administrador, quiero ejecutar una actualización manual de datos de una empresa. | 4 | Fuente financiera, cuota y normalización |
| HU14_04 | Como Administrador, quiero editar los datos descriptivos de una empresa. | 4 | HU14_01 |
| HU14_05 | Como Administrador, quiero dar de alta un nuevo Ticker para ampliar el catálogo. | 4 | HU14_03 |
| HU15_01 | Como administrador, quiero consultar el consumo de tokens y llamadas a la IA para controlar el uso de la plataforma. | 4 | Instrumentación de llamadas de HU07 |
| HU15_02 | Como administrador, quiero visualizar la distribución de usuarios Free y Pro para conocer el uso de los planes. | 4 | Usuarios y planes |
| HU15_03 | Como administrador, quiero recibir avisos del consumo y agotamiento de la cuota de API para gestionar las actualizaciones. | 4 | Llamadas a la fuente financiera |
| HU15_04 | Como administrador, quiero consultar una estimación mensual simulada asociada a los planes Pro para evaluar el modelo de la plataforma. | 4 | HU11_01 y HU11_04 |
| HU15_05 | Como Administrador, quiero revisar un registro centralizado de fallas y errores (Logs). | 4 | Registro de errores y permisos administrativos |

Las dependencias dentro de un mismo sprint requieren orden de desarrollo; las cantidades no reemplazan una estimación de esfuerzo. En el sprint 3 se definen los modelos de escenarios y la integración IA antes de sus conclusiones; el comparador Pro usa planes del sprint 2. En el sprint 4 el PDF usa análisis y comparación previos; notificaciones usan favoritos.

Sprint 2: 116–190 h antes de reserva, frente a 48 h disponibles para trabajo. La distribución anterior de seis historias y 25/25 en los sprints finales queda sustituida. Se conservan los 75 identificadores; la capacidad de los sprints 3 y 4 sigue pendiente de confirmar.
