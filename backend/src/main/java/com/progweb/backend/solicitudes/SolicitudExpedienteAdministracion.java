package com.progweb.backend.solicitudes;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record SolicitudExpedienteAdministracion(@NotBlank @Size(max=30) String matricula,@NotNull @Positive Long periodoId,@NotNull @Positive Long carreraId,@NotNull @Min(1) @Max(12) Integer semestre,@NotBlank @Pattern(regexp="activo|baja|egresado") String inscripcion,@DecimalMin("0") @DecimalMax("10") @Digits(integer=2,fraction=2) BigDecimal promedio,@Pattern(regexp="sin_riesgo|bajo|medio|alto") String riesgo,@Size(max=10000) String condicion) { }
