package com.progweb.backend.servicios;

import java.util.List;
import com.progweb.backend.respuestas.PaginaEstudiantes;
import com.progweb.backend.solicitudes.FiltrosEstudiantes;
import com.progweb.backend.repositorios.RepositorioConsultasCoordinacion;
import com.progweb.backend.respuestas.RespuestaTutorCoordinacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioConsultasCoordinacion {
    private final RepositorioConsultasCoordinacion repositorio;

    public ServicioConsultasCoordinacion(RepositorioConsultasCoordinacion repositorio) {
        this.repositorio = repositorio;
    }

    public List<RespuestaTutorCoordinacion> consultarTutores() {
        return repositorio.consultarTutores();
    }

    public PaginaEstudiantes consultarEstudiantes(FiltrosEstudiantes filtros) {
        long total=repositorio.contarEstudiantes(filtros);
        int paginas=(int)((total+filtros.tamanoActual()-1)/filtros.tamanoActual());
        int pagina=Math.min(filtros.paginaActual(),Math.max(1,paginas));
        // Las tarjetas y gráficas siguen mostrando el resumen general, como antes.
        return new PaginaEstudiantes(repositorio.consultarEstudiantes(filtros,pagina),pagina,filtros.tamanoActual(),total,paginas,
            repositorio.resumenEstudiantes(),repositorio.riesgoEstudiantes(),repositorio.semestresEstudiantes(),repositorio.estudiantesEnRiesgo());
    }
}
