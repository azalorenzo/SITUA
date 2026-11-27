package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitudReenviarCodigoWhatsapp(

        @NotBlank(message = "Escribe tu correo electrónico.")
        @Email(message = "Escribe un correo electrónico válido.")
        String correo

) {
}