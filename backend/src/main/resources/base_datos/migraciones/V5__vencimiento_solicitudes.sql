ALTER TABLE sesiones
    MODIFY estado ENUM('solicitada','confirmada','rechazada','cancelada','completada','no_asistio','vencida') NOT NULL DEFAULT 'solicitada',
    ADD INDEX ix_sesion_vencimiento (estado, inicio_en, id);

ALTER TABLE sesion_cambios
    MODIFY actor_id BIGINT UNSIGNED NULL,
    MODIFY tipo ENUM('solicitud','aceptacion','rechazo','reprogramacion','cancelacion','cierre','registro','vencimiento') NOT NULL,
    MODIFY estado_anterior ENUM('solicitada','confirmada','rechazada','cancelada','completada','no_asistio','vencida') NULL,
    MODIFY estado_nuevo ENUM('solicitada','confirmada','rechazada','cancelada','completada','no_asistio','vencida') NOT NULL;
