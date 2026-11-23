package com.progweb.backend.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.respuestas.RespuestaConversacionTutor;
import com.progweb.backend.respuestas.RespuestaMensajeTutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Consultas de las conversaciones directas de una persona. Siempre se filtra por usuarios.id, que viene del JWT.
@Repository
public class RepositorioMensajesTutor {
    private static final String ULTIMO = " FROM mensajes m WHERE m.conversacion_id = c.id ORDER BY m.enviado_en DESC, m.id DESC LIMIT 1";

    private final JdbcTemplate consultas;

    public RepositorioMensajesTutor(JdbcTemplate consultas) { this.consultas = consultas; }

    public List<RespuestaConversacionTutor> conversaciones(long usuarioId, int limite) {
        return consultas.query("""
            SELECT c.id, o.nombre, o.avatar_url,
                   (SELECT m.contenido """ + ULTIMO + """
            ) AS ultimo_mensaje,
                   (SELECT m.enviado_en """ + ULTIMO + """
            ) AS ultimo_en,
                   (SELECT COUNT(*) FROM mensajes m
                     WHERE m.conversacion_id = c.id AND m.emisor_id <> p.usuario_id
                       AND (p.leido_hasta_en IS NULL OR m.enviado_en > p.leido_hasta_en)) AS no_leidos
            FROM conversacion_participantes p
            JOIN conversaciones c ON c.id = p.conversacion_id AND c.tipo = 'directa'
            JOIN conversacion_participantes q ON q.conversacion_id = c.id AND q.usuario_id <> p.usuario_id
            JOIN usuarios o ON o.id = q.usuario_id
            WHERE p.usuario_id = ?
            ORDER BY ultimo_en DESC, c.id DESC LIMIT ?
            """, (fila, posicion) -> new RespuestaConversacionTutor(fila.getLong("id"), fila.getString("nombre"),
                fila.getString("avatar_url"), fila.getString("ultimo_mensaje"),
                fila.getObject("ultimo_en", LocalDateTime.class), fila.getLong("no_leidos")), usuarioId, limite);
    }

    public boolean participa(long usuarioId, long conversacionId) {
        Long total = consultas.queryForObject("SELECT COUNT(*) FROM conversacion_participantes WHERE conversacion_id=? AND usuario_id=?",
                Long.class, conversacionId, usuarioId);
        return total != null && total > 0;
    }

    // Los últimos mensajes, ya en orden cronológico.
    public List<RespuestaMensajeTutor> mensajes(long usuarioId, long conversacionId, int limite) {
        return consultas.query("""
            SELECT * FROM (
                SELECT id, contenido, enviado_en, emisor_id FROM mensajes
                WHERE conversacion_id = ? ORDER BY enviado_en DESC, id DESC LIMIT ?
            ) ultimos ORDER BY enviado_en, id
            """, (fila, posicion) -> new RespuestaMensajeTutor(fila.getLong("id"), fila.getString("contenido"),
                fila.getObject("enviado_en", LocalDateTime.class), fila.getLong("emisor_id") == usuarioId), conversacionId, limite);
    }

    // La persona solo avanza su propia marca de lectura, hasta el último mensaje de la conversación.
    public void marcarLeida(long usuarioId, long conversacionId) {
        consultas.update("""
            UPDATE conversacion_participantes p
            SET p.leido_hasta_en = (SELECT MAX(m.enviado_en) FROM mensajes m WHERE m.conversacion_id = p.conversacion_id)
            WHERE p.conversacion_id = ? AND p.usuario_id = ?
              AND (SELECT MAX(m.enviado_en) FROM mensajes m WHERE m.conversacion_id = p.conversacion_id) IS NOT NULL
              AND (p.leido_hasta_en IS NULL
                   OR p.leido_hasta_en < (SELECT MAX(m.enviado_en) FROM mensajes m WHERE m.conversacion_id = p.conversacion_id))
            """, conversacionId, usuarioId);
    }

    public long insertarMensaje(long usuarioId, long conversacionId, String contenido, LocalDateTime enviadoEn) {
        var llave = new org.springframework.jdbc.support.GeneratedKeyHolder();
        consultas.update(conexion -> {
            var sentencia = conexion.prepareStatement("INSERT INTO mensajes(conversacion_id,emisor_id,contenido,enviado_en) VALUES (?,?,?,?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            sentencia.setLong(1, conversacionId);
            sentencia.setLong(2, usuarioId);
            sentencia.setString(3, contenido);
            sentencia.setObject(4, enviadoEn);
            return sentencia;
        }, llave);
        return llave.getKey().longValue();
    }
}
