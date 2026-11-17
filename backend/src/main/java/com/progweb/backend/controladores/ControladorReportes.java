package com.progweb.backend.controladores;

import java.util.List;
import jakarta.validation.Valid;
import com.progweb.backend.solicitudes.FiltrosReportes;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.servicios.ServicioReportes;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coordinacion/reportes")
public class ControladorReportes {
    private final ServicioReportes servicio;
    public ControladorReportes(ServicioReportes servicio){this.servicio=servicio;}
    @GetMapping
    public ResponseEntity<PaginaReportes> listar(@Valid @ModelAttribute FiltrosReportes filtros){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.listar(filtros));}
    @GetMapping("/motivos")
    public ResponseEntity<List<RespuestaMotivoReporte>> motivos(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.motivos());}
    @GetMapping(value="/exportar/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportar(@Valid @ModelAttribute FiltrosReportes filtros,@AuthenticationPrincipal Jwt token){return archivo(servicio.exportar(filtros,token.getSubject()),"reportes.pdf");}
    @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> individual(@PathVariable long id,@AuthenticationPrincipal Jwt token){return archivo(servicio.individual(id,token.getSubject()),"reporte-"+id+".pdf");}
    private ResponseEntity<byte[]> archivo(byte[] contenido,String nombre){
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(nombre).build().toString()).body(contenido);
    }
}
