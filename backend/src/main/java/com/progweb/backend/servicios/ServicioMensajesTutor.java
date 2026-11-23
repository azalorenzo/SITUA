package com.progweb.backend.servicios;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioMensajesTutor;
import com.progweb.backend.respuestas.RespuestaConversacionLeida;
import com.progweb.backend.respuestas.RespuestaConversacionTutor;
import com.progweb.backend.respuestas.RespuestaMensajeTutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Mensajes del tutor conectado. La identidad sale siempre del JWT y solo se leen conversaciones donde participa.
@Service
@Transactional(readOnly = true)
public class ServicioMensajesTutor {
    private static final int MAXIMO_CONVERSACIONES = 100;
    private static final int MAXIMO_MENSAJES = 200;

    private final RepositorioMensajesTutor mensajes;
    private final ServicioAutenticacion autenticacion;
    private final Clock reloj;

    public ServicioMensajesTutor(RepositorioMensajesTutor mensajes, ServicioAutenticacion autenticacion, Clock reloj) {
        this.mensajes = mensajes;
        this.autenticacion = autenticacion;
        this.reloj = reloj;
    }

    private long usuarioTutor(String identificador) {
        var usuario = autenticacion.consultarSesion(identificador);
        if (!usuario.roles().contains("tutor")) {
            throw new AccesoRechazado(403, "Esta operación requiere una cuenta de tutor.");
        }
        return usuario.id();
    }

    // Una conversación ajena responde igual que una inexistente.
    private void verificarParticipante(long usuario, long conversacion) {
        if (!mensajes.participa(usuario, conversacion)) {
            throw new AccesoRechazado(404, "No se encontró la conversación.");
        }
    }

    public List<RespuestaConversacionTutor> conversaciones(String identificador) {
        return mensajes.conversaciones(usuarioTutor(identificador), MAXIMO_CONVERSACIONES);
    }

    public List<RespuestaMensajeTutor> consultar(String identificador, long conversacion) {
        long usuario = usuarioTutor(identificador);
        verificarParticipante(usuario, conversacion);
        return mensajes.mensajes(usuario, conversacion, MAXIMO_MENSAJES);
    }

    @Transactional
    public RespuestaConversacionLeida marcarLeida(String identificador, long conversacion) {
        long usuario = usuarioTutor(identificador);
        verificarParticipante(usuario, conversacion);
        mensajes.marcarLeida(usuario, conversacion);
        return new RespuestaConversacionLeida(conversacion);
    }

    @Transactional
    public RespuestaMensajeTutor enviar(String identificador, long conversacion, String contenido) {
        long usuario = usuarioTutor(identificador);
        verificarParticipante(usuario, conversacion);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        long id = mensajes.insertarMensaje(usuario, conversacion, contenido.strip(), ahora);
        return new RespuestaMensajeTutor(id, contenido.strip(), ahora, true);
    }
}
