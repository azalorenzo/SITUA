package com.progweb.backend.respuestas;
import java.util.List;
public record RespuestaInicioTutor(Indicadores indicadores,List<Estudiante> estudiantes,List<RespuestaSesionAgenda> proximas) {
    public record Indicadores(long estudiantes,long realizadas,long reportesPendientes,long programadas) { }
    public record Estudiante(long id,String nombre,String carrera,String nivelRiesgo) { }
}
