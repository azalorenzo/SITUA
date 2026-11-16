package com.progweb.backend.respuestas;
import java.time.LocalDateTime;
public record RespuestaDestinatarioAviso(Long usuarioId,String nombre,String correo,String estado,
        LocalDateTime leidoEn,LocalDateTime ocultadoEn) { }
