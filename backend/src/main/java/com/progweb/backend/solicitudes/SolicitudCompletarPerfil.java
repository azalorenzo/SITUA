package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudCompletarPerfil(@NotNull @Positive Long carreraId,@NotNull @Positive Long periodoId,
    @NotNull @Min(1) @Max(value=12,message="El semestre debe estar entre 1 y 12.") Integer semestre) { }
