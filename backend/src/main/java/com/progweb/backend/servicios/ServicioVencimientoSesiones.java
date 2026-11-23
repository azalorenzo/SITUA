package com.progweb.backend.servicios;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioVencimientoSesiones {
    private final JdbcTemplate consultas;
    public ServicioVencimientoSesiones(JdbcTemplate consultas) { this.consultas = consultas; }

    @Transactional
    public int vencerPendientes(LocalDateTime ahora) {
        return vencer("SELECT id FROM sesiones WHERE estado='solicitada' AND inicio_en<=? ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED", ahora);
    }

    @Transactional
    public int vencerPropias(long perfil, boolean esTutor, LocalDateTime ahora) {
        return vencer("SELECT id FROM sesiones WHERE estado='solicitada' AND inicio_en<=? AND "
                + (esTutor ? "tutor_id" : "estudiante_id") + "=? ORDER BY id FOR UPDATE", ahora, perfil);
    }

    private int vencer(String seleccion, Object... parametros) {
        var pendientes = consultas.queryForList(seleccion, Long.class, parametros);
        for (long id : pendientes) {
            consultas.update("UPDATE sesiones SET estado='vencida' WHERE id=? AND estado='solicitada'", id);
            consultas.update("""
                INSERT INTO sesion_cambios(sesion_id,actor_id,tipo,estado_anterior,estado_nuevo,
                    inicio_anterior,fin_anterior,inicio_nuevo,fin_nuevo,ocurrido_en)
                SELECT id,NULL,'vencimiento','solicitada','vencida',inicio_en,fin_en,inicio_en,fin_en,inicio_en
                FROM sesiones WHERE id=?
                """, id);
        }
        return pendientes.size();
    }
}
