package com.progweb.backend.respuestas;

import java.math.BigDecimal;

// Primer y último promedio medido de un estudiante en el periodo activo.
public record RespuestaProgresoEstudiante(String estudiante, BigDecimal inicial, BigDecimal actual) { }
