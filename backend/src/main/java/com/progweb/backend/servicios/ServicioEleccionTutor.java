package com.progweb.backend.servicios;
import java.util.List;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaEleccionTutor;
import com.progweb.backend.solicitudes.SolicitudElegirTutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioEleccionTutor {
    private final ServicioAutenticacion autenticacion;
    private final RepositorioPerfilEstudiante perfiles;
    private final RepositorioAsignaciones asignaciones;
    private final RepositorioEleccionTutor elecciones;
    public ServicioEleccionTutor(ServicioAutenticacion autenticacion,RepositorioPerfilEstudiante perfiles,
        RepositorioAsignaciones asignaciones,RepositorioEleccionTutor elecciones) {
        this.autenticacion=autenticacion;this.perfiles=perfiles;this.asignaciones=asignaciones;this.elecciones=elecciones;
    }
    private long usuario(String identificador) {
        var usuario=autenticacion.consultarSesion(identificador);
        if(!usuario.roles().contains("estudiante"))throw new AccesoRechazado(403,"Esta opción es para estudiantes.");
        return usuario.id();
    }
    private long estudiante(long usuario,boolean bloquear) {
        Long estudiante=perfiles.estudiante(usuario,bloquear);
        if(estudiante==null)throw new AccesoRechazado(404,"No se encontró tu perfil de estudiante.");
        return estudiante;
    }
    public RespuestaEleccionTutor consultar(String identificador,Long periodoId,int pagina,int tamano,String busqueda) {
        if(pagina<1||tamano<1||tamano>100||busqueda.length()>160||periodoId!=null&&periodoId<1)
            throw new SolicitudRechazada(422,"Revisa los filtros de búsqueda.","busqueda");
        long estudiante=estudiante(usuario(identificador),false);
        var periodos=elecciones.periodos(estudiante);
        if(periodos.isEmpty())return new RespuestaEleccionTutor(periodos,null,null,List.of(),1,tamano,0,0);
        long periodo=periodoId==null?periodos.getFirst().id():periodoId;
        if(periodos.stream().noneMatch(opcion->opcion.id()==periodo))throw new SolicitudRechazada(409,"No tienes inscripción activa en este periodo.","periodoId");
        var asignado=elecciones.asignado(estudiante,periodo);
        if(asignado!=null)return new RespuestaEleccionTutor(periodos,periodo,asignado,List.of(),1,tamano,0,0);
        long total=elecciones.contar(busqueda);int paginas=Math.toIntExact((total+tamano-1)/tamano);
        int actual=Math.min(pagina,Math.max(1,paginas));
        return new RespuestaEleccionTutor(periodos,periodo,null,elecciones.listar(busqueda,tamano,(actual-1L)*tamano),actual,tamano,total,paginas);
    }
    @Transactional
    public RespuestaEleccionTutor elegir(String identificador,SolicitudElegirTutor solicitud) {
        long usuario=usuario(identificador),estudiante=estudiante(usuario,true);
        // Compartimos el bloqueo del estudiante con las asignaciones de coordinación.
        if(!asignaciones.cuentaActiva(estudiante))throw new AccesoRechazado(403,"Tu cuenta está inactiva.");
        if(!asignaciones.periodoActivo(solicitud.periodoId())||!asignaciones.tieneExpediente(estudiante,solicitud.periodoId()))
            throw new SolicitudRechazada(409,"No tienes inscripción activa en este periodo.","periodoId");
        if(asignaciones.tieneAsignacion(estudiante,solicitud.periodoId()))throw new AccesoRechazado(409,"Ya tienes un tutor asignado en este periodo.");
        if(!elecciones.bloquearTutorActivo(solicitud.tutorId()))throw new SolicitudRechazada(409,"El tutor ya no está disponible.","tutorId");
        asignaciones.crear(estudiante,solicitud.tutorId(),solicitud.periodoId(),usuario,null);
        return consultar(identificador,solicitud.periodoId(),1,5,"");
    }
}
