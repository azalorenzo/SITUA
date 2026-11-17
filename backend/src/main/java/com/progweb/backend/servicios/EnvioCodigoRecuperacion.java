package com.progweb.backend.servicios;
public interface EnvioCodigoRecuperacion {
    boolean habilitado();
    void enviar(String correo,String codigo);
}
