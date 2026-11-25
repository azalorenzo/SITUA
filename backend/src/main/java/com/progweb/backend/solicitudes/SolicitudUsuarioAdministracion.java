package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudUsuarioAdministracion(
 @NotBlank @Size(max=160) String nombre,@NotBlank @Email @Size(max=254) String correo,
 @Size(min=8,max=72,message="Usa entre 8 y 72 caracteres.")
 @Pattern(regexp="^(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$",message="Incluye una mayúscula, un número y un carácter especial.") String contrasena,
 @NotBlank @Pattern(regexp="coordinador|tutor|estudiante") String rol,
 @NotBlank @Pattern(regexp="activo|inactivo") String estado,
 @Size(max=30) String matricula,@Positive Long departamentoId) {
 @AssertTrue(message="Escribe la matrícula del estudiante.") public boolean isMatriculaValida(){return !"estudiante".equals(rol)||matricula!=null&&!matricula.isBlank();}
 @AssertTrue(message="Selecciona un departamento para el tutor.") public boolean isDepartamentoValido(){return !"tutor".equals(rol)||departamentoId!=null;}
 @Override public String toString(){return "SolicitudUsuarioAdministracion[contrasena=oculta]";}
}
