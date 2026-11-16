package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaDestinatariosAviso(List<RespuestaDestinatarioAviso> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas) { }
