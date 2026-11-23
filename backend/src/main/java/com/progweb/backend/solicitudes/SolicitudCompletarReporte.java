package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudCompletarReporte(@NotBlank @Size(max=20000) String contenido) { }
