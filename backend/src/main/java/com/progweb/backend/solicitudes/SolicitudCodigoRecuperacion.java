package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudCodigoRecuperacion(@NotBlank @Email @Size(max=254) String correo) { }
