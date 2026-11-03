# Datos del primer avance

`GOOGL-annual.json` y `META-annual.json` se recuperaron sin modificar de
`docs/datos/empresas/` en el commit `93e5ea3` de la rama Rosales. Son inventarios
anuales con URL SEC, etiquetas, fechas, accesiones y hash del snapshot original.
No se volvieron a descargar ni a verificar contra la fuente en vivo en este avance.

Se importan con el comando existente `import_annual_financials`, sin reemplazar
los datos ni las funciones del Sprint 2. Los períodos 2024 de ambas empresas
van del 1 de enero al 31 de diciembre. Los datos ausentes permanecen ausentes.
Una mayor nota académica no equivale a una recomendación de compra.

Microsoft conserva el inventario del Sprint 2 y sirve para mostrar que dos cierres
etiquetados FY2024 con fechas distintas no permiten asignar un ganador.
