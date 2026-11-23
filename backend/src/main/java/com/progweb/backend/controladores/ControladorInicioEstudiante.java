package com.progweb.backend.controladores;
import java.util.Map;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudActividadPersonal;
import com.progweb.backend.servicios.ServicioInicioEstudiante;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/estudiante")
public class ControladorInicioEstudiante {
    private final ServicioInicioEstudiante servicio;
    public ControladorInicioEstudiante(ServicioInicioEstudiante servicio){this.servicio=servicio;}
    @GetMapping("/inicio") public ResponseEntity<RespuestaInicioEstudiante> consultar(@AuthenticationPrincipal Jwt token){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject()));}
    @PostMapping("/actividades") public ResponseEntity<RespuestaActividadPersonal> crear(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudActividadPersonal solicitud){return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.crear(token.getSubject(),solicitud));}
    @PutMapping("/actividades/{id}") public ResponseEntity<RespuestaActividadPersonal> editar(@AuthenticationPrincipal Jwt token,@PathVariable long id,@Valid @RequestBody SolicitudActividadPersonal solicitud){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.editar(token.getSubject(),id,solicitud));}
    @DeleteMapping("/actividades/{id}") public ResponseEntity<Map<String,Boolean>> eliminar(@AuthenticationPrincipal Jwt token,@PathVariable long id){servicio.eliminar(token.getSubject(),id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("eliminado",true));}
}
