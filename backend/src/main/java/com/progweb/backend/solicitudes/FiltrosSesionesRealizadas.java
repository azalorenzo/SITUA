package com.progweb.backend.solicitudes;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
public record FiltrosSesionesRealizadas(@Min(1) Integer pagina,@Min(1) @Max(100) Integer tamano,
    @Size(max=160) String busqueda,@Pattern(regexp="completada|cancelada|no_asistio") String estado,
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
    public FiltrosSesionesRealizadas {if(pagina==null)pagina=1;if(tamano==null)tamano=5;if(busqueda==null)busqueda="";}
}
