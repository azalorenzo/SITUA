package com.progweb.backend.repositorios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.progweb.backend.respuestas.RespuestaEstudianteTutor;
import com.progweb.backend.respuestas.RespuestaObservacionEstudiante;
import com.progweb.backend.respuestas.RespuestaSesionEstudiante;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

// Consultas de los estudiantes que tiene asignados un tutor. Todas se acotan por tutores.id, que sale del JWT.
// Solo cuentan las asignaciones activas del periodo activo, tomado del catálogo y no de la fecha del equipo.
@Repository
public class RepositorioEstudiantesTutor {
    private static final String SELECCION = """
        SELECT a.estudiante_id, x.nombre, x.correo, x.telefono, x.matricula, x.carrera, x.semestre,
               x.promedio, x.nivel_riesgo, x.condicion, p.nombre AS periodo,
               COALESCE(r.sesiones_completadas, 0) AS sesiones_completadas, r.ultima_sesion
        FROM asignaciones_tutor a
        JOIN periodos_academicos p ON p.id = a.periodo_id AND p.estado = 'activo'
        JOIN v_expediente_actual_por_periodo x ON x.estudiante_id = a.estudiante_id AND x.periodo_id = a.periodo_id
        JOIN estudiantes e ON e.id = a.estudiante_id
        JOIN usuarios u ON u.id = e.usuario_id AND u.estado = 'activo'
        LEFT JOIN v_resumen_sesiones r ON r.estudiante_id = a.estudiante_id AND r.tutor_id = a.tutor_id AND r.periodo_id = a.periodo_id
        WHERE a.tutor_id = ? AND a.estado = 'activa' AND x.estado_inscripcion = 'activo'
        """;

    private final JdbcTemplate consultas;

    private final RowMapper<RespuestaEstudianteTutor> convertir = (fila, posicion) -> new RespuestaEstudianteTutor(
            fila.getLong("estudiante_id"), fila.getString("nombre"), fila.getString("correo"), fila.getString("telefono"),
            fila.getString("matricula"), fila.getString("carrera"), fila.getObject("semestre", Integer.class),
            fila.getObject("promedio", BigDecimal.class), fila.getString("nivel_riesgo"), fila.getString("condicion"),
            fila.getString("periodo"), fila.getLong("sesiones_completadas"),
            fila.getObject("ultima_sesion", LocalDateTime.class));

    public RepositorioEstudiantesTutor(JdbcTemplate consultas) { this.consultas = consultas; }

    public Optional<Long> buscarTutorId(long usuarioId) {
        return consultas.queryForList("SELECT id FROM tutores WHERE usuario_id=?", Long.class, usuarioId).stream().findFirst();
    }

    public List<RespuestaEstudianteTutor> listar(long tutorId) {
        return consultas.query(SELECCION + " ORDER BY x.nombre, a.estudiante_id", convertir, tutorId);
    }

    public Optional<RespuestaEstudianteTutor> buscar(long tutorId, long estudianteId) {
        return consultas.query(SELECCION + " AND a.estudiante_id = ?", convertir, tutorId, estudianteId).stream().findFirst();
    }

    // Se usa el periodo de la asignación activa para no mezclar sesiones de periodos anteriores.
    public List<RespuestaSesionEstudiante> sesionesCompletadas(long tutorId, long estudianteId, int limite) {
        return consultas.query("""
            SELECT s.id, s.tema, s.inicio_en, s.observaciones,
                   TIMESTAMPDIFF(MINUTE, COALESCE(s.inicio_real_en, s.inicio_en), COALESCE(s.fin_real_en, s.fin_en)) AS minutos
            FROM sesiones s
            JOIN asignaciones_tutor a ON a.estudiante_id = s.estudiante_id AND a.tutor_id = s.tutor_id
                 AND a.periodo_id = s.periodo_id AND a.estado = 'activa'
            JOIN periodos_academicos p ON p.id = s.periodo_id AND p.estado = 'activo'
            WHERE s.tutor_id = ? AND s.estudiante_id = ? AND s.estado = 'completada'
            ORDER BY s.inicio_en DESC, s.id DESC LIMIT ?
            """, (fila, posicion) -> new RespuestaSesionEstudiante(fila.getLong("id"), fila.getString("tema"),
                fila.getObject("inicio_en", LocalDateTime.class), fila.getLong("minutos"), fila.getString("observaciones")),
            tutorId, estudianteId, limite);
    }

    // Solo las observaciones escritas por esta cuenta de tutor, no las de otras personas.
    public List<RespuestaObservacionEstudiante> observaciones(long usuarioId, long estudianteId, int limite) {
        return consultas.query("""
            SELECT o.id, o.tipo, o.prioridad, o.contenido, o.observado_en
            FROM observaciones_estudiante o
            JOIN periodos_academicos p ON p.id = o.periodo_id AND p.estado = 'activo'
            WHERE o.autor_id = ? AND o.estudiante_id = ?
            ORDER BY o.observado_en DESC, o.id DESC LIMIT ?
            """, (fila, posicion) -> new RespuestaObservacionEstudiante(fila.getLong("id"), fila.getString("tipo"),
                fila.getString("prioridad"), fila.getString("contenido"), fila.getObject("observado_en", LocalDateTime.class)),
            usuarioId, estudianteId, limite);
    }
}
