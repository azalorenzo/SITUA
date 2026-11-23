package com.progweb.backend.solicitudes;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record SolicitudReporteTutor(@NotNull @Positive Long estudianteId,@NotNull @Positive Long periodoId,
    @Positive Long sesionId,@NotNull @Positive Long motivoId,@NotNull LocalDate fecha,
    @NotBlank @Size(max=20000) String contenido,@NotBlank @Pattern(regexp="pendiente|completado") String estado) { }
