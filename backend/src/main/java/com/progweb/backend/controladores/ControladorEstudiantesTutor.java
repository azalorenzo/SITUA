package com.progweb.backend.controladores;

import java.util.List;
import com.progweb.backend.respuestas.RespuestaDetalleEstudianteTutor;
import com.progweb.backend.respuestas.RespuestaEstudianteTutor;
import com.progweb.backend.servicios.ServicioEstudiantesTutor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// Estudiantes del tutor conectado. No reciben el identificador del tutor: se toma del JWT.
@RestController
@RequestMapping("/api/tutor/estudiantes")
public class ControladorEstudiantesTutor {
    private final ServicioEstudiantesTutor servicio;

    public ControladorEstudiantesTutor(ServicioEstudiantesTutor servicio) { this.servicio = servicio; }

    @GetMapping
    public ResponseEntity<List<RespuestaEstudianteTutor>> consultar(@AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue="") String busqueda, @RequestParam(defaultValue="todos") String riesgo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject(), busqueda, riesgo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaDetalleEstudianteTutor> consultarDetalle(@PathVariable long id, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultarDetalle(token.getSubject(), id));
    }
}
