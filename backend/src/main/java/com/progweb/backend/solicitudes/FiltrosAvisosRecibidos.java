package com.progweb.backend.solicitudes;
import jakarta.validation.constraints.*;
public record FiltrosAvisosRecibidos(@Min(1) Integer pagina,@Min(1) @Max(100) Integer tamano,
        @Size(max=200) String busqueda,@Pattern(regexp="todos|leidos|no_leidos") String lectura,
        @Pattern(regexp="todos|normal|media|alta") String prioridad){
    public FiltrosAvisosRecibidos{pagina=pagina==null?1:pagina;tamano=tamano==null?5:tamano;busqueda=busqueda==null?"":busqueda.strip();lectura=lectura==null?"todos":lectura;prioridad=prioridad==null?"todos":prioridad;}
}
