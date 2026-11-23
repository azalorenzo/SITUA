package com.progweb.backend.controladores;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.RespuestaEleccionTutor;
import com.progweb.backend.solicitudes.SolicitudElegirTutor;
import com.progweb.backend.servicios.ServicioEleccionTutor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/estudiante/eleccion-tutor")
public class ControladorEleccionTutor {
    private final ServicioEleccionTutor servicio;
    public ControladorEleccionTutor(ServicioEleccionTutor servicio) { this.servicio=servicio; }
    @GetMapping
    public ResponseEntity<RespuestaEleccionTutor> consultar(@AuthenticationPrincipal Jwt token,
        @RequestParam(required=false) Long periodoId,@RequestParam(defaultValue="1") int pagina,
        @RequestParam(defaultValue="5") int tamano,@RequestParam(defaultValue="") String busqueda) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject(),periodoId,pagina,tamano,busqueda));
    }
    @PostMapping
    public ResponseEntity<RespuestaEleccionTutor> elegir(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudElegirTutor solicitud) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.elegir(token.getSubject(),solicitud));
    }
}
