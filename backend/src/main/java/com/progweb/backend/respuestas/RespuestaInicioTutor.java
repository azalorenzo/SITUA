package com.progweb.backend.respuestas;

import java.util.List;

// Resumen del inicio del tutor, del periodo activo. estudiantesDestacados trae primero a quienes tienen mayor riesgo.
public record RespuestaInicioTutor(long estudiantes, long sesionesRealizadas, long reportesPendientes,
        long sesionesProgramadas, List<RespuestaEstudianteInicio> estudiantesDestacados) { }
