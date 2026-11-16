package com.progweb.backend.respuestas;

import java.time.LocalDateTime;
public record RespuestaAsignacion(Long id, Long estudianteId, String estudiante, String matricula,
        Long tutorId, String tutor, Long periodoId, String periodo, Long asignadoPor,
        LocalDateTime inicioEn, LocalDateTime finEn, String estado, String motivo,
        Long sesionesCompletadas, LocalDateTime ultimaSesion) { }
