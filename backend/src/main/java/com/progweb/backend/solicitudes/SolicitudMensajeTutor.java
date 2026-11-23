package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Texto que escribe la persona. El emisor sale del JWT, nunca del cuerpo.
public record SolicitudMensajeTutor(
        @NotBlank(message = "Escribe un mensaje.")
        @Size(max = 4000, message = "El mensaje no debe superar 4000 caracteres.") String contenido) { }
