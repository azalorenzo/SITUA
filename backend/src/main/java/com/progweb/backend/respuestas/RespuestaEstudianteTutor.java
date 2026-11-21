package com.progweb.backend.respuestas;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Estudiante con asignación activa del tutor en el periodo activo. Las fechas están en UTC sin zona.
// nivelRiesgo es sin_riesgo, bajo, medio o alto; llega nulo cuando todavía no hay medición académica.
public record RespuestaEstudianteTutor(Long id, String nombre, String correo, String telefono, String matricula,
        String carrera, Integer semestre, BigDecimal promedio, String nivelRiesgo, String condicion, String periodo,
        long sesionesCompletadas, LocalDateTime ultimaSesion) { }
