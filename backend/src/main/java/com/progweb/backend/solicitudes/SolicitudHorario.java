package com.progweb.backend.solicitudes;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudHorario(
        @NotNull(message = "Indica la fecha y hora de inicio.") LocalDateTime inicioEn,
        @NotNull(message = "Indica la fecha y hora de fin.") LocalDateTime finEn,
        @NotBlank(message = "Escribe la ubicación o el enlace virtual.")
        @Size(max = 1024, message = "La ubicación no debe superar 1024 caracteres.") String ubicacion) {
}
