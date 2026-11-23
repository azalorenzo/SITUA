package com.progweb.backend.servicios;

import java.time.Clock;
import java.time.LocalDateTime;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioInicioTutor;
import com.progweb.backend.respuestas.RespuestaInicioTutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Resumen del inicio del tutor conectado. La identidad sale siempre del JWT.
@Service
@Transactional(readOnly = true)
public class ServicioInicioTutor {
    private static final int ESTUDIANTES_DESTACADOS = 4;

    private final RepositorioInicioTutor inicio;
    private final ServicioAutenticacion autenticacion;
    private final Clock reloj;

    public ServicioInicioTutor(RepositorioInicioTutor inicio, ServicioAutenticacion autenticacion, Clock reloj) {
        this.inicio = inicio;
        this.autenticacion = autenticacion;
        this.reloj = reloj;
    }

    public RespuestaInicioTutor consultar(String identificador) {
        var usuario = autenticacion.consultarSesion(identificador);
        if (!usuario.roles().contains("tutor")) {
            throw new AccesoRechazado(403, "Esta operación requiere una cuenta de tutor.");
        }
        long tutor = inicio.buscarTutorId(usuario.id())
                .orElseThrow(() -> new AccesoRechazado(404, "Tu cuenta no tiene un perfil de tutor."));
        return new RespuestaInicioTutor(inicio.estudiantes(tutor), inicio.sesionesRealizadas(tutor), inicio.reportesPendientes(tutor),
                inicio.sesionesProgramadas(tutor, LocalDateTime.now(reloj)), inicio.estudiantesDestacados(tutor, ESTUDIANTES_DESTACADOS));
    }
}
