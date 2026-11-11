# Diseño del inicio de sesión

Se reemplazó `Login.tsx` por `src/app/components/InicioSesion.tsx`. La pantalla recrea la tarjeta dividida, los colores y los campos de la referencia, y añade un botón visual para continuar con Google.

El formulario no envía peticiones ni cambia de pantalla. Google, recuperación y registro no tienen integración. Mostrar/ocultar contraseña y la casilla de recordar sesión son únicamente controles locales; no se persiste ninguna sesión. El selector de perfiles del login anterior fue retirado.

La presentación se comprobó en 1440×1000, 882×565 y 390×844. El frontend compiló correctamente. No se modificó el backend.

## Fotografía

Archivo final: `frontend/src/assets/biblioteca-acceso.png` dentro del repositorio.

Se generó con la herramienta integrada de imágenes para este diseño; no se copió la fotografía de la captura de referencia. Se conserva como un recurso local de la aplicación.

Prompt utilizado:

> Use case: photorealistic-natural. Asset type: vertical photograph for the left panel of a university academic login page. Create a realistic candid editorial photograph, portrait 4:5 composition, of four diverse college students sitting around a light wooden study table, with a friendly female academic tutor standing between them leaning slightly over an open book. Students collaborate with notebooks and one laptop. They occupy the lower half to two-thirds of the photograph. The upper half shows an airy bright modern university library, tall windows to the left, pale wood bookshelves and a curved mezzanine balcony with thin dark railings. Warm natural daylight, honey beige wood, soft neutral clothing with a muted burgundy cardigan on the right student. Camera is level from across the table, slightly wide, natural faces and hands, believable professional university photography. Entire image is ONLY the photo, edge to edge. No text, no logos, no user interface, no borders, no watermark.
