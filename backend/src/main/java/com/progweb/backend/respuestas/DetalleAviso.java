package com.progweb.backend.respuestas;
import java.util.List;
public record DetalleAviso(RespuestaAviso aviso,List<RespuestaDestinatarioAviso> destinatarios) { }
