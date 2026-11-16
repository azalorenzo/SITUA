package com.progweb.backend.respuestas;

import java.math.BigDecimal;

public record RespuestaEstudianteCoordinacion(Long id, Long usuarioId, String nombre, String correo,
        String telefono, String avatarUrl, String estado, String matricula, Long periodoId,
        String periodo, String carrera, Integer semestre, BigDecimal promedio, String nivelRiesgo) {
}
