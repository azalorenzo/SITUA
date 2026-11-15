package com.progweb.backend.errores;

import java.util.List;
import java.util.Map;

public class SolicitudRechazada extends RuntimeException {
    private final int estado;
    private final Map<String, List<String>> erroresPorCampo;

    public SolicitudRechazada(int estado, String mensaje, String campo) {
        super(mensaje);
        this.estado = estado;
        this.erroresPorCampo = Map.of(campo, List.of(mensaje));
    }

    public int obtenerEstado() { return estado; }
    public Map<String, List<String>> obtenerErroresPorCampo() { return erroresPorCampo; }
}
