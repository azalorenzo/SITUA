package com.progweb.backend.respuestas;

public record RespuestaTutorCoordinacion(Long id, Long usuarioId, String nombre, String correo,
        String telefono, String avatarUrl, String estado, Long departamentoId, String departamento) {
}
