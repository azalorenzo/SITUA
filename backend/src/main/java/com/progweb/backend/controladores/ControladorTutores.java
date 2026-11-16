package com.progweb.backend.controladores;

import java.util.List;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import com.progweb.backend.servicios.ServicioTutores;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coordinacion")
public class ControladorTutores {
    private final ServicioTutores servicio;
    public ControladorTutores(ServicioTutores servicio) { this.servicio=servicio; }
    @GetMapping("/tutores")
    public ResponseEntity<PaginaTutores> listar(@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="5") int tamano,
            @RequestParam(required=false) String estado,@RequestParam(required=false) Long departamentoId,@RequestParam(defaultValue="") String busqueda) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.listar(pagina,tamano,estado,departamentoId,busqueda));
    }
    @GetMapping("/tutores/opciones")
    public ResponseEntity<List<RespuestaTutorCoordinacion>> opciones() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.opciones());
    }
    @GetMapping("/departamentos")
    public ResponseEntity<List<RespuestaDepartamento>> departamentos() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.departamentos());
    }
    @PostMapping("/tutores")
    public ResponseEntity<RespuestaTutorCoordinacion> crear(@Valid @RequestBody SolicitudCrearTutor solicitud,@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.crear(solicitud,token.getSubject()));
    }
    @PutMapping("/tutores/{id}")
    public ResponseEntity<RespuestaTutorCoordinacion> editar(@PathVariable long id,@Valid @RequestBody SolicitudEditarTutor solicitud,@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.editar(id,solicitud,token.getSubject()));
    }
    @PatchMapping("/tutores/{id}/desactivar")
    public ResponseEntity<RespuestaTutorCoordinacion> desactivar(@PathVariable long id,@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.cambiarEstado(id,false,token.getSubject()));
    }
    @PatchMapping("/tutores/{id}/activar")
    public ResponseEntity<RespuestaTutorCoordinacion> activar(@PathVariable long id,@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.cambiarEstado(id,true,token.getSubject()));
    }
}
