package com.progweb.backend.respuestas;

import java.time.LocalDateTime;

// Sesión completada con un estudiante. La duración usa la hora real si existe y, si no, la programada.
public record RespuestaSesionEstudiante(Long id, String tema, LocalDateTime inicioEn, long duracionMinutos,
        String observaciones) { }
