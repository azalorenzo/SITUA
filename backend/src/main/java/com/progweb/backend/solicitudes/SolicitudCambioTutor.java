package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
public record SolicitudCambioTutor(
        @NotNull(message = "Selecciona un tutor.") @Positive Long tutorId,
        @Size(max = 1000, message = "El motivo no debe superar 1000 caracteres.") String motivo) { }
