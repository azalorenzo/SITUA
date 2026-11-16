# Sistema de tutorías

Proyecto con frontend React/Vite, backend Spring Boot (Java) y MySQL. El inicio de sesión por correo y contraseña ya conecta React con Spring Boot y MySQL mediante JWT. Las listas de tutores y estudiantes de coordinación también consultan MySQL. Las demás pantallas conservan por ahora sus datos de demostración; no hay envío real de comunicaciones.

## Inicio de sesión conectado

El formulario de React está conectado al backend. El registro crea cuentas de estudiante y abre su sesión. El acceso con Google ya funciona en el entorno local configurado. En nuevas instalaciones se activa al configurar el ID de cliente y autorizar los orígenes en Google Cloud. La recuperación de contraseña sigue pendiente.

### Configuración y arranque

En `backend/.env` se mantienen las variables de MySQL y se agregan:

- `JWT_CLAVE`: clave aleatoria en Base64 con al menos 32 bytes. No compartirla con el frontend ni subirla a Git. Cada instalación usa la suya.
- `JWT_MINUTOS_VIGENCIA`: 15 por defecto; admite entre 1 y 1440 minutos.
- `ORIGENES_PERMITIDOS`: por defecto `http://localhost:5173,http://127.0.0.1:5173`.

Para generar una clave en otra computadora:

```powershell
node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"
```

Copiar el resultado solo al valor `JWT_CLAVE` del `.env` local. La clave de esta instalación ya fue generada sin mostrarla. `application.properties` contiene referencias a las variables, no valores secretos.

Levantar el backend desde `backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Levantar React desde `frontend`, en otra terminal:

```powershell
npm install
Copy-Item .env.example .env
npm run dev
```

No reemplazar un `.env` local ya configurado. `VITE_URL_API=/api` utiliza el proxy de Vite hacia `http://127.0.0.1:8080`. En producción, el servidor web deberá redirigir `/api` al backend o habrá que ajustar la URL pública y los orígenes permitidos. Ninguna clave privada pertenece a una variable `VITE_`.

### API de autenticación

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| POST | `/api/autenticacion/iniciar-sesion` | Público | JWT, vencimiento y perfil básico |
| POST | `/api/autenticacion/registrar` | Público | Crea estudiante, devuelve JWT y perfil (201) |
| POST | `/api/autenticacion/google` | Público | Verifica Google, vincula o crea cuenta y devuelve JWT |
| GET | `/api/autenticacion/sesion` | `Authorization: Bearer <token>` | Perfil de la cuenta actual, comprobado en MySQL |

Cuerpo del login, usando una cuenta del seed:

```json
{"correo":"estudiante@demo.example","contrasena":"AlumnoDemo!2026"}
```

La validación exige un correo válido y contraseña con mínimo 8 caracteres, una mayúscula, un número y un carácter especial. Se respeta el límite de BCrypt; React también señala contraseñas demasiado largas. El backend compara el hash, revisa que la cuenta esté activa y tenga roles, y devuelve un DTO sin contraseña ni hash. Un correo inexistente y una contraseña incorrecta generan el mismo mensaje.

El JWT se firma con HS256 e incluye identificador de usuario, roles, emisor, audiencia, identificador único, emisión y vencimiento. Spring Security comprueba firma, emisor, audiencia y caducidad. Las rutas de coordinación se reservan a coordinación; las de tutor y estudiante permiten su rol y coordinación. Coordinación ya tiene endpoints de consulta de tutores y estudiantes; las demás operaciones de negocio siguen pendientes. La consulta de sesión además comprueba el estado actual del usuario en MySQL.

React abre la pantalla de coordinación, tutor o estudiante según los roles devueltos por el servidor; si hay varios, prioriza coordinación y después tutor. Muestra el nombre y correo auténticos, bloquea el botón durante la solicitud y presenta errores debajo de los campos o un mensaje general ante fallos de acceso/red. Las peticiones protegidas usan `Authorization`.

Sin marcar recordar se usa `sessionStorage`; al marcarlo se usa `localStorage`. Solo se guardan token y vencimiento, nunca la contraseña ni roles usados como fuente de permisos. Al recargar se verifica la sesión con el servidor antes de mostrar las pantallas. Recordar no amplía la duración del JWT. Al vencer, se vuelve al login.

