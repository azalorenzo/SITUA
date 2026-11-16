package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.*;
public record SolicitudCrearTutor(
        @NotBlank(message="Escribe el nombre.") @Size(max=160, message="El nombre es demasiado largo.") String nombre,
        @NotBlank(message="Escribe el correo.") @Email(message="Escribe un correo válido.") @Size(max=254) String correo,
        @NotBlank(message="Escribe una contraseña.") @Size(min=8, max=72, message="Usa entre 8 y 72 caracteres.")
        @Pattern(regexp="^(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$", message="Incluye una mayúscula, un número y un carácter especial.") String contrasena,
        @NotNull(message="Selecciona un departamento.") @Positive Long departamentoId) {
    @Override public String toString() { return "SolicitudCrearTutor[contrasena=oculta]"; }
}
