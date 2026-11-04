# Sprint 3 · HU10_02 / T12.4 · Baja autorizada de favoritos

Avance del miércoles 4 de noviembre. El servidor compartido admite
`DELETE /api/v1/favorites/{id}/`, donde `id` es el identificador del
favorito devuelto al guardarlo o consultarlo, no el de la empresa.

| Caso | Resultado esperado y verificado |
| --- | --- |
| El dueño quita su favorito | 204 sin cuerpo. Ya no aparece al volver a consultar. |
| El dueño repite la baja | 404; el favorito ya no existe. |
| Otra cuenta intenta quitarlo | 404 con la misma respuesta que un ID inexistente. |
| Una solicitud llega sin sesión | 401. No cambia la lista. |
| Dos cuentas guardaron la misma empresa | Quitar el favorito de una no cambia el de la otra. |

Pasaron 26 pruebas de favoritos, acceso y comparador en PostgreSQL. Se
verificaron también las respuestas del servicio activo con cuentas de
demostración. El contrato de baja coincide con el cliente Android
revisado en `Rosales`, commit `93e5ea3`: 204 confirma la baja y, ante
404, el cliente vuelve a consultar su propia lista.

La copia Android de ese commit se compiló con el servidor de la demo
y sus pruebas unitarias dieron 132 aprobadas y 3 omitidas.

La interfaz web para quitar y confirmar corresponde a T12.3. Este
avance no da por cerrada la sincronización automática de HU10_03.
