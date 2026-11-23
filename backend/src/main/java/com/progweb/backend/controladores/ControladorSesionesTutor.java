package com.progweb.backend.controladores;
import java.time.LocalDateTime;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.servicios.ServicioSesionesAgenda;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/tutor/sesiones")
public class ControladorSesionesTutor {
    private final ServicioSesionesAgenda servicio;
    public ControladorSesionesTutor(ServicioSesionesAgenda servicio){this.servicio=servicio;}
    @GetMapping public ResponseEntity<PaginaSesiones> listar(@AuthenticationPrincipal Jwt token,@RequestParam(required=false) String estado,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
        @RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="5") int tamano){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.listar(token.getSubject(),true,estado,desde,hasta,pagina,tamano));}
    @PatchMapping("/{id}/{accion}") public ResponseEntity<RespuestaSesionAgenda> cambiar(@AuthenticationPrincipal Jwt token,@PathVariable long id,@PathVariable String accion){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.cambiar(token.getSubject(),true,id,accion));}
}
