package com.progweb.backend.solicitudes;

import jakarta.validation.constraints.*;
public record FiltrosTutorados(@Min(1) Integer pagina,@Min(1) @Max(100) Integer tamano,
        @Size(max=200) String busqueda,@Pattern(regexp="todos|alto|medio|bajo|sin_riesgo|sin_evaluar") String riesgo){
    public FiltrosTutorados{pagina=pagina==null?1:pagina;tamano=tamano==null?10:tamano;busqueda=busqueda==null?"":busqueda.strip();riesgo=riesgo==null?"todos":riesgo.strip().toLowerCase(java.util.Locale.ROOT);}
}
