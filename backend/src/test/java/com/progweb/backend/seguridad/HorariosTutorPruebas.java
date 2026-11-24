package com.progweb.backend.seguridad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.RepositorioRoles;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Pruebas de los horarios del tutor. Los datos se revierten al terminar cada prueba.
@SpringBootTest
@Transactional
class HorariosTutorPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper conversor;
    MockMvc cliente;
    long usuarioTutor, usuarioEstudiante, estudiante, periodo;
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

    @BeforeEach
    void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        long departamento = insertar("INSERT INTO departamentos(nombre) VALUES (?)", "Departamento de horarios de prueba");
        long carrera = insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)", departamento, "HOR-PRUEBA", "Carrera de horarios de prueba");
        usuarioTutor = crearUsuario("Tutora de prueba", "horarios-tutor", "tutor");
        long usuarioOtroTutor = crearUsuario("Otro tutor de prueba", "horarios-otro-tutor", "tutor");
        long usuarioCoordinador = crearUsuario("Coordinación de prueba", "horarios-coordinador", "coordinador");
        usuarioEstudiante = crearUsuario("Estudiante de horarios", "horarios-estudiante", "estudiante");
        insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioTutor, departamento);
        insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)", usuarioOtroTutor, departamento);
        estudiante = insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)", usuarioEstudiante, "HOR-PRUEBA");
        periodo = insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)",
                "HOR-PRUEBA", "Periodo de horarios", "2031-01-01", "2031-12-31", "activo");
        insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)", estudiante, periodo, carrera);
        autorizacionTutor = token(usuarioTutor, "tutor");
        autorizacionOtroTutor = token(usuarioOtroTutor, "tutor");
        autorizacionCoordinador = token(usuarioCoordinador, "coordinador");
        autorizacionEstudiante = token(usuarioEstudiante, "estudiante");
    }

    Map<String, Object> datos(String inicio, String fin) {
        return new HashMap<>(Map.of("inicioEn", "2031-03-10T" + inicio + ":00", "finEn", "2031-03-10T" + fin + ":00", "ubicacion", "Aula de pruebas"));
    }

    ResultActions crear(String autorizacion, Map<String, Object> solicitud) throws Exception {
        return cliente.perform(post("/api/tutor/horarios").header("Authorization", autorizacion)
                .contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(solicitud)));
    }

    ResultActions editar(String autorizacion, long id, Map<String, Object> solicitud) throws Exception {
        return cliente.perform(put("/api/tutor/horarios/" + id).header("Authorization", autorizacion)
                .contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(solicitud)));
    }

    ResultActions eliminar(String autorizacion, long id) throws Exception {
        return cliente.perform(delete("/api/tutor/horarios/" + id).header("Authorization", autorizacion));
    }

    ResultActions consultar(String autorizacion) throws Exception {
        return cliente.perform(get("/api/tutor/horarios").header("Authorization", autorizacion));
    }

    long crearCorrecto(String inicio, String fin) throws Exception {
        String respuesta = crear(autorizacionTutor, datos(inicio, fin)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return conversor.readTree(respuesta).get("id").asLong();
    }

    // Reserva el horario con una sesión del estudiante de prueba, con el estado indicado.
    void reservar(long horario, String estadoSesion) {
        insertar("""
            INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,disponibilidad_id,tema,inicio_en,fin_en,modalidad,ubicacion,enlace_virtual,estado,creado_por)
            SELECT tutor_id,?,?,id,'Tema de prueba',inicio_en,fin_en,modalidad,ubicacion,enlace_virtual,?,? FROM disponibilidades_tutor WHERE id=?
            """, estudiante, periodo, estadoSesion, usuarioEstudiante, horario);
    }

    @Test
    void creaYConsultaHorarioDelTutorConectado() throws Exception {
        crear(autorizacionTutor, datos("10:00", "11:00")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicioEn", startsWith("2031-03-10T10:00")))
                .andExpect(jsonPath("$.finEn", startsWith("2031-03-10T11:00")))
                .andExpect(jsonPath("$.modalidad").value("presencial")).andExpect(jsonPath("$.ubicacion").value("Aula de pruebas"))
                .andExpect(jsonPath("$.enlaceVirtual").value(nullValue())).andExpect(jsonPath("$.estudianteNombre").value(nullValue()));
        consultar(autorizacionTutor).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unEnlaceSeGuardaComoSesionVirtual() throws Exception {
        List<String> enlaces = List.of("https://meet.example.invalid/sala", "HTTP://meet.example.invalid/sala", "www.meet.example.invalid/sala");
        for (int indice = 0; indice < enlaces.size(); indice++) {
            var solicitud = datos("08:00", "08:30");
            solicitud.put("inicioEn", "2031-03-" + (10 + indice) + "T08:00:00");
            solicitud.put("finEn", "2031-03-" + (10 + indice) + "T08:30:00");
            solicitud.put("ubicacion", enlaces.get(indice));
            crear(autorizacionTutor, solicitud).andExpect(status().isCreated()).andExpect(jsonPath("$.modalidad").value("virtual"))
                    .andExpect(jsonPath("$.enlaceVirtual").value(enlaces.get(indice))).andExpect(jsonPath("$.ubicacion").value(nullValue()));
        }
        // La palabra "Virtual" sin enlace se trata como un lugar presencial.
        var texto = datos("12:00", "13:00");
        texto.put("ubicacion", "Virtual");
        crear(autorizacionTutor, texto).andExpect(status().isCreated()).andExpect(jsonPath("$.modalidad").value("presencial"));
    }

    @Test
    void rechazaDatosInvalidosSinGuardar() throws Exception {
        crear(autorizacionTutor, datos("11:00", "10:00")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erroresPorCampo.finEn").isArray());
        crear(autorizacionTutor, datos("10:00", "10:00")).andExpect(status().isUnprocessableEntity());
        var pasado = datos("10:00", "11:00");
        pasado.put("inicioEn", "2020-01-10T10:00:00");
        pasado.put("finEn", "2020-01-10T11:00:00");
        crear(autorizacionTutor, pasado).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.inicioEn").isArray());
        var sinLugar = datos("10:00", "11:00");
        sinLugar.put("ubicacion", "   ");
        crear(autorizacionTutor, sinLugar).andExpect(status().isUnprocessableEntity());
        var lugarLargo = datos("10:00", "11:00");
        lugarLargo.put("ubicacion", "A".repeat(201));
        crear(autorizacionTutor, lugarLargo).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.ubicacion").isArray());
        var sinFechas = new HashMap<String, Object>(Map.of("ubicacion", "Aula"));
        crear(autorizacionTutor, sinFechas).andExpect(status().isUnprocessableEntity());
        consultar(autorizacionTutor).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rechazaHorariosQueSeCruzanPeroPermiteHorasContiguas() throws Exception {
        crearCorrecto("10:00", "11:00");
        crear(autorizacionTutor, datos("10:30", "11:30")).andExpect(status().isConflict());
        crear(autorizacionTutor, datos("09:00", "10:30")).andExpect(status().isConflict());
        crear(autorizacionTutor, datos("11:00", "12:00")).andExpect(status().isCreated());
        // El mismo horario no choca con el de otro tutor.
        crear(autorizacionOtroTutor, datos("10:00", "11:00")).andExpect(status().isCreated());
    }

    @Test
    void editaHorarioPropioSinCruzarseConsigoMismo() throws Exception {
        long id = crearCorrecto("10:00", "11:00");
        var cambio = datos("10:00", "12:00");
        cambio.put("ubicacion", "https://meet.example.invalid/nueva");
        editar(autorizacionTutor, id, cambio).andExpect(status().isOk()).andExpect(jsonPath("$.finEn", startsWith("2031-03-10T12:00")))
                .andExpect(jsonPath("$.modalidad").value("virtual")).andExpect(jsonPath("$.enlaceVirtual").value("https://meet.example.invalid/nueva"));
        // Al editar tampoco se permite cruzarse con otro horario propio.
        crearCorrecto("13:00", "14:00");
        editar(autorizacionTutor, id, datos("13:30", "14:30")).andExpect(status().isConflict());
    }

    @Test
    void eliminaHorarioSinSesiones() throws Exception {
        long id = crearCorrecto("10:00", "11:00");
        eliminar(autorizacionTutor, id).andExpect(status().isOk()).andExpect(jsonPath("$.eliminado").value(true));
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM disponibilidades_tutor WHERE id=?", Long.class, id)).isZero();
        eliminar(autorizacionTutor, id).andExpect(status().isNotFound());
    }

    @Test
    void muestraComoOcupadoElHorarioReservadoYNoPermiteCambiarlo() throws Exception {
        long id = crearCorrecto("10:00", "11:00");
        reservar(id, "confirmada");
        consultar(autorizacionTutor).andExpect(jsonPath("$[0].estudianteNombre").value("Estudiante de horarios"));
        editar(autorizacionTutor, id, datos("12:00", "13:00")).andExpect(status().isConflict());
        eliminar(autorizacionTutor, id).andExpect(status().isConflict());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM disponibilidades_tutor WHERE id=?", Long.class, id)).isEqualTo(1);
    }

    @Test
    void unTutorNoVeNiCambiaHorariosDeOtro() throws Exception {
        long id = crearCorrecto("10:00", "11:00");
        consultar(autorizacionOtroTutor).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        editar(autorizacionOtroTutor, id, datos("12:00", "13:00")).andExpect(status().isNotFound());
        eliminar(autorizacionOtroTutor, id).andExpect(status().isNotFound());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM disponibilidades_tutor WHERE id=?", Long.class, id)).isEqualTo(1);
    }

    @Test
    void ignoraUnTutorIdEnviadoDesdeElCliente() throws Exception {
        var solicitud = datos("10:00", "11:00");
        solicitud.put("tutorId", 999999);
        crear(autorizacionTutor, solicitud).andExpect(status().isCreated());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM disponibilidades_tutor d JOIN tutores t ON t.id=d.tutor_id WHERE t.usuario_id=?",
                Long.class, usuarioTutor)).isEqualTo(1);
    }

    @Test
    void exigeSesionYRolDeTutor() throws Exception {
        cliente.perform(get("/api/tutor/horarios")).andExpect(status().isUnauthorized());
        consultar(autorizacionEstudiante).andExpect(status().isForbidden());
        // Coordinación puede entrar a la ruta, pero sin perfil de tutor no tiene horarios propios.
        consultar(autorizacionCoordinador).andExpect(status().isForbidden());
        crear(autorizacionCoordinador, datos("10:00", "11:00")).andExpect(status().isForbidden());
    }
    @Test void filtraElIntervaloSinIncluirHorariosDeOtroTutor() throws Exception {
        long primero=crearCorrecto("09:00","10:00");crearCorrecto("11:00","12:00");
        cliente.perform(get("/api/tutor/horarios").param("desde","2031-03-10T09:30:00").param("hasta","2031-03-10T10:30:00").header("Authorization",autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(primero));
        cliente.perform(get("/api/tutor/horarios").param("desde","2031-03-10T09:30:00").param("hasta","2031-03-10T10:30:00").header("Authorization",autorizacionOtroTutor))
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test void elResumenCuentaSoloElIntervaloDelTutor() throws Exception {
        long primero=crearCorrecto("09:00","10:00");crearCorrecto("11:00","12:00");reservar(primero,"confirmada");
        cliente.perform(get("/api/tutor/horarios/resumen").param("desde","2031-03-10T08:00:00").param("hasta","2031-03-10T13:00:00").header("Authorization",autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.ocupados").value(1)).andExpect(jsonPath("$.disponibles").value(1));
        cliente.perform(get("/api/tutor/horarios/resumen").header("Authorization",autorizacionOtroTutor)).andExpect(jsonPath("$.total").value(0));
    }
    @Test void rechazaFechasIncompletasInvertidasOExcesivas() throws Exception {
        for(String parametros:List.of("desde=2031-03-10T00:00:00","desde=2031-03-11T00:00:00&hasta=2031-03-10T00:00:00","desde=2031-01-01T00:00:00&hasta=2031-04-01T00:00:00"))
            cliente.perform(get("/api/tutor/horarios?"+parametros).header("Authorization",autorizacionTutor)).andExpect(status().isUnprocessableContent());
    }

}
