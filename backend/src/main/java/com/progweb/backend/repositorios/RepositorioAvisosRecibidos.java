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
        SELECT aviso_id, titulo, descripcion, categoria, prioridad, sesion_id, publicado_en, leido_en
        FROM v_avisos_visibles
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
            fila.getObject("publicado_en", LocalDateTime.class), fila.getObject("leido_en", LocalDateTime.class));

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
}
