package com.progweb.backend.repositorios;

import java.util.List;
import com.progweb.backend.respuestas.PaginaEstudiantes;
import com.progweb.backend.solicitudes.FiltrosEstudiantes;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.progweb.backend.respuestas.RespuestaTutorCoordinacion;
import com.progweb.backend.respuestas.RespuestaEstudianteCoordinacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioConsultasCoordinacion {
    private final JdbcTemplate consultas;

    public RepositorioConsultasCoordinacion(JdbcTemplate consultas) {
        this.consultas = consultas;
    }

    public List<RespuestaTutorCoordinacion> consultarTutores() {
        return consultas.query("""
                SELECT t.id, u.id AS usuario_id, u.nombre, u.correo, u.telefono,
                       u.avatar_url, u.estado, d.id AS departamento_id, d.nombre AS departamento
                FROM tutores t
                JOIN usuarios u ON u.id = t.usuario_id
                JOIN departamentos d ON d.id = t.departamento_id
                ORDER BY u.nombre, t.id
                """, (fila, posicion) -> new RespuestaTutorCoordinacion(
                fila.getLong("id"), fila.getLong("usuario_id"), fila.getString("nombre"),
                fila.getString("correo"), fila.getString("telefono"), fila.getString("avatar_url"),
                fila.getString("estado"), fila.getLong("departamento_id"), fila.getString("departamento")));
    }

    // Conservamos el periodo más reciente, incluso si está cerrado o falta su evaluación.
    private static final String BASE_ESTUDIANTES = """
        FROM estudiantes e JOIN usuarios u ON u.id=e.usuario_id
        LEFT JOIN situaciones_academicas s ON s.id=(
            SELECT situacion.id FROM situaciones_academicas situacion
            JOIN periodos_academicos periodo ON periodo.id=situacion.periodo_id
            WHERE situacion.estudiante_id=e.id
            ORDER BY periodo.fecha_inicio DESC,periodo.id DESC,situacion.id DESC LIMIT 1
        )
        LEFT JOIN periodos_academicos p ON p.id=s.periodo_id
        LEFT JOIN carreras c ON c.id=s.carrera_id
        LEFT JOIN v_ultima_medicion_academica m ON m.situacion_id=s.id
        """;
    private static final String FILTROS_ESTUDIANTES = """
        WHERE (:estado IS NULL OR u.estado=:estado)
        AND (:busqueda='' OR CONCAT_WS(' ',u.nombre,u.correo,e.matricula,c.nombre) LIKE :patron ESCAPE '!')
        """;
    private final RowMapper<RespuestaEstudianteCoordinacion> convertirEstudiante=(fila,posicion)->new RespuestaEstudianteCoordinacion(
        fila.getLong("id"),fila.getLong("usuario_id"),fila.getString("nombre"),fila.getString("correo"),
        fila.getString("telefono"),fila.getString("avatar_url"),fila.getString("estado"),fila.getString("matricula"),
        fila.getObject("periodo_id",Long.class),fila.getString("periodo"),fila.getString("carrera"),
        fila.getObject("semestre",Integer.class),fila.getBigDecimal("promedio"),fila.getString("nivel_riesgo"));

    private MapSqlParameterSource parametros(FiltrosEstudiantes filtros) {
        String texto=filtros.texto();
        String patron="%"+texto.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return new MapSqlParameterSource().addValue("estado",filtros.estadoCuenta(),java.sql.Types.VARCHAR)
            .addValue("busqueda",texto).addValue("patron",patron);
    }
    public long contarEstudiantes(FiltrosEstudiantes filtros) {
        return new NamedParameterJdbcTemplate(consultas).queryForObject("SELECT COUNT(*) "+BASE_ESTUDIANTES+FILTROS_ESTUDIANTES,parametros(filtros),Long.class);
    }
    public List<RespuestaEstudianteCoordinacion> consultarEstudiantes(FiltrosEstudiantes filtros,int pagina) {
        return new NamedParameterJdbcTemplate(consultas).query("""
            SELECT e.id,u.id usuario_id,u.nombre,u.correo,u.telefono,u.avatar_url,u.estado,e.matricula,
                p.id periodo_id,p.nombre periodo,c.nombre carrera,s.semestre,m.promedio,m.nivel_riesgo
            """+BASE_ESTUDIANTES+FILTROS_ESTUDIANTES+" ORDER BY u.nombre,e.id LIMIT :tamano OFFSET :desplazamiento",
            parametros(filtros).addValue("tamano",filtros.tamanoActual()).addValue("desplazamiento",(long)(pagina-1)*filtros.tamanoActual()),convertirEstudiante);
    }
    public PaginaEstudiantes.Resumen resumenEstudiantes() {
        return consultas.queryForObject("""
            SELECT COUNT(*) total,COALESCE(SUM(u.estado='activo'),0) activos,ROUND(AVG(m.promedio),2) promedio,
                COUNT(m.promedio) con_promedio,COALESCE(SUM(m.nivel_riesgo IN ('medio','alto')),0) en_riesgo,
                COALESCE(SUM(m.nivel_riesgo IS NULL),0) sin_evaluacion,COALESCE(SUM(s.semestre IS NULL),0) sin_semestre
            """+BASE_ESTUDIANTES,(fila,posicion)->new PaginaEstudiantes.Resumen(fila.getLong("total"),fila.getLong("activos"),
                fila.getBigDecimal("promedio"),fila.getLong("con_promedio"),fila.getLong("en_riesgo"),fila.getLong("sin_evaluacion"),fila.getLong("sin_semestre")));
    }
    public List<PaginaEstudiantes.NivelRiesgo> riesgoEstudiantes() {
        return consultas.query("SELECT m.nivel_riesgo,COUNT(*) cantidad "+BASE_ESTUDIANTES+
            " GROUP BY m.nivel_riesgo ORDER BY FIELD(COALESCE(m.nivel_riesgo,'sin_evaluacion'),'sin_riesgo','bajo','medio','alto','sin_evaluacion')",
            (fila,posicion)->new PaginaEstudiantes.NivelRiesgo(fila.getString("nivel_riesgo"),fila.getLong("cantidad")));
    }
    public List<PaginaEstudiantes.Semestre> semestresEstudiantes() {
        return consultas.query("SELECT s.semestre,COUNT(*) cantidad "+BASE_ESTUDIANTES+
            " WHERE s.semestre IS NOT NULL GROUP BY s.semestre ORDER BY s.semestre",
            (fila,posicion)->new PaginaEstudiantes.Semestre(fila.getInt("semestre")+"°",fila.getLong("cantidad")));
    }
    public List<PaginaEstudiantes.EstudianteEnRiesgo> estudiantesEnRiesgo() {
        return consultas.query("SELECT e.id,u.nombre,e.matricula,p.nombre periodo,m.nivel_riesgo "+BASE_ESTUDIANTES+
            " WHERE m.nivel_riesgo IN ('medio','alto') ORDER BY u.nombre,e.id",
            (fila,posicion)->new PaginaEstudiantes.EstudianteEnRiesgo(fila.getLong("id"),fila.getString("nombre"),fila.getString("matricula"),fila.getString("periodo"),fila.getString("nivel_riesgo")));
    }
}
