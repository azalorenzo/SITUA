package com.progweb.backend.servicios;

import java.time.*;
import java.util.List;
import java.util.Map;
import com.progweb.backend.repositorios.RepositorioInicioCoordinacion;
import com.progweb.backend.respuestas.RespuestaInicioCoordinacion;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class InicioSinDatosPruebas {
    @Test void resumenSinRegistrosMantieneCerosYSeisMeses(){
        var repositorio=mock(RepositorioInicioCoordinacion.class);
        when(repositorio.indicadores()).thenReturn(new RespuestaInicioCoordinacion.Indicadores(0,0,0,0,0));
        when(repositorio.motivos(0)).thenReturn(List.of());
        when(repositorio.tutoriasCompletadas(any(),any())).thenReturn(Map.of());
        var servicio=new ServicioInicioCoordinacion(repositorio,Clock.fixed(Instant.parse("2027-01-31T23:59:59Z"),ZoneOffset.UTC));
        var respuesta=servicio.consultar();
        assertThat(respuesta.indicadores().totalEstudiantes()).isZero();assertThat(respuesta.motivos()).isEmpty();
        assertThat(respuesta.mensual()).hasSize(6).allMatch(mes->mes.cantidad()==0);
        assertThat(respuesta.mensual().getFirst().mes()).isEqualTo("2026-08");assertThat(respuesta.mensual().getLast().mes()).isEqualTo("2027-01");
    }
}
