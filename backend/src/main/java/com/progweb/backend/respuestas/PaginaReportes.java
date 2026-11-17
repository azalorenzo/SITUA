package com.progweb.backend.respuestas;
import java.util.List;
import java.math.BigDecimal;
public record PaginaReportes(List<RespuestaReporte> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas,
        Resumen resumen,List<Motivo> motivos) {
    public record Resumen(long total,long completados,long pendientes) { }
    public record Motivo(Long id,String nombre,long cantidad,BigDecimal porcentaje) { }
}
