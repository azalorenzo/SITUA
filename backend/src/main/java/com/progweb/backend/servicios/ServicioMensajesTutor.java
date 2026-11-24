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

    private long identificar(String identificador, boolean esTutor) {
        var usuario = autenticacion.consultarSesion(identificador);
        if (!usuario.roles().contains(esTutor?"tutor":"estudiante")) {
            throw new AccesoRechazado(403, "No tienes acceso a esta opción.");
        }
        return usuario.id();
    }

    // Una conversación ajena responde igual que una inexistente.
    private void verificarParticipante(long usuario, long conversacion) {
        if (!mensajes.participa(usuario, conversacion)) {
            throw new AccesoRechazado(404, "No se encontró la conversación.");
        }
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<RespuestaConversacionTutor> conversaciones(String identificador) {
        return conversaciones(identificador,true,"");
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<RespuestaConversacionTutor> conversaciones(String identificador, boolean esTutor, String busqueda) {
        long usuario=identificar(identificador,esTutor);
        String texto=busqueda.strip();
        if(texto.length()>200) throw new AccesoRechazado(422,"La búsqueda es demasiado larga.");
        for(long contacto:mensajes.contactosAsignados(usuario,esTutor)) mensajes.asegurarConversacion(usuario,contacto);
        return mensajes.conversaciones(usuario,MAXIMO_CONVERSACIONES,texto);
    }

    public List<RespuestaMensajeTutor> consultar(String identificador, long conversacion) {
        return consultar(identificador,true,conversacion);
    }
    public List<RespuestaMensajeTutor> consultar(String identificador, boolean esTutor, long conversacion) {
        long usuario=identificar(identificador,esTutor);
        verificarParticipante(usuario,conversacion);
        return mensajes.mensajes(usuario,conversacion,MAXIMO_MENSAJES);
    }

    @Transactional
    public RespuestaConversacionLeida marcarLeida(String identificador, long conversacion) {
        return marcarLeida(identificador,true,conversacion,null);
    }
    @Transactional
    public RespuestaConversacionLeida marcarLeida(String identificador, boolean esTutor, long conversacion, Long ultimoMensajeId) {
        long usuario=identificar(identificador,esTutor);
        verificarParticipante(usuario,conversacion);
        if(!mensajes.marcarLeida(usuario,conversacion,ultimoMensajeId)) throw new AccesoRechazado(404,"No se encontró el mensaje.");
        return new RespuestaConversacionLeida(conversacion);
    }

    @Transactional
    public RespuestaMensajeTutor enviar(String identificador, long conversacion, String contenido) {
        return enviar(identificador,true,conversacion,contenido);
    }
    @Transactional
    public RespuestaMensajeTutor enviar(String identificador, boolean esTutor, long conversacion, String contenido) {
        long usuario=identificar(identificador,esTutor);
        verificarParticipante(usuario,conversacion);
        LocalDateTime ahora=LocalDateTime.ofInstant(reloj.instant(),java.time.ZoneOffset.UTC);
        long id=mensajes.insertarMensaje(usuario,conversacion,contenido.strip(),ahora);
        return new RespuestaMensajeTutor(id,contenido.strip(),ahora,true);
    }
}
