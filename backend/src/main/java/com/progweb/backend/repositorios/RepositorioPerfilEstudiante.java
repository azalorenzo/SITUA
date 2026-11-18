package com.progweb.backend.repositorios;
import java.util.List;
import com.progweb.backend.respuestas.RespuestaPerfilEstudiante.Opcion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class RepositorioPerfilEstudiante {
    private final JdbcTemplate consultas;
    public RepositorioPerfilEstudiante(JdbcTemplate consultas) { this.consultas=consultas; }
    public Long estudiante(long usuario,boolean bloquear) {
        return consultas.query("SELECT id FROM estudiantes WHERE usuario_id=?"+(bloquear?" FOR UPDATE":""),
            (fila,posicion)->fila.getLong(1),usuario).stream().findFirst().orElse(null);
    }
    public boolean completo(long estudiante) {
        return consultas.queryForObject("SELECT COUNT(*) FROM situaciones_academicas WHERE estudiante_id=?",Long.class,estudiante)>0;
    }
    public List<Opcion> carreras() {
        return consultas.query("SELECT c.id,c.nombre FROM carreras c JOIN departamentos d ON d.id=c.departamento_id WHERE c.activa=true AND d.activo=true ORDER BY c.nombre,c.id",
            (fila,posicion)->new Opcion(fila.getLong(1),fila.getString(2)));
    }
    public List<Opcion> periodos() {
        return consultas.query("SELECT id,nombre FROM periodos_academicos WHERE estado='activo' ORDER BY fecha_inicio DESC,id DESC",
            (fila,posicion)->new Opcion(fila.getLong(1),fila.getString(2)));
    }
    public boolean carreraValida(long id) {
        return consultas.queryForObject("SELECT COUNT(*) FROM carreras c JOIN departamentos d ON d.id=c.departamento_id WHERE c.id=? AND c.activa=true AND d.activo=true FOR SHARE",Long.class,id)>0;
    }
    public boolean periodoValido(long id) {
        return consultas.queryForObject("SELECT COUNT(*) FROM periodos_academicos WHERE id=? AND estado='activo' FOR SHARE",Long.class,id)>0;
    }
    public void completar(long estudiante,long carrera,long periodo,int semestre) {
        consultas.update("INSERT INTO situaciones_academicas(estudiante_id,carrera_id,periodo_id,semestre) VALUES (?,?,?,?)",estudiante,carrera,periodo,semestre);
    }
}
