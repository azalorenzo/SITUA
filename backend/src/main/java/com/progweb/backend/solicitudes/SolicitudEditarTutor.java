package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.*;
public record SolicitudEditarTutor(
        @NotBlank(message="Escribe el nombre.") @Size(max=160, message="El nombre es demasiado largo.") String nombre,
        @NotBlank(message="Escribe el correo.") @Email(message="Escribe un correo válido.") @Size(max=254) String correo,
        @NotNull(message="Selecciona un departamento.") @Positive Long departamentoId) { }
