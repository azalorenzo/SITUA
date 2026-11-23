package com.progweb.backend.controladores;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.servicios.ServicioAvisosEstudiante;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/estudiante/avisos")
public class ControladorAvisosEstudiante {
    private final ServicioAvisosEstudiante servicio;
    public ControladorAvisosEstudiante(ServicioAvisosEstudiante servicio){this.servicio=servicio;}
    @GetMapping public ResponseEntity<PaginaAvisosEstudiante> consultar(@AuthenticationPrincipal Jwt token,@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="5") int tamano){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject(),pagina,tamano));}
    @PatchMapping("/{id}/leer") public ResponseEntity<RespuestaAvisoRecibido> marcar(@AuthenticationPrincipal Jwt token,@PathVariable long id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.marcar(token.getSubject(),id));}
    @PatchMapping("/leer-todos") public ResponseEntity<RespuestaAvisosMarcados> marcarTodos(@AuthenticationPrincipal Jwt token){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.marcarTodos(token.getSubject()));}
}
