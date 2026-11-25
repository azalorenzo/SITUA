package com.progweb.backend.respuestas;
import java.util.List;
import java.util.Map;
public record PaginaAdministracion<T>(List<T> contenido,int pagina,int tamano,long total,int paginas,Map<String,Long> resumen) {
 public PaginaAdministracion(List<T> contenido,int pagina,int tamano,long total,int paginas){this(contenido,pagina,tamano,total,paginas,Map.of());}
}
