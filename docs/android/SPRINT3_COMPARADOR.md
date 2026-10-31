# Sprint 3 Android

Base: Script 2, conservando su historial en Rosales. Script 1 y Script 2 no se modifican.
Alcance: solo las tareas Android de HU08_01 y HU08_02. No se implementan Web, pagos, IA comparativa ni gráficos de rendimiento.

## T01.1 Selector dual en Android

Se incorpora Comparar desde Explorar y desde la ficha de una empresa. Hay dos buscadores independientes, hasta cinco sugerencias reales, espera de 300 ms al escribir, validación de empresas distintas, quitar e intercambiar. La selección y los textos se guardan con SavedStateHandle para restauración del estado. Al entrar desde una ficha, esa empresa se propone como A una sola vez.

Comprobación manual: buscar AAPL en A y MSFT en B; intentar seleccionar AAPL dos veces; intercambiar y comprobar que los nombres acompañan a los símbolos; quitar y girar el emulador. Abrir Comparar desde una ficha y comprobar la empresa inicial.

Las pruebas JVM cubren identificadores, duplicados, quitar, intercambio, búsquedas independientes y restauración del ViewModel. Las pruebas de interfaz requieren ejecutar el proyecto en Android Studio y no se confunden con pruebas JVM.

## T01.2 Consulta y pruebas del comparador móvil

Se añade una llamada Retrofit autenticada con A, B y fiscal_year, validación de identidad y ejercicio, progreso, error y reintento. La selección permanece tras un fallo. Cambiar el par o el ejercicio cancela la consulta e invalida el resultado anterior; una respuesta tardía no sustituye otra selección. El ejercicio se restaura con SavedStateHandle. La respuesta del comparador no se guarda como caché sin revalidarla al recrear el proceso.

Pruebas de contrato con respuestas HTTP controladas cubren autenticación, parámetros, orden A/B, datos de otra empresa, ejercicio incorrecto, JSON inválido y recuperación de red. Pruebas del ViewModel cubren reintento, selección incompleta y respuesta tardía. El contrato propuesto para coordinar con Diego está en CONTRATO_COMPARADOR.md.

## Dependencia del servidor

La copia del servidor de Script 2 tiene búsqueda, ficha y pilares por empresa, pero no tiene una ruta de comparación. La integración Android se documentará sin dar por realizado el servidor de Diego ni la aceptación conjunta de Web y Android.

## Ejecutar

Abrir `Script 3/android` en Android Studio, seleccionar JDK 17 y ejecutar app. Se conserva la dirección de API del emulador `http://10.0.2.2:8000/api/v1/`. Puede usarse el servidor ya preparado de Script 2; para restaurar datos en la copia de Script 3 seguir `tools/Start-AndroidServer.ps1 -Prepare` con un entorno Python configurado, según README_Script_2.md. No se copian bases personales, claves, modelos de Ollama ni entornos virtuales.
