# Diseño del inicio de sesión

Se reemplazó `Login.tsx` por `src/app/components/InicioSesion.tsx`. La pantalla recrea la tarjeta dividida, los colores y los campos de la referencia, y añade el acceso con Google, preparado para activarse con su ID de cliente.

El formulario por correo y contraseña ahora se conecta al backend con JWT y abre las pantallas por rol. La casilla de recordar sesión determina el almacenamiento del token. El registro crea estudiantes y abre su sesión. Google está conectado en código y desactivado mientras falte el ID de cliente; al activarlo utiliza el botón oficial. La recuperación continúa pendiente. El selector de perfiles del login anterior fue retirado; el rol se obtiene del servidor.

La presentación se comprobó en 1440×1000, 882×565 y 390×844. El frontend compiló correctamente. La integración actual y sus pruebas se explican en el README general.

## Fotografía

Archivo final: `frontend/src/assets/biblioteca-acceso.png` dentro del repositorio.

Se generó con la herramienta integrada de imágenes para este diseño; no se copió la fotografía de la captura de referencia. Se conserva como un recurso local de la aplicación.

Prompt utilizado:

> Use case: photorealistic-natural. Asset type: vertical photograph for the left panel of a university academic login page. Create a realistic candid editorial photograph, portrait 4:5 composition, of four diverse college students sitting around a light wooden study table, with a friendly female academic tutor standing between them leaning slightly over an open book. Students collaborate with notebooks and one laptop. They occupy the lower half to two-thirds of the photograph. The upper half shows an airy bright modern university library, tall windows to the left, pale wood bookshelves and a curved mezzanine balcony with thin dark railings. Warm natural daylight, honey beige wood, soft neutral clothing with a muted burgundy cardigan on the right student. Camera is level from across the table, slightly wide, natural faces and hands, believable professional university photography. Entire image is ONLY the photo, edge to edge. No text, no logos, no user interface, no borders, no watermark.
