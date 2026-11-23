# Revisión de funcion-test

Se ejecutaron frontend y backend desde `funcion-test`. Aplicación: http://127.0.0.1:5173/. Backend: puerto 8080. No se realizaron cambios funcionales, commits ni publicaciones durante esta revisión.

## Evidencia y alcance

- Se navegaron 18 pantallas: seis de coordinación, siete de tutor y cinco de estudiante, con las cuentas de demostración existentes.
- No se detectaron errores de React ni respuestas fallidas en las consultas de esas pantallas.
- Las consultas reales de coordinación y las nuevas consultas de tutor devolvieron 200.
- Se consultó el detalle de un tutorado asignado, con respuesta 200. El tutor de demostración tiene un tutorado, un horario y ningún aviso recibido en este entorno. Cero avisos no implica un error.
- Se descargó un PDF real de coordinación, con respuesta 200 y contenido PDF.
- El perfil académico del estudiante de demostración está completo y el endpoint devolvió 200.
- Tutor y estudiante recibieron 403 al intentar consultar Inicio de coordinación; estudiante recibió 403 al consultar tutorados.
- La fusión de esta misma versión ya pasó la compilación del frontend y 186 pruebas del backend. No se repitió la suite completa porque no hubo cambios de código desde esa comprobación.
- En esta revisión no se modificaron asignaciones, cuentas, horarios ni lecturas de avisos, ni se enviaron correos. Las altas y modificaciones están respaldadas por pruebas anteriores, no por nuevas operaciones manuales sobre los datos existentes.
- Google y recepción real del correo de recuperación requieren una prueba con una cuenta controlada por su dueño; no se volvieron a comprobar aquí.

## Estado por pantalla

| Rol | Pantalla | Estado |
|---|---|---|
| Coordinación | Inicio | Indicadores y gráficas desde la base. |
| Coordinación | Tutores | Consulta y administración conectadas; búsqueda, filtros y páginas en servidor. |
| Coordinación | Estudiantes | Consulta académica conectada; búsqueda, estado, páginas y resumen en servidor. |
| Coordinación | Asignaciones | Administración conectada a la base. |
| Coordinación | Reportes | Consultas, filtros y PDF conectados. No sustituye el registro de reportes por el tutor. |
| Coordinación | Avisos | Creación, edición, destinatarios y retiro conectados. |
| Tutor | Inicio | Indicadores, estudiantes, calendario y sesiones escritos como ejemplos en el componente. |
| Tutor | Mis tutorados | Lista y detalle conectados. Registrar tutoría sigue simulando éxito y no escribe en la base. Búsqueda y filtro se realizan en el frontend. |
| Tutor | Horarios | Consulta, creación, edición y eliminación conectadas. Los horarios aún no completan una reserva de sesión desde estudiante. |
| Tutor | Sesiones realizadas | Lista y formulario usan estado local y datos de ejemplo. |
| Tutor | Reportes | Datos de ejemplo; exportaciones llaman a una acción de demostración. |
| Tutor | Mensajes | Conversaciones y cambios locales, sin servicio de mensajería. |
| Tutor | Avisos | Consulta y marcado de lectura conectados, con usuario obtenido de la sesión. |
| Estudiante | Completar datos | Consulta y guardado de carrera, periodo y semestre conectados; se abre si falta expediente. |
| Estudiante | Inicio | Próxima tutoría y actividades usan ejemplos y cambios locales. |
| Estudiante | Mi agenda | Eventos y lista de tutores usan ejemplos; altas, cambios y borrados no se guardan en el servidor. |
| Estudiante | Mis tutorías | Sesiones y actividades de ejemplo. |
| Estudiante | Mensajes | Conversaciones locales. |
| Estudiante | Avisos | Lista de ejemplo y lecturas locales; aún no consume los avisos publicados por coordinación. |

## Pendientes para completar el ciclo

1. Elección e inscripción del estudiante con tutor. Reutilizar las asignaciones existentes, con las restricciones de cuenta, periodo e inscripción; coordinación conserva su administración actual.
2. Reserva y atención de sesiones. Consultar disponibilidad real; solicitar una tutoría; confirmar, rechazar o cancelar; evitar reservas dobles; persistir agenda y estados para ambos roles.
3. Registro de tutorías realizadas. Sustituir el éxito simulado de Registrar tutoría, guardar sesión, observaciones y duración y alimentar el historial del tutor y del estudiante.
4. Reportes del tutor. Crear y consultar reportes propios y su contenido para que lleguen a Reportes de coordinación. Actualmente coordinación puede consultar reportes existentes, pero falta el flujo de producción desde tutor.
5. Inicio y tutorías del estudiante, e Inicio del tutor. Reemplazar los ejemplos por consultas de asignaciones, sesiones, reportes y horarios reales.
6. Avisos del estudiante. Consultar destinatarios reales y guardar lectura, siguiendo el módulo de tutor.
7. Mensajería. Guardar conversaciones y mensajes con permisos entre los participantes. Actualización periódica puede ser suficiente para el primer avance.
8. Cierre de integración. Probar el recorrido registro → completar datos → elegir tutor → solicitar sesión → atenderla → registrar seguimiento → consultar reporte y estadísticas, incluyendo persistencia tras recargar y permisos entre cuentas.

