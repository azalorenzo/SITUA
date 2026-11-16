package com.progweb.backend.controladores;

import java.util.List;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import com.progweb.backend.servicios.ServicioAsignaciones;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coordinacion")
public class ControladorAsignaciones {
    private final ServicioAsignaciones servicio;
    public ControladorAsignaciones(ServicioAsignaciones servicio) { this.servicio = servicio; }

    @GetMapping("/periodos")
    public ResponseEntity<List<RespuestaPeriodo>> periodos() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.periodos());
    }
    @GetMapping("/asignaciones")
    public ResponseEntity<PaginaAsignaciones> listar(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "5") int tamano,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "") String busqueda) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.listar(pagina, tamano, periodoId, estado, busqueda));
    }
    @GetMapping("/asignaciones/exportar")
    public ResponseEntity<List<RespuestaAsignacion>> exportar(
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "") String busqueda) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.exportar(periodoId, estado, busqueda));
    }
    @GetMapping("/asignaciones/estudiantes")
    public ResponseEntity<List<RespuestaEstudianteAsignable>> estudiantes(@RequestParam long periodoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.estudiantes(periodoId));
    }
    @PostMapping("/asignaciones")
    public ResponseEntity<RespuestaAsignacion> crear(@Valid @RequestBody SolicitudAsignacion solicitud, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.crear(solicitud, token.getSubject()));
    }
    @PatchMapping("/asignaciones/{id}/cambiar-tutor")
    public ResponseEntity<RespuestaAsignacion> cambiar(@PathVariable long id, @Valid @RequestBody SolicitudCambioTutor solicitud, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.cambiarTutor(id, solicitud, token.getSubject()));
    }
    @PatchMapping("/asignaciones/{id}/finalizar")
    public ResponseEntity<RespuestaAsignacion> finalizar(@PathVariable long id, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.cerrar(id, "finalizada", token.getSubject()));
    }
    @PatchMapping("/asignaciones/{id}/cancelar")
    public ResponseEntity<RespuestaAsignacion> cancelar(@PathVariable long id, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.cerrar(id, "cancelada", token.getSubject()));
    }
}
