package com.progweb.backend.controladores;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import com.progweb.backend.respuestas.RespuestaHorario;
import com.progweb.backend.solicitudes.SolicitudHorario;
import com.progweb.backend.servicios.ServicioHorarios;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutor")
public class ControladorHorarios {
    private final ServicioHorarios servicio;

    public ControladorHorarios(ServicioHorarios servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/horarios")
    public ResponseEntity<List<RespuestaHorario>> listar(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.listarPropios(token.getSubject()));
    }

    @PostMapping("/horarios")
    public ResponseEntity<RespuestaHorario> crear(@Valid @RequestBody SolicitudHorario solicitud,
            @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(servicio.crear(solicitud, token.getSubject()));
    }

    @PutMapping("/horarios/{id}")
    public ResponseEntity<RespuestaHorario> editar(@PathVariable long id,
            @Valid @RequestBody SolicitudHorario solicitud, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.editar(id, solicitud, token.getSubject()));
    }

    // Devuelve 200 con un cuerpo pequeño en vez de 204 sin cuerpo, porque solicitarApi
    // (servicio compartido del frontend) espera siempre un JSON y no debía modificarse
    // para este módulo.
    @DeleteMapping("/horarios/{id}")
    public ResponseEntity<Map<String, Boolean>> eliminar(@PathVariable long id, @AuthenticationPrincipal Jwt token) {
        servicio.eliminar(id, token.getSubject());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("eliminado", true));
    }
}
