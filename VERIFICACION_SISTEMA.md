# Verificación del sistema

Rama revisada: `funcion-test`. Frontend en `http://localhost:5173` y backend en `http://localhost:8080`. Se revisó el código local, incluyendo los últimos avances de Inicio y avisos. La mensajería quedó fuera del alcance solicitado.

## Resultado

Los recorridos principales implementados pasaron sus comprobaciones. Esto no significa que el sistema esté terminado: quedan los pendientes funcionales y las comprobaciones externas indicados abajo.

- Backend: 254 pruebas, sin fallos, errores ni pruebas omitidas.
- Frontend: compilación de producción correcta.
- Navegador Edge: 231 comprobaciones de formularios, consultas, guardado, recarga, navegación, errores y permisos sobre registros ajenos. Las pruebas de recuperación y algunos casos de error usan respuestas simuladas; los recorridos de negocio indicados como persistentes usan la API y MySQL reales.
- API y base de datos: 77 comprobaciones adicionales de acceso, roles, token alterado y estado de migraciones. Se verificaron 18 rutas de consulta como anónimo y con cada uno de los tres roles.
- Flyway: migraciones V1, V2, V3 y V4 aplicadas correctamente, sin entradas fallidas.
- Git: `git diff --check` sin incidencias. No se hizo commit ni push.

## Recorridos comprobados

| Área | Resultado |
| --- | --- |
| Acceso | Login de los tres roles; credenciales incorrectas; restauración de sesión; recordar sesión; cierre; rechazo de token alterado; error de red. |
| Registro y perfil | Alta real, rol estudiante, correo duplicado, confirmación de contraseña y guardado real de carrera, periodo y semestre. Tras recargar, el perfil completo no vuelve a solicitarse. |
| Coordinación: tutores | Crear, editar, desactivar y reactivar; contraseña conservada al editar; cuenta inactiva sin acceso; filtros, búsqueda y páginas del servidor. |
| Coordinación: estudiantes | Consulta, filtros y páginas del servidor; resumen global; promedio cero y datos académicos ausentes. |
| Coordinación: asignaciones | Paginación, fallos de red y reintento en navegador. Alta, cambios, finalización, cancelación y restricciones cubiertas además por pruebas del backend. |
| Coordinación: reportes | Filtros, consulta y PDF individual/general, incluidos documentos de varias páginas. Recepción del reporte creado por tutor. |
| Coordinación: avisos | Crear, editar, seleccionar destinatarios y retirar; búsqueda, filtros y paginación. No se enviaron comunicaciones externas. |
| Coordinación: Inicio y navegación | Indicadores y gráficas contrastados con la API; reintento; navbar en las seis pantallas y cierre desde el menú de usuario. |
| Estudiante: elegir tutor | Consulta y búsqueda paginada, inscripción real, persistencia, rechazo de duplicados y visibilidad de la asignación en coordinación y tutor. |
| Sesiones y agenda | Solicitar, confirmar, rechazar y cancelar; historial compartido; liberación de horario; dos solicitudes simultáneas generan una reserva y un rechazo. |
| Tutor: registro | Registro de tutoría realizada, cierre de sesión confirmada, duración y observaciones persistentes, detalle e historial visibles para estudiante. |
| Tutor: reportes | Crear pendiente, completar, consultar, exportar y recibirlo en coordinación; resumen de sesiones y duración contrastado con los registros. |
| Inicio del estudiante | Próxima sesión real, exclusión de canceladas, talleres y recordatorios: crear, editar, eliminar y conservar tras recargar. Actividades ajenas protegidas. |
| Inicio del tutor | Indicadores propios, estudiante asignado, próximas sesiones y navegación desde los accesos a sus secciones. |
| Avisos del estudiante | Publicación real desde coordinación, destinatarios, páginas del servidor, lectura individual y de todas las páginas, conservación al recargar y retiro. La lectura de un destinatario no cambia la de otro. |
| Avisos y horarios del tutor | Consulta/lectura y administración de horarios cubiertas por pruebas del backend; solicitudes, confirmación y rechazo comprobados también en navegador. |
| Recuperación | Interfaz de dos pasos y errores con respuestas simuladas; correo inexistente validado contra el backend real. Caducidad, intentos, uso único y cambio de contraseña cubiertos por pruebas del backend con envío simulado. |

