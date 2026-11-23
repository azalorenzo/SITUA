package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaAvisosEstudiante(List<RespuestaAvisoRecibido> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas,long noLeidos) { }
