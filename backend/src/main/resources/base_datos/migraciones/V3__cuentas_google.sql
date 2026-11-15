-- Permite cuentas sin contraseña local cuando se identifican con Google.
ALTER TABLE usuarios MODIFY COLUMN password_hash VARCHAR(255) NULL;

CREATE TABLE identidades_externas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED NOT NULL,
    proveedor VARCHAR(20) NOT NULL,
    identificador_externo VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_identidad_proveedor (proveedor, identificador_externo),
    UNIQUE KEY uq_usuario_proveedor (usuario_id, proveedor),
    CONSTRAINT fk_identidad_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT ck_identidad_proveedor CHECK (proveedor = 'google'),
    CONSTRAINT ck_identidad_identificador CHECK (CHAR_LENGTH(TRIM(identificador_externo)) > 0)
) ENGINE=InnoDB;
