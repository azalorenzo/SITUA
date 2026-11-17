package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.*;

public record FiltrosEstudiantes(
        @Min(value=1,message="La página debe ser mayor que cero.") Integer pagina,
        @Min(value=1,message="El tamaño debe ser mayor que cero.") @Max(value=100,message="El tamaño no debe superar 100.") Integer tamano,
        @Size(max=160,message="La búsqueda no debe superar 160 caracteres.") String busqueda,
        @Pattern(regexp="todos|activo|inactivo",message="Selecciona un estado válido.") String estado) {
    public int paginaActual() { return pagina==null?1:pagina; }
    public int tamanoActual() { return tamano==null?10:tamano; }
    public String texto() { return busqueda==null?"":busqueda.strip(); }
    public String estadoCuenta() { return estado==null||estado.equals("todos")?null:estado; }
}
