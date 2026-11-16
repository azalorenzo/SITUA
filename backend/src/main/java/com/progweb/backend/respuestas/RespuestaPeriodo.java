package com.progweb.backend.respuestas;

import java.time.LocalDate;
public record RespuestaPeriodo(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, String estado) { }
