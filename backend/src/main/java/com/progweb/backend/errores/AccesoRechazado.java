package com.progweb.backend.errores;

public class AccesoRechazado extends RuntimeException {
    private final int estado;

    public AccesoRechazado(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public int obtenerEstado() {
        return estado;
    }
}