**Alcance de Salir:** por ahora elimina la sesión del navegador. El JWT emitido sigue siendo válido hasta vencer; la invalidación en el servidor mediante `tokens_revocados` corresponde al siguiente avance de logout. No se presenta esa función como ya implementada.

### Registro de cuentas

Las indicaciones del proyecto piden registro, validación en frontend/backend y contraseñas protegidas con BCrypt. Google es una función adicional solicitada para este avance.

El enlace **Regístrate** solicita nombre, matrícula, correo, contraseña y confirmación. El servidor siempre asigna `estudiante`; enviar otros roles no concede permisos. Correo y matrícula deben ser únicos. Usuario, rol y perfil se guardan juntos: si algo falla, no queda una cuenta a medias. Tras registrarse, se abre la sesión sin marcar recordar.

```json
{"nombre":"Estudiante nuevo","matricula":"ALUMNO-100","correo":"nuevo@example.com","contrasena":"MiClave!123","confirmarContrasena":"MiClave!123"}
```

### Acceso con Google

La integración usa Google Identity Services y un ID de cliente OAuth de tipo **Aplicación web**. No necesita secreto de cliente para este flujo. Crear el cliente siguiendo la [guía oficial de Google](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid) y autorizar como orígenes JavaScript `http://localhost:5173` y `http://127.0.0.1:5173`. Para despliegue, agregar el origen HTTPS real.

Colocar el mismo ID público en ambas variables y reiniciar los dos proyectos:

```dotenv
# backend/.env
GOOGLE_CLIENTE_ID=
# frontend/.env
VITE_GOOGLE_CLIENTE_ID=
```

Al estar vacías, el frontend muestra Google desactivado y la API devuelve 503 si se intenta utilizarlo. No se colocaron credenciales inventadas. Cuando se configuren, aparecerá el botón oficial de Google.

El frontend manda `credencial` y `comprobacion` a `/api/autenticacion/google`. El backend verifica la firma RS256 con las claves públicas de Google, emisor, audiencia, vencimiento, correo verificado y la comprobación del acceso. Guarda el identificador estable de Google (`sub`), no su token.

- Cuenta nueva: el formulario pide matrícula y crea un estudiante. La contraseña local queda en NULL; esa cuenta ingresa con Google.
- Correo ya registrado: pide la contraseña actual del sistema antes de vincular. Conserva sus roles. Las siguientes entradas usan la identidad vinculada.
- Cuenta inactiva: el acceso se rechaza aunque Google sea válido.

Las pruebas verifican firmas RSA y los casos de creación/vinculación con datos controlados. El usuario confirmó el acceso correcto con una cuenta real de Google en el entorno local.

V3 agrega `identidades_externas` y permite `usuarios.password_hash` nulo para cuentas exclusivas de Google. No altera las contraseñas existentes. El diagrama ER y sus metadatos se regeneraron desde el motor.

### Pruebas de este avance

- 99 pruebas del backend: autenticación, JWT, roles, validación, registro, vinculación y verificación criptográfica de Google, consultas de coordinación y administración de asignaciones. Los datos temporales se revierten.
- 13 peticiones y pruebas Bruno ejecutadas contra la API real: autenticación, permisos y consultas de tutores, estudiantes, periodos y asignaciones desde coordinación.
- 10 comprobaciones adicionales de registro real en navegador: campos, confirmación, duplicados, creación de estudiante, restauración de sesión y móvil. La cuenta de prueba se eliminó al terminar.
- 25 comprobaciones de navegador del login: acceso real de los tres perfiles, errores, carga, recarga, almacenamiento según recordar, salida local, token alterado y móvil sin desbordamiento.

La colección versionada está en `bruno`. Abrir esa carpeta en Bruno y ejecutar las peticiones en su orden; el token se conserva solo como variable de ejecución. La prueba de 403 comprueba que el filtro bloquee al estudiante en las rutas de coordinación.

También se puede ejecutar la colección desde esa carpeta con `npx --package @usebruno/cli bru run`. No guardar tokens reales en los archivos versionados. Las pruebas Java se ejecutan con `./mvnw.cmd test` desde `backend`.

## Consultas de coordinación

