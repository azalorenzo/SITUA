# Avisos: indicaciones para integrar las pantallas de destinatarios

Coordinación ya guarda avisos en `avisos` y su lista de destinatarios en `aviso_destinatarios`, dentro de una misma transacción. No se envían correos, SMS. No hay migraciones nuevas para este módulo.

## Qué debe consultar tu módulo

La vista existente `v_avisos_visibles` devuelve el contenido del aviso para cada destinatario. Excluye los avisos retirados y los que ese usuario haya ocultado. Para listar los avisos de la persona conectada, usa una consulta preparada como esta:

```sql
SELECT aviso_id, titulo, descripcion, categoria, prioridad,
       sesion_id, publicado_en, leido_en
FROM v_avisos_visibles
WHERE usuario_id = ?
ORDER BY publicado_en DESC, aviso_id DESC
LIMIT ? OFFSET ?;
```

El primer parámetro es `usuarios.id`, obtenido del JWT validado por el backend. No es `estudiantes.id` ni `tutores.id`. No debe elegirse libremente desde React. Agrega el conteo y los filtros que necesite tu pantalla en el backend.

Implementa tus rutas para estudiante o tutor. Las rutas `/api/coordinacion/avisos` están reservadas a coordinación y no deben usarse desde las pantallas de los destinatarios.

## Tablas y estados

- `avisos`: `id`, `autor_id`, `titulo`, `descripcion`, `categoria`, `prioridad`, `sesion_id`, `publicado_en`, `retirado_en` y auditoría. La prioridad es `normal`, `media` o `alta`. Los avisos generales nuevos no tienen sesión vinculada.
- `aviso_destinatarios`: clave compuesta `(aviso_id, usuario_id)`, `leido_en` y `ocultado_en`. Las dos fechas empiezan en NULL. Cada usuario aparece una sola vez por aviso, aunque tenga varios roles.
- `retirado_en IS NOT NULL`: el aviso se conserva en el historial, pero deja de aparecer en la vista visible para todos sus destinatarios.
- `leido_en`: lectura de ese destinatario. Abrir el detalle desde coordinación no la modifica.
- `ocultado_en`: ocultación personal, independiente del retiro general que hace coordinación.

La selección por grupos incluye estudiantes o tutores activos al momento de guardar. Es una lista fija: registrar un usuario después no lo agrega a avisos anteriores. También hay selección individual. Al editar, coordinación puede conservar la lista actual o reemplazarla; las lecturas y ocultaciones se mantienen para los usuarios que continúan como destinatarios.

## Lectura y ocultación

La consulta y las acciones para marcar leído u ocultar en estudiante/tutor quedan a tu cargo. Usa siempre el usuario validado del JWT y comprueba que el aviso siga visible para esa persona. Actualiza solo su fila en `aviso_destinatarios`, sin marcar lecturas ni ocultaciones de otros destinatarios. No necesitas modificar la estructura de las tablas ni la pantalla de coordinación.

Las fechas almacenadas están en UTC; presenta las fechas en la zona del navegador. No uses `envios_comunicacion` para estos avisos internos.
