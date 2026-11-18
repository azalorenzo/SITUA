package com.progweb.backend.respuestas;

import java.time.LocalDateTime;

public record RespuestaHorario(Long id, LocalDateTime inicioEn, LocalDateTime finEn, String modalidad,
        String ubicacion, String enlaceVirtual, String estado, String estudianteNombre) {
}
