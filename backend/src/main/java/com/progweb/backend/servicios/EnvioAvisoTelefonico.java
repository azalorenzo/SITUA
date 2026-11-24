package com.progweb.backend.servicios;

import java.time.LocalDateTime;

public interface EnvioAvisoTelefonico {
    record Confirmacion(String tutor,LocalDateTime inicio,String lugar) { }
    record Resultado(String estado,String identificador,String codigo) { }
    boolean habilitado(String canal);
    Resultado enviar(String canal,String telefono,Confirmacion confirmacion);
}
