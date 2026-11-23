package com.progweb.backend.respuestas;

import java.time.LocalDateTime;

// Conversación directa de la persona conectada. nombre y avatarUrl son de la otra persona.
// Las fechas están en UTC sin zona. ultimoMensaje y ultimoEnviadoEn llegan nulos si aún no hay mensajes.
public record RespuestaConversacionTutor(Long id, String nombre, String avatarUrl, String ultimoMensaje,
        LocalDateTime ultimoEnviadoEn, long noLeidos) { }