Las comprobaciones de navegador incluyeron ausencia de errores de React y desbordamiento horizontal en las resoluciones específicas usadas por cada recorrido; no sustituyen una revisión en todos los dispositivos.

## Hallazgos y pendientes

### Google requiere revisar la configuración externa

El ID configurado coincide entre frontend y backend y con el proporcionado. Sin embargo, en esta ejecución la biblioteca de Google devolvió `The given origin is not allowed for the given client ID` al probar tanto `http://localhost:5173` como `http://127.0.0.1:5173`.

Las pruebas del backend de validación de credenciales Google pasan, pero no se completó una autenticación real con una cuenta de Google. Hay que comprobar en Google Cloud los orígenes autorizados del cliente exacto y volver a realizar el acceso desde el navegador habitual. No se modificó la configuración de Google Cloud ni se usó una cuenta personal.

### Mis tutorados: filtros corregidos

La búsqueda por nombre o carrera y el filtro de riesgo se ejecutan ahora en el backend, combinados y limitados al tutor identificado por el JWT. React envía los parámetros y muestra la lista recibida. Se conserva la respuesta como lista y el diseño existente; este cambio no añade paginación.

Las siete pruebas nuevas cubren búsqueda, última medición de riesgo, combinación de criterios, datos sin evaluación, permisos, caracteres especiales y filtros inválidos. La suite completa pasa con 261 pruebas y el frontend compila correctamente.

### Solicitudes sin respuesta: corregido

La migración V5 permite guardar el estado Vencida y su cambio automático en el historial. Una tarea cada 30 segundos procesa hasta 100 pendientes; las consultas de sesiones, agenda y resumen actualizan las solicitudes del usuario antes de devolver resultados. Se usa el inicio como límite, se preservan las confirmadas y cada vencimiento genera un solo cambio sin actor humano.

Las seis pruebas nuevas verifican el límite exacto, filtros y permisos de acciones, historial único, consultas propias, agenda y resumen, preservación de confirmadas y repetición del proceso automático. La suite completa pasa con 267 pruebas y el frontend compila correctamente. Se aprobaron además 18 comprobaciones en navegador: vencimiento sin consultar pantallas, conservación de confirmadas, filtro Vencida para ambos roles, ausencia de acciones inválidas e historial único, junto con el recorrido de reservas. Los registros temporales se eliminaron.

### Correo y sesión

Gmail aceptó STARTTLS y la autenticación SMTP con la configuración actual. No se envió un correo ni se comprobó recepción en una bandeja real. Sigue pendiente verificar la entrega de un código con una cuenta propia.

Recordar sesión conserva el JWT en el navegador, pero no renueva su vencimiento. El cierre es local y cambiar la contraseña no revoca inmediatamente los JWT ya emitidos. Son limitaciones actuales, no fallos de las comprobaciones ejecutadas.

### Otros puntos

- Mensajería: pendiente y excluida de esta verificación.
- Los catálogos de carreras, departamentos y periodos no tienen pantallas propias de administración; necesitan datos activos para completar el perfil.
- Vite advierte que el archivo JavaScript principal supera 500 kB. La compilación termina correctamente; puede optimizarse separando la carga por pantallas.

## Evidencia y limpieza

El resultado de Maven está en `backend/verificacion-completa.log`. Las comprobaciones de navegador y API están en los archivos ignorados `frontend/.mysql-tools/verificacion-*.log`.

Se usaron cuentas y registros temporales. Una limpieza de reservas coincidió con el registro de otro caso de prueba y fue bloqueada por las claves foráneas; se limpiaron después los registros concretos y se repitió reservas por separado, con resultado correcto. La consulta final no encontró cuentas temporales recientes restantes. No se eliminaron cuentas de demostración ni se cambiaron contraseñas de usuarios existentes.

No se añadieron pruebas Bruno, comentarios de código ni textos a las pantallas. Esta revisión no cambió el diseño ni implementó los pendientes anteriores.
