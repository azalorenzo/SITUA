package com.progweb.backend.respuestas;
import java.util.List;
public record RespuestaPerfilEstudiante(boolean completo,List<Opcion> carreras,List<Opcion> periodos) {
    public record Opcion(long id,String nombre) { }
}
