package com.progweb.backend.seguridad;

import java.util.List;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.RepositorioRoles;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Pruebas de los reportes y métricas del tutor. Los datos se revierten al terminar cada prueba.
@SpringBootTest
@Transactional
class ReportesTutorPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired JdbcTemplate consultas;
    MockMvc cliente;
    long usuarioTutor, usuarioCoordinador, tutor, otroTutor, periodo, carrera, motivo;
    long ana, beto, gina, reporteAna, reporteBeto, reporteAjeno;
    String autorizacionTutor, autorizacionOtroTutor, autorizacionTutorSinDatos, autorizacionCoordinador, autorizacionEstudiante;

    long insertar(String sql, Object... datos) {
        consultas.update(sql, datos);
        return consultas.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long crearUsuario(String nombre, String correo, String rol) {
        var usuario = new Usuario(nombre, correo + "@example.invalid", null);
        usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());
        return usuarios.saveAndFlush(usuario).obtenerId();
    }

    String token(long usuario, String rol) {
        return "Bearer " + tokens.crearToken(new RespuestaUsuario(usuario, "Prueba", "prueba@example.invalid", null, List.of(rol))).token();
    }

    long[] asignado(String nombre, String matricula, long tutorId) {
        long usuario = crearUsuario(nombre, "rep-" + matricula.toLowerCase(), "estudiante");
        long estudiante = insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)", usuario, matricula);
        long situacion = insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,4)", estudiante, periodo, carrera);
        consultas.update("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,?,'activa')",
                estudiante, tutorId, periodo, usuarioCoordinador, "2026-08-01 12:00:00");
        return new long[] { estudiante, situacion };
    }

    void medicion(long situacion, String fecha, String promedio) {
        consultas.update("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,nivel_riesgo,condicion,registrado_por) VALUES (?,?,?,'medio','Condición de prueba',?)",
                situacion, fecha, promedio, usuarioCoordinador);
    }

    void sesion(long estudiante, long tutorId, String estado, String inicio, String fin, String inicioReal, String finReal) {
        consultas.update("""
            INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,estado,inicio_real_en,fin_real_en,creado_por)
            VALUES (?,?,?,'Tema de prueba',?,?,'presencial','Aula de pruebas',?,?,?,?)
            """, tutorId, estudiante, periodo, inicio, fin, estado, inicioReal, finReal, usuarioTutor);
    }

    long reporte(long estudiante, long tutorId, String fecha, String estado) {
        return insertar("INSERT INTO reportes(estudiante_id,tutor_id,periodo_id,motivo_id,fecha,estado,completado_en,completado_por) VALUES (?,?,?,?,?,?,?,?)",
                estudiante, tutorId, periodo, motivo, fecha, estado,
                "completado".equals(estado) ? "2026-10-20 10:00:00" : null, "completado".equals(estado) ? usuarioCoordinador : null);
    }

    ResultActions panel(String autorizacion) throws Exception {
        return cliente.perform(get("/api/tutor/reportes").header("Authorization", autorizacion));
    }

    @BeforeEach
    void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        long departamento = insertar("INSERT INTO departamentos(nombre) VALUES (?)", "Departamento de reportes de prueba");
        carrera = insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)", departamento, "REP-TUTOR", "Carrera de pruebas");
        usuarioTutor = crearUsuario("Tutora de prueba", "rep-tutor", "tutor");
        long usuarioOtroTutor = crearUsuario("Otro tutor de prueba", "rep-otro-tutor", "tutor");
        long usuarioSinDatos = crearUsuario("Tutor sin datos", "rep-sin-datos", "tutor");
        usuarioCoordinador = crearUsuario("Coordinación de prueba", "rep-coord", "coordinador");
        long usuarioEstudiante = crearUsuario("Estudiante sin tutoría", "rep-solo", "estudiante");
        tutor = insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioTutor, departamento);
        otroTutor = insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioOtroTutor, departamento);
        insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioSinDatos, departamento);
        periodo = insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)",
                "TUT-REP-A", "Periodo activo de prueba", "2020-01-01", "2031-12-31", "activo");
        motivo = insertar("INSERT INTO motivos_reporte(codigo,nombre) VALUES (?,?)", "tut-rep-prueba", "Seguimiento académico de prueba");
        autorizacionTutor = token(usuarioTutor, "tutor");
        autorizacionOtroTutor = token(usuarioOtroTutor, "tutor");
        autorizacionTutorSinDatos = token(usuarioSinDatos, "tutor");
        autorizacionCoordinador = token(usuarioCoordinador, "coordinador");
        autorizacionEstudiante = token(usuarioEstudiante, "estudiante");

        long[] a = asignado("Alumna Ana", "REP-ANA", tutor);
        long[] b = asignado("Alumno Beto", "REP-BETO", tutor);
        long[] g = asignado("Alumna Gina", "REP-GINA", otroTutor);
        ana = a[0]; beto = b[0]; gina = g[0];
        medicion(a[1], "2026-09-15 12:00:00", "6.00");
        medicion(a[1], "2026-10-15 12:00:00", "8.00");
        medicion(b[1], "2026-10-10 12:00:00", "9.00");
        medicion(g[1], "2026-10-10 12:00:00", "5.00");
        sesion(ana, tutor, "completada", "2026-10-01 15:00:00", "2026-10-01 16:00:00", "2026-10-01 15:00:00", "2026-10-01 16:30:00");
        sesion(ana, tutor, "no_asistio", "2026-10-08 15:00:00", "2026-10-08 16:00:00", null, null);
        sesion(beto, tutor, "completada", "2026-10-09 15:00:00", "2026-10-09 16:00:00", null, null);
        sesion(gina, otroTutor, "completada", "2026-10-09 17:00:00", "2026-10-09 18:00:00", null, null);
        reporteAna = reporte(ana, tutor, "2026-10-05", "pendiente");
        reporteBeto = reporte(beto, tutor, "2026-10-12", "completado");
        reporteAjeno = reporte(gina, otroTutor, "2026-10-06", "pendiente");
    }

    @Test
    void elPanelTraeLosReportesDelTutorDelMasNuevoAlMasAntiguo() throws Exception {
        panel(autorizacionTutor).andExpect(status().isOk()).andExpect(jsonPath("$.tutor").value("Tutora de prueba"))
                .andExpect(jsonPath("$.reportes.length()").value(2))
                .andExpect(jsonPath("$.reportes[0].id").value(reporteBeto)).andExpect(jsonPath("$.reportes[0].estudiante").value("Alumno Beto"))
                .andExpect(jsonPath("$.reportes[0].fecha").value("2026-10-12")).andExpect(jsonPath("$.reportes[0].estado").value("completado"))
                .andExpect(jsonPath("$.reportes[0].motivo").value("Seguimiento académico de prueba"))
                .andExpect(jsonPath("$.reportes[1].id").value(reporteAna)).andExpect(jsonPath("$.reportes[1].estado").value("pendiente"));
    }

    @Test
    void calculaLasMetricasDelPeriodoActivo() throws Exception {
        panel(autorizacionTutor).andExpect(jsonPath("$.estudiantesAtendidos").value(2)).andExpect(jsonPath("$.asistenciaPorcentaje").value(67))
                .andExpect(jsonPath("$.horasTutoria").value(closeTo(2.5, 0.001)))
                .andExpect(jsonPath("$.promedioGeneral").value(closeTo(8.5, 0.001)));
    }

    @Test
    void agrupaElRendimientoPorMesYMuestraElProgresoPorEstudiante() throws Exception {
        panel(autorizacionTutor).andExpect(jsonPath("$.rendimiento.length()").value(2))
                .andExpect(jsonPath("$.rendimiento[0].mes").value("2026-09")).andExpect(jsonPath("$.rendimiento[0].promedio").value(closeTo(6.0, 0.001)))
                .andExpect(jsonPath("$.rendimiento[1].mes").value("2026-10")).andExpect(jsonPath("$.rendimiento[1].promedio").value(closeTo(8.5, 0.001)))
                .andExpect(jsonPath("$.progreso.length()").value(2))
                .andExpect(jsonPath("$.progreso[0].estudiante").value("Alumna Ana")).andExpect(jsonPath("$.progreso[0].inicial").value(closeTo(6.0, 0.001)))
                .andExpect(jsonPath("$.progreso[0].actual").value(closeTo(8.0, 0.001)))
                .andExpect(jsonPath("$.progreso[1].estudiante").value("Alumno Beto")).andExpect(jsonPath("$.progreso[1].actual").value(closeTo(9.0, 0.001)));
    }

    @Test
    void sinDatosElPanelVieneVacioYSinMetricas() throws Exception {
        panel(autorizacionTutorSinDatos).andExpect(status().isOk()).andExpect(jsonPath("$.reportes.length()").value(0))
                .andExpect(jsonPath("$.estudiantesAtendidos").value(0)).andExpect(jsonPath("$.asistenciaPorcentaje").value(nullValue()))
                .andExpect(jsonPath("$.promedioGeneral").value(nullValue())).andExpect(jsonPath("$.horasTutoria").value(nullValue()))
                .andExpect(jsonPath("$.rendimiento.length()").value(0)).andExpect(jsonPath("$.progreso.length()").value(0));
    }

    @Test
    void elTutorSoloVeLoSuyo() throws Exception {
        panel(autorizacionOtroTutor).andExpect(jsonPath("$.reportes.length()").value(1)).andExpect(jsonPath("$.reportes[0].id").value(reporteAjeno))
                .andExpect(jsonPath("$.estudiantesAtendidos").value(1)).andExpect(jsonPath("$.promedioGeneral").value(closeTo(5.0, 0.001)));
    }

    @Test
    void descargaElPdfDeSuReporteYDeTodosSusReportes() throws Exception {
        for (String ruta : List.of("/api/tutor/reportes/" + reporteAna + "/pdf", "/api/tutor/reportes/exportar/pdf")) {
            byte[] pdf = cliente.perform(get(ruta).header("Authorization", autorizacionTutor).accept("application/pdf"))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/pdf"))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        }
    }

    @Test
    void noDescargaReportesAjenosInexistentesNiSinReportes() throws Exception {
        cliente.perform(get("/api/tutor/reportes/" + reporteAjeno + "/pdf").header("Authorization", autorizacionTutor)).andExpect(status().isNotFound());
        cliente.perform(get("/api/tutor/reportes/999999999/pdf").header("Authorization", autorizacionTutor)).andExpect(status().isNotFound());
        cliente.perform(get("/api/tutor/reportes/exportar/pdf").header("Authorization", autorizacionTutorSinDatos)).andExpect(status().isNotFound());
    }

    @Test
    void exigeSesionYRolDeTutor() throws Exception {
        cliente.perform(get("/api/tutor/reportes")).andExpect(status().isUnauthorized());
        panel(autorizacionEstudiante).andExpect(status().isForbidden());
        // Coordinación puede entrar a la ruta, pero sin perfil de tutor no tiene reportes propios.
        panel(autorizacionCoordinador).andExpect(status().isForbidden());
        cliente.perform(get("/api/tutor/reportes/" + reporteAna + "/pdf").header("Authorization", autorizacionCoordinador)).andExpect(status().isForbidden());
    }
}
