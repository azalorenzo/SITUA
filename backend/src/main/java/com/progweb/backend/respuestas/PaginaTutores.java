package com.progweb.backend.respuestas;
import java.util.List;
public record PaginaTutores(List<RespuestaTutorCoordinacion> contenido, int pagina, int tamano,
        long totalRegistros, int totalPaginas, Resumen resumen) {
    public record Resumen(long total, long activos, long inactivos) { }
}