El backend y las pantallas de coordinación permiten consultar los tutores y estudiantes reales de MySQL. Ambas opciones están disponibles en el menú de coordinación.

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/coordinacion/tutores` | JWT con rol `coordinador`; respuesta paginada |
| GET | `/api/coordinacion/estudiantes` | JWT con rol `coordinador` |

Estudiantes devuelve una lista JSON completa. Tutores devuelve una respuesta paginada; ambas consultas se ordenan por nombre e identificador. No contienen contraseñas ni hashes. Incluyen cuentas activas e inactivas con su `estado`; Estudiantes sin registros devuelve `[]`; Tutores devuelve `contenido: []` y totales en cero. Sin token se recibe 401; con rol tutor o estudiante, 403. Se usa `Cache-Control: no-store`.

Los tutores incluyen `id` (del perfil tutor), `usuarioId`, `nombre`, `correo`, `telefono`, `avatarUrl`, `estado`, `departamentoId` y `departamento`.

Los estudiantes incluyen `id` (del perfil estudiante), `usuarioId`, `nombre`, `correo`, `telefono`, `avatarUrl`, `estado`, `matricula`, `periodoId`, `periodo`, `carrera`, `semestre`, `promedio` y `nivelRiesgo`. Se elige el periodo con fecha de inicio más reciente de cada estudiante, aunque esté cerrado; los empates se resuelven por identificador de periodo. La medición más reciente se obtiene de la vista existente, con desempate por identificador. Los estudiantes sin expediente también aparecen, con campos académicos nulos. Sin medición, promedio y riesgo son nulos; no se inventa un valor de riesgo.

No se modificaron tablas, migraciones ni entidades compartidas con el módulo de estudiante. Los archivos nuevos de consulta están separados en controlador, servicio, repositorio y respuestas de coordinación.

Para probar en Bruno, ejecutar primero `coordinacion/iniciar-sesion.bru` y después las consultas de esa carpeta. Se utilizan las cuentas del seed y una variable temporal `token_coordinacion`.

### Listas conectadas en React

Las pantallas de tutores y estudiantes consumen las rutas anteriores con el JWT de la sesión. Muestran carga, error con reintento, lista vacía y búsqueda sin resultados. El botón Actualizar vuelve a consultar el servidor. Al cambiar de pantalla se cancela la consulta pendiente para evitar mostrar una respuesta anterior.

En estudiantes, la búsqueda se aplica a los datos recibidos sin distinguir mayúsculas ni acentos, por nombre, correo, matrícula y carrera. En tutores, búsqueda y filtros se ejecutan en MySQL y se reciben solo los registros de la página solicitada. El selector filtra por estado de la cuenta. Las tarjetas y gráficos resumen toda la lista cargada; el contador de la tabla indica cuántos registros coinciden con la búsqueda y el filtro.

Las estadísticas académicas proceden del expediente más reciente de cada estudiante. El promedio general usa únicamente promedios disponibles, sin convertir ausencias en cero. Riesgo nulo se muestra como Sin evaluación. Los registros sin semestre se cuentan aparte. Los periodos pueden ser distintos y se indica en la pantalla. La cifra de horas ficticias fue retirada porque no forma parte de estas consultas.

Las acciones de tutores ya crean, editan y cambian el estado de cuentas en el servidor. Estudiantes continúa como consulta. Las vistas del estudiante y del tutor no se modificaron.

Verificación del frontend: compilación de producción y 22 comprobaciones de navegador, con consultas reales para cotejar filas y JWT; respuestas controladas para fallos de red, listas vacías, reintentos y campos académicos nulos o con promedio cero. No se modificaron registros de MySQL durante estas pruebas.

## Administración de asignaciones

Coordinación puede crear una asignación activa, cambiar el tutor, finalizarla o cancelarla desde Asignaciones. La pantalla consulta MySQL y muestra historial, filtros por periodo y estado, búsqueda y estudiantes sin asignación activa. La tabla ocupa todo el ancho y muestra cinco registros por página, con columnas de estudiante, tutor, fecha, estado y acciones. El icono de detalle despliega periodo, fechas completas, sesiones y motivo. Los paneles de estudiantes sin asignación y acciones rápidas aparecen debajo. Los iconos permiten ver detalle, cambiar tutor, finalizar y cancelar; cancelar conserva el registro.

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/coordinacion/periodos` | Periodos registrados |
| GET | `/api/coordinacion/asignaciones` | Página del historial con filtros, totales y resumen |
| GET | `/api/coordinacion/asignaciones/exportar` | Todos los registros filtrados, solicitados solo para exportar |
| GET | `/api/coordinacion/asignaciones/estudiantes?periodoId=<id>` | Estudiantes elegibles del periodo y su asignación activa, si existe |
| POST | `/api/coordinacion/asignaciones` | Crear una asignación activa (201) |
| PATCH | `/api/coordinacion/asignaciones/{id}/cambiar-tutor` | Cerrar la anterior y crear otra con el nuevo tutor |
| PATCH | `/api/coordinacion/asignaciones/{id}/finalizar` | Finalizar sin borrar |
| PATCH | `/api/coordinacion/asignaciones/{id}/cancelar` | Cancelar sin borrar |

