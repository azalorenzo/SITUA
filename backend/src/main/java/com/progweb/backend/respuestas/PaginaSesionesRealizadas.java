package com.progweb.backend.respuestas;
import java.math.BigDecimal;
import java.util.List;
public record PaginaSesionesRealizadas(List<RespuestaSesionRealizada> contenido,int pagina,int tamano,long totalRegistros,int totalPaginas,Resumen resumen) {
    public record Resumen(long total,long completadas,long canceladas,long noAsistio,BigDecimal horas) { }
}
