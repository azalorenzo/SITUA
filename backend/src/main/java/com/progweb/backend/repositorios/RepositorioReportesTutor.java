package com.progweb.backend.repositorios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.progweb.backend.respuestas.RespuestaPuntoRendimiento;
import com.progweb.backend.respuestas.RespuestaProgresoEstudiante;
import com.progweb.backend.respuestas.RespuestaReporte;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Consultas de reportes y métricas del tutor. Todas se acotan por tutores.id, que sale del JWT.
// Las métricas usan el periodo activo del catálogo, no la fecha del equipo.
@Repository
public class RepositorioReportesTutor {
    // Estudiantes con asignación activa de este tutor en el periodo activo.
    private static final String ASIGNADOS = """
        FROM asignaciones_tutor a
        JOIN periodos_academicos p ON p.id = a.periodo_id AND p.estado = 'activo'
        """;
    private static final String MINUTOS = "TIMESTAMPDIFF(MINUTE, COALESCE(s.inicio_real_en, s.inicio_en), COALESCE(s.fin_real_en, s.fin_en))";

    private final JdbcTemplate consultas;

    public RepositorioReportesTutor(JdbcTemplate consultas) { this.consultas = consultas; }

    public Optional<Long> buscarTutorId(long usuarioId) {
        return consultas.queryForList("SELECT id FROM tutores WHERE usuario_id=?", Long.class, usuarioId).stream().findFirst();
    }

    // Mismos datos que usa el PDF de coordinación, pero solo de los reportes de este tutor.
    public List<RespuestaReporte> reportes(long tutorId, int limite) {
        return consultas.query("""
            SELECT r.id, r.estudiante_id, u.nombre AS estudiante, e.matricula, r.tutor_id, ut.nombre AS tutor,
                   r.periodo_id, p.nombre AS periodo, c.nombre AS carrera, s.semestre, r.motivo_id, m.nombre AS motivo,
                   r.fecha, r.estado, r.contenido, r.completado_en, autor.nombre AS completado_por
            FROM reportes r
            JOIN estudiantes e ON e.id = r.estudiante_id JOIN usuarios u ON u.id = e.usuario_id
            JOIN tutores t ON t.id = r.tutor_id JOIN usuarios ut ON ut.id = t.usuario_id
            JOIN periodos_academicos p ON p.id = r.periodo_id JOIN motivos_reporte m ON m.id = r.motivo_id
            JOIN situaciones_academicas s ON s.estudiante_id = r.estudiante_id AND s.periodo_id = r.periodo_id
            JOIN carreras c ON c.id = s.carrera_id LEFT JOIN usuarios autor ON autor.id = r.completado_por
            WHERE r.tutor_id = ?
            ORDER BY r.fecha DESC, r.id DESC LIMIT ?
            """, (fila, posicion) -> new RespuestaReporte(fila.getLong("id"), fila.getLong("estudiante_id"), fila.getString("estudiante"),
                fila.getString("matricula"), fila.getLong("tutor_id"), fila.getString("tutor"), fila.getLong("periodo_id"),
                fila.getString("periodo"), fila.getString("carrera"), fila.getInt("semestre"), fila.getLong("motivo_id"),
                fila.getString("motivo"), fila.getObject("fecha", LocalDate.class), fila.getString("estado"), fila.getString("contenido"),
                fila.getObject("completado_en", LocalDateTime.class), fila.getString("completado_por")), tutorId, limite);
    }

    public Optional<RespuestaReporte> reporte(long tutorId, long reporteId) {
        return reportes(tutorId, 500).stream().filter(r -> r.id() == reporteId).findFirst();
    }

    public long estudiantesAtendidos(long tutorId) {
        Long total = consultas.queryForObject("""
            SELECT COUNT(DISTINCT s.estudiante_id) FROM sesiones s
            JOIN periodos_academicos p ON p.id = s.periodo_id AND p.estado = 'activo'
            WHERE s.tutor_id = ? AND s.estado = 'completada'
            """, Long.class, tutorId);
        return total == null ? 0 : total;
    }

