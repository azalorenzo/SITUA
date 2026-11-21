package com.progweb.backend.servicios;

import java.util.List;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioEstudiantesTutor;
import com.progweb.backend.respuestas.RespuestaDetalleEstudianteTutor;
import com.progweb.backend.respuestas.RespuestaEstudianteTutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Estudiantes asignados al tutor conectado. La identidad sale siempre del JWT.
@Service
@Transactional(readOnly = true)
public class ServicioEstudiantesTutor {
    private static final int MAXIMO_SESIONES = 100;
    private static final int MAXIMO_OBSERVACIONES = 100;

    private final RepositorioEstudiantesTutor estudiantes;
    private final ServicioAutenticacion autenticacion;

    public ServicioEstudiantesTutor(RepositorioEstudiantesTutor estudiantes, ServicioAutenticacion autenticacion) {
        this.estudiantes = estudiantes;
        this.autenticacion = autenticacion;
    }

    public List<RespuestaEstudianteTutor> consultar(String identificador) {
        var usuario = autenticacion.consultarSesion(identificador);
        return estudiantes.listar(tutorDe(usuario.roles(), usuario.id()));
    }

    // Un estudiante que no está asignado a este tutor responde igual que uno inexistente.
    public RespuestaDetalleEstudianteTutor consultarDetalle(String identificador, long estudianteId) {
        var usuario = autenticacion.consultarSesion(identificador);
        long tutor = tutorDe(usuario.roles(), usuario.id());
        var estudiante = estudiantes.buscar(tutor, estudianteId)
                .orElseThrow(() -> new AccesoRechazado(404, "No se encontró ese estudiante entre tus tutorados."));
        return new RespuestaDetalleEstudianteTutor(estudiante,
                estudiantes.sesionesCompletadas(tutor, estudianteId, MAXIMO_SESIONES),
                estudiantes.observaciones(usuario.id(), estudianteId, MAXIMO_OBSERVACIONES));
    }

    private long tutorDe(java.util.Collection<String> roles, long usuarioId) {
        if (!roles.contains("tutor")) {
            throw new AccesoRechazado(403, "Esta operación requiere una cuenta de tutor.");
        }
        return estudiantes.buscarTutorId(usuarioId)
                .orElseThrow(() -> new AccesoRechazado(404, "Tu cuenta no tiene un perfil de tutor."));
    }
}
