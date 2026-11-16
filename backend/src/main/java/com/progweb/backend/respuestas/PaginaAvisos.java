package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaAvisos(List<RespuestaAviso> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas,Resumen resumen) {
    public record Resumen(long total,long vigentes,long retirados,long importantes) { }
}
