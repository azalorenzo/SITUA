package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudVerificarWhatsapp(

        @NotBlank(message = "Escribe tu correo electrónico.")
        @Email(message = "Escribe un correo electrónico válido.")
        String correo,

        @NotBlank(message = "Escribe el código de verificación.")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "El código debe contener exactamente seis dígitos."
        )
        String codigo

) {
    @Override
    public String toString() {
        return "SolicitudVerificarWhatsapp[correo=" + correo
                + ", codigo=[PROTEGIDO]]";
    }
}