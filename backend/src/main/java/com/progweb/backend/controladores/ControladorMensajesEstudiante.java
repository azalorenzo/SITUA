package com.progweb.backend.controladores;

import java.util.List;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.RespuestaConversacionLeida;
import com.progweb.backend.respuestas.RespuestaConversacionTutor;
import com.progweb.backend.respuestas.RespuestaMensajeTutor;
import com.progweb.backend.servicios.ServicioMensajesTutor;
import com.progweb.backend.solicitudes.SolicitudMensajeTutor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estudiante/mensajes/conversaciones")
public class ControladorMensajesEstudiante {
    private final ServicioMensajesTutor servicio;

    public ControladorMensajesEstudiante(ServicioMensajesTutor servicio) { this.servicio = servicio; }

    @GetMapping
    public ResponseEntity<List<RespuestaConversacionTutor>> conversaciones(@AuthenticationPrincipal Jwt token, @RequestParam(defaultValue="") String busqueda) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.conversaciones(token.getSubject(),false,busqueda));
    }

    @GetMapping("/{id}/mensajes")
    public ResponseEntity<List<RespuestaMensajeTutor>> consultar(@PathVariable long id, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject(),false,id));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<RespuestaConversacionLeida> marcarLeida(@PathVariable long id, @AuthenticationPrincipal Jwt token, @RequestParam(required=false) Long ultimoMensajeId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.marcarLeida(token.getSubject(),false,id,ultimoMensajeId));
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<RespuestaMensajeTutor> enviar(@PathVariable long id, @Valid @RequestBody SolicitudMensajeTutor solicitud,
            @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.enviar(token.getSubject(),false,id,solicitud.contenido()));
    }
}
