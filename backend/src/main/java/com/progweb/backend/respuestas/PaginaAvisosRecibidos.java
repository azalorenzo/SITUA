package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaAvisosRecibidos(List<RespuestaAvisoRecibido> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas,long noLeidos,long importantes){ }
