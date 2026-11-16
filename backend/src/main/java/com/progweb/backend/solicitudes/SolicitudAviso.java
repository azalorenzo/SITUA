package com.progweb.backend.solicitudes;

import java.util.List;
import jakarta.validation.constraints.*;
public record SolicitudAviso(
        @NotBlank(message="Escribe el título.") @Size(max=200,message="El título no debe superar 200 caracteres.") String titulo,
        @NotBlank(message="Escribe la descripción.") @Size(max=10000,message="La descripción no debe superar 10000 caracteres.") String descripcion,
        @NotBlank(message="Selecciona una categoría.") @Size(max=40) String categoria,
        @NotBlank @Pattern(regexp="normal|media|alta",message="Selecciona una prioridad válida.") String prioridad,
        @NotBlank(message="Selecciona los destinatarios.") @Pattern(regexp="todos|estudiantes|tutores|seleccion|conservar",message="Selecciona destinatarios válidos.") String destinatarios,
        @Size(max=500,message="Selecciona hasta 500 usuarios por aviso.") List<@NotNull @Positive Long> usuarioIds) { }
