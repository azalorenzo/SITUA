package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record SolicitudCambiarContrasena(@NotBlank @Email @Size(max=254) String correo,
    @NotBlank @Pattern(regexp="[0-9]{6}") String codigo,
    @NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$",message="Incluye una mayúscula, un número y un carácter especial.") String contrasena,@NotBlank String confirmacion) { }
