package com.progweb.backend.respuestas;

import java.math.BigDecimal;
import java.util.List;

// Datos de la pantalla de reportes del tutor, del periodo activo salvo la lista de reportes, que incluye todos los suyos.
// asistenciaPorcentaje, promedioGeneral y horasTutoria llegan nulos cuando todavía no hay datos para calcularlos.
public record RespuestaPanelReportesTutor(String tutor, List<RespuestaReporteTutor> reportes, long estudiantesAtendidos,
        Integer asistenciaPorcentaje, BigDecimal promedioGeneral, BigDecimal horasTutoria,
        List<RespuestaPuntoRendimiento> rendimiento, List<RespuestaProgresoEstudiante> progreso) { }
