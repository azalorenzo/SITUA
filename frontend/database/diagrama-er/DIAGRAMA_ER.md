# Diagrama ER físico de tutoria

Extraído directamente de information_schema en MySQL 26.10.0, el 11 de noviembre de 2026. Incluye **27 tablas, 236 columnas, 45 claves foráneas y 37 CHECK**. No contiene filas ni contraseñas. Las 4 vistas se documentan aparte: no son tablas adicionales.

- tutoria-er.svg: diagrama completo, ampliable.
- tutoria-er.mmd: fuente Mermaid editable.
- tutoria-er.dot: fuente Graphviz editable.
- estructura.json: metadatos extraídos del motor.

La tabla técnica flyway_schema_history se excluye del dominio: existen 28 tablas físicas en total, de las cuales 27 son funcionales.

## Lectura del modelo

Todas las columnas están representadas con su tipo físico y nulabilidad. PK indica clave primaria; FK, columna participante en una clave foránea; UQ/UK marca únicamente una restricción única de una sola columna. Los índices únicos compuestos se detallan abajo, para no sugerir que cada componente es único individualmente. [G] identifica columnas generadas.

Las cardinalidades se derivan de las restricciones reales, no de suposiciones funcionales. Ninguna FK obliga a un padre a tener hijos; por ello el mínimo de hijos es siempre cero. Un FK con columnas NULL permite que el vínculo no exista. Los extremos del SVG indican cuántos padres corresponden a un hijo (1 o 0..1) y cuántos hijos a un padre (0..N o 0..1). En Mermaid, la línea continua identifica FK incorporada en la PK del hijo, y la discontinua relaciones no identificadoras.

Una FK compuesta corresponde a una sola relación. En SVG las líneas conectan tablas; al pasar el cursor se muestran todas las columnas de la FK. La lista siguiente es la definición completa de las 45 relaciones.

## Relaciones exactas

