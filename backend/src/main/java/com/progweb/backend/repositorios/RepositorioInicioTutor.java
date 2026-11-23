package com.progweb.backend.repositorios;
import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.respuestas.RespuestaInicioTutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class RepositorioInicioTutor {
    private final JdbcTemplate consultas;
    public RepositorioInicioTutor(JdbcTemplate consultas){this.consultas=consultas;}
    private static final String ALUMNOS="""
        FROM asignaciones_tutor a JOIN estudiantes e ON e.id=a.estudiante_id JOIN usuarios u ON u.id=e.usuario_id AND u.estado='activo'
        JOIN situaciones_academicas s ON s.estudiante_id=e.id AND s.periodo_id=a.periodo_id AND s.estado_inscripcion='activo'
        JOIN periodos_academicos p ON p.id=a.periodo_id AND p.estado='activo'
        JOIN carreras c ON c.id=s.carrera_id LEFT JOIN v_ultima_medicion_academica m ON m.situacion_id=s.id
        WHERE a.tutor_id=? AND a.estado='activa'
        """;
    public RespuestaInicioTutor.Indicadores indicadores(long tutor,LocalDateTime ahora){
        long estudiantes=consultas.queryForObject("SELECT COUNT(DISTINCT e.id) "+ALUMNOS,Long.class,tutor);
        long realizadas=consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE tutor_id=? AND estado='completada'",Long.class,tutor);
        long pendientes=consultas.queryForObject("SELECT COUNT(*) FROM reportes WHERE tutor_id=? AND estado='pendiente'",Long.class,tutor);
        long programadas=consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE tutor_id=? AND estado IN ('solicitada','confirmada') AND inicio_en>?",Long.class,tutor,ahora);
        return new RespuestaInicioTutor.Indicadores(estudiantes,realizadas,pendientes,programadas);
    }
    public List<RespuestaInicioTutor.Estudiante> estudiantes(long tutor){
        return consultas.query("SELECT id,nombre,carrera,nivel_riesgo FROM (SELECT e.id,u.nombre,c.nombre carrera,m.nivel_riesgo,ROW_NUMBER() OVER(PARTITION BY e.id ORDER BY p.fecha_inicio DESC,p.id DESC) orden "+ALUMNOS+") alumnos WHERE orden=1 ORDER BY nombre,id LIMIT 5",
            (fila,posicion)->new RespuestaInicioTutor.Estudiante(fila.getLong(1),fila.getString(2),fila.getString(3),fila.getString(4)),tutor);
    }
}
