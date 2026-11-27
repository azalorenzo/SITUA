ALTER TABLE usuarios
    ADD COLUMN telefono_verificado BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN codigo_whatsapp_hash VARCHAR(255) NULL,
    ADD COLUMN codigo_whatsapp_expira_en DATETIME(6) NULL,
    ADD COLUMN intentos_whatsapp TINYINT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN ultimo_codigo_whatsapp_en DATETIME(6) NULL,
    ADD KEY ix_usuarios_codigo_whatsapp (codigo_whatsapp_expira_en),
    ADD CONSTRAINT ck_usuarios_intentos_whatsapp
        CHECK (intentos_whatsapp <= 5);