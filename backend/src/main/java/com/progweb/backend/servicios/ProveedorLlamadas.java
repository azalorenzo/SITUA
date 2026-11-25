package com.progweb.backend.servicios;

import java.time.Instant;

public interface ProveedorLlamadas {
    record Sala(String nombre,String direccion) {}
    boolean habilitado();
    Sala crearSala(String nombre,String tipo,Instant vencimiento);
    String crearAcceso(String sala,String tipo,long usuario,String nombre,Instant vencimiento);
    boolean cerrarSala(String sala,long iniciador,long receptor);
}
