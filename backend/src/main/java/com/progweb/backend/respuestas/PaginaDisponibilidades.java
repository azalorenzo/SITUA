package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaDisponibilidades(List<RespuestaHorario> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas) { }
