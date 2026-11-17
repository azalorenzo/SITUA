package com.progweb.backend.respuestas;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record RespuestaReporte(Long id,Long estudianteId,String estudiante,String matricula,Long tutorId,String tutor,
        Long periodoId,String periodo,String carrera,int semestre,Long motivoId,String motivo,
        LocalDate fecha,String estado,String contenido,LocalDateTime completadoEn,String completadoPor) { }
