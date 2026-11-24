package com.progweb.backend.repositorios;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.progweb.backend.respuestas.RespuestaHorario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

// Consultas propias del módulo de horarios del tutor. Solo lee y escribe
// disponibilidades_tutor y consulta sesiones y tutores de forma puntual;
// no modifica RepositorioTutores ni RepositorioAsignaciones.
@Repository
public class RepositorioDisponibilidad {
    private final JdbcTemplate consultas;

    private static final String CONSULTA = """
            SELECT d.id, d.inicio_en, d.fin_en, d.modalidad, d.ubicacion, d.enlace_virtual, d.estado,
              u.nombre AS estudiante
            FROM disponibilidades_tutor d
            LEFT JOIN sesiones s ON s.disponibilidad_id = d.id AND s.estado IN ('solicitada','confirmada')
            LEFT JOIN estudiantes e ON e.id = s.estudiante_id
            LEFT JOIN usuarios u ON u.id = e.usuario_id
            """;

    private final RowMapper<RespuestaHorario> convertir = (fila, posicion) -> new RespuestaHorario(
            fila.getLong("id"), fila.getObject("inicio_en", LocalDateTime.class),
            fila.getObject("fin_en", LocalDateTime.class), fila.getString("modalidad"),
            fila.getString("ubicacion"), fila.getString("enlace_virtual"), fila.getString("estado"),
            fila.getString("estudiante"));

    public RepositorioDisponibilidad(JdbcTemplate consultas) {
        this.consultas = consultas;
    }

    // Obtiene el tutor_id propio a partir del usuario_id que viene del JWT.
    public Optional<Long> obtenerTutorIdPorUsuario(long usuarioId) {
        return consultas.query("SELECT id FROM tutores WHERE usuario_id = ?",
                (fila, posicion) -> fila.getLong("id"), usuarioId).stream().findFirst();
    }

    public List<RespuestaHorario> listarPorTutor(long tutorId) {
        return consultas.query(CONSULTA + " WHERE d.tutor_id = ? ORDER BY d.inicio_en", convertir, tutorId);
    }

    private static final String INTERVALO=" WHERE d.tutor_id=? AND (? IS NULL OR d.fin_en>?) AND (? IS NULL OR d.inicio_en<?)";
    public List<RespuestaHorario> listarPorTutor(long tutor,LocalDateTime desde,LocalDateTime hasta){
        return consultas.query(CONSULTA+INTERVALO+" ORDER BY d.inicio_en,d.id",convertir,tutor,desde,desde,hasta,hasta);
    }
    public com.progweb.backend.respuestas.ResumenHorarios resumen(long tutor,LocalDateTime desde,LocalDateTime hasta){
        return consultas.queryForObject("""
            SELECT COUNT(*) total,
                COALESCE(SUM(estado='habilitada' AND estudiante IS NULL),0) disponibles,
                COALESCE(SUM(estudiante IS NOT NULL),0) ocupados,
                COALESCE(SUM(estado='bloqueada'),0) bloqueados
            FROM (
            """+CONSULTA+INTERVALO+") horarios",(fila,posicion)->new com.progweb.backend.respuestas.ResumenHorarios(fila.getLong("total"),fila.getLong("disponibles"),fila.getLong("ocupados"),fila.getLong("bloqueados")),tutor,desde,desde,hasta,hasta);
    }

    public Optional<RespuestaHorario> buscar(long id) {
        return consultas.query(CONSULTA + " WHERE d.id = ?", convertir, id).stream().findFirst();
    }

    public boolean perteneceATutor(long id, long tutorId) {
        return !consultas.query("SELECT id FROM disponibilidades_tutor WHERE id=? AND tutor_id=? FOR UPDATE",
                (fila,posicion)->fila.getLong(1),id,tutorId).isEmpty();
    }

    // Evita que un mismo tutor registre dos horarios que se encimen. excluirId se usa al
    // editar, para no comparar el horario consigo mismo.
    public boolean seCruzaConOtro(long tutorId, LocalDateTime inicioEn, LocalDateTime finEn, Long excluirId) {
        return !consultas.query("""
                SELECT id FROM disponibilidades_tutor
                WHERE tutor_id=? AND (? IS NULL OR id<>?) AND inicio_en<? AND fin_en>? FOR UPDATE
                """,(fila,posicion)->fila.getLong(1),tutorId,excluirId,excluirId,finEn,inicioEn).isEmpty();
    }

    // Una vez que un horario tiene alguna sesión (incluso pasada), ya no se edita ni se
    // elimina: se conserva como parte del historial de esa sesión.
    public boolean tieneSesionAsociada(long id) {
        return !consultas.query("SELECT id FROM sesiones WHERE disponibilidad_id=? FOR SHARE",(fila,posicion)->fila.getLong(1),id).isEmpty();
    }

    public void bloquearTutor(long tutorId) {
        consultas.query("SELECT id FROM tutores WHERE id=? FOR UPDATE",(fila,posicion)->fila.getLong(1),tutorId);
    }

    public long crear(long tutorId, LocalDateTime inicioEn, LocalDateTime finEn, String modalidad,
            String ubicacion, String enlaceVirtual) {
        var llave = new GeneratedKeyHolder();
        consultas.update(conexion -> {
            var sentencia = conexion.prepareStatement("""
                    INSERT INTO disponibilidades_tutor(tutor_id, inicio_en, fin_en, modalidad, ubicacion, enlace_virtual)
                    VALUES (?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            sentencia.setLong(1, tutorId);
            sentencia.setObject(2, inicioEn);
            sentencia.setObject(3, finEn);
            sentencia.setString(4, modalidad);
            sentencia.setString(5, ubicacion);
            sentencia.setString(6, enlaceVirtual);
            return sentencia;
        }, llave);
        return llave.getKey().longValue();
    }

    public void actualizar(long id, LocalDateTime inicioEn, LocalDateTime finEn, String modalidad,
            String ubicacion, String enlaceVirtual) {
        consultas.update("""
                UPDATE disponibilidades_tutor
                SET inicio_en = ?, fin_en = ?, modalidad = ?, ubicacion = ?, enlace_virtual = ?
                WHERE id = ?
                """, inicioEn, finEn, modalidad, ubicacion, enlaceVirtual, id);
    }

    public void eliminar(long id) {
        consultas.update("DELETE FROM disponibilidades_tutor WHERE id = ?", id);
    }
}
