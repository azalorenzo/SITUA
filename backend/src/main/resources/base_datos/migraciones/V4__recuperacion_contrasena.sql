CREATE TABLE recuperaciones_contrasena (
    usuario_id BIGINT UNSIGNED NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    solicitado_en DATETIME(6) NOT NULL,
    vence_en DATETIME(6) NOT NULL,
    intentos TINYINT UNSIGNED NOT NULL DEFAULT 0,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_codigo_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT ck_codigo_recuperacion_intentos CHECK (intentos <= 5),
    CONSTRAINT ck_codigo_recuperacion_vencimiento CHECK (vence_en > solicitado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
