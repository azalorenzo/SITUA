package com.progweb.backend.controladores;

import jakarta.validation.Valid;
import com.progweb.backend.respuestas.RespuestaPerfilUsuario;
import com.progweb.backend.servicios.ServicioPerfilUsuario;
import com.progweb.backend.solicitudes.SolicitudEditarPerfil;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class ControladorPerfilUsuario {
    private final ServicioPerfilUsuario servicio;
    public ControladorPerfilUsuario(ServicioPerfilUsuario servicio){this.servicio=servicio;}
    @GetMapping
    public ResponseEntity<RespuestaPerfilUsuario> consultar(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject()));
    }
    @PutMapping
    public ResponseEntity<RespuestaPerfilUsuario> guardar(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudEditarPerfil solicitud) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.guardar(token.getSubject(),solicitud));
    }
}
