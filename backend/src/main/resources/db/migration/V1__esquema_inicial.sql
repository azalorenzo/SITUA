-- V1: esquema inicial reproducible. Flyway selecciona la base de destino.
-- Sistema de tutorías. Esquema inicial para MySQL 8.0.16+ / 26.10.
-- Ejecutar UNA VEZ en una base nueva. No elimina ni reemplaza bases existentes.
-- Instantes DATETIME(6) en UTC, fechas académicas DATE.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

CREATE TABLE usuarios (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(160) NOT NULL,
  correo VARCHAR(254) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  telefono VARCHAR(30) NULL,
  avatar_url VARCHAR(1024) NULL,
  estado ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
  UNIQUE KEY uq_usuarios_correo (correo),
  CONSTRAINT ck_usuarios_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
  CONSTRAINT ck_usuarios_correo CHECK (correo = TRIM(correo) AND CHAR_LENGTH(correo) > 3),
  CONSTRAINT ck_usuarios_hash CHECK (CHAR_LENGTH(password_hash) >= 20),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(30) NOT NULL,
  nombre VARCHAR(60) NOT NULL,
  UNIQUE KEY uq_roles_codigo (codigo),
  CONSTRAINT ck_roles_codigo CHECK (codigo IN ('coordinador','tutor','estudiante')),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE usuario_roles (
  usuario_id BIGINT UNSIGNED NOT NULL,
  rol_id BIGINT UNSIGNED NOT NULL,
  asignado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (usuario_id, rol_id),
  CONSTRAINT fk_ur_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
  CONSTRAINT fk_ur_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE departamentos (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_departamento_nombre (nombre),
  CONSTRAINT ck_departamento_activo CHECK (activo IN (0,1)),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE carreras (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  departamento_id BIGINT UNSIGNED NOT NULL,
  clave VARCHAR(30) NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_carrera_clave (clave),
  CONSTRAINT ck_carrera_activa CHECK (activa IN (0,1)),
  CONSTRAINT fk_carrera_departamento FOREIGN KEY (departamento_id) REFERENCES departamentos (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE tutores (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT UNSIGNED NOT NULL,
  departamento_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uq_tutor_usuario (usuario_id),
  CONSTRAINT fk_tutor_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
  CONSTRAINT fk_tutor_departamento FOREIGN KEY (departamento_id) REFERENCES departamentos (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE estudiantes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT UNSIGNED NOT NULL,
  matricula VARCHAR(30) NOT NULL,
  UNIQUE KEY uq_estudiante_usuario (usuario_id),
  UNIQUE KEY uq_estudiante_matricula (matricula),
  CONSTRAINT ck_estudiante_matricula CHECK (CHAR_LENGTH(TRIM(matricula)) > 0),
  CONSTRAINT fk_estudiante_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE periodos_academicos (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  clave VARCHAR(30) NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  estado ENUM('planeado','activo','cerrado') NOT NULL DEFAULT 'planeado',
  UNIQUE KEY uq_periodo_clave (clave),
  CONSTRAINT ck_periodo_fechas CHECK (fecha_fin >= fecha_inicio),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE situaciones_academicas (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  estudiante_id BIGINT UNSIGNED NOT NULL,
  periodo_id BIGINT UNSIGNED NOT NULL,
  carrera_id BIGINT UNSIGNED NOT NULL,
  semestre SMALLINT UNSIGNED NOT NULL,
  estado_inscripcion ENUM('activo','baja','egresado') NOT NULL DEFAULT 'activo',
  UNIQUE KEY uq_situacion_estudiante_periodo (estudiante_id, periodo_id),
  UNIQUE KEY uq_situacion_contexto (id, estudiante_id, periodo_id),
  KEY ix_situacion_periodo_carrera (periodo_id, carrera_id, estado_inscripcion),
  CONSTRAINT ck_situacion_semestre CHECK (semestre > 0),
  CONSTRAINT fk_situacion_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id),
  CONSTRAINT fk_situacion_periodo FOREIGN KEY (periodo_id) REFERENCES periodos_academicos (id),
  CONSTRAINT fk_situacion_carrera FOREIGN KEY (carrera_id) REFERENCES carreras (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE mediciones_academicas (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  situacion_id BIGINT UNSIGNED NOT NULL,
  medido_en DATETIME(6) NOT NULL,
  promedio DECIMAL(4,2) NULL,
  nivel_riesgo ENUM('sin_riesgo','bajo','medio','alto') NULL,
  condicion TEXT NULL,
  materias_reprobadas SMALLINT UNSIGNED NULL,
  materias_repeticion SMALLINT UNSIGNED NULL,
  materias_especial SMALLINT UNSIGNED NULL,
  registrado_por BIGINT UNSIGNED NOT NULL,
  KEY ix_medicion_ultima (situacion_id, medido_en, id),
  CONSTRAINT ck_medicion_promedio CHECK (promedio BETWEEN 0 AND 10),
  CONSTRAINT fk_medicion_situacion FOREIGN KEY (situacion_id) REFERENCES situaciones_academicas (id),
  CONSTRAINT fk_medicion_autor FOREIGN KEY (registrado_por) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE asignaciones_tutor (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  estudiante_id BIGINT UNSIGNED NOT NULL,
  tutor_id BIGINT UNSIGNED NOT NULL,
  periodo_id BIGINT UNSIGNED NOT NULL,
  asignado_por BIGINT UNSIGNED NOT NULL,
  inicio_en DATETIME(6) NOT NULL,
  fin_en DATETIME(6) NULL,
  estado ENUM('pendiente','activa','finalizada','cancelada') NOT NULL DEFAULT 'pendiente',
  motivo TEXT NULL,
  activa_unica TINYINT GENERATED ALWAYS AS (CASE WHEN estado = 'activa' THEN 1 ELSE NULL END) STORED,
  UNIQUE KEY uq_asignacion_activa (estudiante_id, periodo_id, activa_unica),
  KEY ix_asignacion_tutor (tutor_id, periodo_id, estado),
  CONSTRAINT ck_asignacion_fechas CHECK (fin_en IS NULL OR fin_en >= inicio_en),
  CONSTRAINT ck_asignacion_cierre CHECK ((estado IN ('finalizada','cancelada') AND fin_en IS NOT NULL) OR (estado IN ('pendiente','activa') AND fin_en IS NULL)),
  CONSTRAINT fk_asignacion_situacion FOREIGN KEY (estudiante_id, periodo_id) REFERENCES situaciones_academicas (estudiante_id, periodo_id),
  CONSTRAINT fk_asignacion_tutor FOREIGN KEY (tutor_id) REFERENCES tutores (id),
  CONSTRAINT fk_asignacion_autor FOREIGN KEY (asignado_por) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE disponibilidades_tutor (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tutor_id BIGINT UNSIGNED NOT NULL,
  inicio_en DATETIME(6) NOT NULL,
  fin_en DATETIME(6) NOT NULL,
  modalidad ENUM('presencial','virtual') NOT NULL,
  ubicacion VARCHAR(200) NULL,
  enlace_virtual VARCHAR(1024) NULL,
  estado ENUM('habilitada','bloqueada') NOT NULL DEFAULT 'habilitada',
  UNIQUE KEY uq_disponibilidad_contexto (id, tutor_id, inicio_en, fin_en, modalidad),
  KEY ix_disponibilidad_intervalo (tutor_id, inicio_en, fin_en),
  CONSTRAINT ck_disponibilidad_intervalo CHECK (fin_en > inicio_en),
  CONSTRAINT ck_disponibilidad_lugar CHECK ((modalidad = 'presencial' AND ubicacion IS NOT NULL AND CHAR_LENGTH(TRIM(ubicacion)) > 0) OR (modalidad = 'virtual' AND enlace_virtual IS NOT NULL AND CHAR_LENGTH(TRIM(enlace_virtual)) > 0)),
  CONSTRAINT fk_disponibilidad_tutor FOREIGN KEY (tutor_id) REFERENCES tutores (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE sesiones (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tutor_id BIGINT UNSIGNED NOT NULL,
  estudiante_id BIGINT UNSIGNED NOT NULL,
  periodo_id BIGINT UNSIGNED NOT NULL,
  disponibilidad_id BIGINT UNSIGNED NULL,
  tema VARCHAR(200) NOT NULL,
  inicio_en DATETIME(6) NOT NULL,
  fin_en DATETIME(6) NOT NULL,
  modalidad ENUM('presencial','virtual') NOT NULL,
  ubicacion VARCHAR(200) NULL,
  enlace_virtual VARCHAR(1024) NULL,
  estado ENUM('solicitada','confirmada','rechazada','cancelada','completada','no_asistio') NOT NULL DEFAULT 'solicitada',
  mensaje_solicitud TEXT NULL,
  observaciones TEXT NULL,
  inicio_real_en DATETIME(6) NULL,
  fin_real_en DATETIME(6) NULL,
  creado_por BIGINT UNSIGNED NOT NULL,
  reserva_vigente BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN estado IN ('solicitada','confirmada') THEN disponibilidad_id ELSE NULL END) STORED,
  UNIQUE KEY uq_sesion_reserva_vigente (reserva_vigente),
  UNIQUE KEY uq_sesion_contexto (id, estudiante_id, tutor_id, periodo_id),
  UNIQUE KEY uq_sesion_estudiante_periodo (id, estudiante_id, periodo_id),
  KEY ix_sesion_tutor_agenda (tutor_id, inicio_en, estado),
  KEY ix_sesion_estudiante_agenda (estudiante_id, inicio_en, estado),
  CONSTRAINT ck_sesion_intervalo CHECK (fin_en > inicio_en),
  CONSTRAINT ck_sesion_real CHECK ((inicio_real_en IS NULL AND fin_real_en IS NULL) OR (inicio_real_en IS NOT NULL AND fin_real_en IS NOT NULL AND fin_real_en > inicio_real_en)),
  CONSTRAINT ck_sesion_tema CHECK (CHAR_LENGTH(TRIM(tema)) > 0),
  CONSTRAINT ck_sesion_ubicacion CHECK ((modalidad = 'presencial' AND ubicacion IS NOT NULL AND CHAR_LENGTH(TRIM(ubicacion)) > 0) OR (modalidad = 'virtual' AND enlace_virtual IS NOT NULL AND CHAR_LENGTH(TRIM(enlace_virtual)) > 0)),
  CONSTRAINT fk_sesion_tutor FOREIGN KEY (tutor_id) REFERENCES tutores (id),
  CONSTRAINT fk_sesion_situacion FOREIGN KEY (estudiante_id, periodo_id) REFERENCES situaciones_academicas (estudiante_id, periodo_id),
  CONSTRAINT fk_sesion_disponibilidad FOREIGN KEY (disponibilidad_id, tutor_id, inicio_en, fin_en, modalidad) REFERENCES disponibilidades_tutor (id, tutor_id, inicio_en, fin_en, modalidad),
  CONSTRAINT fk_sesion_autor FOREIGN KEY (creado_por) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE observaciones_estudiante (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  situacion_id BIGINT UNSIGNED NOT NULL,
  estudiante_id BIGINT UNSIGNED NOT NULL,
  periodo_id BIGINT UNSIGNED NOT NULL,
  autor_id BIGINT UNSIGNED NOT NULL,
  sesion_id BIGINT UNSIGNED NULL,
  tipo ENUM('nota','progreso','alerta') NOT NULL DEFAULT 'nota',
  prioridad ENUM('normal','alta') NOT NULL DEFAULT 'normal',
  contenido TEXT NOT NULL,
  observado_en DATETIME(6) NOT NULL,
  KEY ix_observacion_situacion (situacion_id, observado_en),
  CONSTRAINT ck_observacion_texto CHECK (CHAR_LENGTH(TRIM(contenido)) > 0),
  CONSTRAINT fk_observacion_situacion FOREIGN KEY (situacion_id, estudiante_id, periodo_id) REFERENCES situaciones_academicas (id, estudiante_id, periodo_id),
  CONSTRAINT fk_observacion_sesion FOREIGN KEY (sesion_id, estudiante_id, periodo_id) REFERENCES sesiones (id, estudiante_id, periodo_id),
  CONSTRAINT fk_observacion_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE sesion_cambios (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sesion_id BIGINT UNSIGNED NOT NULL,
  actor_id BIGINT UNSIGNED NOT NULL,
  tipo ENUM('solicitud','aceptacion','rechazo','reprogramacion','cancelacion','cierre','registro') NOT NULL,
  estado_anterior ENUM('solicitada','confirmada','rechazada','cancelada','completada','no_asistio') NULL,
  estado_nuevo ENUM('solicitada','confirmada','rechazada','cancelada','completada','no_asistio') NOT NULL,
  inicio_anterior DATETIME(6) NULL,
  fin_anterior DATETIME(6) NULL,
  inicio_nuevo DATETIME(6) NOT NULL,
  fin_nuevo DATETIME(6) NOT NULL,
  motivo TEXT NULL,
  ocurrido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY ix_cambio_sesion (sesion_id, ocurrido_en, id),
  CONSTRAINT ck_cambio_intervalo CHECK (fin_nuevo > inicio_nuevo),
  CONSTRAINT ck_cambio_anterior CHECK ((inicio_anterior IS NULL AND fin_anterior IS NULL) OR (inicio_anterior IS NOT NULL AND fin_anterior IS NOT NULL AND fin_anterior > inicio_anterior)),
  CONSTRAINT fk_cambio_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones (id),
  CONSTRAINT fk_cambio_actor FOREIGN KEY (actor_id) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE materiales_sesion (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sesion_id BIGINT UNSIGNED NOT NULL,
  nombre VARCHAR(200) NOT NULL,
  archivo_url VARCHAR(1024) NULL,
  agregado_por BIGINT UNSIGNED NOT NULL,
  CONSTRAINT fk_material_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones (id),
  CONSTRAINT fk_material_autor FOREIGN KEY (agregado_por) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE actividades_personales (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  propietario_id BIGINT UNSIGNED NOT NULL,
  titulo VARCHAR(200) NOT NULL,
  descripcion TEXT NULL,
  tipo ENUM('taller','recordatorio') NOT NULL,
  fecha DATE NULL,
  inicio_en DATETIME(6) NULL,
  fin_en DATETIME(6) NULL,
  tutor_id BIGINT UNSIGNED NULL,
  responsable_texto VARCHAR(160) NULL,
  ubicacion VARCHAR(200) NULL,
  estado ENUM('pendiente','completada','cancelada') NOT NULL DEFAULT 'pendiente',
  KEY ix_actividad_propietario (propietario_id, estado, fecha, inicio_en),
  CONSTRAINT ck_actividad_tiempo CHECK ((inicio_en IS NULL AND fin_en IS NULL) OR (fecha IS NULL AND inicio_en IS NOT NULL AND fin_en IS NOT NULL AND fin_en > inicio_en)),
  CONSTRAINT ck_actividad_responsable CHECK (tutor_id IS NULL OR responsable_texto IS NULL),
  CONSTRAINT fk_actividad_propietario FOREIGN KEY (propietario_id) REFERENCES usuarios (id),
  CONSTRAINT fk_actividad_tutor FOREIGN KEY (tutor_id) REFERENCES tutores (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE motivos_reporte (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_motivo_codigo (codigo),
  CONSTRAINT ck_motivo_activo CHECK (activo IN (0,1)),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE reportes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  estudiante_id BIGINT UNSIGNED NOT NULL,
  tutor_id BIGINT UNSIGNED NOT NULL,
  periodo_id BIGINT UNSIGNED NOT NULL,
  sesion_id BIGINT UNSIGNED NULL,
  motivo_id BIGINT UNSIGNED NOT NULL,
  fecha DATE NOT NULL,
  contenido TEXT NULL,
  estado ENUM('pendiente','completado') NOT NULL DEFAULT 'pendiente',
  completado_en DATETIME(6) NULL,
  completado_por BIGINT UNSIGNED NULL,
  KEY ix_reporte_tutor (tutor_id, periodo_id, estado, fecha),
  KEY ix_reporte_estudiante (estudiante_id, periodo_id, fecha),
  CONSTRAINT ck_reporte_cierre CHECK ((estado = 'pendiente' AND completado_en IS NULL AND completado_por IS NULL) OR (estado = 'completado' AND completado_en IS NOT NULL AND completado_por IS NOT NULL)),
  CONSTRAINT fk_reporte_situacion FOREIGN KEY (estudiante_id, periodo_id) REFERENCES situaciones_academicas (estudiante_id, periodo_id),
  CONSTRAINT fk_reporte_tutor FOREIGN KEY (tutor_id) REFERENCES tutores (id),
  CONSTRAINT fk_reporte_sesion FOREIGN KEY (sesion_id, estudiante_id, tutor_id, periodo_id) REFERENCES sesiones (id, estudiante_id, tutor_id, periodo_id),
  CONSTRAINT fk_reporte_motivo FOREIGN KEY (motivo_id) REFERENCES motivos_reporte (id),
  CONSTRAINT fk_reporte_autor_cierre FOREIGN KEY (completado_por) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE avisos (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  autor_id BIGINT UNSIGNED NOT NULL,
  titulo VARCHAR(200) NOT NULL,
  descripcion TEXT NOT NULL,
  categoria VARCHAR(40) NOT NULL DEFAULT 'general',
  prioridad ENUM('normal','media','alta') NOT NULL DEFAULT 'normal',
  sesion_id BIGINT UNSIGNED NULL,
  publicado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  retirado_en DATETIME(6) NULL,
  CONSTRAINT ck_aviso_retiro CHECK (retirado_en IS NULL OR retirado_en >= publicado_en),
  CONSTRAINT fk_aviso_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
  CONSTRAINT fk_aviso_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE aviso_destinatarios (
  aviso_id BIGINT UNSIGNED NOT NULL,
  usuario_id BIGINT UNSIGNED NOT NULL,
  leido_en DATETIME(6) NULL,
  ocultado_en DATETIME(6) NULL,
  PRIMARY KEY (aviso_id, usuario_id),
  KEY ix_destinatario_lectura (usuario_id, leido_en, aviso_id),
  CONSTRAINT fk_destinatario_aviso FOREIGN KEY (aviso_id) REFERENCES avisos (id),
  CONSTRAINT fk_destinatario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE conversaciones (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tipo ENUM('directa') NOT NULL DEFAULT 'directa',
  creada_por BIGINT UNSIGNED NOT NULL,
  CONSTRAINT fk_conversacion_autor FOREIGN KEY (creada_por) REFERENCES usuarios (id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE conversacion_participantes (
  conversacion_id BIGINT UNSIGNED NOT NULL,
  usuario_id BIGINT UNSIGNED NOT NULL,
  unido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  leido_hasta_en DATETIME(6) NULL,
  PRIMARY KEY (conversacion_id, usuario_id),
  KEY ix_participante_usuario (usuario_id, conversacion_id),
  CONSTRAINT fk_participante_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversaciones (id),
  CONSTRAINT fk_participante_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE mensajes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  conversacion_id BIGINT UNSIGNED NOT NULL,
  emisor_id BIGINT UNSIGNED NOT NULL,
  contenido TEXT NOT NULL,
  enviado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY ix_mensaje_historial (conversacion_id, enviado_en, id),
  CONSTRAINT ck_mensaje_contenido CHECK (CHAR_LENGTH(TRIM(contenido)) > 0),
  CONSTRAINT fk_mensaje_participante FOREIGN KEY (conversacion_id, emisor_id) REFERENCES conversacion_participantes (conversacion_id, usuario_id),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

-- Solo catálogos. No se crean usuarios, contraseñas ni expedientes ficticios.
START TRANSACTION;
INSERT INTO roles (codigo, nombre) VALUES
 ('coordinador', 'Coordinación'), ('tutor', 'Tutor'), ('estudiante', 'Estudiante');
INSERT INTO motivos_reporte (codigo, nombre) VALUES
 ('bajo_rendimiento', 'Bajo rendimiento'), ('ansiedad_estres', 'Ansiedad/Estrés'),
 ('problemas_familiares', 'Problemas familiares'), ('otros', 'Otros');
COMMIT;

-- La última medición se desempata por id.
CREATE SQL SECURITY INVOKER VIEW v_ultima_medicion_academica AS
SELECT id, situacion_id, medido_en, promedio, nivel_riesgo, condicion,
       materias_reprobadas, materias_repeticion, materias_especial, registrado_por
FROM (
  SELECT m.*, ROW_NUMBER() OVER (PARTITION BY situacion_id ORDER BY medido_en DESC, id DESC) AS posicion
  FROM mediciones_academicas m
) AS ordenadas WHERE posicion = 1;

CREATE SQL SECURITY INVOKER VIEW v_expediente_actual_por_periodo AS
SELECT a.id AS situacion_id, e.id AS estudiante_id, u.nombre, e.matricula, u.correo,
       u.telefono, a.periodo_id, a.carrera_id, c.nombre AS carrera, a.semestre,
       a.estado_inscripcion, m.promedio, m.nivel_riesgo, m.condicion, m.medido_en
FROM situaciones_academicas a
JOIN estudiantes e ON e.id = a.estudiante_id
JOIN usuarios u ON u.id = e.usuario_id
JOIN carreras c ON c.id = a.carrera_id
LEFT JOIN v_ultima_medicion_academica m ON m.situacion_id = a.id;

CREATE SQL SECURITY INVOKER VIEW v_resumen_sesiones AS
SELECT estudiante_id, tutor_id, periodo_id,
       SUM(estado = 'completada') AS sesiones_completadas,
       MAX(CASE WHEN estado = 'completada' THEN inicio_en END) AS ultima_sesion,
       SUM(CASE WHEN estado = 'completada' AND inicio_real_en IS NOT NULL
                THEN TIMESTAMPDIFF(SECOND, inicio_real_en, fin_real_en) ELSE 0 END) / 3600.0 AS horas_reales,
       SUM(estado = 'completada' AND inicio_real_en IS NULL) AS completadas_sin_duracion_real
FROM sesiones GROUP BY estudiante_id, tutor_id, periodo_id;

CREATE SQL SECURITY INVOKER VIEW v_avisos_visibles AS
SELECT a.id AS aviso_id, d.usuario_id, a.titulo, a.descripcion, a.categoria,
       a.prioridad, a.sesion_id, a.publicado_en, d.leido_en
FROM avisos a JOIN aviso_destinatarios d ON d.aviso_id = a.id
WHERE a.retirado_en IS NULL AND d.ocultado_en IS NULL;
