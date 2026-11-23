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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Pruebas del resumen de inicio del tutor. Los datos se revierten al terminar cada prueba.
@SpringBootTest
@Transactional
class InicioTutorPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired JdbcTemplate consultas;
    MockMvc cliente;
    long usuarioTutor, usuarioCoordinador, tutor, otroTutor, periodo, cerrado, carrera;
    long ana, beto, carla, dario, elena, fabio, gina;
    String autorizacionTutor, autorizacionOtroTutor, autorizacionCoordinador, autorizacionEstudiante;

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

    // Estudiante asignado al tutor con la última medición de riesgo indicada (o ninguna).
    long asignado(String nombre, String matricula, long tutorId, long periodoId, String riesgo) {
        long usuario = crearUsuario(nombre, "ini-" + matricula.toLowerCase(), "estudiante");
        long estudiante = insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)", usuario, matricula);
        long situacion = insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)", estudiante, periodoId, carrera);
        consultas.update("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,?,'activa')",
                estudiante, tutorId, periodoId, usuarioCoordinador, "2026-08-01 12:00:00");
        if (riesgo != null) {
            consultas.update("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,nivel_riesgo,condicion,registrado_por) VALUES (?,?,8.00,?,?,?)",
                    situacion, "2026-10-01 12:00:00", riesgo, "Condición de prueba", usuarioCoordinador);
        }
        return estudiante;
    }

    void sesion(long estudiante, long tutorId, long periodoId, String estado, String inicio, String fin) {
        consultas.update("""
            INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,estado,creado_por)
            VALUES (?,?,?,'Tema de prueba',?,?,'presencial','Aula de pruebas',?,?)
            """, tutorId, estudiante, periodoId, inicio, fin, estado, usuarioTutor);
    }

    void reporte(long estudiante, long tutorId, long periodoId, long motivo, String estado) {
        consultas.update("INSERT INTO reportes(estudiante_id,tutor_id,periodo_id,motivo_id,fecha,estado,completado_en,completado_por) VALUES (?,?,?,?,?,?,?,?)",
                estudiante, tutorId, periodoId, motivo, "2026-10-05", estado,
                "completado".equals(estado) ? "2026-10-06 10:00:00" : null, "completado".equals(estado) ? usuarioCoordinador : null);
    }

    ResultActions consultar(String autorizacion) throws Exception {
        return cliente.perform(get("/api/tutor/inicio").header("Authorization", autorizacion));
    }

    @BeforeEach
    void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        long departamento = insertar("INSERT INTO departamentos(nombre) VALUES (?)", "Departamento de inicio de prueba");
        carrera = insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)", departamento, "INI-TUTOR", "Carrera de pruebas");
        usuarioTutor = crearUsuario("Tutora de prueba", "ini-tutor", "tutor");
        long usuarioOtroTutor = crearUsuario("Otro tutor de prueba", "ini-otro-tutor", "tutor");
        usuarioCoordinador = crearUsuario("Coordinación de prueba", "ini-coord", "coordinador");
        long usuarioEstudiante = crearUsuario("Estudiante sin tutoría", "ini-solo", "estudiante");
        tutor = insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioTutor, departamento);
        otroTutor = insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioOtroTutor, departamento);
        periodo = insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)",
                "TUT-INI-A", "Periodo activo de prueba", "2020-01-01", "2031-12-31", "activo");
        cerrado = insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)",
                "TUT-INI-C", "Periodo cerrado de prueba", "2019-01-01", "2019-12-31", "cerrado");
        autorizacionTutor = token(usuarioTutor, "tutor");
        autorizacionOtroTutor = token(usuarioOtroTutor, "tutor");
        autorizacionCoordinador = token(usuarioCoordinador, "coordinador");
        autorizacionEstudiante = token(usuarioEstudiante, "estudiante");
        long motivo = insertar("INSERT INTO motivos_reporte(codigo,nombre) VALUES (?,?)", "tut-ini-prueba", "Motivo de prueba");

        ana = asignado("Alumna Ana", "INI-ANA", tutor, periodo, "alto");
        beto = asignado("Alumno Beto", "INI-BETO", tutor, periodo, "bajo");
        carla = asignado("Alumna Carla", "INI-CARLA", tutor, periodo, null);
        dario = asignado("Alumno Darío", "INI-DARIO", tutor, periodo, "medio");
        elena = asignado("Alumna Elena", "INI-ELENA", tutor, periodo, "sin_riesgo");
        fabio = asignado("Alumno Fabio", "INI-FABIO", tutor, cerrado, "alto");
        gina = asignado("Alumna Gina", "INI-GINA", otroTutor, periodo, "alto");

        sesion(ana, tutor, periodo, "completada", "2026-10-01 15:00:00", "2026-10-01 16:00:00");
        sesion(beto, tutor, periodo, "completada", "2026-10-02 15:00:00", "2026-10-02 16:00:00");
        sesion(fabio, tutor, cerrado, "completada", "2019-10-02 15:00:00", "2019-10-02 16:00:00");
        sesion(gina, otroTutor, periodo, "completada", "2026-10-03 15:00:00", "2026-10-03 16:00:00");
        sesion(ana, tutor, periodo, "confirmada", "2031-03-10 15:00:00", "2031-03-10 16:00:00");
        sesion(beto, tutor, periodo, "confirmada", "2020-03-10 15:00:00", "2020-03-10 16:00:00");
        sesion(carla, tutor, periodo, "solicitada", "2031-03-11 15:00:00", "2031-03-11 16:00:00");
        reporte(ana, tutor, periodo, motivo, "pendiente");
        reporte(beto, tutor, periodo, motivo, "completado");
        reporte(fabio, tutor, cerrado, motivo, "pendiente");
        reporte(gina, otroTutor, periodo, motivo, "pendiente");
    }

    @Test
    void resumeLasCifrasPropiasSinPerderElHistorial() throws Exception {
        consultar(autorizacionTutor).andExpect(status().isOk()).andExpect(jsonPath("$.indicadores.estudiantes").value(5))
                .andExpect(jsonPath("$.indicadores.realizadas").value(3)).andExpect(jsonPath("$.indicadores.reportesPendientes").value(2))
                .andExpect(jsonPath("$.indicadores.programadas").value(2)).andExpect(jsonPath("$.proximas.length()").value(2));
    }

    @Test
    void muestraCincoEstudiantesConSusRiesgosReales() throws Exception {
        consultar(autorizacionTutor).andExpect(jsonPath("$.estudiantes.length()").value(5))
                .andExpect(jsonPath("$.estudiantes[*].id").value(org.hamcrest.Matchers.containsInAnyOrder((int)ana,(int)beto,(int)carla,(int)dario,(int)elena)))
                .andExpect(jsonPath("$.estudiantes[?(@.id == "+ana+")].nivelRiesgo").value(org.hamcrest.Matchers.hasItem("alto")))
                .andExpect(jsonPath("$.estudiantes[?(@.id == "+dario+")].nivelRiesgo").value(org.hamcrest.Matchers.hasItem("medio")));
    }

    @Test
    void otroTutorSoloVeSusPropiasCifras() throws Exception {
        consultar(autorizacionOtroTutor).andExpect(jsonPath("$.indicadores.estudiantes").value(1)).andExpect(jsonPath("$.indicadores.realizadas").value(1))
                .andExpect(jsonPath("$.indicadores.reportesPendientes").value(1)).andExpect(jsonPath("$.indicadores.programadas").value(0))
                .andExpect(jsonPath("$.estudiantes[0].id").value(gina));
    }

    @Test
    void exigeSesionYRolDeTutor() throws Exception {
        cliente.perform(get("/api/tutor/inicio")).andExpect(status().isUnauthorized());
        consultar(autorizacionEstudiante).andExpect(status().isForbidden());
        // Coordinación puede entrar a la ruta, pero sin perfil de tutor no tiene un inicio de tutor.
        consultar(autorizacionCoordinador).andExpect(status().isForbidden());
    }
}
