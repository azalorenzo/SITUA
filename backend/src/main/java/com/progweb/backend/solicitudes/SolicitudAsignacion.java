package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
public record SolicitudAsignacion(
        @NotNull(message = "Selecciona un estudiante.") @Positive Long estudianteId,
        @NotNull(message = "Selecciona un tutor.") @Positive Long tutorId,
        @NotNull(message = "Selecciona un periodo.") @Positive Long periodoId,
        @Size(max = 1000, message = "El motivo no debe superar 1000 caracteres.") String motivo) { }
