package com.progweb.backend.respuestas;

public record RespuestaEstudianteAsignable(Long id, String nombre, String matricula,
        String carrera, Long asignacionActivaId) { }
