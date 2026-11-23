package com.progweb.backend.respuestas;
import java.time.LocalDateTime;
public record RespuestaSesionAgenda(long id,Long disponibilidadId,String tutor,String estudiante,String tema,
    LocalDateTime inicioEn,LocalDateTime finEn,String modalidad,String ubicacion,String enlaceVirtual,
    String estado,long periodoId,String periodo,String mensajeSolicitud,boolean puedeCancelar,boolean puedeResponder,boolean puedeRegistrar,String observaciones,long duracionMinutos) { }
