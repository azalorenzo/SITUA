package com.progweb.backend.servicios;

import java.util.List;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioReportesTutor;
import com.progweb.backend.respuestas.RespuestaPanelReportesTutor;
import com.progweb.backend.respuestas.RespuestaReporte;
import com.progweb.backend.respuestas.RespuestaReporteTutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Reportes y métricas del tutor conectado. La identidad sale siempre del JWT.
@Service
@Transactional(readOnly = true)
public class ServicioReportesTutor {
    private static final int MAXIMO_REPORTES = 500;
    private static final int MAXIMO_ESTUDIANTES_GRAFICA = 12;

    private final RepositorioReportesTutor reportes;
    private final ServicioPdfReportes generador;
    private final ServicioAutenticacion autenticacion;

    public ServicioReportesTutor(RepositorioReportesTutor reportes, ServicioPdfReportes generador, ServicioAutenticacion autenticacion) {
        this.reportes = reportes;
        this.generador = generador;
        this.autenticacion = autenticacion;
    }

    private record Identidad(long tutor, String nombre) { }

    private Identidad identidad(String identificador) {
        var usuario = autenticacion.consultarSesion(identificador);
        if (!usuario.roles().contains("tutor")) {
            throw new AccesoRechazado(403, "Esta operación requiere una cuenta de tutor.");
        }
        long tutor = reportes.buscarTutorId(usuario.id())
                .orElseThrow(() -> new AccesoRechazado(404, "Tu cuenta no tiene un perfil de tutor."));
        return new Identidad(tutor, usuario.nombre());
    }

    public RespuestaPanelReportesTutor panel(String identificador) {
        Identidad tutor = identidad(identificador);
        List<RespuestaReporteTutor> lista = reportes.reportes(tutor.tutor(), MAXIMO_REPORTES).stream()
                .map(r -> new RespuestaReporteTutor(r.id(), r.estudiante(), r.fecha(), r.motivo(), r.estado())).toList();
        return new RespuestaPanelReportesTutor(tutor.nombre(), lista, reportes.estudiantesAtendidos(tutor.tutor()),
                reportes.asistenciaPorcentaje(tutor.tutor()), reportes.promedioGeneral(tutor.tutor()), reportes.horasTutoria(tutor.tutor()),
                reportes.rendimiento(tutor.tutor()), reportes.progreso(tutor.tutor(), MAXIMO_ESTUDIANTES_GRAFICA));
    }

    // Reutiliza el generador de PDF de coordinación con los reportes de este tutor.
    public byte[] pdfIndividual(String identificador, long reporteId) {
        Identidad tutor = identidad(identificador);
        RespuestaReporte reporte = reportes.reporte(tutor.tutor(), reporteId)
                .orElseThrow(() -> new AccesoRechazado(404, "No se encontró el reporte."));
        return generador.generar(List.of(reporte), null);
    }

    public byte[] pdfTodos(String identificador) {
        Identidad tutor = identidad(identificador);
        List<RespuestaReporte> lista = reportes.reportes(tutor.tutor(), MAXIMO_REPORTES);
        if (lista.isEmpty()) {
            throw new AccesoRechazado(404, "Todavía no tienes reportes para exportar.");
        }
        return generador.generar(lista, null);
    }
}
