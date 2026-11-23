package com.progweb.backend.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.progweb.backend.respuestas.RespuestaEstudianteInicio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Cifras del inicio del tutor. Todo se acota por tutores.id, que sale del JWT, y por el periodo activo del catálogo.
@Repository
public class RepositorioInicioTutor {
    private final JdbcTemplate consultas;

    public RepositorioInicioTutor(JdbcTemplate consultas) { this.consultas = consultas; }

    public Optional<Long> buscarTutorId(long usuarioId) {
        return consultas.queryForList("SELECT id FROM tutores WHERE usuario_id=?", Long.class, usuarioId).stream().findFirst();
    }

    private long contar(String sql, Object... datos) {
        Long total = consultas.queryForObject(sql, Long.class, datos);
        return total == null ? 0 : total;
    }

    public long estudiantes(long tutorId) {
        return contar("""
            SELECT COUNT(*) FROM asignaciones_tutor a
            JOIN periodos_academicos p ON p.id = a.periodo_id AND p.estado = 'activo'
            JOIN estudiantes e ON e.id = a.estudiante_id JOIN usuarios u ON u.id = e.usuario_id AND u.estado = 'activo'
            WHERE a.tutor_id = ? AND a.estado = 'activa'
            """, tutorId);
    }

    public long sesionesRealizadas(long tutorId) {
        return contar("""
            SELECT COUNT(*) FROM sesiones s JOIN periodos_academicos p ON p.id = s.periodo_id AND p.estado = 'activo'
            WHERE s.tutor_id = ? AND s.estado = 'completada'
            """, tutorId);
    }

    public long reportesPendientes(long tutorId) {
        return contar("""
            SELECT COUNT(*) FROM reportes r JOIN periodos_academicos p ON p.id = r.periodo_id AND p.estado = 'activo'
            WHERE r.tutor_id = ? AND r.estado = 'pendiente'
            """, tutorId);
    }

    // Sesiones confirmadas que todavía no ocurren.
    public long sesionesProgramadas(long tutorId, LocalDateTime ahora) {
        return contar("SELECT COUNT(*) FROM sesiones WHERE tutor_id = ? AND estado = 'confirmada' AND inicio_en >= ?", tutorId, ahora);
    }

    public List<RespuestaEstudianteInicio> estudiantesDestacados(long tutorId, int limite) {
        return consultas.query("""
            SELECT a.estudiante_id, x.nombre, x.carrera, x.nivel_riesgo
            FROM asignaciones_tutor a
            JOIN periodos_academicos p ON p.id = a.periodo_id AND p.estado = 'activo'
            JOIN v_expediente_actual_por_periodo x ON x.estudiante_id = a.estudiante_id AND x.periodo_id = a.periodo_id
            JOIN estudiantes e ON e.id = a.estudiante_id JOIN usuarios u ON u.id = e.usuario_id AND u.estado = 'activo'
            WHERE a.tutor_id = ? AND a.estado = 'activa' AND x.estado_inscripcion = 'activo'
            ORDER BY CASE x.nivel_riesgo WHEN 'alto' THEN 0 WHEN 'medio' THEN 1 WHEN 'bajo' THEN 2 WHEN 'sin_riesgo' THEN 3 ELSE 4 END,
                     x.nombre, a.estudiante_id
            LIMIT ?
            """, (fila, posicion) -> new RespuestaEstudianteInicio(fila.getLong("estudiante_id"), fila.getString("nombre"),
                fila.getString("carrera"), fila.getString("nivel_riesgo")), tutorId, limite);
    }
}
