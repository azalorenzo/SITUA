package com.progweb.backend.controladores;

import com.progweb.backend.respuestas.PaginaEstudiantes;
import com.progweb.backend.solicitudes.FiltrosEstudiantes;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    public ResponseEntity<PaginaEstudiantes> consultarEstudiantes(@Valid @ModelAttribute FiltrosEstudiantes filtros) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultas.consultarEstudiantes(filtros));
    }
}
