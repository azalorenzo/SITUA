package com.progweb.backend.controladores;

import com.progweb.backend.respuestas.RespuestaPanelReportesTutor;
import com.progweb.backend.servicios.ServicioReportesTutor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// Reportes del tutor conectado. No reciben el identificador del tutor: se toma del JWT.
@RestController
@RequestMapping("/api/tutor/reportes")
public class ControladorReportesTutor {
    private final ServicioReportesTutor servicio;

    public ControladorReportesTutor(ServicioReportesTutor servicio) { this.servicio = servicio; }

    @GetMapping
    public ResponseEntity<RespuestaPanelReportesTutor> panel(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.panel(token.getSubject()));
    }

    @GetMapping(value = "/exportar/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Jwt token) {
        return archivo(servicio.pdfTodos(token.getSubject()), "mis-reportes.pdf");
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> individual(@PathVariable long id, @AuthenticationPrincipal Jwt token) {
        return archivo(servicio.pdfIndividual(token.getSubject(), id), "reporte-" + id + ".pdf");
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String nombre) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }
}
