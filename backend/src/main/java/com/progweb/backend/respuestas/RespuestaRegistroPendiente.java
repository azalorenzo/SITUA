
package com.progweb.backend.respuestas;

public record RespuestaRegistroPendiente(
        String mensaje,
        String correo,
        int segundosVigencia
) {
}
