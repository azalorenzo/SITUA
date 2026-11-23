package com.progweb.backend.servicios;
import java.time.*;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioAvisosEstudiante {
    private final ServicioAutenticacion autenticacion;
    private final RepositorioAvisosRecibidos avisos;
    private final RepositorioSesionesAgenda perfiles;
    private final Clock reloj;
    public ServicioAvisosEstudiante(ServicioAutenticacion autenticacion,RepositorioAvisosRecibidos avisos,RepositorioSesionesAgenda perfiles,Clock reloj){this.autenticacion=autenticacion;this.avisos=avisos;this.perfiles=perfiles;this.reloj=reloj;}
    private long usuario(String identificador){var usuario=autenticacion.consultarSesion(identificador);if(!usuario.roles().contains("estudiante"))throw new AccesoRechazado(403,"Esta opción es para estudiantes.");if(perfiles.estudiante(usuario.id())==null)throw new AccesoRechazado(404,"No se encontró tu perfil.");return usuario.id();}
    public PaginaAvisosEstudiante consultar(String identificador,int pagina,int tamano){long usuario=usuario(identificador);if(pagina<1||tamano<1||tamano>100)throw new SolicitudRechazada(422,"Revisa la página solicitada.","pagina");long total=avisos.contar(usuario);int paginas=Math.toIntExact((total+tamano-1)/tamano),actual=Math.min(pagina,Math.max(1,paginas));return new PaginaAvisosEstudiante(avisos.listarPagina(usuario,tamano,(actual-1L)*tamano),actual,tamano,total,paginas,avisos.noLeidos(usuario));}
    @Transactional public RespuestaAvisoRecibido marcar(String identificador,long id){long usuario=usuario(identificador);avisos.buscar(usuario,id).orElseThrow(()->new AccesoRechazado(404,"No se encontró el aviso."));avisos.marcarLeido(usuario,id,LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC));return avisos.buscar(usuario,id).orElseThrow(()->new AccesoRechazado(404,"No se encontró el aviso."));}
    @Transactional public RespuestaAvisosMarcados marcarTodos(String identificador){return new RespuestaAvisosMarcados(avisos.marcarTodosLeidos(usuario(identificador),LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC)));}
}
