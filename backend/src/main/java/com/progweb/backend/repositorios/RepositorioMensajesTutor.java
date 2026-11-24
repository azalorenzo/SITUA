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

    public List<RespuestaConversacionTutor> conversaciones(long usuarioId, int limite, String busqueda) {
        return consultas.query("""
            SELECT c.id, o.nombre, o.avatar_url,
                   (SELECT m.contenido """ + ULTIMO + """
            ) AS ultimo_mensaje,
                   (SELECT m.enviado_en """ + ULTIMO + """
            ) AS ultimo_en,
                   (SELECT COUNT(*) FROM mensajes m
                     WHERE m.conversacion_id = c.id AND m.emisor_id <> p.usuario_id
                       AND (p.leido_hasta_en IS NULL OR m.enviado_en > p.leido_hasta_en
                           OR (m.enviado_en=p.leido_hasta_en AND p.leido_hasta_id IS NOT NULL AND m.id>p.leido_hasta_id))) AS no_leidos
            FROM conversacion_participantes p
            JOIN conversaciones c ON c.id = p.conversacion_id AND c.tipo = 'directa'
            JOIN conversacion_participantes q ON q.conversacion_id = c.id AND q.usuario_id <> p.usuario_id
            JOIN usuarios o ON o.id = q.usuario_id
            WHERE p.usuario_id = ? AND o.nombre LIKE ? ESCAPE '!'
            ORDER BY ultimo_en DESC, c.id DESC LIMIT ?
            """, (fila, posicion) -> new RespuestaConversacionTutor(fila.getLong("id"), fila.getString("nombre"),
                fila.getString("avatar_url"), fila.getString("ultimo_mensaje"),
                fila.getObject("ultimo_en", LocalDateTime.class), fila.getLong("no_leidos")), usuarioId, "%"+busqueda.replace("!","!!").replace("%","!%").replace("_","!_")+"%", limite);
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

    public boolean marcarLeida(long usuarioId, long conversacionId, Long ultimoMensajeId) {
        var ultimo = consultas.query("SELECT id,enviado_en FROM mensajes WHERE conversacion_id=?"
                +(ultimoMensajeId==null?"":" AND id=?")+" ORDER BY enviado_en DESC,id DESC LIMIT 1",
                (fila,posicion)->new Object[]{fila.getLong("id"),fila.getObject("enviado_en",LocalDateTime.class)},
                ultimoMensajeId==null?new Object[]{conversacionId}:new Object[]{conversacionId,ultimoMensajeId});
        if (ultimo.isEmpty()) return ultimoMensajeId==null;
        var mensaje = ultimo.getFirst();
        consultas.update("""
            UPDATE conversacion_participantes SET leido_hasta_en=?,leido_hasta_id=?
            WHERE conversacion_id=? AND usuario_id=? AND (leido_hasta_en IS NULL OR leido_hasta_en<?
                OR (leido_hasta_en=? AND (leido_hasta_id IS NULL OR leido_hasta_id<?)))
            """,mensaje[1],mensaje[0],conversacionId,usuarioId,mensaje[1],mensaje[1],mensaje[0]);
        return true;
    }

    public List<Long> contactosAsignados(long usuario, boolean esTutor) {
        return consultas.queryForList("""
            SELECT DISTINCT otro.id FROM asignaciones_tutor a
            JOIN estudiantes e ON e.id=a.estudiante_id JOIN tutores t ON t.id=a.tutor_id
            JOIN situaciones_academicas s ON s.estudiante_id=e.id AND s.periodo_id=a.periodo_id AND s.estado_inscripcion='activo'
            JOIN periodos_academicos p ON p.id=a.periodo_id AND p.estado='activo'
            JOIN usuarios otro ON otro.id=
            """+(esTutor?"e.usuario_id":"t.usuario_id")+" AND otro.estado='activo' WHERE a.estado='activa' AND "
                +(esTutor?"t.usuario_id":"e.usuario_id")+"=? ORDER BY otro.id",Long.class,usuario);
    }

    public void asegurarConversacion(long usuario, long contacto) {
        long menor=Math.min(usuario,contacto),mayor=Math.max(usuario,contacto);
        consultas.update("""
            INSERT INTO conversaciones(creada_por,usuario_menor_id,usuario_mayor_id) VALUES (?,?,?)
            ON DUPLICATE KEY UPDATE id=id
            """,usuario,menor,mayor);
        long id=consultas.queryForObject("SELECT id FROM conversaciones WHERE usuario_menor_id=? AND usuario_mayor_id=? FOR UPDATE",Long.class,menor,mayor);
        consultas.update("INSERT IGNORE INTO conversacion_participantes(conversacion_id,usuario_id) VALUES (?,?),(?,?)",id,menor,id,mayor);
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
