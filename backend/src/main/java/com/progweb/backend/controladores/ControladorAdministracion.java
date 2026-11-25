package com.progweb.backend.controladores;
import java.time.LocalDate;
import jakarta.validation.Valid;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import com.progweb.backend.servicios.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/coordinacion/administracion")
public class ControladorAdministracion {
 private final ServicioGestionAdministracion gestion;private final ServicioUsuariosAdministracion usuarios;private final ServicioCatalogosAdministracion catalogos;private final ServicioTutoriasAdministracion tutorias;
 public ControladorAdministracion(ServicioUsuariosAdministracion usuarios,ServicioCatalogosAdministracion catalogos,ServicioTutoriasAdministracion tutorias,ServicioGestionAdministracion gestion){this.gestion=gestion;this.usuarios=usuarios;this.catalogos=catalogos;this.tutorias=tutorias;}
 @GetMapping("/usuarios") public PaginaAdministracion<UsuarioAdministracion> usuarios(@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="10") int tamano,@RequestParam(defaultValue="") String busqueda,@RequestParam(defaultValue="") String rol,@RequestParam(defaultValue="") String estado){return usuarios.listar(pagina,tamano,busqueda,rol,estado);}
 @GetMapping("/usuarios/{id}") public UsuarioAdministracion usuario(@PathVariable long id){return usuarios.consultar(id);}
 @PostMapping("/usuarios") @ResponseStatus(HttpStatus.CREATED) public UsuarioAdministracion crearUsuario(@Valid @RequestBody SolicitudUsuarioAdministracion solicitud,@AuthenticationPrincipal Jwt token){return usuarios.guardar(null,solicitud,token.getSubject());}
 @PutMapping("/usuarios/{id}") public UsuarioAdministracion editarUsuario(@PathVariable long id,@Valid @RequestBody SolicitudUsuarioAdministracion solicitud,@AuthenticationPrincipal Jwt token){return usuarios.guardar(id,solicitud,token.getSubject());}
 @GetMapping("/catalogos/{tipo}") public PaginaAdministracion<CatalogoAdministracion> catalogos(@PathVariable String tipo,@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="10") int tamano,@RequestParam(defaultValue="") String busqueda,@RequestParam(defaultValue="") String estado){return catalogos.listar(tipo,pagina,tamano,busqueda,estado);}
 @PostMapping("/catalogos/{tipo}") @ResponseStatus(HttpStatus.CREATED) public CatalogoAdministracion crearCatalogo(@PathVariable String tipo,@Valid @RequestBody SolicitudCatalogoAdministracion solicitud){return catalogos.guardar(tipo,null,solicitud);}
 @PutMapping("/catalogos/{tipo}/{id}") public CatalogoAdministracion editarCatalogo(@PathVariable String tipo,@PathVariable long id,@Valid @RequestBody SolicitudCatalogoAdministracion solicitud){return catalogos.guardar(tipo,id,solicitud);}
 @DeleteMapping("/catalogos/{tipo}/{id}") public CatalogoAdministracion desactivarCatalogo(@PathVariable String tipo,@PathVariable long id){return catalogos.desactivar(tipo,id);}
 @GetMapping("/tutorias") public PaginaAdministracion<RespuestaSesionAgenda> tutorias(@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="10") int tamano,@RequestParam(defaultValue="") String busqueda,@RequestParam(defaultValue="") String estado,@RequestParam(defaultValue="") String modalidad,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha){return tutorias.listar(pagina,tamano,busqueda,estado,modalidad,fecha);}
 @GetMapping("/tutorias/{id}") public RespuestaSesionAgenda tutoria(@PathVariable long id){return tutorias.consultar(id);}
 @PatchMapping("/tutorias/{id}/{accion}") public RespuestaSesionAgenda cambiarTutoria(@PathVariable long id,@PathVariable String accion,@AuthenticationPrincipal Jwt token){return tutorias.cambiar(id,accion,token.getSubject());}

 @GetMapping("/estudiantes/{id}/expediente") public ExpedienteAdministracion expediente(@PathVariable long id){return gestion.expediente(id);}
 @PutMapping("/estudiantes/{id}/expediente") public ExpedienteAdministracion expediente(@PathVariable long id,@Valid @RequestBody SolicitudExpedienteAdministracion solicitud,@AuthenticationPrincipal Jwt token){return gestion.guardarExpediente(id,solicitud,token.getSubject());}
 @GetMapping("/contextos") public PaginaAdministracion<ServicioGestionAdministracion.Contexto> contextos(@RequestParam(defaultValue="1") int pagina,@RequestParam(defaultValue="10") int tamano,@RequestParam(defaultValue="") String busqueda){return gestion.contextos(pagina,tamano,busqueda);}
 @GetMapping("/tutorias/{id}/contexto") public ServicioGestionAdministracion.Contexto contextoSesion(@PathVariable long id){return gestion.contextoSesion(id);}
 @GetMapping("/reportes/{id}") public RespuestaReporte reporte(@PathVariable long id){return gestion.reporte(id);}
 @PostMapping("/reportes") @ResponseStatus(HttpStatus.CREATED) public RespuestaReporte crearReporte(@Valid @RequestBody SolicitudReporteAdministracion solicitud,@AuthenticationPrincipal Jwt token){return gestion.guardarReporte(null,solicitud,token.getSubject());}
 @PutMapping("/reportes/{id}") public RespuestaReporte editarReporte(@PathVariable long id,@Valid @RequestBody SolicitudReporteAdministracion solicitud,@AuthenticationPrincipal Jwt token){return gestion.guardarReporte(id,solicitud,token.getSubject());}
 @DeleteMapping("/reportes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void eliminarReporte(@PathVariable long id,@AuthenticationPrincipal Jwt token){gestion.eliminarReporte(id,token.getSubject());}
 @PostMapping("/tutorias") @ResponseStatus(HttpStatus.CREATED) public RespuestaSesionAgenda crearTutoria(@Valid @RequestBody SolicitudTutoriaAdministracion solicitud,@AuthenticationPrincipal Jwt token){return tutorias.guardar(null,solicitud,token.getSubject());}
 @PutMapping("/tutorias/{id}") public RespuestaSesionAgenda editarTutoria(@PathVariable long id,@Valid @RequestBody SolicitudTutoriaAdministracion solicitud,@AuthenticationPrincipal Jwt token){return tutorias.guardar(id,solicitud,token.getSubject());}
 @PatchMapping("/tutorias/{id}/cerrar") public RespuestaSesionAgenda cerrarTutoria(@PathVariable long id,@Valid @RequestBody SolicitudCerrarSesion solicitud,@AuthenticationPrincipal Jwt token){return tutorias.cerrar(id,solicitud,token.getSubject());}
}
