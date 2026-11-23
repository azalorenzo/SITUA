package com.progweb.backend.solicitudes;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
public record SolicitudCerrarSesion(@NotBlank @Pattern(regexp="completada|no_asistio") String estado,
    LocalDateTime inicioRealEn,LocalDateTime finRealEn,@Size(max=10000) String observaciones) { }
