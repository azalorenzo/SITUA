package com.progweb.backend.controladores;
import com.progweb.backend.respuestas.RespuestaInicioTutor;
import com.progweb.backend.servicios.ServicioInicioTutor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/tutor/inicio")
public class ControladorInicioTutor {
    private final ServicioInicioTutor servicio;
    public ControladorInicioTutor(ServicioInicioTutor servicio){this.servicio=servicio;}
    @GetMapping public ResponseEntity<RespuestaInicioTutor> consultar(@AuthenticationPrincipal Jwt token){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject()));}
}
