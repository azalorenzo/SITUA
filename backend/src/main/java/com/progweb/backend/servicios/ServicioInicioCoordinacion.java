package com.progweb.backend.servicios;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import com.progweb.backend.repositorios.RepositorioInicioCoordinacion;
import com.progweb.backend.respuestas.RespuestaInicioCoordinacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ServicioInicioCoordinacion {
    private final RepositorioInicioCoordinacion repositorio;
    private final Clock reloj;
    public ServicioInicioCoordinacion(RepositorioInicioCoordinacion repositorio,Clock relojInicio){this.repositorio=repositorio;this.reloj=relojInicio;}
    public RespuestaInicioCoordinacion consultar(){
        var corte=reloj.instant().truncatedTo(ChronoUnit.MICROS);
        var hasta=LocalDateTime.ofInstant(corte,ZoneOffset.UTC);
        var ultimo=YearMonth.from(hasta);var primero=ultimo.minusMonths(5);
        var indicadores=repositorio.indicadores();
        var cantidades=repositorio.tutoriasCompletadas(primero.atDay(1).atStartOfDay(),hasta);
        var mensual=new ArrayList<RespuestaInicioCoordinacion.Mes>();
        var formato=DateTimeFormatter.ofPattern("MMM uuuu",Locale.forLanguageTag("es-MX"));
        // Los meses sin sesiones también aparecen, con su cantidad en cero.
        for(int indice=0;indice<6;indice++){
            var mes=primero.plusMonths(indice);
            mensual.add(new RespuestaInicioCoordinacion.Mes(mes.toString(),mes.format(formato),cantidades.getOrDefault(mes,0L)));
        }
        return new RespuestaInicioCoordinacion(indicadores,repositorio.motivos(indicadores.totalReportes()),mensual,primero.atDay(1),hasta.toLocalDate(),corte);
    }
}
