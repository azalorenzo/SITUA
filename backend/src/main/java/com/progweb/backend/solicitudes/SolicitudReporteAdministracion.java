package com.progweb.backend.solicitudes;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record SolicitudReporteAdministracion(@NotNull @Positive Long estudianteId,@NotNull @Positive Long tutorId,@NotNull @Positive Long periodoId,@NotNull @Positive Long motivoId,@NotNull @PastOrPresent LocalDate fecha,@NotBlank @Size(max=20000) String contenido,@NotBlank @Pattern(regexp="pendiente|completado") String estado) { }
