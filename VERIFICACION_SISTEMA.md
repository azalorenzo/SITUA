# Verificación del sistema

## Dictamen

El núcleo funcional de tutorías está integrado y pasó las pruebas ejecutadas. El sistema todavía no está completo para la entrega descrita en las indicaciones del curso. La revisión inicial utilizó código, peticiones HTTP, pruebas del backend y MySQL. La comprobación posterior de interfaz añadió Edge automatizado en escritorio y móvil.

## Resultado reproducible

| Comprobación | Resultado |
| --- | --- |
| Suite del backend | 309 pruebas aprobadas, sin fallos, errores ni omisiones. |
| Frontend | Compilación de producción correcta. Advertencia por archivo JavaScript principal mayor de 500 kB. |
| Recorrido real por API | 100 comprobaciones aprobadas, usando cuentas y registros temporales. |
| Acceso, roles y migraciones por API | 97 comprobaciones aprobadas; incluye 23 rutas consultadas como anónimo y con cada rol, y rechazo de JWT alterado. |
| MySQL | MySQL Community Server 26.10.0; siete migraciones aplicadas correctamente. |
| Integridad examinada | 49 claves foráneas; cero mensajes sin un participante válido; cero contraseñas no nulas fuera del formato BCrypt. No equivale a auditar todo el historial de Git ni toda la seguridad del servidor. |
| Correo | Conexión STARTTLS y autenticación SMTP correctas. No se envió un correo ni se verificó recepción real. |
| Limpieza | Se eliminaron los registros temporales de esta revisión. La consulta final no encontró cuentas QA recientes restantes. |

Los datos principales existentes incluyen 15 tutores, 19 estudiantes, 17 asignaciones, 15 sesiones, 15 reportes, 15 avisos, 15 conversaciones y 30 mensajes. No se volvieron a cargar seeds ni se borraron datos de demostración.

## Funciones comprobadas

- Acceso de los tres roles, registro exclusivo como estudiante, rechazo de correo duplicado y consulta de sesión.
- Alta real del tutor desde coordinación; desactivación y reactivación. Una cuenta inactiva no puede iniciar sesión ni usar las consultas de tutor con el token anterior.
- Perfil académico obligatorio: guardado y consulta de carrera, periodo y semestre.
- Elección de tutor: asignación persistente, duplicado rechazado, visibilidad en coordinación y consulta de tutorados limitada al tutor conectado.
- Mensajes: creación concurrente de una sola conversación, intercambio real entre roles, identidad del emisor tomada del JWT, historial y lecturas persistentes, rechazo de acceso ajeno.
- Horarios y reservas: creación de disponibilidad, consulta por el estudiante asignado, solicitud, protección contra duplicados, confirmación, cancelación, liberación del horario y rechazo de una nueva solicitud.
- Inicio del estudiante: próxima sesión real y actividades personales guardadas, editadas y eliminadas con permisos de propietario.
- Registro de una tutoría ya realizada: duración de 60 minutos y observaciones visibles en el historial del estudiante.
- Reportes: creación pendiente por tutor, completado, recepción desde coordinación y PDF válido en ambos roles.
- Avisos: publicación seleccionando destinatarios, recepción en los dos roles, lectura individual sin afectar al otro destinatario y retiro desde coordinación.
- Inicio del tutor y de coordinación; análisis del tutor; finalización de asignaciones e historial de mensajes conservado.
- Recuperación: correo inexistente rechazado contra la API real. Los casos de códigos, caducidad, intentos y cambio de contraseña se prueban en la suite del backend con envío simulado.
- La suite también cubre cierre de sesiones confirmadas, no asistencia, vencimiento de solicitudes, consultas sin datos, errores y concurrencia de reservas. La prueba HTTP registra una tutoría pasada directamente mediante la función prevista para ello; no simula que una reserva futura ya se realizó.

## Fallo encontrado y corregido

Al consultar Mensajes simultáneamente como estudiante y tutor, la restricción única evitaba duplicar la conversación, pero el aislamiento transaccional podía ocultar en una de las respuestas los participantes recién creados por la otra transacción. La respuesta inicial podía venir vacía hasta el siguiente refresco.

Las consultas que aseguran y devuelven conversaciones ahora usan READ_COMMITTED. Se añadió una prueba con dos transacciones simultáneas, repetida cinco veces, que exige que ambas respuestas iniciales incluyan la conversación y que solo exista una pareja. Pasó junto con toda la suite y con el recorrido HTTP real.

## Pendientes que impiden declarar terminada la entrega

### Seguridad y acceso

- Backend resuelto: POST /api/autenticacion/cerrar-sesion invalida el JWT presentado y el validador lo rechaza desde la siguiente petición. React ya lo llama antes de limpiar el acceso y permite reintentar ante un fallo.
- Backend resuelto: recuperar la contraseña aumenta version_sesion y rechaza todos los JWT anteriores. Un código incorrecto no invalida las sesiones y los nuevos inicios de sesión siguen funcionando.
- Backend resuelto: SolicitudCambiarContrasena exige longitud, mayúscula, número y carácter especial mediante Bean Validation. Las pruebas usan una cuenta temporal y envío simulado; no se cambió la contraseña de cuentas existentes.
- La autenticación real con Google no se volvió a realizar: requiere una cuenta del propietario y está fuera de una verificación sin navegador. La validación de credenciales Google tiene pruebas automatizadas; eso no acredita la configuración de orígenes ni un acceso real.

