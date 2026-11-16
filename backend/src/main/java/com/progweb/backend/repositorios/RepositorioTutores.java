package com.progweb.backend.repositorios;

import java.util.List;
import java.util.Optional;
import java.sql.Statement;
import com.progweb.backend.respuestas.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioTutores {
    private final JdbcTemplate consultas;
    private final NamedParameterJdbcTemplate paginas;
    private static final String BASE = """
        FROM tutores t JOIN usuarios u ON u.id=t.usuario_id
        JOIN departamentos d ON d.id=t.departamento_id
        """;
    private static final String COLUMNAS = "SELECT t.id,u.id AS usuario_id,u.nombre,u.correo,u.telefono,u.avatar_url,u.estado,d.id AS departamento_id,d.nombre AS departamento ";
    private static final String FILTROS = """
        WHERE (:estado IS NULL OR u.estado=:estado)
          AND (:departamentoId IS NULL OR t.departamento_id=:departamentoId)
          AND (:busqueda='' OR CONCAT_WS(' ',u.nombre,u.correo,d.nombre) LIKE :patron ESCAPE '!')
        """;
    private final RowMapper<RespuestaTutorCoordinacion> convertir = (fila, posicion) -> new RespuestaTutorCoordinacion(
        fila.getLong("id"),fila.getLong("usuario_id"),fila.getString("nombre"),fila.getString("correo"),
        fila.getString("telefono"),fila.getString("avatar_url"),fila.getString("estado"),fila.getLong("departamento_id"),fila.getString("departamento"));

    public RepositorioTutores(JdbcTemplate consultas) { this.consultas=consultas; this.paginas=new NamedParameterJdbcTemplate(consultas); }
    private MapSqlParameterSource parametros(String estado, Long departamentoId, String busqueda) {
        String patron="%"+busqueda.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return new MapSqlParameterSource().addValue("estado",estado,java.sql.Types.VARCHAR)
            .addValue("departamentoId",departamentoId,java.sql.Types.BIGINT).addValue("busqueda",busqueda).addValue("patron",patron);
    }
    public long contar(String estado, Long departamentoId, String busqueda) {
        return paginas.queryForObject("SELECT COUNT(*) "+BASE+FILTROS,parametros(estado,departamentoId,busqueda),Long.class);
    }
    public List<RespuestaTutorCoordinacion> listar(String estado, Long departamentoId, String busqueda, int tamano, long desplazamiento) {
        return paginas.query(COLUMNAS+BASE+FILTROS+" ORDER BY u.nombre,t.id LIMIT :tamano OFFSET :desplazamiento",
            parametros(estado,departamentoId,busqueda).addValue("tamano",tamano).addValue("desplazamiento",desplazamiento),convertir);
    }
    public PaginaTutores.Resumen resumen() {
        return consultas.queryForObject("SELECT COUNT(*) total, COALESCE(SUM(u.estado='activo'),0) activos, COALESCE(SUM(u.estado='inactivo'),0) inactivos "+BASE,
            (fila,posicion) -> new PaginaTutores.Resumen(fila.getLong("total"),fila.getLong("activos"),fila.getLong("inactivos")));
    }
    public List<RespuestaTutorCoordinacion> opciones() {
        return consultas.query(COLUMNAS+BASE+" WHERE u.estado='activo' ORDER BY u.nombre,t.id",convertir);
    }
    public List<RespuestaDepartamento> departamentos() {
        return consultas.query("SELECT id,nombre,activo FROM departamentos ORDER BY nombre,id",
            (fila,posicion) -> new RespuestaDepartamento(fila.getLong("id"),fila.getString("nombre"),fila.getBoolean("activo")));
    }
    public boolean departamentoActivo(long id) {
        return consultas.queryForObject("SELECT COUNT(*) FROM departamentos WHERE id=? AND activo=1",Long.class,id)==1;
    }
    public Optional<RespuestaTutorCoordinacion> buscar(long id) {
        return consultas.query(COLUMNAS+BASE+" WHERE t.id=?",convertir,id).stream().findFirst();
    }
    public Optional<RespuestaTutorCoordinacion> bloquear(long id) {
        consultas.query("SELECT t.id FROM tutores t JOIN usuarios u ON u.id=t.usuario_id WHERE t.id=? FOR UPDATE",(fila,posicion)->fila.getLong(1),id);
        return buscar(id);
    }
    public long crear(long usuarioId, long departamentoId) {
        var llave=new GeneratedKeyHolder();
        consultas.update(conexion -> {
            var sentencia=conexion.prepareStatement("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",Statement.RETURN_GENERATED_KEYS);
            sentencia.setLong(1,usuarioId);sentencia.setLong(2,departamentoId);return sentencia;
        },llave);
        return llave.getKey().longValue();
    }
    public void cambiarDepartamento(long id,long departamentoId) {
        consultas.update("UPDATE tutores SET departamento_id=? WHERE id=?",departamentoId,id);
    }
}
