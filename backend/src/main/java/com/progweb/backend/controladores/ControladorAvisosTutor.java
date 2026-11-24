package com.progweb.backend.controladores;

import java.util.List;
import com.progweb.backend.respuestas.RespuestaAvisoRecibido;
import com.progweb.backend.respuestas.RespuestaAvisosMarcados;
import com.progweb.backend.servicios.ServicioAvisosRecibidos;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// Avisos del tutor conectado. No reciben ningún identificador de usuario: se toma del JWT.
@RestController
@RequestMapping("/api/tutor/avisos")
public class ControladorAvisosTutor {
    private final ServicioAvisosRecibidos servicio;

    public ControladorAvisosTutor(ServicioAvisosRecibidos servicio) { this.servicio = servicio; }

    @GetMapping
    public ResponseEntity<List<RespuestaAvisoRecibido>> consultar(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(token.getSubject()));
    }

    @GetMapping("/pagina")
    public ResponseEntity<com.progweb.backend.respuestas.PaginaAvisosRecibidos> pagina(@AuthenticationPrincipal Jwt token,
            @jakarta.validation.Valid @ModelAttribute com.progweb.backend.solicitudes.FiltrosAvisosRecibidos filtros){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultarPagina(token.getSubject(),filtros));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<RespuestaAvisoRecibido> marcarLeido(@PathVariable long id, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.marcarLeido(token.getSubject(), id));
    }

    @PatchMapping("/leer-todos")
    public ResponseEntity<RespuestaAvisosMarcados> marcarTodosLeidos(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.marcarTodosLeidos(token.getSubject()));
    }
}
