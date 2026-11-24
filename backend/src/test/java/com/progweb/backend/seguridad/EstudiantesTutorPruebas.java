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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Pruebas de los estudiantes asignados al tutor. Los datos se revierten al terminar cada prueba.
@SpringBootTest
@Transactional
class EstudiantesTutorPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired JdbcTemplate consultas;
    MockMvc cliente;
    long usuarioTutor, tutor, otroTutor, periodo, carrera;
    long ana, beto, carla, dario, elena;
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

    // Crea un estudiante con expediente en el periodo indicado y devuelve el id de su situación académica.
    long[] estudiante(String nombre, String matricula, long periodoId, int semestre) {
        long usuario = crearUsuario(nombre, "tutor-est-" + matricula.toLowerCase(), "estudiante");
        long estudiante = insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)", usuario, matricula);
        long situacion = insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,?)",
                estudiante, periodoId, carrera, semestre);
        return new long[] { estudiante, situacion, usuario };
    }

    void asignar(long estudiante, long tutorId, long periodoId, String estado, long actor) {
        consultas.update("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,fin_en,estado) VALUES (?,?,?,?,?,?,?)",
                estudiante, tutorId, periodoId, actor, "2026-08-01 12:00:00", "activa".equals(estado) || "pendiente".equals(estado) ? null : "2026-09-01 12:00:00", estado);
    }

    void medicion(long situacion, String fecha, String promedio, String riesgo, String condicion, long autor) {
        consultas.update("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,nivel_riesgo,condicion,registrado_por) VALUES (?,?,?,?,?,?)",
                situacion, fecha, promedio, riesgo, condicion, autor);
    }

    void sesion(long estudiante, long tutorId, long periodoId, String tema, String inicio, String fin, String estado,
            String inicioReal, String finReal, String observaciones, long autor) {
        consultas.update("""
            INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,estado,inicio_real_en,fin_real_en,observaciones,creado_por)
            VALUES (?,?,?,?,?,?,'presencial','Aula de pruebas',?,?,?,?,?)
            """, tutorId, estudiante, periodoId, tema, inicio, fin, estado, inicioReal, finReal, observaciones, autor);
    }

    void observacion(long situacion, long estudiante, long periodoId, long autor, String tipo, String prioridad, String texto, String fecha) {
        consultas.update("INSERT INTO observaciones_estudiante(situacion_id,estudiante_id,periodo_id,autor_id,tipo,prioridad,contenido,observado_en) VALUES (?,?,?,?,?,?,?,?)",
                situacion, estudiante, periodoId, autor, tipo, prioridad, texto, fecha);
    }

    ResultActions consultar(String autorizacion) throws Exception {
        return cliente.perform(get("/api/tutor/estudiantes").header("Authorization", autorizacion));
    }

    ResultActions detalle(String autorizacion, long estudianteId) throws Exception {
        return cliente.perform(get("/api/tutor/estudiantes/" + estudianteId).header("Authorization", autorizacion));
    }

    @BeforeEach
    void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        long departamento = insertar("INSERT INTO departamentos(nombre) VALUES (?)", "Departamento de estudiantes de prueba");
        carrera = insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)", departamento, "EST-TUTOR", "Carrera de pruebas");
        usuarioTutor = crearUsuario("Tutora de prueba", "tutor-estudiantes", "tutor");
        long usuarioOtroTutor = crearUsuario("Otro tutor de prueba", "tutor-estudiantes-otro", "tutor");
        long usuarioCoordinador = crearUsuario("Coordinación de prueba", "tutor-estudiantes-coord", "coordinador");
        tutor = insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioTutor, departamento);
        otroTutor = insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioOtroTutor, departamento);
        periodo = insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)",
                "TUT-EST-A", "Periodo activo de prueba", "2026-08-01", "2026-12-31", "activo");
        long cerrado = insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)",
                "TUT-EST-C", "Periodo cerrado de prueba", "2025-08-01", "2025-12-31", "cerrado");
        autorizacionTutor = token(usuarioTutor, "tutor");
        autorizacionOtroTutor = token(usuarioOtroTutor, "tutor");
        autorizacionCoordinador = token(usuarioCoordinador, "coordinador");

        // Ana: asignada, con dos mediciones, dos sesiones completadas, una confirmada y observaciones de dos autores.
        long[] a = estudiante("Alumna Ana", "TUT-ANA", periodo, 4);
        ana = a[0];
        consultas.update("UPDATE usuarios SET telefono='555-0101' WHERE id=?", a[2]);
        asignar(ana, tutor, periodo, "activa", usuarioCoordinador);
        medicion(a[1], "2026-09-01 12:00:00", "6.00", "alto", "Medición antigua", usuarioCoordinador);
        medicion(a[1], "2026-11-01 12:00:00", "7.50", "medio", "Materias en repetición", usuarioCoordinador);
        sesion(ana, tutor, periodo, "Cálculo", "2026-10-01 15:00:00", "2026-10-01 16:00:00", "completada",
                "2026-10-01 15:00:00", "2026-10-01 16:30:00", "Practicar derivadas", usuarioTutor);
        sesion(ana, tutor, periodo, "Álgebra", "2026-10-08 15:00:00", "2026-10-08 16:00:00", "completada",
                null, null, null, usuarioTutor);
        sesion(ana, tutor, periodo, "Física", "2026-10-15 15:00:00", "2026-10-15 16:00:00", "confirmada",
                null, null, null, usuarioTutor);
        observacion(a[1], ana, periodo, usuarioTutor, "alerta", "alta", "Requiere atención en matemáticas", "2026-10-10 16:00:00");
        observacion(a[1], ana, periodo, usuarioTutor, "progreso", "normal", "Mejora en las últimas semanas", "2026-10-20 16:00:00");
        observacion(a[1], ana, periodo, usuarioCoordinador, "nota", "normal", "Observación de coordinación", "2026-10-25 16:00:00");

        // Beto: asignado al tutor pero sin ninguna medición todavía.
        long[] b = estudiante("Alumno Beto", "TUT-BETO", periodo, 2);
        beto = b[0];
        asignar(beto, tutor, periodo, "activa", usuarioCoordinador);

        // Carla: asignada a otro tutor. Darío: asignación finalizada. Elena: asignada en un periodo cerrado.
        long[] c = estudiante("Alumna Carla", "TUT-CARLA", periodo, 3);
        carla = c[0];
        asignar(carla, otroTutor, periodo, "activa", usuarioCoordinador);
        long[] d = estudiante("Alumno Darío", "TUT-DARIO", periodo, 5);
        dario = d[0];
        asignar(dario, tutor, periodo, "finalizada", usuarioCoordinador);
        long[] e = estudiante("Alumna Elena", "TUT-ELENA", cerrado, 6);
        elena = e[0];
        asignar(elena, tutor, cerrado, "activa", usuarioCoordinador);

        long usuarioEstudiante = crearUsuario("Estudiante sin tutoría", "tutor-estudiantes-solo", "estudiante");
        autorizacionEstudiante = token(usuarioEstudiante, "estudiante");
    }

    @Test
    void listaSoloEstudiantesActivosDelTutorEnElPeriodoActivo() throws Exception {
        consultar(autorizacionTutor).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(ana)).andExpect(jsonPath("$[0].nombre").value("Alumna Ana"))
                .andExpect(jsonPath("$[0].matricula").value("TUT-ANA")).andExpect(jsonPath("$[0].telefono").value("555-0101"))
                .andExpect(jsonPath("$[0].carrera").value("Carrera de pruebas")).andExpect(jsonPath("$[0].semestre").value(4))
                .andExpect(jsonPath("$[0].periodo").value("Periodo activo de prueba"))
                .andExpect(jsonPath("$[1].id").value(beto));
    }

    @Test
    void usaLaUltimaMedicionYElResumenDeSesiones() throws Exception {
        consultar(autorizacionTutor).andExpect(jsonPath("$[0].promedio").value(closeTo(7.5, 0.001)))
                .andExpect(jsonPath("$[0].nivelRiesgo").value("medio")).andExpect(jsonPath("$[0].condicion").value("Materias en repetición"))
                .andExpect(jsonPath("$[0].sesionesCompletadas").value(2))
                .andExpect(jsonPath("$[0].ultimaSesion", startsWith("2026-10-08T15:00")));
    }

    @Test
    void unEstudianteSinMedicionNiSesionesApareceConValoresVacios() throws Exception {
        consultar(autorizacionTutor).andExpect(jsonPath("$[1].nombre").value("Alumno Beto"))
                .andExpect(jsonPath("$[1].promedio").value(nullValue())).andExpect(jsonPath("$[1].nivelRiesgo").value(nullValue()))
                .andExpect(jsonPath("$[1].sesionesCompletadas").value(0)).andExpect(jsonPath("$[1].ultimaSesion").value(nullValue()));
    }

    @Test
    void elDetalleIncluyeSesionesCompletadasYObservacionesPropias() throws Exception {
        detalle(autorizacionTutor, ana).andExpect(status().isOk()).andExpect(jsonPath("$.estudiante.nombre").value("Alumna Ana"))
                .andExpect(jsonPath("$.sesiones.length()").value(2))
                .andExpect(jsonPath("$.sesiones[0].tema").value("Álgebra")).andExpect(jsonPath("$.sesiones[0].duracionMinutos").value(60))
                .andExpect(jsonPath("$.sesiones[0].observaciones").value(nullValue()))
                .andExpect(jsonPath("$.sesiones[1].tema").value("Cálculo")).andExpect(jsonPath("$.sesiones[1].duracionMinutos").value(90))
                .andExpect(jsonPath("$.sesiones[1].observaciones").value("Practicar derivadas"))
                .andExpect(jsonPath("$.observaciones.length()").value(2))
                .andExpect(jsonPath("$.observaciones[0].tipo").value("progreso")).andExpect(jsonPath("$.observaciones[0].prioridad").value("normal"))
                .andExpect(jsonPath("$.observaciones[1].tipo").value("alerta")).andExpect(jsonPath("$.observaciones[1].prioridad").value("alta"))
                .andExpect(jsonPath("$.observaciones[1].observadoEn", startsWith("2026-10-10T16:00")));
    }

    @Test
    void elDetalleDeUnEstudianteSinHistorialVieneVacio() throws Exception {
        detalle(autorizacionTutor, beto).andExpect(status().isOk()).andExpect(jsonPath("$.sesiones.length()").value(0))
                .andExpect(jsonPath("$.observaciones.length()").value(0));
    }

    @Test
    void noMuestraEstudiantesDeOtroTutorFinalizadosNiDePeriodosCerrados() throws Exception {
        detalle(autorizacionTutor, carla).andExpect(status().isNotFound());
        detalle(autorizacionTutor, dario).andExpect(status().isNotFound());
        detalle(autorizacionTutor, elena).andExpect(status().isNotFound());
        detalle(autorizacionTutor, 999999999L).andExpect(status().isNotFound());
        // El otro tutor ve a Carla y no ve a los estudiantes de la primera tutora.
        consultar(autorizacionOtroTutor).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(carla));
        detalle(autorizacionOtroTutor, ana).andExpect(status().isNotFound());
    }

    @Test
    void ignoraUnTutorIdEnviadoDesdeElCliente() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes?tutorId=" + otroTutor).header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(ana));
    }

    @Test
    void exigeSesionYRolDeTutor() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes")).andExpect(status().isUnauthorized());
        consultar(autorizacionEstudiante).andExpect(status().isForbidden());
        // Coordinación puede entrar a la ruta, pero sin perfil de tutor no tiene tutorados propios.
        consultar(autorizacionCoordinador).andExpect(status().isForbidden());
        detalle(autorizacionCoordinador, ana).andExpect(status().isForbidden());
    }
    @Test
    void buscaPorNombreYCarreraDesdeElServidor() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes").param("busqueda", "  ana  ").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(ana));
        cliente.perform(get("/api/tutor/estudiantes").param("busqueda", "Carrera de pruebas").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void filtraPorElRiesgoDeLaUltimaMedicion() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes").param("riesgo", "Medio").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(ana));
        cliente.perform(get("/api/tutor/estudiantes").param("riesgo", "alto").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void combinaBusquedaYRiesgo() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes").param("busqueda", "Beto").param("riesgo", "medio").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        cliente.perform(get("/api/tutor/estudiantes").param("busqueda", "Ana").param("riesgo", "medio").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void distingueSinEvaluacionDeSinRiesgo() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes").param("riesgo", "sin_evaluar").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(beto));
        cliente.perform(get("/api/tutor/estudiantes").param("riesgo", "sin_riesgo").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void laBusquedaNoAmpliaLosPermisosDelTutor() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes").param("busqueda", "Carla").param("tutorId", String.valueOf(otroTutor)).header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void trataLosComodinesComoTexto() throws Exception {
        for (String busqueda : List.of("%", "_", "!", "' OR 1=1 --")) {
            cliente.perform(get("/api/tutor/estudiantes").param("busqueda", busqueda).header("Authorization", autorizacionTutor))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void rechazaFiltrosInvalidos() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes").param("riesgo", "inventado").header("Authorization", autorizacionTutor))
                .andExpect(status().isUnprocessableContent());
        cliente.perform(get("/api/tutor/estudiantes").param("busqueda", "a".repeat(201)).header("Authorization", autorizacionTutor))
                .andExpect(status().isUnprocessableContent());
    }

    @Test void paginaLosTutoradosDesdeElServidor() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes/pagina").param("tamano","1").header("Authorization",autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(2)).andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido.length()").value(1)).andExpect(jsonPath("$.contenido[0].id").value(ana));
        cliente.perform(get("/api/tutor/estudiantes/pagina").param("tamano","1").param("pagina","999").header("Authorization",autorizacionTutor))
                .andExpect(jsonPath("$.pagina").value(2)).andExpect(jsonPath("$.contenido[0].id").value(beto));
    }
    @Test void combinaFiltrosConLaPaginacionYRespetaElTutor() throws Exception {
        cliente.perform(get("/api/tutor/estudiantes/pagina").param("busqueda","Ana").param("riesgo","medio").header("Authorization",autorizacionTutor))
                .andExpect(jsonPath("$.totalRegistros").value(1)).andExpect(jsonPath("$.contenido[0].id").value(ana));
        cliente.perform(get("/api/tutor/estudiantes/pagina").param("busqueda","Ana").header("Authorization",autorizacionOtroTutor))
                .andExpect(jsonPath("$.totalRegistros").value(0)).andExpect(jsonPath("$.contenido.length()").value(0));
    }
    @Test void validaParametrosYPersistenLosPermisosEnLaPagina() throws Exception {
        for(String parametro:List.of("pagina=0","tamano=101","riesgo=inventado"))cliente.perform(get("/api/tutor/estudiantes/pagina?"+parametro).header("Authorization",autorizacionTutor)).andExpect(status().isUnprocessableContent());
        cliente.perform(get("/api/tutor/estudiantes/pagina")).andExpect(status().isUnauthorized());
        cliente.perform(get("/api/tutor/estudiantes/pagina").header("Authorization",autorizacionEstudiante)).andExpect(status().isForbidden());
    }

}
