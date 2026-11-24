package com.progweb.backend.respuestas;

import java.time.LocalDateTime;

// Aviso visible para la persona conectada. Las fechas están en UTC sin zona.
public record RespuestaAvisoRecibido(Long id, String titulo, String descripcion, String categoria, String prioridad,
        Long sesionId, LocalDateTime publicadoEn, LocalDateTime leidoEn,boolean puedeResponderSesion) { }