Todas las rutas requieren JWT de coordinación. Las escrituras comprueban además que el usuario conserve su cuenta activa y rol de coordinación en MySQL. No se recibe `asignadoPor` desde React: se obtiene del JWT.

Crear recibe `estudianteId`, `tutorId`, `periodoId` y `motivo` opcional de hasta 1000 caracteres. Cambiar tutor recibe `tutorId` y `motivo` opcional. Finalizar y cancelar no requieren cuerpo; se confirman antes de enviar desde la pantalla.

Para crear o cambiar se exige periodo activo, cuentas de estudiante y tutor activas y un expediente del estudiante con inscripción activa en ese periodo. El estado activo del periodo se toma del catálogo, no de la fecha del equipo. No se permiten dos asignaciones activas para el mismo estudiante y periodo. Las operaciones bloquean el registro del estudiante durante su transacción y la restricción única de MySQL también evita duplicados. Si falla un cambio de tutor, se conserva la asignación original. Los registros pendientes existentes pueden finalizarse o cancelarse; las nuevas asignaciones de coordinación quedan activas directamente.

Las fechas de inicio y cierre se generan en UTC desde MySQL y React las muestra en la zona del navegador. No se permite cerrar una asignación con inicio futuro. Cada cambio de tutor genera otra fila y conserva la anterior finalizada, incluyendo el autor de cada alta. El historial no se elimina. Las sesiones completadas y la última sesión se calculan por estudiante, tutor y periodo, dentro del intervalo de cada asignación (inicio incluido, fin excluido), para no contarlas de nuevo si regresa al mismo tutor.

Exportar asignaciones descarga un CSV con todos los registros que coinciden con los filtros, no solo la página visible. Las fechas del CSV se identifican como UTC y los valores se escapan para evitar que los datos se interpreten como fórmulas. El diseño compacto se comprobó sin desplazamiento horizontal de la tabla en anchos de 1440, 1024 y 882 píxeles; en pantallas pequeñas permite desplazamiento dentro de la tabla. Pasaron 15 comprobaciones de navegador de diseño, paginación, detalle, formularios y exportación, sin modificar registros.

El panel de estudiantes sin asignación activa solo cuenta cuentas y expedientes activos del periodo seleccionado; no incluye estudiantes sin expediente ni pretende contar a todos los usuarios registrados. Para asignar a un estudiante recién registrado se necesita cargar antes su situación académica; la pantalla para administrar expedientes todavía está pendiente. No se agregaron migraciones ni se cambiaron las vistas del estudiante. La elección de tutor desde el estudiante sigue como avance separado y debe respetar la misma unicidad por periodo.

Verificación: pruebas de backend con transacciones revertidas, pruebas de navegador del ciclo completo con registros temporales eliminados al terminar, compilación de frontend y colección Bruno de consultas. Los casos incluyen duplicados, cuentas y periodos inactivos, ausencia de expediente, permisos, cambio inválido, historial y conteo de sesiones.

### Paginación de asignaciones en el servidor

`GET /api/coordinacion/asignaciones` recibe `pagina` (desde 1, por defecto 1), `tamano` (1 a 100, por defecto 5), `periodoId` opcional, `estado` opcional (`activa`, `pendiente`, `finalizada`, `cancelada`) y `busqueda` opcional de hasta 160 caracteres.

