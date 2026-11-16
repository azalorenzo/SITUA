package com.progweb.backend.respuestas;
import java.time.LocalDateTime;
public record RespuestaAviso(Long id,Long autorId,String autor,String titulo,String descripcion,String categoria,
        String prioridad,LocalDateTime publicadoEn,LocalDateTime retiradoEn,long destinatarios,long lecturas) { }
