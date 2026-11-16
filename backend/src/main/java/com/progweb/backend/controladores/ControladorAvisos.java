package com.progweb.backend.controladores;

import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudAviso;
import com.progweb.backend.servicios.ServicioAvisos;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coordinacion/avisos")
public class ControladorAvisos {
    private final ServicioAvisos servicio;
    public ControladorAvisos(ServicioAvisos servicio){this.servicio=servicio;}
    @GetMapping
    public ResponseEntity<PaginaAvisos> listar(@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="5") int tamano,
            @RequestParam(required=false) String prioridad,@RequestParam(required=false) String categoria,
            @RequestParam(required=false) String estado,@RequestParam(defaultValue="") String busqueda){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.listar(pagina,tamano,prioridad,categoria,estado,busqueda));
    }
    @GetMapping("/destinatarios")
    public ResponseEntity<PaginaDestinatariosAviso> destinatarios(@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="10") int tamano,
            @RequestParam(required=false) String rol,@RequestParam(defaultValue="") String busqueda){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.personas(pagina,tamano,rol,busqueda));
    }
    @GetMapping("/{id}")
    public ResponseEntity<DetalleAviso> detalle(@PathVariable long id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.detalle(id));}
    @PostMapping
    public ResponseEntity<DetalleAviso> crear(@Valid @RequestBody SolicitudAviso solicitud,@AuthenticationPrincipal Jwt token){
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.crear(solicitud,token.getSubject()));
    }
    @PutMapping("/{id}")
    public ResponseEntity<DetalleAviso> editar(@PathVariable long id,@Valid @RequestBody SolicitudAviso solicitud,@AuthenticationPrincipal Jwt token){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.editar(id,solicitud,token.getSubject()));
    }
    @PatchMapping("/{id}/retirar")
    public ResponseEntity<RespuestaAviso> retirar(@PathVariable long id,@AuthenticationPrincipal Jwt token){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.retirar(id,token.getSubject()));
    }
}