La respuesta incluye `contenido`, `pagina`, `tamano`, `totalRegistros`, `totalPaginas` y `resumen` con `total`, `activas` y `pendientes`. El contenido se limita en MySQL con LIMIT/OFFSET y se ordena por inicio e identificador descendentes. Búsqueda por estudiante, matrícula y tutor y filtros se aplican antes de contar y paginar. La búsqueda no distingue mayúsculas ni acentos según el cotejamiento de la base; `%` y `_` se tratan como texto. El resumen de tarjetas corresponde al periodo seleccionado, independientemente de búsqueda y estado.

React solo recibe la página solicitada y muestra botones para todos los índices disponibles, además de Anterior y Siguiente. Al cambiar filtros vuelve a la primera página. La búsqueda espera brevemente al terminar de escribir y cancela consultas anteriores. Cero resultados devuelve `contenido: []`, `totalRegistros: 0`, `totalPaginas: 0` y `pagina: 1`; no se dibujan índices inexistentes. Si la página solicitada dejó de existir, el servidor devuelve la última disponible.

La exportación utiliza `/api/coordinacion/asignaciones/exportar` con los mismos filtros, sin paginación, únicamente al pulsar Exportar. No se descarga todo el historial al abrir la pantalla. La colección Bruno se actualizó al contrato paginado. Se comprobaron 84 pruebas de backend, 13 casos de navegador de páginas, filtros, exportación y reintentos, y 11 peticiones Bruno. No se modificaron tablas ni migraciones.

## Gestión de tutores