    // Porcentaje de sesiones realizadas frente a las realizadas más las de inasistencia. Nulo si no hay ninguna.
    public Integer asistenciaPorcentaje(long tutorId) {
        return consultas.queryForObject("""
            SELECT CASE WHEN COUNT(*) = 0 THEN NULL
                        ELSE ROUND(100 * SUM(s.estado = 'completada') / COUNT(*)) END
            FROM sesiones s
            JOIN periodos_academicos p ON p.id = s.periodo_id AND p.estado = 'activo'
            WHERE s.tutor_id = ? AND s.estado IN ('completada','no_asistio')
            """, Integer.class, tutorId);
    }

    public BigDecimal horasTutoria(long tutorId) {
        Long minutos = consultas.queryForObject("SELECT COALESCE(SUM(" + MINUTOS + "), 0) FROM sesiones s "
                + "JOIN periodos_academicos p ON p.id = s.periodo_id AND p.estado = 'activo' "
                + "WHERE s.tutor_id = ? AND s.estado = 'completada'", Long.class, tutorId);
        return minutos == null || minutos == 0 ? null : BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    // Promedio de la última medición de cada estudiante asignado.
    public BigDecimal promedioGeneral(long tutorId) {
        return consultas.queryForObject("SELECT ROUND(AVG(x.promedio), 2) " + ASIGNADOS + """
            JOIN v_expediente_actual_por_periodo x ON x.estudiante_id = a.estudiante_id AND x.periodo_id = a.periodo_id
            WHERE a.tutor_id = ? AND a.estado = 'activa' AND x.estado_inscripcion = 'activo' AND x.promedio IS NOT NULL
            """, BigDecimal.class, tutorId);
    }

    public List<RespuestaPuntoRendimiento> rendimiento(long tutorId) {
        return consultas.query("SELECT DATE_FORMAT(m.medido_en, '%Y-%m') AS mes, ROUND(AVG(m.promedio), 2) AS promedio " + ASIGNADOS + """
            JOIN situaciones_academicas sa ON sa.estudiante_id = a.estudiante_id AND sa.periodo_id = a.periodo_id
            JOIN mediciones_academicas m ON m.situacion_id = sa.id
            WHERE a.tutor_id = ? AND a.estado = 'activa' AND m.promedio IS NOT NULL
            GROUP BY DATE_FORMAT(m.medido_en, '%Y-%m') ORDER BY mes
            """, (fila, posicion) -> new RespuestaPuntoRendimiento(fila.getString("mes"), fila.getBigDecimal("promedio")), tutorId);
    }

    public List<RespuestaProgresoEstudiante> progreso(long tutorId, int limite) {
        return consultas.query("""
            SELECT * FROM (
                SELECT u.nombre,
                       (SELECT m.promedio FROM mediciones_academicas m WHERE m.situacion_id = sa.id AND m.promedio IS NOT NULL
                        ORDER BY m.medido_en ASC, m.id ASC LIMIT 1) AS inicial,
                       (SELECT m.promedio FROM mediciones_academicas m WHERE m.situacion_id = sa.id AND m.promedio IS NOT NULL
                        ORDER BY m.medido_en DESC, m.id DESC LIMIT 1) AS actual
                """ + ASIGNADOS + """
                JOIN situaciones_academicas sa ON sa.estudiante_id = a.estudiante_id AND sa.periodo_id = a.periodo_id
                JOIN estudiantes e ON e.id = a.estudiante_id JOIN usuarios u ON u.id = e.usuario_id
                WHERE a.tutor_id = ? AND a.estado = 'activa'
            ) progreso WHERE inicial IS NOT NULL ORDER BY nombre LIMIT ?
            """, (fila, posicion) -> new RespuestaProgresoEstudiante(fila.getString("nombre"), fila.getBigDecimal("inicial"),
                fila.getBigDecimal("actual")), tutorId, limite);
    }
}
