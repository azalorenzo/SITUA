
package com.progweb.backend.solicitudes;

import com.progweb.backend.validaciones.RegistroCoherente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RegistroCoherente
public record SolicitudRegistro(
        @NotBlank(message = "Escribe tu nombre.")
        @Size(max = 160, message = "El nombre es demasiado largo.")
        String nombre,

        @NotBlank(message = "Escribe tu matrícula.")
        @Pattern(regexp = "[\\p{L}\\p{N}_-]{3,30}",
                message = "Usa entre 3 y 30 letras, números o guiones.")
        String matricula,

        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String correo,

        @NotBlank(message = "Escribe tu número de WhatsApp.")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Escribe tu número con código de país. Ejemplo: +525512345678.")
        String telefono,

        @NotBlank(message = "Escribe tu contraseña.")
        @Size(min = 8, max = 72,
                message = "La contraseña debe tener entre 8 y 72 caracteres.")
        @Pattern(regexp = "^(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$",
                message = "Incluye una mayúscula, un número y un carácter especial.")
        String contrasena,

        @NotBlank(message = "Confirma tu contraseña.")
        @Size(max = 72, message = "La confirmación es demasiado larga.")
        String confirmarContrasena
) {
    @Override
    public String toString() {
        return "SolicitudRegistro[correo=" + correo
                + ", telefono=oculto, contraseñas=ocultas]";
    }
}
