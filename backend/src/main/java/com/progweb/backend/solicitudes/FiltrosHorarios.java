package com.progweb.backend.solicitudes;
import java.time.*;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;
public record FiltrosHorarios(@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta){
    @AssertTrue(message="Selecciona un intervalo válido de hasta 62 días.")
    public boolean isIntervaloValido(){return desde==null&&hasta==null||desde!=null&&hasta!=null&&hasta.isAfter(desde)&&Duration.between(desde,hasta).compareTo(Duration.ofDays(62))<=0;}
}
