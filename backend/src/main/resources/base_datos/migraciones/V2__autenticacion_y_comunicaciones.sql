-- V2: soporte persistente para recuperación, revocación JWT y comunicaciones.
-- Los tokens de recuperación se almacenan como digest SHA-256, nunca en claro.
CREATE TABLE tokens_recuperacion (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT UNSIGNED NOT NULL,
 token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 vence_en DATETIME(6) NOT NULL,
 utilizado_en DATETIME(6) NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_recuperacion_hash (token_hash),
 KEY ix_recuperacion_usuario (usuario_id, vence_en, utilizado_en),
 KEY ix_recuperacion_expira (vence_en),
 CONSTRAINT fk_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT ck_recuperacion_hash CHECK (REGEXP_LIKE(token_hash, '^[0-9a-f]{64}$', 'c')),
 CONSTRAINT ck_recuperacion_vencimiento CHECK (vence_en > creado_en),
 CONSTRAINT ck_recuperacion_uso CHECK (utilizado_en IS NULL OR (utilizado_en >= creado_en AND utilizado_en <= vence_en))
) ENGINE=InnoDB;

CREATE TABLE tokens_revocados (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT UNSIGNED NOT NULL,
 jti VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 vence_en DATETIME(6) NOT NULL,
 revocado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 motivo ENUM('logout','cambio_password','administracion') NOT NULL DEFAULT 'logout',
 UNIQUE KEY uq_revocacion_jti (jti),
 KEY ix_revocacion_expira (vence_en),
 KEY ix_revocacion_usuario (usuario_id, revocado_en),
 CONSTRAINT fk_revocacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT ck_revocacion_jti CHECK (CHAR_LENGTH(TRIM(jti)) > 0),
 CONSTRAINT ck_revocacion_vencimiento CHECK (vence_en > revocado_en)
) ENGINE=InnoDB;

CREATE TABLE envios_comunicacion (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT UNSIGNED NOT NULL,
 sesion_id BIGINT UNSIGNED NULL,
 canal ENUM('correo','sms','whatsapp') NOT NULL,
 destinatario VARCHAR(254) NOT NULL,
 tipo_evento VARCHAR(60) NOT NULL,
 clave_idempotencia VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 estado ENUM('pendiente','en_proceso','enviado','entregado','fallido') NOT NULL DEFAULT 'pendiente',
 proveedor VARCHAR(60) NULL,
 id_externo VARCHAR(200) NULL,
 intentos SMALLINT UNSIGNED NOT NULL DEFAULT 0,
 codigo_error VARCHAR(80) NULL,
 detalle_error TEXT NULL,
 programado_en DATETIME(6) NULL,
 enviado_en DATETIME(6) NULL,
 entregado_en DATETIME(6) NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_envio_idempotencia (clave_idempotencia),
 KEY ix_envio_usuario (usuario_id, creado_en),
 KEY ix_envio_pendientes (estado, programado_en, creado_en),
 KEY ix_envio_proveedor (proveedor, id_externo),
 CONSTRAINT fk_envio_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT fk_envio_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones(id),
 CONSTRAINT ck_envio_destinatario CHECK (CHAR_LENGTH(TRIM(destinatario)) > 0),
 CONSTRAINT ck_envio_evento CHECK (CHAR_LENGTH(TRIM(tipo_evento)) > 0),
 CONSTRAINT ck_envio_idempotencia CHECK (CHAR_LENGTH(TRIM(clave_idempotencia)) > 0),
 CONSTRAINT ck_envio_entrega CHECK (entregado_en IS NULL OR (enviado_en IS NOT NULL AND entregado_en >= enviado_en)),
 CONSTRAINT ck_envio_estado CHECK ((estado NOT IN ('enviado','entregado') OR enviado_en IS NOT NULL) AND (estado <> 'entregado' OR entregado_en IS NOT NULL))
) ENGINE=InnoDB;
