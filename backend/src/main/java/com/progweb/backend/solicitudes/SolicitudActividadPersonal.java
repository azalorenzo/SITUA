package com.progweb.backend.solicitudes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
public record SolicitudActividadPersonal(@NotBlank @Size(max=200) String titulo,@Size(max=2000) String descripcion,
    @NotBlank @Pattern(regexp="taller|recordatorio") String tipo,LocalDate fecha,LocalDateTime inicioEn,LocalDateTime finEn,
    @Size(max=160) String responsable,@Size(max=200) String ubicacion) { }
