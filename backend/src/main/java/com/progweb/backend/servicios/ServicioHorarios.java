package com.progweb.backend.servicios;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.errores.SolicitudRechazada;
import com.progweb.backend.repositorios.RepositorioDisponibilidad;
import com.progweb.backend.respuestas.RespuestaHorario;
import com.progweb.backend.solicitudes.SolicitudHorario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioHorarios {
    // El formulario actual de "Mis horarios" solo pide un texto de ubicación, sin un
    // selector de modalidad. Mientras esa pantalla no lo tenga, se considera virtual
    // cuando el texto parece un enlace; el resto se guarda como presencial.
    private static final Pattern ENLACE_VIRTUAL = Pattern.compile("^(https?://|www\\.)", Pattern.CASE_INSENSITIVE);

    private final RepositorioDisponibilidad repositorio;
    private final ServicioAutenticacion autenticacion;
    private final Clock reloj;

    public ServicioHorarios(RepositorioDisponibilidad repositorio, ServicioAutenticacion autenticacion, Clock reloj) {
        this.repositorio = repositorio;
        this.autenticacion = autenticacion;
        this.reloj = reloj;
    }

    // Comprueba que la cuenta siga activa y tenga rol de tutor, y obtiene su propio
    // tutor_id. Este identificador nunca se recibe desde React; siempre sale del JWT.
    private long verificarTutor(String identificador) {
        var usuario = autenticacion.consultarSesion(identificador);
        if (!usuario.roles().contains("tutor")) {
            throw new AccesoRechazado(403, "Esta operación requiere una cuenta de tutor.");
        }
        return repositorio.obtenerTutorIdPorUsuario(usuario.id())
                .orElseThrow(() -> new AccesoRechazado(403, "La cuenta no tiene un perfil de tutor asociado."));
    }

    public List<RespuestaHorario> listarPropios(String identificador) {
        long tutorId = verificarTutor(identificador);
        return repositorio.listarPorTutor(tutorId);
    }

    private void validarIntervalo(SolicitudHorario solicitud) {
        if (!solicitud.finEn().isAfter(solicitud.inicioEn())) {
            throw new SolicitudRechazada(422, "La hora de fin debe ser posterior a la hora de inicio.", "finEn");
        }
        if (solicitud.inicioEn().isBefore(LocalDateTime.now(reloj))) {
            throw new SolicitudRechazada(422, "La hora de inicio debe ser futura.", "inicioEn");
        }
    }

    private record Lugar(String modalidad, String ubicacion, String enlaceVirtual) {
    }

    private Lugar clasificarUbicacion(String ubicacion) {
        String texto = ubicacion.strip();
        if (ENLACE_VIRTUAL.matcher(texto).find()) {
            return new Lugar("virtual", null, texto);
        }
        if (texto.length() > 200) {
            throw new SolicitudRechazada(422, "La ubicación no debe superar 200 caracteres.", "ubicacion");
        }
        return new Lugar("presencial", texto, null);
    }

    @Transactional
    public RespuestaHorario crear(SolicitudHorario solicitud, String identificador) {
        long tutorId = verificarTutor(identificador);
        validarIntervalo(solicitud);
        Lugar lugar = clasificarUbicacion(solicitud.ubicacion());
        if (repositorio.seCruzaConOtro(tutorId, solicitud.inicioEn(), solicitud.finEn(), null)) {
            throw new SolicitudRechazada(409, "Ya tienes un horario registrado que se cruza con este.", "inicioEn");
        }
        long id = repositorio.crear(tutorId, solicitud.inicioEn(), solicitud.finEn(),
                lugar.modalidad(), lugar.ubicacion(), lugar.enlaceVirtual());
        return buscarPropio(id);
    }

    @Transactional
    public RespuestaHorario editar(long id, SolicitudHorario solicitud, String identificador) {
        long tutorId = verificarTutor(identificador);
        if (!repositorio.perteneceATutor(id, tutorId)) {
            throw new AccesoRechazado(404, "No se encontró ese horario.");
        }
        if (repositorio.tieneSesionAsociada(id)) {
            throw new SolicitudRechazada(409, "No se puede editar un horario que ya tiene sesiones registradas.", "id");
        }
        validarIntervalo(solicitud);
        Lugar lugar = clasificarUbicacion(solicitud.ubicacion());
        if (repositorio.seCruzaConOtro(tutorId, solicitud.inicioEn(), solicitud.finEn(), id)) {
            throw new SolicitudRechazada(409, "Ya tienes un horario registrado que se cruza con este.", "inicioEn");
        }
        repositorio.actualizar(id, solicitud.inicioEn(), solicitud.finEn(),
                lugar.modalidad(), lugar.ubicacion(), lugar.enlaceVirtual());
        return buscarPropio(id);
    }

    @Transactional
    public void eliminar(long id, String identificador) {
        long tutorId = verificarTutor(identificador);
        if (!repositorio.perteneceATutor(id, tutorId)) {
            throw new AccesoRechazado(404, "No se encontró ese horario.");
        }
        if (repositorio.tieneSesionAsociada(id)) {
            throw new SolicitudRechazada(409, "No se puede eliminar un horario que ya tiene sesiones registradas.", "id");
        }
        repositorio.eliminar(id);
    }

    private RespuestaHorario buscarPropio(long id) {
        return repositorio.buscar(id).orElseThrow(() -> new AccesoRechazado(404, "No se encontró ese horario."));
    }
}
