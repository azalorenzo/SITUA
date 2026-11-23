package com.progweb.backend.respuestas;

import java.time.LocalDateTime;

// Mensaje de una conversación. enviado es verdadero cuando lo escribió la persona conectada.
public record RespuestaMensajeTutor(Long id, String contenido, LocalDateTime enviadoEn, boolean enviado) { }
