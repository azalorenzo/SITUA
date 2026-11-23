package com.progweb.backend.respuestas;
import java.util.List;
public record RespuestaInicioEstudiante(RespuestaSesionAgenda proxima,List<RespuestaSesionAgenda> virtuales,
    List<RespuestaActividadPersonal> talleres,List<RespuestaActividadPersonal> recordatorios) { }
