package com.progweb.backend.controladores;
import java.util.Map;
import jakarta.validation.Valid;
import com.progweb.backend.servicios.ServicioRecuperacion;
import com.progweb.backend.solicitudes.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/autenticacion/recuperacion")
public class ControladorRecuperacion {
    private final ServicioRecuperacion servicio;
    public ControladorRecuperacion(ServicioRecuperacion servicio) { this.servicio=servicio; }
    @PostMapping("/solicitar")
    public ResponseEntity<Map<String,String>> solicitar(@Valid @RequestBody SolicitudCodigoRecuperacion solicitud) {
        servicio.solicitar(solicitud);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensaje","Código solicitado. Revisa tu correo; si pediste uno recientemente, espera un minuto antes de reenviar."));
    }
    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<Map<String,String>> cambiar(@Valid @RequestBody SolicitudCambiarContrasena solicitud) {
        servicio.cambiar(solicitud);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensaje","Contraseña actualizada. Inicia sesión nuevamente."));
    }
}
