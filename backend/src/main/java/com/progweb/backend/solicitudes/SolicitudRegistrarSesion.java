package com.progweb.backend.solicitudes;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
public record SolicitudRegistrarSesion(@NotNull @Positive Long estudianteId,@NotNull @Positive Long periodoId,
    @NotBlank @Size(max=200) String tema,@NotNull LocalDateTime inicioEn,@NotNull LocalDateTime finEn,
    @NotBlank @Size(max=1024) String ubicacion,@Size(max=10000) String observaciones) { }
