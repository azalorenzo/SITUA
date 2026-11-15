package com.progweb.backend.respuestas;

import java.time.Instant;

public record RespuestaInicioSesion(String token, String tipoToken, Instant venceEn, RespuestaUsuario usuario) {
}
