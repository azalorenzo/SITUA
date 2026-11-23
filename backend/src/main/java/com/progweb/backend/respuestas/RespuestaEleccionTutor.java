package com.progweb.backend.respuestas;
import java.util.List;
public record RespuestaEleccionTutor(List<Periodo> periodos,Long periodoId,Tutor asignado,
    List<Tutor> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas) {
    public record Periodo(long id,String nombre) { }
    public record Tutor(long id,String nombre,String departamento) { }
}
