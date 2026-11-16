package com.progweb.backend.repositorios;

import java.util.List;
import java.util.Optional;
import com.progweb.backend.respuestas.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;

@Repository
public class RepositorioAsignaciones {
    private final JdbcTemplate consultas;
    private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate consultasPaginadas;
    private static final String FILTROS = """
            WHERE (:periodoId IS NULL OR a.periodo_id=:periodoId)
              AND (:estado IS NULL OR a.estado=:estado)
              AND (:busqueda='' OR CONCAT_WS(' ',u.nombre,e.matricula,ut.nombre) LIKE :patron ESCAPE '!')
            """;
    private static final String CONSULTA = """
            SELECT a.*, u.nombre AS estudiante, e.matricula, ut.nombre AS tutor, p.nombre AS periodo,
              (SELECT COUNT(*) FROM sesiones s WHERE s.estudiante_id=a.estudiante_id AND s.tutor_id=a.tutor_id
                AND s.periodo_id=a.periodo_id AND s.estado='completada' AND s.inicio_en>=a.inicio_en
                AND (a.fin_en IS NULL OR s.inicio_en<a.fin_en)) AS sesiones_completadas,
              (SELECT MAX(s.inicio_en) FROM sesiones s WHERE s.estudiante_id=a.estudiante_id AND s.tutor_id=a.tutor_id
                AND s.periodo_id=a.periodo_id AND s.estado='completada' AND s.inicio_en>=a.inicio_en
                AND (a.fin_en IS NULL OR s.inicio_en<a.fin_en)) AS ultima_sesion
            FROM asignaciones_tutor a JOIN estudiantes e ON e.id=a.estudiante_id
            JOIN usuarios u ON u.id=e.usuario_id JOIN tutores t ON t.id=a.tutor_id
            JOIN usuarios ut ON ut.id=t.usuario_id JOIN periodos_academicos p ON p.id=a.periodo_id
            """;
    private final RowMapper<RespuestaAsignacion> convertir = (fila, posicion) -> new RespuestaAsignacion(
            fila.getLong("id"), fila.getLong("estudiante_id"), fila.getString("estudiante"), fila.getString("matricula"),
            fila.getLong("tutor_id"), fila.getString("tutor"), fila.getLong("periodo_id"), fila.getString("periodo"),
            fila.getLong("asignado_por"), fila.getObject("inicio_en", java.time.LocalDateTime.class),
            fila.getObject("fin_en", java.time.LocalDateTime.class), fila.getString("estado"), fila.getString("motivo"),
            fila.getLong("sesiones_completadas"), fila.getObject("ultima_sesion", java.time.LocalDateTime.class));

    public RepositorioAsignaciones(JdbcTemplate consultas) {
        this.consultas = consultas;
        this.consultasPaginadas = new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(consultas);
    }

    private org.springframework.jdbc.core.namedparam.MapSqlParameterSource parametros(Long periodoId, String estado, String busqueda) {
        String patron = "%" + busqueda.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("periodoId", periodoId, java.sql.Types.BIGINT)
            .addValue("estado", estado, java.sql.Types.VARCHAR)
            .addValue("busqueda", busqueda).addValue("patron", patron);
    }

    public long contar(Long periodoId, String estado, String busqueda) {
        return consultasPaginadas.queryForObject("""
            SELECT COUNT(*) FROM asignaciones_tutor a
            JOIN estudiantes e ON e.id=a.estudiante_id JOIN usuarios u ON u.id=e.usuario_id
            JOIN tutores t ON t.id=a.tutor_id JOIN usuarios ut ON ut.id=t.usuario_id
            """ + FILTROS, parametros(periodoId, estado, busqueda), Long.class);
    }

    public ResumenAsignaciones resumen(Long periodoId) {
        return consultasPaginadas.queryForObject("""
            SELECT COUNT(*) AS total, COALESCE(SUM(estado='activa'),0) AS activas,
                COALESCE(SUM(estado='pendiente'),0) AS pendientes
            FROM asignaciones_tutor WHERE (:periodoId IS NULL OR periodo_id=:periodoId)
            """, parametros(periodoId, null, ""), (fila, posicion) -> new ResumenAsignaciones(
                fila.getLong("total"), fila.getLong("activas"), fila.getLong("pendientes")));
    }

    public List<RespuestaAsignacion> listarPagina(Long periodoId, String estado, String busqueda, int tamano, long desplazamiento) {
        var valores = parametros(periodoId, estado, busqueda).addValue("tamano", tamano).addValue("desplazamiento", desplazamiento);
        return consultasPaginadas.query(CONSULTA + FILTROS + " ORDER BY a.inicio_en DESC, a.id DESC LIMIT :tamano OFFSET :desplazamiento", valores, convertir);
    }

