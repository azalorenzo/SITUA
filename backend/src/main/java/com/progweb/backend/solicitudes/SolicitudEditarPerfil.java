package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record SolicitudEditarPerfil(
        @NotBlank(message="Escribe tu nombre.") @Size(max=160) String nombre,
        @NotBlank(message="Escribe tu correo.") @Email(message="Escribe un correo válido.") @Size(max=254) String correo,
        @Size(max=16) @Pattern(regexp="^$|^\\+[1-9][0-9]{7,14}$",message="Escribe el teléfono con código de país.") String telefono,
        @Pattern(regexp="[\\p{L}\\p{N}_-]{3,30}",message="Escribe una matrícula de 3 a 30 caracteres.") String matricula,
        @Positive Long carreraId,@Positive Long periodoId,@Min(1) @Max(value=12,message="El semestre debe estar entre 1 y 12.") Integer semestre,
        @Positive Long departamentoId,@NotNull Boolean aceptaSms) {
    public SolicitudEditarPerfil {
        nombre=nombre==null?null:nombre.strip();
        correo=correo==null?null:correo.strip().toLowerCase(Locale.ROOT);
        telefono=telefono==null?"":telefono.strip();
        matricula=matricula==null?null:matricula.strip();
    }
}
