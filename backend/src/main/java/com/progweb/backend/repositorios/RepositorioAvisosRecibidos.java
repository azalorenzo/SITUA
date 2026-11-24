package com.progweb.backend.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.progweb.backend.respuestas.RespuestaAvisoRecibido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

// Consultas de los avisos que recibe una persona. Siempre se filtra por usuarios.id, que viene del JWT.
// La vista v_avisos_visibles ya excluye los avisos retirados y los que la persona ocultó.
@Repository
public class RepositorioAvisosRecibidos {
    private static final String SELECCION = """
        SELECT aviso_id, titulo, descripcion, categoria, prioridad, sesion_id, publicado_en, leido_en,
            EXISTS(SELECT 1 FROM sesiones s JOIN tutores t ON t.id=s.tutor_id
                WHERE s.id=v.sesion_id AND t.usuario_id=v.usuario_id AND s.estado='solicitada' AND s.inicio_en>UTC_TIMESTAMP(6)) puede_responder_sesion
        FROM v_avisos_visibles v
        """;

    // Un aviso solo se marca si sigue vigente y visible para esa persona; solo se toca su propia fila.
    private static final String CONDICION_MARCADO = """
        d.usuario_id=? AND d.leido_en IS NULL AND d.ocultado_en IS NULL
        AND EXISTS (SELECT 1 FROM avisos a WHERE a.id=d.aviso_id AND a.retirado_en IS NULL)
        """;

    private final JdbcTemplate consultas;

    private final RowMapper<RespuestaAvisoRecibido> convertir = (fila, posicion) -> new RespuestaAvisoRecibido(
            fila.getLong("aviso_id"), fila.getString("titulo"), fila.getString("descripcion"),
            fila.getString("categoria"), fila.getString("prioridad"), fila.getObject("sesion_id", Long.class),
            fila.getObject("publicado_en", LocalDateTime.class), fila.getObject("leido_en", LocalDateTime.class),fila.getBoolean("puede_responder_sesion"));

    public RepositorioAvisosRecibidos(JdbcTemplate consultas) { this.consultas = consultas; }

    public List<RespuestaAvisoRecibido> listar(long usuarioId, int limite) {
        return consultas.query(SELECCION + " WHERE usuario_id=? ORDER BY publicado_en DESC, aviso_id DESC LIMIT ?",
                convertir, usuarioId, limite);
    }

    public Optional<RespuestaAvisoRecibido> buscar(long usuarioId, long avisoId) {
        return consultas.query(SELECCION + " WHERE usuario_id=? AND aviso_id=?", convertir, usuarioId, avisoId)
                .stream().findFirst();
    }

    public int marcarLeido(long usuarioId, long avisoId, LocalDateTime ahora) {
        return consultas.update("UPDATE aviso_destinatarios d SET d.leido_en=? WHERE d.aviso_id=? AND " + CONDICION_MARCADO,
                ahora, avisoId, usuarioId);
    }

    public int marcarTodosLeidos(long usuarioId, LocalDateTime ahora) {
        return consultas.update("UPDATE aviso_destinatarios d SET d.leido_en=? WHERE " + CONDICION_MARCADO, ahora, usuarioId);
    }

    public long contar(long usuario){return consultas.queryForObject("SELECT COUNT(*) FROM v_avisos_visibles WHERE usuario_id=?",Long.class,usuario);}
    public long noLeidos(long usuario){return consultas.queryForObject("SELECT COUNT(*) FROM v_avisos_visibles WHERE usuario_id=? AND leido_en IS NULL",Long.class,usuario);}
    public List<RespuestaAvisoRecibido> listarPagina(long usuario,int tamano,long desplazamiento){return consultas.query(SELECCION+" WHERE usuario_id=? ORDER BY publicado_en DESC,aviso_id DESC LIMIT ? OFFSET ?",convertir,usuario,tamano,desplazamiento);}
    private record Consulta(String sql,java.util.ArrayList<Object> parametros) { }
    private Consulta preparar(long usuario,com.progweb.backend.solicitudes.FiltrosAvisosRecibidos filtros){
        var parametros=new java.util.ArrayList<Object>();parametros.add(usuario);String condiciones=" WHERE usuario_id=?";
        if(!filtros.busqueda().isEmpty()){
            String patron="%"+filtros.busqueda().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            condiciones+=" AND (titulo LIKE ? ESCAPE '!' OR descripcion LIKE ? ESCAPE '!')";parametros.add(patron);parametros.add(patron);
        }
        if(!filtros.prioridad().equals("todos")){condiciones+=" AND prioridad=?";parametros.add(filtros.prioridad());}
        if(filtros.lectura().equals("leidos"))condiciones+=" AND leido_en IS NOT NULL";
        if(filtros.lectura().equals("no_leidos"))condiciones+=" AND leido_en IS NULL";
        return new Consulta(SELECCION+condiciones,parametros);
    }
    public long contarFiltrados(long usuario,com.progweb.backend.solicitudes.FiltrosAvisosRecibidos filtros){var consulta=preparar(usuario,filtros);return consultas.queryForObject("SELECT COUNT(*) FROM ("+consulta.sql()+") avisos",Long.class,consulta.parametros().toArray());}
    public List<RespuestaAvisoRecibido> listarPagina(long usuario,com.progweb.backend.solicitudes.FiltrosAvisosRecibidos filtros,int pagina){
        var consulta=preparar(usuario,filtros);consulta.parametros().add(filtros.tamano());consulta.parametros().add((pagina-1L)*filtros.tamano());
        return consultas.query(consulta.sql()+" ORDER BY publicado_en DESC,aviso_id DESC LIMIT ? OFFSET ?",convertir,consulta.parametros().toArray());
    }
    public long importantes(long usuario){return consultas.queryForObject("SELECT COUNT(*) FROM v_avisos_visibles WHERE usuario_id=? AND prioridad='alta'",Long.class,usuario);}

}
