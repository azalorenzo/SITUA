package com.progweb.backend.controladores;

import jakarta.validation.Valid;
import com.progweb.backend.servicios.ServicioContactoEstudiante;
import com.progweb.backend.solicitudes.SolicitudContactoEstudiante;
import com.progweb.backend.respuestas.RespuestaContactoEstudiante;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estudiante/contacto")
public class ControladorContactoEstudiante {
    private final ServicioContactoEstudiante servicio;
    public ControladorContactoEstudiante(ServicioContactoEstudiante servicio) { this.servicio=servicio; }
    @GetMapping
    public ResponseEntity<RespuestaContactoEstudiante> consultar(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject()));
    }
    @PutMapping
    public ResponseEntity<RespuestaContactoEstudiante> guardar(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudContactoEstudiante solicitud) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.guardar(token.getSubject(),solicitud));
    }
}
