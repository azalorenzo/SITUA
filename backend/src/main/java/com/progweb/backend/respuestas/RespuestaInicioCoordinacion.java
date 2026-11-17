package com.progweb.backend.respuestas;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
public record RespuestaInicioCoordinacion(Indicadores indicadores,List<Motivo> motivos,List<Mes> mensual,
        LocalDate desde,LocalDate hasta,Instant actualizadoEn) {
    public record Indicadores(long totalEstudiantes,long tutoresActivos,long estudiantesEnRiesgo,long sinEvaluacion,long totalReportes) { }
    public record Motivo(Long id,String nombre,long cantidad,BigDecimal porcentaje) { }
    public record Mes(String mes,String etiqueta,long cantidad) { }
}
