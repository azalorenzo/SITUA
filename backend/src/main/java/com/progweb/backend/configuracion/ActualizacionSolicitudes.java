package com.progweb.backend.configuracion;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import com.progweb.backend.servicios.ServicioVencimientoSesiones;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="tutoria.sesiones.vencimiento-automatico", havingValue="true", matchIfMissing=true)
public class ActualizacionSolicitudes {
    private final ServicioVencimientoSesiones vencimiento;
    public ActualizacionSolicitudes(ServicioVencimientoSesiones vencimiento) { this.vencimiento = vencimiento; }

    @Scheduled(fixedDelay=30000)
    public void actualizar() {
        vencimiento.vencerPendientes(LocalDateTime.now(ZoneOffset.UTC));
    }
}
