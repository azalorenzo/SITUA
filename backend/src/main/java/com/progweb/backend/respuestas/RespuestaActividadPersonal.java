package com.progweb.backend.respuestas;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record RespuestaActividadPersonal(long id,String titulo,String descripcion,String tipo,LocalDate fecha,
    LocalDateTime inicioEn,LocalDateTime finEn,String responsable,String ubicacion,String estado) { }
