package com.progweb.backend.solicitudes;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
public record SolicitudTutoriaAdministracion(@NotNull @Positive Long estudianteId,@NotNull @Positive Long tutorId,@NotNull @Positive Long periodoId,@NotBlank @Size(max=200) String tema,@NotNull LocalDateTime inicioEn,@NotNull LocalDateTime finEn,@NotBlank @Pattern(regexp="presencial|virtual") String modalidad,@NotBlank @Size(max=1024) String lugar,@Size(max=10000) String observaciones) { }
