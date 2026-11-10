# Avance para el martes 3 de noviembre

## Qué vas a presentar

Dos funciones conectadas al servidor: **guardar empresas en favoritos** y
**seleccionar dos empresas para consultar el mismo ejercicio**. Se conservan los
datos y las funciones anteriores del Sprint 2.

| Función | Tareas de Diego | Qué puedes demostrar |
|---|---|---|
| HU10_01 · Agregar favoritos | T11.3 web y T11.4 servidor | Guardar desde tarjeta o ficha; consultar lista personal sin duplicados |
| HU08_01 · Elegir dos empresas | T01.3 web y T01.4 servidor | Buscar con teclado, elegir distintas, intercambiar, quitar y recargar conservando selección |
| Requisito de HU08_05 · Acceso | T05.4 servidor; vista previa en la web | Free prepara selección; Pro consulta; el servidor bloquea el acceso sin permiso |

La tabla de notas inicia HU08_02, pero no cierra esa historia ni las demás del
sprint. No se presentan pagos, comparativo de precios, IA comparativa, PDFs,
eliminación de favoritos, sincronización automática, recientes ni etiquetas como
funciones terminadas. Android requiere aún la prueba conjunta en emulador o equipo.

## Cómo abrirlo

1. La demo usa una base nueva, `sprint3_demo_validation`. No cambia la base de
   presentación del Sprint 2. Si los servicios siguen activos, abre
   <http://127.0.0.1:55193/login>.
2. Si están apagados, ejecuta `tools/Start-Sprint3-Demo.ps1` desde la carpeta del
   proyecto. Necesita el entorno local de Python, PostgreSQL, Redis y las
   dependencias web ya preparados en esta computadora.
3. Las dos cuentas de prueba están en `presentation_runtime_sprint3/demo_credentials.json`,
   en la carpeta superior al repositorio, fuera de Git. Usa la cuenta `pro`
   para el comparador y `free` para demostrar el bloqueo. No muestres contraseñas.
   Las contraseñas elegidas en ese archivo se conservan al preparar nuevamente
   la demo; no se cambian cada vez que la inicias.

## Demostración de 4 minutos

1. **Favoritos:** entra con Pro. En Explorar, busca una empresa y pulsa Guardar
   favorito. Abre Favoritos y recarga: sigue allí. Abre la ficha: muestra Guardado.
   Si Microsoft ya quedó guardada durante las pruebas, usa otra, por ejemplo AAPL.
2. **Selección:** abre Comparar. Busca GOOGL como empresa A y META como B. Puedes
   usar flecha abajo y Enter. Mantén FY2024. Se muestran las fechas y las notas
   disponibles; no se rellenan los indicadores que faltan.
3. **Conservación:** recarga, intercambia A/B y quita una empresa. La selección está
   en la dirección de la página. Intentar elegir dos veces la misma empresa se rechaza.
4. **Fechas:** selecciona MSFT y META en FY2024. Ambas dicen 2024, pero Microsoft
   cierra en junio y Meta en diciembre. Por eso no se asignan ganadores.
5. **Privacidad y acceso:** cierra sesión y entra con Free. Favoritos muestra la
   lista de esa cuenta. El comparador ofrece vista previa y el servidor rechaza
   consultar los datos comparados con esa cuenta.

## Qué decir al profesor

“Mi parte de este avance conecta la web y el servidor con dos historias: guardar
favoritos y seleccionar dos empresas. Los favoritos pertenecen a cada cuenta y
persisten al recargar. El comparador conserva empresas y ejercicio, valida el
acceso Pro y comprueba las fechas antes de comparar. Si faltan indicadores o las
fechas no coinciden, lo informa. Angel trabaja Android; falta validar juntos su
cliente contra este servidor. Las siguientes tareas son completar el comparador,
los reportes y las funciones restantes de favoritos.”

## Trabajo de Angel revisado

Se revisó `origin/Rosales` en `93e5ea3`. Tiene código cliente para las 30 tareas
Android. Su informe registra 132 pruebas JVM aprobadas y 3 opcionales omitidas.
Esas pruebas no fueron reejecutadas aquí y no sustituyen una prueba en dispositivo.

Las rutas nuevas respetan sus nombres y formatos: `user/subscription/`,
`compare/?a=&b=&fiscal_year=` y `favorites/`. Se reutilizan dos inventarios públicos
de su rama, con procedencia documentada. No se fusionó todo su servidor porque
las dos ramas tienen migraciones diferentes desde 0005.

La siguiente prueba conjunta es conectar el Android de Angel al mismo servidor
de la demo y contrastar cuenta, empresas, ejercicio y favoritos. No se cambió
su configuración Android ni se da por cerrada esa integración.

## Evidencias y pruebas

Se guardan los resultados reproducibles y las capturas en la carpeta superior
`output/sprint3_advance/`. El recorrido usa inicio de sesión, web, PostgreSQL y API
reales. `tools/verify_sprint3_advance.cjs` repite esas comprobaciones sin publicar
contraseñas. El resultado automático no equivale a aceptación de Android.

Verificación del 2 de noviembre: **369 pruebas del servidor aprobadas**, una
prueba opcional de rendimiento Redis omitida, **86 pruebas web aprobadas**, build
web correcto y **13 comprobaciones reales en Edge aprobadas**. La ejecución
completa del servidor usa PostgreSQL, Redis aislado y un receptor local de correo.
La primera prueba general con SQLite no sirve como validación de las consultas
específicas de PostgreSQL; la ejecución final se hizo con los servicios adecuados.

El comparador usa las mismas notas `pillars-v1` del Sprint 2. Compara solo períodos
completos y versiones coincidentes. La diferencia A−B y los empates usan notas
HALF_UP de dos decimales. GOOGL/META FY2024 tiene notas de liquidez, rentabilidad y
crecimiento; solvencia y eficiencia quedan sin nota por indicadores ausentes.

## Cómo seguir hasta el martes 10

- **3–4 de noviembre:** comprobar estas dos historias con Android; continuar
  tabla de pilares, histórico comparativo y acceso Pro.
- **5–6:** PDF de empresa y eliminar favoritos; sincronización entre clientes.
- **7–8:** continuar análisis/PDF comparativos, gráficos, recientes y etiquetas,
  según el avance verificado de las prioridades.
- **9:** integración, pruebas y evidencias de lo implementado; registrar pendientes.
- **10:** presentación final del sprint. No marcar una historia como cumplida
  solo porque terminó el plazo.

El PDF contiene 15 HU / 60 tareas, con ocho historias prioritarias. El alcance
restante es considerable; se revisará el 5 de noviembre y se reportará cualquier
pendiente con claridad. Este primer avance no es el cierre del Sprint 3.
