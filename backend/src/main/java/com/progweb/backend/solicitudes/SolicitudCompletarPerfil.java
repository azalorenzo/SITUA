package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudCompletarPerfil(@NotNull @Positive Long carreraId,@NotNull @Positive Long periodoId,
    @NotNull @Min(1) @Max(65535) Integer semestre) { }
