package com.progweb.backend.repositorios;
import java.util.List;
import com.progweb.backend.respuestas.RespuestaEleccionTutor.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class RepositorioEleccionTutor {
    private final JdbcTemplate consultas;
    public RepositorioEleccionTutor(JdbcTemplate consultas) { this.consultas=consultas; }
    public List<Periodo> periodos(long estudiante) {
        return consultas.query("""
            SELECT p.id,p.nombre FROM situaciones_academicas s JOIN periodos_academicos p ON p.id=s.periodo_id
            WHERE s.estudiante_id=? AND s.estado_inscripcion='activo' AND p.estado='activo'
            ORDER BY p.fecha_inicio DESC,p.id DESC
            """,(fila,posicion)->new Periodo(fila.getLong(1),fila.getString(2)),estudiante);
    }
    public Tutor asignado(long estudiante,long periodo) {
        return consultas.query("""
            SELECT t.id,u.nombre,d.nombre FROM asignaciones_tutor a
            JOIN tutores t ON t.id=a.tutor_id JOIN usuarios u ON u.id=t.usuario_id
            JOIN departamentos d ON d.id=t.departamento_id
            WHERE a.estudiante_id=? AND a.periodo_id=? AND a.estado='activa'
            """,(fila,posicion)->new Tutor(fila.getLong(1),fila.getString(2),fila.getString(3)),estudiante,periodo).stream().findFirst().orElse(null);
    }
    private static final String BASE="""
        FROM tutores t JOIN usuarios u ON u.id=t.usuario_id JOIN departamentos d ON d.id=t.departamento_id
        WHERE u.estado='activo' AND CONCAT_WS(' ',u.nombre,d.nombre) LIKE ? ESCAPE '!'
        """;
    private String patron(String texto) { return "%"+texto.strip().replace("!","!!").replace("%","!%").replace("_","!_")+"%"; }
    public long contar(String busqueda) { return consultas.queryForObject("SELECT COUNT(*) "+BASE,Long.class,patron(busqueda)); }
    public List<Tutor> listar(String busqueda,int tamano,long desplazamiento) {
        return consultas.query("SELECT t.id,u.nombre,d.nombre "+BASE+" ORDER BY u.nombre,t.id LIMIT ? OFFSET ?",
            (fila,posicion)->new Tutor(fila.getLong(1),fila.getString(2),fila.getString(3)),patron(busqueda),tamano,desplazamiento);
    }
    public boolean bloquearTutorActivo(long tutor) {
        return !consultas.query("SELECT t.id FROM tutores t JOIN usuarios u ON u.id=t.usuario_id WHERE t.id=? AND u.estado='activo' FOR SHARE",
            (fila,posicion)->fila.getLong(1),tutor).isEmpty();
    }
}