    public List<RespuestaAsignacion> exportar(Long periodoId, String estado, String busqueda) {
        return consultasPaginadas.query(CONSULTA + FILTROS + " ORDER BY a.inicio_en DESC, a.id DESC", parametros(periodoId, estado, busqueda), convertir);
    }

    public Optional<RespuestaAsignacion> buscar(long id) {
        return consultas.query(CONSULTA + " WHERE a.id=?", convertir, id).stream().findFirst();
    }

    public List<RespuestaPeriodo> periodos() {
        return consultas.query("SELECT * FROM periodos_academicos ORDER BY fecha_inicio DESC, id DESC",
            (fila, posicion) -> new RespuestaPeriodo(fila.getLong("id"), fila.getString("nombre"),
                fila.getObject("fecha_inicio", java.time.LocalDate.class), fila.getObject("fecha_fin", java.time.LocalDate.class), fila.getString("estado")));
    }

    public List<RespuestaEstudianteAsignable> estudiantes(long periodoId) {
        return consultas.query("""
            SELECT e.id, u.nombre, e.matricula, c.nombre AS carrera, a.id AS asignacion_id
            FROM situaciones_academicas s JOIN estudiantes e ON e.id=s.estudiante_id
            JOIN usuarios u ON u.id=e.usuario_id JOIN carreras c ON c.id=s.carrera_id
            LEFT JOIN asignaciones_tutor a ON a.estudiante_id=e.id AND a.periodo_id=s.periodo_id AND a.estado='activa'
            WHERE s.periodo_id=? AND s.estado_inscripcion='activo' AND u.estado='activo'
            ORDER BY u.nombre, e.id
            """, (fila, posicion) -> new RespuestaEstudianteAsignable(fila.getLong("id"), fila.getString("nombre"),
                fila.getString("matricula"), fila.getString("carrera"), fila.getObject("asignacion_id", Long.class)), periodoId);
    }

    public boolean bloquearEstudiante(long id) {
        return !consultas.query("SELECT id FROM estudiantes WHERE id=? FOR UPDATE", (fila, posicion) -> fila.getLong(1), id).isEmpty();
    }

    public boolean cuentaActiva(long estudianteId) {
        return consultas.queryForObject("SELECT COUNT(*) FROM estudiantes e JOIN usuarios u ON u.id=e.usuario_id WHERE e.id=? AND u.estado='activo'", Long.class, estudianteId) == 1;
    }

    public boolean tutorActivo(long tutorId) {
        return consultas.queryForObject("SELECT COUNT(*) FROM tutores t JOIN usuarios u ON u.id=t.usuario_id WHERE t.id=? AND u.estado='activo'", Long.class, tutorId) == 1;
    }

    public boolean periodoActivo(long periodoId) {
        return consultas.queryForObject("SELECT COUNT(*) FROM periodos_academicos WHERE id=? AND estado='activo'", Long.class, periodoId) == 1;
    }

    public boolean tieneExpediente(long estudianteId, long periodoId) {
        return consultas.queryForObject("SELECT COUNT(*) FROM situaciones_academicas WHERE estudiante_id=? AND periodo_id=? AND estado_inscripcion='activo'", Long.class, estudianteId, periodoId) == 1;
    }

    public boolean tieneAsignacion(long estudianteId, long periodoId) {
        return consultas.queryForObject("SELECT COUNT(*) FROM asignaciones_tutor WHERE estudiante_id=? AND periodo_id=? AND estado='activa'", Long.class, estudianteId, periodoId) > 0;
    }

    public long crear(long estudianteId, long tutorId, long periodoId, long autor, String motivo) {
        var llave = new GeneratedKeyHolder();
        consultas.update(conexion -> {
            var sentencia = conexion.prepareStatement("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado,motivo) VALUES (?,?,?,?,UTC_TIMESTAMP(6),'activa',?)", Statement.RETURN_GENERATED_KEYS);
            sentencia.setLong(1, estudianteId); sentencia.setLong(2, tutorId); sentencia.setLong(3, periodoId);
            sentencia.setLong(4, autor); sentencia.setString(5, motivo);
            return sentencia;
        }, llave);
        return llave.getKey().longValue();
    }

    public boolean cerrar(long id, String estado) {
        return consultas.update("UPDATE asignaciones_tutor SET estado=?, fin_en=UTC_TIMESTAMP(6) WHERE id=? AND estado IN ('activa','pendiente') AND inicio_en<=UTC_TIMESTAMP(6)", estado, id) == 1;
    }
}
