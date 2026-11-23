package com.progweb.backend.controladores;
import java.util.List;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import com.progweb.backend.servicios.ServicioReportesTutor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/tutor/reportes")
public class ControladorReportesTutor {
    private final ServicioReportesTutor servicio;
    public ControladorReportesTutor(ServicioReportesTutor servicio){this.servicio=servicio;}
    private <T> ResponseEntity<T> respuesta(T datos){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(datos);}
    @GetMapping public ResponseEntity<PaginaReportes> listar(@AuthenticationPrincipal Jwt token,@Valid @ModelAttribute FiltrosReportes filtros){return respuesta(servicio.listar(token.getSubject(),filtros));}
    @GetMapping("/analisis") public ResponseEntity<AnalisisReportesTutor> analisis(@AuthenticationPrincipal Jwt token){return respuesta(servicio.analisis(token.getSubject()));}
    @GetMapping("/motivos") public ResponseEntity<List<RespuestaMotivoReporte>> motivos(@AuthenticationPrincipal Jwt token){return respuesta(servicio.motivos(token.getSubject()));}
    @GetMapping("/sesiones") public ResponseEntity<List<RespuestaSesionRealizada>> sesiones(@AuthenticationPrincipal Jwt token,@RequestParam long estudianteId,@RequestParam long periodoId){return respuesta(servicio.sesiones(token.getSubject(),estudianteId,periodoId));}
    @PostMapping public ResponseEntity<RespuestaReporte> crear(@AuthenticationPrincipal Jwt token,@Valid @RequestBody SolicitudReporteTutor solicitud){return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(servicio.crear(token.getSubject(),solicitud));}
    @PatchMapping("/{id}/completar") public ResponseEntity<RespuestaReporte> completar(@AuthenticationPrincipal Jwt token,@PathVariable long id,@Valid @RequestBody SolicitudCompletarReporte solicitud){return respuesta(servicio.completar(token.getSubject(),id,solicitud));}
    @GetMapping(value="/exportar/pdf",produces=MediaType.APPLICATION_PDF_VALUE) public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Jwt token,@Valid @ModelAttribute FiltrosReportes filtros){return archivo(servicio.exportar(token.getSubject(),filtros),"reportes-tutor.pdf");}
    @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE) public ResponseEntity<byte[]> individual(@AuthenticationPrincipal Jwt token,@PathVariable long id){return archivo(servicio.individual(token.getSubject(),id),"reporte-"+id+".pdf");}
    private ResponseEntity<byte[]> archivo(byte[] datos,String nombre){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(nombre).build().toString()).body(datos);}
}
