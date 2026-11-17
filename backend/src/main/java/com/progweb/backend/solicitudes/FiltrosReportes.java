package com.progweb.backend.solicitudes;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
public record FiltrosReportes(
        @Min(value=1,message="La página debe ser mayor que cero.") Integer pagina,
        @Min(1) @Max(value=100,message="El tamaño no debe superar 100.") Integer tamano,
        @Size(max=160,message="La búsqueda no debe superar 160 caracteres.") String busqueda,
        @Pattern(regexp="pendiente|completado",message="Selecciona un estado válido.") String estado,
        @Positive Long periodoId,@Positive Long motivoId,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
    public FiltrosReportes {
        if(pagina==null)pagina=1;if(tamano==null)tamano=5;if(busqueda==null)busqueda="";
    }
}
