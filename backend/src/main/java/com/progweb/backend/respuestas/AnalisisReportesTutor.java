package com.progweb.backend.respuestas;
import java.math.BigDecimal;
import java.util.List;
public record AnalisisReportesTutor(long total,long completados,long pendientes,long estudiantes,
    BigDecimal promedio,BigDecimal horas,BigDecimal asistencia,List<Mes> rendimiento,List<Progreso> progreso) {
    public record Mes(String mes,BigDecimal promedio) { }
    public record Progreso(String estudiante,BigDecimal inicial,BigDecimal actual) { }
}
