package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaSesiones(List<RespuestaSesionAgenda> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas) { }
