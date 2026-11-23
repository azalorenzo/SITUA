package com.progweb.backend.respuestas;

import java.time.LocalDate;

// Reporte de un estudiante del tutor. estado es pendiente o completado.
public record RespuestaReporteTutor(Long id, String estudiante, LocalDate fecha, String motivo, String estado) { }
