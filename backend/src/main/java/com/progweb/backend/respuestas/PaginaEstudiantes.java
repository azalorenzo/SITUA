package com.progweb.backend.respuestas;

import java.math.BigDecimal;
import java.util.List;

public record PaginaEstudiantes(List<RespuestaEstudianteCoordinacion> contenido,
        int pagina,int tamano,long totalRegistros,int totalPaginas,Resumen resumen,
        List<NivelRiesgo> distribucionRiesgo,List<Semestre> distribucionSemestres,List<EstudianteEnRiesgo> enRiesgo) {
    public record Resumen(long total,long activos,BigDecimal promedio,long conPromedio,long enRiesgo,long sinEvaluacion,long sinSemestre) { }
    public record NivelRiesgo(String codigo,long cantidad) { }
    public record Semestre(String semestre,long cantidad) { }
    public record EstudianteEnRiesgo(long id,String nombre,String matricula,String periodo,String nivelRiesgo) { }
}
