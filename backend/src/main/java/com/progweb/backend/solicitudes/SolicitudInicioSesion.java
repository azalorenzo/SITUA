package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudInicioSesion(
        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.")
        @Size(max = 254, message = "El correo es demasiado largo.") String correo,
        @NotBlank(message = "Escribe tu contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        @Pattern(regexp = "^(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$",
                message = "Incluye una mayúscula, un número y un carácter especial.") String contrasena
) {
}
