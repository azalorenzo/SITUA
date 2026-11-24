package com.progweb.backend.controladores;

import java.util.Map;
import com.progweb.backend.servicios.ServicioCierreSesion;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autenticacion")
public class ControladorCierreSesion {
    private final ServicioCierreSesion servicio;
    public ControladorCierreSesion(ServicioCierreSesion servicio){this.servicio=servicio;}
    @PostMapping("/cerrar-sesion") public ResponseEntity<Map<String,String>> cerrar(@AuthenticationPrincipal Jwt token){
        servicio.cerrar(token);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensaje","Sesión cerrada."));
    }
}