## Puntos a conservar o completar

- Los filtros y la búsqueda de Mis tutorados todavía se hacen en React; trasladarlos al servidor para cumplir el criterio del proyecto.
- No presentar el botón Registrar tutoría como persistente hasta conectarlo: hoy muestra éxito sin guardar.
- La creación de carrera, departamento y periodo no cuenta con pantallas de administración en los menús actuales. Confirmar cómo se mantendrán esos catálogos; el registro académico necesita opciones activas.
- Recordar sesión conserva el token pero no renueva su vencimiento. El cierre es local y el cambio de contraseña no invalida de inmediato JWT ya emitidos. Si se requiere acceso prolongado o revocación inmediata, falta ese trabajo.
- Comprobar recepción real de recuperación con una cuenta propia y acceso Google real antes de la entrega.
- Que una pantalla abra sin errores no significa que esté conectada: la clasificación anterior contrasta las consultas del navegador con su código.

Orden recomendado: elección de tutor y sesiones; registro y reportes del tutor; paneles reales; avisos del estudiante y mensajería; prueba completa entre roles.

## Avance posterior: elección de tutor

El primer pendiente quedó implementado: Mis tutorías permite elegir tutor y crea una asignación activa visible para coordinación y tutor. Se conserva la asignación al volver a entrar. Las secciones de sesiones y actividades de esa pantalla siguen pendientes del siguiente avance.

## Avance posterior: sesiones y agenda

El segundo pendiente quedó implementado: solicitud desde Mi agenda, horarios disponibles del tutor asignado, confirmación, rechazo y cancelación desde Horarios del tutor, cancelación desde estudiante y estados persistidos. Mi agenda y Mis tutorías ya muestran datos reales. Sigue pendiente el registro y cierre de tutorías realizadas del punto tres.

## Avance posterior: registro y reportes del tutor

El tercer pendiente quedó implementado: registro real desde Mis tutorados y Sesiones realizadas, cierre de sesiones confirmadas con duración y observaciones, resultado no asistió, historial compartido con estudiante y creación/completado de reportes propios visibles desde coordinación. Reportes del tutor tiene consulta, estadísticas y PDF reales. Los paneles de Inicio, avisos del estudiante y mensajes siguen en los siguientes avances.

## Avance posterior: Inicio y avisos del estudiante

Inicio del estudiante consulta sesiones reales y guarda talleres y recordatorios personales. Inicio del tutor muestra indicadores propios, estudiantes y próximas sesiones. Avisos del estudiante recibe publicaciones dirigidas por coordinación, con paginación y lectura persistente por destinatario. La mensajería y la comprobación final del sistema completo siguen pendientes.

## Avance posterior: filtros de Mis tutorados

La búsqueda por nombre o carrera y el filtro de riesgo se ejecutan en el servidor. Se pueden combinar, conservan los permisos del tutor conectado y utilizan la última medición académica. Se eliminó el filtrado local de la pantalla, conservando su diseño.

## Avance posterior: vencimiento de solicitudes

Las solicitudes sin respuesta pasan a Vencida al alcanzar su inicio. Se actualizan mediante una tarea del backend y al consultar agenda, sesiones o resumen. Se conserva el historial con un cambio automático sin atribuirlo a una persona. Las confirmadas conservan su estado. V5 agrega los valores necesarios y el actor opcional en el historial. La etiqueta y los filtros existentes muestran Vencida sin cambiar la distribución de las pantallas.

## Integración en test-aplicacion2

La rama parte de funcion-test y conserva los avances locales de Inicio, avisos, filtros de Mis tutorados y vencimiento de solicitudes. Se fusionó funcionalidad/mensajes-reportes-inicio-tutor; las demás ramas remotas revisadas ya estaban incluidas por su historial.

Inicio y reportes tenían implementaciones distintas. Se conservaron los contratos actuales, las próximas sesiones, el registro y completado de reportes, los filtros y la paginación del servidor. Se adaptaron las pruebas entrantes a estos contratos. Se incorporó la mensajería del tutor para conversaciones existentes: consulta, envío y lectura persistente. Se descartaron los tipos y servicios duplicados sin consumidores y las peticiones Bruno nuevas, manteniendo la indicación de no añadir esas pruebas todavía.

La mensajería del estudiante sigue pendiente; la integración del tutor no crea conversaciones nuevas ni incorpora WebSocket. La búsqueda de conversaciones de esta implementación entrante sigue siendo local y se limita a la lista recibida.

Verificación: 286 pruebas del backend aprobadas, compilación del frontend correcta y seis comprobaciones de navegador para mensajes reales entre cuentas temporales. Inicio y reportes respondieron correctamente con la sesión del tutor. Se limpiaron los registros temporales. Los commits usan la identidad configurada del propietario y conservan los autores originales de la rama fusionada, sin añadir coautoría del asistente. La rama quedó local, sin publicar en GitHub.
