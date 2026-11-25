package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.*;

public record SolicitudLlamada(@NotNull @Positive Long conversacionId,
        @NotNull @Pattern(regexp="audio|video",message="Selecciona llamada o videollamada.") String tipo) {}
