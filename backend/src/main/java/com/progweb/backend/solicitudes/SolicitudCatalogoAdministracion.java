package com.progweb.backend.solicitudes;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record SolicitudCatalogoAdministracion(@NotBlank @Size(max=150) String nombre,
 @Size(max=30) String clave,@Positive Long departamentoId,
 @NotBlank @Pattern(regexp="activo|inactivo|planeado|cerrado") String estado,LocalDate inicio,LocalDate fin) {
 @AssertTrue(message="La fecha de fin debe ser igual o posterior al inicio.") public boolean isFechasValidas(){return inicio==null&&fin==null||inicio!=null&&fin!=null&&!fin.isBefore(inicio);}
}
