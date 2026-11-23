package com.progweb.backend.servicios;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioReportesTutor {
    private final ServicioAutenticacion autenticacion;
    private final RepositorioSesionesAgenda agenda;
    private final RepositorioRegistroSesiones registros;
    private final RepositorioReportesTutor propios;
    private final RepositorioReportes reportes;
    private final ServicioPdfReportes pdf;
    private final Clock reloj;
    public ServicioReportesTutor(ServicioAutenticacion autenticacion,RepositorioSesionesAgenda agenda,RepositorioRegistroSesiones registros,
        RepositorioReportesTutor propios,RepositorioReportes reportes,ServicioPdfReportes pdf,Clock reloj){
        this.autenticacion=autenticacion;this.agenda=agenda;this.registros=registros;this.propios=propios;this.reportes=reportes;this.pdf=pdf;this.reloj=reloj;
    }
    private record Persona(long usuario,long tutor) { }
    private Persona identificar(String identificador){
        var usuario=autenticacion.consultarSesion(identificador);if(!usuario.roles().contains("tutor"))throw new AccesoRechazado(403,"Esta opción es para tutores.");
        Long tutor=agenda.tutor(usuario.id());if(tutor==null)throw new AccesoRechazado(404,"No se encontró tu perfil de tutor.");return new Persona(usuario.id(),tutor);
    }
    private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);}
    private void validar(FiltrosReportes filtros){if(filtros.desde()!=null&&filtros.hasta()!=null&&filtros.hasta().isBefore(filtros.desde()))throw new SolicitudRechazada(422,"Revisa las fechas.","hasta");}
    public List<RespuestaMotivoReporte> motivos(String identificador){identificar(identificador);return propios.motivos();}
    public List<RespuestaSesionRealizada> sesiones(String identificador,long estudiante,long periodo){return propios.sesiones(identificar(identificador).tutor(),estudiante,periodo);}
    public PaginaReportes listar(String identificador,FiltrosReportes filtros){
        long tutor=identificar(identificador).tutor();validar(filtros);var resumen=reportes.resumenTutor(tutor,filtros);
        int paginas=Math.toIntExact((resumen.total()+filtros.tamano()-1)/filtros.tamano()),actual=Math.min(filtros.pagina(),Math.max(1,paginas));
        return new PaginaReportes(reportes.listarTutor(tutor,filtros,actual),actual,filtros.tamano(),resumen.total(),paginas,resumen,List.of());
    }
    public AnalisisReportesTutor analisis(String identificador){
        long tutor=identificar(identificador).tutor();var resumen=reportes.resumenTutor(tutor,new FiltrosReportes(null,null,null,null,null,null,null,null));
        var momento=ahora();var primerMes=YearMonth.from(momento).minusMonths(5);var resultados=propios.rendimiento(tutor,primerMes.atDay(1).atStartOfDay(),momento);
        var promedios=resultados.stream().collect(Collectors.toMap(AnalisisReportesTutor.Mes::mes,AnalisisReportesTutor.Mes::promedio));
        List<AnalisisReportesTutor.Mes> meses=new ArrayList<>();var formato=DateTimeFormatter.ofPattern("MMM uuuu",Locale.forLanguageTag("es-MX"));
        for(int indice=0;indice<6;indice++){var mes=primerMes.plusMonths(indice);meses.add(new AnalisisReportesTutor.Mes(mes.format(formato),promedios.get(mes.toString())));}
        return new AnalisisReportesTutor(resumen.total(),resumen.completados(),resumen.pendientes(),propios.estudiantesAtendidos(tutor),propios.promedio(tutor),registros.resumen(tutor).horas(),propios.asistencia(tutor),meses,propios.progreso(tutor));
    }
    @Transactional
    public RespuestaReporte crear(String identificador,SolicitudReporteTutor solicitud){
        var persona=identificar(identificador);
        if(solicitud.fecha().isAfter(ahora().toLocalDate()))throw new SolicitudRechazada(422,"La fecha no puede ser futura.","fecha");
        if(!agenda.bloquearEstudiante(solicitud.estudianteId())||!agenda.bloquearTutor(persona.tutor()))throw new AccesoRechazado(404,"No se encontró el estudiante o tutor.");
        boolean vinculada=solicitud.sesionId()!=null&&propios.sesionPropia(solicitud.sesionId(),persona.tutor(),solicitud.estudianteId(),solicitud.periodoId());
        if(solicitud.sesionId()!=null&&!vinculada)throw new SolicitudRechazada(409,"Selecciona una sesión completada de este estudiante y periodo.","sesionId");
        if(!vinculada&&(!agenda.participantesActivos(solicitud.estudianteId(),persona.tutor())||!agenda.inscripcionVigente(solicitud.estudianteId(),persona.tutor(),solicitud.periodoId())))
            throw new SolicitudRechazada(409,"No tienes una asignación activa con este estudiante en ese periodo.","estudianteId");
        if(!propios.motivoActivo(solicitud.motivoId()))throw new SolicitudRechazada(422,"Selecciona un motivo disponible.","motivoId");
        long id=propios.crear(persona.tutor(),persona.usuario(),ahora(),solicitud);return reportes.buscarTutor(id,persona.tutor()).orElseThrow();
    }
    @Transactional
    public RespuestaReporte completar(String identificador,long id,SolicitudCompletarReporte solicitud){
        var persona=identificar(identificador);String estado=propios.bloquear(id,persona.tutor());
        if(estado==null)throw new AccesoRechazado(404,"No se encontró el reporte.");
        if(!estado.equals("pendiente"))throw new AccesoRechazado(409,"El reporte ya está completado.");
        propios.completar(id,persona.usuario(),ahora(),solicitud.contenido());return reportes.buscarTutor(id,persona.tutor()).orElseThrow();
    }
    public byte[] exportar(String identificador,FiltrosReportes filtros){long tutor=identificar(identificador).tutor();validar(filtros);return pdf.generar(reportes.exportarTutor(tutor,filtros),filtros);}
    public byte[] individual(String identificador,long id){long tutor=identificar(identificador).tutor();var reporte=reportes.buscarTutor(id,tutor).orElseThrow(()->new AccesoRechazado(404,"No se encontró el reporte."));return pdf.generar(List.of(reporte),null);}
}
