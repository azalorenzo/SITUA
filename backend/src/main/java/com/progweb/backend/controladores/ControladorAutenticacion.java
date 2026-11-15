package com.progweb.backend.controladores;

import jakarta.validation.Valid;
import com.progweb.backend.servicios.ServicioRegistro;
import com.progweb.backend.servicios.ServicioAccesoGoogle;
import com.progweb.backend.solicitudes.SolicitudRegistro;
import com.progweb.backend.solicitudes.SolicitudAccesoGoogle;

import com.progweb.backend.respuestas.RespuestaInicioSesion;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioAutenticacion;
import com.progweb.backend.solicitudes.SolicitudInicioSesion;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacion")
public class ControladorAutenticacion {
    private final ServicioAutenticacion servicioAutenticacion;

    private final ServicioRegistro registro;
    private final ServicioAccesoGoogle google;

    public ControladorAutenticacion(ServicioAutenticacion servicioAutenticacion, ServicioRegistro registro,
            ServicioAccesoGoogle google) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.registro = registro;
        this.google = google;
    }

    @PostMapping("/registrar")
    public ResponseEntity<RespuestaInicioSesion> registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(registro.registrar(solicitud));
    }

    @PostMapping("/google")
    public ResponseEntity<RespuestaInicioSesion> ingresarConGoogle(@Valid @RequestBody SolicitudAccesoGoogle solicitud) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(google.iniciarSesion(solicitud));
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaInicioSesion> iniciarSesion(
            @Valid @RequestBody SolicitudInicioSesion solicitud) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicioAutenticacion.iniciarSesion(solicitud));
    }

    @GetMapping("/sesion")
    public ResponseEntity<RespuestaUsuario> consultarSesion(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicioAutenticacion.consultarSesion(token.getSubject()));
    }
}