| Restricción | Hijo (columnas) | Padre (columnas) | Padre por hijo | Hijos por padre |
|---|---|---|---|---|
| fk_actividad_propietario | actividades_personales (propietario_id) | usuarios (id) | 1 | 0..N |
| fk_actividad_tutor | actividades_personales (tutor_id) | tutores (id) | 0..1 | 0..N |
| fk_asignacion_autor | asignaciones_tutor (asignado_por) | usuarios (id) | 1 | 0..N |
| fk_asignacion_situacion | asignaciones_tutor (estudiante_id, periodo_id) | situaciones_academicas (estudiante_id, periodo_id) | 1 | 0..N |
| fk_asignacion_tutor | asignaciones_tutor (tutor_id) | tutores (id) | 1 | 0..N |
| fk_destinatario_aviso | aviso_destinatarios (aviso_id) | avisos (id) | 1 | 0..N |
| fk_destinatario_usuario | aviso_destinatarios (usuario_id) | usuarios (id) | 1 | 0..N |
| fk_aviso_autor | avisos (autor_id) | usuarios (id) | 1 | 0..N |
| fk_aviso_sesion | avisos (sesion_id) | sesiones (id) | 0..1 | 0..N |
| fk_carrera_departamento | carreras (departamento_id) | departamentos (id) | 1 | 0..N |
| fk_participante_conversacion | conversacion_participantes (conversacion_id) | conversaciones (id) | 1 | 0..N |
| fk_participante_usuario | conversacion_participantes (usuario_id) | usuarios (id) | 1 | 0..N |
| fk_conversacion_autor | conversaciones (creada_por) | usuarios (id) | 1 | 0..N |
| fk_disponibilidad_tutor | disponibilidades_tutor (tutor_id) | tutores (id) | 1 | 0..N |
| fk_envio_sesion | envios_comunicacion (sesion_id) | sesiones (id) | 0..1 | 0..N |
| fk_envio_usuario | envios_comunicacion (usuario_id) | usuarios (id) | 1 | 0..N |
| fk_estudiante_usuario | estudiantes (usuario_id) | usuarios (id) | 1 | 0..1 |
| fk_material_autor | materiales_sesion (agregado_por) | usuarios (id) | 1 | 0..N |
| fk_material_sesion | materiales_sesion (sesion_id) | sesiones (id) | 1 | 0..N |
| fk_medicion_autor | mediciones_academicas (registrado_por) | usuarios (id) | 1 | 0..N |
| fk_medicion_situacion | mediciones_academicas (situacion_id) | situaciones_academicas (id) | 1 | 0..N |
| fk_mensaje_participante | mensajes (conversacion_id, emisor_id) | conversacion_participantes (conversacion_id, usuario_id) | 1 | 0..N |
| fk_observacion_autor | observaciones_estudiante (autor_id) | usuarios (id) | 1 | 0..N |
| fk_observacion_sesion | observaciones_estudiante (sesion_id, estudiante_id, periodo_id) | sesiones (id, estudiante_id, periodo_id) | 0..1 | 0..N |
| fk_observacion_situacion | observaciones_estudiante (situacion_id, estudiante_id, periodo_id) | situaciones_academicas (id, estudiante_id, periodo_id) | 1 | 0..N |
| fk_reporte_autor_cierre | reportes (completado_por) | usuarios (id) | 0..1 | 0..N |
| fk_reporte_motivo | reportes (motivo_id) | motivos_reporte (id) | 1 | 0..N |
| fk_reporte_sesion | reportes (sesion_id, estudiante_id, tutor_id, periodo_id) | sesiones (id, estudiante_id, tutor_id, periodo_id) | 0..1 | 0..N |
| fk_reporte_situacion | reportes (estudiante_id, periodo_id) | situaciones_academicas (estudiante_id, periodo_id) | 1 | 0..N |
| fk_reporte_tutor | reportes (tutor_id) | tutores (id) | 1 | 0..N |
| fk_cambio_actor | sesion_cambios (actor_id) | usuarios (id) | 1 | 0..N |
| fk_cambio_sesion | sesion_cambios (sesion_id) | sesiones (id) | 1 | 0..N |
| fk_sesion_autor | sesiones (creado_por) | usuarios (id) | 1 | 0..N |
| fk_sesion_disponibilidad | sesiones (disponibilidad_id, tutor_id, inicio_en, fin_en, modalidad) | disponibilidades_tutor (id, tutor_id, inicio_en, fin_en, modalidad) | 0..1 | 0..N |
| fk_sesion_situacion | sesiones (estudiante_id, periodo_id) | situaciones_academicas (estudiante_id, periodo_id) | 1 | 0..N |
| fk_sesion_tutor | sesiones (tutor_id) | tutores (id) | 1 | 0..N |
| fk_situacion_carrera | situaciones_academicas (carrera_id) | carreras (id) | 1 | 0..N |
| fk_situacion_estudiante | situaciones_academicas (estudiante_id) | estudiantes (id) | 1 | 0..N |
| fk_situacion_periodo | situaciones_academicas (periodo_id) | periodos_academicos (id) | 1 | 0..N |
| fk_recuperacion_usuario | tokens_recuperacion (usuario_id) | usuarios (id) | 1 | 0..N |
| fk_revocacion_usuario | tokens_revocados (usuario_id) | usuarios (id) | 1 | 0..N |
| fk_tutor_departamento | tutores (departamento_id) | departamentos (id) | 1 | 0..N |
| fk_tutor_usuario | tutores (usuario_id) | usuarios (id) | 1 | 0..1 |
| fk_ur_rol | usuario_roles (rol_id) | roles (id) | 1 | 0..N |
| fk_ur_usuario | usuario_roles (usuario_id) | usuarios (id) | 1 | 0..N |

## Columnas e índices físicos

### actividades_personales

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| propietario_id | bigint unsigned | FK | NO | NULL |  |
| titulo | varchar(200) |  | NO | NULL |  |
| descripcion | text |  | YES | NULL |  |
| tipo | enum('taller','recordatorio') |  | NO | NULL |  |
| fecha | date |  | YES | NULL |  |
| inicio_en | datetime(6) |  | YES | NULL |  |
| fin_en | datetime(6) |  | YES | NULL |  |
| tutor_id | bigint unsigned | FK | YES | NULL |  |
| responsable_texto | varchar(160) |  | YES | NULL |  |
| ubicacion | varchar(200) |  | YES | NULL |  |
| estado | enum('pendiente','completada','cancelada') |  | NO | pendiente |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_actividad_tutor: INDEX (tutor_id).
- ix_actividad_propietario: INDEX (propietario_id, estado, fecha, inicio_en).
- PRIMARY: UNIQUE (id).

CHECK ck_actividad_responsable: `((`tutor_id` is null) or (`responsable_texto` is null))`.

CHECK ck_actividad_tiempo: `(((`inicio_en` is null) and (`fin_en` is null)) or ((`fecha` is null) and (`inicio_en` is not null) and (`fin_en` is not null) and (`fin_en` > `inicio_en`)))`.

