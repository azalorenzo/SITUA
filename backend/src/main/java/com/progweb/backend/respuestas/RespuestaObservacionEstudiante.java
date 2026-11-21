package com.progweb.backend.respuestas;

import java.time.LocalDateTime;

// Observación que el tutor escribió sobre el estudiante. tipo: nota, progreso o alerta. prioridad: normal o alta.
public record RespuestaObservacionEstudiante(Long id, String tipo, String prioridad, String contenido,
        LocalDateTime observadoEn) { }
