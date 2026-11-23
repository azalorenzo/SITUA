package com.progweb.backend.servicios;
import java.time.*;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudActividadPersonal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioInicioEstudiante {
    private final ServicioAutenticacion autenticacion;
    private final RepositorioSesionesAgenda sesiones;
    private final RepositorioActividadesPersonales actividades;
    private final Clock reloj;
    public ServicioInicioEstudiante(ServicioAutenticacion autenticacion,RepositorioSesionesAgenda sesiones,RepositorioActividadesPersonales actividades,Clock reloj){this.autenticacion=autenticacion;this.sesiones=sesiones;this.actividades=actividades;this.reloj=reloj;}
    private record Persona(long usuario,long estudiante) { }
    private Persona identificar(String identificador){var usuario=autenticacion.consultarSesion(identificador);if(!usuario.roles().contains("estudiante"))throw new AccesoRechazado(403,"Esta opción es para estudiantes.");Long estudiante=sesiones.estudiante(usuario.id());if(estudiante==null)throw new AccesoRechazado(404,"No se encontró tu perfil.");return new Persona(usuario.id(),estudiante);}
    public RespuestaInicioEstudiante consultar(String identificador){var persona=identificar(identificador);var ahora=LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);var proximas=sesiones.proximas(persona.estudiante(),ahora);return new RespuestaInicioEstudiante(proximas.isEmpty()?null:proximas.getFirst(),sesiones.virtualesEstudiante(persona.estudiante(),ahora),actividades.listar(persona.usuario(),"taller"),actividades.listar(persona.usuario(),"recordatorio"));}
    private void validar(SolicitudActividadPersonal solicitud){boolean inicio=solicitud.inicioEn()!=null,fin=solicitud.finEn()!=null;if(inicio!=fin||inicio&&(solicitud.fecha()!=null||!solicitud.finEn().isAfter(solicitud.inicioEn())))throw new SolicitudRechazada(422,"Revisa la fecha y las horas.","finEn");if(solicitud.tipo().equals("taller")&&!inicio&&solicitud.fecha()==null)throw new SolicitudRechazada(422,"Indica la fecha del taller.","fecha");}
    @Transactional public RespuestaActividadPersonal crear(String identificador,SolicitudActividadPersonal solicitud){var persona=identificar(identificador);validar(solicitud);long id=actividades.crear(persona.usuario(),solicitud);return actividades.buscar(persona.usuario(),id);}
    @Transactional public RespuestaActividadPersonal editar(String identificador,long id,SolicitudActividadPersonal solicitud){var persona=identificar(identificador);validar(solicitud);if(!actividades.editar(persona.usuario(),id,solicitud))throw new AccesoRechazado(404,"No se encontró la actividad.");return actividades.buscar(persona.usuario(),id);}
    @Transactional public void eliminar(String identificador,long id){var persona=identificar(identificador);if(!actividades.eliminar(persona.usuario(),id))throw new AccesoRechazado(404,"No se encontró la actividad.");}
}