### Pantallas y listados

- Mis tutorados utiliza /api/tutor/estudiantes/pagina; búsqueda, riesgo y paginación se resuelven en SQL.
- Avisos del tutor utiliza /api/tutor/avisos/pagina, con búsqueda, lectura, prioridad y páginas desde el servidor. Los contadores de lectura y prioridad son globales del destinatario.
- Horarios consulta el mes y el día por desde/hasta y obtiene las tarjetas mediante /api/tutor/horarios/resumen. Errores y confirmaciones se muestran dentro de la aplicación.
- Aceptar y Rechazar desde Avisos cambian la sesión real; solo aparecen en solicitudes futuras propias que siguen pendientes.
- Los tres roles comparten navbar persistente, menú de perfil con cierre y navegación lateral móvil.
- Mensajes muestra los 200 mensajes recientes y hasta 100 conversaciones; no tiene navegación del historial anterior ni avisos globales fuera de esa pantalla. Las llamadas, videollamadas y adjuntos están desactivados. No se requieren para demostrar chat de texto, pero no deben presentarse como funciones disponibles.
- Existen componentes antiguos con mocks, como Students y Sessions, pero no están importados por los recorridos internos actuales de App, StudentApp y TutorApp. No son pruebas de que las pantallas conectadas sigan simuladas.
- Se comprobaron las 18 pantallas a 1440 y 390 píxeles, apertura y cierre del menú móvil y capturas de Inicio, Horarios, Mensajes y Avisos. No se detectó desbordamiento horizontal; esto no sustituye una auditoría completa de accesibilidad.

### Comunicación y despliegue

- No se encontró implementación de SMS ni WhatsApp disparada por acciones del sistema. El chat interno no sustituye WhatsApp.
- El correo local usa Gmail. La consigna exige Postfix en el VPS, JavaMailSender apuntando a ese servidor y registros SPF/DKIM; todavía no está acreditado ese cumplimiento ni la entrega de un correo a una bandeja real.
- No se verificó un despliegue operativo en VPS con JAR y systemd, frontend estático, MySQL, Nginx o Apache, HTTPS y Certbot. El README presenta direcciones locales y no acredita una URL pública de entrega.

### Documentación y evaluación

- Actualizar el diagrama ER y el respaldo de estructura con las migraciones actuales. La fuente ER todavía declara actor_id obligatorio y no muestra los campos agregados para conversaciones y lectura. Los respaldos versionados v2/v3 son anteriores a las migraciones actuales.
- El README conserva algunos párrafos antiguos contradictorios con avances posteriores. El estado inicial se corrigió durante esta revisión, pero la documentación debe consolidarse antes de entregar.
- No se encontraron en el README los enlaces de entrega al prototipo navegable de Figma ni al tablero público de GitHub Projects. Su existencia y configuración externas no se certificaron aquí.
- Contrastar la gestión de coordinación con el requisito de administrador con CRUD de todos los módulos: actualmente se administran tutores, asignaciones y avisos, pero Estudiantes es una consulta y no hay administración general de cuentas de coordinación y roles desde las pantallas.
- Las pruebas Bruno para la API desplegada siguen aplazadas por indicación del usuario. No se crearon ni ejecutaron pruebas Bruno en esta revisión.
- Los últimos cambios locales, incluida la integración del estudiante y la corrección de concurrencia, todavía no están publicados en GitHub.

## Orden propuesto para cerrar

1. Postfix, entrega real de correo, SMS y WhatsApp.
2. Despliegue HTTPS en VPS, documentación final, Figma/Projects y pruebas Bruno sobre la API pública.

WebSocket es opcional para el intercambio actual: las pantallas usan consultas periódicas. No se necesita reimplementar el chat para continuar con los pendientes anteriores.

## Evidencia

- backend/pruebas-bloque-backend.log: suite de 309 pruebas.
- frontend/.mysql-tools/verificacion-bloque-backend.log: 100 comprobaciones del recorrido HTTP.
- frontend/.mysql-tools/verificacion-permisos-bloque.log: 97 comprobaciones de roles y base de datos.
- frontend/.mysql-tools/revisar-base-final.cjs: consulta de motor, migraciones y comprobaciones de integridad indicadas.

Los scripts y logs temporales están ignorados por Git. No se abrió navegador, no se enviaron SMS, WhatsApp ni correos, no se cambiaron contraseñas de cuentas existentes y no se hizo commit ni push.

## Bloque posterior de backend sin cambios en pantallas

Se incorporó V7, el endpoint de cierre, la validación global del JWT revocado y la versión de sesión para invalidar todos los tokens previos al cambio de contraseña. La recuperación aplica la regla completa de contraseña. También se prepararon las páginas de tutorados y avisos, los filtros de intervalos de Horarios y sus contadores SQL.

