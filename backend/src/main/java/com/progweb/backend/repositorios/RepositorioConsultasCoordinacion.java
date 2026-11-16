package com.progweb.backend.repositorios;

import java.util.List;
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

    public List<RespuestaEstudianteCoordinacion> consultarEstudiantes() {
        // Tomamos el periodo más reciente de cada estudiante, aunque ya esté cerrado.
        // Los recién registrados aparecen también, aun sin información académica.
        return consultas.query("""
                SELECT e.id, u.id AS usuario_id, u.nombre, u.correo, u.telefono,
                       u.avatar_url, u.estado, e.matricula, p.id AS periodo_id, p.nombre AS periodo,
                       c.nombre AS carrera, s.semestre, m.promedio, m.nivel_riesgo
                FROM estudiantes e
                JOIN usuarios u ON u.id = e.usuario_id
                LEFT JOIN situaciones_academicas s ON s.id = (
                    SELECT situacion.id FROM situaciones_academicas situacion
                    JOIN periodos_academicos periodo ON periodo.id = situacion.periodo_id
                    WHERE situacion.estudiante_id = e.id
                    ORDER BY periodo.fecha_inicio DESC, periodo.id DESC, situacion.id DESC LIMIT 1
                )
                LEFT JOIN periodos_academicos p ON p.id = s.periodo_id
                LEFT JOIN carreras c ON c.id = s.carrera_id
                LEFT JOIN v_ultima_medicion_academica m ON m.situacion_id = s.id
                ORDER BY u.nombre, e.id
                """, (fila, posicion) -> new RespuestaEstudianteCoordinacion(
                fila.getLong("id"), fila.getLong("usuario_id"), fila.getString("nombre"),
                fila.getString("correo"), fila.getString("telefono"), fila.getString("avatar_url"),
                fila.getString("estado"), fila.getString("matricula"), fila.getObject("periodo_id", Long.class),
                fila.getString("periodo"), fila.getString("carrera"), fila.getObject("semestre", Integer.class),
                fila.getBigDecimal("promedio"), fila.getString("nivel_riesgo")));
    }
}
