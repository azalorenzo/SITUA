package com.progweb.backend.respuestas;

import java.math.BigDecimal;

// Promedio de las mediciones de los estudiantes del tutor en un mes. mes tiene el formato AAAA-MM.
public record RespuestaPuntoRendimiento(String mes, BigDecimal promedio) { }