### asignaciones_tutor

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| estudiante_id | bigint unsigned | FK | NO | NULL |  |
| tutor_id | bigint unsigned | FK | NO | NULL |  |
| periodo_id | bigint unsigned | FK | NO | NULL |  |
| asignado_por | bigint unsigned | FK | NO | NULL |  |
| inicio_en | datetime(6) |  | NO | NULL |  |
| fin_en | datetime(6) |  | YES | NULL |  |
| estado | enum('pendiente','activa','finalizada','cancelada') |  | NO | pendiente |  |
| motivo | text |  | YES | NULL |  |
| activa_unica | tinyint |  | YES | NULL | (case when (`estado` = _utf8mb4\'activa\') then 1 else NULL end) |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_asignacion_autor: INDEX (asignado_por).
- ix_asignacion_tutor: INDEX (tutor_id, periodo_id, estado).
- PRIMARY: UNIQUE (id).
- uq_asignacion_activa: UNIQUE (estudiante_id, periodo_id, activa_unica).

CHECK ck_asignacion_cierre: `(((`estado` in (_utf8mb4\'finalizada\',_utf8mb4\'cancelada\')) and (`fin_en` is not null)) or ((`estado` in (_utf8mb4\'pendiente\',_utf8mb4\'activa\')) and (`fin_en` is null)))`.

CHECK ck_asignacion_fechas: `((`fin_en` is null) or (`fin_en` >= `inicio_en`))`.

### aviso_destinatarios

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| aviso_id | bigint unsigned | PK, FK | NO | NULL |  |
| usuario_id | bigint unsigned | PK, FK | NO | NULL |  |
| leido_en | datetime(6) |  | YES | NULL |  |
| ocultado_en | datetime(6) |  | YES | NULL |  |

Índices:

- ix_destinatario_lectura: INDEX (usuario_id, leido_en, aviso_id).
- PRIMARY: UNIQUE (aviso_id, usuario_id).

### avisos

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| autor_id | bigint unsigned | FK | NO | NULL |  |
| titulo | varchar(200) |  | NO | NULL |  |
| descripcion | text |  | NO | NULL |  |
| categoria | varchar(40) |  | NO | general |  |
| prioridad | enum('normal','media','alta') |  | NO | normal |  |
| sesion_id | bigint unsigned | FK | YES | NULL |  |
| publicado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| retirado_en | datetime(6) |  | YES | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_aviso_autor: INDEX (autor_id).
- fk_aviso_sesion: INDEX (sesion_id).
- PRIMARY: UNIQUE (id).

CHECK ck_aviso_retiro: `((`retirado_en` is null) or (`retirado_en` >= `publicado_en`))`.

### carreras

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| departamento_id | bigint unsigned | FK | NO | NULL |  |
| clave | varchar(30) | UK | NO | NULL |  |
| nombre | varchar(150) |  | NO | NULL |  |
| activa | tinyint(1) |  | NO | 1 |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_carrera_departamento: INDEX (departamento_id).
- PRIMARY: UNIQUE (id).
- uq_carrera_clave: UNIQUE (clave).

CHECK ck_carrera_activa: `(`activa` in (0,1))`.

### conversacion_participantes

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| conversacion_id | bigint unsigned | PK, FK | NO | NULL |  |
| usuario_id | bigint unsigned | PK, FK | NO | NULL |  |
| unido_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| leido_hasta_en | datetime(6) |  | YES | NULL |  |

Índices:

- ix_participante_usuario: INDEX (usuario_id, conversacion_id).
- PRIMARY: UNIQUE (conversacion_id, usuario_id).

### conversaciones

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| tipo | enum('directa') |  | NO | directa |  |
| creada_por | bigint unsigned | FK | NO | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_conversacion_autor: INDEX (creada_por).
- PRIMARY: UNIQUE (id).

### departamentos

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| nombre | varchar(120) | UK | NO | NULL |  |
| activo | tinyint(1) |  | NO | 1 |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- PRIMARY: UNIQUE (id).
- uq_departamento_nombre: UNIQUE (nombre).

CHECK ck_departamento_activo: `(`activo` in (0,1))`.

### disponibilidades_tutor

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| tutor_id | bigint unsigned | FK | NO | NULL |  |
| inicio_en | datetime(6) |  | NO | NULL |  |
| fin_en | datetime(6) |  | NO | NULL |  |
| modalidad | enum('presencial','virtual') |  | NO | NULL |  |
| ubicacion | varchar(200) |  | YES | NULL |  |
| enlace_virtual | varchar(1024) |  | YES | NULL |  |
| estado | enum('habilitada','bloqueada') |  | NO | habilitada |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- ix_disponibilidad_intervalo: INDEX (tutor_id, inicio_en, fin_en).
- PRIMARY: UNIQUE (id).
- uq_disponibilidad_contexto: UNIQUE (id, tutor_id, inicio_en, fin_en, modalidad).

CHECK ck_disponibilidad_intervalo: `(`fin_en` > `inicio_en`)`.

CHECK ck_disponibilidad_lugar: `(((`modalidad` = _utf8mb4\'presencial\') and (`ubicacion` is not null) and (char_length(trim(`ubicacion`)) > 0)) or ((`modalidad` = _utf8mb4\'virtual\') and (`enlace_virtual` is not null) and (char_length(trim(`enlace_virtual`)) > 0)))`.

### envios_comunicacion

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| usuario_id | bigint unsigned | FK | NO | NULL |  |
| sesion_id | bigint unsigned | FK | YES | NULL |  |
| canal | enum('correo','sms','whatsapp') |  | NO | NULL |  |
| destinatario | varchar(254) |  | NO | NULL |  |
| tipo_evento | varchar(60) |  | NO | NULL |  |
| clave_idempotencia | varchar(128) | UK | NO | NULL |  |
| estado | enum('pendiente','en_proceso','enviado','entregado','fallido') |  | NO | pendiente |  |
| proveedor | varchar(60) |  | YES | NULL |  |
| id_externo | varchar(200) |  | YES | NULL |  |
| intentos | smallint unsigned |  | NO | 0 |  |
| codigo_error | varchar(80) |  | YES | NULL |  |
| detalle_error | text |  | YES | NULL |  |
| programado_en | datetime(6) |  | YES | NULL |  |
| enviado_en | datetime(6) |  | YES | NULL |  |
| entregado_en | datetime(6) |  | YES | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_envio_sesion: INDEX (sesion_id).
- ix_envio_pendientes: INDEX (estado, programado_en, creado_en).
- ix_envio_proveedor: INDEX (proveedor, id_externo).
- ix_envio_usuario: INDEX (usuario_id, creado_en).
- PRIMARY: UNIQUE (id).
- uq_envio_idempotencia: UNIQUE (clave_idempotencia).

CHECK ck_envio_destinatario: `(char_length(trim(`destinatario`)) > 0)`.

CHECK ck_envio_entrega: `((`entregado_en` is null) or ((`enviado_en` is not null) and (`entregado_en` >= `enviado_en`)))`.

CHECK ck_envio_estado: `(((`estado` not in (_utf8mb4\'enviado\',_utf8mb4\'entregado\')) or (`enviado_en` is not null)) and ((`estado` <> _utf8mb4\'entregado\') or (`entregado_en` is not null)))`.

CHECK ck_envio_evento: `(char_length(trim(`tipo_evento`)) > 0)`.

CHECK ck_envio_idempotencia: `(char_length(trim(`clave_idempotencia`)) > 0)`.

### estudiantes

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| usuario_id | bigint unsigned | FK, UK | NO | NULL |  |
| matricula | varchar(30) | UK | NO | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- PRIMARY: UNIQUE (id).
- uq_estudiante_matricula: UNIQUE (matricula).
- uq_estudiante_usuario: UNIQUE (usuario_id).

CHECK ck_estudiante_matricula: `(char_length(trim(`matricula`)) > 0)`.

### materiales_sesion

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| sesion_id | bigint unsigned | FK | NO | NULL |  |
| nombre | varchar(200) |  | NO | NULL |  |
| archivo_url | varchar(1024) |  | YES | NULL |  |
| agregado_por | bigint unsigned | FK | NO | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_material_autor: INDEX (agregado_por).
- fk_material_sesion: INDEX (sesion_id).
- PRIMARY: UNIQUE (id).

### mediciones_academicas

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| situacion_id | bigint unsigned | FK | NO | NULL |  |
| medido_en | datetime(6) |  | NO | NULL |  |
| promedio | decimal(4,2) |  | YES | NULL |  |
| nivel_riesgo | enum('sin_riesgo','bajo','medio','alto') |  | YES | NULL |  |
| condicion | text |  | YES | NULL |  |
| materias_reprobadas | smallint unsigned |  | YES | NULL |  |
| materias_repeticion | smallint unsigned |  | YES | NULL |  |
| materias_especial | smallint unsigned |  | YES | NULL |  |
| registrado_por | bigint unsigned | FK | NO | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_medicion_autor: INDEX (registrado_por).
- ix_medicion_ultima: INDEX (situacion_id, medido_en, id).
- PRIMARY: UNIQUE (id).

CHECK ck_medicion_promedio: `(`promedio` between 0 and 10)`.

### mensajes

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| conversacion_id | bigint unsigned | FK | NO | NULL |  |
| emisor_id | bigint unsigned | FK | NO | NULL |  |
| contenido | text |  | NO | NULL |  |
| enviado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_mensaje_participante: INDEX (conversacion_id, emisor_id).
- ix_mensaje_historial: INDEX (conversacion_id, enviado_en, id).
- PRIMARY: UNIQUE (id).

CHECK ck_mensaje_contenido: `(char_length(trim(`contenido`)) > 0)`.

### motivos_reporte

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| codigo | varchar(40) | UK | NO | NULL |  |
| nombre | varchar(120) |  | NO | NULL |  |
| activo | tinyint(1) |  | NO | 1 |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- PRIMARY: UNIQUE (id).
- uq_motivo_codigo: UNIQUE (codigo).

CHECK ck_motivo_activo: `(`activo` in (0,1))`.

### observaciones_estudiante

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| situacion_id | bigint unsigned | FK | NO | NULL |  |
| estudiante_id | bigint unsigned | FK | NO | NULL |  |
| periodo_id | bigint unsigned | FK | NO | NULL |  |
| autor_id | bigint unsigned | FK | NO | NULL |  |
| sesion_id | bigint unsigned | FK | YES | NULL |  |
| tipo | enum('nota','progreso','alerta') |  | NO | nota |  |
| prioridad | enum('normal','alta') |  | NO | normal |  |
| contenido | text |  | NO | NULL |  |
| observado_en | datetime(6) |  | NO | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_observacion_autor: INDEX (autor_id).
- fk_observacion_sesion: INDEX (sesion_id, estudiante_id, periodo_id).
- fk_observacion_situacion: INDEX (situacion_id, estudiante_id, periodo_id).
- ix_observacion_situacion: INDEX (situacion_id, observado_en).
- PRIMARY: UNIQUE (id).

CHECK ck_observacion_texto: `(char_length(trim(`contenido`)) > 0)`.

### periodos_academicos

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| clave | varchar(30) | UK | NO | NULL |  |
| nombre | varchar(80) |  | NO | NULL |  |
| fecha_inicio | date |  | NO | NULL |  |
| fecha_fin | date |  | NO | NULL |  |
| estado | enum('planeado','activo','cerrado') |  | NO | planeado |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- PRIMARY: UNIQUE (id).
- uq_periodo_clave: UNIQUE (clave).

CHECK ck_periodo_fechas: `(`fecha_fin` >= `fecha_inicio`)`.

### reportes

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| estudiante_id | bigint unsigned | FK | NO | NULL |  |
| tutor_id | bigint unsigned | FK | NO | NULL |  |
| periodo_id | bigint unsigned | FK | NO | NULL |  |
| sesion_id | bigint unsigned | FK | YES | NULL |  |
| motivo_id | bigint unsigned | FK | NO | NULL |  |
| fecha | date |  | NO | NULL |  |
| contenido | text |  | YES | NULL |  |
| estado | enum('pendiente','completado') |  | NO | pendiente |  |
| completado_en | datetime(6) |  | YES | NULL |  |
| completado_por | bigint unsigned | FK | YES | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_reporte_autor_cierre: INDEX (completado_por).
- fk_reporte_motivo: INDEX (motivo_id).
- fk_reporte_sesion: INDEX (sesion_id, estudiante_id, tutor_id, periodo_id).
- ix_reporte_estudiante: INDEX (estudiante_id, periodo_id, fecha).
- ix_reporte_tutor: INDEX (tutor_id, periodo_id, estado, fecha).
- PRIMARY: UNIQUE (id).

CHECK ck_reporte_cierre: `(((`estado` = _utf8mb4\'pendiente\') and (`completado_en` is null) and (`completado_por` is null)) or ((`estado` = _utf8mb4\'completado\') and (`completado_en` is not null) and (`completado_por` is not null)))`.

### roles

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| codigo | varchar(30) | UK | NO | NULL |  |
| nombre | varchar(60) |  | NO | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- PRIMARY: UNIQUE (id).
- uq_roles_codigo: UNIQUE (codigo).

CHECK ck_roles_codigo: `(`codigo` in (_utf8mb4\'coordinador\',_utf8mb4\'tutor\',_utf8mb4\'estudiante\'))`.

### sesion_cambios

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| sesion_id | bigint unsigned | FK | NO | NULL |  |
| actor_id | bigint unsigned | FK | NO | NULL |  |
| tipo | enum('solicitud','aceptacion','rechazo','reprogramacion','cancelacion','cierre','registro') |  | NO | NULL |  |
| estado_anterior | enum('solicitada','confirmada','rechazada','cancelada','completada','no_asistio') |  | YES | NULL |  |
| estado_nuevo | enum('solicitada','confirmada','rechazada','cancelada','completada','no_asistio') |  | NO | NULL |  |
| inicio_anterior | datetime(6) |  | YES | NULL |  |
| fin_anterior | datetime(6) |  | YES | NULL |  |
| inicio_nuevo | datetime(6) |  | NO | NULL |  |
| fin_nuevo | datetime(6) |  | NO | NULL |  |
| motivo | text |  | YES | NULL |  |
| ocurrido_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_cambio_actor: INDEX (actor_id).
- ix_cambio_sesion: INDEX (sesion_id, ocurrido_en, id).
- PRIMARY: UNIQUE (id).

CHECK ck_cambio_anterior: `(((`inicio_anterior` is null) and (`fin_anterior` is null)) or ((`inicio_anterior` is not null) and (`fin_anterior` is not null) and (`fin_anterior` > `inicio_anterior`)))`.

CHECK ck_cambio_intervalo: `(`fin_nuevo` > `inicio_nuevo`)`.

### sesiones

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| tutor_id | bigint unsigned | FK | NO | NULL |  |
| estudiante_id | bigint unsigned | FK | NO | NULL |  |
| periodo_id | bigint unsigned | FK | NO | NULL |  |
| disponibilidad_id | bigint unsigned | FK | YES | NULL |  |
| tema | varchar(200) |  | NO | NULL |  |
| inicio_en | datetime(6) | FK | NO | NULL |  |
| fin_en | datetime(6) | FK | NO | NULL |  |
| modalidad | enum('presencial','virtual') | FK | NO | NULL |  |
| ubicacion | varchar(200) |  | YES | NULL |  |
| enlace_virtual | varchar(1024) |  | YES | NULL |  |
| estado | enum('solicitada','confirmada','rechazada','cancelada','completada','no_asistio') |  | NO | solicitada |  |
| mensaje_solicitud | text |  | YES | NULL |  |
| observaciones | text |  | YES | NULL |  |
| inicio_real_en | datetime(6) |  | YES | NULL |  |
| fin_real_en | datetime(6) |  | YES | NULL |  |
| creado_por | bigint unsigned | FK | NO | NULL |  |
| reserva_vigente | bigint unsigned | UK | YES | NULL | (case when (`estado` in (_utf8mb4\'solicitada\',_utf8mb4\'confirmada\')) then `disponibilidad_id` else NULL end) |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_sesion_autor: INDEX (creado_por).
- fk_sesion_disponibilidad: INDEX (disponibilidad_id, tutor_id, inicio_en, fin_en, modalidad).
- fk_sesion_situacion: INDEX (estudiante_id, periodo_id).
- ix_sesion_estudiante_agenda: INDEX (estudiante_id, inicio_en, estado).
- ix_sesion_tutor_agenda: INDEX (tutor_id, inicio_en, estado).
- PRIMARY: UNIQUE (id).
- uq_sesion_contexto: UNIQUE (id, estudiante_id, tutor_id, periodo_id).
- uq_sesion_estudiante_periodo: UNIQUE (id, estudiante_id, periodo_id).
- uq_sesion_reserva_vigente: UNIQUE (reserva_vigente).

CHECK ck_sesion_intervalo: `(`fin_en` > `inicio_en`)`.

CHECK ck_sesion_real: `(((`inicio_real_en` is null) and (`fin_real_en` is null)) or ((`inicio_real_en` is not null) and (`fin_real_en` is not null) and (`fin_real_en` > `inicio_real_en`)))`.

CHECK ck_sesion_tema: `(char_length(trim(`tema`)) > 0)`.

CHECK ck_sesion_ubicacion: `(((`modalidad` = _utf8mb4\'presencial\') and (`ubicacion` is not null) and (char_length(trim(`ubicacion`)) > 0)) or ((`modalidad` = _utf8mb4\'virtual\') and (`enlace_virtual` is not null) and (char_length(trim(`enlace_virtual`)) > 0)))`.

### situaciones_academicas

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| estudiante_id | bigint unsigned | FK | NO | NULL |  |
| periodo_id | bigint unsigned | FK | NO | NULL |  |
| carrera_id | bigint unsigned | FK | NO | NULL |  |
| semestre | smallint unsigned |  | NO | NULL |  |
| estado_inscripcion | enum('activo','baja','egresado') |  | NO | activo |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_situacion_carrera: INDEX (carrera_id).
- ix_situacion_periodo_carrera: INDEX (periodo_id, carrera_id, estado_inscripcion).
- PRIMARY: UNIQUE (id).
- uq_situacion_contexto: UNIQUE (id, estudiante_id, periodo_id).
- uq_situacion_estudiante_periodo: UNIQUE (estudiante_id, periodo_id).

CHECK ck_situacion_semestre: `(`semestre` > 0)`.

### tokens_recuperacion

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| usuario_id | bigint unsigned | FK | NO | NULL |  |
| token_hash | char(64) | UK | NO | NULL |  |
| vence_en | datetime(6) |  | NO | NULL |  |
| utilizado_en | datetime(6) |  | YES | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |

Índices:

- ix_recuperacion_expira: INDEX (vence_en).
- ix_recuperacion_usuario: INDEX (usuario_id, vence_en, utilizado_en).
- PRIMARY: UNIQUE (id).
- uq_recuperacion_hash: UNIQUE (token_hash).

CHECK ck_recuperacion_hash: `regexp_like(`token_hash`,_utf8mb4\'^[0-9a-f]{64}$\',_utf8mb4\'c\')`.

CHECK ck_recuperacion_uso: `((`utilizado_en` is null) or ((`utilizado_en` >= `creado_en`) and (`utilizado_en` <= `vence_en`)))`.

CHECK ck_recuperacion_vencimiento: `(`vence_en` > `creado_en`)`.

### tokens_revocados

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| usuario_id | bigint unsigned | FK | NO | NULL |  |
| jti | varchar(128) | UK | NO | NULL |  |
| vence_en | datetime(6) |  | NO | NULL |  |
| revocado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| motivo | enum('logout','cambio_password','administracion') |  | NO | logout |  |

Índices:

- ix_revocacion_expira: INDEX (vence_en).
- ix_revocacion_usuario: INDEX (usuario_id, revocado_en).
- PRIMARY: UNIQUE (id).
- uq_revocacion_jti: UNIQUE (jti).

CHECK ck_revocacion_jti: `(char_length(trim(`jti`)) > 0)`.

CHECK ck_revocacion_vencimiento: `(`vence_en` > `revocado_en`)`.

### tutores

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| usuario_id | bigint unsigned | FK, UK | NO | NULL |  |
| departamento_id | bigint unsigned | FK | NO | NULL |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- fk_tutor_departamento: INDEX (departamento_id).
- PRIMARY: UNIQUE (id).
- uq_tutor_usuario: UNIQUE (usuario_id).

### usuario_roles

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| usuario_id | bigint unsigned | PK, FK | NO | NULL |  |
| rol_id | bigint unsigned | PK, FK | NO | NULL |  |
| asignado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |

Índices:

- fk_ur_rol: INDEX (rol_id).
- PRIMARY: UNIQUE (usuario_id, rol_id).

### usuarios

| Columna | Tipo | Clave | NULL | Default | Extra / expresión |
|---|---|---|---|---|---|
| id | bigint unsigned | PK | NO | NULL | auto_increment |
| nombre | varchar(160) |  | NO | NULL |  |
| correo | varchar(254) | UK | NO | NULL |  |
| password_hash | varchar(255) |  | NO | NULL |  |
| telefono | varchar(30) |  | YES | NULL |  |
| avatar_url | varchar(1024) |  | YES | NULL |  |
| estado | enum('activo','inactivo') |  | NO | activo |  |
| creado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
| actualizado_en | datetime(6) |  | NO | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6) |

Índices:

- PRIMARY: UNIQUE (id).
- uq_usuarios_correo: UNIQUE (correo).

CHECK ck_usuarios_correo: `((`correo` = trim(`correo`)) and (char_length(`correo`) > 3))`.

CHECK ck_usuarios_hash: `(char_length(`password_hash`) >= 20)`.

CHECK ck_usuarios_nombre: `(char_length(trim(`nombre`)) > 0)`.

## Vistas existentes (no entidades adicionales)

### v_avisos_visibles

```sql
select `a`.`id` AS `aviso_id`,`d`.`usuario_id` AS `usuario_id`,`a`.`titulo` AS `titulo`,`a`.`descripcion` AS `descripcion`,`a`.`categoria` AS `categoria`,`a`.`prioridad` AS `prioridad`,`a`.`sesion_id` AS `sesion_id`,`a`.`publicado_en` AS `publicado_en`,`d`.`leido_en` AS `leido_en` from (`tutoria`.`avisos` `a` join `tutoria`.`aviso_destinatarios` `d` on((`d`.`aviso_id` = `a`.`id`))) where ((`a`.`retirado_en` is null) and (`d`.`ocultado_en` is null))
```

### v_expediente_actual_por_periodo

```sql
select `a`.`id` AS `situacion_id`,`e`.`id` AS `estudiante_id`,`u`.`nombre` AS `nombre`,`e`.`matricula` AS `matricula`,`u`.`correo` AS `correo`,`u`.`telefono` AS `telefono`,`a`.`periodo_id` AS `periodo_id`,`a`.`carrera_id` AS `carrera_id`,`c`.`nombre` AS `carrera`,`a`.`semestre` AS `semestre`,`a`.`estado_inscripcion` AS `estado_inscripcion`,`tutoria`.`m`.`promedio` AS `promedio`,`tutoria`.`m`.`nivel_riesgo` AS `nivel_riesgo`,`tutoria`.`m`.`condicion` AS `condicion`,`tutoria`.`m`.`medido_en` AS `medido_en` from ((((`tutoria`.`situaciones_academicas` `a` join `tutoria`.`estudiantes` `e` on((`e`.`id` = `a`.`estudiante_id`))) join `tutoria`.`usuarios` `u` on((`u`.`id` = `e`.`usuario_id`))) join `tutoria`.`carreras` `c` on((`c`.`id` = `a`.`carrera_id`))) left join `tutoria`.`v_ultima_medicion_academica` `m` on((`tutoria`.`m`.`situacion_id` = `a`.`id`)))
```

### v_resumen_sesiones

```sql
select `tutoria`.`sesiones`.`estudiante_id` AS `estudiante_id`,`tutoria`.`sesiones`.`tutor_id` AS `tutor_id`,`tutoria`.`sesiones`.`periodo_id` AS `periodo_id`,sum((`tutoria`.`sesiones`.`estado` = 'completada')) AS `sesiones_completadas`,max((case when (`tutoria`.`sesiones`.`estado` = 'completada') then `tutoria`.`sesiones`.`inicio_en` end)) AS `ultima_sesion`,(sum((case when ((`tutoria`.`sesiones`.`estado` = 'completada') and (`tutoria`.`sesiones`.`inicio_real_en` is not null)) then timestampdiff(SECOND,`tutoria`.`sesiones`.`inicio_real_en`,`tutoria`.`sesiones`.`fin_real_en`) else 0 end)) / 3600.0) AS `horas_reales`,sum(((`tutoria`.`sesiones`.`estado` = 'completada') and (`tutoria`.`sesiones`.`inicio_real_en` is null))) AS `completadas_sin_duracion_real` from `tutoria`.`sesiones` group by `tutoria`.`sesiones`.`estudiante_id`,`tutoria`.`sesiones`.`tutor_id`,`tutoria`.`sesiones`.`periodo_id`
```

### v_ultima_medicion_academica

```sql
select `ordenadas`.`id` AS `id`,`ordenadas`.`situacion_id` AS `situacion_id`,`ordenadas`.`medido_en` AS `medido_en`,`ordenadas`.`promedio` AS `promedio`,`ordenadas`.`nivel_riesgo` AS `nivel_riesgo`,`ordenadas`.`condicion` AS `condicion`,`ordenadas`.`materias_reprobadas` AS `materias_reprobadas`,`ordenadas`.`materias_repeticion` AS `materias_repeticion`,`ordenadas`.`materias_especial` AS `materias_especial`,`ordenadas`.`registrado_por` AS `registrado_por` from (select `m`.`id` AS `id`,`m`.`situacion_id` AS `situacion_id`,`m`.`medido_en` AS `medido_en`,`m`.`promedio` AS `promedio`,`m`.`nivel_riesgo` AS `nivel_riesgo`,`m`.`condicion` AS `condicion`,`m`.`materias_reprobadas` AS `materias_reprobadas`,`m`.`materias_repeticion` AS `materias_repeticion`,`m`.`materias_especial` AS `materias_especial`,`m`.`registrado_por` AS `registrado_por`,`m`.`creado_en` AS `creado_en`,`m`.`actualizado_en` AS `actualizado_en`,row_number() OVER (PARTITION BY `m`.`situacion_id` ORDER BY `m`.`medido_en` desc,`m`.`id` desc )  AS `posicion` from `tutoria`.`mediciones_academicas` `m`) `ordenadas` where (`ordenadas`.`posicion` = 1)
```

## Restricciones condicionales

- activa_unica limita a una asignación activa por estudiante/periodo, pero permite múltiples asignaciones históricas. La cardinalidad histórica entre tutores y asignaciones sigue siendo 1:N.
- reserva_vigente permite solo una sesión solicitada/confirmada por disponibilidad; sesiones canceladas/rechazadas históricas no consumen esa unicidad. Por eso disponibilidad a sesiones se representa 1:N, no 1:1.
- La pertenencia temporal al periodo, las transiciones de estado, los solapamientos entre diferentes disponibilidades y la cantidad de participantes de un chat no están impuestos por las FK. El diagrama no los inventa.
