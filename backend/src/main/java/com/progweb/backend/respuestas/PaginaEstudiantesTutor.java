package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaEstudiantesTutor(List<RespuestaEstudianteTutor> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas){ }
