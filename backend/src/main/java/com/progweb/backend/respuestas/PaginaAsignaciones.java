package com.progweb.backend.respuestas;

import java.util.List;
public record PaginaAsignaciones(List<RespuestaAsignacion> contenido, int pagina, int tamano,
        long totalRegistros, int totalPaginas, ResumenAsignaciones resumen) { }
