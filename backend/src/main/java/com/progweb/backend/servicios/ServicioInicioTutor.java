package com.progweb.backend.servicios;
import java.time.*;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaInicioTutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioInicioTutor {
    private final ServicioAutenticacion autenticacion;
    private final RepositorioSesionesAgenda sesiones;
    private final RepositorioInicioTutor inicio;
    private final Clock reloj;
    public ServicioInicioTutor(ServicioAutenticacion autenticacion,RepositorioSesionesAgenda sesiones,RepositorioInicioTutor inicio,Clock reloj){this.autenticacion=autenticacion;this.sesiones=sesiones;this.inicio=inicio;this.reloj=reloj;}
    public RespuestaInicioTutor consultar(String identificador){var usuario=autenticacion.consultarSesion(identificador);if(!usuario.roles().contains("tutor"))throw new AccesoRechazado(403,"Esta opción es para tutores.");Long tutor=sesiones.tutor(usuario.id());if(tutor==null)throw new AccesoRechazado(404,"No se encontró tu perfil.");var ahora=LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);return new RespuestaInicioTutor(inicio.indicadores(tutor,ahora),inicio.estudiantes(tutor),sesiones.proximasTutor(tutor,ahora));}
}
