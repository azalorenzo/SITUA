package com.progweb.backend.controladores;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudReservarSesion;
import com.progweb.backend.servicios.ServicioSesionesAgenda;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/estudiante/sesiones")
public class ControladorSesionesEstudiante {
    private final ServicioSesionesAgenda servicio;
    public ControladorSesionesEstudiante(ServicioSesionesAgenda servicio){this.servicio=servicio;}
    private <T> ResponseEntity<T> respuesta(T datos){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(datos);}
    @GetMapping public ResponseEntity<PaginaSesiones> listar(@AuthenticationPrincipal Jwt token,@RequestParam(required=false) String estado,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
        @RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="5") int tamano){return respuesta(servicio.listar(token.getSubject(),false,estado,desde,hasta,pagina,tamano));}
    @GetMapping("/agenda") public ResponseEntity<List<RespuestaSesionAgenda>> agenda(@AuthenticationPrincipal Jwt token,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta){return respuesta(servicio.agenda(token.getSubject(),desde,hasta));}
    @GetMapping("/resumen") public ResponseEntity<ResumenSesionesEstudiante> resumen(@AuthenticationPrincipal Jwt token){return respuesta(servicio.resumen(token.getSubject()));}
    @GetMapping("/opciones") public ResponseEntity<List<OpcionSesionEstudiante>> opciones(@AuthenticationPrincipal Jwt token){return respuesta(servicio.opciones(token.getSubject()));}
    @GetMapping("/disponibles") public ResponseEntity<PaginaDisponibilidades> disponibles(@AuthenticationPrincipal Jwt token,@RequestParam long periodoId,
        @RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="5") int tamano){return respuesta(servicio.disponibles(token.getSubject(),periodoId,pagina,tamano));}
    @PostMapping public ResponseEntity<RespuestaSesionAgenda> solicitar(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudReservarSesion solicitud){
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.solicitar(token.getSubject(),solicitud));}
    @PatchMapping("/{id}/cancelar") public ResponseEntity<RespuestaSesionAgenda> cancelar(@AuthenticationPrincipal Jwt token,@PathVariable long id){return respuesta(servicio.cambiar(token.getSubject(),false,id,"cancelar"));}
}
