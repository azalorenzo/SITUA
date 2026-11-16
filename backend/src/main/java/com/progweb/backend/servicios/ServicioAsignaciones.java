package com.progweb.backend.servicios;

import java.util.List;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.errores.SolicitudRechazada;
import com.progweb.backend.repositorios.RepositorioAsignaciones;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioAsignaciones {
    private final RepositorioAsignaciones repositorio;
    private final ServicioAutenticacion autenticacion;
    public ServicioAsignaciones(RepositorioAsignaciones repositorio, ServicioAutenticacion autenticacion) {
        this.repositorio = repositorio; this.autenticacion = autenticacion;
    }

    private void validarFiltros(Long periodoId, String estado, String busqueda) {
        if (periodoId != null && periodoId < 1) throw new SolicitudRechazada(422, "El periodo no es válido.", "periodoId");
        if (estado != null && !List.of("activa", "pendiente", "finalizada", "cancelada").contains(estado))
            throw new SolicitudRechazada(422, "El estado no es válido.", "estado");
        if (busqueda.length() > 160) throw new SolicitudRechazada(422, "La búsqueda no debe superar 160 caracteres.", "busqueda");
    }

    public PaginaAsignaciones listar(int pagina, int tamano, Long periodoId, String estado, String busqueda) {
        validarFiltros(periodoId, estado, busqueda);
        if (pagina < 1) throw new SolicitudRechazada(422, "La página debe ser mayor que cero.", "pagina");
        if (tamano < 1 || tamano > 100) throw new SolicitudRechazada(422, "El tamaño de página debe estar entre 1 y 100.", "tamano");
        String termino = busqueda.strip();
        long total = repositorio.contar(periodoId, estado, termino);
        int paginas = Math.toIntExact((total + tamano - 1) / tamano);
        // Si se borró o cambió el último registro, regresamos a la última página disponible.
        int actual = paginas == 0 ? 1 : Math.min(pagina, paginas);
        var contenido = repositorio.listarPagina(periodoId, estado, termino, tamano, (actual - 1L) * tamano);
        return new PaginaAsignaciones(contenido, actual, tamano, total, paginas, repositorio.resumen(periodoId));
    }

    public List<RespuestaAsignacion> exportar(Long periodoId, String estado, String busqueda) {
        validarFiltros(periodoId, estado, busqueda);
        return repositorio.exportar(periodoId, estado, busqueda.strip());
    }
    public List<RespuestaPeriodo> periodos() { return repositorio.periodos(); }
    public List<RespuestaEstudianteAsignable> estudiantes(long periodoId) { return repositorio.estudiantes(periodoId); }

    private long verificarAutor(String identificador) {
        var usuario = autenticacion.consultarSesion(identificador);
        if (!usuario.roles().contains("coordinador")) throw new AccesoRechazado(403, "Esta operación requiere acceso de coordinación.");
        return usuario.id();
    }

    private void validarDestino(long estudianteId, long tutorId, long periodoId) {
        if (!repositorio.cuentaActiva(estudianteId)) throw new SolicitudRechazada(409, "La cuenta del estudiante está inactiva.", "estudianteId");
        if (!repositorio.periodoActivo(periodoId)) throw new SolicitudRechazada(409, "Selecciona un periodo activo.", "periodoId");
        if (!repositorio.tieneExpediente(estudianteId, periodoId)) throw new SolicitudRechazada(409, "El estudiante necesita un expediente con inscripción activa en este periodo.", "estudianteId");
        if (!repositorio.tutorActivo(tutorId)) throw new SolicitudRechazada(409, "El tutor no existe o su cuenta está inactiva.", "tutorId");
    }

    @Transactional
    public RespuestaAsignacion crear(SolicitudAsignacion solicitud, String identificador) {
        long autor = verificarAutor(identificador);
        if (!repositorio.bloquearEstudiante(solicitud.estudianteId())) throw new AccesoRechazado(404, "No se encontró el estudiante.");
        validarDestino(solicitud.estudianteId(), solicitud.tutorId(), solicitud.periodoId());
        if (repositorio.tieneAsignacion(solicitud.estudianteId(), solicitud.periodoId())) throw new AccesoRechazado(409, "El estudiante ya tiene una asignación activa en este periodo.");
        long id = repositorio.crear(solicitud.estudianteId(), solicitud.tutorId(), solicitud.periodoId(), autor, solicitud.motivo());
        return buscar(id);
    }

    private RespuestaAsignacion buscar(long id) {
        return repositorio.buscar(id).orElseThrow(() -> new AccesoRechazado(404, "No se encontró la asignación."));
    }

    private RespuestaAsignacion bloquear(long id) {
        var encontrada = buscar(id);
        repositorio.bloquearEstudiante(encontrada.estudianteId());
        return buscar(id);
    }

    @Transactional
    public RespuestaAsignacion cerrar(long id, String estado, String identificador) {
        verificarAutor(identificador);
        var actual = bloquear(id);
        if (!List.of("activa", "pendiente").contains(actual.estado())) throw new AccesoRechazado(409, "La asignación ya está cerrada.");
        if (!repositorio.cerrar(id, estado)) throw new AccesoRechazado(409, "No se puede cerrar una asignación cuyo inicio aún no ocurre.");
        return buscar(id);
    }

    @Transactional
    public RespuestaAsignacion cambiarTutor(long id, SolicitudCambioTutor solicitud, String identificador) {
        long autor = verificarAutor(identificador);
        var actual = bloquear(id);
        if (!"activa".equals(actual.estado())) throw new AccesoRechazado(409, "Solo se puede cambiar el tutor de una asignación activa.");
        if (actual.tutorId().equals(solicitud.tutorId())) throw new SolicitudRechazada(409, "Selecciona un tutor diferente al actual.", "tutorId");
        validarDestino(actual.estudianteId(), solicitud.tutorId(), actual.periodoId());
        // Guardamos el cambio como otra asignación para conservar quién fue su tutor antes.
        if (!repositorio.cerrar(id, "finalizada")) throw new AccesoRechazado(409, "No se puede cambiar una asignación cuyo inicio aún no ocurre.");
        long nueva = repositorio.crear(actual.estudianteId(), solicitud.tutorId(), actual.periodoId(), autor, solicitud.motivo());
        return buscar(nueva);
    }
}
