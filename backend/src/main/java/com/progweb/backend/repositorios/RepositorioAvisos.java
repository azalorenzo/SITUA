package com.progweb.backend.repositorios;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.sql.Statement;
import java.time.LocalDateTime;
import com.progweb.backend.respuestas.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioAvisos {
    private final JdbcTemplate consultas;
    private final NamedParameterJdbcTemplate parametros;
    private static final String BASE="FROM avisos a JOIN usuarios u ON u.id=a.autor_id ";
    private static final String COLUMNAS="""
        SELECT a.*,u.nombre AS autor,
          (SELECT COUNT(*) FROM aviso_destinatarios d WHERE d.aviso_id=a.id) AS destinatarios,
          (SELECT COUNT(*) FROM aviso_destinatarios d WHERE d.aviso_id=a.id AND d.leido_en IS NOT NULL) AS lecturas
        """;
    private static final String FILTROS="""
        WHERE (:prioridad IS NULL OR a.prioridad=:prioridad)
          AND (:categoria IS NULL OR a.categoria=:categoria)
          AND (:estado IS NULL OR (:estado='vigente' AND a.retirado_en IS NULL) OR (:estado='retirado' AND a.retirado_en IS NOT NULL))
          AND (:busqueda='' OR CONCAT_WS(' ',a.titulo,a.descripcion,a.categoria,u.nombre) LIKE :patron ESCAPE '!')
        """;
    private static final String ELEGIBLES="""
        FROM usuarios u WHERE u.estado='activo' AND EXISTS (
          SELECT 1 FROM usuario_roles ur JOIN roles r ON r.id=ur.rol_id
          WHERE ur.usuario_id=u.id AND r.codigo IN ('estudiante','tutor') AND (:rol IS NULL OR r.codigo=:rol))
        """;
    private final RowMapper<RespuestaAviso> convertir=(fila,posicion)->new RespuestaAviso(
        fila.getLong("id"),fila.getLong("autor_id"),fila.getString("autor"),fila.getString("titulo"),fila.getString("descripcion"),
        fila.getString("categoria"),fila.getString("prioridad"),fila.getObject("publicado_en",LocalDateTime.class),
        fila.getObject("retirado_en",LocalDateTime.class),fila.getLong("destinatarios"),fila.getLong("lecturas"));
    public RepositorioAvisos(JdbcTemplate consultas){this.consultas=consultas;this.parametros=new NamedParameterJdbcTemplate(consultas);}
    private MapSqlParameterSource filtros(String prioridad,String categoria,String estado,String busqueda){
        String patron="%"+busqueda.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return new MapSqlParameterSource().addValue("prioridad",prioridad,java.sql.Types.VARCHAR)
            .addValue("categoria",categoria,java.sql.Types.VARCHAR).addValue("estado",estado,java.sql.Types.VARCHAR)
            .addValue("busqueda",busqueda).addValue("patron",patron);
    }
    public long contar(String prioridad,String categoria,String estado,String busqueda){
        return parametros.queryForObject("SELECT COUNT(*) "+BASE+FILTROS,filtros(prioridad,categoria,estado,busqueda),Long.class);
    }
    public List<RespuestaAviso> listar(String prioridad,String categoria,String estado,String busqueda,int tamano,long desplazamiento){
        return parametros.query(COLUMNAS+BASE+FILTROS+" ORDER BY a.publicado_en DESC,a.id DESC LIMIT :tamano OFFSET :desplazamiento",
            filtros(prioridad,categoria,estado,busqueda).addValue("tamano",tamano).addValue("desplazamiento",desplazamiento),convertir);
    }
    public PaginaAvisos.Resumen resumen(){
        return consultas.queryForObject("""
            SELECT COUNT(*) total,COALESCE(SUM(retirado_en IS NULL),0) vigentes,
              COALESCE(SUM(retirado_en IS NOT NULL),0) retirados,
              COALESCE(SUM(retirado_en IS NULL AND prioridad='alta'),0) importantes FROM avisos
            """,(fila,posicion)->new PaginaAvisos.Resumen(fila.getLong("total"),fila.getLong("vigentes"),fila.getLong("retirados"),fila.getLong("importantes")));
    }
    public Optional<RespuestaAviso> buscar(long id){return consultas.query(COLUMNAS+BASE+" WHERE a.id=?",convertir,id).stream().findFirst();}
    public Optional<RespuestaAviso> bloquear(long id){
        consultas.query("SELECT id FROM avisos WHERE id=? FOR UPDATE",(fila,posicion)->fila.getLong(1),id);return buscar(id);
    }
    public List<RespuestaDestinatarioAviso> destinatarios(long id){
        return consultas.query("""
            SELECT u.id,u.nombre,u.correo,u.estado,d.leido_en,d.ocultado_en FROM aviso_destinatarios d
            JOIN usuarios u ON u.id=d.usuario_id WHERE d.aviso_id=? ORDER BY u.nombre,u.id
            """,(fila,posicion)->new RespuestaDestinatarioAviso(fila.getLong("id"),fila.getString("nombre"),fila.getString("correo"),fila.getString("estado"),
                fila.getObject("leido_en",LocalDateTime.class),fila.getObject("ocultado_en",LocalDateTime.class)),id);
    }
    public List<Long> resolver(String grupo,List<Long> seleccion){
        String rol=grupo.equals("estudiantes")?"estudiante":grupo.equals("tutores")?"tutor":null;
        var valores=filtros(null,null,null,"").addValue("rol",rol,java.sql.Types.VARCHAR);
        String condicion="";
        if(grupo.equals("seleccion")){condicion=" AND u.id IN (:ids)";valores.addValue("ids",seleccion);}
        return parametros.query("SELECT u.id "+ELEGIBLES+condicion,valores,(fila,posicion)->fila.getLong(1));
    }
    public long contarPersonas(String rol,String busqueda){
        return parametros.queryForObject("SELECT COUNT(*) "+ELEGIBLES+" AND (:busqueda='' OR CONCAT_WS(' ',u.nombre,u.correo) LIKE :patron ESCAPE '!')",
            filtros(null,null,null,busqueda).addValue("rol",rol,java.sql.Types.VARCHAR),Long.class);
    }
    public List<RespuestaDestinatarioAviso> personas(String rol,String busqueda,int tamano,long desplazamiento){
        return parametros.query("SELECT u.id,u.nombre,u.correo,u.estado "+ELEGIBLES+" AND (:busqueda='' OR CONCAT_WS(' ',u.nombre,u.correo) LIKE :patron ESCAPE '!') ORDER BY u.nombre,u.id LIMIT :tamano OFFSET :desplazamiento",
            filtros(null,null,null,busqueda).addValue("rol",rol,java.sql.Types.VARCHAR).addValue("tamano",tamano).addValue("desplazamiento",desplazamiento),
            (fila,posicion)->new RespuestaDestinatarioAviso(fila.getLong("id"),fila.getString("nombre"),fila.getString("correo"),fila.getString("estado"),null,null));
    }
    public long crear(long autor,String titulo,String descripcion,String categoria,String prioridad){
        var llave=new GeneratedKeyHolder();consultas.update(conexion->{
            var sentencia=conexion.prepareStatement("INSERT INTO avisos(autor_id,titulo,descripcion,categoria,prioridad,publicado_en) VALUES (?,?,?,?,?,UTC_TIMESTAMP(6))",Statement.RETURN_GENERATED_KEYS);
            sentencia.setLong(1,autor);sentencia.setString(2,titulo);sentencia.setString(3,descripcion);sentencia.setString(4,categoria);sentencia.setString(5,prioridad);return sentencia;
        },llave);return llave.getKey().longValue();
    }
    public void editar(long id,String titulo,String descripcion,String categoria,String prioridad){
        consultas.update("UPDATE avisos SET titulo=?,descripcion=?,categoria=?,prioridad=? WHERE id=?",titulo,descripcion,categoria,prioridad,id);
    }
    public void guardarDestinatarios(long avisoId,List<Long> ids){
        var existentes=destinatarios(avisoId).stream().map(RespuestaDestinatarioAviso::usuarioId).collect(java.util.stream.Collectors.toSet());
        var nuevos=new ArrayList<>(ids);nuevos.removeAll(existentes);
        // Conservamos las lecturas de quienes siguen recibiendo el aviso.
        parametros.update("DELETE FROM aviso_destinatarios WHERE aviso_id=:avisoId AND usuario_id NOT IN (:ids)",new MapSqlParameterSource().addValue("avisoId",avisoId).addValue("ids",ids));
        consultas.batchUpdate("INSERT INTO aviso_destinatarios(aviso_id,usuario_id) VALUES (?,?)",nuevos,200,(sentencia,id)->{
            sentencia.setLong(1,avisoId);sentencia.setLong(2,id);
        });
    }
    public boolean retirar(long id){
        return consultas.update("UPDATE avisos SET retirado_en=UTC_TIMESTAMP(6) WHERE id=? AND retirado_en IS NULL AND publicado_en<=UTC_TIMESTAMP(6)",id)==1;
    }
}
