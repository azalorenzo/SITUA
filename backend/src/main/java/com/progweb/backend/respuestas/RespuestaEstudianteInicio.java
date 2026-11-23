package com.progweb.backend.respuestas;

// Estudiante que se muestra en el inicio del tutor. nivelRiesgo llega nulo si aún no hay medición.
public record RespuestaEstudianteInicio(Long id, String nombre, String carrera, String nivelRiesgo) { }
