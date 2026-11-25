package com.progweb.backend.servicios;
import java.time.*;
import java.util.List;
import java.util.regex.Pattern;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioRegistroSesiones {
    private final ServicioAutenticacion autenticacion;
    private final RepositorioSesionesAgenda agenda;
    private final RepositorioRegistroSesiones registros;
    private final Clock reloj;
    public ServicioRegistroSesiones(ServicioAutenticacion autenticacion,RepositorioSesionesAgenda agenda,RepositorioRegistroSesiones registros,Clock reloj){
        this.autenticacion=autenticacion;this.agenda=agenda;this.registros=registros;this.reloj=reloj;
    }
    private record Persona(long usuario,long tutor) { }
    private Persona identificar(String identificador){
        var usuario=autenticacion.consultarSesion(identificador);
        if(!usuario.roles().contains("tutor"))throw new AccesoRechazado(403,"Esta opción es para tutores.");
        Long tutor=agenda.tutor(usuario.id());if(tutor==null)throw new AccesoRechazado(404,"No se encontró tu perfil de tutor.");
        return new Persona(usuario.id(),tutor);
    }
    private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);}
    private void bloquear(long estudiante,long tutor){
        if(!agenda.bloquearEstudiante(estudiante)||!agenda.bloquearTutor(tutor))throw new AccesoRechazado(404,"No se encontró el estudiante o tutor.");
    }
    private void validarDuracion(LocalDateTime inicio,LocalDateTime fin){
        if(inicio==null||fin==null||!fin.isAfter(inicio)||Duration.between(inicio,fin).toMinutes()<1||Duration.between(inicio,fin).compareTo(Duration.ofHours(24))>0)
            throw new SolicitudRechazada(422,"Indica un horario válido de entre un minuto y un día.","finEn");
        if(fin.isAfter(ahora()))throw new SolicitudRechazada(422,"La sesión todavía no ha terminado.","finEn");
    }
    public List<OpcionRegistroSesion> opciones(String identificador,Long estudianteId){return registros.opciones(identificar(identificador).tutor(),estudianteId);}
    public PaginaSesionesRealizadas listar(String identificador,FiltrosSesionesRealizadas filtros){
        var persona=identificar(identificador);
        if(filtros.desde()!=null&&filtros.hasta()!=null&&!filtros.hasta().isAfter(filtros.desde()))throw new SolicitudRechazada(422,"Revisa el intervalo de fechas.","hasta");
        long total=registros.contar(persona.tutor(),filtros);int paginas=Math.toIntExact((total+filtros.tamano()-1)/filtros.tamano());
        int pagina=Math.min(filtros.pagina(),Math.max(1,paginas));
        return new PaginaSesionesRealizadas(registros.listar(persona.tutor(),filtros,pagina),pagina,filtros.tamano(),total,paginas,registros.resumen(persona.tutor()));
    }
    @Transactional
    public RespuestaSesionRealizada registrar(String identificador,SolicitudRegistrarSesion solicitud){
        return registrarPersona(identificar(identificador),solicitud);
    }
    private Persona coordinacion(String sujeto,long tutor){var usuario=autenticacion.consultarSesion(sujeto);if(!usuario.roles().contains("coordinador"))throw new AccesoRechazado(403,"Se requiere coordinación.");return new Persona(usuario.id(),tutor);}
    @Transactional public RespuestaSesionRealizada registrarCoordinacion(String sujeto,long tutor,SolicitudRegistrarSesion solicitud){return registrarPersona(coordinacion(sujeto,tutor),solicitud);}
    private RespuestaSesionRealizada registrarPersona(Persona persona,SolicitudRegistrarSesion solicitud){
        validarDuracion(solicitud.inicioEn(),solicitud.finEn());bloquear(solicitud.estudianteId(),persona.tutor());
        if(!agenda.participantesActivos(solicitud.estudianteId(),persona.tutor())||!agenda.inscripcionVigente(solicitud.estudianteId(),persona.tutor(),solicitud.periodoId()))
            throw new SolicitudRechazada(409,"No tienes una asignación activa con este estudiante en ese periodo.","estudianteId");
        if(registros.cruce(solicitud.estudianteId(),persona.tutor(),solicitud.inicioEn(),solicitud.finEn(),null))
            throw new SolicitudRechazada(409,"Ya hay una sesión en ese horario. Si estaba programada, ciérrala desde Horarios.","inicioEn");
        String lugar=solicitud.ubicacion().strip();boolean virtual=Pattern.compile("^(https?://|www\\.)",Pattern.CASE_INSENSITIVE).matcher(lugar).find();
        if(!virtual&&lugar.length()>200)throw new SolicitudRechazada(422,"La ubicación no debe superar 200 caracteres.","ubicacion");
        long id=registros.registrar(persona.tutor(),persona.usuario(),solicitud,virtual?"virtual":"presencial",virtual?null:lugar,virtual?lugar:null);
        registros.observacion(id,solicitud.estudianteId(),solicitud.periodoId(),persona.usuario(),solicitud.observaciones(),solicitud.finEn());
        agenda.registrarCambio(id,persona.usuario(),"registro",null,"completada",solicitud.inicioEn(),solicitud.finEn());
        return registros.buscar(id,persona.tutor());
    }
    @Transactional
    public RespuestaSesionRealizada cerrar(String identificador,long id,SolicitudCerrarSesion solicitud){
        return cerrarPersona(identificar(identificador),id,solicitud);
    }
    @Transactional public RespuestaSesionRealizada cerrarCoordinacion(String sujeto,long tutor,long id,SolicitudCerrarSesion solicitud){return cerrarPersona(coordinacion(sujeto,tutor),id,solicitud);}
    private RespuestaSesionRealizada cerrarPersona(Persona persona,long id,SolicitudCerrarSesion solicitud){
        var sesion=agenda.sesion(id,persona.tutor(),true,false);
        if(sesion==null)throw new AccesoRechazado(404,"No se encontró esa sesión.");
        bloquear(sesion.estudiante(),persona.tutor());
        if(sesion.disponibilidad()!=null)agenda.horario(sesion.disponibilidad(),true);
        sesion=agenda.sesion(id,persona.tutor(),true,true);
        if(sesion==null||!sesion.estado().equals("confirmada"))throw new AccesoRechazado(409,"Solo se puede cerrar una sesión confirmada.");
        if(sesion.inicio().isAfter(ahora()))throw new AccesoRechazado(409,"La sesión todavía no ha comenzado.");
        if(solicitud.estado().equals("completada")){
            validarDuracion(solicitud.inicioRealEn(),solicitud.finRealEn());
            if(registros.cruce(sesion.estudiante(),persona.tutor(),solicitud.inicioRealEn(),solicitud.finRealEn(),id))throw new AccesoRechazado(409,"El horario real se cruza con otra sesión.");
        }else if(!solicitud.estado().equals("no_asistio"))throw new AccesoRechazado(422,"Selecciona un resultado válido.");
        registros.cerrar(id,solicitud);
        registros.observacion(id,sesion.estudiante(),sesion.periodo(),persona.usuario(),solicitud.observaciones(),solicitud.estado().equals("completada")?solicitud.finRealEn():ahora());
        agenda.registrarCambio(id,persona.usuario(),"cierre",sesion.estado(),solicitud.estado(),sesion.inicio(),sesion.fin());
        return registros.buscar(id,persona.tutor());
    }
}
