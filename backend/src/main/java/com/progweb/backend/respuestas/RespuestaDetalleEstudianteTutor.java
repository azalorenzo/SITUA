package com.progweb.backend.respuestas;

import java.util.List;

// Detalle de un estudiante del tutor: sus datos, el historial de sesiones completadas y las observaciones del tutor.
public record RespuestaDetalleEstudianteTutor(RespuestaEstudianteTutor estudiante,
        List<RespuestaSesionEstudiante> sesiones, List<RespuestaObservacionEstudiante> observaciones) { }