La pantalla de Tutores permite agregar, editar, desactivar y reactivar cuentas. El formulario de alta utiliza nombre, correo, contraseña y departamento; al editar solo se cambian nombre, correo y departamento. No se muestran hashes ni contraseñas existentes ni se permite escoger roles. La cuenta nueva recibe únicamente el rol tutor y puede iniciar sesión al terminar el alta.

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/coordinacion/tutores` | Tutores paginados con búsqueda y filtros |
| GET | `/api/coordinacion/tutores/opciones` | Tutores activos para los selectores de Asignaciones |
| GET | `/api/coordinacion/departamentos` | Catálogo de departamentos, incluidos inactivos para filtrar registros anteriores |
| POST | `/api/coordinacion/tutores` | Crear cuenta y perfil juntos (201) |
| PUT | `/api/coordinacion/tutores/{id}` | Editar nombre, correo y departamento |
| PATCH | `/api/coordinacion/tutores/{id}/desactivar` | Desactivar sin borrar historial |
| PATCH | `/api/coordinacion/tutores/{id}/activar` | Reactivar la cuenta |

Todas las rutas requieren coordinación. Las escrituras comprueban además el estado y los roles actuales del autor en MySQL. La creación es transaccional, usa BCrypt y rechaza correos ya registrados, sin convertir cuentas existentes en tutores. La contraseña debe tener al menos 8 caracteres, mayúscula, número y carácter especial, y no superar 72 bytes UTF-8. El `.env` no cambia y no hay migraciones nuevas.

La lista recibe `pagina` desde 1, `tamano` de 1 a 100 (por defecto 5), `busqueda` de hasta 160 caracteres, `estado` opcional (`activo` o `inactivo`) y `departamentoId` opcional. Responde `contenido`, `pagina`, `tamano`, `totalRegistros`, `totalPaginas` y `resumen` con total, activos e inactivos de todos los tutores. MySQL filtra antes de contar y paginar con LIMIT/OFFSET; la búsqueda cubre nombre, correo y departamento. React muestra los índices disponibles y vuelve a la primera página al cambiar filtros. La consulta de opciones se pide aparte para no limitar los selectores a los primeros cinco tutores.

El alta requiere departamento activo; cambiar de departamento también. Se permite conservar el departamento actual aunque después quede inactivo. Editar conserva contraseña, teléfono, avatar y permisos de la cuenta. Crear o editar un correo duplicado devuelve 409; campos inválidos, 422. Los registros inexistentes devuelven 404.

Desactivar bloquea nuevos inicios de sesión y la restauración mediante consulta de sesión. Los JWT ya emitidos mantienen su vencimiento; la revocación de sesiones sigue pendiente. Las asignaciones y sesiones del tutor se conservan, y el tutor inactivo deja de aparecer entre las opciones para nuevas asignaciones. El estado pertenece a la cuenta compartida, por lo que también afecta sus otros roles si los tiene. Las cuentas de coordinación no pueden desactivarse desde esta pantalla. React pide confirmación antes de cambiar el estado.

Verificación: 99 pruebas Java aprobadas, compilación del frontend, 20 comprobaciones de navegador de alta, edición, duplicados, paginación, filtros, login, estados y compatibilidad con Asignaciones, y colección Bruno actualizada. Las pruebas Java revierten sus datos y las de navegador eliminan su tutor y departamentos temporales. No se modificaron los datos de demostración existentes.

## Conexión del backend con MySQL

El backend ya puede conectarse a `tutoria`. Usa las variables `BD_SERVIDOR`, `BD_PUERTO`, `BD_NOMBRE`, `BD_USUARIO` y `BD_CONTRASENA`, definidas en `backend/.env` o en el entorno del proceso. El archivo `.env` se lee como propiedades de Java: escribe los valores sin comillas ni `export`. Para una contraseña con barras invertidas, escribe cada barra dos veces.

`backend/.env.example` sirve como plantilla sin secretos; `application.properties.example` muestra la configuración compartida. El `.env` real está excluido por el `.gitignore` general.

Desde `backend`, ejecutar `./mvnw.cmd spring-boot:run` en Windows (o `./mvnw spring-boot:run` en Linux). La prueba de arranque se ejecuta con `./mvnw.cmd test`. Hibernate no crea ni cambia tablas. Flyway revisa las migraciones al arrancar y aplica solamente las que faltan, antes de iniciar JPA.

## Respuestas de error del backend

El paquete `errores` contiene `RespuestaError` y `ManejadorErrores`. Cubre validación de formularios con `@Valid`, JSON que no se puede leer y errores de acceso. Los errores de los filtros de Spring Security usan el mismo formato JSON.

Cuando los campos no cumplen sus reglas, la respuesta es HTTP 422:

```json
{
  "estado": 422,
  "mensaje": "Revisa los campos del formulario.",
  "erroresPorCampo": {
    "correo": ["Escribe un correo válido."]
  }
}
```

Cada campo puede tener varios mensajes. Los errores del formulario completo se agrupan en `general`. Un cuerpo vacío, JSON mal escrito o un tipo incorrecto devuelve HTTP 400 con un mensaje general y `erroresPorCampo` vacío. No se devuelve el contenido rechazado ni detalles de la excepción.

Las reglas se escriben en los DTOs mediante Bean Validation; el manejador organiza sus mensajes. El login devuelve 401 para credenciales incorrectas y 403 para cuentas inactivas o sin rol. Los errores de recursos inexistentes y otros módulos se agregarán en sus respectivos avances.

Las seis pruebas de este manejador usan un controlador que existe solamente en los archivos de prueba, sin necesitar MySQL. La prueba general de arranque continúa utilizando la configuración local de la base. Ejecutar todo con `./mvnw.cmd test` desde `backend`; Maven reconoce también los archivos terminados en `Pruebas` para conservar los nombres en español. El avance inicial pasó sus siete pruebas.

## Entidades de usuarios y roles

El paquete `entidades` contiene `Usuario` y `Rol`, que representan las tablas existentes `usuarios` y `roles`. La relación muchos a muchos utiliza `usuario_roles`; su fecha `asignado_en` sigue siendo completada por MySQL. No se modificó el esquema ni el diagrama ER.

Los atributos y métodos propios están en español. `contrasenaHash` corresponde a la columna existente `password_hash`; esta clase recibe un hash ya generado, no cifra contraseñas por sí sola. El usuario permite actualizar sus datos, activar/desactivar su estado y asignar o quitar roles. Las fechas se leen desde MySQL después de recargar la entidad.

Los roles se cargan cuando se consultan (LAZY), sin guardar ni borrar automáticamente los roles compartidos al cambiar un usuario. Las entidades no implementan servicios, endpoints ni autenticación, y no se devuelven al frontend. Las consultas están en los repositorios descritos abajo.

Se añadieron tres pruebas sobre MySQL: lectura del catálogo de roles, guardado/lectura de un usuario con varios roles y eliminación de vínculos sin borrar roles compartidos. Estas pruebas usan `ddl-auto=validate` para comprobar las tablas sin cambiarlas y revierten sus datos al terminar. El avance de entidades pasó sus diez pruebas.

## Repositorios de usuarios y roles

El paquete `repositorios` contiene `RepositorioUsuarios` y `RepositorioRoles`. Son las clases de acceso a datos utilizadas por el servicio de autenticación; no exponen rutas HTTP directamente.

- `buscarPorCorreo` busca un usuario y mantiene sus roles sin cargar hasta que se necesiten.
- `buscarPorCorreoConRoles` obtiene al usuario y sus roles juntos. También devuelve al usuario si aún no tiene roles.
- `buscarPorIdConRoles` permite recuperar la cuenta actual al verificar una sesión.
- `buscarPorCodigo` busca un rol por su código, por ejemplo `coordinador`, no por su nombre visible.

Las búsquedas devuelven `Optional`: si no hay coincidencia, el resultado está vacío. Los valores recibidos se pasan como parámetros de la consulta. Los métodos propios están en español; se conservan los nombres de las clases, anotaciones y operaciones heredadas de Spring Data.

Se agregaron seis pruebas sobre MySQL para comprobar búsquedas, resultados vacíos, carga de roles y entradas con comillas. Los registros de esas pruebas se revierten al terminar. El avance de repositorios pasó sus dieciséis pruebas. No se modificó el esquema ni el ER.

## Base de datos

`tutoria` está en MySQL Community Server 26.10.0 con **28 tablas funcionales, 241 columnas, 46 claves foráneas, 39 CHECK y 4 vistas**. Existe además la tabla técnica `flyway_schema_history` (29 tablas físicas en total).

Las 24 tablas originales se adoptaron como baseline V1 tras comparar columnas y claves con la estructura existente. Flyway aplicó V2 sin eliminar cuentas ni datos:

- `tokens_recuperacion`: digest SHA-256 del token, usuario, vencimiento y uso. El token original no se almacena.
- `tokens_revocados`: identificador JWT `jti`, usuario, vencimiento y motivo de revocación. No almacena el JWT completo.
- `envios_comunicacion`: correo/SMS/WhatsApp, destinatario, evento, idempotencia, proveedor, intentos, estado y errores.

El login ya está implementado en la API. Recuperación de contraseña, revocación de tokens al cerrar sesión y comunicaciones reales permanecen pendientes; sus tablas ya están preparadas.

## Migraciones e instalación

Las migraciones oficiales están en `backend/src/main/resources/base_datos/migraciones`. No editar una migración ya aplicada; crear V3, V4, etc. El POM de `database` permite trabajar con Flyway sin arrancar Spring Boot.

Requisitos: Java, Maven Wrapper del backend, Node.js, MySQL y una base vacía creada con utf8mb4/utf8mb4_0900_ai_ci. Las migraciones crean las tablas, no el servidor ni las credenciales.

Desde la raíz:

```powershell
npm install --prefix frontend/.mysql-tools --no-save --package-lock=false mysql2 @viz-js/viz @resvg/resvg-js
# Crear frontend/database/connection.local.json según connection.example.json.
node database/flyway.cjs migrate
node database/flyway.cjs validate
# Solo para desarrollo/demostración:
node database/seed.cjs
```

`migrate` en una base vacía ejecuta V1, V2 y V3. La base local existente ya está en V3: no ejecutar baseline nuevamente. Para adoptar otra base preexistente, comparar su estructura con V1 y respaldarla antes de usar baseline; el baseline automático está deshabilitado. `clean` está deshabilitado.

La herramienta lee las credenciales del archivo local excluido por Git y las entrega a Flyway mediante variables de entorno. No las imprime ni las incorpora al POM. `FLYWAY_URL` permite elegir otra conexión JDBC; `FLYWAY_OFFLINE=true` permite usar las dependencias ya descargadas. El backend utiliza la misma versión de Flyway (13.12.1) y ejecuta automáticamente las migraciones al arrancar. Las herramientas de `database` siguen disponibles para mantenimiento.

## Preparar la base en otra computadora

No hace falta copiar la base de datos de otro integrante ni compartir su contraseña. Cada persona usa su propio MySQL y sus credenciales locales.

Crear solamente la base vacía desde MySQL Workbench o un cliente MySQL:

```sql
CREATE DATABASE tutoria
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

