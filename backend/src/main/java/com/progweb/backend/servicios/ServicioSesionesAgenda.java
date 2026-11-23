package com.progweb.backend.servicios;

import java.time.*;
import java.util.List;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.RepositorioSesionesAgenda;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudReservarSesion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ServicioSesionesAgenda {
    private final RepositorioSesionesAgenda sesiones;
    private final ServicioAutenticacion autenticacion;
    private final Clock reloj;
    private final ServicioVencimientoSesiones vencimiento;
    public ServicioSesionesAgenda(RepositorioSesionesAgenda sesiones,ServicioAutenticacion autenticacion,Clock reloj,ServicioVencimientoSesiones vencimiento) {
        this.sesiones=sesiones;this.autenticacion=autenticacion;this.reloj=reloj;this.vencimiento=vencimiento;
    }
    private LocalDateTime ahora() { return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC); }
    private record Persona(long usuario,long perfil,boolean esTutor) { }
    private Persona identificar(String identificador,boolean esTutor) {
        var usuario=autenticacion.consultarSesion(identificador);
        if(!usuario.roles().contains(esTutor?"tutor":"estudiante"))throw new AccesoRechazado(403,"No tienes acceso a esta opción.");
        Long perfil=esTutor?sesiones.tutor(usuario.id()):sesiones.estudiante(usuario.id());
        if(perfil==null)throw new AccesoRechazado(404,"No se encontró tu perfil.");
        return new Persona(usuario.id(),perfil,esTutor);
    }
    private void validarPagina(int pagina,int tamano) {
        if(pagina<1||tamano<1||tamano>100)throw new SolicitudRechazada(422,"Revisa la página solicitada.","pagina");
    }
    private void validarFechas(LocalDateTime desde,LocalDateTime hasta,boolean obligatorio) {
        if(obligatorio&&(desde==null||hasta==null)||desde!=null&&hasta!=null&&(!hasta.isAfter(desde)||Duration.between(desde,hasta).toDays()>62))
            throw new SolicitudRechazada(422,"Selecciona un intervalo válido de hasta dos meses.","desde");
    }
    @Transactional
    public PaginaSesiones listar(String identificador,boolean esTutor,String estado,LocalDateTime desde,LocalDateTime hasta,int pagina,int tamano) {
        var persona=identificar(identificador,esTutor);validarPagina(pagina,tamano);validarFechas(desde,hasta,false);
        if(estado!=null&&!List.of("solicitada","confirmada","rechazada","cancelada","completada","no_asistio","vencida").contains(estado))throw new SolicitudRechazada(422,"Selecciona un estado válido.","estado");
        vencimiento.vencerPropias(persona.perfil(),esTutor,ahora());
        long total=sesiones.contar(persona.perfil(),esTutor,estado,desde,hasta);int paginas=Math.toIntExact((total+tamano-1)/tamano);
        int actual=Math.min(pagina,Math.max(1,paginas));
        return new PaginaSesiones(sesiones.listar(persona.perfil(),esTutor,estado,desde,hasta,tamano,(actual-1L)*tamano,ahora()),actual,tamano,total,paginas);
    }
    @Transactional
    public List<RespuestaSesionAgenda> agenda(String identificador,LocalDateTime desde,LocalDateTime hasta) {
        var persona=identificar(identificador,false);validarFechas(desde,hasta,true);
        vencimiento.vencerPropias(persona.perfil(),false,ahora());
        return sesiones.agenda(persona.perfil(),desde,hasta,ahora());
    }
    @Transactional
    public ResumenSesionesEstudiante resumen(String identificador) {
        var persona=identificar(identificador,false);vencimiento.vencerPropias(persona.perfil(),false,ahora());return new ResumenSesionesEstudiante(sesiones.proximas(persona.perfil(),ahora()),sesiones.actividades(persona.perfil()));
    }
    public List<OpcionSesionEstudiante> opciones(String identificador) { return sesiones.opciones(identificar(identificador,false).perfil()); }
    public PaginaDisponibilidades disponibles(String identificador,long periodo,int pagina,int tamano) {
        var persona=identificar(identificador,false);validarPagina(pagina,tamano);
        if(sesiones.opciones(persona.perfil()).stream().noneMatch(opcion->opcion.periodoId()==periodo))throw new SolicitudRechazada(409,"No tienes un tutor asignado en ese periodo.","periodoId");
        var momento=ahora();long total=sesiones.contarDisponibles(persona.perfil(),periodo,momento);
        int paginas=Math.toIntExact((total+tamano-1)/tamano),actual=Math.min(pagina,Math.max(1,paginas));
        return new PaginaDisponibilidades(sesiones.disponibles(persona.perfil(),periodo,momento,tamano,(actual-1L)*tamano),actual,tamano,total,paginas);
    }
    private void bloquearParticipantes(long estudiante,long tutor) {
        // El mismo orden en todas las reservas evita que dos solicitudes se crucen al guardar.
        if(!sesiones.bloquearEstudiante(estudiante)||!sesiones.bloquearTutor(tutor))throw new AccesoRechazado(409,"La sesión ya no está disponible.");
    }
    private void validarAsignacion(long estudiante,long tutor,long periodo) {
        if(!sesiones.participantesActivos(estudiante,tutor)||!sesiones.inscripcionVigente(estudiante,tutor,periodo))
            throw new SolicitudRechazada(409,"No hay una asignación activa con este tutor en el periodo seleccionado.","periodoId");
    }
    @Transactional
    public RespuestaSesionAgenda solicitar(String identificador,SolicitudReservarSesion solicitud) {
        var persona=identificar(identificador,false);
        var horario=sesiones.horario(solicitud.disponibilidadId(),false);
        if(horario==null)throw new AccesoRechazado(404,"No se encontró ese horario.");
        bloquearParticipantes(persona.perfil(),horario.tutor());
        horario=sesiones.horario(solicitud.disponibilidadId(),true);
        if(horario==null||!horario.estado().equals("habilitada")||!horario.inicio().isAfter(ahora()))throw new AccesoRechazado(409,"Ese horario ya no está disponible.");
        validarAsignacion(persona.perfil(),horario.tutor(),solicitud.periodoId());
        if(sesiones.ocupado(persona.perfil(),horario.tutor(),horario.inicio(),horario.fin(),null))throw new AccesoRechazado(409,"El horario se cruza con una sesión ya reservada.");
        long id=sesiones.crear(persona.perfil(),solicitud.periodoId(),horario,solicitud.tema().strip(),solicitud.mensaje()==null?null:solicitud.mensaje().strip(),persona.usuario());
        sesiones.registrarCambio(id,persona.usuario(),"solicitud",null,"solicitada",horario.inicio(),horario.fin());
        return sesiones.respuesta(id,persona.perfil(),false,ahora());
    }
    @Transactional
    public RespuestaSesionAgenda cambiar(String identificador,boolean esTutor,long id,String accion) {
        var persona=identificar(identificador,esTutor);
        if(!List.of("confirmar","rechazar","cancelar").contains(accion)||!esTutor&&!accion.equals("cancelar"))throw new AccesoRechazado(403,"No puedes realizar esta acción.");
        var sesion=sesiones.sesion(id,persona.perfil(),esTutor,false);
        if(sesion==null)throw new AccesoRechazado(404,"No se encontró esa sesión.");
        bloquearParticipantes(sesion.estudiante(),sesion.tutor());
        if(sesion.disponibilidad()!=null)sesiones.horario(sesion.disponibilidad(),true);
        sesion=sesiones.sesion(id,persona.perfil(),esTutor,true);
        if(sesion==null)throw new AccesoRechazado(404,"No se encontró esa sesión.");
        boolean cancelar=accion.equals("cancelar");
        if(!sesion.inicio().isAfter(ahora())||!(cancelar?List.of("solicitada","confirmada").contains(sesion.estado()):sesion.estado().equals("solicitada")))
            throw new AccesoRechazado(409,"La sesión ya no permite esta acción.");
        String nuevo=cancelar?"cancelada":accion.equals("confirmar")?"confirmada":"rechazada";
        if(nuevo.equals("confirmada")) {
            validarAsignacion(sesion.estudiante(),sesion.tutor(),sesion.periodo());
            if(sesiones.ocupado(sesion.estudiante(),sesion.tutor(),sesion.inicio(),sesion.fin(),id))throw new AccesoRechazado(409,"El horario se cruza con otra sesión.");
            if(sesion.disponibilidad()!=null){var horario=sesiones.horario(sesion.disponibilidad(),true);if(horario==null||!horario.estado().equals("habilitada"))throw new AccesoRechazado(409,"Ese horario ya no está disponible.");}
        }
        sesiones.cambiarEstado(id,nuevo);
        sesiones.registrarCambio(id,persona.usuario(),cancelar?"cancelacion":accion.equals("confirmar")?"aceptacion":"rechazo",sesion.estado(),nuevo,sesion.inicio(),sesion.fin());
        return sesiones.respuesta(id,persona.perfil(),esTutor,ahora());
    }
}
