package com.progweb.backend.respuestas;

import java.util.List;

public record RespuestaUsuario(Long id, String nombre, String correo, String avatarUrl, List<String> roles) {
}
