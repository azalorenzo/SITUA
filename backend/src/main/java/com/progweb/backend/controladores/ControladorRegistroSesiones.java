package com.progweb.backend.controladores;
import java.util.List;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import com.progweb.backend.servicios.ServicioRegistroSesiones;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/tutor/sesiones-realizadas")
public class ControladorRegistroSesiones {
    private final ServicioRegistroSesiones servicio;
    public ControladorRegistroSesiones(ServicioRegistroSesiones servicio){this.servicio=servicio;}
    @GetMapping public ResponseEntity<PaginaSesionesRealizadas> listar(@AuthenticationPrincipal Jwt token,@Valid @ModelAttribute FiltrosSesionesRealizadas filtros){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.listar(token.getSubject(),filtros));}
    @GetMapping("/opciones") public ResponseEntity<List<OpcionRegistroSesion>> opciones(@AuthenticationPrincipal Jwt token,@RequestParam(required=false) Long estudianteId){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.opciones(token.getSubject(),estudianteId));}
    @PostMapping public ResponseEntity<RespuestaSesionRealizada> registrar(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudRegistrarSesion solicitud){return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.registrar(token.getSubject(),solicitud));}
    @PatchMapping("/{id}/cerrar") public ResponseEntity<RespuestaSesionRealizada> cerrar(@AuthenticationPrincipal Jwt token,@PathVariable long id,@Valid @RequestBody SolicitudCerrarSesion solicitud){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.cerrar(token.getSubject(),id,solicitud));}
}