Desde la carpeta `backend`, copiar la plantilla y completar los datos de esa computadora:

```powershell
Copy-Item .env.example .env
# Editar .env con las credenciales de MySQL y una JWT_CLAVE propia en Base64.
.\mvnw.cmd spring-boot:run
```

Al arrancar, Flyway ejecuta V1, V2 y V3 sobre esa base vacía. Quedan creadas las 28 tablas funcionales, las 4 vistas, los catálogos iniciales y su tabla de historial. Volver a arrancar no repite las migraciones aplicadas. Hibernate conserva `ddl-auto=none`. Se comprobaron ambos arranques con una base temporal vacía y se confirmó que la base principal conserva sus datos.

Los usuarios y ejemplos se cargan aparte: abrir `database/seeds/001_datos_demo.sql` en MySQL Workbench, seleccionar `tutoria` como base de trabajo y ejecutar el script. También se puede usar `node database/seed.cjs` con la configuración local de las herramientas. El seed crea las cuentas documentadas abajo y se puede repetir sin duplicarlas. El arranque del backend no carga estos datos automáticamente.

Si ya existe una base con tablas, no importar el esquema antiguo ni activar baseline automático para saltarse errores. La base local original ya tiene su baseline registrado; una base nueva vacía no necesita ese paso. Las migraciones que ya estén aplicadas se conservan sin editar.

