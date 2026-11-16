package com.progweb.backend.servicios;

import java.util.List;
import com.progweb.backend.repositorios.RepositorioConsultasCoordinacion;
import com.progweb.backend.respuestas.RespuestaTutorCoordinacion;
import com.progweb.backend.respuestas.RespuestaEstudianteCoordinacion;
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

    public List<RespuestaEstudianteCoordinacion> consultarEstudiantes() {
        return repositorio.consultarEstudiantes();
    }
}
