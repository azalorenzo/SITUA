package com.progweb.backend.controladores;

import java.util.List;
import com.progweb.backend.respuestas.RespuestaTutorCoordinacion;
import com.progweb.backend.respuestas.RespuestaEstudianteCoordinacion;
import com.progweb.backend.servicios.ServicioConsultasCoordinacion;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coordinacion")
public class ControladorConsultasCoordinacion {
    private final ServicioConsultasCoordinacion consultas;

    public ControladorConsultasCoordinacion(ServicioConsultasCoordinacion consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/estudiantes")
    public ResponseEntity<List<RespuestaEstudianteCoordinacion>> consultarEstudiantes() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultas.consultarEstudiantes());
    }
}
