package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudAccesoGoogle(
        @NotBlank(message = "No se recibió la identificación de Google.")
        @Size(max = 16384, message = "La identificación de Google no es válida.") String credencial,
        @NotBlank(message = "No se recibió la comprobación del acceso.")
        @Size(min = 16, max = 128, message = "La comprobación del acceso no es válida.") String comprobacion,
        @Pattern(regexp = "[\\p{L}\\p{N}_-]{3,30}", message = "Usa entre 3 y 30 letras, números o guiones.") String matricula,
        @Size(max = 72, message = "La contraseña es demasiado larga.") String contrasenaVinculacion
) {
    @Override
    public String toString() {
        return "SolicitudAccesoGoogle[credencial=oculta, contrasenaVinculacion=oculta]";
    }
}
