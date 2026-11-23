package com.progweb.backend.respuestas;
import java.util.List;
import java.time.LocalDateTime;
public record ResumenSesionesEstudiante(List<RespuestaSesionAgenda> proximas,List<Actividad> actividades) {
    public record Actividad(long id,String tipo,String tema,String estado,LocalDateTime ocurridoEn) { }
}