Pasaron 309 pruebas del backend, 100 comprobaciones del recorrido HTTP y 97 de roles y migraciones. La suite verifica que cambiar la contraseña rechaza los tokens previos y permite un nuevo login; las peticiones reales verifican que cerrar una sesión no cierra otra de la misma cuenta. Los parámetros y permisos de las consultas nuevas están cubiertos.

Este bloque no modificó React. Falta conectar el botón Salir y sustituir las consultas de las pantallas por las rutas paginadas y filtradas. Siguen pendientes los accesos antiguos de Avisos del tutor, alertas nativas, navbar de los otros roles, verificación visual móvil, comunicación externa, despliegue y documentación de entrega. No se añadieron pruebas Bruno ni se publicaron cambios.


## Conexión posterior de las pantallas

Se completaron el cierre del backend, páginas y filtros de tutorados y avisos, intervalos y contadores de Horarios, acciones reales de Avisos y navbar de estudiante y tutor. Se eliminaron los alert y confirm nativos y el modal de reserva ficticio sin uso.

Pasaron 309 pruebas generales del backend y 11 de Avisos tras añadir la comprobación de permisos y vigencia de las acciones. Edge automatizado aprobó 46 comprobaciones de navegación, 17 de acciones reales, filtros y cierre y 11 adicionales de menú móvil, perfil y conversaciones. Se revisaron capturas móviles, las 18 pantallas en escritorio y móvil, confirmación y rechazo persistidos, validación y eliminación de horarios, recuperación ante fallo de red al salir y rechazo del JWT revocado. Los datos temporales fueron eliminados. Sin Bruno ni publicaciones.


## Preparación de comunicación externa

V8 incorpora preferencias telefónicas y notificaciones_sesion. El estudiante guarda o retira teléfono y permisos desde sus datos. La confirmación real del tutor prepara SMS y WhatsApp en una cola persistente, con envío posterior mediante Twilio. Los dos canales permanecen desactivados hasta configurar las credenciales y la plantilla de WhatsApp.

Postfix y OpenDKIM quedaron instalados y activos en el VPS, limitados a conexiones locales. JavaMailSender entregó un mensaje de prueba a Postfix y se comprobó la firma DKIM. Falta validar los registros DNS definitivos, retirar la restricción de salida SMTP en AWS, configurar PTR y acreditar recepción en Gmail. La configuración Gmail del desarrollo se conserva.

Pasaron 324 pruebas del backend, compilación del frontend y once comprobaciones móviles de contacto. Twilio se comprobó con respuestas simuladas; no se acredita entrega real de SMS o WhatsApp. La guía, variables y estado de producción están en README. El ER debe incorporar V8. Sin Bruno, commit ni push.


## Publicación en el VPS

Frontend estático y JAR publicados en https://situa.oaxacoders.tech/, API en /api. Backend y MySQL tienen servicios exclusivos de systemd y puertos locales 8095 y 3307. MySQL 8.4.13 es independiente del MariaDB de los otros proyectos. Base nueva con V1–V8 y datos de demostración.

Pasaron 26 comprobaciones públicas de API e interfaz con los tres roles, HTTPS y logout. Los archivos Nginx anteriores conservaron sus hashes y los servicios anteriores sus procesos; no se modificaron las bases o aplicaciones ajenas a SITUA. Certificado Let's Encrypt y renovación exclusivos del dominio. Los envíos externos y el origen OAuth Google de producción siguen pendientes de habilitación. No se hizo commit ni push ni se añadieron pruebas Bruno.


## Correo operativo mediante Brevo

SITUA utiliza Brevo SMTP por 587 con STARTTLS. La autenticación se comprobó antes de actualizar las variables del servicio y solo se reinició situa-backend. Una recuperación real solicitada a la API pública generó su código y envió el correo; el propietario confirmó recepción en su bandeja de Gmail. La cuenta temporal y su recuperación se eliminaron. No se modificaron cuentas existentes ni otros proyectos del VPS.

La dependencia del desbloqueo del puerto 25 deja de impedir la recuperación. Esta alternativa de proveedor externo debe acordarse para la evaluación porque difiere del requisito original de entrega propia con Postfix. Postfix permanece instalado y Twilio sigue pendiente de sus credenciales. Sin commit ni push.


## Pruebas Bruno de producción

La colección antigua .bru se sustituyó por OpenCollection YAML, con el mismo formato base de ACT 4. Se abre desde la carpeta bruno y apunta a la API HTTPS pública de SITUA. Bruno CLI 4.3.0 aprobó las 79 solicitudes y 119 comprobaciones, incluyendo casos de error previstos, permisos, consultas, paginación, filtros, PDF, operaciones sobre horarios, recordatorios y avisos, y JWT revocado después del logout.

Se eliminaron los datos temporales de esta ejecución y se verificaron cero registros restantes de sus usuarios, avisos, horarios y actividades. La colección documenta que futuras ejecuciones desde la app crean una cuenta y conservan el aviso retirado hasta su limpieza administrativa, porque no existen endpoints de borrado para esos registros. La colección ACT 4 no se modificó; no se cambió el código del backend ni las pantallas. Sin commit ni push.
