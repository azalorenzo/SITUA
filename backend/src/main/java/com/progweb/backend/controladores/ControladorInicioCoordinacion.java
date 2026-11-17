package com.progweb.backend.controladores;
import com.progweb.backend.servicios.ServicioInicioCoordinacion;
import com.progweb.backend.respuestas.RespuestaInicioCoordinacion;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/coordinacion/inicio")
public class ControladorInicioCoordinacion {
    private final ServicioInicioCoordinacion servicio;
    public ControladorInicioCoordinacion(ServicioInicioCoordinacion servicio){this.servicio=servicio;}
    @GetMapping public ResponseEntity<RespuestaInicioCoordinacion> consultar(){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar());
    }
}
