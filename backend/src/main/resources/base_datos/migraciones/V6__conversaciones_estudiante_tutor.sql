ALTER TABLE conversaciones
    ADD usuario_menor_id BIGINT UNSIGNED NULL,
    ADD usuario_mayor_id BIGINT UNSIGNED NULL,
    ADD UNIQUE KEY uq_conversacion_personas (usuario_menor_id,usuario_mayor_id),
    ADD CONSTRAINT fk_conversacion_menor FOREIGN KEY (usuario_menor_id) REFERENCES usuarios(id),
    ADD CONSTRAINT fk_conversacion_mayor FOREIGN KEY (usuario_mayor_id) REFERENCES usuarios(id),
    ADD CONSTRAINT ck_conversacion_personas CHECK (
        (usuario_menor_id IS NULL AND usuario_mayor_id IS NULL)
        OR (usuario_menor_id IS NOT NULL AND usuario_mayor_id IS NOT NULL AND usuario_menor_id<usuario_mayor_id));

UPDATE conversaciones c JOIN (
    SELECT MIN(conversacion_id) id,menor,mayor FROM (
        SELECT conversacion_id,MIN(usuario_id) menor,MAX(usuario_id) mayor
        FROM conversacion_participantes GROUP BY conversacion_id HAVING COUNT(*)=2
    ) participantes GROUP BY menor,mayor
) pares ON pares.id=c.id
SET c.usuario_menor_id=pares.menor,c.usuario_mayor_id=pares.mayor;

ALTER TABLE conversacion_participantes ADD leido_hasta_id BIGINT UNSIGNED NULL;
