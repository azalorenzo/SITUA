package com.progweb.backend.controladores;
import jakarta.validation.Valid;
import com.progweb.backend.servicios.ServicioPerfilEstudiante;
import com.progweb.backend.solicitudes.SolicitudCompletarPerfil;
import com.progweb.backend.respuestas.RespuestaPerfilEstudiante;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/estudiante/perfil")
public class ControladorPerfilEstudiante {
    private final ServicioPerfilEstudiante servicio;
    public ControladorPerfilEstudiante(ServicioPerfilEstudiante servicio) { this.servicio=servicio; }
    @GetMapping
    public ResponseEntity<RespuestaPerfilEstudiante> consultar(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject()));
    }
    @PutMapping
    public ResponseEntity<RespuestaPerfilEstudiante> completar(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudCompletarPerfil solicitud) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.completar(token.getSubject(),solicitud));
    }
}
