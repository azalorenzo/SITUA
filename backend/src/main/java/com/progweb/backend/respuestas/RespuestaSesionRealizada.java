package com.progweb.backend.respuestas;
import java.time.LocalDateTime;
public record RespuestaSesionRealizada(long id,long estudianteId,String estudiante,long periodoId,String periodo,
    String tema,LocalDateTime inicioEn,LocalDateTime finEn,String estado,String ubicacion,String observaciones,long duracionMinutos) { }
