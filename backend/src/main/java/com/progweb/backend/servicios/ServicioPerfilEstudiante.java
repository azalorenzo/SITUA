package com.progweb.backend.servicios;
import java.util.List;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.RepositorioPerfilEstudiante;
import com.progweb.backend.respuestas.RespuestaPerfilEstudiante;
import com.progweb.backend.solicitudes.SolicitudCompletarPerfil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioPerfilEstudiante {
    private final RepositorioPerfilEstudiante perfiles;
    private final ServicioAutenticacion autenticacion;
    public ServicioPerfilEstudiante(RepositorioPerfilEstudiante perfiles,ServicioAutenticacion autenticacion) { this.perfiles=perfiles;this.autenticacion=autenticacion; }
    private long identificar(String identificador,boolean bloquear) {
        var usuario=autenticacion.consultarSesion(identificador);
        if(!usuario.roles().contains("estudiante"))throw new AccesoRechazado(403,"Esta opción es para estudiantes.");
        Long estudiante=perfiles.estudiante(usuario.id(),bloquear);
        if(estudiante==null)throw new AccesoRechazado(404,"No se encontró tu perfil de estudiante.");
        return estudiante;
    }
    public RespuestaPerfilEstudiante consultar(String identificador) {
        long estudiante=identificar(identificador,false);
        if(perfiles.completo(estudiante))return new RespuestaPerfilEstudiante(true,List.of(),List.of());
        return new RespuestaPerfilEstudiante(false,perfiles.carreras(),perfiles.periodos());
    }
    @Transactional
    public RespuestaPerfilEstudiante completar(String identificador,SolicitudCompletarPerfil solicitud) {
        long estudiante=identificar(identificador,true);
        // Si ya se guardó, no reemplazamos el expediente ni creamos otro.
        if(!perfiles.completo(estudiante)) {
            if(!perfiles.carreraValida(solicitud.carreraId()))throw new SolicitudRechazada(422,"Selecciona una carrera disponible.","carreraId");
            if(!perfiles.periodoValido(solicitud.periodoId()))throw new SolicitudRechazada(422,"Selecciona un periodo disponible.","periodoId");
            perfiles.completar(estudiante,solicitud.carreraId(),solicitud.periodoId(),solicitud.semestre());
        }
        return new RespuestaPerfilEstudiante(true,List.of(),List.of());
    }
}
