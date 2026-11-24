package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.*;

public record SolicitudContactoEstudiante(
        @Size(max=16) @Pattern(regexp="^$|^\\+[1-9][0-9]{7,14}$",message="Escribe el número con código de país, por ejemplo +525512345678.") String telefono,
        @NotNull Boolean aceptaSms,@NotNull Boolean aceptaWhatsapp) {
    public SolicitudContactoEstudiante { telefono=telefono==null?"":telefono.strip(); }
    @AssertTrue(message="Escribe tu teléfono para recibir avisos.")
    public boolean isTelefonoParaAvisos() { return !Boolean.TRUE.equals(aceptaSms)&&!Boolean.TRUE.equals(aceptaWhatsapp)||!telefono.isEmpty(); }
}
