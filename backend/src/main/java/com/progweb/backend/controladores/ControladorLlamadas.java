package com.progweb.backend.controladores;

import jakarta.validation.Valid;
import com.progweb.backend.servicios.ServicioLlamadas;
import com.progweb.backend.solicitudes.SolicitudLlamada;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llamadas")
public class ControladorLlamadas {
    private final ServicioLlamadas servicio;
    public ControladorLlamadas(ServicioLlamadas servicio){this.servicio=servicio;}
    private <T> ResponseEntity<T> respuesta(T datos){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(datos);}
    @GetMapping("/actual")
    public ResponseEntity<ServicioLlamadas.Actual> actual(@AuthenticationPrincipal Jwt token){return respuesta(servicio.actual(token.getSubject()));}
    @PostMapping
    public ResponseEntity<ServicioLlamadas.Llamada> iniciar(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudLlamada solicitud){return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.iniciar(token.getSubject(),solicitud.conversacionId(),solicitud.tipo()));}
    @PostMapping("/{id}/aceptar")
    public ResponseEntity<ServicioLlamadas.Llamada> aceptar(@AuthenticationPrincipal Jwt token,@PathVariable long id){return respuesta(servicio.aceptar(token.getSubject(),id));}
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<ServicioLlamadas.Actual> rechazar(@AuthenticationPrincipal Jwt token,@PathVariable long id){return respuesta(servicio.terminar(token.getSubject(),id,true));}
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ServicioLlamadas.Actual> finalizar(@AuthenticationPrincipal Jwt token,@PathVariable long id){return respuesta(servicio.terminar(token.getSubject(),id,false));}
    @PostMapping("/{id}/acceso")
    public ResponseEntity<ServicioLlamadas.Acceso> acceso(@AuthenticationPrincipal Jwt token,@PathVariable long id){return respuesta(servicio.acceso(token.getSubject(),id));}
}