## Datos de demostración y acceso futuro

El seed explícito `database/seeds/001_datos_demo.sql` conserva las cuentas existentes y se puede repetir sin duplicar los ejemplos. Se verificó también desde una base vacía.

| Rol | Correo | Contraseña de demostración |
|---|---|---|
| Coordinación / administrador | coordinacion@demo.example | CoordDemo!2026 |
| Tutor | tutor@demo.example | TutorDemo!2026 |
| Estudiante | estudiante@demo.example | AlumnoDemo!2026 |

Hay 31 cuentas (1 coordinación, 15 tutores y 15 estudiantes), 15 sesiones, 15 reportes, 15 avisos, 15 conversaciones y 30 mensajes. Los perfiles adicionales usan tutor02..tutor15@demo.example y estudiante02..estudiante15@demo.example, con la misma contraseña de demostración de su rol. Todas las contraseñas están hasheadas con BCrypt.

Los registros de comunicaciones son ficticios, están marcados como fallidos con `DEMO_NO_ENVIAR` y no representan entregas reales. No se generaron tokens de recuperación ni JWT activos. Los catálogos y tablas técnicas no necesitan 15 filas: roles tiene 3 y motivos tiene 4. El detalle por tabla está en `database/verificacion-v2.json`.

Estas cuentas permiten iniciar sesión desde React cuando ambos proyectos están levantados y el seed está cargado. Los datos del resto de las pantallas todavía son de demostración.

## Diagrama entidad-relación actualizado

El diagrama procede de los metadatos del MySQL real; incluye las tres tablas nuevas, FK compuestas y columnas generadas. El historial técnico de Flyway se excluye del dominio.

![Diagrama ER físico](frontend/database/diagrama-er/tutoria-er.png)

- [SVG ampliable](frontend/database/diagrama-er/tutoria-er.svg)
- [Mermaid editable](frontend/database/diagrama-er/tutoria-er.mmd)
- [Diccionario, índices y restricciones](frontend/database/diagrama-er/DIAGRAMA_ER.md)
- [Respaldo SQL de estructura V3 sin datos sensibles](database/respaldo_estructura_v3.sql)

## Verificación realizada

```powershell
node frontend/database/manage.cjs --check
node frontend/database/manage.cjs --test
node database/verificar-v2.cjs
node database/verificar-migraciones.cjs
node database/exportar-er.cjs
```

Pasaron 21 pruebas de integridad (12 originales y 9 de V2). Sus registros temporales se revierten. También se ejecutaron V1, V2 y V3 en una base temporal vacía, se repitió el seed y se verificaron los conteos; esa base temporal se eliminó al terminar. La base principal conserva sus datos.

Flyway informa que MySQL 26.10 es más reciente que su última versión verificada. Las migraciones y comprobaciones de este proyecto sí se ejecutaron correctamente sobre este servidor; conservar estas pruebas al actualizar dependencias.

El respaldo local anterior a la migración está en `database/backups`, excluido de Git. El archivo `frontend/database/001_tutoria.sql` se conserva como referencia histórica V1; no debe usarse para nuevas instalaciones. El comando antiguo `manage.cjs --create` está deshabilitado.

Referencia: [baseline de Flyway](https://github.com/flyway/flyway/blob/main/documentation/Reference/Commands/Baseline.md).
