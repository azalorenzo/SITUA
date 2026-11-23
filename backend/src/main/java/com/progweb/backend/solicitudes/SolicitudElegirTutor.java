package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudElegirTutor(@NotNull @Positive Long tutorId,@NotNull @Positive Long periodoId) { }
