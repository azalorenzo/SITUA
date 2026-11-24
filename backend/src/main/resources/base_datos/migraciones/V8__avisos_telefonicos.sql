ALTER TABLE usuarios
    ADD COLUMN acepta_sms BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN acepta_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN preferencias_contacto_en DATETIME(6) NULL;

CREATE TABLE notificaciones_sesion (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sesion_id BIGINT UNSIGNED NOT NULL,
    canal ENUM('sms','whatsapp') NOT NULL,
    estado ENUM('pendiente','procesando','aceptada','error','incierta','omitida') NOT NULL DEFAULT 'pendiente',
    identificador_proveedor VARCHAR(34) NULL,
    codigo_error VARCHAR(64) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_notificacion_sesion_canal (sesion_id,canal),
    KEY ix_notificacion_estado (estado,id),
    CONSTRAINT fk_notificacion_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones(id) ON DELETE CASCADE
);
