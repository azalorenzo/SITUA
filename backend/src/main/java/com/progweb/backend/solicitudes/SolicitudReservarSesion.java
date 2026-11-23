package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudReservarSesion(@NotNull @Positive Long disponibilidadId,@NotNull @Positive Long periodoId,
    @NotBlank @Size(max=200) String tema,@Size(max=1000) String mensaje) { }
